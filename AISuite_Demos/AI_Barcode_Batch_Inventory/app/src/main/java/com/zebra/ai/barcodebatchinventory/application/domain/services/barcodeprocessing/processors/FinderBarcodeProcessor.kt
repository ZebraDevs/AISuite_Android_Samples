package com.zebra.ai.barcodebatchinventory.application.domain.services.barcodeprocessing.processors

import android.graphics.RectF
import android.util.Log
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

    private val PERF_TAG = "AppPerfMon"
    private var processFrameCount = 0L

    @Volatile
    var showCheckmarksOnBarcodes = false

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
        val startNs = System.nanoTime()
        val frameNum = ++processFrameCount
        val processingThread = Thread.currentThread().name

        val barcodeEntities = entities.filterIsInstance<BarcodeEntity>()
        detectedBarcodeCount = barcodeEntities.size

        val decodedEntities = ArrayList<BarcodeEntity>(barcodeEntities.size)
        for (entity in barcodeEntities) {
            if (!entity.value.isNullOrEmpty()) {
                decodedEntities.add(entity)
            }
        }
        decodedBarcodeCount = decodedEntities.size

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
        var skippedUndecoded = 0
        for (entity in barcodeEntities) {
            val item = createBarcodeOverlayItem(
                entity = entity,
                showCheckmarks = showCheckmarks,
                completedBarcodeValues = completedBarcodeValues,
                shouldShowUndecoded = shouldShowUndecoded
            )
            if (item != null) {
                overlays.add(item)
            } else if (entity.value.isNullOrEmpty()) {
                skippedUndecoded++
            }
        }

        val durationUs = (System.nanoTime() - startNs) / 1000
        Log.i(PERF_TAG, "Process: app=BatchInventory" +
                " frame#=$frameNum" +
                " inputEntities=${entities.size}" +
                " barcodeEntities=${barcodeEntities.size}" +
                " decoded=${decodedEntities.size}" +
                " overlaysProduced=${overlays.size}" +
                " skippedUndecoded=$skippedUndecoded" +
                " showUndecoded=$shouldShowUndecoded" +
                " showCheckmarks=$showCheckmarks" +
                " completedCount=${completedBarcodeValues?.size ?: 0}" +
                " processDur=${durationUs}us" +
                " thread=$processingThread")

        return BarcodeProcessingResult(overlayItems = overlays)
    }

    private fun createBarcodeOverlayItem(
        entity: BarcodeEntity,
        showCheckmarks: Boolean,
        completedBarcodeValues: HashSet<String>?,
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

        val isCaptured = completedBarcodeValues?.contains(barcodeValue) == true
        return BarcodeOverlayItem(
            bounds = RectF(entity.boundingBox),
            barcodeData = barcodeValue,
            icon = if (isCaptured && showCheckmarks) capturedBarcodeRepository.getCompletedIcon() else null,
            text = ""
        )
    }
}
