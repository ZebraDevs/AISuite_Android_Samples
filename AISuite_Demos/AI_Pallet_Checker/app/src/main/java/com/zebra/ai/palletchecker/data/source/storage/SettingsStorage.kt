package com.zebra.ai.palletchecker.data.source.storage

import android.app.Application
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zebra.ai.palletchecker.domain.enums.ModelInput
import com.zebra.ai.palletchecker.domain.enums.Resolution
import com.zebra.ai.palletchecker.domain.model.AppSettings
import java.io.File


/**
 * Storage manager for application settings.
 */
class SettingsStorage(application: Application) : JsonStorage<AppSettings>(Gson()) {

    private val settingsFile: File = File(
        application.getExternalFilesDir("") ?: application.filesDir,
        "app_settings.json"
    )

    override fun getFile(): File = settingsFile

    private fun hasAiDecodeKeyInJson(): Boolean {
        val file = getFile()
        if (!file.exists()) return false
        return runCatching { file.readText().contains("\"enableAIbarcodeDecode\"") }.getOrDefault(false)
    }

    /**
     * Loads the application settings.
     *
     * @return The settings object, or a default if not found.
     */
    fun loadSettings(): AppSettings {
        val defaultSettings = AppSettings(resolution = Resolution.TWELVE_MP, modelInput = ModelInput.LARGE)

        val loadedSettings = loadData(object : TypeToken<AppSettings>() {})

        // Migration: Default missing AI decode toggle to enabled for legacy JSON files before running subsequent migrations.
        var normalizedSettings = if (loadedSettings != null && !hasAiDecodeKeyInJson()) {
            loadedSettings.copy(enableAIbarcodeDecode = true)
        } else {
            loadedSettings
        }

        if(normalizedSettings != null && normalizedSettings.modelInput == null) {
            val migrated = normalizedSettings.copy(
                modelInput = ModelInput.LARGE
            )
            saveSettings(migrated)
            normalizedSettings = migrated
        }

        if (normalizedSettings != null
            && normalizedSettings.resolution == Resolution.MAX
            && normalizedSettings.customResolutionWidth == 0
        ) {
            val migrated = normalizedSettings.copy(
                customResolutionWidth = Resolution.TWELVE_MP.width,
                customResolutionHeight = Resolution.TWELVE_MP.height
            )
            saveSettings(migrated)
            return migrated
        }

        if (normalizedSettings != null && normalizedSettings !== loadedSettings) {
            saveSettings(normalizedSettings)
        }

        return normalizedSettings ?: defaultSettings
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