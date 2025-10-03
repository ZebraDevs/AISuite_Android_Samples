package com.zebra.ai.barcodefinder.presentation.model

import androidx.camera.core.Preview
import com.zebra.ai.barcodefinder.domain.model.ActionableBarcode
import com.zebra.ai.barcodefinder.domain.model.ScanResult

/**
 * Represents the UI state for the EntityTracker screen
 */
data class EntityTrackerUiState(
    val isInitialized: Boolean = false,
    val preview: Preview? = null,
    val selectedBarcode: ActionableBarcode? = null,
    val showDialog: Boolean = false,
    val scanResults: List<ScanResult> = emptyList()
)