package com.zebra.ai.barcodebatchinventory.sdkcoordinator.exceptions

/**
 * Exception thrown when camera initialization fails.
 */
class CameraInitializationException(
    message: String,
    cause: Throwable? = null
) : CoordinatorException(message, cause)