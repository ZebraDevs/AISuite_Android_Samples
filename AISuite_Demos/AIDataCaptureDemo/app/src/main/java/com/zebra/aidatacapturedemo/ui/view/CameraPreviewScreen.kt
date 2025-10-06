package com.zebra.aidatacapturedemo.ui.view

import android.content.Context
import android.util.Log
import android.util.Size
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalDensity
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.zebra.ai.vision.detector.InvalidInputException
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.data.UsecaseState
import com.zebra.aidatacapturedemo.ui.view.Variables.cameraPreviewViewSize
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel
import kotlinx.coroutines.launch


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
    val cameraPreviewResolution = when (viewModel.getSelectedResolution()) {
        0 -> android.util.Size(1280, 720)
        1 -> android.util.Size(1920, 1080)
        2 -> android.util.Size(2688, 1512)
        3 -> android.util.Size(3840, 2160)
        else -> throw InvalidInputException(
            "Invalid video dimension selection"
        )
    }

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
            previewView,
            cameraPreviewResolution,
            lifecycleOwner,
            activityLifecycle
        )
    }
    val density = LocalDensity.current
    val widthInDp: Dp = with(density) { cameraPreviewViewSize.height.toDp() }
    val heightInDp: Dp = with(density) { cameraPreviewViewSize.width.toDp() }

    AndroidView(
        { previewView },
        modifier = Modifier
            .size(widthInDp, heightInDp)
            .offset(0.dp, activityInnerPadding.calculateTopPadding().value.dp)
    )

    when (val selectedDemo = uiState.usecaseSelected) {
        UsecaseState.OCRFind.value,
        UsecaseState.OCR.value -> {

            DrawOCRResult(
                uiState = uiState,
                activityInnerPadding = activityInnerPadding,
                cameraPreviewResolution = cameraPreviewResolution,
                cameraPreviewViewSize = cameraPreviewViewSize
            )
        }

        UsecaseState.Barcode.value -> {
            DrawBarcodeResult(
                uiState = uiState,
                activityInnerPadding = activityInnerPadding,
                cameraPreviewResolution = cameraPreviewResolution,
                cameraPreviewViewSize = cameraPreviewViewSize
            )

        }

        UsecaseState.Retail.value -> {
            DrawRetailShelfResult(
                uiState = uiState,
                activityInnerPadding = activityInnerPadding,
                cameraPreviewResolution = cameraPreviewResolution,
                cameraPreviewViewSize = cameraPreviewViewSize
            )
        }

        UsecaseState.Product.value -> {
            DrawRetailShelfResult(
                uiState = uiState,
                activityInnerPadding = activityInnerPadding,
                cameraPreviewResolution = cameraPreviewResolution,
                cameraPreviewViewSize = cameraPreviewViewSize
            )
            DrawProductRecognitionResult(
                uiState = uiState,
                activityInnerPadding = activityInnerPadding,
                cameraPreviewResolution = cameraPreviewResolution,
                cameraPreviewViewSize = cameraPreviewViewSize
            )
            if (showInfo.value) {
                HandleTopInfo(
                    activityInnerPadding = activityInnerPadding,
                    R.drawable.camera_icon,
                    stringResource(R.string.instruction_1),
                    showInfo
                )
            }
        }

        UsecaseState.Main.value -> {

        }

        else -> {
            TODO("Unhandled usecaseState received = $selectedDemo")
        }
    }

    // Calculate the Camera PreviewView Bottom and set the Bottom Bar padding accordingly at the Bottom align
    val camPreviewViewBottomInDp = heightInDp + activityInnerPadding.calculateTopPadding().value.dp
    val resources = LocalContext.current.resources
    val screenHeightInDp = with(density) {
        resources.displayMetrics.heightPixels.toDp() // px to dp
    }

    val calculatedBottomBarBottomPadding =
        (screenHeightInDp - camPreviewViewBottomInDp) + activityInnerPadding.calculateBottomPadding()

    showBottomBar(
        navController = navController,
        viewModel = viewModel,
        calculatedBottomBarBottomPadding = calculatedBottomBarBottomPadding,
        activityInnerPadding = activityInnerPadding
    )
}

