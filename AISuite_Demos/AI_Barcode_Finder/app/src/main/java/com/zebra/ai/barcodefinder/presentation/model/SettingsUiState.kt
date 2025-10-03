package com.zebra.ai.barcodefinder.presentation.model

/**
 * UI state for the settings screen.
 * @property isLoading Whether a settings update is in progress
 * @property error Error message if any
 * @property isSdkInitialized Whether the SDK is initialized
 * @property scanStarted Whether a scan has started
 * @property isScanning Whether scanning is in progress
 */
data class SettingsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSdkInitialized: Boolean = false,
    // TODO - Revisit scanStarted and isScanning logic, and remove if not needed
    val scanStarted: Boolean = false,
    val isScanning: Boolean = false
)
