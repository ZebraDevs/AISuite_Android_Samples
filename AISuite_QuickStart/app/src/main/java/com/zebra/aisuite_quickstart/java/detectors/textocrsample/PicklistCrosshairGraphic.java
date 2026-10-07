// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.detectors.textocrsample;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.zebra.aisuite_quickstart.GraphicOverlay;

/**
 * Draws a fixed "+" crosshair at the center of the preview, independent of any detection
 * result. For picklist OCR, this gives the user a simple visual target to line the word they
 * want scanned up with, rather than having to guess where the frame's center is.
 */
public class PicklistCrosshairGraphic extends GraphicOverlay.Graphic {
    private static final float ARM_LENGTH_PX = 40f;
    private static final float STROKE_WIDTH_PX = 6f;

    private final Paint crosshairPaint;

    public PicklistCrosshairGraphic(GraphicOverlay overlay) {
        super(overlay);
        crosshairPaint = new Paint();
        crosshairPaint.setColor(Color.YELLOW);
        crosshairPaint.setStrokeWidth(STROKE_WIDTH_PX);
        crosshairPaint.setStyle(Paint.Style.STROKE);
        crosshairPaint.setAntiAlias(true);
    }

    @Override
    public void draw(Canvas canvas) {
        float centerX = canvas.getWidth() / 2f;
        float centerY = canvas.getHeight() / 2f;

        canvas.drawLine(centerX - ARM_LENGTH_PX, centerY, centerX + ARM_LENGTH_PX, centerY, crosshairPaint);
        canvas.drawLine(centerX, centerY - ARM_LENGTH_PX, centerX, centerY + ARM_LENGTH_PX, crosshairPaint);
    }
}
