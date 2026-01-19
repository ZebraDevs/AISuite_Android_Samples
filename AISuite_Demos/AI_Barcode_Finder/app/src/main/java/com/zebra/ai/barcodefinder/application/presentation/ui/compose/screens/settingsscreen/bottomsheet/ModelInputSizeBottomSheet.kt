// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.application.presentation.ui.compose.screens.settingsscreen.bottomsheet

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
import com.zebra.ai.barcodefinder.sdkcoordinator.enums.ModelInput
import com.zebra.ai.barcodefinder.application.presentation.ui.compose.screens.settingsscreen.components.ZebraBottomSheet
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.AppColors
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.application.presentation.viewmodel.SettingsViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelInputSizeBottomSheet(
    settingsViewModel: SettingsViewModel = viewModel(),
    currentSelection: ModelInput = ModelInput.SMALL_640,
    onBackClick: () -> Unit = {},
    onBackPressed: () -> Unit = {}
) {
    // State for collapsible section

    // Bottom Sheet Modal Design - takes up 75% of screen height
    ZebraBottomSheet(
        title = stringResource(id = R.string.model_input_size_bottom_sheet_title),
        onBackClick = onBackClick,
        descriptionText = {
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight(400))) {
                        append(stringResource(id = R.string.model_input_size_bottom_sheet_description1)) // Replacing hardcoded text
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight(700))) {
                        append(stringResource(id = R.string.model_input_size_bottom_sheet_description2)) // Replacing hardcoded text
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight(400))) {
                        append(stringResource(id = R.string.model_input_size_bottom_sheet_description3)) // Replacing hardcoded text
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight(700))) {
                        append(stringResource(id = R.string.model_input_size_bottom_sheet_note_label)) // Replacing hardcoded text
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight(400))) {
                        append(stringResource(id = R.string.model_input_size_bottom_sheet_description4)) // Replacing hardcoded text
                    }
                },
                color = AppColors.TextBlack,
                fontSize = AppDimensions.dialogTextFontSizeMedium,
                lineHeight = AppDimensions.linePaddingDefault
            )
        },
        subTitleText = (stringResource(id = R.string.model_input_size_bottom_sheet_input_size_options)),
        descriptionBulletItems = (
                listOf(
                    stringResource(id = R.string.model_input_size_bottom_sheet_input_small) to listOf(
                        stringResource(id = R.string.model_input_size_bottom_sheet_input_small_fastest) to listOf(stringResource(id = R.string.model_input_size_bottom_sheet_input_small_fastest_bold)),
                        stringResource(id = R.string.model_input_size_bottom_sheet_input_small_best) to listOf(""),
                        stringResource(id = R.string.model_input_size_bottom_sheet_input_small_miss) to listOf("")
                    ),
                    stringResource(id = R.string.model_input_size_bottom_sheet_input_medium) to listOf(
                        stringResource(id = R.string.model_input_size_bottom_sheet_input_medium_balanced) to listOf(
                            ""
                        ),
                        stringResource(id = R.string.model_input_size_bottom_sheet_input_medium_handles) to listOf("")
                    ),
                    stringResource(id = R.string.model_input_size_bottom_sheet_input_large) to listOf(
                        stringResource(id = R.string.model_input_size_bottom_sheet_input_large_accuracy) to listOf(stringResource(id = R.string.model_input_size_bottom_sheet_input_large_accuracy)),
                        stringResource(id = R.string.model_input_size_bottom_sheet_input_large_best) to listOf(""),
                        stringResource(id = R.string.model_input_size_bottom_sheet_input_large_slower) to listOf(
                            stringResource(id = R.string.model_input_size_bottom_sheet_input_large_slower_bold_01),
                            stringResource(id = R.string.model_input_size_bottom_sheet_input_large_slower_bold_02)
                        ),
                        stringResource(id = R.string.model_input_size_bottom_sheet_input_large_not_recommended) to listOf(
                            stringResource(id = R.string.model_input_size_bottom_sheet_input_large_not_recommended_bold)
                        )
                    )
                )
                ),
        recommendationBulltetItems = (
                listOf(
                    stringResource(id = R.string.model_input_size_bottom_sheet_recommendation_title) to listOf(
                        stringResource(id = R.string.model_input_size_bottom_sheet_recommendation_start) to listOf(stringResource(id = R.string.model_input_size_bottom_sheet_recommendation_start_bold)),
                        stringResource(id = R.string.model_input_size_bottom_sheet_recommendation_increase) to listOf(""),
                        stringResource(id = R.string.model_input_size_bottom_sheet_recommendation_demanding) to listOf(stringResource(id = R.string.model_input_size_bottom_sheet_recommendation_demanding_bold))
                    )
                )
                ),
        tipText = {
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                        append(stringResource(id = R.string.model_input_size_bottom_sheet_tip_title)) // Bold text
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                        append(stringResource(id = R.string.model_input_size_bottom_sheet_tip_larger_sizes))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(stringResource(id = R.string.model_input_size_bottom_sheet_tip_experiment))
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
fun ModelInputSizeScreenPreview() {
    MaterialTheme {
        ModelInputSizeBottomSheet(
            currentSelection = ModelInput.MEDIUM_1280,
            onBackClick = {}
        )
    }
}
