package com.example.documentscanner.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.documentscanner.data.repository.DocumentRepository

class DocumentDetailViewModelFactory(
    private val repository: DocumentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DocumentDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DocumentDetailViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
