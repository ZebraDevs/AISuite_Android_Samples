package com.zebra.aidatacapturedemo.ui.view

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.Size
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.activity.compose.BackHandler
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.data.UsecaseState
import com.zebra.aidatacapturedemo.model.OverlayTransform
import com.zebra.aidatacapturedemo.model.detectShelfGaps
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel
import kotlinx.coroutines.launch
import kotlin.math.min


private const val TAG = "CameraPreviewScreen"

private const val RETAIL_SHELF_GAP_STROKE_WIDTH = 4f

/**
 * CameraPreviewScreen composable function displays the live camera feed with overlay results such as OCR, Barcode, and Retail Module detection.
 *
 * - Overlays are rendered using grouped transformation parameters via the OverlayTransform data class, improving maintainability and reducing parameter clutter.
 * - The composable supports interactive zoom and pan gestures, with smooth spring-based snap-back behavior and bounds checking.
 * - Overlay composables (e.g., DrawOCRResult, DrawBarcodeResult) use the OverlayTransform to ensure accurate coordinate mapping and consistent appearance across device resolutions and zoom levels.
 * - The UI adapts to system UI insets (status bar, navigation bar) and updates the app bar title and navigation state as needed.
 * - All overlay drawing logic is centralized and uses the correct transformation values for precise alignment.
 * - The composable is lifecycle-aware and manages camera resources appropriately.
 *
 * @param viewModel The ViewModel managing UI state and business logic for the screen.
 * @param navController Navigation controller for handling navigation events.
 * @param context Android context, used for resource access and session data saving.
 * @param activityInnerPadding Padding values to respect the Scaffold's top and bottom bars.
 * @param activityLifecycle The Lifecycle of the activity to manage camera resources appropriately.
 */
@Composable
fun CameraPreviewScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavController,
    context: Context,
    activityInnerPadding: PaddingValues,
    innerPadding: PaddingValues,
    activityLifecycle: Lifecycle
) {
    val uiState = viewModel.uiState.collectAsState().value
    val lifecycleOwner = LocalLifecycleOwner.current
    var showInfo = remember { mutableStateOf(true) }
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

    LaunchedEffect(key1 = "clear all the previous results") {
        // clear all the previous results during Fresh Launch
        when (uiState.usecaseSelected) {
            UsecaseState.OCRBarcodeFind.value -> {
                viewModel.updateOcrResultData(results = null)
                viewModel.updateBarcodeResultData(results = listOf())
            }

            UsecaseState.OCR.value -> {
                viewModel.updateOcrResultData(results = null)
            }

            UsecaseState.Barcode.value -> {
                viewModel.updateBarcodeResultData(results = listOf())
            }

            UsecaseState.Retail.value -> {
                viewModel.updateRetailShelfDetectionResult(results = null)
            }

            UsecaseState.Product.value -> {
                viewModel.updateRetailShelfDetectionResult(results = null)
                viewModel.updateProductRecognitionResult(results = null)
            }
        }
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
    Box( // Bottom layer
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = displayStatusBarHeightInDp, bottom = displayNavigationBarHeightInDp
            )
    ) {
        AndroidView( // 2 layer
            { previewView })

        when (val selectedDemo = uiState.usecaseSelected) {
            UsecaseState.OCRBarcodeFind.value -> {

                if (uiState.isBarcodeModelEnabled) {

                    // For Barcode results during CHARACTER_MATCH filter: Play beep (or) vibrate when filtered results are found.
                    if (hasActiveSelectedBarcodeCharacterMatch(uiState) && uiState.barcodeResults.isNotEmpty()) {
                        if (uiState.ocrBarcodeFindSettings.feedbackSettings.audioBeep) {
                            FeedbackUtils.beep()
                        }
                        if (uiState.ocrBarcodeFindSettings.feedbackSettings.vibration) {
                            FeedbackUtils.vibrate()
                        }
                    }

                    DrawBarcodeResult(
                        uiState = uiState,
                        transform = OverlayTransform(scaler, gapX, gapY, displayMetricsDensity)
                    )
                }

                if (uiState.isOCRModelEnabled) {
                    val hasRegex = hasActiveSelectedOcrRegex(uiState)
                    val hasCharacterMatch = hasActiveSelectedOcrCharacterMatch(uiState)

                    when {
                        hasRegex -> {
                            val checkIconDrawable =
                                ContextCompat.getDrawable(context, R.drawable.ic_check)
                            DrawOCRResultWithTextSizeScalingAndCheckMark(
                                uiState = uiState,
                                transform = OverlayTransform(
                                    scaler,
                                    gapX,
                                    gapY,
                                    displayMetricsDensity
                                ),
                                checkIconDrawable = checkIconDrawable,
                                displayTotalHeightInPx = displayTotalHeightInPx,
                                displayTotalWidthInPx = displayTotalWidthInPx
                            )
                            playFeedback(uiState)
                        }

                        hasCharacterMatch -> {
                            val checkIconDrawable =
                                ContextCompat.getDrawable(context, R.drawable.ic_check)
                            DrawOCRResultWithTextSizeScalingAndCheckMark(
                                uiState = uiState,
                                transform = OverlayTransform(
                                    scaler,
                                    gapX,
                                    gapY,
                                    displayMetricsDensity
                                ),
                                checkIconDrawable = checkIconDrawable,
                                displayTotalHeightInPx = displayTotalHeightInPx,
                                displayTotalWidthInPx = displayTotalWidthInPx
                            )
                            playFeedback(uiState)
                        }

                        else -> {
                            DrawOCRResult(
                                uiState = uiState,
                                transform = OverlayTransform(
                                    scaler,
                                    gapX,
                                    gapY,
                                    displayMetricsDensity
                                )
                            )
                        }
                    }
                }
            }

            UsecaseState.OCR.value -> {
                DrawOCRResult(
                    uiState = uiState,
                    transform = OverlayTransform(scaler, gapX, gapY, displayMetricsDensity)
                )
            }

            UsecaseState.Barcode.value -> {
                DrawBarcodeResult(
                    uiState = uiState,
                    transform = OverlayTransform(scaler, gapX, gapY, displayMetricsDensity)
                )
            }

            UsecaseState.Retail.value -> {
                DrawRetailShelfResult(
                    uiState = uiState,
                    scaler = scaler,
                    gapX = gapX,
                    gapY = gapY,
                    displayMetricsDensity = displayMetricsDensity
                )
                DrawRetailShelfGapResult(
                    uiState = uiState,
                    scaler = scaler,
                    gapX = gapX,
                    gapY = gapY,
                    displayMetricsDensity = displayMetricsDensity
                )
                DrawProductRecognitionResult(
                    uiState = uiState, scaler = scaler, gapX = gapX, gapY = gapY
                )
            }

            UsecaseState.Product.value -> {
                DrawRetailShelfResult(
                    uiState = uiState,
                    scaler = scaler,
                    gapX = gapX,
                    gapY = gapY,
                    displayMetricsDensity = displayMetricsDensity
                )
                DrawProductRecognitionResult(
                    uiState = uiState, scaler = scaler, gapX = gapX, gapY = gapY
                )

                if (showInfo.value && uiState.cameraError == null) {
                    HandleTopInfo(
                        icon = R.drawable.camera_icon,
                        info = stringResource(R.string.instruction_1),
                        showInfo = showInfo
                    )
                }
            }

            UsecaseState.Main.value -> {

            }

            else -> {
                // unhandled use case — intentionally no-op
            }
        }
    }

    uiState.cameraError?.let {
        showCameraErrorText()
    }

    if (uiState.usecaseSelected == UsecaseState.OCRBarcodeFind.value && uiState.isOCRModelEnabled) {
        activeOcrSearchTermsText(uiState)?.let { terms ->
            showInformationBox(
                info = "Looking for: [$terms]",
                topPadding = innerPadding.calculateTopPadding()
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

@Composable
private fun showCameraErrorText() {
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.warning_icon),
                contentDescription = "Warning icon",
            )
            Spacer(modifier = Modifier.width(8.dp)) // Space between icon and text
            Text(
                text = stringResource(R.string.instruction_6), style = TextStyle(
                    fontSize = 14.sp,
                    fontFamily = FontFamily(Font(R.font.ibm_plex_sans_regular)),
                    color = Variables.blackText,
                )
            )
        }
    }
}

