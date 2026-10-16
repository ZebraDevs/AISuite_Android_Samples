// Copyright (c) 2024-2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.aidatacapturedemo.ui.view

import android.content.Context
import android.util.Log
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.data.FitResult
import com.zebra.aidatacapturedemo.data.ProductData
import com.zebra.aidatacapturedemo.helper.ZoomConfig
import com.zebra.aidatacapturedemo.helper.datawedge.DataWedgeManager
import com.zebra.aidatacapturedemo.model.OverlayTransform
import com.zebra.aidatacapturedemo.ui.view.Variables.borderPrimaryMain
import com.zebra.aidatacapturedemo.ui.view.Variables.mainPrimary
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.min

private const val TAG = "ProductsResultCapturedScreen"

/**
 * ProductsResultCapturedScreen composable function to display the captured high resolution
 * image with bounding boxes and product SKU's
 * The user can tap on the product bounding boxes that brings up a dialog box.
 *      The dialog box displays cropped product image displayed and an edit box wherein the user can
 *      input SKU manually, or scan a barcode by pressing yellow scan button that then invokes
 *      Datawedge Profile 0 (in enabled) to scan the barcode.
 *      User can then press confirm button to associate the SKU to the product image and bounding box.
 * When the user presses save product database button, the products.db is saved in the Downloads
 * folder and a timestamped folder is created in Pictures Folder,
 * within which Product SKU folder is created and the product image crops are saved that folder.
 */
@Composable
fun ProductsResultCapturedScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavController,
    activityInnerPadding: PaddingValues,
    context: Context,
    zoomConfig: ZoomConfig = ZoomConfig()
) {
    val uiState = viewModel.uiState.collectAsState().value
    val productResultsList = uiState.productResults
    val isSoftScanBarcodeButtonEnabled = FeatureFlags.IS_SOFT_SCAN_BARCODE_BUTTON_ENABLED
    val lifecycleOwner = LocalLifecycleOwner.current

    val dataWedgeManager = if (isSoftScanBarcodeButtonEnabled) {
        remember(context.applicationContext) {
            DataWedgeManager(context.applicationContext)
        }
    } else null

    val stopSoftScan = {
        if (isSoftScanBarcodeButtonEnabled) {
            dataWedgeManager?.stopSoftScan()
        }
    }

    val startSoftScan = {
        if (isSoftScanBarcodeButtonEnabled) {
            dataWedgeManager?.startSoftScan()
        }
    }

    BackHandler(enabled = true) {
        stopSoftScan()
        viewModel.handleBackButton(navController)
    }

    if (isSoftScanBarcodeButtonEnabled) {
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE,
                    Lifecycle.Event.ON_STOP,
                    Lifecycle.Event.ON_DESTROY -> stopSoftScan()
                    else -> Unit
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                stopSoftScan()
            }
        }
    }

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

        var showInfo = remember { mutableStateOf(true) }

        var isProductEnrollmentProgressBarVisible by remember { mutableStateOf(false) }
        val productShowDialog = remember { mutableStateOf(false) }
        val productSKUChanged = remember { mutableStateOf(false) }
        val selectedProductForDialog = remember { mutableStateOf<ProductData?>(null) }
        val selectedProductSku = rememberSaveable { mutableStateOf("") }
