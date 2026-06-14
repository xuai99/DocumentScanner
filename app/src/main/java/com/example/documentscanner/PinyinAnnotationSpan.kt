package com.example.documentscanner

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan

class PinyinAnnotationSpan(
    private val pinyin: String,
    private val annotationColor: Int
) : ReplacementSpan() {

    private val annotationScale = 0.5f

    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        val charWidth = paint.measureText(text, start, end).toInt()
        val pinyinWidth = (paint.measureText(pinyin) * annotationScale).toInt()
        val width = maxOf(charWidth, pinyinWidth)

        if (fm != null) {
            val originalFm = paint.fontMetricsInt
            val annotationHeight = ((originalFm.bottom - originalFm.top) * annotationScale).toInt()
            fm.top = originalFm.top - annotationHeight - 4
            fm.ascent = originalFm.ascent - annotationHeight - 4
            fm.bottom = originalFm.bottom
            fm.descent = originalFm.descent
        }

        return width
    }

    override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {

        val charWidth = paint.measureText(text, start, end)
        val pinyinWidth = paint.measureText(pinyin) * annotationScale
        val spanWidth = maxOf(charWidth, pinyinWidth)

        val charX = x + (spanWidth - charWidth) / 2f
        canvas.drawText(text, start, end, charX, y.toFloat(), paint)

        val originalSize = paint.textSize
        val originalColor = paint.color
        paint.textSize = originalSize * annotationScale
        paint.color = annotationColor

        val pinyinX = x + (spanWidth - pinyinWidth) / 2f
        val pinyinY = y.toFloat() - originalSize - 2f

        canvas.drawText(pinyin, pinyinX, pinyinY, paint)

        paint.textSize = originalSize
        paint.color = originalColor
    }
}
