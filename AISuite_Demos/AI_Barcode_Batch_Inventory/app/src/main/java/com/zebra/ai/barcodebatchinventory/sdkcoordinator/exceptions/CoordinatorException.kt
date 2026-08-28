package com.zebra.ai.barcodebatchinventory.sdkcoordinator.exceptions

/**
 * Base exception for coordinator-related issues.
 */
sealed class CoordinatorException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)