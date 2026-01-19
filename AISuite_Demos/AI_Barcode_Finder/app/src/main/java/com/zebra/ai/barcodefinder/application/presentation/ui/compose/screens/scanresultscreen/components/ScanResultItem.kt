// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodefinder.application.presentation.ui.compose.screens.scanresultscreen.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.zebra.ai.barcodefinder.R
import com.zebra.ai.barcodefinder.application.domain.model.ActionableBarcode
import com.zebra.ai.barcodefinder.application.domain.model.ScanResult
import com.zebra.ai.barcodefinder.application.presentation.ui.compose.components.ZebraText
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.AppTextStyles.HamburgerDescriptionText
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.AppTextStyles.ScanResultTextSmall
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.white

@Composable
fun SimpleScanResultItem(result: ScanResult, barcode: ActionableBarcode?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppDimensions.MediumPadding),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SmallPadding)
    ) {
        // Product name
        ZebraText(
            textValue = result.productName,
        )

        // Barcode
        ZebraText(
            textValue = "${stringResource(R.string.scan_result_item_scan_barcode)} ${result.barcode}",
            style = ScanResultTextSmall
        )

        // Status with icon (always show, use barcodeIcon from ScanStatus if available)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SmallPadding)
        ) {

            val barcodeIcon = result.status.barcodeIcon
            Box(contentAlignment = Alignment.Center) {
                if (barcodeIcon != null) {
                    Image(
                        bitmap = barcodeIcon.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(AppDimensions.dimension_24dp)
                    )
                } else {
                    // Fallback with colored background
                    Box(
                        modifier = Modifier
                            .size(AppDimensions.dimension_24dp)
                            .background(
                                result.status.backgroundColor, shape = RoundedCornerShape(
                                    AppDimensions.halfPercentage
                                )
                            )
                    )
                    Icon(
                        imageVector = result.status.icon,
                        contentDescription = null,
                        tint = white,
                        modifier = Modifier.size(AppDimensions.modifier16)
                    )
                }

                // Add overlay text for quantity
                if (result.status.overlayText.isNotEmpty()) {
                    ZebraText(
                        textValue = result.status.overlayText,
                        textColor = white,
                        fontWeight = FontWeight.Bold,
                        fontSize = HamburgerDescriptionText.fontSize,
                    )
                }
            }

            // Status text
            ZebraText(
                textValue = result.status.text
                    .replace(stringResource(id = R.string.scan_result_item_replenish_yes), stringResource(id = R.string.scan_result_item_replenish_true))
                    .replace(stringResource(id = R.string.scan_result_item_replenish_no), stringResource(id = R.string.scan_result_item_replenish_false)),
                fontSize = ScanResultTextSmall.fontSize,
            )
        }
    }
}
