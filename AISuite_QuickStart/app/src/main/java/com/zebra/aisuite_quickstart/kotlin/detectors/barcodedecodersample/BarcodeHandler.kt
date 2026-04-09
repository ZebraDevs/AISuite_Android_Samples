// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.detectors.barcodedecodersample

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.core.content.ContextCompat
import com.zebra.ai.vision.detector.BarcodeDecoder
import com.zebra.ai.vision.detector.InferencerOptions
import com.zebra.aisuite_quickstart.kotlin.CameraXLivePreviewActivity
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
    private val mavenModelName = "barcode-localizer"

    // Model input sizes
    companion object {
        private const val LIVE_PREVIEW_SIZE = 640
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
        val liveDecoderSettings = createDecoderSettings(LIVE_PREVIEW_SIZE)
        CoroutineScope(executor.asCoroutineDispatcher()).launch {
            createBarcodeDecoderWithFallback(liveDecoderSettings, System.currentTimeMillis())
        }
    }

    /**
     * Initializes the capture BarcodeDecoder with higher resolution (1280px) settings.
     */
    fun initializeCaptureDecoder() {
        val captureDecoderSettings = createDecoderSettings(CAPTURE_SIZE)
        CoroutineScope(captureExecutor.asCoroutineDispatcher()).launch {
            createCaptureDecoderWithFallback(captureDecoderSettings, System.currentTimeMillis())
        }
    }

    /**
     * Creates BarcodeDecoder.Settings with the specified input size.
     */
    private fun createDecoderSettings(inputSize: Int): BarcodeDecoder.Settings {
        return BarcodeDecoder.Settings(mavenModelName).apply {
            Symbology.CODE39.enable(true)
            Symbology.CODE128.enable(true)
            val rpo = arrayOf(
                InferencerOptions.DSP,
                InferencerOptions.CPU,
                InferencerOptions.GPU
            )
            detectorSetting.inferencerOptions.apply {
                runtimeProcessorOrder = rpo
                defaultDims.height = inputSize
                defaultDims.width = inputSize
            }
        }
    }

    /**
     * Creates the live preview decoder. Fires loadingCallback only when both decoders are ready.
     */
    private suspend fun createBarcodeDecoderWithFallback(decoderSettings: BarcodeDecoder.Settings, startTime: Long) {
        try {
            val decoderInstance = BarcodeDecoder.getBarcodeDecoder(decoderSettings, executor).await()
            barcodeDecoder = decoderInstance
            // Only notify and attach analyzer when both decoders are ready
            if (captureDecoder != null) {
                loadingCallback?.invoke(true)
                attachAnalysisAfterModelLoading()
            }
            Log.d(tag, "BarcodeDecoder() obj creation time = ${System.currentTimeMillis() - startTime} ms, input size: ${decoderSettings.detectorSetting.inferencerOptions.defaultDims.width}")
        } catch (e: Exception) {
            loadingCallback?.invoke(false)
            Log.e(tag, "Fatal error: decoder creation failed - ${e.message}")
        }
    }

    /**
     * Creates the capture decoder. Fires loadingCallback only when both decoders are ready.
     */
    private suspend fun createCaptureDecoderWithFallback(decoderSettings: BarcodeDecoder.Settings, startTime: Long) {
        try {
            val decoderInstance = BarcodeDecoder.getBarcodeDecoder(decoderSettings, captureExecutor).await()
            captureDecoder = decoderInstance
            // Only notify and attach analyzer when both decoders are ready
            if (barcodeDecoder != null) {
                loadingCallback?.invoke(true)
                attachAnalysisAfterModelLoading()
            }
            Log.d(tag, "Capture BarcodeDecoder created in ${System.currentTimeMillis() - startTime} ms")
        } catch (e: Exception) {
            loadingCallback?.invoke(false)
            Log.e(tag, "Capture decoder creation failed: ${e.message}")
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
            Log.d(tag, "Live preview barcode decoder disposed")
            barcodeDecoder = null
        }
        captureDecoder?.let {
            it.dispose()
            Log.d(tag, "Capture barcode decoder disposed")
            captureDecoder = null
        }
    }

}
