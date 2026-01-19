package com.zebra.ai.barcodefinder.application.domain.usecase

import com.zebra.ai.barcodefinder.application.data.source.repository.SettingsRepository
import com.zebra.ai.barcodefinder.sdkcoordinator.EntityTrackerCoordinator
import com.zebra.ai.barcodefinder.sdkcoordinator.enums.CoordinatorState
import com.zebra.ai.barcodefinder.sdkcoordinator.model.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeUseCase(
    private val entityTrackerCoordinator: EntityTrackerCoordinator,
    private val settingsRepository: SettingsRepository
) {

    /**
     * Initialize the EntityTrackerCoordinator
     */
    fun initializeEntityTrackerCoordinator() {
        settingsRepository.loadSettings()
        val appSettings = settingsRepository.settings.value

        CoroutineScope(Dispatchers.Main).launch {
            entityTrackerCoordinator.configureSdk(appSettings, reset = false)
        }
    }

    /**
     * Dispose the EntityTrackerCoordinator resources
     */
    fun disposeEntityTrackerCoordinatorResources() {
        entityTrackerCoordinator.dispose()
    }

    /**
     * Reinitialize the EntityTrackerCoordinator applying new settings
     */
    fun updateEntityTrackerCoordinator() {
        settingsRepository.loadSettings()
        val appSettings = settingsRepository.settings.value

        CoroutineScope(Dispatchers.Main).launch {
            entityTrackerCoordinator.configureSdk(appSettings, reset = true)
        }
    }

    /**
     * Observe the EntityTrackerCoordinator state
     */
    fun observeEntityTrackerCoordinatorState(): StateFlow<CoordinatorState> {
        return entityTrackerCoordinator.coordinatorState
    }

    /**
     * Observes the current settings as a [kotlinx.coroutines.flow.StateFlow].
     *
     * @return A [kotlinx.coroutines.flow.StateFlow] of [AppSettings].
     */
    fun observeSettings(): StateFlow<AppSettings> {
        return settingsRepository.settings
    }

    /**
     * Updates the application settings by applying the provided update function.
     *
     * @param update A function that takes the current [AppSettings] and returns the updated [AppSettings].
     */
    fun updateSettings(update: (AppSettings) -> AppSettings) {
        val currentSettings = settingsRepository.loadSettings()
        val updatedSettings = update(currentSettings)
        settingsRepository.saveSettings(updatedSettings)
    }


}