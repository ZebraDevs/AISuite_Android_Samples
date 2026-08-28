package com.zebra.ai.palletchecker.presentation.ui.compose.screens.results

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.google.gson.Gson
import com.zebra.ai.palletchecker.domain.model.BarcodeConfig
import com.zebra.ai.palletchecker.domain.model.FIELD_TYPE
import com.zebra.ai.palletchecker.helpers.LOGI
import com.zebra.ai.palletchecker.helpers.ScaleType
import com.zebra.ai.palletchecker.helpers.toPalletBoxUiModel
import com.zebra.ai.palletchecker.presentation.model.BOX_VALIDATION
import com.zebra.ai.palletchecker.presentation.model.PBoxUIModel
import com.zebra.ai.palletchecker.presentation.model.SAPPalletBox
import com.zebra.ai.palletchecker.presentation.model.SessionMode
import com.zebra.ai.palletchecker.presentation.model.convertMillisecondsToISO8601
import com.zebra.ai.palletchecker.presentation.ui.compose.components.ContentWithLabel
import com.zebra.ai.palletchecker.presentation.ui.compose.components.ZebraText
import com.zebra.ai.palletchecker.presentation.ui.compose.components.palletBoxCanvas
import com.zebra.ai.palletchecker.presentation.ui.compose.components.wandDebugBarcodeCanvas
import com.zebra.ai.palletchecker.presentation.ui.theme.AppDimensions
import com.zebra.ai.palletchecker.presentation.ui.theme.Black
import com.zebra.ai.palletchecker.presentation.ui.theme.ThemeDark
import com.zebra.ai.palletchecker.presentation.ui.theme.white
import com.zebra.ai.palletchecker.presentation.viewmodel.ConfigureViewModel
import com.zebra.ai.palletchecker.presentation.viewmodel.MainViewModel
import com.zebra.ai.palletchecker.presentation.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import org.json.JSONObject

