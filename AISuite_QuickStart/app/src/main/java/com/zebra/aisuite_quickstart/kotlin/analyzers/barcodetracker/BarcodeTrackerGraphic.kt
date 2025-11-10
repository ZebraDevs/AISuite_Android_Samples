// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.analyzers.barcodetracker

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.zebra.aisuite_quickstart.GraphicOverlay

/**
 * The BarcodeTrackerGraphic class extends GraphicOverlay.Graphic and is responsible for
 * rendering visual elements on a canvas to represent detected barcodes. This includes
 * drawing bounding boxes around detected barcodes and displaying the decoded text associated
 * with each barcode.
 *
 * This class uses Android's Canvas and Paint classes to perform drawing operations, allowing
 * for the visual representation of barcode detection results in an Android application.
 *
 * Usage:
 * - Instantiate the BarcodeTrackerGraphic with a reference to the GraphicOverlay, and lists
 *   of bounding boxes and decoded strings.
 * - The draw(Canvas) method is called to render the graphics on the screen.
 *
 * Dependencies:
 * - GraphicOverlay: A custom view component that manages the drawing of multiple graphics
 *   on top of a camera preview or other content.
 * - Android Paint and Canvas classes: Used to perform drawing operations.
 *
 * Note: This class is typically used in conjunction with a barcode detection system to visually
 * display the results of the detection process in an Android application.
 */
class BarcodeTrackerGraphic(
    overlay: GraphicOverlay,
    boxes: List<Rect>?,
    decodedStrings: List<String>?
) : GraphicOverlay.Graphic(overlay) {

    private val boxPaint: Paint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val contentRectPaint: Paint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        strokeWidth = 6f
    }

    private val contentTextPaint: Paint = Paint().apply {
        color = Color.DKGRAY
        alpha = 255
        textSize = 36F
    }

    private val boundingBoxes: MutableList<Rect> = mutableListOf()
    private val contentRectBoxes: MutableList<Rect> = mutableListOf()
    private val decodedValues: MutableList<String> = mutableListOf()
    private val contentPadding = 25

    init {
       // overlay.clear()

        boxes?.let { boundingBoxes.addAll(it) }
        decodedStrings?.let { decodedValues.addAll(it) }

        for (i in boundingBoxes.indices) {
            val textWidth = contentTextPaint.measureText(decodedValues[i]).toInt()
            contentRectBoxes.add(Rect(
                boundingBoxes[i].left,
                boundingBoxes[i].bottom + contentPadding / 2,
                boundingBoxes[i].left + textWidth + contentPadding * 2,
                boundingBoxes[i].bottom + contentTextPaint.textSize.toInt() + contentPadding
            ))
        }

        // Redraw the overlay, as this graphic has been added.
        postInvalidate()
    }

    /**
     * Draws the bounding boxes and decoded text on the given Canvas.
     *
     * @param canvas The canvas on which to draw the graphic.
     */
    override fun draw(canvas: Canvas) {
        for (rect in boundingBoxes) {
            canvas.drawRect(rect, boxPaint)
        }

        // Draw the text content of the barcode
        for (i in decodedValues.indices) {
            if (decodedValues[i].trim().isNotEmpty()) {
                // Draw the rectangle for barcode content
                canvas.drawRect(contentRectBoxes[i], contentRectPaint)

                canvas.drawText(
                    decodedValues[i],
                    boundingBoxes[i].left + contentPadding.toFloat(),
                    boundingBoxes[i].bottom + contentPadding * 2.toFloat(),
                    contentTextPaint
                )
            }
        }
    }
}
