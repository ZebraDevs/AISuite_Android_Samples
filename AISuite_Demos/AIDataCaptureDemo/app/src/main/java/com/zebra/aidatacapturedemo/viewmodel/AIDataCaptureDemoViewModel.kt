// Copyright (c) 2024-2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.aidatacapturedemo.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.util.Log
import android.util.Size
import android.view.ScaleGestureDetector
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getString
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import com.google.common.util.concurrent.ListenableFuture
import com.zebra.ai.vision.detector.AIVisionSDK
import com.zebra.ai.vision.detector.BBox
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.data.BarcodeFieldFilterConfig
import com.zebra.aidatacapturedemo.data.BarcodeFilterData
import com.zebra.aidatacapturedemo.data.BarcodeSettings
import com.zebra.aidatacapturedemo.data.BarcodeSymbology
import com.zebra.aidatacapturedemo.data.FilterField
import com.zebra.aidatacapturedemo.data.FilterMenuAction
import com.zebra.aidatacapturedemo.data.FilterType
import com.zebra.aidatacapturedemo.data.ModuleData
import com.zebra.aidatacapturedemo.data.OcrBarcodeFindSettings
import com.zebra.aidatacapturedemo.data.OcrFieldFilterConfig
import com.zebra.aidatacapturedemo.data.OcrFilterData
import com.zebra.aidatacapturedemo.data.ProductData
import com.zebra.aidatacapturedemo.data.ProductRecognitionSettings
import com.zebra.aidatacapturedemo.data.ResultData
import com.zebra.aidatacapturedemo.data.RetailShelfSettings
import com.zebra.aidatacapturedemo.data.TextOcrSettings
import com.zebra.aidatacapturedemo.data.UsecaseState
import com.zebra.aidatacapturedemo.model.BarcodeAnalyzer
import com.zebra.aidatacapturedemo.model.FileUtils
import com.zebra.aidatacapturedemo.model.FileUtils.Companion.clearOcrBarcodeCaptureSessionPrefs
import com.zebra.aidatacapturedemo.model.FileUtils.Companion.databaseFile
import com.zebra.aidatacapturedemo.model.FileUtils.Companion.mCacheDir
import com.zebra.aidatacapturedemo.model.GenericEntityTrackerAnalyzer
import com.zebra.aidatacapturedemo.model.ProductEnrollmentRecognition
import com.zebra.aidatacapturedemo.model.RetailShelfRecognizer
import com.zebra.aidatacapturedemo.model.TextOCRAnalyzer
import com.zebra.aidatacapturedemo.ui.view.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import androidx.core.content.edit
import com.zebra.ai.vision.detector.ImageData
import com.zebra.aidatacapturedemo.model.DiagnosticExporter

private const val TAG = "AIDataCaptureDemoViewModel"
private const val CAMERA_TAG = "AIDCDemo_CameraProp"

// Bounds a single AIVisionSDK.getInstance(context).init() attempt — if the native call hasn't
// returned by then, we treat that attempt as stuck and try again (see initializeSdk()) rather
// than waiting on it forever.
private const val SDK_INIT_ATTEMPT_TIMEOUT_MS = 20_000L

// Delay between retry attempts after a stuck/failed init, so we don't spin-loop while whatever
// is stale on the SDK/OS side (e.g. a license service binder connection) has a chance to clear.
private const val SDK_INIT_RETRY_DELAY_MS = 5_000L

// Caps how many times we retry a stuck init before giving up. This is a client-side fallback
// for an SDK-side defect (see initializeSdk()) — it isn't a real fix, so it must not retry
// forever: each stuck attempt permanently ties up a thread (see sdkInitExecutor), so an
// unbounded loop would eventually exhaust memory/threads instead of failing cleanly.
private const val SDK_INIT_MAX_RETRIES = 5

/**
 * ViewModel class for the AIDataCaptureDemo
 * Initializes AIVisionSDK, before using its components and all the models
 */
