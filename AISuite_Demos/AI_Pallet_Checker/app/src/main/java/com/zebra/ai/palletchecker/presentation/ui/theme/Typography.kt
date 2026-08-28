// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

/**
 * Defines text styles and typography for the application UI.
 * Centralizes font families, base styles, and custom text styles for consistent appearance.
 */

package com.zebra.ai.palletchecker.presentation.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.zebra.ai.palletchecker.R

val IBMPlexSans = FontFamily(
    Font(R.font.ibm_plex_sans)
)

val BaseTextStyle = TextStyle(
    fontFamily = IBMPlexSans,
    color = AppColors.TextBlack,
    fontSize = 16.sp,
    fontWeight = FontWeight(400),
    lineHeight = 24.sp
)

object AppTextStyles {
    // App Name On The Top Appbar
    val TitleTextLight = BaseTextStyle.copy(
        color = AppColors.TextTitle,
        fontSize = 20.sp,
        fontWeight = FontWeight(700),
        lineHeight = 28.sp
    )

    val TitleTextDark = BaseTextStyle.copy(
        color = AppColors.TextBlack,
        fontSize = 20.sp,
        fontWeight = FontWeight(500),
        lineHeight = 28.sp
    )

    val TextFieldReqText = BaseTextStyle.copy(
        color = AppColors.TextBlack,
        fontSize = 16.sp,
        fontWeight = FontWeight(500),
        lineHeight = 20.sp
    )

    val ActionableBarcodeDialogTitleText = BaseTextStyle.copy(
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 20.sp
    )

    val EulaText = BaseTextStyle.copy(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight(400),
        color = dimGray,
        letterSpacing = 0.4.sp
    )

    val AboutBoldTextStyle = BaseTextStyle.copy(
        fontWeight = FontWeight(500),
    )
    val AboutSmallTextStyle = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFamily = IBMPlexSans,
        fontWeight = FontWeight(400),
        color = aboutGray,

        textAlign = TextAlign.Right,
    )
}
