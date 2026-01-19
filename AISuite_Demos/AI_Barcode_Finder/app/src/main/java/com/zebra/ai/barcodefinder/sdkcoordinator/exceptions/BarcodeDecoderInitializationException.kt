package com.zebra.ai.barcodefinder.sdkcoordinator.exceptions

/**
 * Exception thrown when barcode decoder initialization fails.
 */
class BarcodeDecoderInitializationException(
    message: String,
    cause: Throwable? = null
) : CoordinatorException(message, cause)