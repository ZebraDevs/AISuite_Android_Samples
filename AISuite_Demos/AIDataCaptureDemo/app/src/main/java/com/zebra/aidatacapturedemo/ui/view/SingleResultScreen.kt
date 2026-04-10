package com.zebra.aidatacapturedemo.ui.view

import android.content.Context
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.getSystemService
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.model.FileUtils.Companion.loadOcrBarcodeCaptureSessionDataFromPrefs
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel
import kotlin.math.min

/**
 * SingleResultScreen is a Composable function that displays the captured image along with the
 * OCR or Barcode results. It retrieves the captured image from the session data, calculates
 * the appropriate scaling and positioning for the bounding boxes,
 * and draws them on top of the image. The screen also handles back navigation to return
 * to the list of results.
 */
private const val TAG = "OCRBarcodeResultCapturedScreen"

@Composable
fun SingleResultScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavHostController,
    innerPadding: PaddingValues,
    context: Context,
    resultRowData: ResultRowData
) {
    val uiState = viewModel.uiState.collectAsState().value

    BackHandler(enabled = true) {
        viewModel.handleBackButton(navController)
    }
    viewModel.updateAppBarTitle(stringResource(R.string.back_to_all_results))
    val sessionData = loadOcrBarcodeCaptureSessionDataFromPrefs(
        context,
        uiState.ocrBarcodeCaptureSessionIndex.toString()
    )
    val capturedBitmap = sessionData?.captureImage?.let { base64String ->
        if (base64String.isNotEmpty()) {
            val bytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } else null
    }
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
            }
            if (resultRowData.isBarcode) {
                // Draw Barcode results
                DrawSingleBarcodeResult(
                    uiState = uiState,
                    scaler = scaler,
                    gapX = gapX,
                    gapY = gapY,
                    displayMetricsDensity = displayMetricsDensity,
                    resultRowData
                )

            } else {
                // Draw OCR results
                DrawSingleOCRResultWithTextSizeScaling(
                    scaler = scaler,
                    gapX = gapX,
                    gapY = gapY,
                    displayMetricsDensity = displayMetricsDensity,
                    displayTotalHeightInPx = displayTotalHeightInPx,
                    displayTotalWidthInPx = displayTotalWidthInPx,
                    resultRowData
                )
            }
        }
    }
}

@Composable
fun DrawSingleOCRResultWithTextSizeScaling(
    scaler: Float,
    gapX: Float,
    gapY: Float,
    displayMetricsDensity: Float,
    displayTotalHeightInPx: Int,
    displayTotalWidthInPx: Int,
    resultRowData: ResultRowData
) {
    Canvas( // Layer 3
        modifier = Modifier
            .fillMaxSize()
    ) {
        val bBoxTop = resultRowData.boundingBox.top.toFloat()
        val bBoxLeft = resultRowData.boundingBox.left.toFloat()
        val bBoxBottom = resultRowData.boundingBox.bottom.toFloat()
        val bBoxRight = resultRowData.boundingBox.right.toFloat()

        var scaledBBoxLeftInPx = (scaler * bBoxLeft) + gapX
        var scaledBBoxTopInPx = (scaler * bBoxTop) + gapY
        var scaledBBoxRightInPx = (scaler * bBoxRight) + gapX
        var scaledBBoxBottomInPx = (scaler * bBoxBottom) + gapY

        // Define the size and position of the rectangle
        var rectangleWidth = scaledBBoxRightInPx - scaledBBoxLeftInPx
        var rectangleHeight = scaledBBoxBottomInPx - scaledBBoxTopInPx

        // This is preventing the Text to show too small on the drawing
        if (rectangleHeight <= 20f || rectangleWidth <= 20f) {

            // Firstly, try increase the BBox Height by 40Px
            scaledBBoxTopInPx -= 20f

            // Make sure, the scaling fit within the Screen at Top.
            if (scaledBBoxTopInPx < 0) {
                scaledBBoxTopInPx = 0f
            }

            scaledBBoxBottomInPx += 20f
            // Make sure, the scaling fit within the Screen at Bottom.
            if (scaledBBoxBottomInPx > displayTotalHeightInPx.toFloat()) {
                scaledBBoxBottomInPx = displayTotalHeightInPx.toFloat()
            }

            // recalculate the height
            rectangleHeight = scaledBBoxBottomInPx - scaledBBoxTopInPx

            // Secondly, try increase the BBox Width by 40Px
            scaledBBoxLeftInPx -= 20f

            // Make sure, the scaling fit within the Screen at Left.
            if (scaledBBoxLeftInPx < 0) {
                scaledBBoxLeftInPx = 0f
            }

            scaledBBoxRightInPx += 20f
            // Make sure, the scaling fit within the Screen at Right.
            if (scaledBBoxRightInPx > displayTotalWidthInPx.toFloat()) {
                scaledBBoxRightInPx = displayTotalWidthInPx.toFloat()
            }

            // recalculate the Width
            rectangleWidth = scaledBBoxRightInPx - scaledBBoxLeftInPx
        }

        val topLeftOffset = Offset(scaledBBoxLeftInPx, scaledBBoxTopInPx)

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
            val textWidth = paint.measureText(resultRowData.text)
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
            resultRowData.text,
            textOffsetX,
            textOffsetY,
            paint
        )
    }
}

@Composable
fun DrawSingleBarcodeResult(
    uiState: AIDataCaptureDemoUiState,
    scaler: Float,
    gapX: Float,
    gapY: Float,
    displayMetricsDensity: Float,
    resultRowData: ResultRowData
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val bBoxTop = resultRowData.boundingBox.top.toFloat()
        val bBoxLeft = resultRowData.boundingBox.left.toFloat()
        val bBoxBottom = resultRowData.boundingBox.bottom.toFloat()
        val bBoxRight = resultRowData.boundingBox.right.toFloat()

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

    // Draw Decoded Text if found
    val bBoxLeft = resultRowData.boundingBox.left.toFloat()
    val bBoxBottom = resultRowData.boundingBox.bottom.toFloat()

    val scaledBBoxLeftInDp = (((scaler * bBoxLeft) + gapX) / displayMetricsDensity).dp
    val scaledBBoxBottomInDp = (((scaler * bBoxBottom) + gapY) / displayMetricsDensity).dp

    if (resultRowData.text != "") {
        Text(
            text = resultRowData.text,
            fontSize = 10.sp,
            color = Color.White,
            style = TextStyle(
                platformStyle = PlatformTextStyle(
                    includeFontPadding = false
                )
            ),
            modifier = Modifier
                .offset(x = scaledBBoxLeftInDp, y = scaledBBoxBottomInDp + 2.dp)
                .background(Color(0xBF000000))
                .padding(2.dp)
        )
    }
}
