package com.example.documentscanner.translation

import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException

sealed class TranslationError {
    object NetworkRequired : TranslationError()
    object ModelDownloadFailed : TranslationError()
    object Timeout : TranslationError()
    data class Other(val cause: Throwable) : TranslationError()
}

sealed class TranslationProgress {
    object DownloadingModel : TranslationProgress()
    object Translating : TranslationProgress()
}

class TranslationService private constructor() {
    private var translator: Translator? = null

    companion object {
        @Volatile
        private var instance: TranslationService? = null
        private const val DOWNLOAD_TIMEOUT_MS = 60_000L
        private const val TRANSLATE_TIMEOUT_MS = 15_000L

        fun getInstance(): TranslationService {
            return instance ?: synchronized(this) {
                instance ?: TranslationService().also { instance = it }
            }
        }
    }

    private fun getTranslator(): Translator {
        if (translator == null) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.CHINESE)
                .setTargetLanguage(TranslateLanguage.ENGLISH)
                .build()
            translator = Translation.getClient(options)
        }
        return translator!!
    }

    suspend fun translateToEnglish(
        text: String,
        onProgress: (TranslationProgress) -> Unit
    ): Result<String> {
        return try {
            val trans = getTranslator()
            val conditions = DownloadConditions.Builder().build()
            onProgress(TranslationProgress.DownloadingModel)
            withTimeout(DOWNLOAD_TIMEOUT_MS) {
                trans.downloadModelIfNeeded(conditions).await()
            }
            onProgress(TranslationProgress.Translating)
            val result = withTimeout(TRANSLATE_TIMEOUT_MS) {
                trans.translate(text).await()
            }
            Result.success(result)
        } catch (e: TimeoutCancellationException) {
            Result.failure(TranslationException(TranslationError.Timeout, e))
        } catch (e: MlKitException) {
            val err = if (e.errorCode == MlKitException.NETWORK_ISSUE) {
                TranslationError.NetworkRequired
            } else {
                TranslationError.ModelDownloadFailed
            }
            Result.failure(TranslationException(err, e))
        } catch (e: Exception) {
            Result.failure(TranslationException(TranslationError.Other(e), e))
        }
    }

    fun cleanup() {
        translator?.close()
        translator = null
    }
}

class TranslationException(
    val error: TranslationError,
    cause: Throwable
) : Exception(cause)
