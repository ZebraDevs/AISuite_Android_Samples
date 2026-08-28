package com.zebra.ai.palletchecker.data.source.storage

import android.app.Application
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zebra.ai.palletchecker.domain.enums.ModelInput
import com.zebra.ai.palletchecker.domain.enums.Resolution
import com.zebra.ai.palletchecker.domain.enums.SettingsMode
import com.zebra.ai.palletchecker.domain.model.AppSettings
import java.io.File


/**
 * Storage manager for application settings.
 */
class SettingsStorage(application: Application, val mode: SettingsMode) : JsonStorage<AppSettings>(Gson()) {

    private val settingsFile: File = File(
        application.getExternalFilesDir("") ?: application.filesDir,
        "app_settings.json"
    )

    private val wandSettingsFile: File = File(
        application.getExternalFilesDir("") ?: application.filesDir,
        "wand_settings.json"
    )

    override fun getFile(): File = if(mode == SettingsMode.SNAP)  settingsFile else wandSettingsFile

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
        val defaultSettings = if(mode == SettingsMode.SNAP) {
            AppSettings(resolution = Resolution.TWELVE_MP, modelInput = ModelInput.LARGE_1600)
        } else {
            /// Use 2 MP for wand mode to balance barcode decoding accuracy with lower memory and battery usage.
            AppSettings(resolution = Resolution.TWO_MP, modelInput = ModelInput.SMALL_640)
        }

        val loadedSettings = loadData(object : TypeToken<AppSettings>() {})

        // Migration: Default missing AI decode toggle to enabled for legacy JSON files before running subsequent migrations.
        val normalizedSettings = if (loadedSettings != null && !hasAiDecodeKeyInJson()) {
            loadedSettings.copy(enableAIbarcodeDecode = true)
        } else {
            loadedSettings
        }

        if (mode == SettingsMode.WAND && normalizedSettings != null && normalizedSettings.modelInput != ModelInput.SMALL_640) {
            val upgradedSettings = normalizedSettings.copy(modelInput = ModelInput.SMALL_640)
            saveSettings(upgradedSettings)
            return upgradedSettings
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