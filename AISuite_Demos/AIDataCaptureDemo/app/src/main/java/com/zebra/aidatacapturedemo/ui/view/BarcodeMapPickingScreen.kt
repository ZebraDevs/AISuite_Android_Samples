// Copyright (c) 2024-2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.aidatacapturedemo.ui.view

import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
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
import android.annotation.SuppressLint

import androidx.compose.ui.draw.alpha
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.view.PreviewView
import androidx.compose.ui.platform.LocalLifecycleOwner
import android.util.Size

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
fun BarcodeMapPickingScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavController,
    @Suppress("UNUSED_PARAMETER") context: Context,
    @Suppress("UNUSED_PARAMETER") activityInnerPadding: PaddingValues,
    innerPadding: PaddingValues,
    activityLifecycle: Lifecycle
) {
    val uiState by viewModel.uiState.collectAsState()
    val localContext = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    BackHandler(enabled = true) {
        viewModel.handleBackButton(navController)
    }
    viewModel.updateAppBarTitle("Picking Map")

    // Initialize Camera for Live Scanning on Map
    LaunchedEffect(Unit) {
        // We use a standard resolution for picking
        viewModel.updateCameraReady(false)
    }

    // Register BroadcastReceiver for DataWedge
    DisposableEffect(Unit) {
        val filter = IntentFilter("com.zebra.aidatacapturedemo.SCAN")
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val scanData = intent?.getStringExtra("com.symbol.datawedge.data_string")
                if (scanData != null) {
                    viewModel.processHardwareScan(scanData)
                }
            }
        }
        localContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        onDispose {
            localContext.unregisterReceiver(receiver)
        }
    }

    // Removed automatic selectedToteId update to prevent overwriting "Show on Map" target
    /*
    LaunchedEffect(uiState.barcodeResults) {
        if (uiState.barcodeResults.isNotEmpty()) {
            val detectedText = uiState.barcodeResults.first().text
            viewModel.updateSelectedToteId(detectedText)
        }
    }
    */

    Box(modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
        // 1. Background Camera Preview (Low alpha to ensure it's active but hidden)
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    this.scaleType = PreviewView.ScaleType.FILL_CENTER
                    viewModel.setupCameraController(
                        previewView = this,
                        analysisUseCaseCameraResolution = Size(1280, 720),
                        lifecycleOwner = lifecycleOwner,
                        activityLifecycle = activityLifecycle
                    )
                }
            },
            modifier = Modifier.fillMaxSize().alpha(0.1f)
        )

        // 2. Full screen Abstract Map (The "Digital Twin")
        AbstractMapLayer(uiState)

        // 3. Guidance Overlay
        val feedback = uiState.pickingFeedback
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 100.dp), // Increased padding to avoid being blocked by top bar
            contentAlignment = Alignment.TopCenter
        ) {
            if (feedback != null) {
                val isWarning = feedback.contains("incorrect", ignoreCase = true) || 
                                feedback.contains("already picked", ignoreCase = true) ||
                                feedback.contains("Unrecognized", ignoreCase = true)
                
                Text(
                    text = feedback,
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    modifier = Modifier
                        .background(
                            if (isWarning) Color.Red else Color(0xFF006D39),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                )
            } else {
                Text(
                    text = "Scan Tote Barcode",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    ),
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun AbstractMapLayer(uiState: AIDataCaptureDemoUiState) {
    val barcodeResults = uiState.pickingBarcodeResults
    if (barcodeResults.isEmpty()) return

    // Calculate the bounding box of all detected barcodes to center the content
    val minX = barcodeResults.minOf { it.boundingBox.left }.toFloat()
    val maxX = barcodeResults.maxOf { it.boundingBox.right }.toFloat()
    val minY = barcodeResults.minOf { it.boundingBox.top }.toFloat()
    val maxY = barcodeResults.maxOf { it.boundingBox.bottom }.toFloat()

    val contentWidth = maxX - minX
    val contentHeight = maxY - minY

    val displayMetrics = LocalContext.current.resources.displayMetrics
    val displayMetricsDensity = displayMetrics.density
    val windowManager = getSystemService(LocalContext.current, WindowManager::class.java)
    val windowMetrics: WindowMetrics = windowManager!!.currentWindowMetrics
    val displayTotalWidthInPx = windowMetrics.bounds.width()
    val displayTotalHeightInPx = windowMetrics.bounds.height()

    // Use 80% of the screen to keep them "far from the side"
    val paddingFactor = 0.8f
    val availableWidth = displayTotalWidthInPx * paddingFactor
    val availableHeight = displayTotalHeightInPx * paddingFactor

    val scaler = min(
        availableWidth / contentWidth,
        availableHeight / contentHeight
    ).coerceAtMost(displayMetricsDensity * 2.0f) // Cap scaler so single barcodes don't explode

    // Calculate offsets to center the content bounding box on screen
    val gapX = (displayTotalWidthInPx - (scaler * contentWidth)) / 2f - (scaler * minX)
    val gapY = (displayTotalHeightInPx - (scaler * contentHeight)) / 2f - (scaler * minY)

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
    val barcodeResults = uiState.pickingBarcodeResults
    if (barcodeResults.isEmpty()) return

    // Grouping logic for columns first (to identify vertical stacks)
    val sortedByX = barcodeResults.sortedBy { it.boundingBox.centerX() }
    val columns = mutableListOf<MutableList<ResultData>>()

    if (sortedByX.isNotEmpty()) {
        var currentColumn = mutableListOf<ResultData>()
        currentColumn.add(sortedByX[0])
        columns.add(currentColumn)

        for (i in 1 until sortedByX.size) {
            val prev = sortedByX[i - 1]
            val curr = sortedByX[i]
            // Overlap threshold for same column: 60% of width
            if (abs(curr.boundingBox.centerX() - prev.boundingBox.centerX()) < (prev.boundingBox.width() * 0.6)) {
                currentColumn.add(curr)
            } else {
                currentColumn = mutableListOf<ResultData>()
                currentColumn.add(curr)
                columns.add(currentColumn)
            }
        }
    }

    // Sort each column by Y (top-to-bottom)
    columns.forEach { it.sortBy { item -> item.boundingBox.centerY() } }

    // Synthesize rows from columns for consistent alignment and labeling
    val rows = mutableListOf<List<ResultData>>()
    val maxItemsInColumn = columns.maxOfOrNull { it.size } ?: 0
    for (rowIdx in 0 until maxItemsInColumn) {
        val row = mutableListOf<ResultData>()
        for (colIdx in 0 until columns.size) {
            if (rowIdx < columns[colIdx].size) {
                row.add(columns[colIdx][rowIdx])
            }
        }
        rows.add(row)
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        rows.forEach { row ->
            val sortedRow = row.sortedBy { it.boundingBox.left }
            val avgHeight = sortedRow.map { it.boundingBox.height() }.average().toFloat()
            val avgCenterY = sortedRow.map { it.boundingBox.centerY() }.average().toFloat()

            sortedRow.forEach { barcode ->
                val bBoxWidth = barcode.boundingBox.width().toFloat()
                val centerX = barcode.boundingBox.centerX().toFloat()

                val scaledWidth = (scaler * bBoxWidth) * 2.0f
                val scaledHeight = (scaler * avgHeight) * 3.5f
                val scaledLeft = (scaler * centerX) + gapX - (scaledWidth / 2)
                val scaledTop = (scaler * avgCenterY) + gapY - (scaledHeight / 2)

                // Use the pre-calculated labels from the ViewModel
                val label = uiState.pickingBarcodeLabels[barcode.text] ?: ""
                
                // Find quantity if this tote is one of the targets for the current product
                val qty = uiState.targetTotes.find { it.first == label }?.second
                
                // Check if this specific tote box has already been validated by a scan
                val isValidated = uiState.validatedTotes.contains(label)

                // Highlight if this box's label matches the selected tote OR it's a target AND not validated yet
                val isTarget = (uiState.selectedToteId == label || qty != null) && !isValidated
                
                val displayText = if (qty != null && !isValidated) "QTY: $qty" else barcode.text

                drawAbstractPickingUnit(
                    barcode = displayText,
                    label = label,
                    left = scaledLeft,
                    top = scaledTop,
                    width = scaledWidth,
                    height = scaledHeight,
                    density = displayMetricsDensity,
                    isTarget = isTarget
                )
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
        val displayId = if (barcode.startsWith("QTY:")) barcode else if (barcode.length > 5) barcode.takeLast(5) else barcode
        drawContext.canvas.nativeCanvas.drawText(displayId, textX, textY, paint)
    }
}