//        Added because if we are at 2x zoom and try to enroll product initially it will not display
        val skuOverrides = remember { mutableStateMapOf<ProductData, String>() }

        if (productSKUChanged.value) {
            selectedProductForDialog.value?.let { product ->
                val oldSku = product.text
                val newSku = selectedProductSku.value
                if (oldSku.isNotEmpty()) {
                    // Re-edit: update ALL products that had the same old SKU
                    // so they all get enrolled under the new SKU in the database
                    productResultsList.forEach { p ->
                        if (p.text == oldSku) {
                            p.text = newSku
                            skuOverrides[p] = newSku
                        }
                    }
                } else {
                    // First-time assignment: only update this one product
                    product.text = newSku
                    skuOverrides[product] = newSku
                }
            }
            productSKUChanged.value = false
        }

        selectedProductForDialog.value?.takeIf { productShowDialog.value }?.let { productData ->
            ProductAlertDialog(
                productShowDialog = productShowDialog,
                productImage = productData.crop.asImageBitmap(),
                productSKU = selectedProductSku,
                productSKUChanged = productSKUChanged,
                isSoftScanBarcodeButtonEnabled = isSoftScanBarcodeButtonEnabled,
                onScanBarcodeClick = startSoftScan,
                onStopSoftScan = stopSoftScan
            )
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

        Box( // Bottom layer
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = displayStatusBarHeightInDp,
                    bottom = displayNavigationBarHeightInDp
                )
                .background(color = Color.Black)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(zoomConfig, productShowDialog.value) {
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
                    .pointerInput(
                        productShowDialog.value,
                        productResultsList.size,
                        scaler,
                        gapX,
                        gapY,
                        displayMetricsDensity,
                        animScale.value,
                        animOffsetX.value,
                        animOffsetY.value
                    ) {
                        detectTapGestures { tapOffset ->
                            if (productShowDialog.value) return@detectTapGestures

                            findTappedProduct(
                                tapOffset = tapOffset,
                                containerWidth = size.width.toFloat(),
                                containerHeight = size.height.toFloat(),
                                zoomScale = animScale.value,
                                translationX = animOffsetX.value,
                                translationY = animOffsetY.value,
                                productResultsList = productResultsList,
                                transform = OverlayTransform(
                                    scaler = scaler,
                                    gapX = gapX,
                                    gapY = gapY,
                                    displayMetricsDensity = displayMetricsDensity
                                )
                            )?.let { productData ->
                                stopSoftScan()
                                selectedProductForDialog.value = productData
                                selectedProductSku.value = productData.text
                                productShowDialog.value = true
                            }
                        }
                    }
                    .pointerInput(zoomConfig, productShowDialog.value) {
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

            // CAPTURED IMAGE
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = capturedBitmap.asImageBitmap(),
                    contentDescription = "Captured Image",
                    contentScale = ContentScale.Fit
                )
            }

            // draw Shelf Label, Peg Label & Shelf Row only
            DrawRetailShelfLabelsAndRowsUsingBox(
                uiState = uiState,
                scaler = scaler,
                gapX = gapX,
                gapY = gapY,
                displayMetricsDensity = displayMetricsDensity
            )

            // draw products (with Recognition)
            DrawRetailShelfProductsUsingBox(
                productResultsList = productResultsList,
                transform = OverlayTransform(
                    scaler = scaler,
                    gapX = gapX,
                    gapY = gapY,
                    displayMetricsDensity = displayMetricsDensity
                ),
                zoomScale = animScale.value,
                zoomConfig = zoomConfig,
                skuOverrides = skuOverrides
            )
            }

            if (showInfo.value) {
                val topInfoInstruction = if (isProductEnrollmentProgressBarVisible) {
                    stringResource(R.string.instruction_enrolling_into_db)
                } else if (productResultsList.isEmpty()) {
                    stringResource(R.string.instruction_5)
                } else {
                    stringResource(R.string.instruction_2)
                }

                val startIcon = if (productResultsList.isEmpty()) {
                    R.drawable.warning_icon
                } else {
                    R.drawable.icon_add
                }

                HandleTopInfo(
                    startIcon,
                    topInfoInstruction,
                    showInfo
                )
            }
        }

        if (productResultsList.isNotEmpty()) {
            val coroutineScope = rememberCoroutineScope()

            DrawEnrollProductsIcon(
                isProductEnrollmentProgressBarVisible = isProductEnrollmentProgressBarVisible,
                isProductEnrollmentProgressBarVisibleOnChange = {
                    isProductEnrollmentProgressBarVisible = it
                },
                uiState = uiState,
                viewModel = viewModel,
                coroutineScope = coroutineScope,
                productResults = productResultsList,
                navController = navController,
                activityInnerPadding = activityInnerPadding
            )
        }
    }
}

