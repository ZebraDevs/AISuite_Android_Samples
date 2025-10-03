// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.presentation.ui.compose.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit

import com.zebra.ai.barcodefinder.presentation.ui.theme.BaseTextStyle
import com.zebra.ai.barcodefinder.presentation.ui.theme.textWhite

/**
 * Displays styled text using the Zebra app's default or custom styles.
 * Allows customization of color, font, size, weight, line height, and alignment.
 *
 * @param textValue The string to display
 * @param modifier Modifier for Compose layout
 * @param textColor Optional text color override
 * @param font Optional font family override
 * @param fontSize Optional font size override
 * @param fontWeight Optional font weight override
 * @param lineHeight Optional line height override
 * @param style Optional base text style
 * @param textAlign Optional text alignment
 */
@Composable
fun ZebraText(
    textValue: String,
    modifier: Modifier = Modifier,
    textColor: Color? = null,
    font: FontFamily? = null,
    fontSize: TextUnit? = null,
    fontWeight: FontWeight? = null,
    lineHeight: TextUnit? = null,
    style: TextStyle = BaseTextStyle,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE, // Default to allow multiple lines
    overflow: TextOverflow = TextOverflow.Clip // Default overflow behavior
) {
    val finalStyle = style.copy(
        color = textColor ?: style.color,
        fontFamily = font ?: style.fontFamily,
        fontSize = fontSize ?: style.fontSize,
        fontWeight = fontWeight ?: style.fontWeight,
        lineHeight = lineHeight ?: style.lineHeight
    )

    Text(
        text = textValue,
        modifier = modifier,
        style = finalStyle,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow
    )
}


@Preview(name = "Default Custom Text")
@Composable
fun PreviewDefaultCustomText() {
    ZebraText(textValue = "Hello Zebra!", textColor = textWhite)
}