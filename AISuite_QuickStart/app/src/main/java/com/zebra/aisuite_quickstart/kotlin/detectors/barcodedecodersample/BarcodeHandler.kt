// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.detectors.barcodedecodersample

import android.content.Context
import com.zebra.aisuite_quickstart.utils.AppLog
import androidx.camera.core.ImageAnalysis
import androidx.core.content.ContextCompat
import com.zebra.ai.vision.detector.BarcodeDecoder
import com.zebra.ai.vision.detector.InferencerOptions
import com.zebra.aisuite_quickstart.kotlin.CameraXLivePreviewActivity
import com.zebra.aisuite_quickstart.utils.CommonUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * The BarcodeHandler class is responsible for setting up and managing the barcode detection
 * process using a BarcodeDecoder and BarcodeAnalyzer. This class is designed to work within
 * an Android application context and facilitates asynchronous operations for barcode detection.

 * The BarcodeHandler configures the decoder, assigns an analyzer for image analysis, and provides
 * methods to stop and dispose of resources when they are no longer needed.

 * Usage:
 * - Instantiate the BarcodeHandler with the necessary context, callback, and image analysis configuration.
 * - The class automatically initializes the BarcodeDecoder and BarcodeAnalyzer.
 * - Use getBarcodeAnalyzer() to retrieve the current instance of the BarcodeAnalyzer.
 * - Call stop() to dispose of the BarcodeDecoder and release resources.

 * Dependencies:
 * - Android Context: Required for managing resources and executing tasks on the main thread.
 * - ExecutorService: Used for asynchronous task execution.
 * - ImageAnalysis: Provides the framework for analyzing image data.
 * - BarcodeDecoder: Handles the decoding of barcode symbologies.
 * - BarcodeAnalyzer: Analyzes images to detect barcodes and report results via a callback.

 * Exception Handling:
 * - Handles AIVisionSDKLicenseException during decoder initialization.
 * - Logs any other exceptions encountered during the setup process.

 * Note: Ensure that the appropriate permissions and dependencies are configured
 * in the AndroidManifest and build files to utilize camera and image processing capabilities.
 */
