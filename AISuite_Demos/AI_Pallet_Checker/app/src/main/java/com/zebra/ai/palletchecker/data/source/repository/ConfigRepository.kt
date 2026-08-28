package com.zebra.ai.palletchecker.data.source.repository

import com.zebra.ai.palletchecker.data.source.storage.ConfigStorage
import com.zebra.ai.palletchecker.domain.model.AppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConfigRepository private constructor(private val settingsStorageManager: ConfigStorage) {

    // StateFlow to hold the current Configuration
    private val _config = MutableStateFlow(settingsStorageManager.loadSettings())
    val config: StateFlow<AppConfig> = _config.asStateFlow()


    // Update Configuration and save them to storage
    fun updateSettings(newSettings: AppConfig) {
        if (!_config.value.isEquals(newSettings)) {
            _config.value = newSettings
            settingsStorageManager.saveSettings(newSettings)
        }
    }

    // Load Configuration (if needed externally)
    fun loadSettings(): AppConfig {
        val loadedSettings = settingsStorageManager.loadSettings()
        _config.value = loadedSettings
        return loadedSettings
    }

    companion object {
        @Volatile
        private var INSTANCE: ConfigRepository?  = null

        // Provide the singleton instance of SettingsManager
        fun getInstance(settingsStorageManager: ConfigStorage): ConfigRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ConfigRepository(settingsStorageManager).also { INSTANCE = it }
            }

        }
    }
}