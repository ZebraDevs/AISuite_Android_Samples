// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodefinder.data.model

/**
 * Data class representing the result of a barcode scan operation in the app.
 * Contains product information, the scanned barcode, scan status, and any additional info.
 * Used to pass scan results between components, display results to the user, or store scan history.
 *
 * @property productName The name of the product associated with the scanned barcode.
 * @property barcode The barcode string that was scanned.
 * @property status The status of the scan (see ScanStatus enum for details).
 * @property additionalInfo Optional field for extra information about the scan result.
 */
data class ScanResult(
    val productName: String,
    val barcode: String,
    val status: ScanStatus,
    val additionalInfo: String? = null
)
