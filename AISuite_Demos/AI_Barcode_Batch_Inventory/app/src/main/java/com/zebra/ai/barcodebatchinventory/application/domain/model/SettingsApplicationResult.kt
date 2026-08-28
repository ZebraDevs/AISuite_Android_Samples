package com.zebra.ai.barcodebatchinventory.application.domain.model

sealed class SettingsApplicationResult {
    object Idle : SettingsApplicationResult()
    object InProgress : SettingsApplicationResult()
    object Success : SettingsApplicationResult()
    data class Error(
        val exception: Exception?,
        val message: String,
        val isRecoverable: Boolean = true
    ) : SettingsApplicationResult()
}
