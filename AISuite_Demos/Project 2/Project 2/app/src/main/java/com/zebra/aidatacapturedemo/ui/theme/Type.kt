package com.zebra.aidatacapturedemo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.zebra.aidatacapturedemo.R

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontSize = 14.4.sp,
        lineHeight = 21.6.sp,
        fontFamily = FontFamily(Font(R.font.ibm_plex_sans_regular)),
        fontWeight = FontWeight(500),
        color = Color(0xFF1D1E23),
    )
)