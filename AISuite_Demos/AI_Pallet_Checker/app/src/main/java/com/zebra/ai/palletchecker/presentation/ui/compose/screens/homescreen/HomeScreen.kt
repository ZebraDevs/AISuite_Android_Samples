// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.palletchecker.presentation.ui.compose.screens.homescreen


import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
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
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zebra.ai.palletchecker.helpers.LOGD
import com.zebra.ai.palletchecker.presentation.ui.compose.components.LocalModelStoreOwner
import com.zebra.ai.palletchecker.R
import com.zebra.ai.palletchecker.domain.enums.AutoTriggerMode
import com.zebra.ai.palletchecker.presentation.viewmodel.ConfigureViewModel
import com.zebra.ai.palletchecker.presentation.viewmodel.MainViewModel
import com.zebra.ai.palletchecker.presentation.viewmodel.ScreenState
import com.zebra.ai.palletchecker.presentation.viewmodel.SettingsViewModel

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
    configViewModel: ConfigureViewModel = viewModel(),
    onStartScan: () -> Unit = {},
    onBackPressed: () -> Unit = {},
) {
    var isNavBarVisible by remember { mutableStateOf(false) }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination =navBackStackEntry?.destination

    val owner = LocalModelStoreOwner.current

    val mainViewmodel: MainViewModel = viewModel(viewModelStoreOwner = owner.viewModelStoreOwner!!)
    var modelInitState = mainViewmodel.modelInitState.collectAsState()

    val activity = LocalActivity.current
    val isFromOtherApps = activity?.intent?.action?.equals("android.intent.action.WAREHOUSE_MAIN")
    val exepectedBoxesCount = LocalActivity.current?.intent?.getStringExtra("BOX_COUNT")?.toIntOrNull()
    val TAG = "HomeScreen"

    LaunchedEffect(Unit) {
        LOGD(TAG,"Launch App From Outside")
        LOGD(TAG,"BOX COUNT : $exepectedBoxesCount")
        if (isFromOtherApps == true) {
            exepectedBoxesCount?.let {
                if (it != 0)
                    configViewModel.updateProductQty(exepectedBoxesCount)
            }
            settingsViewModel.updateAutoTriggerMode(AutoTriggerMode.FIXED_QUANTITY)
            settingsViewModel.updateFixedQuantityThreshold(4)
        }
    }

    // Observe ViewModel state
    val settings by settingsViewModel.settings.collectAsState()
    val wandSettings by settingsViewModel.wandSettings.collectAsState()

    val settingsUiState by settingsViewModel.uiState.collectAsState()

    LaunchedEffect(
        settings.modelInput,
        settings.enableAIbarcodeDecode,
        wandSettings.enableAIbarcodeDecode,
        settings.barcodeSymbology
    ) {
        mainViewmodel.initModels { settingsViewModel }
    }

    LaunchedEffect(settingsUiState.scanStarted) {
        if (settingsUiState.scanStarted) {
            settingsViewModel.resetScanStarted()
        }
    }

    val context = LocalContext.current
    var cameraPermissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraPermissionDenied = !isGranted
    }
    LaunchedEffect(Unit) {
        val permissionStatus = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        )
        if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize(), contentWindowInsets = WindowInsets.systemBars) { paddingValues ->
        if (cameraPermissionDenied) {
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = stringResource(id = R.string.home_screen_camera_permission),
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {

            AppNavigation(navController = navController,
                settingsViewModel = settingsViewModel,
                configViewModel = configViewModel,
                mainViewmodel = mainViewmodel,
                settings = settings,
                settingsUiState = settingsUiState,
                isSDKInitialized = modelInitState.value,
                isNavBarVisible = isNavBarVisible,
                onNavBarVisibilityChange = { isVisible ->
                    isNavBarVisible = isVisible
                },
                paddingValues = paddingValues)
        }
    }

    BackHandler(enabled = true) {
        if (isNavBarVisible) {
            isNavBarVisible = false
        } else {
            if(currentDestination?.hasRoute(ScreenState.HOME::class) == true) {
                onBackPressed()
            }
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
