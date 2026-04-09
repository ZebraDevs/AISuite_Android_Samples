// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.application.domain.model

/**
 * Represents the result of applying settings to the SDK coordinator.
 * Used to communicate success, progress, or failure states to the presentation layer.
 */
sealed class SettingsApplicationResult {
    /** Settings have not been applied yet or state has been reset */
    object Idle : SettingsApplicationResult()
    
    /** Settings are currently being applied to SDK */
    object InProgress : SettingsApplicationResult()
    
    /** Settings were successfully applied */
    object Success : SettingsApplicationResult()
    
    /** Settings application failed with an exception */
    data class Error(
        val exception: Exception?,
        val message: String,
        val isRecoverable: Boolean = true
    ) : SettingsApplicationResult()
}
