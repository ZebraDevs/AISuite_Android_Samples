// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.screens.finderscreen

import android.Manifest
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zebra.ai.barcodebatchinventory.R
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.components.BarcodeOverlay
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.components.ZebraButton
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.screens.finderscreen.components.CaptureProgressIndicator
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.screens.finderscreen.components.ScanningStatusPill
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.screens.finderscreen.components.ViewResultOverlay
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.screens.finderscreen.components.ZoomIndicator
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodebatchinventory.application.presentation.viewmodel.FinderViewModel
import kotlinx.coroutines.launch

private const val TAG = "BatchScannerScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinderScreen(
    resumeScanning: Boolean = false,
    onBackPressed: () -> Unit = {},
    onViewResultPressed: () -> Unit = {}
) {
    Log.d(TAG, "FinderScreen Composable is being composed. resumeScanning=$resumeScanning")
    val finderViewModel: FinderViewModel = viewModel()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val overlayItems by finderViewModel.overlayItems.collectAsState(initial = emptyList())

    LaunchedEffect(overlayItems) {
        Log.i("AppPerfMon", "UIDisplay: app=BatchInventory" +
                " overlaysRendered=${overlayItems.size}" +
                " thread=${Thread.currentThread().name}")
    }

    val entityTrackerInitState by finderViewModel.entityTrackerInitState.collectAsState()

    val zoomScale by finderViewModel.zoomScale.collectAsState()
    val zoomState by finderViewModel.zoomState.collectAsState()

    // Capture session states
    val isCaptureInProgress by finderViewModel.isCaptureInProgress.collectAsState()
    val captureProgress by finderViewModel.captureProgress.collectAsState()
    val decodedBarcodeCount by finderViewModel.decodedBarcodeCount.collectAsState()
    val showCompletionCheckmark by finderViewModel.showCompletionCheckmark.collectAsState()
    val captureSuccess by finderViewModel.captureSuccess.collectAsState()

    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "Camera permission granted by user.")
        } else {
            Log.w(TAG, "Camera permission denied by user.")
        }
    }

    LaunchedEffect(Unit) {
        Log.d(TAG, "LaunchedEffect for permission check triggered.")
        val permissionStatus = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        )
        if (permissionStatus != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission not granted, launching permission request.")
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            Log.d(TAG, "Camera permission already granted.")
        }
    }

    // Check for previously saved barcodes and enable checkmarks
    // This runs BEFORE the lifecycle observer
    LaunchedEffect(resumeScanning) {
        Log.d(TAG, "LaunchedEffect for resume parameter triggered. resumeScanning=$resumeScanning")
        // Just log for now - actual logic is in ON_RESUME lifecycle event
    }

    DisposableEffect(Unit) {
        Log.d(TAG, "DisposableEffect for keeping screen on.")
        val window = (context as? androidx.activity.ComponentActivity)?.window
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            Log.d(TAG, "Disposing FinderScreen, clearing FLAG_KEEP_SCREEN_ON.")
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->

            if (event == Lifecycle.Event.ON_RESUME) {
                Log.d(TAG, "ON_RESUME detected. resumeScanning=$resumeScanning")

                finderViewModel.bindScanSessionToLifecycle()

                if (previewView != null) {
                    scope.launch {
                        finderViewModel.bindCameraToLifecycle(lifecycleOwner, previewView!!,zoomScale)

                        // Handle resume scanning logic after camera is bound and ready
                        if (resumeScanning) {
                            Log.d(TAG, "Resuming scanning from results screen")
                            finderViewModel.resumeScanningFromResults()
                        } else {
                            Log.d(TAG, "Normal screen load, checking for saved barcodes")
                            finderViewModel.resumeScanning()
                        }
                    }
                } else {
                    // If this logs, we know the view reference was lost or never set
                    Log.e(TAG, "ERROR: PreviewView is NULL! Cannot re-bind camera.")
                }
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                Log.d(TAG, "ON_PAUSE detected.")

                finderViewModel.unbindScanSessionFromLifecycle()

                finderViewModel.unbindCameraToLifecycle() // Ensure you have added this method to ViewModel!
                // Don't reset scan session - we want to preserve checkmarks state when navigating to results
                // finderViewModel.resetScanSession()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            Log.d(TAG, "Disposing ON_START/ON_STOP observer cleanup.")
            lifecycleOwner.lifecycle.removeObserver(observer)
            finderViewModel.unbindScanSessionFromLifecycle()
            finderViewModel.unbindCameraToLifecycle()
            finderViewModel.clearOverlayItems()
            finderViewModel.resetScanSession()
        }
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            val newScale = zoomScale * zoom
                            finderViewModel.updateZoomScale(newScale)
                        }
                    }
            ) {
                if (entityTrackerInitState.isInitialized) {

                    AndroidView(
                        factory = { ctx ->
                            Log.d(TAG, "AndroidView FACTORY executing. Creating PreviewView.")
                            PreviewView(ctx).also { view ->
                                view.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                                view.contentDescription = "CameraPreview"

                                // CAPTURE THE VIEW
                                Log.d(TAG, "SAVING PreviewView reference to state.")
                                previewView = view
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .semantics {
                                contentDescription = "FinderScreen"
                                stateDescription = if (isCaptureInProgress) "CaptureInProgress" else "CaptureIdle"
                            }
                    )

                    BarcodeOverlay(
                        items = overlayItems,
                        onItemClick = { item ->
                            Log.d(TAG, "Barcode overlay clicked: ${item.barcodeData.orEmpty()}")
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .semantics { contentDescription = "ViewFinder" }
                    )
                }
            }

            if (entityTrackerInitState.isInitialized) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    ViewResultOverlay(
                        onClick = {
                            Log.d(TAG, "ViewResultOverlay clicked.")
                            onViewResultPressed()
                        }
                    )
                }
            } else {
                Log.d(TAG, "entityTrackerInitState is NOT initialized.")
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
                                Log.d(TAG, "Grant Camera button clicked.")
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        )
                    }
                }
            }

            // Camera capture button positioned at bottom center
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = AppDimensions.MediumPadding)
            ) {
                // Outer circle (white border)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                        .border(
                            width = 4.dp,
                            color = MaterialTheme.colorScheme.surface,
                            shape = CircleShape
                        )
                        .semantics { contentDescription = "CaptureButton" }
                        .clickable {
                            Log.d(TAG, "Capture button clicked.")
                            finderViewModel.startCaptureSession()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Inner circle (capture button)
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = CircleShape
                            )
                    )
                }
            }

            // Zoom indicators positioned at bottom right corner, stacked vertically
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = AppDimensions.MediumPadding, bottom = AppDimensions.MediumPadding),
                verticalArrangement = Arrangement.spacedBy(AppDimensions.dimension_8dp)
            ) {
                // Default zoom indicator (1x) - shown only when zoomed in
                if (zoomScale > 1f) {
                    ZoomIndicator(
                        onClick = {
                            Log.d(TAG, "Default ZoomIndicator clicked. Resetting zoom to default 1f")
                            finderViewModel.updateZoomScale(1f)
                        },
                        zoomValue = 1f,
                        backgroundAlpha = 0.88f
                    )
                }

                // Main zoom indicator
                ZoomIndicator(
                    onClick = {
                        Log.d(TAG, "ZoomIndicator clicked. Current zoom: $zoomScale")
                        val currentZoom = zoomScale
                        val minZoom = zoomState?.minZoomRatio ?: 0.6f
                        val maxZoom = zoomState?.maxZoomRatio ?: 8f
                        val zoomSteps = listOf(1f, 2f, 4f, 8f)

                        val nextStep = zoomSteps.firstOrNull { it > currentZoom }
                        val newZoom = when {
                            nextStep != null && nextStep <= maxZoom -> nextStep
                            currentZoom >= maxZoom -> minZoom
                            else -> maxZoom
                        }
                        Log.d(TAG, "Updating zoom to: $newZoom")
                        finderViewModel.updateZoomScale(newZoom)
                    },
                    zoomValue = zoomScale,
                    backgroundAlpha = if (zoomScale == 1f) 0f else 0.88f
                )
            }

            if (isCaptureInProgress && !showCompletionCheckmark) {
                Box(
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    ScanningStatusPill()
                }
            }

            // Capture progress indicator in center of screen
            // Show during progress OR when showing completion checkmark
            if (isCaptureInProgress || showCompletionCheckmark) {
                Box(
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    CaptureProgressIndicator(
                        progress = captureProgress,
                        showCheckmark = showCompletionCheckmark,
                        isSuccess = captureSuccess,
                        decodedBarcodeCount = decodedBarcodeCount
                    )
                }
            }
        }

        // Batch inventory mode: No action confirmation dialog needed
        // All decoded barcodes are automatically added to results
    }

    BackHandler {
        Log.d(TAG, "Back button pressed, invoking onBackPressed callback.")
        onBackPressed()
    }
}

