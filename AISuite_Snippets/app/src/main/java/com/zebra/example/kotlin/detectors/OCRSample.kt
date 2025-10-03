// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.example.kotlin.detectors

import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageProxy
import com.zebra.ai.vision.detector.AIVisionSDKException
import com.zebra.ai.vision.detector.AIVisionSDKLicenseException
import com.zebra.ai.vision.detector.ImageData
import com.zebra.ai.vision.detector.InferencerOptions
import com.zebra.ai.vision.detector.TextOCR
import com.zebra.ai.vision.entity.ParagraphEntity
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
 * OCRSample is a Kotlin class that demonstrates how to initialize and use an
 * Optical Character Recognition (OCR) system to process images and extract text
 * asynchronously using Kotlin coroutines.
 */
class OCRSample {

    // Tag used for logging purposes
    private val TAG = "OCRSample"

    // Instance of TextOCR used for text recognition
    private var textOCR: TextOCR? = null

    // Executor service for handling asynchronous operations
    private val executor = Executors.newSingleThreadExecutor()
    private val mavenModelName = "text-ocr-recognizer"

    /**
     * Initializes the OCRSample by setting up the text OCR system.
     */
    init {
        initializeTextOCR()
    }

    /**
     * Configures and initializes the TextOCR with specific settings.
     * The OCR system is set up to recognize text within certain dimensions.
     */
    private fun initializeTextOCR() {
        // Create settings for the text OCR using the apply scope function
        val textOCRSettings = TextOCR.Settings(mavenModelName).apply {
            val rpo = arrayOf(
                InferencerOptions.DSP,
                InferencerOptions.CPU,
                InferencerOptions.GPU
            )

            detectionInferencerOptions.runtimeProcessorOrder = rpo
            recognitionInferencerOptions.runtimeProcessorOrder = rpo
            detectionInferencerOptions.defaultDims.apply {
                height = 640
                width = 640
            }
        }

        // Record the start time for profiling
        val startTime = System.currentTimeMillis()

        // Use a coroutine scope with the executor's coroutine dispatcher to initialize the OCR
        CoroutineScope(executor.asCoroutineDispatcher()).launch {
            try {
                // Await the asynchronous creation of the TextOCR instance
                val ocrInstance = TextOCR.getTextOCR(textOCRSettings, executor).await()
                textOCR = ocrInstance

                Log.d(TAG, "TextOCR() obj creation / model loading time = ${System.currentTimeMillis() - startTime} milli sec")
            } catch (e: AIVisionSDKLicenseException) {
                Log.e(TAG, "AIVisionSDKLicenseException: TextOCR object creation failed, ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Fatal error: TextOCR creation failed - ${e.message}")
            }
        }
    }

    /**
     * Processes an image using the TextOCR to extract text.
     * This function operates asynchronously and updates results on the UI thread.
     *
     * @param image The image to be processed, provided as an ImageProxy
     */
    private suspend fun processImage(image: ImageProxy) {
        try {
            Log.d(TAG, "Starting image analysis")

            // Process the image asynchronously to detect text
            val result = processImageAsync(image)

            // Switch to the main dispatcher to update UI with results
            withContext(Dispatchers.Main) {
                for (entity in result) {
                    val lines = entity.textParagraph.lines
                    for (line in lines) {
                        for (word in line.words) {
                            val bbox = word.bbox

                            if (bbox != null && bbox.x != null && bbox.y != null && bbox.x.size >= 3 && bbox.y.size >= 3) {
                                val minX = bbox.x[0]
                                val maxX = bbox.x[2]
                                val minY = bbox.y[0]
                                val maxY = bbox.y[2]

                                val rect = Rect(minX.toInt(), minY.toInt(), maxX.toInt(), maxY.toInt())
                                val decodedValue = word.decodes[0].content
                                Log.v(TAG, "Decoded value: $decodedValue")
                            }
                        }
                    }
                }
                image.close()
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Error during image processing: ${ex.message}")
            image.close()
        }
    }

    /**
     * Asynchronously processes an image to extract text using the TextOCR.
     * This method suspends the coroutine until the text processing is complete.
     *
     * @param image The image to be processed, provided as an ImageProxy
     * @return A list of detected ParagraphEntity objects
     */
    private suspend fun processImageAsync(image: ImageProxy): List<ParagraphEntity> {
        return suspendCancellableCoroutine { cont ->
            try {
                textOCR?.process(ImageData.fromImageProxy(image))
                    ?.thenAccept { result ->
                        cont.resume(result) // Resume the coroutine with the result
                    }
                    ?.exceptionally { ex ->
                        cont.resumeWithException(ex) // Resume with exception
                        image.close()
                        null
                    }
            } catch (e: AIVisionSDKException) {
                cont.resumeWithException(e)
                image.close()
            }
        }
    }
}
