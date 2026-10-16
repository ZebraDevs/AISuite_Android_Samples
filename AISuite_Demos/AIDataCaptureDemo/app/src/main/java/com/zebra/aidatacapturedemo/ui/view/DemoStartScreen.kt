@file:Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package com.zebra.aidatacapturedemo.ui.view

import android.content.Context
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.data.UsecaseState
import com.zebra.aidatacapturedemo.ui.view.Variables.mainDisabled
import com.zebra.aidatacapturedemo.ui.view.Variables.mainPrimary
import com.zebra.aidatacapturedemo.ui.view.filters.InitToast
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
    // Screen is only "active" when its back stack entry is RESUMED.
    // During back navigation, state moves to STARTED/STOPPED before composable leaves.
    val lifecycleState by LocalLifecycleOwner.current.lifecycle
        .currentStateFlow.collectAsState()
    val isScreenActive = lifecycleState.isAtLeast(Lifecycle.State.STARTED)

    // Compact layout threshold for smaller displays.
    val windowManager = getSystemService(context, WindowManager::class.java)
    val windowMetrics: WindowMetrics = windowManager.currentWindowMetrics
    val isCompactHeight = windowMetrics.bounds.height() <= 800

    // UI-only state for Start button enabled/disabled.
    val isStartDisabled = remember { mutableStateOf(true) }

    val uiState = viewModel.uiState.collectAsState().value

    InitToast(uiState, viewModel)

    BackHandler(enabled = true) { viewModel.handleBackButton(navController) }
    RecognizerInitErrorHandler(uiState, viewModel, navController)
    // Computes loading and updates start-enabled state.
    LoadingScreen(
        viewModel = viewModel,
        navController = navController,
        uiState = uiState,
        isStartDisabledChanged = { newValue ->
            if (uiState.activeScreen == Screen.DemoStart) {
                isStartDisabled.value = newValue
            }
        }
    )

    // Only render main content if screen is ready to avoid flicker
    if (isScreenActive) DemoStartMainContent(
        uiState = uiState,
        viewModel = viewModel,
        navController = navController,
        isStartDisabled = isStartDisabled.value,
        isCompactHeight = isCompactHeight,
        innerPadding = innerPadding
    )
}

