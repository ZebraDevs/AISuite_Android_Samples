// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.palletchecker.presentation.ui.compose.screens.settingsscreen.bottomsheet

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
import com.zebra.ai.palletchecker.R
import com.zebra.ai.palletchecker.presentation.ui.compose.screens.settingsscreen.components.HyperlinkText
import com.zebra.ai.palletchecker.presentation.ui.compose.screens.settingsscreen.components.ZebraBottomSheet
import com.zebra.ai.palletchecker.presentation.ui.theme.AppColors
import com.zebra.ai.palletchecker.presentation.ui.theme.AppDimensions

/**
 * Official documentation for the inferencer performance mode (Balanced vs Burst/Turbo).
 */
private const val PERFORMANCE_MODE_DOC_URL =
    "https://techdocs.zebra.com/ai-datacapture/4-1/class/inferenceroptions/#performancemode"

/**
 * "More >" documentation bottom sheet for the Capture Mode / Turbo (Burst) setting.
 *
 * Explains the trade-offs between Standard (Balanced) and Turbo (Burst) capture modes and
 * links out to the official AI Data Capture SDK documentation. Follows the same
 * [ZebraBottomSheet] pattern used by the other Settings "More >" sheets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureModeBottomSheet(
    onBackClick: () -> Unit = {},
    onBackPressed: () -> Unit = {}
) {
    ZebraBottomSheet(
        title = stringResource(id = R.string.capture_mode_bottom_sheet_title),
        onBackClick = onBackClick,
        descriptionText = {
            Column {
                Text(
                    text = stringResource(id = R.string.capture_mode_bottom_sheet_description1),
                    fontSize = AppDimensions.dialogTextFontSizeMedium,
                    color = AppColors.TextBlack,
                    lineHeight = AppDimensions.linePaddingDefault
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(stringResource(id = R.string.capture_mode_bottom_sheet_note_label))
                        }
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                            append(stringResource(id = R.string.capture_mode_bottom_sheet_description2))
                        }
                    },
                    color = AppColors.TextBlack,
                    fontSize = AppDimensions.dialogTextFontSizeMedium,
                    lineHeight = AppDimensions.linePaddingDefault
                )

                HyperlinkText(
                    stringResource(id = R.string.capture_mode_bottom_sheet_doc_link),
                    PERFORMANCE_MODE_DOC_URL
                )
            }
        },
        subTitleText = stringResource(id = R.string.capture_mode_bottom_sheet_subtitle),
        descriptionBulletItems = listOf(
            stringResource(id = R.string.capture_mode_bottom_sheet_balanced_title) to listOf(
                stringResource(id = R.string.capture_mode_bottom_sheet_balanced_default) to listOf(
                    stringResource(id = R.string.capture_mode_bottom_sheet_balanced_default_bold)
                ),
                stringResource(id = R.string.capture_mode_bottom_sheet_balanced_power) to listOf("")
            ),
            stringResource(id = R.string.capture_mode_bottom_sheet_turbo_title) to listOf(
                stringResource(id = R.string.capture_mode_bottom_sheet_turbo_speed) to listOf(
                    stringResource(id = R.string.capture_mode_bottom_sheet_turbo_speed_bold)
                ),
                stringResource(id = R.string.capture_mode_bottom_sheet_turbo_power) to listOf(""),
                stringResource(id = R.string.capture_mode_bottom_sheet_turbo_support) to listOf("")
            )
        ),
        recommendationBulltetItems = listOf(
            stringResource(id = R.string.capture_mode_bottom_sheet_recommendation_title) to listOf(
                stringResource(id = R.string.capture_mode_bottom_sheet_recommendation) to listOf(
                    stringResource(id = R.string.capture_mode_bottom_sheet_recommendation_bold)
                )
            )
        ),
        tipText = {
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                        append(stringResource(id = R.string.capture_mode_bottom_sheet_tip_title))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                        append(stringResource(id = R.string.capture_mode_bottom_sheet_tip))
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
fun CaptureModeBottomSheetPreview() {
    MaterialTheme {
        CaptureModeBottomSheet(onBackClick = {})
    }
}
