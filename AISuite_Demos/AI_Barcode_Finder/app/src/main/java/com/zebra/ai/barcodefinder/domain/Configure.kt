package com.zebra.ai.barcodefinder.domain

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.zebra.ai.barcodefinder.domain.enums.ActionState
import com.zebra.ai.barcodefinder.domain.enums.ActionType
import com.zebra.ai.barcodefinder.domain.enums.RepositoryState
import com.zebra.ai.barcodefinder.domain.model.ActionableBarcode
import com.zebra.ai.barcodefinder.domain.model.BarcodeOverlayItem
import com.zebra.ai.barcodefinder.data.source.repository.ActionableBarcodeRepository
import com.zebra.ai.vision.entity.BarcodeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/**
 * Class responsible for configuring and managing barcode-related functionalities.
 *
 * Responsibilities:
 * - Interfaces with EntityTrackerFacade to manage entity tracking and preview functionalities.
 * - Interacts with ActionableBarcodeRepository to manage and retrieve actionable barcode configurations.
 * - Provides methods to update and retrieve state information such as repository state and configured barcodes.
 * - Facilitates mapping of entity tracking results to UI overlay items.
 * - Manages lifecycle operations for camera binding and permission states.
 *
 * Usage:
 * - Use this class to load, update, and clear configured actionable barcodes.
 * - Provides flows for observing repository state and configured barcodes.
 * - Handles conversion of entity tracking results into UI-friendly overlay items.
 * - Capable of handling camera permissions and binding camera operations to lifecycle owners.
 */
class Configure(
    private val entityTrackerFacade: EntityTrackerFacade,
    private val actionableBarcodeRepository: ActionableBarcodeRepository,
) {

    // Expose repository state as a Flow
    val repositoryState: Flow<RepositoryState> = entityTrackerFacade.repositoryState

    // Get the preview from the repository
    fun getPreview() = entityTrackerFacade.getPreview()

    // Get entity tracking results and map them to overlay items
    fun getEntityTrackingResults(): Flow<List<BarcodeOverlayItem>> {
        return entityTrackerFacade.getEntityTrackingResults().map { results ->
            results.mapNotNull { entity ->
                when (entity) {
                    is BarcodeEntity -> createBarcodeOverlayItem(entity)
                    else -> null
                }
            }
        }
    }

    // Load configured actionable barcodes
    fun loadConfiguredActionableBarcodes() {
        actionableBarcodeRepository.loadConfiguredActionableBarcodes()
    }

    // Get configured barcodes as a Flow
    fun getConfiguredBarcodes(): StateFlow<List<ActionableBarcode>> {
        return actionableBarcodeRepository.getLiveConfiguredBarcodes()
    }

    // Create a BarcodeOverlayItem from a BarcodeEntity
    private fun createBarcodeOverlayItem(entity: BarcodeEntity): BarcodeOverlayItem {
        val barcodeValue = entity.value
        return if (!barcodeValue.isNullOrEmpty()) {
            val actionableBarcode =
                actionableBarcodeRepository.getLiveConfiguredActionableBarcodeFromConfigList(barcodeValue)
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
                    text = if (actionableBarcode.actionType == ActionType.TYPE_QUANTITY_PICKUP &&
                        actionableBarcode.actionState == ActionState.STATE_ACTION_NOT_COMPLETED
                    ) {
                        actionableBarcode.quantity.toString()
                    } else ""
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

    // Apply configurations
    fun applyConfigurations() {
        actionableBarcodeRepository.applyConfigurations()
    }

    // Update or remove barcodes
    fun addBarcode(barcode: ActionableBarcode) {
        actionableBarcodeRepository.updateActionableBarcodeInConfigList(barcode)
    }

    fun deleteBarcode(barcode: ActionableBarcode) {
        actionableBarcodeRepository.removeActionableBarcodeFromConfigList(barcode)
    }

    fun clearAllBarcodes() {
        actionableBarcodeRepository.clearConfiguredActionableBarcodes()
    }

    /**
     * Reloads the configured barcodes from the repository.
     */
    fun reloadConfiguredBarcodes() {
        actionableBarcodeRepository.loadConfiguredActionableBarcodes()
    }

    // Method to update camera permission state
    fun updatePermissionState(hasPermission: Boolean) {
        if (hasPermission) {
            entityTrackerFacade.onCameraPermissionGranted()
        } else {
            entityTrackerFacade.onCameraPermissionDenied()
        }
    }

    // Method to get the icon for a specific ActionType
    fun getIconForActionType(actionType: ActionType): Bitmap? {
        return actionableBarcodeRepository.getIconForActionType(actionType)
    }

    fun bindCameraToLifecycle(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        entityTrackerFacade.bindCameraToLifecycle(lifecycleOwner, previewView)
    }

}