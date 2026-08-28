// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.palletchecker.presentation.ui.theme

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
val borderPrimaryMain = Color(0xFF0073E6)
val disabledMain = Color(0xFF8D95A3)

val gray = Color.Gray
val white = Color.White

val navBarBackgroundColor = Color(0xFF151519)
val navBarDividerColor = Color(0xFF3C414B)
val headerBackgroundColor = Color(0xFF151519)

val navBarZebraTextColor = Color(0xFF646A78)
val mainInverse = Color(0xFFF3F6FA) // Used for settingsDescriptionRowBackground

val aboutGray = navBarZebraTextColor
val aboutBodyGray = mainInverse


// New colors for ZebraBottomSheet.kt
val ScrimColor = Color.Black.copy(alpha = 0.3f)
val BottomSheetDividerColor = Color.LightGray // Renamed to avoid clash
val CardContainerColor = Color(0xFFF0F2F5)
val CardHeaderBackgroundColor = Color(0xFFE8EAED)
val IconTintColor = Color(0xFF5F6368)
val CardHeaderTextPrimaryColor = Color(0xFF202124)
val CardDividerColor = Color(0xFFCED2DB)

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

val Black = Color(0xFF333333)
val BlackLight = Color(0xFF666666)

val ButtonThemeColor = Color(0xFF000000)

val SuccessColor = Color(0xFF257729)


val ThemeDark = Color(0xFF0073E6)
val ThemeLight = Color(0xFFBADBFA)
val WhiteLight = Color(0xFFF1F7FD)
val labelColor = Color(0xFF00BCD4)

val validBoxColor = Color(0xFF28790A)
val qtyMismatchedColor = Color(0xFFEC082A)
val partialReadColor = Color(0xFFFFC107)
val NotDetectedMainLblColor = Color(0xFF9C27B0)
val ClassId2Color = Color(0xFFA40C40)
val ClassId3Color = Color(0xFF2196F3)
val OtherClassId = Color(0xFFFF9800)

val BarcodeDetectedFill = Color(0x9028790A)
val configBackgroundColor = Color(0xFFF8FBFF)
val configCardContentColor = Color(0xFFF2F3F5)

enum class SCAN_RESULT_COLOR(val color: Color) {
    ENABLE(ThemeDark),
    SUCCESS(SuccessColor),
    DISABLE(BlackLight)
}

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
