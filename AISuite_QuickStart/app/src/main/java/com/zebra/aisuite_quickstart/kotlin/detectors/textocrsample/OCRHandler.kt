// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.detectors.textocrsample

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.core.content.ContextCompat
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
    private val imageAnalysis: ImageAnalysis,
    private val loadingCallback: ((Boolean) -> Unit)? = null
) {

    private val tag = "OCRHandler"
    private var textOCR: TextOCR? = null // For live preview
    var captureOCR: TextOCR? = null // For capture mode

    lateinit var ocrAnalyzer: TextOCRAnalyzer
    private val executor = Executors.newSingleThreadExecutor()
    private val captureExecutor = Executors.newSingleThreadExecutor()
    private val mavenModelName = "text-ocr-recognizer"

    // Model input sizes
    companion object {
        private const val LIVE_PREVIEW_SIZE = 640
        private const val CAPTURE_SIZE = 1280 // Higher resolution for capture
    }

    init {
        initializeTextOCR()
        initializeCaptureOCR()
    }

    /**
     * Creates OCR settings with specified input size.
     *
     * @param inputSize The input dimension size for the OCR model.
     * @return Configured TextOCR.Settings instance.
     */
    private fun createOCRSettings(inputSize: Int): TextOCR.Settings {
        return TextOCR.Settings(mavenModelName).apply {
            val rpo = arrayOf(
                InferencerOptions.DSP,
                InferencerOptions.CPU,
                InferencerOptions.GPU
            )

            detectionInferencerOptions.runtimeProcessorOrder = rpo
            recognitionInferencerOptions.runtimeProcessorOrder = rpo

            detectionInferencerOptions.defaultDims.apply {
                height = inputSize
                width = inputSize
            }
        }
    }

    /**
     * Initializes the live preview TextOCR with smaller input size for real-time processing.
     */
    private fun initializeTextOCR() {
        try {
            val liveOCRSettings = createOCRSettings(LIVE_PREVIEW_SIZE)
            CoroutineScope(executor.asCoroutineDispatcher()).launch {
                createTextOCRWithFallback(liveOCRSettings)
            }
        } catch (e: Exception) {
            loadingCallback?.invoke(false)
            Log.e(tag, "Fatal error: load failed - ${e.message}")
        }
    }

    /**
     * Initializes the capture OCR with higher resolution settings.
     */
    fun initializeCaptureOCR() {
        try {
            val captureOCRSettings = createOCRSettings(CAPTURE_SIZE)
            CoroutineScope(captureExecutor.asCoroutineDispatcher()).launch {
                createCaptureOCRWithFallback(captureOCRSettings)
            }
        } catch (ex: Exception) {
            loadingCallback?.invoke(false)
            Log.e(tag, "Capture OCR initialization failed: ${ex.message}")
        }
    }

    /**
     * Creates the live preview TextOCR instance with fallback error handling.
     * Only notifies loading complete and attaches analyzer when both models are loaded.
     */
    private suspend fun createTextOCRWithFallback(textOCRSettings: TextOCR.Settings) {
        val startTime = System.currentTimeMillis()
        try {
            val ocrInstance = TextOCR.getTextOCR(textOCRSettings, executor).await()
            textOCR = ocrInstance

            if (captureOCR != null) {
                loadingCallback?.invoke(true)
                attachAnalysisAfterModelLoading()
            }

            Log.d(
                tag,
                "TextOCR() obj creation / model loading time = ${System.currentTimeMillis() - startTime} ms" +
                        " and input size: ${textOCRSettings.detectionInferencerOptions.defaultDims.width}"
            )
        } catch (e: Exception) {
            loadingCallback?.invoke(false)
            Log.e(tag, "Fatal error: TextOCR creation failed - ${e.message}")
        }
    }

    /**
     * Creates the capture TextOCR instance with fallback error handling.
     * Only notifies loading complete and attaches analyzer when both models are loaded.
     */
    private suspend fun createCaptureOCRWithFallback(textOCRSettings: TextOCR.Settings) {
        val startTime = System.currentTimeMillis()
        try {
            val ocrInstance = TextOCR.getTextOCR(textOCRSettings, captureExecutor).await()
            captureOCR = ocrInstance

            if (textOCR != null) {
                loadingCallback?.invoke(true)
                attachAnalysisAfterModelLoading()
            }

            Log.d(tag, "Capture TextOCR created in ${System.currentTimeMillis() - startTime} ms")
        } catch (e: Exception) {
            loadingCallback?.invoke(false)
            Log.e(tag, "Capture OCR creation failed: ${e.message}")
        }
    }

    /**
     * Attaches the TextOCRAnalyzer to the ImageAnalysis once both models are loaded.
     */
    private fun attachAnalysisAfterModelLoading() {
        ocrAnalyzer = TextOCRAnalyzer(callback, textOCR)
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), ocrAnalyzer)
    }

    /**
     * Stops the executor services and disposes of both TextOCR instances, releasing any resources held.
     * This method should be called when OCR processing is no longer needed.
     */
    fun stop() {
        executor.shutdownNow()
        captureExecutor.shutdownNow()
        textOCR?.let {
            it.dispose()
            Log.v(tag, "Live preview OCR is disposed")
            textOCR = null
        }
        captureOCR?.let {
            it.dispose()
            Log.v(tag, "Capture OCR is disposed")
            captureOCR = null
        }
    }

}
