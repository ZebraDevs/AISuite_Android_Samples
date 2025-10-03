// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodefinder.presentation.ui.compose.screens.navbarscreen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.zebra.ai.barcodefinder.presentation.ui.compose.components.ZebraText
import com.zebra.ai.barcodefinder.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.presentation.ui.theme.white

@Composable
fun NavigationMenuItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(
                horizontal = AppDimensions.MediumPadding,
                vertical = AppDimensions.dimension_12dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.MediumPadding)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) white else white.copy(alpha = 0.5f),
            modifier = Modifier.size(AppDimensions.IconSize)
        )
        ZebraText(
            textValue = title,
            textColor = if (enabled) white else white.copy(alpha = 0.5f),
            fontSize = AppDimensions.dialogTextFontSizeMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
