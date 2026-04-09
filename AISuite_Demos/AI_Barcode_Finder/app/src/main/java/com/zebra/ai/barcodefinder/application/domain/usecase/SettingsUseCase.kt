package com.zebra.ai.barcodefinder.application.domain.usecase

import android.util.Log
import com.zebra.ai.barcodefinder.application.domain.model.SettingsApplicationResult
import com.zebra.ai.barcodefinder.sdkcoordinator.model.AppSettings
import com.zebra.ai.barcodefinder.sdkcoordinator.model.BarcodeSymbology
import com.zebra.ai.barcodefinder.application.data.source.repository.SettingsRepository
import com.zebra.ai.barcodefinder.sdkcoordinator.EntityTrackerCoordinator
import com.zebra.ai.barcodefinder.sdkcoordinator.enums.CoordinatorState
import com.zebra.ai.barcodefinder.sdkcoordinator.enums.ProcessorType
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/**
 * Use case for managing application settings, including observing, updating, resetting, and modifying symbologies.
 *
 * @param settingsRepository The repository for managing settings.
 */
private const val TAG = "SettingsUseCase"

class SettingsUseCase(
    private val entityTrackerCoordinator: EntityTrackerCoordinator,
    private val settingsRepository: SettingsRepository
) {

    fun loadSettings(): AppSettings {
        return settingsRepository.loadSettings()
    }

    /**
     * Observes the current settings as a [kotlinx.coroutines.flow.StateFlow].
     *
     * @return A [kotlinx.coroutines.flow.StateFlow] of [AppSettings].
     */
    fun observeSettings(): StateFlow<AppSettings> {
        return settingsRepository.settings
    }

    /**
     * Resets the application settings to their default values.
     */
    fun resetSettings() {
        settingsRepository.resetSettings()
    }

    /**
     * Updates the application settings by applying the provided update function.
     *
     * @param update A function that takes the current [AppSettings] and returns the updated [AppSettings].
     */
    fun updateSettings(update: (AppSettings) -> AppSettings) {
        val currentSettings = settingsRepository.loadSettings()
        val updatedSettings = update(currentSettings)
        settingsRepository.saveSettings(updatedSettings)
    }

    /**
     * Applies settings to the EntityTrackerCoordinator and returns the result.
     *
     * @return SettingsApplicationResult indicating success or failure with details
     */
    suspend fun applySettingsToCoordinator(): SettingsApplicationResult {
        settingsRepository.loadSettings()
        return applyDraftSettingsToCoordinator(settingsRepository.settings.value)
    }

    /**
     * Triggers async SDK configuration and returns immediately (fire-and-forget).
     * Snapshots the last known good settings for rollback by [observeConfigurationResult].
     * Call [observeConfigurationResult] afterwards to receive the outcome.
     *
     * @param draftSettings The new settings to apply.
    */
    suspend fun applyDraftSettingsToCoordinator(draftSettings: AppSettings): SettingsApplicationResult {
        // Snapshot last good settings BEFORE we start, for rollback if needed.
        // Draft settings are intentionally NOT persisted here — the repository
        // keeps the last successfully applied (known-compatible) settings so that
        // if the ViewModel scope dies mid-flight, reentry uses safe settings.
        lastAppliedSettings = settingsRepository.settings.value
        try {
            entityTrackerCoordinator.configureSdk(draftSettings, reset = true)
        } catch (e: Exception) {
            // configureSdk is designed to be safe (sets ERROR_SDK state internally),
            // but we guard here against any unexpected synchronous failure so the
            // caller is never left with an unhandled crash.
            Log.e(TAG, "Unexpected error triggering SDK configuration: ${e.message}", e)
        }

        return observeConfigurationResult(draftSettings)
    }

    /**
     * Suspends until the coordinator reaches a terminal state, then translates it
     * into a [SettingsApplicationResult] for the presentation layer.
     * Triggers a best-effort rollback on any error state before returning.
     *
     * Must be called after [triggerDraftSettingsConfiguration].
     *
     * @param draftSettings The settings that were applied (committed to repository on success).
     * @return [SettingsApplicationResult] translated from the terminal coordinator state.
     */
    suspend fun observeConfigurationResult(draftSettings: AppSettings): SettingsApplicationResult {
        val terminalState = entityTrackerCoordinator.coordinatorState
            .first { it.isTerminal() }

        return when (terminalState) {
            CoordinatorState.COORDINATOR_READY -> {
                // Only persist draft settings after the SDK confirms successful
                // configuration. This is the single commit point — if the
                // coroutine was cancelled before reaching here, the repository
                // still holds the last known-compatible settings.
                settingsRepository.saveSettings(draftSettings)
                Log.d(TAG, "Settings applied and committed successfully")
                SettingsApplicationResult.Success
            }

            CoordinatorState.ERROR_UNSUPPORTED_PROCESSOR -> {
                Log.e(TAG, "Configuration failed: unsupported processor type — retrying with AUTO")
                // Apply the draft as-is but with AUTO processor. All other settings
                // (model input, resolution, symbologies, feedback) are preserved.
                val fallback = draftSettings.copy(processorType = ProcessorType.AUTO)
                try {
                    entityTrackerCoordinator.configureSdk(fallback, reset = true)
                    // Wait until the state leaves the current terminal so we don't
                    // immediately re-read ERROR_UNSUPPORTED_PROCESSOR.
                    entityTrackerCoordinator.coordinatorState.first { !it.isTerminal() }
                    // Now await the result of the AUTO-processor pipeline.
                    val fallbackState = entityTrackerCoordinator.coordinatorState.first { it.isTerminal() }
                    if (fallbackState == CoordinatorState.COORDINATOR_READY) {
                        settingsRepository.saveSettings(fallback)
                        Log.d(TAG, "AUTO fallback succeeded — settings saved")
                    } else {
                        Log.e(TAG, "AUTO fallback also failed: $fallbackState")
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "AUTO fallback error", ex)
                }
                SettingsApplicationResult.Error(
                    null,
                    "Selected inference type is not supported on this device. Switching to Auto-select for optimal performance.",
                    isRecoverable = true
                )
            }

            CoordinatorState.ERROR_BARCODE_DECODER -> {
                Log.e(TAG, "Configuration failed: barcode decoder error")
                rollbackToPreviousSettings()
                SettingsApplicationResult.Error(
                    null,
                    "Failed to initialize barcode decoder. Please try again.",
                    isRecoverable = true
                )
            }

            CoordinatorState.ERROR_ENTITY_TRACKER -> {
                Log.e(TAG, "Configuration failed: entity tracker error")
                rollbackToPreviousSettings()
                SettingsApplicationResult.Error(
                    null,
                    "Failed to initialize entity tracker. Please try again.",
                    isRecoverable = true
                )
            }

            CoordinatorState.ERROR_CAMERA -> {
                Log.e(TAG, "Configuration failed: camera error")
                rollbackToPreviousSettings()
                SettingsApplicationResult.Error(
                    null,
                    "Failed to initialize camera. Please check camera permissions.",
                    isRecoverable = false
                )
            }

            CoordinatorState.ERROR_SDK -> {
                Log.e(TAG, "Configuration failed: SDK error")
                rollbackToPreviousSettings()
                SettingsApplicationResult.Error(
                    null,
                    "Failed to initialize AI Vision SDK. Please restart the app.",
                    isRecoverable = false
                )
            }

            CoordinatorState.ERROR_AI_VISION_SDK -> {
                Log.e(TAG, "Configuration failed: AI Vision SDK initialization error")
                rollbackToPreviousSettings()
                SettingsApplicationResult.Error(
                    null,
                    "Failed to initialize AI Vision SDK. Please restart the app.",
                    isRecoverable = false
                )
            }

            CoordinatorState.ERROR_BARCODE_DECODER_SETTINGS -> {
                Log.e(TAG, "Configuration failed: barcode decoder settings error")
                rollbackToPreviousSettings()
                SettingsApplicationResult.Error(
                    null,
                    "Failed to configure barcode decoder settings. Please try different settings.",
                    isRecoverable = true
                )
            }

            CoordinatorState.ERROR_DISPOSE -> {
                Log.e(TAG, "Configuration failed: dispose error")
                rollbackToPreviousSettings()
                SettingsApplicationResult.Error(
                    null,
                    "Failed to reset previous configuration. Please restart the app.",
                    isRecoverable = false
                )
            }

            else -> {
                // Exhaustive when - should never reach here
                Log.e(TAG, "Unexpected terminal state: $terminalState")
                SettingsApplicationResult.Error(
                    null,
                    "Configuration failed with unexpected state: $terminalState",
                    isRecoverable = true
                )
            }
        }
    }

    /**
     * Best-effort rollback to the last known good settings.
     * Failures are swallowed - the original error is already surfaced to the UI.
     */
    private fun rollbackToPreviousSettings() {
        val snapshot = lastAppliedSettings ?: run {
            Log.w(TAG, "No snapshot available for rollback")
            return
        }
        try {
            // Repository already holds the last known-good settings (draft was
            // never persisted), so we only need to reconfigure the SDK.
            entityTrackerCoordinator.configureSdk(snapshot, reset = true)
            Log.d(TAG, "Rollback to previous settings triggered")
        } catch (ex: Exception) {
            Log.e(TAG, "Rollback failed", ex)
        }
    }

    // Holds a snapshot of the last committed settings between trigger and observe calls
    private var lastAppliedSettings: AppSettings? = null

    /**
     * Updates the state of a specific barcode symbology (enabled/disabled).
     *
     * @param symbologyName The name of the symbology to update.
     * @param enabled Whether the symbology should be enabled or disabled.
     * @return The updated [BarcodeSymbology] object.
     */
    fun updateSymbology(symbologyName: String, enabled: Boolean): BarcodeSymbology {
        return updateSymbology(settingsRepository.settings.value.barcodeSymbology, symbologyName, enabled)
    }

    fun updateSymbology(
        currentSymbology: BarcodeSymbology,
        symbologyName: String,
        enabled: Boolean
    ): BarcodeSymbology {
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
}