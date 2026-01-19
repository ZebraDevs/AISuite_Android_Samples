package com.zebra.ai.barcodefinder.application.domain.model

data class BarcodeProcessingResult(
    val overlayItems: MutableList<BarcodeOverlayItem> = mutableListOf(),
    var scanResults: List<ScanResult> = emptyList(),
)