@Composable
fun drawBbox(
    topLeftX: Dp,
    topLeftY: Dp,
    bottomRightX: Dp,
    bottomRightY: Dp,
    text: String? = null,
    retailShelfResultClassType: Int? = null,
    selectedDemo: String
) {
    Canvas(modifier = Modifier.fillMaxSize()) {

        val startX = topLeftX.value.dp.toPx()
        val startY = topLeftY.value.dp.toPx()
        val endX = bottomRightX.value.dp.toPx()
        val endY = bottomRightY.value.dp.toPx()

        // Define the size and position of the rectangle
        val rectangleWidth = endX - startX
        val rectangleHeight = endY - startY
        val topLeftOffset = Offset(startX, startY)

        when (selectedDemo) {
            UsecaseState.OCR.value,
            UsecaseState.OCRFind.value -> {
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
                    style = Stroke(width = 1.dp.toPx())
                )

                // Prepare to draw the text
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                // Calculate the maximum text size that fits in the rectangle
                val padding = 0.5.dp.toPx() // Padding from the border
                var textSize = 2f

                // Incrementally increase text size until it just fits
                do {
                    paint.textSize = textSize
                    val textWidth = paint.measureText(text)
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
                    text!!,
                    textOffsetX,
                    textOffsetY,
                    paint
                )
            }

            UsecaseState.Barcode.value -> {

                drawRect(
                    color = Color.Green,
                    topLeft = topLeftOffset,
                    size = androidx.compose.ui.geometry.Size(rectangleWidth, rectangleHeight),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            UsecaseState.Retail.value -> {

                val boxColor = when (retailShelfResultClassType) {
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
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            UsecaseState.Product.value -> {
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
                    startX,
                    startY + (rectangleHeight) / 2
                )
                drawContext.canvas.nativeCanvas.drawText(
                    text!!,
                    textOffset.x,
                    textOffset.y,
                    paint
                )
            }
        }

    }

    if (selectedDemo == UsecaseState.Barcode.value && text != null && text != "") {
        Text(
            text = text,
            fontSize = 10.sp,
            color = Color.Black,
            style = TextStyle(
                platformStyle = PlatformTextStyle(
                    includeFontPadding = false
                )
            ),
            modifier = Modifier
                .offset(x = topLeftX, y = bottomRightY + 2.dp)
                .background(Color.White)
                .padding(2.dp)
        )
    }

}


@Composable
fun DrawOCRResult(
    uiState: AIDataCaptureDemoUiState,
    activityInnerPadding: PaddingValues,
    cameraPreviewResolution: Size,
    cameraPreviewViewSize: Size
) {
    if (uiState.ocrResults.isNotEmpty()) {
        val resources = LocalContext.current.resources
        val screenDensity = resources.displayMetrics.density
        val scaleFactor =
            cameraPreviewViewSize.width.toFloat() / cameraPreviewResolution.width.toFloat()
        uiState.ocrResults.forEach { ocrResultData ->
            if (ocrResultData.text.isNotEmpty()) {
                val topLeftX = Dp(ocrResultData.boundingBox.left * scaleFactor / screenDensity)
                val topLeftY =
                    Dp((ocrResultData.boundingBox.top * scaleFactor / screenDensity) + activityInnerPadding.calculateTopPadding().value)

                val bottomRightX = Dp(ocrResultData.boundingBox.right * scaleFactor / screenDensity)
                val bottomRightY =
                    Dp((ocrResultData.boundingBox.bottom * scaleFactor / screenDensity) + activityInnerPadding.calculateTopPadding().value)

                drawBbox(
                    topLeftX = topLeftX,
                    topLeftY = topLeftY,
                    bottomRightX = bottomRightX,
                    bottomRightY = bottomRightY,
                    text = ocrResultData.text,
                    selectedDemo = UsecaseState.OCR.value
                )
            }
        }
    }
}

@Composable
fun DrawBarcodeResult(
    uiState: AIDataCaptureDemoUiState,
    activityInnerPadding: PaddingValues,
    cameraPreviewResolution: Size,
    cameraPreviewViewSize: Size
) {
    val resources = LocalContext.current.resources
    val screenDensity = resources.displayMetrics.density
    val scaleFactor =
        cameraPreviewViewSize.width.toFloat() / cameraPreviewResolution.width.toFloat()

    uiState.barcodeResults.forEach { barcodeData ->
        barcodeData?.let {
            val topLeftX = Dp(barcodeData.boundingBox.left * scaleFactor / screenDensity)
            val topLeftY =
                Dp((barcodeData.boundingBox.top * scaleFactor / screenDensity) + activityInnerPadding.calculateTopPadding().value)

            val bottomRightX =
                Dp(barcodeData.boundingBox.right * scaleFactor / screenDensity)
            val bottomRightY =
                Dp((barcodeData.boundingBox.bottom * scaleFactor / screenDensity) + activityInnerPadding.calculateTopPadding().value)

            drawBbox(
                topLeftX = topLeftX,
                topLeftY = topLeftY,
                bottomRightX = bottomRightX,
                bottomRightY = bottomRightY,
                text = barcodeData.text,
                selectedDemo = UsecaseState.Barcode.value
            )

            val value = barcodeData.text
            val count = uiState.barcodeCountMap.get(value) ?: 0
            uiState.barcodeCountMap.put(value, count+1)
        }
    }

    val list = uiState.barcodeCountMap.map { (key, value) -> "$key($value)" }
    val result = list.joinToString(separator = "\n")
    drawBbox(
        0.dp,
        0.dp,
        Variables.cameraPreviewViewSize.width.dp,
        100.dp,
        result,
        selectedDemo = UsecaseState.Barcode.value)
    uiState.barcodeCountMap.clear()
}

@Composable
fun DrawRetailShelfResult(
    uiState: AIDataCaptureDemoUiState,
    activityInnerPadding: PaddingValues,
    cameraPreviewResolution: Size,
    cameraPreviewViewSize: Size
) {
    if (uiState.bboxes.isNotEmpty()) {
        val resources = LocalContext.current.resources
        val screenDensity = resources.displayMetrics.density
        val scaleFactor =
            cameraPreviewViewSize.width.toFloat() / cameraPreviewResolution.width.toFloat()

        uiState.bboxes.forEach { bBox ->
            bBox?.let {
                val topLeftX = Dp(bBox.xmin * scaleFactor / screenDensity)
                val topLeftY =
                    Dp((bBox.ymin * scaleFactor / screenDensity) + activityInnerPadding.calculateTopPadding().value)

                val bottomRightX = Dp(bBox.xmax * scaleFactor / screenDensity)
                val bottomRightY =
                    Dp((bBox.ymax * scaleFactor / screenDensity) + activityInnerPadding.calculateTopPadding().value)

                drawBbox(
                    topLeftX = topLeftX,
                    topLeftY = topLeftY,
                    bottomRightX = bottomRightX,
                    bottomRightY = bottomRightY,
                    retailShelfResultClassType = bBox.cls,
                    selectedDemo = UsecaseState.Retail.value
                )
            }
        }
    }
}

@Composable
fun DrawProductRecognitionResult(
    uiState: AIDataCaptureDemoUiState,
    activityInnerPadding: PaddingValues,
    cameraPreviewResolution: Size,
    cameraPreviewViewSize: Size
) {
    if (uiState.productResults.isNotEmpty()) {
        val resources = LocalContext.current.resources
        val screenDensity = resources.displayMetrics.density
        val scaleFactor =
            cameraPreviewViewSize.width.toFloat() / cameraPreviewResolution.width.toFloat()

        uiState.productResults.forEach { productResult ->
            if (productResult.text.isNotEmpty()) {
                // The product is recognized. Hence draw the SKU on Canvas
                val bBox = productResult.bBox

                val topLeftX = Dp(bBox.xmin * scaleFactor / screenDensity)
                val topLeftY =
                    Dp((bBox.ymin * scaleFactor / screenDensity) + activityInnerPadding.calculateTopPadding().value)

                val bottomRightX = Dp(bBox.xmax * scaleFactor / screenDensity)
                val bottomRightY =
                    Dp((bBox.ymax * scaleFactor / screenDensity) + activityInnerPadding.calculateTopPadding().value)

                drawBbox(
                    topLeftX = topLeftX,
                    topLeftY = topLeftY,
                    bottomRightX = bottomRightX,
                    bottomRightY = bottomRightY,
                    text = productResult.text,
                    selectedDemo = UsecaseState.Product.value
                )
            }
        }
    }
}

@Composable
fun showBottomBar(
    navController: NavController,
    viewModel: AIDataCaptureDemoViewModel,
    calculatedBottomBarBottomPadding: Dp,
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
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.Companion.vectorResource(R.drawable.flashlight_icon),
                        contentDescription = "Torch",
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                torchEnabled.value = !torchEnabled.value
                                viewModel.enableTorch(torchEnabled.value)
                            },
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
                        ),
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
                        ),
                        modifier = Modifier.clickable {
                            var zoomRatio: Float = uiState.zoomLevel * 2.0f
                            if (zoomRatio > 4.0f) {
                                zoomRatio = 1.0F
                            }
                            viewModel.setZoom(zoomRatio)
                        })
                }
            }
        }
    }
}

@Composable
fun HandleTopInfo(
    activityInnerPadding: PaddingValues,
    icon: Int,
    info: String,
    showInfo: MutableState<Boolean>
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(activityInnerPadding)
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
                )
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

