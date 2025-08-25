// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodefinder.ui.compose.screens.settingsscreen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.zebra.ai.barcodefinder.ui.compose.components.ZebraText
import com.zebra.ai.barcodefinder.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.ui.theme.switchCheckedThumbColor
import com.zebra.ai.barcodefinder.ui.theme.switchCheckedTrackColor
import com.zebra.ai.barcodefinder.ui.theme.switchOptionTextColor
import com.zebra.ai.barcodefinder.ui.theme.switchUncheckedThumbColor
import com.zebra.ai.barcodefinder.ui.theme.switchUncheckedTrackColor

@Composable
fun SwitchOption(
    title: String,
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
        ZebraText(
            textValue = title,
            fontSize = AppDimensions.dialogTextFontSizeSmall,
            textColor = switchOptionTextColor,
            modifier = Modifier.weight(AppDimensions.WeightFull)
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = switchCheckedThumbColor,
                checkedTrackColor = switchCheckedTrackColor,
                uncheckedThumbColor = switchUncheckedThumbColor,
                uncheckedTrackColor = switchUncheckedTrackColor
            )
        )
    }
}
