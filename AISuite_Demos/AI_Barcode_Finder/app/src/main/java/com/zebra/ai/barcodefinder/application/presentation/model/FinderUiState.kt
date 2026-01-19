package com.zebra.ai.barcodefinder.application.presentation.model

import com.zebra.ai.barcodefinder.application.domain.model.ActionableBarcode
import com.zebra.ai.barcodefinder.application.domain.model.ScanResult

/**
 * Represents the UI state for the Finder screen
 */
data class FinderUiState(
    val isInitialized: Boolean = false,
    val selectedBarcode: ActionableBarcode? = null,
    val showDialog: Boolean = false,
    val scanResults: List<ScanResult> = emptyList()
)