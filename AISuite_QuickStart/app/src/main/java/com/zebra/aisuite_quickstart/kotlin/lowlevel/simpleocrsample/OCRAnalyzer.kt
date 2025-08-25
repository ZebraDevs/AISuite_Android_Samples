// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.lowlevel.simpleocrsample

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.zebra.ai.vision.detector.InvalidInputException
import com.zebra.ai.vision.detector.TextOCR
import com.zebra.ai.vision.internal.detector.Word
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
 * The OCRAnalyzer class implements the ImageAnalysis.Analyzer interface and is responsible
 * for asynchronously analyzing image frames to perform Optical Character Recognition (OCR) using
 * a TextOCR engine. This class utilizes Kotlin coroutines to handle image processing tasks and
 * callbacks to deliver recognition results.

 * This class is designed to be used within an Android application as part of a camera-based OCR
 * solution. It processes image data and returns recognized text results through a callback interface.

 * Usage:
 * - Instantiate the OCRAnalyzer with the appropriate DetectionCallback and TextOCR instance.
 * - Implement the DetectionCallback interface to handle OCR results.
 * - The analyze(ImageProxy) method is called by the camera framework to process image frames.
 * - Call stop() to terminate ongoing analysis and release resources.

 * Dependencies:
 * - Android ImageProxy: Provides access to image data from the camera.
 * - CoroutineScope: Used for asynchronous task execution, leveraging Kotlin coroutines.
 * - TextOCR: Processes image data to detect and recognize words.

 * Concurrency:
 * - Uses a CoroutineScope to ensure that image analysis tasks are processed asynchronously.
 * - Manages concurrency with flags to control analysis state and prevent re-entry.

 * Note: Ensure that the appropriate permissions and dependencies are configured
 * in the AndroidManifest and build files to utilize camera and image processing capabilities.
 */
class OCRAnalyzer(
    private val callback: DetectionCallback,
    private val textOCR: TextOCR
) : ImageAnalysis.Analyzer {

    /**
     * Interface for handling the results of the OCR detection process.
     * Implement this interface to define how OCR results are processed.
     */
    interface DetectionCallback {
        fun onDetectionTextResult(list: Array<Word>)
    }

    private val TAG = "TextOCRAnalyzer"
    private var isAnalyzing = true
    private var isStopped = false
    private val executor = Executors.newSingleThreadExecutor()

    // Create a CoroutineScope with the IO dispatcher and a Job for lifecycle management
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    /**
     * Analyzes the given image to perform OCR. This method is called by the camera
     * framework to process image frames asynchronously.
     *
     * @param image The image frame to analyze.
     */
    override fun analyze(image: ImageProxy) {
        if (!isAnalyzing || isStopped) {
            image.close()
            return
        }

        isAnalyzing = false // Set to false to prevent re-entry

        scope.launch {
            try {
                Log.d(TAG, "Starting image analysis")
                val words = processImageAsync(image)
                withContext(Dispatchers.Main) {
                    if (!isStopped) {
                        callback.onDetectionTextResult(words)
                    }
                    isAnalyzing = true
                    image.close()
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Error during image processing: ${ex.message}")
                isAnalyzing = true
                image.close()
            }
        }
    }

    /**
     * Processes the image asynchronously using the TextOCR.
     *
     * @param image The ImageProxy containing the image data to process.
     * @return An array of Word objects representing the detected text.
     */
    private suspend fun processImageAsync(image: ImageProxy): Array<Word> {
        return suspendCancellableCoroutine { cont ->
            try {
                val bitmap = image.toBitmap()
                textOCR.detectWords(bitmap, executor).thenAccept { words ->
                    cont.resume(words) // Resume the coroutine with the result
                }.exceptionally { ex ->
                    cont.resumeWithException(ex) // Resume with exception
                    isAnalyzing = true
                    image.close()
                    null
                }
            } catch (e: InvalidInputException) {
                cont.resumeWithException(e)
                isAnalyzing = true
                image.close()
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


