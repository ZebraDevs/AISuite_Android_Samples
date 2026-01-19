// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodefinder.application.presentation.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Centralizes the color palette for the application UI.
 * Defines all colors used for backgrounds, text, icons, borders, and components.
 */

val darkBackground = Color(0xFF151519)

val textBlack = Color(0xFF1D1E23)
val textGrey = Color(0xFF545963)
val textWhite = Color(0xFFFFFFFF)
val dimGray = Color(0xFF636363)

val mainSubtle = Color(0xFF545963)
val borderPrimaryMain = Color(0xFF0073E6)
val disabledMain = Color(0xFF8D95A3)

val boarderGray = Color(0xFF545963)
val boarderColor = Color(0xFFCED2DB)

val iconBlack = textBlack
val iconBlue = Color(0xFF007AFF)
val iconRed = Color(0xFFFF3B30)
val iconGreen = Color(0xFF34C759)

val gray = Color.Gray
val white = Color.White

val navBarBackgroundColor = Color(0xFF151519)
val navBarDividerColor = Color(0xFF3C414B)
val headerBackgroundColor = Color(0xFF151519)


val boxYellow = Color(0xFFF8D249)
val navBarZebraTextColor = Color(0xFF646A78)
val mainInverse = Color(0xFFF3F6FA) // Used for settingsDescriptionRowBackground
val surfaceDefaultInverse = Color(0xFF151519)

val zoomIndicatorColor = Color(0xE0545963)

val aboutGray = navBarZebraTextColor
val aboutBodyGray = mainInverse


// New colors for ZebraBottomSheet.kt
val ScrimColor = Color.Black.copy(alpha = 0.3f)
val HandleColor = Color.Gray
val BottomSheetDividerColor = Color.LightGray // Renamed to avoid clash
val CardContainerColor = Color(0xFFF0F2F5)
val CardHeaderBackgroundColor = Color(0xFFE8EAED)
val IconTintColor = Color(0xFF5F6368)
val CardHeaderTextPrimaryColor = Color(0xFF202124)
val CardDividerColor = Color(0xFFCED2DB)
val HorizontalDividerColor = Color(0xFFCED2DB)

val darkGreyHeader = Color(0xFF2E2E2E)
val lightGreyHeader = Color(0xFF666666)

val dividerColor = Color(0xFFE0E0E0)

// New colors for SwitchOption.kt
val switchOptionTextColor = textBlack // Or Color.Black if specific
val switchCheckedThumbColor = white // Or Color.White
val switchCheckedTrackColor = Color(0xFF2196F3) // Consider a name like colorPrimary or switchBlue
val switchUncheckedThumbColor = white // Or Color.White
val switchUncheckedTrackColor = gray // Or mainSubtle

// New colors for SettingsCard.kt
val settingsCardHeaderBackgroundColor = Color(0xFFE0E3E9)
val settingsCardTitleColor = Color(0xFF1D1E23)
val settingsCardIconTintColor = Color(0xFF5F6368)

// New colors for RadioButtonOption.kt
val SelectedRadioButton = Color(0xFF1976D2)

// New colors for SettingsScreen.kt
val settingsLazyColumnBackground = Color(0xFFF5F5F5)

// New colors for MenuOverlay.kt
val menuOverlayPreviewScrim = Color.Black.copy(alpha = 0.5f)

object AppColors {
    val Primary = Color(0xFF0073E6)
    val GrayBackground = Color(0xFF8D95A3)
    val DarkBackground = Color(0xFF151519)
    val Divider = Color(0xFF3C414B)
    val TextRed = Color.Red
    val DialogTextGray = Color.Gray
    val ButtonRed = Color.Red
    val ButtonTextWhite = Color.White

    val TextBlack = Color(0xFF1D1E23)
    val TextAction = Color(0xFF000000)
    val TextGray = Color(0xFF545963)
    val TextWhite = Color(0xFFFFFFFF)
    val TextTitle = Color(0xFFF3F6FA)
    val TextHamburgerMenu = Color(0xFFE0E3E9)
    val TextHamburgerDescription = Color(0xFF646A78)
}
