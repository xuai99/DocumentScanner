package com.example.documentscanner.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.documentscanner.data.model.ScannedDocument
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM scanned_documents ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<ScannedDocument>>

    @Query("SELECT * FROM scanned_documents WHERE id = :id")
    suspend fun getDocumentById(id: Long): ScannedDocument?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: ScannedDocument): Long

    @Delete
    suspend fun deleteDocument(document: ScannedDocument)

    @Query("DELETE FROM scanned_documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Long)
}
