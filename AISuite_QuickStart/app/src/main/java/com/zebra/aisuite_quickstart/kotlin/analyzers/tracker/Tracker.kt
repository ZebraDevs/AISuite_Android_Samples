// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.analyzers.tracker

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import com.zebra.ai.vision.analyzer.tracking.EntityTrackerAnalyzer
import com.zebra.ai.vision.detector.AIVisionSDKLicenseException
import com.zebra.ai.vision.detector.BarcodeDecoder
import com.zebra.ai.vision.detector.Detector
import com.zebra.ai.vision.detector.ImageData
import com.zebra.ai.vision.detector.InferencerOptions
import com.zebra.ai.vision.detector.ModuleRecognizer
import com.zebra.ai.vision.detector.TextOCR
import com.zebra.ai.vision.entity.Entity
import com.zebra.aisuite_quickstart.filtertracker.FilterDialog
import java.io.BufferedOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Tracker(
    private val context: Context,
    private val callback: DetectionCallback,
    private val imageAnalysis: ImageAnalysis,
    private val selectedFilterItems: MutableList<String?>,
    private val loadingCallback: ((Boolean) -> Unit)? = null
) {

    interface DetectionCallback {
        fun handleEntities(result: EntityTrackerAnalyzer.Result)
        fun handleCaptureFrameEntities(barcodeEntities: List<out Entity>?, ocrEntities: List<out Entity>?, moduleEntities: List<out Entity>?
        )
    }

    companion object {
        private const val TAG = "Tracker"
        private const val LIVE_PREVIEW_SIZE = 640
        private const val CAPTURE_SIZE = 1280 // Higher resolution for capture
    }

    // Live preview instances
    private var barcodeDecoder: BarcodeDecoder? = null
    private var textOCR: TextOCR? = null
    private var moduleRecognizer: ModuleRecognizer? = null

    // Capture instances
    private var captureBarcodeDecoder: BarcodeDecoder? = null
    private var captureOcr: TextOCR? = null
    private var captureModuleRecognizer: ModuleRecognizer? = null

    private var executor: ExecutorService = Executors.newFixedThreadPool(3)
    private val captureExecutor: ExecutorService = Executors.newFixedThreadPool(3)
    var entityTrackerAnalyzer: EntityTrackerAnalyzer? = null
    private val mavenModelName = "barcode-localizer"
    private val mavenOCRModelName = "text-ocr-recognizer"
    private val mavenProductModelName = "product-and-shelf-recognizer"

    private val analyzerList: MutableList<Detector<out MutableList<out Entity?>?>?> = ArrayList()
    private val captureAnalyzerList: MutableList<Detector<out MutableList<out Entity?>?>?> = ArrayList()

    private var modelsLoaded = false
    var captureModelsLoaded = false

    init {
        if (selectedFilterItems.isNotEmpty()) {
            for (item in selectedFilterItems) {
                if (item.equals(FilterDialog.BARCODE_TRACKER, ignoreCase = true)) {
                    initializeBarcodeDecoder()
                    initializeCaptureBarcodeDecoder()
                } else if (item.equals(FilterDialog.OCR_TRACKER, ignoreCase = true)) {
                    initializeTextOCR()
                    initializeCaptureOcr()
                } else if (item.equals(FilterDialog.PRODUCT_AND_SHELF, ignoreCase = true)) {
                    initializeModuleRecognizer()
                    initializeCaptureModuleRecognizer()
                }
            }
        } else {
            loadingCallback?.invoke(false)
            Log.d(TAG, "None of the filter selected")
        }
    }


    private fun getProcessorOrder(): Array<Int> =
        arrayOf(InferencerOptions.DSP, InferencerOptions.CPU, InferencerOptions.GPU)

    private fun configureInferencerOptions(settings: Any) {
        val rpo = getProcessorOrder()
        when (settings) {
            is BarcodeDecoder.Settings -> {
                settings.Symbology.CODE39.enable(true)
                settings.Symbology.CODE128.enable(true)
                settings.detectorSetting.inferencerOptions.runtimeProcessorOrder = rpo
                settings.detectorSetting.inferencerOptions.defaultDims.height = 640
                settings.detectorSetting.inferencerOptions.defaultDims.width = 640
            }
            is TextOCR.Settings -> {
                settings.detectionInferencerOptions.runtimeProcessorOrder = rpo
                settings.recognitionInferencerOptions.runtimeProcessorOrder = rpo
                settings.detectionInferencerOptions.defaultDims.height = 640
                settings.detectionInferencerOptions.defaultDims.width = 640
            }
        }
    }

    /**
     * Creates barcode decoder settings with specified input size.
     */
    private fun createBarcodeDecoderSettings(inputSize: Int): BarcodeDecoder.Settings {
        return BarcodeDecoder.Settings(mavenModelName).apply {
            val rpo = getProcessorOrder()
            Symbology.CODE39.enable(true)
            Symbology.CODE128.enable(true)
            detectorSetting.inferencerOptions.runtimeProcessorOrder = rpo
            detectorSetting.inferencerOptions.defaultDims.height = inputSize
            detectorSetting.inferencerOptions.defaultDims.width = inputSize
        }
    }

    /**
     * Creates TextOCR settings with specified input size.
     */
    private fun createTextOCRSettings(inputSize: Int): TextOCR.Settings {
        return TextOCR.Settings(mavenOCRModelName).apply {
            val rpo = getProcessorOrder()
            detectionInferencerOptions.runtimeProcessorOrder = rpo
            recognitionInferencerOptions.runtimeProcessorOrder = rpo
            detectionInferencerOptions.defaultDims.height = inputSize
            detectionInferencerOptions.defaultDims.width = inputSize
        }
    }

    /**
     * Creates module recognizer settings with specified input size.
     */
    private fun createModuleRecognizerSettings(inputSize: Int): ModuleRecognizer.Settings {
        val indexFilename = "product.index"
        val labelsFilename = "product.txt"
        val toPath = "${context.filesDir}/"
        copyFromAssets(indexFilename, toPath)
        copyFromAssets(labelsFilename, toPath)

        return ModuleRecognizer.Settings(mavenProductModelName).apply {
            inferencerOptions.apply {
                runtimeProcessorOrder = getProcessorOrder()
                defaultDims.height = inputSize
                defaultDims.width = inputSize
            }
            enableProductRecognitionWithIndex(
                mavenProductModelName,
                "$toPath$indexFilename",
                "$toPath$labelsFilename"
            )
        }
    }

    // ========================
    // Live Preview Initializers
    // ========================

    fun initializeBarcodeDecoder() {
        try {
            val liveDecoderSettings = createBarcodeDecoderSettings(LIVE_PREVIEW_SIZE)
            createBarcodeDecoderWithFallback(liveDecoderSettings)
        } catch (ex: Exception) {
            loadingCallback?.invoke(false)
            Log.e(TAG, "Model Loading: Barcode decoder returned with exception ${ex.message}")
        }
    }

    private fun createBarcodeDecoderWithFallback(settings: BarcodeDecoder.Settings) {
        val startTime = System.currentTimeMillis()
        BarcodeDecoder.getBarcodeDecoder(settings, executor).thenAccept { decoder ->
            barcodeDecoder = decoder
            createAnalyzer(listOfNotNull(barcodeDecoder))
            Log.d(TAG, "BarcodeDecoder() obj creation time = ${System.currentTimeMillis() - startTime} ms")
        }.exceptionally { throwable ->
            loadingCallback?.invoke(false)
            handleException(throwable)
            null
        }
    }

    private fun initializeTextOCR() {
        try {
            val liveOCRSettings = createTextOCRSettings(LIVE_PREVIEW_SIZE)
            createTextOCRWithFallback(liveOCRSettings)
        } catch (e: Exception) {
            loadingCallback?.invoke(false)
            Log.e(TAG, "Fatal error: load failed - ${e.message}")
        }
    }

    private fun createTextOCRWithFallback(settings: TextOCR.Settings) {
        val startTime = System.currentTimeMillis()
        TextOCR.getTextOCR(settings, executor).thenAccept { ocr ->
            textOCR = ocr
            createAnalyzer(listOfNotNull(textOCR))
            Log.d(TAG, "TextOCR() obj creation / model loading time = ${System.currentTimeMillis() - startTime} ms")
        }.exceptionally { throwable ->
            loadingCallback?.invoke(false)
            handleException(throwable)
            null
        }
    }

    fun initializeModuleRecognizer() {
        try {
            Log.i(TAG, "Initializing ModuleRecognizer for Product Recognition")
            val liveRecognizerSettings = createModuleRecognizerSettings(LIVE_PREVIEW_SIZE)
            createModuleRecognizerWithFallback(liveRecognizerSettings)
        } catch (e: Exception) {
            loadingCallback?.invoke(false)
            Log.e(TAG, "Fatal error during initialization setup: ${e.message}")
        }
    }

    private fun createModuleRecognizerWithFallback(settings: ModuleRecognizer.Settings) {
        val startTime = System.currentTimeMillis()
        ModuleRecognizer.getModuleRecognizer(settings, executor)
            .thenAccept { recognizerInstance ->
                Log.i(TAG, "ModuleRecognizer instance created successfully for Product Recognition")
                moduleRecognizer = recognizerInstance
                createAnalyzer(listOfNotNull(moduleRecognizer))
                Log.d(TAG, "Product Recognition creation time: ${System.currentTimeMillis() - startTime}ms")
            }
            .exceptionally { throwable ->
                loadingCallback?.invoke(false)
                Log.e(TAG, "Failed to create ModuleRecognizer for Product Recognition: ${throwable.message}")
                null
            }
    }

    // ========================
    // Capture Initializers
    // ========================

    /**
     * Initializes the capture barcode decoder with higher resolution settings.
     */
    fun initializeCaptureBarcodeDecoder() {
        try {
            val captureDecoderSettings = createBarcodeDecoderSettings(CAPTURE_SIZE)
            createCaptureBarcodeDecoderWithFallback(captureDecoderSettings)
        } catch (ex: Exception) {
            loadingCallback?.invoke(false)
            Log.e(TAG, "Capture barcode decoder initialization failed: ${ex.message}")
        }
    }

    private fun createCaptureBarcodeDecoderWithFallback(settings: BarcodeDecoder.Settings) {
        val startTime = System.currentTimeMillis()
        BarcodeDecoder.getBarcodeDecoder(settings, captureExecutor).thenAccept { decoderInstance ->
            captureBarcodeDecoder = decoderInstance
            createCaptureAnalyzer(listOfNotNull(captureBarcodeDecoder))
            Log.d(TAG, "Capture BarcodeDecoder() obj creation time = ${System.currentTimeMillis() - startTime} ms")
        }.exceptionally { e ->
            loadingCallback?.invoke(false)
            Log.e(TAG, "Capture barcode decoder creation failed: ${e.message}")
            null
        }
    }

    /**
     * Initializes the capture OCR scanner with higher resolution settings.
     */
    fun initializeCaptureOcr() {
        try {
            val captureOcrSettings = createTextOCRSettings(CAPTURE_SIZE)
            createCaptureTextOCRWithFallback(captureOcrSettings)
        } catch (ex: Exception) {
            loadingCallback?.invoke(false)
            Log.e(TAG, "Capture OCR scanner initialization failed: ${ex.message}")
        }
    }

    private fun createCaptureTextOCRWithFallback(settings: TextOCR.Settings) {
        val startTime = System.currentTimeMillis()
        TextOCR.getTextOCR(settings, captureExecutor).thenAccept { ocrInstance ->
            captureOcr = ocrInstance
            createCaptureAnalyzer(listOfNotNull(captureOcr))
            Log.d(TAG, "Capture TextOCR() obj creation / model loading time = ${System.currentTimeMillis() - startTime} ms")
        }.exceptionally { e ->
            loadingCallback?.invoke(false)
            Log.e(TAG, "Capture OCR scanner creation failed: ${e.message}")
            null
        }
    }

    /**
     * Initializes the capture module recognizer with higher resolution settings.
     */
    fun initializeCaptureModuleRecognizer() {
        try {
            val captureModuleSettings = createModuleRecognizerSettings(CAPTURE_SIZE)
            createCaptureModuleRecognizerWithFallback(captureModuleSettings)
        } catch (ex: Exception) {
            loadingCallback?.invoke(false)
            Log.e(TAG, "Capture module recognizer initialization failed: ${ex.message}")
        }
    }

    private fun createCaptureModuleRecognizerWithFallback(settings: ModuleRecognizer.Settings) {
        val startTime = System.currentTimeMillis()
        ModuleRecognizer.getModuleRecognizer(settings, captureExecutor).thenAccept { moduleInstance ->
            captureModuleRecognizer = moduleInstance
            createCaptureAnalyzer(listOfNotNull(captureModuleRecognizer))
            Log.d(TAG, "Capture ModuleRecognizer Creation Time: ${System.currentTimeMillis() - startTime}ms")
        }.exceptionally { e ->
            loadingCallback?.invoke(false)
            Log.e(TAG, "Capture module recognizer creation failed: ${e.message}")
            null
        }
    }


    /**
     * Creates and configures an EntityTrackerAnalyzer with the provided analyzers.
     * Tracks loading of live preview models. Only attaches analyzer when both
     * live and capture models are fully loaded.
     */
    @Synchronized
    private fun createAnalyzer(analyzers: List<Detector<out MutableList<out Entity>>>) {
        analyzerList.add(analyzers[0])
        if (analyzerList.size == selectedFilterItems.size) {
            modelsLoaded = true
            if (captureModelsLoaded) {
                loadingCallback?.invoke(true)
                attachAnalysisAfterModelLoading()
            }
        }
    }

    /**
     * Tracks loading of capture models. Only attaches analyzer when both
     * live and capture models are fully loaded.
     */
    @Synchronized
    private fun createCaptureAnalyzer(analyzers: List<Detector<out MutableList<out Entity>>>) {
        captureAnalyzerList.add(analyzers[0])
        if (captureAnalyzerList.size == selectedFilterItems.size) {
            if (modelsLoaded) {
                loadingCallback?.invoke(true)
                attachAnalysisAfterModelLoading()
            }
            captureModelsLoaded = true
        }
    }

    /**
     * Attaches the EntityTrackerAnalyzer to the ImageAnalysis once both live preview
     * and capture models are loaded.
     */
    fun attachAnalysisAfterModelLoading() {
        entityTrackerAnalyzer = EntityTrackerAnalyzer(
            analyzerList,
            ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
            executor,
            ::handleEntities
        )
        imageAnalysis.setAnalyzer(
            ContextCompat.getMainExecutor(context),
            entityTrackerAnalyzer!!
        )
    }

    // ========================
    // Capture Processing
    // ========================

    /**
     * Processes a captured image using all active capture detectors in parallel.
     * Results are delivered via handleCaptureFrameEntities callback.
     *
     * @param image The ImageProxy containing the captured image data to process.
     */
    fun processImage(image: ImageProxy) {
        try {
            val imageData = ImageData.fromImageProxy(image)
            val futures = mutableListOf<CompletableFuture<*>>()

            var barcodeResult: List<out Entity>? = null
            var ocrResult: List<out Entity>? = null
            var moduleResult: List<out Entity>? = null

            for (item in selectedFilterItems) {
                if (item.equals(FilterDialog.BARCODE_TRACKER, ignoreCase = true) && captureBarcodeDecoder != null) {
                    futures.add(
                        captureBarcodeDecoder!!.process(imageData)
                            .thenAccept { result -> barcodeResult = result }
                    )
                } else if (item.equals(FilterDialog.OCR_TRACKER, ignoreCase = true) && captureOcr != null) {
                    futures.add(
                        captureOcr!!.process(imageData)
                            .thenAccept { result -> ocrResult = result }
                    )
                } else if (item.equals(FilterDialog.PRODUCT_AND_SHELF, ignoreCase = true) && captureModuleRecognizer != null) {
                    futures.add(
                        captureModuleRecognizer!!.process(imageData)
                            .thenAccept { result -> moduleResult = result }
                    )
                }
            }

            CompletableFuture.allOf(*futures.toTypedArray())
                .thenRun {
                    callback.handleCaptureFrameEntities(barcodeResult, ocrResult, moduleResult)
                }
                .exceptionally { e ->
                    Log.e(TAG, "Error processing capture image: ${e.message}")
                    null
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in processImage: ${e.message}")
        } finally {
            image.close()
        }
    }


    private fun handleEntities(result: EntityTrackerAnalyzer.Result) {
        callback.handleEntities(result)
    }

    /**
     * Starts or restarts the analysis process. Recreates the executor and entity tracker analyzer.
     */
    fun startAnalyzing() {
        Log.d(TAG, "startAnalyzing() called.")
        executor = Executors.newFixedThreadPool(3)
        entityTrackerAnalyzer = EntityTrackerAnalyzer(
            analyzerList,
            ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
            executor,
            ::handleEntities
        )
    }

    /**
     * Stops and disposes of all live preview and capture instances, releasing resources.
     */
    fun stop() {
        captureExecutor.shutdownNow()

        // Dispose live preview instances
        barcodeDecoder?.let {
            it.dispose()
            Log.d(TAG, "Barcode decoder is disposed")
            barcodeDecoder = null
        }
        textOCR?.let {
            it.dispose()
            Log.d(TAG, "TextOCR is disposed")
            textOCR = null
        }
        moduleRecognizer?.let {
            it.dispose()
            Log.d(TAG, "Module Recognizer is disposed")
            moduleRecognizer = null
        }

        // Dispose capture instances
        captureBarcodeDecoder?.let {
            it.dispose()
            Log.d(TAG, "Capture barcode decoder disposed")
            captureBarcodeDecoder = null
        }
        captureOcr?.let {
            it.dispose()
            Log.d(TAG, "Capture OCR scanner disposed")
            captureOcr = null
        }
        captureModuleRecognizer?.let {
            it.dispose()
            Log.d(TAG, "Capture module recognizer disposed")
            captureModuleRecognizer = null
        }
    }

    /**
     * Stops the ExecutorService, terminating any ongoing analysis tasks.
     */
    fun stopAnalyzing() {
        executor.shutdownNow()
    }

    fun getModuleRecognizer(): ModuleRecognizer? = moduleRecognizer
    fun getBarcodeDecoder(): BarcodeDecoder? = barcodeDecoder
    fun getTextOCR(): TextOCR? = textOCR

    private fun copyFromAssets(filename: String, toPath: String) {
        val bufferSize = 8192
        try {
            context.assets.open(filename).use { stream ->
                Files.newOutputStream(Paths.get("$toPath$filename")).use { fos ->
                    BufferedOutputStream(fos).use { output ->
                        val data = ByteArray(bufferSize)
                        var count: Int
                        while (stream.read(data).also { count = it } != -1) {
                            output.write(data, 0, count)
                        }
                        output.flush()
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error in copy from assets: ${e.message}")
        }
    }

    private fun handleException(e: Throwable): Void? {
        val message = if (e is AIVisionSDKLicenseException)
            "License error: ${e.message}"
        else
            "Fatal error: ${e.message}"
        Log.e(TAG, message)
        return null
    }
}