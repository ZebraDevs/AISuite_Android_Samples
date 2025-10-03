// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.zebra.ai.barcodefinder.domain.EntityTrackerFacade
import com.zebra.ai.barcodefinder.domain.model.ActionableBarcode
import com.zebra.ai.barcodefinder.domain.model.BarcodeOverlayItem
import com.zebra.ai.barcodefinder.presentation.model.EntityTrackerUiState
import com.zebra.ai.barcodefinder.domain.Finder
import com.zebra.ai.barcodefinder.domain.Settings
import com.zebra.ai.barcodefinder.domain.enums.ProcessorType
import com.zebra.ai.barcodefinder.domain.model.AppSettings
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


/**
 * ViewModel for tracking barcode entities and managing UI state for the EntityTracker screen.
 * Handles camera lifecycle, barcode selection, overlay management, scan results, and SDK settings.
 *
 * @constructor Creates an EntityTrackerViewModel with the given application context.
 * @param application The application context
 */
class FinderViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "EntityTrackerViewModel"
    private var finder: Finder? = null

    private val _uiState = MutableStateFlow(EntityTrackerUiState())
    val uiState: StateFlow<EntityTrackerUiState> = _uiState.asStateFlow()

    private val _overlayItems = MutableSharedFlow<List<BarcodeOverlayItem>>(replay = 1)
    val overlayItems: SharedFlow<List<BarcodeOverlayItem>> = _overlayItems.asSharedFlow()

    private val settingsUseCase = Settings(application)

    private val _errorFlowInference = MutableSharedFlow<String>()
    val errorFlowInference: SharedFlow<String> = _errorFlowInference.asSharedFlow()

    private val entityTrackerFacade: EntityTrackerFacade = EntityTrackerFacade.getInstance(application)



    /**
     * Initializes the ViewModel and sets up state observation for UI and barcode tracking.
     */
    init {
        try{
            finder = Finder(application)
        }catch (e: Exception){
            if(settingsUseCase.getSettings().value.processorType == ProcessorType.DSP) {
                updateProcessorType(ProcessorType.AUTO)
                finder = Finder(application)
            } else {
                throw e
            }
        }
        // Setup combined UI state updates
        observeErrorState()
        setupUiStateUpdates()
        observeOverlayItems()
    }

    /**
     * Binds the camera to the lifecycle and preview view.
     * @param lifecycleOwner The lifecycle owner
     * @param previewView The camera preview view
     */
    fun bindCameraToLifecycle(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        finder?.bindCameraToLifecycle(lifecycleOwner, previewView)
    }

    /**
     * Starts camera analysis and binds the camera to the lifecycle and preview view.
     * @param lifecycleOwner The lifecycle ownerclass EntityTrackerUiState {
}
     * @param previewView The camera preview view
     */
    fun startAnalyzing(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        finder?.startAnalyzing(lifecycleOwner, previewView)
    }

    /**
     * Selects a barcode for dialog display and configuration.
     * @param barcode The barcode to select
     */
    fun selectBarcode(barcode: ActionableBarcode) {
        finder?.selectBarcode(barcode)
    }

    /**
     * Dismisses the dialog and clears the selected barcode.
     */
    fun dismissDialog() {
        finder?.dismissDialog()
    }

    /**
     * Updates the camera permission state in the repository.
     * @param hasPermission Whether camera permission is granted
     */
    fun updatePermissionState(hasPermission: Boolean) {
        finder?.updatePermissionState(hasPermission)
    }

    /**
     * Clears all overlay items from the camera preview.
     */
    fun clearOverlayItems() {
        finder?.clearOverlayItems()
    }

    /**
     * Handles quantity pickup action for a barcode and updates user data.
     * @param barcode The barcode to update
     * @param quantityPicked The quantity picked
     * @param replenishStock Whether stock was replenished
     */
    fun handleQuantityPickup(barcode: ActionableBarcode, quantityPicked: Int, replenishStock: Boolean = false) {
        viewModelScope.launch {
            finder?.handleQuantityPickup(barcode, quantityPicked, replenishStock)
        }
    }

    /**
     * Handles product recall action for a barcode.
     * @param barcode The barcode to recall
     */
    fun handleProductRecall(barcode: ActionableBarcode) {
        viewModelScope.launch {
            finder?.handleProductRecall(barcode)
        }
    }


    /**
     * Handles confirm pickup action for a barcode.
     * @param barcode The barcode to confirm
     */
    fun handleConfirmPickup(barcode: ActionableBarcode) {
        viewModelScope.launch {
            finder?.handleConfirmPickup(barcode)
        }
    }

    /**
     * Clears all completed barcode scan results.
     */
    fun clearBarcodeResults() {
        finder?.clearBarcodeResults()
    }
    /**
     * Applies current settings to the SDK and reinitializes components if needed.
     */
    fun applySettingsToSdk() {
        finder?.applySettingsToSdk()
    }

    /**
     * Updates the processor type and syncs with the AI Vision SDK.
     */
    fun updateProcessorType(processorType: ProcessorType) {
        updateSettingsWith { it.copy(processorType = processorType) }
    }

    /**
     * Updates settings using the provided update function and syncs with repository.
     * @param update Function to update AppSettings
     */
    private fun updateSettingsWith(update: (AppSettings) -> AppSettings) {
        settingsUseCase.updateSettings(update)
    }

    /**
     * Sets up the combined UI state by observing all individual state flows.
     */
    private fun setupUiStateUpdates() {
        viewModelScope.launch {
            finder?.uiStateFlow?.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    private fun observeErrorState() {
        viewModelScope.launch {
            entityTrackerFacade.errorState.collect { exception ->
                exception?.let {
                    Log.d(TAG, "Error observed: ${it.message}")

                    // Process the exception (extract root cause, take action, etc.)
                    processException(it)
                }
            }
        }
    }

    private fun processException(exception: Throwable) {
        val rootCause = getRootCause(exception)
        val rootCauseMessage = rootCause?.message

        // Check if the error requires switching to AUTO processor type
        if (rootCauseMessage?.contains("Given runtimes are not available", ignoreCase = true) == true) {
            Log.d(TAG, "Switching processor type to AUTO due to DSP error: $rootCauseMessage")
            updateProcessorType(ProcessorType.AUTO)
            applySettingsToSdk()

            viewModelScope.launch {
                _errorFlowInference.emit(rootCauseMessage ?: "An unexpected error occurred")
            }
        }
    }

    private fun getRootCause(throwable: Throwable?): Throwable? {
        var rootCause = throwable
        while (rootCause?.cause != null && rootCause.cause != rootCause) {
            rootCause = rootCause.cause
        }
        return rootCause
    }

    private fun observeOverlayItems() {
        viewModelScope.launch {
            finder?.overlayItemsFlow?.collect { items ->
                _overlayItems.emit(items)
            }
        }
    }
}
