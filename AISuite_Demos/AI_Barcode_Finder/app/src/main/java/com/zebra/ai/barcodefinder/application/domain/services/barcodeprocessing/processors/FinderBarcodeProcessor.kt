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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FinderBarcodeProcessor(
    entityTrackerCoordinator: EntityTrackerCoordinator,
    private val actionableBarcodeRepository: ActionableBarcodeRepository,
    private val settingsRepository: SettingsRepository,
    private val barcodeScanSessionManager: BarcodeScanSessionManager?,
) : BaseBarcodeProcessor(entityTrackerCoordinator) {

    override suspend fun processScreenSpecificLogic(entities: List<Entity>): BarcodeProcessingResult = coroutineScope {
        // If barcodeScanSessionManager is provided, launch its processing in the background.
        barcodeScanSessionManager?.let {
            // Filter the entities to only include the specified action types
            val filteredEntities = filterActionableEntities(entities)
            launch {
                it.processBarcodes(filteredEntities)
            }
        }

        // Part A (Deferred): Create overlay items concurrently.
        val deferredOverlayItems = async {
            entities.filterIsInstance<BarcodeEntity>()
                .map { entity ->
                    async { createBarcodeOverlayItem(entity) }
                }
                .awaitAll()
                .filterNotNull()
        }

        // Part B (Deferred): Generate the list of scan results concurrently without blocking.
        val deferredScanResults = async {
            actionableBarcodeRepository.actionCompletedBarcodes.first().map { convertToScanResult(it) }
        }

        // Await the results from both concurrent tasks and return the final result.
        BarcodeProcessingResult(
            overlayItems = deferredOverlayItems.await().toMutableList(),
            scanResults = deferredScanResults.await()
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

    private fun filterActionableEntities(entities: List<Entity>): List<BarcodeEntity> {
        val actionableTypes = setOf(
            ActionType.TYPE_RECALL,
            ActionType.TYPE_CONFIRM_PICKUP,
            ActionType.TYPE_QUANTITY_PICKUP
        )
        val completedBarcodes = actionableBarcodeRepository.getActionCompletedBarcodes().toSet()

        return entities.filterIsInstance<BarcodeEntity>().filter { entity ->
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
