package com.zebra.ai.palletchecker.data.source.storage

import android.app.Application
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zebra.ai.palletchecker.domain.model.AppConfig
import java.io.File


/**
 * Storage manager for application configuration.
 */
class ConfigStorage(application: Application) : JsonStorage<AppConfig>(Gson()) {

    private val configFile: File = File(
        application.getExternalFilesDir("") ?: application.filesDir,
        "config.json"
    )


    override fun getFile(): File = configFile

    /**
     * Loads the application settings.
     *
     * @return The settings object, or a default if not found.
     */
    fun loadSettings(): AppConfig {

        val defaultConfig = AppConfig()

        return loadData(object : TypeToken<AppConfig>() {}) ?: defaultConfig
    }

    /**
     * Saves the application settings.
     *
     * @param settings The settings to save.
     */
    fun saveSettings(settings: AppConfig) {
        saveData(settings)
    }
}