// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.presentation.ui.compose.screens.settingsscreen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.zebra.ai.barcodefinder.R
import com.zebra.ai.barcodefinder.presentation.enums.ButtonType
import com.zebra.ai.barcodefinder.domain.enums.ModelInput
import com.zebra.ai.barcodefinder.domain.enums.ProcessorType
import com.zebra.ai.barcodefinder.domain.enums.Resolution
import com.zebra.ai.barcodefinder.domain.model.BarcodeSymbology
import com.zebra.ai.barcodefinder.presentation.ui.compose.components.ZebraButton
import com.zebra.ai.barcodefinder.presentation.ui.compose.components.ZebraText
import com.zebra.ai.barcodefinder.presentation.ui.compose.screens.settingsscreen.bottomsheet.BarcodeSymbologyBottomSheet
import com.zebra.ai.barcodefinder.presentation.ui.compose.screens.settingsscreen.bottomsheet.InferenceBottomSheet
import com.zebra.ai.barcodefinder.presentation.ui.compose.screens.settingsscreen.bottomsheet.ModelInputSizeBottomSheet
import com.zebra.ai.barcodefinder.presentation.ui.compose.screens.settingsscreen.bottomsheet.ResolutionBottomSheet
import com.zebra.ai.barcodefinder.presentation.ui.compose.screens.settingsscreen.components.RadioButtonOption
import com.zebra.ai.barcodefinder.presentation.ui.compose.screens.settingsscreen.components.SettingsCard
import com.zebra.ai.barcodefinder.presentation.ui.compose.screens.settingsscreen.components.SwitchOption
import com.zebra.ai.barcodefinder.presentation.ui.theme.AppColors
import com.zebra.ai.barcodefinder.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.presentation.ui.theme.AppTextStyles
import com.zebra.ai.barcodefinder.presentation.ui.theme.borderPrimaryMain
import com.zebra.ai.barcodefinder.presentation.ui.theme.darkBackground
import com.zebra.ai.barcodefinder.presentation.ui.theme.mainInverse
import com.zebra.ai.barcodefinder.presentation.ui.theme.settingsLazyColumnBackground
import com.zebra.ai.barcodefinder.presentation.ui.theme.textBlack
import com.zebra.ai.barcodefinder.presentation.ui.theme.white
import com.zebra.ai.barcodefinder.presentation.viewmodel.SettingsViewModel

