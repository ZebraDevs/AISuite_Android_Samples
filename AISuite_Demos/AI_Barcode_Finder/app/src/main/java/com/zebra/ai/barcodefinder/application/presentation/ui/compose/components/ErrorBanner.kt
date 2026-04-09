// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.application.presentation.ui.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.AppColors
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.AppTextStyles

/**
 * Displays an error banner at the top of the screen.
 * Used for non-blocking error messages that don't require immediate action.
 *
 * @param message The error message to display
 * @param isVisible Whether the banner should be visible
 * @param onDismiss Callback when user dismisses the banner
 */
@Composable
fun ErrorBanner(
    message: String,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFEBEE)) // Light red background
                .padding(
                    horizontal = AppDimensions.dimension_16dp,
                    vertical = AppDimensions.dimension_12dp
                ),
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.dimension_12dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error",
                tint = Color(0xFFD32F2F), // Red icon
                modifier = Modifier.size(24.dp)
            )
            
            ZebraText(
                textValue = message,
                style = AppTextStyles.AboutBoldTextStyle,
                textColor = AppColors.TextBlack,
                modifier = Modifier.weight(1f)
            )
            
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color(0xFFD32F2F)
                )
            }
        }
    }
}
