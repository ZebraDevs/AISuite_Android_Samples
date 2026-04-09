// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import static java.lang.Math.abs;

import androidx.camera.core.ImageProxy;

public class CommonUtils {
    public static final String PREFS_NAME ="FilterPreferences";
    public static final String PREFS_NAME_KOTLIN ="FilterPreferencesKotlin";
    public static final String WAREHOUSE_LOCALIZER = "Warehouse Localizer(beta)";
    /**
     * Rotates the bitmap of the given ImageProxy if needed based on its rotation metadata.
     *
     * @param imageProxy The ImageProxy to be converted and possibly rotated.
     * @return The rotated Bitmap.
     */
    public static Bitmap rotateBitmapIfNeeded(ImageProxy imageProxy) {
        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
        return rotateBitmap(imageProxy.toBitmap(), rotationDegrees);
    }

    /**
     * Rotates the given bitmap by the specified number of degrees.
     *
     * @param bitmap The bitmap to be rotated.
     * @param degrees The degrees to rotate the bitmap.
     * @return The rotated Bitmap.
     */
    private static Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
        if (degrees == 0) return bitmap;

        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
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
    public static void getTextSizeWithinBounds(Canvas canvas, String text, float minX, float minY, float maxX, float maxY, Paint paint) {
        // Define the maximum width and height the text should fit into
        float maxWidth = abs(maxX - minX);
        float maxHeight = abs(maxY - minY);

        // Start with a reasonable text size
        float textSize = 150f; // Initial text size
        paint.setTextSize(textSize);

        // Create a Rect to store text bounds
        Rect textBounds = new Rect();

        // Measure text and adjust size
        paint.getTextBounds(text, 0, text.length(), textBounds);

        while ((textBounds.width() > maxWidth || textBounds.height() > maxHeight) && textSize > 0) {
            textSize -= 1; // Decrease the text size
            paint.setTextSize(textSize);
            paint.getTextBounds(text, 0, text.length(), textBounds);
        }

        Log.v("Text and size", text + " " + textSize);

        // Calculate x and y coordinates to center the text within the bounding box
        float textX = minX + (maxWidth - textBounds.width()) / 2;
        float textY = minY + (maxHeight + textBounds.height()) / 2;

        // Draw the text on the canvas
        //   Log.v("text and coords ",text+" "+textX+" "+textY);
        canvas.drawText(text, textX, textY, paint);
    }
}
