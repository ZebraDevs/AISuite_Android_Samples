package com.zebra.ai.barcodebatchinventory.application.domain.services.barcodeprocessing.processors

import android.graphics.RectF
import com.zebra.ai.barcodebatchinventory.application.data.source.repository.CapturedBarcodeRepository
import com.zebra.ai.barcodebatchinventory.application.data.source.repository.SettingsRepository
import com.zebra.ai.barcodebatchinventory.application.domain.model.BarcodeOverlayItem
import com.zebra.ai.barcodebatchinventory.application.domain.model.BarcodeProcessingResult
import com.zebra.ai.barcodebatchinventory.application.domain.services.barcodeprocessing.BaseBarcodeProcessor
import com.zebra.ai.barcodebatchinventory.application.domain.services.feedback.BarcodeScanSessionManager
import com.zebra.ai.barcodebatchinventory.sdkcoordinator.EntityTrackerCoordinator
import com.zebra.ai.vision.entity.BarcodeEntity
import com.zebra.ai.vision.entity.Entity

class FinderBarcodeProcessor(
    entityTrackerCoordinator: EntityTrackerCoordinator,
    private val capturedBarcodeRepository: CapturedBarcodeRepository,
    private val settingsRepository: SettingsRepository,
    private val barcodeScanSessionManager: BarcodeScanSessionManager?,
) : BaseBarcodeProcessor(entityTrackerCoordinator) {

    @Volatile
    var showCheckmarksOnBarcodes = false

    /**
     * Values decoded in the capture session in progress, or null outside a capture. Decoded
     * values are added here as frames are processed so their ticks draw immediately instead of
     * after the session is saved. Must be thread-safe.
     */
    @Volatile
    var liveSessionValues: MutableSet<String>? = null

    @Volatile
    var detectedBarcodeCount: Int = 0
        private set

    @Volatile
    var decodedBarcodeCount: Int = 0
        private set

    fun resetDetectedBarcodeCount() {
        detectedBarcodeCount = 0
        decodedBarcodeCount = 0
    }

    override suspend fun processScreenSpecificLogic(entities: List<Entity>): BarcodeProcessingResult {
        val barcodeEntities = entities.filterIsInstance<BarcodeEntity>()
        detectedBarcodeCount = barcodeEntities.size

        val decodedEntities = ArrayList<BarcodeEntity>(barcodeEntities.size)
        for (entity in barcodeEntities) {
            if (!entity.value.isNullOrEmpty()) {
                decodedEntities.add(entity)
            }
        }
        decodedBarcodeCount = decodedEntities.size

        val liveValues = liveSessionValues
        if (liveValues != null) {
            for (entity in decodedEntities) {
                val value = entity.value?.trim()
                if (!value.isNullOrEmpty()) liveValues.add(value)
            }
        }

        if (decodedEntities.isNotEmpty()) {
            barcodeScanSessionManager?.processBarcodes(decodedEntities)
        }

        val shouldShowUndecoded = settingsRepository.settings.value.feedbackType.showUndecodedBarcode
        val showCheckmarks = showCheckmarksOnBarcodes
        val completedBarcodeValues: HashSet<String>? = if (showCheckmarks) {
            capturedBarcodeRepository.getCapturedBarcodeSet()
        } else {
            null
        }

        val overlays = ArrayList<BarcodeOverlayItem>(barcodeEntities.size)
        for (entity in barcodeEntities) {
            val item = createBarcodeOverlayItem(
                entity = entity,
                showCheckmarks = showCheckmarks,
                completedBarcodeValues = completedBarcodeValues,
                liveValues = liveValues,
                shouldShowUndecoded = shouldShowUndecoded
            )
            if (item != null) {
                overlays.add(item)
            }
        }

        return BarcodeProcessingResult(overlayItems = overlays, detectedCount = barcodeEntities.size)
    }

    private fun createBarcodeOverlayItem(
        entity: BarcodeEntity,
        showCheckmarks: Boolean,
        completedBarcodeValues: HashSet<String>?,
        liveValues: Set<String>?,
        shouldShowUndecoded: Boolean
    ): BarcodeOverlayItem? {
        val barcodeValue = entity.value?.trim()
        if (barcodeValue.isNullOrEmpty()) {
            return if (shouldShowUndecoded) {
                BarcodeOverlayItem(
                    bounds = RectF(entity.boundingBox),
                    icon = capturedBarcodeRepository.getUndecodedIcon(),
                    text = ""
                )
            } else {
                null
            }
        }

        // Saved in an earlier session, or decoded so far in the capture in progress
        val isCaptured = completedBarcodeValues?.contains(barcodeValue) == true ||
                liveValues?.contains(barcodeValue) == true
        return BarcodeOverlayItem(
            bounds = RectF(entity.boundingBox),
            barcodeData = barcodeValue,
            icon = if (isCaptured && showCheckmarks) capturedBarcodeRepository.getCompletedIcon() else null,
            text = ""
        )
    }
}
