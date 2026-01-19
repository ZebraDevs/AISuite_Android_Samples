// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodefinder.application.domain.model

import android.graphics.Bitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sealed class representing the status of a barcode scan operation in the app.
 * Each subclass models a specific scan outcome, with associated UI text, icon, background color, and optional barcode icon.
 * Used to display scan results, status messages, and visual cues to the user after scanning a barcode.
 *
 * @property text The status message to display in the UI.
 * @property icon The icon representing the scan status (Compose ImageVector).
 * @property backgroundColor The background color for the status indicator.
 * @property overlayText Optional overlay text for additional info (e.g., quantity).
 * @property barcodeIcon Optional bitmap for a custom barcode icon.
 */
sealed class ScanStatus(
    val text: String,
    val icon: ImageVector,
    val backgroundColor: Color,
    val overlayText: String = "",
    open val barcodeIcon: Bitmap? = null
) {
    /**
     * Status indicating a product recall has been confirmed for the scanned barcode.
     */
    class RecallConfirmed(barcodeIcon: Bitmap? = null) : ScanStatus(
        text = "Recall has been confirmed",
        icon = Icons.Default.Warning,
        backgroundColor = Color(0xFFE53E3E),
        barcodeIcon = barcodeIcon
    )

    /**
     * Status indicating a pickup has been confirmed for the scanned barcode.
     */
    class PickupConfirmed(barcodeIcon: Bitmap? = null) : ScanStatus(
        text = "Pickup confirmed",
        icon = Icons.Default.Add,
        backgroundColor = Color(0xFF2196F3),
        barcodeIcon = barcodeIcon
    )

    /**
     * Status representing a scan event where a quantity of items has been picked, with optional replenish info.
     *
     * @property quantity The total quantity associated with the scan (displayed as overlay text).
     * @property pickedQuantity The number of items picked in this scan event.
     * @property replenish Whether the item needs to be replenished.
     * @property barcodeIcon Optional bitmap for a custom barcode icon.
     *
     * Usage:
     * - Displays the quantity as overlay text on the status indicator.
     * - Used for UI logic and visual cues, not for status messages.
     */
    class QuantityPicked(
        val quantity: Int,
        val pickedQuantity: Int,
        val replenish: Boolean,
        barcodeIcon: Bitmap? = null
    ) : ScanStatus(
        text = "Picked: $pickedQuantity${if (replenish) ", Replenish: Yes" else ""}",
        icon = Icons.Default.ShoppingCart,
        backgroundColor = Color(0xFF2196F3),
        overlayText = quantity.toString(),
        barcodeIcon = barcodeIcon
    )

    /**
     * Status indicating no action is needed for the scanned barcode.
     */
    class NoActionNeeded(barcodeIcon: Bitmap? = null) : ScanStatus(
        text = "No action needed",
        icon = Icons.Default.CheckCircle,
        backgroundColor = Color(0xFF4CAF50),
        barcodeIcon = barcodeIcon
    )
}