class AIDataCaptureDemoViewModel(
    private val cacheDir: String,
    private val context: Context,
    private val assetManager: AssetManager
) : ViewModel() {

    // Used to set up a link between the Model and UI View.
    private val _uiState = MutableStateFlow(AIDataCaptureDemoUiState())
    val uiState: StateFlow<AIDataCaptureDemoUiState> = _uiState.asStateFlow()

    private var executor: Executor? = null

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var analysisUseCase: ImageAnalysis? = null
    private lateinit var imageCaptureResolutionSelector: ResolutionSelector
    private var imageCapture: ImageCapture? = null

    private var ocrAnalyzer: TextOCRAnalyzer? = null
    private var retailShelfAnalyzer: RetailShelfRecognizer? = null

    private var retiringRetailShelfAnalyzer: RetailShelfRecognizer? = null
    private var barcodeAnalyzer: BarcodeAnalyzer? = null
    private var genericEntityTrackerAnalyzer: GenericEntityTrackerAnalyzer? = null
    private var productEnrollmentRecognition: ProductEnrollmentRecognition? = null

    // Holds a ProductEnrollmentRecognition instance that deinitModel() has torn down but whose
    // async native inits (Localizer/FeatureExtractor/etc.) may still be in flight. initModel()
    // waits for it to fully settle before starting a new instance, so we never have two full
    // native model instances (e.g. 8MP capture + "High" input size) allocating memory at once.
    private var retiringProductEnrollmentRecognition: ProductEnrollmentRecognition? = null

    // Same retiring-instance pattern as Retail/Product, ported to OCR/Barcode Find: without
    // this, a settings-triggered reinit could dispose the native TextOCR/BarcodeDecoder while a
    // new instance is starting up, or leave the old instance's native init in flight when a new
    // one begins.
    private var retiringOcrAnalyzer: TextOCRAnalyzer? = null
    private var retiringBarcodeAnalyzer: BarcodeAnalyzer? = null

    // The activity lifecycle passed to setupCameraController(), saved so setAnalyzer() can be
    // re-invoked after a settings-triggered reinit rebuilds the analyzer instances. Previously
    // setAnalyzer() was only ever called once, at initial camera setup — applySettings() would
    // dispose and recreate the native TextOCR/BarcodeDecoder/Localizer objects but the camera
    // stayed bound to the OLD analyzer, which still held a reference to the now-disposed native
    // detector. The next frame would then crash natively with no Java stack trace. This is the
    // root cause of the OCR/Barcode Find "crashes after any settings change" bug.
    private var savedActivityLifecycle: Lifecycle? = null

    companion object {
        fun factory() = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                AIDataCaptureDemoViewModel(
                    application.filesDir.absolutePath,
                    application as Context,
                    application.assets
                )
            }
        }
    }

    // Fatal SDK init error message, surfaced to the UI if set.
    private val _sdkInitError = MutableStateFlow<String?>(null)
    val sdkInitError: StateFlow<String?> = _sdkInitError.asStateFlow()

    // Dedicated pool for SDK init attempts, isolated from Dispatchers.Default.
    private val sdkInitExecutor = Executors.newCachedThreadPool()
    private val sdkInitDispatcher = sdkInitExecutor.asCoroutineDispatcher()

    init {
        executor = Dispatchers.Default.asExecutor()
        loadPersistedSettings()
        initializeSdk()
    }

    // Retries AIVisionSDK.getInstance(context).init() up to SDK_INIT_MAX_RETRIES times, each
    // attempt bounded by SDK_INIT_ATTEMPT_TIMEOUT_MS with SDK_INIT_RETRY_DELAY_MS between
    // attempts. Sets sdkInitError if every attempt fails/times out, or immediately on a
    // non-recoverable UnsupportedOperationException (e.g. non-Zebra device).
    private fun initializeSdk() {
        viewModelScope.launch {
            repeat(SDK_INIT_MAX_RETRIES) { attempt ->
                val result = withTimeoutOrNull(SDK_INIT_ATTEMPT_TIMEOUT_MS) {
                    withContext(sdkInitDispatcher) {
                        try {
                            val isInitDone = AIVisionSDK.getInstance(context).init()
                            Log.i(TAG, "AI Vision SDK Init ret = $isInitDone")

                            // Get the SDK version
                            val sdkVersion = AIVisionSDK.getInstance(context).sdkVersion
                            Log.i(TAG, "AI Vision SDK Version = $sdkVersion")
                            Result.success(Unit)
                        } catch (e: UnsupportedOperationException) {
                            Log.e(TAG, "AI Vision SDK init failed: ${e.message}")
                            Result.failure(e)
                        }
                    }
                }
                when {
                    result == null -> {
                        Log.w(
                            TAG,
                            "AI Vision SDK init attempt ${attempt + 1}/$SDK_INIT_MAX_RETRIES stuck" +
                                    " past ${SDK_INIT_ATTEMPT_TIMEOUT_MS}ms" +
                                    " — retrying in ${SDK_INIT_RETRY_DELAY_MS}ms"
                        )
                        delay(SDK_INIT_RETRY_DELAY_MS)
                    }
                    result.isSuccess -> return@launch
                    else -> {
                        // Non-Zebra/unsupported device — retrying won't change this outcome.
                        _sdkInitError.value = result.exceptionOrNull()?.message
                            ?: getString(context, R.string.unsupported_device_error)
                        return@launch
                    }
                }
            }
            Log.e(TAG, "AI Vision SDK init still stuck after $SDK_INIT_MAX_RETRIES attempts")
            _sdkInitError.value = getString(context, R.string.sdk_init_failed_error)
        }
    }

    fun exportDiagnosticBundle() {
        if (!uiState.value.textOCRSettings.enableDiagnosticLogging) {
            updateToastMessage("Logging is disabled")
            return
        }
        val framesDir = File(context.filesDir, "diagnostic_frames")
        if (framesDir.listFiles()?.any { it.isDirectory && it.name.startsWith("session_") } != true) {
            updateToastMessage("No session data to export")
            return
        }
        updateToastMessage("Exporting diagnostic bundle...")
        viewModelScope.launch(Dispatchers.IO) {
            ocrAnalyzer?.flushLog()
            val success = DiagnosticExporter().export(context, framesDir)
            updateToastMessage(if (success) "Diagnostic bundle exported to Downloads" else "Export failed")
        }
    }

    private fun loadPersistedSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            var barcode = FileUtils.loadBarcodeSettings()
            val ocr = FileUtils.loadOCRSettings()
            var ocrBarcodeFind = FileUtils.loadOCRBarcodeFindSettings()
            var retailShelf = FileUtils.loadRetailShelfSettings()
            var productRecognition = FileUtils.loadProductRecognitionSettings()
            val ocrFilterData = FileUtils.loadOcrFilterData()
            val barcodeFilterData = FileUtils.loadBarcodeFilterData()

            // 2560 is only supported for pure OCR (single decoder, pre-cached). A value of 2560
            // saved from before this fix would otherwise leave these usecases stuck on the
            // unsupported/crashing size — clamp down to the next supported size (1600) on load.
            if (barcode.commonSettings.inputSizeSelected == 2560) {
                barcode = barcode.copy(commonSettings = barcode.commonSettings.copy(inputSizeSelected = 1600))
            }
            if (ocrBarcodeFind.commonSettings.inputSizeSelected == 2560) {
                ocrBarcodeFind = ocrBarcodeFind.copy(commonSettings = ocrBarcodeFind.commonSettings.copy(inputSizeSelected = 1600))
            }
            if (retailShelf.commonSettings.inputSizeSelected == 2560) {
                retailShelf = retailShelf.copy(commonSettings = retailShelf.commonSettings.copy(inputSizeSelected = 1600))
            }
            if (productRecognition.commonSettings.inputSizeSelected == 2560) {
                productRecognition = productRecognition.copy(commonSettings = productRecognition.commonSettings.copy(inputSizeSelected = 1600))
            }

            _uiState.update { current ->
                current.copy(
                    barcodeSettings = barcode,
                    textOCRSettings = ocr,
                    ocrBarcodeFindSettings = ocrBarcodeFind,
                    retailShelfSettings = retailShelf,
                    productRecognitionSettings = productRecognition,
                    ocrFilterData = ocrFilterData,
                    barcodeFilterData = barcodeFilterData
                )
            }
        }
    }

    fun handleMemoryPressureDetected(context: String) {
        Log.w(TAG, "Memory pressure detected in $context")
        updateToastMessage(
            "Device memory is critically low. If scanning feels slow, try lowering "
                    + "camera resolution or model input size in Settings."
        )
    }

    /**
     * This function is used to initialize the model based on the selected index
     */
    fun initModel() {
        // Clear any detection/recognition results left over from a previous session before the
        // new analyzer's first live frame arrives — otherwise stale boxes/product names from
        // last time briefly overlap the fresh camera preview until the first frame overwrites
        // them. Only clearing this on back-press (handleBackButton) missed the case of entering
        // a usecase fresh from the list (or switching usecases), which goes through initModel()
        // directly without ever hitting that back-press path.
        updateProductRecognitionResult(results = null)
        updateRetailShelfDetectionResult(results = null)

        CoroutineScope(executor!!.asCoroutineDispatcher()).launch {
            // Always wait for any retiring heavy analyzers before creating new ones
            awaitRetiringAnalyzers()

            if (genericEntityTrackerAnalyzer == null) {
                genericEntityTrackerAnalyzer =
                    GenericEntityTrackerAnalyzer(
                        uiState,
                        viewModel = this@AIDataCaptureDemoViewModel
                    )
            }

            // Constructing an analyzer (e.g. BarcodeDecoder.Settings(...)) can throw synchronously
            // if the SDK fails to prepare its on-disk model directory (seen in practice as
            // AIVisionSDKException: "Failed to create BarcodeDecoder settings" / IOException:
            // "Failed to create models directory"). This coroutine has no exception handler, so
            // an uncaught exception here crashes the whole app — guard each construction so a
            // failure degrades that one analyzer (with a user-facing error) instead of crashing.
            try {
                when (uiState.value.usecaseSelected) {
                    UsecaseState.Barcode.value -> {
                        repairCorruptedModelDir("barcode-decoder")
                        barcodeAnalyzer = BarcodeAnalyzer(
                            uiState = uiState,
                            viewModel = this@AIDataCaptureDemoViewModel
                        )
                        barcodeAnalyzer?.initialize()
                    }

                    UsecaseState.Retail.value -> {
                        retailShelfAnalyzer = RetailShelfRecognizer(
                            uiState = uiState,
                            viewModel = this@AIDataCaptureDemoViewModel,
                            cacheDir = context.filesDir.absolutePath
                        )
                        retailShelfAnalyzer?.initialize()
                    }

                    UsecaseState.Product.value -> {
                        productEnrollmentRecognition =
                            ProductEnrollmentRecognition(
                                uiState = uiState,
                                viewModel = this@AIDataCaptureDemoViewModel,
                                context = context,
                                cacheDir = context.filesDir.absolutePath
                            )
                        productEnrollmentRecognition?.initialize()
                    }

                    UsecaseState.OCRBarcodeFind.value -> {
                        if (uiState.value.isOCRModelEnabled) {
                            ocrAnalyzer = TextOCRAnalyzer(
                                uiState = uiState,
                                viewModel = this@AIDataCaptureDemoViewModel,
                                context = context
                            )
                            ocrAnalyzer?.initialize()
                            // Stage BarcodeDecoder's init after TextOCR's native load settles (or
                            // times out) instead of firing both simultaneously — two DSP-bound
                            // native loads contending at once at high input size is the same class
                            // of OOM/contention crash already fixed for Product Enrollment's
                            // multi-component init.
                            awaitReadyOrTimeout(timeoutMs = 45_000) { uiState.value.isOcrModelDemoReady }
                        }
                        if (uiState.value.isBarcodeModelEnabled) {
                            try {
                                repairCorruptedModelDir("barcode-decoder")
                                barcodeAnalyzer = BarcodeAnalyzer(
                                    uiState = uiState,
                                    viewModel = this@AIDataCaptureDemoViewModel
                                )
                                barcodeAnalyzer?.initialize()
                            } catch (e: Exception) {
                                // Isolated from the OCR init above — a Barcode failure here
                                // shouldn't tear down an already-successful OCR init.
                                Log.e(TAG, "Failed to initialize BarcodeAnalyzer: ${e.message}", e)
                                barcodeAnalyzer = null
                                updateRecognizerInitError(
                                    "Barcode could not be initialized on this device. OCR will still work."
                                )
                            }
                        }
                    }

                    UsecaseState.OCR.value -> {
                        ocrAnalyzer = TextOCRAnalyzer(
                            uiState = uiState,
                            viewModel = this@AIDataCaptureDemoViewModel,
                            context = context
                        )
                        ocrAnalyzer?.initialize()
                        // Without this, rebindAnalyzerWithRetry()'s much shorter retry budget
                        // (6 * 300ms = 1.8s) frequently loses the race against a cold model load
                        // (copying+decrypting the model tar on a fresh install, then loading it,
                        // can take 3-4+ seconds) and gives up before the detector is ready —
                        // leaving the camera bound to an analyzer whose detector is still null, so
                        // live scanning silently does nothing until the screen is reopened. Same
                        // pattern already used for OCRBarcodeFind's staged OCR init above.
                        awaitReadyOrTimeout(timeoutMs = 45_000) { uiState.value.isOcrModelDemoReady }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "initModel failed to initialize analyzer: ${e.message}", e)
                updateRecognizerInitError(
                    "Failed to initialize the model on this device. Please restart the app and try again."
                )
            }

            // Re-bind the camera to the newly (re)created analyzer instance. Without this,
            // a settings-triggered reinit leaves the camera bound to whichever analyzer was
            // wired up at initial camera setup — which, for OCR/Barcode/OCRBarcodeFind, holds a
            // now-disposed native detector reference and crashes natively on the next frame.
            // Retail/Product survive the same gap today only because their analyzer objects
            // carry their own isRetiring no-op guard; OCR/Barcode has no equivalent, since
            // frames there go through the vendor's EntityTrackerAnalyzer, not our own classes.
            savedActivityLifecycle?.let { lifecycle ->
                rebindAnalyzerWithRetry(lifecycle)
            }
        }
    }

    /**
     * The SDK's own model-directory setup (Utils.cleanPreviousVersions/copyModelFromAssest) has
     * failed with "Failed to create models directory" for [modelDirName] — seen in practice as an
     * uncaught AIVisionSDKException when constructing BarcodeAnalyzer. If a stray file (rather
     * than a directory) is sitting at that path — e.g. left over from an earlier interrupted
     * copy/crash — mkdir() fails every time until it's removed. Best-effort cleanup; if this
     * fails too, the caller's try/catch still prevents a crash.
     */
    private fun repairCorruptedModelDir(modelDirName: String) {
        try {
            val dir = File(context.filesDir, modelDirName)
            if (dir.exists() && !dir.isDirectory) {
                Log.w(TAG, "Removing corrupted non-directory entry at ${dir.absolutePath}")
                dir.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "repairCorruptedModelDir failed for $modelDirName: ${e.message}")
        }
    }

    /**
     * Suspends until [isReady] is true or [timeoutMs] elapses — bounded so a failed/slow native
     * init can't hang the caller forever. Used to stage OCRBarcodeFind's two native model loads
     * (TextOCR then BarcodeDecoder) sequentially instead of firing both at once.
     *
     * Reacts to [uiState] emissions instead of polling on a fixed delay — [isReady] is checked
     * against the current value first (no wasted wait if already true), then re-checked on every
     * subsequent state emission, so this returns the instant readiness flips rather than up to
     * one poll interval late.
     */
    private suspend fun awaitReadyOrTimeout(timeoutMs: Long, isReady: () -> Boolean) {
        if (isReady()) return
        withTimeoutOrNull(timeoutMs) {
            uiState.first { isReady() }
        }
    }

    /**
     * Binds the camera to the current usecase's analyzer, reactively waiting for the native
     * detector to become ready first if it isn't yet (analyzer initialize() calls are async, so
     * the detector may not exist the instant initialize() returns). setAnalyzer() itself is
     * null-safe (skips a decoder that isn't ready yet rather than crashing), so worst case here
     * is one decoder's frames don't flow until the timeout below gives up — never a crash.
     */
    private suspend fun rebindAnalyzerWithRetry(activityLifecycle: Lifecycle) {
        setAnalyzer(activityLifecycle)
        if (isCurrentUsecaseAnalyzerReady()) return
        val becameReady = withTimeoutOrNull(10_000) {
            uiState.first { isCurrentUsecaseAnalyzerReady() }
        } != null
        setAnalyzer(activityLifecycle)
        if (!becameReady) {
            Log.w(TAG, "rebindAnalyzerWithRetry: gave up waiting for analyzer readiness")
        }
    }

    private fun isCurrentUsecaseAnalyzerReady(): Boolean = when (uiState.value.usecaseSelected) {
        UsecaseState.Barcode.value -> uiState.value.isBarcodeModelDemoReady
        UsecaseState.Retail.value, UsecaseState.Product.value -> uiState.value.isRetailShelfModelDemoReady
        UsecaseState.OCRBarcodeFind.value -> {
            val needsOcr = uiState.value.isOCRModelEnabled
            val needsBarcode = uiState.value.isBarcodeModelEnabled
            (!needsOcr || uiState.value.isOcrModelDemoReady) &&
                    (!needsBarcode || uiState.value.isBarcodeModelDemoReady)
        }
        UsecaseState.OCR.value -> uiState.value.isOcrModelDemoReady
        else -> true
    }

    /**
     * This function is used to de-initialize the model based on the selected value
     */
    fun deInitModel() {
        when (uiState.value.usecaseSelected) {
            UsecaseState.Barcode.value -> {
                barcodeAnalyzer?.let {
                    it.deinitialize()
                    retiringBarcodeAnalyzer = it
                }
                barcodeAnalyzer = null
            }

            UsecaseState.Retail.value -> {
                retailShelfAnalyzer?.let {
                    it.deinitialize()
                    retiringRetailShelfAnalyzer = it
                }
                retailShelfAnalyzer = null
            }

            UsecaseState.Product.value -> {
                // Only touch retiringProductEnrollmentRecognition when there's a live instance
                // to retire — otherwise a repeated deinitModel() call (e.g. rapid back presses)
                // would overwrite it with null and orphan an already-retiring instance's
                // in-flight native futures.
                productEnrollmentRecognition?.let {
                    it.deinitialize()
                    retiringProductEnrollmentRecognition = it
                }
                productEnrollmentRecognition = null
            }

            UsecaseState.OCRBarcodeFind.value -> {
                ocrAnalyzer?.let {
                    it.deinitialize()
                    retiringOcrAnalyzer = it
                }
                ocrAnalyzer = null

                barcodeAnalyzer?.let {
                    it.deinitialize()
                    retiringBarcodeAnalyzer = it
                }
                barcodeAnalyzer = null
            }

            UsecaseState.OCR.value -> {
                ocrAnalyzer?.let {
                    it.deinitialize()
                    retiringOcrAnalyzer = it
                }
                ocrAnalyzer = null
            }
        }
        genericEntityTrackerAnalyzer?.deinitialize()
        genericEntityTrackerAnalyzer = null
    }

    override fun onCleared() {
        deInitModel()
        sdkInitExecutor.shutdownNow()
        super.onCleared()
    }

    @SuppressLint("ClickableViewAccessibility", "RestrictedApi")
    public fun setupCameraController(
        previewView: PreviewView,
        analysisUseCaseCameraResolution: Size,
        lifecycleOwner: LifecycleOwner,
        activityLifecycle: Lifecycle
    ) {
        cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                initializeCameraUseCases(
                    previewView, analysisUseCaseCameraResolution, lifecycleOwner, activityLifecycle
                )
            } catch (e: IllegalArgumentException) {
                handleCameraSetupError(e)
            } catch (e: Exception) {
                Log.e(TAG, " Exception while setting up the camera : ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @SuppressLint("ClickableViewAccessibility", "RestrictedApi")
    private fun initializeCameraUseCases(
        previewView: PreviewView,
        analysisUseCaseCameraResolution: Size,
        lifecycleOwner: LifecycleOwner,
        activityLifecycle: Lifecycle
    ) {
        printCameraSupportedResolution()
        val isBackCameraAvailable = hasBackCamera(cameraProvider = cameraProvider!!)
        Log.d(TAG, "isBackCameraAvailable = $isBackCameraAvailable")

        val selectedCameraLensFacing = if (isBackCameraAvailable) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(selectedCameraLensFacing)
            .build()

        val previewUsecase = buildPreviewUseCase()

        analysisUseCase = buildAnalysisUseCase(analysisUseCaseCameraResolution)

        // Saved so a settings-triggered reinit can re-bind the camera to the new analyzer
        // instance later (see rebindAnalyzerWithRetry()).
        savedActivityLifecycle = activityLifecycle

        // Set the appropriate analyzer based on the selectedUsecase
        setAnalyzer(activityLifecycle)
        cameraProvider?.unbindAll()

        // Bind use cases to lifecycle
        camera = bindCameraUseCases(lifecycleOwner, cameraSelector, previewUsecase)

        previewUsecase.setSurfaceProvider(previewView.surfaceProvider)
        updateCameraReady(true)
        logAttachedResolutions(previewUsecase)
        setupPinchToZoom(previewView)
    }

    private fun buildPreviewUseCase(): Preview {
        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(
                AspectRatioStrategy(
                    AspectRatio.RATIO_16_9,
                    AspectRatioStrategy.FALLBACK_RULE_NONE
                )
            )
            .build()
        return Preview.Builder().setResolutionSelector(resolutionSelector).build()
    }

    private fun buildAnalysisUseCase(resolution: Size): ImageAnalysis {
        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(
                AspectRatioStrategy(
                    AspectRatio.RATIO_16_9,
                    AspectRatioStrategy.FALLBACK_RULE_NONE
                )
            )
            .setResolutionStrategy(
                ResolutionStrategy(resolution, ResolutionStrategy.FALLBACK_RULE_NONE)
            ).build()

        return ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }

    private fun requiresImageCapture(): Boolean {
        val usecase = uiState.value.usecaseSelected
        return usecase == UsecaseState.Product.value || (uiState.value.usecaseSelected == UsecaseState.Retail.value) ||
                (usecase == UsecaseState.OCRBarcodeFind.value && uiState.value.isCaptureOrLiveEnabled == 0)
    }

    private fun bindCameraUseCases(
        lifecycleOwner: LifecycleOwner,
        cameraSelector: CameraSelector,
        previewUsecase: Preview
    ): Camera? {
        if (requiresImageCapture()) {
            imageCaptureResolutionSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(
                    AspectRatioStrategy(
                        AspectRatio.RATIO_16_9,
                        AspectRatioStrategy.FALLBACK_RULE_NONE
                    )
                )
                .build()

            imageCapture = ImageCapture.Builder()
                .setResolutionSelector(imageCaptureResolutionSelector)
                .setCaptureMode(CAPTURE_MODE_MAXIMIZE_QUALITY).build()

            return cameraProvider?.bindToLifecycle(
                lifecycleOwner, cameraSelector, previewUsecase, imageCapture, analysisUseCase
            )
        }
        return cameraProvider?.bindToLifecycle(
            lifecycleOwner, cameraSelector, previewUsecase, analysisUseCase
        )
    }

    @SuppressLint("RestrictedApi")
    @Suppress("RestrictedApi")
    private fun logAttachedResolutions(previewUsecase: Preview) {
        val previewUseCaseSize = previewUsecase.attachedSurfaceResolution ?: Size(0, 0)
        Log.d(TAG, "Attached PreviewUsecase Resolution = $previewUseCaseSize")

        val analysisUseCaseSize = analysisUseCase?.attachedSurfaceResolution ?: Size(0, 0)
        Log.d(TAG, "Attached analysisUsecase Resolution = $analysisUseCaseSize")

        val imageCaptureUseCaseSize = imageCapture?.attachedSurfaceResolution ?: Size(0, 0)
        Log.d(TAG, "Attached imageCaptureUsecase Resolution = $imageCaptureUseCaseSize")
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPinchToZoom(previewView: PreviewView) {
        val scaleGestureDetector = ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    handlePinchToZoom(detector)
                    return true
                }
            })

        previewView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun handlePinchToZoom(detector: ScaleGestureDetector) {
        camera?.cameraControl ?: return
        val cameraInfo = camera?.cameraInfo ?: return

        val currentZoomRatio = cameraInfo.zoomState.value?.zoomRatio ?: 1.0f
        val newZoomRatio = currentZoomRatio * detector.scaleFactor

        val minZoomRatio = cameraInfo.zoomState.value?.minZoomRatio ?: 1.0f
        val maxZoomRatio = cameraInfo.zoomState.value?.maxZoomRatio ?: 1.0f
        val clampedZoomRatio = newZoomRatio.coerceIn(minZoomRatio, maxZoomRatio)

        setZoom(clampedZoomRatio)
    }

    private fun handleCameraSetupError(e: IllegalArgumentException) {
        Log.e(TAG, "IllegalArgumentException while setting up the camera. Exception = ${e.message}")
        val message = e.message ?: return
        if (message.contains("May be attempting to bind too many use cases") ||
            message.contains("No available output size is found")
        ) {
            val errorMessage = getString(context, R.string.instruction_6)
            toast(toastString = errorMessage)
            updateCameraErrorMessage(errorMessage = errorMessage)
        }
    }

    private fun printCameraSupportedResolution() {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        Log.d(CAMERA_TAG, "Printing Camera's supported Resolutions:")
        cameraManager.cameraIdList.forEach { cameraId ->
            cameraId?.let { it ->


                Log.d(CAMERA_TAG, "cameraId = $it")
                val characteristics = cameraManager.getCameraCharacteristics(it)

                val facing = characteristics.get<Int?>(CameraCharacteristics.LENS_FACING)

                if (facing != null) {
                    when (facing) {
                        CameraCharacteristics.LENS_FACING_FRONT -> {
                            Log.d(CAMERA_TAG, "Camera facing = front-facing camera")
                        }

                        CameraCharacteristics.LENS_FACING_BACK -> {
                            Log.d(CAMERA_TAG, "Camera facing = back-facing camera")
                        }

                        CameraCharacteristics.LENS_FACING_EXTERNAL -> {
                            Log.d(CAMERA_TAG, "Camera facing = external camera")
                        }
                    }
                }
                val configMap =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

                android.graphics.ImageFormat.PRIVATE
                configMap?.outputFormats?.forEach { format ->

                    val formatName = when (format) {
                        1144402265 -> "DEPTH16"
                        1768253795 -> "DEPTH_JPEG"
                        257 -> "DEPTH_POINT_CLOUD"
                        42 -> "FLEX_RGBA_8888"
                        41 -> "FLEX_RGB_888"
                        1212500294 -> "HEIC"
                        4102 -> "HEIC_ULTRAHDR"
                        256 -> "JPEG"
                        4101 -> "JPEG_R"
                        16 -> "NV16"
                        17 -> "NV21"
                        34 -> "PRIVATE"
                        37 -> "RAW10"
                        38 -> "RAW12"
                        36 -> "RAW_PRIVATE"
                        32 -> "RAW_SENSOR"
                        4 -> "RGB_565"
                        0 -> "UNKNOWN"
                        538982489 -> "Y8"
                        54 -> "YCBCR_P010"
                        60 -> "YCBCR_P210"
                        35 -> "YUV_420_888"
                        39 -> "YUV_422_888"
                        40 -> "YUV_444_888"
                        20 -> "YUY2"
                        842094169 -> "YV12"
                        else -> "N/A"
                    }
                    Log.d(CAMERA_TAG, "Format = $formatName")
                    Log.d(CAMERA_TAG, "Supported Preview Size:")
                    val previewSizes: Array<Size>? = configMap?.getOutputSizes(format)
                    previewSizes?.forEach { size ->
                        val aspectRatio = size.width.toFloat() / size.height.toFloat()
                        val mp = (size.width.toFloat() * size.height.toFloat()) / 1000000
                        Log.d(
                            CAMERA_TAG,
                            "${size.width}x${size.height}, Ratio : ${aspectRatio}, MP : $mp"
                        )
                    }

                    Log.d(CAMERA_TAG, "Supported HigRes Size:")
                    val highResSizes: Array<Size>? = configMap?.getHighResolutionOutputSizes(format)
                    highResSizes?.forEach { size ->
                        val aspectRatio = size.width.toFloat() / size.height.toFloat()
                        val mp = (size.width.toFloat() * size.height.toFloat()) / 1000000
                        Log.d(
                            CAMERA_TAG,
                            "${size.width}x${size.height}, Ratio : ${aspectRatio}, MP : $mp"
                        )
                    }
                }

                // Get supported high-resolution capture sizes
                val captureSizes: Array<Size>? =
                    configMap?.getHighResolutionOutputSizes(android.graphics.ImageFormat.JPEG)
                Log.d(CAMERA_TAG, "Supported High-Resolution Capture Sizes:")
                captureSizes?.forEach { size ->
                    Log.d(CAMERA_TAG, "Capture resolution: ${size.width}x${size.height}")
                }
            }
        }
    }

    private fun hasBackCamera(cameraProvider: ProcessCameraProvider): Boolean {
        return try {
            cameraProvider.hasCamera(DEFAULT_BACK_CAMERA)
        } catch (e: Exception) {
            // A camera may not be available for the requested selector
            false
        }
    }

    /**
     * Turn on torch
     */
    fun enableTorch(enabled: Boolean) {
        if (camera?.cameraInfo?.hasFlashUnit() == true) {
            camera?.cameraControl?.enableTorch(enabled)
        }
    }

    /**
     * Set Zoom Value
     */
    fun setZoom(zoomValue: Float) {
        _uiState.update { currentState ->
            currentState.copy(
                zoomLevel = zoomValue
            )
        }
        camera?.cameraControl?.setZoomRatio(uiState.value.zoomLevel)
    }

    /**
     * Update the selected usecase
     */
    fun updateSelectedUsecase(usecase: String) {
        _uiState.update { currentState ->
            currentState.copy(
                usecaseSelected = usecase
            )
        }
    }

    /**
     * Update the selected processor
     */
    fun updateSelectedProcessor(index: Int) {
        _uiState.update { currentState ->
            when (currentState.usecaseSelected) {
                UsecaseState.Barcode.value -> currentState.copy(
                    barcodeSettings = currentState.barcodeSettings.copy(
                        commonSettings = currentState.barcodeSettings.commonSettings
                            .copy(processorSelectedIndex = index)
                    )
                )
                UsecaseState.Retail.value -> currentState.copy(
                    retailShelfSettings = currentState.retailShelfSettings.copy(
                        commonSettings = currentState.retailShelfSettings.commonSettings
                            .copy(processorSelectedIndex = index)
                    )
                )
                UsecaseState.Product.value -> currentState.copy(
                    productRecognitionSettings = currentState.productRecognitionSettings.copy(
                            commonSettings = currentState.productRecognitionSettings.commonSettings
                                .copy(processorSelectedIndex = index)
                        )
                )
                UsecaseState.OCRBarcodeFind.value -> currentState.copy(
                    ocrBarcodeFindSettings = currentState.ocrBarcodeFindSettings.copy(
                            commonSettings = currentState.ocrBarcodeFindSettings.commonSettings
                                .copy(processorSelectedIndex = index)
                        )
                )
                UsecaseState.OCR.value -> currentState.copy(
                    textOCRSettings = currentState.textOCRSettings.copy(
                        commonSettings = currentState.textOCRSettings.commonSettings
                            .copy(processorSelectedIndex = index)
                    )
                )
                else -> currentState
            }
        }
        if ((uiState.value.usecaseSelected == UsecaseState.OCRBarcodeFind.value) ||
            (uiState.value.usecaseSelected == UsecaseState.OCR.value)) {
            updateSelectedDimensions(640)
        }
    }

    /**
     * Update the selected DPM mode (0 = Disabled, 1 = Small, 2 = Large). Only applicable to
     * Barcode / OCR & Barcode Find use cases.
     */
    fun updateDpmMode(index: Int) {
        _uiState.update { currentState ->
            when (currentState.usecaseSelected) {
                UsecaseState.Barcode.value -> currentState.copy(
                    barcodeSettings = currentState.barcodeSettings.copy(
                        commonSettings = currentState.barcodeSettings.commonSettings
                            .copy(dpmModeSelectedIndex = index)
                    )
                )

                UsecaseState.OCRBarcodeFind.value -> currentState.copy(
                    ocrBarcodeFindSettings = currentState.ocrBarcodeFindSettings.copy(
                        commonSettings = currentState.ocrBarcodeFindSettings.commonSettings
                            .copy(dpmModeSelectedIndex = index)
                    )
                )

                else -> currentState
            }
        }
    }


    /**
     * Update the selected model input dimension. [dimension] is the actual pixel size (e.g. 640,
     * 832, 1280, 1600, 2560) — NOT a radio index — since the set of supported sizes now differs
     * per usecase (per the SDK team + docs): OCR supports up to 2560 (single decoder, pre-cached);
     * Barcode/OCRBarcodeFind cap at 1600 (Barcode has no 2560 support, and OCRBarcodeFind runs
     * both decoders at once so it's limited by Barcode); Product/Retail support 640/832/1280/1600.
     */
    fun updateSelectedDimensions(dimension: Int) {
        _uiState.update { currentState ->
            when (currentState.usecaseSelected) {
                UsecaseState.Barcode.value -> currentState.copy(
                    barcodeSettings = currentState.barcodeSettings.copy(
                        commonSettings = currentState.barcodeSettings.commonSettings
                            .copy(inputSizeSelected = dimension)
                    )
                )

                UsecaseState.Retail.value -> currentState.copy(
                    retailShelfSettings = currentState.retailShelfSettings.copy(
                        commonSettings = currentState.retailShelfSettings.commonSettings
                            .copy(inputSizeSelected = dimension)
                    )
                )

                UsecaseState.Product.value -> currentState.copy(
                    productRecognitionSettings =
                        currentState.productRecognitionSettings.copy(
                            commonSettings = currentState.productRecognitionSettings.commonSettings
                                .copy(inputSizeSelected = dimension)
                        )
                )

                UsecaseState.OCRBarcodeFind.value -> {
                    var dim = dimension
                    if (currentState.ocrBarcodeFindSettings.commonSettings.processorSelectedIndex == 2)
                        dim = 640
                    currentState.copy(
                        ocrBarcodeFindSettings =
                            currentState.ocrBarcodeFindSettings.copy(
                                commonSettings = currentState.ocrBarcodeFindSettings.commonSettings
                                    .copy(inputSizeSelected = dim)
                            )
                    )
                }

                UsecaseState.OCR.value -> {
                    var dim = dimension
                    if (currentState.textOCRSettings.commonSettings.processorSelectedIndex == 2)
                        dim = 640
                    currentState.copy(
                        textOCRSettings = currentState.textOCRSettings.copy(
                            commonSettings = currentState.textOCRSettings.commonSettings
                                .copy(inputSizeSelected = dim)
                        )
                    )
                }

                else -> currentState
            }
        }
    }

    fun updateSelectedResolution(index: Int) {
        _uiState.update { currentState ->
            when (currentState.usecaseSelected) {
                UsecaseState.Barcode.value -> currentState.copy(
                    barcodeSettings = currentState.barcodeSettings.copy(
                        commonSettings = currentState.barcodeSettings.commonSettings
                            .copy(resolutionSelectedIndex = index)
                    )
                )

                UsecaseState.Retail.value -> currentState.copy(
                    retailShelfSettings = currentState.retailShelfSettings.copy(
                        commonSettings = currentState.retailShelfSettings.commonSettings
                            .copy(resolutionSelectedIndex = index)
                    )
                )

                UsecaseState.Product.value -> currentState.copy(
                    productRecognitionSettings = currentState.productRecognitionSettings.copy(
                        commonSettings = currentState.productRecognitionSettings.commonSettings
                            .copy(resolutionSelectedIndex = index)
                    )
                )

                UsecaseState.OCRBarcodeFind.value -> currentState.copy(
                    ocrBarcodeFindSettings = currentState.ocrBarcodeFindSettings.copy(
                        commonSettings = currentState.ocrBarcodeFindSettings.commonSettings
                            .copy(resolutionSelectedIndex = index)
                    )
                )

                UsecaseState.OCR.value -> currentState.copy(
                    textOCRSettings = currentState.textOCRSettings.copy(
                        commonSettings = currentState.textOCRSettings.commonSettings
                            .copy(resolutionSelectedIndex = index)
                    )
                )

                else -> currentState
            }
        }
    }

    fun getSelectedResolution(): Int? {
        val currentResolutionSelectedIndex = when (uiState.value.usecaseSelected) {
            UsecaseState.Barcode.value -> {
                _uiState.value.barcodeSettings.commonSettings.resolutionSelectedIndex
            }

            UsecaseState.Retail.value -> {
                _uiState.value.retailShelfSettings.commonSettings.resolutionSelectedIndex
            }

            UsecaseState.Product.value -> {
                _uiState.value.productRecognitionSettings.commonSettings.resolutionSelectedIndex
            }

            UsecaseState.OCRBarcodeFind.value -> {
                _uiState.value.ocrBarcodeFindSettings.commonSettings.resolutionSelectedIndex
            }

            UsecaseState.OCR.value -> {
                _uiState.value.textOCRSettings.commonSettings.resolutionSelectedIndex
            }

            else -> {
                null
            }
        }
        return currentResolutionSelectedIndex
    }

    fun getProcessorSelectedIndex(): Int? {
        val currentProcessorSelectedIndex = when (uiState.value.usecaseSelected) {
            UsecaseState.Barcode.value -> {
                _uiState.value.barcodeSettings.commonSettings.processorSelectedIndex
            }

            UsecaseState.Retail.value -> {
                _uiState.value.retailShelfSettings.commonSettings.processorSelectedIndex
            }

            UsecaseState.Product.value -> {
                _uiState.value.productRecognitionSettings.commonSettings.processorSelectedIndex
            }

            UsecaseState.OCRBarcodeFind.value -> {
                _uiState.value.ocrBarcodeFindSettings.commonSettings.processorSelectedIndex
            }

            UsecaseState.OCR.value -> {
                _uiState.value.textOCRSettings.commonSettings.processorSelectedIndex
            }

            else -> {
                null
            }
        }
        return currentProcessorSelectedIndex
    }

    /**
     * Get the selected DPM mode index (0 = Disabled, 1 = Small, 2 = Large).
     * Only meaningful for Barcode / OCR & Barcode Find use cases.
     */
    fun getDpmModeSelectedIndex(): Int {
        return when (uiState.value.usecaseSelected) {
            UsecaseState.Barcode.value -> {
                _uiState.value.barcodeSettings.commonSettings.dpmModeSelectedIndex
            }

            UsecaseState.OCRBarcodeFind.value -> {
                _uiState.value.ocrBarcodeFindSettings.commonSettings.dpmModeSelectedIndex
            }

            else -> {
                0
            }
        }
    }

    fun getInputSizeSelected(): Int? {
        val currentInputSizeSelected = when (uiState.value.usecaseSelected) {
            UsecaseState.Barcode.value -> {
                _uiState.value.barcodeSettings.commonSettings.inputSizeSelected
            }

            UsecaseState.Retail.value -> {
                _uiState.value.retailShelfSettings.commonSettings.inputSizeSelected
            }

            UsecaseState.Product.value -> {
                _uiState.value.productRecognitionSettings.commonSettings.inputSizeSelected
            }

            UsecaseState.OCRBarcodeFind.value -> {
                _uiState.value.ocrBarcodeFindSettings.commonSettings.inputSizeSelected
            }

            UsecaseState.OCR.value -> {
                _uiState.value.textOCRSettings.commonSettings.inputSizeSelected
            }

            else -> {
                null
            }
        }
        return currentInputSizeSelected
    }

    fun setSelectedFilterField(fieldId: String, fieldTitle: String) {
        _uiState.value = _uiState.value.copy(
            selectedFilterFieldId = fieldId,
            selectedFilterFieldTitle = fieldTitle
        )
    }

    fun setSelectedFilterType(filterType: FilterType) {
        _uiState.value = _uiState.value.copy(selectedFilterType = filterType)
    }

    fun upsertOcrField(field: FilterField) {
        val current = _uiState.value.ocrFilterData
        val fields = current.fieldList.toMutableList()
        val existingIndex = fields.indexOfFirst { it.id == field.id }
        if (existingIndex != -1) fields[existingIndex] = field else fields.add(field)
        _uiState.value = _uiState.value.copy(
            ocrFilterData = current.copy(fieldList = fields)
        )

        val toSave = _uiState.value.ocrFilterData
        viewModelScope.launch(Dispatchers.IO) {
            FileUtils.saveOcrFilterData(ocrFilterData = toSave)
        }
    }

    fun upsertBarcodeField(field: FilterField) {
        val current = _uiState.value.barcodeFilterData
        val fields = current.fieldList.toMutableList()
        val existingIndex = fields.indexOfFirst { it.id == field.id }
        if (existingIndex != -1) fields[existingIndex] = field else fields.add(field)
        _uiState.value = _uiState.value.copy(
            barcodeFilterData = current.copy(fieldList = fields)
        )

        val toSave = _uiState.value.barcodeFilterData
        viewModelScope.launch(Dispatchers.IO) {
            FileUtils.saveBarcodeFilterData(barcodeFilterData = toSave)
        }
    }

    fun removeOcrFields(fieldIds: List<String>) {
        val current = _uiState.value.ocrFilterData
        _uiState.value = _uiState.value.copy(
            ocrFilterData = current.copy(
                fieldList = current.fieldList.filterNot { it.id in fieldIds }.toMutableList(),
                fieldFilterConfigs = current.fieldFilterConfigs.filterNot { it.fieldId in fieldIds }
                    .toMutableList(),
                selectedFieldIds = mutableListOf()
            )
        )

        val toSave = _uiState.value.ocrFilterData
        viewModelScope.launch(Dispatchers.IO) {
            FileUtils.saveOcrFilterData(ocrFilterData = toSave)
        }
    }

    fun removeBarcodeFields(fieldIds: List<String>) {
        val current = _uiState.value.barcodeFilterData
        _uiState.value = _uiState.value.copy(
            barcodeFilterData = current.copy(
                fieldList = current.fieldList.filterNot { it.id in fieldIds }.toMutableList(),
                fieldFilterConfigs = current.fieldFilterConfigs.filterNot { it.fieldId in fieldIds }
                    .toMutableList(),
                selectedFieldIds = mutableListOf()
            )
        )

        val toSave = _uiState.value.barcodeFilterData
        viewModelScope.launch(Dispatchers.IO) {
            FileUtils.saveBarcodeFilterData(barcodeFilterData = toSave)
        }
    }

    fun resetOcrFields(defaults: List<FilterField>) {
        val current = _uiState.value.ocrFilterData
        _uiState.value = _uiState.value.copy(
            ocrFilterData = current.copy(
                fieldList = defaults.toMutableList(),
                fieldFilterConfigs = mutableListOf()
            )
        )

        val toSave = _uiState.value.ocrFilterData
        viewModelScope.launch(Dispatchers.IO) {
            FileUtils.saveOcrFilterData(ocrFilterData = toSave)
        }
    }

    fun resetBarcodeFields(defaults: List<FilterField>) {
        val current = _uiState.value.barcodeFilterData
        _uiState.value = _uiState.value.copy(
            barcodeFilterData = current.copy(
                fieldList = defaults.toMutableList(),
                fieldFilterConfigs = mutableListOf()
            )
        )

        val toSave = _uiState.value.barcodeFilterData
        viewModelScope.launch(Dispatchers.IO) {
            FileUtils.saveBarcodeFilterData(barcodeFilterData = toSave)
        }
    }

    fun saveOcrFieldConfig(config: OcrFieldFilterConfig) {
        val current = _uiState.value.ocrFilterData
        val configs = current.fieldFilterConfigs.toMutableList()
        val index = configs.indexOfFirst { it.fieldId == config.fieldId }
        if (index >= 0) configs[index] = config else configs.add(config)
        _uiState.value = _uiState.value.copy(
            ocrFilterData = current.copy(fieldFilterConfigs = configs)
        )

        val toSave = _uiState.value.ocrFilterData
        viewModelScope.launch(Dispatchers.IO) {
            FileUtils.saveOcrFilterData(ocrFilterData = toSave)
        }
    }

    fun saveBarcodeFieldConfig(config: BarcodeFieldFilterConfig) {
        val current = _uiState.value.barcodeFilterData
        val configs = current.fieldFilterConfigs.toMutableList()
        val index = configs.indexOfFirst { it.fieldId == config.fieldId }
        if (index >= 0) configs[index] = config else configs.add(config)
        _uiState.value = _uiState.value.copy(
            barcodeFilterData = current.copy(fieldFilterConfigs = configs)
        )

        val toSave = _uiState.value.barcodeFilterData
        viewModelScope.launch(Dispatchers.IO) {
            FileUtils.saveBarcodeFilterData(barcodeFilterData = toSave)
        }
    }

    fun updateSelectedOcrFieldIds(ids: List<String>) {
        val current = _uiState.value.ocrFilterData
        _uiState.value = _uiState.value.copy(
            ocrFilterData = current.copy(selectedFieldIds = ids.distinct().toMutableList())
        )

        val toSave = _uiState.value.ocrFilterData
        viewModelScope.launch(Dispatchers.IO) {
            FileUtils.saveOcrFilterData(ocrFilterData = toSave)
        }
    }

    fun updateSelectedBarcodeFieldIds(ids: List<String>) {
        val current = _uiState.value.barcodeFilterData
        _uiState.value = _uiState.value.copy(
            barcodeFilterData = current.copy(selectedFieldIds = ids.distinct().toMutableList())
        )

        val toSave = _uiState.value.barcodeFilterData
        viewModelScope.launch(Dispatchers.IO) {
            FileUtils.saveBarcodeFilterData(barcodeFilterData = toSave)
        }
    }

    fun updateOcrFilterData(ocrFilterData: OcrFilterData) {
        _uiState.update { currentState ->
            currentState.copy(
                ocrFilterData = ocrFilterData
            )
        }

        val toSave = _uiState.value.ocrFilterData
        viewModelScope.launch(Dispatchers.IO) {
            FileUtils.saveOcrFilterData(ocrFilterData = toSave)
        }
    }

    fun updateBarcodeFilterData(barcodeFilterData: BarcodeFilterData) {
        _uiState.update { currentState ->
            currentState.copy(
                barcodeFilterData = barcodeFilterData
            )
        }

        val toSave = _uiState.value.barcodeFilterData
        viewModelScope.launch(Dispatchers.IO) {
            FileUtils.saveBarcodeFilterData(barcodeFilterData = toSave)
        }
    }

    /**
     * Update the barcode symbologies
     */
    fun updateBarcodeSymbology(name: String, enabled: Boolean) {
        var currentSymbology = BarcodeSymbology()
        if (_uiState.value.usecaseSelected == UsecaseState.OCRBarcodeFind.value) {
            currentSymbology = _uiState.value.ocrBarcodeFindSettings.barcodeSymbology
        } else {
            currentSymbology = _uiState.value.barcodeSettings.barcodeSymbology
        }
        val updatedSymbology = when (name) {
            getString(context, R.string.australian_postal) ->
                currentSymbology.copy(australian_postal = enabled)

            getString(context, R.string.aztec) -> {
                currentSymbology.copy(aztec = enabled)
            }

            getString(context, R.string.canadian_postal) -> {
                currentSymbology.copy(canadian_postal = enabled)
            }

            getString(context, R.string.chinese_2of5) -> {
                currentSymbology.copy(chinese_2of5 = enabled)
            }

            getString(context, R.string.codabar) -> {
                currentSymbology.copy(codabar = enabled)
            }

            getString(context, R.string.code11) -> {
                currentSymbology.copy(code11 = enabled)

            }

            getString(context, R.string.code39) -> {
                currentSymbology.copy(code39 = enabled)

            }

            getString(context, R.string.code93) -> {
                currentSymbology.copy(code93 = enabled)

            }

            getString(context, R.string.code128) -> {
                currentSymbology.copy(code128 = enabled)

            }

            getString(context, R.string.composite_ab) -> {
                currentSymbology.copy(composite_ab = enabled)

            }

            getString(context, R.string.composite_c) -> {
                currentSymbology.copy(composite_c = enabled)

            }

            getString(context, R.string.d2of5) -> {
                currentSymbology.copy(d2of5 = enabled)

            }

            getString(context, R.string.datamatrix) -> {
                currentSymbology.copy(datamatrix = enabled)

            }

            getString(context, R.string.dotcode) -> {
                currentSymbology.copy(dotcode = enabled)

            }

            getString(context, R.string.dutch_postal) -> {
                currentSymbology.copy(dutch_postal = enabled)

            }

            getString(context, R.string.ean_8) -> {
                currentSymbology.copy(ean_8 = enabled)

            }

            getString(context, R.string.ean_13) -> {
                currentSymbology.copy(ean_13 = enabled)

            }

            getString(context, R.string.finnish_postal_4s) -> {
                currentSymbology.copy(finnish_postal_4s = enabled)

            }

            getString(context, R.string.grid_matrix) -> {
                currentSymbology.copy(grid_matrix = enabled)

            }

            getString(context, R.string.gs1_databar) -> {
                currentSymbology.copy(gs1_databar = enabled)

            }

            getString(context, R.string.gs1_databar_expanded) -> {
                currentSymbology.copy(gs1_databar_expanded = enabled)

            }

            getString(context, R.string.gs1_databar_lim) -> {
                currentSymbology.copy(gs1_databar_lim = enabled)

            }

            getString(context, R.string.gs1_datamatrix) -> {
                currentSymbology.copy(gs1_datamatrix = enabled)
            }

            getString(context, R.string.gs1_qrcode) -> {
                currentSymbology.copy(gs1_qrcode = enabled)

            }

            getString(context, R.string.hanxin) -> {
                currentSymbology.copy(hanxin = enabled)

            }

            getString(context, R.string.i2of5) -> {
                currentSymbology.copy(i2of5 = enabled)

            }

            getString(context, R.string.japanese_postal) -> {
                currentSymbology.copy(japanese_postal = enabled)

            }

            getString(context, R.string.korean_3of5) -> {
                currentSymbology.copy(korean_3of5 = enabled)

            }

            getString(context, R.string.mailmark) -> {
                currentSymbology.copy(mailmark = enabled)
            }

            getString(context, R.string.matrix_2of5) -> {
                currentSymbology.copy(matrix_2of5 = enabled)

            }

            getString(context, R.string.maxicode) -> {
                currentSymbology.copy(maxicode = enabled)

            }

            getString(context, R.string.micropdf) -> {
                currentSymbology.copy(micropdf = enabled)

            }

            getString(context, R.string.microqr) -> {
                currentSymbology.copy(microqr = enabled)

            }

            getString(context, R.string.msi) -> {
                currentSymbology.copy(msi = enabled)

            }

            getString(context, R.string.pdf417) -> {
                currentSymbology.copy(pdf417 = enabled)

            }

            getString(context, R.string.qrcode) -> {
                currentSymbology.copy(qrcode = enabled)

            }

            getString(context, R.string.tlc39) -> {
                currentSymbology.copy(tlc39 = enabled)

            }

            getString(context, R.string.trioptic39) -> {
                currentSymbology.copy(trioptic39 = enabled)

            }

            getString(context, R.string.uk_postal) -> {
                currentSymbology.copy(uk_postal = enabled)

            }

            getString(context, R.string.upc_a) -> {
                currentSymbology.copy(upc_a = enabled)

            }

            getString(context, R.string.upce0) -> {
                currentSymbology.copy(upce0 = enabled)

            }

            getString(context, R.string.upce1) -> {
                currentSymbology.copy(upce1 = enabled)
            }

            getString(context, R.string.usplanet) -> {
                currentSymbology.copy(usplanet = enabled)

            }

            getString(context, R.string.uspostnet) -> {
                currentSymbology.copy(uspostnet = enabled)

            }

            getString(context, R.string.us4state) -> {
                currentSymbology.copy(us4state = enabled)

            }

            getString(context, R.string.us4state_fics) -> {
                currentSymbology.copy(us4state_fics = enabled)

            }

            else -> {
                currentSymbology
            }
        }
        _uiState.update { currentState ->
            if (currentState.usecaseSelected == UsecaseState.OCRBarcodeFind.value) {
                currentState.copy(
                    ocrBarcodeFindSettings = currentState.ocrBarcodeFindSettings
                        .copy(barcodeSymbology = updatedSymbology)
                )
            } else {
                currentState.copy(
                    barcodeSettings = currentState.barcodeSettings
                        .copy(barcodeSymbology = updatedSymbology)
                )
            }
        }
    }

    fun updateFeedback(name: String, enabled: Boolean) {
        if (_uiState.value.usecaseSelected == UsecaseState.OCRBarcodeFind.value) {
            var currentFeedback =
                _uiState.value.ocrBarcodeFindSettings.feedbackSettings

            val updatedFeedback = when (name) {
                getString(context, R.string.audio) ->
                    currentFeedback.copy(audioBeep = enabled)

                getString(context, R.string.haptic) -> {
                    currentFeedback.copy(vibration = enabled)
                }

                getString(context, R.string.show_all_detected_barcodes) -> {
                    currentFeedback.copy(showDetectedBarcode = enabled)
                }

                else -> {
                    currentFeedback
                }
            }
            _uiState.update { currentState ->
                currentState.copy(
                    ocrBarcodeFindSettings = currentState.ocrBarcodeFindSettings
                        .copy(feedbackSettings = updatedFeedback)
                )
            }
        }
    }

    fun updateEnableAIBarcodeDecode(enabled: Boolean) {
        _uiState.update { currentState ->
            when (currentState.usecaseSelected) {
                UsecaseState.OCRBarcodeFind.value -> currentState.copy(
                    ocrBarcodeFindSettings = currentState.ocrBarcodeFindSettings.copy(
                        commonSettings = currentState.ocrBarcodeFindSettings.commonSettings
                            .copy(enableAIBarcodeDecode = enabled)
                    )
                )

                else -> currentState.copy(
                    barcodeSettings = currentState.barcodeSettings.copy(
                        commonSettings = currentState.barcodeSettings.commonSettings
                            .copy(enableAIBarcodeDecode = enabled)
                    )
                )
            }
        }
    }

    fun updateBarcodeModelEnabled(enabled: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                isBarcodeModelEnabled = enabled
            )
        }
    }

    // Remembers the user's barcode-toggle preference before Target mode forces it off, so it can
    // be restored on leaving Target mode. Lives in the ViewModel (not the screen) so it survives
    // navigating away to scan and back.
    private var priorBarcodeEnabledBeforeTargetMode: Boolean? = null

    fun updateCaptureOrLiveEnabled(mode: Int) {
        val previousMode = _uiState.value.isCaptureOrLiveEnabled
        when {
            // Entering Target mode (only on the real transition, not on recomposition):
            // Target supports OCR only — save the barcode preference, then force barcode off.
            mode == 2 && previousMode != 2 -> {
                priorBarcodeEnabledBeforeTargetMode = _uiState.value.isBarcodeModelEnabled
                _uiState.update {
                    it.copy(
                        isCaptureOrLiveEnabled = mode,
                        isBarcodeModelEnabled = false,
                        isOCRModelEnabled = true
                    )
                }
            }
            // Leaving Target mode: restore the saved barcode preference.
            mode != 2 && previousMode == 2 -> {
                val restore = priorBarcodeEnabledBeforeTargetMode ?: true
                priorBarcodeEnabledBeforeTargetMode = null
                _uiState.update {
                    it.copy(
                        isCaptureOrLiveEnabled = mode,
                        isBarcodeModelEnabled = restore
                    )
                }
            }
            else -> {
                _uiState.update { it.copy(isCaptureOrLiveEnabled = mode) }
            }
        }
    }

    fun updateTargetFrameSize(widthRatio: Float, heightRatio: Float) {
        _uiState.update { currentState ->
            currentState.copy(
                targetFrameWidthRatio = widthRatio,
                targetFrameHeightRatio = heightRatio
            )
        }
        // Persist target frame size
        val prefs = context.getSharedPreferences("target_mode_prefs", Context.MODE_PRIVATE)
        prefs.edit {
            putFloat("target_frame_width_ratio", widthRatio)
                .putFloat("target_frame_height_ratio", heightRatio)
                .putBoolean("target_frame_user_customized", true)
        }
    }

    fun updateTargetFramePosition(offsetXRatio: Float, offsetYRatio: Float) {
        _uiState.update { currentState ->
            currentState.copy(
                targetFrameOffsetXRatio = offsetXRatio,
                targetFrameOffsetYRatio = offsetYRatio
            )
        }
        // Persist target frame position
        val prefs = context.getSharedPreferences("target_mode_prefs", Context.MODE_PRIVATE)
        prefs.edit {
            putFloat("target_frame_offset_x_ratio", offsetXRatio)
                .putFloat("target_frame_offset_y_ratio", offsetYRatio)
                .putBoolean("target_frame_user_customized", true)
        }
    }

    fun loadTargetFrameSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = context.getSharedPreferences("target_mode_prefs", Context.MODE_PRIVATE)
            val widthRatio = prefs.getFloat(
                "target_frame_width_ratio",
                _uiState.value.ocrBarcodeFindSettings.targetModeSetting.defaultFrameWidthRatio
            )
            val heightRatio = prefs.getFloat(
                "target_frame_height_ratio",
                _uiState.value.ocrBarcodeFindSettings.targetModeSetting.defaultFrameHeightRatio
            )
            val offsetXRatio = prefs.getFloat(
                "target_frame_offset_x_ratio",
                _uiState.value.ocrBarcodeFindSettings.targetModeSetting.defaultFrameOffsetXRatio
            )
            val offsetYRatio = prefs.getFloat(
                "target_frame_offset_y_ratio",
                _uiState.value.ocrBarcodeFindSettings.targetModeSetting.defaultFrameOffsetYRatio
            )
            _uiState.update { currentState ->
                currentState.copy(
                    targetFrameWidthRatio = widthRatio,
                    targetFrameHeightRatio = heightRatio,
                    targetFrameOffsetXRatio = offsetXRatio,
                    targetFrameOffsetYRatio = offsetYRatio
                )
            }
        }
    }

    /**
     * Update the default target mode frame size from percentage values (1–100).
     * Converts to ratio (0.0–1.0) and persists inside ocrBarcodeFindSettings.
     */
    fun updateDefaultTargetModeFrameSize(widthPercent: Float, heightPercent: Float) {
        val widthRatio = (widthPercent / 100f).coerceIn(0.10f, 1f)
        val heightRatio = (heightPercent / 100f).coerceIn(0.10f, 1f)
        _uiState.update { currentState ->
            val updatedTargetMode =
                currentState.ocrBarcodeFindSettings.targetModeSetting.copy(
                    defaultFrameWidthRatio = widthRatio,
                    defaultFrameHeightRatio = heightRatio
                )
            val updatedSettings =
                currentState.ocrBarcodeFindSettings.copy(targetModeSetting = updatedTargetMode)
            currentState.copy(
                ocrBarcodeFindSettings = updatedSettings,
                targetFrameWidthRatio = widthRatio,
                targetFrameHeightRatio = heightRatio
            )
        }
    }

    fun commitDefaultTargetModeFrameSize() {
        val settingsToSave = _uiState.value.ocrBarcodeFindSettings
        val widthRatio = settingsToSave.targetModeSetting.defaultFrameWidthRatio
        val heightRatio = settingsToSave.targetModeSetting.defaultFrameHeightRatio

        viewModelScope.launch(Dispatchers.IO) {
            FileUtils.saveOCRBarcodeFindSettings(settingsToSave)
            withContext(Dispatchers.Main) {
                val prefs = context.getSharedPreferences(
                    "target_mode_prefs",
                    Context.MODE_PRIVATE
                )
                val userCustomized = prefs.getBoolean("target_frame_user_customized", false)
                if (!userCustomized) {
                    prefs.edit {
                        putFloat("target_frame_width_ratio", widthRatio)
                        putFloat("target_frame_height_ratio", heightRatio)
                    }
                }
            }
        }
    }

    fun updateOCRModelEnabled(enabled: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                isOCRModelEnabled = enabled
            )
        }
    }

    fun updateAllBarcodeOCRCaptureFilter(mode: Int) {
        _uiState.update { currentState ->
            currentState.copy(
                allBarcodeOCRCaptureFilter = mode
            )
        }
    }

    /**
     * Update the current bitmap used for processing by the models
     */
    fun updateBitmap(bitmap: Bitmap, rotation: Int) {
        val matrix: Matrix = Matrix()
        matrix.postRotate(rotation.toFloat())
        val rotatedBitmap =
            Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )

        _uiState.update { currentState ->
            currentState.copy(
                currentBitmap = rotatedBitmap
            )
        }
    }

    suspend fun takePicture(): Bitmap = suspendCancellableCoroutine { continuation ->
        val cameraExecutor = executor
        val currentImageCapture = imageCapture

        if (cameraExecutor == null || currentImageCapture == null) {
            continuation.resumeWithException(IllegalStateException("Camera is not ready"))
            return@suspendCancellableCoroutine
        }

        try {
            currentImageCapture.takePicture(
                cameraExecutor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        try {
                            val highResBitmap = rotateBitmapIfNeeded(imageProxy = image)
                            image.close()

                            if (highResBitmap != null) {
                                continuation.resume(highResBitmap)
                            } else {
                                continuation.resumeWithException(
                                    IllegalStateException("Failed to convert captured image to bitmap")
                                )
                            }
                        } catch (e: OutOfMemoryError) {
                            image.close()
                            continuation.resumeWithException(e)
                        } catch (e: Exception) {
                            image.close()
                            continuation.resumeWithException(e)
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        continuation.resumeWithException(exception)
                    }
                }
            )
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

    fun rotateBitmapIfNeeded(imageProxy: ImageProxy): Bitmap? {
        try {
            val bitmap = imageProxy.toBitmap()
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            return rotateBitmap(bitmap, rotationDegrees)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting image to bitmap: " + e.message)
            return null
        }
    }

    private fun rotateBitmap(bitmap: Bitmap?, degrees: Int): Bitmap? {
        if (degrees == 0 || bitmap == null) return bitmap

        try {
            val matrix = Matrix()
            matrix.postRotate(degrees.toFloat())
            val rotated = Bitmap.createBitmap(
                /* source = */ bitmap,
                /* x = */ 0,
                /* y = */ 0,
                /* width = */ bitmap.width,
                /* height = */ bitmap.height,
                /* m = */ matrix,
                /* filter = */ true
            )
            if (rotated !== bitmap) bitmap.recycle()
            return rotated
        } catch (e: Exception) {
            Log.e(TAG, "Error rotating bitmap: " + e.message)
            return bitmap
        }
    }

    private var previewRotationBuffer: Bitmap? = null

    private fun getOrCreatePreviewRotationBuffer(width: Int, height: Int): Bitmap {
        val existing = previewRotationBuffer
        if (existing != null && !existing.isRecycled && existing.width == width &&
            existing.height == height) {
            return existing
        }
        existing?.recycle()
        val fresh = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        previewRotationBuffer = fresh
        return fresh
    }

    fun rotateBitmapForPreview(imageProxy: ImageProxy): Bitmap? {
        try {
            val source = imageProxy.toBitmap()
            val degrees = imageProxy.imageInfo.rotationDegrees
            if (degrees == 0) return source

            val swapDims = degrees == 90 || degrees == 270
            val targetWidth = if (swapDims) source.height else source.width
            val targetHeight = if (swapDims) source.width else source.height

            val target = getOrCreatePreviewRotationBuffer(targetWidth, targetHeight)
            val canvas = Canvas(target)
            canvas.drawColor(0, PorterDuff.Mode.CLEAR)

            val matrix = Matrix()
            matrix.postTranslate(-source.width / 2f, -source.height / 2f)
            matrix.postRotate(degrees.toFloat())
            matrix.postTranslate(targetWidth / 2f, targetHeight / 2f)
            canvas.drawBitmap(source, matrix, null)

            source.recycle()
            return target
        } catch (e: Exception) {
            Log.e(TAG, "Error rotating preview bitmap: " + e.message)
            return null
        }
    }

    private fun setAnalyzer(activityLifecycle: Lifecycle) {
        when (uiState.value.usecaseSelected) {
            UsecaseState.Barcode.value -> {
                val tracker = genericEntityTrackerAnalyzer ?: return
                barcodeAnalyzer?.let {
                    val detector = it.getDetector() ?: return
                    tracker.addDecoder(detector)
                    val analyzer =
                        tracker.setupEntityTrackerAnalyzer(activityLifecycle) as ImageAnalysis.Analyzer
                    analysisUseCase?.setAnalyzer(executor!!, analyzer)
                }
            }

            UsecaseState.Retail.value -> {
                retailShelfAnalyzer?.let {
                    analysisUseCase?.setAnalyzer(executor!!, it)
                }
            }

            UsecaseState.Product.value -> {
                productEnrollmentRecognition?.let {
                    analysisUseCase?.setAnalyzer(executor!!, it)
                }
            }

            UsecaseState.OCRBarcodeFind.value -> {
                // Setup Analyzer only if Live mode or Target Mode is enabled.
                // In case of Captured Image mode, Barcode and OCR decoders process captured image instead of using the GenericEntityTrackerAnalyzer
                if (uiState.value.isCaptureOrLiveEnabled == 1 || uiState.value.isCaptureOrLiveEnabled == 2) {
                    val tracker = genericEntityTrackerAnalyzer ?: return
                    var anyDecoderAdded = false
                    ocrAnalyzer?.getDetector()?.let {
                        tracker.addDecoder(it)
                        anyDecoderAdded = true
                    }
                    barcodeAnalyzer?.getDetector()?.let {
                        tracker.addDecoder(it)
                        anyDecoderAdded = true
                    }
                    if (!anyDecoderAdded) return
                    val analyzer =
                        tracker.setupEntityTrackerAnalyzer(activityLifecycle) as ImageAnalysis.Analyzer
                    analysisUseCase?.setAnalyzer(executor!!, analyzer)
                }
            }

            UsecaseState.OCR.value -> {
                val tracker = genericEntityTrackerAnalyzer ?: return
                ocrAnalyzer?.let { ocr ->
                    val detector = ocr.getDetector() ?: return
                    tracker.addDecoder(detector)
                    val entityAnalyzer =
                        tracker.setupEntityTrackerAnalyzer(activityLifecycle) as ImageAnalysis.Analyzer
                    val wrappedAnalyzer = ImageAnalysis.Analyzer { imageProxy ->
                        if (ocr.isDiagnosticLoggingEnabled()) {
                            ocr.stashLatestBitmap(imageProxy.toBitmap(), imageProxy.imageInfo.timestamp)
                        }
                        entityAnalyzer.analyze(imageProxy)
                    }
                    analysisUseCase?.setAnalyzer(executor!!, wrappedAnalyzer)
                }
            }
        }
    }

    fun updateAppBarTitle(title: String) {
        if (_uiState.value.appBarTitle == title) return // prevent redundant overwrite

        _uiState.update { currentState ->
            currentState.copy(
                appBarTitle = title
            )
        }
    }

    fun updateOCRTextFieldValues(name: String, value: String) {
        val advancedOCRSetting = _uiState.value.textOCRSettings.advancedOCRSetting
        val updatedOCRSetting =
            when (name) {
                getString(context, R.string.heatmap_threshold) -> {
                    advancedOCRSetting.copy(heatmapThreshold = value)
                }

                getString(context, R.string.box_threshold) -> {
                    advancedOCRSetting.copy(boxThreshold = value)
                }

                getString(context, R.string.min_box_area) -> {
                    advancedOCRSetting.copy(minBoxArea = value)
                }

                getString(context, R.string.min_box_size) -> {
                    advancedOCRSetting.copy(minBoxSize = value)
                }

                getString(context, R.string.unclip_ratio) -> {
                    advancedOCRSetting.copy(unclipRatio = value)
                }

                getString(context, R.string.min_ratio_for_rotation) -> {
                    advancedOCRSetting.copy(minRatioForRotation = value)
                }

                getString(context, R.string.character_confidence_threshold) -> {
                    advancedOCRSetting.copy(maxWordCombinations = value)
                }

                getString(context, R.string.max_word_combinations) -> {
                    advancedOCRSetting.copy(maxWordCombinations = value)
                }

                getString(context, R.string.topk_ignore_cutoff) -> {
                    advancedOCRSetting.copy(topkIgnoreCutoff = value)
                }

                getString(context, R.string.topk_ignore_cutoff) -> {
                    advancedOCRSetting.copy(topkIgnoreCutoff = value)
                }

                getString(context, R.string.total_probability_threshold) -> {
                    advancedOCRSetting.copy(totalProbabilityThreshold = value)
                }

                getString(context, R.string.width_distance_ratio) -> {
                    advancedOCRSetting.copy(widthDistanceRatio = value)
                }

                getString(context, R.string.height_distance_ratio) -> {
                    advancedOCRSetting.copy(heightDistanceRatio = value)
                }

                getString(context, R.string.center_distance_ratio) -> {
                    advancedOCRSetting.copy(centerDistanceRatio = value)
                }

                getString(context, R.string.paragraph_height_distance) -> {
                    advancedOCRSetting.copy(paragraphHeightDistance = value)
                }

                getString(context, R.string.paragraph_height_ratio_threshold) -> {
                    advancedOCRSetting.copy(paragraphHeightRatioThreshold = value)
                }

                getString(context, R.string.top_correlation_threshold) -> {
                    advancedOCRSetting.copy(topCorrelationThreshold = value)
                }

                getString(context, R.string.merge_points_cutoff) -> {
                    advancedOCRSetting.copy(mergePointsCutoff = value)
                }

                getString(context, R.string.split_margin_factor) -> {
                    advancedOCRSetting.copy(splitMarginFactor = value)
                }

                getString(context, R.string.aspect_ratio_lower_threshold) -> {
                    advancedOCRSetting.copy(aspectRatioLowerThreshold = value)
                }

                getString(context, R.string.aspect_ratio_upper_threshold) -> {
                    advancedOCRSetting.copy(aspectRatioUpperThreshold = value)
                }

                getString(context, R.string.topK_merged_predictions) -> {
                    advancedOCRSetting.copy(topKMergedPredictions = value)
                }

                else -> {
                    advancedOCRSetting
                }
            }
        _uiState.update { currentState ->
            currentState.copy(
                textOCRSettings =
                    currentState.textOCRSettings.copy(advancedOCRSetting = updatedOCRSetting)
            )
        }
    }

    fun updateOCRSwitchOptions(name: String, enabled: Boolean) {
        if (name == getString(context, R.string.enable_diagnostic_logging)) {
            _uiState.update { currentState ->
                currentState.copy(
                    textOCRSettings = currentState.textOCRSettings
                        .copy(enableDiagnosticLogging = enabled)
                )
            }
            return
        }
        val advancedOCRSetting = _uiState.value.textOCRSettings.advancedOCRSetting
        val updatedOCRSetting = when (name) {
            getString(context, R.string.enable_tiling) -> {
                advancedOCRSetting.copy(enableTiling = enabled)
            }

            getString(context, R.string.enable_grouping) -> {
                advancedOCRSetting.copy(enableGrouping = enabled)
            }

            else -> {
                advancedOCRSetting
            }
        }
        _uiState.update { currentState ->
            currentState.copy(
                textOCRSettings =
                    currentState.textOCRSettings.copy(advancedOCRSetting = updatedOCRSetting)
            )
        }
    }

    fun updateSimilarityThreshold(threshold: Float) {
        _uiState.update { currentState ->
            when (currentState.usecaseSelected) {
                UsecaseState.Retail.value -> currentState.copy(
                    retailShelfSettings = currentState.retailShelfSettings
                        .copy(similarityThreshold = threshold)
                )

                UsecaseState.Product.value -> currentState.copy(
                    productRecognitionSettings = currentState.productRecognitionSettings
                        .copy(similarityThreshold = threshold)
                )

                else -> currentState
            }
        }
    }

    fun applySettings() {
        Log.d(TAG, "applySettings: Starting settings update for ${uiState.value.usecaseSelected}")

        deInitModel()

        CoroutineScope(executor!!.asCoroutineDispatcher()).launch {
            try {
                val startTime = System.currentTimeMillis()

                // Wait all retiring heavy analyzers (Retail + Product), not just current usecase
                awaitRetiringAnalyzers()

                saveSettings()
                Log.d(TAG, "applySettings: Settings saved to disk")

                initModel()

                val elapsed = System.currentTimeMillis() - startTime
                Log.d(TAG, "applySettings: Complete in ${elapsed}ms")

            } catch (e: Exception) {
                Log.e(TAG, "applySettings failed: ${e.message}", e)
                initModel()
            }
        }
    }

    private fun awaitRetiringAnalyzers() {
        retiringRetailShelfAnalyzer?.let { retiring ->
            Log.d(TAG, "Waiting for retiring RetailShelfRecognizer...")
            retiring.awaitPendingInit()
            Log.d(TAG, "Retiring RetailShelfRecognizer settled")
        }
        retiringRetailShelfAnalyzer = null

        retiringProductEnrollmentRecognition?.let { retiring ->
            Log.d(TAG, "Waiting for retiring ProductEnrollmentRecognition...")
            retiring.awaitPendingInit()
            Log.d(TAG, "Retiring ProductEnrollmentRecognition settled")
        }
        retiringProductEnrollmentRecognition = null

        retiringOcrAnalyzer?.let { retiring ->
            Log.d(TAG, "Waiting for retiring TextOCRAnalyzer...")
            retiring.awaitPendingInit()
            Log.d(TAG, "Retiring TextOCRAnalyzer settled")
        }
        retiringOcrAnalyzer = null

        retiringBarcodeAnalyzer?.let { retiring ->
            Log.d(TAG, "Waiting for retiring BarcodeAnalyzer...")
            retiring.awaitPendingInit()
            Log.d(TAG, "Retiring BarcodeAnalyzer settled")
        }
        retiringBarcodeAnalyzer = null
    }

    fun saveSettings() {
        val usecase = uiState.value.usecaseSelected
        val barcodeSettings = uiState.value.barcodeSettings
        val retailShelfSettings = uiState.value.retailShelfSettings
        val productRecognitionSettings = uiState.value.productRecognitionSettings
        val ocrBarcodeFindSettings = uiState.value.ocrBarcodeFindSettings
        val textOCRSettings = uiState.value.textOCRSettings

        viewModelScope.launch(Dispatchers.IO) {
            when (usecase) {
                UsecaseState.Barcode.value ->
                    FileUtils.saveBarcodeSettings(barcodeSettings)
                UsecaseState.Retail.value ->
                    FileUtils.saveRetailShelfSettings(retailShelfSettings)
                UsecaseState.Product.value ->
                    FileUtils.saveProductRecognitionSettings(productRecognitionSettings)
                UsecaseState.OCRBarcodeFind.value ->
                    FileUtils.saveOCRBarcodeFindSettings(ocrBarcodeFindSettings)
                UsecaseState.OCR.value -> FileUtils.saveOCRSettings(textOCRSettings)
                UsecaseState.Main.value -> {}
            }
        }
    }


    fun restoreDefaultSettings() {
        _uiState.update { currentState ->
            when (currentState.usecaseSelected) {
                UsecaseState.Barcode.value -> currentState.copy(barcodeSettings = BarcodeSettings())
                UsecaseState.Retail.value -> currentState.copy(retailShelfSettings = RetailShelfSettings())
                UsecaseState.Product.value -> currentState.copy(productRecognitionSettings = ProductRecognitionSettings())
                UsecaseState.OCRBarcodeFind.value -> currentState.copy(ocrBarcodeFindSettings = OcrBarcodeFindSettings())
                UsecaseState.OCR.value -> currentState.copy(textOCRSettings = TextOcrSettings())
                else -> currentState
            }
        }
        if (uiState.value.usecaseSelected == UsecaseState.OCRBarcodeFind.value) {
            updateOCRModelEnabled(true)
            updateBarcodeModelEnabled(true)
            restoreFiltersToDefault()
            context.getSharedPreferences("target_mode_prefs", Context.MODE_PRIVATE)
                .edit {
                    remove("target_frame_width_ratio")
                    remove("target_frame_height_ratio")
                    remove("target_frame_offset_x_ratio")
                    remove("target_frame_offset_y_ratio")
                    remove("target_frame_user_customized")
                }
        }
    }

    private fun restoreFiltersToDefault() {
        updateOcrFilterData(ocrFilterData = OcrFilterData())
        updateBarcodeFilterData(barcodeFilterData = BarcodeFilterData())
    }

    fun getString(resId: Int): String {
        return getString(context, resId)
    }

    fun postFilterMenuAction(action: FilterMenuAction) {
        _uiState.update { currentState ->
            currentState.copy(pendingFilterMenuAction = action)
        }
    }

    fun clearPendingFilterMenuAction() {
        if (_uiState.value.pendingFilterMenuAction == FilterMenuAction.NONE) return
        _uiState.update { currentState ->
            currentState.copy(pendingFilterMenuAction = FilterMenuAction.NONE)
        }
    }

    /**
     * This function is used to load a new product database into the
     * production recognition pipeline.
     * Returns false if the file is not a valid .db file.
     */
    fun loadProductIndex(uri: Uri, onResult: (Boolean) -> Unit) {
        val fileName = getFileNameFromUri(uri)
        if (fileName == null || !fileName.endsWith(".db", ignoreCase = true)) {
            onResult(false)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            // Copy to a staging file first and validate it by actually trying to open it as a
            // FeatureStorage before it's allowed to become the live products.db. Without this,
            // a corrupted/invalid file (e.g. a renamed .txt/.jpg) silently overwrites a working
            // database, and every later attempt to open Product & Shelf Enrollment/Recognizer
            // hangs trying to load it (the SDK's native open call has no timeout of its own).
            val stagingFile = File(mCacheDir, "products_import_staging.db")
            try {
                FileUtils.saveFile(uri, stagingFile.toUri())
                val isValid = productEnrollmentRecognition
                    ?.validateProductDBFile(stagingFile.absolutePath) == true
                if (isValid) {
                    val productDBFile = File(mCacheDir, databaseFile)
                    Files.move(
                        stagingFile.toPath(),
                        productDBFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                    productEnrollmentRecognition?.applyProductDB()
                    withContext(Dispatchers.Main) { onResult(true) }
                } else {
                    stagingFile.delete()
                    withContext(Dispatchers.Main) { onResult(false) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadProductIndex: import failed -> ${e.message}")
                stagingFile.delete()
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    fun exportProductDatabase(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            FileUtils.saveProductDBFile()
            withContext(Dispatchers.Main) { onComplete() }
        }
    }

    /**
     * Resolves the display name (file name) from a content URI.
     */
    private fun getFileNameFromUri(uri: Uri): String? {
        var fileName: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        fileName = it.getString(nameIndex)
                    }
                }
            }
        }
        if (fileName == null) {
            fileName = uri.lastPathSegment
        }
        return fileName
    }

    /**
     * This function is used to delete the product data from the
     * production recognition pipeline
     */
    fun deleteProductIndex() {
        viewModelScope.launch(Dispatchers.IO) {
            productEnrollmentRecognition?.deleteProductDB()
        }
    }

    /**
     * This function is used to add product data into the
     * existing product database used by the production recognition pipeline
     */
    fun enrollProductIndex() {
        if (uiState.value.bboxes.size == 0) {
            return
        }
        productEnrollmentRecognition?.enrollProductIndex(uiState.value.productResults)
    }


    fun updateBarcodeModelDemoReady(isReady: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                isBarcodeModelDemoReady = isReady
            )
        }
    }

    fun updateRetailShelfModelDemoReady(isReady: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                isRetailShelfModelDemoReady = isReady
            )
        }
    }

    fun updateOcrModelDemoReady(isReady: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                isOcrModelDemoReady = isReady
            )
        }
    }

    fun updateOcrBarcodeCaptureSessionCount(count: Int) {
        _uiState.update { currentState ->
            currentState.copy(
                ocrBarcodeCaptureSessionCount = count
            )
        }
    }

    fun updateOcrBarcodeCaptureSessionIndex(index: Int) {
        _uiState.update { currentState ->
            currentState.copy(
                ocrBarcodeCaptureSessionIndex = index
            )
        }
    }

    fun updateProductEnrollmentState(state: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                isProductEnrollmentCompleted = state
            )
        }
    }

    fun updateBarcodeResultData(results: List<ResultData>) {
        _uiState.update { it ->
            it.copy(
                barcodeResults = results
            )
        }
    }

    fun updateRetailShelfDetectionResult(results: Array<BBox?>?) {
        val bBoxesResult = results?.toList() ?: emptyList()

        _uiState.update { currentState ->
            currentState.copy(
                bboxes = bBoxesResult
            )
        }
    }

    fun updateModuleRecognitionResult(results: ModuleData) {
        _uiState.update { currentState ->
            currentState.copy(
                moduleResults = results
            )
        }
    }

    fun updateProductRecognitionResult(results: MutableList<ProductData>?) {
        val productRecognitionResult = results ?: mutableListOf()
        _uiState.update { productResults ->
            productResults.copy(
                productResults = productRecognitionResult
            )
        }
    }

    fun updateCaptureBitmap(bitmap: Bitmap?) {
        _uiState.update { productResults ->
            productResults.copy(
                captureBitmap = bitmap
            )
        }
    }

    fun updateOcrResultData(results: List<ResultData>?) {
        val ocrResults = results ?: listOf()
        _uiState.update { textResults ->
            textResults.copy(
                ocrResults = ocrResults
            )
        }
    }