@Composable
fun DrawOCRResult(
    uiState: AIDataCaptureDemoUiState,
    transform: OverlayTransform,
    minSizeGuard: MinSizeGuardConfig = MinSizeGuardConfig()
) {
    Canvas( // Layer 3
        modifier = Modifier.fillMaxSize()
    ) {
        val scaler = transform.scaler
        val gapX = transform.gapX
        val gapY = transform.gapY
        val displayMetricsDensity = transform.displayMetricsDensity
        uiState.ocrResults.forEach { ocrResultData ->
            if (ocrResultData.text.isNotEmpty()) {

                val bBoxTop = ocrResultData.boundingBox.top.toFloat()
                val bBoxLeft = ocrResultData.boundingBox.left.toFloat()
                val bBoxBottom = ocrResultData.boundingBox.bottom.toFloat()
                val bBoxRight = ocrResultData.boundingBox.right.toFloat()

                val rawLeft = (scaler * bBoxLeft) + gapX
                val rawTop = (scaler * bBoxTop) + gapY
                val rawRight = (scaler * bBoxRight) + gapX
                val rawBottom = (scaler * bBoxBottom) + gapY

                // ── Apply configurable minimum-size guard ─────────────────────
                val expanded = expandBBoxIfNeeded(
                    bbox = CanvasBBox(rawLeft, rawTop, rawRight, rawBottom),
                    config = minSizeGuard,
                    canvasWidth = size.width,
                    canvasHeight = size.height
                )

                val rectangleWidth = expanded.width
                val rectangleHeight = expanded.height
                val topLeftOffset = Offset(expanded.left, expanded.top)

                // Draw the filled rectangle
                drawRect(
                    color = Color(0xBF000000),
                    topLeft = topLeftOffset,
                    size = androidx.compose.ui.geometry.Size(rectangleWidth, rectangleHeight)
                )

                // Draw the border over the filled rectangle
                drawRect(
                    color = Color(0xFFFF7B00),
                    topLeft = topLeftOffset,
                    size = androidx.compose.ui.geometry.Size(rectangleWidth, rectangleHeight),
                    style = Stroke(width = (1f * displayMetricsDensity))
                )

                // Prepare to draw the text
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                // Calculate the maximum text size that fits in the rectangle
                val padding = 0.5f * displayMetricsDensity // Padding from the border
                var textSize = 2f

                // Incrementally increase text size until it just fits
                do {
                    paint.textSize = textSize
                    val textWidth = paint.measureText(ocrResultData.text)
                    val textHeight = paint.descent() - paint.ascent()
                    if (textWidth + padding * 2 <= rectangleWidth && textHeight + padding * 2 <= rectangleHeight) {
                        textSize += 1f
                    } else {
                        break
                    }
                } while (true)

                // Adjust the text size to be slightly smaller
                paint.textSize = textSize - 1f

                // Calculate the position to draw the text
                val textOffsetX = topLeftOffset.x + rectangleWidth / 2
                val textOffsetY =
                    topLeftOffset.y + rectangleHeight / 2 - (paint.ascent() + paint.descent()) / 2

                // Draw the text using nativeCanvas
                drawContext.canvas.nativeCanvas.drawText(
                    ocrResultData.text, textOffsetX, textOffsetY, paint
                )
            }
        }
    }
}

