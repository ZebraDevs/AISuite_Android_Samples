// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.application.presentation.ui.compose.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.zebra.ai.barcodefinder.application.presentation.enums.ButtonType
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.BaseTextStyle
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.borderPrimaryMain
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.disabledMain
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.textWhite

val BackgroundColorKey = SemanticsPropertyKey<String>("backgroundColor")
var SemanticsPropertyReceiver.backgroundColor by BackgroundColorKey

/**
 * Displays a customizable button with optional icons, color, and shape.
 * Supports raised, text, and outlined button types.
 *
 * @param text The button label
 * @param onClick Callback for button click
 * @param modifier Modifier for Compose layout
 * @param buttonType Type of button (Raised, Text, Outlined)
 * @param enabled Whether the button is enabled
 * @param textColor Optional text color override
 * @param backgroundColor Optional background color override
 * @param leadingIcon Optional composable for leading icon
 * @param trailingIcon Optional composable for trailing icon
 * @param shapes Shape of the button corners
 * @param textSize Size of the button text
 */
@Composable
fun ZebraButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonType: ButtonType = ButtonType.Raised,
    enabled: Boolean = true,
    textColor: Color? = null,
    backgroundColor: Color? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shapes: RoundedCornerShape = RoundedCornerShape(AppDimensions.dimension_4dp),
    textSize: TextUnit = BaseTextStyle.fontSize, // Added textSize parameter with default value
    textModifier: Modifier = Modifier
) {
    val defaultTextColor = borderPrimaryMain
    val defaultBackgroundColor = when (buttonType) {
        ButtonType.Raised -> borderPrimaryMain
        else -> textWhite
    }

    // Define button colors based on type
    val buttonColors = when (buttonType) {
        ButtonType.Raised -> ButtonDefaults.buttonColors(
            disabledContainerColor = disabledMain,
            disabledContentColor = textWhite,
            containerColor = backgroundColor ?: defaultBackgroundColor,
            contentColor = textColor ?: textWhite
        )

        ButtonType.Text -> ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,  // Ensure background is transparent
            contentColor = textColor
                ?: defaultTextColor // This should be borderPrimaryMain by default
        )

        ButtonType.Outlined -> ButtonDefaults.buttonColors(
            containerColor = backgroundColor ?: defaultBackgroundColor,
            contentColor = textColor ?: defaultTextColor
        )
    }

    val buttonContent: @Composable RowScope.() -> Unit = {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.size(AppDimensions.dimension_8dp))
        }
        Text(
            text = text,
            modifier = textModifier,
            lineHeight = AppDimensions.linePaddingDefault,
            style = TextStyle(
                fontFamily = BaseTextStyle.fontFamily,
                fontSize = textSize,
                fontWeight = FontWeight.Bold,
            )
        )
        if (trailingIcon != null) {
            Spacer(modifier = Modifier.size(AppDimensions.dimension_8dp))
            trailingIcon()
        }
    }

    when (buttonType) {
        ButtonType.Raised -> {
            Button(
                onClick = onClick,
                modifier = modifier,
                colors = buttonColors,
                enabled = enabled,
                content = buttonContent,
                contentPadding = PaddingValues(AppDimensions.zeroPadding),
                shape = shapes
            )
        }

        ButtonType.Text -> {
            TextButton(
                onClick = onClick,
                modifier = modifier.padding(AppDimensions.zeroPadding), // Ensure no extra padding
                colors = buttonColors,
                enabled = enabled,
                contentPadding = PaddingValues(AppDimensions.zeroPadding), // Override default padding
                content = buttonContent
            )
        }

        ButtonType.Outlined -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                content = buttonContent,
                colors = buttonColors,
                shape = shapes,
                border = BorderStroke(
                    AppDimensions.navBarDividerThickness,
                    textColor ?: defaultTextColor
                ) // Border color matches text color
            )
        }
    }
}
