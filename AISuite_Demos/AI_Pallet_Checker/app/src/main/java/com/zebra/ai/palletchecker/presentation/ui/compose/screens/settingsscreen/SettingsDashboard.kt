package com.zebra.ai.palletchecker.presentation.ui.compose.screens.settingsscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.zebra.ai.palletchecker.R
import com.zebra.ai.palletchecker.domain.enums.ProcessorType
import com.zebra.ai.palletchecker.domain.enums.Resolution
import com.zebra.ai.palletchecker.domain.model.BarcodeSymbology
import com.zebra.ai.palletchecker.presentation.ui.compose.components.ZebraText
import com.zebra.ai.palletchecker.presentation.ui.compose.screens.settingsscreen.bottomsheet.BarcodeSymbologyBottomSheet
import com.zebra.ai.palletchecker.presentation.ui.compose.screens.settingsscreen.bottomsheet.CaptureModeBottomSheet
import com.zebra.ai.palletchecker.presentation.ui.compose.screens.settingsscreen.bottomsheet.InferenceBottomSheet
import com.zebra.ai.palletchecker.presentation.ui.compose.screens.settingsscreen.bottomsheet.ModelInputSizeBottomSheet
import com.zebra.ai.palletchecker.presentation.ui.compose.screens.settingsscreen.bottomsheet.ResolutionBottomSheet
import com.zebra.ai.palletchecker.presentation.ui.theme.AppColors
import com.zebra.ai.palletchecker.presentation.ui.theme.AppTextStyles
import com.zebra.ai.palletchecker.presentation.ui.theme.darkBackground
import com.zebra.ai.palletchecker.presentation.ui.theme.settingsLazyColumnBackground
import com.zebra.ai.palletchecker.presentation.ui.theme.white
import com.zebra.ai.palletchecker.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDashboard(
    settingsViewModel: SettingsViewModel,
    onBackClick: () -> Unit = {},
    onBarcodeModelVersionsClick: () -> Unit = {},
    onResetToDefaultClick: () -> Unit = {},
    onBackPressed: () -> Unit = {},
    expectedBoxesToAudit: Int = 10
) {
    val settings by settingsViewModel.settings.collectAsState()
    var showModelInputSizeDialog by remember { mutableStateOf(false) }
    var showResolutionDialog by remember { mutableStateOf(false) }
    var showInferenceDialog by remember { mutableStateOf(false) }
    var showBarcodeSymbologyDialog by remember { mutableStateOf(false) }
    var showCaptureModeDialog by remember { mutableStateOf(false) }
    var showSnapWandSettings by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(settingsLazyColumnBackground)
            .semantics { contentDescription = "SettingsScreen" }
    ) {
        TopAppBar(windowInsets = WindowInsets(0.dp),
            title = {
                ZebraText(
                    textValue = stringResource(id = R.string.setting_screen_barcode_recognizer_title),
                    style = AppTextStyles.TitleTextLight,
                    textColor = AppColors.TextWhite,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.setting_screen_back_icon_description),
                        tint = white
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = darkBackground
            )
        )

        SettingsScreen(
            settingsViewModel,
            onBackClick,
            onBarcodeModelVersionsClick,
            onResetToDefaultClick,
            onBackPressed,
            showFinerDecoderSettings = showSnapWandSettings,
            showBottomSheet = { type ->
                when (type) {
                    1 -> {
                        showResolutionDialog = true
                    }

                    0 -> {
                        showModelInputSizeDialog = true
                    }

                    2 -> {
                        showInferenceDialog = true
                    }

                    3 -> {
                        showBarcodeSymbologyDialog = true
                    }

                    4 -> {
                        showCaptureModeDialog = true
                    }
                }

            },
            expectedBoxesToAudit = expectedBoxesToAudit
        )
    }


    if (showModelInputSizeDialog) {
        settingsViewModel.let { viewModel ->
            ModelInputSizeBottomSheet(
                settingsViewModel = viewModel,
                currentSelection = settings.modelInput,
                onBackClick = { showModelInputSizeDialog = false },
                onBackPressed = { showModelInputSizeDialog = false }
            )
        }
    }


    if (showResolutionDialog) {
        settingsViewModel.let { viewModel ->
            ResolutionBottomSheet(
                settingsViewModel = viewModel,
                currentSelection = settings.resolution ?: Resolution.TWO_MP,
                onBackClick = { showResolutionDialog = false },
                onBackPressed = { showResolutionDialog = false }
            )
        }
    }

    if (showInferenceDialog) {
        settingsViewModel.let { viewModel ->
            InferenceBottomSheet(
                settingsViewModel = viewModel,
                currentSelection = settings.processorType ?: ProcessorType.DSP,
                onBackClick = { showInferenceDialog = false },
                onBackPressed = { showInferenceDialog = false }
            )
        }
    }

    if (showCaptureModeDialog) {
        CaptureModeBottomSheet(
            onBackClick = { showCaptureModeDialog = false },
            onBackPressed = { showCaptureModeDialog = false }
        )
    }

    if (showBarcodeSymbologyDialog) {
        settingsViewModel.let { viewModel ->
            BarcodeSymbologyBottomSheet(
                settingsViewModel = viewModel,
                currentSymbology = settings.barcodeSymbology ?: BarcodeSymbology(),
                onBackClick = { showBarcodeSymbologyDialog = false },
                onBackPressed = { showBarcodeSymbologyDialog = false }
            )
        }
    }
}