/**
 * Renders OCR bounding-box overlays on a captured bitmap.
 *
 * Derives the coordinate mapping from the DrawScope's own canvas size so overlays align
 * precisely with `ContentScale.Fit`. Boxes smaller than 20 px are expanded for legibility,
 * text size auto-fits the box, and both stroke width and text size are counter-scaled by
 * [zoomScale] so overlays stay visually consistent at any zoom level.
 *
 * @param uiState               Current UI state with the [ResultData] list to draw.
 * @param bitmapWidth           Source bitmap width in pixels (used to compute scale factor).
 * @param bitmapHeight          Source bitmap height in pixels (used to compute scale factor).
 * @param displayMetricsDensity Screen density multiplier (dp → px).
 * @param zoomScale             Current pinch-zoom scale; stroke and text are divided by this value.
 * @param strokeWidthDp         Border stroke width in dp (rendered as `strokeWidthDp × density / zoomScale`).
 * @param minSizeGuard          Controls how small boxes are expanded for legibility.
 *                               Defaults to proportional expansion with a 20 px minimum.
 */
@Composable
fun DrawOCRResultWithTextSizeScaling(
    uiState: AIDataCaptureDemoUiState,
    bitmapWidth: Int,
    bitmapHeight: Int,
    transform: OverlayTransform,
    strokeWidthDp: Float = 1f,
    minSizeGuard: MinSizeGuardConfig = MinSizeGuardConfig()
) {
    Canvas( // Layer 3
        modifier = Modifier.fillMaxSize()
    ) {
        // ── Coordinate mapping ────────────────────────────────────────────────
        // Map bitmap coordinates → canvas coordinates.
        // Using DrawScope.size guarantees the mapping exactly matches
        // ContentScale.Fit, regardless of any outer padding or app-bar height.
        val scaler = minOf(
            size.width / bitmapWidth.toFloat(), size.height / bitmapHeight.toFloat()
        )
        // Letterbox gaps: how many px of black padding appear on each side
        val scaledBmWidth = scaler * bitmapWidth
        val scaledBmHeight = scaler * bitmapHeight
        val gapX = (size.width - scaledBmWidth) / 2f   // horizontal letterbox offset
        val gapY = (size.height - scaledBmHeight) / 2f // vertical letterbox offset
        val displayMetricsDensity = transform.displayMetricsDensity
        val zoomScale = transform.scaler

        uiState.ocrResults.forEach { ocrResultData ->
            if (ocrResultData.text.isNotEmpty()) {

                // ── Project bbox from bitmap space → canvas space ─────────────
                val bBoxTop = ocrResultData.boundingBox.top.toFloat()
                val bBoxLeft = ocrResultData.boundingBox.left.toFloat()
                val bBoxBottom = ocrResultData.boundingBox.bottom.toFloat()
                val bBoxRight = ocrResultData.boundingBox.right.toFloat()

                val rawLeft = (scaler * bBoxLeft) + gapX
                val rawTop = (scaler * bBoxTop) + gapY
                val rawRight = (scaler * bBoxRight) + gapX
                val rawBottom = (scaler * bBoxBottom) + gapY

                // ── Apply configurable minimum-size guard ─────────────────────
                val expanded = expandBBoxIfNeeded(
                    bbox = CanvasBBox(rawLeft, rawTop, rawRight, rawBottom),
                    config = minSizeGuard,
                    canvasWidth = size.width,
                    canvasHeight = size.height
                )

                val rectangleWidth = expanded.width
                val rectangleHeight = expanded.height
                val topLeftOffset = Offset(expanded.left, expanded.top)

                // Draw the filled rectangle
                drawRect(
                    color = Color(0xBF000000),
                    topLeft = topLeftOffset,
                    size = androidx.compose.ui.geometry.Size(rectangleWidth, rectangleHeight)
                )

                // Draw the border over the filled rectangle
                // Counter-scale stroke width to stay visually consistent when zoomed
                drawRect(
                    color = Color(0xFFFF7B00),
                    topLeft = topLeftOffset,
                    size = androidx.compose.ui.geometry.Size(rectangleWidth, rectangleHeight),
                    style = Stroke(width = (strokeWidthDp * displayMetricsDensity) / zoomScale)
                )

                // Prepare to draw the text
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                // Inner padding — increased for more breathing room between the
                // text and the orange border of the OCR bounding box.
                val padding = 4f * displayMetricsDensity
                var textSize = 2f

                // Incrementally increase text size until it just fits
                do {
                    paint.textSize = textSize
                    val textWidth = paint.measureText(ocrResultData.text)
                    val textHeight = paint.descent() - paint.ascent()
                    if (textWidth + padding * 2 <= rectangleWidth && textHeight + padding * 2 <= rectangleHeight) {
                        textSize += 1f
                    } else {
                        break
                    }
                } while (true)

                // Counter-scale text size by zoom so the label stays visually
                // proportional when the user pinches in or out, matching the
                // product-label behaviour in ProductsResultCapturedScreen.
                // coerceAtLeast(16f) ensures a readable minimum even for tiny boxes.
                paint.textSize = ((textSize - 1f) / zoomScale.coerceAtLeast(1f)).coerceAtLeast(16f)

                // Vertically center the text inside the box (baseline correction)
                val textOffsetX = topLeftOffset.x + rectangleWidth / 2
                val textOffsetY =
                    topLeftOffset.y + rectangleHeight / 2 - (paint.ascent() + paint.descent()) / 2

                // Draw the text using nativeCanvas
                drawContext.canvas.nativeCanvas.drawText(
                    ocrResultData.text, textOffsetX, textOffsetY, paint
                )
            }
        }
    }
}

