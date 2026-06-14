package com.example.documentscanner.ui.detail

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.documentscanner.translation.TranslationService
import com.document.scanner.R
import com.example.documentscanner.data.local.AppDatabase
import com.example.documentscanner.data.model.ScannedDocument
import com.example.documentscanner.data.repository.DocumentRepository
import com.google.android.material.appbar.MaterialToolbar
import java.io.File

class DocumentDetailActivity : AppCompatActivity() {

    private lateinit var viewModel: DocumentDetailViewModel
    private lateinit var toolbar: MaterialToolbar
    private lateinit var btnMore: ImageButton
    private lateinit var viewPager: ViewPager2
    private lateinit var tvPageIndicator: TextView

    private var currentDocument: ScannedDocument? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document_detail)

        toolbar = findViewById(R.id.toolbar)
        btnMore = findViewById(R.id.btnMore)
        viewPager = findViewById(R.id.viewPager)
        tvPageIndicator = findViewById(R.id.tvPageIndicator)

        toolbar.setNavigationOnClickListener { finish() }
        btnMore.setOnClickListener { showOptionsMenu(it) }

        val database = AppDatabase.getDatabase(this)
        val repository = DocumentRepository(database.documentDao())
        val factory = DocumentDetailViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[DocumentDetailViewModel::class.java]

        val documentId = intent.getLongExtra("DOCUMENT_ID", -1L)
        if (documentId == -1L) {
            Toast.makeText(this, "Invalid document", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        viewModel.loadDocument(documentId)
        viewModel.document.observe(this) { document ->
            if (document == null) {
                Toast.makeText(this, "Document not found", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                displayDocument(document)
            }
        }
    }

    private fun displayDocument(document: ScannedDocument) {
        currentDocument = document
        toolbar.title = document.displayName

        val imageUris = document.imageUriList()
        val texts = document.extractedTextList()

        val adapter = DocumentPageAdapter(
            imageUris = imageUris,
            texts = texts,
            coroutineScope = lifecycleScope,
            getState = { pos -> viewModel.getPageState(pos) },
            setState = { pos, state -> viewModel.updatePageState(pos, state) }
        )
        viewPager.adapter = adapter

        if (imageUris.size > 1) {
            tvPageIndicator.visibility = View.VISIBLE
            tvPageIndicator.text = "Page 1 / ${imageUris.size}"
            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    tvPageIndicator.text = "Page ${position + 1} / ${imageUris.size}"
                }
            })
        } else {
            tvPageIndicator.visibility = View.GONE
        }
    }

    private fun showOptionsMenu(anchorView: View) {
        val document = currentDocument ?: return
        val popup = PopupMenu(this, anchorView)
        popup.menuInflater.inflate(R.menu.document_options_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_open_pdf -> {
                    openPdfExternally(document)
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

    private fun openPdfExternally(document: ScannedDocument) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            val pdfUri = FileProvider.getUriForFile(
                applicationContext,
                "${packageName}.FileProvider",
                File(document.pdfUri.removePrefix("file://"))
            )
            setDataAndType(pdfUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No app found to open PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareDocument(document: ScannedDocument) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            val pdfUri = FileProvider.getUriForFile(
                applicationContext,
                "${packageName}.FileProvider",
                File(document.pdfUri.removePrefix("file://"))
            )
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share Document"))
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            TranslationService.getInstance().cleanup()
        }
    }

    private fun deleteDocument(document: ScannedDocument) {
        AlertDialog.Builder(this)
            .setTitle("Delete Document")
            .setMessage("Delete ${document.displayName}?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteDocument()
                Toast.makeText(this, "Document deleted", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
