// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.viewmodel

import android.app.Application
import android.graphics.RectF
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.zebra.ai.barcodefinder.common.enums.ActionState
import com.zebra.ai.barcodefinder.common.enums.ActionType
import com.zebra.ai.barcodefinder.data.model.ActionableBarcode
import com.zebra.ai.barcodefinder.data.model.BarcodeOverlayItem
import com.zebra.ai.barcodefinder.data.model.ScanResult
import com.zebra.ai.barcodefinder.data.model.ScanStatus
import com.zebra.ai.barcodefinder.data.repository.ActionableBarcodeRepository
import com.zebra.ai.barcodefinder.data.repository.EntityTrackerRepository
import com.zebra.ai.vision.entity.BarcodeEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Represents the UI state for the EntityTracker screen
 */
data class EntityTrackerUiState(
    val isInitialized: Boolean = false,
    val preview: Preview? = null,
    val selectedBarcode: ActionableBarcode? = null,
    val showDialog: Boolean = false,
    val scanResults: List<ScanResult> = emptyList()
)

/**
 * ViewModel for tracking barcode entities and managing UI state for the EntityTracker screen.
 * Handles camera lifecycle, barcode selection, overlay management, scan results, and SDK settings.
 *
 * @constructor Creates an EntityTrackerViewModel with the given application context.
 * @param application The application context
 */
class EntityTrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "EntityTrackerViewModel"

    /** Combined UI state for the EntityTracker screen. */
    private val _uiState = MutableStateFlow(EntityTrackerUiState())
    val uiState: StateFlow<EntityTrackerUiState> = _uiState.asStateFlow()

    /** Overlay items for barcode visualization on the camera preview. */
    private val _overlayItems = MutableSharedFlow<List<BarcodeOverlayItem>>(replay = 1)
    val overlayItems: SharedFlow<List<BarcodeOverlayItem>> = _overlayItems.asSharedFlow()

    /** Singleton repositories for barcode and entity tracking. */
    private val repository: EntityTrackerRepository =
        EntityTrackerRepository.getInstance(application)
    private val actionableBarcodeRepository: ActionableBarcodeRepository =
        ActionableBarcodeRepository.getInstance(application)

    private val _isInitialized = MutableStateFlow(false)
    private val _preview = MutableStateFlow<Preview?>(null)
    private val _selectedBarcode = MutableStateFlow<ActionableBarcode?>(null)
    private val _showDialog = MutableStateFlow(false)
    private val _scanResults = MutableStateFlow<List<ScanResult>>(emptyList())

    /** User data keys for ActionableBarcode userData. */
    private object BarcodeUserDataKeys {
        const val REPLENISH_STOCK = "replenishStock"
        const val PICKED_QUANTITY = "pickedQuantity"
        const val RESULT = "result"
    }

    /**
     * Initializes the ViewModel and sets up state observation for UI and barcode tracking.
     */
    init {
        // Setup combined UI state updates
        setupUiStateUpdates()

        viewModelScope.launch {
            repository.getEntityTrackingResults().collect { results ->
                val overlayItems: List<BarcodeOverlayItem> = results.mapNotNull { entity ->
                    if (entity is BarcodeEntity) {
                        if (entity.value == null || entity.value.isEmpty()) {
                            val actionableBarcode = actionableBarcodeRepository.getEmptyBarcode()
                            BarcodeOverlayItem(
                                bounds = RectF(entity.boundingBox),
                                actionableBarcode = actionableBarcode,
                                icon = actionableBarcode.getActiveIcon(), // Set icon if available
                            )
                        } else {
                            val actionableBarcode =
                                actionableBarcodeRepository.getActionableBarcodeFromTrackList(entity.value)
                            BarcodeOverlayItem(
                                bounds = RectF(entity.boundingBox),
                                actionableBarcode = actionableBarcode,
                                icon = actionableBarcode.getActiveIcon(), // Set icon if available
                                text = if (actionableBarcode.actionType == ActionType.TYPE_QUANTITY_PICKUP && actionableBarcode.actionState == ActionState.STATE_ACTION_NOT_COMPLETED) {
                                    actionableBarcode.quantity.toString()
                                } else {
                                    ""
                                }
                            )
                        }
                    } else {
                        null
                    }
                }

                _overlayItems.emit(overlayItems)
            }
        }
        viewModelScope.launch {
            // Observe repository state and update preview when ready
            repository.repositoryState.collect { state ->
                when (state) {
                    com.zebra.ai.barcodefinder.data.repository.RepositoryState.REPOSITORY_READY -> {
                        _isInitialized.value = true
                        _preview.value = repository.getPreview()
                    }

                    else -> {
                        _isInitialized.value = false
                        _preview.value = null
                    }
                }
            }
        }
        // Observe completed barcodes and map to ScanResult using StateFlow
        viewModelScope.launch {
            actionableBarcodeRepository.actionCompletedBarcodes.collectLatest { completedBarcodes ->
                _scanResults.value = completedBarcodes.map { convertToScanResult(it) }
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
     * Starts camera analysis and binds the camera to the lifecycle and preview view.
     * @param lifecycleOwner The lifecycle owner
     * @param previewView The camera preview view
     */
    fun startAnalyzing(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        // Bind the camera to the lifecycle when available
        _preview.value?.let {
            repository.bindCameraToLifecycle(lifecycleOwner, previewView)
        }
    }

    /**
     * Selects a barcode for dialog display and configuration.
     * @param barcode The barcode to select
     */
    fun selectBarcode(barcode: ActionableBarcode) {
        _selectedBarcode.value = barcode
        _showDialog.value = true
    }

    /**
     * Dismisses the dialog and clears the selected barcode.
     */
    fun dismissDialog() {
        _showDialog.value = false
        _selectedBarcode.value = null
    }

    /**
     * Updates the camera permission state in the repository.
     * @param hasPermission Whether camera permission is granted
     */
    fun updatePermissionState(hasPermission: Boolean) {
        if (hasPermission) {
            repository.onCameraPermissionGranted()
        } else {
            repository.onCameraPermissionDenied()
        }
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
    fun handleQuantityPickup(
        barcode: ActionableBarcode,
        quantityPicked: Int,
        replenishStock: Boolean = false
    ) {
        viewModelScope.launch {
            actionableBarcodeRepository.addActionCompletedBarcode(
                barcode, mapOf(
                    BarcodeUserDataKeys.PICKED_QUANTITY to quantityPicked.toString(),
                    BarcodeUserDataKeys.REPLENISH_STOCK to replenishStock.toString()
                )
            )
        }
    }

    /**
     * Handles product recall action for a barcode.
     * @param barcode The barcode to recall
     */
    fun handleProductRecall(barcode: ActionableBarcode) {
        viewModelScope.launch {
            actionableBarcodeRepository.addActionCompletedBarcode(barcode)
        }
    }

    /**
     * Handles confirm pickup action for a barcode.
     * @param barcode The barcode to confirm
     */
    fun handleConfirmPickup(barcode: ActionableBarcode) {
        viewModelScope.launch {
            actionableBarcodeRepository.addActionCompletedBarcode(barcode)
        }
    }

    /**
     * Clears all completed barcode scan results.
     */
    fun clearBarcodeResults() {
        actionableBarcodeRepository.clearActionCompletedBarcodes()
    }

    /**
     * Converts an ActionableBarcode to a ScanResult for UI display.
     * @param barcode The barcode to convert
     * @return ScanResult for the barcode
     */
    private fun convertToScanResult(barcode: ActionableBarcode): ScanResult {
        val icon = barcode.getIconForState(ActionState.STATE_ACTION_NOT_COMPLETED)
        val status = when (barcode.actionType) {
            ActionType.TYPE_RECALL -> ScanStatus.RecallConfirmed(icon)
            ActionType.TYPE_CONFIRM_PICKUP -> ScanStatus.PickupConfirmed(icon)
            ActionType.TYPE_QUANTITY_PICKUP -> {
                val replenish =
                    barcode.getUserDataValue(BarcodeUserDataKeys.REPLENISH_STOCK)?.toBoolean()
                        ?: false
                val pickedQuantity =
                    barcode.getUserDataValue(BarcodeUserDataKeys.PICKED_QUANTITY)?.toIntOrNull()
                        ?: 0
                val quantity = barcode.quantity
                ScanStatus.QuantityPicked(quantity, pickedQuantity, replenish, icon)
            }

            else -> ScanStatus.NoActionNeeded(icon)
        }
        return ScanResult(
            productName = barcode.productName,
            barcode = barcode.barcodeData,
            status = status,
            additionalInfo = barcode.getUserDataValue(BarcodeUserDataKeys.RESULT)
        )
    }

    /**
     * Applies current settings to the SDK and reinitializes components if needed.
     */
    fun applySettingsToSdk() {
        repository.applySettingsToSdk()
    }

    /**
     * Sets up the combined UI state by observing all individual state flows.
     */
    private fun setupUiStateUpdates() {
        viewModelScope.launch {
            combine(
                _selectedBarcode, _showDialog, _isInitialized, _preview, _scanResults
            ) { selectedBarcode, showDialog, isInitialized, preview, scanResults ->
                EntityTrackerUiState(
                    selectedBarcode = selectedBarcode,
                    showDialog = showDialog,
                    isInitialized = isInitialized,
                    preview = preview,
                    scanResults = scanResults
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }
}
