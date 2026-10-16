package com.zebra.aidatacapturedemo.ui.view

import android.app.Activity
import android.content.Context
import android.util.Size
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.activity.compose.BackHandler
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.data.ResultData
import com.zebra.aidatacapturedemo.model.OverlayTransform
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel
import kotlin.math.abs
import kotlin.math.min

// Corner/edge handle detection zone
private const val HANDLE_SIZE_PX = 60f
private const val MIN_FRAME_RATIO = 0.10f
private const val MAX_FRAME_RATIO = 0.95f

//private const val IS_BARCODE_SUPPORTED = true

/**
 * TargetModePreviewScreen provides an interactive live camera view with a resizable
 * target frame for OCR scanning. The user can resize the target frame by dragging
 * its corners or edges. OCR results from inside the target frame are displayed
 * in real-time using Best Read logic.
 *
 * The target frame size is persisted so it is remembered across sessions.
 */
@Composable
fun TargetModePreviewScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavController,
    context: Context,
    activityInnerPadding: PaddingValues,
    activityLifecycle: Lifecycle
) {
    val uiState = viewModel.uiState.collectAsState().value
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val analysisUseCaseCameraResolution = when (viewModel.getSelectedResolution()) {
        0 -> Size(1280, 720)
        1 -> Size(1920, 1080)
        2 -> Size(2688, 1512)
        3 -> Size(3840, 2160)
        else -> Size(1920, 1080)
    }

    // GET DEVICE RESOLUTION:
    val displayMetrics = LocalContext.current.resources.displayMetrics
    val displayMetricsDensity = displayMetrics.density

    // TOP STATUS BAR
    val displayStatusBarPaddingValues = WindowInsets.statusBars.asPaddingValues()
    val displayStatusBarHeightInDp = displayStatusBarPaddingValues.calculateTopPadding()
    val displayStatusBarHeightInPx = displayStatusBarHeightInDp.value * displayMetricsDensity

    val windowManager = getSystemService(context, WindowManager::class.java)!!
    val windowMetrics: WindowMetrics = windowManager.currentWindowMetrics

    val displayTotalWidthInPx = windowMetrics.bounds.width()
    val displayTotalHeightInPx = windowMetrics.bounds.height()

    // BOTTOM NAVIGATION BAR
    val displayNavigationBarPaddingValues = WindowInsets.navigationBars.asPaddingValues()
    val displayNavigationBarHeightInDp = displayNavigationBarPaddingValues.calculateBottomPadding()
    val displayNavigationBarHeightInPx =
        displayNavigationBarHeightInDp.value * displayMetricsDensity

    val availableHeightInPx =
        displayTotalHeightInPx.toFloat() - displayStatusBarHeightInPx - displayNavigationBarHeightInPx

    // The following computed values are used for drawing Bbox overlay on the preview
    val scaler = min(
        displayTotalWidthInPx.toFloat() / analysisUseCaseCameraResolution.height.toFloat(),
        availableHeightInPx / analysisUseCaseCameraResolution.width.toFloat()
    )
    val scaledWidth = scaler * analysisUseCaseCameraResolution.height.toFloat()
    val scaledHeight = scaler * analysisUseCaseCameraResolution.width.toFloat()
    val gapX = (displayTotalWidthInPx - scaledWidth) / 2f
    val gapY = (availableHeightInPx - scaledHeight) / 2f

    val previewView = remember { PreviewView(context) }

    // Load persisted target frame size
    LaunchedEffect(Unit) {
        viewModel.loadTargetFrameSize()
    }

    // Target frame state: size as ratio, center position as ratio
    val targetWidthRatio = remember { mutableFloatStateOf(uiState.targetFrameWidthRatio) }
    val targetHeightRatio = remember { mutableFloatStateOf(uiState.targetFrameHeightRatio) }
    val targetOffsetXRatio = remember { mutableFloatStateOf(uiState.targetFrameOffsetXRatio) }
    val targetOffsetYRatio = remember { mutableFloatStateOf(uiState.targetFrameOffsetYRatio) }

    // Update local state when UI state changes (e.g. after load from prefs)
    LaunchedEffect(
        uiState.targetFrameWidthRatio,
        uiState.targetFrameHeightRatio,
        uiState.targetFrameOffsetXRatio,
        uiState.targetFrameOffsetYRatio
    ) {
        targetWidthRatio.floatValue = uiState.targetFrameWidthRatio
        targetHeightRatio.floatValue = uiState.targetFrameHeightRatio
        targetOffsetXRatio.floatValue = uiState.targetFrameOffsetXRatio
        targetOffsetYRatio.floatValue = uiState.targetFrameOffsetYRatio
    }

    LaunchedEffect(key1 = "clear_results_target_mode") {
        viewModel.updateOcrResultData(results = null)
        viewModel.updateBarcodeResultData(results = listOf())
        viewModel.setZoom(1.0f)
    }

    BackHandler(enabled = true) {
        viewModel.handleBackButton(navController)
    }

    // Keep the screen on while the live camera preview is active so that the
    // display timeout doesn't stop/dim the screen and tear down the camera session.
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = generateSequence(view.context) { ctx ->
            (ctx as? android.content.ContextWrapper)?.baseContext
        }.filterIsInstance<Activity>().firstOrNull()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(lifecycleOwner) {
        viewModel.setupCameraController(
            previewView = previewView,
            analysisUseCaseCameraResolution = analysisUseCaseCameraResolution,
            lifecycleOwner = lifecycleOwner,
            activityLifecycle = activityLifecycle
        )
    }

    previewView.scaleType = PreviewView.ScaleType.FIT_CENTER

    // --- Filter results to only those inside the target frame ---
    // Frame center in px, then derive left/top/right/bottom
    val containerWidthPx = displayTotalWidthInPx.toFloat()
    val frameWidthPx = targetWidthRatio.floatValue * containerWidthPx
    val frameHeightPx = targetHeightRatio.floatValue * availableHeightInPx
    val frameCenterXPx = targetOffsetXRatio.floatValue * containerWidthPx
    val frameCenterYPx = targetOffsetYRatio.floatValue * availableHeightInPx
    val frameLeftPx = frameCenterXPx - frameWidthPx / 2f
    val frameTopPx = frameCenterYPx - frameHeightPx / 2f
    val frameRightPx = frameCenterXPx + frameWidthPx / 2f
    val frameBottomPx = frameCenterYPx + frameHeightPx / 2f

    val filteredOcrResults by remember(
        uiState.ocrResults,
        targetWidthRatio.floatValue,
        targetHeightRatio.floatValue,
        targetOffsetXRatio.floatValue,
        targetOffsetYRatio.floatValue
    ) {
        derivedStateOf {
            filterResultsInsideFrame(
                uiState.ocrResults,
                scaler,
                gapX,
                gapY,
                frameLeftPx,
                frameTopPx,
                frameRightPx,
                frameBottomPx
            )
        }
    }

    val filteredBarcodeResults by remember(
        uiState.barcodeResults,
        targetWidthRatio.floatValue,
        targetHeightRatio.floatValue,
        targetOffsetXRatio.floatValue,
        targetOffsetYRatio.floatValue
    ) {
        derivedStateOf {
            filterResultsInsideFrame(
                uiState.barcodeResults,
                scaler,
                gapX,
                gapY,
                frameLeftPx,
                frameTopPx,
                frameRightPx,
                frameBottomPx
            )
        }
    }

    // Build a combined text list for the floating overview
    val detectedTexts by remember(filteredOcrResults, filteredBarcodeResults) {
        derivedStateOf {
            val texts = mutableListOf<String>()
            filteredOcrResults.forEach { if (it.text.isNotBlank()) texts.add(it.text) }
            filteredBarcodeResults.forEach { if (it.text.isNotBlank()) texts.add(it.text) }
            texts
        }
    }
    var showInstruction by remember { mutableStateOf(true) }
    val isFrameReady = targetWidthRatio.floatValue > 0f && targetHeightRatio.floatValue > 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = displayStatusBarHeightInDp, bottom = displayNavigationBarHeightInDp
            )
    ) {
        // Camera preview layer
        AndroidView({ previewView })

        // Target frame overlay with dimming outside the frame
        if (isFrameReady) TargetFrameOverlay(
            widthRatio = targetWidthRatio.floatValue,
            heightRatio = targetHeightRatio.floatValue,
            offsetXRatio = targetOffsetXRatio.floatValue,
            offsetYRatio = targetOffsetYRatio.floatValue,
            onResize = { newW, newH ->
                showInstruction = false
                targetWidthRatio.floatValue = newW
                targetHeightRatio.floatValue = newH
                viewModel.updateTargetFrameSize(newW, newH)
            },
            onMove = { newX, newY ->
                showInstruction = false
                targetOffsetXRatio.floatValue = newX
                targetOffsetYRatio.floatValue = newY
                viewModel.updateTargetFramePosition(newX, newY)
            },
            displayMetricsDensity = displayMetricsDensity
        )

        // Draw only OCR results inside the target frame
//        if (uiState.isOCRModelEnabled && filteredOcrResults.isNotEmpty()) {
//            DrawFilteredOCRResult(
//                results = filteredOcrResults,
//                transform = OverlayTransform(scaler, gapX, gapY, displayMetricsDensity)
//            )
//        }

        // Draw only Barcode results inside the target frame
        if (/*IS_BARCODE_SUPPORTED && */isFrameReady && uiState.isBarcodeModelEnabled && filteredBarcodeResults.isNotEmpty()) {
            DrawFilteredBarcodeResult(
                results = filteredBarcodeResults,
                transform = OverlayTransform(scaler, gapX, gapY, displayMetricsDensity)
            )
        }

        // Floating text overview panel anchored to the target frame
        if (isFrameReady && detectedTexts.isNotEmpty()) {
            FloatingResultsPanel(
                detectedTexts = detectedTexts,
                targetWidthRatio = targetWidthRatio.floatValue,
                targetHeightRatio = targetHeightRatio.floatValue,
                targetOffsetXRatio = targetOffsetXRatio.floatValue,
                targetOffsetYRatio = targetOffsetYRatio.floatValue,
                displayMetricsDensity = displayMetricsDensity
            )
        }

        // Instruction text at the top
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp, start = 21.dp, end = 21.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            if (showInstruction) {
                Text(
                    text = stringResource(R.string.target_mode_instruction), style = TextStyle(
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                        fontWeight = FontWeight(400),
                        color = Color.White,
                    ), modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }

    uiState.cameraError?.let {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.instruction_6),
                style = TextStyle(fontSize = 14.sp, color = Variables.blackText)
            )
        }
    }

    if (uiState.isCameraReady) {
        showBottomBar(
            navController = navController,
            viewModel = viewModel,
            activityInnerPadding = activityInnerPadding
        )
    }
}

