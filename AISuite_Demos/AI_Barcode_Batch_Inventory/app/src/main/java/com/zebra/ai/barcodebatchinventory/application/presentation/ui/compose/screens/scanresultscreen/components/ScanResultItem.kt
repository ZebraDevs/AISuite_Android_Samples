// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.screens.scanresultscreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.zebra.ai.barcodebatchinventory.application.domain.model.ScanResult
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.components.ZebraText
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.theme.AppTextStyles.ScanResultTextSmall

@Composable
fun SimpleScanResultItem(result: ScanResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "BarcodeResult:${result.barcode};Quantity:${result.quantity}" }
            .padding(AppDimensions.MediumPadding),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SmallPadding)
    ) {
        // Barcode value with quantity on the right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Barcode value
            ZebraText(
                textValue = result.barcode,
                style = ScanResultTextSmall
            )

            // Quantity display - only show if quantity > 1
            if (result.quantity > 1) {
                ZebraText(
                    textValue = "Qty ${result.quantity}",
                    style = ScanResultTextSmall
                )
            }
        }
    }
}
