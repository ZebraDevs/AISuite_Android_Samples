package com.zebra.ai.barcodefinder.sdkcoordinator.model

/**
 * FeedbackType holds boolean flags for each feedback type.
 */

data class FeedbackType(
    var audio: Boolean = false,
    var haptics: Boolean = false,
    var showUndecodedBarcode: Boolean = false
)