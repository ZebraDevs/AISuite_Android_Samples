// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.zebra.ai.barcodefinder.domain.enums.ModelInput
import com.zebra.ai.barcodefinder.domain.enums.ProcessorType
import com.zebra.ai.barcodefinder.domain.enums.Resolution
import com.zebra.ai.barcodefinder.domain.model.AppSettings
import com.zebra.ai.barcodefinder.domain.model.BarcodeSymbology
import com.zebra.ai.barcodefinder.presentation.model.SettingsUiState
import com.zebra.ai.barcodefinder.domain.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for managing app settings and scan state.
 * Handles updating model input, resolution, processor type, barcode symbology,
 * and scan state, syncing changes with the EntityTrackerFacade.
 *
 * @constructor Creates a SettingsViewModel with the given application context.
 * @param application The application context
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsUseCase = Settings(application)

    /** UI state for the settings screen. */
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /** Current app settings from the repository. */
    val settings: StateFlow<AppSettings> = settingsUseCase.getSettings()

    /**
     * Updates settings using the provided update function and syncs with repository.
     * @param update Function to update AppSettings
     */
    private fun updateSettingsWith(update: (AppSettings) -> AppSettings) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        settingsUseCase.updateSettings(update)
        _uiState.value = _uiState.value.copy(isLoading = false)
    }

    /**
     * Updates the model input size and syncs with the AI Vision SDK.
     */
    fun updateModelInput(modelInput: ModelInput) {
        updateSettingsWith { it.copy(modelInput = modelInput) }
    }

    /**
     * Updates the camera resolution and syncs with the AI Vision SDK.
     */
    fun updateResolution(resolution: Resolution) {
        updateSettingsWith { it.copy(resolution = resolution) }
    }

    /**
     * Updates the processor type and syncs with the AI Vision SDK.
     */
    fun updateProcessorType(processorType: ProcessorType) {
        updateSettingsWith { it.copy(processorType = processorType) }
    }

    /**
     * Updates the barcode symbology and syncs with the AI Vision SDK.
     */
    fun updateBarcodeSymbology(symbology: BarcodeSymbology) {
        updateSettingsWith { it.copy(barcodeSymbology = symbology) }
    }

    /**
     * Updates an individual symbology setting by name for UI convenience.
     * @param symbologyName The name of the symbology
     * @param enabled Whether the symbology is enabled
     */
    fun updateSymbology(symbologyName: String, enabled: Boolean) {
        val updatedSymbology = settingsUseCase.updateSymbology(symbologyName, enabled)
        updateBarcodeSymbology(updatedSymbology)
    }

    /**
     * Resets all settings to their default values and syncs with the AI Vision SDK.
     */
    fun resetToDefaults() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        settingsUseCase.resetToDefaults()
        _uiState.value = _uiState.value.copy(isLoading = false)
    }

    /**
     * Resets the scan started state.
     */
    fun resetScanStarted() {
        _uiState.value = _uiState.value.copy(scanStarted = false)
    }

    /**
     * Starts a scan by setting the scan started flag.
     */
    fun startScan() {
        _uiState.value = _uiState.value.copy(scanStarted = true)
    }

    /**
     * Handles the start scan action (alias for startScan for UI compatibility).
     */
    fun onStartScan() {
        startScan()
    }

}

