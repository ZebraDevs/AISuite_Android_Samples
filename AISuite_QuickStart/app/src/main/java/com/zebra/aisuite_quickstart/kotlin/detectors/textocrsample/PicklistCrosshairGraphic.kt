// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.detectors.textocrsample

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.zebra.aisuite_quickstart.GraphicOverlay

/**
 * Draws a fixed "+" crosshair at the center of the preview, independent of any detection
 * result. For picklist OCR, this gives the user a simple visual target to line the word they
 * want scanned up with, rather than having to guess where the frame's center is.
 */
class PicklistCrosshairGraphic(overlay: GraphicOverlay) : GraphicOverlay.Graphic(overlay) {

    companion object {
        private const val ARM_LENGTH_PX = 40f
        private const val STROKE_WIDTH_PX = 6f
    }

    private val crosshairPaint = Paint().apply {
        color = Color.YELLOW
        strokeWidth = STROKE_WIDTH_PX
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    override fun draw(canvas: Canvas) {
        val centerX = canvas.width / 2f
        val centerY = canvas.height / 2f

        canvas.drawLine(centerX - ARM_LENGTH_PX, centerY, centerX + ARM_LENGTH_PX, centerY, crosshairPaint)
        canvas.drawLine(centerX, centerY - ARM_LENGTH_PX, centerX, centerY + ARM_LENGTH_PX, crosshairPaint)
    }
}
