// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.analyzers.tracker

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.core.content.ContextCompat
import com.zebra.ai.vision.analyzer.tracking.EntityTrackerAnalyzer
import com.zebra.ai.vision.detector.AIVisionSDKLicenseException
import com.zebra.ai.vision.detector.BarcodeDecoder
import com.zebra.ai.vision.detector.Detector
import com.zebra.ai.vision.detector.InferencerOptions
import com.zebra.ai.vision.detector.TextOCR
import com.zebra.ai.vision.entity.Entity
import com.zebra.aisuite_quickstart.filtertracker.FilterType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * The Tracker class is responsible for initializing and managing the barcode detection
 * process using a BarcodeDecoder and EntityTrackerAnalyzer. This class is designed to work
 * within an Android application context and facilitate asynchronous operations for barcode
 * detection and tracking.

 * The Tracker sets up the decoder, assigns an analyzer for image analysis, and provides
 * methods to stop and dispose of resources when they are no longer needed.

 * Usage:
 * - Instantiate the Tracker with the necessary context, callback, and image analysis configuration.
 * - The class automatically initializes the BarcodeDecoder and EntityTrackerAnalyzer.
 * - The handleEntities() method is called to process detection results.
 * - Call stop() to dispose of the BarcodeDecoder and release resources.
 * - Call stopAnalyzing() to terminate the executor service and stop ongoing analysis tasks.

 * Dependencies:
 * - Android Context: Required for managing resources and executing tasks on the main thread.
 * - ExecutorService: Used for asynchronous task execution.
 * - ImageAnalysis: Provides the framework for analyzing image data.
 * - BarcodeDecoder: Handles the decoding of barcode symbologies.
 * - EntityTrackerAnalyzer: Analyzes images to track and decode barcodes.

 * Exception Handling:
 * - Handles AIVisionSDKLicenseException during decoder initialization.
 * - Logs any other exceptions encountered during the setup process.

 * Note: Ensure that the appropriate permissions and dependencies are configured
 * in the AndroidManifest and build files to utilize camera and image processing capabilities.
 */
