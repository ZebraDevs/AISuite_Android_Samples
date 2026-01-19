package com.zebra.ai.barcodefinder.application.presentation.model

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

data class NavigationItem(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit = {},
    val modifier: Modifier
)