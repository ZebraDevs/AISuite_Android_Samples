// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.analyzers.customdetector

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.zebra.aisuite_quickstart.GraphicOverlay

/**
 * CustomDetectionGraphic renders bounding boxes for all four custom detector model outputs.
 * Each model type is drawn in a distinct color so results are visually distinct:
 *
 *   Cyan   — Barcode Decoder
 *   Yellow — ML Kit OCR
 *   Green  — YOLO Object Detection
 *   Red    — MobileNet SSD Object Detection
 *
 * All bounding boxes must already be mapped to overlay coordinates by the caller via
 * BoundingBoxMapper.mapBoundingBoxToOverlay() before being passed to this graphic.
 */
class CustomDetectionGraphic(
    overlay                        : GraphicOverlay,
    private val barcodeRects       : List<Rect>,
    private val barcodeLabels      : List<String>,
    private val ocrRects           : List<Rect>,
    private val ocrLabels          : List<String>,
    private val yoloRects          : List<Rect>,
    private val mobileNetRects     : List<Rect>
) : GraphicOverlay.Graphic(overlay) {

    private val strokePaint = Paint().apply {
        style       = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }
    private val labelPaint = Paint().apply {
        textSize       = 36f
        isAntiAlias    = true
        isFakeBoldText = true
    }

    override fun draw(canvas: Canvas) {
        barcodeRects.forEachIndexed { i, box ->
            drawBox(canvas, box, Color.CYAN)
            val label = barcodeLabels.getOrElse(i) { "" }
            if (label.isNotEmpty()) drawLabel(canvas, label, box, Color.CYAN)
        }
        ocrRects.forEachIndexed { i, box ->
            drawBox(canvas, box, Color.YELLOW)
            val label = ocrLabels.getOrElse(i) { "" }
            if (label.isNotEmpty()) drawLabel(canvas, label, box, Color.YELLOW)
        }
        for (box in yoloRects)      drawBox(canvas, box, Color.GREEN)
        for (box in mobileNetRects) drawBox(canvas, box, Color.RED)
    }

    private fun drawBox(canvas: Canvas, box: Rect, color: Int) {
        strokePaint.color = color
        canvas.drawRect(box, strokePaint)
    }

    private fun drawLabel(canvas: Canvas, text: String, box: Rect, color: Int) {
        labelPaint.color = color
        canvas.drawText(
            if (text.length > 20) text.substring(0, 20) + "…" else text,
            box.left.toFloat(),
            (box.top - 8).toFloat(),
            labelPaint
        )
    }
}
