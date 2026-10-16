// Copyright (c) 2024-2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.aidatacapturedemo.model

import android.util.Log
import androidx.lifecycle.Lifecycle
import com.zebra.ai.vision.detector.AIVisionSDKException
import com.zebra.ai.vision.detector.BarcodeDecoder
import com.zebra.ai.vision.detector.ImageData
import com.zebra.ai.vision.detector.InvalidInputException
import com.zebra.ai.vision.entity.BarcodeEntity
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.data.PROFILING
import com.zebra.aidatacapturedemo.data.ResultData
import com.zebra.aidatacapturedemo.data.UsecaseState
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.Collections.synchronizedList
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger


/**
 * [BarcodeAnalyzer] class is used to detect & Track barcodes found on the Camera Live Preview
 *
 * @param uiState - Used to read all the UI Current State
 * @param viewModel - Used to write any UI State Changes via [AIDataCaptureDemoViewModel]
 */
class BarcodeAnalyzer(
    val uiState: StateFlow<AIDataCaptureDemoUiState>,
    val viewModel: AIDataCaptureDemoViewModel
) {

    private lateinit var mActivityLifecycle: Lifecycle
    private val TAG = "BarcodeAnalyzer"
    private var barcodeDecoder: BarcodeDecoder? = null

    //    private val decoderSettings = BarcodeDecoder.Settings("barcode-localizer")
    private val decoderSettings = BarcodeDecoder.Settings("barcode-decoder")
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    /** Guards barcodeDecoder so initialize()/deinitialize()/executeHighRes() never touch the
     * native object concurrently — doing so crashes natively with no Java stack trace, same
     * risk ProductEnrollmentRecognition/RetailShelfRecognizer already guard against. */
    private val pipelineLock = Mutex()

    private var isRetiring = false
    private var disposeJob: Job? = null

    // Incremented on every initialize()/deinitialize() call. Async init callbacks compare
    // their captured generation against the current one before assigning a native instance,
    // so a stale callback from a superseded init disposes its instance instead of leaking it.
    private val initGeneration = AtomicInteger(0)

    // Futures for native inits still in flight when deinitialize() runs. Awaited by
    // awaitPendingInit() before a new initialize() call starts, so we never have two full
    // native BarcodeDecoder instances allocated at once.
    private val pendingFutures = synchronizedList(mutableListOf<CompletableFuture<*>>())

    /**
     * initialize function is used to initialize the BarcodeDecoder for Barcode Analyzer
     * use case. It configures the model settings based on the current UI state and creates an
     * instance of BarcodeDecoder. If the initialization fails due to unsupported inference type,
     * it updates the UI with appropriate messages and takes corrective actions.
     */
    fun initialize() {
        deinitialize()
        isRetiring = false
        updateBarcodeModelDemoReady(false)

        val myGeneration = initGeneration.get()
        attemptInit(myGeneration, allowFallbackRetry = true)
    }

    /**
     * Attempts to create the native BarcodeDecoder instance for [myGeneration]. If creation
     * fails because the selected inference type (e.g. GPU) isn't supported on this device,
     * switches the setting to Auto-select and — when [allowFallbackRetry] is true — immediately
     * retries once with that corrected setting, instead of only persisting it for the next app
     * launch and leaving this session's init (and whatever is awaiting
     * isBarcodeModelDemoReady) stalled until it times out (MCAIS-2846). [allowFallbackRetry] is
     * false on that retry, so a second failure under Auto-select surfaces as a real init error
     * rather than looping.
     */
    private fun attemptInit(myGeneration: Int, allowFallbackRetry: Boolean) {
        try {
            configure()

            val mStart = System.currentTimeMillis()
            val future = BarcodeDecoder.getBarcodeDecoder(decoderSettings, executorService)
                .thenAccept { barcodeDecoderInstance: BarcodeDecoder ->
                    if (myGeneration != initGeneration.get()) {
                        Log.w(
                            TAG,
                            "BarcodeAnalyzer was deinitialized before init completed – disposing leaked decoder"
                        )
                        barcodeDecoderInstance.dispose()
                        return@thenAccept
                    }
                    barcodeDecoder = barcodeDecoderInstance
                    updateBarcodeModelDemoReady(true)
                    Log.e(
                        PROFILING,
                        "BarcodeAnalyzer obj creation / model loading time = ${System.currentTimeMillis() - mStart} milli sec"
                    )
                    Log.i(TAG, "BarcodeAnalyzer init Success")
                }.exceptionally { e: Throwable ->
                    if (myGeneration != initGeneration.get()) return@exceptionally null
                    Log.e(TAG, "BarcodeAnalyzer init Failed -> " + e.message)
                    val isUnsupportedRuntime = e.message?.contains("Given runtimes are not available") == true ||
                            e.message?.contains("Initialize barcodeDecoder due to SNPE exception") == true
                    when {
                        isUnsupportedRuntime && allowFallbackRetry -> {
                            viewModel.updateToastMessage(message = "Selected inference type is not supported on this device. Switching to Auto-select for optimal performance.")
                            viewModel.updateSelectedProcessor(0) //Auto-Select
                            viewModel.saveSettings()
                            attemptInit(myGeneration, allowFallbackRetry = false)
                        }
                        isUnsupportedRuntime -> viewModel.updateRecognizerInitError(
                            "Barcode could not be initialized on this device, even with Auto-select."
                        )
                    }
                    null
                }
            pendingFutures.add(future)
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "initialize() ran out of memory loading BarcodeDecoder: ${e.message}")
            viewModel.updateRecognizerInitError(
                "Not enough memory to load the model at the current settings. Close other apps and try again, or lower resolution/model input size."
            )
            viewModel.handleMemoryPressureDetected("model init")
        } catch (ex: IOException) {
            Log.e(TAG, "getBarcodeDecoder init Failed -> " + ex.message)
        }
    }

    /** Native dispose is deferred to [disposeJob], which waits for any in-flight
     * executeHighRes() call on this instance to finish first — disposing while a call is
     * still inside native process() crashes natively with no Java stack trace. */
    fun deinitialize() {
        initGeneration.incrementAndGet()
        isRetiring = true
        disposeJob = scope.launch {
            pipelineLock.withLock {
                barcodeDecoder?.dispose()
                barcodeDecoder = null
            }
        }
    }

    /**
     * Blocks until any native inits still in flight from a superseded initialize() call have
     * settled, and until this instance's deferred dispose from deinitialize() has settled.
     * Must be called off the main thread, before starting a new initialize() call on this
     * (now-retired) instance, to avoid two full native BarcodeDecoder instances being allocated
     * at once.
     */
    fun awaitPendingInit() {
        val toAwait: List<CompletableFuture<*>>
        synchronized(pendingFutures) {
            toAwait = pendingFutures.toList()
            pendingFutures.clear()
        }
        toAwait.forEach { future ->
            try {
                future.get(10, TimeUnit.SECONDS)
            } catch (e: Exception) {
                Log.w(TAG, "Timed out waiting for a superseded init to settle:${e.message}")
            }
        }
        disposeJob?.let { pendingDispose ->
            try {
                runBlocking {
                    withTimeoutOrNull(10_000) { pendingDispose.join() }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Timed out waiting for deferred dispose to settle:${e.message}")
            }
        }
        executorService.shutdown()
        job.cancel()
    }

    fun getDetector(): BarcodeDecoder? {
        return barcodeDecoder
    }

    /** executeHighRes function takes a high resolution bitmap as input and processes it using the
     * BarcodeDecoder instance. It runs the processing in a background thread using an ExecutorService.
     * The function handles exceptions that may occur during processing and logs appropriate messages.
     * Upon successful processing, it calls the onDetectionBarcodeResult function with the results.
     */
    fun executeHighRes(imageData: ImageData) {
        executorService.submit {
            runBlocking {
                pipelineLock.withLock {
                    try {
                        if (isRetiring) return@withLock
                        barcodeDecoder?.process(imageData)?.thenAccept { result ->
                            onDetectionBarcodeResult(result)
                        }
                    } catch (e: OutOfMemoryError) {
                        Log.e(TAG, "executeHighRes ran out of memory: ${e.message}")
                        viewModel.handleMemoryPressureDetected("capture")
                    } catch (e: InvalidInputException) {
                        Log.e(TAG, e.message ?: "InvalidInputException occurred")
                    } catch (e: AIVisionSDKException) {
                        Log.e(TAG, e.message ?: "AIVisionSDKException occurred")
                    }
                }
            }
        }
    }

    private fun configure() {
        try {
            if (uiState.value.usecaseSelected == UsecaseState.OCRBarcodeFind.value) {
                //Swap the values as the presented index is reverse of what model expects
                val processorOrder =
                    when (uiState.value.ocrBarcodeFindSettings.commonSettings.processorSelectedIndex) {
                        0 -> arrayOf(2, 0, 1) // AUTO
                        1 -> arrayOf(2) // DSP
                        2 -> arrayOf(1) // GPU
                        3 -> arrayOf(0) //CPU
                        else -> {
                            arrayOf(2, 0, 1)
                        }
                    }
                decoderSettings.detectorSetting.inferencerOptions.runtimeProcessorOrder =
                    processorOrder

                decoderSettings.detectorSetting.inferencerOptions.defaultDims.width =
                    uiState.value.ocrBarcodeFindSettings.commonSettings.inputSizeSelected
                decoderSettings.detectorSetting.inferencerOptions.defaultDims.height =
                    uiState.value.ocrBarcodeFindSettings.commonSettings.inputSizeSelected

                decoderSettings.enableAIBarcodeDecode =
                    uiState.value.ocrBarcodeFindSettings.commonSettings.enableAIBarcodeDecode

                decoderSettings.dpmSettings.mode =
                    when (uiState.value.ocrBarcodeFindSettings.commonSettings.dpmModeSelectedIndex) {
                        1 -> BarcodeDecoder.Settings.DpmSettings.DpmMode.SMALL
                        2 -> BarcodeDecoder.Settings.DpmSettings.DpmMode.LARGE
                        else -> BarcodeDecoder.Settings.DpmSettings.DpmMode.DISABLED
                    }

                decoderSettings.Symbology.AUSTRALIAN_POSTAL.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.australian_postal)
                decoderSettings.Symbology.AZTEC.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.aztec)
                decoderSettings.Symbology.CANADIAN_POSTAL.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.canadian_postal)
                decoderSettings.Symbology.CHINESE_2OF5.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.chinese_2of5)
                decoderSettings.Symbology.CODABAR.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.codabar)
                decoderSettings.Symbology.CODE11.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.code11)
                decoderSettings.Symbology.CODE39.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.code39)
                decoderSettings.Symbology.CODE93.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.code93)
                decoderSettings.Symbology.CODE128.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.code128)
                decoderSettings.Symbology.COMPOSITE_AB.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.composite_ab)
                decoderSettings.Symbology.COMPOSITE_C.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.composite_c)
                decoderSettings.Symbology.D2OF5.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.d2of5)
                decoderSettings.Symbology.DATAMATRIX.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.datamatrix)
                decoderSettings.Symbology.DOTCODE.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.dotcode)
                decoderSettings.Symbology.DUTCH_POSTAL.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.dutch_postal)
                decoderSettings.Symbology.EAN8.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.ean_8)
                decoderSettings.Symbology.EAN13.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.ean_13)
                decoderSettings.Symbology.FINNISH_POSTAL_4S.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.finnish_postal_4s)
                decoderSettings.Symbology.GRID_MATRIX.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.grid_matrix)
                decoderSettings.Symbology.GS1_DATABAR.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.gs1_databar)
                decoderSettings.Symbology.GS1_DATABAR_EXPANDED.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.gs1_databar_expanded)
                decoderSettings.Symbology.GS1_DATABAR_LIM.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.gs1_databar_lim)
                decoderSettings.Symbology.GS1_DATAMATRIX.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.gs1_datamatrix)
                decoderSettings.Symbology.GS1_QRCODE.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.gs1_qrcode)
                decoderSettings.Symbology.HANXIN.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.hanxin)
                decoderSettings.Symbology.I2OF5.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.i2of5)
                decoderSettings.Symbology.JAPANESE_POSTAL.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.japanese_postal)
                decoderSettings.Symbology.KOREAN_3OF5.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.korean_3of5)
                decoderSettings.Symbology.MAILMARK.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.mailmark)
                decoderSettings.Symbology.MATRIX_2OF5.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.matrix_2of5)
                decoderSettings.Symbology.MAXICODE.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.maxicode)
                decoderSettings.Symbology.MICROPDF.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.micropdf)
                decoderSettings.Symbology.MICROQR.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.microqr)
                decoderSettings.Symbology.MSI.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.msi)
                decoderSettings.Symbology.PDF417.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.pdf417)
                decoderSettings.Symbology.QRCODE.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.qrcode)
                decoderSettings.Symbology.TLC39.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.tlc39)
                decoderSettings.Symbology.TRIOPTIC39.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.trioptic39)
                decoderSettings.Symbology.UK_POSTAL.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.uk_postal)
                decoderSettings.Symbology.UPCA.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.upc_a)
                decoderSettings.Symbology.UPCE0.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.upce0)
                decoderSettings.Symbology.UPCE1.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.upce1)
                decoderSettings.Symbology.USPLANET.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.usplanet)
                decoderSettings.Symbology.USPOSTNET.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.uspostnet)
                decoderSettings.Symbology.US4STATE.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.us4state)
                decoderSettings.Symbology.US4STATE_FICS.enable(uiState.value.ocrBarcodeFindSettings.barcodeSymbology.us4state_fics)
            } else {
                //Swap the values as the presented index is reverse of what model expects
                val processorOrder =
                    when (uiState.value.barcodeSettings.commonSettings.processorSelectedIndex) {
                        0 -> arrayOf(2, 0, 1) // AUTO
                        1 -> arrayOf(2) // DSP
                        2 -> arrayOf(1) // GPU
                        3 -> arrayOf(0) //CPU
                        else -> {
                            arrayOf(2, 0, 1)
                        }
                    }
                decoderSettings.detectorSetting.inferencerOptions.runtimeProcessorOrder =
                    processorOrder

                decoderSettings.detectorSetting.inferencerOptions.defaultDims.width =
                    uiState.value.barcodeSettings.commonSettings.inputSizeSelected
                decoderSettings.detectorSetting.inferencerOptions.defaultDims.height =
                    uiState.value.barcodeSettings.commonSettings.inputSizeSelected

                decoderSettings.enableAIBarcodeDecode =
                    uiState.value.barcodeSettings.commonSettings.enableAIBarcodeDecode

                decoderSettings.dpmSettings.mode =
                    when (uiState.value.barcodeSettings.commonSettings.dpmModeSelectedIndex) {
                        1 -> BarcodeDecoder.Settings.DpmSettings.DpmMode.SMALL
                        2 -> BarcodeDecoder.Settings.DpmSettings.DpmMode.LARGE
                        else -> BarcodeDecoder.Settings.DpmSettings.DpmMode.DISABLED
                    }

                decoderSettings.Symbology.AUSTRALIAN_POSTAL.enable(uiState.value.barcodeSettings.barcodeSymbology.australian_postal)
                decoderSettings.Symbology.AZTEC.enable(uiState.value.barcodeSettings.barcodeSymbology.aztec)
                decoderSettings.Symbology.CANADIAN_POSTAL.enable(uiState.value.barcodeSettings.barcodeSymbology.canadian_postal)
                decoderSettings.Symbology.CHINESE_2OF5.enable(uiState.value.barcodeSettings.barcodeSymbology.chinese_2of5)
                decoderSettings.Symbology.CODABAR.enable(uiState.value.barcodeSettings.barcodeSymbology.codabar)
                decoderSettings.Symbology.CODE11.enable(uiState.value.barcodeSettings.barcodeSymbology.code11)
                decoderSettings.Symbology.CODE39.enable(uiState.value.barcodeSettings.barcodeSymbology.code39)
                decoderSettings.Symbology.CODE93.enable(uiState.value.barcodeSettings.barcodeSymbology.code93)
                decoderSettings.Symbology.CODE128.enable(uiState.value.barcodeSettings.barcodeSymbology.code128)
                decoderSettings.Symbology.COMPOSITE_AB.enable(uiState.value.barcodeSettings.barcodeSymbology.composite_ab)
                decoderSettings.Symbology.COMPOSITE_C.enable(uiState.value.barcodeSettings.barcodeSymbology.composite_c)
                decoderSettings.Symbology.D2OF5.enable(uiState.value.barcodeSettings.barcodeSymbology.d2of5)
                decoderSettings.Symbology.DATAMATRIX.enable(uiState.value.barcodeSettings.barcodeSymbology.datamatrix)
                decoderSettings.Symbology.DOTCODE.enable(uiState.value.barcodeSettings.barcodeSymbology.dotcode)
                decoderSettings.Symbology.DUTCH_POSTAL.enable(uiState.value.barcodeSettings.barcodeSymbology.dutch_postal)
                decoderSettings.Symbology.EAN8.enable(uiState.value.barcodeSettings.barcodeSymbology.ean_8)
                decoderSettings.Symbology.EAN13.enable(uiState.value.barcodeSettings.barcodeSymbology.ean_13)
                decoderSettings.Symbology.FINNISH_POSTAL_4S.enable(uiState.value.barcodeSettings.barcodeSymbology.finnish_postal_4s)
                decoderSettings.Symbology.GRID_MATRIX.enable(uiState.value.barcodeSettings.barcodeSymbology.grid_matrix)
                decoderSettings.Symbology.GS1_DATABAR.enable(uiState.value.barcodeSettings.barcodeSymbology.gs1_databar)
                decoderSettings.Symbology.GS1_DATABAR_EXPANDED.enable(uiState.value.barcodeSettings.barcodeSymbology.gs1_databar_expanded)
                decoderSettings.Symbology.GS1_DATABAR_LIM.enable(uiState.value.barcodeSettings.barcodeSymbology.gs1_databar_lim)
                decoderSettings.Symbology.GS1_DATAMATRIX.enable(uiState.value.barcodeSettings.barcodeSymbology.gs1_datamatrix)
                decoderSettings.Symbology.GS1_QRCODE.enable(uiState.value.barcodeSettings.barcodeSymbology.gs1_qrcode)
                decoderSettings.Symbology.HANXIN.enable(uiState.value.barcodeSettings.barcodeSymbology.hanxin)
                decoderSettings.Symbology.I2OF5.enable(uiState.value.barcodeSettings.barcodeSymbology.i2of5)
                decoderSettings.Symbology.JAPANESE_POSTAL.enable(uiState.value.barcodeSettings.barcodeSymbology.japanese_postal)
                decoderSettings.Symbology.KOREAN_3OF5.enable(uiState.value.barcodeSettings.barcodeSymbology.korean_3of5)
                decoderSettings.Symbology.MAILMARK.enable(uiState.value.barcodeSettings.barcodeSymbology.mailmark)
                decoderSettings.Symbology.MATRIX_2OF5.enable(uiState.value.barcodeSettings.barcodeSymbology.matrix_2of5)
                decoderSettings.Symbology.MAXICODE.enable(uiState.value.barcodeSettings.barcodeSymbology.maxicode)
                decoderSettings.Symbology.MICROPDF.enable(uiState.value.barcodeSettings.barcodeSymbology.micropdf)
                decoderSettings.Symbology.MICROQR.enable(uiState.value.barcodeSettings.barcodeSymbology.microqr)
                decoderSettings.Symbology.MSI.enable(uiState.value.barcodeSettings.barcodeSymbology.msi)
                decoderSettings.Symbology.PDF417.enable(uiState.value.barcodeSettings.barcodeSymbology.pdf417)
                decoderSettings.Symbology.QRCODE.enable(uiState.value.barcodeSettings.barcodeSymbology.qrcode)
                decoderSettings.Symbology.TLC39.enable(uiState.value.barcodeSettings.barcodeSymbology.tlc39)
                decoderSettings.Symbology.TRIOPTIC39.enable(uiState.value.barcodeSettings.barcodeSymbology.trioptic39)
                decoderSettings.Symbology.UK_POSTAL.enable(uiState.value.barcodeSettings.barcodeSymbology.uk_postal)
                decoderSettings.Symbology.UPCA.enable(uiState.value.barcodeSettings.barcodeSymbology.upc_a)
                decoderSettings.Symbology.UPCE0.enable(uiState.value.barcodeSettings.barcodeSymbology.upce0)
                decoderSettings.Symbology.UPCE1.enable(uiState.value.barcodeSettings.barcodeSymbology.upce1)
                decoderSettings.Symbology.USPLANET.enable(uiState.value.barcodeSettings.barcodeSymbology.usplanet)
                decoderSettings.Symbology.USPOSTNET.enable(uiState.value.barcodeSettings.barcodeSymbology.uspostnet)
                decoderSettings.Symbology.US4STATE.enable(uiState.value.barcodeSettings.barcodeSymbology.us4state)
                decoderSettings.Symbology.US4STATE_FICS.enable(uiState.value.barcodeSettings.barcodeSymbology.us4state_fics)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error: configure failed - ${e.message}")
        }
    }

    private fun updateBarcodeModelDemoReady(isReady: Boolean) {
        viewModel.updateBarcodeModelDemoReady(isReady = isReady)
    }

    private fun onDetectionBarcodeResult(entityList: List<BarcodeEntity?>?) {
        var rectList: MutableList<ResultData> = mutableListOf()
        entityList?.forEach { entity ->
            if (entity != null) {
                val value = entity.value
                val rect = entity.boundingBox
                rectList += ResultData(boundingBox = rect, text = value)
            }
        }

        // In OCR+Barcode Find mode, hide undecoded barcode boxes when "Show detected barcodes" is off.
        val hideUndecoded =
            uiState.value.usecaseSelected == UsecaseState.OCRBarcodeFind.value &&
                    !uiState.value.ocrBarcodeFindSettings.feedbackSettings.showDetectedBarcode

        if (hideUndecoded) {
            // Keep only decoded barcodes (non-empty text payload).
            rectList = rectList.filter { it.text.isNotBlank() }.toMutableList()
        }

        viewModel.updateBarcodeResultData(
            results = FilterUtils.getBarcodeFilteredResultData(
                uiState = uiState.value,
                outputBarcodeResultData = rectList
            )
        )
    }
}