/**
 * Draws a target frame overlay with a semi-transparent dimming outside the frame.
 * - Drag **edges / corners** → resize
 * - Drag **inside** the frame → move (reposition)
 * The frame is positioned by its centre (offsetXRatio, offsetYRatio) as fractions of the
 * container, so it is no longer forced to stay centred.
 */
@Composable
private fun TargetFrameOverlay(
    widthRatio: Float,
    heightRatio: Float,
    offsetXRatio: Float,
    offsetYRatio: Float,
    onResize: (Float, Float) -> Unit,
    onMove: (Float, Float) -> Unit,
    displayMetricsDensity: Float
) {
    val dragEdge = remember { mutableStateOf(DragEdge.NONE) }

    val currentWidthRatio = rememberUpdatedState(widthRatio)
    val currentHeightRatio = rememberUpdatedState(heightRatio)
    val currentOffsetX = rememberUpdatedState(offsetXRatio)
    val currentOffsetY = rememberUpdatedState(offsetYRatio)
    val currentOnResize = rememberUpdatedState(onResize)
    val currentOnMove = rememberUpdatedState(onMove)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(onDragStart = { offset ->
                    val cW = size.width.toFloat()
                    val cH = size.height.toFloat()
                    val wR = currentWidthRatio.value
                    val hR = currentHeightRatio.value
                    val cx = currentOffsetX.value * cW
                    val cy = currentOffsetY.value * cH
                    val frameLeft = cx - wR * cW / 2f
                    val frameTop = cy - hR * cH / 2f
                    val frameRight = cx + wR * cW / 2f
                    val frameBottom = cy + hR * cH / 2f

                    dragEdge.value = detectDragEdge(
                        offset, frameLeft, frameTop, frameRight, frameBottom
                    )
                }, onDrag = { change, dragAmount ->
                    change.consume()
                    if (dragEdge.value == DragEdge.NONE) return@detectDragGestures

                    val cW = size.width.toFloat()
                    val cH = size.height.toFloat()
                    val wR = currentWidthRatio.value
                    val hR = currentHeightRatio.value
                    val oX = currentOffsetX.value
                    val oY = currentOffsetY.value

                    when (dragEdge.value) {
                        // Move
                        DragEdge.INSIDE -> {
                            val halfW = wR / 2f
                            val halfH = hR / 2f
                            val newX = (oX + dragAmount.x / cW).coerceIn(halfW, 1f - halfW)
                            val newY = (oY + dragAmount.y / cH).coerceIn(halfH, 1f - halfH)
                            currentOnMove.value(newX, newY)
                        }
                        // Resize
                        DragEdge.LEFT, DragEdge.RIGHT -> {
                            val delta = dragAmount.x / cW
                            val adj =
                                if (dragEdge.value == DragEdge.RIGHT) delta * 2f else -delta * 2f
                            val newW = (wR + adj).coerceIn(MIN_FRAME_RATIO, MAX_FRAME_RATIO)
                            currentOnResize.value(newW, hR)
                        }

                        DragEdge.TOP, DragEdge.BOTTOM -> {
                            val delta = dragAmount.y / cH
                            val adj =
                                if (dragEdge.value == DragEdge.BOTTOM) delta * 2f else -delta * 2f
                            val newH = (hR + adj).coerceIn(MIN_FRAME_RATIO, MAX_FRAME_RATIO)
                            currentOnResize.value(wR, newH)
                        }

                        DragEdge.TOP_LEFT -> currentOnResize.value(
                            (wR - dragAmount.x * 2f / cW).coerceIn(
                                MIN_FRAME_RATIO, MAX_FRAME_RATIO
                            ), (hR - dragAmount.y * 2f / cH).coerceIn(
                                MIN_FRAME_RATIO, MAX_FRAME_RATIO
                            )
                        )

                        DragEdge.TOP_RIGHT -> currentOnResize.value(
                            (wR + dragAmount.x * 2f / cW).coerceIn(
                                MIN_FRAME_RATIO, MAX_FRAME_RATIO
                            ), (hR - dragAmount.y * 2f / cH).coerceIn(
                                MIN_FRAME_RATIO, MAX_FRAME_RATIO
                            )
                        )

                        DragEdge.BOTTOM_LEFT -> currentOnResize.value(
                            (wR - dragAmount.x * 2f / cW).coerceIn(
                                MIN_FRAME_RATIO, MAX_FRAME_RATIO
                            ), (hR + dragAmount.y * 2f / cH).coerceIn(
                                MIN_FRAME_RATIO, MAX_FRAME_RATIO
                            )
                        )

                        DragEdge.BOTTOM_RIGHT -> currentOnResize.value(
                            (wR + dragAmount.x * 2f / cW).coerceIn(
                                MIN_FRAME_RATIO, MAX_FRAME_RATIO
                            ), (hR + dragAmount.y * 2f / cH).coerceIn(
                                MIN_FRAME_RATIO, MAX_FRAME_RATIO
                            )
                        )

                        DragEdge.NONE -> {}
                    }
                }, onDragEnd = { dragEdge.value = DragEdge.NONE })
            }) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cW = size.width
            val cH = size.height
            val frameWidth = widthRatio * cW
            val frameHeight = heightRatio * cH
            val cx = offsetXRatio * cW
            val cy = offsetYRatio * cH
            val frameLeft = cx - frameWidth / 2f
            val frameTop = cy - frameHeight / 2f
            val frameRight = cx + frameWidth / 2f
            val frameBottom = cy + frameHeight / 2f

            // Dim outside
            val innerPath = Path().apply {
                addRect(Rect(frameLeft, frameTop, frameRight, frameBottom))
            }
            clipPath(innerPath, clipOp = androidx.compose.ui.graphics.ClipOp.Difference) {
                drawRect(color = Color.Black.copy(alpha = 0.5f), topLeft = Offset.Zero, size = size)
            }

            // Dashed orange border
            val strokeWidth = 2f * displayMetricsDensity
            drawRect(
                color = Color(0xFFFF7B00),
                topLeft = Offset(frameLeft, frameTop),
                size = androidx.compose.ui.geometry.Size(frameWidth, frameHeight),
                style = Stroke(
                    width = strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                )
            )

            // Corner handles
            val hLen = 24f * displayMetricsDensity
            val hStroke = 3f * displayMetricsDensity
            val hColor = Color.White
            drawLine(
                hColor, Offset(frameLeft, frameTop), Offset(frameLeft + hLen, frameTop), hStroke
            )
            drawLine(
                hColor, Offset(frameLeft, frameTop), Offset(frameLeft, frameTop + hLen), hStroke
            )
            drawLine(
                hColor, Offset(frameRight, frameTop), Offset(frameRight - hLen, frameTop), hStroke
            )
            drawLine(
                hColor, Offset(frameRight, frameTop), Offset(frameRight, frameTop + hLen), hStroke
            )
            drawLine(
                hColor,
                Offset(frameLeft, frameBottom),
                Offset(frameLeft + hLen, frameBottom),
                hStroke
            )
            drawLine(
                hColor,
                Offset(frameLeft, frameBottom),
                Offset(frameLeft, frameBottom - hLen),
                hStroke
            )
            drawLine(
                hColor,
                Offset(frameRight, frameBottom),
                Offset(frameRight - hLen, frameBottom),
                hStroke
            )
            drawLine(
                hColor,
                Offset(frameRight, frameBottom),
                Offset(frameRight, frameBottom - hLen),
                hStroke
            )
        }
    }
}

