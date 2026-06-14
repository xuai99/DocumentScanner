package com.example.documentscanner.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.documentscanner.data.model.ScannedDocument
import com.example.documentscanner.data.repository.DocumentRepository
import kotlinx.coroutines.launch

data class PageState(
    val pinyinShown: Boolean = false,
    val translationShown: Boolean = false,
    val isTranslating: Boolean = false,
    val downloadingModel: Boolean = false,
    val translatedText: String? = null
)

class DocumentDetailViewModel(private val repository: DocumentRepository) : ViewModel() {

    private val _document = MutableLiveData<ScannedDocument?>()
    val document: LiveData<ScannedDocument?> = _document

    private val pageStates = mutableMapOf<Int, PageState>()

    fun loadDocument(documentId: Long) {
        viewModelScope.launch {
            val doc = repository.getDocumentById(documentId)
            _document.postValue(doc)
        }
    }

    fun deleteDocument() {
        viewModelScope.launch {
            _document.value?.let { doc ->
                repository.deleteDocument(doc)
            }
        }
    }

    fun getPageState(position: Int): PageState =
        pageStates[position] ?: PageState()

    fun updatePageState(position: Int, state: PageState) {
        pageStates[position] = state
    }
}
