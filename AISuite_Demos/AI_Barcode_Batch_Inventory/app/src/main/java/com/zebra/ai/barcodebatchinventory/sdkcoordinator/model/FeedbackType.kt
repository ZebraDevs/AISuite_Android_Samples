package com.zebra.ai.barcodebatchinventory.sdkcoordinator.model

/**
 * FeedbackType holds boolean flags for each feedback type.
 */

data class FeedbackType(
    var audio: Boolean = true,
    var haptics: Boolean = false,
    var showUndecodedBarcode: Boolean = false
)