// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.application.presentation.ui.compose.screens.homescreen


import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zebra.ai.barcodefinder.R
import com.zebra.ai.barcodefinder.application.presentation.enums.ButtonType
import com.zebra.ai.barcodefinder.application.presentation.ui.compose.components.ZebraButton
import com.zebra.ai.barcodefinder.application.presentation.ui.compose.components.ZebraText
import com.zebra.ai.barcodefinder.application.presentation.ui.compose.components.backgroundColor
import com.zebra.ai.barcodefinder.application.presentation.ui.compose.screens.homescreen.defaults.HomeScreenUiDefaults
import com.zebra.ai.barcodefinder.application.presentation.ui.compose.screens.navbarscreen.NavBarScreen
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.AppColors
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.AppTextStyles
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.borderPrimaryMain
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.disabledMain
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.headerBackgroundColor
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.iconGreen
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.iconRed
import com.zebra.ai.barcodefinder.application.presentation.viewmodel.HomeViewModel

/**
 * Displays the Home screen with navigation to other screens and scan start logic.
 * Handles scan state, navigation, and permission management.
 *
 * @param settingsViewModel The ViewModel for app settings
 * @param finderViewModel The ViewModel for entity tracking
 * @param onStartScan Callback for starting a scan
 * @param onBackPressed Callback for handling back press
 */
