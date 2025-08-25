// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.lowlevel.simplebarcodesample

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.zebra.ai.vision.detector.AIVisionSDKException
import com.zebra.ai.vision.detector.BarcodeDecoder
import com.zebra.ai.vision.detector.InvalidInputException
import com.zebra.ai.vision.detector.Localizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * The BarcodeSampleAnalyzer class implements the ImageAnalysis.Analyzer interface and is
 * responsible for asynchronously analyzing image frames to detect barcodes using a Localizer
 * and BarcodeDecoder. This class utilizes Kotlin coroutines to handle image processing tasks
 * and callbacks to deliver detection results.

 * This class is designed to be used within an Android application as part of a camera-based
 * barcode scanning solution. It processes image data and returns barcode detection results
 * through a callback interface.

 * Usage:
 * - Instantiate the BarcodeSampleAnalyzer with the appropriate SampleBarcodeDetectionCallback,
 *   Localizer, and BarcodeDecoder.
 * - Implement the SampleBarcodeDetectionCallback interface to handle barcode detection results.
 * - The analyze(ImageProxy) method is called by the camera framework to process image frames.
 * - Call stop() to terminate ongoing analysis and release resources.

 * Dependencies:
 * - Android ImageProxy: Provides access to image data from the camera.
 * - CoroutineScope: Used for asynchronous task execution, leveraging Kotlin coroutines.
 * - Localizer: Detects potential regions of interest for barcode decoding.
 * - BarcodeDecoder: Decodes barcodes from images.

 * Concurrency:
 * - Uses a CoroutineScope to ensure that image analysis tasks are processed asynchronously.
 * - Manages concurrency with flags to control analysis state and prevent re-entry.

 * Note: Ensure that the appropriate permissions and dependencies are configured
 * in the AndroidManifest and build files to utilize camera and image processing capabilities.
 */
class BarcodeSampleAnalyzer(
    private val callback: SampleBarcodeDetectionCallback,
    private val localizer: Localizer,
    private val barcodeDecoder: BarcodeDecoder
) : ImageAnalysis.Analyzer {

    /**
     * Interface for handling the results of the barcode detection process.
     * Implement this interface to define how detection results are processed.
     */
    interface SampleBarcodeDetectionCallback {
        fun onDetectionResult(list: Array<BarcodeDecoder.Result>)
    }

    private val TAG = "BarcodeAnalyzer"
    private var isAnalyzing = true
    private var isStopped = false

    private val executor = Executors.newSingleThreadExecutor()

    // Create a CoroutineScope with the IO dispatcher and a Job for lifecycle management
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    /**
     * Analyzes the given image to detect barcodes. This method is called by the camera
     * framework to process image frames asynchronously.
     *
     * @param image The image frame to analyze.
     */
    override fun analyze(image: ImageProxy) {
        if (!isAnalyzing || isStopped) {
            image.close()
            return
        }

        isAnalyzing = false // Prevent re-entry

        scope.launch {
            try {
                Log.d(TAG, "Starting image analysis")
                val barcodes = processImageAsync(image)
                withContext(Dispatchers.Main) {
                    if (!isStopped) {
                        callback.onDetectionResult(barcodes)
                    }
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Error during image processing: ${ex.message}")
            } finally {
                isAnalyzing = true
                image.close() // Ensure image is closed
            }
        }
    }

    /**
     * Processes the image asynchronously using the Localizer and BarcodeDecoder.
     *
     * @param image The ImageProxy containing the image data to process.
     * @return An array of BarcodeDecoder.Result representing the detected barcodes.
     */
    private suspend fun processImageAsync(image: ImageProxy): Array<BarcodeDecoder.Result> {
        return suspendCancellableCoroutine { cont ->
            try {
                val bitmap = image.toBitmap()
                localizer.detect(bitmap, executor).thenCompose { bBoxes ->
                    try {
                        barcodeDecoder.decode(bitmap, bBoxes, executor)
                    } catch (e: InvalidInputException) {
                        throw RuntimeException(e)
                    }
                }.thenAccept { barcodes ->
                    cont.resume(barcodes)
                }.exceptionally { ex ->
                    cont.resumeWithException(ex)
                    null
                }
            } catch (e: AIVisionSDKException) {
                cont.resumeWithException(e)
            }
        }
    }

    /**
     * Stops the analysis process and cancels the coroutine job. This method should be
     * called to release resources and halt image analysis when it is no longer required.
     */
    fun stop() {
        isStopped = true
        job.cancel() // Cancel the coroutine job to stop processing
    }
}



