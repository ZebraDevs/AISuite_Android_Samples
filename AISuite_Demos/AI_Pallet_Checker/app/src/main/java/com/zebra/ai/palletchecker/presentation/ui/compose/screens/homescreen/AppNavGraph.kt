package com.zebra.ai.palletchecker.presentation.ui.compose.screens.homescreen

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.zebra.ai.palletchecker.domain.model.AppSettings
import com.zebra.ai.palletchecker.presentation.model.SettingsUiState
import com.zebra.ai.palletchecker.presentation.ui.compose.screens.EULAScreen.EULAScreen
import com.zebra.ai.palletchecker.presentation.ui.compose.screens.aboutscreen.AboutScreen
import com.zebra.ai.palletchecker.presentation.ui.compose.screens.configurescreen.Configuration
import com.zebra.ai.palletchecker.presentation.ui.compose.screens.homescreen.components.HomeScreenContent
import com.zebra.ai.palletchecker.presentation.ui.compose.screens.results.ResultsScreen
import com.zebra.ai.palletchecker.presentation.ui.compose.screens.settingsscreen.SettingsDashboard
import com.zebra.ai.palletchecker.presentation.ui.compose.screens.settingsscreen.bottomsheet.BarcodeSymbologyBottomSheet
import com.zebra.ai.palletchecker.presentation.ui.compose.screens.settingsscreen.bottomsheet.InferenceBottomSheet
import com.zebra.ai.palletchecker.presentation.ui.compose.screens.settingsscreen.bottomsheet.ModelInputSizeBottomSheet
import com.zebra.ai.palletchecker.presentation.ui.compose.screens.settingsscreen.bottomsheet.ResolutionBottomSheet
import com.zebra.ai.palletchecker.presentation.ui.compose.screens.viewfinder.ViewFinder
import com.zebra.ai.palletchecker.presentation.viewmodel.CameraViewModel
import com.zebra.ai.palletchecker.presentation.viewmodel.ConfigureViewModel
import com.zebra.ai.palletchecker.presentation.viewmodel.MainViewModel
import com.zebra.ai.palletchecker.presentation.viewmodel.PROCESS_TYPE
import com.zebra.ai.palletchecker.presentation.viewmodel.ScreenState
import com.zebra.ai.palletchecker.presentation.viewmodel.SettingsViewModel

