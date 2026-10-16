package com.zebra.aidatacapturedemo.ui.view

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.ui.view.Variables.blackText
import com.zebra.aidatacapturedemo.ui.view.Variables.warningBorder
import com.zebra.aidatacapturedemo.ui.view.Variables.warningColor
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel

/**
 * AdvancedOCRSettingsScreen is a Composable function that displays the advanced settings for the
 * OCR model in the AI Data Capture Demo app. It includes options for detection parameters,
 * recognition parameters, and grouping settings. The screen also provides a warning message and
 * a link to TechDocs for more information. The settings are displayed in an expandable list format,
 * allowing users to easily navigate and modify the OCR settings as needed.
 *
 * @param viewModel The ViewModel instance that holds the UI state and handles user interactions for the AI Data Capture Demo.
 * @param navController The NavController used for navigation between screens in the app.
 * @param innerPadding The padding values to be applied to the content of the screen, typically provided by Scaffold.
 * @param context The Context of the current state of the application, used for actions such as opening URLs.
 */
@Composable
fun AdvancedOCRSettingsScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavController,
    innerPadding: PaddingValues,
    context: Context
) {
    BackHandler(enabled = true) {
        viewModel.handleBackButton(navController)
    }
    val settingsItemsList = ExpandableSettingsItemsList()
    settingsItemsList.AddOCRSettings()

    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(color = Variables.surfaceDefault)
    ) {
        Column(
            modifier = Modifier.padding(top = 12.dp, start = 12.dp, end = 12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .border(width = 1.dp, warningBorder, shape = RoundedCornerShape(size = 4.dp))
                    .padding(0.5.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(color = warningColor, shape = RoundedCornerShape(size = 4.dp))
                    .padding(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.warning_icon),
                    contentDescription = "image description",
                    contentScale = ContentScale.None,
                    modifier = Modifier
                        .padding(0.75.dp)
                        .width(18.dp)
                        .height(18.dp)
                )
                Text(
                    text = stringResource(R.string.instruction_3),
                    style = TextStyle(
                        fontSize = 13.sp,
                        lineHeight = 18.93.sp,
                        fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                        fontWeight = FontWeight(400),
                        color = blackText,
                        letterSpacing = 0.24.sp,
                    )
                )
            }
        }

        Row(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
        ) {
            Text(
                text = "Visit Techdocs for information on the advanced settings >",
                style = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                    fontWeight = FontWeight(400),
                    color = Variables.mainPrimary,
                ),
                modifier = Modifier.clickable {
                    openTechDocsUrl(context = context)
                }
            )
        }

        val items = remember {
            List(settingsItemsList.itemsTitle.size) { index ->
                ExpandableSettingsItem(settingsItemsList.itemsTitle[index].title)
            }
        }
        val expandedStates =
            remember { mutableStateListOf(*BooleanArray(items.size) { false }.toTypedArray()) }
        val listState = rememberLazyListState()

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
            horizontalAlignment = Alignment.Start,
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            state = listState
        ) {
            itemsIndexed(items, key = { index, _ -> index }) { index, item ->
                ExpandableSettingsListItem(
                    item = item,
                    index = index,
                    isExpanded = expandedStates[index],
                    onExpandedChange = {
                        for (i in items.indices) {
                            expandedStates[i] = false
                        }
                        expandedStates[index] = it
                    },
                    viewModel, navController
                )
            }
        }
    }
}

private fun openTechDocsUrl(context: Context) {
    val intent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://techdocs.zebra.com/ai-datacapture/latest/textocr/")
    )
    context.startActivity(intent)
}

@Composable
fun ExpandableSettingsItemsList.AddOCRSettings() {
    itemsTitle.add(ExpandableSettingsItem(stringResource(R.string.detection_parameters)))
    itemsTitle.add(ExpandableSettingsItem(stringResource(R.string.recognition_parameters)))
    itemsTitle.add(ExpandableSettingsItem(stringResource((R.string.grouping))))
}

