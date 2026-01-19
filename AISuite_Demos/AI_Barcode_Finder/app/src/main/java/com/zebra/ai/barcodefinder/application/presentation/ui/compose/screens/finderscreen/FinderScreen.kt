// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodefinder.application.presentation.ui.compose.screens.finderscreen

import android.Manifest
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zebra.ai.barcodefinder.R
import com.zebra.ai.barcodefinder.application.domain.enums.ActionState
import com.zebra.ai.barcodefinder.application.presentation.ui.compose.components.BarcodeOverlay
import com.zebra.ai.barcodefinder.application.presentation.ui.compose.components.ZebraButton
import com.zebra.ai.barcodefinder.application.presentation.ui.compose.screens.finderscreen.components.FinderActionHandler
import com.zebra.ai.barcodefinder.application.presentation.ui.compose.screens.finderscreen.components.ViewResultOverlay
import com.zebra.ai.barcodefinder.application.presentation.ui.compose.screens.finderscreen.components.ZoomIndicator
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.application.presentation.viewmodel.FinderViewModel
import kotlinx.coroutines.launch

private const val TAG = "EntityTrackerViewModel"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinderScreen(
    onBackPressed: () -> Unit = {},
    onViewResultPressed: () -> Unit = {}
) {
    Log.d(TAG, "FinderScreen Composable is being composed.")
    val finderViewModel: FinderViewModel = viewModel()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val showConfirmActionDialog by finderViewModel.showConfirmActionDialog.collectAsState()
    val selectedBarcode by finderViewModel.selectedBarcode.collectAsState()
    val overlayItems by finderViewModel.overlayItems.collectAsState(initial = emptyList())
    val entityTrackerInitState by finderViewModel.entityTrackerInitState.collectAsState()

    val zoomScale by finderViewModel.zoomScale.collectAsState()
    val zoomState by finderViewModel.zoomState.collectAsState()

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

                finderViewModel.bindScanSessionToLifecycle()

                if (previewView != null) {
                    scope.launch {
                        finderViewModel.bindCameraToLifecycle(lifecycleOwner, previewView!!,zoomScale)
                    }
                } else {
                    // If this logs, we know the view reference was lost or never set
                    Log.e(TAG, "ERROR: PreviewView is NULL! Cannot re-bind camera.")
                }

            } else if (event == Lifecycle.Event.ON_PAUSE) {
                Log.d(TAG, "ON_PAUSE detected.")

                finderViewModel.unbindScanSessionFromLifecycle()

                finderViewModel.unbindCameraToLifecycle() // Ensure you have added this method to ViewModel!
                finderViewModel.resetScanSession()
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
                                view.contentDescription = "CameraPreview"

                                // CAPTURE THE VIEW
                                Log.d(TAG, "SAVING PreviewView reference to state.")
                                previewView = view
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .semantics { contentDescription = "FinderScreen" }
                    )

                    BarcodeOverlay(
                        items = overlayItems,
                        onItemClick = { item ->
                            item.actionableBarcode?.let { actionableBarcode ->
                                Log.d(TAG, "BarcodeOverlay item clicked: ${actionableBarcode.barcodeData}")
                                if (actionableBarcode.actionState != ActionState.STATE_ACTION_COMPLETED) {
                                    finderViewModel.selectBarcode(actionableBarcode)
                                } else {
                                    Log.d(TAG, "Clicked barcode is already completed, not selecting.")
                                }
                            }
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

            Box(
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
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
                    zoomValue = zoomScale
                )
            }
        }

        if (showConfirmActionDialog && selectedBarcode != null) {
            Log.d(TAG, "Displaying FinderActionHandler dialog.")
            FinderActionHandler(
                selectedBarcode = selectedBarcode,
                showDialog = showConfirmActionDialog,
                onDismiss = {
                    Log.d(TAG, "FinderActionHandler dialog dismissed.")
                    finderViewModel.dismissDialog()
                }
            )
        }
    }

    BackHandler {
        Log.d(TAG, "Back button pressed, invoking onBackPressed callback.")
        onBackPressed()
    }
}
