package com.zebra.ai.barcodefinder.sdkcoordinator.enums

enum class CoordinatorState {
    NOT_INITIALIZED,
    CONFIGURING,
    AI_VISION_SDK_INITIALIZED,
    BARCODE_DECODER_SETTINGS_INITIALIZED,
    BARCODE_DECODER_INITIALIZED,
    ENTITY_TRACKER_INITIALIZED,
    CAMERA_PERMISSION_REQUIRED,
    CAMERA_PERMISSION_RECEIVED,
    CAMERA_PERMISSION_DENIED,
    CAMERA_INITIALIZED,
    COORDINATOR_READY,

    // Error states - each maps to a specific domain error in the UseCase layer
    ERROR_AI_VISION_SDK,
    ERROR_BARCODE_DECODER_SETTINGS,
    ERROR_UNSUPPORTED_PROCESSOR,
    ERROR_BARCODE_DECODER,
    ERROR_ENTITY_TRACKER,
    ERROR_CAMERA,
    ERROR_SDK,
    ERROR_DISPOSE;

    /**
     * Returns true if this state is a terminal outcome (success or actionable error).
     * Observers use this to know when to stop waiting for a result.
     */
    fun isTerminal(): Boolean = when (this) {
        COORDINATOR_READY,
        ERROR_AI_VISION_SDK,
        ERROR_BARCODE_DECODER_SETTINGS,
        ERROR_UNSUPPORTED_PROCESSOR,
        ERROR_BARCODE_DECODER,
        ERROR_ENTITY_TRACKER,
        ERROR_CAMERA,
        ERROR_SDK,
        ERROR_DISPOSE -> true
        else -> false
    }
}