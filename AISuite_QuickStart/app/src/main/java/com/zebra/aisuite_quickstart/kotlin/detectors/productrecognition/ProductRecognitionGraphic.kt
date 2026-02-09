// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.detectors.productrecognition

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.zebra.aisuite_quickstart.GraphicOverlay

/**
 * The ProductRecognitionGraphic class extends GraphicOverlay.Graphic and is responsible for
 * rendering visual elements on a canvas to represent detected product labels, pegs, shelves,
 * and recognized products. This includes drawing bounding boxes and displaying associated text
 * labels within these boxes.

 * This class uses Android's Canvas and Paint classes to perform drawing operations, allowing for
 * the visual representation of product recognition results in an Android application.

 * Usage:
 * - Instantiate the ProductRecognitionGraphic with a reference to the GraphicOverlay, along with
 *   lists of bounding boxes for label shelves, label pegs, shelves, recognized products, and a list
 *   of decoded product names.
 * - The draw(Canvas) method is called to render the graphics on the screen.

 * Dependencies:
 * - GraphicOverlay: A custom view component that manages the drawing of multiple graphics on top of
 *   a camera preview or other content.
 * - Android Paint and Canvas classes: Used to perform drawing operations.

 * Note: This class is typically used in conjunction with a product recognition system to visually
 * display the results of the recognition process in an Android application.
 */
class ProductRecognitionGraphic(
    overlay: GraphicOverlay,
    labelShelfRects: List<Rect>?,
    labelPegRects: List<Rect>?,
    shelfRects: List<Rect>?,
    recognizedRects: List<Rect>?,
    decodedStrings: List<String>?
) : GraphicOverlay.Graphic(overlay) {

    private val boxPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val labelShelfPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        alpha = 255
        strokeWidth = 6f
    }

    private val shelfPaint = Paint().apply {
        color = Color.MAGENTA
        style = Paint.Style.STROKE
        alpha = 255
        strokeWidth = 6f
    }

    private val contentTextPaint = Paint().apply {
        color = Color.WHITE
        alpha = 255
        textSize = 20f
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        alpha = 255
        textSize = 20f
    }

    private val labelShelfBBoxes = mutableListOf<Rect>().apply {
        labelShelfRects?.let { addAll(it) }
    }

    private val labelPegBBoxes = mutableListOf<Rect>().apply {
        labelPegRects?.let { addAll(it) }
    }

    private val shelfBBoxes = mutableListOf<Rect>().apply {
        shelfRects?.let { addAll(it) }
    }

    private val productBBoxes = mutableListOf<Rect>().apply {
        recognizedRects?.let { addAll(it) }
    }

    private val decodedProducts = mutableListOf<String>().apply {
        decodedStrings?.let { addAll(it) }
    }

    init {
        postInvalidate() // Redraw the overlay, as this graphic has been added.
    }

    /**
     * Draws the bounding boxes and associated text labels for detected products, shelves, and labels on the given Canvas.
     *
     * @param canvas The canvas on which to draw the graphic.
     */
    override fun draw(canvas: Canvas) {
        labelShelfBBoxes.forEach { rect ->
            canvas.drawRect(rect, labelShelfPaint)
            canvas.drawText("LabelShelf", rect.left.toFloat(), rect.bottom.toFloat(), textPaint)
        }

        labelPegBBoxes.forEach { rect ->
            canvas.drawRect(rect, labelShelfPaint)
            canvas.drawText("LabelPeg", rect.left.toFloat(), rect.bottom.toFloat(), textPaint)
        }

        shelfBBoxes.forEach { rect ->
            canvas.drawRect(rect, shelfPaint)
            // Calculate the center of the rectangle
            val centerX = rect.left + (rect.width() / 2)
            val centerY = rect.top + rect.height()
            canvas.drawText("Shelf", centerX.toFloat(), centerY.toFloat(), textPaint)
        }

        decodedProducts.forEachIndexed { index, product ->
            val rect = productBBoxes[index]
            canvas.drawRect(rect, boxPaint)
            getTextSizeWithinBounds(product, rect.left.toFloat(), rect.top.toFloat(), rect.right.toFloat(), rect.bottom.toFloat(), contentTextPaint)
            canvas.drawText(product, rect.left.toFloat(), rect.bottom.toFloat(), contentTextPaint)
        }
    }

    /**
     * Adjusts the text size to ensure that it fits within the specified bounds.
     *
     * @param text The text to be drawn.
     * @param minX The minimum x-coordinate of the bounding area.
     * @param minY The minimum y-coordinate of the bounding area.
     * @param maxX The maximum x-coordinate of the bounding area.
     * @param maxY The maximum y-coordinate of the bounding area.
     * @param paint The Paint object used for drawing the text.
     */
    private fun getTextSizeWithinBounds(text: String, minX: Float, minY: Float, maxX: Float, maxY: Float, paint: Paint) {
        val maxWidth = maxX - minX
        val maxHeight = maxY - minY

        var textSize = 100f // Initial text size
        paint.textSize = textSize

        val textBounds = Rect()

        paint.getTextBounds(text, 0, text.length, textBounds)

        while ((textBounds.width() > maxWidth || textBounds.height() > maxHeight) && textSize > 0) {
            textSize -= 1 // Decrease the text size
            paint.textSize = textSize
            paint.getTextBounds(text, 0, text.length, textBounds)
        }
    }
}