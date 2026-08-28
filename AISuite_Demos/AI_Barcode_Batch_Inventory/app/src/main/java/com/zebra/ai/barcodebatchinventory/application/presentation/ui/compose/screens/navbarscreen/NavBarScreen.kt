// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.screens.navbarscreen

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.zebra.ai.barcodebatchinventory.R
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.components.ZebraText
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.screens.navbarscreen.components.NavigationMenuItem
import com.zebra.ai.barcodebatchinventory.application.presentation.model.NavigationItem
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.theme.navBarBackgroundColor
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.theme.navBarDividerColor
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.theme.navBarZebraTextColor

/**
 * Displays the navigation bar screen with navigation options and feedback.
 * Handles navigation to home, settings, about, and feedback actions.
 *
 * @param onNavigateToHome Callback for navigating to home
 * @param onNavigateToSettings Callback for navigating to settings
 * @param onNavigateToAbout Callback for navigating to about
 * @param onSendFeedback Callback for sending feedback
 * @param onBackPressed Callback for handling back press
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavBarScreen(
    onNavigateToHome: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onSendFeedback: () -> Unit = {},
    onBackPressed: () -> Unit = {}
) {
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxHeight()
            .width(AppDimensions.navBarrWidth)
            .background(navBarBackgroundColor),
        verticalArrangement = Arrangement.SpaceBetween, // Changed to push content to top and bottom
        horizontalAlignment = Alignment.Start,
    ) {
        // Top section with header and navigation
        Column {
            // Divider above navigation menu
            HorizontalDivider(
                color = navBarDividerColor,
                thickness = AppDimensions.navBarDividerThickness,
                modifier = Modifier.fillMaxWidth()
            )

            // Navigation Items with reduced spacing
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppDimensions.spacerHeight8) // Reduced from 16.dp
                    .semantics{contentDescription="Navbar"},
                verticalArrangement = Arrangement.spacedBy(AppDimensions.zeroPadding) // Reduced from 8.dp
            ) {
                val navigationItems = listOf(
                    NavigationItem(
                        stringResource(id = R.string.navbar_screen_home),
                        Icons.Default.Home,
                        onNavigateToHome,
                        modifier = Modifier.semantics{contentDescription="NavbarHome"}
                    ),
                    NavigationItem(
                        stringResource(id = R.string.navbar_screen_settings),
                        Icons.Default.Settings,
                        onNavigateToSettings,
                        modifier = Modifier.semantics{contentDescription="NavbarSettings"}
                    ),
                    NavigationItem(
                        stringResource(id = R.string.navbar_screen_about),
                        Icons.Default.Info,
                        onNavigateToAbout,
                        modifier = Modifier.semantics{contentDescription="NavbarAbout"}
                    ),
                    NavigationItem(
                        stringResource(id = R.string.navbar_screen_feedback),
                        ImageVector.vectorResource(id = R.drawable.feedback),
                        {
                            val url = context.getString(R.string.navbar_screen_feedback_url)
                            if (url.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.testTag("NavbarSendFeedback")
                    )
                )

                navigationItems.forEach { item ->
                    NavigationMenuItem(
                        title = item.title,
                        icon = item.icon,
                        onClick = item.onClick,
                        modifier = item.modifier,
                        enabled = true
                    )
                }
            }
        }

        // Bottom section with Zebra branding
        Column {
            // Divider above Zebra branding
            HorizontalDivider(
                color = navBarDividerColor,
                thickness = AppDimensions.navBarDividerThickness,
                modifier = Modifier.fillMaxWidth()
            )

            // Zebra Branding at Bottom
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = AppDimensions.modifier16,
                        AppDimensions.modifier24
                    ), // Increased vertical padding
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.zebra_logo),
                    contentDescription = null,
                    contentScale = ContentScale.None,
                    modifier = Modifier
                        .padding(AppDimensions.navBarZebraLogoPadding)
                        .width(AppDimensions.navBarZebraLogoWidth)
                        .height(AppDimensions.navBarZebraLogoHeight)
                )

                ZebraText(
                    textValue = stringResource(id = R.string.navbar_screen_watermark),
                    textColor = navBarZebraTextColor,
                    fontSize = AppDimensions.dialogTextFontSizeExtraSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = AppDimensions.SmallPadding)
                )
            }
        }
    }

    BackHandler {
        onBackPressed()
    }
}


@Preview(showBackground = true, name = "Navigation Drawer")
@Composable
fun NavBarScreenPreview() {
    MaterialTheme {
        NavBarScreen()
    }
}

@Preview(showBackground = true, name = "Navigation Drawer - Dark")
@Composable
fun NavBarScreenDarkPreview() {
    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        NavBarScreen()
    }
}
