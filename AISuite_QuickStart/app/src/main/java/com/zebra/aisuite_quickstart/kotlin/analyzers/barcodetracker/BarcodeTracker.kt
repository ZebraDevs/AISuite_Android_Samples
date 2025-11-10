// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.analyzers.barcodetracker

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.core.content.ContextCompat
import com.zebra.ai.vision.analyzer.tracking.EntityTrackerAnalyzer
import com.zebra.ai.vision.detector.AIVisionSDKLicenseException
import com.zebra.ai.vision.detector.BarcodeDecoder
import com.zebra.ai.vision.detector.InferencerOptions
import com.zebra.ai.vision.detector.TextOCR
import com.zebra.aisuite_quickstart.filtertracker.FilterType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * The BarcodeTracker class is responsible for initializing and managing the barcode detection
 * process using a BarcodeDecoder and EntityTrackerAnalyzer. This class is designed to work
 * within an Android application context and facilitate asynchronous operations for barcode
 * detection and tracking.

 * The BarcodeTracker sets up the decoder, assigns an analyzer for image analysis, and provides
 * methods to stop and dispose of resources when they are no longer needed.

 * Usage:
 * - Instantiate the BarcodeTracker with the necessary context, callback, and image analysis configuration.
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
class BarcodeTracker(
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
        if (filterType == FilterType.BOTH) initializeBarcodeDecoderOCR()
        if (filterType == FilterType.BARCODE) initializeBarcodeDecoder()
        if (filterType == FilterType.OCR) initializeTextOCR()
    }

    /**
     * Initializes the BarcodeDecoder with predefined settings for barcode symbologies
     * and detection parameters. This method sets up the necessary components for analyzing
     * and decoding barcodes from image data asynchronously using coroutines.
     */
    private fun initializeBarcodeDecoderOCR() {
        val rpo = arrayOf(
            InferencerOptions.DSP,
            InferencerOptions.CPU,
            InferencerOptions.GPU
        )
        val decoderSettings = BarcodeDecoder.Settings(mavenModelName).apply {

            Symbology.CODE39.enable(true)
            Symbology.CODE128.enable(true)

            detectorSetting.inferencerOptions.apply {
                runtimeProcessorOrder = rpo
                defaultDims.height = 640
                defaultDims.width = 640
            }
        }
        val textOCRSettings = TextOCR.Settings(mavenOCRModelName).apply {

            detectionInferencerOptions.runtimeProcessorOrder = rpo
            recognitionInferencerOptions.runtimeProcessorOrder = rpo
            detectionInferencerOptions.defaultDims.apply {
                height = 640
                width = 640
            }
        }

        val start_Time = System.currentTimeMillis()

        CoroutineScope(executor.asCoroutineDispatcher()).launch {
            try {
                val decoderInstance = BarcodeDecoder.getBarcodeDecoder(decoderSettings, executor).await()
                barcodeDecoder = decoderInstance
                barcodeInitialized = true
                setupTrackerIfReady()

                Log.d(TAG, "Entity Tracker BarcodeDecoder() obj creation time = ${System.currentTimeMillis() - start_Time} milli sec")
            }
            catch (e: AIVisionSDKLicenseException) {
                Log.e(TAG, "AIVisionSDKLicenseException: Barcode Decoder object creation failed, ${e.message}")
            }
            catch (e: Exception) {
                Log.e(TAG, "Fatal error: decoder creation failed - ${e.message}")
            }
        }

        val startTime = System.currentTimeMillis()

        CoroutineScope(executor.asCoroutineDispatcher()).launch {
            try {
                val ocrInstance = TextOCR.getTextOCR(textOCRSettings, executor).await()
                textOCR = ocrInstance
                ocrInitialized = true
                setupTrackerIfReady()

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
     * Initializes the BarcodeDecoder with predefined settings for barcode symbologies
     * and detection parameters. This method sets up the necessary components for analyzing
     * and decoding barcodes from image data asynchronously using coroutines.
     */
    private fun initializeBarcodeDecoder() {
        val decoderSettings = BarcodeDecoder.Settings(mavenModelName).apply {
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

        val startTime = System.currentTimeMillis()

        CoroutineScope(executor.asCoroutineDispatcher()).launch {
            try {
                val decoderInstance = BarcodeDecoder.getBarcodeDecoder(decoderSettings, executor).await()
                barcodeDecoder = decoderInstance
                entityTrackerAnalyzer = EntityTrackerAnalyzer(
                    listOf(barcodeDecoder),
                    ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
                    executor,
                    ::handleEntities
                )
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), entityTrackerAnalyzer!!)

                Log.d(TAG, "BarcodeDecoder() obj creation time = ${System.currentTimeMillis() - startTime} milli sec")
            }
            catch (e: AIVisionSDKLicenseException) {
                Log.e(TAG, "AIVisionSDKLicenseException: Barcode Decoder object creation failed, ${e.message}")
            }
            catch (e: Exception) {
                Log.e(TAG, "Fatal error: decoder creation failed - ${e.message}")
            }
        }
    }

    /**
     * Initializes the TextOCR with predefined settings for text detection and recognition.
     * This method sets up the necessary components for analyzing and recognizing text from
     * image data asynchronously using coroutines.
     */
    private fun initializeTextOCR() {
        val textOCRSettings = TextOCR.Settings(mavenOCRModelName).apply {
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

        val startTime = System.currentTimeMillis()

        CoroutineScope(executor.asCoroutineDispatcher()).launch {
            try {
                val ocrInstance = TextOCR.getTextOCR(textOCRSettings, executor).await()
                textOCR = ocrInstance
                entityTrackerAnalyzer = EntityTrackerAnalyzer(
                    listOf(textOCR),
                    ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
                    executor,
                    ::handleEntities
                )
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), entityTrackerAnalyzer!!)

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
     * Sets up the Tracker if all components (localizer, feature extractor, recognizer) are initialized.
     */
    private fun setupTrackerIfReady() {
        if (barcodeInitialized && ocrInitialized) {
            entityTrackerAnalyzer = EntityTrackerAnalyzer(
                listOf(barcodeDecoder, textOCR),
                ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
                executor,
                ::handleEntities
            )
            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), entityTrackerAnalyzer!!)
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
