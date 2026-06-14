package com.example.documentscanner

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import javax.security.auth.callback.Callback

class ExtractTextFromPdfs {

    private val TAG = "XuaiZhe";
    private var pdfStringInfo = ""

    fun extractText(imageUri: Uri, context: Context, callback: (String) -> Unit): String {

        val pdfPath = InputImage.fromFilePath( context , imageUri)

        val textRecognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

        textRecognizer.process(pdfPath).addOnSuccessListener { text ->
            callback(text.text)
        }.addOnFailureListener { e ->
            pdfStringInfo = e.message.toString()
        }

        return  pdfStringInfo
    }
}