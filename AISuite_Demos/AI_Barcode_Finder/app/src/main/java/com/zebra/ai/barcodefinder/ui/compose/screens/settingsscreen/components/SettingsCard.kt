// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodefinder.ui.compose.screens.settingsscreen.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import com.zebra.ai.barcodefinder.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.ui.theme.settingsCardHeaderBackgroundColor
import com.zebra.ai.barcodefinder.ui.theme.settingsCardIconTintColor
import com.zebra.ai.barcodefinder.ui.theme.settingsCardTitleColor
import com.zebra.ai.barcodefinder.ui.theme.white

@Composable
fun SettingsCard(
    title: String,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = white), // White background for the main card
        elevation = CardDefaults.cardElevation(defaultElevation = AppDimensions.smallWidth),
        shape = RectangleShape
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppDimensions.zeroPadding)
                .animateContentSize(animationSpec = spring())
        ) {
            // Section Header with slightly gray background
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(settingsCardHeaderBackgroundColor) // Slightly gray background for header
                    .clickable { onExpandToggle() }
                    .padding(AppDimensions.dimension_8dp)
                    .height(AppDimensions.dimension_24dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = AppDimensions.dialogTextFontSizeMedium,
                    fontWeight = FontWeight(AppDimensions.fontWeight500),
                    lineHeight = AppDimensions.linePaddingDefault,
                    color = settingsCardTitleColor // Darker text color
                )
                Icon(
                    imageVector = if (isExpanded)
                        Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = settingsCardIconTintColor // Medium gray for icon
                )
            }

            // Animated expandable content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(AppDimensions.animationDurationMillis)),
                exit = shrinkVertically(animationSpec = tween(AppDimensions.animationDurationMillis))
            ) {
                content()
            }
        }
    }
}
