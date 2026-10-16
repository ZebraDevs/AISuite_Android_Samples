// Copyright (c) 2024-2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.aidatacapturedemo.ui.view

import android.content.Context
import android.util.Log
import com.zebra.aidatacapturedemo.model.OverlayTransform
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.helper.ZoomConfig
import com.zebra.aidatacapturedemo.model.FileUtils.Companion.saveOcrBarcodeCaptureSessionDataToPrefs
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * OCRBarcodeResultCapturedScreen is a Composable function that displays the captured image along with
 * the OCR and Barcode results as overlays. It handles the back button press to navigate back to the
 * previous screen and saves the capture session data to preferences when new results are available.
 * The screen also calculates the necessary scaling and padding to properly display the captured image
 * and overlays on different device resolutions.
 */
private const val TAG = "OCRBarcodeResultCapturedScreen"


/**
 * OCRBarcodeResultCapturedScreen — displays the captured image with OCR / barcode overlays.
 *
 * This composable function presents the captured image and overlays the OCR and barcode results on top of it. It supports zoom and pan gestures, allowing users to interactively inspect the image and overlays. The function also handles saving session data when new results are available and manages navigation and app bar title updates. The UI adapts to different device resolutions by scaling and positioning overlays appropriately. A floating action button is provided for navigation to the results screen.
 *
 * @param viewModel The ViewModel managing UI state and business logic for the screen.
 * @param navController Navigation controller for handling navigation events.
 * @param activityInnerPadding Padding values to respect the Scaffold's top and bottom bars.
 * @param context Android context, used for saving session data.
 * @param zoomConfig Configuration for zoom, pan, animation, and rendering. Allows customization of gesture sensitivity and overlay appearance.
 */