/**
 * Displays the Settings screen for configuring barcode finder options.
 * Allows users to adjust model input size, resolution, inference settings, and barcode symbology.
 * Handles navigation, reset to default, and model version actions.
 *
 * @param settingsViewModel The ViewModel providing settings state and logic
 * @param onBackClick Callback for navigation back
 * @param onBarcodeModelVersionsClick Callback for viewing barcode model versions
 * @param onResetToDefaultClick Callback for resetting settings to default
 * @param onBackPressed Callback for handling back press
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onBackClick: () -> Unit = {},
    onBarcodeModelVersionsClick: () -> Unit = {},
    onResetToDefaultClick: () -> Unit = {},
    onBackPressed: () -> Unit = {}
) {
    val settings by settingsViewModel.settings.collectAsState()
    var modelInputSizeExpanded by remember { mutableStateOf(false) }
    var resolutionExpanded by remember { mutableStateOf(false) }
    var inferenceExpanded by remember { mutableStateOf(false) }
    var barcodeSymbologyExpanded by remember { mutableStateOf(false) }

    // State for bottom dialog
    var showModelInputSizeDialog by remember { mutableStateOf(false) }
    var showResolutionDialog by remember { mutableStateOf(false) }
    var showInferenceDialog by remember { mutableStateOf(false) }
    var showBarcodeSymbologyDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(white)
            .semantics{contentDescription="SettingsScreen"}
    ) {
        // Top App Bar - Using same color as NavBarScreen header
        TopAppBar(
            title = {
                ZebraText(
                    textValue = stringResource(id = R.string.setting_screen_barcode_recognizer_title),
                    style = AppTextStyles.TitleTextLight,
                    textColor = AppColors.TextWhite
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.setting_screen_back_icon_description),
                        tint = white // Changed from Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = darkBackground // Same color as NavBarScreen header
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(settingsLazyColumnBackground) // Light gray background
                .padding(
                    start = AppDimensions.dimension_4dp,
                    top = AppDimensions.dimension_12dp,
                    end = AppDimensions.dimension_4dp,
                    bottom = AppDimensions.dimension_24dp
                ),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.dimension_8dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Model Input Size Section
            item {
                SettingsCard(
                    title = stringResource(id = R.string.setting_screen_model_input_size_title),
                    isExpanded = modelInputSizeExpanded,
                    onExpandToggle = { modelInputSizeExpanded = !modelInputSizeExpanded }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        // Description section with light gray background
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(
                                AppDimensions.dimension_10dp,
                                Alignment.Start
                            ),
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier
                                .background(mainInverse) // Light gray background, no rounded corners
                                .padding(
                                    start = AppDimensions.dimension_12dp,
                                    top = AppDimensions.dimension_16dp,
                                    end = AppDimensions.dimension_12dp,
                                    bottom = AppDimensions.dimension_16dp
                                )
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(
                                    AppDimensions.dimension_10dp,
                                    Alignment.CenterVertically
                                ),
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(AppDimensions.zeroPadding)
                            ) {
                                Text(
                                    buildAnnotatedString {
                                        withStyle(
                                            style = SpanStyle(
                                                fontWeight = FontWeight(
                                                    AppDimensions.fontWeight400
                                                )
                                            )
                                        ) {
                                            append(stringResource(id = R.string.setting_screen_model_input_size_description_part1))
                                        }
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                            append(stringResource(id = R.string.setting_screen_model_input_size_description_part2))
                                        }
                                        withStyle(
                                            style = SpanStyle(
                                                fontWeight = FontWeight(
                                                    AppDimensions.fontWeight400
                                                )
                                            )
                                        ) {
                                            append(stringResource(id = R.string.setting_screen_model_input_size_description_part3))
                                        }
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                            append(stringResource(id = R.string.setting_screen_note_label))
                                        }
                                        withStyle(
                                            style = SpanStyle(
                                                fontWeight = FontWeight(
                                                    AppDimensions.fontWeight400
                                                )
                                            )
                                        ) {
                                            append(stringResource(id = R.string.setting_screen_model_input_size_description_part4))
                                        }
                                    },
                                    color = textBlack, // Changed from Color.Black
                                    fontSize = AppDimensions.dialogTextFontSizeSmall,
                                    lineHeight = AppDimensions.BulletLineHeight,
                                    modifier = Modifier.padding(AppDimensions.zeroPadding) // Ensure no padding by default
                                )
                                ZebraText(
                                    textValue = stringResource(id = R.string.setting_screen_more_button),
                                    textColor = borderPrimaryMain,
                                    fontSize = AppDimensions.dialogTextFontSizeSmall,
                                    modifier = Modifier.clickable {
                                        showModelInputSizeDialog = true
                                    }
                                )
                            }
                        }
                        // Radio buttons section with white background and proper padding
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics{contentDescription="SettingsOptionsRadioButtons: Model Input Size"}
                                .padding(
                                    horizontal = AppDimensions.dimension_2dp,
                                    vertical = AppDimensions.zeroPadding
                                ),
                            verticalArrangement = Arrangement.spacedBy(AppDimensions.dimension_12dp)
                        ) {
                            RadioButtonOption(
                                title = stringResource(id = R.string.setting_screen_model_input_640_title),
                                subtitle = stringResource(id = R.string.setting_screen_model_input_640_subtitle),
                                selected = settings.modelInput == ModelInput.SMALL_640,
                                onSelected = {
                                    settingsViewModel.updateModelInput(ModelInput.SMALL_640)
                                }
                            )
                            RadioButtonOption(
                                title = stringResource(id = R.string.setting_screen_model_input_1280_title),
                                subtitle = stringResource(id = R.string.setting_screen_model_input_1280_subtitle),
                                selected = settings.modelInput == ModelInput.MEDIUM_1280,
                                onSelected = {
                                    settingsViewModel.updateModelInput(ModelInput.MEDIUM_1280)
                                }
                            )
                            RadioButtonOption(
                                title = stringResource(id = R.string.setting_screen_model_input_1600_title),
                                subtitle = stringResource(id = R.string.setting_screen_model_input_1600_subtitle),
                                selected = settings.modelInput == ModelInput.LARGE_1600,
                                onSelected = {
                                    settingsViewModel.updateModelInput(ModelInput.LARGE_1600)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(AppDimensions.dimension_16dp))
                    }
                }
            }


            // Settings Resolution Section
            item {
                SettingsCard(
                    title = stringResource(id = R.string.setting_screen_resolution_title),
                    isExpanded = resolutionExpanded,
                    onExpandToggle = { resolutionExpanded = !resolutionExpanded }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        // Description section with light gray background
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(
                                AppDimensions.dimension_10dp,
                                Alignment.Start
                            ),
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier
                                .background(mainInverse) // Light gray background, no rounded corners
                                .padding(
                                    start = AppDimensions.dimension_12dp,
                                    top = AppDimensions.dimension_16dp,
                                    end = AppDimensions.dimension_12dp,
                                    bottom = AppDimensions.dimension_16dp
                                )
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(
                                    AppDimensions.dimension_10dp,
                                    Alignment.CenterVertically
                                ),
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    buildAnnotatedString {
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                                            append(stringResource(id = R.string.setting_screen_resolution_description_part1))
                                        }
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                            append(stringResource(id = R.string.setting_screen_resolution_description_part2))
                                        }
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                                            append(stringResource(id = R.string.setting_screen_resolution_description_part3))
                                        }
                                    },
                                    color = textBlack, // Changed from Color.Black
                                    fontSize = AppDimensions.dialogTextFontSizeSmall,
                                    lineHeight = AppDimensions.BulletLineHeight
                                )
                                ZebraText(
                                    textValue = stringResource(id = R.string.setting_screen_more_button),
                                    textColor = borderPrimaryMain,
                                    fontSize = AppDimensions.dialogTextFontSizeSmall,
                                    modifier = Modifier.clickable {
                                        showResolutionDialog = true
                                    }
                                )

                            }
                        }

                        // Radio buttons section with white background and proper padding
                        Column(
                            modifier = Modifier.padding(
                                horizontal = AppDimensions.dimension_2dp,
                                vertical = AppDimensions.zeroPadding
                            )
                                .semantics{contentDescription="SettingsOptionsRadioButtons: Resolution"},
                            verticalArrangement = Arrangement.spacedBy(AppDimensions.dimension_12dp)
                        ) {
                            RadioButtonOption(
                                title = stringResource(id = R.string.setting_screen_resolution_1mp_title), // Replacing "1MP (1280 × 720)"
                                subtitle = stringResource(id = R.string.setting_screen_resolution_1mp_subtitle), // Replacing
                                selected = settings.resolution == Resolution.ONE_MP,
                                onSelected = {
                                    settingsViewModel.updateResolution(Resolution.ONE_MP)
                                }
                            )
                            RadioButtonOption(
                                title = stringResource(id = R.string.setting_screen_resolution_2mp_title), // Replacing "2MP (1920 × 1080)"
                                subtitle = stringResource(id = R.string.setting_screen_resolution_2mp_subtitle), // Replacing
                                selected = settings.resolution == Resolution.TWO_MP,
                                onSelected = {
                                    settingsViewModel.updateResolution(Resolution.TWO_MP)
                                }
                            )
                            RadioButtonOption(
                                title = stringResource(id = R.string.setting_screen_resolution_4mp_title), // Replacing "4MP (2688 × 1512)"
                                subtitle = stringResource(id = R.string.setting_screen_resolution_4mp_subtitle), // Replacing
                                selected = settings.resolution == Resolution.FOUR_MP,
                                onSelected = {
                                    settingsViewModel.updateResolution(Resolution.FOUR_MP)
                                }
                            )
                            RadioButtonOption(
                                title = stringResource(id = R.string.setting_screen_resolution_8mp_title), // Replacing "8MP (3840 × 2160)"
                                subtitle = stringResource(id = R.string.setting_screen_resolution_8mp_subtitle), // Replacing
                                selected = settings.resolution == Resolution.EIGHT_MP,
                                onSelected = {
                                    settingsViewModel.updateResolution(Resolution.EIGHT_MP)
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(AppDimensions.dimension_16dp))
                    }
                }
            }

            // Settings Inference Section
            item {
                SettingsCard(
                    title = stringResource(id = R.string.setting_screen_inference_title),
                    isExpanded = inferenceExpanded,
                    onExpandToggle = { inferenceExpanded = !inferenceExpanded },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(
                                AppDimensions.dimension_10dp,
                                Alignment.Start
                            ),
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier
                                .background(mainInverse) // Light gray background, no rounded corners
                                .padding(
                                    start = AppDimensions.dimension_12dp,
                                    top = AppDimensions.dimension_16dp,
                                    end = AppDimensions.dimension_12dp,
                                    bottom = AppDimensions.dimension_16dp
                                )
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(
                                    AppDimensions.dimension_10dp,
                                    Alignment.CenterVertically
                                ),
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // can't add as ZebraText
                                Text(
                                    buildAnnotatedString {
                                        withStyle(
                                            style = SpanStyle(
                                                fontWeight = FontWeight(
                                                    AppDimensions.fontWeight400
                                                )
                                            )
                                        ) {
                                            append(stringResource(id = R.string.setting_screen_inference_description1))
                                        }
                                        withStyle(
                                            style = SpanStyle(
                                                fontWeight = FontWeight(
                                                    AppDimensions.fontWeight400
                                                )
                                            )
                                        ) {
                                            append(stringResource(id = R.string.setting_screen_inference_description2))
                                        }
                                    },
                                    fontSize = AppDimensions.dialogTextFontSizeSmall,
                                    color = textBlack, // Changed from Color.Black
                                    lineHeight = AppDimensions.BulletLineHeight
                                )
                                ZebraText(
                                    textValue = stringResource(id = R.string.setting_screen_more_button),
                                    textColor = borderPrimaryMain,
                                    fontSize = AppDimensions.dialogTextFontSizeSmall,
                                    modifier = Modifier.clickable {
                                        showInferenceDialog = true
                                    }
                                )
                            }
                        }

                        // Radio buttons section with white background and proper padding
                        Column(
                            modifier = Modifier.padding(
                                horizontal = AppDimensions.dimension_2dp,
                                vertical = AppDimensions.zeroPadding
                            )
                                .semantics{contentDescription="SettingsOptionsRadioButtons: Inference (processor) Type"},
                            verticalArrangement = Arrangement.spacedBy(AppDimensions.dimension_12dp)
                        ) {
                            RadioButtonOption(
                                title = stringResource(id = R.string.setting_screen_auto), // Replacing "DSP (Digital Signal Processor)"
                                subtitle = stringResource(id = R.string.setting_screen_auto_select_best_available_inference_type), // Replacing "Best Choice"
                                selected = settings.processorType == ProcessorType.AUTO,
                                onSelected = {
                                    settingsViewModel.updateProcessorType(ProcessorType.AUTO)
                                }
                            )
                            RadioButtonOption(
                                title = stringResource(id = R.string.setting_screen_inference_dsp_title), // Replacing "DSP (Digital Signal Processor)"
                                subtitle = stringResource(id = R.string.setting_screen_inference_dsp_subtitle), // Replacing "Best Choice"
                                selected = settings.processorType == ProcessorType.DSP,
                                onSelected = {
                                    settingsViewModel.updateProcessorType(ProcessorType.DSP)
                                }
                            )
                            RadioButtonOption(
                                title = stringResource(id = R.string.setting_screen_inference_gpu_title), // Replacing "GPU (Graphics Processing Unit)"
                                subtitle = stringResource(id = R.string.setting_screen_inference_gpu_subtitle),
                                selected = settings.processorType == ProcessorType.GPU,
                                onSelected = {
                                    settingsViewModel.updateProcessorType(ProcessorType.GPU)
                                }
                            )
                            RadioButtonOption(
                                title = stringResource(id = R.string.setting_screen_inference_cpu_title), // Replacing "CPU (Central Processing Unit)"
                                subtitle = stringResource(id = R.string.setting_screen_inference_cpu_subtitle),
                                selected = settings.processorType == ProcessorType.CPU,
                                onSelected = {
                                    settingsViewModel.updateProcessorType(ProcessorType.CPU)
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(AppDimensions.dimension_16dp))
                    }
                }
            }

            // --- Barcode Symbology Section ---
            item {
                SettingsCard(
                    title = stringResource(id = R.string.setting_screen_barcode_symbologies_title),
                    isExpanded = barcodeSymbologyExpanded,
                    onExpandToggle = { barcodeSymbologyExpanded = !barcodeSymbologyExpanded }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(
                                AppDimensions.dimension_10dp,
                                Alignment.Start
                            ),
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier
                                .background(mainInverse) // Light gray background, no rounded corners
                                .padding(
                                    start = AppDimensions.dimension_12dp,
                                    top = AppDimensions.dimension_16dp,
                                    end = AppDimensions.dimension_12dp,
                                    bottom = AppDimensions.dimension_16dp
                                )
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(
                                    AppDimensions.dimension_10dp,
                                    Alignment.CenterVertically
                                ),
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    buildAnnotatedString {
                                        withStyle(style = SpanStyle(fontWeight = FontWeight(400))) {
                                            append(stringResource(id = R.string.setting_screen_barcode_symbology_description1))
                                        }
                                        withStyle(style = SpanStyle(fontWeight = FontWeight(700))) {
                                            append(stringResource(id = R.string.setting_screen_barcode_symbology_description2))
                                        }
                                        withStyle(style = SpanStyle(fontWeight = FontWeight(400))) {
                                            append(stringResource(id = R.string.setting_screen_barcode_symbology_description3))
                                        }
                                    },
                                    color = AppColors.TextBlack,
                                    fontSize = AppDimensions.dialogTextFontSizeSmall,
                                    lineHeight = AppDimensions.genaralLineHight_20sp
                                )

                                Row(modifier = Modifier.padding(top = AppDimensions.spacerHeight8)) {
                                    Text(
                                        buildAnnotatedString {
                                            withStyle(style = SpanStyle(fontWeight = FontWeight(700))) {
                                                append(stringResource(id = R.string.setting_screen_note_label))
                                            }
                                            withStyle(style = SpanStyle(fontWeight = FontWeight(400))) {
                                                append(stringResource(id = R.string.setting_screen_symbology_optimization_info))
                                            }
                                        },
                                        color = AppColors.TextBlack,
                                        fontSize = AppDimensions.dialogTextFontSizeSmall,
                                        lineHeight = AppDimensions.BulletLineHeight
                                    )
                                }
                                ZebraText(
                                    textValue = stringResource(id = R.string.setting_screen_more_button),
                                    textColor = borderPrimaryMain,
                                    fontSize = AppDimensions.dialogTextFontSizeSmall,
                                    modifier = Modifier.clickable {
                                        showBarcodeSymbologyDialog = true
                                    }
                                )
                            }
                        }

                        // Switch options section with white background - showing all symbologies
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = white), // Changed from Color.White
                            elevation = CardDefaults.cardElevation(defaultElevation = AppDimensions.zeroPadding),
                            shape = RoundedCornerShape(AppDimensions.zeroPadding)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics{contentDescription="SettingsOptionsRadioButtons: Barcode Symbology Types"}
                                    .padding(vertical = AppDimensions.dimension_8dp),
                                verticalArrangement = Arrangement.spacedBy(AppDimensions.zeroPadding)
                            ) {
                                SwitchOption(
                                    title = "Australian Postal",
                                    checked = settings.barcodeSymbology.australianPostal,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Australian Postal",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Aztec",
                                    checked = settings.barcodeSymbology.aztec,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Aztec",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Canadian Postal",
                                    checked = settings.barcodeSymbology.canadianPostal,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Canadian Postal",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Chinese 2of5",
                                    checked = settings.barcodeSymbology.chinese2of5,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Chinese 2of5",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Codabar",
                                    checked = settings.barcodeSymbology.codabar,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Codabar",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Code 11",
                                    checked = settings.barcodeSymbology.code11,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Code 11",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Code 39",
                                    checked = settings.barcodeSymbology.code39,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Code 39",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Code 93",
                                    checked = settings.barcodeSymbology.code93,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Code 93",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Code 128",
                                    checked = settings.barcodeSymbology.code128,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Code 128",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Composite AB",
                                    checked = settings.barcodeSymbology.compositeAB,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Composite AB",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Composite C",
                                    checked = settings.barcodeSymbology.compositeC,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Composite C",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "D2of5",
                                    checked = settings.barcodeSymbology.d2of5,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "D2of5",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Data Matrix",
                                    checked = settings.barcodeSymbology.datamatrix,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "DataMatrix",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "DotCode",
                                    checked = settings.barcodeSymbology.dotcode,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "DotCode",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Dutch Postal",
                                    checked = settings.barcodeSymbology.dutchPostal,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Dutch Postal",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "EAN-8",
                                    checked = settings.barcodeSymbology.ean8,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "EAN-8",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "EAN-13",
                                    checked = settings.barcodeSymbology.ean13,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "EAN-13",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Finnish Postal 4S",
                                    checked = settings.barcodeSymbology.finnishPostal4s,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Finnish Postal 4S",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Grid Matrix",
                                    checked = settings.barcodeSymbology.gridMatrix,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Grid Matrix",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "GS1 DataBar",
                                    checked = settings.barcodeSymbology.gs1Databar,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "GS1 DataBar",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "GS1 DataBar Expanded",
                                    checked = settings.barcodeSymbology.gs1DatabarExpanded,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "GS1 DataBar Expanded",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "GS1 DataBar Limited",
                                    checked = settings.barcodeSymbology.gs1DatabarLim,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "GS1 DataBar Limited",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "GS1 DataMatrix",
                                    checked = settings.barcodeSymbology.gs1Datamatrix,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "GS1 DataMatrix",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "GS1 QR Code",
                                    checked = settings.barcodeSymbology.gs1Qrcode,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "GS1 QR Code",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Han xin",
                                    checked = settings.barcodeSymbology.hanxin,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Hanxin",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "I2of5",
                                    checked = settings.barcodeSymbology.i2of5,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "I2of5",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Japanese Postal",
                                    checked = settings.barcodeSymbology.japanesePostal,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Japanese Postal",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Korean 3of5",
                                    checked = settings.barcodeSymbology.korean3of5,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Korean 3of5",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Mail mark",
                                    checked = settings.barcodeSymbology.mailmark,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Mailmark",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Matrix 2of5",
                                    checked = settings.barcodeSymbology.matrix2of5,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Matrix 2of5",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "MaxiCode",
                                    checked = settings.barcodeSymbology.maxicode,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "MaxiCode",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "MicroPDF",
                                    checked = settings.barcodeSymbology.micropdf,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "MicroPDF",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "MicroQR",
                                    checked = settings.barcodeSymbology.microqr,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "MicroQR",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "MSI",
                                    checked = settings.barcodeSymbology.msi,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "MSI",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "PDF 417",
                                    checked = settings.barcodeSymbology.pdf417,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "PDF417",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "QR Code",
                                    checked = settings.barcodeSymbology.qrcode,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "QR Code",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "TLC 39",
                                    checked = settings.barcodeSymbology.tlc39,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "TLC39",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Trioptic 39",
                                    checked = settings.barcodeSymbology.trioptic39,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Trioptic 39",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "UK Postal",
                                    checked = settings.barcodeSymbology.ukPostal,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "UK Postal",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "UPC-A",
                                    checked = settings.barcodeSymbology.upcA,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "UPC-A",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "UPCE-0",
                                    checked = settings.barcodeSymbology.upcE,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "UPC-E",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "UPCE-1",
                                    checked = settings.barcodeSymbology.upce1,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "UPC-E1",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "USPlanet",
                                    checked = settings.barcodeSymbology.usplanet,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "US Planet",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "USPostnet",
                                    checked = settings.barcodeSymbology.uspostnet,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "US Postnet",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "US4State",
                                    checked = settings.barcodeSymbology.us4state,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "US 4-State",
                                            it
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "US4State FICS",
                                    checked = settings.barcodeSymbology.us4stateFics,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "US 4-State FICS",
                                            it
                                        )
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(AppDimensions.dimension_16dp))
                    }
                }
            }

            // Reset to Default Settings Button - Always visible at bottom
            item {
                Spacer(modifier = Modifier.height(AppDimensions.dimension_24dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    ZebraButton(
                        buttonType = ButtonType.Text,
                        onClick = onResetToDefaultClick,
                        text = stringResource(id = R.string.setting_screen_reset_to_default),
                        textColor = borderPrimaryMain,
                        backgroundColor = settingsLazyColumnBackground
                    )
                }

                Spacer(modifier = Modifier.height(AppDimensions.dimension_24dp))
            }
        }
    }

    // Show barcode Model Input Size screen when requested
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

    // Show SettingsResolution screen when requested
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

    // Show SettingsInference Type screen when requested
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

    // Show barcode symbology screen when requested
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

    BackHandler {
        onBackPressed()
    }

}

// Remove the old ModelInputSizeOption composable as it's now replaced by RadioButtonOption
@Preview(showBackground = true, name = "AI Vision SDK Settings Screen")
@Composable
fun SettingsScreenPreview() {
    MaterialTheme {
        SettingsScreen(
            settingsViewModel = TODO(),
            onBackClick = TODO(),
            onResetToDefaultClick = TODO(),
            onBackPressed = TODO()
        )
    }
}
