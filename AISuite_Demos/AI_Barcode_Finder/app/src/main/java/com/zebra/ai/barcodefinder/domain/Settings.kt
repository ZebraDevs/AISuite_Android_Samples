package com.zebra.ai.barcodefinder.domain

import android.app.Application
import com.zebra.ai.barcodefinder.domain.model.AppSettings
import com.zebra.ai.barcodefinder.domain.model.BarcodeSymbology
import com.zebra.ai.barcodefinder.data.source.repository.SettingsRepository
import com.zebra.ai.barcodefinder.data.source.storage.SettingsStorage
import kotlinx.coroutines.flow.StateFlow

/**
 * Use case for managing application settings and interacting with the settings repository.
 *
 * Responsibilities:
 * - Provides functionality to update application settings, including individual barcode symbologies.
 * - Interfaces with the SettingsRepository to persist and retrieve settings.
 * - Allows resetting of all settings to their default values.
 * - Offers a reactive way to access current settings using StateFlow for observing changes.
 *
 * Usage:
 * - Use this class to update settings by passing a lambda function to `updateSettings`.
 * - Modify individual symbology settings using `updateSymbology`.
 * - Reset all settings to their default values using `resetToDefaults`.
 * - Retrieve the current settings via `getSettings` to observe settings changes in real time.
 * - Integrate with UI components and other parts of the application to ensure settings are applied consistently.
 */
class Settings(application: Application) {
    private val settingsRepository = SettingsRepository.getInstance(SettingsStorage(application))

    /**
     * Updates settings using the provided update function and syncs with the repository.
     * @param update A lambda to modify the current settings.
     */
    fun updateSettings(update: (AppSettings) -> AppSettings) {
        val currentSettings = settingsRepository.settings.value
        val updatedSettings = update(currentSettings)
        settingsRepository.updateSettings(updatedSettings)
    }

    /**
     * Updates an individual symbology setting by name.
     * @param symbologyName The name of the symbology to update.
     * @param enabled Whether the symbology should be enabled or disabled.
     * @return The updated BarcodeSymbology object.
     */
    fun updateSymbology(symbologyName: String, enabled: Boolean): BarcodeSymbology {
        val currentSymbology = settingsRepository.settings.value.barcodeSymbology
        return when (symbologyName) {
            "Australian Postal" -> currentSymbology.copy(australianPostal = enabled)
            "Aztec" -> currentSymbology.copy(aztec = enabled)
            "Canadian Postal" -> currentSymbology.copy(canadianPostal = enabled)
            "Chinese 2of5" -> currentSymbology.copy(chinese2of5 = enabled)
            "Codabar" -> currentSymbology.copy(codabar = enabled)
            "Code 11" -> currentSymbology.copy(code11 = enabled)
            "Code 39" -> currentSymbology.copy(code39 = enabled)
            "Code 93" -> currentSymbology.copy(code93 = enabled)
            "Code 128" -> currentSymbology.copy(code128 = enabled)
            "Composite AB" -> currentSymbology.copy(compositeAB = enabled)
            "Composite C" -> currentSymbology.copy(compositeC = enabled)
            "D2of5" -> currentSymbology.copy(d2of5 = enabled)
            "DataMatrix" -> currentSymbology.copy(datamatrix = enabled)
            "DotCode" -> currentSymbology.copy(dotcode = enabled)
            "Dutch Postal" -> currentSymbology.copy(dutchPostal = enabled)
            "EAN-8" -> currentSymbology.copy(ean8 = enabled)
            "EAN-13" -> currentSymbology.copy(ean13 = enabled)
            "Finnish Postal 4S" -> currentSymbology.copy(finnishPostal4s = enabled)
            "Grid Matrix" -> currentSymbology.copy(gridMatrix = enabled)
            "GS1 DataBar" -> currentSymbology.copy(gs1Databar = enabled)
            "GS1 DataBar Expanded" -> currentSymbology.copy(gs1DatabarExpanded = enabled)
            "GS1 DataBar Limited" -> currentSymbology.copy(gs1DatabarLim = enabled)
            "GS1 DataMatrix" -> currentSymbology.copy(gs1Datamatrix = enabled)
            "GS1 QR Code" -> currentSymbology.copy(gs1Qrcode = enabled)
            "Hanxin" -> currentSymbology.copy(hanxin = enabled)
            "I2of5" -> currentSymbology.copy(i2of5 = enabled)
            "Japanese Postal" -> currentSymbology.copy(japanesePostal = enabled)
            "Korean 3of5" -> currentSymbology.copy(korean3of5 = enabled)
            "Mailmark" -> currentSymbology.copy(mailmark = enabled)
            "Matrix 2of5" -> currentSymbology.copy(matrix2of5 = enabled)
            "MaxiCode" -> currentSymbology.copy(maxicode = enabled)
            "MicroPDF" -> currentSymbology.copy(micropdf = enabled)
            "MicroQR" -> currentSymbology.copy(microqr = enabled)
            "MSI" -> currentSymbology.copy(msi = enabled)
            "PDF417" -> currentSymbology.copy(pdf417 = enabled)
            "QR Code" -> currentSymbology.copy(qrcode = enabled)
            "TLC39" -> currentSymbology.copy(tlc39 = enabled)
            "Trioptic 39" -> currentSymbology.copy(trioptic39 = enabled)
            "UK Postal" -> currentSymbology.copy(ukPostal = enabled)
            "UPC-A" -> currentSymbology.copy(upcA = enabled)
            "UPC-E" -> currentSymbology.copy(upcE = enabled)
            "UPC-E1" -> currentSymbology.copy(upce1 = enabled)
            "US Planet" -> currentSymbology.copy(usplanet = enabled)
            "US Postnet" -> currentSymbology.copy(uspostnet = enabled)
            "US 4-State" -> currentSymbology.copy(us4state = enabled)
            "US 4-State FICS" -> currentSymbology.copy(us4stateFics = enabled)
            else -> currentSymbology
        }
    }

    /**
     * Resets all settings to their default values.
     */
    fun resetToDefaults() {
        settingsRepository.updateSettings(AppSettings())
    }

    /**
     * Retrieves the current settings as a StateFlow.
     * @return A StateFlow emitting the current AppSettings.
     */
    fun getSettings() : StateFlow<AppSettings>{
        return settingsRepository.settings
    }
}