@Composable
fun AppNavigation(
    settingsViewModel: SettingsViewModel,
    configViewModel: ConfigureViewModel,
    mainViewmodel: MainViewModel,
    settings: AppSettings,
    settingsUiState: SettingsUiState,
    isSDKInitialized: Boolean = false,
    onNavBarVisibilityChange: (Boolean) -> Unit,
    isNavBarVisible: Boolean = false,
    paddingValues: PaddingValues,
    navController: NavHostController = rememberNavController()
) {
    val activity = LocalActivity.current
    val isFromOtherApps = activity?.intent?.action?.equals("android.intent.action.WAREHOUSE_MAIN")

    NavHost(
        navController = navController,
        startDestination = if (isFromOtherApps == true) ScreenState.FINDER(PROCESS_TYPE.CAPTURE_PALLET_BOX.name) else ScreenState.HOME,
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        composable<ScreenState.HOME> { backstackEntry ->
            val appConfig by configViewModel.config.collectAsState()
            HomeScreenContent(
                settingsViewModel = settingsViewModel,
                settings = settings,
                uiState = settingsUiState,
                isSDKInitialized = isSDKInitialized,
                onMenuClick = {
                    onNavBarVisibilityChange(!isNavBarVisible)
                },
                isNavBarVisible = isNavBarVisible,
                onNavigateToHome = {
                    navController.navigate(ScreenState.HOME)
                    onNavBarVisibilityChange(false)
                },
                onNavigateToConfigureDemo = {
                    navController.navigate(ScreenState.CONFIGURE)
                    onNavBarVisibilityChange(false)
                },
                onNavigateToSettings = {
                    navController.navigate(ScreenState.SETTINGS)
                    onNavBarVisibilityChange(false)
                },
                onNavigateToAbout = {
                    navController.navigate(ScreenState.ABOUT)
                    onNavBarVisibilityChange(false)
                },
                onSendFeedback = {
                    onNavBarVisibilityChange(false)
                },
                onStartScan = {
                    navController.navigate(ScreenState.FINDER(processType = PROCESS_TYPE.CAPTURE_PALLET_BOX.name)){
                        launchSingleTop = true
                    }
                },
                expectedBoxes = appConfig.expectedBoxes,
                context = LocalContext.current,
                modifier = Modifier
                    .semantics { contentDescription = "HomeScreen" }
            )
        }

        composable<ScreenState.SETTINGS>{backStackEntry->
            val configForSettings by configViewModel.config.collectAsState()
            SettingsDashboard(
                settingsViewModel = settingsViewModel,
                onBackClick = {
                    navController.popBackStack()
                },
                onBackPressed = {
                    navController.popBackStack()
                },
                onResetToDefaultClick = {
                    settingsViewModel.resetToDefaults()
                },
                expectedBoxesToAudit = configForSettings.expectedBoxes
            )
        }

        composable<ScreenState.RESOLUTION> { backStackEntry->
            ResolutionBottomSheet(
                onBackClick = {
                    navController.popBackStack()
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }

        composable<ScreenState.MODEL_INPUT> { backStackEntry->
            ModelInputSizeBottomSheet(
                settingsViewModel = settingsViewModel,
                currentSelection = settings.modelInput,
                onBackClick = {
                    navController.popBackStack()
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }

        composable<ScreenState.INFERENCE> { backStackEntry->
            InferenceBottomSheet(
                onBackClick = {
                    navController.popBackStack()
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }

        composable<ScreenState.SYMBOLOGY> { backStackEntry->
            BarcodeSymbologyBottomSheet(
                onBackClick = {
                    navController.popBackStack()
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }

        composable<ScreenState.CONFIGURE> { backStackEntry->
            Configuration (
                onBackPress = {
                    navController.popBackStack()
                }
            )
        }

        composable<ScreenState.FINDER> { backStackEntry->
            val profileRoute = backStackEntry.toRoute<ScreenState.FINDER>()
            val processType = PROCESS_TYPE.valueOf(profileRoute.processType)
            val cameraViewModel: CameraViewModel = viewModel(backStackEntry)
            ViewFinder(
                settingsViewModel,
                configViewModel,
                cameraModel=cameraViewModel,
                onBackPress = {
                    if (isFromOtherApps == true) {
                        activity.setResult(Activity.RESULT_CANCELED)
                        activity.finish()
                        return@ViewFinder
                    }
                    if (processType == PROCESS_TYPE.CAPTURE_PALLET_BOX) {
                        // Pre-snap phase: go back to HOME
                        navController.popBackStack(ScreenState.HOME, inclusive = false)
                    } else if(processType == PROCESS_TYPE.AUDIT_PARTAIL_PALLET_BOX) {
                        // Wand mode: clean up and start a fresh snap session
                        mainViewmodel.cancelWandTimer()
                        mainViewmodel.clearSession()
                        mainViewmodel.clearCachedPalletBoxes()
                        navController.navigate(ScreenState.FINDER(processType = PROCESS_TYPE.CAPTURE_PALLET_BOX.name)) {
                            popUpTo(ScreenState.HOME) { inclusive = false }
                            launchSingleTop = true
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
                processType = processType,
                onResult = { type ->

                    if (type == PROCESS_TYPE.CAPTURE_PALLET_BOX || type == PROCESS_TYPE.AUDIT_PARTAIL_PALLET_BOX) {
                        navController.navigate(ScreenState.RESULT) {
                            launchSingleTop = true
                        }
                    }

                }
            )
        }

        composable<ScreenState.RESULT> { backStackEntry->
            ResultsScreen(
                settingsViewModel,
                configViewModel,
                mainViewmodel,
                isFromWand = mainViewmodel.hasWandCompleted(),
                onBackPress = {
                    // Back from results always starts a clean new session
                    mainViewmodel.clearSession()
                    mainViewmodel.clearCachedPalletBoxes()
                    navController.navigate(ScreenState.FINDER(processType = PROCESS_TYPE.CAPTURE_PALLET_BOX.name)) {
                        popUpTo(ScreenState.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onAuditPreview = { allBoxes, expectedBoxes, sourceResolution, targetViewSize, pipEnabled ->
                    mainViewmodel.seedSnapValidations(allBoxes)
                    mainViewmodel.updatePartialDetections(allBoxes, expectedBoxes, sourceResolution, targetViewSize, pipEnabled)
                    navController.navigate(ScreenState.FINDER(processType = PROCESS_TYPE.AUDIT_PARTAIL_PALLET_BOX.name)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<ScreenState.ABOUT> { backStackEntry->
            AboutScreen(
                onMenuClick = {
                    navController.navigate(ScreenState.HOME) {
                        launchSingleTop = true
                    }
                },
                onLicenseClick = {
                    navController.navigate(ScreenState.EULA) {
                        launchSingleTop = true
                    }
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }

        composable<ScreenState.EULA> { backStackEntry->
            EULAScreen(
                onCloseClick = {
                    navController.popBackStack()
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
    }
}

