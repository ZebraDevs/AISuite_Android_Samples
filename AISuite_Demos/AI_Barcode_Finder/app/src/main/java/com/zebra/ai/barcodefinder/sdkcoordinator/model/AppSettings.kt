package com.zebra.ai.barcodefinder.sdkcoordinator.model

import com.zebra.ai.barcodefinder.sdkcoordinator.enums.DpmMode
import com.zebra.ai.barcodefinder.sdkcoordinator.enums.ModelInput
import com.zebra.ai.barcodefinder.sdkcoordinator.enums.PerformanceMode
import com.zebra.ai.barcodefinder.sdkcoordinator.enums.ProcessorType
import com.zebra.ai.barcodefinder.sdkcoordinator.enums.Resolution

/**
 * AppSettings holds all configuration options for the barcode .
 * This includes model input size, camera resolution, processor type, and barcode symbology settings.
 * Used to persist, compare, and apply user or system configuration throughout the app.
 */
data class AppSettings(
    val modelInput: ModelInput = ModelInput.SMALL, // AI model input size
    val customModelWidth: Int = DEFAULT_CUSTOM_MODEL_SIZE,  // Exact input width (px) when modelInput == CUSTOM
    val customModelHeight: Int = DEFAULT_CUSTOM_MODEL_SIZE, // Exact input height (px) when modelInput == CUSTOM
    val resolution: Resolution = Resolution.TWO_MP,    // Camera resolution
    val processorType: ProcessorType = ProcessorType.AUTO, // Hardware processor type
    val enableAIBarcodeDecode: Boolean = true, // Enable AI-powered barcode detection and decoding pipeline
    val dpmMode: DpmMode = DpmMode.DISABLED, // Direct Part Mark (DPM) decoding mode
    val performanceMode: PerformanceMode = PerformanceMode.BALANCED, // Inferencer performance profile (Turbo = BURST)
    val barcodeSymbology: BarcodeSymbology = BarcodeSymbology(), // Barcode symbology config
    val feedbackType: FeedbackType = FeedbackType()
) {
    companion object {
        const val DEFAULT_CUSTOM_MODEL_SIZE = 2560
        const val CUSTOM_MODEL_SIZE_MIN = 640
        const val CUSTOM_MODEL_SIZE_MAX = 2560
        const val CUSTOM_MODEL_SIZE_STEP = 32

        /**
         * A custom dimension must lie within [CUSTOM_MODEL_SIZE_MIN]..[CUSTOM_MODEL_SIZE_MAX]
         * and be a multiple of [CUSTOM_MODEL_SIZE_STEP].
         */
        fun isValidCustomModelSize(size: Int): Boolean =
            size in CUSTOM_MODEL_SIZE_MIN..CUSTOM_MODEL_SIZE_MAX &&
                    size % CUSTOM_MODEL_SIZE_STEP == 0
    }

    /**
     * Compares this AppSettings instance to another for equality of all fields.
     * Useful for detecting changes in configuration.
     */
    fun isEquals(other: AppSettings): Boolean {
        return modelInput == other.modelInput &&
                customModelWidth == other.customModelWidth &&
                customModelHeight == other.customModelHeight &&
                resolution == other.resolution &&
                processorType == other.processorType &&
                enableAIBarcodeDecode == other.enableAIBarcodeDecode &&
                dpmMode == other.dpmMode &&
                performanceMode == other.performanceMode &&
                barcodeSymbology == other.barcodeSymbology &&
                feedbackType == other.feedbackType
    }
}