@Composable
private fun DrawRetailShelfLabelsAndRowsUsingBox(
    uiState: AIDataCaptureDemoUiState,
    scaler: Float,
    gapX: Float,
    gapY: Float,
    displayMetricsDensity: Float,
) {
    uiState.bboxes.filter { it?.cls != 1 }.forEach { bBox ->
        bBox?.let {

            val bBoxTop = bBox.ymin
            val bBoxLeft = bBox.xmin
            val bBoxBottom = bBox.ymax
            val bBoxRight = bBox.xmax

            val scaledBBoxLeftInDp = (((scaler * bBoxLeft) + gapX) / displayMetricsDensity).dp
            val scaledBBoxTopInDp = (((scaler * bBoxTop) + gapY) / displayMetricsDensity).dp
            val scaledBBoxRightInDp = (((scaler * bBoxRight) + gapX) / displayMetricsDensity).dp
            val scaledBBoxBottomInDp = (((scaler * bBoxBottom) + gapY) / displayMetricsDensity).dp

            val rectangleWidth = scaledBBoxRightInDp - scaledBBoxLeftInDp
            val rectangleHeight = scaledBBoxBottomInDp - scaledBBoxTopInDp
//                val topLeftOffset = Offset(scaledBBoxLeftInDp, scaledBBoxTopInDp)

            when (bBox.cls) {
                2, 3 -> {   //Shelf Labels, Peg Labels
                    Box(
                        modifier = Modifier
                            .padding(
                                start = scaledBBoxLeftInDp,
                                top = scaledBBoxTopInDp
                            )
                            .border(
                                BorderStroke(width = 1.dp, color = Color.Blue)
                            )
                            .width(width = rectangleWidth)
                            .height(height = rectangleHeight)
                    )
                }

                4 -> {  //Shelf Row
                    Box(
                        modifier = Modifier
                            .padding(
                                start = scaledBBoxLeftInDp,
                                top = scaledBBoxTopInDp
                            )
                            .border(
                                BorderStroke(width = 1.dp, color = Color.Red)
                            )
                            .width(width = rectangleWidth)
                            .height(height = rectangleHeight)
                    )
                }

                else -> {   // unknown
                    Box(
                        modifier = Modifier
                            .padding(
                                start = scaledBBoxLeftInDp,
                                top = scaledBBoxTopInDp
                            )
                            .border(
                                BorderStroke(width = 1.dp, color = Color.Magenta)
                            )
                            .width(width = rectangleWidth)
                            .height(height = rectangleHeight)
                    )
                }
            }
        }
    }
}

@Composable
fun DrawRetailShelfProductsUsingBox(
    productResultsList: MutableList<ProductData>,
    transform: OverlayTransform,
    zoomScale: Float = 1f,
    zoomConfig: ZoomConfig = ZoomConfig(),
    skuOverrides: Map<ProductData, String> = emptyMap()
) {
    val scaler = transform.scaler
    val gapX = transform.gapX
    val gapY = transform.gapY
    val displayMetricsDensity = transform.displayMetricsDensity

    productResultsList.forEach { productData ->
        val bBoxTop = productData.bBox.ymin
        val bBoxLeft = productData.bBox.xmin
        val bBoxBottom = productData.bBox.ymax
        val bBoxRight = productData.bBox.xmax

        val scaledBBoxLeftInDp = (((scaler * bBoxLeft) + gapX) / displayMetricsDensity).dp
        val scaledBBoxTopInDp = (((scaler * bBoxTop) + gapY) / displayMetricsDensity).dp
        val scaledBBoxRightInDp = (((scaler * bBoxRight) + gapX) / displayMetricsDensity).dp
        val scaledBBoxBottomInDp = (((scaler * bBoxBottom) + gapY) / displayMetricsDensity).dp

        val rectangleWidth = scaledBBoxRightInDp - scaledBBoxLeftInDp
        val rectangleHeight = scaledBBoxBottomInDp - scaledBBoxTopInDp
        val displayText = skuOverrides[productData] ?: productData.text

        if (displayText.isNotEmpty()) { // Product is Recognized with Higher confidence
            Box(
                modifier = Modifier
                    .padding(
                        start = scaledBBoxLeftInDp,
                        top = scaledBBoxTopInDp
                    )
                    .border(
                        BorderStroke(width = 1.dp, color = Color.Green)
                    )
                    .background(color = Color(0xAA004830).copy(alpha = 0.5F))
                    .width(width = rectangleWidth)
                    .height(height = rectangleHeight)
            ) {
                AutoSizedCenteredProductLabel(
                    text = displayText,
                    boxWidth = rectangleWidth,
                    boxHeight = rectangleHeight,
                    zoomScale = zoomScale,
                    zoomConfig = zoomConfig
                )
            }
        } else { // Product not Recognized
            Box(
                modifier = Modifier
                    .padding(
                        start = scaledBBoxLeftInDp,
                        top = scaledBBoxTopInDp
                    )
                    .border(
                        BorderStroke(width = 1.dp, color = Color.Green)
                    )
                    .width(width = rectangleWidth)
                    .height(height = rectangleHeight)
            )
        }
    }
}

