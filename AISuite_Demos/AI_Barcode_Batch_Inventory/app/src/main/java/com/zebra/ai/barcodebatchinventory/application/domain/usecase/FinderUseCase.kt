package com.zebra.ai.barcodebatchinventory.application.domain.usecase

import androidx.camera.core.ZoomState
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.zebra.ai.barcodebatchinventory.application.data.source.repository.CapturedBarcodeRepository
import com.zebra.ai.barcodebatchinventory.sdkcoordinator.EntityTrackerCoordinator
import com.zebra.ai.barcodebatchinventory.application.data.source.repository.SettingsRepository
import com.zebra.ai.barcodebatchinventory.application.domain.services.feedback.BarcodeScanSessionManager
import com.zebra.ai.barcodebatchinventory.application.domain.services.barcodeprocessing.BaseBarcodeProcessor
import com.zebra.ai.barcodebatchinventory.application.domain.model.BarcodeProcessingResult
import com.zebra.ai.barcodebatchinventory.application.domain.services.barcodeprocessing.processors.FinderBarcodeProcessor
import com.zebra.ai.barcodebatchinventory.sdkcoordinator.enums.CoordinatorState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Use case class for managing batch inventory scan operations.
 *
 * Responsibilities:
 * - Process barcode results for the batch inventory scan screen.
 * - Coordinate captured barcode overlays and feedback.
 * - Bind and unbind the camera and scan session lifecycle.
 */
class FinderUseCase(
    private val capturedBarcodeRepository: CapturedBarcodeRepository,
    private val entityTrackerCoordinator: EntityTrackerCoordinator,
    private val settingsRepository: SettingsRepository,
    private val barcodeScanSessionManager: BarcodeScanSessionManager
) {

    private val barcodeProcessor: BaseBarcodeProcessor = FinderBarcodeProcessor(
        entityTrackerCoordinator,
        capturedBarcodeRepository,
        settingsRepository,
        barcodeScanSessionManager,
    )

    /**
     * Processes barcode results for the batch inventory scan screen.
     * Returns a Flow of BarcodeProcessingResult.
     */
    fun processBarcode(): Flow<BarcodeProcessingResult> {
        return barcodeProcessor.getProcessingFlow()
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

    /**
     * Starts barcode decoding.
     */
    fun startDecoding() {
        entityTrackerCoordinator.startDecoding()
    }

    /** Lock AF on center for the scan window. */
    fun triggerAutoFocus() {
        entityTrackerCoordinator.triggerAutoFocus()
    }

    /** Release AF lock so continuous picture AF resumes between scans. */
    fun enableContinuousAutoFocus() {
        entityTrackerCoordinator.enableContinuousAutoFocus()
    }

    /**
     * Stops barcode decoding.
     * @param clearOverlays Whether to clear the overlay items or keep them visible
     */
    fun stopDecoding(clearOverlays: Boolean = true) {
        entityTrackerCoordinator.stopDecoding(clearOverlays)
    }

    /**
     * Controls whether to show checkmarks on decoded barcodes.
     */
    fun setShowCheckmarksOnBarcodes(show: Boolean) {
        (barcodeProcessor as? FinderBarcodeProcessor)?.showCheckmarksOnBarcodes = show
    }

    /**
     * Returns the number of barcode entities (decoded + undecoded) detected in the most recent frame.
     * Used by the ViewModel to calculate the dynamic capture timeout.
     */
    fun getDetectedBarcodeCount(): Int {
        return (barcodeProcessor as? FinderBarcodeProcessor)?.detectedBarcodeCount ?: 0
    }

    /**
     * Returns the number of decoded barcode entities in the most recent frame.
     */
    fun getDecodedBarcodeCount(): Int {
        return (barcodeProcessor as? FinderBarcodeProcessor)?.decodedBarcodeCount ?: 0
    }

    /**
     * Resets the detected barcode count to 0 so Phase 1 waits for a fresh detection frame.
     * Must be called before starting a new capture session.
     */
    fun resetDetectedBarcodeCount() {
        (barcodeProcessor as? FinderBarcodeProcessor)?.resetDetectedBarcodeCount()
    }

    /**
     * Triggers audio and haptic feedback (called after capture timeout completes)
     */
    fun triggerFeedback() {
        barcodeScanSessionManager.triggerFeedback()
    }

    /**
     * Refreshes the entity overlays to update the display with current settings.
     */
    fun refreshEntityOverlays() {
        entityTrackerCoordinator.refreshEntityOverlays()
    }

}