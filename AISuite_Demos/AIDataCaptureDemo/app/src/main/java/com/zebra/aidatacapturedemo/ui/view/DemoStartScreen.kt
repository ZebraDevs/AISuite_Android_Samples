package com.zebra.aidatacapturedemo.ui.view

import android.content.Context
import android.util.Log
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.getSystemService
import androidx.navigation.NavController
import com.zebra.ai.vision.detector.InferencerOptions
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.data.UsecaseState
import com.zebra.aidatacapturedemo.ui.view.Variables.mainDisabled
import com.zebra.aidatacapturedemo.ui.view.Variables.mainPrimary
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel

@Composable
fun DemoStartScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavController,
    innerPadding: PaddingValues,
    context: Context
) {
    val uiState = viewModel.uiState.collectAsState().value
    getDemoTitle(uiState.usecaseSelected)?.let { viewModel.updateAppBarTitle(stringResource(it)) }


    uiState.toastMessage?.let {
        viewModel.toast(it)
        viewModel.updateToastMessage(message = null)
    }
    // Intercept back presses on this screen
    BackHandler(enabled = true) {
        viewModel.handleBackButton(navController)
    }

    val windowManager = getSystemService(context, WindowManager::class.java)
    val windowMetrics: WindowMetrics = windowManager.currentWindowMetrics
    // draw smaller icon if device display height is 800px or less
    if (windowMetrics.bounds.height() <= 800) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(color = Variables.surfaceDefault)
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(innerPadding)
        ) {

            // Icon
            Spacer(Modifier.height(10.dp))

            UsecaseIcon(selectedUsecase = uiState.usecaseSelected)

            Spacer(Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .padding(start = 16.dp, end = 16.dp)
            ) {
                // Heading:
                val titleStringId = getSettingHeading(uiState.usecaseSelected)
                if (titleStringId == null) {
                    TextviewBold(info = "")
                } else {
                    TextviewBold(info = stringResource(titleStringId))
                }

                // Model Input Details:
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    viewModel.getInputSizeSelected()?.let {
                        TextviewBold(info = "\u2022   Model Input:")
                        TextviewNormal(info = "  $it x $it")
                    }
                }

                // Resolution Details:
                Spacer(modifier = Modifier.height(2.dp))
                Row {
                    viewModel.getSelectedResolution()?.let {
                        TextviewBold(info = "\u2022   Resolution:")
                        val resolution = getSelectedResolution(it)
                        TextviewNormal(info = "  $resolution")
                    }
                }

                // Inference Type Details:
                Spacer(modifier = Modifier.height(2.dp))
                Row {
                    viewModel.getProcessorSelectedIndex()?.let {
                        TextviewBold(info = "\u2022   Inference (processor) Type:")
                        val inferenceType = getSelectedInferenceType(it)
                        TextviewNormal(info = "  $inferenceType")
                    }
                }

                if (uiState.usecaseSelected == UsecaseState.OCRBarcodeFind.value) {
                    // Barcode Switch
                    Spacer(modifier = Modifier.height(4.dp))
                    Row {
                        SwitchOption(
                            uiState.isBarcodeModelEnabled,
                            SwitchOptionData(
                                R.string.barcode_model,
                                onItemSelected = { title, enabled ->

                                    viewModel.updateBarcodeModelEnabled(enabled)
                                    viewModel.deinitModel()
                                    viewModel.initModel()
                                })
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Row {
                        SwitchOption(
                            uiState.isOCRModelEnabled,
                            SwitchOptionData(
                                R.string.ocr_model,
                                onItemSelected = { title, enabled ->
                                    viewModel.updateOCRModelEnabled(enabled)
                                    viewModel.deinitModel()
                                    viewModel.initModel()
                                })
                        )
                    }
                }
                // Restore Clickable Text:
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.restore_default),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clickable {
                            viewModel.restoreDefaultSettings()
                            viewModel.applySettings()
                        },
                    style = TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontFamily = FontFamily(Font(R.font.ibm_plex_sans_medium)),
                        fontWeight = FontWeight(500),
                        color = Variables.borderPrimaryMain,
                    )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                StartScanButton(uiState, navController)
            }
        }

    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(color = Variables.surfaceDefault)
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(innerPadding)
        ) {

            // Icon
            Spacer(Modifier.height(37.dp))

            UsecaseIcon(selectedUsecase = uiState.usecaseSelected)

            Spacer(Modifier.height(48.dp))
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .padding(start = 16.dp, end = 16.dp)
            ) {
                // Heading:
                val titleStringId = getSettingHeading(uiState.usecaseSelected)
                if (titleStringId == null) {
                    TextviewBold(info = "")
                } else {
                    TextviewBold(info = stringResource(titleStringId))
                }

                // Model Input Details:
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    viewModel.getInputSizeSelected()?.let {
                        TextviewBold(info = "\u2022   Model Input:")
                        TextviewNormal(info = "  $it x $it")
                    }
                }

                // Resolution Details:
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    viewModel.getSelectedResolution()?.let {
                        TextviewBold(info = "\u2022   Resolution:")
                        val resolution = getSelectedResolution(it)
                        TextviewNormal(info = "  $resolution")
                    }
                }

                // Inference Type Details:
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    viewModel.getProcessorSelectedIndex()?.let {
                        TextviewBold(info = "\u2022   Inference (processor) Type:")
                        val inferenceType = getSelectedInferenceType(it)
                        TextviewNormal(info = "  $inferenceType")
                    }
                }

                if (uiState.usecaseSelected == UsecaseState.OCRBarcodeFind.value) {
                    // Barcode Switch
                    Spacer(modifier = Modifier.height(16.dp))
                    Row {
                        SwitchOption(
                            uiState.isBarcodeModelEnabled,
                            SwitchOptionData(
                                R.string.barcode_model,
                                onItemSelected = { title, enabled ->
                                    viewModel.updateBarcodeModelEnabled(enabled)
                                    viewModel.deinitModel()
                                    viewModel.initModel()
                                })
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row {
                        SwitchOption(
                            uiState.isOCRModelEnabled,
                            SwitchOptionData(
                                R.string.ocr_model,
                                onItemSelected = { title, enabled ->
                                    viewModel.updateOCRModelEnabled(enabled)
                                    viewModel.deinitModel()
                                    viewModel.initModel()
                                })
                        )
                    }
                }
                // Restore Clickable Text:
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.restore_default),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clickable {
                            viewModel.restoreDefaultSettings()
                            viewModel.applySettings()
                        },
                    style = TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontFamily = FontFamily(Font(R.font.ibm_plex_sans_medium)),
                        fontWeight = FontWeight(500),
                        color = Variables.borderPrimaryMain,
                    )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                StartScanButton(uiState, navController)
            }
        }
    }
}