@SuppressLint("UnrememberedGetBackStackEntry")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    navBarOnNavigateToHome: () -> Unit = {},
    navBarOnNavigateToConfigureDemo: () -> Unit = {},
    navBarOnNavigateToSettings: () -> Unit = {},
    navBarOnNavigateToAbout: () -> Unit = {},
    navBarOnSendFeedback: () -> Unit = {},
    onStartScan: () -> Unit = {},
    onBackPressed: () -> Unit = {},
) {

    var isNavBarVisible by remember { mutableStateOf(false) }

    // Observe settings
    val settings by homeViewModel.settings.collectAsState()

    // Observe EntityTracker Init state
    val entityTrackerInitState by homeViewModel.entityTrackerInitState.collectAsState()

    val appContext = LocalContext.current
    val inferenceErrorMessage = stringResource(id = R.string.home_screen_inference_error_toast)

    val topBarHeight = HomeScreenUiDefaults.TopBarHeight
    val navBarWidthFraction = HomeScreenUiDefaults.NavBarWidthFraction
    val navBarAnimationDuration = HomeScreenUiDefaults.AnimationDuration
    val scrimAlpha = HomeScreenUiDefaults.ScrimAlpha

    val context = LocalContext.current
    var cameraPermissionDenied by remember { mutableStateOf(false) }

    // Pulse animation for the Start Scan button when ready
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulseColor by infiniteTransition.animateColor(
        initialValue = borderPrimaryMain,
        targetValue = borderPrimaryMain.copy(alpha = 0.7f),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseColor"
    )

    // The launcher stays in the Composable, as it's part of the UI layer.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Notify the ViewModel, when the user responds to the permission dialog
        homeViewModel.onPermissionResult(isGranted)
    }

    // Request camera permission on first composition
    LaunchedEffect(Unit) {
        // Re-checking the permission status from viewModel.
        // This is useful if the user changes the permission from the app settings.
        homeViewModel.checkCameraPermission()

        // If permission is denied, launch the request.
        if (homeViewModel.cameraPermissionDenied) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold { paddingValues ->
        if (cameraPermissionDenied) {
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = stringResource(id = R.string.home_screen_camera_permission),
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            Box(modifier = Modifier
                .padding(paddingValues)
                .semantics { contentDescription = "HomeScreen" }
                .fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ZebraText(
                                    textValue = stringResource(id = R.string.home_screen_content_app_name),
                                    style = AppTextStyles.TitleTextLight,
                                    textColor = AppColors.TextWhite
                                )
                                // SDK Status Indicator
                                Surface(
                                    shape = RoundedCornerShape(AppDimensions.dimension_12dp),
                                    color = if (entityTrackerInitState.isInitialized) iconGreen.copy(alpha = 0.2f) else iconRed.copy(alpha = 0.2f),
                                    modifier = Modifier.padding(end = AppDimensions.dimension_16dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = AppDimensions.dimension_8dp, vertical = AppDimensions.dimension_2dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(AppDimensions.dimension_8dp)
                                                .background(
                                                    color = if (entityTrackerInitState.isInitialized) iconGreen else iconRed,
                                                    shape = CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(AppDimensions.dimension_4dp))
                                        Text(
                                            text = if (entityTrackerInitState.isInitialized) "READY" else "INIT",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AppColors.TextWhite,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    isNavBarVisible = !isNavBarVisible
                                }
                            ) {
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
                        // Settings Summary Card
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppDimensions.dimension_16dp),
                            shape = RoundedCornerShape(AppDimensions.dimension_8dp),
                            color = AppColors.TextWhite,
                            shadowElevation = AppDimensions.dimension_2dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Divider.copy(alpha = 0.1f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(AppDimensions.dimension_16dp),
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
                                        .semantics { contentDescription = "HomeScreenText" }
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
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
                        }
                        Spacer(modifier = Modifier.height(AppDimensions.dimension_24dp))
                        ZebraButton(
                            buttonType = ButtonType.Text,
                            text = stringResource(id = R.string.home_screen_content_restore_settings),
                            onClick = {
                                homeViewModel.resetToDefaultSettings()
                                homeViewModel.applySettingsToSDK()
                                android.widget.Toast.makeText(appContext, "Settings restored to default", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = entityTrackerInitState.isInitialized,
                            textColor = borderPrimaryMain
                            // fontSize and fontWeight from original Text are not directly supported by ZebraButton's internal Text
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        ZebraButton(
                            buttonType = ButtonType.Raised,
                            text = stringResource(id = R.string.home_screen_content_start_scan_button),
                            textSize = AppDimensions.dialogTextFontSizeMedium,
                            onClick = {
                                onStartScan()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = AppDimensions.dimension_12dp,
                                    bottom = AppDimensions.dimension_24dp,
                                    start = AppDimensions.dimension_16dp,
                                    end = AppDimensions.dimension_16dp
                                )
                                .semantics {
                                    this.backgroundColor =
                                        if (entityTrackerInitState.isInitialized) "blue" else "gray"
                                },
                            enabled = entityTrackerInitState.isInitialized,
                            shapes = RoundedCornerShape(AppDimensions.dimension_4dp),
                            backgroundColor = if (entityTrackerInitState.isInitialized) pulseColor else disabledMain,
                            textColor = AppColors.TextWhite,
//                            leadingIcon = if (!entityTrackerInitState.isInitialized) {
//                                {
//                                    CircularProgressIndicator(
//                                        modifier = Modifier.size(AppDimensions.dimension_20dp),
//                                        color = AppColors.TextWhite // Changed from Color.White
//                                    )
//                                }
//                            } else null,
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
                            .clickable {
                                isNavBarVisible = !isNavBarVisible
                            }
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
                            onNavigateToHome = navBarOnNavigateToHome,
                            onNavigateToConfigureDemo = navBarOnNavigateToConfigureDemo,
                            onNavigateToSettings = navBarOnNavigateToSettings,
                            onNavigateToAbout = navBarOnNavigateToAbout,
                            onSendFeedback = navBarOnSendFeedback,
                            onBackPressed = navBarOnNavigateToHome,
                            isSDKInitialized = entityTrackerInitState.isInitialized
                        )
                    }
                }

            }
        }
    }

    BackHandler {
        if (isNavBarVisible) {
            isNavBarVisible = false // Hide the navigation bar if visible
        } else {
            onBackPressed() // Call the passed onBackPressed function
        }
    }
}


@Preview(showBackground = true, name = "Home Screen")
@Composable
fun HomeScreenPreview() {
    MaterialTheme {
//        HomeScreen()
    }
}

