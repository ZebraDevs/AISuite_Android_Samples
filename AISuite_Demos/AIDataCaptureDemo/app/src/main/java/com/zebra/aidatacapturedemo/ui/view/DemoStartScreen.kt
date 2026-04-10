package com.zebra.aidatacapturedemo.ui.view

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
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

/*
 * DemoStartScreen is the initial screen of the AI Data Capture Demo app, providing users with an
 * overview of the selected use case and its settings before starting the scanning process.
 * It displays relevant information such as model input size, resolution, and inference type
 * based on the user's selections. The screen also includes a loading overlay while models are
 * being initialized and handles back button presses to ensure proper navigation flow.
 */
@Composable
fun DemoStartScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavController,
    innerPadding: PaddingValues,
    context: Context
) {
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
    LoadingScreen(viewModel, navController, uiState, isStartDisabledChanged = {isStartDisabled.value = it})
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
                Row {
                    viewModel.getInputSizeSelected()?.let {
                        SmallScreenTextviewNormal(info = "\u2022   Model Input:")
                        SmallScreenTextviewNormal(info = "  $it x $it")
                    }
                }

                // Resolution Details:
                Row {
                    viewModel.getSelectedResolution()?.let {
                        SmallScreenTextviewNormal(info = "\u2022   Resolution:")
                        val resolution = getSelectedResolution(it)
                        SmallScreenTextviewNormal(info = "  $resolution")
                    }
                }

                // Inference Type Details:
                Row {
                    viewModel.getProcessorSelectedIndex()?.let {
                        SmallScreenTextviewNormal(info = "\u2022   Inference (processor) Type:")
                        val inferenceType = getSelectedInferenceType(it)
                        SmallScreenTextviewNormal(info = "  $inferenceType")
                    }
                }

                if (uiState.usecaseSelected == UsecaseState.OCRBarcodeFind.value) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.Start),
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .background(color = Variables.colorsSurfaceDisabled, shape = RoundedCornerShape(size = Variables.radiusMinimal))
                            .padding(horizontal = Variables.spacingMinimum)
                    ) {
                        SingleChoiceSegmentedButton(viewModel,uiState.isCaptureOrLiveEnabled)
                    }

                    // Barcode Switch
                    Spacer(modifier = Modifier.height(4.dp))
                    Row {
                        SwitchOptionForModelSelectionScreen(
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
                        SwitchOptionForModelSelectionScreen(
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

                    // Restore Clickable Text:
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
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontFamily = FontFamily(Font(R.font.ibm_plex_sans_medium)),
                            fontWeight = FontWeight(500),
                            color = Variables.borderPrimaryMain,
                        )
                    )
                }else{
                    Spacer(modifier = Modifier.height(30.dp))

                    // Restore Clickable Text:
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
                            fontSize = 14.sp,
                            lineHeight = 24.sp,
                            fontFamily = FontFamily(Font(R.font.ibm_plex_sans_medium)),
                            fontWeight = FontWeight(500),
                            color = Variables.borderPrimaryMain,
                        )
                    )
                }

            }

            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                if(isStartDisabled.value == true){
                    SmallScreenButtonOption(
                        ButtonData(
                            R.string.start_scan,
                            mainDisabled,
                            1.0F,
                            false,
                            onButtonClick = {
                            })
                    )
                } else {
                    SmallScreenButtonOption(
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
                    Spacer(modifier = Modifier.height(12.dp))
                    TextviewBold(info = "Select Capture Setup")
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.Start),
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .background(color = Variables.colorsSurfaceDisabled, shape = RoundedCornerShape(size = Variables.radiusMinimal))
                            .padding(horizontal = Variables.spacingMinimum)
                    ) {
                        SingleChoiceSegmentedButton(viewModel,uiState.isCaptureOrLiveEnabled)
                    }
                    // Barcode Switch
                    Spacer(modifier = Modifier.height(12.dp))
                    Row {
                        SwitchOptionForModelSelectionScreen(
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
                    Spacer(modifier = Modifier.height(12.dp))
                    Row {
                        SwitchOptionForModelSelectionScreen(
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
                Spacer(modifier = Modifier.height(12.dp))
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
fun SingleChoiceSegmentedButton(viewModel: AIDataCaptureDemoViewModel, currentChoice : Int, modifier: Modifier = Modifier) {
    var selectedIndex = remember { mutableIntStateOf(currentChoice) }
    val options = listOf("Image Capture", "Live Video")
    val icons = listOf(R.drawable.camera_icon, R.drawable.video_icon)

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, label ->
            SegmentedButton(
                shape = RoundedCornerShape(size = Variables.radiusMinimal),
                colors = SegmentedButtonColors(
                    Variables.surfaceDefault,
                    mainPrimary,
                    Variables.surfaceDefault,
                    inactiveContainerColor = Variables.colorsSurfaceDisabled,
                    inactiveContentColor = Variables.colorsMainSubtle,
                    inactiveBorderColor = Variables.colorsSurfaceDisabled,
                    disabledActiveContainerColor = Variables.colorsSurfaceDisabled,
                    disabledActiveContentColor = Variables.colorsMainSubtle,
                    disabledActiveBorderColor = Variables.colorsSurfaceDisabled,
                    disabledInactiveContainerColor = Variables.colorsSurfaceDisabled,
                    disabledInactiveContentColor = Variables.colorsMainSubtle,
                    disabledInactiveBorderColor = Variables.colorsSurfaceDisabled,
                ),
                modifier = Modifier.weight(1f),
                onClick = {
                    selectedIndex.value = index
                    viewModel.updateCaptureOrLiveEnabled(index)
                    viewModel.deinitModel()
                    viewModel.initModel()
                },
                selected = index == selectedIndex.value,
                label = {
                    Text(
                        text = label,
                        style = TextStyle(
                            fontSize = Variables.TypefaceFontSize14,
                            lineHeight = Variables.TypefaceLineHeight20,
                            fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                            fontWeight = FontWeight(500),
                            color = if (index == selectedIndex.value){
                                Variables.colorsBorderPrimaryLegacy
                            }else{
                                Variables.colorsTextDefault
                            },
                            textAlign = TextAlign.Center,
                        )
                    )
                },
                icon = { Icon(
                    painter = painterResource(id = icons[index]),
                    contentDescription = "Camera Icon",
                ) }
            )
        }
    }
}

@Composable
private fun LoadingScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavController,
    uiState: AIDataCaptureDemoUiState,
    isStartDisabledChanged: (Boolean) -> Unit,
) {
    var isLoading = remember { mutableStateOf(true) }
    when (uiState.usecaseSelected) {
        UsecaseState.OCRBarcodeFind.value -> {
            if (uiState.isBarcodeModelEnabled && uiState.isOCRModelEnabled) {
                if(uiState.isBarcodeModelDemoReady && uiState.isOcrModelDemoReady) {
                    isLoading.value = false
                    isStartDisabledChanged(false)
                } else {
                    isLoading.value = true
                    isStartDisabledChanged(true)
                }
            }
            else if (!uiState.isBarcodeModelEnabled && !uiState.isOCRModelEnabled) {
                isLoading.value = false
                isStartDisabledChanged(true)
            } else if (uiState.isBarcodeModelEnabled && !uiState.isOCRModelEnabled) {
                if(uiState.isBarcodeModelDemoReady) {
                    isLoading.value = false
                    isStartDisabledChanged(false)
                } else {
                    isLoading.value = true
                    isStartDisabledChanged(true)
                }
            } else if (!uiState.isBarcodeModelEnabled && uiState.isOCRModelEnabled) {
                if(uiState.isOcrModelDemoReady) {
                    isLoading.value = false
                    isStartDisabledChanged(false)
                } else {
                    isLoading.value = true
                    isStartDisabledChanged(true)
                }
            } else {
                isLoading.value = false
                isStartDisabledChanged(true)
            }
        }
        UsecaseState.Barcode.value -> {
            if (uiState.isBarcodeModelDemoReady) {
                isLoading.value = false
                isStartDisabledChanged(false)
            } else {
                isLoading.value = true
                isStartDisabledChanged(true)
            }
        }
        UsecaseState.OCR.value -> {
            if (uiState.isOcrModelDemoReady) {
                isLoading.value = false
                isStartDisabledChanged(false)
            } else {
                isLoading.value = true
                isStartDisabledChanged(true)
            }
        }
        UsecaseState.Retail.value,
        UsecaseState.Product.value -> {
            if (uiState.isRetailShelfModelDemoReady) {
                isLoading.value = false
                isStartDisabledChanged(false)
            } else {
                isLoading.value = true
                isStartDisabledChanged(true)
            }
        }
    }
    if(isLoading.value == true) {
        ModalLoadingOverlay(
            onDismissRequest = {
                viewModel.handleBackButton(navController = navController)
            }
        )
    }
}