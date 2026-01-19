package com.zebra.ai.barcodefinder.sdkcoordinator.exceptions

/**
 * Base exception for coordinator-related issues.
 */
sealed class CoordinatorException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)