private fun findTappedProduct(
    tapOffset: Offset,
    containerWidth: Float,
    containerHeight: Float,
    zoomScale: Float,
    translationX: Float,
    translationY: Float,
    productResultsList: MutableList<ProductData>,
    transform: OverlayTransform
): ProductData? {
    val normalizedTapOffset = normalizeTapOffsetForCurrentTransform(
        tapOffset = tapOffset,
        containerWidth = containerWidth,
        containerHeight = containerHeight,
        zoomScale = zoomScale,
        translationX = translationX,
        translationY = translationY
    )
    val hitPadding = 12f / zoomScale.coerceAtLeast(1f)

    return productResultsList
        .filter { productData ->
        val left = (transform.scaler * productData.bBox.xmin) + transform.gapX
        val top = (transform.scaler * productData.bBox.ymin) + transform.gapY
        val right = (transform.scaler * productData.bBox.xmax) + transform.gapX
        val bottom = (transform.scaler * productData.bBox.ymax) + transform.gapY

            normalizedTapOffset.x in (left - hitPadding)..(right + hitPadding) &&
                normalizedTapOffset.y in (top - hitPadding)..(bottom + hitPadding)
        }
        .minByOrNull { productData ->
            (productData.bBox.xmax - productData.bBox.xmin) * (productData.bBox.ymax - productData.bBox.ymin)
        }
}

private fun normalizeTapOffsetForCurrentTransform(
    tapOffset: Offset,
    containerWidth: Float,
    containerHeight: Float,
    zoomScale: Float,
    translationX: Float,
    translationY: Float
): Offset {
    if (zoomScale == 1f && translationX == 0f && translationY == 0f) {
        return tapOffset
    }

    val centerX = containerWidth / 2f
    val centerY = containerHeight / 2f

    return Offset(
        x = centerX + ((tapOffset.x - translationX - centerX) / zoomScale),
        y = centerY + ((tapOffset.y - translationY - centerY) / zoomScale)
    )
}

