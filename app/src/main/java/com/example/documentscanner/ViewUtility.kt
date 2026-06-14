package com.example.documentscanner

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.documentscanner.ui.history.HistoryActivity
import com.example.documentscanner.ui.main.MainViewModel

class ViewUtility(private val viewModel: MainViewModel) {

    fun shareCurrentDocument( context: Context) {
        viewModel.currentDocument.value?.let { document ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, Uri.parse(document.pdfUri))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Document"))
        }
    }

    fun deleteCurrentDocument(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Delete Document")
            .setMessage("Are you sure you want to delete this document?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteCurrentDocument()
                Toast.makeText(context, "Document deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun openHistory(context: Context) {
        context.startActivity(Intent(context, HistoryActivity::class.java))
    }


    fun saveScanDocumentResult(pdfUri: Uri, joinedImageUris: String, joinedText: String, pages: List<Uri>, context: Context) {
        viewModel.saveScanResult(pdfUri, joinedImageUris, joinedText, context, pages.size)
    }

}

