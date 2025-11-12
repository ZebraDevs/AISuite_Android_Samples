package com.zebra.aidatacapturedemo.ui.view

import android.content.Context
import android.graphics.drawable.Drawable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import com.zebra.aidatacapturedemo.data.OCRFilterType
import com.zebra.aidatacapturedemo.data.UsecaseState
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel
import kotlinx.coroutines.launch
import kotlin.math.min


@Composable
fun CameraPreviewScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavController,
    context: Context,
    activityInnerPadding: PaddingValues,
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
            UsecaseState.OCRFind.value,
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
                top = displayStatusBarHeightInDp,
                bottom = displayNavigationBarHeightInDp
            )
    ) {
        AndroidView( // 2 layer
            { previewView }
        )

        when (val selectedDemo = uiState.usecaseSelected) {
            UsecaseState.OCRFind.value -> {

                if (uiState.selectedOCRFilterData.ocrFilterType == OCRFilterType.EXACT_MATCH) {
                    val checkIconDrawable = ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_check
                    )
                    DrawOCRResultWithCheckMark(
                        uiState = uiState,
                        scaler = scaler,
                        gapX = gapX,
                        gapY = gapY,
                        displayMetricsDensity = displayMetricsDensity,
                        checkIconDrawable = checkIconDrawable
                    )
                    if (uiState.ocrResults.isNotEmpty()) {
                        FeedbackUtils.vibrate()
                        FeedbackUtils.beep()
                    }
                    showInformationBox(
                        info = "Looking for: ${uiState.selectedOCRFilterData.exactMatchStringList}",
                        topPadding = activityInnerPadding.calculateTopPadding() + displayStatusBarHeightInDp
                    )
                } else if (uiState.selectedOCRFilterData.ocrFilterType == OCRFilterType.STARTS_WITH) {
                    val checkIconDrawable = ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_check
                    )
                    DrawOCRResultWithCheckMark(
                        uiState = uiState,
                        scaler = scaler,
                        gapX = gapX,
                        gapY = gapY,
                        displayMetricsDensity = displayMetricsDensity,
                        checkIconDrawable = checkIconDrawable
                    )
                    if (uiState.ocrResults.isNotEmpty()) {
                        FeedbackUtils.vibrate()
                        FeedbackUtils.beep()
                    }
                    showInformationBox(
                        info = "Looking for Starts with: ${uiState.selectedOCRFilterData.startsWithString}",
                        topPadding = activityInnerPadding.calculateTopPadding() + displayStatusBarHeightInDp
                    )
                } else if (uiState.selectedOCRFilterData.ocrFilterType == OCRFilterType.CONTAINS) {
                    val checkIconDrawable = ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_check
                    )
                    DrawOCRResultWithCheckMark(
                        uiState = uiState,
                        scaler = scaler,
                        gapX = gapX,
                        gapY = gapY,
                        displayMetricsDensity = displayMetricsDensity,
                        checkIconDrawable = checkIconDrawable
                    )
                    if (uiState.ocrResults.isNotEmpty()) {
                        FeedbackUtils.vibrate()
                        FeedbackUtils.beep()
                    }
                    showInformationBox(
                        info = "Looking for Contains: ${uiState.selectedOCRFilterData.containsString}",
                        topPadding = activityInnerPadding.calculateTopPadding() + displayStatusBarHeightInDp
                    )
                } else if (uiState.selectedOCRFilterData.ocrFilterType == OCRFilterType.REGEX) {
                    val checkIconDrawable = ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_check
                    )
                    DrawOCRResultWithCheckMark(
                        uiState = uiState,
                        scaler = scaler,
                        gapX = gapX,
                        gapY = gapY,
                        displayMetricsDensity = displayMetricsDensity,
                        checkIconDrawable = checkIconDrawable
                    )
                    if (uiState.ocrResults.isNotEmpty()) {
                        FeedbackUtils.vibrate()
                        FeedbackUtils.beep()
                    }
                    showInformationBox(
                        info = "Looking for Regex: ${uiState.selectedOCRFilterData.regexString}",
                        topPadding = activityInnerPadding.calculateTopPadding() + displayStatusBarHeightInDp
                    )
                }else {
                    DrawOCRResult(
                        uiState = uiState,
                        scaler = scaler,
                        gapX = gapX,
                        gapY = gapY,
                        displayMetricsDensity = displayMetricsDensity
                    )
                }
            }

            UsecaseState.OCR.value -> {

                DrawOCRResult(
                    uiState = uiState,
                    scaler = scaler,
                    gapX = gapX,
                    gapY = gapY,
                    displayMetricsDensity = displayMetricsDensity
                )
            }

            UsecaseState.Barcode.value -> {
                DrawBarcodeResult(
                    uiState = uiState,
                    scaler = scaler,
                    gapX = gapX,
                    gapY = gapY,
                    displayMetricsDensity = displayMetricsDensity
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
                    uiState = uiState,
                    scaler = scaler,
                    gapX = gapX,
                    gapY = gapY
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
                TODO("Unhandled usecaseState received = $selectedDemo")
            }
        }
    }

    uiState.cameraError?.let {
        showCameraErrorText()
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
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
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
                text = stringResource(R.string.instruction_6),
                style = TextStyle(
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
    scaler: Float,
    gapX: Float,
    gapY: Float,
    displayMetricsDensity: Float
) {
    Canvas( // Layer 3
        modifier = Modifier
            .fillMaxSize()
    ) {
        uiState.ocrResults.forEach { ocrResultData ->
            if (ocrResultData.text.isNotEmpty()) {

                val bBoxTop = ocrResultData.boundingBox.top.toFloat()
                val bBoxLeft = ocrResultData.boundingBox.left.toFloat()
                val bBoxBottom = ocrResultData.boundingBox.bottom.toFloat()
                val bBoxRight = ocrResultData.boundingBox.right.toFloat()

                val scaledBBoxLeftInPx = (scaler * bBoxLeft) + gapX
                val scaledBBoxTopInPx = (scaler * bBoxTop) + gapY
                val scaledBBoxRightInPx = (scaler * bBoxRight) + gapX
                val scaledBBoxBottomInPx = (scaler * bBoxBottom) + gapY

                // Define the size and position of the rectangle
                val rectangleWidth = scaledBBoxRightInPx - scaledBBoxLeftInPx
                val rectangleHeight = scaledBBoxBottomInPx - scaledBBoxTopInPx
                val topLeftOffset = Offset(scaledBBoxLeftInPx, scaledBBoxTopInPx)

                // Draw the filled rectangle
                drawRect(
                    color = Color.White.copy(alpha = 0.8F),
                    topLeft = topLeftOffset,
                    size = androidx.compose.ui.geometry.Size(rectangleWidth, rectangleHeight)
                )

                // Draw the border over the filled rectangle
                drawRect(
                    color = Color.Green,
                    topLeft = topLeftOffset,
                    size = androidx.compose.ui.geometry.Size(rectangleWidth, rectangleHeight),
                    style = Stroke(width = (1f * displayMetricsDensity))
                )

                // Prepare to draw the text
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
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
                    ocrResultData.text,
                    textOffsetX,
                    textOffsetY,
                    paint
                )
            }
        }
    }
}

