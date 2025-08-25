// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodefinder.data.model

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.ui.graphics.Color

/**
 * Data class representing an overlay item to be drawn on the camera preview
 */
data class BarcodeOverlayItem(
    val bounds: RectF,
    val actionableBarcode: ActionableBarcode? = null,
    val icon: Bitmap? = null,
    val text: String = "",
    val backgroundColor: Color = Color.Transparent
)
