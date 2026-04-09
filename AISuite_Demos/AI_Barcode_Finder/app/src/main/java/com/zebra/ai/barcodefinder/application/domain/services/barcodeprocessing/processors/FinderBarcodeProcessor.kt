package com.zebra.ai.barcodefinder.application.domain.services.barcodeprocessing.processors

import android.graphics.RectF
import com.zebra.ai.barcodefinder.application.data.source.repository.ActionableBarcodeRepository
import com.zebra.ai.barcodefinder.application.data.source.repository.SettingsRepository
import com.zebra.ai.barcodefinder.application.domain.services.barcodeprocessing.BaseBarcodeProcessor
import com.zebra.ai.barcodefinder.application.domain.enums.ActionState
import com.zebra.ai.barcodefinder.application.domain.enums.ActionType
import com.zebra.ai.barcodefinder.application.domain.enums.BarcodeUserDataKeys
import com.zebra.ai.barcodefinder.application.domain.model.BarcodeProcessingResult
import com.zebra.ai.barcodefinder.application.domain.services.feedback.BarcodeScanSessionManager
import com.zebra.ai.barcodefinder.application.domain.model.ActionableBarcode
import com.zebra.ai.barcodefinder.application.domain.model.BarcodeOverlayItem
import com.zebra.ai.barcodefinder.application.domain.model.ScanResult
import com.zebra.ai.barcodefinder.application.domain.model.ScanStatus
import com.zebra.ai.barcodefinder.sdkcoordinator.EntityTrackerCoordinator
import com.zebra.ai.vision.entity.BarcodeEntity
import com.zebra.ai.vision.entity.Entity

class FinderBarcodeProcessor(
    entityTrackerCoordinator: EntityTrackerCoordinator,
    private val actionableBarcodeRepository: ActionableBarcodeRepository,
    private val settingsRepository: SettingsRepository,
    private val barcodeScanSessionManager: BarcodeScanSessionManager?,
) : BaseBarcodeProcessor(entityTrackerCoordinator) {

    override suspend fun processScreenSpecificLogic(entities: List<Entity>): BarcodeProcessingResult {
        // Single filterIsInstance pass; result shared across feedback and overlay creation
        val barcodeEntities = entities.filterIsInstance<BarcodeEntity>()

        // Fire feedback for actionable barcodes on the current Default thread
        barcodeScanSessionManager?.let {
            val filteredEntities = filterActionableEntities(barcodeEntities)
            it.processBarcodes(filteredEntities)
        }

        // Single-pass overlay creation — no coroutine scheduling overhead per entity
        val overlayItems = ArrayList<BarcodeOverlayItem>(barcodeEntities.size)
        for (entity in barcodeEntities) {
            val item = createBarcodeOverlayItem(entity)
            if (item != null) overlayItems.add(item)
        }

        // Mutex-guarded read — safe across Default and Main threads
        val scanResults = actionableBarcodeRepository.getActionCompletedBarcodes()
            .map { convertToScanResult(it) }

        return BarcodeProcessingResult(
            overlayItems = overlayItems,
            scanResults = scanResults
        )
    }

    private fun createBarcodeOverlayItem(entity: BarcodeEntity): BarcodeOverlayItem? {
        val actionableBarcode = if (entity.value.isNullOrEmpty()) {
            val shouldShowUndecoded = settingsRepository.settings.value.feedbackType.showUndecodedBarcode
            if (shouldShowUndecoded) actionableBarcodeRepository.getEmptyBarcode() else return null
        } else {
            actionableBarcodeRepository.getActionableBarcodeFromTrackList(entity.value)
        }

        return BarcodeOverlayItem(
            bounds = RectF(entity.boundingBox),
            actionableBarcode = actionableBarcode,
            icon = actionableBarcode.getActiveIcon(),
            text = if (
                actionableBarcode.actionType == ActionType.TYPE_QUANTITY_PICKUP &&
                actionableBarcode.actionState == ActionState.STATE_ACTION_NOT_COMPLETED
            ) actionableBarcode.quantity.toString() else ""
        )
    }

    private fun convertToScanResult(barcode: ActionableBarcode): ScanResult {
        val icon = barcode.getIconForState(ActionState.STATE_ACTION_NOT_COMPLETED)
        val status = when (barcode.actionType) {
            ActionType.TYPE_RECALL -> ScanStatus.RecallConfirmed(icon)
            ActionType.TYPE_CONFIRM_PICKUP -> ScanStatus.PickupConfirmed(icon)
            ActionType.TYPE_QUANTITY_PICKUP -> {
                val replenish = barcode.getUserDataValue(BarcodeUserDataKeys.REPLENISH_STOCK)?.toBoolean() ?: false
                val pickedQuantity = barcode.getUserDataValue(BarcodeUserDataKeys.PICKED_QUANTITY)?.toIntOrNull() ?: 0
                ScanStatus.QuantityPicked(barcode.quantity, pickedQuantity, replenish, icon)
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

    private suspend fun filterActionableEntities(barcodeEntities: List<BarcodeEntity>): List<BarcodeEntity> {
        val actionableTypes = setOf(
            ActionType.TYPE_RECALL,
            ActionType.TYPE_CONFIRM_PICKUP,
            ActionType.TYPE_QUANTITY_PICKUP
        )
        val completedBarcodes = actionableBarcodeRepository.getActionCompletedBarcodes().toSet()

        return barcodeEntities.filter { entity ->
            // We can only filter barcodes that have a value and can be looked up.
            // Undecoded barcodes (where entity.value is null or empty) don't have a type yet.
            if (entity.value.isNullOrEmpty()) {
                false
            } else {
                val actionableBarcode = actionableBarcodeRepository.getActionableBarcodeFromTrackList(entity.value)

                // Condition 1: The barcode's action type must be in our set of actionable types.
                val isActionableType = actionableBarcode.actionType in actionableTypes

                // Condition 2: The barcode must NOT be in the set of completed barcodes.
                val isNotCompleted = actionableBarcode !in completedBarcodes

                // The entity passes the filter only if BOTH conditions are true.
                isActionableType && isNotCompleted
            }
        }
    }
}
