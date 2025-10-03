// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.presentation.ui.compose.screens.finderscreen.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zebra.ai.barcodefinder.domain.enums.ActionType
import com.zebra.ai.barcodefinder.domain.model.ActionableBarcode
import com.zebra.ai.barcodefinder.presentation.ui.compose.screens.finderscreen.dialog.ActionConfirmPickupDialog
import com.zebra.ai.barcodefinder.presentation.ui.compose.screens.finderscreen.dialog.ActionProductRecallDialog
import com.zebra.ai.barcodefinder.presentation.ui.compose.screens.finderscreen.dialog.ActionQuantityPickupDialog
import com.zebra.ai.barcodefinder.presentation.viewmodel.FinderViewModel

@Composable
fun FinderActionHandler(
    selectedBarcode: ActionableBarcode?,
    showDialog: Boolean,
    onDismiss: () -> Unit,
    finderViewModel: FinderViewModel = viewModel()
) {
    // Observe EntityTracker UI state
    val uiState by finderViewModel.uiState.collectAsState()

    // Handle barcode selection
    LaunchedEffect(selectedBarcode, showDialog) {
        if (showDialog && selectedBarcode != null) {
            finderViewModel.selectBarcode(selectedBarcode)
        } else if (!showDialog) {
            finderViewModel.dismissDialog()
        }
    }

    // Display appropriate action screen based on barcode type
    if (uiState.showDialog && uiState.selectedBarcode != null && !uiState.selectedBarcode!!.isConfirmed) {
        when (uiState.selectedBarcode!!.actionType) {
            ActionType.TYPE_QUANTITY_PICKUP -> {
                val quantityToPick =
                    uiState.selectedBarcode!!.quantity // Set Configured value as default
                var quantityPicked by remember { mutableStateOf(quantityToPick.toString()) }
                var replenishStock by remember { mutableStateOf(false) }

                ActionQuantityPickupDialog(
                    productName = uiState.selectedBarcode!!.productName,
                    productId = uiState.selectedBarcode!!.barcodeData,
                    quantityToPick = uiState.selectedBarcode!!.quantity,
                    quantityPicked = quantityPicked,
                    replenishStock = replenishStock,
                    onQuantityPickedChange = { quantityPicked = it },
                    onReplenishStockChange = { replenishStock = it },
                    onCancel = {
                        finderViewModel.dismissDialog()
                        onDismiss()
                    },
                    onConfirm = {
                        val pickedQty = quantityPicked.toIntOrNull() ?: 0
                        finderViewModel.handleQuantityPickup(
                            barcode = uiState.selectedBarcode!!,
                            quantityPicked = pickedQty,
                            replenishStock = replenishStock
                        )
                        onDismiss()
                    },
                    onDismiss = {
                        finderViewModel.dismissDialog()
                        onDismiss()
                    }
                )
            }

            ActionType.TYPE_RECALL -> {
                ActionProductRecallDialog(
                    productName = uiState.selectedBarcode!!.productName,
                    productId = uiState.selectedBarcode!!.barcodeData,
                    onCancel = {
                        finderViewModel.dismissDialog()
                        onDismiss()
                    },
                    onConfirm = {
//                        uiState.selectedBarcode!!.isConfirmed = true
                        finderViewModel.handleProductRecall(uiState.selectedBarcode!!)
                        onDismiss()
                    },
                    onDismiss = {
                        finderViewModel.dismissDialog()
                        onDismiss()
                    }
                )
            }

            ActionType.TYPE_CONFIRM_PICKUP -> {
                ActionConfirmPickupDialog(
                    productName = uiState.selectedBarcode!!.productName,
                    productId = uiState.selectedBarcode!!.barcodeData,
                    onCancel = {
                        finderViewModel.dismissDialog()
                        onDismiss()
                    },
                    onConfirm = {
//                        uiState.selectedBarcode!!.isConfirmed = true
                        finderViewModel.handleConfirmPickup(uiState.selectedBarcode!!)
                        onDismiss()
                    },
                    onDismiss = {
                        finderViewModel.dismissDialog()
                        onDismiss()
                    }
                )
            }

            else -> {
                // For other action types, dismiss the dialog
                LaunchedEffect(Unit) {
                    finderViewModel.dismissDialog()
                    onDismiss()
                }
            }
        }
    }
}
