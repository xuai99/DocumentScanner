package com.example.documentscanner.ui.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.documentscanner.data.model.ScannedDocument
import com.example.documentscanner.data.repository.DocumentRepository
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: DocumentRepository) : ViewModel() {

    val allDocuments: LiveData<List<ScannedDocument>> =
        repository.allDocuments.asLiveData()

    fun deleteDocument(document: ScannedDocument) {
        viewModelScope.launch {
            repository.deleteDocument(document)
        }
    }
}

class HistoryViewModelFactory(
    private val repository: DocumentRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            return HistoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
