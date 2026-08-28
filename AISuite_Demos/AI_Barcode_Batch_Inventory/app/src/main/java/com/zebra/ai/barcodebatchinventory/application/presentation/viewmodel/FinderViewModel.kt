// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodebatchinventory.application.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.zebra.ai.barcodebatchinventory.application.data.source.repository.CapturedBarcodeRepository
import com.zebra.ai.barcodebatchinventory.application.data.source.repository.SettingsRepository
import com.zebra.ai.barcodebatchinventory.application.data.source.storage.SettingsJsonStorage
import com.zebra.ai.barcodebatchinventory.application.domain.services.feedback.BarcodeScanSessionManager
import com.zebra.ai.barcodebatchinventory.application.domain.model.BarcodeOverlayItem
import com.zebra.ai.barcodebatchinventory.sdkcoordinator.EntityTrackerCoordinator
import com.zebra.ai.barcodebatchinventory.application.domain.usecase.FinderUseCase
import com.zebra.ai.barcodebatchinventory.application.presentation.model.EntityTrackerInitState
import com.zebra.ai.barcodebatchinventory.sdkcoordinator.enums.CoordinatorState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.camera.core.ZoomState
import com.zebra.ai.barcodebatchinventory.application.data.services.SystemFeedbackService
import kotlin.coroutines.cancellation.CancellationException

/**
 * ViewModel for tracking barcode entities and managing UI state for batch inventory scanning.
 * Handles camera lifecycle, barcode selection, overlay management, scan results, and SDK settings.
 *
 * @constructor Creates a FinderViewModel with the given application context.
 * @param application The application context
 */
class FinderViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "BatchScannerViewModel"

    companion object {
        private const val BATCH_CAPTURE_WINDOW_MS = 5000L
    }

    private val _overlayItems = MutableSharedFlow<List<BarcodeOverlayItem>>(replay = 1)
    val overlayItems: SharedFlow<List<BarcodeOverlayItem>> = _overlayItems.asSharedFlow()

    private val _entityTrackerInitState = MutableStateFlow<EntityTrackerInitState>(EntityTrackerInitState())
    val entityTrackerInitState: StateFlow<EntityTrackerInitState> = _entityTrackerInitState.asStateFlow()

    private val entityTrackerCoordinator: EntityTrackerCoordinator = EntityTrackerCoordinator.getInstance(application)

    //TODO :: Implement proper DI
    val settingsJsonStorage = SettingsJsonStorage(application)
    val settingsRepository = SettingsRepository.getInstance(settingsJsonStorage)
    val capturedBarcodeRepository = CapturedBarcodeRepository.getInstance(application)

    val feedbackEngine = SystemFeedbackService(application)

    private val barcodeScanSessionManager = BarcodeScanSessionManager.getInstance(feedbackEngine)

    // Finder UseCase creation
    val finderUseCase = FinderUseCase(capturedBarcodeRepository, entityTrackerCoordinator, settingsRepository, barcodeScanSessionManager)

    private val _zoomScale = MutableStateFlow(1f)
    val zoomScale: StateFlow<Float> = _zoomScale.asStateFlow()

    // Initialize the property directly on the declaration line
    val zoomState: StateFlow<ZoomState?> = finderUseCase.observeZoomState()

    // Capture session state management
    private val _isCaptureInProgress = MutableStateFlow(false)
    val isCaptureInProgress: StateFlow<Boolean> = _isCaptureInProgress.asStateFlow()

    private val _captureProgress = MutableStateFlow(0f)
    val captureProgress: StateFlow<Float> = _captureProgress.asStateFlow()

    private val _decodedBarcodeCount = MutableStateFlow(0)
    val decodedBarcodeCount: StateFlow<Int> = _decodedBarcodeCount.asStateFlow()

    private val _showCompletionCheckmark = MutableStateFlow(false)
    val showCompletionCheckmark: StateFlow<Boolean> = _showCompletionCheckmark.asStateFlow()

    private val _captureSuccess = MutableStateFlow(false)
    val captureSuccess: StateFlow<Boolean> = _captureSuccess.asStateFlow()

    private val _isCaptureCompleted = MutableStateFlow(false)
    val isCaptureCompleted: StateFlow<Boolean> = _isCaptureCompleted.asStateFlow()

    private val _shouldNavigateToResults = MutableStateFlow(false)
    val shouldNavigateToResults: StateFlow<Boolean> = _shouldNavigateToResults.asStateFlow()

    private var captureJob: kotlinx.coroutines.Job? = null

    // Temporary storage for barcodes during capture session
    private val captureSessionBarcodes = mutableSetOf<String>()

    // Job for collecting barcodes during capture
    private var barcodeCollectionJob: kotlinx.coroutines.Job? = null

    // Throttle overlay emissions when not capturing to avoid UI lag
    private var lastOverlayEmitTime = 0L
    private val IDLE_OVERLAY_INTERVAL_MS = 200L  // ~5fps when idle

    /**
     * Initializes the ViewModel and sets up state observation for UI and barcode tracking.
     */
    init {
        Log.d(TAG, "FinderViewModel initialized for batch-only flow; configure/action UI is removed")
        observerEntityTrackerInitState()
        startBarcodeProcessing()
    }

    /**
     * Observes decoded barcodes during capture session and stores them temporarily
     * This runs only during the active capture session
     */
    private fun startCollectingBarcodes() {
        // Cancel any existing collection job
        barcodeCollectionJob?.cancel()

        barcodeCollectionJob = viewModelScope.launch {
            // Skip the first (replayed) emission to avoid counting barcodes from the previous session
            var firstEmission = true
            overlayItems.collect { items ->
                if (firstEmission) {
                    firstEmission = false
                    return@collect
                }
                // Only collect barcodes if capture is in progress
                if (_isCaptureInProgress.value) {
                    items.forEach { item ->
                        item.barcodeData?.let { barcodeData ->
                            if (barcodeData.isNotEmpty()) {
                                captureSessionBarcodes.add(barcodeData)
                                _decodedBarcodeCount.value = captureSessionBarcodes.size
                                Log.d(TAG, "Collected barcode during capture (Total: ${captureSessionBarcodes.size})")
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Stops collecting barcodes
     */
    private fun stopCollectingBarcodes() {
        barcodeCollectionJob?.cancel()
        barcodeCollectionJob = null
    }

    // The UI will call this when it becomes visible/active
    fun bindScanSessionToLifecycle() {
        Log.d(TAG, "bindToLifecycle() called")
        finderUseCase.bindScanSessionToLifecycle()
    }

    // The UI will call this when it goes to the background or is destroyed
    fun unbindScanSessionFromLifecycle() {
        Log.d(
            TAG,
            "BatchLifecycle: unbind scan session captureInProgress=${_isCaptureInProgress.value} " +
                    "repoCount=${capturedBarcodeRepository.getCapturedBarcodes().size}"
        )

        // Cancel any running capture session so Phase 2 stops immediately on onPause
        captureJob?.cancel()
        captureJob = null
        stopCollectingBarcodes()
        _isCaptureInProgress.value = false

        finderUseCase.unbindScanSessionFromLifecycle()
        finderUseCase.unbindCamera()
    }

    fun resetScanSession(){
        Log.d(TAG, "BatchFeedback: reset scan session")
        finderUseCase.restScanSession()
    }

    fun clearOverlayItems() {
        val emitOk = _overlayItems.tryEmit(emptyList())
        Log.d(TAG, "BatchOverlay: cleared replayed overlay items emitOk=$emitOk")
    }

    fun updateZoomScale(scale: Float) {
        Log.d(TAG, "updateZoomScale() called with scale: $scale")
        val clampedScale = zoomState.value?.let { scale.coerceIn(it.minZoomRatio, it.maxZoomRatio) } ?: scale

        // Update the UI zoom scale (smooth animation handled in Composable)
        _zoomScale.value = clampedScale

        // Update the hardware zoom asynchronously to avoid blocking the UI
        viewModelScope.launch {
            finderUseCase.setZoomRatio(clampedScale)
        }
    }

    /**
     * Resumes scanning session and shows green ticks on previously saved barcodes.
     * Called when FinderScreen loads to check for previously saved barcodes.
     */
    fun resumeScanning() {
        Log.d(TAG, "resumeScanning() called")

        // Reset decoded barcode count for new session
        _decodedBarcodeCount.value = 0

        // Check if there are any previously saved barcodes
        val savedBarcodes = capturedBarcodeRepository.getCapturedBarcodes()
        val hasSavedBarcodes = savedBarcodes.isNotEmpty()
        Log.d(
            TAG,
            "BatchResume: source=finder savedTotal=${savedBarcodes.size} " +
                    "savedUnique=${HashSet(savedBarcodes).size} showCheckmarks=$hasSavedBarcodes"
        )

        if (hasSavedBarcodes) {
            // Show checkmarks on previously saved barcodes
            finderUseCase.setShowCheckmarksOnBarcodes(true)
            _isCaptureCompleted.value = true
            Log.d(TAG, "Showing checkmarks for ${savedBarcodes.size} previously saved barcodes")
        } else {
            // No saved barcodes, start fresh
            finderUseCase.setShowCheckmarksOnBarcodes(false)
            _isCaptureCompleted.value = false
            Log.d(TAG, "No previously saved barcodes found")
        }

        // Always keep decoding active so the pipeline is warm when capture starts
        finderUseCase.startDecoding()
        finderUseCase.enableContinuousAutoFocus()
        Log.d(TAG, "Decoding started — pipeline warm for capture")
    }

    /**
     * Called when user clicks "Resume Scanning" button from results screen.
     * Enables checkmarks so previously saved barcodes will show green ticks when detected.
     */
    fun resumeScanningFromResults() {
        Log.d(TAG, "resumeScanningFromResults() called")

        // Reset decoded barcode count for new session
        _decodedBarcodeCount.value = 0

        // Check if there are any previously saved barcodes
        val savedBarcodes = capturedBarcodeRepository.getCapturedBarcodes()
        val hasSavedBarcodes = savedBarcodes.isNotEmpty()

        Log.d(
            TAG,
            "BatchResume: source=results savedTotal=${savedBarcodes.size} " +
                    "savedUnique=${HashSet(savedBarcodes).size} showCheckmarks=$hasSavedBarcodes"
        )

        if (hasSavedBarcodes) {
            // Enable checkmarks flag so green ticks will appear when barcodes are detected
            finderUseCase.setShowCheckmarksOnBarcodes(true)
            _isCaptureCompleted.value = true
            Log.d(TAG, "Enabled checkmarks for ${savedBarcodes.size} previously saved barcodes")
        } else {
            // No saved barcodes, start fresh
            finderUseCase.setShowCheckmarksOnBarcodes(false)
            _isCaptureCompleted.value = false
            Log.d(TAG, "No previously saved barcodes found")
        }

        // Always keep decoding active so the pipeline is warm when capture starts
        finderUseCase.startDecoding()
        finderUseCase.enableContinuousAutoFocus()
        Log.d(TAG, "Decoding started — pipeline warm for capture")
    }

    /**
     * Starts a capture session (called when capture button is clicked).
     *
     * Collects all decoded barcodes during a 5-second capture window.
     * Progress bar advances linearly over the capture duration.
     */
    fun startCaptureSession() {
        Log.d(TAG, "startCaptureSession() called")

        captureJob?.cancel()
        captureJob = null
        Log.d(TAG, "Previous captureJob cancelled, starting new session")

        startCaptureSessionInternal()
    }

    private fun startCaptureSessionInternal() {

        // Clear temporary barcode collection for this capture session
        captureSessionBarcodes.clear()

        // Reset states
        _captureSuccess.value = false
        _isCaptureInProgress.value = true
        _captureProgress.value = 0f
        _decodedBarcodeCount.value = 0
        _showCompletionCheckmark.value = false
        _isCaptureCompleted.value = false
        _shouldNavigateToResults.value = false

        // Keep checkmarks on previously saved barcodes visible during capture
        finderUseCase.setShowCheckmarksOnBarcodes(true)

        finderUseCase.resetDetectedBarcodeCount()

        // Ensure decoding is active (pipeline should already be warm)
        finderUseCase.startDecoding()

        // Refocus center on every scan press — prior focus lock can be stale on 2nd+ sessions
        finderUseCase.triggerAutoFocus()

        Log.d(
            TAG,
            "BatchCaptureStart: tempStateCleared repoCountBefore=${capturedBarcodeRepository.getCapturedBarcodes().size}"
        )

        // Start collecting decoded barcodes from overlay items
        startCollectingBarcodes()

        captureJob = viewModelScope.launch {
            val updateIntervalMs = 200L
            val captureWindowMs = BATCH_CAPTURE_WINDOW_MS  // Fixed 5-second batch capture window
            var elapsed = 0L

            Log.d(TAG, "Capture: collecting barcodes for ${captureWindowMs}ms...")

            // Collect all decoded barcodes during the 5-second capture window
            while (isActive && elapsed < captureWindowMs) {
                kotlinx.coroutines.delay(updateIntervalMs)
                elapsed += updateIntervalMs

                val collected = captureSessionBarcodes.size
                _decodedBarcodeCount.value = collected
                _captureProgress.value = (elapsed.toFloat() / captureWindowMs).coerceAtMost(1f)

                val currentDecoded = finderUseCase.getDecodedBarcodeCount()
                val currentDetected = finderUseCase.getDetectedBarcodeCount()

                Log.d(TAG, "Capture [${elapsed}ms]: collected=$collected" +
                        " frame=$currentDecoded/$currentDetected")
            }

            // Fill progress bar on completion
            _captureProgress.value = 1f

            val collectedBarcodes = captureSessionBarcodes.toSet()
            val repoCountBeforeSave = capturedBarcodeRepository.getCapturedBarcodes().size
            Log.d(
                TAG,
                "BatchCaptureComplete: windowMs=$captureWindowMs uniqueCaptured=${collectedBarcodes.size} " +
                        "repoCountBeforeSave=$repoCountBeforeSave"
            )

            // Stop collecting barcodes from overlay
            stopCollectingBarcodes()

            // Capture is successful when at least one barcode was decoded
            _captureSuccess.value = collectedBarcodes.isNotEmpty()
            Log.d(TAG, "Capture success: ${_captureSuccess.value} (collected=${collectedBarcodes.size})")

            _isCaptureInProgress.value = false
            _captureProgress.value = 1f

            // Trigger audio and haptic feedback after timeout completes
            finderUseCase.triggerFeedback()

            // Save all collected barcodes to the repository (this will increment quantities)
            // Use the snapshot to ensure we save what was collected, regardless of current visibility
            collectedBarcodes.forEach { barcodeData ->
                capturedBarcodeRepository.addCapturedBarcode(barcodeData)
            }
            Log.d(
                TAG,
                "BatchCaptureSaved: savedCount=${collectedBarcodes.size} " +
                        "repoCountBefore=$repoCountBeforeSave " +
                        "repoCountAfter=${capturedBarcodeRepository.getCapturedBarcodes().size}"
            )

            // Mark capture as completed first - this will trigger checkmarks on barcodes
            _isCaptureCompleted.value = true

            // Enable checkmarks on all decoded barcodes
            // Keep decoding ACTIVE so the icons continue tracking with device movement
            finderUseCase.setShowCheckmarksOnBarcodes(true)

            // Note: We DON'T stop decoding here - we keep it active so the overlay
            // items continue to update and track with device movement
            // The barcodes won't be added again to the repository because they're already there

            // Show center checkmark briefly
            _showCompletionCheckmark.value = true

            // Wait 1 second then hide the center checkmark
            // User must manually click "View Results" to navigate
            kotlinx.coroutines.delay(1000L)
            _showCompletionCheckmark.value = false
            // Removed automatic navigation - user clicks View Results button instead

            // Release AF lock so continuous picture AF resumes while the user aims for the next scan.
            finderUseCase.enableContinuousAutoFocus()
        }
    }

    /**
     * Stops barcode decoding and cancels capture session.
     */
    fun stopCaptureSession() {
        _captureSuccess.value = false
        Log.d(TAG, "stopCaptureSession() called")
        captureJob?.cancel()
        stopCollectingBarcodes()
        finderUseCase.stopDecoding()
        finderUseCase.enableContinuousAutoFocus()
        finderUseCase.setShowCheckmarksOnBarcodes(false)
        _isCaptureInProgress.value = false
        _captureProgress.value = 0f
        _showCompletionCheckmark.value = false
        _isCaptureCompleted.value = false
    }

    /**
     * Resets the navigation flag after navigating to results.
     */
    fun resetNavigationFlag() {
        _shouldNavigateToResults.value = false
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared() called")
    }

    /**
     * Binds the camera to the lifecycle and preview view.
     * @param lifecycleOwner The lifecycle owner
     * @param previewView The camera preview view
     */
    fun bindCameraToLifecycle(lifecycleOwner: LifecycleOwner, previewView: PreviewView,initialZoom: Float = 1.0f) {
        Log.d(TAG, "bindCameraToLifecycle() called for lifecycle owner: $lifecycleOwner")
        finderUseCase.bindCamera(lifecycleOwner = lifecycleOwner, previewView = previewView, initialZoom = initialZoom)
    }

    fun unbindCameraToLifecycle(){
        finderUseCase.unbindCamera()
    }

    private var vmEmitCount = 0L

    private fun startBarcodeProcessing() {
        Log.d(TAG, "startBarcodeProcessing() called")
        viewModelScope.launch {
            try {
                finderUseCase.processBarcode().collect { result ->
                    vmEmitCount++

                    // Throttle overlay updates when not capturing to reduce UI recomposition
                    if (!_isCaptureInProgress.value) {
                        val now = System.currentTimeMillis()
                        if (now - lastOverlayEmitTime < IDLE_OVERLAY_INTERVAL_MS) return@collect
                        lastOverlayEmitTime = now
                    }

                    val emitOk = _overlayItems.tryEmit(result.overlayItems)
                    Log.i("AppPerfMon", "VMEmit: app=BatchInventory" +
                            " emit#=$vmEmitCount" +
                            " overlayCount=${result.overlayItems.size}" +
                            " emitOk=$emitOk" +
                            " thread=${Thread.currentThread().name}")
                    if (!emitOk) {
                        _overlayItems.emit(result.overlayItems)
                    }
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "startBarcodeProcessing coroutine was cancelled as expected.")
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "An unexpected error occurred in startBarcodeProcessing", e)
            }
        }
    }

    /**
     * observe the EntityTrackerCoordinator state in EntityTrackerCoordinator and update the EntityTrackerInitState
     */
    fun observerEntityTrackerInitState() {
        Log.d(TAG, "observerEntityTrackerInitState() called")
        finderUseCase.observeEntityTrackerCoordinatorState()
            .map { coordinatorState ->
                Log.d(TAG, "CoordinatorState observed: $coordinatorState")
                if(coordinatorState == CoordinatorState.COORDINATOR_READY) {
                    _entityTrackerInitState.update { it.copy(isInitialized = true) }
                    Log.d(TAG, "Entity tracker init state is updated to ready")
                } else {
                    _entityTrackerInitState.update { it.copy(isInitialized = false) }
                    Log.d(TAG, "Entity tracker init state is updated to not ready")
                }
            }
            .launchIn(viewModelScope)
    }
}
