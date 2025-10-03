// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.presentation.ui.compose.screens.settingsscreen.bottomsheet

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zebra.ai.barcodefinder.R
import com.zebra.ai.barcodefinder.domain.enums.ProcessorType
import com.zebra.ai.barcodefinder.presentation.ui.compose.screens.settingsscreen.components.HyperlinkText
import com.zebra.ai.barcodefinder.presentation.ui.compose.screens.settingsscreen.components.ZebraBottomSheet
import com.zebra.ai.barcodefinder.presentation.ui.theme.AppColors
import com.zebra.ai.barcodefinder.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InferenceBottomSheet(
    settingsViewModel: SettingsViewModel = viewModel(),
    currentSelection: ProcessorType = ProcessorType.DSP,
    onBackClick: () -> Unit = {},
    onBackPressed: () -> Unit = {}
) {
    // State for collapsible section

    // Bottom Sheet Modal Design - takes up 75% of screen height
    ZebraBottomSheet(
        title = stringResource(id = R.string.inference_bottom_sheet_title),
        onBackClick = onBackClick,
        descriptionText = {
            Column {
                Text(
                    text = stringResource(id = R.string.inference_bottom_sheet_description1),
                    fontSize = AppDimensions.dialogTextFontSizeMedium,
                    color = AppColors.TextBlack,
                    lineHeight = AppDimensions.linePaddingDefault
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(stringResource(id = R.string.inference_bottom_sheet_note_label))
                        }
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                            append(stringResource(id = R.string.inference_bottom_sheet_description3))
                        }
                    },
                    color = AppColors.TextBlack,
                    fontSize = AppDimensions.dialogTextFontSizeMedium,
                    lineHeight = AppDimensions.linePaddingDefault
                )

                HyperlinkText(
                    stringResource(id = R.string.inference_bottom_sheet_description4),
                    "https://supportcommunity.zebra.com/s/article/000022440?language=en_US"
                )
            }
        },
        subTitleText = stringResource(id = R.string.inference_bottom_sheet_subtitle),
        descriptionBulletItems = (
                listOf(
                    stringResource(id = R.string.inference_bottom_sheet_dsp_title) to listOf(
                        stringResource(id = R.string.inference_bottom_sheet_dsp_fastest) to listOf(stringResource(id = R.string.inference_bottom_sheet_dsp_fastest_bold)),
                        stringResource(id = R.string.inference_bottom_sheet_dsp_ideal_use) to listOf(""),
                        stringResource(id = R.string.inference_bottom_sheet_dsp_note) to listOf(stringResource(id = R.string.inference_bottom_sheet_dsp_note_bold))
                    ),
                    stringResource(id = R.string.inference_bottom_sheet_gpu_title) to listOf(
                        stringResource(id = R.string.inference_bottom_sheet_gpu_best) to listOf(stringResource(id = R.string.inference_bottom_sheet_gpu_best)),
                        stringResource(id = R.string.inference_bottom_sheet_gpu_speed) to listOf(""),
                        stringResource(id = R.string.inference_bottom_sheet_gpu_power_usage) to listOf(""),
                        stringResource(id = R.string.inference_bottom_sheet_gpu_fallback) to listOf("")
                    ),
                    stringResource(id = R.string.inference_bottom_sheet_cpu_title) to listOf(
                        stringResource(id = R.string.inference_bottom_sheet_cpu_trial_use) to listOf(stringResource(id = R.string.inference_bottom_sheet_cpu_trial_use)),
                        stringResource(id = R.string.inference_bottom_sheet_cpu_availability) to listOf(),
                        stringResource(id = R.string.inference_bottom_sheet_cpu_speed) to listOf(),
                        stringResource(id = R.string.inference_bottom_sheet_cpu_usage_condition) to listOf()
                    )
                )
                ),
        recommendationBulltetItems = (
                listOf(
                    stringResource(id = R.string.inference_bottom_sheet_recommendation_title) to listOf(
                        stringResource(id = R.string.inference_bottom_sheet_processor_recommendation_dsp) to listOf(
                            stringResource(id = R.string.inference_bottom_sheet_processor_recommendation_dsp)
                        )
                    )
                )
                ),
        tipText = {
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                        append(stringResource(id = R.string.inference_bottom_sheet_tip_title)) // Bold text
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                        append(stringResource(id = R.string.inference_bottom_sheet_processor_tip))
                    }
                },
                color = AppColors.TextBlack,
                fontSize = AppDimensions.dialogTextFontSizeMedium,
                lineHeight = AppDimensions.linePaddingDefault
            )
        }
    )

    BackHandler {
        onBackPressed()
    }
}

@Preview(showBackground = true)
@Composable
fun InferenceScreenPreview() {
    MaterialTheme {
        InferenceBottomSheet(
            onBackClick = {}
        )
    }
}
