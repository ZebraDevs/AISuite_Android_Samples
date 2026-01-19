package com.zebra.ai.barcodefinder.application.domain.services.barcodeprocessing.processors

import android.graphics.RectF
import com.zebra.ai.barcodefinder.application.data.source.repository.ActionableBarcodeRepository
import com.zebra.ai.barcodefinder.application.data.source.repository.SettingsRepository
import com.zebra.ai.barcodefinder.application.domain.services.barcodeprocessing.BaseBarcodeProcessor
import com.zebra.ai.barcodefinder.application.domain.enums.ActionState
import com.zebra.ai.barcodefinder.application.domain.enums.ActionType
import com.zebra.ai.barcodefinder.application.domain.model.BarcodeProcessingResult
import com.zebra.ai.barcodefinder.application.domain.model.BarcodeOverlayItem
import com.zebra.ai.barcodefinder.sdkcoordinator.EntityTrackerCoordinator
import com.zebra.ai.vision.entity.BarcodeEntity
import com.zebra.ai.vision.entity.Entity
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class ConfigureBarcodeProcessor(
    entityTrackerCoordinator: EntityTrackerCoordinator,
    private val actionableBarcodeRepository: ActionableBarcodeRepository,
    private val settingsRepository: SettingsRepository,
) : BaseBarcodeProcessor(entityTrackerCoordinator) {

    override suspend fun processScreenSpecificLogic(entities: List<Entity>): BarcodeProcessingResult = coroutineScope {
        // Concurrently process all barcode entities to create overlay items.
        val overlayItems = entities.filterIsInstance<BarcodeEntity>()
            .map { entity ->
                async { createBarcodeOverlayItem(entity) }
            }
            .awaitAll()
            .filterNotNull()

        BarcodeProcessingResult(overlayItems = overlayItems.toMutableList())
    }

    private fun createBarcodeOverlayItem(entity: BarcodeEntity): BarcodeOverlayItem? {
        val barcodeValue = entity.value

        return if (!barcodeValue.isNullOrEmpty()) {
            val actionableBarcode =
                actionableBarcodeRepository.getLiveConfiguredActionableBarcodeFromConfigList(barcodeValue)
            if (actionableBarcode == null) {
                val newActionableBarcode = actionableBarcodeRepository.getActionableForData(barcodeValue)
                BarcodeOverlayItem(
                    bounds = RectF(entity.boundingBox),
                    actionableBarcode = newActionableBarcode,
                    backgroundColor = actionableBarcodeRepository.getBackgroundColorForActionType(
                        ActionType.TYPE_NO_ACTION
                    )
                )
            } else {
                BarcodeOverlayItem(
                    bounds = RectF(entity.boundingBox),
                    actionableBarcode = actionableBarcode,
                    icon = actionableBarcode.getIconForState(ActionState.STATE_ACTION_NOT_COMPLETED),
                    text = if (actionableBarcode.actionType == ActionType.TYPE_QUANTITY_PICKUP &&
                        actionableBarcode.actionState == ActionState.STATE_ACTION_NOT_COMPLETED
                    ) {
                        actionableBarcode.quantity.toString()
                    } else ""
                )
            }
        } else {
            val shouldShowUndecoded = settingsRepository.settings.value.feedbackType.showUndecodedBarcode

            if (shouldShowUndecoded) {
                val emptyBarcodeAction = actionableBarcodeRepository.getEmptyBarcode()
                BarcodeOverlayItem(
                    bounds = RectF(entity.boundingBox),
                    actionableBarcode = emptyBarcodeAction,
                    backgroundColor = actionableBarcodeRepository.getBackgroundColorForActionType(
                        ActionType.TYPE_NONE
                    )
                )
            } else {
                // If the setting is FALSE, return null to prevent creating an item.
                null
            }
        }
    }
}
