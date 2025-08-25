// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.viewmodel

import android.app.Application
import android.graphics.RectF
import android.util.Log
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.zebra.ai.barcodefinder.common.enums.ActionState
import com.zebra.ai.barcodefinder.common.enums.ActionType
import com.zebra.ai.barcodefinder.data.model.ActionableBarcode
import com.zebra.ai.barcodefinder.data.model.BarcodeOverlayItem
import com.zebra.ai.barcodefinder.data.repository.ActionableBarcodeRepository
import com.zebra.ai.barcodefinder.data.repository.EntityTrackerRepository
import com.zebra.ai.vision.entity.BarcodeEntity
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
    private val repository: EntityTrackerRepository =
        EntityTrackerRepository.getInstance(application)
    private val actionableBarcodeRepository: ActionableBarcodeRepository =
        ActionableBarcodeRepository.getInstance(application)

    /** Indicates if the SDK and camera are initialized. */
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    /** Holds the current camera preview controller. */
    private val _cameraController = MutableStateFlow<Preview?>(null)
    val cameraController: StateFlow<Preview?> = _cameraController.asStateFlow()

    /** List of overlay items to display on the camera preview. */
    private val _overlayItems = MutableStateFlow<List<BarcodeOverlayItem>>(emptyList())
    val overlayItems: StateFlow<List<BarcodeOverlayItem>> = _overlayItems.asStateFlow()

    /** List of configured actionable barcodes. */
    private val _configuredBarcodes = MutableStateFlow<List<ActionableBarcode>>(emptyList())
    val configuredBarcodes: StateFlow<List<ActionableBarcode>> =
        actionableBarcodeRepository.liveConfiguredActionableBarcodes.asStateFlow()

    /** The currently selected barcode for configuration. */
    private val _selectedBarcode = MutableStateFlow<ActionableBarcode?>(null)
    val selectedBarcode: StateFlow<ActionableBarcode?> = _selectedBarcode.asStateFlow()

    /** Controls visibility of the configure action dialog. */
    private val _showConfigureActionDialog = MutableStateFlow(false)
    val showConfigureActionDialog: StateFlow<Boolean> = _showConfigureActionDialog.asStateFlow()

    /** Controls visibility of the actionable barcode dialog. */
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
            repository.repositoryState.collect { state ->
                when (state) {
                    com.zebra.ai.barcodefinder.data.repository.RepositoryState.REPOSITORY_READY -> {
                        _isInitialized.value = true
                        _cameraController.value = repository.getPreview()
                        Log.e(TAG, "SDK Initialized Successfully")
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
        actionableBarcodeRepository.loadConfiguredActionableBarcodes()
    }

    /**
     * Observes entity tracking results and updates overlay items for the camera preview.
     */
    private fun observeEntityTrackingResults() {
        viewModelScope.launch {
            repository.getEntityTrackingResults().collect { results ->
                val overlayItems = results.mapNotNull { entity ->
                    when (entity) {
                        is BarcodeEntity -> createBarcodeOverlayItem(entity)
                        else -> null
                    }
                }
                _overlayItems.value = overlayItems
            }
        }
    }

    /**
     * Creates a BarcodeOverlayItem from a BarcodeEntity, mapping to actionable barcodes and icons.
     * @param entity The BarcodeEntity to convert
     * @return BarcodeOverlayItem for overlay rendering
     */
    private fun createBarcodeOverlayItem(entity: BarcodeEntity): BarcodeOverlayItem {
        val barcodeValue = entity.value
        return if (!barcodeValue.isNullOrEmpty()) {
            val actionableBarcode =
                actionableBarcodeRepository.getLiveConfiguredActionableBarcodeFromConfigList(
                    barcodeValue
                )
            if (actionableBarcode == null) {
                val newActionableBarcode =
                    actionableBarcodeRepository.getActionableForData(barcodeValue)
                BarcodeOverlayItem(
                    bounds = RectF(entity.boundingBox),
                    actionableBarcode = newActionableBarcode,
                    backgroundColor = actionableBarcodeRepository.getBackgroundColorForActionType(
                        ActionType.TYPE_NO_ACTION
                    )
                )
            } else {
                val iconBitmap =
                    actionableBarcode.getIconForState(ActionState.STATE_ACTION_NOT_COMPLETED)
                BarcodeOverlayItem(
                    bounds = RectF(entity.boundingBox),
                    actionableBarcode = actionableBarcode,
                    icon = iconBitmap,
                    text = if (actionableBarcode.actionType == ActionType.TYPE_QUANTITY_PICKUP && actionableBarcode.actionState == ActionState.STATE_ACTION_NOT_COMPLETED) {
                        actionableBarcode.quantity.toString()
                    } else "",
                )
            }
        } else {
            val emptyBarcodeAction = actionableBarcodeRepository.getEmptyBarcode()
            BarcodeOverlayItem(
                bounds = RectF(entity.boundingBox),
                actionableBarcode = emptyBarcodeAction,
                backgroundColor = actionableBarcodeRepository.getBackgroundColorForActionType(
                    ActionType.TYPE_NONE
                )
            )
        }
    }

    /**
     * Observes configured barcodes and updates the state flow.
     */
    private fun observeConfiguredBarcodes() {
        viewModelScope.launch {
            val barcodeDataList = actionableBarcodeRepository.getLiveConfiguredBarcodes()
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

    /**
     * Binds the camera to the lifecycle and preview view.
     * @param lifecycleOwner The lifecycle owner
     * @param previewView The camera preview view
     */
    fun bindCameraToLifecycle(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        repository.bindCameraToLifecycle(lifecycleOwner, previewView)
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
        if (hasPermission) repository.onCameraPermissionGranted() else repository.onCameraPermissionDenied()
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
        actionableBarcodeRepository.applyConfigurations()
        showActionableBarcodeDialog(false)
    }

    /**
     * Gets the icon bitmap for a given action type.
     * @param actionType The action type
     * @return Bitmap for the action type icon
     */
    fun getIconForActionType(actionType: ActionType): android.graphics.Bitmap? {
        return actionableBarcodeRepository.getIconForActionType(actionType)
    }

    /**
     * Adds a barcode to the configuration list and updates state.
     * @param barcode The barcode to add
     */
    fun addBarcode(barcode: ActionableBarcode) {
        viewModelScope.launch {
            actionableBarcodeRepository.updateActionableBarcodeInConfigList(barcode)
            val barcodeDataList = actionableBarcodeRepository.getLiveConfiguredBarcodes()
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

    /**
     * Deletes a barcode from the configuration list and updates state.
     * @param barcode The barcode to delete
     */
    fun deleteBarcode(barcode: ActionableBarcode) {
        viewModelScope.launch {
            actionableBarcodeRepository.removeActionableBarcodeFromConfigList(barcode)
            val barcodeDataList = actionableBarcodeRepository.getLiveConfiguredBarcodes()
            _configuredBarcodes.value = barcodeDataList

            // Keep showing dialog if it was manually shown, even when list becomes empty
            if (_wasDialogManuallyShown.value) {
                _showActionableBarcodeDialog.value = true
            }
        }
    }

    /**
     * Clears all configured barcodes and updates state.
     */
    fun clearAllBarcodes() {
        viewModelScope.launch {
            actionableBarcodeRepository.clearConfiguredActionableBarcodes()
            val barcodeDataList = actionableBarcodeRepository.getLiveConfiguredBarcodes()
            _configuredBarcodes.value = barcodeDataList.map { barcodeData ->
                ActionableBarcode(
                    barcodeData = barcodeData.barcodeData,
                    productName = barcodeData.productName,
                    actionType = barcodeData.actionType,
                    quantityValue = barcodeData.quantity,
                    actionState = barcodeData.actionState
                )
            }

            // Keep showing dialog if it was manually shown, even when list becomes empty
            if (_wasDialogManuallyShown.value) {
                _showActionableBarcodeDialog.value = true
            }
        }
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
        _selectedBarcode.value = null
        _showConfigureActionDialog.value = false
        _wasDialogManuallyShown.value = false
        reloadConfiguredBarcodes()

        // Only show dialog if configurations exist
        viewModelScope.launch {
            val barcodeDataList = actionableBarcodeRepository.getLiveConfiguredBarcodes()
            _showActionableBarcodeDialog.value = barcodeDataList.isNotEmpty()
        }
    }
}
