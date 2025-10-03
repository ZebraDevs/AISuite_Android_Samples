// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.presentation.ui.compose.screens.homescreen


import android.Manifest
import android.widget.Toast
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zebra.ai.barcodefinder.R
import com.zebra.ai.barcodefinder.presentation.enums.Screen
import com.zebra.ai.barcodefinder.domain.model.AppSettings
import com.zebra.ai.barcodefinder.presentation.model.SettingsUiState
import com.zebra.ai.barcodefinder.presentation.ui.compose.screens.EULAScreen.EULAScreen
import com.zebra.ai.barcodefinder.presentation.ui.compose.screens.aboutscreen.AboutScreen
import com.zebra.ai.barcodefinder.presentation.ui.compose.screens.configurescreen.ConfigureScreen
import com.zebra.ai.barcodefinder.presentation.ui.compose.screens.finderscreen.FinderScreen
import com.zebra.ai.barcodefinder.presentation.ui.compose.screens.homescreen.components.HomeScreenContent
import com.zebra.ai.barcodefinder.presentation.ui.compose.screens.scanresultscreen.ScanResultsScreen
import com.zebra.ai.barcodefinder.presentation.ui.compose.screens.settingsscreen.SettingsScreen
import com.zebra.ai.barcodefinder.presentation.ui.compose.screens.settingsscreen.bottomsheet.BarcodeSymbologyBottomSheet
import com.zebra.ai.barcodefinder.presentation.ui.compose.screens.settingsscreen.bottomsheet.InferenceBottomSheet
import com.zebra.ai.barcodefinder.presentation.ui.compose.screens.settingsscreen.bottomsheet.ModelInputSizeBottomSheet
import com.zebra.ai.barcodefinder.presentation.ui.compose.screens.settingsscreen.bottomsheet.ResolutionBottomSheet
import com.zebra.ai.barcodefinder.presentation.viewmodel.FinderViewModel
import com.zebra.ai.barcodefinder.presentation.viewmodel.SettingsViewModel

/**
 * Displays the Home screen with navigation to other screens and scan start logic.
 * Handles scan state, navigation, and permission management.
 *
 * @param settingsViewModel The ViewModel for app settings
 * @param finderViewModel The ViewModel for entity tracking
 * @param onStartScan Callback for starting a scan
 * @param onBackPressed Callback for handling back press
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    settingsViewModel: SettingsViewModel = viewModel(),
    finderViewModel: FinderViewModel = viewModel(),
    onStartScan: () -> Unit = {},
    onBackPressed: () -> Unit = {},
) {
    var currentScreen by remember { mutableStateOf(Screen.Home) }
    var isNavBarVisible by remember { mutableStateOf(false) }
    var previousScreen by remember { mutableStateOf(Screen.Home) }

    // Observe ViewModel state
    val settings by settingsViewModel.settings.collectAsState()

    // Observe scan started state to trigger navigation
    val settingsUiState by settingsViewModel.uiState.collectAsState()

    // Observe EntityTracker UI state
    val entityTrackerUiState by finderViewModel.uiState.collectAsState()

    val appContext = LocalContext.current
    val inferenceErrorMessage = stringResource(id = R.string.home_screen_inference_error_toast)
    // Observe errorFlow to display Toast messages
    LaunchedEffect(finderViewModel.errorFlowInference) {
        finderViewModel.errorFlowInference.collect { errorMessage ->
            // Show a Toast message for the error
            Toast.makeText(appContext, inferenceErrorMessage, Toast.LENGTH_LONG).show()
        }
    }

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
        finderViewModel.updatePermissionState(isGranted)
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
                    text = stringResource(id = R.string.home_screen_camera_permission),
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
                        finderViewModel = finderViewModel,
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
                        context = context,
                        modifier = Modifier
                            .padding(paddingValues)
                            .semantics{contentDescription = "HomeScreen"}
                    )
                }

                Screen.Settings -> {
                    SettingsScreen(
                        settingsViewModel = settingsViewModel,
                        onBackClick = {
                            finderViewModel.applySettingsToSdk()
                            currentScreen = Screen.Home
                        },
                        onBackPressed = {
                            finderViewModel.applySettingsToSdk()
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
                        finderViewModel = finderViewModel,
                        onBackPressed = {
                            currentScreen = Screen.ScanResults
                            previousScreen = Screen.Home
                        },
                        onViewResultPressed ={
                            currentScreen = Screen.ScanResults
                            previousScreen = Screen.Finder
                        }
                    )
                }

                Screen.ScanResults -> {
                    ScanResultsScreen(
                        barcodeViewModel = finderViewModel,
                        onBackPressed = {
                            currentScreen = previousScreen
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

