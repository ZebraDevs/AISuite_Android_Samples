package com.zebra.ai.barcodefinder.sdkcoordinator

import android.app.Application
import android.util.Log
import android.util.Size
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
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
import com.zebra.ai.barcodefinder.sdkcoordinator.enums.CoordinatorState
import com.zebra.ai.barcodefinder.sdkcoordinator.exceptions.BarcodeDecoderInitializationException
import com.zebra.ai.barcodefinder.sdkcoordinator.exceptions.CameraInitializationException
import com.zebra.ai.barcodefinder.sdkcoordinator.exceptions.EntityTrackerInitializationException
import com.zebra.ai.barcodefinder.sdkcoordinator.exceptions.SDKInitializationException
import com.zebra.ai.barcodefinder.sdkcoordinator.exceptions.UnsupportedProcessorException
import com.zebra.ai.barcodefinder.sdkcoordinator.model.AppSettings
import com.zebra.ai.barcodefinder.sdkcoordinator.support.BarcodeDecoderSettingsBuilder
import com.zebra.ai.barcodefinder.sdkcoordinator.support.PermissionHandler
import com.zebra.ai.vision.analyzer.tracking.EntityTrackerAnalyzer
import com.zebra.ai.vision.detector.AIVisionSDK
import com.zebra.ai.vision.detector.BarcodeDecoder
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
 * - Initialize SDK and camera components using applySettingsToSdk and initializeCamera methods.
 * - Bind camera use cases to lifecycle owners using bindCameraToLifecycle.
 * - Observe entity tracking results via getEntityTrackingResults.
 * - Dispose resources using dispose when no longer needed.
 * - Ensure camera permissions are managed via onCameraPermissionGranted and onCameraPermissionDenied.
 */
class EntityTrackerCoordinator private constructor(private val application: Application) {
    private val TAG = "EntityTrackerCoordinator"

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

    // SharedFlow for entity tracking results (observed by UI)
    private val _entityTrackingResults = MutableSharedFlow<List<Entity>>(replay = 1)
    private val entityTrackingResults: SharedFlow<List<Entity>> = _entityTrackingResults

    private val _zoomState = MutableStateFlow<ZoomState?>(null)
    val zoomState: StateFlow<ZoomState?> = _zoomState.asStateFlow()


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
            initializeBarcodeDecoderStep()
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
            Log.d(TAG, "AI Vision SDK initialized successfully")
        } catch (e: Exception) {
            _coordinatorState.value = CoordinatorState.ERROR_AI_VISION_SDK
            Log.e(TAG, "SDK initialization failed: ${e.message}", e)
        }
    }

    private fun initializeBarcodeDecoderSettingsStep(appSettings: AppSettings) {
        try {
            barcodeDecoderSettings = BarcodeDecoderSettingsBuilder()
                .configureSymbologies(appSettings.barcodeSymbology)
                .configureProcessorType(appSettings.processorType)
                .configureModelInput(
                    appSettings.modelInput.width,
                    appSettings.modelInput.height
                )
                .build()
            _coordinatorState.value = CoordinatorState.BARCODE_DECODER_SETTINGS_INITIALIZED
        } catch (e: Exception) {
            _coordinatorState.value = CoordinatorState.ERROR_BARCODE_DECODER_SETTINGS
            Log.e(TAG, "Error creating barcode decoder settings: ${e.message}", e)
        }
    }

    private fun initializeBarcodeDecoderStep(): CompletableFuture<BarcodeDecoder> {
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
                    // ToDo: The filtering done to identify the Unsupported runtime is dependant on the SDK Exception message. It makes it unreliable and fragile. A dedicated exception type or error code from the SDK would be much more robust.
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
                    // Proceed with camera initialization
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

                    imageAnalysis = ImageAnalysis.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

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
        initialZoom: Float = 1.0f // Added optional parameter with default 1.0f
    ) {
        try {
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

                //Apply the initial zoom immediately
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
            _entityTrackingResults.tryEmit(emptyList())  // clear out if there is any residual data in previous scanning session
            Log.d(TAG, "All camera use cases unbound successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error unbinding camera use cases: ${e.message}", e)
        }
    }

    fun setZoomRatio(ratio: Float) {
        camera?.cameraControl?.setZoomRatio(ratio)?.addListener(
            {
                Log.d(TAG, "Zoom ratio updated successfully: $ratio")
            },
            ContextCompat.getMainExecutor(application) // Ensure this runs on the main thread
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

            // Update state
            _coordinatorState.value = CoordinatorState.NOT_INITIALIZED
            Log.d(TAG, "Disposed coordinator resources successfully")
        } catch (e: Exception) {
            _coordinatorState.value = CoordinatorState.ERROR_DISPOSE
            Log.e(TAG, "Error disposing resources: ${e.message}", e)
        }
    }
}