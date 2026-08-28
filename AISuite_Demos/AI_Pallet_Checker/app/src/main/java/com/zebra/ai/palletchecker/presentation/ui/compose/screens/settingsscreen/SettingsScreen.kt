// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.palletchecker.presentation.ui.compose.screens.settingsscreen

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zebra.ai.palletchecker.R
import com.zebra.ai.palletchecker.presentation.enums.ButtonType
import com.zebra.ai.palletchecker.domain.enums.AutoTriggerMode
import com.zebra.ai.palletchecker.domain.enums.ModelInput
import com.zebra.ai.palletchecker.domain.enums.ProcessorType
import com.zebra.ai.palletchecker.domain.enums.Resolution
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
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onBackClick: () -> Unit = {},
    onBarcodeModelVersionsClick: () -> Unit = {},
    onResetToDefaultClick: () -> Unit = {},
    onBackPressed: () -> Unit = {},
    showFinerDecoderSettings: Boolean = false,
    showBottomSheet: (Any) -> Unit = {},
    expectedBoxesToAudit: Int = 10
) {
    val settings by settingsViewModel.settings.collectAsState()
    val wandSettings by settingsViewModel.wandSettings.collectAsState()
    var modelInputSizeExpanded by remember { mutableStateOf(false) }
    var resolutionExpanded by remember { mutableStateOf(false) }
    var inferenceExpanded by remember { mutableStateOf(false) }
    var barcodeSymbologyExpanded by remember { mutableStateOf(false) }
    var barcodeDecoderOptionsExpanded by remember { mutableStateOf(false) }

    var fixedQtyText by remember { mutableStateOf(settings.fixedQuantityThreshold.toString()) }
    var percentageText by remember { mutableStateOf(settings.percentageThreshold.toString()) }
    var expectedTotalText by remember { mutableStateOf(expectedBoxesToAudit.toString()) }
    var multiSnapRetryText by remember { mutableStateOf(settings.multiSnapRetryCount.toString()) }

    LaunchedEffect(settings.fixedQuantityThreshold) {
        val newVal = settings.fixedQuantityThreshold.toString()
        if (fixedQtyText.toIntOrNull() != settings.fixedQuantityThreshold) fixedQtyText = newVal
    }
    LaunchedEffect(settings.percentageThreshold) {
        val newVal = settings.percentageThreshold.toString()
        if (percentageText.toIntOrNull() != settings.percentageThreshold) percentageText = newVal
    }
    LaunchedEffect(expectedBoxesToAudit) {
        val newVal = expectedBoxesToAudit.toString()
        if (expectedTotalText.toIntOrNull() != expectedBoxesToAudit) {
            expectedTotalText = newVal
            settingsViewModel.updateExpectedTotalBoxes(expectedBoxesToAudit)
        }
    }
    LaunchedEffect(settings.expectedTotalBoxes) {
        val newVal = settings.expectedTotalBoxes.toString()
        if (expectedTotalText.toIntOrNull() != settings.expectedTotalBoxes) expectedTotalText =
            newVal
    }
    LaunchedEffect(settings.multiSnapRetryCount) {
        val newVal = settings.multiSnapRetryCount.toString()
        if (multiSnapRetryText.toIntOrNull() != settings.multiSnapRetryCount) multiSnapRetryText =
            newVal
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(white)
            .semantics { contentDescription = "SettingsScreen" }
    ) {

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
                                    modifier = Modifier.padding(AppDimensions.zeroPadding)
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

                            RadioButtonOption(
                                title = stringResource(id = R.string.setting_screen_model_input_2560_title),
                                subtitle = stringResource(id = R.string.setting_screen_model_input_2560_title),
                                selected = settings.modelInput == ModelInput.LARGE_2560,
                                onSelected = {
                                    settingsViewModel.updateModelInput(ModelInput.LARGE_2560)
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
                            val snapResolutions by settingsViewModel.supportedSnapResolutions.collectAsState()

                            val aspectRatioOptions = listOf("16:9", "4:3", "All")
                            var selectedAspectFilter by remember { mutableStateOf("4:3") }
                            var aspectDropdownExpanded by remember { mutableStateOf(false) }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = AppDimensions.dimension_12dp,
                                        end = AppDimensions.dimension_12dp,
                                        top = AppDimensions.dimension_8dp
                                    )
                            ) {
                                Text(
                                    text = "Aspect Ratio",
                                    fontSize = AppDimensions.dialogTextFontSizeSmall,
                                    color = textBlack,
                                    fontWeight = FontWeight.Medium
                                )
                                ExposedDropdownMenuBox(
                                    expanded = aspectDropdownExpanded,
                                    onExpandedChange = { aspectDropdownExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = selectedAspectFilter,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(
                                                expanded = aspectDropdownExpanded
                                            )
                                        },
                                        modifier = Modifier
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                            .width(120.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontSize = AppDimensions.dialogTextFontSizeSmall,
                                            color = textBlack
                                        ),
                                        singleLine = true
                                    )
                                    ExposedDropdownMenu(
                                        expanded = aspectDropdownExpanded,
                                        onDismissRequest = { aspectDropdownExpanded = false }
                                    ) {
                                        aspectRatioOptions.forEach { option ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = option,
                                                        fontSize = AppDimensions.dialogTextFontSizeSmall
                                                    )
                                                },
                                                onClick = {
                                                    selectedAspectFilter = option
                                                    aspectDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            val filteredResolutions = snapResolutions
                                .filter { it.width.toLong() * it.height.toLong() >= 8_000_000L }
                                .let { list ->
                                    if (selectedAspectFilter == "All") list
                                    else list.filter { it.aspectRatio == selectedAspectFilter }
                                }
                            filteredResolutions.forEach { camRes ->
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
                                            com.zebra.ai.palletchecker.domain.enums.SettingsMode.SNAP
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
                                    color = textBlack,
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
                                    settingsViewModel.updateProcessorType(ProcessorType.AUTO)
                                }
                            )
                            RadioButtonOption(
                                title = stringResource(id = R.string.setting_screen_inference_dsp_title),
                                subtitle = stringResource(id = R.string.setting_screen_inference_dsp_subtitle),
                                selected = settings.processorType == ProcessorType.DSP,
                                onSelected = {
                                    settingsViewModel.updateProcessorType(ProcessorType.DSP)
                                }
                            )
                            RadioButtonOption(
                                title = stringResource(id = R.string.setting_screen_inference_gpu_title),
                                subtitle = stringResource(id = R.string.setting_screen_inference_gpu_subtitle),
                                selected = settings.processorType == ProcessorType.GPU,
                                onSelected = {
                                    settingsViewModel.updateProcessorType(ProcessorType.GPU)
                                }
                            )
                            RadioButtonOption(
                                title = stringResource(id = R.string.setting_screen_inference_cpu_title),
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

            item {
                var autoTriggerExpanded by remember { mutableStateOf(false) }

                SettingsCard(
                    title = stringResource(id = R.string.setting_auto_trigger_title),
                    isExpanded = autoTriggerExpanded,
                    onExpandToggle = { autoTriggerExpanded = !autoTriggerExpanded }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
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
                                    text = stringResource(id = R.string.setting_auto_trigger_description),
                                    color = textBlack,
                                    fontSize = AppDimensions.dialogTextFontSizeSmall,
                                    lineHeight = AppDimensions.BulletLineHeight
                                )
                            }
                        }

                        SwitchOption(
                            title = stringResource(id = R.string.setting_auto_trigger_enable_label),
                            checked = settings.autoTriggerEnabled,
                            onCheckedChange = { settingsViewModel.updateAutoTriggerEnabled(it) }
                        )

                        if (settings.autoTriggerEnabled) {
                            Spacer(modifier = Modifier.height(AppDimensions.dimension_8dp))

                            Text(
                                text = stringResource(id = R.string.setting_auto_trigger_mode_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = AppDimensions.dialogTextFontSizeSmall,
                                color = textBlack,
                                modifier = Modifier.padding(
                                    horizontal = AppDimensions.dimension_16dp
                                )
                            )

                            Spacer(modifier = Modifier.height(AppDimensions.dimension_4dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = AppDimensions.dimension_2dp,
                                        vertical = AppDimensions.zeroPadding
                                    ),
                                verticalArrangement = Arrangement.spacedBy(AppDimensions.dimension_12dp)
                            ) {
                                RadioButtonOption(
                                    title = stringResource(id = R.string.setting_auto_trigger_mode_pallet_base_title),
                                    subtitle = stringResource(id = R.string.setting_auto_trigger_mode_pallet_base_subtitle),
                                    selected = settings.autoTriggerMode == AutoTriggerMode.PALLET_BASE,
                                    onSelected = {
                                        settingsViewModel.updateAutoTriggerMode(
                                            AutoTriggerMode.PALLET_BASE
                                        )
                                    }
                                )

                                RadioButtonOption(
                                    title = stringResource(id = R.string.setting_auto_trigger_mode_fixed_qty_title),
                                    subtitle = stringResource(id = R.string.setting_auto_trigger_mode_fixed_qty_subtitle),
                                    selected = settings.autoTriggerMode == AutoTriggerMode.FIXED_QUANTITY,
                                    onSelected = {
                                        settingsViewModel.updateAutoTriggerMode(
                                            AutoTriggerMode.FIXED_QUANTITY
                                        )
                                    }
                                )

                                if (settings.autoTriggerMode == AutoTriggerMode.FIXED_QUANTITY) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = AppDimensions.dimension_16dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.setting_auto_trigger_fixed_qty_label),
                                            fontSize = AppDimensions.dialogTextFontSizeSmall,
                                            color = textBlack,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(AppDimensions.dimension_8dp))
                                        OutlinedTextField(
                                            value = expectedBoxesToAudit.toString(),
                                            onValueChange = { },
                                            enabled = false,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.width(80.dp)
                                        )
                                    }
                                }

                                RadioButtonOption(
                                    title = stringResource(id = R.string.setting_auto_trigger_mode_percentage_title),
                                    subtitle = stringResource(id = R.string.setting_auto_trigger_mode_percentage_subtitle),
                                    selected = settings.autoTriggerMode == AutoTriggerMode.PERCENTAGE_BASED,
                                    onSelected = {
                                        settingsViewModel.updateAutoTriggerMode(
                                            AutoTriggerMode.PERCENTAGE_BASED
                                        )
                                    }
                                )

                                if (settings.autoTriggerMode == AutoTriggerMode.PERCENTAGE_BASED) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = AppDimensions.dimension_16dp),
                                        verticalArrangement = Arrangement.spacedBy(AppDimensions.dimension_8dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stringResource(id = R.string.setting_auto_trigger_percentage_label),
                                                fontSize = AppDimensions.dialogTextFontSizeSmall,
                                                color = textBlack,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(AppDimensions.dimension_8dp))
                                            OutlinedTextField(
                                                value = percentageText,
                                                onValueChange = { newValue ->
                                                    percentageText = newValue
                                                    newValue.toIntOrNull()?.let {
                                                        settingsViewModel.updatePercentageThreshold(
                                                            it
                                                        )
                                                    }
                                                },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                modifier = Modifier.width(80.dp)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stringResource(id = R.string.setting_auto_trigger_expected_total_label),
                                                fontSize = AppDimensions.dialogTextFontSizeSmall,
                                                color = textBlack,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(AppDimensions.dimension_8dp))
                                            OutlinedTextField(
                                                value = expectedTotalText,
                                                onValueChange = { newValue ->
                                                    expectedTotalText = newValue
                                                    newValue.toIntOrNull()?.let {
                                                        settingsViewModel.updateExpectedTotalBoxes(
                                                            it
                                                        )
                                                    }
                                                },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                modifier = Modifier.width(80.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(AppDimensions.dimension_16dp))
                    }
                }
            }

            item {
                var multiSnapExpanded by remember { mutableStateOf(false) }

                SettingsCard(
                    title = "Burst Capture",
                    isExpanded = multiSnapExpanded,
                    onExpandToggle = { multiSnapExpanded = !multiSnapExpanded }
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
                            Column(
                                verticalArrangement = Arrangement.spacedBy(
                                    AppDimensions.dimension_10dp,
                                    Alignment.CenterVertically
                                ),
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "When enabled, the snap phase takes multiple high-resolution captures to maximise barcode decoding. " +
                                            "Each retry merges newly decoded barcodes with previous attempts. " +
                                            "Disable for single-snap mode (faster but may miss some barcodes).",
                                    color = textBlack,
                                    fontSize = AppDimensions.dialogTextFontSizeSmall,
                                    lineHeight = AppDimensions.BulletLineHeight
                                )
                            }
                        }

                        SwitchOption(
                            title = "Enable Burst Capture",
                            checked = settings.multiSnapEnabled,
                            onCheckedChange = { settingsViewModel.updateMultiSnapEnabled(it) }
                        )

                        if (settings.multiSnapEnabled) {
                            Spacer(modifier = Modifier.height(AppDimensions.dimension_8dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = AppDimensions.dimension_16dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Max number of captures (1–10)",
                                    fontSize = AppDimensions.dialogTextFontSizeSmall,
                                    color = textBlack,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(AppDimensions.dimension_8dp))
                                OutlinedTextField(
                                    value = multiSnapRetryText,
                                    onValueChange = { newValue ->
                                        multiSnapRetryText = newValue
                                        newValue.toIntOrNull()
                                            ?.let { settingsViewModel.updateMultiSnapRetryCount(it) }
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.width(80.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(AppDimensions.dimension_16dp))
                    }
                }
            }

            item {
                var pipThumbnailExpanded by remember { mutableStateOf(false) }

                SettingsCard(
                    title = stringResource(id = R.string.setting_screen_live_overlay_title),
                    isExpanded = pipThumbnailExpanded,
                    onExpandToggle = { pipThumbnailExpanded = !pipThumbnailExpanded }
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
                                checked = wandSettings.livePipThumbnailEnabled,
                                onCheckedChange = settingsViewModel::updateLivePipThumbnailEnabled
                            )
                        }

                        Spacer(modifier = Modifier.height(AppDimensions.dimension_16dp))
                    }
                }
            }

            item {
                var debugModeExpanded by remember { mutableStateOf(false) }

                SettingsCard(
                    title = "Debug Mode",
                    isExpanded = debugModeExpanded,
                    onExpandToggle = { debugModeExpanded = !debugModeExpanded }
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
                                text = "Enable debug visualizations to see internal states during pallet capture workflow. Useful for development and troubleshooting.",
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
                                    title = "Enable Image Capture Debug mode",
                                    checked = settings.debugSettings.debugModeEnabled,
                                    onCheckedChange = settingsViewModel::updateDebugModeEnabled
                                )

                                if (settings.debugSettings.debugModeEnabled) {
                                    SwitchOption(
                                        title = "Show Pre-Image Capture bounding boxes",
                                        checked = settings.debugSettings.showPreSnapBoundingBoxes,
                                        onCheckedChange = settingsViewModel::updateShowPreSnapBoundingBoxes
                                    )

                                    if (showFinerDecoderSettings) {
                                        SwitchOption(
                                            title = "Show Spatial Map Indices",
                                            checked = settings.debugSettings.showSpatialMapIndices,
                                            onCheckedChange = settingsViewModel::updateShowSpatialMapIndices
                                        )

                                        SwitchOption(
                                            title = "Show Snap Barcode Labels",
                                            checked = settings.debugSettings.showSnapBarcodeLabels,
                                            onCheckedChange = settingsViewModel::updateShowSnapBarcodeLabels
                                        )

                                        SwitchOption(
                                            title = "Show Snap Tracking IDs",
                                            checked = settings.debugSettings.showSnapTrackingIds,
                                            onCheckedChange = settingsViewModel::updateShowSnapTrackingIds
                                        )
                                    }

                                    SwitchOption(
                                        title = "Show Captured Snap Barcode Labels",
                                        checked = settings.debugSettings.showCapturedSnapBarcodeLabels,
                                        onCheckedChange = settingsViewModel::updateShowCapturedSnapBarcodeLabels
                                    )


                                    SwitchOption(
                                        title = "Dump Snap Images to Filesystem",
                                        checked = settings.debugSettings.dumpSnapImagesToFilesystem,
                                        onCheckedChange = settingsViewModel::updateDumpSnapImagesToFilesystem
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(AppDimensions.dimension_12dp))

                        Text(
                            text = "Wand Mode Debug",
                            color = textBlack,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = AppDimensions.dialogTextFontSizeSmall,
                            modifier = Modifier.padding(
                                start = AppDimensions.dimension_12dp,
                                bottom = AppDimensions.dimension_4dp
                            )
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = white),
                            elevation = CardDefaults.cardElevation(defaultElevation = AppDimensions.zeroPadding),
                            shape = RoundedCornerShape(AppDimensions.zeroPadding)
                        ) {
                            Column {
                                SwitchOption(
                                    title = "Enable Wand Debug Mode",
                                    checked = wandSettings.debugSettings.debugModeEnabled,
                                    onCheckedChange = settingsViewModel::updateWandDebugModeEnabled
                                )

                                if (wandSettings.debugSettings.debugModeEnabled) {
                                    SwitchOption(
                                        title = "Show Wand Barcode Labels",
                                        checked = wandSettings.debugSettings.showWandBarcodeLabels,
                                        onCheckedChange = settingsViewModel::updateShowWandBarcodeLabels
                                    )

                                    SwitchOption(
                                        title = "Show Wand Tracking IDs",
                                        checked = wandSettings.debugSettings.showWandTrackingIds,
                                        onCheckedChange = settingsViewModel::updateShowWandTrackingIds
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
