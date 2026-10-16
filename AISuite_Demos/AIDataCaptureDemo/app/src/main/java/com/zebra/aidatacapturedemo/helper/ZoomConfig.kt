package com.zebra.aidatacapturedemo.helper

import androidx.compose.animation.core.Spring

/**
 * Configuration for zoom, pan, and overlay rendering in image preview screens.
 *
 * This data class centralizes all tunable parameters for gesture-based zooming and panning,
 * as well as overlay appearance (bounding box stroke and barcode label text size).
 *
 * @property minZoom Minimum zoom level (1f = original size, <1f allows zoom-out).
 * @property maxZoom Maximum zoom level (e.g. 5f = 5× magnification).
 * @property snapBackStiffness Spring stiffness for snap-back animation when scale < minZoom.
 * @property snapBackDampingRatio Spring damping for snap-back animation.
 * @property strokeWidthDp Base bounding box stroke width in dp (counter-scaled at render time).
 * @property barcodeTextSizePx Base barcode label text size in px (counter-scaled at render time).
 * @property panSensitivity Multiplier for pan gesture delta (1f = 1:1, lower = slower, higher = faster).
 * @property productTextMaxScale Maximum scale factor (1.0 = full box-fitted size) for product SKU text at default (1×) zoom.
 * @property productTextMinScale Minimum scale factor for product SKU text at maximum zoom.
 * @property productTextAbsoluteMinSp Absolute minimum font size in sp for product SKU text, even for very small boxes.
 */
data class ZoomConfig(

    val minZoom: Float = 1f,

    val maxZoom: Float = 6f,

    val snapBackStiffness: Float = Spring.StiffnessMediumLow,

    val snapBackDampingRatio: Float = Spring.DampingRatioMediumBouncy,

    val strokeWidthDp: Float = 1f,

    val barcodeTextSizePx: Float = 26f,

    val panSensitivity: Float = 1f,

    val productTextMaxScale: Float = 2.0f,

    val productTextMinScale: Float = 0.75f,

    val productTextAbsoluteMinSp: Float = 10f
)