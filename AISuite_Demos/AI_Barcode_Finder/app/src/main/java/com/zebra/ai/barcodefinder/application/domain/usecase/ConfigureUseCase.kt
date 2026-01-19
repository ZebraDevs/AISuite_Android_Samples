package com.zebra.ai.barcodefinder.application.domain.usecase

import android.graphics.Bitmap
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.zebra.ai.barcodefinder.application.data.source.repository.ActionableBarcodeRepository
import com.zebra.ai.barcodefinder.application.domain.enums.ActionType
import com.zebra.ai.barcodefinder.application.domain.model.ActionableBarcode
import com.zebra.ai.barcodefinder.application.domain.model.BarcodeOverlayItem
import com.zebra.ai.barcodefinder.sdkcoordinator.EntityTrackerCoordinator
import com.zebra.ai.barcodefinder.sdkcoordinator.enums.CoordinatorState
import com.zebra.ai.barcodefinder.application.data.source.repository.SettingsRepository
import com.zebra.ai.barcodefinder.application.domain.services.barcodeprocessing.BaseBarcodeProcessor
import com.zebra.ai.barcodefinder.application.domain.services.barcodeprocessing.processors.ConfigureBarcodeProcessor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/**
 * Use case for managing barcode configurations and UI-related operations.
 *
 * Responsibilities:
 * - Applying configurations.
 * - Clearing, reloading, and observing configured barcodes.
 * - Managing individual barcodes in the configuration.
 * - Processing barcode overlays for the UI.
 * - Retrieving icons for action types.
 */
class ConfigureUseCase(
    private val repository: ActionableBarcodeRepository,
    private val entityTrackerCoordinator: EntityTrackerCoordinator,
    private val settingsRepository: SettingsRepository,
) {
    val barcodeProcessor: BaseBarcodeProcessor = ConfigureBarcodeProcessor(
        entityTrackerCoordinator,
        repository,
        settingsRepository,
    )

    /**
     * Applies the current barcode configurations.
     */
    fun applyConfigurations() {
        repository.applyConfigurations()
    }

    /**
     * Clears all configured actionable barcodes.
     */
    fun clearAllConfiguredBarcodes() {
        repository.clearAllConfiguredActionableBarcodes()
    }

    /**
     * Processes barcode overlays for the UI and returns a [kotlinx.coroutines.flow.Flow] of [com.zebra.ai.barcodefinder.application.domain.model.BarcodeOverlayItem].
     */
    fun processBarcode(): Flow<List<BarcodeOverlayItem>> {
        return barcodeProcessor.getProcessingFlow().map { it.overlayItems }
    }

    /**
     * Retrieves the icon associated with a specific [com.zebra.ai.barcodefinder.application.domain.enums.ActionType].
     *
     * @param actionType The action type for which the icon is to be retrieved.
     * @return The icon as a [android.graphics.Bitmap], or null if no icon is available.
     */
    fun getIconForActionType(actionType: ActionType): Bitmap? {
        return repository.getIconForActionType(actionType)
    }

    /**
     * Observes the list of configured actionable barcodes as a [kotlinx.coroutines.flow.StateFlow].
     */
    fun observeConfiguredBarcodes(): StateFlow<List<ActionableBarcode>> {
        return repository.getLiveConfiguredBarcodes()
    }

    /**
     * Reloads the configured actionable barcodes from the repository.
     */
    fun reloadConfiguredBarcodes() {
        repository.loadConfiguredActionableBarcodes()
    }

    /**
     * Removes an actionable barcode from the configuration list.
     *
     * @param barcode The barcode to remove.
     */
    fun removeBarcodeFromConfigList(barcode: ActionableBarcode) {
        repository.removeActionableBarcodeFromConfigList(barcode)
    }

    /**
     * Updates a barcode in the configuration list.
     *
     * @param barcode The barcode to update.
     */
    fun updateBarcode(barcode: ActionableBarcode) {
        repository.updateActionableBarcodeInConfigList(barcode)
    }

    fun initializeEntityTrackerCoordinator() {
        settingsRepository.loadSettings()
        val appSettings = settingsRepository.settings.value

        entityTrackerCoordinator.configureSdk(appSettings, reset = false)
    }

    fun updateEntityTrackerCoordinator() {
        settingsRepository.loadSettings()
        val appSettings = settingsRepository.settings.value

        entityTrackerCoordinator.configureSdk(appSettings, reset = true)
    }



    fun observeEntityTrackerCoordinatorState(): StateFlow<CoordinatorState> {
        return entityTrackerCoordinator.coordinatorState
    }

    fun bindCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        entityTrackerCoordinator.bindCameraToLifecycle(lifecycleOwner, previewView)
    }

    /**
     * Unbinds all camera use cases.
     */
    fun unbindCamera() {
        entityTrackerCoordinator.unbindCamera()
    }

    fun getEntityTrackerCoordinatorPreview(): Preview? {
        return entityTrackerCoordinator.getPreview()
    }


}