@Composable
private fun DemoStartMainContent(
    uiState: AIDataCaptureDemoUiState,
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavController,
    isStartDisabled: Boolean,
    isCompactHeight: Boolean,
    innerPadding: PaddingValues
) {
    // Spacing + typography values vary by compact/regular height.
    val topSpacer = if (isCompactHeight) 10.dp else 37.dp
    val iconToDetailsSpacer = if (isCompactHeight) 10.dp else 48.dp
    val detailsTopModelSpacer = if (isCompactHeight) 0.dp else 8.dp
    val detailsSmallSpacer = if (isCompactHeight) 0.dp else 4.dp
    val ocrFindSpacer = if (isCompactHeight) 4.dp else 12.dp

    val onRestoreDefaults = remember(viewModel) {
        {
            viewModel.restoreDefaultSettings()
            viewModel.applySettings()
        }
    }

    val onStartClick = remember(navController, uiState.isCaptureOrLiveEnabled) {
        {
            if (uiState.isCaptureOrLiveEnabled == 2) {
                navController.navigate(route = Screen.TargetModePreview.route)
            } else {
                navController.navigate(route = Screen.Preview.route)
            }
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(color = Variables.surfaceDefault)
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(innerPadding)
    ) {
        Spacer(Modifier.height(topSpacer))
        UseCaseIcon(selectedUseCase = uiState.usecaseSelected)
        Spacer(Modifier.height(iconToDetailsSpacer))

        Column(
            modifier = Modifier
                .wrapContentHeight()
                .padding(start = 16.dp, end = 16.dp)
        ) {
            DemoStartHeader(
                uiState = uiState,
                viewModel = viewModel,
                isCompactHeight = isCompactHeight,
                detailsTopModelSpacer = detailsTopModelSpacer,
                detailsSmallSpacer = detailsSmallSpacer
            )

            if (uiState.usecaseSelected == UsecaseState.OCRBarcodeFind.value) {
                Spacer(modifier = Modifier.height(ocrFindSpacer))
                if (!isCompactHeight) {
                    TextviewBold(info = "Select Capture Setup")
                    Spacer(modifier = Modifier.height(ocrFindSpacer))
                }

                DemoStartOcrBarcodeFindControls(
                    uiState = uiState,
                    viewModel = viewModel,
                    spacer = ocrFindSpacer
                )
            } else if (isCompactHeight) {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
        Spacer(modifier = Modifier.weight(1f)) // pushes footer to bottom consistently

        DemoStartFooter(
            isCompactHeight = isCompactHeight,
            isStartDisabled = isStartDisabled,
            onStartClick = onStartClick,
            onRestoreDefaults = onRestoreDefaults
        )
    }
}

@Composable
private fun DemoStartHeader(
    uiState: AIDataCaptureDemoUiState,
    viewModel: AIDataCaptureDemoViewModel,
    isCompactHeight: Boolean,
    detailsTopModelSpacer: Dp,
    detailsSmallSpacer: Dp
) {
    val titleStringId = getSettingHeading(uiState.usecaseSelected)
    if (titleStringId == null) TextviewBold(info = "")
    else TextviewBold(info = stringResource(titleStringId))

    Spacer(modifier = Modifier.height(detailsTopModelSpacer))
    Row {
        viewModel.getInputSizeSelected()?.let {
            if (isCompactHeight) SmallScreenTextviewNormal(info = "\u2022   Model Input:")
            else TextviewBold(info = "\u2022   Model Input:")
            if (isCompactHeight) SmallScreenTextviewNormal(info = "  $it x $it")
            else TextviewNormal(info = "  $it x $it")
        }
    }

    Spacer(modifier = Modifier.height(detailsSmallSpacer))
    Row {
        viewModel.getSelectedResolution()?.let {
            val resolution = getSelectedResolution(it)
            if (isCompactHeight) SmallScreenTextviewNormal(info = "\u2022   Resolution:")
            else TextviewBold(info = "\u2022   Resolution:")
            if (isCompactHeight) SmallScreenTextviewNormal(info = "  $resolution")
            else TextviewNormal(info = "  $resolution")
        }
    }

    Spacer(modifier = Modifier.height(detailsSmallSpacer))
    Row {
        viewModel.getProcessorSelectedIndex()?.let {
            val inferenceType = getSelectedInferenceType(it)
            if (isCompactHeight) SmallScreenTextviewNormal(info = "\u2022   Inference (processor) Type:")
            else TextviewBold(info = "\u2022   Inference (processor) Type:")
            if (isCompactHeight) SmallScreenTextviewNormal(info = "  $inferenceType")
            else TextviewNormal(info = "  $inferenceType")
        }
    }

    // Text/OCR Recognizer is the only usecase where 2560 model input size is still offered
    // (single decoder, SDK-supported, pre-cached) \u2014 but combined with 8MP resolution it's still
    // a heavy, documented-cost combination. Shown inline here instead of a dialog so it doesn't
    // interrupt the user every time.
    if (uiState.usecaseSelected == UsecaseState.OCR.value &&
        viewModel.getInputSizeSelected() == 2560 &&
        viewModel.getSelectedResolution() == 3
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFFFFF4E5),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = Color(0xFFB25E02),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Higher resolution and model input size boost accuracy but use more memory/battery — may stress low-end devices. Lower them for better performance.",
                color = Color(0xFF7A4A00),
                fontSize = 13.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun DemoStartOcrBarcodeFindControls(
    uiState: AIDataCaptureDemoUiState,
    viewModel: AIDataCaptureDemoViewModel,
    spacer: Dp
) {
    // Capture mode selector row.
    Row(
        horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.Start),
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(
                color = Variables.colorsSurfaceDisabled,
                shape = RoundedCornerShape(size = Variables.radiusMinimal)
            )
            .padding(horizontal = Variables.spacingMinimum)
    ) {
        SingleChoiceSegmentedButton(viewModel, uiState.isCaptureOrLiveEnabled)
    }

    if (uiState.isCaptureOrLiveEnabled != 2) {
        // Barcode model toggle.
        Spacer(modifier = Modifier.height(spacer))
        Row {
            SwitchOptionForModelSelectionScreen(
                uiState.isBarcodeModelEnabled,
                SwitchOptionData(R.string.barcode_model) { _, enabled ->
                    viewModel.updateBarcodeModelEnabled(enabled)
                    viewModel.deInitModel()
                    viewModel.initModel()
                }
            )
        }

        // OCR model toggle.
        Spacer(modifier = Modifier.height(spacer))
        Row {
            SwitchOptionForModelSelectionScreen(
                uiState.isOCRModelEnabled,
                SwitchOptionData(R.string.ocr_model) { _, enabled ->
                    viewModel.updateOCRModelEnabled(enabled)
                    viewModel.deInitModel()
                    viewModel.initModel()
                }
            )
        }
    }
}

@Composable
private fun RestoreDefaultsText(
    fontSize: TextUnit,
    lineHeight: TextUnit,
    onClick: () -> Unit
) {
    Text(
        text = stringResource(R.string.restore_default),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable { onClick() },
        style = TextStyle(
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontFamily = FontFamily(Font(R.font.ibm_plex_sans_medium)),
            fontWeight = FontWeight(500),
            color = Variables.borderPrimaryMain
        )
    )
}

@Composable
private fun DemoStartFooter(
    isCompactHeight: Boolean,
    isStartDisabled: Boolean,
    onStartClick: () -> Unit,
    onRestoreDefaults: () -> Unit
) {
    val restoreTextFontSize = if (isCompactHeight) 12.sp else 16.sp
    val restoreTextLineHeight = if (isCompactHeight) 16.sp else 24.sp

    val buttonData = ButtonData(
        titleId = R.string.start_scan,
        color = if (isStartDisabled) mainDisabled else mainPrimary,
        alpha = 1.0f,
        enabled = !isStartDisabled,
        onButtonClick = { if (!isStartDisabled) onStartClick() }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isCompactHeight) 24.dp else 16.dp,
                vertical = 12.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RestoreDefaultsText(
            fontSize = restoreTextFontSize,
            lineHeight = restoreTextLineHeight,
            onClick = onRestoreDefaults
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (isCompactHeight) {
            SmallScreenButtonOption(buttonData)
        } else {
            ButtonOption(buttonData)
        }
    }
}

/** Converts processor index to a user-friendly label for the details section. */
@Composable
private fun getSelectedInferenceType(processorSelectedIndex: Int): String {
    return when (processorSelectedIndex) {
        0 -> stringResource(R.string.processor_auto)
        1 -> stringResource(R.string.processor_dsp_short)
        2 -> stringResource(R.string.processor_gpu_short)
        3 -> stringResource(R.string.processor_cpu_short)
        else -> stringResource(R.string.processor_auto)
    }
}

/** Converts selected resolution index to display text shown on Demo Start. */
@Composable
private fun getSelectedResolution(resolutionSelectedIndex: Int): String {
    return when (resolutionSelectedIndex) {
        0 -> stringResource(R.string.resolution_size_1280)
        1 -> stringResource(R.string.resolution_size_1920)
        2 -> stringResource(R.string.resolution_size_2688)
        3 -> stringResource(R.string.resolution_size_3840)
        else -> stringResource(R.string.resolution_size_1920)
    }
}

/** Gradient use-case icon card (used on top of Demo Start). */
@Composable
fun UseCaseIcon(selectedUseCase: String) {
    val iconId = getIconId(selectedUseCase) ?: return
    val iconColors = listOf(
        getIconMainColor(selectedUseCase),
        getIconSecondaryColor(selectedUseCase)
    )

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
                brush = Brush.verticalGradient(colors = iconColors)
            )
    ) {
        Image(
            painter = painterResource(id = iconId),
            contentDescription = "image description",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(64.dp)
                .height(64.dp)
        )
    }
}

/**
 * Capture setup toggle:
 * - index 0 => Image Capture
 * - index 1 => Live Video
 * Uses ViewModel as single source of truth for selected index.
 */
@Composable
fun SingleChoiceSegmentedButton(
    viewModel: AIDataCaptureDemoViewModel,
    currentChoice: Int,
    modifier: Modifier = Modifier
) {
    val selectedIndex = remember { mutableIntStateOf(currentChoice) }
    val options = listOf("Image\nCapture", "Live\nVideo", "Target\nMode")
    val icons = listOf(R.drawable.camera_icon, R.drawable.video_icon, R.drawable.ic_target_mode)

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex.intValue
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp),
                onClick = {
                    selectedIndex.intValue = index
                    viewModel.updateCaptureOrLiveEnabled(index)
                    viewModel.deInitModel()
                    viewModel.initModel()
                },
                selected = isSelected,
                // Put everything inside label; suppress the default check-mark icon slot
                icon = {},
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 1.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = icons[index]),
                            contentDescription = label,
                            modifier = Modifier.size(18.dp),
                            tint = if (isSelected) Variables.colorsBorderPrimaryLegacy
                            else Variables.colorsMainSubtle
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = label,
                            style = TextStyle(
                                fontSize = 14.sp,
                                lineHeight = 14.sp,
                                fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                                fontWeight = FontWeight(500),
                                color = if (isSelected) Variables.colorsBorderPrimaryLegacy
                                else Variables.colorsTextDefault,
                                textAlign = TextAlign.Center,
                            )
                        )
                    }
                }
            )
        }
    }
}

