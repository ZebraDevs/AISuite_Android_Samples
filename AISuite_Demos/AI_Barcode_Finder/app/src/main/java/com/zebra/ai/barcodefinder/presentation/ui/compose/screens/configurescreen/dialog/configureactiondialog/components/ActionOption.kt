// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodefinder.presentation.ui.compose.screens.configurescreen.dialog.configureactiondialog.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import com.zebra.ai.barcodefinder.presentation.ui.compose.components.ZebraIcon
import com.zebra.ai.barcodefinder.presentation.ui.compose.components.ZebraText
import com.zebra.ai.barcodefinder.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.presentation.ui.theme.borderPrimaryMain

@Composable
fun ActionOption(
    icon: Painter,
    iconColor: Color,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    overlayText: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppDimensions.dimension_6dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.dimension_8dp)
    ) {
        // Icon with colored background
        ZebraIcon(
            icon = icon,
            iconColor = iconColor,
            overlayText = overlayText,
            size = AppDimensions.dimension_24dp
        )

        // Text
        ZebraText(
            textValue = text,
            modifier = Modifier.weight(AppDimensions.WeightFull),
            fontWeight = FontWeight.Medium
        )

        // Radio button
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = borderPrimaryMain,
                unselectedColor = Color.Gray
            )
        )
    }
}