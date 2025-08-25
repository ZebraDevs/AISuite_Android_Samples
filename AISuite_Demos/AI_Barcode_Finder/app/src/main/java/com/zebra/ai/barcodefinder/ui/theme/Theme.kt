// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

/**
 * Sets up the application's MaterialTheme, color scheme, and typography.
 * Provides a custom AppTheme composable for consistent theming across the UI.
 */

package com.zebra.ai.barcodefinder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    background = darkBackground,


    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
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
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography,
        content = content
    )
}