@Composable
internal fun AutoSizedCenteredProductLabel(
    text: String,
    boxWidth: Dp,
    boxHeight: Dp,
    zoomScale: Float,
    zoomConfig: ZoomConfig
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()

        val availableWidthPx = with(density) { (boxWidth - 8.dp).roundToPx() }.coerceAtLeast(1)
        val availableHeightPx = with(density) { (boxHeight - 4.dp).roundToPx() }.coerceAtLeast(1)
        val availableWidthSp = with(density) { availableWidthPx.toSp().value }
        val availableHeightSp = with(density) { availableHeightPx.toSp().value }

        val zoomRange = (zoomConfig.maxZoom - zoomConfig.minZoom).takeIf { it > 0f } ?: 1f
        val zoomFraction = ((zoomScale - zoomConfig.minZoom) / zoomRange).coerceIn(0f, 1f)
        val maxTextScale = maxOf(zoomConfig.productTextMaxScale, zoomConfig.productTextMinScale)
        val minTextScale = minOf(zoomConfig.productTextMaxScale, zoomConfig.productTextMinScale)
        val scaleFactor = maxTextScale + (minTextScale - maxTextScale) * zoomFraction

        val textLength = text.length.coerceAtLeast(1)
        val estimatedWidthFitSp = availableWidthSp / (textLength * 0.58f)
        val estimatedHeightFitSp = availableHeightSp * 0.6f
        val absoluteMinTextSp = zoomConfig.productTextAbsoluteMinSp.coerceAtLeast(2f)
        val zoomAdjustedMinSp = (absoluteMinTextSp / zoomScale.coerceAtLeast(1f)).coerceAtLeast(2f)

        val baseFitSp = minOf(estimatedWidthFitSp, estimatedHeightFitSp).coerceAtLeast(zoomAdjustedMinSp)

        val maxCandidateSp = (baseFitSp * maxTextScale).coerceAtLeast(zoomAdjustedMinSp)
        Log.e("Monil","maxCandidateSp :: ${maxCandidateSp == zoomAdjustedMinSp}")

        val minCandidateSp = maxOf(
            (baseFitSp * minTextScale).coerceAtLeast(2f),
            zoomAdjustedMinSp
        )

        val fitResult = remember(
            text,
            availableWidthPx,
            availableHeightPx,
            scaleFactor,
            maxCandidateSp,
            minCandidateSp,
            textMeasurer
        ) {
            calculateFittedFontSizeSp(
                text = text,
                textMeasurer = textMeasurer,
                availableWidthPx = availableWidthPx,
                availableHeightPx = availableHeightPx,
                desiredFontSizeSp = (baseFitSp * scaleFactor).coerceIn(minCandidateSp, maxCandidateSp),
                minFontSizeSp = minCandidateSp
            )
        }

        Text(
            text = text,
            color = Color.White,
            fontSize = fitResult.fontSizeSp.sp,
            lineHeight = (fitResult.fontSizeSp * 1.1f).sp,   // tight line spacing
            textAlign = TextAlign.Center,
            maxLines = fitResult.maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = if (fitResult.maxLines > 1) {
                // Constrain width so text actually wraps to multiple lines
                Modifier
                    .padding(horizontal = 1.dp)
                    .width(boxWidth - 6.dp)
            } else {
                // Single line: allow unbounded so small text still centers nicely
                Modifier
                    .padding(horizontal = 2.dp)
                    .wrapContentSize(unbounded = true)
            }
        )
    }
}


/**
 * Finds the best font size and line count for text inside a product box.
 *
 * Strategy:
 * 1. Try single-line first at the desired font size and shrink until it fits.
 * 2. If single-line doesn't fit at [minFontSizeSp], try multi-line:
 *    calculate how many lines fit at [minFontSizeSp] and measure with wrapping.
 * 3. If the text still overflows, return [minFontSizeSp] with the computed max
 *    lines — the Text composable will add an ellipsis automatically.
 */
