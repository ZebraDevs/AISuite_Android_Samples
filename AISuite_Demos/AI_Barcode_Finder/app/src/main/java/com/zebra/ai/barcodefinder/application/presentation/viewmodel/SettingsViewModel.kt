// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.application.presentation.viewmodel

import com.zebra.ai.barcodefinder.sdkcoordinator.model.BarcodeSymbology
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zebra.ai.barcodefinder.application.data.source.repository.SettingsRepository
import com.zebra.ai.barcodefinder.application.data.source.storage.SettingsJsonStorage
import com.zebra.ai.barcodefinder.sdkcoordinator.enums.ModelInput
import com.zebra.ai.barcodefinder.sdkcoordinator.enums.ProcessorType
import com.zebra.ai.barcodefinder.sdkcoordinator.enums.Resolution
import com.zebra.ai.barcodefinder.sdkcoordinator.model.AppSettings
import com.zebra.ai.barcodefinder.application.domain.usecase.SettingsUseCase
import com.zebra.ai.barcodefinder.sdkcoordinator.EntityTrackerCoordinator
import com.zebra.ai.barcodefinder.sdkcoordinator.model.FeedbackType
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing app settings and scan state.
 * Handles updating model input, resolution, processor type, barcode symbology,
 * and scan state, syncing changes with the EntityTrackerFacade.
 *
 * @constructor Creates a SettingsViewModel with the given application context.
 * @param application The application context
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    //TODO : Need to replace by Proper DI
    val settingsJsonStorage = SettingsJsonStorage(application)
    val settingsRepository = SettingsRepository.getInstance(settingsJsonStorage)
    private val entityTrackerCoordinator = EntityTrackerCoordinator.getInstance(application)

    val settingsUseCase = SettingsUseCase(entityTrackerCoordinator, settingsRepository)

    /** Current app settings from the repository. */
    val settings: StateFlow<AppSettings> = settingsUseCase.observeSettings()

    /**
     * Updates settings using the provided update function and syncs with repository.
     * @param update Function to update AppSettings
     */
    private fun updateSettingsWith(update: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            try {
                settingsUseCase.updateSettings(update)
            } finally {
            }
        }
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

    fun updateFeedbackType(feedbackType: FeedbackType) {
        updateSettingsWith { it.copy(feedbackType = feedbackType) }
    }

    /**
     * Updates an individual symbology setting by name for UI convenience.
     * @param symbologyName The name of the symbology
     * @param enabled Whether the symbology is enabled
     */
    fun updateSymbology(symbologyName: String, enabled: Boolean) {
        val updatedSymbology = settingsUseCase.updateSymbology(symbologyName,enabled)
        updateBarcodeSymbology(updatedSymbology)
    }

    /**
     * Resets all settings to their default values and syncs with the AI Vision SDK.
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            try {
                settingsUseCase.resetSettings()
            } finally {
            }
        }
    }

    /**
     * Applying the updated settings to the EntityTracker Coordinator
     */
    fun applySettingsToSDK() {
        settingsUseCase.updateEntityTrackerCoordinatorWithNewSettings()
    }

}

