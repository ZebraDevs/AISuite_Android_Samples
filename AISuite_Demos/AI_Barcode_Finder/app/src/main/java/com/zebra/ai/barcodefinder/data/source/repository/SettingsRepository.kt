package com.zebra.ai.barcodefinder.data.source.repository

import com.zebra.ai.barcodefinder.data.source.storage.SettingsStorage
import com.zebra.ai.barcodefinder.domain.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for managing application settings and their persistence.
 *
 * Responsibilities:
 * - Maintains the current application settings using a StateFlow for reactive updates.
 * - Provides methods to update settings and automatically persist them.
 * - Loads settings from storage to ensure the application uses the most recent configurations.
 *
 * Usage:
 * - Singleton pattern: use getInstance(settingsStorageManager) to obtain the repository.
 * - Interacts with other parts of the application to provide and update settings data.
 * - Note: JSON persistence is handled internally by this repository through the SettingsStorageManager.
 */
class SettingsRepository private constructor(private val settingsStorageManager: SettingsStorage) {

    // StateFlow to hold the current settings
    private val _settings = MutableStateFlow(settingsStorageManager.loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    // Update settings and save them to storage
    fun updateSettings(newSettings: AppSettings) {
        if (!_settings.value.isEquals(newSettings)) {
            _settings.value = newSettings
            settingsStorageManager.saveSettings(newSettings)
        }
    }

    // Load settings (if needed externally)
    fun loadSettings(): AppSettings {
        val loadedSettings = settingsStorageManager.loadSettings()
        _settings.value = loadedSettings
        return loadedSettings
    }

    companion object {
        @Volatile
        private var INSTANCE: SettingsRepository? = null

        // Provide the singleton instance of SettingsManager
        fun getInstance(settingsStorageManager: SettingsStorage): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(settingsStorageManager).also { INSTANCE = it }
            }
        }
    }
}