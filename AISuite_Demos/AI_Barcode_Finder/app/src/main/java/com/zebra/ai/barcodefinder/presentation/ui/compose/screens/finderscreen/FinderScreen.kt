// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.presentation.ui.compose.screens.finderscreen

import android.Manifest
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zebra.ai.barcodefinder.R
import com.zebra.ai.barcodefinder.domain.enums.ActionState
import com.zebra.ai.barcodefinder.presentation.ui.compose.components.BarcodeOverlay
import com.zebra.ai.barcodefinder.presentation.ui.compose.components.ZebraButton
import com.zebra.ai.barcodefinder.presentation.ui.compose.components.ZebraText
import com.zebra.ai.barcodefinder.presentation.ui.compose.screens.finderscreen.components.FinderActionHandler
import com.zebra.ai.barcodefinder.presentation.ui.compose.screens.finderscreen.components.ViewResultOverlay
import com.zebra.ai.barcodefinder.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.presentation.ui.theme.textBlack
import com.zebra.ai.barcodefinder.presentation.ui.theme.white
import com.zebra.ai.barcodefinder.presentation.viewmodel.FinderViewModel


private const val TAG = "BarcodeFinderScreen"

/**
 * Displays the Finder screen for barcode scanning and camera preview.
 * Handles camera permission, overlay, and barcode selection.
 *
 * @param finderViewModel The ViewModel for entity tracking logic
 * @param onBackPressed Callback for handling back press
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinderScreen(
    finderViewModel: FinderViewModel = viewModel(),
    onBackPressed: () -> Unit = {},
    onViewResultPressed: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Observe EntityTracker UI state
    val uiState by finderViewModel.uiState.collectAsState()
    val overlayItems by finderViewModel.overlayItems.collectAsState(initial = emptyList())

    // Permission launcher for Compose
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        finderViewModel.updatePermissionState(isGranted)
        if (isGranted) {
            Log.d(TAG, "Camera permission granted")
        } else {
            Log.w(TAG, "Camera permission denied")
        }
    }

    // Automatically request camera permission if not granted
    LaunchedEffect(Unit) {
        val permissionStatus = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        )
        if (permissionStatus != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Add DisposableEffect for awake screen
    DisposableEffect(Unit) {
        val window = (context as? androidx.activity.ComponentActivity)?.window
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Start camera when decoder is ready and permission is granted
    LaunchedEffect(uiState.isInitialized) {
        if (uiState.isInitialized) {
            // Note: Camera binding will happen in AndroidView factory
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    // Handle onResume
                    finderViewModel.clearOverlayItems()
                }

                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold { paddingValues ->
        // Camera Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isInitialized) {
                // Camera Preview with CameraProvider
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).also { previewView ->
                            // Bind camera to lifecycle when PreviewView is created
                            finderViewModel.bindCameraToLifecycle(
                                lifecycleOwner,
                                previewView
                            )
                            previewView.contentDescription = "CameraPreview"
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Barcode Overlay
                BarcodeOverlay(
                    items = overlayItems,
                    onItemClick = { item ->
                        item.actionableBarcode?.let { actionableBarcode ->
                            // Only show dialog if action is not completed
                            if (actionableBarcode.actionState != ActionState.STATE_ACTION_COMPLETED) {
                                finderViewModel.selectBarcode(actionableBarcode)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics{ contentDescription="ViewFinder" }
                )
                Box(
                    modifier  = Modifier
                        .align(Alignment.TopEnd)
                ){
                    ViewResultOverlay(
                        onClick =  onViewResultPressed
                    )
                }

                // Optional: Status indicator overlay
                if (!uiState.isInitialized) {
                    Card(
                        modifier = Modifier
                            .padding(AppDimensions.MediumPadding),
                        colors = CardDefaults.cardColors(
                            containerColor = textBlack
                        )
                    ) {
                        ZebraText(
                            textValue = stringResource(R.string.finder_screen_init_barcode_scanner),
                            textColor = white,
                            modifier = Modifier.padding(AppDimensions.MediumPadding),
                            fontSize = AppDimensions.dialogTextFontSizeSmall
                        )
                    }
                }
            } else {
                // Permission not granted UI
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(AppDimensions.MediumPadding),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.finder_screen_camera_permission),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(AppDimensions.modifier16))
                        ZebraButton(
                            text = stringResource(R.string.finder_screen_grant_camera),
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        )
                    }
                }
            }
        }

        // Barcode action dialog - Use the comprehensive dialog system
        if (uiState.showDialog && uiState.selectedBarcode != null) {
            FinderActionHandler(
                selectedBarcode = uiState.selectedBarcode,
                showDialog = uiState.showDialog,
                onDismiss = {
                    finderViewModel.dismissDialog()
                }
            )
        }
    }

    BackHandler {
        onBackPressed()
    }
}
