package com.zebra.ai.barcodefinder.application.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.zebra.ai.barcodefinder.application.data.source.repository.SettingsRepository
import com.zebra.ai.barcodefinder.application.data.source.storage.SettingsJsonStorage
import com.zebra.ai.barcodefinder.application.domain.usecase.HomeUseCase
import com.zebra.ai.barcodefinder.sdkcoordinator.EntityTrackerCoordinator
import com.zebra.ai.barcodefinder.sdkcoordinator.model.AppSettings
import com.zebra.ai.barcodefinder.sdkcoordinator.support.PermissionHandler
import com.zebra.ai.barcodefinder.application.presentation.model.EntityTrackerInitState
import com.zebra.ai.barcodefinder.sdkcoordinator.enums.CoordinatorState
import com.zebra.ai.barcodefinder.sdkcoordinator.enums.ProcessorType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "HomeViewModel"
    private val entityTrackerCoordinator = EntityTrackerCoordinator.getInstance(application)

    //TODO need to handle this with DI
    private val settingsRepository = SettingsRepository.getInstance(SettingsJsonStorage(application))

    private val homeUseCase = HomeUseCase(entityTrackerCoordinator, settingsRepository)

    private val _entityTrackerInitState = MutableStateFlow<EntityTrackerInitState>(EntityTrackerInitState())
    val entityTrackerInitState: StateFlow<EntityTrackerInitState> = _entityTrackerInitState.asStateFlow()

    private val _settings = MutableStateFlow<AppSettings>(AppSettings() )
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    // State to hold whether the camera permission is denied. The homeScreen will observe this.
    var cameraPermissionDenied by mutableStateOf(true)
        private set // Only ViewModel can change this state

    // Flag to ensure the coordinator is only initialized once.
    private var isCoordinatorInitialized = false

    // Guard against concurrent SDK reconfiguration (e.g. rapid button taps).
    // Set synchronously before the coroutine launches so the UI disables immediately.
    private var isApplyingSettings = false

    init {
        try {
            observerEntityTrackerInitState()
            observerSettings()
            checkCameraPermission()
            try {
                initializeCoordinatorIfCameraPermissionGranted() // Try to initialize EntityTrackerCoordinator immediately if permission is already granted.
            } catch (e: Exception){
                Log.e(TAG, "Error during ViewModel initialization", e)
                updateProcessorTypeToAuto()
            }
        } catch (e: Exception) {
            throw e
        }
    }

    override fun onCleared() {
        super.onCleared()
        homeUseCase.disposeEntityTrackerCoordinatorResources()
    }

    fun checkCameraPermission() {
        cameraPermissionDenied = !hasCameraPermission()
    }

    private fun hasCameraPermission(): Boolean {
        return PermissionHandler.checkCameraPermission(application) == CoordinatorState.CAMERA_PERMISSION_RECEIVED
    }

    /**
     * A public method to update the state based on the user's response
     * to the permission dialog.
     * @param isGranted True if the user granted the permission.
     */
    fun onPermissionResult(isGranted: Boolean) {
        cameraPermissionDenied = !isGranted
        if (isGranted) {
            try {
                // If camera permission was just granted, try to initialize.
                initializeCoordinatorIfCameraPermissionGranted()
            } catch (_: Exception) {
                updateProcessorTypeToAuto()
            }
        }
    }

    /**
     * Initializes the coordinator only if camera permission is granted and it hasn't
     * been initialized before.
     */
    private fun initializeCoordinatorIfCameraPermissionGranted() {
        if (hasCameraPermission() && !isCoordinatorInitialized) {
            viewModelScope.launch {
                homeUseCase.initializeEntityTrackerCoordinator()
                isCoordinatorInitialized = true
            }
        }
    }

    /**
     * Apply the settings to the SDK and reinitialize SDK.
     *
     * Sets isInitialized=false synchronously before launching the coroutine so the
     * UI disables on the same frame as the tap, preventing a second tap from
     * sneaking through before Compose recomposes.
     *
     * isApplyingSettings is NOT cleared here — configureSdk() returns as soon as
     * it creates the CompletableFuture, well before the async pipeline finishes.
     * The flag is cleared in observerEntityTrackerInitState() when coordinatorState
     * reaches a terminal value, which marks the true end of reconfiguration.
     */
    fun applySettingsToSDK() {
        if (isApplyingSettings) {
            Log.d(TAG, "applySettingsToSDK() ignored — reconfiguration already in progress")
            return
        }
        isApplyingSettings = true
        // Disable the button synchronously on this frame, before the coroutine
        // is scheduled, to prevent a second tap sneaking through.
        _entityTrackerInitState.update { it.copy(isInitialized = false) }
        viewModelScope.launch {
            try {
                homeUseCase.updateEntityTrackerCoordinator()
            } catch (e: Exception) {
                Log.e(TAG, "applySettingsToSDK() failed to trigger reconfiguration", e)
                // isApplyingSettings will be cleared by the coordinatorState observer
                // reacting to the terminal error state emitted by the coordinator.
            }
        }
    }

    /**
     * Updates settings using the provided update function and syncs with repository.
     * @param update Function to update AppSettings
     */
    private fun updateSettingsWith(update: (AppSettings) -> AppSettings) {
        homeUseCase.updateSettings(update)
    }

    /**
     * Update the Processor type to AUTO in application settings
     */
    private fun updateProcessorTypeToAuto() {
        if(homeUseCase.observeSettings().value.processorType == ProcessorType.DSP) {
            Log.d(TAG, "Processor type is DSP, switching to AUTO")
            updateSettingsWith { it.copy(processorType = ProcessorType.AUTO) }
        } else {
            Log.e(TAG, "Entity Tracker can not be initialize even after updating the Processor type to AUTO")
        }
    }

    /**
     * Resets all settings to their default values and syncs with the AI Vision SDK
     */
    fun resetToDefaultSettings() {
        settingsRepository.resetSettings()
        Log.d(TAG, "Reset settings to default settings")
    }

    /**
     * Observes the EntityTrackerCoordinator state and updates EntityTrackerInitState.
     *
     * This is also the single point where isApplyingSettings is cleared, because
     * terminal coordinator states mark the true end of the async CompletableFuture
     * pipeline — not the return of configureSdk().
     */
    fun observerEntityTrackerInitState() {
        homeUseCase.observeEntityTrackerCoordinatorState()
            .map { coordinatorState ->
                if (coordinatorState == CoordinatorState.COORDINATOR_READY) {
                    isApplyingSettings = false
                    _entityTrackerInitState.update { it.copy(isInitialized = true) }
                    Log.d(TAG, "Entity tracker init state is updated to true")
                } else if (coordinatorState.isTerminal()) {
                    // Terminal error state: pipeline done (failed), release the guard
                    // so the user can retry. isInitialized stays false.
                    isApplyingSettings = false
                    _entityTrackerInitState.update { it.copy(isInitialized = false) }
                    Log.d(TAG, "Entity tracker reached terminal error state: $coordinatorState")
                } else {
                    _entityTrackerInitState.update { it.copy(isInitialized = false) }
                    Log.d(TAG, "Entity tracker init state is updated to false, coordinatorState=$coordinatorState")
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Observing the settings in settings repository to reflect the updates in settings in the homeScreen
     */
    fun observerSettings() {
        viewModelScope.launch {
            settingsRepository.settings
                .collect { settingsInRepository ->
                    _settings.update { settingsInRepository }
            }
        }
    }

}