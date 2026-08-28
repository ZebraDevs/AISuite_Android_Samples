// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.palletchecker.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.zebra.ai.palletchecker.camera.CameraResolutionProvider
import com.zebra.ai.palletchecker.domain.Settings
import com.zebra.ai.palletchecker.domain.enums.AutoTriggerMode
import com.zebra.ai.palletchecker.domain.enums.ModelInput
import com.zebra.ai.palletchecker.domain.enums.ProcessorType
import com.zebra.ai.palletchecker.domain.enums.Resolution
import com.zebra.ai.palletchecker.domain.enums.SettingsMode
import com.zebra.ai.palletchecker.domain.model.AppSettings
import com.zebra.ai.palletchecker.domain.model.BarcodeSymbology
import com.zebra.ai.palletchecker.presentation.model.SettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for managing app settings and scan state.
 * Handles updating model input, resolution, processor type, barcode symbology,
 * and scan state, syncing changes with the EntityTrackerFacade.
 *
 * @constructor Creates a SettingsViewModel with the given application context.
 * @param application The application context
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsUseCase = Settings(application, SettingsMode.SNAP)
    private val wandsettingsUseCase = Settings(application, SettingsMode.WAND)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsUseCase.getSettings()

    val wandSettings: StateFlow<AppSettings> = wandsettingsUseCase.getSettings()

    private val _supportedSnapResolutions = MutableStateFlow<List<CameraResolutionProvider.CameraResolution>>(emptyList())
    val supportedSnapResolutions: StateFlow<List<CameraResolutionProvider.CameraResolution>> = _supportedSnapResolutions.asStateFlow()

    private val _supportedWandResolutions = MutableStateFlow<List<CameraResolutionProvider.CameraResolution>>(emptyList())
    val supportedWandResolutions: StateFlow<List<CameraResolutionProvider.CameraResolution>> = _supportedWandResolutions.asStateFlow()

    init {
        val snapResolutions = CameraResolutionProvider.getSnapResolutions(application)
        _supportedSnapResolutions.value = snapResolutions
        _supportedWandResolutions.value = CameraResolutionProvider.getWandResolutions(application)

        val current = settings.value
        val savedIsInList = snapResolutions.any {
            it.width == current.customResolutionWidth && it.height == current.customResolutionHeight
        }
        val needsDefault = current.resolution != Resolution.MAX
                || current.customResolutionWidth == 0
                || !savedIsInList

        if (needsDefault) {
            val best4x3 = snapResolutions
                .filter { it.aspectRatio == "4:3" && it.width.toLong() * it.height.toLong() >= 8_000_000L }
                .maxByOrNull { it.width.toLong() * it.height.toLong() }

            val bestRes = best4x3
                ?: snapResolutions.filter { it.aspectRatio == "4:3" }.maxByOrNull { it.width.toLong() * it.height.toLong() }
                ?: snapResolutions.maxByOrNull { it.width.toLong() * it.height.toLong() }
            bestRes?.let { updateCustomResolution(it.width, it.height, SettingsMode.SNAP) }
        }
    }

    /**
     * Updates settings using the provided update function and syncs with repository.
     * @param update Function to update AppSettings
     */
    private fun updateSettingsWith(update: (AppSettings) -> AppSettings, mode: SettingsMode = SettingsMode.SNAP) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        if (mode == SettingsMode.SNAP) {
            settingsUseCase.updateSettings(update)
        } else {
            wandsettingsUseCase.updateSettings(update)
        }
        _uiState.value = _uiState.value.copy(isLoading = false)
    }

    /**
     * Updates the model input size and syncs with the AI Vision SDK.
     */
    fun updateModelInput(modelInput: ModelInput, mode: SettingsMode = SettingsMode.SNAP) {
        updateSettingsWith ({ it.copy(modelInput = modelInput) },mode)
    }

    /**
     * Updates the camera resolution and syncs with the AI Vision SDK.
     */
    fun updateResolution(resolution: Resolution, mode: SettingsMode = SettingsMode.SNAP) {
        updateSettingsWith ({ it.copy(resolution = resolution) }, mode)
    }

    /**
     * Sets a device-queried custom resolution.  Stores resolution = MAX and
     * records the actual width × height so that CameraHelper / ResultsScreen
     * can use the concrete size via effectiveWidth()/effectiveHeight().
     */
    fun updateCustomResolution(width: Int, height: Int, mode: SettingsMode = SettingsMode.SNAP) {
        updateSettingsWith({
            it.copy(
                resolution = Resolution.MAX,
                customResolutionWidth = width,
                customResolutionHeight = height
            )
        }, mode)
    }

    /**
     * Updates the processor type and syncs with the AI Vision SDK.
     */
    fun updateProcessorType(processorType: ProcessorType, mode: SettingsMode = SettingsMode.SNAP) {
        updateSettingsWith( { it.copy(processorType = processorType) },mode)
    }

    /**
     * Updates the barcode symbology and syncs with the AI Vision SDK.
     */
    fun updateBarcodeSymbology(symbology: BarcodeSymbology, mode: SettingsMode = SettingsMode.SNAP) {
        updateSettingsWith( { it.copy(barcodeSymbology = symbology) },mode)
    }

    /**
     * Updates an individual symbology setting by name for UI convenience.
     * @param symbologyName The name of the symbology
     * @param enabled Whether the symbology is enabled
     */
    fun updateSymbology(symbologyName: String, enabled: Boolean, mode: SettingsMode = SettingsMode.SNAP) {
        val updatedSymbology =
            settingsUseCase.updateSymbology(symbologyName, enabled)
        updateBarcodeSymbology(updatedSymbology, SettingsMode.SNAP)
        updateBarcodeSymbology(updatedSymbology, SettingsMode.WAND)
    }

    /**
     * Resets all settings to their default values and syncs with the AI Vision SDK.
     */
    fun resetToDefaults() {

        _uiState.value = _uiState.value.copy(isLoading = true)

        settingsUseCase.resetToDefaults()
        wandsettingsUseCase.resetToDefaults()

        val snapResolutions = _supportedSnapResolutions.value
        val best4x3 = snapResolutions
            .filter { it.aspectRatio == "4:3" && it.width.toLong() * it.height.toLong() >= 8_000_000L }
            .maxByOrNull { it.width.toLong() * it.height.toLong() }
        val bestRes = best4x3
            ?: snapResolutions.filter { it.aspectRatio == "4:3" }.maxByOrNull { it.width.toLong() * it.height.toLong() }
            ?: snapResolutions.maxByOrNull { it.width.toLong() * it.height.toLong() }
        bestRes?.let { updateCustomResolution(it.width, it.height, SettingsMode.SNAP) }

        _uiState.value = _uiState.value.copy(isLoading = false)
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

    /**
     * Updates AI-powered barcode decode pipeline toggle for both Snap and Wand modes.
     */
    fun updateAiBarcodeDecodeEnabled(enabled: Boolean) {
        updateSettingsWith({ it.copy(enableAIbarcodeDecode = enabled) }, SettingsMode.SNAP)
        updateSettingsWith({ it.copy(enableAIbarcodeDecode = enabled) }, SettingsMode.WAND)
    }

    /**
     * Updates the live PiP thumbnail setting.
     */
    fun updateLivePipThumbnailEnabled(enabled: Boolean) {
        updateSettingsWith ({ it.copy(livePipThumbnailEnabled = enabled) }, SettingsMode.WAND)
    }

    /**
     * Updates the audit progress indicator visibility in wand mode.
     */
    fun updateShowAuditProgress(enabled: Boolean) {
        updateSettingsWith({ it.copy(showAuditProgress = enabled) }, SettingsMode.WAND)
    }

    /**
     * Updates the live bounding box overlay visibility during wand mode.
     */
    fun updateShowLiveBoundingBoxes(enabled: Boolean) {
        updateSettingsWith({ it.copy(showLiveBoundingBoxes = enabled) }, SettingsMode.WAND)
    }

    /**
     * Enables or disables multi-snap retry (multiple captures to find all barcodes).
     * When disabled, only a single snap is taken.
     */
    fun updateMultiSnapEnabled(enabled: Boolean) {
        updateSettingsWith({ it.copy(multiSnapEnabled = enabled) }, SettingsMode.SNAP)
    }

    /**
     * Updates the number of multi-snap retry attempts (1–10).
     */
    fun updateMultiSnapRetryCount(count: Int) {
        updateSettingsWith({ it.copy(multiSnapRetryCount = count.coerceIn(1, 10)) }, SettingsMode.SNAP)
    }

    /**
     * Enables or disables auto-trigger (auto-snap). When disabled, a manual capture button is shown.
     */
    fun updateAutoTriggerEnabled(enabled: Boolean) {
        updateSettingsWith({ it.copy(autoTriggerEnabled = enabled) }, SettingsMode.SNAP)
    }

    /**
     * Updates the auto-trigger mode/criteria (Pallet Base, Fixed Quantity, Percentage Based).
     */
    fun updateAutoTriggerMode(mode: AutoTriggerMode) {
        updateSettingsWith({ it.copy(autoTriggerMode = mode) }, SettingsMode.SNAP)
    }

    /**
     * Updates the fixed quantity threshold for auto-snap trigger.
     */
    fun updateFixedQuantityThreshold(value: Int) {
        updateSettingsWith({ it.copy(fixedQuantityThreshold = value.coerceAtLeast(1)) }, SettingsMode.SNAP)
    }

    /**
     * Updates the percentage threshold for auto-snap trigger.
     */
    fun updatePercentageThreshold(value: Int) {
        updateSettingsWith({ it.copy(percentageThreshold = value.coerceIn(1, 100)) }, SettingsMode.SNAP)
    }

    /**
     * Updates the expected total boxes (used for percentage-based auto-snap).
     */
    fun updateExpectedTotalBoxes(value: Int) {
        updateSettingsWith({ it.copy(expectedTotalBoxes = value.coerceAtLeast(1)) }, SettingsMode.SNAP)
    }

    /**
     * Enables or disables debug mode globally.
     */
    fun updateDebugModeEnabled(enabled: Boolean) {
        updateSettingsWith({
            it.copy(debugSettings = it.debugSettings.copy(
                debugModeEnabled = enabled,
                showPreSnapBoundingBoxes = if (enabled) true else it.debugSettings.showPreSnapBoundingBoxes,
                showCapturedSnapBarcodeLabels = if (enabled) true else it.debugSettings.showCapturedSnapBarcodeLabels
            ))
        }, SettingsMode.SNAP)
    }

    /**
     * Enables or disables showing pre-snap bounding boxes during live preview.
     */
    fun updateShowPreSnapBoundingBoxes(enabled: Boolean) {
        updateSettingsWith({
            it.copy(debugSettings = it.debugSettings.copy(showPreSnapBoundingBoxes = enabled))
        }, SettingsMode.SNAP)
    }

    /**
     * Enables or disables showing spatial map indices on boxes.
     */
    fun updateShowSpatialMapIndices(enabled: Boolean) {
        updateSettingsWith({
            it.copy(debugSettings = it.debugSettings.copy(showSpatialMapIndices = enabled))
        }, SettingsMode.SNAP)
    }


    /**
     * Enables or disables showing barcode data labels on boxes during snap mode debug.
     */
    fun updateShowSnapBarcodeLabels(enabled: Boolean) {
        updateSettingsWith({
            it.copy(debugSettings = it.debugSettings.copy(showSnapBarcodeLabels = enabled))
        }, SettingsMode.SNAP)
    }

    /**
     * Enables or disables showing barcode data labels on boxes in the captured snap result view.
     */
    fun updateShowCapturedSnapBarcodeLabels(enabled: Boolean) {
        updateSettingsWith({
            it.copy(debugSettings = it.debugSettings.copy(showCapturedSnapBarcodeLabels = enabled))
        }, SettingsMode.SNAP)
    }

    /**
     * Enables or disables wand debug mode globally.
     */
    fun updateWandDebugModeEnabled(enabled: Boolean) {
        updateSettingsWith({
            it.copy(debugSettings = it.debugSettings.copy(
                debugModeEnabled = enabled,
                showWandBarcodeLabels = if (enabled) true else it.debugSettings.showWandBarcodeLabels
            ))
        }, SettingsMode.WAND)
    }

    /**
     * Enables or disables showing barcode data labels on boxes during wand mode debug.
     */
    fun updateShowWandBarcodeLabels(enabled: Boolean) {
        updateSettingsWith({
            it.copy(debugSettings = it.debugSettings.copy(showWandBarcodeLabels = enabled))
        }, SettingsMode.WAND)
    }

    /**
     * Enables or disables showing tracking IDs on boxes during snap (pre-snap) debug mode.
     * Tracking IDs are shown at the top-right corner of the box with an amber badge.
     */
    fun updateShowSnapTrackingIds(enabled: Boolean) {
        updateSettingsWith({
            it.copy(debugSettings = it.debugSettings.copy(showSnapTrackingIds = enabled))
        }, SettingsMode.SNAP)
    }

    /**
     * Enables or disables showing tracking IDs on boxes during wand debug mode.
     * Tracking IDs are shown at the top-right corner of the box with an amber badge.
     */
    fun updateShowWandTrackingIds(enabled: Boolean) {
        updateSettingsWith({
            it.copy(debugSettings = it.debugSettings.copy(showWandTrackingIds = enabled))
        }, SettingsMode.WAND)
    }


    /**
     * Enables or disables dumping of Snap ImageCapture frames to the device filesystem
     * (Pictures/PalletCheckerDebug/). Only applies to Snap mode.
     */
    fun updateDumpSnapImagesToFilesystem(enabled: Boolean) {
        updateSettingsWith({
            it.copy(debugSettings = it.debugSettings.copy(dumpSnapImagesToFilesystem = enabled))
        }, SettingsMode.SNAP)
    }
}
