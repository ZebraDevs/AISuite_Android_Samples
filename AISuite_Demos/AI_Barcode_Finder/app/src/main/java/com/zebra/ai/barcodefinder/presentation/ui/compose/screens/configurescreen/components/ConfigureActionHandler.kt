// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.presentation.ui.compose.screens.configurescreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zebra.ai.barcodefinder.domain.enums.ActionType
import com.zebra.ai.barcodefinder.domain.model.ActionableBarcode
import com.zebra.ai.barcodefinder.presentation.ui.compose.screens.configurescreen.dialog.actionablebarcodedialog.ActionableBarcodeDialog
import com.zebra.ai.barcodefinder.presentation.ui.compose.screens.configurescreen.dialog.configureactiondialog.ConfigureActionDialog
import com.zebra.ai.barcodefinder.presentation.viewmodel.ConfigureViewModel

@Composable
fun ConfigureActionHandler(
    selectedBarcode: ActionableBarcode?,
    onDismiss: () -> Unit,
    onBarcodeConfigured: ((String, String, ActionType, Int) -> Unit)? = null,
    onShowActionableScreen: (() -> Unit)? = null,
    showActionableBarcodeDialog: Boolean = false,
    onGoToConfigureDemo: (() -> Unit)? = null,
    onNavigateToHome: () -> Unit = {},
    configureViewModel: ConfigureViewModel = viewModel()
) {
    LocalContext.current
    val selectedBarcodeState by configureViewModel.selectedBarcode.collectAsState()
    val showDialogState by configureViewModel.showConfigureActionDialog.collectAsState()

    // State for ConfigureActionScreen
    var selectedAction by remember { mutableStateOf(ActionType.TYPE_CONFIRM_PICKUP) }
    var productName by remember { mutableStateOf("") }
    var quantity by remember { mutableIntStateOf(1) }

    // Handle barcode selection
    LaunchedEffect(selectedBarcode, showDialogState) {
        if (showDialogState && selectedBarcode != null) {
            // Initialize product name from selected barcode
            productName = selectedBarcode.productName
            // Set default action based on barcode type
            selectedAction = selectedBarcode.actionType
            // Initialize quantity from barcode
            quantity = selectedBarcode.quantity
        } else if (!showDialogState) {
            configureViewModel.dismissConfigureActionDialog()
        }
    }

    // Display ConfigureActionScreen for any barcode type
    if (showDialogState && selectedBarcodeState != null) {
        ConfigureActionDialog(
            barcode = selectedBarcodeState!!.barcodeData,
            productName = productName,
            selectedAction = selectedAction,
            quantity = quantity,
            onActionSelected = { selectedAction = it },
            onProductNameChanged = { productName = it },
            onQuantityChanged = { quantity = it },
            onApply = {
                // Add to ActionableBarcodeScreen if callback is provided
                onBarcodeConfigured?.invoke(
                    selectedBarcodeState!!.barcodeData,
                    productName,
                    selectedAction,
                    quantity
                )
                configureViewModel.dismissConfigureActionDialog()
                onDismiss()

                // Show ActionableBarcodeScreen after applying
                onShowActionableScreen?.invoke()
            },
            onCancel = {
                configureViewModel.dismissConfigureActionDialog()
                onDismiss()
            },
            onDismiss = {
                configureViewModel.dismissConfigureActionDialog()
                onDismiss()
            }
        )
    }

    // Display ActionableBarcodeDialog when requested
    if (showActionableBarcodeDialog) {
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .background(com.zebra.ai.barcodefinder.presentation.ui.theme.AppDimensions.WeightHalf.let { alpha ->
                    androidx.compose.ui.graphics.Color.Black.copy(
                        alpha = alpha
                    )
                }),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            ActionableBarcodeDialog(
                configureViewModel = configureViewModel,
                onGoToConfigureDemo = onGoToConfigureDemo,
                onNavigateToHome = onNavigateToHome
            )
        }
    }
}
