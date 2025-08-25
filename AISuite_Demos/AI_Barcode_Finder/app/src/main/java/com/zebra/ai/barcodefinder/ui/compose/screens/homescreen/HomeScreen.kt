// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.ui.compose.screens.homescreen


import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zebra.ai.barcodefinder.R
import com.zebra.ai.barcodefinder.common.enums.Screen
import com.zebra.ai.barcodefinder.data.model.AppSettings
import com.zebra.ai.barcodefinder.ui.compose.screens.EULAScreen
import com.zebra.ai.barcodefinder.ui.compose.screens.aboutscreen.AboutScreen
import com.zebra.ai.barcodefinder.ui.compose.screens.configurescreen.ConfigureScreen
import com.zebra.ai.barcodefinder.ui.compose.screens.finderscreen.FinderScreen
import com.zebra.ai.barcodefinder.ui.compose.screens.homescreen.components.HomeScreenContent
import com.zebra.ai.barcodefinder.ui.compose.screens.scanresultscreen.ScanResultsScreen
import com.zebra.ai.barcodefinder.ui.compose.screens.settingsscreen.SettingsScreen
import com.zebra.ai.barcodefinder.ui.compose.screens.settingsscreen.bottomsheet.BarcodeSymbologyBottomSheet
import com.zebra.ai.barcodefinder.ui.compose.screens.settingsscreen.bottomsheet.InferenceBottomSheet
import com.zebra.ai.barcodefinder.ui.compose.screens.settingsscreen.bottomsheet.ModelInputSizeBottomSheet
import com.zebra.ai.barcodefinder.ui.compose.screens.settingsscreen.bottomsheet.ResolutionBottomSheet
import com.zebra.ai.barcodefinder.viewmodel.EntityTrackerViewModel
import com.zebra.ai.barcodefinder.viewmodel.SettingsUiState
import com.zebra.ai.barcodefinder.viewmodel.SettingsViewModel