@Composable
fun AddOCRDetectionOptions(viewModel: AIDataCaptureDemoViewModel) {
    val currentUIState = viewModel.uiState.collectAsState().value
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        TextInputOption(
            TextInputData(
                R.string.heatmap_threshold,
                currentUIState.textOCRSettings.advancedOCRSetting.heatmapThreshold.toString(),
                onItemSelected = { title, value ->
                    viewModel.updateOCRTextFieldValues(title, value)
                })
        )
        TextInputOption(
            TextInputData(
                R.string.box_threshold,
                currentUIState.textOCRSettings.advancedOCRSetting.boxThreshold.toString(),
                onItemSelected = { title, value ->
                    viewModel.updateOCRTextFieldValues(title, value)
                })
        )
        TextInputOption(
            TextInputData(
                R.string.min_box_area,
                currentUIState.textOCRSettings.advancedOCRSetting.minBoxArea.toString(),
                onItemSelected = { title, value ->
                    viewModel.updateOCRTextFieldValues(title, value)
                })
        )
        TextInputOption(
            TextInputData(
                R.string.min_box_size,
                currentUIState.textOCRSettings.advancedOCRSetting.minBoxSize.toString(),
                onItemSelected = { title, value ->
                    viewModel.updateOCRTextFieldValues(title, value)
                })
        )
        TextInputOption(
            TextInputData(
                R.string.unclip_ratio,
                currentUIState.textOCRSettings.advancedOCRSetting.unclipRatio.toString(),
                onItemSelected = { title, value ->
                    viewModel.updateOCRTextFieldValues(title, value)
                })
        )
        TextInputOption(
            TextInputData(
                R.string.min_ratio_for_rotation,
                currentUIState.textOCRSettings.advancedOCRSetting.minRatioForRotation.toString(),
                onItemSelected = { title, value ->
                    viewModel.updateOCRTextFieldValues(title, value)
                })
        )
    }
}

@Composable
fun AddOCRRecognitionOptions(viewModel: AIDataCaptureDemoViewModel) {
    var tiling by remember { mutableStateOf(false) }
    val currentUIState = viewModel.uiState.collectAsState().value
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        TextInputOption(
            TextInputData(
                R.string.max_word_combinations,
                currentUIState.textOCRSettings.advancedOCRSetting.maxWordCombinations.toString(),
                onItemSelected = { title, value ->
                    viewModel.updateOCRTextFieldValues(title, value)
                })
        )
        TextInputOption(
            TextInputData(
                R.string.topk_ignore_cutoff,
                currentUIState.textOCRSettings.advancedOCRSetting.topkIgnoreCutoff.toString(),
                onItemSelected = { title, value ->
                    viewModel.updateOCRTextFieldValues(title, value)
                })
        )
        TextInputOption(
            TextInputData(
                R.string.total_probability_threshold,
                currentUIState.textOCRSettings.advancedOCRSetting.totalProbabilityThreshold.toString(),
                onItemSelected = { title, value ->
                    viewModel.updateOCRTextFieldValues(title, value)
                })
        )
        //}
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp),
            thickness = 2.dp
        )
        SwitchOption(
            currentUIState.textOCRSettings.advancedOCRSetting.enableTiling,
            SwitchOptionData(R.string.enable_tiling, onItemSelected = { title, enabled ->
                tiling = enabled
                viewModel.updateOCRSwitchOptions(title, enabled)
            })
        )
        AddOCRTilingOptions(viewModel, tiling)
    }
}

@Composable
fun AddOCRTilingOptions(viewModel: AIDataCaptureDemoViewModel, enabled: Boolean) {
    val currentUIState = viewModel.uiState.collectAsState().value
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        TextInputOption(
            TextInputData(
                R.string.top_correlation_threshold,
                currentUIState.textOCRSettings.advancedOCRSetting.topCorrelationThreshold.toString(),
                onItemSelected = { title, value ->
                    viewModel.updateOCRTextFieldValues(title, value)
                }), enabled
        )
        TextInputOption(
            TextInputData(
                R.string.merge_points_cutoff,
                currentUIState.textOCRSettings.advancedOCRSetting.mergePointsCutoff.toString(),
                onItemSelected = { title, value ->
                    viewModel.updateOCRTextFieldValues(title, value)
                }), enabled
        )
        TextInputOption(
            TextInputData(
                R.string.split_margin_factor,
                currentUIState.textOCRSettings.advancedOCRSetting.splitMarginFactor.toString(),
                onItemSelected = { title, value ->
                    viewModel.updateOCRTextFieldValues(title, value)
                }), enabled
        )
        TextInputOption(
            TextInputData(
                R.string.aspect_ratio_lower_threshold,
                currentUIState.textOCRSettings.advancedOCRSetting.aspectRatioLowerThreshold.toString(),
                onItemSelected = { title, value ->
                    viewModel.updateOCRTextFieldValues(title, value)
                }), enabled
        )
        TextInputOption(
            TextInputData(
                R.string.aspect_ratio_upper_threshold,
                currentUIState.textOCRSettings.advancedOCRSetting.aspectRatioUpperThreshold.toString(),
                onItemSelected = { title, value ->
                    viewModel.updateOCRTextFieldValues(title, value)
                }), enabled
        )
        TextInputOption(
            TextInputData(
                R.string.topK_merged_predictions,
                currentUIState.textOCRSettings.advancedOCRSetting.topKMergedPredictions.toString(),
                onItemSelected = { title, value ->
                    viewModel.updateOCRTextFieldValues(title, value)
                }), enabled
        )
    }
}

