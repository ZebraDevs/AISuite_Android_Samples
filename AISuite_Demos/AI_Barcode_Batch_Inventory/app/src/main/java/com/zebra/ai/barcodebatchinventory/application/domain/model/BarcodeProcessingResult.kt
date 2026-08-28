package com.zebra.ai.barcodebatchinventory.application.domain.model

data class BarcodeProcessingResult(
    val overlayItems: MutableList<BarcodeOverlayItem> = mutableListOf(),
    var scanResults: List<ScanResult> = emptyList(),
)