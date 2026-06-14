package com.example.documentscanner.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scanned_documents")
data class ScannedDocument(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val displayName: String,
    val pdfUri: String,
    val imageUri: String,
    val extractedText: String? = null,
    val pageCount: Int = 1,
    val fileSizeBytes: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
) {
    fun imageUriList(): List<String> = imageUri.split("|||").filter { it.isNotBlank() }
    fun extractedTextList(): List<String?> {
        if (extractedText.isNullOrBlank()) return List(imageUriList().size) { null }
        return extractedText.split("|||").map { it.ifBlank { null } }
    }
    fun firstImageUri(): String = imageUriList().firstOrNull() ?: imageUri
}
