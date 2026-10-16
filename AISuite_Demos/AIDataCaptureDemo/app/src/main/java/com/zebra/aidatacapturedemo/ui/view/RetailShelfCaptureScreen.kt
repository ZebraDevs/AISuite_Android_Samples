package com.zebra.aidatacapturedemo.ui.view

import android.content.Context
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getSystemService
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.zebra.ai.vision.detector.BBox
import com.zebra.aidatacapturedemo.data.ProductData
import com.zebra.aidatacapturedemo.helper.ZoomConfig
import com.zebra.aidatacapturedemo.model.detectShelfGaps
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel
import kotlinx.coroutines.launch
import kotlin.math.min

private val RetailShelfGapStrokeWidth = 3.dp
private const val CLS_SHELF_LABEL = 2
private const val CLS_PEG_LABEL = 3
private const val CLS_SHELF = 4

@Composable
fun RetailShelfCaptureScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavController,
    context: Context,
    zoomConfig: ZoomConfig = ZoomConfig()
) {
    val uiState = viewModel.uiState.collectAsState().value
    BackHandler(enabled = true) { viewModel.handleBackButton(navController) }

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
        val displayMetrics = LocalContext.current.resources.displayMetrics
        val displayMetricsDensity = displayMetrics.density

        val windowManager = getSystemService(context, WindowManager::class.java)
        val windowMetrics: WindowMetrics = windowManager.currentWindowMetrics

        val displayTotalWidthInPx = windowMetrics.bounds.width()
        val displayTotalHeightInPx = windowMetrics.bounds.height()

        val displayStatusBarPaddingValues = WindowInsets.statusBars.asPaddingValues()
        val displayStatusBarHeightInDp = displayStatusBarPaddingValues.calculateTopPadding()
        val displayStatusBarHeightInPx = displayStatusBarHeightInDp.value * displayMetricsDensity

        val displayNavigationBarPaddingValues = WindowInsets.navigationBars.asPaddingValues()
        val displayNavigationBarHeightInDp =
            displayNavigationBarPaddingValues.calculateBottomPadding()
        val displayNavigationBarHeightInPx =
            displayNavigationBarHeightInDp.value * displayMetricsDensity

        val availableHeightInPx = displayTotalHeightInPx.toFloat() - displayStatusBarHeightInPx -
                displayNavigationBarHeightInPx

        val scaler = min(
            displayTotalWidthInPx.toFloat() / capturedBitmap.width.toFloat(),
            availableHeightInPx / capturedBitmap.height.toFloat()
        )
        val scaledWidth = scaler * capturedBitmap.width.toFloat()
        val scaledHeight = scaler * capturedBitmap.height.toFloat()
        val gapX = (displayTotalWidthInPx - scaledWidth) / 2f
        val gapY = (availableHeightInPx - scaledHeight) / 2f

        // ── Animatable zoom / pan state ────────────────────────────────────
        val animScale = remember { Animatable(1f) }
        val animOffsetX = remember { Animatable(0f) }
        val animOffsetY = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()

        val snapBackSpec = remember(zoomConfig) {
            spring<Float>(
                dampingRatio = zoomConfig.snapBackDampingRatio,
                stiffness = zoomConfig.snapBackStiffness
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = displayStatusBarHeightInDp, bottom =
                        displayNavigationBarHeightInDp
                )
                .background(color = Color.Black)
        ) {
            // Zoomable + pannable container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(zoomConfig) {
                        detectTransformGestures(
                            onGesture = { centroid, pan, zoom, _ ->
                                scope.launch {
                                    val oldScale = animScale.value
                                    val newScale = (oldScale * zoom).coerceIn(
                                        zoomConfig.minZoom, zoomConfig.maxZoom
                                    )

                                    val w2 = size.width / 2f
                                    val h2 = size.height / 2f
                                    val scaleRatio = newScale / oldScale

                                    val newOffsetX =
                                        (centroid.x - w2) * (1f - scaleRatio) + animOffsetX.value * scaleRatio + pan.x * zoomConfig.panSensitivity

                                    val newOffsetY =
                                        (centroid.y - h2) * (1f - scaleRatio) + animOffsetY.value * scaleRatio + pan.y * zoomConfig.panSensitivity

                                    val maxX = (newScale - 1f) * size.width / 2f
                                    val maxY = (newScale - 1f) * size.height / 2f

                                    animScale.snapTo(newScale)
                                    animOffsetX.snapTo(newOffsetX.coerceIn(-maxX, maxX))
                                    animOffsetY.snapTo(newOffsetY.coerceIn(-maxY, maxY))
                                }
                            })
                    }
                    .pointerInput(zoomConfig) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val allUp = event.changes.all { !it.pressed }
                                if (allUp && animScale.value < zoomConfig.minZoom) {
                                    scope.launch {
                                        launch { animScale.animateTo(zoomConfig.minZoom, snapBackSpec) }
                                        launch { animOffsetX.animateTo(0f, snapBackSpec) }
                                        launch { animOffsetY.animateTo(0f, snapBackSpec) }
                                    }
                                }
                            }
                        }
                    }
                    .graphicsLayer(
                        scaleX = animScale.value,
                        scaleY = animScale.value,
                        translationX = animOffsetX.value,
                        translationY = animOffsetY.value
                    )
            ) {
                Image(
                    painter = rememberAsyncImagePainter(capturedBitmap),
                    contentDescription = "Captured Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                DrawRetailShelfResult(
                    bboxes = uiState.bboxes,
                    productResultsList = uiState.productResults,
                    scaler = scaler,
                    gapX = gapX,
                    gapY = gapY,
                    displayMetricsDensity = displayMetricsDensity,
                    zoomScale = animScale.value,
                    zoomConfig = zoomConfig
                )
            }
        }
    }
}

