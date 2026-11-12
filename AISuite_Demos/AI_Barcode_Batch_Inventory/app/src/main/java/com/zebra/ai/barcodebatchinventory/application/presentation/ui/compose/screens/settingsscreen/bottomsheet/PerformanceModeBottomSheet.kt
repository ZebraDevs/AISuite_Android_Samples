// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.screens.settingsscreen.bottomsheet

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
import com.zebra.ai.barcodebatchinventory.R
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.screens.settingsscreen.components.HyperlinkText
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.screens.settingsscreen.components.ZebraBottomSheet
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.theme.AppColors
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.theme.AppDimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceModeBottomSheet(
    onBackClick: () -> Unit = {},
    onBackPressed: () -> Unit = {}
) {
    ZebraBottomSheet(
        title = stringResource(id = R.string.performance_mode_bottom_sheet_title),
        onBackClick = onBackClick,
        descriptionText = {
            Column {
                Text(
                    text = stringResource(id = R.string.performance_mode_bottom_sheet_description),
                    fontSize = AppDimensions.dialogTextFontSizeMedium,
                    color = AppColors.TextBlack,
                    lineHeight = AppDimensions.linePaddingDefault
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(stringResource(id = R.string.performance_mode_bottom_sheet_note_label))
                        }
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                            append(stringResource(id = R.string.performance_mode_bottom_sheet_note))
                        }
                    },
                    color = AppColors.TextBlack,
                    fontSize = AppDimensions.dialogTextFontSizeMedium,
                    lineHeight = AppDimensions.linePaddingDefault
                )

                HyperlinkText(
                    stringResource(id = R.string.performance_mode_bottom_sheet_docs_link),
                    stringResource(id = R.string.performance_mode_bottom_sheet_docs_url)
                )
            }
        },
        subTitleText = stringResource(id = R.string.performance_mode_bottom_sheet_subtitle),
        descriptionBulletItems = listOf(
            stringResource(id = R.string.performance_mode_bottom_sheet_standard_title) to listOf(
                stringResource(id = R.string.performance_mode_bottom_sheet_standard_default) to listOf(
                    stringResource(id = R.string.performance_mode_bottom_sheet_standard_default_bold)
                ),
                stringResource(id = R.string.performance_mode_bottom_sheet_standard_detail) to listOf("")
            ),
            stringResource(id = R.string.performance_mode_bottom_sheet_turbo_title) to listOf(
                stringResource(id = R.string.performance_mode_bottom_sheet_turbo_speed) to listOf(
                    stringResource(id = R.string.performance_mode_bottom_sheet_turbo_speed_bold)
                ),
                stringResource(id = R.string.performance_mode_bottom_sheet_turbo_power) to listOf(""),
                stringResource(id = R.string.performance_mode_bottom_sheet_turbo_devices) to listOf("")
            )
        ),
        recommendationBulltetItems = listOf(
            stringResource(id = R.string.performance_mode_bottom_sheet_recommendation_title) to listOf(
                stringResource(id = R.string.performance_mode_bottom_sheet_recommendation) to listOf(
                    stringResource(id = R.string.performance_mode_bottom_sheet_recommendation_bold)
                )
            )
        ),
        tipText = {
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                        append(stringResource(id = R.string.inference_bottom_sheet_tip_title))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                        append(stringResource(id = R.string.performance_mode_bottom_sheet_tip))
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
fun PerformanceModeScreenPreview() {
    MaterialTheme {
        PerformanceModeBottomSheet(onBackClick = {})
    }
}
