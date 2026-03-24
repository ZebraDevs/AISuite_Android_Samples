package com.zebra.aisuite_quickstart.kotlin.detectors.warehouselocalizer

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.zebra.aisuite_quickstart.GraphicOverlay

class WareHouseLocalizerGraphic(overlay: GraphicOverlay,
                       boxes: List<Rect>?,) : GraphicOverlay.Graphic(overlay) {

    private val boxPaint: Paint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    private val boundingBoxes: MutableList<Rect> = mutableListOf()
    init {
        overlay.clear()

        boxes?.let { boundingBoxes.addAll(it) }

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
    }
}