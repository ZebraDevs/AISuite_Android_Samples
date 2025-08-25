// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.lowlevel.simpleocrsample

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.core.content.ContextCompat
import com.zebra.ai.vision.detector.AIVisionSDKLicenseException
import com.zebra.ai.vision.detector.InferencerOptions
import com.zebra.ai.vision.detector.TextOCR
import com.zebra.ai.vision.internal.detector.Word
import com.zebra.aisuite_quickstart.kotlin.CameraXLivePreviewActivity
import com.zebra.aisuite_quickstart.kotlin.detectors.textocrsample.TextOCRAnalyzer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * The OCRSample class implements the OCRAnalyzer.DetectionCallback interface and is responsible
 * for setting up and managing the OCR (Optical Character Recognition) process using a TextOCR
 * engine. This class is designed to work within an Android application context and facilitates
 * asynchronous operations for text recognition.

 * The OCRSample class configures the TextOCR component, assigns an analyzer for image analysis,
 * and provides methods to stop and dispose of resources when they are no longer needed.

 * Usage:
 * - Instantiate the OCRSample with the necessary context, callback, and image analysis configuration.
 * - The class automatically initializes the TextOCR and TextOCRAnalyzer.
 * - Use getTextOCR() to retrieve the current instance of the TextOCR engine.
 * - Call stop() to dispose of the TextOCR engine and release resources.
 * - Implement the onDetectionTextResult() method to handle OCR results.

 * Dependencies:
 * - Android Context: Required for resource management and executing tasks on the main thread.
 * - ExecutorService: Used for asynchronous task execution.
 * - ImageAnalysis: Provides the framework for analyzing image data.
 * - TextOCR: Handles the detection and recognition of text within images.

 * Exception Handling:
 * - Handles AIVisionSDKLicenseException and generic exceptions during initialization.
 * - Logs any other exceptions encountered during the setup process.

 * Note: Ensure that the appropriate permissions and dependencies are configured
 * in the AndroidManifest and build files to utilize camera and image processing capabilities.
 */
class OCRSample(
    private val context: Context,
    private val callback: CameraXLivePreviewActivity,
    private val imageAnalysis: ImageAnalysis
) : OCRAnalyzer.DetectionCallback {

    private val TAG = "OCRSample"
    private var textOCR: TextOCR? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val mavenModelName = "text-ocr-recognizer"

    init {
        initializeTextOCR()
    }

    /**
     * Initializes the TextOCR engine with predefined settings for text detection and recognition.
     * This method sets up the necessary components for analyzing and recognizing text from image data.
     */
    private fun initializeTextOCR() {
        val textOCRSettings = TextOCR.Settings(mavenModelName).apply {
            val rpo = arrayOf(InferencerOptions.DSP)

            detectionInferencerOptions.runtimeProcessorOrder = rpo
            recognitionInferencerOptions.runtimeProcessorOrder = rpo
            detectionInferencerOptions.defaultDims.apply {
                height = 640
                width = 640
            }
        }

        val startTime = System.currentTimeMillis()

        CoroutineScope(executor.asCoroutineDispatcher()).launch {
            try {
                val ocrInstance = TextOCR.getTextOCR(textOCRSettings, executor).await()
                textOCR = ocrInstance
                val ocrAnalyzer = TextOCRAnalyzer(callback, ocrInstance)
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), ocrAnalyzer)

                Log.d(TAG, "TextOCR() obj creation / model loading time = ${System.currentTimeMillis() - startTime} milli sec")
            }
            catch (e: AIVisionSDKLicenseException) {
                Log.e(TAG, "AIVisionSDKLicenseException: TextOCR object creation failed, ${e.message}")
            }
            catch (e: Exception) {
                Log.e(TAG, "Fatal error: TextOCR creation failed - ${e.message}")
            }
        }
    }

    /**
     * Disposes of the TextOCR engine, releasing any resources held.
     * This method should be called when OCR processing is no longer needed.
     */
    fun stop() {
        textOCR?.let {
            it.dispose()
            Log.d(TAG, "OCR is disposed")
        }
    }

    /**
     * Retrieves the current instance of the TextOCR engine.
     *
     * @return The TextOCR instance, or null if not yet initialized.
     */
    fun getTextOCR(): TextOCR? {
        return textOCR
    }

    /**
     * Callback method invoked when OCR text detection results are available.
     *
     * @param list An array of Word objects representing detected words.
     */
    override fun onDetectionTextResult(list: Array<Word>) {
        for (word in list) {
            // Append each word's content followed by a newline
            if (word.decodes.isNotEmpty()) {
                Log.d(TAG, "Detected word: ${word.decodes[0]}")
            }
        }
    }
}
