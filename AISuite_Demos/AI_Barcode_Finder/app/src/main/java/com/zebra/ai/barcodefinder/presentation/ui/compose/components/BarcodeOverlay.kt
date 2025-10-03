// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.presentation.ui.compose.components

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import android.view.OrientationEventListener
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.zebra.ai.barcodefinder.domain.model.BarcodeOverlayItem
import com.zebra.ai.barcodefinder.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.presentation.ui.theme.white

private const val TAG = "BarcodeOverlay"

/**
 * Draws barcode overlays on the camera preview using Jetpack Compose.
 * Handles device orientation, tap detection, and delegates drawing to drawBarcodeOverlayItem.
 *
 * @param items List of overlay items to draw (each represents a detected barcode)
 * @param modifier Modifier for Compose layout
 * @param onItemClick Callback for when an overlay item is tapped
 */
@Composable
fun BarcodeOverlay(
    items: List<BarcodeOverlayItem>,
    modifier: Modifier = Modifier,
    onItemClick: (BarcodeOverlayItem) -> Unit = {}
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val context = LocalContext.current

    // Track the actual device orientation using OrientationEventListener
    var deviceOrientation by remember { mutableIntStateOf(0) }

    // Set up orientation listener to detect physical device rotation
    DisposableEffect(context) {
        val orientationListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation != ORIENTATION_UNKNOWN) {
                    deviceOrientation = orientation
                }
            }
        }
        if (orientationListener.canDetectOrientation()) {
            orientationListener.enable()
        }
        onDispose {
            orientationListener.disable()
        }
    }

    // Determine if device is physically rotated to landscape
    // Based on debugging, adjust these ranges if needed:
    // Typical ranges: 0-45°=Portrait, 45-135°=Left, 135-225°=Upside, 225-315°=Right, 315-360°=Portrait
    val isDeviceInLandscape = when (deviceOrientation) {
        in 45..135 -> true   // Left landscape (counter-clockwise rotation)
        in 225..315 -> true  // Right landscape (clockwise rotation)
        else -> false        // Portrait orientations
    }

    val isClockwiseLandscape = when (deviceOrientation) {
        in 45..135 -> -1 // Left landscape (counter-clockwise rotation)
        in 225..315 -> 1  // Right landscape (clockwise rotation)
        else -> 1        // Portrait orientations
    }

    // Debug logging for landscape detection
    LaunchedEffect(deviceOrientation) {
        Log.d(TAG, "Orientation: $deviceOrientation°, Landscape: $isDeviceInLandscape")
    }

    // Store items in a ref to avoid restarting pointerInput
    val itemsRef = rememberUpdatedState(items)

    // Convert dp values to pixels
    val iconBoxSizePx = with(density) { AppDimensions.dimension_28dp.toPx() }
    val textSizePx = with(density) { AppDimensions.barcodeTextFontSizeDefault.toPx() }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    itemsRef.value.forEach { item ->
                        if (item.bounds.contains(offset.x, offset.y)) {
                            onItemClick(item)
                        }
                    }
                }
            }
    ) {
        items.forEach { item ->
            drawBarcodeOverlayItem(
                item = item,
                iconBoxSizePx = iconBoxSizePx,
                textSizePx = textSizePx,
                textMeasurer = textMeasurer,
                isLandscape = isDeviceInLandscape,
                isClockwiseLandscape = isClockwiseLandscape
            )
        }
    }
}

/**
 * Draws a single barcode overlay item (bounding box, icon, and text) on the canvas.
 * Handles landscape/portrait orientation and applies rotation as needed.
 *
 * @param item The overlay item to draw
 * @param iconBoxSizePx Size of the icon box in pixels
 * @param textSizePx Size of the text in pixels
 * @param textMeasurer Utility for measuring text layout
 * @param isLandscape Whether the device is in landscape orientation
 * @param isClockwiseLandscape Direction of landscape rotation
 */
private fun DrawScope.drawBarcodeOverlayItem(
    item: BarcodeOverlayItem,
    iconBoxSizePx: Float,
    textSizePx: Float,
    textMeasurer: TextMeasurer,
    isLandscape: Boolean,
    isClockwiseLandscape: Int
) {
    val bounds = item.bounds

    // Draw background if specified
    if (item.backgroundColor != Color.Transparent) {
        drawRect(
            color = item.backgroundColor,
            topLeft = Offset(
                bounds.left.toFloat(),
                bounds.top.toFloat()
            ),
            size = Size(
                bounds.width().toFloat(),
                bounds.height().toFloat()
            )
        )
    }

    // Draw icon if available
    item.icon?.let { bitmap ->
        val iconBounds = calculateIconBounds(bounds, bitmap, iconBoxSizePx)

        // Convert Android Bitmap to Compose ImageBitmap
        val imageBitmap = bitmap.asImageBitmap()

        // Apply rotation if in landscape mode
        if (isLandscape) {
            // Rotate 90 degrees around the center of the icon
            rotate(
                degrees = isClockwiseLandscape * 90f,
                pivot = Offset(
                    iconBounds.centerX(),
                    iconBounds.centerY()
                )
            ) {
                drawImage(
                    image = imageBitmap,
                    dstOffset = IntOffset(
                        iconBounds.left.toInt(),
                        iconBounds.top.toInt()
                    ),
                    dstSize = IntSize(
                        iconBounds.width().toInt(),
                        iconBounds.height().toInt()
                    )
                )
            }
        } else {
            // Draw normally in portrait mode
            drawImage(
                image = imageBitmap,
                dstOffset = IntOffset(
                    iconBounds.left.toInt(),
                    iconBounds.top.toInt()
                ),
                dstSize = IntSize(
                    iconBounds.width().toInt(),
                    iconBounds.height().toInt()
                )
            )
        }

        // Draw text on top of icon if available
        if (item.text.isNotEmpty()) {
            val textStyle = TextStyle(
                color = white,
                fontSize = textSizePx.sp,
                fontWeight = FontWeight(AppDimensions.fontWeight700)
            )

            val textLayoutResult = textMeasurer.measure(
                text = item.text,
                style = textStyle
            )

            val textX = iconBounds.centerX() - textLayoutResult.size.width / 2
            val textY = iconBounds.centerY() - textLayoutResult.size.height / 2

            // Apply same rotation to text if in landscape mode
            if (isLandscape) {
                rotate(
                    degrees = isClockwiseLandscape * 90f,
                    pivot = Offset(
                        iconBounds.centerX(),
                        iconBounds.centerY()
                    )
                ) {
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(textX, textY)
                    )
                }
            } else {
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(textX, textY)
                )
            }
        }
    }
}

/**
 * Calculates the bounds for drawing an icon centered within the entity bounds.
 * Scales the icon to fit within a square of iconBoxSizePx.
 *
 * @param entityBounds The bounding box of the entity/barcode
 * @param icon The bitmap icon to draw
 * @param iconBoxSizePx The maximum size for the icon box in pixels
 * @return RectF representing the area to draw the icon
 */
private fun calculateIconBounds(entityBounds: RectF, icon: Bitmap, iconBoxSizePx: Float): RectF {
    val scale = minOf(
        iconBoxSizePx / icon.width,
        iconBoxSizePx / icon.height
    )

    val drawWidth = (icon.width * scale)
    val drawHeight = (icon.height * scale)
    val cx = entityBounds.centerX()
    val cy = entityBounds.centerY()

    return RectF(
        cx - drawWidth / 2,
        cy - drawHeight / 2,
        cx + drawWidth / 2,
        cy + drawHeight / 2
    )
}
