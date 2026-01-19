// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodefinder.application.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.zebra.ai.barcodefinder.application.data.source.repository.ActionableBarcodeRepository
import com.zebra.ai.barcodefinder.application.data.source.repository.SettingsRepository
import com.zebra.ai.barcodefinder.application.data.source.storage.SettingsJsonStorage
import com.zebra.ai.barcodefinder.application.domain.services.feedback.BarcodeScanSessionManager
import com.zebra.ai.barcodefinder.application.domain.model.ActionableBarcode
import com.zebra.ai.barcodefinder.application.domain.model.BarcodeOverlayItem
import com.zebra.ai.barcodefinder.sdkcoordinator.EntityTrackerCoordinator
import com.zebra.ai.barcodefinder.application.domain.usecase.FinderUseCase
import com.zebra.ai.barcodefinder.application.presentation.model.EntityTrackerInitState
import com.zebra.ai.barcodefinder.sdkcoordinator.enums.CoordinatorState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.camera.core.ZoomState
import com.zebra.ai.barcodefinder.application.data.services.SystemFeedbackService
import kotlin.coroutines.cancellation.CancellationException

/**
 * ViewModel for tracking barcode entities and managing UI state for the EntityTracker screen.
 * Handles camera lifecycle, barcode selection, overlay management, scan results, and SDK settings.
 *
 * @constructor Creates an EntityTrackerViewModel with the given application context.
 * @param application The application context
 */
class FinderViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "EntityTrackerViewModel"

    private val _overlayItems = MutableSharedFlow<List<BarcodeOverlayItem>>(replay = 1)
    val overlayItems: SharedFlow<List<BarcodeOverlayItem>> = _overlayItems.asSharedFlow()

    private val _selectedBarcode = MutableStateFlow<ActionableBarcode?>(null)
    val selectedBarcode: StateFlow<ActionableBarcode?> = _selectedBarcode.asStateFlow()

    private val _showConfirmActionDialog = MutableStateFlow(false)
    val showConfirmActionDialog: StateFlow<Boolean> = _showConfirmActionDialog.asStateFlow()

    private val _entityTrackerInitState = MutableStateFlow<EntityTrackerInitState>(EntityTrackerInitState())
    val entityTrackerInitState: StateFlow<EntityTrackerInitState> = _entityTrackerInitState.asStateFlow()

    private val entityTrackerCoordinator: EntityTrackerCoordinator = EntityTrackerCoordinator.getInstance(application)

    //TODO :: Implement proper DI
    val settingsJsonStorage = SettingsJsonStorage(application)
    val settingsRepository = SettingsRepository.getInstance(settingsJsonStorage)
    val actionableBarcodeRepository = ActionableBarcodeRepository.getInstance(application)

    val feedbackEngine = SystemFeedbackService(application)

    private val barcodeScanSessionManager = BarcodeScanSessionManager.getInstance(feedbackEngine)

    // Finder UseCase creation
    val finderUseCase = FinderUseCase(actionableBarcodeRepository, entityTrackerCoordinator,settingsRepository,barcodeScanSessionManager)

    private val _zoomScale = MutableStateFlow(1f)
    val zoomScale: StateFlow<Float> = _zoomScale.asStateFlow()

    // Initialize the property directly on the declaration line
    val zoomState: StateFlow<ZoomState?> = finderUseCase.observeZoomState()

    /**
     * Initializes the ViewModel and sets up state observation for UI and barcode tracking.
     */
    init {
        Log.d(TAG, "FinderViewModel initialized")
        observerEntityTrackerInitState()
        startBarcodeProcessing()
    }

    // The UI will call this when it becomes visible/active
    fun bindScanSessionToLifecycle() {
        Log.d(TAG, "bindToLifecycle() called")
        finderUseCase.bindScanSessionToLifecycle()
    }

    // The UI will call this when it goes to the background or is destroyed
    fun unbindScanSessionFromLifecycle() {
        Log.d(TAG, "unbindFromLifecycle() called")
        finderUseCase.unbindScanSessionFromLifecycle()
        finderUseCase.unbindCamera()
    }

    fun resetScanSession(){
        finderUseCase.restScanSession()
    }

    fun updateZoomScale(scale: Float) {
        Log.d(TAG, "updateZoomScale() called with scale: $scale")
        val clampedScale = zoomState.value?.let { scale.coerceIn(it.minZoomRatio, it.maxZoomRatio) } ?: scale

        // Update the UI zoom scale (smooth animation handled in Composable)
        _zoomScale.value = clampedScale

        // Update the hardware zoom asynchronously to avoid blocking the UI
        viewModelScope.launch {
            finderUseCase.setZoomRatio(clampedScale)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared() called")
    }

    /**
     * Binds the camera to the lifecycle and preview view.
     * @param lifecycleOwner The lifecycle owner
     * @param previewView The camera preview view
     */
    fun bindCameraToLifecycle(lifecycleOwner: LifecycleOwner, previewView: PreviewView,initialZoom: Float = 1.0f) {
        Log.d(TAG, "bindCameraToLifecycle() called for lifecycle owner: $lifecycleOwner")
        finderUseCase.bindCamera(lifecycleOwner = lifecycleOwner, previewView = previewView, initialZoom = initialZoom)
    }

    fun unbindCameraToLifecycle(){
        finderUseCase.unbindCamera()
    }

    /**
     * Selects a barcode for dialog display and configuration.
     * @param barcode The barcode to select
     */
    fun selectBarcode(barcode: ActionableBarcode) {
        _selectedBarcode.value = barcode
        _showConfirmActionDialog.value = true
    }

    /**
     * Dismisses the dialog and clears the selected barcode.
     */
    fun dismissDialog() {
        _showConfirmActionDialog.value = false
        _selectedBarcode.value = null
    }

    /**
     * Clears all overlay items from the camera preview.
     */
    fun clearOverlayItems() {
        _overlayItems.tryEmit(emptyList())
    }

    /**
     * Handles quantity pickup action for a barcode and updates user data.
     * @param barcode The barcode to update
     * @param quantityPicked The quantity picked
     * @param replenishStock Whether stock was replenished
     */
    fun handleQuantityPickup(barcode: ActionableBarcode, quantityPicked: Int, replenishStock: Boolean = false) {
        viewModelScope.launch {
            finderUseCase.handleQuantityPickup(barcode, quantityPicked, replenishStock)
        }
    }

    /**
     * Handles confirm pickup action for a barcode.
     * @param barcode The barcode to confirm & recall
     */
    fun handleActionCompletedBarcode(barcode: ActionableBarcode) {
        viewModelScope.launch {
            finderUseCase.addCompletedBarcode(barcode)
        }
    }

    private fun startBarcodeProcessing() {
        Log.d(TAG, "startBarcodeProcessing() called")
        viewModelScope.launch {
            try {
                finderUseCase.processBarcode().collect { result ->
                    _overlayItems.emit(result.overlayItems)
                }
            } catch (e: CancellationException) { // <-- CORRECTED LINE
                // This is the expected and normal way a coroutine is cancelled.
                Log.d(TAG, "startBarcodeProcessing coroutine was cancelled as expected.")
                throw e // Always re-throw CancellationException.
            } catch (e: Exception) {
                // This block will now only catch REAL, unexpected errors.
                Log.e(TAG, "An unexpected error occurred in startBarcodeProcessing", e)
            }
        }
    }

    /**
     * observe the EntityTrackerCoordinator state in EntityTrackerCoordinator and update the EntityTrackerInitState
     */
    fun observerEntityTrackerInitState() {
        Log.d(TAG, "observerEntityTrackerInitState() called")
        finderUseCase.observeEntityTrackerCoordinatorState()
            .map { coordinatorState ->
                Log.d(TAG, "CoordinatorState observed: $coordinatorState")
                if(coordinatorState == CoordinatorState.COORDINATOR_READY) {
                    _entityTrackerInitState.update { it.copy(isInitialized = true) }
                    Log.d(TAG, "Entity tracker init state is updated to ready")
                } else {
                    _entityTrackerInitState.update { it.copy(isInitialized = false) }
                    Log.d(TAG, "Entity tracker init state is updated to not ready")
                }
            }
            .launchIn(viewModelScope)
    }
}
