// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.application.presentation.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.zebra.ai.barcodefinder.application.data.services.SystemFeedbackService
import com.zebra.ai.barcodefinder.application.data.source.repository.ActionableBarcodeRepository
import com.zebra.ai.barcodefinder.application.domain.enums.ActionType
import com.zebra.ai.barcodefinder.sdkcoordinator.enums.CoordinatorState
import com.zebra.ai.barcodefinder.application.domain.model.ActionableBarcode
import com.zebra.ai.barcodefinder.application.domain.model.BarcodeOverlayItem
import com.zebra.ai.barcodefinder.application.data.source.repository.SettingsRepository
import com.zebra.ai.barcodefinder.application.data.source.storage.SettingsJsonStorage
import com.zebra.ai.barcodefinder.application.domain.services.feedback.BarcodeScanSessionManager
import com.zebra.ai.barcodefinder.sdkcoordinator.EntityTrackerCoordinator
import com.zebra.ai.barcodefinder.application.domain.usecase.ConfigureUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * ViewModel for configuring barcode actions and managing camera/overlay state.
 * Handles interaction between UI and repositories, including barcode configuration,
 * overlay management, dialog state, and camera lifecycle.
 *
 * @constructor Creates a ConfigureViewModel with the given application context.
 * @param application The application context
 */
class ConfigureViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "ConfigureViewModel"


    // TODO : Need to add the DI
    private val entityTrackerCoordinator: EntityTrackerCoordinator = EntityTrackerCoordinator.getInstance(application)
    private val actionableBarcodeRepository = ActionableBarcodeRepository.getInstance(application)
    val settingsJsonStorage = SettingsJsonStorage(application)
    val settingsRepository = SettingsRepository.getInstance(settingsJsonStorage)

    private val configureUseCase = ConfigureUseCase(repository = actionableBarcodeRepository,entityTrackerCoordinator,settingsRepository)

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _cameraController = MutableStateFlow<Preview?>(null)
    val cameraController: StateFlow<Preview?> = _cameraController.asStateFlow()

    private val _overlayItems = MutableStateFlow<List<BarcodeOverlayItem>>(emptyList())
    val overlayItems: StateFlow<List<BarcodeOverlayItem>> = _overlayItems.asStateFlow()

    private val _configuredBarcodes = MutableStateFlow<List<ActionableBarcode>>(emptyList())
    val configuredBarcodes: StateFlow<List<ActionableBarcode>> = _configuredBarcodes.asStateFlow()

    private val _selectedBarcode = MutableStateFlow<ActionableBarcode?>(null)
    val selectedBarcode: StateFlow<ActionableBarcode?> = _selectedBarcode.asStateFlow()

    private val _showConfigureActionDialog = MutableStateFlow(false)
    val showConfigureActionDialog: StateFlow<Boolean> = _showConfigureActionDialog.asStateFlow()

    private val _showActionableBarcodeDialog = MutableStateFlow(false)
    val showActionableBarcodeDialog: StateFlow<Boolean> = _showActionableBarcodeDialog.asStateFlow()

    // Track if dialog was manually shown to keep it open even when list becomes empty
    private val _wasDialogManuallyShown = MutableStateFlow(false)

    val feedbackEngine = SystemFeedbackService(application)

    private val barcodeScanSessionManager = BarcodeScanSessionManager.getInstance(feedbackEngine)

    /**
     * Initializes the ViewModel by observing repository and barcode states.
     */
    init {
        observeRepositoryState()
        startBarcodeProcessing()
        observeConfiguredBarcodes()
    }

    /**
     * Observes the repository state and updates initialization and camera controller.
     */
    private fun observeRepositoryState() {
        viewModelScope.launch {
            configureUseCase.observeEntityTrackerCoordinatorState().collect { state ->
                when (state) {
                    CoordinatorState.COORDINATOR_READY -> {
                        _isInitialized.value = true
                        _cameraController.value = configureUseCase.getEntityTrackerCoordinatorPreview()
                        Log.d(TAG, "SDK Initialized Successfully")
                    }

                    else -> {
                        _isInitialized.value = false
                        _cameraController.value = null
                        Log.e(TAG, "SDK Initialization Failed : $state")
                    }
                }
            }
        }
    }

    /**
     * Observes entity tracking results and updates overlay items for the camera preview.
     */
    private fun startBarcodeProcessing() {
        configureUseCase.processBarcode()
            .onEach { overlayItems ->
                // Update the state with the overlay items
                _overlayItems.value = overlayItems
            }
            .catch { throwable ->
                // Check if the exception is a cancellation.
                if (throwable is CancellationException) {
                    // This is an expected cancellation. Re-throw it to allow the
                    // coroutine to cancel properly.
                    throw throwable
                } else {
                    // This is a real, unexpected error. Handle it.
                    handleError(throwable)
                }
            }
            .launchIn(viewModelScope) // Launch the flow in the ViewModel's scope
    }

    private fun handleError(throwable: Throwable) {
        // Log the error or update the UI with an error state
        throwable.printStackTrace()
    }

    /**
     * Observes configured barcodes and updates the state flow.
     */
    private fun observeConfiguredBarcodes() {
        viewModelScope.launch {
            configureUseCase.observeConfiguredBarcodes().collect { barcodeDataList ->
                _configuredBarcodes.value = barcodeDataList.map { barcodeData ->
                    ActionableBarcode(
                        barcodeData = barcodeData.barcodeData,
                        productName = barcodeData.productName,
                        actionType = barcodeData.actionType,
                        quantityValue = barcodeData.quantity,
                        actionState = barcodeData.actionState
                    )
                }
            }
        }
    }

    /**
     * Binds the camera to the lifecycle and preview view.
     *
     * @param lifecycleOwner The lifecycle owner
     * @param previewView The camera preview view
     */
    fun bindCameraToLifecycle(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        configureUseCase.bindCamera(lifecycleOwner = lifecycleOwner, previewView = previewView)
    }

    fun unbindCamera() {
        configureUseCase.unbindCamera()
    }

    /**
     * Selects a barcode for configuration and shows the dialog.
     * @param barcode The barcode to select
     */
    fun selectBarcode(barcode: ActionableBarcode) {
        _selectedBarcode.value = barcode
        _showConfigureActionDialog.value = true
    }

    /**
     * Dismisses the configure action dialog and clears the selected barcode.
     */
    fun dismissConfigureActionDialog() {
        _showConfigureActionDialog.value = false
        _selectedBarcode.value = null
    }

    /**
     * Shows or hides the actionable barcode dialog.
     * @param show Whether to show the dialog
     */
    fun showActionableBarcodeDialog(show: Boolean) {
        _showActionableBarcodeDialog.value = show
        if (show) {
            _wasDialogManuallyShown.value = true
        }
    }

    /**
     * Clears all overlay items from the camera preview.
     */
    fun clearOverlayItems() {
        _overlayItems.value = emptyList()
    }

    /**
     * Applies actionable barcode configurations and hides the dialog.
     */
    fun onApplyActionableBarcodes() {
        configureUseCase.applyConfigurations()
        _showActionableBarcodeDialog.value = false
        barcodeScanSessionManager.resetSessionState()
    }

    /**
     * Gets the icon bitmap for a given action type.
     * @param actionType The action type
     * @return Bitmap for the action type icon
     */
    fun getIconForActionType(actionType: ActionType): Bitmap? {
        return configureUseCase.getIconForActionType(actionType)
    }

    /**
     * Adds a barcode to the configuration list and updates state.
     * @param barcode The barcode to add
     */
    fun addBarcode(barcode: ActionableBarcode) {
        viewModelScope.launch {
            configureUseCase.updateBarcode(barcode = barcode)
        }
    }

    /**
     * Deletes a barcode from the configuration list and updates state.
     * @param barcode The barcode to delete
     */
    fun deleteBarcode(barcode: ActionableBarcode) {
        viewModelScope.launch {
            configureUseCase.removeBarcodeFromConfigList(barcode)
        }
        _showActionableBarcodeDialog.value = true
    }

    /**
     * Clears all configured barcodes and updates state.
     */
    fun clearAllBarcodes() {
        viewModelScope.launch {
            configureUseCase.clearAllConfiguredBarcodes()
        }
        _showActionableBarcodeDialog.value = true
    }

    /**
     * Edits a barcode, showing the configuration dialog.
     * @param barcode The barcode to edit
     */
    fun editBarcode(barcode: ActionableBarcode) {
        _selectedBarcode.value = barcode
        _showActionableBarcodeDialog.value = false
        _showConfigureActionDialog.value = true
    }

    /**
     * Resets the ViewModel state and reloads barcode configurations.
     */
    fun resetViewModel() {
        // Reset UI state
        _selectedBarcode.value = null
        _showConfigureActionDialog.value = false
        _wasDialogManuallyShown.value = false
        configureUseCase.reloadConfiguredBarcodes()

        // Collect the Flow and update dialog visibility
        viewModelScope.launch {
            configureUseCase.observeConfiguredBarcodes().collect { barcodeDataList ->
                // Update the dialog visibility based on the collected list
                _showActionableBarcodeDialog.value = barcodeDataList.isNotEmpty()
            }
        }
    }
}