@Composable
fun DrawOCRResultWithTextSizeScalingAndCheckMark(
    uiState: AIDataCaptureDemoUiState,
    transform: OverlayTransform,
    checkIconDrawable: Drawable?,
    displayTotalHeightInPx: Int,
    displayTotalWidthInPx: Int,
    minSizeGuard: MinSizeGuardConfig = MinSizeGuardConfig()
) {
    Canvas( // Layer 3
        modifier = Modifier.fillMaxSize()
    ) {
        val scaler = transform.scaler
        val gapX = transform.gapX
        val gapY = transform.gapY
        val displayMetricsDensity = transform.displayMetricsDensity
        uiState.ocrResults.forEach { ocrResultData ->
            if (ocrResultData.text.isNotEmpty()) {

                val bBoxTop = ocrResultData.boundingBox.top.toFloat()
                val bBoxLeft = ocrResultData.boundingBox.left.toFloat()
                val bBoxBottom = ocrResultData.boundingBox.bottom.toFloat()
                val bBoxRight = ocrResultData.boundingBox.right.toFloat()

                val rawLeft = (scaler * bBoxLeft) + gapX
                val rawTop = (scaler * bBoxTop) + gapY
                val rawRight = (scaler * bBoxRight) + gapX
                val rawBottom = (scaler * bBoxBottom) + gapY

                // ── Apply configurable minimum-size guard ─────────────────────
                val expanded = expandBBoxIfNeeded(
                    bbox = CanvasBBox(rawLeft, rawTop, rawRight, rawBottom),
                    config = minSizeGuard,
                    canvasWidth = displayTotalWidthInPx.toFloat(),
                    canvasHeight = displayTotalHeightInPx.toFloat()
                )

                val rectangleWidth = expanded.width
                val rectangleHeight = expanded.height
                val scaledBBoxLeftInPx = expanded.left
                val scaledBBoxTopInPx = expanded.top
                val topLeftOffset = Offset(expanded.left, expanded.top)

                // Draw the filled rectangle
                drawRect(
                    color = Color(0xBF000000),
                    topLeft = topLeftOffset,
                    size = androidx.compose.ui.geometry.Size(rectangleWidth, rectangleHeight)
                )

                // Draw the border over the filled rectangle
                drawRect(
                    color = Color(0xFFFF7B00),
                    topLeft = topLeftOffset,
                    size = androidx.compose.ui.geometry.Size(rectangleWidth, rectangleHeight),
                    style = Stroke(width = (1f * displayMetricsDensity))
                )

                // Prepare to draw the text
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                // Calculate the maximum text size that fits in the rectangle
                val padding = 0.5f * displayMetricsDensity // Padding from the border
                var textSize = 2f

                // Incrementally increase text size until it just fits
                do {
                    paint.textSize = textSize
                    val textWidth = paint.measureText(ocrResultData.text)
                    val textHeight = paint.descent() - paint.ascent()
                    if (textWidth + padding * 2 <= rectangleWidth && textHeight + padding * 2 <= rectangleHeight) {
                        textSize += 1f
                    } else {
                        break
                    }
                } while (true)

                // Adjust the text size to be slightly smaller
                paint.textSize = textSize - 1f

                // Calculate the position to draw the text
                val textOffsetX = topLeftOffset.x + rectangleWidth / 2
                val textOffsetY =
                    topLeftOffset.y + rectangleHeight / 2 - (paint.ascent() + paint.descent()) / 2

                // Draw the text using nativeCanvas
                drawContext.canvas.nativeCanvas.drawText(
                    ocrResultData.text, textOffsetX, textOffsetY, paint
                )

                checkIconDrawable?.let { icon ->
                    icon.setBounds(
                        (scaledBBoxLeftInPx + (rectangleWidth / 2) - 30F).toInt(),
                        (scaledBBoxTopInPx - 30F - 35f).toInt(),
                        (scaledBBoxLeftInPx + (rectangleWidth / 2) + 30F).toInt(),
                        (scaledBBoxTopInPx + 30F - 35f).toInt()
                    )
                    icon.draw(drawContext.canvas.nativeCanvas)

                }
            }
        }
    }
}

@Composable
fun DrawBarcodeResult(
    uiState: AIDataCaptureDemoUiState,
    transform: OverlayTransform
) {
    // Draw Decoded Text if found
    val scaler = transform.scaler
    val gapX = transform.gapX
    val gapY = transform.gapY
    val displayMetricsDensity = transform.displayMetricsDensity
    Canvas(
        modifier = Modifier.fillMaxSize()
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
            }
        }
    }

    // Draw Decoded Text if found
    uiState.barcodeResults.forEach { barcodeData ->
        barcodeData?.let {
            val bBoxLeft = barcodeData.boundingBox.left.toFloat()
            val bBoxBottom = barcodeData.boundingBox.bottom.toFloat()

            val scaledBBoxLeftInDp = (((scaler * bBoxLeft) + gapX) / displayMetricsDensity).dp
            val scaledBBoxBottomInDp = (((scaler * bBoxBottom) + gapY) / displayMetricsDensity).dp

            if (barcodeData.text != "") {
                Text(
                    text = barcodeData.text,
                    fontSize = 10.sp,
                    color = Color.White,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = false
                        )
                    ),
                    modifier = Modifier
                        .offset(
                            x = scaledBBoxLeftInDp, y = scaledBBoxBottomInDp + 2.dp
                        )
                        .background(Color(0xBF000000))
                        .padding(2.dp)
                )
            }
        }
    }
}

