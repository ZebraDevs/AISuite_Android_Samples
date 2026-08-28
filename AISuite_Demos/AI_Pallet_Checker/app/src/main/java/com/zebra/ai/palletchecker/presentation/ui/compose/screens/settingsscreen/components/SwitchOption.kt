// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.palletchecker.presentation.ui.compose.screens.settingsscreen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.zebra.ai.palletchecker.presentation.ui.compose.components.ZebraText
import com.zebra.ai.palletchecker.presentation.ui.theme.AppDimensions
import com.zebra.ai.palletchecker.presentation.ui.theme.switchCheckedThumbColor
import com.zebra.ai.palletchecker.presentation.ui.theme.switchCheckedTrackColor
import com.zebra.ai.palletchecker.presentation.ui.theme.switchOptionTextColor
import com.zebra.ai.palletchecker.presentation.ui.theme.switchUncheckedThumbColor
import com.zebra.ai.palletchecker.presentation.ui.theme.switchUncheckedTrackColor
import com.zebra.ai.palletchecker.presentation.ui.theme.textGrey

@Composable
fun SwitchOption(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
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
        Column(modifier = Modifier.weight(AppDimensions.WeightFull)) {
            ZebraText(
                textValue = title,
                fontSize = AppDimensions.dialogTextFontSizeSmall,
                textColor = switchOptionTextColor,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(AppDimensions.dimension_2dp))
                ZebraText(
                    textValue = subtitle,
                    fontSize = AppDimensions.dialogTextFontSizeExtraSmall,
                    textColor = textGrey,
                )
            }
        }
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
