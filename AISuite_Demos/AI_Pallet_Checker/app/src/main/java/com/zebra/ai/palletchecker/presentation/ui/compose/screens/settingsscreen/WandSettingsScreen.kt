package com.zebra.ai.palletchecker.presentation.ui.compose.screens.settingsscreen


// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.


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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.zebra.ai.palletchecker.R
import com.zebra.ai.palletchecker.domain.enums.ModelInput
import com.zebra.ai.palletchecker.domain.enums.ProcessorType
import com.zebra.ai.palletchecker.domain.enums.Resolution
import com.zebra.ai.palletchecker.domain.enums.SettingsMode
import com.zebra.ai.palletchecker.presentation.enums.ButtonType
import com.zebra.ai.palletchecker.presentation.ui.compose.components.ZebraButton
import com.zebra.ai.palletchecker.presentation.ui.compose.components.ZebraText
import com.zebra.ai.palletchecker.presentation.ui.compose.screens.settingsscreen.components.RadioButtonOption
import com.zebra.ai.palletchecker.presentation.ui.compose.screens.settingsscreen.components.SettingsCard
import com.zebra.ai.palletchecker.presentation.ui.compose.screens.settingsscreen.components.SwitchOption
import com.zebra.ai.palletchecker.presentation.ui.theme.AppColors
import com.zebra.ai.palletchecker.presentation.ui.theme.AppDimensions
import com.zebra.ai.palletchecker.presentation.ui.theme.borderPrimaryMain
import com.zebra.ai.palletchecker.presentation.ui.theme.mainInverse
import com.zebra.ai.palletchecker.presentation.ui.theme.settingsLazyColumnBackground
import com.zebra.ai.palletchecker.presentation.ui.theme.textBlack
import com.zebra.ai.palletchecker.presentation.ui.theme.textGrey
import com.zebra.ai.palletchecker.presentation.ui.theme.white
import com.zebra.ai.palletchecker.presentation.viewmodel.SettingsViewModel

