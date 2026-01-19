package com.zebra.ai.barcodefinder.sdkcoordinator.model

import com.zebra.ai.barcodefinder.sdkcoordinator.enums.ModelInput
import com.zebra.ai.barcodefinder.sdkcoordinator.enums.ProcessorType
import com.zebra.ai.barcodefinder.sdkcoordinator.enums.Resolution

/**
 * AppSettings holds all configuration options for the barcode .
 * This includes model input size, camera resolution, processor type, and barcode symbology settings.
 * Used to persist, compare, and apply user or system configuration throughout the app.
 */
data class AppSettings(
    val modelInput: ModelInput = ModelInput.SMALL_640, // AI model input size
    val resolution: Resolution = Resolution.TWO_MP,    // Camera resolution
    val processorType: ProcessorType = ProcessorType.AUTO, // Hardware processor type
    val barcodeSymbology: BarcodeSymbology = BarcodeSymbology(), // Barcode symbology config
    val feedbackType: FeedbackType = FeedbackType()
) {
    /**
     * Compares this AppSettings instance to another for equality of all fields.
     * Useful for detecting changes in configuration.
     */
    fun isEquals(other: AppSettings): Boolean {
        return modelInput == other.modelInput &&
                resolution == other.resolution &&
                processorType == other.processorType &&
                barcodeSymbology == other.barcodeSymbology &&
                feedbackType == other.feedbackType
    }
}