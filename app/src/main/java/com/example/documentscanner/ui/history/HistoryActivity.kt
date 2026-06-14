package com.example.documentscanner.ui.history

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.document.scanner.R
import com.example.documentscanner.data.local.AppDatabase
import com.example.documentscanner.data.model.ScannedDocument
import com.example.documentscanner.data.repository.DocumentRepository
import com.google.android.material.appbar.MaterialToolbar
import androidx.core.net.toUri
import java.io.File

class HistoryActivity : AppCompatActivity() {

    private lateinit var viewModel: HistoryViewModel
    private lateinit var adapter: HistoryAdapter
    private lateinit var rvHistory: RecyclerView
    private lateinit var layoutEmptyHistory: LinearLayout

    private lateinit var shareDocument: Uri;

    private val TAG = "HistoryActivity - "

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        initializeViews()
        initializeViewModel()
        setupRecyclerView()
        setupObservers()
        setupToolbar()
    }

    private fun initializeViews() {
        rvHistory = findViewById(R.id.rvHistory)
        layoutEmptyHistory = findViewById(R.id.layoutEmptyHistory)
    }

    private fun initializeViewModel() {
        val database = AppDatabase.getDatabase(this)
        val repository = DocumentRepository(database.documentDao())
        val factory = HistoryViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[HistoryViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter(
            onItemClick = { document -> openDocumentDetail(document) },
            onMoreClick = { document, view -> showOptionsMenu(document, view) }
        )
        rvHistory.layoutManager = LinearLayoutManager(this)
        rvHistory.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.allDocuments.observe(this) { documents ->
            layoutEmptyHistory.visibility = if (documents.isEmpty()) View.VISIBLE else View.GONE
            rvHistory.visibility = if (documents.isEmpty()) View.GONE else View.VISIBLE
            adapter.submitList(documents)
        }
    }

    private fun setupToolbar() {
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
    }

    private fun openDocumentDetail(document: ScannedDocument) {
        val intent = Intent(this, com.example.documentscanner.ui.detail.DocumentDetailActivity::class.java).apply {
            putExtra("DOCUMENT_ID", document.id)
        }
        startActivity(intent)
    }

    private fun openDocument(document: ScannedDocument) {
        val intent = Intent(this, com.example.documentscanner.ui.detail.DocumentDetailActivity::class.java).apply {
            putExtra("DOCUMENT_ID", document.id)
        }
        startActivity(intent)
    }

    private fun showOptionsMenu(document: ScannedDocument, anchorView: View) {
        val popup = PopupMenu(this, anchorView)
        popup.menuInflater.inflate(R.menu.document_options_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_open_pdf -> {
                    openDocument(document)
                    true
                }

                R.id.action_share -> {
                    shareDocument(document)
                    true
                }

                R.id.action_delete -> {
                    deleteDocument(document)
                    true
                }

                else -> false
            }
        }
        popup.show()
    }

    private fun shareDocument(document: ScannedDocument) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            shareDocument = FileProvider.getUriForFile(applicationContext, "${packageName}.FileProvider", File(removePrefixOnFile(document))
            )
            putExtra(Intent.EXTRA_STREAM, shareDocument)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share Document"))
    }

    private fun deleteDocument(document: ScannedDocument) {
        AlertDialog.Builder(this)
            .setTitle("Delete Document")
            .setMessage("Delete ${document.displayName}?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteDocument(document)
                Toast.makeText(this, "Document deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun removePrefixOnFile( document : ScannedDocument): String {
        return document.pdfUri.removePrefix("file://")
    }

}


