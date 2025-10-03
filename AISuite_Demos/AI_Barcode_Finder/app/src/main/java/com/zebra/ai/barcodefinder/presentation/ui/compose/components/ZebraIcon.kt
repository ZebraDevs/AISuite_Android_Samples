// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.presentation.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.zebra.ai.barcodefinder.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.presentation.ui.theme.AppTextStyles.IconText

/**
 * Displays an icon or overlay text inside a rounded colored box.
 * Used for consistent icon rendering in the app, with optional text overlay.
 *
 * @param icon The painter for the icon to display
 * @param iconColor The background color of the icon box
 * @param overlayText Optional text to overlay instead of the icon
 * @param size The size of the icon box
 * @param shapeRadius The corner radius of the icon box
 * @param textStyle The style for overlay text
 */
@Composable
fun ZebraIcon(
    icon: Painter? = null,
    iconColor: Color = Color.Gray,
    overlayText: String? = null,
    size: Dp = AppDimensions.dimension_32dp,
    shapeRadius: Dp = AppDimensions.dimension_16dp,
    textStyle: TextStyle = IconText
) {
    Box(
        modifier = Modifier
            .size(size) // Use the size parameter directly
            .background(
                color = iconColor,
                shape = RoundedCornerShape(shapeRadius)
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            overlayText != null -> {
                ZebraText(
                    textValue = overlayText,
                    style = textStyle,
                )
            }

            icon != null -> {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = Color.Unspecified, // Retains original painter colors
                    modifier = Modifier.size(size) // Ensure inner icon also respects size
                )
            }
        }
    }
}
