// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.palletchecker.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.zebra.ai.palletchecker.camera.CameraResolutionProvider
import com.zebra.ai.palletchecker.domain.Settings
import com.zebra.ai.palletchecker.domain.enums.AutoTriggerMode
import com.zebra.ai.palletchecker.domain.enums.ModelInput
import com.zebra.ai.palletchecker.domain.enums.PerformanceMode
import com.zebra.ai.palletchecker.domain.enums.ProcessorType
import com.zebra.ai.palletchecker.domain.enums.Resolution
import com.zebra.ai.palletchecker.domain.model.AppSettings
import com.zebra.ai.palletchecker.domain.model.BarcodeSymbology
import com.zebra.ai.palletchecker.helpers.DeviceCapabilities
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
    private val settingsUseCase = Settings(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsUseCase.getSettings()

    private val _supportedSnapResolutions = MutableStateFlow<List<CameraResolutionProvider.CameraResolution>>(emptyList())
    val supportedSnapResolutions: StateFlow<List<CameraResolutionProvider.CameraResolution>> = _supportedSnapResolutions.asStateFlow()

    init {
        val snapResolutions = CameraResolutionProvider.getSnapResolutions(application)
        _supportedSnapResolutions.value = snapResolutions

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
            bestRes?.let { updateCustomResolution(it.width, it.height) }
        }
    }

    /**
     * Updates settings using the provided update function and syncs with repository.
     * @param update Function to update AppSettings
     */
    private fun updateSettingsWith(update: (AppSettings) -> AppSettings) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        settingsUseCase.updateSettings(update)
        _uiState.value = _uiState.value.copy(isLoading = false)
    }

    /**
     * Updates the model input size and syncs with the AI Vision SDK.
     */
    fun updateModelInput(modelInput: ModelInput) {
        updateSettingsWith ({ it.copy(modelInput = modelInput) })
    }

    /**
     * Selects [ModelInput.CUSTOM]. Stored dimensions that are not valid (see
     * [AppSettings.isValidCustomModelSize], e.g. 0 from settings saved before these fields
     * existed) are replaced with the default so an invalid size is never applied.
     */
    fun selectCustomModelInput() {
        updateSettingsWith ({ current ->
            current.copy(
                modelInput = ModelInput.CUSTOM,
                customModelWidth = current.customModelWidth
                    .takeIf { AppSettings.isValidCustomModelSize(it) }
                    ?: AppSettings.DEFAULT_CUSTOM_MODEL_SIZE,
                customModelHeight = current.customModelHeight
                    .takeIf { AppSettings.isValidCustomModelSize(it) }
                    ?: AppSettings.DEFAULT_CUSTOM_MODEL_SIZE
            )
        })
    }

    /**
     * Selects [ModelInput.CUSTOM] with the given dimensions. Dimensions that are not valid are
     * rejected (settings stay unchanged); the UI reports the error to the user.
     */
    fun updateCustomModelInput(width: Int, height: Int) {
        if (!AppSettings.isValidCustomModelSize(width) ||
            !AppSettings.isValidCustomModelSize(height)
        ) return
        updateSettingsWith ({
            it.copy(
                modelInput = ModelInput.CUSTOM,
                customModelWidth = width,
                customModelHeight = height
            )
        })
    }

    /**
     * Whether this device's chipset supports the inferencer BURST ("Turbo") performance mode.
     * The Settings UI only shows the Capture Mode / Turbo option when this is true.
     */
    val isBurstModeSupported: Boolean = DeviceCapabilities.isBurstModeSupported

    /**
     * Updates the inferencer performance mode (Turbo on = BURST, off = BALANCED).
     */
    fun updatePerformanceMode(performanceMode: PerformanceMode) {
        updateSettingsWith({ it.copy(performanceMode = performanceMode) })
    }

    /**
     * Updates the camera resolution and syncs with the AI Vision SDK.
     */
    fun updateResolution(resolution: Resolution) {
        updateSettingsWith ({ it.copy(resolution = resolution) })
    }

    /**
     * Sets a device-queried custom resolution.  Stores resolution = MAX and
     * records the actual width × height so that CameraHelper / ResultsScreen
     * can use the concrete size via effectiveWidth()/effectiveHeight().
     */
    fun updateCustomResolution(width: Int, height: Int) {
        updateSettingsWith({
            it.copy(
                resolution = Resolution.MAX,
                customResolutionWidth = width,
                customResolutionHeight = height
            )
        })
    }

    /**
     * Updates the processor type and syncs with the AI Vision SDK.
     */
    fun updateProcessorType(processorType: ProcessorType) {
        updateSettingsWith( { it.copy(processorType = processorType) })
    }

    /**
     * Updates the barcode symbology and syncs with the AI Vision SDK.
     */
    fun updateBarcodeSymbology(symbology: BarcodeSymbology) {
        updateSettingsWith( { it.copy(barcodeSymbology = symbology) })
    }

    /**
     * Updates an individual symbology setting by name for UI convenience.
     * @param symbologyName The name of the symbology
     * @param enabled Whether the symbology is enabled
     */
    fun updateSymbology(symbologyName: String, enabled: Boolean) {
        val updatedSymbology =
            settingsUseCase.updateSymbology(symbologyName, enabled)
        updateBarcodeSymbology(updatedSymbology)
    }

    /**
     * Resets all settings to their default values and syncs with the AI Vision SDK.
     */
    fun resetToDefaults() {

        _uiState.value = _uiState.value.copy(isLoading = true)

        settingsUseCase.resetToDefaults()

        val snapResolutions = _supportedSnapResolutions.value
        val best4x3 = snapResolutions
            .filter { it.aspectRatio == "4:3" && it.width.toLong() * it.height.toLong() >= 8_000_000L }
            .maxByOrNull { it.width.toLong() * it.height.toLong() }
        val bestRes = best4x3
            ?: snapResolutions.filter { it.aspectRatio == "4:3" }.maxByOrNull { it.width.toLong() * it.height.toLong() }
            ?: snapResolutions.maxByOrNull { it.width.toLong() * it.height.toLong() }
        bestRes?.let { updateCustomResolution(it.width, it.height) }

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
        updateSettingsWith({ it.copy(enableAIbarcodeDecode = enabled) })
    }

    /**
     * Updates the live PiP thumbnail setting.
     */
    fun updateLivePipThumbnailEnabled(enabled: Boolean) {
        updateSettingsWith ({ it.copy(livePipThumbnailEnabled = enabled) })
    }

    /**
     * Updates the audit progress indicator visibility in wand mode.
     */
    fun updateShowAuditProgress(enabled: Boolean) {
        updateSettingsWith({ it.copy(showAuditProgress = enabled) })
    }

    /**
     * Updates the live bounding box overlay visibility during wand mode.
     */
    fun updateShowLiveBoundingBoxes(enabled: Boolean) {
        updateSettingsWith({ it.copy(showLiveBoundingBoxes = enabled) })
    }

    /**
     * Enables or disables multi-snap retry (multiple captures to find all barcodes).
     * When disabled, only a single snap is taken.
     */
    fun updateMultiSnapEnabled(enabled: Boolean) {
        updateSettingsWith({ it.copy(multiSnapEnabled = enabled) })
    }

    /**
     * Updates the number of multi-snap retry attempts (1–10).
     */
    fun updateMultiSnapRetryCount(count: Int) {
        updateSettingsWith({ it.copy(multiSnapRetryCount = count.coerceIn(1, 10)) })
    }

    /**
     * Enables or disables auto-trigger (auto-snap). When disabled, a manual capture button is shown.
     */
    fun updateAutoTriggerEnabled(enabled: Boolean) {
        updateSettingsWith({ it.copy(autoTriggerEnabled = enabled) })
    }

    /**
     * Updates the auto-trigger mode/criteria (Pallet Base, Fixed Quantity, Percentage Based).
     */
    fun updateAutoTriggerMode(mode: AutoTriggerMode) {
        updateSettingsWith({ it.copy(autoTriggerMode = mode) })
    }

    /**
     * Updates the fixed quantity threshold for auto-snap trigger.
     */
    fun updateFixedQuantityThreshold(value: Int) {
        updateSettingsWith({ it.copy(fixedQuantityThreshold = value.coerceAtLeast(1)) })
    }

    /**
     * Updates the percentage threshold for auto-snap trigger.
     */
    fun updatePercentageThreshold(value: Int) {
        updateSettingsWith({ it.copy(percentageThreshold = value.coerceIn(1, 100)) })
    }

    /**
     * Updates the expected total boxes (used for percentage-based auto-snap).
     */
    fun updateExpectedTotalBoxes(value: Int) {
        updateSettingsWith({ it.copy(expectedTotalBoxes = value.coerceAtLeast(1)) })
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
        })
    }

    /**
     * Enables or disables showing pre-snap bounding boxes during live preview.
     */
    fun updateShowPreSnapBoundingBoxes(enabled: Boolean) {
        updateSettingsWith({
            it.copy(debugSettings = it.debugSettings.copy(showPreSnapBoundingBoxes = enabled))
        })
    }

    /**
     * Enables or disables showing spatial map indices on boxes.
     */
    fun updateShowSpatialMapIndices(enabled: Boolean) {
        updateSettingsWith({
            it.copy(debugSettings = it.debugSettings.copy(showSpatialMapIndices = enabled))
        })
    }


    /**
     * Enables or disables showing barcode data labels on boxes during snap mode debug.
     */
    fun updateShowSnapBarcodeLabels(enabled: Boolean) {
        updateSettingsWith({
            it.copy(debugSettings = it.debugSettings.copy(showSnapBarcodeLabels = enabled))
        })
    }

    /**
     * Enables or disables showing barcode data labels on boxes in the captured snap result view.
     */
    fun updateShowCapturedSnapBarcodeLabels(enabled: Boolean) {
        updateSettingsWith({
            it.copy(debugSettings = it.debugSettings.copy(showCapturedSnapBarcodeLabels = enabled))
        })
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
        })
    }

    /**
     * Enables or disables showing barcode data labels on boxes during wand mode debug.
     */
    fun updateShowWandBarcodeLabels(enabled: Boolean) {
        updateSettingsWith({
            it.copy(debugSettings = it.debugSettings.copy(showWandBarcodeLabels = enabled))
        })
    }

    /**
     * Enables or disables showing tracking IDs on boxes during snap (pre-snap) debug mode.
     * Tracking IDs are shown at the top-right corner of the box with an amber badge.
     */
    fun updateShowSnapTrackingIds(enabled: Boolean) {
        updateSettingsWith({
            it.copy(debugSettings = it.debugSettings.copy(showSnapTrackingIds = enabled))
        })
    }

    /**
     * Enables or disables showing tracking IDs on boxes during wand debug mode.
     * Tracking IDs are shown at the top-right corner of the box with an amber badge.
     */
    fun updateShowWandTrackingIds(enabled: Boolean) {
        updateSettingsWith({
            it.copy(debugSettings = it.debugSettings.copy(showWandTrackingIds = enabled))
        })
    }


    /**
     * Enables or disables dumping of Snap ImageCapture frames to the device filesystem
     * (Pictures/PalletCheckerDebug/). Only applies to Snap mode.
     */
    fun updateDumpSnapImagesToFilesystem(enabled: Boolean) {
        updateSettingsWith({
            it.copy(debugSettings = it.debugSettings.copy(dumpSnapImagesToFilesystem = enabled))
        })
    }
}
