package com.zebra.ai.ppod.ui.components.mainScreen

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class BorderState {
    IDLE,
    GOOD,
    BAD
}

@Composable
fun BorderGlow(
    borderState: BorderState = BorderState.IDLE
) {
    val color = when (borderState) {
        BorderState.IDLE -> Color.Yellow
        BorderState.GOOD -> Color.Green
        BorderState.BAD -> Color.Red
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .border(width = 3.dp, color = color)) {}
}