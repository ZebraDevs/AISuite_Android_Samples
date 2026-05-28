// Copyright (c) 2024-2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.aidatacapturedemo.ui.view

import android.content.Context
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.getSystemService
import androidx.navigation.NavController
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel
import kotlin.math.min

/**
 * BarcodeMapResultScreen is a Composable function that displays the relative positions
 * of the detected barcodes and their IDs on a clean background.
 */
@Composable
fun BarcodeMapResultScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavController,
    activityInnerPadding: PaddingValues,
    context: Context
) {
    val uiState = viewModel.uiState.collectAsState().value

    BackHandler(enabled = true) {
        viewModel.handleBackButton(navController)
    }
    viewModel.updateAppBarTitle("Barcode Map")

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

        // The following computed values are used for drawing Bbox overlay
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
                .background(color = Color(0xFFF0F0F0)) // Clean light gray background
        ) {

            // MAP CANVAS
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // We don't draw the captured image anymore as per user request
                
                DrawBarcodeMapOnCanvas(
                    uiState = uiState,
                    scaler = scaler,
                    gapX = gapX,
                    gapY = gapY,
                    displayMetricsDensity = displayMetricsDensity
                )
            }

            // SUMMARY OVERLAY
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = "Detected ${uiState.barcodeResults.size} Barcodes",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    ),
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // SAVE BUTTON
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 24.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Button(
                    onClick = {
                        viewModel.saveBarcodeLayout()
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006400))
                ) {
                    Text(
                        text = "Save Layout",
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawBarcodeMapOnCanvas(
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
        uiState.barcodeResults.forEachIndexed { index, barcodeData ->
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

                // 1. Draw Bounding Box
                drawRect(
                    color = Color(0xFF00FF00), // zebra green
                    topLeft = topLeftOffset,
                    size = androidx.compose.ui.geometry.Size(rectangleWidth, rectangleHeight),
                    style = Stroke(width = (2f * displayMetricsDensity))
                )

                // 2. Draw semi-transparent overlay inside the box
                drawRect(
                    color = Color(0x3300FF00),
                    topLeft = topLeftOffset,
                    size = androidx.compose.ui.geometry.Size(rectangleWidth, rectangleHeight)
                )

                // 3. Prepare label text
                val idText = if (barcodeData.text.isNotEmpty()) barcodeData.text else "N/A"
                val labelText = "#${index + 1}: $idText"

                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 14f * displayMetricsDensity
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    isAntiAlias = true
                }

                val textWidth = paint.measureText(labelText)
                val fontMetrics = paint.fontMetrics
                val textHeight = fontMetrics.descent - fontMetrics.ascent

                // Position label above the barcode
                val padding = 4f * displayMetricsDensity
                val labelX = scaledBBoxLeftInPx
                var labelY = scaledBBoxTopInPx - textHeight - (2 * padding)

                // Ensure label is within screen bounds
                if (labelY < 0) {
                    labelY = scaledBBoxBottomInPx + padding
                }

                // Draw label background
                drawRect(
                    color = Color(0xFF006400), // Darker green
                    topLeft = Offset(labelX, labelY),
                    size = androidx.compose.ui.geometry.Size(textWidth + (2 * padding), textHeight + padding)
                )

                // Draw text
                drawContext.canvas.nativeCanvas.drawText(
                    labelText,
                    labelX + padding,
                    labelY + textHeight - fontMetrics.descent,
                    paint
                )
            }
        }
    }
}
