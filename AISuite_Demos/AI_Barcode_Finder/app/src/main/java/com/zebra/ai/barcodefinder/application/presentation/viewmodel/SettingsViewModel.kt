// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.application.presentation.viewmodel

import com.zebra.ai.barcodefinder.sdkcoordinator.model.BarcodeSymbology
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zebra.ai.barcodefinder.application.data.source.repository.SettingsRepository
import com.zebra.ai.barcodefinder.application.data.source.storage.SettingsJsonStorage
import com.zebra.ai.barcodefinder.application.domain.model.SettingsApplicationResult
import com.zebra.ai.barcodefinder.sdkcoordinator.enums.ModelInput
import com.zebra.ai.barcodefinder.sdkcoordinator.enums.ProcessorType
import com.zebra.ai.barcodefinder.sdkcoordinator.enums.Resolution
import com.zebra.ai.barcodefinder.sdkcoordinator.model.AppSettings
import com.zebra.ai.barcodefinder.application.domain.usecase.SettingsUseCase
import com.zebra.ai.barcodefinder.sdkcoordinator.EntityTrackerCoordinator
import com.zebra.ai.barcodefinder.sdkcoordinator.model.FeedbackType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
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
    private val TAG = "SettingsViewModel"

    //TODO : Need to replace by Proper DI
    val settingsJsonStorage = SettingsJsonStorage(application)
    val settingsRepository = SettingsRepository.getInstance(settingsJsonStorage)
    private val entityTrackerCoordinator = EntityTrackerCoordinator.getInstance(application)

    val settingsUseCase = SettingsUseCase(entityTrackerCoordinator, settingsRepository)

    /** Current app settings from the repository. */
    val settings: StateFlow<AppSettings> = settingsUseCase.observeSettings()

    /** Draft settings edited by the user in the Settings screen (not persisted until Apply succeeds). */
    private val _draftSettings = MutableStateFlow(settings.value)
    val draftSettings: StateFlow<AppSettings> = _draftSettings.asStateFlow()

    /** Tracks the state of applying settings to SDK */
    private val _applicationState = MutableStateFlow<SettingsApplicationResult>(
        SettingsApplicationResult.Idle
    )
    val applicationState: StateFlow<SettingsApplicationResult> = _applicationState.asStateFlow()

    // Guard against concurrent SDK reconfiguration (e.g. rapid back-button taps or
    // system-back gestures fired before the BackHandler's enabled=false takes effect).
    // Unlike HomeViewModel, applyDraftSettingsToCoordinator() is a suspend function that
    // awaits the terminal state, so the flag is cleared in a finally block inside the
    // coroutine rather than in the coordinator-state observer.
    private var isApplyingSettings = false

    /**
     * Single-shot navigation events. The ViewModel emits [NavigationEvent.NavigateBack] only
     * after the SDK confirms successful configuration, so the screen defers navigation
     * until the result is known.
     */
    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    sealed class NavigationEvent {
        object NavigateBack : NavigationEvent()
    }

    /**
     * Updates draft settings using the provided update function.
     * Clears error state as soon as the user changes a setting.
     */
    private fun updateDraftSettingsWith(update: (AppSettings) -> AppSettings) {
        if (_applicationState.value is SettingsApplicationResult.Error) {
            resetApplicationState()
        }
        _draftSettings.update(update)
    }

    /**
     * Updates the model input size and syncs with the AI Vision SDK.
     */
    fun updateModelInput(modelInput: ModelInput) {
        updateDraftSettingsWith { it.copy(modelInput = modelInput) }
    }

    /**
     * Updates the camera resolution and syncs with the AI Vision SDK.
     */
    fun updateResolution(resolution: Resolution) {
        updateDraftSettingsWith { it.copy(resolution = resolution) }
    }

    /**
     * Updates the processor type and syncs with the AI Vision SDK.
     */
    fun updateProcessorType(processorType: ProcessorType) {
        updateDraftSettingsWith { it.copy(processorType = processorType) }
    }

    /**
     * Updates the barcode symbology and syncs with the AI Vision SDK.
     */
    fun updateBarcodeSymbology(symbology: BarcodeSymbology) {
        updateDraftSettingsWith { it.copy(barcodeSymbology = symbology) }
    }

    fun updateFeedbackType(feedbackType: FeedbackType) {
        updateDraftSettingsWith { it.copy(feedbackType = feedbackType) }
    }

    /**
     * Updates an individual symbology setting by name for UI convenience.
     * @param symbologyName The name of the symbology
     * @param enabled Whether the symbology is enabled
     */
    fun updateSymbology(symbologyName: String, enabled: Boolean) {
        val updatedSymbology = settingsUseCase.updateSymbology(
            currentSymbology = _draftSettings.value.barcodeSymbology,
            symbologyName = symbologyName,
            enabled = enabled
        )
        updateDraftSettingsWith { it.copy(barcodeSymbology = updatedSymbology) }
    }

    /**
     * Resets all settings to their default values and syncs with the AI Vision SDK.
     */
    fun resetToDefaults() {
        updateDraftSettingsWith { AppSettings() }
    }

    /**
     * Applies the draft settings to the SDK coordinator.
     *
     * Navigation is deferred: the screen stays visible while the SDK reconfigures
     * (showing a loading overlay). On success the ViewModel emits [NavigationEvent.NavigateBack]
     * so the UI navigates only after settings are committed. On error the screen remains
     * open so the [ErrorBanner] is visible and the user can adjust settings and retry.
     *
     * Draft settings are NOT persisted before calling configureSdk — the repository
     * retains the last successfully applied (known-compatible) settings. This ensures
     * that if the ViewModel scope dies mid-flight, reentry uses safe settings.
     */
    fun applySettingsToSDK() {
        if (isApplyingSettings) {
            Log.d(TAG, "applySettingsToSDK() ignored — reconfiguration already in progress")
            return
        }
        isApplyingSettings = true
        viewModelScope.launch {
            try {
                _applicationState.value = SettingsApplicationResult.InProgress

                val result = settingsUseCase.applyDraftSettingsToCoordinator(_draftSettings.value)
                _applicationState.value = result

                when (result) {
                    is SettingsApplicationResult.Success -> {
                        Log.d(TAG, "Settings applied successfully")
                        // Keep draft in sync with the now-committed settings.
                        _draftSettings.value = settings.value
                        // Navigate back — ViewModel is the single source of truth for when
                        // it is safe to leave the Settings screen.
                        _navigationEvents.send(NavigationEvent.NavigateBack)
                    }
                    is SettingsApplicationResult.Error -> {
                        Log.e(TAG, "Failed to apply settings: ${result.message}", result.exception)
                        // Roll back draft to last known-good so the UI shows safe values.
                        // Stay on screen — ErrorBanner will be visible for the user to react.
                        _draftSettings.value = settings.value
                    }
                    else -> { /* Idle or InProgress */ }
                }
            } finally {
                // applyDraftSettingsToCoordinator() is a suspend function that awaits the
                // terminal coordinator state, so the pipeline is fully complete by the time
                // we reach here — safe to release the guard regardless of outcome.
                isApplyingSettings = false
            }
        }
    }

    /**
     * Resets the application state to Idle.
     * Call this after handling errors in the UI.
     */

    fun resetApplicationState() {
        _applicationState.value = SettingsApplicationResult.Idle
    }

}

