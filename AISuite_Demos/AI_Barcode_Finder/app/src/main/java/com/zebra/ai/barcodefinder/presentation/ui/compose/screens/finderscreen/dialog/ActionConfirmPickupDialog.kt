// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.presentation.ui.compose.screens.finderscreen.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import com.zebra.ai.barcodefinder.R
import com.zebra.ai.barcodefinder.presentation.enums.ButtonType
import com.zebra.ai.barcodefinder.presentation.ui.compose.components.ZebraButton
import com.zebra.ai.barcodefinder.presentation.ui.compose.components.ZebraIcon
import com.zebra.ai.barcodefinder.presentation.ui.compose.components.ZebraText
import com.zebra.ai.barcodefinder.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.presentation.ui.theme.AppShapes
import com.zebra.ai.barcodefinder.presentation.ui.theme.mainInverse

@Composable
fun ActionConfirmPickupDialog(
    productName: String = "",
    productId: String = "",
    onCancel: () -> Unit = {},
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .semantics{contentDescription="PickupActionConfirmDialog"}
                .padding(AppDimensions.MediumPadding),
            shape = AppShapes.large,
            colors = CardDefaults.cardColors(
                containerColor = mainInverse
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = AppDimensions.largeElevation
            )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimensions.LargePadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppDimensions.spacerHeight16)
            ) {

                item {
                    ZebraIcon(
                        icon = painterResource(id = R.drawable.confirm_pickup_marker),
                        iconColor = Color.Transparent,
                        overlayText = null,
                        size = AppDimensions.iconSizeLarge,
                        shapeRadius = AppDimensions.iconSizeLarge / 2,
                    )
                }

                item {
                    ZebraText(
                        textValue = stringResource(R.string.confirm_pickup_action_dialog_name),
                        modifier = Modifier.semantics{contentDescription="ConfirmActionDialogTitle"},
                        fontSize = AppDimensions.dialogTextFontSizeLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }

                // Product information
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(AppDimensions.spacerHeight12)
                    ) {
                        ZebraText(
                            textValue = productName,
                            fontSize = AppDimensions.dialogTextFontSizeMedium,
                            modifier = Modifier.semantics{contentDescription="ConfirmActionProductName"},
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        )

                        ZebraText(
                            textValue = "${stringResource(R.string.confirm_pickup_action_dialog_id)}: $productId",
                            modifier = Modifier.semantics{contentDescription="ConfirmActionProductID"},
                            fontSize = AppDimensions.dialogTextFontSizeSmall,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppDimensions.spacerHeight12)
                    ) {
                        // Cancel button as OutlinedButton
                        ZebraButton(
                            buttonType = ButtonType.Outlined,
                            text = stringResource(id = R.string.confirm_pickup_action_dialog_cancel),
                            onClick = onCancel,
                            modifier = Modifier.weight(1f),
                        )

                        // Confirm button as RaisedButton
                        ZebraButton(
                            buttonType = ButtonType.Raised,
                            text = stringResource(id = R.string.confirm_pickup_action_dialog_confirm),
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true, name = "Action Confirm Pickup Dialog")
@Composable
fun ActionConfirmPickupScreenPreview() {
    MaterialTheme {
        ActionConfirmPickupDialog()
    }
}
