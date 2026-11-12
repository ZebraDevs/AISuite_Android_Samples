package com.zebra.ai.barcodebatchinventory.application.domain.model

data class BarcodeProcessingResult(
    val overlayItems: MutableList<BarcodeOverlayItem> = mutableListOf(),
    var scanResults: List<ScanResult> = emptyList(),
    /** All barcode entities in the frame, including localized-but-undecoded ones. */
    val detectedCount: Int = 0,
)