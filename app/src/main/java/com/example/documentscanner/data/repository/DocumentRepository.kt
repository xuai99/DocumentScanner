package com.example.documentscanner.data.repository

import com.example.documentscanner.data.local.DocumentDao
import com.example.documentscanner.data.model.ScannedDocument
import kotlinx.coroutines.flow.Flow

class DocumentRepository(private val documentDao: DocumentDao) {

    val allDocuments: Flow<List<ScannedDocument>> = documentDao.getAllDocuments()

    suspend fun insertDocument(document: ScannedDocument): Long {
        return documentDao.insertDocument(document)
    }

    suspend fun getDocumentById(id: Long): ScannedDocument? {
        return documentDao.getDocumentById(id)
    }

    suspend fun deleteDocument(document: ScannedDocument) {
        documentDao.deleteDocument(document)
    }
}
