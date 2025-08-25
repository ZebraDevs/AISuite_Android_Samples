// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.zebra.ai.barcodefinder.common.enums.ModelInput
import com.zebra.ai.barcodefinder.common.enums.ProcessorType
import com.zebra.ai.barcodefinder.common.enums.Resolution
import com.zebra.ai.barcodefinder.data.model.AppSettings
import com.zebra.ai.barcodefinder.data.model.BarcodeSymbology
import com.zebra.ai.barcodefinder.data.repository.EntityTrackerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for managing app settings and scan state.
 * Handles updating model input, resolution, processor type, barcode symbology,
 * and scan state, syncing changes with the EntityTrackerRepository.
 *
 * @constructor Creates a SettingsViewModel with the given application context.
 * @param application The application context
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val entityTrackerRepository = EntityTrackerRepository.getInstance(application)

    /** UI state for the settings screen. */
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /** Current app settings from the repository. */
    val settings: StateFlow<AppSettings> = entityTrackerRepository.settings

    /**
     * Updates settings using the provided update function and syncs with repository.
     * @param update Function to update AppSettings
     */
    private fun updateSettingsWith(
        update: (AppSettings) -> AppSettings
    ) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        val currentSettings = settings.value
        val updatedSettings = update(currentSettings)
        entityTrackerRepository.updateSettings(updatedSettings)
        _uiState.value = _uiState.value.copy(
            isLoading = false,
        )
    }

    /**
     * Updates the model input size and syncs with the AI Vision SDK.
     */
    fun updateModelInput(modelInput: ModelInput) {
        updateSettingsWith(
            { it.copy(modelInput = modelInput) }
        )
    }

    /**
     * Updates the camera resolution and syncs with the AI Vision SDK.
     */
    fun updateResolution(resolution: Resolution) {
        updateSettingsWith(
            { it.copy(resolution = resolution) }
        )
    }

    /**
     * Updates the processor type and syncs with the AI Vision SDK.
     */
    fun updateProcessorType(processorType: ProcessorType) {
        updateSettingsWith(
            { it.copy(processorType = processorType) }
        )
    }

    /**
     * Updates the barcode symbology and syncs with the AI Vision SDK.
     */
    fun updateBarcodeSymbology(symbology: BarcodeSymbology) {
        updateSettingsWith(
            { it.copy(barcodeSymbology = symbology) }
        )
    }

    /**
     * Updates an individual symbology setting by name for UI convenience.
     * @param symbologyName The name of the symbology
     * @param enabled Whether the symbology is enabled
     */
    fun updateSymbology(symbologyName: String, enabled: Boolean) {
        val currentSymbology = settings.value.barcodeSymbology
        val updatedSymbology = when (symbologyName) {
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

        updateBarcodeSymbology(updatedSymbology)
    }

    /**
     * Resets all settings to their default values and syncs with the AI Vision SDK.
     */
    fun resetToDefaults() {
        _uiState.value = _uiState.value.copy(isLoading = true)

        entityTrackerRepository.updateSettings(AppSettings())

        _uiState.value = _uiState.value.copy(
            isLoading = false
        )
    }

    /**
     * Resets the scan started state.
     */
    fun resetScanStarted() {
        _uiState.value = _uiState.value.copy(scanStarted = false)
    }

    /**
     * Starts a scan by setting the scan started flag.
     */
    fun startScan() {
        _uiState.value = _uiState.value.copy(scanStarted = true)
    }

    /**
     * Handles the start scan action (alias for startScan for UI compatibility).
     */
    fun onStartScan() {
        startScan()
    }

}

/**
 * UI state for the settings screen.
 * @property isLoading Whether a settings update is in progress
 * @property error Error message if any
 * @property isSdkInitialized Whether the SDK is initialized
 * @property scanStarted Whether a scan has started
 * @property isScanning Whether scanning is in progress
 */
data class SettingsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSdkInitialized: Boolean = false,
    // TODO - Revisit scanStarted and isScanning logic, and remove if not needed
    val scanStarted: Boolean = false,
    val isScanning: Boolean = false
)
