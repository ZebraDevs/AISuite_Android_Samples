// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.detectors.textocrsample

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.core.content.ContextCompat
import com.zebra.ai.vision.detector.AIVisionSDKLicenseException
import com.zebra.ai.vision.detector.InferencerOptions
import com.zebra.ai.vision.detector.TextOCR
import com.zebra.aisuite_quickstart.kotlin.CameraXLivePreviewActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * The OCRHandler class is responsible for initializing and managing the OCR (Optical Character
 * Recognition) process using a TextOCR and TextOCRAnalyzer. This class is designed to work within
 * an Android application context and facilitates asynchronous operations for text recognition.

 * The OCRHandler configures the OCR model, assigns an analyzer for image analysis, and provides
 * methods to stop and dispose of resources when they are no longer needed.

 * Usage:
 * - Instantiate the OCRHandler with the necessary context, callback, and image analysis configuration.
 * - The class automatically initializes the TextOCR and TextOCRAnalyzer.
 * - Use getOCRAnalyzer() to retrieve the current instance of the TextOCRAnalyzer.
 * - Call stop() to dispose of the TextOCR and release resources.

 * Dependencies:
 * - Android Context: Required for managing resources and executing tasks on the main thread.
 * - ExecutorService: Used for asynchronous task execution.
 * - ImageAnalysis: Provides the framework for analyzing image data.
 * - TextOCR: Handles the detection and recognition of text within images.
 * - TextOCRAnalyzer: Analyzes images to detect text and report results via a callback.

 * Exception Handling:
 * - Handles AIVisionSDKLicenseException during OCR initialization.
 * - Logs any other exceptions encountered during the setup process.

 * Note: Ensure that the appropriate permissions and dependencies are configured
 * in the AndroidManifest and build files to utilize camera and image processing capabilities.
 */
class OCRHandler(
    private val context: Context,
    private val callback: CameraXLivePreviewActivity,
    private val imageAnalysis: ImageAnalysis
) {

    private val TAG = "OCRHandler"
    private var textOCR: TextOCR? = null

    private lateinit var ocrAnalyzer: TextOCRAnalyzer
    private val executor = Executors.newSingleThreadExecutor()
    private val mavenModelName = "text-ocr-recognizer"

    init {
        initializeTextOCR()
    }

    /**
     * Initializes the TextOCR with predefined settings for text detection and recognition.
     * This method sets up the necessary components for analyzing and recognizing text from
     * image data asynchronously using coroutines.
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
                ocrAnalyzer = TextOCRAnalyzer(callback, ocrInstance)
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
     * Stops the executor service and disposes of the TextOCR, releasing any resources held.
     * This method should be called when OCR processing is no longer needed.
     */
    fun stop() {
        executor.shutdownNow()
        textOCR?.let {
            it.dispose()
            Log.v(TAG, "OCR is disposed")
            textOCR = null
        }
    }

    /**
     * Retrieves the current instance of the TextOCRAnalyzer.
     *
     * @return The TextOCRAnalyzer instance.
     */
    fun getOCRAnalyzer(): TextOCRAnalyzer {
        return ocrAnalyzer
    }
}
