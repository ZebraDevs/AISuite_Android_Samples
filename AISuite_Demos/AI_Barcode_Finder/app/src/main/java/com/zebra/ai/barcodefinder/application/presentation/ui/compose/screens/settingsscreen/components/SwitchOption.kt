// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodefinder.application.presentation.ui.compose.screens.settingsscreen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.zebra.ai.barcodefinder.application.presentation.ui.compose.components.ZebraText
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.switchCheckedThumbColor
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.switchCheckedTrackColor
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.switchOptionTextColor
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.switchUncheckedThumbColor
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.switchUncheckedTrackColor

@Composable
fun SwitchOption(
    title: String,
    subtitle: String? = null, // Optional subtitle parameter with default value null
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(
                horizontal = AppDimensions.dimension_16dp,
                vertical = AppDimensions.dimension_8dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Use a Column to stack title and subtitle
        Column(
            modifier = Modifier
                .weight(1f) // Ensures the Column takes up the available space
        ) {
            // Title text
            ZebraText(
                textValue = title,
                fontSize = AppDimensions.radioButtonTitleFontSize,
                lineHeight = AppDimensions.radioButtonTitleLineHeight,
                textColor = switchOptionTextColor,
            )

            // Subtitle text, only shown if subtitle is not null or empty
            if (!subtitle.isNullOrEmpty()) {
                ZebraText(
                    textValue = subtitle,
                    textColor = Color.Gray, // Subtitle color
                    fontSize = AppDimensions.dialogTextFontSizeExtraSmall,
                    lineHeight = AppDimensions.dialogTextFontSizeMedium,
                    modifier = Modifier.padding(top = AppDimensions.dimension_2dp) // Add spacing between title and subtitle
                )
            }
        }

        // Switch component
        Switch(
            checked = checked,
            onCheckedChange = null, // The clickable modifier handles the toggle
            colors = SwitchDefaults.colors(
                checkedThumbColor = switchCheckedThumbColor,
                checkedTrackColor = switchCheckedTrackColor,
                uncheckedThumbColor = switchUncheckedThumbColor,
                uncheckedTrackColor = switchUncheckedTrackColor
            )
        )
    }
}
