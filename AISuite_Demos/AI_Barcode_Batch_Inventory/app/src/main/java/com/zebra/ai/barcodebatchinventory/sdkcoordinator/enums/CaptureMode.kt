package com.zebra.ai.barcodebatchinventory.sdkcoordinator.enums

/** How a batch capture session decides when to end. */
enum class CaptureMode {
    /** Runs for the fixed duration in [com.zebra.ai.barcodebatchinventory.sdkcoordinator.model.AppSettings.captureDurationMs]. */
    FIXED,

    /** Ends as soon as the detectable barcodes are decoded, bounded by a max timeout. */
    DYNAMIC
}
