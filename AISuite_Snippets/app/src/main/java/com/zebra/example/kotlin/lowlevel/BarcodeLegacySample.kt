// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.example.kotlin.lowlevel

import android.util.Log
import androidx.camera.core.ImageProxy
import com.zebra.ai.vision.detector.AIVisionSDKException
import com.zebra.ai.vision.detector.AIVisionSDKLicenseException
import com.zebra.ai.vision.detector.AIVisionSDKSNPEException
import com.zebra.ai.vision.detector.BarcodeDecoder
import com.zebra.ai.vision.detector.InferencerOptions
import com.zebra.ai.vision.detector.InvalidInputException
import com.zebra.ai.vision.detector.Localizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * BarcodeLegacySample is a Kotlin class that demonstrates how to use a barcode
 * localizer and decoder to detect and decode barcodes in images. The class uses
 * asynchronous operations to initialize models and process images.
 */
class BarcodeLegacySample {

    // Tag used for logging purposes
    private val TAG = "BarcodeLegacySample"

    // Instances of Localizer and BarcodeDecoder used for processing images
    private var barcodeDecoder: BarcodeDecoder? = null
    private var localizer: Localizer? = null

    // Executor service for handling asynchronous operations
    private val executor = Executors.newSingleThreadExecutor()
    private val mavenModelName = "barcode-localizer"

    /**
     * Initializes the BarcodeLegacySample by setting up the localizer and barcode decoder.
     */
    init {
        initialiseModels()
    }

    /**
     * Configures and initializes the Localizer and BarcodeDecoder with specific settings.
     * Handles exceptions related to model loading and initialization.
     */
    private fun initialiseModels() {
        try {
            // Measure the time taken to create Localizer settings
            val mStart = System.currentTimeMillis()
            val locSettings = Localizer.Settings(mavenModelName)
            val diff = System.currentTimeMillis() - mStart
            Log.d(TAG, "Barcode Localizer.settings() obj creation time = $diff milli sec")

            // Define runtime processor order
            val rpo = arrayOf(
                InferencerOptions.DSP,
                InferencerOptions.CPU,
                InferencerOptions.GPU
            )

            locSettings.inferencerOptions.runtimeProcessorOrder = rpo
            locSettings.inferencerOptions.defaultDims.height = 640
            locSettings.inferencerOptions.defaultDims.width = 640

            // Initialize the Localizer asynchronously
            val start = System.currentTimeMillis()
            Localizer.getLocalizer(locSettings, executor)
                .thenAccept { localizerInstance: Localizer? ->
                    localizer = localizerInstance
                    Log.d(
                        TAG,
                        "Barcode Localizer(locSettings) obj creation / model loading time = " + (System.currentTimeMillis() - start) + " milli sec"
                    )
                }.exceptionally { e: Throwable? ->
                    handleException(e, "Barcode Localizer")
                    null
                }

            // Create settings for the barcode decoder
            val decoderSettings = BarcodeDecoder.Settings(mavenModelName)

            decoderSettings.Symbology.CODE39.enable(true)
            decoderSettings.Symbology.CODE128.enable(true)

            decoderSettings.detectorSetting.inferencerOptions.runtimeProcessorOrder = rpo
            decoderSettings.detectorSetting.inferencerOptions.defaultDims.height = 640
            decoderSettings.detectorSetting.inferencerOptions.defaultDims.width = 640
            decoderSettings.enableLocalization = false

            // Initialize the BarcodeDecoder asynchronously
            val m_Start = System.currentTimeMillis()
            BarcodeDecoder.getBarcodeDecoder(decoderSettings, executor)
                .thenAccept { decoderInstance: BarcodeDecoder? ->
                    barcodeDecoder = decoderInstance
                    Log.d(
                        TAG,
                        "BarcodeDecoder() obj creation time = " + (System.currentTimeMillis() - m_Start) + " milli sec"
                    )
                }.exceptionally { e: Throwable? ->
                    handleException(e, "Barcode Decoder")
                    null
                }
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error: load failed - ${e.message}")
        }
    }

    /**
     * Processes an image to detect and decode barcodes using the Localizer and BarcodeDecoder.
     * This function operates asynchronously and logs the detected barcodes and their symbologies.
     *
     * @param imageProxy The image to be processed, provided as an ImageProxy
     */
    private fun processImage(imageProxy: ImageProxy) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bitmap = imageProxy.toBitmap()

                try {
                    val bBoxes = localizer?.detect(bitmap, executor)?.get()
                    Log.d(TAG, "Detections: ${bBoxes?.size}")
                    bBoxes?.size?.let {
                        if (it > 0) {
                            val barcodes = barcodeDecoder?.decode(bitmap, bBoxes, executor)?.get()
                            barcodes?.forEach { barcode ->
                                val decodedString = barcode.value
                                val decodedBbox = barcode.bboxData
                                Log.d(TAG, "Decoded barcode: $decodedString")
                            }
                        }
                    }
                } catch (e: InvalidInputException) {
                    Log.e(TAG, "Error during barcode detection/decoding: ${e.message}")
                } finally {
                    imageProxy.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in decoding or on drawing canvas: ${e.message}")
                imageProxy.close()
            }
        }
    }

    /**
     * Handles exceptions during model creation and processing, logging the appropriate error messages.
     *
     * @param e The exception that occurred
     * @param context A string describing the context of the error, such as "Barcode Localizer" or "Barcode Decoder"
     */
    private fun handleException(e: Throwable?, context: String) {
        when (e) {
            is AIVisionSDKLicenseException -> Log.e(TAG, "AIVisionSDKLicenseException: $context object creation failed, ${e.message}")
            is AIVisionSDKSNPEException -> Log.e(TAG, "AIVisionSDKSNPEException: $context object creation failed, ${e.message}")
            is InvalidInputException -> Log.e(TAG, "InvalidInputException: $context object creation failed, ${e.message}")
            is AIVisionSDKException -> Log.e(TAG, "AIVisionSDKException: $context object creation failed, ${e.message}")
            else -> Log.e(TAG, "Unhandled exception in $context: ${e?.message}")
        }
    }
}
