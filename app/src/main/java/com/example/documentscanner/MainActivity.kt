package com.example.documentscanner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.document.scanner.R
import com.example.documentscanner.data.local.AppDatabase
import com.example.documentscanner.data.model.ScannedDocument
import com.example.documentscanner.data.repository.DocumentRepository
import com.example.documentscanner.ui.history.HistoryActivity
import com.example.documentscanner.ui.main.MainViewModel
import com.example.documentscanner.ui.main.MainViewModelFactory
import android.text.SpannableStringBuilder
import android.text.Spanned
import androidx.core.content.ContextCompat
import com.github.promeg.pinyinhelper.Pinyin
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var buttonTrigger: Button
    private lateinit var btnShare: MaterialButton
    private lateinit var btnDelete: MaterialButton
    private lateinit var btnSettings: ImageButton
    private lateinit var ivPreview: ImageView
    private lateinit var tvResult: TextView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var layoutActions: LinearLayout
    private lateinit var cardResult: MaterialCardView
    private lateinit var btnShowPinyin: MaterialButton
    private var isPinyinVisible = false
    private var plainText: String? = null

    private lateinit var scannerLauncher: ActivityResultLauncher<IntentSenderRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        initializeViewModel()
        scannerLauncher = registerScannerLauncher()
        setupObservers()
        setupClickListeners()
        triggerDocumentScanner(buttonTrigger, initializeScanner(), scannerLauncher)

    }

    private fun initializeViews() {
        buttonTrigger = findViewById(R.id.btnScan)
        btnShare = findViewById(R.id.btnShare)
        btnDelete = findViewById(R.id.btnDelete)
        btnSettings = findViewById(R.id.btnSettings)
        ivPreview = findViewById(R.id.ivPreview)
        tvResult = findViewById(R.id.tvResult)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        layoutActions = findViewById(R.id.layoutActions)
        cardResult = findViewById(R.id.cardResult)
        btnShowPinyin = findViewById(R.id.btnShowPinyin)
    }

    private fun initializeViewModel() {
        val database = AppDatabase.getDatabase(this)
        val repository = DocumentRepository(database.documentDao())
        val factory = MainViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]
        //ViewUtility().initializeViewModel(viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java])
    }

    private fun setupObservers() {
        viewModel.scanState.observe(this) { state ->
            when (state) {
                is MainViewModel.ScanState.Empty -> showEmptyState()
                is MainViewModel.ScanState.Success -> showSuccessState(state.document)
                is MainViewModel.ScanState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    showEmptyState()
                }
                MainViewModel.ScanState.Scanning -> { /* Optional loading state */ }
            }
        }
    }

    private fun setupClickListeners() {
        btnShare.setOnClickListener { shareCurrentDocument() }
        btnDelete.setOnClickListener { deleteCurrentDocument() }
        btnSettings.setOnClickListener { openHistory() }
        btnShowPinyin.setOnClickListener { togglePinyin() }
    }

    private fun showEmptyState() {
        layoutEmptyState.visibility = View.VISIBLE
        layoutActions.visibility = View.GONE
        cardResult.visibility = View.GONE
        ivPreview.setImageDrawable(null)
        btnShowPinyin.visibility = View.GONE
        isPinyinVisible = false
        plainText = null
    }

    private fun showSuccessState(document: ScannedDocument) {
        layoutEmptyState.visibility = View.GONE
        layoutActions.visibility = View.VISIBLE
        cardResult.visibility = View.VISIBLE
        ivPreview.setImageURI(Uri.parse(document.firstImageUri()))

        plainText = document.extractedTextList().firstOrNull()
        tvResult.text = plainText ?: "PDF saved: ${document.displayName}"

        if (!document.extractedText.isNullOrBlank()) {
            btnShowPinyin.visibility = View.VISIBLE
        } else {
            btnShowPinyin.visibility = View.GONE
        }
        isPinyinVisible = false
        btnShowPinyin.setText(R.string.show_pinyin)
    }

    private fun shareCurrentDocument() {
        viewModel.currentDocument.value?.let { document ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, Uri.parse(document.pdfUri))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share Document"))
        }
    }

    private fun deleteCurrentDocument() {
        AlertDialog.Builder(this)
            .setTitle("Delete Document")
            .setMessage("Are you sure you want to delete this document?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteCurrentDocument()
                Toast.makeText(this, "Document deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openHistory() {
        startActivity(Intent(this, HistoryActivity::class.java))
    }

    private fun togglePinyin() {
        val text = plainText ?: return
        isPinyinVisible = !isPinyinVisible
        if (isPinyinVisible) {
            tvResult.text = buildPinyinAnnotatedText(text)
            tvResult.setLineSpacing(24f, 1.0f)
            btnShowPinyin.setText(R.string.hide_pinyin)
        } else {
            tvResult.text = text
            tvResult.setLineSpacing(4f, 1.0f)
            btnShowPinyin.setText(R.string.show_pinyin)
        }
    }

    private fun buildPinyinAnnotatedText(text: String): SpannableStringBuilder {
        val spannable = SpannableStringBuilder(text)
        val annotationColor = ContextCompat.getColor(this, R.color.primary)

        var i = text.length - 1
        while (i >= 0) {
            val char = text[i]
            if (Pinyin.isChinese(char)) {
                val pinyin = Pinyin.toPinyin(char).lowercase()
                val span = PinyinAnnotationSpan(pinyin, annotationColor)
                spannable.setSpan(span, i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            i--
        }
        return spannable
    }

    private fun initializeScanner(): GmsDocumentScanner {

        return GmsDocumentScanning.getClient(GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(false)
            .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
            .setScannerMode(SCANNER_MODE_FULL)
            .build())
    }

    private fun registerScannerLauncher() : ActivityResultLauncher<IntentSenderRequest> {

        val extractTextFromPdfs = ExtractTextFromPdfs()

        scannerLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val resultData = result.data
                if (resultData != null) {
                    val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(resultData)
                    scanResult?.pages?.let { pages ->
                        if (pages.isNotEmpty()) {
                            scanResult.pdf?.let { pdf ->
                                val pdfUri = pdf.uri
                                val allImageUris = pages.map { it.imageUri.toString() }
                                val joinedImageUris = allImageUris.joinToString("|||")
                                extractAllPagesText(extractTextFromPdfs, pages.map { it.imageUri }, 0, mutableListOf()) { extractedTexts ->
                                    val joinedText = extractedTexts.joinToString("|||")
                                    Log.i("XuaiZhe", "registerScannerLauncher: pages=${pages.size}")
                                    viewModel.saveScanResult(pdfUri, joinedImageUris, joinedText, this, pages.size)

                                }
                            }
                        }
                    }
                }
            }
        }

        return scannerLauncher
    }

    private fun extractAllPagesText(
        extractor: ExtractTextFromPdfs,
        pageUris: List<Uri>,
        index: Int,
        results: MutableList<String>,
        onDone: (List<String>) -> Unit
    ) {
        if (index >= pageUris.size) {
            onDone(results)
            return
        }
        extractor.extractText(pageUris[index], this) { text ->
            results.add(text)
            extractAllPagesText(extractor, pageUris, index + 1, results, onDone)
        }
    }


    private fun triggerDocumentScanner(buttonTrigger: Button, scanner: GmsDocumentScanner, scannerLauncher: ActivityResultLauncher<IntentSenderRequest>)  {

        buttonTrigger.setOnClickListener {
            scanner.getStartScanIntent(this)
                .addOnSuccessListener { intentSender ->
                    scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error starting scanner: ${e.message}", Toast.LENGTH_SHORT).show()
                }

        }


    }
}