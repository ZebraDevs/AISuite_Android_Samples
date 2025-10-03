// Copyright (c) 2024-2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.aidatacapturedemo.ui.view

import android.graphics.Bitmap
import android.graphics.Point
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.zebra.ai.vision.detector.BBox
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.data.ProductData
import com.zebra.aidatacapturedemo.model.FileUtils
import com.zebra.aidatacapturedemo.ui.view.Variables.borderPrimaryMain
import com.zebra.aidatacapturedemo.ui.view.Variables.cameraPreviewViewSize
import com.zebra.aidatacapturedemo.ui.view.Variables.mainPrimary
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel
import kotlinx.coroutines.CoroutineScope

private const val TAG = "CameraCapturedImageScreen"

data class ScaledProductData(
    val point: Point,
    var text: String,
    val bbox: BBox,
    val scaledRect: Rect,
    var crop: Bitmap,
    var edited: Boolean
)

val productDataList = ArrayList<ScaledProductData>()

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
    innerPadding: PaddingValues,
    activityInnerPadding: PaddingValues
) {

    val uiState = viewModel.uiState.collectAsState().value
    val productResults = uiState.productResults
    var showInfo = remember { mutableStateOf(true) }

    BackHandler(enabled = true) {
        viewModel.handleBackButton(navController)
    }

    if (uiState.captureBitmap == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
        }
    } else {
        var isProductEnrollmentProgressBarVisible by remember { mutableStateOf(false) }

        val density = LocalDensity.current
        val widthInDp: Dp = with(density) { cameraPreviewViewSize.height.toDp() }
        val heightInDp: Dp = with(density) { cameraPreviewViewSize.width.toDp() }
        val capturedBitmap = uiState.captureBitmap!!
        Image(
            painter = rememberAsyncImagePainter(capturedBitmap),
            contentDescription = "Image",
            modifier = Modifier
                .size(widthInDp, heightInDp)
                .offset(0.dp, innerPadding.calculateTopPadding().value.dp),
            contentScale = ContentScale.FillBounds
        )

        val coroutineScope = rememberCoroutineScope()
        val resources = LocalContext.current.resources
        val screenDensity = resources.displayMetrics.density
        val screenWidth = resources.displayMetrics.widthPixels
        val scaleFactor = screenWidth / uiState.captureBitmap!!.width.toFloat()
        val adjustedInnerPaddingHeight =
            innerPadding.calculateTopPadding().value * screenDensity // Dp value will be converted to px and added for top padding to draw bBoxes

        createProductDataList(
            uiState.captureBitmap!!,
            productResults,
            uiState.bboxes as Array<BBox>,
            scaleFactor,
            adjustedInnerPaddingHeight
        )

        DrawCaptureImageCanvas(
            productResults,
            uiState.bboxes as Array<BBox>,
            scaleFactor,
            adjustedInnerPaddingHeight
        )
        if (showInfo.value) {
            val topInfoInstruction = if (isProductEnrollmentProgressBarVisible) {
                stringResource(R.string.instruction_enrolling_into_db)
            } else {
                stringResource(R.string.instruction_2)
            }

            HandleTopInfo(
                activityInnerPadding = innerPadding,
                R.drawable.icon_add,
                topInfoInstruction,
                showInfo
            )
        }

        // Calculate the Camera PreviewView Bottom and set the Bottom Bar padding accordingly at the Bottom align
        val camPreviewViewBottomInDp = heightInDp + innerPadding.calculateTopPadding().value.dp
        val screenHeightInDp = with(density) {
            resources.displayMetrics.heightPixels.toDp() // px to dp
        }
        val calculatedBottomBarBottomPadding =
            (screenHeightInDp - camPreviewViewBottomInDp) + innerPadding.calculateBottomPadding()

        DrawEnrollProductsIcon(
            isProductEnrollmentProgressBarVisible = isProductEnrollmentProgressBarVisible,
            isProductEnrollmentProgressBarVisibleOnChange = { isProductEnrollmentProgressBarVisible = it},
            uiState = uiState,
            viewModel = viewModel,
            coroutineScope = coroutineScope,
            productResults = productResults,
            navController = navController,
            calculatedBottomBarBottomPadding = calculatedBottomBarBottomPadding,
            activityInnerPadding = activityInnerPadding
        )
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
    calculatedBottomBarBottomPadding: Dp,
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
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
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
    productResults.clear()
    for (productData in productDataList) {
        productResults.add(
            ProductData(
                productData.point,
                productData.text,
                productData.bbox,
                productData.crop
            )
        )
        if (productData.text.isNotEmpty())
            FileUtils.saveBitmap(
                productData.crop,
                timestampedFolder + "/" + productData.text,
                "productcrop"
            )
    }
    viewModel.toast("Saved product crops in ${timestampedFolder} ")
}

