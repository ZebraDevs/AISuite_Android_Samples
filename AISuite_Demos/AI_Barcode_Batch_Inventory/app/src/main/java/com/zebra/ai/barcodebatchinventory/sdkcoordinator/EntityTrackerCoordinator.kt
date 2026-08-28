// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodebatchinventory.sdkcoordinator

import android.app.Application
import android.hardware.camera2.CaptureRequest
import android.util.Log
import android.util.Range
import android.util.Size
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.ZoomState
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.zebra.ai.barcodebatchinventory.sdkcoordinator.enums.CoordinatorState
import com.zebra.ai.barcodebatchinventory.sdkcoordinator.exceptions.BarcodeDecoderInitializationException
import com.zebra.ai.barcodebatchinventory.sdkcoordinator.exceptions.CameraInitializationException
import com.zebra.ai.barcodebatchinventory.sdkcoordinator.exceptions.EntityTrackerInitializationException
import com.zebra.ai.barcodebatchinventory.sdkcoordinator.exceptions.SDKInitializationException
import com.zebra.ai.barcodebatchinventory.sdkcoordinator.exceptions.UnsupportedProcessorException
import com.zebra.ai.barcodebatchinventory.sdkcoordinator.model.AppSettings
import com.zebra.ai.barcodebatchinventory.sdkcoordinator.support.BarcodeDecoderSettingsBuilder
import com.zebra.ai.barcodebatchinventory.sdkcoordinator.support.PermissionHandler
import com.zebra.ai.vision.analyzer.tracking.EntityTrackerAnalyzer
import com.zebra.ai.vision.detector.AIVisionSDK
import com.zebra.ai.vision.detector.BarcodeDecoder
import com.zebra.ai.vision.entity.BarcodeEntity
import com.zebra.ai.vision.entity.Entity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Facade for managing entity tracking and camera operations within the application.
 *
 * Responsibilities:
 * - Manages the lifecycle and initialization of camera components for preview and analysis.
 * - Sets up and configures AI Vision SDK and barcode decoding functionalities.
 * - Handles camera permissions and binds camera use cases to lifecycle owners.
 * - Provides a reactive flow of entity tracking results for UI components to observe.
 * - Manages repository state transitions and updates based on initialization and permissions.
 *
 * Usage:
 * - Singleton pattern: use getInstance(application) to obtain the facade.
 * - Initialize SDK and camera components using configureSdk method.
 * - Bind camera use cases to lifecycle owners using bindCameraToLifecycle.
 * - Observe entity tracking results via observeEntityTrackingResults.
 * - Dispose resources using dispose when no longer needed.
 * - Ensure camera permissions are managed via PermissionHandler.
 */
class EntityTrackerCoordinator private constructor(private val application: Application) {
    private val TAG = "EntityTrackerCoordinator"
    private val PERF_TAG = "AppPerfMon"
    private var callbackCount = 0L
    private var firstCallbackTimeMs = 0L

