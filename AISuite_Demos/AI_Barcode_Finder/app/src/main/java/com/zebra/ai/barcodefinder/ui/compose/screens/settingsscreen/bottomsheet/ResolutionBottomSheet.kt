// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.ui.compose.screens.settingsscreen.bottomsheet

import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zebra.ai.barcodefinder.R
import com.zebra.ai.barcodefinder.common.enums.Resolution
import com.zebra.ai.barcodefinder.ui.compose.screens.settingsscreen.components.ZebraBottomSheet
import com.zebra.ai.barcodefinder.ui.theme.AppColors
import com.zebra.ai.barcodefinder.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResolutionBottomSheet(
    settingsViewModel: SettingsViewModel = viewModel(),
    currentSelection: Resolution = Resolution.FOUR_MP,
    onBackClick: () -> Unit = {},
    onBackPressed: () -> Unit = {}
) {
    // State for collapsible section

    // Bottom Sheet Modal Design - takes up 75% of screen height
    ZebraBottomSheet(
        title = stringResource(id = R.string.setting_resolution_title),
        onBackClick = onBackClick,
        descriptionText = {
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight(400))) {
                        append(stringResource(id = R.string.setting_camera_resolution_description1))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight(700))) {
                        append(stringResource(id = R.string.setting_camera_resolution_description2))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight(400))) {
                        append(stringResource(id = R.string.setting_camera_resolution_description3))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight(700))) {
                        append(stringResource(id = R.string.note_label))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight(400))) {
                        append(stringResource(id = R.string.setting_camera_resolution_description4))
                    }
                },
                color = AppColors.TextBlack,
                fontSize = AppDimensions.dialogTextFontSizeMedium,
                lineHeight = AppDimensions.linePaddingDefault
            )
        },
        subTitleText = stringResource(id = R.string.setting_resolution_options_title),
        descriptionBulletItems = (
                listOf(
                    stringResource(id = R.string.setting_resolution_1mp) to listOf(
                        stringResource(id = R.string.setting_resolution_1mp_fastest) to listOf("Fastest"),
                        stringResource(id = R.string.setting_resolution_1mp_best) to listOf(""),
                        stringResource(id = R.string.setting_resolution_1mp_miss) to listOf("")
                    ),
                    stringResource(id = R.string.setting_resolution_2mp) to listOf(
                        stringResource(id = R.string.setting_resolution_2mp_good) to listOf(""),
                        stringResource(id = R.string.setting_resolution_2mp_handles) to listOf("standard barcode scanning")
                    ),
                    stringResource(id = R.string.setting_resolution_4mp) to listOf(
                        stringResource(id = R.string.setting_resolution_4mp_detail) to listOf(""),
                        stringResource(id = R.string.setting_resolution_4mp_memory) to listOf("Higher memory and battery use")
                    ),
                    stringResource(id = R.string.setting_resolution_8mp) to listOf(
                        stringResource(id = R.string.setting_resolution_8mp_maximum) to listOf("Maximum detail and accuracy"),
                        stringResource(id = R.string.setting_resolution_8mp_best) to listOf(""),
                        stringResource(id = R.string.setting_resolution_8mp_slowest) to listOf("Slowest and highest memory/battery consumption"),
                        stringResource(id = R.string.setting_resolution_8mp_limited) to listOf("Not recommended for CPU/GPU")
                    )
                )
                ),
        recommendationBulltetItems = (
                listOf(
                    stringResource(id = R.string.setting_recommendation_title) to listOf(
                        stringResource(id = R.string.setting_recommendation_start_res) to listOf("Start with 1 or 2MP"),
                        stringResource(id = R.string.setting_recommendation_increase_res) to listOf(
                            "Increase",
                            "only if"
                        ),
                        stringResource(id = R.string.setting_recommendation_match) to listOf("Match your camera resolution to your model input size")
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
                        append(stringResource(id = R.string.setting_tip_make_sure))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(stringResource(id = R.string.setting_tip_at_least))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                        append(stringResource(id = R.string.setting_tip_in_image))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(stringResource(id = R.string.setting_tip_experiment_res))
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
fun ResolutionScreenPreview() {
    MaterialTheme {
        ResolutionBottomSheet(
            currentSelection = Resolution.FOUR_MP,
            onBackClick = {}
        )
    }
}
