package com.zebra.ai.barcodefinder.domain


import android.app.Application
import android.graphics.RectF
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.zebra.ai.barcodefinder.domain.enums.ActionState
import com.zebra.ai.barcodefinder.domain.enums.ActionType
import com.zebra.ai.barcodefinder.domain.enums.BarcodeUserDataKeys
import com.zebra.ai.barcodefinder.domain.enums.RepositoryState
import com.zebra.ai.barcodefinder.domain.model.*
import com.zebra.ai.barcodefinder.data.source.repository.ActionableBarcodeRepository
import com.zebra.ai.barcodefinder.presentation.model.EntityTrackerUiState
import com.zebra.ai.vision.entity.BarcodeEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

/**
 * Class responsible for managing barcode scanning, analysis, and interaction with the UI.
 *
 * Responsibilities:
 * - Interfaces with EntityTrackerFacade and ActionableBarcodeRepository to manage barcode scanning and tracking.
 * - Provides reactive flows for UI components to observe state changes and overlay items.
 * - Handles camera binding and manages lifecycle integration for barcode analysis.
 * - Supports user interactions such as selecting barcodes, handling quantity pickups, and managing dialogs.
 * - Updates UI state based on repository state and completed barcode actions.
 *
 * Usage:
 * - Use this class to initialize and manage camera lifecycle and permissions.
 * - Provides methods to handle user interactions with barcodes like selecting and dismissing dialogs.
 * - Observes entity tracking results to update overlay items for UI display.
 * - Interacts with repositories to update and clear barcode actions and settings.
 * - Utilize flows such as `uiStateFlow` and `overlayItemsFlow` for observing changes in UI state and overlay items.
 */
class Finder(application: Application) {

    private val entityTrackerFacade = EntityTrackerFacade.getInstance(application)
    private val actionableBarcodeRepository = ActionableBarcodeRepository.getInstance(application)

    private val _uiStateFlow = MutableStateFlow(EntityTrackerUiState())
    val uiStateFlow: StateFlow<EntityTrackerUiState> = _uiStateFlow.asStateFlow()

    private val _overlayItemsFlow = MutableSharedFlow<List<BarcodeOverlayItem>>(replay = 1)
    val overlayItemsFlow: SharedFlow<List<BarcodeOverlayItem>> = _overlayItemsFlow.asSharedFlow()



    init {
        setupUiStateUpdates()
        observeEntityTrackingResults()
    }

    fun bindCameraToLifecycle(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        entityTrackerFacade.bindCameraToLifecycle(lifecycleOwner, previewView)
    }

    fun startAnalyzing(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        entityTrackerFacade.getPreview()?.let {
            entityTrackerFacade.bindCameraToLifecycle(lifecycleOwner, previewView)
        }
    }

    fun selectBarcode(barcode: ActionableBarcode) {
        _uiStateFlow.update { it.copy(selectedBarcode = barcode, showDialog = true) }
    }

    fun dismissDialog() {
        _uiStateFlow.update { it.copy(selectedBarcode = null, showDialog = false) }
    }

    fun updatePermissionState(hasPermission: Boolean) {
        if (hasPermission) {
            entityTrackerFacade.onCameraPermissionGranted()
        } else {
            entityTrackerFacade.onCameraPermissionDenied()
        }
    }

    fun clearOverlayItems() {
        _overlayItemsFlow.tryEmit(emptyList())
    }

    fun handleQuantityPickup(
        barcode: ActionableBarcode,
        quantityPicked: Int,
        replenishStock: Boolean
    ) {
        actionableBarcodeRepository.addActionCompletedBarcode(
            barcode,
            mapOf(
                BarcodeUserDataKeys.PICKED_QUANTITY to quantityPicked.toString(),
                BarcodeUserDataKeys.REPLENISH_STOCK to replenishStock.toString()
            )
        )
    }

    fun handleProductRecall(barcode: ActionableBarcode) {
        actionableBarcodeRepository.addActionCompletedBarcode(barcode)
    }

    fun handleConfirmPickup(barcode: ActionableBarcode) {
        actionableBarcodeRepository.addActionCompletedBarcode(barcode)
    }

    fun clearBarcodeResults() {
        actionableBarcodeRepository.clearActionCompletedBarcodes()
    }

    fun applySettingsToSdk() {
        entityTrackerFacade.applySettingsToSdk()
    }

    private fun setupUiStateUpdates() {
        combine(
            entityTrackerFacade.repositoryState,
            actionableBarcodeRepository.actionCompletedBarcodes
        ) { repositoryState, completedBarcodes ->
            EntityTrackerUiState(
                isInitialized = repositoryState == RepositoryState.REPOSITORY_READY,
                scanResults = completedBarcodes.map { convertToScanResult(it) }
            )
        }.onEach { newState ->
            _uiStateFlow.value = newState
        }.launchIn(CoroutineScope(Dispatchers.IO))
    }

    private fun observeEntityTrackingResults() {
        entityTrackerFacade.getEntityTrackingResults()
            .map { entities ->
                entities.mapNotNull { entity ->
                    if (entity is BarcodeEntity) {
                        // Handle empty or non-empty barcodes
                        val actionableBarcode = if (entity.value.isNullOrEmpty()) {
                            actionableBarcodeRepository.getEmptyBarcode()
                        } else {
                            actionableBarcodeRepository.getActionableBarcodeFromTrackList(entity.value)
                        }

                        // Create BarcodeOverlayItem with conditional text
                        BarcodeOverlayItem(
                            bounds = RectF(entity.boundingBox),
                            actionableBarcode = actionableBarcode,
                            icon = actionableBarcode.getActiveIcon(),
                            text = if (
                                actionableBarcode.actionType == ActionType.TYPE_QUANTITY_PICKUP &&
                                actionableBarcode.actionState == ActionState.STATE_ACTION_NOT_COMPLETED
                            ) {
                                actionableBarcode.quantity.toString() // Add quantity text if conditions are met
                            } else {
                                "" // No text for other cases
                            }
                        )
                    } else {
                        null // Ignore non-barcode entities
                    }
                }
            }
            .onEach { overlayItems ->
                _overlayItemsFlow.emit(overlayItems)
            }
            .launchIn(CoroutineScope(Dispatchers.IO)) // Launch in IO scope
    }

    // Helper function to create BarcodeOverlayItem
    private fun createBarcodeOverlayItem(
        entity: BarcodeEntity,
        actionableBarcode: ActionableBarcode
    ): BarcodeOverlayItem {
        return BarcodeOverlayItem(
            bounds = RectF(entity.boundingBox),
            actionableBarcode = actionableBarcode,
            icon = actionableBarcode.getActiveIcon(), // Set icon if available
            text = if (
                actionableBarcode.actionType == ActionType.TYPE_QUANTITY_PICKUP &&
                actionableBarcode.actionState == ActionState.STATE_ACTION_NOT_COMPLETED
            ) {
                actionableBarcode.quantity.toString() // Add quantity text if conditions are met
            } else {
                "" // No text for other cases
            }
        )
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
}