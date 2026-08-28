package com.zebra.ai.palletchecker.presentation.ui.compose.screens.homescreen.defaults

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import com.zebra.ai.palletchecker.presentation.ui.theme.AppDimensions

object HomeScreenUiDefaults {
    @OptIn(ExperimentalMaterial3Api::class)
    val TopBarHeight = TopAppBarDefaults.TopAppBarExpandedHeight
    const val NavBarWidthFraction = 0.95f
    const val AnimationDuration = 300
    val ScrimAlpha = AppDimensions.WeightHalf
}