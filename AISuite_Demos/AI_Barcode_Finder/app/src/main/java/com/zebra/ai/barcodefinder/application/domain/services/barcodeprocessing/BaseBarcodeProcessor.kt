package com.zebra.ai.barcodefinder.application.domain.services.barcodeprocessing

import com.zebra.ai.barcodefinder.application.domain.model.BarcodeProcessingResult
import com.zebra.ai.barcodefinder.sdkcoordinator.EntityTrackerCoordinator
import com.zebra.ai.vision.entity.Entity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Defines the template for a screen-specific barcode processor.
 *
 * This abstract class handles the common orchestration logic of subscribing to the entity
 * stream from the [EntityTrackerCoordinator].
 *
 * Subclasses are required to implement the screen-specific logic in [processScreenSpecificLogic],
 * including the now-optional feedback processing.
 */
abstract class BaseBarcodeProcessor(
    private val entityTrackerCoordinator: EntityTrackerCoordinator
) {

    /**
     * Constructs and returns the complete processing flow.
     * This method acts as the "template method", orchestrating the shared logic and
     * calling the abstract method for subclass-specific behavior.
     *
     * @return A [Flow] of [BarcodeProcessingResult] ready for UI consumption.
     */
    fun getProcessingFlow(): Flow<BarcodeProcessingResult> {
        return entityTrackerCoordinator.observeEntityTrackingResults()
            .map { entities ->
                // The actual processing logic, including concurrency, is now delegated
                // to the implementing class.
                processScreenSpecificLogic(entities)
            }
    }

    /**
     * Abstract suspend method to be implemented by subclasses. This method should contain all the
     * unique processing logic for a specific screen (e.g., Configure or Finder).
     *
     * @param entities The list of entities from the vision SDK for the current frame.
     * @return A [BarcodeProcessingResult] containing the complete UI state for the screen.
     */
    protected abstract suspend fun processScreenSpecificLogic(entities: List<Entity>): BarcodeProcessingResult
}
