package com.zebra.ai.barcodefinder.sdkcoordinator.exceptions

/**
 * Exception thrown when SDK initialization fails.
 */
class SDKInitializationException(
    message: String,
    cause: Throwable? = null
) : CoordinatorException(message, cause)