/**
 * Create the product data list from the product results and bounding boxes
 */
fun createProductDataList(
    inputImage: Bitmap,
    productResults: List<ProductData>,
    bboxes: Array<BBox>,
    scaleFactor: Float,
    adjustedInnerPaddingHeight: Float
) {
    // Sort so that labels are not obscured by shelf's
    productDataList.clear()
    bboxes.sortByDescending { it.cls }
    var productIndex: Int = 0
    for (box in bboxes) {
        // Calculate the corners of the rectangle
        val topLeft =
            Offset(box.xmin * scaleFactor, (box.ymin * scaleFactor) + adjustedInnerPaddingHeight)
        val bottomRight =
            Offset(box.xmax * scaleFactor, (box.ymax * scaleFactor) + adjustedInnerPaddingHeight)

        if (box.cls == 1) {
            // When there are no product indicies
            if (productResults.size != 0) {
                // Save product boundingbox Rects to check for taps
                if (productIndex < productResults.size) {
                    productDataList.add(
                        ScaledProductData(
                            productResults[productIndex].point,
                            productResults[productIndex].text,
                            productResults[productIndex].bBox,
                            Rect(topLeft, bottomRight),
                            productResults[productIndex].crop,
                            false
                        )
                    )
                }
            } else {
                if ((box.xmin.toInt() + (box.xmax - box.xmin).toInt() < inputImage.width) &&
                    (box.ymin.toInt() + (box.ymax - box.ymin).toInt() < inputImage.height)
                ) {
                    productDataList.add(
                        ScaledProductData(
                            Point(box.xmin.toInt(), box.ymin.toInt()),
                            "",
                            box,
                            Rect(topLeft, bottomRight),
                            Bitmap.createBitmap(
                                inputImage,
                                box.xmin.toInt(),
                                box.ymin.toInt(),
                                (box.xmax - box.xmin).toInt(),
                                (box.ymax - box.ymin).toInt()
                            ),
                            false
                        )
                    )
                } else {
                    Log.i(TAG, "Product BBox out of bounds")
                }
            }
            productIndex++
        }
    }
    Log.i(TAG, "productDataList.size = " + productDataList.size)
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

/**
 * DrawCaptureImageCanvas composable function to draw the bounding boxes and product SKU's on the captured image
 * The user can tap on the product bounding boxes that brings up a dialog box.
 */
@Composable
fun DrawCaptureImageCanvas(
    productResults: List<ProductData>,
    bboxes: Array<BBox>,
    scaleFactor: Float,
    adjustedInnerPaddingHeight: Float
) {
    val productShowDialog = remember { mutableStateOf(false) }
    val productSKUChanged = remember { mutableStateOf(false) }
    val productIndex = remember { mutableStateOf(0) }
    var productSKU = rememberSaveable { mutableStateOf("Text") }

    if (productShowDialog.value) {
        ProductAlertDialog(
            productShowDialog = productShowDialog,
            productImage = productDataList[productIndex.value].crop.asImageBitmap(),
            productSKU = productSKU,
            productSKUChanged = productSKUChanged
        )
    }
    if (productSKUChanged.value) {
        productDataList[productIndex.value].edited = true
        productDataList[productIndex.value].text = productSKU.value

        // TODO: The inclusion of this dummy fun is costing sometime, which is required for the following canvas call to trigger it's respective drawing,
        //  else the Canvas draw call are not recomposed
        drawProductSKUImageCanvasTemp(rectColor = Color(0xFF004830))

        productSKUChanged.value = false
    }

    val textMeasurer = rememberTextMeasurer()
    Canvas(
        modifier = Modifier
            .padding(0.dp)
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onPress = { tapOffset ->
                    // When the user taps on the Canvas, check if the tap offset is one of the tracked Rects.
                    var index = 0
                    for (product in productDataList) {
                        if (product.scaledRect.contains(tapOffset)) {
                            Log.i(TAG, "DrawCaptureImageCanvas index = " + index)
                            productSKU.value = product.text
                            productIndex.value = index
                            productShowDialog.value = true
                            break // Don't need to check other points, we found the one we needed
                        }
                        index++
                    }
                })
            }) {
        drawLabelShelfBBoxOnImageCanvas(bboxes, scaleFactor, adjustedInnerPaddingHeight)
        drawProductBBoxOnImageCanvas()
        drawProductSKUImageCanvas(textMeasurer)
    }
}