/**
 * Displays the Settings screen for configuring pallet checker options.
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
fun WandSettingsScreen(
    settingsViewModel: SettingsViewModel,
    onBackClick: () -> Unit = {},
    onBarcodeModelVersionsClick: () -> Unit = {},
    onResetToDefaultClick: () -> Unit = {},
    onBackPressed: () -> Unit = {},
    showBottomSheet: (Int) -> Unit = {}
) {
    val settings by settingsViewModel.wandSettings.collectAsState()
    var modelInputSizeExpanded by remember { mutableStateOf(false) }
    var resolutionExpanded by remember { mutableStateOf(false) }
    var inferenceExpanded by remember { mutableStateOf(false) }
    var barcodeSymbologyExpanded by remember { mutableStateOf(false) }
    var inputSourceExpanded by remember { mutableStateOf(false) }
    var liveOverlayExpanded by remember { mutableStateOf(false) }
    var barcodeDecoderOptionsExpanded by remember { mutableStateOf(false) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(white)
            .semantics { contentDescription = "SettingsScreen" }
    ) {

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(settingsLazyColumnBackground)
                .padding(
                    start = AppDimensions.dimension_4dp,
                    top = AppDimensions.dimension_12dp,
                    end = AppDimensions.dimension_4dp,
                    bottom = AppDimensions.dimension_24dp
                ),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.dimension_8dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(
                                AppDimensions.dimension_10dp,
                                Alignment.Start
                            ),
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier
                                .background(mainInverse)
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
                                    color = textBlack,
                                    fontSize = AppDimensions.dialogTextFontSizeSmall,
                                    lineHeight = AppDimensions.BulletLineHeight,
                                    modifier = Modifier.padding(AppDimensions.zeroPadding) // Ensure no padding by default
                                )
                                ZebraText(
                                    textValue = stringResource(id = R.string.setting_screen_more_button),
                                    textColor = borderPrimaryMain,
                                    fontSize = AppDimensions.dialogTextFontSizeSmall,
                                    modifier = Modifier.clickable {
                                        showBottomSheet(0)
                                    }
                                )
                            }
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics {
                                    contentDescription =
                                        "SettingsOptionsRadioButtons: Model Input Size"
                                }
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
                                    settingsViewModel.updateModelInput(
                                        ModelInput.SMALL_640,
                                        SettingsMode.WAND
                                    )
                                }
                            )
                            RadioButtonOption(
                                title = stringResource(id = R.string.setting_screen_model_input_1280_title),
                                subtitle = stringResource(id = R.string.setting_screen_model_input_1280_subtitle),
                                selected = settings.modelInput == ModelInput.MEDIUM_1280,
                                onSelected = {
                                    settingsViewModel.updateModelInput(
                                        ModelInput.MEDIUM_1280,
                                        SettingsMode.WAND
                                    )
                                }
                            )
                            RadioButtonOption(
                                title = stringResource(id = R.string.setting_screen_model_input_1600_title),
                                subtitle = stringResource(id = R.string.setting_screen_model_input_1600_subtitle),
                                selected = settings.modelInput == ModelInput.LARGE_1600,
                                onSelected = {
                                    settingsViewModel.updateModelInput(
                                        ModelInput.LARGE_1600,
                                        SettingsMode.WAND
                                    )
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(AppDimensions.dimension_16dp))
                    }
                }
            }


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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(
                                AppDimensions.dimension_10dp,
                                Alignment.Start
                            ),
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier
                                .background(mainInverse)
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
                                    color = textBlack,
                                    fontSize = AppDimensions.dialogTextFontSizeSmall,
                                    lineHeight = AppDimensions.BulletLineHeight
                                )
                                ZebraText(
                                    textValue = stringResource(id = R.string.setting_screen_more_button),
                                    textColor = borderPrimaryMain,
                                    fontSize = AppDimensions.dialogTextFontSizeSmall,
                                    modifier = Modifier.clickable {
                                        showBottomSheet(1)
                                    }
                                )

                            }
                        }

                        Column(
                            modifier = Modifier
                                .padding(
                                    horizontal = AppDimensions.dimension_2dp,
                                    vertical = AppDimensions.zeroPadding
                                )
                                .semantics {
                                    contentDescription = "SettingsOptionsRadioButtons: Resolution"
                                },
                            verticalArrangement = Arrangement.spacedBy(AppDimensions.dimension_12dp)
                        ) {

                            val wandResolutions by settingsViewModel.supportedWandResolutions.collectAsState()
                            wandResolutions.forEach { camRes ->
                                RadioButtonOption(
                                    title = camRes.label,
                                    subtitle = camRes.aspectRatio,
                                    selected = settings.resolution == Resolution.MAX
                                            && settings.customResolutionWidth == camRes.width
                                            && settings.customResolutionHeight == camRes.height,
                                    onSelected = {
                                        settingsViewModel.updateCustomResolution(
                                            camRes.width,
                                            camRes.height,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(AppDimensions.dimension_16dp))
                    }
                }
            }

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
                                .background(mainInverse)
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
                                        showBottomSheet(2)
                                    }
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .padding(
                                    horizontal = AppDimensions.dimension_2dp,
                                    vertical = AppDimensions.zeroPadding
                                )
                                .semantics {
                                    contentDescription =
                                        "SettingsOptionsRadioButtons: Inference (processor) Type"
                                },
                            verticalArrangement = Arrangement.spacedBy(AppDimensions.dimension_12dp)
                        ) {
                            RadioButtonOption(
                                title = stringResource(id = R.string.setting_screen_auto),
                                subtitle = stringResource(id = R.string.setting_screen_auto_select_best_available_inference_type),
                                selected = settings.processorType == ProcessorType.AUTO,
                                onSelected = {
                                    settingsViewModel.updateProcessorType(
                                        ProcessorType.AUTO,
                                        SettingsMode.WAND
                                    )
                                }
                            )
                            RadioButtonOption(
                                title = stringResource(id = R.string.setting_screen_inference_dsp_title),
                                subtitle = stringResource(id = R.string.setting_screen_inference_dsp_subtitle),
                                selected = settings.processorType == ProcessorType.DSP,
                                onSelected = {
                                    settingsViewModel.updateProcessorType(
                                        ProcessorType.DSP,
                                        SettingsMode.WAND
                                    )
                                }
                            )
                            RadioButtonOption(
                                title = stringResource(id = R.string.setting_screen_inference_gpu_title),
                                subtitle = stringResource(id = R.string.setting_screen_inference_gpu_subtitle),
                                selected = settings.processorType == ProcessorType.GPU,
                                onSelected = {
                                    settingsViewModel.updateProcessorType(
                                        ProcessorType.GPU,
                                        SettingsMode.WAND
                                    )
                                }
                            )
                            RadioButtonOption(
                                title = stringResource(id = R.string.setting_screen_inference_cpu_title), // Replacing "CPU (Central Processing Unit)"
                                subtitle = stringResource(id = R.string.setting_screen_inference_cpu_subtitle),
                                selected = settings.processorType == ProcessorType.CPU,
                                onSelected = {
                                    settingsViewModel.updateProcessorType(
                                        ProcessorType.CPU,
                                        SettingsMode.WAND
                                    )
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(AppDimensions.dimension_16dp))
                    }
                }
            }

            item {
                SettingsCard(
                    title = stringResource(id = R.string.setting_screen_barcode_decoder_options_title),
                    isExpanded = barcodeDecoderOptionsExpanded,
                    onExpandToggle = {
                        barcodeDecoderOptionsExpanded = !barcodeDecoderOptionsExpanded
                    }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = white),
                            elevation = CardDefaults.cardElevation(defaultElevation = AppDimensions.zeroPadding),
                            shape = RoundedCornerShape(AppDimensions.zeroPadding)
                        ) {

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                ,
                                horizontalAlignment = Alignment.Start
                            ) {
                                SwitchOption(
                                    title = stringResource(id = R.string.setting_screen_ai_barcode_decode_title),
                                    subtitle = "",
                                    checked = settings.enableAIbarcodeDecode,
                                    onCheckedChange = settingsViewModel::updateAiBarcodeDecodeEnabled
                                )

                                Spacer(modifier = Modifier.height(AppDimensions.dimension_2dp))
                                Text(modifier = Modifier.fillMaxWidth().padding(
                                    horizontal = AppDimensions.dimension_16dp,
                                    vertical = AppDimensions.dimension_8dp),
                                    text = stringResource(id = R.string.setting_screen_barcode_decoder_options_description),
                                    fontSize = AppDimensions.dialogTextFontSizeExtraSmall,
                                    color = textGrey,
                                )
                            }

                        }

                        Spacer(modifier = Modifier.height(AppDimensions.dimension_16dp))
                    }
                }
            }
            
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
                                        showBottomSheet(3)
                                    }
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = white),
                            elevation = CardDefaults.cardElevation(defaultElevation = AppDimensions.zeroPadding),
                            shape = RoundedCornerShape(AppDimensions.zeroPadding)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics {
                                        contentDescription =
                                            "SettingsOptionsRadioButtons: Barcode Symbology Types"
                                    }
                                    .padding(vertical = AppDimensions.dimension_8dp),
                                verticalArrangement = Arrangement.spacedBy(AppDimensions.zeroPadding)
                            ) {
                                SwitchOption(
                                    title = "Australian Postal",
                                    checked = settings.barcodeSymbology.australianPostal,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Australian Postal",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Aztec",
                                    checked = settings.barcodeSymbology.aztec,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Aztec",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Canadian Postal",
                                    checked = settings.barcodeSymbology.canadianPostal,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Canadian Postal",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Chinese 2of5",
                                    checked = settings.barcodeSymbology.chinese2of5,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Chinese 2of5",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Codabar",
                                    checked = settings.barcodeSymbology.codabar,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Codabar",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Code 11",
                                    checked = settings.barcodeSymbology.code11,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Code 11",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Code 39",
                                    checked = settings.barcodeSymbology.code39,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Code 39",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Code 93",
                                    checked = settings.barcodeSymbology.code93,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Code 93",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Code 128",
                                    checked = settings.barcodeSymbology.code128,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Code 128",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Composite AB",
                                    checked = settings.barcodeSymbology.compositeAB,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Composite AB",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Composite C",
                                    checked = settings.barcodeSymbology.compositeC,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Composite C",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "D2of5",
                                    checked = settings.barcodeSymbology.d2of5,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "D2of5",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Data Matrix",
                                    checked = settings.barcodeSymbology.datamatrix,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "DataMatrix",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "DotCode",
                                    checked = settings.barcodeSymbology.dotcode,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "DotCode",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Dutch Postal",
                                    checked = settings.barcodeSymbology.dutchPostal,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Dutch Postal",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "EAN-8",
                                    checked = settings.barcodeSymbology.ean8,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "EAN-8",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "EAN-13",
                                    checked = settings.barcodeSymbology.ean13,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "EAN-13",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Finnish Postal 4S",
                                    checked = settings.barcodeSymbology.finnishPostal4s,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Finnish Postal 4S",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Grid Matrix",
                                    checked = settings.barcodeSymbology.gridMatrix,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Grid Matrix",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "GS1 DataBar",
                                    checked = settings.barcodeSymbology.gs1Databar,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "GS1 DataBar",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "GS1 DataBar Expanded",
                                    checked = settings.barcodeSymbology.gs1DatabarExpanded,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "GS1 DataBar Expanded",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "GS1 DataBar Limited",
                                    checked = settings.barcodeSymbology.gs1DatabarLim,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "GS1 DataBar Limited",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "GS1 DataMatrix",
                                    checked = settings.barcodeSymbology.gs1Datamatrix,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "GS1 DataMatrix",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "GS1 QR Code",
                                    checked = settings.barcodeSymbology.gs1Qrcode,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "GS1 QR Code",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Han xin",
                                    checked = settings.barcodeSymbology.hanxin,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Hanxin",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "I2of5",
                                    checked = settings.barcodeSymbology.i2of5,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "I2of5",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Japanese Postal",
                                    checked = settings.barcodeSymbology.japanesePostal,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Japanese Postal",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Korean 3of5",
                                    checked = settings.barcodeSymbology.korean3of5,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Korean 3of5",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Mail mark",
                                    checked = settings.barcodeSymbology.mailmark,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Mailmark",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Matrix 2of5",
                                    checked = settings.barcodeSymbology.matrix2of5,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Matrix 2of5",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "MaxiCode",
                                    checked = settings.barcodeSymbology.maxicode,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "MaxiCode",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "MicroPDF",
                                    checked = settings.barcodeSymbology.micropdf,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "MicroPDF",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "MicroQR",
                                    checked = settings.barcodeSymbology.microqr,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "MicroQR",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "MSI",
                                    checked = settings.barcodeSymbology.msi,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "MSI",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "PDF 417",
                                    checked = settings.barcodeSymbology.pdf417,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "PDF417",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "QR Code",
                                    checked = settings.barcodeSymbology.qrcode,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "QR Code",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "TLC 39",
                                    checked = settings.barcodeSymbology.tlc39,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "TLC39",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "Trioptic 39",
                                    checked = settings.barcodeSymbology.trioptic39,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "Trioptic 39",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "UK Postal",
                                    checked = settings.barcodeSymbology.ukPostal,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "UK Postal",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "UPC-A",
                                    checked = settings.barcodeSymbology.upcA,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "UPC-A",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "UPCE-0",
                                    checked = settings.barcodeSymbology.upcE,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "UPC-E",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "UPCE-1",
                                    checked = settings.barcodeSymbology.upce1,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "UPC-E1",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "USPlanet",
                                    checked = settings.barcodeSymbology.usplanet,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "US Planet",
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                                SwitchOption(
                                    title = "USPostnet",
                                    checked = settings.barcodeSymbology.uspostnet,
                                    onCheckedChange = {
                                        settingsViewModel.updateSymbology(
                                            "US Postnet",
                                            it,
                                            SettingsMode.WAND
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
                                            it,
                                            SettingsMode.WAND
                                        )
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(AppDimensions.dimension_16dp))
                    }
                }
            }

            item {
                SettingsCard(
                    title = stringResource(id = R.string.setting_screen_live_overlay_title),
                    isExpanded = liveOverlayExpanded,
                    onExpandToggle = { liveOverlayExpanded = !liveOverlayExpanded }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(
                                AppDimensions.dimension_10dp,
                                Alignment.Start
                            ),
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier
                                .background(mainInverse)
                                .padding(
                                    start = AppDimensions.dimension_12dp,
                                    top = AppDimensions.dimension_16dp,
                                    end = AppDimensions.dimension_12dp,
                                    bottom = AppDimensions.dimension_16dp
                                )
                        ) {
                            Text(
                                text = stringResource(id = R.string.setting_screen_live_overlay_description),
                                color = textBlack,
                                fontSize = AppDimensions.dialogTextFontSizeSmall,
                                lineHeight = AppDimensions.BulletLineHeight
                            )
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = white),
                            elevation = CardDefaults.cardElevation(defaultElevation = AppDimensions.zeroPadding),
                            shape = RoundedCornerShape(AppDimensions.zeroPadding)
                        ) {
                            SwitchOption(
                                title = stringResource(id = R.string.setting_screen_live_pip_thumbnail_toggle),
                                checked = settings.livePipThumbnailEnabled,
                                onCheckedChange = settingsViewModel::updateLivePipThumbnailEnabled
                            )
                        }

                        Spacer(modifier = Modifier.height(AppDimensions.dimension_8dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = white),
                            elevation = CardDefaults.cardElevation(defaultElevation = AppDimensions.zeroPadding),
                            shape = RoundedCornerShape(AppDimensions.zeroPadding)
                        ) {
                            SwitchOption(
                                title = "Show audit progress indicator",
                                checked = settings.showAuditProgress,
                                onCheckedChange = settingsViewModel::updateShowAuditProgress
                            )
                        }

                        Spacer(modifier = Modifier.height(AppDimensions.dimension_8dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = white),
                            elevation = CardDefaults.cardElevation(defaultElevation = AppDimensions.zeroPadding),
                            shape = RoundedCornerShape(AppDimensions.zeroPadding)
                        ) {
                            SwitchOption(
                                title = "Show live bounding boxes during wand",
                                checked = settings.showLiveBoundingBoxes,
                                onCheckedChange = settingsViewModel::updateShowLiveBoundingBoxes
                            )
                        }

                        Spacer(modifier = Modifier.height(AppDimensions.dimension_16dp))
                    }
                }
            }

            item {
                var wandDebugModeExpanded by remember { mutableStateOf(false) }

                SettingsCard(
                    title = "Debug Mode",
                    isExpanded = wandDebugModeExpanded,
                    onExpandToggle = { wandDebugModeExpanded = !wandDebugModeExpanded }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(
                                AppDimensions.dimension_10dp,
                                Alignment.Start
                            ),
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier
                                .background(mainInverse)
                                .padding(
                                    start = AppDimensions.dimension_12dp,
                                    top = AppDimensions.dimension_16dp,
                                    end = AppDimensions.dimension_12dp,
                                    bottom = AppDimensions.dimension_16dp
                                )
                        ) {
                            Text(
                                text = "Enable debug visualizations to see internal states during wand mode. Shows barcode data labels on box bounding boxes with color-coded primary barcodes.",
                                color = textBlack,
                                fontSize = AppDimensions.dialogTextFontSizeSmall,
                                lineHeight = AppDimensions.BulletLineHeight
                            )
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = white),
                            elevation = CardDefaults.cardElevation(defaultElevation = AppDimensions.zeroPadding),
                            shape = RoundedCornerShape(AppDimensions.zeroPadding)
                        ) {
                            Column {
                                SwitchOption(
                                    title = "Enable Debug Mode",
                                    checked = settings.debugSettings.debugModeEnabled,
                                    onCheckedChange = settingsViewModel::updateWandDebugModeEnabled
                                )

                                if (settings.debugSettings.debugModeEnabled) {
                                    SwitchOption(
                                        title = "Show Wand Barcode Labels",
                                        checked = settings.debugSettings.showWandBarcodeLabels,
                                        onCheckedChange = settingsViewModel::updateShowWandBarcodeLabels
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(AppDimensions.dimension_16dp))
                    }
                }
            }

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

    BackHandler {
        onBackPressed()
    }

}

@Preview(showBackground = true, name = "AI Vision SDK Settings Screen")
@Composable
fun WandSettingsScreenPreview() {
    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            WandSettingsScreen(
                settingsViewModel = TODO(),
                onBackClick = TODO(),
                onResetToDefaultClick = TODO(),
                onBackPressed = TODO()
            )
        }
    }
}