@Composable
fun DrawOCRResultWithCheckMark(
    uiState: AIDataCaptureDemoUiState,
    scaler: Float,
    gapX: Float,
    gapY: Float,
    displayMetricsDensity: Float,
    checkIconDrawable: Drawable?
) {
    Canvas( // Layer 3
        modifier = Modifier
            .fillMaxSize()
    ) {
        uiState.ocrResults.forEach { ocrResultData ->
            if (ocrResultData.text.isNotEmpty()) {

                val bBoxTop = ocrResultData.boundingBox.top.toFloat()
                val bBoxLeft = ocrResultData.boundingBox.left.toFloat()
                val bBoxBottom = ocrResultData.boundingBox.bottom.toFloat()
                val bBoxRight = ocrResultData.boundingBox.right.toFloat()

                val scaledBBoxLeftInPx = (scaler * bBoxLeft) + gapX
                val scaledBBoxTopInPx = (scaler * bBoxTop) + gapY
                val scaledBBoxRightInPx = (scaler * bBoxRight) + gapX
                val scaledBBoxBottomInPx = (scaler * bBoxBottom) + gapY

                // Define the size and position of the rectangle
                val rectangleWidth = scaledBBoxRightInPx - scaledBBoxLeftInPx
                val rectangleHeight = scaledBBoxBottomInPx - scaledBBoxTopInPx
                val topLeftOffset = Offset(scaledBBoxLeftInPx, scaledBBoxTopInPx)

                // Draw the filled rectangle
                drawRect(
                    color = Color.White.copy(alpha = 0.8F),
                    topLeft = topLeftOffset,
                    size = androidx.compose.ui.geometry.Size(rectangleWidth, rectangleHeight)
                )

                // Draw the border over the filled rectangle
                drawRect(
                    color = Color.Green,
                    topLeft = topLeftOffset,
                    size = androidx.compose.ui.geometry.Size(rectangleWidth, rectangleHeight),
                    style = Stroke(width = (1f * displayMetricsDensity))
                )

                // Prepare to draw the text
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
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
                    ocrResultData.text,
                    textOffsetX,
                    textOffsetY,
                    paint
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

            if (barcodeData.text != null && barcodeData.text != "") {
                Text(
                    text = barcodeData.text,
                    fontSize = 10.sp,
                    color = Color.Black,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = false
                        )
                    ),
                    modifier = Modifier
                        .offset(x = scaledBBoxLeftInDp, y = scaledBBoxBottomInDp + 2.dp)
                        .background(Color.White)
                        .padding(2.dp)
                )
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
        modifier = Modifier
            .fillMaxSize()
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
    uiState: AIDataCaptureDemoUiState,
    scaler: Float,
    gapX: Float,
    gapY: Float
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
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
                    scaledBBoxLeftInPx,
                    scaledBBoxTopInPx + (rectangleHeight) / 2
                )
                drawContext.canvas.nativeCanvas.drawText(
                    productResult.text,
                    textOffset.x,
                    textOffset.y,
                    paint
                )
            }
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
    var torchEnabled = remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
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
                            },
                            shape = RoundedCornerShape(percent = 50)
                        )
                        .clickable {
                            torchEnabled.value = !torchEnabled.value
                            viewModel.enableTorch(torchEnabled.value)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.Companion.vectorResource(R.drawable.flashlight_icon),
                        contentDescription = "Torch",
                        modifier = Modifier
                            .size(20.dp),
                        tint = if (torchEnabled.value) Variables.mainDefault else Variables.stateDefaultEnabled
                    )
                }
                if (uiState.usecaseSelected == UsecaseState.Product.value) {
                    var isClickable = remember { mutableStateOf(true) }
                    Icon(
                        imageVector = ImageVector.Companion.vectorResource(R.drawable.shutter_button),
                        contentDescription = "Capture Image",
                        modifier = Modifier
                            .size(70.dp)
                            .padding(4.dp)
                            .clickable(enabled = isClickable.value) {
                                isClickable.value = false
                                viewModel.viewModelScope.launch {
                                    // Stop analysing the Preview Frames
                                    viewModel.stopPreviewAnalysis()

                                    // Grab High Res Bitmap
                                    val highResBitmap = viewModel.takePicture()

                                    // set the High Res Bitmap to ViewModel
                                    viewModel.updateCaptureBitmap(bitmap = highResBitmap)

                                    // Send the High Res Image for Processing
                                    viewModel.executeHighRes(highResBitmap = highResBitmap)

                                    navController.navigate(route = Screen.Capture.route)
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
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val roundedValue = ((uiState.zoomLevel * 10).toInt()).toFloat() / 10
                    Text(
                        text = roundedValue.toString() + "x",
                        style = TextStyle(
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
    icon: Int,
    info: String,
    showInfo: MutableState<Boolean>
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
                    Color.Black.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(size = 360.13556.dp)
                )
        ) {
            Row(
                modifier = Modifier.padding(
                    top = Variables.spacingLarge,
                    bottom = Variables.spacingMedium
                ),
                verticalAlignment = Alignment.CenterVertically
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
                            Color.Black,
                            shape = RoundedCornerShape(size = 360.13556.dp)
                        )
                )
                Spacer(modifier = Modifier.width(Variables.spacingLarge))
            }
        }
    }
}

@Composable
fun showInformationBox(
    info: String,
    topPadding: Dp
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
                    Color.Black.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(size = 360.13556.dp)
                )
        ) {
            Row(
                modifier = Modifier.padding(
                    top = Variables.spacingLarge,
                    bottom = Variables.spacingMedium
                ),
                verticalAlignment = Alignment.CenterVertically
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