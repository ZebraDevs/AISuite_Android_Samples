// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.detectors.productrecognition

import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.core.content.ContextCompat
import com.zebra.ai.vision.detector.BarcodeDecoder
import com.zebra.ai.vision.detector.EntityType
import com.zebra.ai.vision.detector.InferencerOptions
import com.zebra.ai.vision.detector.ModuleRecognizer
import com.zebra.aisuite_quickstart.utils.AppLog
import com.zebra.aisuite_quickstart.utils.CommonUtils
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
        private const val CAPTURE_SIZE = 1280 // Higher resolution for capture
    }

    private val tag = "ProductRecognitionHandler"
    private val executor = Executors.newFixedThreadPool(4)
    private val captureExecutor = Executors.newFixedThreadPool(4)
    var productRecognitionAnalyzer: ProductRecognitionAnalyzer? = null
    private var moduleRecognizer: ModuleRecognizer? = null // For live preview
    var captureRecognizer: ModuleRecognizer? = null // For capture mode
    private val mavenModelName = "product-and-shelf-recognizer"
    private val barcodeMavenModelName = "barcode-decoder"

    // --- Asset Setup ---
    val productIndexFilename = "product_index.zip"
    val toPath = "${context.filesDir}/"
    private val sharedPreferences = context.getSharedPreferences(CommonUtils.SETTINGS_PREFS, Context.MODE_PRIVATE)

    init {
        copyFromAssets(productIndexFilename, toPath)
        initializeModuleRecognizer()
        initializeCaptureRecognizer()
    }


    /**
     * Creates recognizer settings with specified input size.
     *
     * @param inputSize The input dimension size for the recognizer model.
     * @return Configured ModuleRecognizer.Settings instance.
     */
    private fun createRecognizerSettings(
        inputSize: Int
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

            enableProductRecognition(
                mavenModelName,
                "$toPath$productIndexFilename"
            )
        }
    }

    private fun createCaptureRecognizerSettings(
    ): ModuleRecognizer.Settings {
        return ModuleRecognizer.Settings(mavenModelName).apply {
            inferencerOptions.apply {
                runtimeProcessorOrder = arrayOf(
                    InferencerOptions.DSP,
                    InferencerOptions.CPU,
                    InferencerOptions.GPU
                )
                defaultDims.height = CAPTURE_SIZE
                defaultDims.width = CAPTURE_SIZE
                val labelBarcodeSettings: BarcodeDecoder.Settings =
                    BarcodeDecoder.Settings(barcodeMavenModelName)
                labelBarcodeSettings.enableAIBarcodeDecode = true
                labelBarcodeSettings.detectorSetting.inferencerOptions.defaultDims.height = CAPTURE_SIZE
                labelBarcodeSettings.detectorSetting.inferencerOptions.defaultDims.width = CAPTURE_SIZE
                val barcodeSettingsMap: MutableMap<EntityType?, BarcodeDecoder.Settings?> =
                    HashMap()
                barcodeSettingsMap[EntityType.LABEL] = labelBarcodeSettings
                enableBarcodeRecognition(barcodeSettingsMap)
            }

            enableProductRecognition(
                mavenModelName,
                "$toPath$productIndexFilename"
            )
        }
    }

    /**
     * Initialize ModuleRecognizer with product recognition enabled for live preview.
     */
    private fun initializeModuleRecognizer() {
        CoroutineScope(executor.asCoroutineDispatcher()).launch {
            try {
                val modelInputSize = sharedPreferences.getInt(CommonUtils.PREF_MODEL_INPUT_SIZE, 640)
                // --- Settings Configuration ---
                val liveRecognizerSettings = createRecognizerSettings(
                    modelInputSize
                )

                // --- Launch Initializer ---
                createModuleRecognizer(
                    liveRecognizerSettings,
                    System.currentTimeMillis()
                )

            } catch (e: Exception) {
                loadingCallback?.invoke(false)
                AppLog.e(tag, "Fatal error during initialization setup: ${e.message}")
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
                val captureRecognizerSettings = createCaptureRecognizerSettings()

                createCaptureRecognizer(
                    captureRecognizerSettings,
                    System.currentTimeMillis()
                )
            } catch (ex: Exception) {
                loadingCallback?.invoke(false)
                AppLog.e(tag, "Capture recognizer initialization failed: ${ex.message}")
            }
        }
    }

    /**
     * Creates the live preview ModuleRecognizer instance.
     * Only notifies loading complete and attaches analyzer when both models are loaded.
     */
    private suspend fun createModuleRecognizer(
        settings: ModuleRecognizer.Settings,
        startTime: Long
    ) {
        try {
            val recognizerInstance =
                ModuleRecognizer.getModuleRecognizer(settings, executor).await()
            moduleRecognizer = recognizerInstance

            if (captureRecognizer != null) {
                loadingCallback?.invoke(true)
                attachAnalysisAfterModelLoading()
            }

            val creationTime = System.currentTimeMillis() - startTime
            AppLog.i(tag, "ModuleRecognizer model loading time: $creationTime milli sec and input size: ${settings.inferencerOptions.defaultDims.width}")
        } catch (e: Exception) {
            loadingCallback?.invoke(false)
            AppLog.e(tag, "ModuleRecognizer model loading failed: ${e.message}")
        }
    }

    /**
     * Creates the capture ModuleRecognizer instance.
     * Only notifies loading complete and attaches analyzer when both models are loaded.
     */
    private suspend fun createCaptureRecognizer(
        settings: ModuleRecognizer.Settings,
        startTime: Long
    ) {
        try {
            val recognizerInstance =
                ModuleRecognizer.getModuleRecognizer(settings, captureExecutor).await()
            captureRecognizer = recognizerInstance

            if (moduleRecognizer != null) {
                loadingCallback?.invoke(true)
                attachAnalysisAfterModelLoading()
            }

            AppLog.i(tag, "Capture ModuleRecognizer model loading time: ${System.currentTimeMillis() - startTime} milli sec and input size: ${settings.inferencerOptions.defaultDims.width}")
        } catch (e: Exception) {
            loadingCallback?.invoke(false)
            AppLog.e(tag, "Capture ModuleRecognizer model loading failed: ${e.message}")
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
            AppLog.e(tag, "Error copying from assets: ${e.message}")
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
            AppLog.i(tag, "ModuleRecognizer disposed")
            moduleRecognizer = null
        }
        captureRecognizer?.let {
            it.dispose()
            AppLog.i(tag, "Capture module recognizer disposed")
            captureRecognizer = null
        }
    }
}