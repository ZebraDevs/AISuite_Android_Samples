// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.detectors.barcodedecodersample

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.core.content.ContextCompat
import com.zebra.ai.vision.detector.AIVisionSDKLicenseException
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
    private val imageAnalysis: ImageAnalysis
) {
    private val TAG = "BarcodeHandler"
    private var barcodeDecoder: BarcodeDecoder? = null
    private val executor = Executors.newSingleThreadExecutor()
    private var barcodeAnalyzer: BarcodeAnalyzer? = null
    private val mavenModelName = "barcode-localizer"

    init {
        initializeBarcodeDecoder()
    }

    /**
     * Initializes the BarcodeDecoder with predefined settings for barcode symbologies
     * and detection parameters. This method sets up the necessary components for analyzing
     * and decoding barcodes from image data asynchronously using coroutines.
     */
    private fun initializeBarcodeDecoder() {
        val decoderSettings = BarcodeDecoder.Settings(mavenModelName).apply {
            Symbology.CODE39.enable(true)
            Symbology.CODE128.enable(true)
            val rpo = arrayOf(
                InferencerOptions.DSP,
                InferencerOptions.CPU,
                InferencerOptions.GPU
            )

            detectorSetting.inferencerOptions.apply {
                runtimeProcessorOrder = rpo
                defaultDims.height = 640
                defaultDims.width = 640
            }
        }

        val startTime = System.currentTimeMillis()

        CoroutineScope(executor.asCoroutineDispatcher()).launch {
            try {
                val decoderInstance = BarcodeDecoder.getBarcodeDecoder(decoderSettings, executor).await()
                barcodeDecoder = decoderInstance
                barcodeAnalyzer = BarcodeAnalyzer(callback, decoderInstance)
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), barcodeAnalyzer!!)

                Log.d(TAG, "BarcodeDecoder() obj creation time = ${System.currentTimeMillis() - startTime} milli sec")
            }
            catch (e: AIVisionSDKLicenseException) {
                Log.e(TAG, "AIVisionSDKLicenseException: Barcode Decoder object creation failed, ${e.message}")
            }
            catch (e: Exception) {
                Log.e(TAG, "Fatal error: decoder creation failed - ${e.message}")
            }
        }
    }

    /**
     * Stops the executor service and disposes of the BarcodeDecoder, releasing any resources held.
     * This method should be called when barcode detection is no longer needed.
     */
    fun stop() {
        executor.shutdownNow()
        barcodeDecoder?.let {
            it.dispose()
            Log.d(TAG, "Barcode decoder is disposed")
            barcodeDecoder = null
        }
    }

    /**
     * Retrieves the current instance of the BarcodeAnalyzer.
     *
     * @return The BarcodeAnalyzer instance, or null if not yet initialized.
     */
    fun getBarcodeAnalyzer(): BarcodeAnalyzer? {
        return barcodeAnalyzer
    }
}
