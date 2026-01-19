package com.zebra.ai.barcodefinder.application.domain.model

// class to represent a barcode and its ID
data class IdentifiedBarcode(
    val id: Int,
    val value: String
)