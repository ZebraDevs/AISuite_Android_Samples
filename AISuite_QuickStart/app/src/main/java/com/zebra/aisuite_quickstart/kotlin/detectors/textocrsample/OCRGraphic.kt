// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.detectors.textocrsample

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.zebra.aisuite_quickstart.GraphicOverlay
import java.lang.Math.abs

/**
 * The OCRGraphic class extends GraphicOverlay.Graphic and is responsible for rendering visual
 * elements on a canvas to represent detected text from an OCR (Optical Character Recognition)
 * process. This includes drawing bounding boxes around detected text areas and displaying the
 * recognized text within these boxes.

 * This class uses Android's Canvas and Paint classes to perform drawing operations, allowing
 * for the visual representation of text detection results in an Android application.

 * Usage:
 * - Instantiate the OCRGraphic with a reference to the GraphicOverlay, along with lists of
 *   bounding boxes and recognized text strings.
 * - The draw(Canvas) method is called to render the graphics on the screen.

 * Dependencies:
 * - GraphicOverlay: A custom view component that manages the drawing of multiple graphics
 *   on top of a camera preview or other content.
 * - Android Paint and Canvas classes: Used to perform drawing operations.

 * Note: This class is typically used in conjunction with an OCR detection system to visually
 * display the results of the detection process in an Android application.
 */
class OCRGraphic(
    overlay: GraphicOverlay,
    boxes: List<Rect>?,
    decodedStrings: List<String>?
) : GraphicOverlay.Graphic(overlay) {

    private val boxPaint: Paint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val contentTextPaint: Paint = Paint().apply {
        color = Color.WHITE
        alpha = 255
        textSize = 36f
    }

    private val boundingBoxes: MutableList<Rect> = mutableListOf()
    private val decodedValues: MutableList<String> = mutableListOf()

    init {

        boxes?.let { boundingBoxes.addAll(it) }
        decodedStrings?.let { decodedValues.addAll(it) }
    }

    /**
     * Draws the bounding boxes and recognized text on the given Canvas.
     *
     * @param canvas The canvas on which to draw the graphic.
     */
    override fun draw(canvas: Canvas) {
        for (rect in boundingBoxes) {
            canvas.drawRect(rect, boxPaint)
        }

        for (i in decodedValues.indices) {
            getTextSizeWithinBounds(
                decodedValues[i],
                boundingBoxes[i].left.toFloat(),
                boundingBoxes[i].top.toFloat(),
                boundingBoxes[i].right.toFloat(),
                boundingBoxes[i].bottom.toFloat(),
                contentTextPaint
            )
            canvas.drawText(
                decodedValues[i],
                boundingBoxes[i].left.toFloat(),
                boundingBoxes[i].bottom.toFloat(),
                contentTextPaint
            )
        }
        // Redraw the overlay, as this graphic has been added.
        postInvalidate()
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
    private fun getTextSizeWithinBounds(
        text: String,
        minX: Float,
        minY: Float,
        maxX: Float,
        maxY: Float,
        paint: Paint
    ) {
        // Define the maximum width and height the text should fit into
        val maxWidth = kotlin.math.abs(maxX - minX)
        val maxHeight = kotlin.math.abs(maxY - minY)

        // Start with a reasonable text size
        var textSize = 100f // Initial text size
        paint.textSize = textSize

        // Create a Rect to store text bounds
        val textBounds = Rect()

        // Measure text and adjust size
        paint.getTextBounds(text, 0, text.length, textBounds)

        while ((textBounds.width() > maxWidth || textBounds.height() > maxHeight) && textSize > 0) {
            textSize -= 1 // Decrease the text size
            paint.textSize = textSize
            paint.getTextBounds(text, 0, text.length, textBounds)
        }
    }
}
