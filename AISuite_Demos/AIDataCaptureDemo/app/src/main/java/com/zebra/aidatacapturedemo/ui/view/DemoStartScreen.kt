package com.zebra.aidatacapturedemo.ui.view

import android.R.attr.strokeWidth
import android.content.Context
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScrollModifierNode
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat.getSystemService
import androidx.navigation.NavController
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
    var isLoading = remember { mutableStateOf(true) }
    var isStartDisabled = remember { mutableStateOf(true) }

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
                if(isStartDisabled.value == true){
                    ButtonOption(
                        ButtonData(
                            R.string.start_scan,
                            mainDisabled,
                            1.0F,
                            false,
                            onButtonClick = {
                            })
                    )
                } else {
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
                }
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
                if(isStartDisabled.value == true){
                    ButtonOption(
                        ButtonData(
                            R.string.start_scan,
                            mainDisabled,
                            1.0F,
                            false,
                            onButtonClick = {
                            })
                    )
                } else {
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
                }
            }
        }
    }
    LoadingScreen(uiState, isLoading, isStartDisabled)
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
fun ModalLoadingOverlay(onDismissRequest: () -> Unit) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false, // Crucial for full width
            decorFitsSystemWindows = false // Allows drawing under system bars if configured in Activity
        )
    ) {
        // Block user interaction with the UI below the overlay
        Box(
            modifier = Modifier
                .wrapContentSize()
                .background(Color.White.copy(alpha = 0.0f)) // Semi-transparent background
                .pointerInput(Unit) {
                    // Intercept all tap gestures so they don't reach the underlying content
                    detectTapGestures(onTap = { /* Do nothing */ })
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(164.dp)
                    .height(164.dp)
                    .background(
                        color = Variables.surfaceDefault,
                        shape = RoundedCornerShape(size = 8.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Variables.borderDefault,
                        shape = RoundedCornerShape(size = 8.dp)
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(33.dp))
                CircularProgressIndicator(
                    color = mainPrimary,
                    modifier = Modifier
                        .width(56.dp)
                        .height(56.dp),
                    strokeWidth = 7.dp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Loading",
                    style = TextStyle(
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                        fontWeight = FontWeight(400),
                        color = Variables.colorsMainSubtle,
                        textAlign = TextAlign.Center,
                    )
                )
            }
        }

        // Handle the back button press to prevent dismissal during critical ops
        BackHandler {
            onDismissRequest()
        }
    }
}

@Composable
fun LoadingScreen(uiState: AIDataCaptureDemoUiState, isLoading: MutableState<Boolean>, isStartDisabled : MutableState<Boolean>) {
    when (uiState.usecaseSelected) {
        UsecaseState.OCRBarcodeFind.value -> {
            if (uiState.isBarcodeModelEnabled && uiState.isOCRModelEnabled) {
                if(uiState.isBarcodeModelDemoReady && uiState.isOcrModelDemoReady) {
                    isLoading.value = false
                    isStartDisabled.value = false
                } else {
                    isLoading.value = true
                    isStartDisabled.value = true
                }
            }
            else if (!uiState.isBarcodeModelEnabled && !uiState.isOCRModelEnabled) {
                isLoading.value = false
                isStartDisabled.value = true
            } else if (uiState.isBarcodeModelEnabled && !uiState.isOCRModelEnabled) {
                if(uiState.isBarcodeModelDemoReady) {
                    isLoading.value = false
                    isStartDisabled.value = false
                } else {
                    isLoading.value = true
                    isStartDisabled.value = true
                }
            } else if (!uiState.isBarcodeModelEnabled && uiState.isOCRModelEnabled) {
                if(uiState.isOcrModelDemoReady) {
                    isLoading.value = false
                    isStartDisabled.value = false
                } else {
                    isLoading.value = true
                    isStartDisabled.value = true
                }
            } else {
                isLoading.value = false
                isStartDisabled.value = true
            }
        }
        UsecaseState.Barcode.value -> {
            if (uiState.isBarcodeModelDemoReady) {
                isLoading.value = false
                isStartDisabled.value = false
            } else {
                isLoading.value = true
                isStartDisabled.value = true
            }
        }
        UsecaseState.OCR.value -> {
            if (uiState.isOcrModelDemoReady) {
                isLoading.value = false
                isStartDisabled.value = false
            } else {
                isLoading.value = true
                isStartDisabled.value = true
            }
        }
        UsecaseState.Retail.value,
        UsecaseState.Product.value -> {
            if (uiState.isRetailShelfModelDemoReady) {
                isLoading.value = false
                isStartDisabled.value = false
            } else {
                isLoading.value = true
                isStartDisabled.value = true
            }
        }
    }
    if(isLoading.value == true) {
        ModalLoadingOverlay(
            onDismissRequest = {
                // Optional: handle back button press during loading
                // You can choose to ignore it or set isLoading to false
                // BackHandler is used to block accidental navigation
            }
        )
    }
}