@Composable
fun OCRBarcodeResultCapturedScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavController,
    activityInnerPadding: PaddingValues,
    context: Context,
    zoomConfig: ZoomConfig = ZoomConfig()          // ← inject custom config here
) {
    val uiState = viewModel.uiState.collectAsState().value

    LaunchedEffect(key1 = uiState.ocrResults.size, key2 = uiState.barcodeResults.size) {
        if ((uiState.ocrResults.isNotEmpty()) || (uiState.barcodeResults.isNotEmpty())) {
            viewModel.updateOcrBarcodeCaptureSessionIndex(uiState.ocrBarcodeCaptureSessionCount)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                saveOcrBarcodeCaptureSessionDataToPrefs(
                    context, uiState.ocrBarcodeCaptureSessionCount.toString(), uiState
                )
            }
            Log.d(TAG, "Saved Information")
        }
    }

    BackHandler(enabled = true) {
        viewModel.handleBackButton(navController)
    }
    val capturedBitmap = uiState.captureBitmap
    if (capturedBitmap == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
        }
    } else {
        // Screen density — still needed for stroke-width scaling in dp→px conversion
        val displayMetricsDensity = LocalContext.current.resources.displayMetrics.density

        // ── Animatable zoom / pan state ────────────────────────────────────
        val animScale = remember { Animatable(1f) }
        val animOffsetX = remember { Animatable(0f) }
        val animOffsetY = remember { Animatable(0f) }

        val scope = rememberCoroutineScope()

        // Spring spec built from ZoomConfig — created once, not every frame
        val snapBackSpec = remember(zoomConfig) {
            spring<Float>(
                dampingRatio = zoomConfig.snapBackDampingRatio,
                stiffness = zoomConfig.snapBackStiffness
            )
        }

        /* Outer container
         * Use activityInnerPadding so we respect the Scaffold's top bar (which
         * includes status-bar + segment-button app-bar) and the navigation bar.
         * Previously only status-bar + nav-bar were subtracted, causing every
         * bounding box to be shifted upward by the app-bar height.
         */
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(activityInnerPadding)
                .background(color = Color.Black)
        ) {
            // Zoomable + pannable container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(zoomConfig) {          // re-runs if config changes
                        detectTransformGestures(
                            onGesture = { centroid, pan, zoom, _ ->
                                // ── DURING gesture: snapTo (instant, no allocation) ──
                                scope.launch {
                                    val oldScale = animScale.value
                                    val newScale = (oldScale * zoom).coerceIn(
                                        zoomConfig.minZoom, zoomConfig.maxZoom
                                    )

                                    // Focal-point zoom: keep the gesture centroid pinned.
                                    val w2 = size.width / 2f
                                    val h2 = size.height / 2f
                                    val scaleRatio = newScale / oldScale

                                    val newOffsetX =
                                        (centroid.x - w2) * (1f - scaleRatio) + animOffsetX.value * scaleRatio + pan.x * zoomConfig.panSensitivity

                                    val newOffsetY =
                                        (centroid.y - h2) * (1f - scaleRatio) + animOffsetY.value * scaleRatio + pan.y * zoomConfig.panSensitivity

                                    // Max pan bounds so image edge never crosses viewport
                                    val maxX = (newScale - 1f) * size.width / 2f
                                    val maxY = (newScale - 1f) * size.height / 2f

                                    animScale.snapTo(newScale)
                                    animOffsetX.snapTo(newOffsetX.coerceIn(-maxX, maxX))
                                    animOffsetY.snapTo(newOffsetY.coerceIn(-maxY, maxY))
                                }
                            })
                    }
                    // ── AFTER gesture: spring animation back to identity if needed ──
                    .pointerInput(zoomConfig) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val allUp = event.changes.all { !it.pressed }
                                if (allUp && animScale.value < zoomConfig.minZoom) {
                                    scope.launch {
                                        launch {
                                            animScale.animateTo(
                                                zoomConfig.minZoom, snapBackSpec
                                            )
                                        }
                                        launch { animOffsetX.animateTo(0f, snapBackSpec) }
                                        launch { animOffsetY.animateTo(0f, snapBackSpec) }
                                    }
                                }
                            }
                        }
                    }
                    .graphicsLayer(
                        scaleX = animScale.value,
                        scaleY = animScale.value,
                        translationX = animOffsetX.value,
                        translationY = animOffsetY.value
                    ), contentAlignment = Alignment.Center) {
                Image(
                    painter = rememberAsyncImagePainter(capturedBitmap),
                    contentDescription = "Captured Image",
                    contentScale = ContentScale.Fit
                )
                if (uiState.allBarcodeOCRCaptureFilter == 0 || uiState.allBarcodeOCRCaptureFilter == 2) {
                    // scaler/gapX/gapY are now computed inside DrawScope from the
                    // canvas's actual size — guaranteed to match ContentScale.Fit.
                    DrawOCRResultWithTextSizeScaling(
                        uiState = uiState,
                        bitmapWidth = capturedBitmap.width,
                        bitmapHeight = capturedBitmap.height,
                        transform = OverlayTransform(
                            scaler = animScale.value,
                            displayMetricsDensity = displayMetricsDensity
                        ),
                        strokeWidthDp = zoomConfig.strokeWidthDp,
                        minSizeGuard = MinSizeGuardConfig(
                            minSizePx = 20f,
                            preserveAspectRatio = true
                        )
                    )
                }
                if (uiState.allBarcodeOCRCaptureFilter == 0 || uiState.allBarcodeOCRCaptureFilter == 1) {
                    // Draw Barcode results
                    DrawBarcodeResultOnCanvas(
                        uiState = uiState,
                        bitmapWidth = capturedBitmap.width,
                        bitmapHeight = capturedBitmap.height,
                        displayMetricsDensity = displayMetricsDensity,
                        zoomScale = animScale.value,
                        barcodeTextSizePx = zoomConfig.barcodeTextSizePx
                    )
                }
            }

            // RoundIconButton stays outside the zoomable layer — always fixed in place
            RoundIconButton(
                R.drawable.ic_next,
                onClick = { navController.navigate(route = Screen.OCRBarcodeResults.route) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 80.dp, end = 12.dp)
            )
        }
    }
}