@Composable
private fun getSelectedInferenceType(processorSelectedIndex: Int): String {
    return when (processorSelectedIndex) {
        0 -> {
            stringResource(R.string.processor_auto)
        }

        1 -> {
            stringResource(R.string.processor_dsp_short)
        }

        2 -> {
            stringResource(R.string.processor_gpu_short)
        }

        3 -> {
            stringResource(R.string.processor_cpu_short)
        }

        else -> {
            stringResource(R.string.processor_auto)
        }
    }
}

@Composable
private fun getSelectedResolution(resolutionSelectedIndex: Int): String {
    return when (resolutionSelectedIndex) {
        0 -> {
            "${stringResource(R.string.resolution_size_1280)}"
        }

        1 -> {
            "${stringResource(R.string.resolution_size_1920)}"
        }

        2 -> {
            "${stringResource(R.string.resolution_size_2688)}"
        }

        3 -> {
            "${stringResource(R.string.resolution_size_3840)}"
        }

        else -> {
            TODO("Unknown Resolution found $resolutionSelectedIndex")
        }
    }
}

@Composable
fun UsecaseIcon(selectedUsecase: String) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .width(88.dp)
            .height(88.dp)
            .background(
                shape = RoundedCornerShape(
                    topStart = 6.dp,
                    topEnd = 6.dp,
                    bottomStart = 6.dp,
                    bottomEnd = 6.dp
                ),
                brush = Brush.verticalGradient(
                    colors = listOf(
                        getIconMainColor(selectedUsecase),
                        getIconSecondaryColor(selectedUsecase)
                    )
                )
            )
    ) {
        getIconId(selectedUsecase)?.let {
            Image(
                painter = painterResource(id = it),
                contentDescription = "image description",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(64.dp)
                    .height(64.dp)
            )
        }
    }
}

@Composable
fun StartScanButton(uiState: AIDataCaptureDemoUiState, navController: NavController) {

    when (uiState.usecaseSelected) {
        UsecaseState.OCRBarcodeFind.value -> {
            // (uiState.isBarcodeModelEnabled || uiState.isOCRModelEnabled) is used to make sure,
            // the Start Scan button stay disabled when both the switch are disabled.
            if (uiState.isBarcodeModelDemoReady &&
                uiState.isOcrModelDemoReady &&
                (uiState.isBarcodeModelEnabled || uiState.isOCRModelEnabled)) {
                ButtonOption(
                    ButtonData(
                        R.string.start_scan,
                        mainPrimary,
                        1.0F,
                        true,
                        onButtonClick = {
                            navController.navigate(route = Screen.Preview.route)
                        })
                )
            } else {
                ButtonOption(
                    ButtonData(
                        R.string.start_scan,
                        mainDisabled,
                        1.0F,
                        false,
                        onButtonClick = {
                        })
                )
            }
        }

        UsecaseState.Barcode.value -> {
            if (uiState.isBarcodeModelDemoReady) {
                ButtonOption(
                    ButtonData(
                        R.string.start_scan,
                        mainPrimary,
                        1.0F,
                        true,
                        onButtonClick = {
                            navController.navigate(route = Screen.Preview.route)
                        })
                )
            } else {
                ButtonOption(
                    ButtonData(
                        R.string.start_scan,
                        mainDisabled,
                        1.0F,
                        false,
                        onButtonClick = {
                        })
                )
            }
        }

        UsecaseState.OCR.value -> {
            if (uiState.isOcrModelDemoReady) {
                ButtonOption(
                    ButtonData(
                        R.string.start_scan,
                        mainPrimary,
                        1.0F,
                        true,
                        onButtonClick = {
                            navController.navigate(route = Screen.Preview.route)
                        })
                )
            } else {
                ButtonOption(
                    ButtonData(
                        R.string.start_scan,
                        mainDisabled,
                        1.0F,
                        false,
                        onButtonClick = {
                        })
                )
            }
        }

        UsecaseState.Retail.value,
        UsecaseState.Product.value -> {
            if (uiState.isRetailShelfModelDemoReady) {
                ButtonOption(
                    ButtonData(
                        R.string.start_scan,
                        mainPrimary,
                        1.0F,
                        true,
                        onButtonClick = {
                            navController.navigate(route = Screen.Preview.route)
                        })
                )
            } else {
                ButtonOption(
                    ButtonData(
                        R.string.start_scan,
                        mainDisabled,
                        1.0F,
                        false,
                        onButtonClick = {
                        })
                )
            }
        }
    }
}