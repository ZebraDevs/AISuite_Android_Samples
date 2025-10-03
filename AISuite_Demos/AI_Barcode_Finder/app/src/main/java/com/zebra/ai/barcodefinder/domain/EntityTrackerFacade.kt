package com.zebra.ai.barcodefinder.domain

import android.app.Application
import android.util.Log
import android.util.Size
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.zebra.ai.barcodefinder.domain.enums.RepositoryState
import com.zebra.ai.barcodefinder.domain.proxy.BarcodeDecoderProxy
import com.zebra.ai.barcodefinder.data.source.repository.SettingsRepository
import com.zebra.ai.barcodefinder.data.source.storage.SettingsStorage
import com.zebra.ai.barcodefinder.domain.enums.ProcessorType
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
class EntityTrackerFacade private constructor(private val application: Application) {
    private val TAG = "EntityTrackerFacade"

    companion object {
        @Volatile
        private var INSTANCE: EntityTrackerFacade? = null

        /**
         * Returns the singleton instance of EntityTrackerFacade.
         */
        fun getInstance(application: Application): EntityTrackerFacade {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: try {
                    EntityTrackerFacade(application).also { INSTANCE = it }
                } catch (e: Exception) {
                    Log.e("EntityTrackerFacade", "Caught exception during singleton creation", e)
                    throw e
                }
            }
        }
    }

    // State management for repository lifecycle
    private val _repositoryState = MutableStateFlow(RepositoryState.NOT_INITIALIZED)
    val repositoryState: StateFlow<RepositoryState> = _repositoryState.asStateFlow()

    private val _errorState = MutableStateFlow<Throwable?>(null)
    val errorState: StateFlow<Throwable?> = _errorState.asStateFlow()

    // Camera components used for preview and analysis
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var camera: Camera? = null

    // AI components for entity tracking and barcode decoding
    private var entityTrackerAnalyzer: EntityTrackerAnalyzer? = null
    private var barcodeDecoder: BarcodeDecoder? = null
    private var aiVisionSDK: AIVisionSDK? = null

    // SharedFlow for entity tracking results (observed by UI)
    private val _entityTrackingResults = MutableSharedFlow<List<Entity>>(replay = 1)
    private val entityTrackingResults: SharedFlow<List<Entity>> = _entityTrackingResults

    // Executors for running camera and entity analysis tasks in background threads
    private val cameraExecutor: Executor = Executors.newSingleThreadExecutor()
    private val entityExecutor: Executor = Executors.newSingleThreadExecutor()

    private val permissionHandler: PermissionHandler = PermissionHandler(application)
    private val barcodeDecoderProxy = BarcodeDecoderProxy()
    private val settingsRepository = SettingsRepository.Companion.getInstance(
        SettingsStorage(
            application
        )
    )

    /**
     * Applies current settings to the SDK and reinitializes barcode decoder if needed.
     */
    fun applySettingsToSdk() {
        dispose()
        initializeBarcodeDecoder()
        settingsRepository.updateSettings(settingsRepository.settings.value) // Save settings if needed
    }

    /**
     * Initializes repository by loading settings and SDK components.
     */
    init {
        settingsRepository.loadSettings()
        initializeSdk()
    }

    /**
     * Initializes the AI Vision SDK and barcode decoder.
     */
    private fun initializeSdk() {
        try {
            aiVisionSDK = AIVisionSDK.getInstance(application)
            if (aiVisionSDK!!.init()) {
                // Initialize Barcode Decoder with default settings
                initializeBarcodeDecoder()
            } else {
                Log.e(TAG, "SDK initialization failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "SDK initialization exception: ${e.message}", e)
            throw e
        }
    }

    /**
     * Initializes the barcode decoder and triggers entity tracker analyzer setup.
     */
    private fun initializeBarcodeDecoder() {
        try {
            val barcodeSettings = barcodeDecoderProxy.configureSettings(settingsRepository.settings.value)

            barcodeDecoderProxy.createBarcodeDecoder(barcodeSettings, cameraExecutor)
                .thenAccept { decoderInstance ->
                    barcodeDecoder = decoderInstance
                    _repositoryState.value = RepositoryState.BARCODE_DECODER_INITIALIZED

                    // Initialize Entity Tracker Analyzer after BarcodeDecoder is ready
                    initializeEntityTrackerAnalyzer()
                }
                .exceptionally { ex ->
                    ex.printStackTrace()
                    _errorState.value = ex
                    throw  ex
                }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Initializes the entity tracker analyzer for AI-based entity tracking.
     */
    private fun initializeEntityTrackerAnalyzer() {
        try {
            val decoder = barcodeDecoder ?: return

            entityTrackerAnalyzer = EntityTrackerAnalyzer(
                listOf(decoder),
                ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED,
                entityExecutor
            ) { result ->
                // Handle EntityTrackerAnalyzer results
                val entities = result.getValue(decoder)
                if (entities?.isNotEmpty() == true) {
                    // Always emit, even if contents are the same
                    _entityTrackingResults.tryEmit(entities)
                } else {
                    _entityTrackingResults.tryEmit(emptyList())
                }
            }

            _repositoryState.value = RepositoryState.ENTITY_TRACKER_INITIALIZED

            // Check camera permission
            checkCameraPermission()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Checks and requests camera permission if not already granted.
     */
    private fun checkCameraPermission() {
        val permissionState = permissionHandler.checkCameraPermission()
        _repositoryState.value = permissionState

        if (permissionState == RepositoryState.CAMERA_PERMISSION_RECEIVED) {
            initializeCamera()
        }
    }

    /**
     * Call this method when camera permission is granted
     */
    fun onCameraPermissionGranted() {
        val permissionState = permissionHandler.onCameraPermissionGranted(_repositoryState.value)
        _repositoryState.value = permissionState

        if (permissionState == RepositoryState.CAMERA_PERMISSION_RECEIVED) {
            initializeCamera()
        }
    }

    /**
     * Call this method when camera permission is denied
     */
    fun onCameraPermissionDenied() {
        _repositoryState.value = permissionHandler.onCameraPermissionDenied()
    }

    /**
     * Initializes the camera for preview and analysis use cases.
     */
    private fun initializeCamera() {
        try {
            val selectedSize =
                Size(
                    settingsRepository.settings.value.resolution.width,
                    settingsRepository.settings.value.resolution.height
                )
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

            val cameraProviderFuture = ProcessCameraProvider.Companion.getInstance(application)
            cameraProviderFuture.addListener({
                try {
                    cameraProvider = cameraProviderFuture.get()

                    // Preview use case
                    preview = Preview.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .build()

                    // Image analysis use case
                    imageAnalysis = ImageAnalysis.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    // Set the analyzer
                    imageAnalysis?.setAnalyzer(
                        cameraExecutor,
                        entityTrackerAnalyzer as ImageAnalysis.Analyzer
                    )

                    _repositoryState.value = RepositoryState.CAMERA_INITIALIZED
                    _repositoryState.value = RepositoryState.REPOSITORY_READY

                } catch (e: Exception) {
                    Log.e(TAG, "Camera initialization failed", e)
                }
            }, ContextCompat.getMainExecutor(application))

        } catch (e: Exception) {
            Log.e(TAG, "Camera setup failed", e)
        }
    }

    /**
     * Binds the camera use cases to the lifecycle owner and connects the preview to the PreviewView.
     */
    fun bindCameraToLifecycle(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
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

                // Connect the preview to the PreviewView
                preview?.surfaceProvider = previewView.surfaceProvider

                // Update EntityTrackerAnalyzer transform when PreviewView is ready
                previewView.previewStreamState.observe(lifecycleOwner) { streamState ->
                    if (streamState == PreviewView.StreamState.STREAMING) {
                        entityTrackerAnalyzer?.updateTransform(previewView.sensorToViewTransform)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Camera binding failed", e)
        }
    }

    fun getPreview(): Preview? = preview

    fun getEntityTrackingResults(): Flow<List<Entity>> = entityTrackingResults

    /**
     * Disposes the repository resources and resets the state.
     */
    fun dispose() {
        _repositoryState.value = RepositoryState.NOT_INITIALIZED
        barcodeDecoder?.dispose()
        barcodeDecoder = null
        entityTrackerAnalyzer = null
    }
}