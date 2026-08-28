package com.zebra.ai.barcodebatchinventory.application.presentation.model

import com.zebra.ai.barcodebatchinventory.application.domain.model.ScanResult

/**
 * Represents the UI state for the batch inventory scan screen.
 */
data class FinderUiState(
    val isInitialized: Boolean = false,
    val scanResults: List<ScanResult> = emptyList()
)
