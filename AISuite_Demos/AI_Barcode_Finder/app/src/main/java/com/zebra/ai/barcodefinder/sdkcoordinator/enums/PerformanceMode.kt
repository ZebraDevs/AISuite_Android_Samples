package com.zebra.ai.barcodefinder.sdkcoordinator.enums

/**
 * Operational performance profile for the on-device AI inferencer (DSP).
 *
 * Maps to the AI Data Capture SDK's `InferencerOptions.PerformanceMode` when applied by
 * [com.zebra.ai.barcodefinder.sdkcoordinator.support.BarcodeDecoderSettingsBuilder].
 *
 * - [BALANCED]: (Default) Optimizes for power efficiency. Recommended for continuous
 *   detection and recognition use cases.
 * - [BURST]: Maximizes processing performance ("Turbo Mode") at the expense of increased
 *   power consumption. BURST is supported on select chipsets only (e.g. the Zebra 6690
 *   family); requesting it on an unsupported device throws an `AIVisionSDKException` when the
 *   barcode decoder is initialized, which is surfaced to the user like other setting errors.
 */
enum class PerformanceMode {
    BALANCED,
    BURST
}
