// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.presentation.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.zebra.ai.barcodefinder.domain.enums.ActionType
import com.zebra.ai.barcodefinder.domain.enums.RepositoryState
import com.zebra.ai.barcodefinder.domain.model.ActionableBarcode
import com.zebra.ai.barcodefinder.domain.model.BarcodeOverlayItem
import com.zebra.ai.barcodefinder.data.source.repository.ActionableBarcodeRepository
import com.zebra.ai.barcodefinder.domain.EntityTrackerFacade
import com.zebra.ai.barcodefinder.domain.Configure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    private val configure: Configure = Configure(
        EntityTrackerFacade.getInstance(application),
        ActionableBarcodeRepository.getInstance(application),
    )

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

    /**
     * Initializes the ViewModel by observing repository and barcode states.
     */
    init {
        observeRepositoryState()
        observeEntityTrackingResults()
        observeConfiguredBarcodes()
    }

    /**
     * Observes the repository state and updates initialization and camera controller.
     */
    private fun observeRepositoryState() {
        viewModelScope.launch {
            configure.repositoryState.collect { state ->
                when (state) {
                    RepositoryState.REPOSITORY_READY -> {
                        _isInitialized.value = true
                        _cameraController.value = configure.getPreview()
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
     * Reloads the configured barcodes from the repository.
     */
    private fun reloadConfiguredBarcodes() {
        configure.reloadConfiguredBarcodes()
    }

    /**
     * Observes entity tracking results and updates overlay items for the camera preview.
     */
    private fun observeEntityTrackingResults() {
        viewModelScope.launch {
            configure.getEntityTrackingResults().collect { overlayItems ->
                _overlayItems.value = overlayItems
            }
        }
    }
    /**
     * Observes configured barcodes and updates the state flow.
     */
    private fun observeConfiguredBarcodes() {
        viewModelScope.launch {
            configure.getConfiguredBarcodes().collect { barcodeDataList ->
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
        configure.bindCameraToLifecycle(lifecycleOwner, previewView)
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
     * Updates the camera permission state in the repository.
     * @param hasPermission Whether camera permission is granted
     */
    fun updatePermissionState(hasPermission: Boolean) {
        configure.updatePermissionState(hasPermission)
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
        configure.applyConfigurations()
        _showActionableBarcodeDialog.value = false
    }

    /**
     * Gets the icon bitmap for a given action type.
     * @param actionType The action type
     * @return Bitmap for the action type icon
     */
    fun getIconForActionType(actionType: ActionType): Bitmap? {
        return configure.getIconForActionType(actionType)
    }

    /**
     * Adds a barcode to the configuration list and updates state.
     * @param barcode The barcode to add
     */
    fun addBarcode(barcode: ActionableBarcode) {
        viewModelScope.launch {
            configure.addBarcode(barcode)
        }
    }

    /**
     * Deletes a barcode from the configuration list and updates state.
     * @param barcode The barcode to delete
     */
    fun deleteBarcode(barcode: ActionableBarcode) {
        viewModelScope.launch {
            configure.deleteBarcode(barcode)
        }
        _showActionableBarcodeDialog.value = true
    }

    /**
     * Clears all configured barcodes and updates state.
     */
    fun clearAllBarcodes() {
        viewModelScope.launch {
            configure.clearAllBarcodes()
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
        reloadConfiguredBarcodes()

        // Collect the Flow and update dialog visibility
        viewModelScope.launch {
            configure.getConfiguredBarcodes().collect { barcodeDataList ->
                // Update the dialog visibility based on the collected list
                _showActionableBarcodeDialog.value = barcodeDataList.isNotEmpty()
            }
        }
    }
}
