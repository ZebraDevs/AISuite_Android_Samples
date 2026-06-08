// Copyright (c) 2024-2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.aidatacapturedemo.ui.view

import android.content.Context
import android.util.Log
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.getSystemService
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.model.FileUtils.Companion.saveOcrBarcodeCaptureSessionDataToPrefs
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel
import kotlin.math.min

/**
 * OCRBarcodeResultCapturedScreen is a Composable function that displays the captured image along with
 * the OCR and Barcode results as overlays. It handles the back button press to navigate back to the
 * previous screen and saves the capture session data to preferences when new results are available.
 * The screen also calculates the necessary scaling and padding to properly display the captured image
 * and overlays on different device resolutions.
 */
private const val TAG = "OCRBarcodeResultCapturedScreen"

@Composable
fun OCRBarcodeResultCapturedScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavController,
    activityInnerPadding: PaddingValues,
    context: Context
) {
    val uiState = viewModel.uiState.collectAsState().value

    LaunchedEffect(key1 = uiState.ocrResults.size, key2 = uiState.barcodeResults.size) {
        if ((uiState.ocrResults.size > 0) || (uiState.barcodeResults.size > 0)) {
            viewModel.updateOcrBarcodeCaptureSessionIndex(uiState.ocrBarcodeCaptureSessionCount)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                saveOcrBarcodeCaptureSessionDataToPrefs(
                    context,
                    uiState.ocrBarcodeCaptureSessionCount.toString(),
                    uiState
                )
            }
            Log.d(TAG, "Saved Information")
        }
    }

    BackHandler(enabled = true) {
        viewModel.handleBackButton(navController)
    }
    viewModel.updateAppBarTitle("")
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
        // GET DEVICE RESOLUTION:
        val displayMetrics = LocalContext.current.resources.displayMetrics
        val displayMetricsDensity = displayMetrics.density

        val windowManager = getSystemService(context, WindowManager::class.java)
        val windowMetrics: WindowMetrics = windowManager.currentWindowMetrics

        val displayTotalWidthInPx = windowMetrics.bounds.width()
        val displayTotalHeightInPx = windowMetrics.bounds.height()

        // TOP STATUS BAR
        val displayStatusBarPaddingValues = WindowInsets.statusBars.asPaddingValues()
        val displayStatusBarHeightInDp = displayStatusBarPaddingValues.calculateTopPadding()
        val displayStatusBarHeightInPx = displayStatusBarHeightInDp.value * displayMetricsDensity

        // BOTTOM NAVIGATION BAR
        val displayNavigationBarPaddingValues = WindowInsets.navigationBars.asPaddingValues()
        val displayNavigationBarHeightInDp =
            displayNavigationBarPaddingValues.calculateBottomPadding()
        val displayNavigationBarHeightInPx =
            displayNavigationBarHeightInDp.value * displayMetricsDensity

        val availableHeightInPx =
            displayTotalHeightInPx.toFloat() - displayStatusBarHeightInPx - displayNavigationBarHeightInPx

        // The following computed values are used for drawing Bbox overlay on the preview
        val scaler = min(
            displayTotalWidthInPx.toFloat() / capturedBitmap.width.toFloat(),
            availableHeightInPx / capturedBitmap.height.toFloat()
        )
        val scaledWidth = scaler * capturedBitmap.width.toFloat()
        val scaledHeight = scaler * capturedBitmap.height.toFloat()
        val gapX = (displayTotalWidthInPx - scaledWidth) / 2f
        val gapY = (availableHeightInPx - scaledHeight) / 2f


        Box( // Bottom layer
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = displayStatusBarHeightInDp,
                    bottom = displayNavigationBarHeightInDp
                )
                .background(color = Color.Black)
        ) {

            // CAPTURED IMAGE
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(capturedBitmap),
                    contentDescription = "Captured Image",
                    contentScale = ContentScale.Fit
                )
                if ((uiState.allBarcodeOCRCaptureFilter == 0 || uiState.allBarcodeOCRCaptureFilter == 2)) {
                    // Draw OCR results
                    DrawOCRResultWithTextSizeScaling(
                        uiState = uiState,
                        scaler = scaler,
                        gapX = gapX,
                        gapY = gapY,
                        displayMetricsDensity = displayMetricsDensity,
                        displayTotalHeightInPx = displayTotalHeightInPx,
                        displayTotalWidthInPx = displayTotalWidthInPx
                    )
                }
                if ((uiState.allBarcodeOCRCaptureFilter == 0 || uiState.allBarcodeOCRCaptureFilter == 1)) {
                    // Draw Barcode results
                    DrawBarcodeResultOnCanvas(
                        uiState = uiState,
                        scaler = scaler,
                        gapX = gapX,
                        gapY = gapY,
                        displayMetricsDensity = displayMetricsDensity
                    )
                }
                // Place RoundIconButton at bottom end with required padding
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
}

@Composable
private fun DrawBarcodeResultOnCanvas(
    uiState: AIDataCaptureDemoUiState,
    scaler: Float,
    gapX: Float,
    gapY: Float,
    displayMetricsDensity: Float
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
    ) {
        uiState.barcodeResults.forEach { barcodeData ->
            barcodeData?.let {

                val bBoxTop = barcodeData.boundingBox.top.toFloat()
                val bBoxLeft = barcodeData.boundingBox.left.toFloat()
                val bBoxBottom = barcodeData.boundingBox.bottom.toFloat()
                val bBoxRight = barcodeData.boundingBox.right.toFloat()

                val scaledBBoxLeftInPx = (scaler * bBoxLeft) + gapX
                val scaledBBoxTopInPx = (scaler * bBoxTop) + gapY
                val scaledBBoxRightInPx = (scaler * bBoxRight) + gapX
                val scaledBBoxBottomInPx = (scaler * bBoxBottom) + gapY

                // Define the size and position of the rectangle
                val rectangleWidth = scaledBBoxRightInPx - scaledBBoxLeftInPx
                val rectangleHeight = scaledBBoxBottomInPx - scaledBBoxTopInPx
                val topLeftOffset = Offset(scaledBBoxLeftInPx, scaledBBoxTopInPx)

                drawRect(
                    color = Color.Green,
                    topLeft = topLeftOffset,
                    size = androidx.compose.ui.geometry.Size(rectangleWidth, rectangleHeight),
                    style = Stroke(width = (1f * displayMetricsDensity))
                )

                if (barcodeData.text != null && barcodeData.text != "") {

                    val barcodeRectangleHeight = scaledBBoxBottomInPx - scaledBBoxTopInPx
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 30f
                    }

                    val barcodeTextOffset = Offset(
                        scaledBBoxLeftInPx,
                        scaledBBoxTopInPx + (barcodeRectangleHeight) / 2
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        barcodeData.text,
                        barcodeTextOffset.x,
                        barcodeTextOffset.y,
                        paint
                    )
                }
            }
        }
    }
}