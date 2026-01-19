package com.zebra.ai.barcodefinder.application.data.source.storage

import android.app.Application
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zebra.ai.barcodefinder.sdkcoordinator.model.AppSettings
import java.io.File


/**
 * Storage manager for application settings.
 */
class SettingsJsonStorage(application: Application) : JsonStorage<AppSettings>(Gson()) {

    private val settingsFile: File = File(
        application.getExternalFilesDir("") ?: application.filesDir,
        "app_settings.json"
    )

    override fun getFile(): File = settingsFile

    /**
     * Loads the application settings.
     *
     * @return The settings object, or a default if not found.
     */
    fun loadSettings(): AppSettings {
        return loadData(object : TypeToken<AppSettings>() {}) ?: AppSettings()
    }

    /**
     * Saves the application settings.
     *
     * @param settings The settings to save.
     */
    fun saveSettings(settings: AppSettings) {
        saveData(settings)
    }
}