/**
 * Enum to identify which edge, corner, or interior zone is being dragged.
 * INSIDE → move the whole frame; edges/corners → resize.
 */
private enum class DragEdge {
    NONE, INSIDE, LEFT, RIGHT, TOP, BOTTOM, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}

/**
 * Detects which edge, corner, or interior of the target frame a touch point is near.
 */
private fun detectDragEdge(
    touchPoint: Offset, frameLeft: Float, frameTop: Float, frameRight: Float, frameBottom: Float
): DragEdge {
    val nearLeft = abs(touchPoint.x - frameLeft) < HANDLE_SIZE_PX
    val nearRight = abs(touchPoint.x - frameRight) < HANDLE_SIZE_PX
    val nearTop = abs(touchPoint.y - frameTop) < HANDLE_SIZE_PX
    val nearBottom = abs(touchPoint.y - frameBottom) < HANDLE_SIZE_PX

    // Check corners / edges first (higher priority than inside)
    val edge = when {
        nearLeft && nearTop -> DragEdge.TOP_LEFT
        nearRight && nearTop -> DragEdge.TOP_RIGHT
        nearLeft && nearBottom -> DragEdge.BOTTOM_LEFT
        nearRight && nearBottom -> DragEdge.BOTTOM_RIGHT
        nearLeft -> DragEdge.LEFT
        nearRight -> DragEdge.RIGHT
        nearTop -> DragEdge.TOP
        nearBottom -> DragEdge.BOTTOM
        else -> DragEdge.NONE
    }
    if (edge != DragEdge.NONE) return edge

    // If inside the frame → move
    val insideX = touchPoint.x in frameLeft..frameRight
    val insideY = touchPoint.y in frameTop..frameBottom
    return if (insideX && insideY) DragEdge.INSIDE else DragEdge.NONE
}

