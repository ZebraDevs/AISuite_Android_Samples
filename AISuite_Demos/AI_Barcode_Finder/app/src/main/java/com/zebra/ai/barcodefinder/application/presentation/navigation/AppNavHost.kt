// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodefinder.application.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zebra.ai.barcodefinder.application.presentation.ui.compose.screens.EULAScreen.EULAScreen
import com.zebra.ai.barcodefinder.application.presentation.ui.compose.screens.aboutscreen.AboutScreen
import com.zebra.ai.barcodefinder.application.presentation.ui.compose.screens.configurescreen.ConfigureScreen
import com.zebra.ai.barcodefinder.application.presentation.ui.compose.screens.finderscreen.FinderScreen
import com.zebra.ai.barcodefinder.application.presentation.ui.compose.screens.homescreen.HomeScreen
import com.zebra.ai.barcodefinder.application.presentation.ui.compose.screens.scanresultscreen.ScanResultsScreen
import com.zebra.ai.barcodefinder.application.presentation.ui.compose.screens.settingsscreen.SettingsScreen
import com.zebra.ai.barcodefinder.application.presentation.ui.compose.screens.settingsscreen.bottomsheet.BarcodeSymbologyBottomSheet
import com.zebra.ai.barcodefinder.application.presentation.ui.compose.screens.settingsscreen.bottomsheet.InferenceBottomSheet
import com.zebra.ai.barcodefinder.application.presentation.ui.compose.screens.settingsscreen.bottomsheet.ModelInputSizeBottomSheet
import com.zebra.ai.barcodefinder.application.presentation.ui.compose.screens.settingsscreen.bottomsheet.ResolutionBottomSheet
import com.zebra.ai.barcodefinder.application.presentation.viewmodel.HomeViewModel

@Composable
fun AppNavHost(
    onExit: () -> Unit
) {
    // preserve the homeViewModel within the lifetime of the application
    val homeViewModel: HomeViewModel = viewModel()
    val navController = rememberNavController()

    var isNavBarVisible by remember { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = NavRoutes.Home.route
    ) {

        composable(NavRoutes.Home.route) {
            HomeScreen(
                homeViewModel = homeViewModel,
                navBarOnNavigateToHome = { navController.navigate(NavRoutes.Home.route) },
                navBarOnNavigateToConfigureDemo = { navController.navigate(NavRoutes.Configure.route) },
                navBarOnNavigateToSettings = { navController.navigate(NavRoutes.Settings.route) },
                navBarOnNavigateToAbout = { navController.navigate(NavRoutes.About.route) },
                navBarOnSendFeedback = {},
                onStartScan = { navController.navigate(NavRoutes.Finder.route) },
                onBackPressed = { onExit() }
            )
        }

        // route for Finder screen
        composable(NavRoutes.Finder.route) {
            FinderScreen(
                onBackPressed = {
                    navController.navigate(NavRoutes.ScanResults.createRoute(from = "backButton")) {
                        // This ensures the FinderScreen is removed from the back stack
                        // Pressing back goes to Results, then Home
                        popUpTo(NavRoutes.Finder.route) { inclusive = true }
                    }
                },
                onViewResultPressed = {
                    // Clicking "View Results" goes to Results, then back to Finder
                    navController.navigate(NavRoutes.ScanResults.createRoute(from = "viewResultsButton"))
                }
            )
        }

        // route for Scan results screen
        composable(
            route = NavRoutes.ScanResults.route,
            arguments = listOf(navArgument("from") {defaultValue="backButton"})
        ){ backStackEntry ->
            backStackEntry.arguments?.getString("from") ?: "backButton"
            ScanResultsScreen(
                onBackPressed = {
                    // If from="backButton", BackStack is [Home, ScanResults] -> pops to [Home]
                    // If from="viewResultsButton", BackStack is [Home, Finder, ScanResults] -> pops to [Home, Finder]
                    navController.popBackStack(NavRoutes.ScanResults.route, inclusive = true)
                }
            )
        }

        // route for Configure screen
        composable(NavRoutes.Configure.route) {
            ConfigureScreen(
                onBackClick = {
                    navController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.Home.route) { inclusive = true }
                    }
                },
                onBackPressed = {
                    navController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.Home.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.Home.route) { inclusive = true }
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
