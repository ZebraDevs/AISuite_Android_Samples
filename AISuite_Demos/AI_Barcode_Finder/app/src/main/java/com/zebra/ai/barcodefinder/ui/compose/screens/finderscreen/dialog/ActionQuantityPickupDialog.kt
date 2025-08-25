// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.ui.compose.screens.finderscreen.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import com.zebra.ai.barcodefinder.R
import com.zebra.ai.barcodefinder.common.enums.ButtonType
import com.zebra.ai.barcodefinder.ui.compose.components.ZebraButton
import com.zebra.ai.barcodefinder.ui.compose.components.ZebraIcon
import com.zebra.ai.barcodefinder.ui.compose.components.ZebraText
import com.zebra.ai.barcodefinder.ui.theme.AppColors
import com.zebra.ai.barcodefinder.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.ui.theme.AppShapes
import com.zebra.ai.barcodefinder.ui.theme.AppTextStyles.IconText
import com.zebra.ai.barcodefinder.ui.theme.boarderColor
import com.zebra.ai.barcodefinder.ui.theme.borderPrimaryMain
import com.zebra.ai.barcodefinder.ui.theme.gray
import com.zebra.ai.barcodefinder.ui.theme.mainInverse
import com.zebra.ai.barcodefinder.ui.theme.white

@Composable
fun ActionQuantityPickupDialog(
    productName: String = "Product name here",
    productId: String = stringResource(R.string.id_placeholder),
    quantityToPick: Int = 1,
    quantityPicked: String = "1",
    replenishStock: Boolean = false,
    onQuantityPickedChange: (String) -> Unit = {},
    onReplenishStockChange: (Boolean) -> Unit = {},
    onCancel: () -> Unit = {},
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
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
                        iconColor = borderPrimaryMain,
                        overlayText = quantityToPick.toString(),
                        size = AppDimensions.iconSizeLarge,
                        shapeRadius = AppDimensions.iconSizeLarge / 2,
                        textStyle = IconText.copy(
                            fontSize = AppDimensions.dialogTextFontSizeExtraLarge,
                            fontWeight = FontWeight.Bold,
                            color = white
                        )
                    )
                }

                item {
                    Text(
                        text = "${stringResource(R.string.quantity_to_be_picked)} $quantityToPick",
                        fontSize = AppDimensions.fontSize_18sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextBlack,
                        textAlign = TextAlign.Center
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
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )

                        ZebraText(
                            textValue = "${stringResource(R.string.id)}: $productId",
                            fontSize = AppDimensions.dialogTextFontSizeSmall,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Quantity Picked input field
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppDimensions.modifier20, AppDimensions.zeroPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppDimensions.modifier20)
                    ) {
                        ZebraText(
                            textValue = stringResource(R.string.quantity_picked),
                            fontSize = AppDimensions.dialogTextFontSizeMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(AppDimensions.WeightDobleAndHalf)
                        )

                        OutlinedTextField(
                            value = quantityPicked,
                            onValueChange = onQuantityPickedChange,
                            modifier = Modifier
                                .width(AppDimensions.dimension_64dp) // Set the fixed width here
                                .height(AppDimensions.dimension_48dp), // Optionally set a fixed height
                            placeholder = {
                                ZebraText(
                                    "",
                                    fontSize = AppDimensions.dialogTextFontSizeMedium
                                )
                            },
                            textStyle = TextStyle(
                                textAlign = TextAlign.Center
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = AppShapes.medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = boarderColor,
                                unfocusedBorderColor = Color.Gray.copy(alpha = AppDimensions.WeightHalf)
                            )
                        )
                    }
                }

                // Replenish Stock checkbox
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppDimensions.modifier20, AppDimensions.zeroPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppDimensions.spacerHeight8)
                    ) {
                        ZebraText(
                            textValue = stringResource(R.string.replenish_stock),
                            fontSize = AppDimensions.dialogTextFontSizeMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(AppDimensions.WeightTriple)
                        )

                        Checkbox(
                            checked = replenishStock,
                            onCheckedChange = onReplenishStockChange,
                            modifier = Modifier.weight(AppDimensions.WeightFull),
                            colors = CheckboxDefaults.colors(
                                checkedColor = borderPrimaryMain,
                                uncheckedColor = gray
                            )
                        )
                    }
                }

                // Action buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppDimensions.modifier20)
                    ) {
                        ZebraButton(
                            buttonType = ButtonType.Outlined,
                            text = stringResource(R.string.cancel),
                            onClick = onCancel,
                            modifier = Modifier.weight(AppDimensions.WeightFull)
                        )
                        ZebraButton(
                            buttonType = ButtonType.Raised,
                            text = stringResource(R.string.confirm),
                            onClick = onConfirm,
                            modifier = Modifier.weight(AppDimensions.WeightFull),
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewActionQuantityPickupDialog() {
    ActionQuantityPickupDialog(
        productName = "Sample Product",
        productId = "12345",
        quantityToPick = 10,
        quantityPicked = "5",
        replenishStock = true,
        onQuantityPickedChange = {},
        onReplenishStockChange = {},
        onCancel = {},
        onConfirm = {},
        onDismiss = {}
    )
}
