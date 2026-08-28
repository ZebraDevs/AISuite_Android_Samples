// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

/**
 * Sets up the application's MaterialTheme, color scheme, and typography.
 * Provides a custom AppTheme composable for consistent theming across the UI.
 */

package com.zebra.ai.palletchecker.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.zebra.ai.palletchecker.presentation.ui.compose.components.VectorProvider

private val LightColorScheme = lightColorScheme(
    background = darkBackground,
)

// 1. Create a Typography instance using IBMPlexSans
private val AppTypography = Typography(
    displayLarge = androidx.compose.ui.text.TextStyle(fontFamily = AppFonts.IBMPlexSans),
    displayMedium = androidx.compose.ui.text.TextStyle(fontFamily = AppFonts.IBMPlexSans),
    displaySmall = androidx.compose.ui.text.TextStyle(fontFamily = AppFonts.IBMPlexSans),
    headlineLarge = androidx.compose.ui.text.TextStyle(fontFamily = AppFonts.IBMPlexSans),
    headlineMedium = androidx.compose.ui.text.TextStyle(fontFamily = AppFonts.IBMPlexSans),
    headlineSmall = androidx.compose.ui.text.TextStyle(fontFamily = AppFonts.IBMPlexSans),
    titleLarge = androidx.compose.ui.text.TextStyle(fontFamily = AppFonts.IBMPlexSans),
    titleMedium = androidx.compose.ui.text.TextStyle(fontFamily = AppFonts.IBMPlexSans),
    titleSmall = androidx.compose.ui.text.TextStyle(fontFamily = AppFonts.IBMPlexSans),
    bodyLarge = androidx.compose.ui.text.TextStyle(fontFamily = AppFonts.IBMPlexSans),
    bodyMedium = androidx.compose.ui.text.TextStyle(fontFamily = AppFonts.IBMPlexSans),
    bodySmall = androidx.compose.ui.text.TextStyle(fontFamily = AppFonts.IBMPlexSans),
    labelLarge = androidx.compose.ui.text.TextStyle(fontFamily = AppFonts.IBMPlexSans),
    labelMedium = androidx.compose.ui.text.TextStyle(fontFamily = AppFonts.IBMPlexSans),
    labelSmall = androidx.compose.ui.text.TextStyle(fontFamily = AppFonts.IBMPlexSans)
)

// 2. Create a custom MaterialTheme composable
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    //Load vector resources once at the application root to prevent redundant allocations and drawing cycles
    VectorProvider {
        MaterialTheme(
            colorScheme = LightColorScheme,
            typography = AppTypography,
            content = content
        )
    }

}
