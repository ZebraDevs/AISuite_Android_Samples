// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodefinder.presentation.ui.compose.screens.homescreen.components

import android.content.Context
import android.widget.Toast
import com.zebra.ai.barcodefinder.presentation.ui.compose.screens.homescreen.defaults.HomeScreenUiDefaults
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.zebra.ai.barcodefinder.R
import com.zebra.ai.barcodefinder.presentation.enums.ButtonType
import com.zebra.ai.barcodefinder.domain.model.AppSettings
import com.zebra.ai.barcodefinder.presentation.model.SettingsUiState
import com.zebra.ai.barcodefinder.presentation.ui.compose.components.ZebraButton
import com.zebra.ai.barcodefinder.presentation.ui.compose.components.ZebraText
import com.zebra.ai.barcodefinder.presentation.ui.compose.components.backgroundColor
import com.zebra.ai.barcodefinder.presentation.ui.compose.screens.navbarscreen.NavBarScreen
import com.zebra.ai.barcodefinder.presentation.ui.theme.AppColors
import com.zebra.ai.barcodefinder.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.presentation.ui.theme.AppTextStyles
import com.zebra.ai.barcodefinder.presentation.ui.theme.borderPrimaryMain
import com.zebra.ai.barcodefinder.presentation.ui.theme.disabledMain
import com.zebra.ai.barcodefinder.presentation.ui.theme.headerBackgroundColor
import com.zebra.ai.barcodefinder.presentation.viewmodel.FinderViewModel
import com.zebra.ai.barcodefinder.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    settingsViewModel: SettingsViewModel,
    finderViewModel: FinderViewModel,
    settings: AppSettings,
    uiState: SettingsUiState,
    isSDKInitialized: Boolean,
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {},
    isNavBarVisible: Boolean = false,
    onNavigateToHome: () -> Unit = {},
    onNavigateToConfigureDemo: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onSendFeedback: () -> Unit = {},
    onStartScan: () -> Unit = {}, // <-- Added missing parameter
    context: Context
) {
    val topBarHeight = HomeScreenUiDefaults.TopBarHeight
    val navBarWidthFraction = HomeScreenUiDefaults.NavBarWidthFraction
    val navBarAnimationDuration = HomeScreenUiDefaults.AnimationDuration
    val scrimAlpha = HomeScreenUiDefaults.ScrimAlpha

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    ZebraText(
                        textValue = stringResource(id = R.string.home_screen_content_app_name),
                        style = AppTextStyles.TitleTextLight,
                        textColor = AppColors.TextWhite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = AppColors.TextWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = headerBackgroundColor
                ),
            )
            // HomeScreen Content always visible
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.TextWhite)
                    .padding(AppDimensions.zeroPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val configuration = LocalConfiguration.current
                if (configuration.screenHeightDp > 550) {
                    Image(
                        painter = painterResource(id = R.drawable.icon_main),
                        contentDescription = null,
                        modifier = Modifier
                            .size(180.dp)
                            .padding(top = AppDimensions.dimension_24dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(AppDimensions.dimension_40dp))
                }
                Spacer(modifier = Modifier.height(AppDimensions.dimension_12dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppDimensions.dimension_16dp),
                    verticalArrangement = Arrangement.spacedBy(AppDimensions.dimension_8dp)
                ) {
                    ZebraText(
                        textValue = stringResource(id = R.string.home_screen_content_settings_header),
                        fontSize = AppDimensions.dialogTextFontSizeMedium,
                        fontWeight = FontWeight.Bold,
                        textColor = AppColors.TextBlack,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics{contentDescription="HomeScreenText"}
                            .padding(start = AppDimensions.dimension_16dp)
                    )
//                    Spacer(modifier = Modifier.height(AppDimensions.dimension_2dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = AppDimensions.dimension_16dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        ZebraText(
                            textValue = stringResource(id = R.string.home_screen_content_bullet_point),
                            fontSize = AppDimensions.dialogTextFontSizeMedium,
                            textColor = AppColors.TextBlack,
                            fontWeight = FontWeight.Bold
                        )
                        Column {
                            Text( // Keep Material Text for buildAnnotatedString
                                text = buildAnnotatedString {
                                        append(stringResource(id = R.string.home_screen_content_model_input))
                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Light)) {
                                        append(stringResource(id = settings.modelInput.homeDisplayNameResId))
                                    }
                                },
                                fontSize = AppDimensions.dialogTextFontSizeMedium,
                                color = AppColors.TextBlack,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
//                    Spacer(modifier = Modifier.height(AppDimensions.dimension_2dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = AppDimensions.dimension_16dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        ZebraText(
                            textValue = stringResource(id = R.string.home_screen_content_bullet_point),
                            fontSize = AppDimensions.dialogTextFontSizeMedium,
                            textColor = AppColors.TextBlack,
                            fontWeight = FontWeight.Bold
                        )
                        Column {
                            Text( // Keep Material Text for buildAnnotatedString
                                text = buildAnnotatedString {
                                    append(stringResource(id = R.string.home_screen_content_resolution))
                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Light)) {
                                        append(stringResource(id = settings.resolution.displayNameResId))
                                    }
                                },
                                fontSize = AppDimensions.dialogTextFontSizeMedium,
                                color = AppColors.TextBlack,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
//                    Spacer(modifier = Modifier.height(AppDimensions.dimension_2dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = AppDimensions.dimension_16dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        ZebraText(
                            textValue = stringResource(id = R.string.home_screen_content_bullet_point),
                            fontSize = AppDimensions.dialogTextFontSizeMedium,
                            textColor = AppColors.TextBlack,
                            fontWeight = FontWeight.Bold
                        )
                        Column {
                            Text( // Keep Material Text for buildAnnotatedString
                                text = buildAnnotatedString {
                                    append(stringResource(id = R.string.home_screen_content_processor_type))
                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Light)) {
                                        append(stringResource(id = settings.processorType.displayNameResId))
                                    }
                                },
                                fontSize = AppDimensions.dialogTextFontSizeMedium,
                                color = AppColors.TextBlack,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(AppDimensions.dimension_24dp))
                ZebraButton(
                    buttonType = ButtonType.Text,
                    text = stringResource(id = R.string.home_screen_content_restore_settings),
                    onClick = {
                        settingsViewModel.resetToDefaults()
                        finderViewModel.applySettingsToSdk()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading,
                    textColor = borderPrimaryMain
                    // fontSize and fontWeight from original Text are not directly supported by ZebraButton's internal Text
                )
                Spacer(modifier = Modifier.weight(1f))
                ZebraButton(
                    buttonType = ButtonType.Raised,
                    text = if (uiState.isScanning) "" else stringResource(id = R.string.home_screen_content_start_scan_button),
                    textSize = AppDimensions.dialogTextFontSizeMedium,
                    onClick = {
                        settingsViewModel.onStartScan()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = AppDimensions.dimension_12dp,
                            bottom = AppDimensions.dimension_24dp,
                            start = AppDimensions.dimension_16dp,
                            end = AppDimensions.dimension_16dp
                        )
                        .semantics{
                            this.backgroundColor = if (isSDKInitialized) "blue" else "gray"
                        },
                    enabled = isSDKInitialized && !uiState.isLoading && !uiState.isScanning,
                    shapes = RoundedCornerShape(AppDimensions.dimension_4dp),
                    backgroundColor = if (isSDKInitialized) borderPrimaryMain else disabledMain,
                    textColor = AppColors.TextWhite,
                    leadingIcon = if (uiState.isScanning) {
                        {
                            CircularProgressIndicator(
                                modifier = Modifier.size(AppDimensions.dimension_20dp),
                                color = AppColors.TextWhite // Changed from Color.White
                            )
                        }
                    } else null,
                    textModifier = Modifier.padding(vertical = AppDimensions.dimension_14dp)
                    // Original fontSize and fontWeight from Text for "Start Scan" are applied by ZebraButton's default Text styling
                )
                Spacer(modifier = Modifier.height(AppDimensions.dimension_20dp))
            }
        }

        // Animated scrim overlay (only over content, not TopBar)
        AnimatedVisibility(
            visible = isNavBarVisible,
            enter = fadeIn(animationSpec = tween(durationMillis = navBarAnimationDuration)),
            exit = fadeOut(animationSpec = tween(durationMillis = navBarAnimationDuration)),
            modifier = Modifier
                .padding(top = topBarHeight)
                .align(Alignment.TopStart)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.TextBlack.copy(alpha = scrimAlpha))
                    .clickable { onMenuClick() }
            )
        }

        // Animated NavBarScreen overlay (below TopBar, above content)
        AnimatedVisibility(
            visible = isNavBarVisible,
            enter = slideInHorizontally(
                initialOffsetX = { full -> -full },
                animationSpec = tween(durationMillis = navBarAnimationDuration)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { full -> -full },
                animationSpec = tween(durationMillis = navBarAnimationDuration)
            ),
            modifier = Modifier
                .padding(top = topBarHeight)
                .align(Alignment.TopStart)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(navBarWidthFraction)
            ) {
                NavBarScreen(
                    onNavigateToHome = onNavigateToHome,
                    onNavigateToConfigureDemo = onNavigateToConfigureDemo,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToAbout = onNavigateToAbout,
                    onSendFeedback = onSendFeedback,
                    onBackPressed = onNavigateToHome,
                    isSDKInitialized = isSDKInitialized,
                    isLoading = uiState.isLoading,
                    isScanning = uiState.isScanning
                )
            }
        }

    }
}