@OptIn(ExperimentalGetImage::class)
@Composable
fun ResultsScreen(
    settingsViewModel: SettingsViewModel,
    configModel: ConfigureViewModel,
    mainViewModel: MainViewModel,
    onBackPress: () -> Unit = {},
    onAuditPreview: (List<PBoxUIModel>, Int, IntSize, IntSize, Boolean) -> Unit = { _, _, _, _, _ -> },
    isFromWand: Boolean = false
) {
    val activity = LocalActivity.current
    var settings = settingsViewModel.settings.collectAsState()
    val wandSettings = settingsViewModel.wandSettings.collectAsState()
    val config = configModel.config.collectAsState()
    var palletSession = mainViewModel.getPalletSession().collectAsState()
    val palletResults = mainViewModel.getPalletResults()?.collectAsState(emptyList())

    var isNewSession =
        remember { mutableStateOf(palletSession.value.storedPalletDetails.isEmpty()) }
    var isImageLoadError by remember { mutableStateOf(false) }

    var imageSize by remember { mutableStateOf(IntSize(0, 0)) }
    var palletBoxes = remember { mutableStateListOf<PBoxUIModel>() }

    val isShelfContinuous = palletSession.value.sessionMode == SessionMode.SHELF_CONTINUOUS
    val hasUnmappedBoxes = palletSession.value.hasUnmappedWandBoxes
    var showResult by remember { mutableStateOf(isFromWand && (isShelfContinuous || hasUnmappedBoxes)) }
    var showLiquidUI by remember { mutableStateOf(false) }
    var shouldMoveToAudit by remember { mutableStateOf(false) }

    var auditPreviewSnapshot by remember { mutableStateOf<List<PBoxUIModel>>(emptyList()) }
    var shouldEndSession by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if (!showResult) {
            onBackPress()
        } else if (isShelfContinuous) {
            onBackPress()
        } else {
            showResult = false
        }
    }

    LaunchedEffect(Unit) {
        isImageLoadError = false
        if (isNewSession.value) {
            mainViewModel.processCapturedImage()
        }
    }

    LaunchedEffect(shouldMoveToAudit) {
        if (shouldMoveToAudit && !isFromWand) { // Extended from 2s to 5s for debugging — allows cross-checking snap state vs PIP
            delay(2000L)
            val bitmap = palletSession.value.storedPalletCaptureImage
            val sourceResolution = if (bitmap != null) {
                IntSize(bitmap.width, bitmap.height)
            } else {
                IntSize(settings.value.effectiveHeight(), settings.value.effectiveWidth())
            }
            val targetViewSize = imageSize

            val boxesForAudit = auditPreviewSnapshot.ifEmpty { palletBoxes.toList() }
            val greenCount = boxesForAudit.count { it.validation == BOX_VALIDATION.VERIFIED }
            val yellowCount = boxesForAudit.count { it.validation == BOX_VALIDATION.PARTIAL_DETECTION }
            val redCount = boxesForAudit.count { it.validation == BOX_VALIDATION.MISMATCH_QTY }
            LOGI("SnapDiag", "onAuditPreview: passing ${boxesForAudit.size} boxes " +
                "(green=$greenCount yellow=$yellowCount red=$redCount) to wand session")

            onAuditPreview(
                boxesForAudit,
                config.value.expectedBoxes,
                sourceResolution,
                targetViewSize,
                wandSettings.value.livePipThumbnailEnabled
            )
            shouldMoveToAudit = false
        }
    }

    LaunchedEffect(palletResults?.value, imageSize) {

        if (imageSize.width == 0 || imageSize.height == 0) return@LaunchedEffect

        palletBoxes.clear()

        val cachedPallets = mainViewModel.getCachedPalletBoxes()

        val bitmap = palletSession.value.storedPalletCaptureImage
        val sourceSize = if (bitmap != null) {
            IntSize(bitmap.width, bitmap.height)
        } else {
            IntSize(settings.value.effectiveHeight(), settings.value.effectiveWidth())
        }

        palletResults?.let {
            val seenKeys = linkedMapOf<String, PBoxUIModel>()
            for (pBox in palletResults.value) {
                if (pBox.classId == 3) continue

                var pBoxUiModel =
                    pBox.toPalletBoxUiModel(
                        sourceSize, imageSize,
                        scaleType = ScaleType.FIT_CENTER,
                        appConfig = config.value.listOfConfig
                    )

                if (isFromWand) {
                    pBoxUiModel = pBoxUiModel.copy(validation = pBox.validation)
                } else {
                    val find = pBox.barcodeList.find { cachedPallets.contains(it.data) }
                    find?.let {
                        val validation = cachedPallets[find.data]?.validation
                        validation?.let {
                            pBoxUiModel = pBoxUiModel.copy(validation = validation)
                        }
                    }
                }

                val normalizedKey = pBoxUiModel.stableKey
                    .removePrefix("barcode:")
                    .ifEmpty { "id-${pBoxUiModel.id}" }
                seenKeys[normalizedKey] = pBoxUiModel
            }

            val maxBoxes = config.value.expectedBoxes
            val cappedValues = if (maxBoxes > 0) seenKeys.values.take(maxBoxes) else seenKeys.values.toList()
            palletBoxes.addAll(
                cappedValues.mapIndexed { index, box ->
                    box.copy(id = index + 1)
                }
            )

            if (!isFromWand) {
                val hasPartialBoxes = palletBoxes.any { it.validation == BOX_VALIDATION.PARTIAL_DETECTION }
                val snapBoxCount = palletBoxes.size
                val needsMoreBoxes = snapBoxCount < config.value.expectedBoxes
                if (hasPartialBoxes || needsMoreBoxes) {
                    auditPreviewSnapshot = palletBoxes.toList()
                    shouldMoveToAudit = true
                }
            }
            shouldEndSession = palletBoxes.isNotEmpty() && !shouldMoveToAudit
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            , contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    imageSize = coords.size
                },
            contentAlignment = Alignment.Center
        ) {

            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(palletSession.value.storedPalletCaptureImage)
                    .build(),
                contentDescription = "Loaded image",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                content = {
                    val state = painter.state
                    if (state is AsyncImagePainter.State.Loading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (state is AsyncImagePainter.State.Error) {
                        isImageLoadError = true
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ZebraText(
                                textValue = "The captured image encountered an issue. Kindly attempt to capture the image again.",
                                textColor = White
                            )
                        }
                    } else {
                        SubcomposeAsyncImageContent()
                    }
                }
            )
        }

        if (!isImageLoadError) {
            val displayBoxes = palletBoxes.toList()
                .mapIndexed { index, box -> box.copy(id = index + 1) }

            if (displayBoxes.isNotEmpty()) {
                palletBoxCanvas(
                    Modifier
                        .fillMaxSize(), displayBoxes)

                if (settings.value.debugSettings.debugModeEnabled &&
                    settings.value.debugSettings.showCapturedSnapBarcodeLabels
                ) {
                    wandDebugBarcodeCanvas(
                        modifier = Modifier.fillMaxSize(),
                        boxes = displayBoxes
                    )
                }
            }


            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .align(alignment = Alignment.TopCenter),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Top

            ) {

                Button(

                    onClick = {
                        showResult = !showResult
                    },
                    modifier = Modifier
                        .wrapContentWidth()
                        .defaultMinSize(minHeight = 36.dp)
                        .padding(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = Gray,
                        containerColor = ThemeDark
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Show Results", color = White, fontWeight = FontWeight.Bold)
                }

                if (showLiquidUI) {

                    Spacer(modifier = Modifier.width(10.dp))
                    Button(

                        onClick = {
                            setPalletResults(
                                mainViewModel,
                                activity!!,
                                palletBoxes=palletBoxes ?: emptyList(),
                                config = config.value.listOfConfig
                            )
                        },
                        modifier = Modifier
                            .wrapContentWidth()
                            .defaultMinSize(minHeight = 36.dp)
                            .padding(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = Gray,
                            containerColor = ThemeDark
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Send to SAP", color = White, fontWeight = FontWeight.Bold)
                    }
                }
            }

        } else {
            Button(
                onClick = {
                    onBackPress()
                },
                modifier = Modifier
                    .wrapContentWidth()
                    .defaultMinSize(minHeight = 36.dp)
                    .padding(10.dp)
                    .align(alignment = Alignment.BottomCenter),
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = Gray,
                    containerColor = ThemeDark
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Back", color = White, fontWeight = FontWeight.Bold)
            }
        }


    }

    if (showResult) {
        val resultBoxes = palletBoxes.toList()
            .mapIndexed { index, box -> box.copy(id = index + 1) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(white)
        ) {



            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showLiquidUI) {
                    item {


                        Spacer(modifier = Modifier.width(10.dp))
                        Button(

                            onClick = {
                                setPalletResults(
                                    mainViewModel,
                                    activity!!,
                                    palletBoxes = palletBoxes ?: emptyList(),
                                    config = config.value.listOfConfig
                                )
                            },
                            modifier = Modifier
                                .wrapContentWidth()
                                .defaultMinSize(minHeight = 36.dp)
                                .padding(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = Gray,
                                containerColor = ThemeDark
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Send to SAP", color = White, fontWeight = FontWeight.Bold)
                        }


                    }
                }
                item {
                    Spacer(modifier = Modifier.size(10.dp))
                }
                items(resultBoxes) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AppDimensions.menuOverlayCardCornerRadius),
                        colors = CardDefaults.cardColors(containerColor = white),
                        elevation = CardDefaults.cardElevation(defaultElevation = AppDimensions.largeElevation)
                    ) {
                        ContentWithLabel("Box ${item.id}", item.validation) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp)
                            ) {
                                val selectedConfigs =
                                    config.value.listOfConfig.filter { it.isSelected }
                                val isSkuEnabled =
                                    selectedConfigs.any { it.type == FIELD_TYPE.PRODUCT_SKU.name }
                                val isQtyEnabled =
                                    selectedConfigs.any { it.type == FIELD_TYPE.QTY.name }

                                val productSku = item.palletBarcodes
                                    .firstOrNull { it.isMainBarcode && it.data.isNotEmpty() }
                                val qtyBarcodes = item.palletBarcodes
                                    .filter { it.isQtyBarcode && it.data.isNotEmpty() }

                                if (isSkuEnabled) {
                                    if (productSku != null) {
                                        Text(
                                            text = buildAnnotatedString {
                                                withStyle(SpanStyle(fontWeight = FontWeight.Medium)) {
                                                    append("Product SKU: ")
                                                }
                                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                                    append(productSku.data)
                                                }
                                            },
                                            color = Black
                                        )
                                    } else {
                                        Text(
                                            text = "Product SKU: —",
                                            color = Black,
                                            fontWeight = FontWeight.Normal
                                        )
                                    }
                                }

                                if (isQtyEnabled) {
                                    if (qtyBarcodes.isNotEmpty()) {
                                        qtyBarcodes.forEachIndexed { index, qtyBarcode ->
                                            Text(
                                                text = buildAnnotatedString {
                                                    withStyle(SpanStyle(fontWeight = FontWeight.Medium)) {
                                                        append("Qty${if (qtyBarcodes.size > 1) " ${index + 1}" else ""}: ")
                                                    }
                                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                                        append(qtyBarcode.data)
                                                    }
                                                },
                                                color = Black
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "Qty: —",
                                            color = Black,
                                            fontWeight = FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}

private fun setPalletResults(
    mainViewModel: MainViewModel,
    activity: Activity,
    palletBoxes : List<PBoxUIModel>,
    config: List<BarcodeConfig>
) {
    val intent = Intent()
    if (palletBoxes.isNotEmpty()) {

        val sap = SAPPalletBox(quantity = mainViewModel.findTotalQtyForPallets(appConfig = config))
        val json = JSONObject().apply {
            put("material", sap.material)
            put("quantity", sap.quantity)
            put("plant", sap.plant)
            put("storage_location", sap.storage_location)
            put("movementType", sap.movementType)
            put("user_id", sap.user_id)
            put("timestamp", convertMillisecondsToISO8601(System.currentTimeMillis()))
            put("source", sap.source)
            put("transaction", sap.transaction)
        }

        intent.putExtra("metadata", json.toString())
        intent.putExtra("result", Gson().toJson(sap))
    }
    activity.setResult(RESULT_OK, intent)
    activity.finish()
}
