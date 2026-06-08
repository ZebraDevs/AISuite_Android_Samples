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
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.zebra.aidatacapturedemo.data.ResultData
import kotlin.math.abs

/**
 * BarcodeMapResultScreen is a Composable function that displays an abstract
 * geometrical layout of detected barcodes on a clean background.
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
    viewModel.updateAppBarTitle("Barcode Layout Map")

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

        // The following computed values are used for drawing
        val scaler = min(
            displayTotalWidthInPx.toFloat() / capturedBitmap.width.toFloat(),
            availableHeightInPx / capturedBitmap.height.toFloat()
        )
        val gapX = (displayTotalWidthInPx - (scaler * capturedBitmap.width.toFloat())) / 2f
        val gapY = (availableHeightInPx - (scaler * capturedBitmap.height.toFloat())) / 2f

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = displayStatusBarHeightInDp,
                    bottom = displayNavigationBarHeightInDp
                )
                .background(color = Color(0xFFF0F2F5)) // Clean modern background
        ) {
            // ABSTRACT MAP CANVAS
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.barcodeResults.isEmpty()) {
                    CircularProgressIndicator(color = Color(0xFF006D39))
                } else {
                    DrawAbstractBarcodeMap(
                        uiState = uiState,
                        scaler = scaler,
                        gapX = gapX,
                        gapY = gapY,
                        displayMetricsDensity = displayMetricsDensity
                    )
                }
            }

            // SUMMARY OVERLAY
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Barcode Layout Map",
                        style = TextStyle(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1C1E)
                        )
                    )
                    Text(
                        text = "${uiState.barcodeResults.size} barcodes mapped",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF44474E)
                        )
                    )
                }
            }

            // SAVE BUTTON
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 32.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Button(
                    onClick = {
                        viewModel.saveBarcodeLayout()
                        navController.navigate(Screen.CustomerInformation.route)
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(54.dp),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006D39)) // Dark green
                ) {
                    Text(
                        text = "Save Barcode Map",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawAbstractBarcodeMap(
    uiState: AIDataCaptureDemoUiState,
    scaler: Float,
    gapX: Float,
    gapY: Float,
    displayMetricsDensity: Float
) {
    val barcodeResults = uiState.barcodeResults
    if (barcodeResults.isEmpty()) return

    // Grouping logic for rows
    val sortedByY = barcodeResults.sortedBy { it.boundingBox.centerY() }
    val rows = mutableListOf<MutableList<ResultData>>()
    
    if (sortedByY.isNotEmpty()) {
        var currentRow = mutableListOf<ResultData>()
        currentRow.add(sortedByY[0])
        rows.add(currentRow)

        for (i in 1 until sortedByY.size) {
            val prev = sortedByY[i - 1]
            val curr = sortedByY[i]
            
            // Overlap threshold for same row
            if (abs(curr.boundingBox.centerY() - prev.boundingBox.centerY()) < (prev.boundingBox.height() * 0.6)) {
                currentRow.add(curr)
            } else {
                currentRow = mutableListOf<ResultData>()
                currentRow.add(curr)
                rows.add(currentRow)
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        rows.forEach { row ->
            val sortedRow = row.sortedBy { it.boundingBox.left }
            
            // Normalize row metrics to average
            val avgHeight = sortedRow.map { it.boundingBox.height() }.average().toFloat()
            val avgCenterY = sortedRow.map { it.boundingBox.centerY() }.average().toFloat()

            var currentLeftX = -1f

            sortedRow.forEachIndexed { index, barcode ->
                val bBoxWidth = barcode.boundingBox.width().toFloat()
                var left = barcode.boundingBox.left.toFloat()
                
                // Snapping logic: if close to previous, snap to it
                if (currentLeftX != -1f) {
                    if (abs(left - currentLeftX) < bBoxWidth * 0.4) {
                        left = currentLeftX
                    }
                }

                val scaledLeft = (scaler * left) + gapX
                val scaledTop = (scaler * (avgCenterY - avgHeight/2)) + gapY
                val scaledWidth = (scaler * bBoxWidth) * 1.2f
                val scaledHeight = (scaler * avgHeight) * 1.2f

                val label = uiState.barcodeLabels[barcode.text] ?: ""

                drawAbstractUnit(
                    barcode = barcode.text,
                    label = label,
                    left = scaledLeft,
                    top = scaledTop,
                    width = scaledWidth,
                    height = scaledHeight,
                    density = displayMetricsDensity
                )
                
                // Track where the next one should start if it snaps
                currentLeftX = left + bBoxWidth
            }
        }
    }
}

private fun DrawScope.drawAbstractUnit(
    barcode: String,
    label: String,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    density: Float
) {
    val themeColor = Color(0xFF00FF00) // Vibrant Green
    val rectSize = androidx.compose.ui.geometry.Size(width, height)
    val topLeft = Offset(left, top)

    // 1. Draw simple geometrical shape (Rectangle)
    drawRect(
        color = themeColor.copy(alpha = 0.2f),
        topLeft = topLeft,
        size = rectSize
    )

    // 2. Draw sharp outline
    drawRect(
        color = themeColor,
        topLeft = topLeft,
        size = rectSize,
        style = Stroke(width = 2f * density)
    )

    // 3. Draw label badge above the box
    if (label.isNotEmpty()) {
        val radius = 12f * density
        val centerX = left + width / 2
        val centerY = top - radius - 2f * density // Positioned above the box
        
        drawCircle(
            color = Color(0xFF006D39),
            radius = radius,
            center = Offset(centerX, centerY)
        )
        
        val labelPaint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.WHITE
            this.textSize = 10f * density
            this.textAlign = android.graphics.Paint.Align.CENTER
            this.isAntiAlias = true
            this.isFakeBoldText = true
        }
        
        val labelY = centerY - (labelPaint.fontMetrics.ascent + labelPaint.fontMetrics.descent) / 2
        drawContext.canvas.nativeCanvas.drawText(label, centerX, labelY, labelPaint)
    }

    // 4. Center-aligned Barcode text
    val paint = android.graphics.Paint().apply {
        this.color = android.graphics.Color.BLACK
        this.textSize = 11f * density
        this.textAlign = android.graphics.Paint.Align.CENTER
        this.isAntiAlias = true
        this.isFakeBoldText = true
    }

    val textX = left + width / 2
    val textY = top + height / 2 - (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2

    // Only draw ID if it fits within the simplified shape
    if (width > 20 * density) {
        val displayId = if (barcode.length > 5) barcode.takeLast(5) else barcode
        drawContext.canvas.nativeCanvas.drawText(displayId, textX, textY, paint)
    }
}