/**
 * Displays the Home screen with navigation to other screens and scan start logic.
 * Handles scan state, navigation, and permission management.
 *
 * @param settingsViewModel The ViewModel for app settings
 * @param entityTrackerViewModel The ViewModel for entity tracking
 * @param onStartScan Callback for starting a scan
 * @param onBackPressed Callback for handling back press
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    settingsViewModel: SettingsViewModel = viewModel(),
    entityTrackerViewModel: EntityTrackerViewModel = viewModel(),
    onStartScan: () -> Unit = {},
    onBackPressed: () -> Unit = {}
) {
    var currentScreen by remember { mutableStateOf(Screen.Home) }
    var isNavBarVisible by remember { mutableStateOf(false) }

    // Observe ViewModel state
    val settings by settingsViewModel.settings.collectAsState()

    // Observe scan started state to trigger navigation
    val settingsUiState by settingsViewModel.uiState.collectAsState()

    // Observe EntityTracker UI state
    val entityTrackerUiState by entityTrackerViewModel.uiState.collectAsState()


    LaunchedEffect(settingsUiState.scanStarted) {
        if (settingsUiState.scanStarted) {
            currentScreen = Screen.Finder
            // Reset the scan started state
            settingsViewModel.resetScanStarted()
        }
    }

    val context = LocalContext.current
    var cameraPermissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraPermissionDenied = !isGranted
        entityTrackerViewModel.updatePermissionState(isGranted)
    }
    // Request camera permission on first composition
    LaunchedEffect(Unit) {
        val permissionStatus = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        )
        if (permissionStatus != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold { paddingValues ->
        if (cameraPermissionDenied) {
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = stringResource(id = R.string.config_camera_permission),
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            // Remove the ModalNavigationDrawer approach
            when (currentScreen) {
                Screen.Home -> {
                    HomeScreenContent(
                        settingsViewModel = settingsViewModel,
                        entityTrackerViewModel = entityTrackerViewModel,
                        settings = settings,
                        uiState = settingsUiState,
                        isSDKInitialized = entityTrackerUiState.isInitialized, // Pass to content
                        onMenuClick = {
                            isNavBarVisible = !isNavBarVisible // Toggle nav bar visibility
                        },
                        isNavBarVisible = isNavBarVisible,
                        onNavigateToHome = {
                            currentScreen = Screen.Home
                            isNavBarVisible = false
                        },
                        onNavigateToConfigureDemo = {
                            currentScreen = Screen.Configure
                            isNavBarVisible = false
                        },
                        onNavigateToSettings = {
                            currentScreen = Screen.Settings
                            isNavBarVisible = false
                        },
                        onNavigateToAbout = {
                            currentScreen = Screen.About
                            isNavBarVisible = false
                        },
                        onSendFeedback = {
                            isNavBarVisible = false
                        },
                        onStartScan = onStartScan, // Pass the onStartScan callback through
                        modifier = Modifier.padding(paddingValues)
                    )
                }

                Screen.Settings -> {
                    SettingsScreen(
                        settingsViewModel = settingsViewModel,
                        onBackClick = {
                            entityTrackerViewModel.applySettingsToSdk()
                            currentScreen = Screen.Home
                        },
                        onBackPressed = {
                            entityTrackerViewModel.applySettingsToSdk()
                            currentScreen = Screen.Home
                        },
                        onResetToDefaultClick = {
                            settingsViewModel.resetToDefaults()
                        }
                    )
                }

                Screen.SettingsResolution -> {
                    ResolutionBottomSheet(
                        onBackClick = {
                            currentScreen = Screen.Settings
                        },
                        onBackPressed = {
                            currentScreen = Screen.Settings
                        }
                    )
                }

                Screen.SettingsModelInputSize -> {
                    ModelInputSizeBottomSheet(
                        settingsViewModel = settingsViewModel,
                        currentSelection = settings.modelInput,
                        onBackClick = {
                            currentScreen = Screen.Settings
                        },
                        onBackPressed = {
                            currentScreen = Screen.Settings
                        }
                    )
                }

                Screen.SettingsInference -> {
                    InferenceBottomSheet(
                        onBackClick = {
                            currentScreen = Screen.Settings
                        },
                        onBackPressed = {
                            currentScreen = Screen.Settings
                        }
                    )
                }

                Screen.SettingsBarcodeSymbology -> {
                    BarcodeSymbologyBottomSheet(
                        onBackClick = {
                            currentScreen = Screen.Settings
                        },
                        onBackPressed = {
                            currentScreen = Screen.Settings
                        }
                    )
                }

                Screen.Configure -> {
                    ConfigureScreen(
                        onBackClick = {
                            currentScreen = Screen.Home
                        },
                        onBackPressed = {
                            currentScreen = Screen.Home
                        },
                        onNavigateToHome = {
                            currentScreen = Screen.Home
                        }
                    )
                }

                Screen.About -> {
                    AboutScreen(
                        onMenuClick = {
                            currentScreen = Screen.Home
                        },
                        onLicenseClick = {
                            currentScreen = Screen.EULA
                        },
                        onBackPressed = {
                            currentScreen = Screen.Home
                        }
                    )
                }

                Screen.EULA -> {
                    EULAScreen(
                        onCloseClick = {
                            currentScreen = Screen.About
                        },
                        onBackPressed = {
                            currentScreen = Screen.About
                        }
                    )
                }

                Screen.Finder -> {
                    FinderScreen(
                        entityTrackerViewModel = entityTrackerViewModel,
                        onBackPressed = {
                            currentScreen = Screen.ScanResults
                        }
                    )
                }

                Screen.ScanResults -> {
                    ScanResultsScreen(
                        barcodeViewModel = entityTrackerViewModel,
                        onBackPressed = {
                            currentScreen = Screen.Home
                        }
                    )
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
        HomeScreen()
    }
}

@Preview(showBackground = true, name = "Home Content Only")
@Composable
fun HomeScreenContentPreview() {
    MaterialTheme {
        val mockSettings = AppSettings()
        val mockUiState = SettingsUiState()

        HomeScreenContent(
            settingsViewModel = viewModel(),
            entityTrackerViewModel = viewModel(),
            settings = mockSettings,
            uiState = mockUiState,
            isSDKInitialized = true, // Provide a default for preview
            onMenuClick = {},
            isNavBarVisible = false,
            onNavigateToHome = {},
            onNavigateToConfigureDemo = {},
            onNavigateToSettings = {},
            onNavigateToAbout = {},
            onSendFeedback = {}
        )
    }
}