private fun calculateFittedFontSizeSp(
    text: String,
    textMeasurer: TextMeasurer,
    availableWidthPx: Int,
    availableHeightPx: Int,
    desiredFontSizeSp: Float,
    minFontSizeSp: Float
): FitResult {
    var candidateSp = desiredFontSizeSp.coerceAtLeast(minFontSizeSp)

    while (candidateSp >= minFontSizeSp) {
        val layout = textMeasurer.measure(
            text = text,
            style = TextStyle(fontSize = candidateSp.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            constraints = Constraints(maxWidth = availableWidthPx)
        )
        if (layout.size.width <= availableWidthPx && layout.size.height <= availableHeightPx
            && !layout.hasVisualOverflow) {
            return FitResult(candidateSp, 1)
        }
        candidateSp -= 0.5f
    }

    candidateSp = desiredFontSizeSp.coerceAtLeast(minFontSizeSp)

    while (candidateSp >= minFontSizeSp) {
        val tightLineHeight = (candidateSp * 1.1f).sp
        val style = TextStyle(fontSize = candidateSp.sp, lineHeight = tightLineHeight)

        val singleLineLayout = textMeasurer.measure(text = "A", style = style, maxLines = 1)
        val lineHeightPx = singleLineLayout.size.height
        if (lineHeightPx <= 0) { candidateSp -= 0.5f; continue }

        val maxLinesByHeight = (availableHeightPx / lineHeightPx).coerceAtLeast(1)
        if (maxLinesByHeight <= 1) { candidateSp -= 0.5f; continue }

        val multiLayout = textMeasurer.measure(
            text = text,
            style = style,
            maxLines = maxLinesByHeight,
            overflow = TextOverflow.Ellipsis,
            constraints = Constraints(maxWidth = availableWidthPx)
        )
        if (multiLayout.size.height <= availableHeightPx && !multiLayout.hasVisualOverflow) {
            return FitResult(candidateSp, maxLinesByHeight)
        }
        candidateSp -= 0.5f
    }

    candidateSp = minFontSizeSp
    val fallbackLineHeight = (candidateSp * 1.1f).sp
    val fallbackStyle = TextStyle(fontSize = candidateSp.sp, lineHeight = fallbackLineHeight)
    val fallbackLineLayout = textMeasurer.measure(text = "A", style = fallbackStyle, maxLines = 1)
    val fallbackLineHeightPx = fallbackLineLayout.size.height
    val fallbackMaxLines = if (fallbackLineHeightPx > 0) (availableHeightPx / fallbackLineHeightPx).coerceAtLeast(1) else 1

    return FitResult(candidateSp, fallbackMaxLines)
}

@Composable
fun DrawEnrollProductsIcon(
    isProductEnrollmentProgressBarVisible: Boolean,
    isProductEnrollmentProgressBarVisibleOnChange: (Boolean) -> Unit,
    uiState: AIDataCaptureDemoUiState,
    coroutineScope: CoroutineScope,
    productResults: MutableList<ProductData>,
    navController: NavController,
    viewModel: AIDataCaptureDemoViewModel,
    activityInnerPadding: PaddingValues
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(bottom = activityInnerPadding.calculateBottomPadding())
                .background(Color.Black.copy(alpha = 0.4f)),
        ) {
            Row(
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 12.dp
                ),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            borderPrimaryMain,
                            shape = RoundedCornerShape(size = 4.dp)
                        )
                        .padding(
                            start = Variables.spacingLarge,
                            top = Variables.spacingMedium,
                            end = Variables.spacingLarge,
                            bottom = Variables.spacingMedium
                        )
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clickable {
                            val hasSkuAvlForEnrollment = viewModel.uiState.value.productResults.filter { it.text.isNotEmpty() }
                            if (hasSkuAvlForEnrollment.isNotEmpty()) {
                                isProductEnrollmentProgressBarVisibleOnChange(true)
                                viewModel.enrollProductIndex()
                            } else {
                                viewModel.toast("No products with SKU available for enrollment")
                            }
                        }
                ) {
                    Text(
                        text = stringResource(R.string.save_active_database),
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            fontFamily = FontFamily(Font(R.font.ibm_plex_sans_regular)),
                            fontWeight = FontWeight(500),
                            color = Variables.stateDefaultEnabled,
                        )
                    )
                }
            }
        }
    }

    if (isProductEnrollmentProgressBarVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Variables.borderDefault, trackColor = mainPrimary)
        }
    }

    if (uiState.isProductEnrollmentCompleted) {
        viewModel.handleBackButton(navController)
        isProductEnrollmentProgressBarVisibleOnChange(false)
    }
}

/**
 * ProductAlertDialog composable function to display the dialog box when the user taps on the product bounding box
 * The dialog box displays cropped product image displayed and an edit box wherein the user can
 * input SKU manually, or scan a barcode by pressing yellow scan button that then invokes
 * Datawedge Profile 0 (in enabled) to scan the barcode.
 * User can then press confirm button to associate the SKU to the product image and bounding box.
 */