@Composable
private fun DrawModuleRecognitionResult(
    uiState: AIDataCaptureDemoUiState,
    scaler: Float,
    gapX: Float,
    gapY: Float,
    displayMetricsDensity: Float
) {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        uiState.moduleResults.shelves.forEach {
            it.let { shelf ->

                val bBoxTop = shelf.boundingBox.top.toFloat()
                val bBoxLeft = shelf.boundingBox.left.toFloat()
                val bBoxBottom = shelf.boundingBox.bottom.toFloat()
                val bBoxRight = shelf.boundingBox.right.toFloat()

                val scaledBBoxLeftInPx = (scaler * bBoxLeft) + gapX
                val scaledBBoxTopInPx = (scaler * bBoxTop) + gapY
                val scaledBBoxRightInPx = (scaler * bBoxRight) + gapX
                val scaledBBoxBottomInPx = (scaler * bBoxBottom) + gapY

                val rectangleWidth = scaledBBoxRightInPx - scaledBBoxLeftInPx
                val rectangleHeight = scaledBBoxBottomInPx - scaledBBoxTopInPx
                val topLeftOffset = Offset(scaledBBoxLeftInPx, scaledBBoxTopInPx)

                drawRect(
                    color = Color.Red,
                    topLeft = topLeftOffset,
                    size = androidx.compose.ui.geometry.Size(rectangleWidth, rectangleHeight),
                    style = Stroke(width = (1.5f * displayMetricsDensity))
                )

                // Compute gaps inline from shelf products (mirrors ShelfEntityImpl logic)
                val sortedProducts = shelf.products.sortedBy { it.boundingBox.left }
                if (sortedProducts.isNotEmpty()) {
                    val shelfRect = shelf.boundingBox

                    // A gap is significant only if its width is at least half the width of
                    // the smaller neighbouring product, making detection fully dynamic.
                    fun drawGap(
                        gLeft: Int,
                        gTop: Int,
                        gRight: Int,
                        gBottom: Int,
                        minGapWidth: Int
                    ) {
                        if (gRight - gLeft < minGapWidth) return
                        drawRect(
                            color = Color.Yellow,
                            topLeft = Offset((scaler * gLeft) + gapX, (scaler * gTop) + gapY),
                            size = androidx.compose.ui.geometry.Size(
                                scaler * (gRight - gLeft),
                                scaler * (gBottom - gTop)
                            ),
                            style = Stroke(width = (RETAIL_SHELF_GAP_STROKE_WIDTH * displayMetricsDensity))
                        )
                    }

                    // Gap before first product — threshold based on the leftmost product's width
                    val firstRect = sortedProducts.first().boundingBox
                    drawGap(
                        gLeft = shelfRect.left,
                        gTop = minOf(shelfRect.top, firstRect.top),
                        gRight = firstRect.left,
                        gBottom = maxOf(shelfRect.bottom, firstRect.bottom),
                        minGapWidth = (firstRect.width() * 0.7f).toInt()
                    )

                    // Gaps between adjacent products — threshold based on the smaller of the two neighbours
                    for (i in 0 until sortedProducts.size - 1) {
                        val leftRect = sortedProducts[i].boundingBox
                        val rightRect = sortedProducts[i + 1].boundingBox
                        drawGap(
                            gLeft = leftRect.right,
                            gTop = minOf(leftRect.top, rightRect.top),
                            gRight = rightRect.left,
                            gBottom = maxOf(leftRect.bottom, rightRect.bottom),
                            minGapWidth = (minOf(
                                leftRect.width(),
                                rightRect.width()
                            ) * 0.7f).toInt()
                        )
                    }

                    // Gap after last product — threshold based on the rightmost product's width
                    val lastRect = sortedProducts.last().boundingBox
                    drawGap(
                        gLeft = lastRect.right,
                        gTop = minOf(shelfRect.top, lastRect.top),
                        gRight = shelfRect.right,
                        gBottom = maxOf(shelfRect.bottom, lastRect.bottom),
                        minGapWidth = (lastRect.width() * 0.7f).toInt()
                    )
                }
            }
        }
        uiState.moduleResults.labelEntity.forEach {
            it.let { labelEntity ->

                val bBoxTop = labelEntity.boundingBox.top.toFloat()
                val bBoxLeft = labelEntity.boundingBox.left.toFloat()
                val bBoxBottom = labelEntity.boundingBox.bottom.toFloat()
                val bBoxRight = labelEntity.boundingBox.right.toFloat()

                val scaledBBoxLeftInPx = (scaler * bBoxLeft) + gapX
                val scaledBBoxTopInPx = (scaler * bBoxTop) + gapY
                val scaledBBoxRightInPx = (scaler * bBoxRight) + gapX
                val scaledBBoxBottomInPx = (scaler * bBoxBottom) + gapY

                val rectangleWidth = scaledBBoxRightInPx - scaledBBoxLeftInPx
                val rectangleHeight = scaledBBoxBottomInPx - scaledBBoxTopInPx
                val topLeftOffset = Offset(scaledBBoxLeftInPx, scaledBBoxTopInPx)

                drawRect(
                    color = Color.Blue,
                    topLeft = topLeftOffset,
                    size = androidx.compose.ui.geometry.Size(rectangleWidth, rectangleHeight),
                    style = Stroke(width = (1.5f * displayMetricsDensity))
                )

                val barcodes = labelEntity.barcodes
                Log.d(TAG, "Barcodes size: " + barcodes.size)
                if (!barcodes.isEmpty()) {
                    barcodes.forEach { barcode ->
                        val barcodeRect = barcode.boundingBox

                        val bBarcodeBoxTop = barcodeRect.top.toFloat()
                        val bBarcodeBoxLeft = barcodeRect.left.toFloat()
                        val bBarcodeBoxBottom = barcodeRect.bottom.toFloat()
                        val bBarcodeBoxRight = barcodeRect.right.toFloat()

                        val scaledBarcodeBBoxLeftInPx = (scaler * bBarcodeBoxLeft) + gapX
                        val scaledBarcodeBBoxTopInPx = (scaler * bBarcodeBoxTop) + gapY
                        val scaledBarcodeBBoxRightInPx = (scaler * bBarcodeBoxRight) + gapX
                        val scaledBarcodeBBoxBottomInPx = (scaler * bBarcodeBoxBottom) + gapY

                        val barcodeRectangleHeight =
                            scaledBarcodeBBoxBottomInPx - scaledBarcodeBBoxTopInPx
                        Log.d(TAG, "Detected entity - Value: " + barcode.value)
                        Log.d(TAG, "Detected entity - Symbology: " + barcode.symbology)

                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 30f
                        }

                        val barcodeTextOffset = Offset(
                            scaledBarcodeBBoxLeftInPx,
                            scaledBarcodeBBoxTopInPx + (barcodeRectangleHeight) / 2
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            barcode.value, barcodeTextOffset.x, barcodeTextOffset.y, paint
                        )
                    }
                }
            }
        }
        uiState.moduleResults.productEntity.forEach {
            it.let { productEntity ->

                val bBoxTop = productEntity.boundingBox.top.toFloat()
                val bBoxLeft = productEntity.boundingBox.left.toFloat()
                val bBoxBottom = productEntity.boundingBox.bottom.toFloat()
                val bBoxRight = productEntity.boundingBox.right.toFloat()

                val scaledBBoxLeftInPx = (scaler * bBoxLeft) + gapX
                val scaledBBoxTopInPx = (scaler * bBoxTop) + gapY
                val scaledBBoxRightInPx = (scaler * bBoxRight) + gapX
                val scaledBBoxBottomInPx = (scaler * bBoxBottom) + gapY

                val rectangleWidth = scaledBBoxRightInPx - scaledBBoxLeftInPx
                val rectangleHeight = scaledBBoxBottomInPx - scaledBBoxTopInPx
                val topLeftOffset = Offset(scaledBBoxLeftInPx, scaledBBoxTopInPx)

                val topSku = productEntity.topKSKUs?.firstOrNull()?.let {
                    if (it.accuracy > uiState.retailShelfSettings.similarityThreshold / 100) it.productSKU else ""
                } ?: ""
                if (topSku.isEmpty()) {
                    drawRect(
                        color = Color.Green,
                        topLeft = topLeftOffset,
                        size = androidx.compose.ui.geometry.Size(rectangleWidth, rectangleHeight),
                        style = Stroke(width = (1.5f * displayMetricsDensity))
                    )
                } else {
                    drawRect(
                        color = Color(0xAA004830).copy(alpha = 0.5F),
                        topLeft = topLeftOffset,
                        size = androidx.compose.ui.geometry.Size(rectangleWidth, rectangleHeight)
                    )

                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 30f
                    }

                    val textOffset = Offset(
                        scaledBBoxLeftInPx, scaledBBoxTopInPx + (rectangleHeight) / 2
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        topSku, textOffset.x, textOffset.y, paint
                    )
                }
            }
        }

    }
}

