// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodefinder.ui.compose.screens.configurescreen.dialog.configureactiondialog.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.zebra.ai.barcodefinder.R
import com.zebra.ai.barcodefinder.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.ui.theme.gray

@Composable
fun QuantityPicker(
    quantity: Int,
    onQuantityChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Initialize displayQuantity with the initial quantity value (allowing it to be 0)
    var displayQuantity by remember { mutableStateOf(quantity.toString()) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.dimension_8dp)
    ) {
        // Minus button
        OutlinedButton(
            onClick = {
                val currentQuantity = displayQuantity.toIntOrNull() ?: 0
                if (currentQuantity > 0) {
                    val newQuantity = currentQuantity - 1
                    displayQuantity = newQuantity.toString()
                    onQuantityChanged(newQuantity)
                }
            },
            modifier = Modifier.size(AppDimensions.dimension_48dp),
            contentPadding = PaddingValues(AppDimensions.zeroPadding),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.Gray
            ),
            shape = RoundedCornerShape(AppDimensions.dimension_4dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.quantity_remove),
                contentDescription = null,
                tint = Color.Unspecified
            )
        }

        // Quantity display
        OutlinedTextField(
            value = displayQuantity,
            onValueChange = { newValue ->
                if (newValue.isEmpty()) {
                    displayQuantity = ""
                    onQuantityChanged(0)
                } else {
                    newValue.toIntOrNull()?.let {
                        displayQuantity = newValue
                        onQuantityChanged(it)
                    }
                }
            },
            modifier = Modifier
                .width(AppDimensions.dimension_72dp)
                .height(AppDimensions.dimension_48dp),
            textStyle = TextStyle(
                textAlign = TextAlign.Center,
                fontSize = AppDimensions.dialogTextFontSizeMedium,
                fontWeight = FontWeight.Medium
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = gray,
                unfocusedBorderColor = gray
            )
        )

        // Plus button
        OutlinedButton(
            onClick = {
                val currentQuantity = displayQuantity.toIntOrNull() ?: 0
                val newQuantity = currentQuantity + 1
                displayQuantity = newQuantity.toString()
                onQuantityChanged(newQuantity)
            },
            modifier = Modifier.size(AppDimensions.dimension_48dp),
            contentPadding = PaddingValues(AppDimensions.zeroPadding),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = gray
            ),
            shape = RoundedCornerShape(AppDimensions.dimension_4dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.quantity_add),
                contentDescription = null,
                tint = Color.Unspecified
            )
        }
    }
}