@Composable
fun AddEnableOCRGroupingOptions(viewModel: AIDataCaptureDemoViewModel) {
    val currentUIState = viewModel.uiState.collectAsState().value
    var grouping by remember { mutableStateOf(false) }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        SwitchOption(
            currentUIState.textOCRSettings.advancedOCRSetting.enableGrouping,
            SwitchOptionData(R.string.enable_grouping, onItemSelected = { title, enabled ->
                grouping = enabled
                viewModel.updateOCRSwitchOptions(title, enabled)
            })
        )
        AddOCRGroupingOptions(viewModel, grouping)
    }
}

@Composable
fun AddOCRGroupingOptions(viewModel: AIDataCaptureDemoViewModel, enabled: Boolean) {
    val currentUIState = viewModel.uiState.collectAsState().value
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        TextInputOption(
            TextInputData(
                R.string.width_distance_ratio,
                currentUIState.textOCRSettings.advancedOCRSetting.widthDistanceRatio.toString(),
                onItemSelected = { title, value ->
                    viewModel.updateOCRTextFieldValues(title, value)
                }), enabled
        )
        TextInputOption(
            TextInputData(
                R.string.height_distance_ratio,
                currentUIState.textOCRSettings.advancedOCRSetting.heightDistanceRatio.toString(),
                onItemSelected = { title, value ->
                    viewModel.updateOCRTextFieldValues(title, value)
                }), enabled
        )
        TextInputOption(
            TextInputData(
                R.string.center_distance_ratio,
                currentUIState.textOCRSettings.advancedOCRSetting.centerDistanceRatio.toString(),
                onItemSelected = { title, value ->
                    viewModel.updateOCRTextFieldValues(title, value)
                }), enabled
        )
        TextInputOption(
            TextInputData(
                R.string.paragraph_height_distance,
                currentUIState.textOCRSettings.advancedOCRSetting.paragraphHeightDistance.toString(),
                onItemSelected = { title, value ->
                    viewModel.updateOCRTextFieldValues(title, value)
                }), enabled
        )
        TextInputOption(
            TextInputData(
                R.string.paragraph_height_ratio_threshold,
                currentUIState.textOCRSettings.advancedOCRSetting.paragraphHeightRatioThreshold.toString(),
                onItemSelected = { title, value ->
                    viewModel.updateOCRTextFieldValues(title, value)
                }), enabled
        )
    }
}

@Composable
fun ExpandableSettingsItemsList.AddTargetModeSettings()  {
    itemsTitle.add(ExpandableSettingsItem(stringResource(R.string.target_mode)))
}

@Composable
fun AddTargetModeSettingUi(viewModel: AIDataCaptureDemoViewModel) {
    val uiState = viewModel.uiState.collectAsState().value
    val targetMode = uiState.ocrBarcodeFindSettings.targetModeSetting

    // Convert stored ratios (0.0–1.0) to percentage (0–100) for display
    var widthPercent by remember(targetMode.defaultFrameWidthRatio) {
        mutableFloatStateOf(targetMode.defaultFrameWidthRatio * 100f)
    }
    var heightPercent by remember(targetMode.defaultFrameHeightRatio) {
        mutableFloatStateOf(targetMode.defaultFrameHeightRatio * 100f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top),
        horizontalAlignment = Alignment.Start
    ) {
        SingleValueInputSlider(
            value = widthPercent,
            onValueChange = { newVal ->
                widthPercent = newVal
                viewModel.updateDefaultTargetModeFrameSize(newVal, heightPercent)
            }, onValueChangeFinished = { viewModel.commitDefaultTargetModeFrameSize() },
            valueRange = 10f..100f,
            steps = 17,
            label = "${stringResource(R.string.default_frame_width)} (%)"
        )

        SingleValueInputSlider(
            value = heightPercent,
            onValueChange = { newVal ->
                heightPercent = newVal
                viewModel.updateDefaultTargetModeFrameSize(widthPercent, newVal)
            }, onValueChangeFinished = { viewModel.commitDefaultTargetModeFrameSize() },
            valueRange = 5f..100f,
            steps = 18,
            label = "${stringResource(R.string.default_frame_height)} (%)"
        )
    }
}

