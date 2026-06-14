package com.example.documentscanner.ui.detail

import android.content.Context
import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.document.scanner.R
import com.example.documentscanner.PinyinAnnotationSpan
import com.example.documentscanner.translation.TranslationError
import com.example.documentscanner.translation.TranslationException
import com.example.documentscanner.translation.TranslationProgress
import com.example.documentscanner.translation.TranslationService
import com.github.promeg.pinyinhelper.Pinyin
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DocumentPageAdapter(
    private val imageUris: List<String>,
    private val texts: List<String?>,
    private val coroutineScope: CoroutineScope,
    private val getState: (Int) -> PageState,
    private val setState: (Int, PageState) -> Unit
) : RecyclerView.Adapter<DocumentPageAdapter.PageViewHolder>() {

    private val translationService = TranslationService.getInstance()

    inner class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPreview: ImageView = view.findViewById(R.id.ivPagePreview)
        val cardText: MaterialCardView = view.findViewById(R.id.cardPageText)
        val tvText: TextView = view.findViewById(R.id.tvPageText)
        val btnShowPinyin: MaterialButton = view.findViewById(R.id.btnPageShowPinyin)
        val btnTranslate: MaterialButton = view.findViewById(R.id.btnPageTranslate)
        val layoutEmpty: LinearLayout = view.findViewById(R.id.layoutPageEmpty)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_document_page, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val imageUri = imageUris.getOrNull(position)
        val plainText = texts.getOrNull(position)

        if (!imageUri.isNullOrBlank()) {
            holder.ivPreview.setImageURI(Uri.parse(imageUri))
        }

        if (plainText.isNullOrBlank()) {
            holder.cardText.visibility = View.GONE
            holder.layoutEmpty.visibility = View.VISIBLE
            holder.btnShowPinyin.visibility = View.GONE
            holder.btnTranslate.visibility = View.GONE
            return
        }

        val hasChinese = plainText.any { Pinyin.isChinese(it) }
        holder.cardText.visibility = View.VISIBLE
        holder.layoutEmpty.visibility = View.GONE
        holder.btnShowPinyin.visibility = if (hasChinese) View.VISIBLE else View.GONE
        holder.btnTranslate.visibility = if (hasChinese) View.VISIBLE else View.GONE

        val state = getState(position)
        val ctx = holder.itemView.context

        when {
            state.isTranslating -> {
                holder.tvText.text = plainText
                holder.tvText.setLineSpacing(4f, 1.0f)
                holder.btnShowPinyin.isEnabled = false
                holder.btnShowPinyin.setText(R.string.show_pinyin)
                holder.btnTranslate.isEnabled = false
                holder.btnTranslate.text = if (state.downloadingModel) {
                    ctx.getString(R.string.downloading_model)
                } else {
                    ctx.getString(R.string.translating)
                }
            }
            state.translationShown -> {
                holder.tvText.text = state.translatedText
                    ?: error("translationShown=true but translatedText=null at pos $position")
                holder.tvText.setLineSpacing(4f, 1.0f)
                holder.btnTranslate.setText(R.string.show_original)
                holder.btnTranslate.isEnabled = true
                holder.btnShowPinyin.isEnabled = false
                holder.btnShowPinyin.setText(R.string.show_pinyin)
            }
            state.pinyinShown -> {
                holder.tvText.text = buildPinyinText(plainText, holder)
                holder.tvText.setLineSpacing(24f, 1.0f)
                holder.btnShowPinyin.setText(R.string.hide_pinyin)
                holder.btnShowPinyin.isEnabled = true
                holder.btnTranslate.isEnabled = true
                holder.btnTranslate.setText(R.string.translate_to_english)
            }
            else -> {
                holder.tvText.text = plainText
                holder.tvText.setLineSpacing(4f, 1.0f)
                holder.btnShowPinyin.setText(R.string.show_pinyin)
                holder.btnShowPinyin.isEnabled = true
                holder.btnTranslate.isEnabled = true
                holder.btnTranslate.setText(R.string.translate_to_english)
            }
        }

        holder.btnShowPinyin.setOnClickListener {
            val current = getState(position)
            if (!current.translationShown && !current.isTranslating) {
                setState(position, current.copy(pinyinShown = !current.pinyinShown))
                notifyItemChanged(position)
            }
        }

        holder.btnTranslate.setOnClickListener {
            val current = getState(position)
            when {
                current.isTranslating -> Unit
                current.translationShown -> {
                    setState(position, current.copy(translationShown = false))
                    notifyItemChanged(position)
                }
                else -> startTranslation(position, plainText, ctx)
            }
        }
    }

    override fun getItemCount() = imageUris.size

    private fun startTranslation(position: Int, text: String, ctx: Context) {
        setState(position, getState(position).copy(
            isTranslating = true,
            downloadingModel = true,
            pinyinShown = false
        ))
        notifyItemChanged(position)

        coroutineScope.launch {
            translationService.translateToEnglish(text) { progress ->
                setState(position, getState(position).copy(
                    downloadingModel = progress is TranslationProgress.DownloadingModel
                ))
                notifyItemChanged(position)
            }.fold(
                onSuccess = { translated ->
                    setState(position, PageState(
                        pinyinShown = false,
                        translationShown = true,
                        isTranslating = false,
                        downloadingModel = false,
                        translatedText = translated
                    ))
                    notifyItemChanged(position)
                },
                onFailure = { error ->
                    val msgRes = (error as? TranslationException)?.error?.let {
                        when (it) {
                            TranslationError.NetworkRequired -> R.string.translation_error_network
                            TranslationError.Timeout -> R.string.translation_error_timeout
                            TranslationError.ModelDownloadFailed -> R.string.translation_error_model
                            is TranslationError.Other -> R.string.translation_error_generic
                        }
                    } ?: R.string.translation_error_generic
                    Toast.makeText(ctx, msgRes, Toast.LENGTH_LONG).show()
                    setState(position, getState(position).copy(
                        isTranslating = false,
                        downloadingModel = false
                    ))
                    notifyItemChanged(position)
                }
            )
        }
    }

    private fun buildPinyinText(text: String, holder: PageViewHolder): SpannableStringBuilder {
        val spannable = SpannableStringBuilder(text)
        val color = ContextCompat.getColor(holder.itemView.context, R.color.primary)
        var i = text.length - 1
        while (i >= 0) {
            val char = text[i]
            if (Pinyin.isChinese(char)) {
                val pinyin = Pinyin.toPinyin(char).lowercase()
                spannable.setSpan(PinyinAnnotationSpan(pinyin, color), i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            i--
        }
        return spannable
    }
}
