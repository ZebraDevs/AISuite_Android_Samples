// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.example.kotlin.lowlevel

import android.util.Log
import androidx.camera.core.ImageProxy
import com.zebra.ai.vision.detector.AIVisionSDKLicenseException
import com.zebra.ai.vision.detector.InferencerOptions
import com.zebra.ai.vision.detector.InvalidInputException
import com.zebra.ai.vision.detector.TextOCR
import com.zebra.ai.vision.internal.detector.Paragraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * OCRLegacySample is a Kotlin class that demonstrates the use of Optical Character Recognition (OCR)
 * to extract text from images. It initializes an OCR model and processes images to detect and output
 * text, with options for grouping text into paragraphs or detecting individual words.
 */
class OCRLegacySample {

    // Tag used for logging purposes
    private val TAG = "OCRLegacySample"

    // Instance of TextOCR used for text recognition
    private var textOCR: TextOCR? = null

    // Flag to enable or disable grouping of detected text into paragraphs
    private var enableGrouping = false

    // Executor service for handling asynchronous operations
    private val executor = Executors.newSingleThreadExecutor()
    private val mavenModelName = "text-ocr-recognizer"

    /**
     * Initializes the OCRLegacySample by setting up the text OCR system.
     */
    init {
        initializeTextOCR()
    }

    /**
     * Configures and initializes the TextOCR with specific settings.
     * The OCR system is set up to recognize text within specific dimensions and processor order.
     */
    private fun initializeTextOCR() {
        // Create settings for the text OCR using the apply scope function
        val textOCRSettings = TextOCR.Settings(mavenModelName).apply {
            val rpo = arrayOf(InferencerOptions.DSP)

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
     * This function operates asynchronously and can either group text into paragraphs or detect individual words.
     *
     * @param imageProxy The image to be processed, provided as an ImageProxy
     */
    private fun processImage(imageProxy: ImageProxy) {
        CoroutineScope(Dispatchers.IO).launch {
            // Ensure the text OCR is initialized before processing the image
            try {
                val bitmap = imageProxy.toBitmap()

                try {
                    if (enableGrouping) {
                        // Detect and log paragraphs of text
                        val paragraphs: Array<out Paragraph?>? = textOCR?.detectParagraphs(bitmap, executor)?.get()
                        paragraphs?.forEach { paragraph ->
                            val paragraphText = paragraph.toString()
                            Log.d(TAG, "Paragraph Text: $paragraphText")
                        }
                    } else {
                        // Detect and log individual words
                        val words = textOCR?.detectWords(bitmap, executor)?.get()
                        words?.forEach { word ->
                            val decodedValue = word.decodes[0].content
                            Log.d(TAG, "Decoded value: $decodedValue")
                        }
                    }
                } catch (e: InvalidInputException) {
                    Log.e(TAG, "Exception occurred: ${e.message}")
                } finally {
                    imageProxy.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while processing image: ${e.message}")
                imageProxy.close()
            }
        }
    }
}
