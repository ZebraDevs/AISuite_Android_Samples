// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.palletchecker.presentation.ui.compose.screens.navbarscreen

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Edit
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
import com.zebra.ai.palletchecker.R
import com.zebra.ai.palletchecker.presentation.ui.compose.components.ZebraText
import com.zebra.ai.palletchecker.presentation.ui.compose.screens.navbarscreen.components.NavigationMenuItem
import com.zebra.ai.palletchecker.presentation.model.NavigationItem
import com.zebra.ai.palletchecker.presentation.ui.theme.AppDimensions
import com.zebra.ai.palletchecker.presentation.ui.theme.navBarBackgroundColor
import com.zebra.ai.palletchecker.presentation.ui.theme.navBarDividerColor
import com.zebra.ai.palletchecker.presentation.ui.theme.navBarZebraTextColor

/**
 * Displays the navigation bar screen with navigation options and feedback.
 * Handles navigation to home, configure, settings, about, and feedback actions.
 *
 * @param onNavigateToHome Callback for navigating to home
 * @param onNavigateToConfigureDemo Callback for navigating to configure demo
 * @param onNavigateToSettings Callback for navigating to settings
 * @param onNavigateToAbout Callback for navigating to about
 * @param onSendFeedback Callback for sending feedback
 * @param onBackPressed Callback for handling back press
 * @param isSDKInitialized Whether the SDK is initialized
 * @param isLoading Whether loading is in progress
 * @param isScanning Whether scanning is in progress
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavBarScreen(
    onNavigateToHome: () -> Unit = {},
    onNavigateToConfigureDemo: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onSendFeedback: () -> Unit = {},
    onBackPressed: () -> Unit = {},
    isSDKInitialized: Boolean = true,
    isLoading: Boolean = false,
    isScanning: Boolean = false
) {
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxHeight()
            .width(AppDimensions.navBarrWidth)
            .background(navBarBackgroundColor),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.Start,
    ) {

        Column {

            HorizontalDivider(
                color = navBarDividerColor,
                thickness = AppDimensions.navBarDividerThickness,
                modifier = Modifier.fillMaxWidth()
            )

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppDimensions.spacerHeight8)
                    .semantics{contentDescription="Navbar"},
                verticalArrangement = Arrangement.spacedBy(AppDimensions.zeroPadding)
            ) {
                val navigationItems = listOf(
                    NavigationItem(
                        stringResource(id = R.string.navbar_screen_home),
                        Icons.Default.Home,
                        onNavigateToHome,
                        modifier = Modifier.semantics{contentDescription="NavbarHome"}
                    ),
                    NavigationItem(
                        stringResource(id = R.string.navbar_screen_configure_demo),
                        Icons.Default.Edit,
                        onNavigateToConfigureDemo,
                        modifier = Modifier.semantics{contentDescription="NavbarConfigureDemo"}
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
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://app.smartsheet.com/b/form/01987b43551e7cc1965c1090c4398664")
                            )
                            context.startActivity(intent)
                        },
                        modifier = Modifier.testTag("NavbarSendFeedback")
                    )
                )

                navigationItems.forEachIndexed { index, item ->
                    val isConfigureDemo =
                        item.title == stringResource(id = R.string.navbar_screen_configure_demo)
                    val enabled = if (isConfigureDemo) {
                        isSDKInitialized && !isLoading && !isScanning
                    } else {
                        true
                    }

                    NavigationMenuItem(
                        title = item.title,
                        icon = item.icon,
                        onClick = item.onClick,
                        modifier = item.modifier,
                        enabled = enabled
                    )
                }
            }
        }

        Column {

            HorizontalDivider(
                color = navBarDividerColor,
                thickness = AppDimensions.navBarDividerThickness,
                modifier = Modifier.fillMaxWidth()
            )

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = AppDimensions.modifier16,
                        AppDimensions.modifier24
                    ),
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
