package com.zebra.ai.palletchecker.presentation.ui.compose.screens.settingsscreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zebra.ai.palletchecker.R
import com.zebra.ai.palletchecker.domain.enums.ModelInput
import com.zebra.ai.palletchecker.domain.enums.ProcessorType
import com.zebra.ai.palletchecker.domain.enums.Resolution
import com.zebra.ai.palletchecker.domain.enums.SettingsMode
import com.zebra.ai.palletchecker.domain.model.BarcodeSymbology
import com.zebra.ai.palletchecker.presentation.ui.compose.components.ZebraText
import com.zebra.ai.palletchecker.presentation.ui.compose.screens.settingsscreen.bottomsheet.BarcodeSymbologyBottomSheet
import com.zebra.ai.palletchecker.presentation.ui.compose.screens.settingsscreen.bottomsheet.InferenceBottomSheet
import com.zebra.ai.palletchecker.presentation.ui.compose.screens.settingsscreen.bottomsheet.ModelInputSizeBottomSheet
import com.zebra.ai.palletchecker.presentation.ui.compose.screens.settingsscreen.bottomsheet.ResolutionBottomSheet
import com.zebra.ai.palletchecker.presentation.ui.theme.AppColors
import com.zebra.ai.palletchecker.presentation.ui.theme.AppTextStyles
import com.zebra.ai.palletchecker.presentation.ui.theme.Black
import com.zebra.ai.palletchecker.presentation.ui.theme.WhiteLight
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
    val settings by settingsViewModel.wandSettings.collectAsState()
    var mode by remember { mutableStateOf(SettingsMode.SNAP) }
    var showModelInputSizeDialog by remember { mutableStateOf(false) }
    var showResolutionDialog by remember { mutableStateOf(false) }
    var showInferenceDialog by remember { mutableStateOf(false) }
    var showBarcodeSymbologyDialog by remember { mutableStateOf(false) }
    var settingsClickCounter by remember { mutableStateOf(0) }
    val MAX_CLICK_SETTINGS = 5
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
                    modifier = Modifier.clickable{
                        if(!showSnapWandSettings) {
                            settingsClickCounter++
                            if (settingsClickCounter > MAX_CLICK_SETTINGS) {
                                showSnapWandSettings = true
                            }
                        }
                    }
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
            actions = {
                if(showSnapWandSettings) {
                    Row(
                        modifier = Modifier
                            .wrapContentSize()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            modifier = Modifier
                                .wrapContentWidth()
                                .height(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (mode == SettingsMode.SNAP) Color.White else Color.Black,
                                contentColor = White
                            ),
                            border = BorderStroke(1.dp, color = WhiteLight),
                            shape = RoundedCornerShape(5.dp, 0.dp, 0.dp, 5.dp),
                            onClick = { mode = SettingsMode.SNAP }) {
                            Text(
                                text = "SNAP",
                                color = if (mode == SettingsMode.SNAP) Black else White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp
                            )
                        }

                        Button(
                            modifier = Modifier
                                .wrapContentWidth()
                                .height(28.dp)
                                .padding(1.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (mode == SettingsMode.WAND) Color.White else Color.Black,
                                contentColor = White
                            ),
                            border = BorderStroke(1.dp, color = WhiteLight),
                            shape = RoundedCornerShape(0.dp, 5.dp, 5.dp, 0.dp),
                            onClick = { mode = SettingsMode.WAND }) {
                            Text(
                                text = "WAND",
                                color = if (mode == SettingsMode.WAND) Black else White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = darkBackground
            )
        )


        if (mode == SettingsMode.SNAP) {
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
                    }

                },
                expectedBoxesToAudit = expectedBoxesToAudit
            )
        } else {
            WandSettingsScreen(
                settingsViewModel,
                onBackClick,
                onBarcodeModelVersionsClick,
                onResetToDefaultClick,
                onBackPressed,
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
                    }

                }
            )
        }
    }


    if (showModelInputSizeDialog) {
        settingsViewModel.let { viewModel ->
            ModelInputSizeBottomSheet(
                settingsViewModel = viewModel,
                currentSelection = settings.modelInput ?: ModelInput.SMALL_640,
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