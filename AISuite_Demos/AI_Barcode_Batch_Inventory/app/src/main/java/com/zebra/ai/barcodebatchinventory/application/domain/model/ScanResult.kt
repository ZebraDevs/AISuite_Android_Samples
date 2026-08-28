// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodebatchinventory.application.domain.model

/**
 * A batch inventory result for one unique barcode value.
 */
data class ScanResult(
    val barcode: String,
    val quantity: Int = 1
)
