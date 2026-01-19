package com.zebra.ai.barcodefinder.application.presentation.model

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Data class representing a single menu option in the app's UI.
 * Each option consists of an icon, a title, and an action to perform when selected.
 * Used for building dynamic menus, overlays, or dialogs where users can select actions.
 *
 * @property icon The icon to display for this menu option (Compose ImageVector).
 * @property title The text label for this menu option.
 * @property onClick The callback to invoke when the option is selected.
 */
data class MenuOption(
    val icon: ImageVector,
    val title: String,
    val onClick: () -> Unit
)

