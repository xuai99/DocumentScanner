package com.example.documentscanner.ui.main

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.documentscanner.data.model.ScannedDocument
import com.example.documentscanner.data.repository.DocumentRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(private val repository: DocumentRepository) : ViewModel() {

    private val _currentDocument = MutableLiveData<ScannedDocument?>()
    val currentDocument: LiveData<ScannedDocument?> = _currentDocument

    private val _scanState = MutableLiveData<ScanState>(ScanState.Empty)
    val scanState: LiveData<ScanState> = _scanState

    sealed class ScanState {
        object Empty : ScanState()
        object Scanning : ScanState()
        data class Success(val document: ScannedDocument) : ScanState()
        data class Error(val message: String) : ScanState()
    }

    fun saveScanResult(pdfUri: Uri, imageUri: String, extractedText: String?, context: Context, pageCount: Int = 1) {
        viewModelScope.launch {
            try {
                val fileName = "Document_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"
                val fileSize = getFileSize(context, pdfUri)

                val document = ScannedDocument(
                    fileName = fileName,
                    displayName = fileName,
                    pdfUri = pdfUri.toString(),
                    imageUri = imageUri,
                    extractedText = extractedText,
                    pageCount = pageCount,
                    fileSizeBytes = fileSize
                )

                val id = repository.insertDocument(document)
                val savedDocument = repository.getDocumentById(id)
                Log.i("DocumentScanner", "saveScanResult: ${savedDocument?.pdfUri}")
                _currentDocument.postValue(savedDocument)
                _scanState.postValue(ScanState.Success(savedDocument!!))
            } catch (e: Exception) {
                _scanState.postValue(ScanState.Error(e.message ?: "Failed to save document"))
            }
        }
    }

    fun deleteCurrentDocument() {
        viewModelScope.launch {
            _currentDocument.value?.let { document ->
                repository.deleteDocument(document)
                clearCurrentDocument()
            }
        }
    }

    fun clearCurrentDocument() {
        _currentDocument.value = null
        _scanState.value = ScanState.Empty
    }

    private fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use {
                it.statSize
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}

class MainViewModelFactory(
    private val repository: DocumentRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