@Composable
fun ProductAlertDialog(
    productShowDialog: MutableState<Boolean>,
    productImage: ImageBitmap,
    productSKU: MutableState<String>,
    productSKUChanged: MutableState<Boolean>,
    isSoftScanBarcodeButtonEnabled: Boolean,
    onScanBarcodeClick: () -> Unit,
    onStopSoftScan: () -> Unit
) {
    if (productShowDialog.value) {
        val savedOriginalSKU = remember(productShowDialog.value) { productSKU.value }
        val focusRequester = remember { FocusRequester() }
        AlertDialog(
            onDismissRequest = {
                onStopSoftScan()
                productSKUChanged.value = false
                productSKU.value = savedOriginalSKU
                productShowDialog.value = false
            },
            title = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, top = 20.dp, end = 20.dp)
                ) {
                    Text(
                        text = stringResource(R.string.enterproductsku),
                        style = TextStyle(
                            fontSize = 20.sp,
                            lineHeight = 28.sp,
                            fontFamily = FontFamily(Font(R.font.ibm_plex_sans_medium)),
                            fontWeight = FontWeight(500),
                            color = Variables.mainDefault,
                        )
                    )
                }
            },
            containerColor = Variables.surfaceDefault,
            text = {
                Column(Modifier.fillMaxWidth()) {
                    Image(
                        painter = BitmapPainter(
                            productImage,
                            IntOffset(0, 0),
                            IntSize(productImage.width, productImage.height)
                        ),
                        contentDescription = "Product Crop",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .size(200.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                    TextField(
                        value = productSKU.value,
                        placeholder = { Text(stringResource(R.string.enterproducthint)) },
                        onValueChange = { productSKU.value = it },
                        maxLines = 1,
                        textStyle = TextStyle(
                            fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                            fontWeight = FontWeight(500),
                            color = Variables.mainDefault
                        ),
                        modifier = Modifier
                            .background(
                                color = Color(0xFFFFFFFF),
                                shape = RoundedCornerShape(size = 3.6.dp)
                            )
                            .fillMaxWidth()
                            .padding(start = 14.4.dp, top = 8.dp, end = 14.4.dp, bottom = 8.dp)
                            .focusRequester(focusRequester),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Variables.mainInverse,
                            unfocusedContainerColor = Variables.mainInverse,
                            cursorColor = Variables.mainPrimary,
                            focusedIndicatorColor = Variables.mainPrimary,
                            unfocusedIndicatorColor = Variables.mainPrimary,
                            selectionColors = TextSelectionColors(
                                handleColor = mainPrimary,
                                backgroundColor = mainPrimary
                            )
                        )
                    )

                    if (isSoftScanBarcodeButtonEnabled) {
                        Button(
                            onClick = { onScanBarcodeClick() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.4.dp, top = 8.dp, end = 14.4.dp, bottom = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Variables.mainPrimary
                            ),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_menu_barcode),
                                contentDescription = "Scan Barcode icon",
                                tint = Variables.whiteIcon
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.scan_barcode),
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    lineHeight = 24.sp,
                                    fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                                    fontWeight = FontWeight(500),
                                    color = Variables.stateDefaultEnabled,
                                    textAlign = TextAlign.Center,
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onStopSoftScan()
                        productSKUChanged.value = true
                        productShowDialog.value = false
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .width(121.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Variables.mainPrimary
                    ),
                    shape = RoundedCornerShape(4.dp)
                )
                {
                    Text(
                        text = stringResource(R.string.apply),
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                            fontWeight = FontWeight(500),
                            color = Variables.stateDefaultEnabled,
                            textAlign = TextAlign.Center,
                        )
                    )
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        onStopSoftScan()
                        productSKUChanged.value = false
                        productShowDialog.value = false
                        productSKU.value = savedOriginalSKU
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .width(121.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                            fontWeight = FontWeight(500),
                            color = Variables.mainPrimary,
                            textAlign = TextAlign.Center,
                        )
                    )
                }
            },
        )
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}
