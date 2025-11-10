// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.lowlevel.simplebarcodesample

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.core.content.ContextCompat
import com.zebra.ai.vision.detector.AIVisionSDKLicenseException
import com.zebra.ai.vision.detector.BarcodeDecoder
import com.zebra.ai.vision.detector.InferencerOptions
import com.zebra.ai.vision.detector.Localizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * The BarcodeSample class implements the BarcodeSampleAnalyzer.SampleBarcodeDetectionCallback
 * interface and is responsible for initializing and managing the barcode detection process
 * using a Localizer and BarcodeDecoder. This class is designed to work within an Android
 * application context and facilitates asynchronous operations for barcode detection.

 * The BarcodeSample class configures necessary components and manages their lifecycle,
 * providing methods to stop and dispose of resources when they are no longer needed.

 * Usage:
 * - Instantiate the BarcodeSample with the necessary context, callback, and image analysis configuration.
 * - The class automatically initializes the Localizer and BarcodeDecoder.
 * - Use getBarcodeDecoder() to retrieve the current instance of the BarcodeDecoder.
 * - Call stop() to dispose of the BarcodeDecoder and release resources.
 * - Implement the onDetectionResult() method to handle barcode detection results.

 * Dependencies:
 * - Android Context: Required for resource management and executing tasks on the main thread.
 * - ExecutorService: Used for asynchronous task execution.
 * - ImageAnalysis: Provides the framework for analyzing image data.
 * - Localizer: Detects potential regions of interest for barcode decoding.
 * - BarcodeDecoder: Handles the decoding of barcodes from images.

 * Exception Handling:
 * - Handles AIVisionSDKLicenseException and generic exceptions during initialization.
 * - Logs any other exceptions encountered during the setup process.

 * Note: Ensure that the appropriate permissions and dependencies are configured
 * in the AndroidManifest and build files to utilize camera and image processing capabilities.
 */
class BarcodeSample(
    private val context: Context,
    private val callback: BarcodeSampleAnalyzer.SampleBarcodeDetectionCallback,
    private val imageAnalysis: ImageAnalysis
) {

    private val TAG = "BarcodeSample"
    private var localizer: Localizer? = null
    private var barcodeDecoder: BarcodeDecoder? = null

    private var barcodeAnalyzer: BarcodeSampleAnalyzer? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val mavenModelName = "barcode-localizer"

    init {
        initializeBarcodeDecoder()
    }

    /**
     * Initializes the Localizer and BarcodeDecoder with predefined settings.
     * This method sets up the necessary components for analyzing and decoding
     * barcodes from image data asynchronously using coroutines.
     */
    private fun initializeBarcodeDecoder() {
        CoroutineScope(executor.asCoroutineDispatcher()).launch {
            try {
                val mStart = System.currentTimeMillis()
                val locSettings = Localizer.Settings(mavenModelName)
                val diff = System.currentTimeMillis() - mStart
                Log.d(TAG, "Barcode Localizer.settings() obj creation time = $diff milli sec")

                val rpo = arrayOf(
                    InferencerOptions.DSP,
                    InferencerOptions.CPU,
                    InferencerOptions.GPU
                )

                locSettings.inferencerOptions.runtimeProcessorOrder = rpo
                locSettings.inferencerOptions.defaultDims.height = 640
                locSettings.inferencerOptions.defaultDims.width = 640

                val start = System.currentTimeMillis()
                try {
                    localizer = Localizer.getLocalizer(locSettings, executor).await()
                    Log.d(TAG, "Barcode Localizer(locSettings) obj creation / model loading time = ${System.currentTimeMillis() - start} milli sec")
                } catch (e: AIVisionSDKLicenseException) {
                    Log.e(TAG, "AIVisionSDKLicenseException: Barcode Localizer object creation failed, ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Fatal error: load failed - ${e.message}")
                }

                val decoderSettings = BarcodeDecoder.Settings(mavenModelName)

                val mStartDecoder = System.currentTimeMillis()
                try {
                    barcodeDecoder = BarcodeDecoder.getBarcodeDecoder(decoderSettings, executor).await()
                    barcodeAnalyzer = BarcodeSampleAnalyzer(callback, localizer!!, barcodeDecoder!!)
                    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), barcodeAnalyzer!!)
                    Log.d(TAG, "BarcodeDecoder() obj creation time = ${System.currentTimeMillis() - mStartDecoder} milli sec")
                } catch (e: AIVisionSDKLicenseException) {
                    Log.e(TAG, "AIVisionSDKLicenseException: Barcode Decoder object creation failed, ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Fatal error: decoder creation failed - ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fatal error: load failed - ${e.message}")
            }
        }
    }

    /**
     * Disposes of the BarcodeDecoder, releasing any resources held.
     * This method should be called when barcode detection is no longer needed.
     */
    fun stop() {
        barcodeDecoder?.let {
            it.dispose()
            Log.v(TAG, "Barcode decoder is disposed")
            barcodeDecoder = null
        }
    }

    /**
     * Retrieves the current instance of the BarcodeDecoder.
     *
     * @return The BarcodeDecoder instance, or null if not yet initialized.
     */
    fun getBarcodeDecoder(): BarcodeDecoder? {
        return barcodeDecoder
    }

    fun getBarcodeAnalyzer(): BarcodeSampleAnalyzer? {
        return barcodeAnalyzer
    }

}
