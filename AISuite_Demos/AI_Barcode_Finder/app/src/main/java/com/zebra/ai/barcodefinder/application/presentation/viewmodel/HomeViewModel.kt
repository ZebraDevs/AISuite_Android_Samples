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
            homeUseCase.initializeEntityTrackerCoordinator()
            isCoordinatorInitialized = true
        }
    }

    /**
     * Apply the settings to the SDK and reinitialize SDK
     */
    fun applySettingsToSDK() {
        homeUseCase.updateEntityTrackerCoordinator()
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
     * observe the EntityTrackerCoordinator state in EntityTrackerCoordinator and update the EntityTrackerInitState
     */
    fun observerEntityTrackerInitState() {
        homeUseCase.observeEntityTrackerCoordinatorState()
            .map { coordinatorState ->
                if(coordinatorState == CoordinatorState.COORDINATOR_READY) {
                    _entityTrackerInitState.update { it.copy(isInitialized = true) }
                    Log.d(TAG, "Entity tracker init state is updated to true")
                } else {
                    _entityTrackerInitState.update { it.copy(isInitialized = false) }
                    Log.d(TAG, "Entity tracker init state is updated to false")
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