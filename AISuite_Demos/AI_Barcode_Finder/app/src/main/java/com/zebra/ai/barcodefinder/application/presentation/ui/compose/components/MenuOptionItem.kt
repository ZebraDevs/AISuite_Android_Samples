// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.application.presentation.ui.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.zebra.ai.barcodefinder.application.presentation.model.MenuOption
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.CardDividerColor
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.gray

/**
 * Displays a single menu option item with icon, title, and optional divider.
 * Used in menu lists to represent selectable actions.
 *
 * @param option The menu option to display
 * @param showDivider Whether to show a divider below the item
 */
@Composable
fun MenuOptionItem(
    option: MenuOption,
    showDivider: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { option.onClick() }
                .padding(
                    horizontal = AppDimensions.dimension_16dp,
                    vertical = AppDimensions.dimension_12dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = option.title,
                tint = gray,
                modifier = Modifier.size(AppDimensions.dimension_20dp)
            )

            Spacer(modifier = Modifier.width(AppDimensions.dimension_12dp))

            ZebraText(
                textValue = option.title
            )
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = AppDimensions.dimension_16dp),
                color = CardDividerColor,
                thickness = AppDimensions.dimension_0_5dp
            )
        }
    }
}