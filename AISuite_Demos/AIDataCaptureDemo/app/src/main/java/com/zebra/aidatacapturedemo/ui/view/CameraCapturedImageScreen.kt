// Copyright (c) 2024-2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.aidatacapturedemo.ui.view

import android.content.Context
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.getSystemService
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.data.ProductData
import com.zebra.aidatacapturedemo.model.FileUtils
import com.zebra.aidatacapturedemo.ui.view.Variables.borderPrimaryMain
import com.zebra.aidatacapturedemo.ui.view.Variables.mainPrimary
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel
import kotlinx.coroutines.CoroutineScope
import kotlin.math.min

private const val TAG = "CameraCapturedImageScreen"

//describe the composable function
/**
 * CameraCapturedImageScreen composable function to display the captured high resolution
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
fun CameraCapturedImageScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavController,
    activityInnerPadding: PaddingValues,
    context: Context
) {
    val uiState = viewModel.uiState.collectAsState().value
    val productResultsList = uiState.productResults

    BackHandler(enabled = true) {
        viewModel.handleBackButton(navController)
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
                scaler = scaler,
                gapX = gapX,
                gapY = gapY,
                displayMetricsDensity = displayMetricsDensity
            )

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
private fun DrawRetailShelfProductsUsingBox(
    productResultsList: MutableList<ProductData>,
    scaler: Float,
    gapX: Float,
    gapY: Float,
    displayMetricsDensity: Float
) {
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

        val productShowDialog = remember { mutableStateOf(false) }
        val productSKUChanged = remember { mutableStateOf(false) }
        var productSKU = rememberSaveable { mutableStateOf(productData.text) }

        if (productShowDialog.value) {
            ProductAlertDialog(
                productShowDialog = productShowDialog,
                productImage = productData.crop.asImageBitmap(),
                productSKU = productSKU,
                productSKUChanged = productSKUChanged
            )
        }

        // After effect of ProductAlertDialog opened and Closed
        if (productSKUChanged.value) {
            productData.text = productSKU.value
            productSKUChanged.value = false
        }

        if (productData.text != null && productData.text != "") { // Product is Recognized with Higher confidence
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
                    .clickable {
                        productShowDialog.value = true
                    }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = productData.text,
                        color = Color.White,
                        fontSize = (30f / displayMetricsDensity).sp
                    )
                }
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
                    .clickable {
                        productShowDialog.value = true
                    }
            )
        }
    }
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
                            saveProductDataList(viewModel, productResults)
                            viewModel.enrollProductIndex()
                            isProductEnrollmentProgressBarVisibleOnChange(true)
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
 * Save the product data (SKU and Image) list to the database and save the product image crops to the Pictures folder
 */
fun saveProductDataList(
    viewModel: AIDataCaptureDemoViewModel,
    productResults: MutableList<ProductData>
) {
    val timestampedFolder = FileUtils.getTimeStampedFolderName()
    for (productData in productResults) {
        if (productData.text.isNotEmpty()) {
            FileUtils.saveBitmap(
                productData.crop,
                timestampedFolder + "/" + productData.text,
                "productcrop"
            )
        }
    }
    viewModel.toast("Saved product crops in ${timestampedFolder} ")
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
    productSKUChanged: MutableState<Boolean>
) {
    var savedOriginalSKU: String = productSKU.value
    if (productShowDialog.value) {
        val focusRequester = remember { FocusRequester() }
        AlertDialog(
            onDismissRequest = {
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
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