@Composable
private fun DrawRetailShelfResult(
    uiState: AIDataCaptureDemoUiState,
    scaler: Float,
    gapX: Float,
    gapY: Float,
    displayMetricsDensity: Float
) {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        uiState.bboxes.forEach { bBox ->
            bBox?.let {

                val bBoxTop = bBox.ymin
                val bBoxLeft = bBox.xmin
                val bBoxBottom = bBox.ymax
                val bBoxRight = bBox.xmax

                val scaledBBoxLeftInPx = (scaler * bBoxLeft) + gapX
                val scaledBBoxTopInPx = (scaler * bBoxTop) + gapY
                val scaledBBoxRightInPx = (scaler * bBoxRight) + gapX
                val scaledBBoxBottomInPx = (scaler * bBoxBottom) + gapY

                val rectangleWidth = scaledBBoxRightInPx - scaledBBoxLeftInPx
                val rectangleHeight = scaledBBoxBottomInPx - scaledBBoxTopInPx
                val topLeftOffset = Offset(scaledBBoxLeftInPx, scaledBBoxTopInPx)

                val boxColor = when (bBox.cls) {
                    1 -> Color.Green // Products
                    2 -> Color.Blue // Shelf Labels
                    3 -> Color.Blue // Peg Labels
                    4 -> Color.Red // Shelf Row
                    else -> {
                        Color.Magenta   // unknown
                    }
                }
                drawRect(
                    color = boxColor,
                    topLeft = topLeftOffset,
                    size = androidx.compose.ui.geometry.Size(rectangleWidth, rectangleHeight),
                    style = Stroke(width = (1.5f * displayMetricsDensity))
                )
            }
        }
    }
}

@Composable
private fun DrawProductRecognitionResult(
    uiState: AIDataCaptureDemoUiState, scaler: Float, gapX: Float, gapY: Float
) {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        uiState.productResults.forEach { productResult ->
            if (productResult.text.isNotEmpty()) {

                val bBoxTop = productResult.bBox.ymin
                val bBoxLeft = productResult.bBox.xmin
                val bBoxBottom = productResult.bBox.ymax
                val bBoxRight = productResult.bBox.xmax

                val scaledBBoxLeftInPx = (scaler * bBoxLeft) + gapX
                val scaledBBoxTopInPx = (scaler * bBoxTop) + gapY
                val scaledBBoxRightInPx = (scaler * bBoxRight) + gapX
                val scaledBBoxBottomInPx = (scaler * bBoxBottom) + gapY

                val rectangleWidth = scaledBBoxRightInPx - scaledBBoxLeftInPx
                val rectangleHeight = scaledBBoxBottomInPx - scaledBBoxTopInPx
                val topLeftOffset = Offset(scaledBBoxLeftInPx, scaledBBoxTopInPx)

                // Draw the filled rectangle
                drawRect(
                    color = Color(0xAA004830).copy(alpha = 0.5F),
                    topLeft = topLeftOffset,
                    size = androidx.compose.ui.geometry.Size(rectangleWidth, rectangleHeight)
                )

                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 30f
                }

                val textOffset = Offset(
                    scaledBBoxLeftInPx, scaledBBoxTopInPx + (rectangleHeight) / 2
                )
                drawContext.canvas.nativeCanvas.drawText(
                    productResult.text, textOffset.x, textOffset.y, paint
                )
            }
        }
    }
}