// ──────────────────────────────────────────────────────────────────────────────
// Filtering helpers
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Returns only those [ResultData] whose bounding-box center, once projected to screen
 * coordinates, falls inside the target frame rectangle.
 */
private fun filterResultsInsideFrame(
    results: List<ResultData>,
    scaler: Float,
    gapX: Float,
    gapY: Float,
    frameLeft: Float,
    frameTop: Float,
    frameRight: Float,
    frameBottom: Float
): List<ResultData> {
    return results.filter { result ->
        val cx = scaler * (result.boundingBox.left + result.boundingBox.right) / 2f + gapX
        val cy = scaler * (result.boundingBox.top + result.boundingBox.bottom) / 2f + gapY
        cx in frameLeft..frameRight && cy in frameTop..frameBottom
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Draw helpers (filtered lists – avoid touching the shared DrawOCRResult)
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Draws OCR bounding-box overlays only for the pre-filtered results.
 */
@Composable
private fun DrawFilteredOCRResult(
    results: List<ResultData>, transform: OverlayTransform
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val scaler = transform.scaler
        val gapX = transform.gapX
        val gapY = transform.gapY
        val density = transform.displayMetricsDensity

        results.forEach { ocrResultData ->
            if (ocrResultData.text.isNotEmpty()) {
                val rawLeft = scaler * ocrResultData.boundingBox.left.toFloat() + gapX
                val rawTop = scaler * ocrResultData.boundingBox.top.toFloat() + gapY
                val rawRight = scaler * ocrResultData.boundingBox.right.toFloat() + gapX
                val rawBottom = scaler * ocrResultData.boundingBox.bottom.toFloat() + gapY

                val w = rawRight - rawLeft
                val h = rawBottom - rawTop
                val topLeft = Offset(rawLeft, rawTop)

                // Filled background
                drawRect(
                    color = Color(0xBF000000),
                    topLeft = topLeft,
                    size = androidx.compose.ui.geometry.Size(w, h)
                )
                // Orange border
                drawRect(
                    color = Color(0xFFFF7B00),
                    topLeft = topLeft,
                    size = androidx.compose.ui.geometry.Size(w, h),
                    style = Stroke(width = 1f * density)
                )

                // Auto-fit text
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                val padding = 0.5f * density
                var textSize = 2f
                do {
                    paint.textSize = textSize
                    val tw = paint.measureText(ocrResultData.text)
                    val th = paint.descent() - paint.ascent()
                    if (tw + padding * 2 <= w && th + padding * 2 <= h) textSize += 1f else break
                } while (true)
                paint.textSize = textSize - 1f

                drawContext.canvas.nativeCanvas.drawText(
                    ocrResultData.text,
                    rawLeft + w / 2,
                    rawTop + h / 2 - (paint.ascent() + paint.descent()) / 2,
                    paint
                )
            }
        }
    }
}

/**
 * Draws barcode bounding-box overlays only for the pre-filtered results.
 */
@Composable
private fun DrawFilteredBarcodeResult(
    results: List<ResultData>, transform: OverlayTransform
) {
    val scaler = transform.scaler
    val gapX = transform.gapX
    val gapY = transform.gapY
    val density = transform.displayMetricsDensity

    Canvas(modifier = Modifier.fillMaxSize()) {
        results.forEach { barcodeData ->
            val rawLeft = scaler * barcodeData.boundingBox.left.toFloat() + gapX
            val rawTop = scaler * barcodeData.boundingBox.top.toFloat() + gapY
            val rawRight = scaler * barcodeData.boundingBox.right.toFloat() + gapX
            val rawBottom = scaler * barcodeData.boundingBox.bottom.toFloat() + gapY

            drawRect(
                color = Color.Green,
                topLeft = Offset(rawLeft, rawTop),
                size = androidx.compose.ui.geometry.Size(rawRight - rawLeft, rawBottom - rawTop),
                style = Stroke(width = 1f * density)
            )
        }
    }

    // Barcode text labels
    results.forEach { barcodeData ->
        if (barcodeData.text.isNotBlank()) {
            val scaledLeftDp =
                ((scaler * barcodeData.boundingBox.left.toFloat() + gapX) / density).dp
            val scaledBottomDp =
                ((scaler * barcodeData.boundingBox.bottom.toFloat() + gapY) / density).dp
            Text(
                text = barcodeData.text,
                fontSize = 10.sp,
                color = Color.White,
                modifier = Modifier
                    .offset(x = scaledLeftDp, y = scaledBottomDp + 2.dp)
                    .background(Color(0xBF000000))
                    .padding(2.dp)
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Floating results panel
// ──────────────────────────────────────────────────────────────────────────────

/**
 * A semi-transparent floating box that displays all detected text inside the target frame.
 *
 * **Positioning logic (handles full-screen frame):**
 * - If there is enough room below the target frame (≥ 80 dp), the panel is anchored just
 *   below the frame.
 * - Otherwise it is placed just *above* the frame's top edge.
 * - The panel is horizontally centred and its width matches the target frame width
 *   (clamped to a reasonable min/max).
 */
@Composable
private fun FloatingResultsPanel(
    detectedTexts: List<String>,
    targetWidthRatio: Float,
    targetHeightRatio: Float,
    targetOffsetXRatio: Float,
    targetOffsetYRatio: Float,
    @Suppress("UNUSED_PARAMETER") displayMetricsDensity: Float
) {
    // Frame bottom/top as fraction of container height
    val frameBottomFraction = targetOffsetYRatio + targetHeightRatio / 2f
    val frameTopFraction = targetOffsetYRatio - targetHeightRatio / 2f

    val spaceBelow = 1f - frameBottomFraction
    val placeBelow = spaceBelow > 0.08f

    val panelGapDp = 8.dp
    val panelMaxHeight = 120.dp

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val cH = this.maxHeight
        val cW = this.maxWidth

        val frameWidthDp = cW * targetWidthRatio
        val frameLeftDp = cW * (targetOffsetXRatio - targetWidthRatio / 2f)

        val panelTopDp = if (placeBelow) {
            cH * frameBottomFraction + panelGapDp
        } else {
            val desired = cH * frameTopFraction - panelGapDp - panelMaxHeight
            if (desired < 0.dp) 0.dp else desired
        }

        Column(
            modifier = Modifier
                .offset(x = frameLeftDp, y = panelTopDp)
                .widthIn(min = 160.dp, max = frameWidthDp.coerceAtLeast(160.dp))
                .heightIn(max = panelMaxHeight)
                .background(Color.Black.copy(alpha = 0.75f), shape = RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState())
        ) {
            detectedTexts.forEachIndexed { index, text ->
                Text(
                    text = text,
                    style = TextStyle(
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                        fontWeight = FontWeight(400),
                        color = Color.White,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                )
                if (index < detectedTexts.lastIndex) {
                    HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.3f))
                }
            }
        }
    }
}
