package com.zebra.ai.barcodefinder.sdkcoordinator.exceptions

/**
 * Exception thrown when entity tracker initialization fails.
 */
class EntityTrackerInitializationException(
    message: String,
    cause: Throwable? = null
) : CoordinatorException(message, cause)