/**
 * Draw the label and shelf bounding boxes on the captured image
 */
fun DrawScope.drawLabelShelfBBoxOnImageCanvas(
    bboxes: Array<BBox>,
    scaleFactor: Float,
    adjustedInnerPaddingHeight: Float
) {
    // Sort so that labels are not obscured by shelf's
    bboxes.sortByDescending { it.cls }
    var productIndex: Int = 0
    for (box in bboxes) {

        // Calculate the corners of the rectangle
        val topLeft =
            Offset(box.xmin * scaleFactor, (box.ymin * scaleFactor) + adjustedInnerPaddingHeight)
        val topRight =
            Offset(box.xmax * scaleFactor, (box.ymin * scaleFactor) + adjustedInnerPaddingHeight)
        val bottomLeft =
            Offset(box.xmin * scaleFactor, (box.ymax * scaleFactor) + adjustedInnerPaddingHeight)
        val bottomRight =
            Offset(box.xmax * scaleFactor, (box.ymax * scaleFactor) + adjustedInnerPaddingHeight)

        val boxColor = when (box.cls) {
            1 -> Color.Green // Products
            2 -> Color.Blue // Shelf Labels
            3 -> Color.Blue // Peg Labels
            4 -> Color.Red // Shelf Row
            else -> {
                Color.Magenta   // unknown
            }
        }
        if (box.cls != 1) {
            // Draw the box
            drawRect(
                color = boxColor,
                size = Size(width = topRight.x - topLeft.x, height = bottomLeft.y - topLeft.y),
                topLeft = topLeft,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}


/**
 * Draw the product bounding boxes and SKU's on the captured image
 */
fun DrawScope.drawProductBBoxOnImageCanvas() {
    var index = 0
    for (productData in productDataList) {

        var boxColor = Color.Green
        // Draw the box
        if (productDataList[index].edited == true) {
            drawRect(
                color = boxColor,
                size = Size(
                    width = productData.scaledRect.right - productData.scaledRect.left,
                    height = productData.scaledRect.bottom - productData.scaledRect.top
                ),
                topLeft = Offset(productData.scaledRect.left, productData.scaledRect.top)
            )
        } else {
            drawRect(
                color = boxColor,
                size = Size(
                    width = productData.scaledRect.right - productData.scaledRect.left,
                    height = productData.scaledRect.bottom - productData.scaledRect.top
                ),
                topLeft = Offset(productData.scaledRect.left, productData.scaledRect.top),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
        index++
    }
}

/**
 * Draw the product SKU's on the captured image
 */
fun DrawScope.drawProductSKUImageCanvas(textMeasurer: TextMeasurer) {
    val backgroundColor = Color(0x66000000)
    for (productData in productDataList) {
        if (productData.text.isNotEmpty()) {
            drawRect(
                color = Color(0xAA004830),
                size = Size(
                    width = productData.scaledRect.right - productData.scaledRect.left,
                    height = productData.scaledRect.bottom - productData.scaledRect.top
                ),
                topLeft = Offset(productData.scaledRect.left, productData.scaledRect.top)
            )

            drawText(
                textMeasurer = textMeasurer,
                text = productData.text,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 10.sp,
                    background = backgroundColor
                ),
                softWrap = true,
                topLeft = Offset(
                    productData.scaledRect.left,
                    productData.scaledRect.top + (productData.scaledRect.bottom - productData.scaledRect.top) / 2
                ),
                overflow = TextOverflow.Clip,
                size = Size(
                    width = productData.scaledRect.right - productData.scaledRect.left,
                    height = productData.scaledRect.bottom - productData.scaledRect.top
                )
            )
        }
    }
}

@Composable
fun drawProductSKUImageCanvasTemp(rectColor: Color) {
    val backgroundColor = Color(0x66000000)
    for (productData in productDataList) {
        if (productData.text.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val topLeftOffset = Offset(productData.scaledRect.left, productData.scaledRect.top)
                val bottomRightOffset =
                    Offset(productData.scaledRect.right, productData.scaledRect.bottom)
                // Draw the filled rectangle
                drawRect(
                    color = rectColor,
                    topLeft = topLeftOffset,
                    size = Size(
                        width = productData.scaledRect.right - productData.scaledRect.left,
                        height = productData.scaledRect.bottom - productData.scaledRect.top
                    )
                )

                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 30f

                }

                drawContext.canvas.nativeCanvas.drawText(
                    productData.text,
                    topLeftOffset.x,
                    topLeftOffset.y + ((bottomRightOffset.y - topLeftOffset.y) / 2),
                    paint
                )
            }
        }
    }
}
