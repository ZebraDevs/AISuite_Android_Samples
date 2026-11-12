package com.zebra.ai.barcodebatchinventory.sdkcoordinator.model

import com.zebra.ai.barcodebatchinventory.sdkcoordinator.enums.CaptureMode
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
    val modelInput: ModelInput = ModelInput.LARGE_1600,
    /** Width in pixels used when [modelInput] is [ModelInput.CUSTOM]. */
    val customModelWidth: Int = DEFAULT_CUSTOM_MODEL_SIZE,
    /** Height in pixels used when [modelInput] is [ModelInput.CUSTOM]. */
    val customModelHeight: Int = DEFAULT_CUSTOM_MODEL_SIZE,
    val resolution: Resolution = Resolution.EIGHT_MP,
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
    val enableAIBarcodeDecode: Boolean = true,
    /**
     * Fixed duration of one batch capture session in milliseconds.
     * Allowed values: [CAPTURE_DURATION_OPTIONS_MS]. Default: 3 seconds.
     */
    val captureDurationMs: Long = DEFAULT_CAPTURE_DURATION_MS,
    /**
     * [CaptureMode.FIXED] uses [captureDurationMs]; [CaptureMode.DYNAMIC] ends the session
     * when the detectable barcodes are decoded, bounded by [DYNAMIC_CAPTURE_MAX_TIMEOUT_MS].
     */
    val captureMode: CaptureMode = CaptureMode.DYNAMIC,
    /**
     * DSP performance profile. false = BALANCED (default, power efficient),
     * true = BURST (max performance, higher power use, select devices only).
     */
    val enableBurstMode: Boolean = false
) {
    companion object {
        const val DEFAULT_CAPTURE_DURATION_MS = 3000L
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
        const val DYNAMIC_CAPTURE_MAX_TIMEOUT_MS = 3000L
        val CAPTURE_DURATION_OPTIONS_MS = listOf(1000L, 3000L, 5000L)
    }

    fun isEquals(other: AppSettings): Boolean {
        return modelInput == other.modelInput &&
                customModelWidth == other.customModelWidth &&
                customModelHeight == other.customModelHeight &&
                resolution == other.resolution &&
                processorType == other.processorType &&
                barcodeSymbology == other.barcodeSymbology &&
                feedbackType == other.feedbackType &&
                captureTimeoutPerBarcodeMs == other.captureTimeoutPerBarcodeMs &&
                enableAIBarcodeDecode == other.enableAIBarcodeDecode &&
                captureDurationMs == other.captureDurationMs &&
                captureMode == other.captureMode &&
                enableBurstMode == other.enableBurstMode
    }
}