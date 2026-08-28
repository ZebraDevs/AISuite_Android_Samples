package com.zebra.ai.palletchecker.presentation.model

import com.zebra.ai.palletchecker.domain.model.BarcodeConfig

data class ConfigUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val listConfig: List<BarcodeConfig> = emptyList(),
    val expandedList: List<String> = emptyList()
)