    companion object {
        @Volatile
        private var INSTANCE: EntityTrackerCoordinator? = null

        /**
         * Returns the singleton instance of EntityTrackerCoordinator.
         */
        fun getInstance(application: Application): EntityTrackerCoordinator {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: try {
                    EntityTrackerCoordinator(application).also { INSTANCE = it }
                } catch (e: Exception) {
                    Log.e("EntityTrackerFacade", "Caught exception during singleton creation", e)
                    throw e
                }
            }
        }
    }

    // State management for EntityTrackerCoordinator lifecycle
    private val _coordinatorState = MutableStateFlow(CoordinatorState.NOT_INITIALIZED)
    val coordinatorState: StateFlow<CoordinatorState> = _coordinatorState.asStateFlow()

    // Error message for unsupported device and other critical errors
    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    // SharedFlow for entity tracking results (observed by UI)
    private val _entityTrackingResults = MutableSharedFlow<List<Entity>>(replay = 1)
    private val entityTrackingResults: SharedFlow<List<Entity>> = _entityTrackingResults

    private val _zoomState = MutableStateFlow<ZoomState?>(null)
    val zoomState: StateFlow<ZoomState?> = _zoomState.asStateFlow()

    // Store the last decoded entities to replay them with checkmarks after stopping (BI-specific)
    private var lastDecodedEntities: List<Entity> = emptyList()
    private var currentLifecycleOwner: LifecycleOwner? = null
    private var currentPreviewView: PreviewView? = null

    // Camera components used for preview and analysis
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var camera: Camera? = null

    // AI components for entity tracking and barcode decoding
    private var entityTrackerAnalyzer: EntityTrackerAnalyzer? = null
    private var barcodeDecoder: BarcodeDecoder? = null
    private var barcodeDecoderSettings: BarcodeDecoder.Settings? = null
    private var aiVisionSDK: AIVisionSDK? = null

    // Executors for running camera and entity analysis tasks in background threads
    private val cameraExecutor: Executor = Executors.newSingleThreadExecutor()
    private val entityExecutor: Executor = Executors.newSingleThreadExecutor()

    // Guards callbacks after view finder unbind. @Volatile ensures main-thread visibility.
    @Volatile private var isViewFinderActive = false

    /**
     * Initialization block to ensure the SDK is initialized when the coordinator is created.
     */
    init {
        initializeSdkStep()
    }

    fun configureSdk(
        appSettings: AppSettings,
        reset: Boolean = false
    ) {
        val processName = if (reset) "SDK Reinitialization" else "SDK Initialization"
        Log.d(TAG, "Starting: $processName")

        _coordinatorState.value = CoordinatorState.CONFIGURING

        if (reset) {
            dispose()
        }

        // Step 1: Initialize SDK
        initializeSdkStep()

        // Step 2: Initialize BarcodeDecoder Settings
        if (_coordinatorState.value == CoordinatorState.AI_VISION_SDK_INITIALIZED) {
            initializeBarcodeDecoderSettingsStep(appSettings)
        }

        // Step 3: Initialize BarcodeDecoder
        if (_coordinatorState.value == CoordinatorState.BARCODE_DECODER_SETTINGS_INITIALIZED) {
            initializeBarcodeDecoderStep(appSettings)
                .thenRun {
                    // Step 4: Initialize EntityTracker Step
                    if (_coordinatorState.value == CoordinatorState.BARCODE_DECODER_INITIALIZED) {
                        initializeEntityTrackerStep()
                    }

                    // Step 5: Check Camera Permission Step
                    if (_coordinatorState.value == CoordinatorState.ENTITY_TRACKER_INITIALIZED) {
                        checkCameraPermissionStep()
                    }

                    // Step 6: Initialize Camera Step
                    if (_coordinatorState.value == CoordinatorState.CAMERA_PERMISSION_RECEIVED) {
                        initializeCameraStep(
                            appSettings.resolution.width,
                            appSettings.resolution.height
                        )
                    }

                    if (_coordinatorState.value == CoordinatorState.COORDINATOR_READY) {
                        Log.d(TAG, "$processName completed successfully")
                    } else {
                        Log.w(
                            TAG,
                            "$processName did not complete. Current state: ${_coordinatorState.value}"
                        )
                    }
                }
        }
    }

    private fun initializeSdkStep() {
        try {
            aiVisionSDK = AIVisionSDK.getInstance(application)
            if (!aiVisionSDK!!.init()) {
                throw SDKInitializationException("Failed to initialize AI Vision SDK")
            }
            _coordinatorState.value = CoordinatorState.AI_VISION_SDK_INITIALIZED
            _errorMessage.value = ""
            Log.d(TAG, "AI Vision SDK initialized successfully")
        } catch (e: UnsupportedOperationException) {
            _coordinatorState.value = CoordinatorState.ERROR_UNSUPPORTED_DEVICE
            _errorMessage.value = e.message ?: "Device is not supported"
            Log.e(TAG, "SDK initialization failed: ${e.message}", e)
        } catch (e: Exception) {
            _coordinatorState.value = CoordinatorState.ERROR_AI_VISION_SDK
            _errorMessage.value = e.message ?: "Failed to initialize SDK"
            Log.e(TAG, "SDK initialization failed: ${e.message}", e)
        }
    }

    private fun initializeBarcodeDecoderSettingsStep(
        appSettings: AppSettings
    ) {
        try {
            barcodeDecoderSettings = BarcodeDecoderSettingsBuilder()
                .configureSymbologies(appSettings.barcodeSymbology)
                .configureProcessorType(appSettings.processorType)
                .configureModelInput(
                    appSettings.modelInput.width,
                    appSettings.modelInput.height
                )
                .configureAIBarcodeDecode(appSettings.enableAIBarcodeDecode)
                .build()
            _coordinatorState.value = CoordinatorState.BARCODE_DECODER_SETTINGS_INITIALIZED
            Log.d(TAG, "Barcode decoder settings built: enableAIBarcodeDecode=${barcodeDecoderSettings?.enableAIBarcodeDecode}")
        } catch (e: Exception) {
            _coordinatorState.value = CoordinatorState.ERROR_BARCODE_DECODER_SETTINGS
            Log.e(TAG, "Error creating barcode decoder settings: ${e.message}", e)
        }
    }

    private fun initializeBarcodeDecoderStep(appSettings: AppSettings): CompletableFuture<BarcodeDecoder> {
        return BarcodeDecoder.getBarcodeDecoder(barcodeDecoderSettings, cameraExecutor)
            .thenApply { decoderInstance ->
                barcodeDecoder = decoderInstance
                _coordinatorState.value = CoordinatorState.BARCODE_DECODER_INITIALIZED
                Log.d(TAG, "Barcode decoder initialized successfully")
                decoderInstance
            }
            .exceptionally { e ->
                val rootCause = getRootCause(e)
                if (rootCause is com.zebra.ai.vision.detector.AIVisionSDKException &&
                    rootCause.message?.contains("Given runtimes are not available") == true
                ) {
                    Log.e(TAG, "Unsupported processor configuration: ${rootCause.message}", e)
                    _coordinatorState.value = CoordinatorState.ERROR_UNSUPPORTED_PROCESSOR
                } else {
                    Log.e(TAG, "Barcode decoder initialization failed: ${e.message}", e)
                    _coordinatorState.value = CoordinatorState.ERROR_BARCODE_DECODER
                }
                null
            }
    }

    private fun initializeEntityTrackerStep() {
        if (barcodeDecoder == null) {
            Log.e(TAG, "Barcode decoder is not initialized")
        }

        try {
            entityTrackerAnalyzer = EntityTrackerAnalyzer(
                listOf(barcodeDecoder!!),
                ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED,
                entityExecutor
            ) resultCallback@ { result ->
                // Gate: skip callbacks after unbind.
                if (!isViewFinderActive) return@resultCallback
                // ?.let handles dispose() nullifying the field between gate check and getValue().
                val entities = barcodeDecoder?.let { result.getValue(it) } ?: emptyList()
                
                // Performance logging (BI-specific)
                if (callbackCount == 0L) {
                    firstCallbackTimeMs = System.currentTimeMillis()
                }
                callbackCount++
                
                // Store for checkmark replay (BI-specific)
                lastDecodedEntities = entities
                
                _entityTrackingResults.tryEmit(entities)
            }
            _coordinatorState.value = CoordinatorState.ENTITY_TRACKER_INITIALIZED
            Log.d(TAG, "Entity tracker analyzer initialized successfully")
        } catch (e: Exception) {
            _coordinatorState.value = CoordinatorState.ERROR_ENTITY_TRACKER
            Log.e(TAG, "Error initializing entity tracker analyzer", e)
        }
    }

    /**
     * Step to check and request camera permission if not already granted.
     * If permission is denied, it updates the coordinator state and stops further execution.
     */
    private fun checkCameraPermissionStep() {
        try {
            // Check camera permission and update state
            val permissionState = PermissionHandler.Companion.checkCameraPermission(application)
            _coordinatorState.value = permissionState

            when (permissionState) {
                CoordinatorState.CAMERA_PERMISSION_RECEIVED -> {
                    Log.d(TAG, "Camera permission granted.")
                }

                CoordinatorState.CAMERA_PERMISSION_REQUIRED -> {
                    Log.e(
                        TAG,
                        "Camera permission required. Cannot proceed with camera initialization."
                    )
                    throw CameraInitializationException("Camera permission required.")
                }

                CoordinatorState.CAMERA_PERMISSION_DENIED -> {
                    Log.e(
                        TAG,
                        "Camera permission denied. Cannot proceed with camera initialization."
                    )
                    throw CameraInitializationException("Camera permission denied.")
                }

                else -> {
                    Log.e(TAG, "Unexpected permission state: $permissionState")
                    throw CameraInitializationException("Unexpected permission state: $permissionState")
                }
            }
        } catch (e: Exception) {
            _coordinatorState.value = CoordinatorState.ERROR_CAMERA
            Log.e(TAG, "Camera permission check failed: ${e.message}", e)
        }
    }

    /**
     * Prepares the camera by initializing resolution, aspect ratio, and use cases (Preview, ImageAnalysis).
     * Note: This function does NOT start the camera. Use `cameraProvider.bindToLifecycle` to activate it.
     */
    private fun initializeCameraStep(resolutionWidth: Int, resolutionHeight: Int) {
        try {
            val selectedSize = Size(resolutionWidth, resolutionHeight)

            Log.d(TAG, "Using camera resolution: ${selectedSize.width}x${selectedSize.height}")

            val resolutionSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(
                    AspectRatioStrategy(
                        AspectRatio.RATIO_16_9,
                        AspectRatioStrategy.FALLBACK_RULE_NONE
                    )
                )
                .setResolutionStrategy(
                    ResolutionStrategy(
                        selectedSize,
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                    )
                )
                .build()

            val cameraProviderFuture = ProcessCameraProvider.getInstance(application)
            cameraProviderFuture.addListener({
                try {
                    cameraProvider = cameraProviderFuture.get()
                    preview = Preview.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .build()

                    val imageAnalysisBuilder = ImageAnalysis.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    
                    // BI-specific: Apply Camera2Interop perf settings before building
                    Camera2Interop.Extender(imageAnalysisBuilder)
                        .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(30, 30))
                        .setCaptureRequestOption(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_OFF)
                        .setCaptureRequestOption(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF)
                    
                    imageAnalysis = imageAnalysisBuilder.build()

                    imageAnalysis?.setAnalyzer(
                        cameraExecutor,
                        entityTrackerAnalyzer as ImageAnalysis.Analyzer
                    )

                    _coordinatorState.value = CoordinatorState.CAMERA_INITIALIZED
                    Log.d(TAG, "Camera initialized successfully")

                    _coordinatorState.value = CoordinatorState.COORDINATOR_READY

                } catch (e: Exception) {
                    Log.e(TAG, "Error initializing camera", e)
                }
            }, ContextCompat.getMainExecutor(application))
        } catch (e: Exception) {
            _coordinatorState.value = CoordinatorState.ERROR_CAMERA
            Log.e(TAG, "Camera initialization failed: ${e.message}", e)
        }
    }

    /**
     * Binds the camera use cases to the lifecycle owner and connects the preview to the PreviewView.
     */
    fun bindCameraToLifecycle(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        initialZoom: Float = 1.0f
    ) {
        try {
            currentLifecycleOwner = lifecycleOwner
            currentPreviewView = previewView
            
            cameraProvider?.let { provider ->
                // Unbind all use cases before binding new ones
                provider.unbindAll()

                // Bind use cases to camera
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )

                // Apply the initial zoom immediately
                camera?.cameraControl?.setZoomRatio(initialZoom)

                camera?.cameraInfo?.zoomState?.observe(lifecycleOwner) { state ->
                    _zoomState.value = state
                }

                // Connect the preview to the PreviewView
                preview?.surfaceProvider = previewView.surfaceProvider

                // Update EntityTrackerAnalyzer transform when PreviewView is ready
                previewView.previewStreamState.observe(lifecycleOwner) { streamState ->
                    if (streamState == PreviewView.StreamState.STREAMING) {
                        entityTrackerAnalyzer?.updateTransform(previewView.sensorToViewTransform)
                    }
                }

                // Mark view finder as active only after successful binding and setup
                isViewFinderActive = true
            } ?: run {
                // Ensure flag is not incorrectly set when provider is unavailable
                isViewFinderActive = false
            }
        } catch (e: Exception) {
            isViewFinderActive = false
            Log.e(TAG, "Camera binding failed", e)
        }
    }

    /**
     * Unbinds all currently bound camera use cases.
     */
    fun unbindCamera() {
        try {
            // Gate off before unbindAll() to skip in-flight callbacks.
            isViewFinderActive = false
            cameraProvider?.unbindAll()
            
            // BI-specific: Emit last entities for checkmark overlay on unbind
            if (lastDecodedEntities.isNotEmpty()) {
                _entityTrackingResults.tryEmit(lastDecodedEntities)
            } else {
                _entityTrackingResults.tryEmit(emptyList())
            }
            
            Log.d(TAG, "All camera use cases unbound successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error unbinding camera use cases: ${e.message}", e)
        }
    }

    /**
     * Starts barcode decoding by attaching the analyzer to ImageAnalysis.
     * Resets performance tracking counters.
     */
    fun startDecoding() {
        val analyzer = entityTrackerAnalyzer
        if (analyzer == null) {
            Log.e(TAG, "Cannot start decoding - entity tracker analyzer not initialized")
            return
        }

        imageAnalysis?.setAnalyzer(cameraExecutor, analyzer as ImageAnalysis.Analyzer)
        firstCallbackTimeMs = 0L
        callbackCount = 0L
        Log.d(TAG, "Decoding started - analyzer attached to ImageAnalysis")
    }

    /**
     * One-shot center-point AF that locks for the capture window. Call on scan press.
     */
    fun triggerAutoFocus() {
        val pv = currentPreviewView ?: return
        val ctl = camera?.cameraControl ?: return
        if (pv.width == 0 || pv.height == 0) return
        val centerPoint = pv.meteringPointFactory.createPoint(pv.width / 2f, pv.height / 2f)
        val action = FocusMeteringAction.Builder(centerPoint, FocusMeteringAction.FLAG_AF)
            .disableAutoCancel()
            .build()
        ctl.cancelFocusAndMetering()
        ctl.startFocusAndMetering(action)
        Log.d(TAG, "Auto-focus locked on center (scan window)")
    }

    /**
     * Releases any locked focus so CameraX's default continuous picture AF resumes.
     */
    fun enableContinuousAutoFocus() {
        val ctl = camera?.cameraControl ?: return
        ctl.cancelFocusAndMetering()
        Log.d(TAG, "Continuous auto-focus enabled (idle)")
    }

    /**
     * Stops barcode decoding by detaching the analyzer from ImageAnalysis.
     */
    fun stopDecoding(clearOverlays: Boolean = true) {
        imageAnalysis?.clearAnalyzer()
        if (clearOverlays) {
            _entityTrackingResults.tryEmit(emptyList())
            lastDecodedEntities = emptyList()
        }
        Log.d(TAG, "Decoding stopped - analyzer detached (clearOverlays=$clearOverlays)")
    }

    /**
     * Forces a refresh of the entity tracking results by replaying the last decoded entities.
     */
    fun refreshEntityOverlays() {
        if (lastDecodedEntities.isNotEmpty()) {
            _entityTrackingResults.tryEmit(lastDecodedEntities)
            Log.d(TAG, "Refreshed entity overlays with ${lastDecodedEntities.size} entities")
        }
    }

    fun setZoomRatio(ratio: Float) {
        camera?.cameraControl?.setZoomRatio(ratio)?.addListener(
            {
                Log.d(TAG, "Zoom ratio updated successfully: $ratio")
            },
            ContextCompat.getMainExecutor(application)
        )
    }

    fun getPreview(): Preview? = preview

    fun observeEntityTrackingResults(): Flow<List<Entity>> = entityTrackingResults

    /**
     * Recursively unwraps exceptions to find the root cause.
     * CompletableFuture often wraps exceptions in CompletionException.
     */
    private fun getRootCause(throwable: Throwable): Throwable {
        val visited = mutableSetOf<Throwable>()
        var cause = throwable

        while (cause.cause != null && cause.cause != cause) {
            val next = cause.cause!!
            // Prevent infinite loops in case of circular exception chains
            if (!visited.add(next)) {
                break
            }
            cause = next
        }
        return cause
    }

    /**
     * Disposes the Coordinator resources and resets the state.
     */
    fun dispose() {
        try {
            // Gate off first (dispose can bypass unbindCamera).
            isViewFinderActive = false
            // Unbind all camera use cases and release the camera provider
            cameraProvider?.unbindAll()
            cameraProvider = null

            // Dispose of the barcode decoder
            barcodeDecoder?.dispose()
            barcodeDecoder = null

            // Reset other resources
            entityTrackerAnalyzer = null
            currentLifecycleOwner = null
            currentPreviewView = null
            lastDecodedEntities = emptyList()

            // Update state
            _coordinatorState.value = CoordinatorState.NOT_INITIALIZED
            Log.d(TAG, "Disposed coordinator resources successfully")
        } catch (e: Exception) {
            _coordinatorState.value = CoordinatorState.ERROR_DISPOSE
            Log.e(TAG, "Error disposing resources: ${e.message}", e)
        }
    }
}
