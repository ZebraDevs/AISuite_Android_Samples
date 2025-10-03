// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.presentation.ui.compose.screens.configurescreen.dialog.configureactiondialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.zebra.ai.barcodefinder.R
import com.zebra.ai.barcodefinder.domain.enums.ActionType
import com.zebra.ai.barcodefinder.presentation.enums.ButtonType
import com.zebra.ai.barcodefinder.presentation.ui.compose.components.ZebraButton
import com.zebra.ai.barcodefinder.presentation.ui.compose.components.ZebraText
import com.zebra.ai.barcodefinder.presentation.ui.compose.screens.configurescreen.dialog.configureactiondialog.components.ActionOption
import com.zebra.ai.barcodefinder.presentation.ui.compose.screens.configurescreen.dialog.configureactiondialog.components.QuantityPicker
import com.zebra.ai.barcodefinder.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.presentation.ui.theme.AppShapes
import com.zebra.ai.barcodefinder.presentation.ui.theme.AppTextStyles.ConfigDialogTitleText
import com.zebra.ai.barcodefinder.presentation.ui.theme.borderPrimaryMain
import com.zebra.ai.barcodefinder.presentation.ui.theme.gray
import com.zebra.ai.barcodefinder.presentation.ui.theme.iconBlack
import com.zebra.ai.barcodefinder.presentation.ui.theme.iconBlue
import com.zebra.ai.barcodefinder.presentation.ui.theme.iconGreen
import com.zebra.ai.barcodefinder.presentation.ui.theme.iconRed
import com.zebra.ai.barcodefinder.presentation.ui.theme.textBlack
import com.zebra.ai.barcodefinder.presentation.ui.theme.textGrey
import com.zebra.ai.barcodefinder.presentation.ui.theme.white


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigureActionDialog(
    barcode: String = "",
    productName: String = "",
    selectedAction: ActionType = ActionType.TYPE_CONFIRM_PICKUP,
    quantity: Int = 1,
    onActionSelected: (ActionType) -> Unit = {},
    onProductNameChanged: (String) -> Unit = {},
    onQuantityChanged: (Int) -> Unit = {},
    onApply: () -> Unit = {},
    onCancel: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {

    // Determine if the "Apply" button should be enabled
    val isApplyEnabled = if (selectedAction == ActionType.TYPE_QUANTITY_PICKUP) {
        quantity > 0
    } else {
        true
    }


    val configuration = LocalConfiguration.current
    var lazyColumnMaxHeight =
        if (configuration.screenHeightDp > 550) (configuration.screenHeightDp * 0.5f).dp else (configuration.screenHeightDp * 0.4f).dp

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .semantics{contentDescription="ConfigureActionDialog"}
                .padding(AppDimensions.LargePadding),
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
                horizontalAlignment = Alignment.Start
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppDimensions.dimension_56dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Barcode header
                    ZebraText(
                        textValue = "${stringResource(R.string.configure_action_dialog_barcode)}: $barcode",
                        textColor = textBlack,
                        style = ConfigDialogTitleText
                    )
                }
                Spacer(modifier = Modifier.height(AppDimensions.spacerHeight6))
                // Product Name field
                OutlinedTextField(
                    value = productName,
                    onValueChange = onProductNameChanged,
                    label = {
                        ZebraText(
                            stringResource(R.string.configure_action_dialog_product_name),
                            textColor = textGrey
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.product_name),
                            contentDescription = null,
                            tint = iconBlack
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics{contentDescription = "ProductNameTextField"},
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = gray,
                        unfocusedBorderColor = gray,
                        focusedLabelColor = gray,
                        unfocusedLabelColor = gray
                    ),
                    singleLine = true
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(
                            AppDimensions.WeightFull,
                            fill = false
                        ) // Allows it to take up as much space as needed, but not more
                        .heightIn(max = lazyColumnMaxHeight),  // Limit height to keep dialog reasonable
                    verticalArrangement = Arrangement.spacedBy(AppDimensions.spacerHeight8)
                ) {
                    item {
                        ActionOption(
                            icon = painterResource(id = R.drawable.confirm_pickup_marker),
                            iconColor = iconBlue,
                            text = stringResource(R.string.configure_action_dialog_pickup),
                            isSelected = selectedAction == ActionType.TYPE_CONFIRM_PICKUP,
                            onClick = { onActionSelected(ActionType.TYPE_CONFIRM_PICKUP) },
                            modifier = Modifier.semantics{contentDescription="PickupActionRadioButton"}
                        )

                        // Quantity to pick with conditional quantity picker
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ActionOption(
                                icon = painterResource(id = R.drawable.quantity_marker),
                                iconColor = iconBlue,
                                text = stringResource(R.string.configure_action_dialog_quantity_to_pick),
                                isSelected = selectedAction == ActionType.TYPE_QUANTITY_PICKUP,
                                onClick = { onActionSelected(ActionType.TYPE_QUANTITY_PICKUP) },
                                overlayText = "#",
                                modifier = Modifier.semantics{contentDescription="QuantityPickupActionRadioButton"}
                            )

                            // Show quantity picker when "Quantity to pick" is selected
                            if (selectedAction == ActionType.TYPE_QUANTITY_PICKUP) {
                                Spacer(modifier = Modifier.height(AppDimensions.spacerHeight8))
                                QuantityPicker(
                                    quantity = quantity,
                                    onQuantityChanged = onQuantityChanged
                                )
                            }
                        }

                        ActionOption(
                            icon = painterResource(id = R.drawable.recall_marker),
                            iconColor = iconRed,
                            text = stringResource(R.string.configure_action_dialog_product_recall),
                            isSelected = selectedAction == ActionType.TYPE_RECALL,
                            onClick = { onActionSelected(ActionType.TYPE_RECALL) },
                            modifier = Modifier.semantics{contentDescription="ProductRecallActionRadioButton"}
                        )

                        ActionOption(
                            icon = painterResource(id = R.drawable.no_action_marker),
                            iconColor = iconGreen,
                            text = stringResource(R.string.configure_action_dialog_no_action),
                            isSelected = selectedAction == ActionType.TYPE_NO_ACTION,
                            onClick = { onActionSelected(ActionType.TYPE_NO_ACTION) },
                            modifier = Modifier.semantics{contentDescription="NoActionRadioButton"}
                        )
                    }
                }
                Spacer(modifier = Modifier.height(AppDimensions.spacerHeight16))
                // Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppDimensions.dimension_48dp), // Adjust this weight as needed
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.spacerHeight16)
                ) {

                    ZebraButton(
                        buttonType = ButtonType.Text, // Assuming you want a raised button
                        text = stringResource(R.string.configure_action_dialog_cancel),
                        onClick = onCancel,
                        modifier = Modifier.weight(AppDimensions.WeightFull),
                        textColor = borderPrimaryMain,
                    )

                    ZebraButton(
                        buttonType = ButtonType.Raised, // Assuming you want a raised button
                        text = stringResource(R.string.configure_action_dialog_apply),
                        onClick = {
                            val finalProductName =
                                if (productName.isBlank()) barcode.trim() else productName.trim()
                            onProductNameChanged(finalProductName)
                            onApply()
                        },
                        enabled = isApplyEnabled, // Enable or disable based on the condition
                        modifier = Modifier.weight(AppDimensions.WeightFull),
                    )
                }
            }
        }
    }
}


@Preview(showBackground = true, name = "Configure Action Screen")
@Composable
fun ConfigureActionScreenPreview() {
    var selectedAction by remember { mutableStateOf(ActionType.TYPE_CONFIRM_PICKUP) }
    var productName by remember { mutableStateOf("") }

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = AppDimensions.WeightHalf)),
            contentAlignment = Alignment.Center
        ) {
            ConfigureActionDialog(
                quantity = 1,
                selectedAction = selectedAction,
                productName = productName,
                onActionSelected = { selectedAction = it },
                onProductNameChanged = { productName = it }
            )
        }
    }
}

@Preview(showBackground = true, name = "Configure Action - Quantity Selected")
@Composable
fun ConfigureActionQuantityPreview() {
    var selectedAction by remember { mutableStateOf<ActionType>(ActionType.TYPE_QUANTITY_PICKUP) }
    var productName by remember { mutableStateOf("") }
    var quantity by remember { mutableIntStateOf(1) }

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            ConfigureActionDialog(
                productName = productName,
                selectedAction = selectedAction,
                quantity = quantity,
                onActionSelected = { selectedAction = it },
                onProductNameChanged = { productName = it },
                onQuantityChanged = { quantity = it }
            )
        }
    }
}
