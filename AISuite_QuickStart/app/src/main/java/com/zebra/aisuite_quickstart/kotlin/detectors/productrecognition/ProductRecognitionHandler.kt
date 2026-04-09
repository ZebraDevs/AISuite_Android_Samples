// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.detectors.productrecognition

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.core.content.ContextCompat
import com.zebra.ai.vision.detector.BarcodeDecoder
import com.zebra.ai.vision.detector.EntityType
import com.zebra.ai.vision.detector.InferencerOptions
import com.zebra.ai.vision.detector.ModuleRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Executors

class ProductRecognitionHandler(
    private val context: Context,
    private val callback: ProductRecognitionAnalyzer.DetectionCallback,
    private val imageAnalysis: ImageAnalysis,
    private val loadingCallback: ((Boolean) -> Unit)? = null
) {
    companion object {
        private const val LIVE_PREVIEW_SIZE = 640
        private const val CAPTURE_SIZE = 1280 // Higher resolution for capture
    }

    private val tag = "ProductRecognitionHandler"
    private val executor = Executors.newFixedThreadPool(4)
    private val captureExecutor = Executors.newFixedThreadPool(4)
    var productRecognitionAnalyzer: ProductRecognitionAnalyzer? = null
    private var moduleRecognizer: ModuleRecognizer? = null // For live preview
    var captureRecognizer: ModuleRecognizer? = null // For capture mode
    private val mavenModelName = "product-and-shelf-recognizer"
    private val barcodeMavenModelName = "barcode-localizer"
    // --- Asset Setup ---
    val indexFilename = "product.index"
    val labelsFilename = "product.txt"
    val toPath = "${context.filesDir}/"

    init {
        copyFromAssets(indexFilename, toPath)
        copyFromAssets(labelsFilename, toPath)
        initializeModuleRecognizer()
        initializeCaptureRecognizer()
    }


    /**
     * Creates recognizer settings with specified input size.
     *
     * @param inputSize The input dimension size for the recognizer model.
     * @param toPath The path where asset files are stored.
     * @param indexFilename The product index filename.
     * @param labelsFilename The product labels filename.
     * @return Configured ModuleRecognizer.Settings instance.
     */
    private fun createRecognizerSettings(
        inputSize: Int,
        toPath: String,
        indexFilename: String,
        labelsFilename: String
    ): ModuleRecognizer.Settings {
        return ModuleRecognizer.Settings(mavenModelName).apply {
            inferencerOptions.apply {
                runtimeProcessorOrder = arrayOf(
                    InferencerOptions.DSP,
                    InferencerOptions.CPU,
                    InferencerOptions.GPU
                )
                defaultDims.height = inputSize
                defaultDims.width = inputSize
                val labelBarcodeSettings: BarcodeDecoder.Settings =
                    BarcodeDecoder.Settings(barcodeMavenModelName)
                val barcodeSettingsMap: MutableMap<EntityType?, BarcodeDecoder.Settings?> =
                    HashMap()
                barcodeSettingsMap[EntityType.LABEL] = labelBarcodeSettings
                enableBarcodeRecognition(barcodeSettingsMap)
            }

            enableProductRecognitionWithIndex(
                mavenModelName,
                "$toPath$indexFilename",
                "$toPath$labelsFilename"
            )
        }
    }

    /**
     * Initialize ModuleRecognizer with product recognition enabled for live preview.
     */
    private fun initializeModuleRecognizer() {
        CoroutineScope(executor.asCoroutineDispatcher()).launch {
            try {
                Log.i(tag, "Initializing ModuleRecognizer")

                // --- Settings Configuration ---
                val liveRecognizerSettings = createRecognizerSettings(
                    LIVE_PREVIEW_SIZE, toPath, indexFilename, labelsFilename
                )

                // --- Launch Initializer with Fallback ---
                createModuleRecognizerWithFallback(liveRecognizerSettings, System.currentTimeMillis())

            } catch (e: Exception) {
                loadingCallback?.invoke(false)
                Log.e(tag, "Fatal error during initialization setup: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Initializes the capture recognizer with higher resolution settings.
     */
    fun initializeCaptureRecognizer() {
        CoroutineScope(captureExecutor.asCoroutineDispatcher()).launch {
            try {

                // Create settings for capture
                val captureRecognizerSettings = createRecognizerSettings(
                    CAPTURE_SIZE, toPath, indexFilename, labelsFilename
                )

                createCaptureRecognizerWithFallback(captureRecognizerSettings, System.currentTimeMillis())
            } catch (ex: Exception) {
                loadingCallback?.invoke(false)
                Log.e(tag, "Capture recognizer initialization failed: ${ex.message}")
            }
        }
    }

    /**
     * Creates the live preview ModuleRecognizer instance with fallback error handling.
     * Only notifies loading complete and attaches analyzer when both models are loaded.
     */
    private suspend fun createModuleRecognizerWithFallback(
        settings: ModuleRecognizer.Settings,
        startTime: Long
    ) {
        try {
            val recognizerInstance = ModuleRecognizer.getModuleRecognizer(settings, executor).await()
            moduleRecognizer = recognizerInstance

            if (captureRecognizer != null) {
                loadingCallback?.invoke(true)
                attachAnalysisAfterModelLoading()
            }

            val creationTime = System.currentTimeMillis() - startTime
            Log.d(
                tag,
                "ModuleRecognizer Creation Time: ${creationTime}ms and input size: ${settings.inferencerOptions.defaultDims.width}"
            )
        } catch (e: Exception) {
            loadingCallback?.invoke(false)
            Log.e(tag, "Failed to initialize ModuleRecognizer: ${e.message}")
        }
    }

    /**
     * Creates the capture ModuleRecognizer instance with fallback error handling.
     * Only notifies loading complete and attaches analyzer when both models are loaded.
     */
    private suspend fun createCaptureRecognizerWithFallback(
        settings: ModuleRecognizer.Settings,
        startTime: Long
    ) {
        try {
            val recognizerInstance = ModuleRecognizer.getModuleRecognizer(settings, captureExecutor).await()
            captureRecognizer = recognizerInstance

            if (moduleRecognizer != null) {
                loadingCallback?.invoke(true)
                attachAnalysisAfterModelLoading()
            }

            Log.d(tag, "Capture ModuleRecognizer created in ${System.currentTimeMillis() - startTime} ms")
        } catch (e: Exception) {
            loadingCallback?.invoke(false)
            Log.e(tag, "Capture recognizer creation failed: ${e.message}")
        }
    }

    /**
     * Attaches the ProductRecognitionAnalyzer to the ImageAnalysis once both models are loaded.
     */
    private fun attachAnalysisAfterModelLoading() {
        productRecognitionAnalyzer = ProductRecognitionAnalyzer(callback, moduleRecognizer)
        imageAnalysis.setAnalyzer(
            ContextCompat.getMainExecutor(context),
            productRecognitionAnalyzer!!
        )
    }

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
            Log.e(tag, "Error copying from assets: ${e.message}")
        }
    }


    /**
     * Stops the executor services and disposes of both ModuleRecognizer instances.
     */
    fun stop() {
        executor.shutdownNow()
        captureExecutor.shutdownNow()
        productRecognitionAnalyzer?.stopAnalyzing()
        moduleRecognizer?.let {
            it.dispose()
            Log.d(tag, "ModuleRecognizer disposed")
            moduleRecognizer = null
        }
        captureRecognizer?.let {
            it.dispose()
            Log.d(tag, "Capture module recognizer disposed")
            captureRecognizer = null
        }
    }
}