package com.zebra.ai.palletchecker.domain

import android.app.Application
import com.zebra.ai.palletchecker.data.source.repository.ConfigRepository
import com.zebra.ai.palletchecker.data.source.storage.ConfigStorage
import com.zebra.ai.palletchecker.domain.model.AppConfig
import kotlinx.coroutines.flow.StateFlow


class Configuration(application: Application) {
    private val configRepository = ConfigRepository.getInstance(ConfigStorage(application))
    /**
     * Updates settings using the provided update function and syncs with the repository.
     * @param update A lambda to modify the current settings.
     */
    fun updateSettings(update: (AppConfig) -> AppConfig) {
        val currentSettings = configRepository.config.value
        val updatedSettings = update(currentSettings)
        configRepository.updateSettings(updatedSettings)
    }

    /**
     * Resets all settings to their default values.
     */
    fun resetToDefaults() {
        configRepository.updateSettings(AppConfig())
    }

    /**
     * Retrieves the current config as a StateFlow.
     * @return A StateFlow emitting the current AppSettings.
     */
    fun getConfig() : StateFlow<AppConfig>{
        return configRepository.config
    }
}