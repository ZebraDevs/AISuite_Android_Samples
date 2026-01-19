// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.handlers

import android.content.Context
import android.content.Context.CAMERA_SERVICE
import android.content.res.Configuration
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import android.view.WindowManager
import com.zebra.aisuite_quickstart.kotlin.CameraXLivePreviewActivity

/**
 * BoundingBoxMapper handles the mapping of bounding boxes from image coordinates
 * to overlay coordinates, including rotation transformations.
 */
class BoundingBoxMapper(
    private val activity: CameraXLivePreviewActivity,
    private val context: Context
) {
    companion object {
        private const val TAG = "BoundingBoxMapper"
        private const val ROTATION_0 = 0
        private const val ROTATION_90 = 1
        private const val ROTATION_180 = 2
        private const val ROTATION_270 = 3
    }

    private var imageWidth: Int = 0
    private var imageHeight: Int = 0
    private var initialRotation: Int = ROTATION_0
    private var isFrontCamera: Boolean = false
    private  var isTablet = false
    private var cameraOrientation: Int? = 0
    private var isHorizontalCameraTablet = false


    init{
        isTablet = isTablet(context)
        detectCameraOrientation()
    }

    fun setImageDimensions(width: Int, height: Int) {
        imageWidth = width
        imageHeight = height
    }

    fun setInitialRotation(rotation: Int) {
        initialRotation = rotation
    }

    fun setFrontCamera(frontCamera: Boolean) {
        isFrontCamera = frontCamera
    }

    fun mapBoundingBoxToOverlay(bbox: Rect): Rect {
        val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val currentRotation = display.rotation

        val relativeRotation = ((currentRotation - initialRotation + 4) % 4)
        Log.d(TAG, "current rotation: $currentRotation, initial rotation: $initialRotation, relative: $relativeRotation")

        val overlayWidth = activity.binding.previewView.width
        val overlayHeight = activity.binding.previewView.height

        if (overlayWidth == 0 || overlayHeight == 0) {
            return bbox
        }

        val transformedBbox = transformBoundingBoxForRotation(bbox, relativeRotation)

        var effectiveImageWidth = imageWidth
        var effectiveImageHeight = imageHeight

        if(isHorizontalCameraTablet) {
            if ((isTablet && (relativeRotation == ROTATION_0 || relativeRotation == ROTATION_180)) ||
                (!isTablet && (relativeRotation == ROTATION_90 || relativeRotation == ROTATION_270))
            ) {
                effectiveImageWidth = imageHeight
                effectiveImageHeight = imageWidth
            }
        }
        Log.d(TAG, "overlay width: $overlayWidth, height: $overlayHeight, effective width: $effectiveImageWidth, height: $effectiveImageHeight")

        val scaleX = overlayWidth.toFloat() / effectiveImageWidth
        val scaleY = overlayHeight.toFloat() / effectiveImageHeight
        val scale = maxOf(scaleX, scaleY)

        val offsetX = (overlayWidth - effectiveImageWidth * scale) / 2f
        val offsetY = (overlayHeight - effectiveImageHeight * scale) / 2f

        // Handle mirroring for front camera
        if (isFrontCamera) {
            val left = transformedBbox.left
            val right = transformedBbox.right
            transformedBbox.left = effectiveImageWidth - right
            transformedBbox.right = effectiveImageWidth - left
        }

        return Rect(
            (transformedBbox.left * scale + offsetX).toInt(),
            (transformedBbox.top * scale + offsetY).toInt(),
            (transformedBbox.right * scale + offsetX).toInt(),
            (transformedBbox.bottom * scale + offsetY).toInt()
        )
    }

    private fun transformBoundingBoxForRotation(bbox: Rect, relativeRotation: Int): Rect {
        return when (relativeRotation) {
            ROTATION_0 -> Rect(bbox)
            ROTATION_90 -> Rect(
                bbox.top,
                imageWidth - bbox.right,
                bbox.bottom,
                imageWidth - bbox.left
            )
            ROTATION_180 -> Rect(
                imageWidth - bbox.right,
                imageHeight - bbox.bottom,
                imageWidth - bbox.left,
                imageHeight - bbox.top
            )
            ROTATION_270 -> Rect(
                imageHeight - bbox.bottom,
                bbox.left,
                imageHeight - bbox.top,
                bbox.right
            )
            else -> {
                Log.w(TAG, "Unknown relative rotation: $relativeRotation, using original bbox")
                Rect(bbox)
            }
        }
    }

    private fun isTablet(context: Context): Boolean {
        return (context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }

    private fun detectCameraOrientation() {
        try {
            val cameraManager = context.getSystemService(CAMERA_SERVICE) as CameraManager
            var found = false
            for (id in cameraManager.cameraIdList) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if ((isFrontCamera && facing == CameraCharacteristics.LENS_FACING_FRONT) ||
                    (!isFrontCamera && facing == CameraCharacteristics.LENS_FACING_BACK)
                ) {
                    cameraOrientation = chars.get(CameraCharacteristics.SENSOR_ORIENTATION)
                    found = true

                    // Detect horizontal camera tablets (you may need to adjust this logic)
                    isHorizontalCameraTablet =
                        isTablet && (cameraOrientation == 0 || cameraOrientation == 180)

                    Log.d(
                        TAG,
                        "Camera orientation: $cameraOrientation, isHorizontalCameraTablet: $isHorizontalCameraTablet"
                    )
                    break
                }
            }
            if (!found) {
                Log.e(TAG, "No suitable camera found for orientation detection")
            }

        } catch (e: java.lang.Exception) {
            Log.e(TAG, "Failed to get camera orientation", e)
        }
    }
}