@Composable
private fun DrawRetailShelfGapResult(
    uiState: AIDataCaptureDemoUiState,
    scaler: Float,
    gapX: Float,
    gapY: Float,
    displayMetricsDensity: Float
) {
    val gaps = detectShelfGaps(uiState.bboxes.filterNotNull()).filter { it.genuine }
    Canvas(modifier = Modifier.fillMaxSize()) {
        gaps.forEach { gap ->
            val scaledLeft = (scaler * gap.left) + gapX
            val scaledTop = (scaler * gap.top) + gapY
            val scaledRight = (scaler * gap.right) + gapX
            val scaledBottom = (scaler * gap.bottom) + gapY
            drawRect(
                color = Color.Yellow,
                topLeft = Offset(scaledLeft, scaledTop),
                size = androidx.compose.ui.geometry.Size(
                    scaledRight - scaledLeft,
                    scaledBottom - scaledTop
                ),
                style = Stroke(width = (3f * displayMetricsDensity))
            )
        }
    }
}


@Composable
fun showBottomBar(
    navController: NavController,
    viewModel: AIDataCaptureDemoViewModel,
    activityInnerPadding: PaddingValues
) {
    val uiState = viewModel.uiState.collectAsState().value
    val torchEnabled = remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(bottom = activityInnerPadding.calculateBottomPadding())
                .background(Color.Black.copy(alpha = 0.4f)),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.CenterVertically)
                        .background(
                            color = if (torchEnabled.value) {
                                Color.White.copy(alpha = 0.4f)
                            } else {
                                Color.Black.copy(alpha = 0.4f)
                            }, shape = RoundedCornerShape(percent = 50)
                        )
                        .clickable {
                            torchEnabled.value = !torchEnabled.value
                            viewModel.enableTorch(torchEnabled.value)
                        }, contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.flashlight_icon),
                        contentDescription = "Torch",
                        modifier = Modifier.size(20.dp),
                        tint = if (torchEnabled.value) Variables.mainDefault else Variables.stateDefaultEnabled
                    )
                }
                if ((uiState.usecaseSelected == UsecaseState.Product.value) || (uiState.usecaseSelected == UsecaseState.Retail.value) || ((uiState.usecaseSelected == UsecaseState.OCRBarcodeFind.value) && (uiState.isCaptureOrLiveEnabled == 0))) {
                    val isClickable = remember { mutableStateOf(true) }
                    // Model must be fully initialized before capture — tapping the shutter before
                    // the pipeline is ready runs takePicture()/executeHighRes() against an
                    // uninitialized native SDK, which crashes natively (SIGSEGV) with no Java
                    // stack trace.
                    val isModelReady = when (uiState.usecaseSelected) {
                        UsecaseState.Product.value, UsecaseState.Retail.value -> uiState.isRetailShelfModelDemoReady
                        UsecaseState.OCRBarcodeFind.value -> (!uiState.isOCRModelEnabled || uiState.isOcrModelDemoReady) && (!uiState.isBarcodeModelEnabled || uiState.isBarcodeModelDemoReady)
                        else -> true
                    }
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.shutter_button),
                        contentDescription = "Capture Image",
                        modifier = Modifier
                            .size(70.dp)
                            .padding(4.dp)
                            .alpha(if (isClickable.value && isModelReady) 1f else 0.4f)
                            .clickable(enabled = isClickable.value && isModelReady) {
                                isClickable.value = false

                                viewModel.viewModelScope.launch {
                                    try {
                                        // Stop analysing the Preview Frames
                                        viewModel.stopPreviewAnalysis()

                                        // Grab High Res Bitmap
                                        val highResBitmap = viewModel.takePicture()

                                        // set the High Res Bitmap to ViewModel
                                        viewModel.updateCaptureBitmap(bitmap = highResBitmap)

                                        // Clear stale live-preview results so the captured
                                        // screen doesn't briefly show mispositioned boxes
                                        viewModel.updateRetailShelfDetectionResult(null)
                                        viewModel.updateProductRecognitionResult(null)

                                        // Send the High Res Image for Processing
                                        viewModel.executeHighRes(highResBitmap = highResBitmap)

                                        when (uiState.usecaseSelected) {
                                            UsecaseState.OCRBarcodeFind.value -> {
                                                viewModel.updateOcrBarcodeCaptureSessionCount(
                                                    uiState.ocrBarcodeCaptureSessionCount + 1
                                                )
                                                navController.navigate(route = Screen.OCRBarcodeCapture.route)
                                            }

                                            UsecaseState.Retail.value -> {
                                                navController.navigate(route = Screen.RetailShelfCapture.route)
                                            }

                                            else -> {
                                                navController.navigate(route = Screen.ProductsCapture.route)
                                            }
                                        }
                                    } catch (e: androidx.camera.core.ImageCaptureException) {
                                        Log.e("CameraPreviewScreen", "Capture failed: ${e.message}")
                                        isClickable.value = true
                                        viewModel.startPreviewAnalysis()
                                    } catch (e: Exception) {
                                        Log.e(
                                            "CameraPreviewScreen",
                                            "Unexpected error during capture: ${e.message}"
                                        )
                                        isClickable.value = true
                                        viewModel.startPreviewAnalysis()
                                    }
                                }
                            },
                        tint = Variables.stateDefaultEnabled
                    )
                }
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.CenterVertically)
                        .background(
                            Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(percent = 50)
                        )
                        .clickable {
                            var zoomRatio: Float = uiState.zoomLevel * 2.0f
                            if (zoomRatio > 4.0f) {
                                zoomRatio = 1.0F
                            }
                            viewModel.setZoom(zoomRatio)
                        }, contentAlignment = Alignment.Center
                ) {
                    val roundedValue = ((uiState.zoomLevel * 10).toInt()).toFloat() / 10
                    Text(
                        text = roundedValue.toString() + "x", style = TextStyle(
                            fontSize = 12.sp,
                            fontFamily = FontFamily(Font(R.font.ibm_plex_sans_regular)),
                            fontWeight = FontWeight(400),
                            color = Variables.mainInverse
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun HandleTopInfo(
    icon: Int, info: String, showInfo: MutableState<Boolean>
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(top = 16.dp, start = 21.dp, end = 21.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 0.87838.dp,
                    color = Color(0xFFF8D249),
                    shape = RoundedCornerShape(size = 360.13556.dp)
                )
                .background(
                    color = Color(0xD9151519), shape = RoundedCornerShape(size = 360.13556.dp)
                )
        ) {
            Row(
                modifier = Modifier.padding(
                    top = Variables.spacingLarge, bottom = Variables.spacingMedium
                ), verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(Variables.spacingLarge))
                Image(
                    painter = painterResource(id = icon),
                    contentDescription = "shutter",
                )
                Spacer(modifier = Modifier.width(Variables.spacingLarge))
                Text(
                    text = info,
                    modifier = Modifier
                        .padding(end = 36.dp)
                        .weight(1f),
                    style = TextStyle(
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                        fontWeight = FontWeight(400),
                        color = Variables.mainInverse,
                    )
                )
                Image(
                    painter = painterResource(id = R.drawable.icon_close),
                    contentDescription = "close",
                    modifier = Modifier
                        .clickable {
                            showInfo.value = false
                        }
                        .background(
                            Color.Black, shape = RoundedCornerShape(size = 360.13556.dp)
                        ))
                Spacer(modifier = Modifier.width(Variables.spacingLarge))
            }
        }
    }
}

