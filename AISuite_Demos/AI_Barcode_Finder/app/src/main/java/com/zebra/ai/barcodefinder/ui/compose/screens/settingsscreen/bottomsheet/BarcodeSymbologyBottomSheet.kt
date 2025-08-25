// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.ui.compose.screens.settingsscreen.bottomsheet

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zebra.ai.barcodefinder.R
import com.zebra.ai.barcodefinder.data.model.BarcodeSymbology
import com.zebra.ai.barcodefinder.ui.compose.screens.settingsscreen.components.ZebraBottomSheet
import com.zebra.ai.barcodefinder.ui.theme.AppColors
import com.zebra.ai.barcodefinder.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeSymbologyBottomSheet(
    settingsViewModel: SettingsViewModel = viewModel(),
    currentSymbology: BarcodeSymbology = BarcodeSymbology(),
    onBackClick: () -> Unit = {},
    onBackPressed: () -> Unit = {}
) {
    // State for collapsible section

    // Bottom Sheet Modal Design - takes up 75% of screen height
    ZebraBottomSheet(
        title = stringResource(id = R.string.setting_barcode_symbologies_title),
        onBackClick = onBackClick,
        descriptionText = {
            Column {
                Text(
                    buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight(400))) {
                            append(stringResource(id = R.string.setting_barcode_symbology_description1))
                        }
                        withStyle(style = SpanStyle(fontWeight = FontWeight(700))) {
                            append(stringResource(id = R.string.setting_barcode_symbology_description2))
                        }
                        withStyle(style = SpanStyle(fontWeight = FontWeight(400))) {
                            append(stringResource(id = R.string.setting_barcode_symbology_description3))
                        }
                    },
                    color = AppColors.TextBlack,
                    fontSize = AppDimensions.dialogTextFontSizeMedium,
                    lineHeight = AppDimensions.linePaddingDefault
                )

                Row(modifier = Modifier.padding(top = AppDimensions.spacerHeight8)) {
                    Text(
                        buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight(700))) {
                                append(stringResource(id = R.string.note_label))
                            }
                            withStyle(style = SpanStyle(fontWeight = FontWeight(400))) {
                                append(stringResource(id = R.string.setting_symbology_optimization_info))
                            }
                        },
                        color = AppColors.TextBlack,
                        fontSize = AppDimensions.dialogTextFontSizeMedium,
                        lineHeight = AppDimensions.linePaddingDefault
                    )
                }
            }
        },
        subTitleText = stringResource(id = R.string.setting_barcode_symbology_types),
        descriptionBulletItems = (
                listOf(
                    stringResource(id = R.string.setting_1d_barcodes_title) to listOf(
                        stringResource(id = R.string.setting_1d_barcodes_description) to listOf(""),
                        stringResource(id = R.string.setting_1d_barcodes_usage) to listOf(""),
                        stringResource(id = R.string.setting_1d_barcodes_advantages) to listOf("")
                    ),
                    stringResource(id = R.string.setting_2d_barcodes_title) to listOf(
                        stringResource(id = R.string.setting_2d_barcodes_description) to listOf(""),
                        stringResource(id = R.string.setting_2d_barcodes_advantages) to listOf(""),
                        stringResource(id = R.string.setting_2d_barcodes_capability) to listOf(
                            ""
                        )
                    ),
                    stringResource(id = R.string.setting_gs1_barcodes_title) to listOf(
                        stringResource(id = R.string.setting_gs1_barcodes_description) to listOf(""),
                        stringResource(id = R.string.setting_gs1_barcodes_types) to listOf(""),
                        stringResource(id = R.string.setting_gs1_barcodes_usage) to listOf("")
                    ),
                    stringResource(id = R.string.setting_postal_barcodes_title) to listOf(
                        stringResource(id = R.string.setting_postal_barcodes_description) to listOf(
                            ""
                        ),
                        stringResource(id = R.string.setting_postal_barcodes_formats) to listOf(""),
                        stringResource(id = R.string.setting_postal_barcodes_advantages) to listOf("")
                    )
                )
                ),
        recommendationBulltetItems = null
    )

    BackHandler {
        onBackPressed()
    }
}

@Preview(showBackground = true)
@Composable
fun BarcodeSymbologyScreenPreview() {
    MaterialTheme {
        BarcodeSymbologyBottomSheet(
            currentSymbology = BarcodeSymbology(),
            onBackClick = {}
        )
    }
}
