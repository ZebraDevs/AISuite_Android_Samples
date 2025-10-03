// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodefinder.presentation.ui.compose.screens.configurescreen.dialog.actionablebarcodedialog.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import com.zebra.ai.barcodefinder.R
import com.zebra.ai.barcodefinder.domain.enums.ActionType
import com.zebra.ai.barcodefinder.domain.model.ActionableBarcode
import com.zebra.ai.barcodefinder.presentation.ui.compose.components.ZebraIcon
import com.zebra.ai.barcodefinder.presentation.ui.compose.components.ZebraText
import com.zebra.ai.barcodefinder.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.presentation.ui.theme.AppShapes
import com.zebra.ai.barcodefinder.presentation.ui.theme.AppTextStyles.ProductNameText
import com.zebra.ai.barcodefinder.presentation.ui.theme.boarderGray
import com.zebra.ai.barcodefinder.presentation.ui.theme.borderPrimaryMain
import com.zebra.ai.barcodefinder.presentation.ui.theme.gray
import com.zebra.ai.barcodefinder.presentation.ui.theme.iconRed
import com.zebra.ai.barcodefinder.presentation.ui.theme.white


@Composable
fun ActionableBarcodeItem(
    barcode: ActionableBarcode,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    icon : Bitmap?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                AppDimensions.dimension_0_5dp,
                boarderGray,
                AppShapes.small
            ), // Add a gray border
        colors = CardDefaults.cardColors(
            containerColor = white
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimensions.dimension_12dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconBitmap = icon

            if (iconBitmap != null && barcode.actionType != ActionType.TYPE_QUANTITY_PICKUP) {
                // Use Image directly if you have a bitmap
                Image(
                    bitmap = iconBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(AppDimensions.modifier32)
                )
            } else {
                val (iconColor, iconContent) = when (barcode.actionType) {
                    ActionType.TYPE_RECALL -> iconRed to "−"
                    ActionType.TYPE_QUANTITY_PICKUP -> borderPrimaryMain to "${barcode.quantity}"
                    ActionType.TYPE_CONFIRM_PICKUP -> borderPrimaryMain to "+"
                    else -> gray to "?"
                }

                // Use ZebraIcon for text overlay or fallback icon
                ZebraIcon(
                    icon = null,  // No painter provided for text scenario
                    iconColor = iconColor,
                    overlayText = iconContent,
                    size = AppDimensions.modifier32,
                    shapeRadius = AppDimensions.dimension_16dp // Use CircleShape if needed
                )
            }


            Spacer(modifier = Modifier.width(AppDimensions.modifier8))

            // Configuration name
            ZebraText(
                textValue = barcode.productName,
                textColor = ProductNameText.color,
                modifier = Modifier.weight(AppDimensions.WeightFull),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis

            )

            var expanded by remember { mutableStateOf(false) }

            // Menu button
            IconButton(
                modifier = Modifier.size(AppDimensions.dimension_32dp),
                onClick = { expanded = true }
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = gray,
                    modifier = Modifier
                        .width(AppDimensions.modifier20)
                        .height(AppDimensions.modifier20)
                )
            }

            DropdownMenu(
                expanded = expanded, // Control menu visibility using the expanded state
                onDismissRequest = {
                    expanded = false
                },// Dismiss menu when clicking outside or pressing back button
                modifier = Modifier.background(white), // Set background color to white
                offset = DpOffset(
                    x = AppDimensions.dropDownMenuOffset,
                    y = AppDimensions.dropDownMenuZeroOffset
                )
            ) {
                DropdownMenuItem(
                    text = { ZebraText(stringResource(R.string.actionable_barcode_item_edit)) }, // Display the "Edit" option
                    onClick = {
                        expanded = false // Dismiss menu after clicking "Edit"
                        onEditClick() // Invoke the onMenuClick callback only when "Edit" is clicked
                    },
                    leadingIcon = { // Add the leading icon for the "Edit" option
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.actionable_barcode_item_edit),
                            tint = gray // Set icon color to gray
                        )
                    }
                )
                DropdownMenuItem(
                    text = { ZebraText(stringResource(R.string.actionable_barcode_item_delete)) }, // Display the "Delete" option
                    onClick = {
                        onDeleteClick()
                        expanded = false // Dismiss menu after clicking "Delete"

                    },
                    leadingIcon = { // Add the leading icon for the "Delete" option
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.actionable_barcode_item_delete),
                            tint = gray // Set icon color to gray
                        )
                    }
                )
            }
        }
    }
}
