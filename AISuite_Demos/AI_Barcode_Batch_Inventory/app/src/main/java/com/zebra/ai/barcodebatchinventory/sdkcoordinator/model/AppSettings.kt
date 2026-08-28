package com.zebra.ai.barcodebatchinventory.sdkcoordinator.model

import com.zebra.ai.barcodebatchinventory.sdkcoordinator.enums.ModelInput
import com.zebra.ai.barcodebatchinventory.sdkcoordinator.enums.ProcessorType
import com.zebra.ai.barcodebatchinventory.sdkcoordinator.enums.Resolution
import com.zebra.ai.vision.detector.BarcodeDecoder

/**
 * AppSettings holds all configuration options for the barcode decoder.
 * This includes model input size, camera resolution, processor type, and barcode symbology settings.
 * Used to persist, compare, and apply user or system configuration throughout the app.
 */
data class AppSettings(
    val modelInput: ModelInput = ModelInput.SMALL_640,
    val resolution: Resolution = Resolution.TWO_MP,
    val processorType: ProcessorType = ProcessorType.AUTO,
    val barcodeSymbology: BarcodeSymbology = BarcodeSymbology(),
    val feedbackType: FeedbackType = FeedbackType(),
    /**
     * Per-barcode decode timeout in milliseconds.
     * The total capture timeout is calculated dynamically as:
     *   totalTimeout = detectedBarcodeCount × captureTimeoutPerBarcodeMs
     * Minimum total timeout is always at least captureTimeoutPerBarcodeMs (covers 1 barcode).
     * Default: 500ms per barcode.
     */
    val captureTimeoutPerBarcodeMs: Long = 500L,
    val enableAIBarcodeDecode: Boolean = true
) {
    fun isEquals(other: AppSettings): Boolean {
        return modelInput == other.modelInput &&
                resolution == other.resolution &&
                processorType == other.processorType &&
                barcodeSymbology == other.barcodeSymbology &&
                feedbackType == other.feedbackType &&
                captureTimeoutPerBarcodeMs == other.captureTimeoutPerBarcodeMs &&
                enableAIBarcodeDecode == other.enableAIBarcodeDecode
    }
}