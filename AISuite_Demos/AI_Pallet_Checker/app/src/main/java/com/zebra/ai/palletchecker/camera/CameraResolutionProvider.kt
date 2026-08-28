// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.palletchecker.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.util.Size
import com.zebra.ai.palletchecker.camera.CameraResolutionProvider.getSnapResolutions
import com.zebra.ai.palletchecker.camera.CameraResolutionProvider.getWandResolutions
import com.zebra.ai.palletchecker.helpers.LOGE
import com.zebra.ai.palletchecker.helpers.LOGI
import kotlin.math.abs

/**
 * Queries the device's back-facing camera for all supported output resolutions.
 *
 * Results are sorted from smallest to largest (by total pixel count) and include
 * their aspect ratio label (e.g. "16:9", "4:3") for display in settings UI.
 *
 * Two phase-aware helpers are provided:
 *  - [getSnapResolutions] – intersection of YUV_420_888 (ImageAnalysis) and JPEG
 *    (ImageCapture) sizes, because the Snap phase binds Preview + ImageCapture + ImageAnalysis.
 *  - [getWandResolutions] – YUV_420_888 sizes only, because Wand binds Preview + ImageAnalysis.
 */
object CameraResolutionProvider {

    private const val TAG = "CameraResolutionProvider"

    /**
     * Data class representing a single supported camera resolution with a human-readable label.
     */
    data class CameraResolution(
        val size: Size,
        val label: String,
        val aspectRatio: String
    ) {
        val width get() = size.width
        val height get() = size.height
    }

    val allowedAspectRatios = setOf("16:9", "4:3")
    private const val MIN_PIXELS = 1_000_000L

    /**
     * Returns sorted, 1-MP-or-higher 16:9/4:3 resolutions supported by both ImageAnalysis and ImageCapture.
     * Used to identify the usable resolution set for the Snap phase.
     */
    fun getSnapResolutions(context: Context): List<CameraResolution> {
        val analysisSizes = querySizes(context, ImageFormat.YUV_420_888)
        val captureSizes = querySizes(context, ImageFormat.JPEG)
        val common = analysisSizes.intersect(captureSizes.toSet())
        return common
            .filter { it.width.toLong() * it.height.toLong() >= MIN_PIXELS }
            .sortedBy { it.width.toLong() * it.height.toLong() }
            .map { toCameraResolution(it) }
            .filter { it.aspectRatio in allowedAspectRatios }
    }

    /**
     * Returns sorted, 1-MP-or-higher 16:9/4:3 resolutions supported by ImageAnalysis (YUV_420_888).
     * Represents the format usable specifically during the Wand phase (Preview + Analysis).
     */
    fun getWandResolutions(context: Context): List<CameraResolution> {
        return querySizes(context, ImageFormat.YUV_420_888)
            .filter { it.width.toLong() * it.height.toLong() >= MIN_PIXELS }
            .sortedBy { it.width.toLong() * it.height.toLong() }
            .map { toCameraResolution(it) }
            .filter { it.aspectRatio in allowedAspectRatios }
    }

    private fun querySizes(context: Context, imageFormat: Int): List<Size> {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val backCameraId = findBackCameraId(cameraManager) ?: run {
            LOGE(TAG, "No back-facing camera found")
            return emptyList()
        }

        val characteristics = cameraManager.getCameraCharacteristics(backCameraId)
        val configMap = characteristics.get(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
        ) ?: run {
            LOGE(TAG, "No stream configuration map for camera $backCameraId")
            return emptyList()
        }

        val standardSizes: Array<Size> = configMap.getOutputSizes(imageFormat) ?: emptyArray()
        val highResSizes: Array<Size> = try {
            configMap.getHighResolutionOutputSizes(imageFormat) ?: emptyArray()
        } catch (_: Exception) {
            emptyArray()
        }

        val allSizes = (standardSizes.toList() + highResSizes.toList()).distinct()

        LOGI(
            TAG, "querySizes format=$imageFormat → ${allSizes.size} sizes: " +
                    allSizes.joinToString { "${it.width}×${it.height}" }
        )

        return allSizes
    }

    private fun findBackCameraId(cameraManager: CameraManager): String? {
        return cameraManager.cameraIdList.firstOrNull { id ->
            val chars = cameraManager.getCameraCharacteristics(id)
            chars.get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK
        }
    }

    /**
     * Computes the human-readable aspect ratio string for a given width × height.
     * Returns simplified ratios like "16:9", "4:3", "3:2", "1:1", etc.
     * Falls back to GCD-reduced ratio for uncommon aspect ratios.
     */
    internal fun computeAspectRatioLabel(width: Int, height: Int): String {
        val ratio = width.toFloat() / height.toFloat()

        val knownRatios = listOf(
            "16:9" to 16f / 9f,
            "4:3" to 4f / 3f,
            "3:2" to 3f / 2f,
            "18:9" to 18f / 9f,
            "21:9" to 21f / 9f,
            "5:4" to 5f / 4f,
            "5:3" to 5f / 3f,
            "1:1" to 1f,
        )

        for ((label, known) in knownRatios) {
            if (abs(ratio - known) < 0.02f) return label
        }

        val g = gcd(width, height)
        return "${width / g}:${height / g}"
    }

    private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

    private fun toCameraResolution(size: Size): CameraResolution {
        val mp = (size.width.toLong() * size.height.toLong()) / 1_000_000.0
        val aspectLabel = computeAspectRatioLabel(size.width, size.height)
        val mpText = if (mp >= 1.0) {
            "${mp.toInt()}MP"
        } else {
            "${String.format(java.util.Locale.US, "%.1f", mp)}MP"
        }
        val label = "$mpText (${size.width} × ${size.height}) [$aspectLabel]"
        return CameraResolution(size, label, aspectLabel)
    }
}


