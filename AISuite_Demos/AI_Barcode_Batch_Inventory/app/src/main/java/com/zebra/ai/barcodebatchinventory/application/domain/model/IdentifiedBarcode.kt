package com.zebra.ai.barcodebatchinventory.application.domain.model

// class to represent a barcode and its ID
data class IdentifiedBarcode(
    val id: Int,
    val value: String
)