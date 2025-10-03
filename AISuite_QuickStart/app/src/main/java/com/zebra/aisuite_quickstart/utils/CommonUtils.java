package com.zebra.aisuite_quickstart.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import androidx.camera.core.ImageProxy;

public class CommonUtils {
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
}
