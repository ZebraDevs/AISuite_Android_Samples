// Copyright (c) 2024-2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.aidatacapturedemo.ui.view

import android.content.Context
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel
import kotlin.math.min
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.zebra.aidatacapturedemo.data.ResultData
import kotlin.math.abs

@Composable
fun BarcodeMapPickingScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavController,
    @Suppress("UNUSED_PARAMETER") context: Context,
    @Suppress("UNUSED_PARAMETER") activityInnerPadding: PaddingValues,
    @Suppress("UNUSED_PARAMETER") activityLifecycle: Lifecycle
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(enabled = true) {
        viewModel.handleBackButton(navController)
    }
    viewModel.updateAppBarTitle("Item Picking")

    // Logic to "decide which tote": 
    // In this demo, if a barcode is detected, we match it to a tote.
    LaunchedEffect(uiState.barcodeResults) {
        if (uiState.barcodeResults.isNotEmpty()) {
            val detectedText = uiState.barcodeResults.first().text
            // In a real app, we'd lookup which tote 'detectedText' belongs to.
            // For this demo, let's just use the first detected one as the target
            viewModel.updateSelectedToteId(detectedText)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Full screen Abstract Map (The "Digital Twin")
        AbstractMapLayer(uiState)

        // 2. Guidance Overlay
        val feedback = uiState.pickingFeedback
        if (feedback != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 100.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = feedback,
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    modifier = Modifier
                        .background(
                            if (feedback.contains("incorrect")) Color.Red else Color(0xFF006D39),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 100.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = "Scan Item Barcode",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    ),
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AbstractMapLayer(uiState: AIDataCaptureDemoUiState) {
    val capturedBitmap = uiState.captureBitmap ?: return
    
    val displayMetrics = LocalContext.current.resources.displayMetrics
    val displayMetricsDensity = displayMetrics.density
    val windowManager = getSystemService(LocalContext.current, WindowManager::class.java)
    val windowMetrics: WindowMetrics = windowManager!!.currentWindowMetrics
    val displayTotalWidthInPx = windowMetrics.bounds.width()
    val displayTotalHeightInPx = windowMetrics.bounds.height()

    // Simplified scaling logic for the abstract map
    val scaler = min(
        displayTotalWidthInPx.toFloat() / capturedBitmap.width.toFloat(),
        displayTotalHeightInPx.toFloat() / capturedBitmap.height.toFloat()
    )
    val gapX = (displayTotalWidthInPx - (scaler * capturedBitmap.width.toFloat())) / 2f
    val gapY = (displayTotalHeightInPx - (scaler * capturedBitmap.height.toFloat())) / 2f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5))
    ) {
        DrawAbstractBarcodeMapLayer(
            uiState = uiState,
            scaler = scaler,
            gapX = gapX,
            gapY = gapY,
            displayMetricsDensity = displayMetricsDensity
        )
    }
}

@Composable
private fun DrawAbstractBarcodeMapLayer(
    uiState: AIDataCaptureDemoUiState,
    scaler: Float,
    gapX: Float,
    gapY: Float,
    displayMetricsDensity: Float
) {
    val barcodeResults = uiState.barcodeResults
    if (barcodeResults.isEmpty()) return

    // Grouping logic for rows (Reusing logic from Result screen)
    val sortedByY = barcodeResults.sortedBy { it.boundingBox.centerY() }
    val rows = mutableListOf<MutableList<ResultData>>()
    
    if (sortedByY.isNotEmpty()) {
        var currentRow = mutableListOf<ResultData>()
        currentRow.add(sortedByY[0])
        rows.add(currentRow)

        for (i in 1 until sortedByY.size) {
            val prev = sortedByY[i - 1]
            val curr = sortedByY[i]
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
            val avgHeight = sortedRow.map { it.boundingBox.height() }.average().toFloat()
            val avgCenterY = sortedRow.map { it.boundingBox.centerY() }.average().toFloat()

            var currentLeftX = -1f

            sortedRow.forEach { barcode ->
                val bBoxWidth = barcode.boundingBox.width().toFloat()
                var left = barcode.boundingBox.left.toFloat()
                
                if (currentLeftX != -1f) {
                    if (abs(left - currentLeftX) < bBoxWidth * 0.4) {
                        left = currentLeftX
                    }
                }

                val scaledLeft = (scaler * left) + gapX
                val scaledTop = (scaler * (avgCenterY - avgHeight/2)) + gapY
                val scaledWidth = (scaler * bBoxWidth) * 1.2f
                val scaledHeight = (scaler * avgHeight) * 1.2f

                // Highlight if it's the selected tote
                val isTarget = uiState.selectedToteId == barcode.text
                val label = uiState.barcodeLabels[barcode.text] ?: ""

                drawAbstractPickingUnit(
                    barcode = barcode.text,
                    label = label,
                    left = scaledLeft,
                    top = scaledTop,
                    width = scaledWidth,
                    height = scaledHeight,
                    density = displayMetricsDensity,
                    isTarget = isTarget
                )
                
                currentLeftX = left + bBoxWidth
            }
        }
    }
}

private fun DrawScope.drawAbstractPickingUnit(
    barcode: String,
    label: String,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    density: Float,
    isTarget: Boolean
) {
    val themeColor = if (isTarget) Color(0xFFFFCC00) else Color(0xFF00FF00) // Gold for target, Green for others
    val rectSize = androidx.compose.ui.geometry.Size(width, height)
    val topLeft = Offset(left, top)

    drawRect(
        color = themeColor.copy(alpha = if (isTarget) 0.8f else 0.2f),
        topLeft = topLeft,
        size = rectSize
    )

    drawRect(
        color = if (isTarget) Color.Red else themeColor,
        topLeft = topLeft,
        size = rectSize,
        style = Stroke(width = (if (isTarget) 4f else 2f) * density)
    )

    // Draw label badge above the box
    if (label.isNotEmpty()) {
        val radius = 12f * density
        val centerX = left + width / 2
        val centerY = top - radius - 2f * density // Positioned above the box

        drawCircle(
            color = if (isTarget) Color.Red else Color(0xFF006D39),
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

    val paint = android.graphics.Paint().apply {
        this.color = if (isTarget) android.graphics.Color.WHITE else android.graphics.Color.BLACK
        this.textSize = (if (isTarget) 14f else 11f) * density
        this.textAlign = android.graphics.Paint.Align.CENTER
        this.isAntiAlias = true
        this.isFakeBoldText = true
    }

    val textX = left + width / 2
    val textY = top + height / 2 - (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2

    if (width > 20 * density) {
        val displayId = if (barcode.length > 5) barcode.takeLast(5) else barcode
        drawContext.canvas.nativeCanvas.drawText(displayId, textX, textY, paint)
    }
}