class BarcodeHandler(
    private val context: Context,
    private val callback: CameraXLivePreviewActivity,
    private val imageAnalysis: ImageAnalysis,
    private val loadingCallback: ((Boolean) -> Unit)? = null
) {
    private val tag = "BarcodeHandler"
    private var barcodeDecoder: BarcodeDecoder? = null   // For live preview
    var captureDecoder: BarcodeDecoder? = null   // For capture mode
    private val executor = Executors.newSingleThreadExecutor()
    private val captureExecutor = Executors.newSingleThreadExecutor()
    var barcodeAnalyzer: BarcodeAnalyzer? = null
    private val mavenModelName = "barcode-decoder"
    private val sharedPreferences = context.getSharedPreferences(CommonUtils.SETTINGS_PREFS, Context.MODE_PRIVATE)

    // Model input sizes
    companion object {
        private const val CAPTURE_SIZE = 1280  // Higher resolution for capture
    }

    init {
        initializeBarcodeDecoder()
        initializeCaptureDecoder()
    }


    /**
     * Initializes the live preview BarcodeDecoder with 640px input size.
     */
    fun initializeBarcodeDecoder() {
        val liveDecoderSettings = createLiveDecoderSettings()
        CoroutineScope(executor.asCoroutineDispatcher()).launch {
            createBarcodeDecoder(liveDecoderSettings, System.currentTimeMillis())
        }
    }

    /**
     * Initializes the capture BarcodeDecoder with higher resolution (1280px) settings.
     */
    fun initializeCaptureDecoder() {
        val captureDecoderSettings = createCaptureDecoderSettings()
        CoroutineScope(captureExecutor.asCoroutineDispatcher()).launch {
            createCaptureDecoder(captureDecoderSettings, System.currentTimeMillis())
        }
    }

    /**
     * Creates live preview BarcodeDecoder.Settings using the model resolution selected in Settings.
     */
    private fun createLiveDecoderSettings(): BarcodeDecoder.Settings {
        return createBaseDecoderSettings().apply {
            detectorSetting.inferencerOptions.setModelResolution(CommonUtils.getModelResolution(sharedPreferences))
        }
    }

    /**
     * Creates capture BarcodeDecoder.Settings with the specified input size.
     */
    private fun createCaptureDecoderSettings(): BarcodeDecoder.Settings {
        return createBaseDecoderSettings().apply {
            detectorSetting.inferencerOptions.setModelResolution(InferencerOptions.Resolution(1280, 1280))
        }
    }

    private fun createBaseDecoderSettings(): BarcodeDecoder.Settings {
        return BarcodeDecoder.Settings(mavenModelName).apply {
            Symbology.CODE39.enable(true)
            Symbology.CODE93.enable(true)
            Symbology.CODE128.enable(true)
            Symbology.CODABAR.enable(true)
            Symbology.EAN8.enable(true)
            Symbology.EAN13.enable(true)
            Symbology.UPCE0.enable(true)
            Symbology.I2OF5.enable(true)
            val rpo = arrayOf(
                InferencerOptions.DSP,
                InferencerOptions.CPU,
                InferencerOptions.GPU
            )
            detectorSetting.inferencerOptions.apply {
                runtimeProcessorOrder = rpo
            }
            enableAIBarcodeDecode = true
        }
    }

    /**
     * Creates the live preview decoder. Fires loadingCallback only when both decoders are ready.
     */
    private suspend fun createBarcodeDecoder(
        decoderSettings: BarcodeDecoder.Settings,
        startTime: Long
    ) {
        try {
            val decoderInstance =
                BarcodeDecoder.getBarcodeDecoder(decoderSettings, executor).await()
            barcodeDecoder = decoderInstance
            // Only notify and attach analyzer when both decoders are ready
            if (captureDecoder != null) {
                loadingCallback?.invoke(true)
                attachAnalysisAfterModelLoading()
            }
            AppLog.i(tag, "BarcodeDecoder model loading time: ${System.currentTimeMillis() - startTime} milli sec and model resolution: ${CommonUtils.getModelResolution(sharedPreferences)}")
        } catch (e: Exception) {
            loadingCallback?.invoke(false)
            AppLog.e(tag, "BarcodeDecoder model loading failed - ${e.message}")
        }
    }

    /**
     * Creates the capture decoder. Fires loadingCallback only when both decoders are ready.
     */
    private suspend fun createCaptureDecoder(
        decoderSettings: BarcodeDecoder.Settings,
        startTime: Long
    ) {
        try {
            val decoderInstance =
                BarcodeDecoder.getBarcodeDecoder(decoderSettings, captureExecutor).await()
            captureDecoder = decoderInstance
            // Only notify and attach analyzer when both decoders are ready
            if (barcodeDecoder != null) {
                loadingCallback?.invoke(true)
                attachAnalysisAfterModelLoading()
            }
            AppLog.i(tag, "Capture BarcodeDecoder model loading time: ${System.currentTimeMillis() - startTime} milli sec and input size: ${decoderSettings.detectorSetting.inferencerOptions.defaultDims.width}")
        } catch (e: Exception) {
            loadingCallback?.invoke(false)
            AppLog.e(tag, "Capture decoder model loading failed: ${e.message}")
        }
    }

    /**
     * Attaches the BarcodeAnalyzer to the ImageAnalysis use case after both decoders are loaded.
     */
    fun attachAnalysisAfterModelLoading() {
        barcodeAnalyzer = BarcodeAnalyzer(callback, barcodeDecoder!!)
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), barcodeAnalyzer!!)
    }

    /**
     * Stops both executor services and disposes of both BarcodeDecoders.
     */
    fun stop() {
        executor.shutdownNow()
        captureExecutor.shutdownNow()
        barcodeDecoder?.let {
            it.dispose()
            AppLog.i(tag, "Live preview barcode decoder disposed")
            barcodeDecoder = null
        }
        captureDecoder?.let {
            it.dispose()
            AppLog.i(tag, "Capture barcode decoder disposed")
            captureDecoder = null
        }
    }

}
