// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.ui.compose.screens.configurescreen

import android.Manifest
import android.annotation.SuppressLint
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zebra.ai.barcodefinder.R
import com.zebra.ai.barcodefinder.common.enums.ActionState
import com.zebra.ai.barcodefinder.common.enums.ActionType
import com.zebra.ai.barcodefinder.data.model.ActionableBarcode
import com.zebra.ai.barcodefinder.ui.compose.components.BarcodeOverlay
import com.zebra.ai.barcodefinder.ui.compose.components.ZebraButton
import com.zebra.ai.barcodefinder.ui.compose.components.ZebraText
import com.zebra.ai.barcodefinder.ui.compose.screens.configurescreen.components.CameraOverlay
import com.zebra.ai.barcodefinder.ui.compose.screens.configurescreen.components.ConfigureActionHandler
import com.zebra.ai.barcodefinder.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.viewmodel.ConfigureViewModel

private const val TAG = "ConfigureScreen"

/**
 * Displays the Configure screen for barcode configuration and camera preview.
 * Handles camera permission, overlay, and barcode selection/configuration.
 *
 * @param configureViewModel The ViewModel for configuration logic
 * @param onBackClick Callback for navigation back
 * @param onBackPressed Callback for handling back press
 * @param onGoToConfigureDemo Optional navigation lambda for demo
 * @param onNavigateToHome Callback for navigating to home
 */
@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigureScreen(
    configureViewModel: ConfigureViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onBackPressed: () -> Unit = {},
    onGoToConfigureDemo: (() -> Unit)? = null, // Add navigation lambda
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Camera controller
    configureViewModel.cameraController.collectAsState().value

    // State for overlay visibility
    var isOverlayVisible by remember { mutableStateOf(true) }

    // Observe state from ConfigureViewModel
    val configurations = configureViewModel.configuredBarcodes.collectAsState().value
    val showActionableBarcodeDialog =
        configureViewModel.showActionableBarcodeDialog.collectAsState().value
    val overlayItems = configureViewModel.overlayItems.collectAsState(initial = emptyList()).value
    val selectedBarcode = configureViewModel.selectedBarcode.collectAsState().value
    val isInitialized = configureViewModel.isInitialized.collectAsState().value

    // Permission launcher for Compose
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        configureViewModel.updatePermissionState(isGranted)
        if (isGranted) {
            Log.d(TAG, "Camera permission granted")
        } else {
            Log.w(TAG, "Camera permission denied")
        }
    }

    // Add DisposableEffect for awake screen
    DisposableEffect(Unit) {
        val window = (context as? androidx.activity.ComponentActivity)?.window
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        configureViewModel.clearOverlayItems()
        configureViewModel.resetViewModel()

        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Start camera when decoder is ready and permission is granted
    LaunchedEffect(isInitialized) {
        if (isInitialized) {
            // Note: Camera binding will happen in AndroidView factory
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        // Camera Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isInitialized) {
                // Camera Preview with CameraProvider
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).also { previewView ->
                            // Bind camera to lifecycle when PreviewView is created
                            configureViewModel.bindCameraToLifecycle(lifecycleOwner, previewView)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Barcode Overlay
                BarcodeOverlay(
                    items = overlayItems,
                    onItemClick = { item ->
                        if (item.actionableBarcode?.actionType != ActionType.TYPE_NONE) {
                            item.actionableBarcode?.let { actionableBarcode ->
                                configureViewModel.selectBarcode(actionableBarcode)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                CameraOverlay(
                    isVisible = isOverlayVisible,
                    onClose = { isOverlayVisible = false }
                )
                // Status indicator overlay
            } else {
                // Permission not granted UI
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(AppDimensions.spacerHeight16),
                        verticalArrangement = Arrangement.Center
                    ) {
                        ZebraText(
                            textValue = stringResource(R.string.config_camera_permission),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(AppDimensions.spacerHeight16))
                        ZebraButton(
                            text = stringResource(R.string.config_grant_camera),
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        )
                    }
                }
            }
        }

        // Barcode action dialog and actionable barcode dialog
        ConfigureActionHandler(
            selectedBarcode = selectedBarcode,
            onDismiss = {
                configureViewModel.dismissConfigureActionDialog()
            },
            onBarcodeConfigured = { barcode, productName, action, quantity ->
                val actionType = action
                configureViewModel.addBarcode(
                    ActionableBarcode(
                        barcodeData = barcode,
                        productName = productName,
                        actionType = actionType,
                        quantityValue = quantity,
                        actionState = ActionState.STATE_ACTION_NOT_COMPLETED
                    )
                )
            },
            onShowActionableScreen = {
                configureViewModel.showActionableBarcodeDialog(true)
            },
            showActionableBarcodeDialog = showActionableBarcodeDialog,
            configurations = configurations,
            onGoToConfigureDemo = onGoToConfigureDemo,
            onNavigateToHome = onNavigateToHome,
            configureViewModel = configureViewModel
        )
    }

    BackHandler {
        onBackPressed()
    }
}

@Preview(showBackground = true, name = "Configure Demo Screen")
@Composable
fun ConfigureDemoScreenPreview() {
    MaterialTheme {
        ConfigureScreen(
            onBackClick = {},
            onBackPressed = {},
            onNavigateToHome = {}
        )
    }
}