@Composable
private fun DrawRetailShelfResult(
    bboxes: List<BBox?>,
    productResultsList: MutableList<ProductData>,
    scaler: Float,
    gapX: Float,
    gapY: Float,
    displayMetricsDensity: Float,
    zoomScale: Float = 1f,
    zoomConfig: ZoomConfig = ZoomConfig()
) {
    fun leftDp(leftPx: Float) = (((scaler * leftPx) + gapX) / displayMetricsDensity).dp
    fun topDp(topPx: Float) = (((scaler * topPx) + gapY) / displayMetricsDensity).dp
    fun rightDp(rightPx: Float) = (((scaler * rightPx) + gapX) / displayMetricsDensity).dp
    fun bottomDp(bottomPx: Float) = (((scaler * bottomPx) + gapY) / displayMetricsDensity).dp

    @Composable
    fun DrawBox(
        left: Float, top: Float, right: Float, bottom: Float, color: Color,
        strokeWidth: Dp = 1.dp
    ) {
        val l = leftDp(left)
        val t = topDp(top)
        val r = rightDp(right)
        val b = bottomDp(bottom)
        Box(
            modifier = Modifier
                .padding(start = l, top = t)
                .border(BorderStroke(strokeWidth, color))
                .width(r - l)
                .height(b - t)
        )
    }

    val nonProductBoxes = bboxes.filterNotNull()

    // Shelf rows & labels — same classes/colors as the generic live overlay.
    nonProductBoxes.forEach { bBox ->
        val color = when (bBox.cls) {
            CLS_SHELF -> Color.Red
            CLS_SHELF_LABEL, CLS_PEG_LABEL -> Color.Blue
            else -> Color.Magenta
        }
        DrawBox(bBox.xmin, bBox.ymin, bBox.xmax, bBox.ymax, color)
    }

    // Shelf gaps — geometry only locates candidates and filters trivial spacing; Localizer
    // confidence (BBox.prob) on the bordering products decides whether to trust them.
    detectShelfGaps(nonProductBoxes)
        .filter { it.genuine }
        .forEach { gap ->
            DrawBox(
                gap.left, gap.top, gap.right, gap.bottom, Color.Yellow,
                RetailShelfGapStrokeWidth
            )
        }

    // Recognized products — same rendering as Product & Shelf Enrollment's results.
    productResultsList.forEach { productData ->
        val l = leftDp(productData.bBox.xmin)
        val t = topDp(productData.bBox.ymin)
        val r = rightDp(productData.bBox.xmax)
        val b = bottomDp(productData.bBox.ymax)
        val w = r - l
        val h = b - t
        val topSku = productData.text

        if (topSku.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(start = l, top = t)
                    .border(BorderStroke(1.dp, Color.Green))
                    .width(w)
                    .height(h)
            )
    } else {
            Box(
                modifier = Modifier
                    .padding(start = l, top = t)
                    .border(BorderStroke(1.dp, Color.Green))
                    .background(Color(0xAA004830).copy(alpha = 0.5F))
                    .width(w)
                    .height(h)
            ) {
                AutoSizedCenteredProductLabel(
                    text = topSku,
                    boxWidth = w,
                    boxHeight = h,
                    zoomScale = zoomScale,
                    zoomConfig = zoomConfig
                )
            }
        }
    }
}