// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodebatchinventory.application.presentation.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.screens.EULAScreen.EULAScreen
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.screens.aboutscreen.AboutScreen
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.screens.finderscreen.FinderScreen
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.screens.homescreen.HomeScreen
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.screens.scanresultscreen.ScanResultsScreen
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.screens.settingsscreen.SettingsScreen
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.screens.settingsscreen.bottomsheet.BarcodeSymbologyBottomSheet
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.screens.settingsscreen.bottomsheet.InferenceBottomSheet
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.screens.settingsscreen.bottomsheet.ModelInputSizeBottomSheet
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.screens.settingsscreen.bottomsheet.ResolutionBottomSheet
import com.zebra.ai.barcodebatchinventory.application.presentation.viewmodel.HomeViewModel

private const val NAV_TAG = "BatchNavigation"

@Composable
fun AppNavHost(
    onExit: () -> Unit
) {
    // preserve the homeViewModel within the lifetime of the application
    val homeViewModel: HomeViewModel = viewModel()
    val navController = rememberNavController()

    var isNavBarVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Log.d(NAV_TAG, "Navigation graph initialized for batch inventory; configure route is not registered")
    }

    NavHost(
        navController = navController,
        startDestination = NavRoutes.Home.route
    ) {

        composable(NavRoutes.Home.route) {
            HomeScreen(
                homeViewModel = homeViewModel,
                navBarOnNavigateToHome = {
                    Log.d(NAV_TAG, "Navigate: Home -> Home")
                    navController.navigate(NavRoutes.Home.route)
                },
                navBarOnNavigateToSettings = {
                    Log.d(NAV_TAG, "Navigate: Home -> Settings")
                    navController.navigate(NavRoutes.Settings.route)
                },
                navBarOnNavigateToAbout = {
                    Log.d(NAV_TAG, "Navigate: Home -> About")
                    navController.navigate(NavRoutes.About.route)
                },
                navBarOnSendFeedback = {},
                onStartScan = {
                    Log.d(NAV_TAG, "Navigate: Home -> Finder resumeScanning=false")
                    navController.navigate(NavRoutes.Finder.createRoute(resumeScanning = false))
                },
                onBackPressed = {
                    Log.d(NAV_TAG, "Navigate: Home -> Exit")
                    onExit()
                }
            )
        }

        // route for batch inventory scan screen
        composable(
            route = NavRoutes.Finder.route,
            arguments = listOf(navArgument("resumeScanning") { defaultValue = false })
        ) { backStackEntry ->
            val resumeScanning = backStackEntry.arguments?.getBoolean("resumeScanning") ?: false
            LaunchedEffect(resumeScanning) {
                Log.d(NAV_TAG, "Route: Finder resumeScanning=$resumeScanning")
            }
            FinderScreen(
                resumeScanning = resumeScanning,
                onBackPressed = {
                    Log.d(NAV_TAG, "Navigate: Finder -> ScanResults source=backButton")
                    navController.navigate(NavRoutes.ScanResults.createRoute(from = "backButton")) {
                        // This ensures the FinderScreen is removed from the back stack
                        // Pressing back goes to Results, then Home
                        popUpTo(NavRoutes.Finder.route) { inclusive = true }
                    }
                },
                onViewResultPressed = {
                    // Clicking "View Results" goes to Results, then back to Finder
                    Log.d(NAV_TAG, "Navigate: Finder -> ScanResults source=viewResultsButton")
                    navController.navigate(NavRoutes.ScanResults.createRoute(from = "viewResultsButton"))
                }
            )
        }

        // route for Scan results screen
        composable(
            route = NavRoutes.ScanResults.route,
            arguments = listOf(navArgument("from") {defaultValue="backButton"})
        ){ backStackEntry ->
            val source = backStackEntry.arguments?.getString("from") ?: "backButton"
            val scanResultsViewModel: com.zebra.ai.barcodebatchinventory.application.presentation.viewmodel.ScanResultsViewModel = viewModel()
            LaunchedEffect(source) {
                Log.d(NAV_TAG, "Route: ScanResults source=$source")
            }

            ScanResultsScreen(
                onBackPressed = {
                    // Always navigate to Home screen when back button is pressed
                    Log.d(NAV_TAG, "Navigate: ScanResults -> Home")
                    navController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.Home.route) { inclusive = true }
                    }
                },
                onResumeScanning = {
                    // Check if there are saved results to determine if resuming or starting fresh
                    val hasSavedBarcodes = scanResultsViewModel.scanResultsState.value.isNotEmpty()

                    // Navigate back to the batch inventory scan screen
                    // If list is empty (cleared), start fresh (resumeScanning=false)
                    // If list has items, resume with green ticks (resumeScanning=true)
                    Log.d(
                        NAV_TAG,
                        "Navigate: ScanResults -> Finder resumeScanning=$hasSavedBarcodes " +
                                "resultGroups=${scanResultsViewModel.scanResultsState.value.size}"
                    )
                    navController.navigate(NavRoutes.Finder.createRoute(resumeScanning = hasSavedBarcodes)) {
                        // Remove ScanResults from back stack
                        popUpTo(NavRoutes.ScanResults.route) { inclusive = true }
                    }
                }
            )
        }


        // route for Settings screen
        composable(NavRoutes.Settings.route) {
            SettingsScreen(
                onBackClick = {
                    navController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.Home.route) { inclusive = true }
                    }
                    isNavBarVisible = !isNavBarVisible
                },
                onBackPressed = {
                    navController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.Home.route) { inclusive = true }
                    }
                    isNavBarVisible = !isNavBarVisible
                },
            )
        }

        // route for Resolution Settings bottom sheet
        composable(NavRoutes.SettingsResolution.route) {
            ResolutionBottomSheet(
                onBackClick = { navController.navigate(NavRoutes.Settings.route) },
                onBackPressed = { navController.navigate(NavRoutes.Settings.route) }
            )
        }

        // route for Model InputSize Settings bottom sheet
        composable(NavRoutes.SettingsModelInputSize.route) {
            ModelInputSizeBottomSheet(
                onBackClick = { navController.navigate(NavRoutes.Settings.route) },
                onBackPressed = { navController.navigate(NavRoutes.Settings.route) }
            )
        }

        // route for Inference Settings bottom sheet
        composable(NavRoutes.SettingsInference.route) {
            InferenceBottomSheet (
                onBackClick = { navController.navigate(NavRoutes.Settings.route) },
                onBackPressed = { navController.navigate(NavRoutes.Settings.route) }
            )
        }

        // route for Barcode Symbology Settings bottom sheet
        composable(NavRoutes.SettingsBarcodeSymbology.route) {
            BarcodeSymbologyBottomSheet (
                onBackClick = { navController.navigate(NavRoutes.Settings.route) },
                onBackPressed = { navController.navigate(NavRoutes.Settings.route) }
            )
        }

        // route for About Screen
        composable(NavRoutes.About.route) {
            AboutScreen (
                onMenuClick = {
                    navController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.Home.route) { inclusive = true }
                    }
                },
                onLicenseClick = { navController.navigate(NavRoutes.EULA.route) },
                onBackPressed = {
                    navController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.Home.route) { inclusive = true }
                    }
                }
            )
        }

        // route for EULA Screen
        composable(NavRoutes.EULA.route) {
            EULAScreen (
                onCloseClick = { navController.navigate(NavRoutes.About.route) },
                onBackPressed = { navController.navigate(NavRoutes.About.route) }
            )
        }
    }

}