/**
 * Decides if loading overlay should be visible and whether Start Scan is enabled.
 * Sends enabled/disabled state as side-effect to avoid state writes during composition.
 */
@Composable
fun LoadingScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavController,
    uiState: AIDataCaptureDemoUiState,
    isStartDisabledChanged: (Boolean) -> Unit
) {
    // Captures latest callback without recomposing dependent children
    val latestCallback = rememberUpdatedState(isStartDisabledChanged)

    val isLoading = when (uiState.usecaseSelected) {
        UsecaseState.OCRBarcodeFind.value -> {
            when {
                uiState.isBarcodeModelEnabled && uiState.isOCRModelEnabled ->
                    !(uiState.isBarcodeModelDemoReady && uiState.isOcrModelDemoReady)

                !uiState.isBarcodeModelEnabled && !uiState.isOCRModelEnabled -> false
                uiState.isBarcodeModelEnabled -> !uiState.isBarcodeModelDemoReady
                else -> !uiState.isOcrModelDemoReady
            }
        }

        UsecaseState.Barcode.value -> !uiState.isBarcodeModelDemoReady
        UsecaseState.OCR.value -> !uiState.isOcrModelDemoReady
        UsecaseState.Retail.value, UsecaseState.Product.value -> !uiState.isRetailShelfModelDemoReady
        else -> false
    }

    val isStartDisabled = when (uiState.usecaseSelected) {
        UsecaseState.OCRBarcodeFind.value ->
            isLoading || (!uiState.isBarcodeModelEnabled && !uiState.isOCRModelEnabled)

        else -> isLoading
    }

    LaunchedEffect(isStartDisabled) {
        latestCallback.value(isStartDisabled)
    }

    if (isLoading) {
        // Retail/Product re-init now waits for a superseded instance's native inits to fully
        // settle before starting the new one (to avoid two native model instances allocating
        // memory at once), which can roughly double load time at high resolution/input-size
        // settings. Give those use cases more room before declaring a timeout.
        val timeoutMs = when (uiState.usecaseSelected) {
            UsecaseState.Retail.value, UsecaseState.Product.value -> 60_000L
            else -> 30_000L
        }
        ModalLoadingOverlay(
            timeoutMs = timeoutMs,
            onTimeout = {
                viewModel.toast("Loading timed out. Please try again.")
                viewModel.handleBackButton(navController)
            }
        )
    }
}

@Composable
private fun RecognizerInitErrorHandler(
    uiState: AIDataCaptureDemoUiState,
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavController
) {
    LaunchedEffect(uiState.recognizerInitError) {
        uiState.recognizerInitError?.let { message ->
            viewModel.toast(message)
            viewModel.updateRecognizerInitError(null)
            viewModel.handleBackButton(navController)
        }
    }
}