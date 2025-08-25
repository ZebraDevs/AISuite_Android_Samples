// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodefinder.data.repository

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
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
import com.google.gson.Gson
import com.zebra.ai.barcodefinder.common.enums.ProcessorType
import com.zebra.ai.barcodefinder.data.model.AppSettings
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
import java.io.File
import java.io.FileWriter
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Enum representing the overall state of the repository and its components.
 */
enum class RepositoryState {
    NOT_INITIALIZED,
    BARCODE_DECODER_INITIALIZED,
    ENTITY_TRACKER_INITIALIZED,
    CAMERA_PERMISSION_REQUIRED,
    CAMERA_PERMISSION_RECEIVED,
    CAMERA_PERMISSION_DENIED,
    CAMERA_INITIALIZED,
    REPOSITORY_READY
}

/**
 * Singleton repository for managing camera, barcode decoding, entity tracking, and app settings.
 * Provides state flows for UI observation and handles initialization and configuration of AI components.
 */
class EntityTrackerRepository private constructor(private val application: Application) {
    private val TAG = "EntityTrackerRepository"

    // Flag to indicate if barcode decoder needs reinitialization due to settings change
    private var isBarcodeDecoderReinitializationNeeded: Boolean = false

    companion object {
        @Volatile
        private var INSTANCE: EntityTrackerRepository? = null

        /**
         * Returns the singleton instance of EntityTrackerRepository.
         */
        fun getInstance(application: Application): EntityTrackerRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EntityTrackerRepository(application).also { INSTANCE = it }
            }
        }
    }

    // State management for repository lifecycle
    private val _repositoryState = MutableStateFlow(RepositoryState.NOT_INITIALIZED)
    val repositoryState: StateFlow<RepositoryState> = _repositoryState.asStateFlow()

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

    // Gson instance for serializing/deserializing settings
    private val gson = Gson()

    // File for storing app settings persistently
    private val settingsFile: File by lazy {
        File(application.getExternalFilesDir("") ?: application.filesDir, "app_settings.json")
    }

    // StateFlow for current app settings
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    /**
     * Loads application settings from persistent storage.
     */
    private fun loadSettings(): AppSettings {
        return if (settingsFile.exists()) {
            try {
                val json = settingsFile.readText()
                gson.fromJson(json, AppSettings::class.java) ?: AppSettings()
            } catch (_: Exception) {
                AppSettings()
            }
        } else {
            AppSettings()
        }
    }

    /**
     * Saves the provided settings to persistent storage.
     */
    fun saveSettings(settings: AppSettings) {
        try {
            FileWriter(settingsFile).use { writer ->
                gson.toJson(settings, writer)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Updates repository settings from the UI/ViewModel. Triggers barcode decoder reinitialization if changed.
     */
    fun updateSettings(appSettings: AppSettings) {
        if (!appSettings.isEquals(_settings.value)) {
            _settings.value = appSettings
            isBarcodeDecoderReinitializationNeeded = true
        }
    }

    /**
     * Applies current settings to the SDK and reinitializes barcode decoder if needed.
     */
    fun applySettingsToSdk() {
        if (isBarcodeDecoderReinitializationNeeded) {
            dispose()
            initializeBarcodeDecoder()
            saveSettings(_settings.value)
            isBarcodeDecoderReinitializationNeeded = false
        }
    }

    /**
     * Initializes repository by loading settings and SDK components.
     */
    init {
        val loaded = loadSettings()
        if (_settings.value != loaded) {
            _settings.value = loaded
        }
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
        }
    }

    /**
     * Initializes the barcode decoder and triggers entity tracker analyzer setup.
     */
    private fun initializeBarcodeDecoder() {
        try {
            val barcodeSettings = createBarcodeDecoderSettings()

            BarcodeDecoder.getBarcodeDecoder(barcodeSettings, cameraExecutor)
                .thenAccept { decoderInstance ->
                    barcodeDecoder = decoderInstance
                    _repositoryState.value = RepositoryState.BARCODE_DECODER_INITIALIZED

                    // Initialize Entity Tracker Analyzer after BarcodeDecoder is ready
                    initializeEntityTrackerAnalyzer()
                }
                .exceptionally { ex ->
                    ex.printStackTrace()
                    null
                }
        } catch (e: Exception) {
            e.printStackTrace()
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
        val hasPermission = ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            _repositoryState.value = RepositoryState.CAMERA_PERMISSION_RECEIVED
            initializeCamera()
        } else {
            _repositoryState.value = RepositoryState.CAMERA_PERMISSION_REQUIRED
        }
    }

    /**
     * Call this method when camera permission is granted
     */
    fun onCameraPermissionGranted() {
        if (_repositoryState.value == RepositoryState.CAMERA_PERMISSION_REQUIRED ||
            _repositoryState.value == RepositoryState.ENTITY_TRACKER_INITIALIZED
        ) {
            _repositoryState.value = RepositoryState.CAMERA_PERMISSION_RECEIVED
            initializeCamera()
        }
    }

    /**
     * Call this method when camera permission is denied
     */
    fun onCameraPermissionDenied() {
        _repositoryState.value = RepositoryState.CAMERA_PERMISSION_DENIED
    }

    /**
     * Initializes the camera for preview and analysis use cases.
     */
    private fun initializeCamera() {
        try {
            val selectedSize =
                Size(_settings.value.resolution.width, _settings.value.resolution.height)
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
     * Creates and configures the barcode decoder settings based on app settings.
     */
    private fun createBarcodeDecoderSettings(): BarcodeDecoder.Settings {
        val modelName = "barcode-localizer"
        val settings = BarcodeDecoder.Settings(modelName)
        settings.Symbology
        val appSettings = _settings.value
        try {
            settings.detectorSetting?.let { localizerSettings ->
                localizerSettings.inferencerOptions?.let { inferencerOptions ->
                    val processorOrder = when (appSettings.processorType) {
                        ProcessorType.DSP -> arrayOf(2)
                        ProcessorType.GPU -> arrayOf(1)
                        ProcessorType.CPU -> arrayOf(0)
                    }
                    inferencerOptions.runtimeProcessorOrder =
                        processorOrder.map { Integer.valueOf(it) }.toTypedArray()
                    Log.d(
                        TAG,
                        "Applied processor type: ${appSettings.processorType.displayNameResId} (${appSettings.processorType.identifierResId})"
                    )
                    inferencerOptions.defaultDims?.let { dynamicDims ->
                        val inputWidth = appSettings.modelInput.width
                        val inputHeight = appSettings.modelInput.height
                        dynamicDims.width = inputWidth
                        dynamicDims.height = inputHeight
                        Log.d(
                            TAG,
                            "Applied model input: ${appSettings.modelInput.displayNameResId} (${inputWidth}x${inputHeight})"
                        )
                    }
                }
            }
        } catch (ex: Exception) {
            Log.w(TAG, "Failed to apply detector settings: ${ex.message}")
        }
        try {
            settings.Symbology?.let { symbology ->
                with(appSettings.barcodeSymbology) {
                    symbology.AUSTRALIAN_POSTAL.enable(australianPostal)
                    symbology.AZTEC.enable(aztec)
                    symbology.CANADIAN_POSTAL.enable(canadianPostal)
                    symbology.CHINESE_2OF5.enable(chinese2of5)
                    symbology.CODABAR.enable(codabar)
                    symbology.CODE11.enable(code11)
                    symbology.CODE39.enable(code39)
                    symbology.CODE93.enable(code93)
                    symbology.CODE128.enable(code128)
                    symbology.COMPOSITE_AB.enable(compositeAB)
                    symbology.COMPOSITE_C.enable(compositeC)
                    symbology.D2OF5.enable(d2of5)
                    symbology.DATAMATRIX.enable(datamatrix)
                    symbology.DOTCODE.enable(dotcode)
                    symbology.DUTCH_POSTAL.enable(dutchPostal)
                    symbology.EAN8.enable(ean8)
                    symbology.EAN13.enable(ean13)
                    symbology.FINNISH_POSTAL_4S.enable(finnishPostal4s)
                    symbology.GRID_MATRIX.enable(gridMatrix)
                    symbology.GS1_DATABAR.enable(gs1Databar)
                    symbology.GS1_DATABAR_EXPANDED.enable(gs1DatabarExpanded)
                    symbology.GS1_DATABAR_LIM.enable(gs1DatabarLim)
                    symbology.GS1_DATAMATRIX.enable(gs1Datamatrix)
                    symbology.GS1_QRCODE.enable(gs1Qrcode)
                    symbology.HANXIN.enable(hanxin)
                    symbology.I2OF5.enable(i2of5)
                    symbology.JAPANESE_POSTAL.enable(japanesePostal)
                    symbology.KOREAN_3OF5.enable(korean3of5)
                    symbology.MAILMARK.enable(mailmark)
                    symbology.MATRIX_2OF5.enable(matrix2of5)
                    symbology.MAXICODE.enable(maxicode)
                    symbology.MICROPDF.enable(micropdf)
                    symbology.MICROQR.enable(microqr)
                    symbology.MSI.enable(msi)
                    symbology.PDF417.enable(pdf417)
                    symbology.QRCODE.enable(qrcode)
                    symbology.TLC39.enable(tlc39)
                    symbology.TRIOPTIC39.enable(trioptic39)
                    symbology.UK_POSTAL.enable(ukPostal)
                    symbology.UPCA.enable(upcA)
                    symbology.UPCE0.enable(upcE)
                    symbology.UPCE1.enable(upce1)
                    symbology.USPLANET.enable(usplanet)
                    symbology.USPOSTNET.enable(uspostnet)
                    symbology.US4STATE.enable(us4state)
                    symbology.US4STATE_FICS.enable(us4stateFics)
                }
            }
        } catch (ex: Exception) {
            Log.w(TAG, "Failed to apply symbology settings: ${ex.message}")
        }
        return settings
    }

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
