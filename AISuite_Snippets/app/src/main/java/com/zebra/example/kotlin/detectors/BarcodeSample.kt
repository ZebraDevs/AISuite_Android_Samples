// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.example.kotlin.detectors

import android.util.Log
import androidx.camera.core.ImageProxy
import com.zebra.ai.vision.detector.AIVisionSDKException
import com.zebra.ai.vision.detector.AIVisionSDKLicenseException
import com.zebra.ai.vision.detector.BarcodeDecoder
import com.zebra.ai.vision.detector.ImageData
import com.zebra.ai.vision.detector.InferencerOptions
import com.zebra.ai.vision.entity.BarcodeEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * BarcodeSample is a Kotlin class that demonstrates how to initialize and use a
 * barcode decoder to process images and detect barcodes asynchronously using Kotlin coroutines.
 */
class BarcodeSample {

    // Tag used for logging purposes
    private val TAG = "BarcodeSample"

    // Instance of BarcodeDecoder used for decoding barcodes
    private var barcodeDecoder: BarcodeDecoder? = null

    // Executor service for handling asynchronous operations
    private val executor = Executors.newSingleThreadExecutor()
    private val mavenModelName = "barcode-localizer"

    /**
     * Initializes the BarcodeSample by setting up the barcode decoder.
     */
    init {
        initializeBarcodeDecoder()
    }

    /**
     * Configures and initializes the barcode decoder with specific settings.
     * The decoder is set up to detect specific symbologies and dimensions.
     */
    private fun initializeBarcodeDecoder() {
        // Create settings for the barcode decoder using the apply scope function
        val decoderSettings = BarcodeDecoder.Settings("barcode-localizer").apply {
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

        // Record the start time for profiling
        val startTime = System.currentTimeMillis()

        // Use a coroutine scope with the executor's coroutine dispatcher to initialize the decoder
        CoroutineScope(executor.asCoroutineDispatcher()).launch {
            try {
                // Await the asynchronous creation of the barcode decoder
                val decoderInstance = BarcodeDecoder.getBarcodeDecoder(decoderSettings, executor).await()
                barcodeDecoder = decoderInstance

                Log.d(TAG, "BarcodeDecoder() obj creation time = ${System.currentTimeMillis() - startTime} milli sec")
            } catch (e: AIVisionSDKLicenseException) {
                Log.e(TAG, "AIVisionSDKLicenseException: Barcode Decoder object creation failed, ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Fatal error: decoder creation failed - ${e.message}")
            }
        }
    }

    /**
     * Processes an image using the barcode decoder to detect barcodes.
     * This function operates asynchronously and updates results on the UI thread.
     *
     * @param image The image to be processed, provided as an ImageProxy
     */
    private suspend fun processImage(image: ImageProxy) {
        try {
            Log.d(TAG, "Starting image analysis")

            // Process the image asynchronously to detect barcodes
            val result = processImageAsync(image)

            // Switch to the main dispatcher to update UI with results
            withContext(Dispatchers.Main) {
                // Update results to the UI thread
                result.forEach { bb ->
                    val rect = bb.boundingBox
                    Log.d(TAG, "Detected entity - Value: ${bb.value}")
                }
                image.close()
            }
        } catch (ex: Exception) {
            image.close()
            Log.e(TAG, "Error during image processing: ${ex.message}")
        }
    }

    /**
     * Asynchronously processes an image to detect barcodes using the barcode decoder.
     * This method suspends the coroutine until the barcode processing is complete.
     *
     * @param image The image to be processed, provided as an ImageProxy
     * @return A list of detected BarcodeEntity objects
     */
    private suspend fun processImageAsync(image: ImageProxy): List<BarcodeEntity> {
        return suspendCancellableCoroutine { cont ->
            try {
                barcodeDecoder?.process(ImageData.fromImageProxy(image))
                    ?.thenAccept { result ->
                        cont.resume(result) // Resume the coroutine with the result
                    }
                    ?.exceptionally { ex ->
                        cont.resumeWithException(ex) // Resume with exception
                        null
                    }
            } catch (e: AIVisionSDKException) {
                cont.resumeWithException(e)
            }
        }
    }
}
