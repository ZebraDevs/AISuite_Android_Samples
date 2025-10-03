// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.presentation.ui.compose.screens.settingsscreen.bottomsheet

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
import com.zebra.ai.barcodefinder.domain.enums.Resolution
import com.zebra.ai.barcodefinder.presentation.ui.compose.screens.settingsscreen.components.ZebraBottomSheet
import com.zebra.ai.barcodefinder.presentation.ui.theme.AppColors
import com.zebra.ai.barcodefinder.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.presentation.viewmodel.SettingsViewModel

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
        title = stringResource(id = R.string.resolution_bottom_sheet_title),
        onBackClick = onBackClick,
        descriptionText = {
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight(400))) {
                        append(stringResource(id = R.string.resolution_bottom_sheet_description1))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight(700))) {
                        append(stringResource(id = R.string.resolution_bottom_sheet_description2))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight(400))) {
                        append(stringResource(id = R.string.resolution_bottom_sheet_description3))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight(700))) {
                        append(stringResource(id = R.string.resolution_bottom_sheet_note_label))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight(400))) {
                        append(stringResource(id = R.string.resolution_bottom_sheet_description4))
                    }
                },
                color = AppColors.TextBlack,
                fontSize = AppDimensions.dialogTextFontSizeMedium,
                lineHeight = AppDimensions.linePaddingDefault
            )
        },
        subTitleText = stringResource(id = R.string.resolution_bottom_sheet_options_title),
        descriptionBulletItems = (
                listOf(
                    stringResource(id = R.string.resolution_bottom_sheet_resolution_1mp) to listOf(
                        stringResource(id = R.string.resolution_bottom_sheet_1mp_fastest) to listOf(stringResource(id = R.string.resolution_bottom_sheet_1mp_fastest_bold)),
                        stringResource(id = R.string.resolution_bottom_sheet_1mp_best) to listOf(""),
                        stringResource(id = R.string.resolution_bottom_sheet_1mp_miss) to listOf("")
                    ),
                    stringResource(id = R.string.resolution_bottom_sheet_resolution_2mp) to listOf(
                        stringResource(id = R.string.resolution_bottom_sheet_2mp_good) to listOf(""),
                        stringResource(id = R.string.resolution_bottom_sheet_2mp_handles) to listOf(stringResource(id = R.string.resolution_bottom_sheet_2mp_handles_bold))
                    ),
                    stringResource(id = R.string.resolution_bottom_sheet_resolution_4mp) to listOf(
                        stringResource(id = R.string.resolution_bottom_sheet_4mp_detail) to listOf(""),
                        stringResource(id = R.string.resolution_bottom_sheet_4mp_memory) to listOf(stringResource(id = R.string.resolution_bottom_sheet_4mp_memory))
                    ),
                    stringResource(id = R.string.resolution_bottom_sheet_resolution_8mp) to listOf(
                        stringResource(id = R.string.resolution_bottom_sheet_8mp_maximum) to listOf(stringResource(id = R.string.resolution_bottom_sheet_8mp_maximum)),
                        stringResource(id = R.string.resolution_bottom_sheet_8mp_best) to listOf(""),
                        stringResource(id = R.string.resolution_bottom_sheet_8mp_slowest) to listOf(stringResource(id = R.string.resolution_bottom_sheet_8mp_slowest)),
                        stringResource(id = R.string.resolution_bottom_sheet_8mp_limited) to listOf(stringResource(id = R.string.resolution_bottom_sheet_8mp_limited_bold))
                    )
                )
                ),
        recommendationBulltetItems = (
                listOf(
                    stringResource(id = R.string.resolution_bottom_sheet_recommendation_title) to listOf(
                        stringResource(id = R.string.resolution_bottom_sheet_recommendation_start_res) to listOf(stringResource(id = R.string.resolution_bottom_sheet_recommendation_start_res_bold)),
                        stringResource(id = R.string.resolution_bottom_sheet_recommendation_increase_res) to listOf(
                            stringResource(id = R.string.resolution_bottom_sheet_recommendation_increase_res_bold_01),
                            stringResource(id = R.string.resolution_bottom_sheet_recommendation_increase_res_bold_02)
                        ),
                        stringResource(id = R.string.resolution_bottom_sheet_recommendation_match) to listOf(stringResource(id = R.string.resolution_bottom_sheet_recommendation_match_bold))
                    )
                )
                ),
        tipText = {
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                        append(stringResource(id = R.string.resolution_bottom_sheet_tip_title)) // Bold text
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                        append(stringResource(id = R.string.resolution_bottom_sheet_tip_make_sure))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(stringResource(id = R.string.resolution_bottom_sheet_tip_at_least))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                        append(stringResource(id = R.string.resolution_bottom_sheet_tip_in_image))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(stringResource(id = R.string.resolution_bottom_sheet_tip_experiment_res))
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
