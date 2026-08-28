// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.palletchecker.domain.model

import com.zebra.ai.palletchecker.domain.enums.AutoTriggerMode
import com.zebra.ai.palletchecker.domain.enums.ModelInput
import com.zebra.ai.palletchecker.domain.enums.ProcessorType
import com.zebra.ai.palletchecker.domain.enums.Resolution

/**
 * AppSettings holds all configuration options for the pallet checker app.
 * This includes model input size, camera resolution, processor type, and barcode symbology settings.
 * Used to persist, compare, and apply user or system configuration throughout the app.
 */
data class AppSettings(
    val modelInput: ModelInput = ModelInput.LARGE_1600, // AI model input size
    val resolution: Resolution = Resolution.TWELVE_MP,    // Camera resolution
    val customResolutionWidth: Int = 0,   // Actual width when resolution == MAX (device-queried)
    val customResolutionHeight: Int = 0,  // Actual height when resolution == MAX (device-queried)
    val processorType: ProcessorType = ProcessorType.AUTO, // Hardware processor type
    val barcodeSymbology: BarcodeSymbology = BarcodeSymbology(), // Barcode symbology config
    val livePipThumbnailEnabled: Boolean = true,
    val autoTriggerEnabled: Boolean = true, // true = auto-snap, false = manual capture button
    val autoTriggerMode: AutoTriggerMode = AutoTriggerMode.FIXED_QUANTITY, // criteria for auto-snap
    val fixedQuantityThreshold: Int = 2,   // used when autoTriggerMode == FIXED_QUANTITY
    val percentageThreshold: Int = 80,     // used when autoTriggerMode == PERCENTAGE_BASED
    val expectedTotalBoxes: Int = 10,      // total boxes expected (used for percentage calculation)
    val showAuditProgress: Boolean = true, // Show/hide audit progress indicator in wand mode
    val showLiveBoundingBoxes: Boolean = true, // Show/hide live bounding box overlay during wand mode
    val multiSnapEnabled: Boolean = true,       // Enable multi-snap retry to capture missing barcodes
    val multiSnapRetryCount: Int = 3,           // Number of snap attempts (1 = single snap, no retry)
    val debugSettings: DebugSettings = DebugSettings(), // Debug visualization options
    val enableAIbarcodeDecode: Boolean = true,
) {
    /**
     * Returns the effective camera width to use.
     * When resolution is MAX, returns the device-queried custom width;
     * otherwise returns the enum's predefined width.
     */
    fun effectiveWidth(): Int =
        if (resolution == Resolution.MAX && customResolutionWidth > 0) customResolutionWidth
        else resolution.width

    /**
     * Returns the effective camera height to use.
     * When resolution is MAX, returns the device-queried custom height;
     * otherwise returns the enum's predefined height.
     */
    fun effectiveHeight(): Int =
        if (resolution == Resolution.MAX && customResolutionHeight > 0) customResolutionHeight
        else resolution.height

    /**
     * Compares this AppSettings instance to another for equality of all fields.
     * Useful for detecting changes in configuration.
     */
    fun isEquals(other: AppSettings): Boolean {
        return modelInput == other.modelInput &&
                resolution == other.resolution &&
                customResolutionWidth == other.customResolutionWidth &&
                customResolutionHeight == other.customResolutionHeight &&
                processorType == other.processorType &&
                barcodeSymbology == other.barcodeSymbology &&
                livePipThumbnailEnabled == other.livePipThumbnailEnabled &&
                autoTriggerEnabled == other.autoTriggerEnabled &&
                autoTriggerMode == other.autoTriggerMode &&
                fixedQuantityThreshold == other.fixedQuantityThreshold &&
                percentageThreshold == other.percentageThreshold &&
                expectedTotalBoxes == other.expectedTotalBoxes &&
                showAuditProgress == other.showAuditProgress &&
                showLiveBoundingBoxes == other.showLiveBoundingBoxes &&
                multiSnapEnabled == other.multiSnapEnabled &&
                multiSnapRetryCount == other.multiSnapRetryCount &&
                debugSettings == other.debugSettings &&
                enableAIbarcodeDecode == other.enableAIbarcodeDecode
    }
}

/**
 * BarcodeSymbology holds boolean flags for each supported barcode type.
 * Used to enable/disable recognition of specific barcode formats in the app.
 * All fields default to recommended values for typical use cases.
 */
data class BarcodeSymbology(
    var aztec: Boolean = true,
    var codabar: Boolean = true,
    var code128: Boolean = true,
    var code39: Boolean = true,
    var ean8: Boolean = true,
    var ean13: Boolean = true,
    var gs1Databar: Boolean = true,
    var datamatrix: Boolean = true,
    var gs1DatabarExpanded: Boolean = true,
    var mailmark: Boolean = true,
    var maxicode: Boolean = true,
    var pdf417: Boolean = true,
    var qrcode: Boolean = true,
    var upcA: Boolean = true,
    var upcE: Boolean = true,
    var upcean: Boolean = true,
    var compositeAB: Boolean = false,
    var compositeC: Boolean = false,
    var i2of5: Boolean = false,
    var dotcode: Boolean = false,
    var gridMatrix: Boolean = false,
    var gs1Datamatrix: Boolean = false,
    var gs1Qrcode: Boolean = false,
    var microqr: Boolean = false,
    var micropdf: Boolean = false,
    var uspostnet: Boolean = false,
    var usplanet: Boolean = false,
    var ukPostal: Boolean = false,
    var japanesePostal: Boolean = false,
    var australianPostal: Boolean = false,
    var canadianPostal: Boolean = false,
    var dutchPostal: Boolean = false,
    var us4state: Boolean = false,
    var us4stateFics: Boolean = false,
    var msi: Boolean = false,
    var code93: Boolean = false,
    var trioptic39: Boolean = false,
    var d2of5: Boolean = false,
    var chinese2of5: Boolean = false,
    var korean3of5: Boolean = false,
    var code11: Boolean = false,
    var tlc39: Boolean = false,
    var hanxin: Boolean = false,
    var matrix2of5: Boolean = false,
    var upce1: Boolean = false,
    var gs1DatabarLim: Boolean = false,
    var finnishPostal4s: Boolean = false
)