class Tracker(
    private val context: Context,
    private val callback: DetectionCallback,
    private val imageAnalysis: ImageAnalysis,
    filterType: FilterType
) {

    /**
     * Interface for handling the results of the barcode detection process.
     * Implement this interface to define how results are processed after detection.
     */
    interface DetectionCallback {
        fun handleEntities(result: EntityTrackerAnalyzer.Result)
    }

    private val TAG = "EntityTrackerHandler"
    private var barcodeDecoder: BarcodeDecoder? = null

    private var textOCR: TextOCR? = null
    private val executor = Executors.newSingleThreadExecutor()
    private var entityTrackerAnalyzer: EntityTrackerAnalyzer? = null
    private val mavenModelName = "barcode-localizer"
    private val mavenOCRModelName = "text-ocr-recognizer"
    private var barcodeInitialized = false
    private var ocrInitialized = false

    init {
        when (filterType) {
                        FilterType.BOTH -> initializeBoth()
                        FilterType.BARCODE -> initializeBarcodeDecoder()
                        FilterType.OCR -> initializeTextOCR()
                        FilterType.NONE -> Log.d(TAG,"None of the filter selected")
        }

    }

    /**
     * Defines the preferred order of processors for AI inference operations.
     * Returns an array prioritizing DSP, then CPU, then GPU for optimal performance.
     *
     * @return Array of InferencerOptions constants representing processor priority order
     */
    private fun getProcessorOrder() = arrayOf(
        InferencerOptions.DSP,
        InferencerOptions.CPU,
        InferencerOptions.GPU
    )

    /**
     * Creates and configures settings for barcode detection.
     * Enables CODE39 and CODE128 symbologies and sets up inference options
     * with processor order and default dimensions for optimal barcode detection.
     *
     * @return Configured BarcodeDecoder.Settings instance
     */
    private fun configureBarcodeSettings() = BarcodeDecoder.Settings(mavenModelName).apply {
        Symbology.CODE39.enable(true)
        Symbology.CODE128.enable(true)
        detectorSetting.inferencerOptions.apply {
            runtimeProcessorOrder = getProcessorOrder()
            defaultDims.height = 640
            defaultDims.width = 640
        }
    }

    /**
     * Creates and configures settings for text OCR detection and recognition.
     * Sets up both detection and recognition inference options with processor
     * order and default dimensions for optimal text processing.
     *
     * @return Configured TextOCR.Settings instance
     */
    private fun configureOCRSettings() = TextOCR.Settings(mavenOCRModelName).apply {
        val rpo = getProcessorOrder()
        detectionInferencerOptions.runtimeProcessorOrder = rpo
        recognitionInferencerOptions.runtimeProcessorOrder = rpo
        detectionInferencerOptions.defaultDims.apply {
            height = 640
            width = 640
        }
    }

    /**
     * Asynchronously creates and initializes a BarcodeDecoder instance.
     * Measures and logs the creation time for performance monitoring.
     * Handles exceptions gracefully and returns null on failure.
     *
     * @return BarcodeDecoder instance on success, null on failure
     */
    private suspend fun createBarcodeDecoder(): BarcodeDecoder? = try {
        val startTime = System.currentTimeMillis()
        val decoder = BarcodeDecoder.getBarcodeDecoder(configureBarcodeSettings(), executor).await()
        Log.d(TAG, "BarcodeDecoder creation time: ${System.currentTimeMillis() - startTime} ms")
        decoder
    } catch (e: Exception) {
        handleException("BarcodeDecoder", e)
        null
    }

    /**
     * Asynchronously creates and initializes a TextOCR instance.
     * Measures and logs the creation time for performance monitoring.
     * Handles exceptions gracefully and returns null on failure.
     *
     * @return TextOCR instance on success, null on failure
     */
    private suspend fun createTextOCR(): TextOCR? = try {
        val startTime = System.currentTimeMillis()
        val ocr = TextOCR.getTextOCR(configureOCRSettings(), executor).await()
        Log.d(TAG, "TextOCR creation time: ${System.currentTimeMillis() - startTime} ms")
        ocr
    } catch (e: Exception) {
        handleException("TextOCR", e)
        null
    }

    /**
     * Handles exceptions during component initialization.
     * Differentiates between license errors and other fatal errors,
     * providing appropriate error messages for debugging.
     *
     * @param component Name of the component that failed to initialize
     * @param e Exception that occurred during initialization
     */
    private fun handleException(component: String, e: Exception) {
        val message = when (e) {
            is AIVisionSDKLicenseException -> "License error: ${e.message}"
            else -> "Fatal error: ${e.message}"
        }
        Log.e(TAG, "$component creation failed - $message")
    }

    /**
     * Creates and configures an EntityTrackerAnalyzer with the provided analyzers.
     * Sets up the analyzer with original coordinate system and assigns it to the
     * image analysis pipeline using the main executor.
     *
     * @param analyzers List of detector instances (BarcodeDecoder, TextOCR, etc.)
     */
    private fun createAnalyzer(analyzers: List<Any?>) {
        entityTrackerAnalyzer = EntityTrackerAnalyzer(
            analyzers.filterIsInstance<Detector<out List<Entity?>?>?>(),
            ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
            executor,
            ::handleEntities
        )
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), entityTrackerAnalyzer!!)
    }

    /**
     * Initializes both barcode decoder and text OCR components concurrently.
     * Uses separate coroutines for each component to allow parallel initialization
     * and calls setupTrackerIfReady() when each component completes.
     */
    private fun initializeBoth() {
        CoroutineScope(executor.asCoroutineDispatcher()).launch {
            barcodeDecoder = createBarcodeDecoder()
            barcodeInitialized = true
            setupTrackerIfReady()
        }

        CoroutineScope(executor.asCoroutineDispatcher()).launch {
            textOCR = createTextOCR()
            ocrInitialized = true
            setupTrackerIfReady()
        }
    }

    /**
     * Initializes the BarcodeDecoder with predefined settings for barcode symbologies
     * and detection parameters. This method sets up the necessary components for analyzing
     * and decoding barcodes from image data asynchronously using coroutines.
     */
    private fun initializeBarcodeDecoder() {
        CoroutineScope(executor.asCoroutineDispatcher()).launch {
            barcodeDecoder = createBarcodeDecoder()
            barcodeDecoder?.let { createAnalyzer(listOf(it)) }
        }
    }

    /**
     * Initializes the TextOCR with predefined settings for text detection and recognition.
     * This method sets up the necessary components for analyzing and recognizing text from
     * image data asynchronously using coroutines.
     */
    private fun initializeTextOCR() {
        CoroutineScope(executor.asCoroutineDispatcher()).launch {
            textOCR = createTextOCR()
            textOCR?.let { createAnalyzer(listOf(it)) }
        }
    }

    /**
     * Sets up the Tracker if all components (localizer, feature extractor, recognizer) are initialized.
     */
    private fun setupTrackerIfReady() {
        if (barcodeInitialized && ocrInitialized) {
            createAnalyzer(listOf(barcodeDecoder, textOCR))
        }
    }


    /**
     * Handles the results of the tracker detection by invoking the callback with the result.
     *
     * @param result The result of the barcode detection process.
     */
    private fun handleEntities(result: EntityTrackerAnalyzer.Result) {
        callback.handleEntities(result)
    }

    /**
     * Stops and disposes of the BarcodeDecoder and textOCR, releasing any resources held.
     * This method should be called when tracker is no longer needed.
     */
    fun stop() {
        barcodeDecoder?.let {
            it.dispose()
            Log.d(TAG, "Barcode decoder is disposed")
            barcodeDecoder = null
        }
        textOCR?.let {
            it.dispose()
            Log.v(TAG, "OCR is disposed")
            textOCR = null
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

    /**
     * Retrieves the current instance of the TextOCR.
     *
     * @return The TextOCR instance, or null if not yet initialized.
     */
    fun getTextOCR(): TextOCR? {
        return textOCR
    }

    /**
     * Stops the ExecutorService, terminating any ongoing analysis tasks.
     * This method should be called to clean up resources when analysis is no longer required.
     */
    fun stopAnalyzing() {
        executor.shutdownNow()
    }
}
