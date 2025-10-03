// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodefinder.presentation.ui.compose.screens.settingsscreen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.zebra.ai.barcodefinder.presentation.ui.compose.components.ZebraText
import com.zebra.ai.barcodefinder.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.presentation.ui.theme.SelectedRadioButton

@Composable
fun RadioButtonOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onSelected: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected() },
        verticalAlignment = Alignment.Top
    ) {
        Spacer(modifier = Modifier.width(AppDimensions.dimension_12dp))
        Column(
            modifier = Modifier
                .weight(AppDimensions.WeightFull)
                .padding(top = AppDimensions.dimension_12dp)
        ) {
            ZebraText(
                textValue = title,
                fontSize = AppDimensions.radioButtonTitleFontSize,
                lineHeight = AppDimensions.radioButtonTitleLineHeight
            )
            if (subtitle.isNotEmpty()) {
                ZebraText(
                    textValue = subtitle,
                    textColor = Color.Gray,
                    fontSize = AppDimensions.dialogTextFontSizeExtraSmall,
                    lineHeight = AppDimensions.dialogTextFontSizeMedium,
                    modifier = Modifier.padding(top = AppDimensions.dimension_2dp),
                )
            }
        }
        RadioButton(

            selected = selected,
            onClick = onSelected,
            colors = RadioButtonDefaults.colors(
                selectedColor = SelectedRadioButton,
                unselectedColor = Color.Gray
            )
        )
    }
}
