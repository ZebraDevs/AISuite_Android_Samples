// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodefinder.application.presentation.ui.compose.screens.finderscreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.zebra.ai.barcodefinder.application.presentation.ui.compose.components.ZebraText
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.mainInverse
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.zoomIndicatorColor

@Composable
fun ZoomIndicator(
    onClick: () -> Unit,
    zoomValue: Float,
    backgroundAlpha: Float = 0.88f
) {
    // This outer Box is for screen positioning. Its alignment is controlled by its parent.
    Box(
        modifier = Modifier
            .padding(
                bottom = AppDimensions.LargePadding,
            )
    ) {
        // This inner Box creates the circular UI element.
        Box(
            contentAlignment = Alignment.Center, // This centers the text inside the circle.
            modifier = Modifier
                .size(AppDimensions.dimension_42dp)
                .clip(CircleShape) // The circular clip turns the square into a perfect circle.
                .clickable(onClick = onClick)
                .background(color = zoomIndicatorColor.copy(alpha = backgroundAlpha))
        ) {
            val displayText = if (zoomValue % 1 == 0f) {
                "${zoomValue.toInt()}x"
            } else {
                "%.1fx".format(zoomValue)
            }
            ZebraText(
                textValue = displayText,
                style = TextStyle(
                    fontSize = AppDimensions.dialogTextFontSizeExtraSmall,
                    lineHeight = AppDimensions.dialogTextFontSizeMedium,
                    fontWeight = FontWeight.Normal
                ),
                textColor = mainInverse
            )
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun PreviewZoomIndicator() {
    ZoomIndicator(onClick = {}, zoomValue = 2.5f)
}
