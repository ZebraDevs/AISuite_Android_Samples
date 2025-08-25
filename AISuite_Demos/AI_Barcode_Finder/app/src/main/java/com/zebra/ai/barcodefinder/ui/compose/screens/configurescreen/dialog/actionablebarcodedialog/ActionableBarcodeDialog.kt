// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.ui.compose.screens.configurescreen.dialog.actionablebarcodedialog

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zebra.ai.barcodefinder.R
import com.zebra.ai.barcodefinder.common.enums.ButtonType
import com.zebra.ai.barcodefinder.ui.compose.components.ZebraButton
import com.zebra.ai.barcodefinder.ui.compose.components.ZebraText
import com.zebra.ai.barcodefinder.ui.compose.screens.configurescreen.dialog.actionablebarcodedialog.components.ActionableBarcodeItem
import com.zebra.ai.barcodefinder.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.ui.theme.AppShapes
import com.zebra.ai.barcodefinder.ui.theme.AppTextStyles.ActionableBarcodeDialogTitleText
import com.zebra.ai.barcodefinder.ui.theme.borderPrimaryMain
import com.zebra.ai.barcodefinder.ui.theme.textWhite
import com.zebra.ai.barcodefinder.ui.theme.white
import com.zebra.ai.barcodefinder.viewmodel.ConfigureViewModel


@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionableBarcodeDialog(
    onApply: () -> Unit = {},
    onDismiss: () -> Unit = {},
    onGoToConfigureDemo: (() -> Unit)? = null,
    configureViewModel: ConfigureViewModel = viewModel(),
    onNavigateToHome: () -> Unit
) {
    val showDialog by configureViewModel.showActionableBarcodeDialog.collectAsState()
    if (!showDialog) return

    val actionableBarcodes by configureViewModel.configuredBarcodes.collectAsState()

    val configuration = LocalConfiguration.current
    val lazyColumnMaxHeight =
        if (configuration.screenHeightDp > 550) (configuration.screenHeightDp * 0.4f).dp else (configuration.screenHeightDp * 0.3f).dp

    // Match ConfigureActionScreen dialog style
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Dialog(onDismissRequest = {
            // Navigate to Home when clicking outside
            configureViewModel.showActionableBarcodeDialog(false) // Hide the dialog
            onNavigateToHome() // Navigate to Home
        }) {

            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = AppShapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = white
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = AppDimensions.largeElevation
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppDimensions.LargePadding),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(AppDimensions.spacerHeight16)
                ) {
                    // Title
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical = AppDimensions.LargePadding,
                                horizontal = AppDimensions.dimension_14dp
                            ),
                        contentAlignment = Alignment.Center
                    ) {

                        ZebraText(
                            style = ActionableBarcodeDialogTitleText.copy(fontWeight = FontWeight.Medium),
                            textValue = stringResource(R.string.current_barcode_configuration),
                            fontSize = ActionableBarcodeDialogTitleText.fontSize,
                            fontWeight = ActionableBarcodeDialogTitleText.fontWeight,
                            lineHeight = ActionableBarcodeDialogTitleText.lineHeight,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Barcode list
                    LazyColumn(
                        modifier = Modifier
                            .heightIn(max = lazyColumnMaxHeight), // Limit height to keep dialog reasonable
                        verticalArrangement = Arrangement.spacedBy(AppDimensions.dimension_12dp)
                    ) {
                        items(actionableBarcodes) { barcodeItem ->
                            ActionableBarcodeItem(
                                barcode = barcodeItem,
                                onEditClick = {
                                    configureViewModel.editBarcode(barcodeItem)
                                },
                                onDeleteClick = {
                                    configureViewModel.deleteBarcode(barcodeItem)
                                },
                                configureViewModel = configureViewModel
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .padding(top = AppDimensions.dimension_32dp)
                            .align(Alignment.CenterHorizontally)
                    ) {

                        // Add more barcodes button
                        ZebraButton(
                            buttonType = ButtonType.Text, // Use TextButton style
                            text = stringResource(R.string.add_more_barcode),
                            modifier = Modifier.padding(vertical = AppDimensions.dimension_8dp),
                            onClick = {
                                onGoToConfigureDemo?.invoke()
                                configureViewModel.showActionableBarcodeDialog(false) // Hide dialog via ViewModel
                                onDismiss() // Dismiss the dialog when navigating
                            },
//                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            textColor = borderPrimaryMain, // Set the text color
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.barcode_scan),
                                    contentDescription = null,
                                    tint = Color.Unspecified
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.add_more_barcode),
                                    contentDescription = null,
                                    tint = Color.Unspecified
                                )
                            }
                        )
                    }

                    // Bottom buttons - match ConfigureActionScreen style
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppDimensions.spacerHeight16)
                    ) {
                        // Clear All button - styled like a Cancel button
                        ZebraButton(
                            buttonType = ButtonType.Text, // Use TextButton style
                            text = stringResource(R.string.clear_all),
                            onClick = { configureViewModel.clearAllBarcodes() },
                            modifier = Modifier.weight(AppDimensions.WeightFull),
                            textColor = borderPrimaryMain,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.clear_all),
                                    tint = borderPrimaryMain,
                                    modifier = Modifier.size(AppDimensions.modifier20)
                                )
                            }
                        )

                        // Apply button - match ConfigureActionScreen style
                        ZebraButton(
                            buttonType = ButtonType.Raised, // Use RaisedButton style
                            text = stringResource(R.string.apply),
                            onClick = {
                                configureViewModel.onApplyActionableBarcodes()
                                onApply()
                                onNavigateToHome()
                                onDismiss() // Dismiss the dialog when Apply is clicked
                            },
                            modifier = Modifier.weight(AppDimensions.WeightFull),
                            textColor = textWhite, // Set the text color
                            backgroundColor = borderPrimaryMain, // Set the background color
                            shapes = AppShapes.small as RoundedCornerShape // Set the shape
                        )
                    }
                }
            }
        }
    }
}