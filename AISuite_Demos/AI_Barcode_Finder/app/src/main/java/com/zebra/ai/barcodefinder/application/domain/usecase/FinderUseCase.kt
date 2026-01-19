package com.zebra.ai.barcodefinder.application.domain.usecase

import androidx.camera.core.ZoomState
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.zebra.ai.barcodefinder.application.data.source.repository.ActionableBarcodeRepository
import com.zebra.ai.barcodefinder.application.domain.enums.BarcodeUserDataKeys
import com.zebra.ai.barcodefinder.application.domain.model.ActionableBarcode
import com.zebra.ai.barcodefinder.sdkcoordinator.EntityTrackerCoordinator
import com.zebra.ai.barcodefinder.application.data.source.repository.SettingsRepository
import com.zebra.ai.barcodefinder.application.domain.services.feedback.BarcodeScanSessionManager
import com.zebra.ai.barcodefinder.application.domain.services.barcodeprocessing.BaseBarcodeProcessor
import com.zebra.ai.barcodefinder.application.domain.model.BarcodeProcessingResult
import com.zebra.ai.barcodefinder.application.domain.services.barcodeprocessing.processors.FinderBarcodeProcessor
import com.zebra.ai.barcodefinder.sdkcoordinator.enums.CoordinatorState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Use case class for managing Finder-related barcode operations.
 *
 * Responsibilities:
 * - Handle quantity pickup for barcodes.
 * - Process barcode results for the Finder screen.
 * - Clear action-completed barcodes.
 * - Add action-completed barcodes to the repository.
 */
class FinderUseCase(
    private val actionableBarcodeRepository: ActionableBarcodeRepository,
    private val entityTrackerCoordinator: EntityTrackerCoordinator,
    private val settingsRepository: SettingsRepository,
    private val barcodeScanSessionManager: BarcodeScanSessionManager
) {

    private val barcodeProcessor: BaseBarcodeProcessor = FinderBarcodeProcessor(
        entityTrackerCoordinator,

        actionableBarcodeRepository,
        settingsRepository,
        barcodeScanSessionManager,
    )

    /**
     * Processes barcode results for the Finder screen.
     * Returns a Flow of BarcodeProcessingResult.
     */
    fun processBarcode(): Flow<BarcodeProcessingResult> {
        return barcodeProcessor.getProcessingFlow()
    }

    /**
     * Handles quantity pickup for a barcode.
     *
     * @param barcode The barcode to handle.
     * @param quantityPicked The quantity picked.
     * @param replenishStock Whether to replenish stock.
     */
    fun handleQuantityPickup(barcode: ActionableBarcode, quantityPicked: Int, replenishStock: Boolean = false) {
        actionableBarcodeRepository.addActionCompletedBarcode(
            barcode,
            mapOf(
                BarcodeUserDataKeys.PICKED_QUANTITY to quantityPicked.toString(),
                BarcodeUserDataKeys.REPLENISH_STOCK to replenishStock.toString()
            )
        )
    }

    /**
     * Adds an action-completed barcode to the repository.
     *
     * @param barcode The barcode to add.
     */
    fun addCompletedBarcode(barcode: ActionableBarcode) {
        actionableBarcodeRepository.addActionCompletedBarcode(barcode)
    }

    // The UI will call this when it becomes visible/active
    fun bindScanSessionToLifecycle() {
        val feedbackSettings =
            settingsRepository.settings.value.feedbackType
        if(feedbackSettings.audio || feedbackSettings.haptics)
        {
            barcodeScanSessionManager.bind(feedbackSettings)
        }
    }


    // The UI will call this when it goes to the background or is destroyed
    fun unbindScanSessionFromLifecycle() {
        barcodeScanSessionManager.unbind()
        entityTrackerCoordinator.unbindCamera()
    }

    fun restScanSession() {
        barcodeScanSessionManager.resetSessionState()
    }

    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        initialZoom: Float = 1.0f // Added optional parameter
    ) {
        entityTrackerCoordinator.bindCameraToLifecycle(lifecycleOwner, previewView, initialZoom)
    }

    /**
     * Unbinds all camera use cases.
     */
    fun unbindCamera() {
        entityTrackerCoordinator.unbindCamera()
    }

    fun observeEntityTrackerCoordinatorState(): StateFlow<CoordinatorState> {
        return entityTrackerCoordinator.coordinatorState
    }

    fun observeZoomState(): StateFlow<ZoomState?> {
        return entityTrackerCoordinator.zoomState
    }

    fun setZoomRatio(ratio: Float) {
        entityTrackerCoordinator.setZoomRatio(ratio)
    }

}