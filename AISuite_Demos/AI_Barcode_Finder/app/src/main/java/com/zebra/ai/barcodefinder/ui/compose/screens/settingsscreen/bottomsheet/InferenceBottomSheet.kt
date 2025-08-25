// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.ui.compose.screens.settingsscreen.bottomsheet

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
import com.zebra.ai.barcodefinder.common.enums.ProcessorType
import com.zebra.ai.barcodefinder.ui.compose.screens.settingsscreen.components.HyperlinkText
import com.zebra.ai.barcodefinder.ui.compose.screens.settingsscreen.components.ZebraBottomSheet
import com.zebra.ai.barcodefinder.ui.theme.AppColors
import com.zebra.ai.barcodefinder.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.viewmodel.SettingsViewModel

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
        title = stringResource(id = R.string.setting_inference_title),
        onBackClick = onBackClick,
        descriptionText = {
            Column {
                Text(
                    text = stringResource(id = R.string.setting_inference_description1),
                    fontSize = AppDimensions.dialogTextFontSizeMedium,
                    color = AppColors.TextBlack,
                    lineHeight = AppDimensions.linePaddingDefault
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(stringResource(id = R.string.note_label))
                        }
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                            append(stringResource(id = R.string.setting_inference_description3))
                        }
                    },
                    color = AppColors.TextBlack,
                    fontSize = AppDimensions.dialogTextFontSizeMedium,
                    lineHeight = AppDimensions.linePaddingDefault
                )

                HyperlinkText(
                    stringResource(id = R.string.setting_inference_description4),
                    stringResource(id = R.string.setting_inference_description_link)
                )
            }
        },
        subTitleText = stringResource(id = R.string.setting_inference_subtitle),
        descriptionBulletItems = (
                listOf(
                    stringResource(id = R.string.setting_inference_dsp_title) to listOf(
                        stringResource(id = R.string.setting_inference_dsp_fastest) to listOf("Recommended"),
                        stringResource(id = R.string.setting_inference_dsp_ideal_use) to listOf(""),
                        stringResource(id = R.string.setting_inference_dsp_note) to listOf("Note:")
                    ),
                    stringResource(id = R.string.setting_inference_gpu_title) to listOf(
                        stringResource(id = R.string.setting_inference_gpu_best) to listOf("Next best option:"),
                        stringResource(id = R.string.setting_inference_gpu_power_usage) to listOf(""),
                        stringResource(id = R.string.setting_inference_gpu_fallback) to listOf("")
                    ),
                    stringResource(id = R.string.setting_inference_cpu_title) to listOf(
                        stringResource(id = R.string.setting_inference_cpu_fallback) to listOf(
                            "Fallback",
                            "slowest and least efficient"
                        ),
                        stringResource(id = R.string.setting_inference_cpu_usage_condition) to listOf(
                            "Only use if DSP/GPU are not available"
                        )
                    )
                )
                ),
        recommendationBulltetItems = (
                listOf(
                    stringResource(id = R.string.setting_recommendation_title) to listOf(
                        stringResource(id = R.string.setting_inference_processor_recommendation_dsp_gpu) to listOf(
                            "Use DSP if available on your device; otherwise, use GPU."
                        ),
                        stringResource(id = R.string.setting_inference_processor_recommendation_cpu) to listOf(
                            ""
                        )
                    )
                )
                ),
        tipText = {
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                        append(stringResource(id = R.string.setting_tip_title)) // Bold text
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                        append(stringResource(id = R.string.setting_inference_processor_tip))
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