/**
 * DrawBarcodeResultOnCanvas — overlays barcode bounding boxes and labels on the captured image.
 *
 * This composable draws green rectangles around detected barcodes and displays their decoded text labels above each box. The overlay is scaled and positioned to match the displayed image, ensuring accurate alignment regardless of device resolution or zoom. Stroke width and text size are adjusted for consistent appearance at any zoom level. The function uses ContentScale.Fit logic to map bitmap coordinates to the canvas.
 *
 * @param uiState The current UI state containing barcode detection results.
 * @param bitmapWidth The width of the captured bitmap in pixels.
 * @param bitmapHeight The height of the captured bitmap in pixels.
 * @param displayMetricsDensity The device's display density, used for scaling stroke width.
 * @param zoomScale The current zoom scale applied to the image and overlays.
 * @param barcodeTextSizePx The base text size (in pixels) for barcode labels, counter-scaled for zoom.
 */
@Composable
private fun DrawBarcodeResultOnCanvas(
    uiState: AIDataCaptureDemoUiState,
    bitmapWidth: Int,
    bitmapHeight: Int,
    displayMetricsDensity: Float,
    zoomScale: Float = 1f,
    barcodeTextSizePx: Float = 30f
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // ── Map bitmap coordinates → canvas coordinates ────────────────────
        // Using DrawScope.size guarantees the mapping matches ContentScale.Fit
        // exactly, regardless of any outer padding or app-bar height.
        val scaler = min(
            size.width / bitmapWidth.toFloat(), size.height / bitmapHeight.toFloat()
        )
        val scaledBmWidth = scaler * bitmapWidth
        val scaledBmHeight = scaler * bitmapHeight
        val gapX = (size.width - scaledBmWidth) / 2f
        val gapY = (size.height - scaledBmHeight) / 2f

        uiState.barcodeResults.forEach { barcodeData ->
            barcodeData?.let {
                val scaledBBoxLeftInPx = (scaler * barcodeData.boundingBox.left.toFloat()) + gapX
                val scaledBBoxTopInPx = (scaler * barcodeData.boundingBox.top.toFloat()) + gapY
                val scaledBBoxRightInPx = (scaler * barcodeData.boundingBox.right.toFloat()) + gapX
                val scaledBBoxBottomInPx =
                    (scaler * barcodeData.boundingBox.bottom.toFloat()) + gapY

                val rectangleWidth = scaledBBoxRightInPx - scaledBBoxLeftInPx
                val rectangleHeight = scaledBBoxBottomInPx - scaledBBoxTopInPx
                val topLeftOffset = Offset(scaledBBoxLeftInPx, scaledBBoxTopInPx)

                // Stroke width counter-scaled: always looks strokeWidthDp thick at any zoom
                drawRect(
                    color = Color.Green,
                    topLeft = topLeftOffset,
                    size = androidx.compose.ui.geometry.Size(rectangleWidth, rectangleHeight),
                    style = Stroke(width = (1f * displayMetricsDensity))
                )

                if (barcodeData.text.isNotEmpty()) {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        // Counter-scale text size by zoom to match product-label behaviour:
                        // text shrinks relative to the box as the user zooms in, so it
                        // stays visually consistent at any zoom level.
                        // Remove zoomscale to make the text size fixed and not scale with zoom like OCR text.
                        textSize = barcodeTextSizePx // / zoomScale.coerceAtLeast(1f)
                        // CENTER alignment: x param becomes the horizontal centre
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }

                    // Horizontal centre of the bounding box
                    val textX = scaledBBoxLeftInPx + rectangleWidth / 2f

                    // Draw label just ABOVE the top edge of the box.
                    // An extra gap (8 dp worth of pixels) gives the label
                    // more breathing room so it doesn't sit tight on the border.
                    val extraGapPx = 8f * displayMetricsDensity
                    val textY = scaledBBoxTopInPx - extraGapPx + paint.ascent() * -1f  // ascent is negative

                    drawContext.canvas.nativeCanvas.drawText(
                        barcodeData.text, textX, textY, paint
                    )
                }
            }
        }
    }
}