//    fun updateExactMatchString(exactMatchString: String) {
//        _uiState.update { selectedExactMatchString ->
//            selectedExactMatchString.copy(
//                selectedExactMatchString = exactMatchString
//            )
//        }
//    }

    fun loadInputStreamFromAsset(fileName: String): String {
        try {
            val inputStream = assetManager.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val stringBuilder = StringBuilder()
            var line: String = reader.readLine()
            while (line != null) {
                stringBuilder.append(line)
                line = reader.readLine()
            }
            val htmlString = stringBuilder.toString();
            return htmlString

        } catch (e: IOException) {
            e.printStackTrace()
            return ""
        }
    }

    fun handleBackButton(navController: NavController) {
        val currentScreen = uiState.value.activeScreen

        if (currentScreen == Screen.DemoStart) {
            deInitModel()
            clearOcrBarcodeCaptureSession()
            updateCaptureBitmap(null)
            updateSelectedUsecase(UsecaseState.Main.value)
            updateAppBarTitle(context.getString(R.string.app_name))
        } else if (currentScreen == Screen.DemoSetting) {
            applySettings()
        } else if (currentScreen == Screen.Preview) {
            updateCameraReady(isReady = false)
            updateCameraErrorMessage(errorMessage = null)
        } else if (currentScreen == Screen.TargetModePreview) {
            updateCameraReady(isReady = false)
            updateCameraErrorMessage(errorMessage = null)
        } else if (currentScreen == Screen.ProductsCapture) {
            updateCaptureBitmap(null)
            if (uiState.value.usecaseSelected == UsecaseState.Product.value) {
                updateProductRecognitionResult(results = null)
                updateRetailShelfDetectionResult(results = null)
                updateProductEnrollmentState(state = false)
                startPreviewAnalysis()
            }
        } else if (currentScreen == Screen.RetailShelfCapture) {
            updateCaptureBitmap(null)
            if (uiState.value.usecaseSelected == UsecaseState.Retail.value) {
                updateProductRecognitionResult(results = null)
                updateRetailShelfDetectionResult(results = null)
                startPreviewAnalysis()
            }
        } else if (currentScreen == Screen.OCRBarcodeCapture) {
            updateCaptureBitmap(null)
            if (uiState.value.usecaseSelected == UsecaseState.OCRBarcodeFind.value &&
                uiState.value.isCaptureOrLiveEnabled == 0
            ) {
                startPreviewAnalysis()
            }
        } else if (currentScreen == Screen.OCRFieldConfig) {
            updateSelectedFilterType(filterType = FilterType.NONE)
        } else if (currentScreen == Screen.BarcodeFieldConfig) {
            updateSelectedFilterType(filterType = FilterType.NONE)
        }

        setZoom(1.0f)
        navController.navigateUp()
    }

    fun toast(toastString: String) {
        Toast.makeText(context, toastString, Toast.LENGTH_LONG).show()
    }

    fun updateActiveScreenData(activeScreen: Screen) {
        _uiState.update { uiStateData ->
            uiStateData.copy(
                activeScreen = activeScreen
            )
        }
    }

    fun stopPreviewAnalysis() {
        when (uiState.value.usecaseSelected) {
            UsecaseState.Barcode.value -> {

            }

            UsecaseState.Retail.value -> {
                retailShelfAnalyzer?.stopPreviewAnalysis()
            }

            UsecaseState.Product.value -> {
                productEnrollmentRecognition?.stopPreviewAnalysis()
            }

            UsecaseState.OCRBarcodeFind.value -> {

            }

            UsecaseState.OCR.value -> {

            }
        }
    }

    fun startPreviewAnalysis() {
        when (uiState.value.usecaseSelected) {
            UsecaseState.Barcode.value -> {

            }

            UsecaseState.Retail.value -> {
                retailShelfAnalyzer?.startPreviewAnalysis()
            }

            UsecaseState.Product.value -> {
                productEnrollmentRecognition?.startPreviewAnalysis()
            }

            UsecaseState.OCRBarcodeFind.value -> {

            }

            UsecaseState.OCR.value -> {

            }
        }
    }

    fun captureLatestOcrFrame() {
        ocrAnalyzer?.captureLatestOcrFrame()
    }

    fun logOcrFrameResults(
        entityList: List<com.zebra.ai.vision.entity.ParagraphEntity>,
        isLineLevel: Boolean
    ) {
        ocrAnalyzer?.logFrameResults(entityList, isLineLevel)
    }

    fun executeHighRes(highResBitmap: Bitmap) {
        when (uiState.value.usecaseSelected) {
            UsecaseState.Barcode.value -> {
            }

            UsecaseState.Retail.value -> {
                retailShelfAnalyzer?.executeHighRes(highResBitmap)
            }

            UsecaseState.Product.value -> {
                productEnrollmentRecognition?.executeHighRes(highResBitmap)
                    ?: Log.e(TAG, "ProductEnrollmentRecognition is not initialized")
            }

            UsecaseState.OCRBarcodeFind.value -> {
                if (uiState.value.isCaptureOrLiveEnabled == 0) {
                    // Build the ImageData once and share it between both decoders instead of
                    // each converting the same 8MP bitmap separately (halves the conversion cost).
                    val sharedImageData = ImageData.fromBitmap(highResBitmap, 0)
                    if (uiState.value.isOCRModelEnabled) {
                        ocrAnalyzer?.executeHighRes(sharedImageData)
                    }
                    if (uiState.value.isBarcodeModelEnabled) {
                        barcodeAnalyzer?.executeHighRes(sharedImageData)
                    }
                }
            }

            UsecaseState.OCR.value -> {
            }
        }
    }

    fun updateRecognizerInitError(message: String?) {
        _uiState.update { uiStateData ->
            uiStateData.copy(
                recognizerInitError = message
            )
        }
    }

    fun updateToastMessage(message: String?) {
        _uiState.update { uiStateData ->
            uiStateData.copy(
                toastMessage = message
            )
        }
    }

    fun updateCameraReady(isReady: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                isCameraReady = isReady
            )
        }
    }

    fun updateCameraErrorMessage(errorMessage: String?) {
        _uiState.update { currentState ->
            currentState.copy(
                cameraError = errorMessage
            )
        }
    }

    fun updateSelectedFilterType(filterType: FilterType) {
        _uiState.update { uiStateData ->
            uiStateData.copy(
                selectedFilterType = filterType
            )
        }
    }

    fun isRetailPreviewAnalysisStopped(): Boolean {
        return retailShelfAnalyzer?.isPreviewAnalysisStopped() ?: false
    }

    fun updateCaptureModuleRecognitionResult(results: ModuleData) {
        Log.d(
            TAG,
            "Capture result - Shelves: ${results.shelves.size}, Labels: ${results.labelEntity.size}, Products: ${results.productEntity.size}"
        )
        results.productEntity.forEach { product ->
            run {
                Log.d("Topskus size", "" + product.topKSKUs.size)
            }
        }
        // Convert the SDK result to ModuleData and store it
        _uiState.update { currentState ->
            currentState.copy(
                captureModuleResults = results,
                isCaptureProcessing = false
            )
        }
    }

    fun clearOcrBarcodeCaptureSession() {
        updateOcrBarcodeCaptureSessionIndex(0)
        updateOcrBarcodeCaptureSessionCount(0)
        viewModelScope.launch(Dispatchers.IO) {
            clearOcrBarcodeCaptureSessionPrefs(context)
        }
    }
}
