// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.application.presentation.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zebra.ai.barcodefinder.application.presentation.model.MenuOption
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.menuOverlayPreviewScrim
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.white
import androidx.compose.ui.res.stringResource
import com.zebra.ai.barcodefinder.R

/**
 * Displays a modal overlay with menu options for editing or deleting an item.
 * Uses a dialog with a card layout and delegates option rendering to MenuOptionItem.
 *
 * @param onDismiss Callback when the overlay is dismissed
 * @param onEdit Callback for the edit action
 * @param onDelete Callback for the delete action
 */
@Composable
fun MenuOverlay(
    onDismiss: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val menuOptions = listOf(
        MenuOption(
            icon = Icons.Default.Edit,
            title = stringResource(R.string.menu_overlay_edit),
            onClick = {
                onEdit()
                onDismiss()
            }
        ),
        MenuOption(
            icon = Icons.Default.Delete,
            title = stringResource(R.string.menu_overlay_delete),
            onClick = {
                onDelete()
                onDismiss()
            }
        )
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .wrapContentSize()
                .padding(AppDimensions.CardPadding),
            shape = RoundedCornerShape(AppDimensions.menuOverlayCardCornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = white
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = AppDimensions.largeElevation)
        ) {
            Column(
                modifier = Modifier.padding(AppDimensions.zeroPadding)
            ) {
                menuOptions.forEachIndexed { index, option ->
                    MenuOptionItem(
                        option = option,
                        showDivider = index <= menuOptions.size - 1
                    )
                }
            }
        }
    }
}

/**
 * Preview for MenuOverlay composable, showing the overlay centered on a scrim background.
 */
@Preview(showBackground = true)
@Composable
fun MenuOverlayScreenPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(menuOverlayPreviewScrim),
            contentAlignment = Alignment.Center
        ) {
            MenuOverlay()
        }
    }
}

/**
 * Preview for MenuOptionItem composable, showing example menu options.
 */
@Preview(showBackground = true)
@Composable
fun MenuOptionItemPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(AppDimensions.MediumPadding)
        ) {
            MenuOptionItem(
                option = MenuOption(
                    icon = Icons.Default.Edit,
                    title = stringResource(R.string.menu_overlay_edit),
                    onClick = {}
                ),
                showDivider = true
            )
            MenuOptionItem(
                option = MenuOption(
                    icon = Icons.Default.Delete,
                    title = stringResource(R.string.menu_overlay_delete),
                    onClick = {}
                ),
                showDivider = false
            )
        }
    }
}
