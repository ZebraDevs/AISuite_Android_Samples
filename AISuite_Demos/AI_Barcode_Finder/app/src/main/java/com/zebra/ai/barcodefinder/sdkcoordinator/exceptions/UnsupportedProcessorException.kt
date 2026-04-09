// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.sdkcoordinator.exceptions

/**
 * Exception thrown when the selected processor type (CPU, GPU, DSP, etc.) 
 * is not available or supported on the current device.
 * 
 * This is a domain-level exception that abstracts away SDK-specific details.
 */
class UnsupportedProcessorException(
    message: String,
    cause: Throwable? = null
) : CoordinatorException(message, cause)