@Composable
fun showInformationBox(
    info: String, topPadding: Dp
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(top = topPadding, start = 21.dp, end = 21.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 0.87838.dp,
                    color = Color(0xFFF8D249),
                    shape = RoundedCornerShape(size = 360.13556.dp)
                )
                .background(
                    Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(size = 360.13556.dp)
                )
        ) {
            Row(
                modifier = Modifier.padding(
                    top = Variables.spacingLarge, bottom = Variables.spacingMedium
                ), verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(Variables.spacingLarge))
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search Icon",
                    tint = Variables.mainInverse
                )
                Spacer(modifier = Modifier.width(Variables.spacingLarge))
                Text(
                    text = info,
                    modifier = Modifier
                        .padding(end = 36.dp)
                        .weight(1f),
                    style = TextStyle(
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                        fontWeight = FontWeight(400),
                        color = Variables.mainInverse,
                    )
                )
                Spacer(modifier = Modifier.width(Variables.spacingLarge))
            }
        }
    }
}

/**
 * Returns true when any selected OCR field has RegEx filtering enabled.
 * Used to switch preview overlay style to checkmark mode for regex-filtered OCR results.
 */
private fun hasActiveSelectedOcrRegex(uiState: AIDataCaptureDemoUiState): Boolean {
    val selectedIds = uiState.ocrFilterData.selectedFieldIds.toSet()
    return uiState.ocrFilterData.fieldFilterConfigs.any { cfg ->
        cfg.fieldId in selectedIds && cfg.regexEnabled
    }
}

/**
 * Returns true when any selected OCR field has Character Match enabled
 * with at least one non-empty match token (startsWith / contains / exactMatch).
 * Used for overlay style + feedback behavior in OCR+Barcode Find preview.
 */
private fun hasActiveSelectedOcrCharacterMatch(uiState: AIDataCaptureDemoUiState): Boolean {
    val selectedIds = uiState.ocrFilterData.selectedFieldIds.toSet()
    return uiState.ocrFilterData.fieldFilterConfigs.any { cfg ->
        cfg.fieldId in selectedIds &&
                cfg.characterMatchEnabled &&
                (
                        !cfg.characterMatchData?.startsWithStringList.isNullOrEmpty() ||
                                !cfg.characterMatchData?.containsStringList.isNullOrEmpty() ||
                                !cfg.characterMatchData?.exactMatchStringList.isNullOrEmpty()
                        )
    }
}

/**
 * Builds the "Looking for: [...]" display text from the currently selected OCR
 * field(s)' active character-match tokens (voice search or manual), or null when
 * no character-match filter is active.
 */
private fun activeOcrSearchTermsText(uiState: AIDataCaptureDemoUiState): String? {
    val selectedIds = uiState.ocrFilterData.selectedFieldIds.toSet()
    val terms = uiState.ocrFilterData.fieldFilterConfigs
        .filter { it.fieldId in selectedIds && it.characterMatchEnabled }
        .flatMap { cfg ->
            val cm = cfg.characterMatchData
            (cm?.startsWithStringList.orEmpty() +
                    cm?.containsStringList.orEmpty() +
                    cm?.exactMatchStringList.orEmpty())
        }
        .filter { it.isNotBlank() }
        .distinct()
    return terms.takeIf { it.isNotEmpty() }?.joinToString(", ")
}

/**
 * Returns true when any selected Barcode field has Character Match enabled
 * with at least one non-empty match token.
 * Used to trigger feedback (beep/vibration) only when field-scoped barcode matching is active.
 */
private fun hasActiveSelectedBarcodeCharacterMatch(uiState: AIDataCaptureDemoUiState): Boolean {
    val selectedIds = uiState.barcodeFilterData.selectedFieldIds.toSet()
    return uiState.barcodeFilterData.fieldFilterConfigs.any { cfg ->
        cfg.fieldId in selectedIds &&
                cfg.characterMatchEnabled &&
                (
                        !cfg.characterMatchData?.startsWithStringList.isNullOrEmpty() ||
                                !cfg.characterMatchData?.containsStringList.isNullOrEmpty() ||
                                !cfg.characterMatchData?.exactMatchStringList.isNullOrEmpty()
                        )
    }
}

private fun playFeedback(uiState: AIDataCaptureDemoUiState) {
    if (uiState.ocrResults.isNotEmpty()) {
        if (uiState.ocrBarcodeFindSettings.feedbackSettings.audioBeep) {
            FeedbackUtils.beep()
        }
        if (uiState.ocrBarcodeFindSettings.feedbackSettings.vibration) {
            FeedbackUtils.vibrate()
        }
    }
}