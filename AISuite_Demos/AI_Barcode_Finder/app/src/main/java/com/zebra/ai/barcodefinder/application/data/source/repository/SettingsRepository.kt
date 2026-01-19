package com.zebra.ai.barcodefinder.application.data.source.repository

import com.zebra.ai.barcodefinder.application.data.source.storage.SettingsJsonStorage
import com.zebra.ai.barcodefinder.sdkcoordinator.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsRepository private constructor(
    private val settingsJsonStorage: SettingsJsonStorage
)  {
    companion object {
        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(settingsJsonStorage: SettingsJsonStorage): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(settingsJsonStorage).also { INSTANCE = it }
            }
        }
    }

    private val _settings = MutableStateFlow(settingsJsonStorage.loadSettings())
    val settings: StateFlow<AppSettings> = _settings

    fun loadSettings(): AppSettings {
        val loadedSettings = settingsJsonStorage.loadSettings()
        _settings.value = loadedSettings
        return loadedSettings
    }

    fun saveSettings(newSettings: AppSettings) {
        _settings.value = newSettings
        settingsJsonStorage.saveSettings(newSettings)
    }

    fun resetSettings() {
        val defaultSettings = AppSettings()
        _settings.value = defaultSettings
        settingsJsonStorage.saveSettings(defaultSettings)
    }
}