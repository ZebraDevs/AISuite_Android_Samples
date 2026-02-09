// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.lowlevel.productrecognitionsample

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.core.content.ContextCompat
import com.zebra.ai.vision.detector.AIVisionSDKLicenseException
import com.zebra.ai.vision.detector.FeatureExtractor
import com.zebra.ai.vision.detector.InferencerOptions
import com.zebra.ai.vision.detector.Localizer
import com.zebra.ai.vision.detector.Recognizer
import com.zebra.aisuite_quickstart.kotlin.CameraXLivePreviewActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Executors

/**
 * The ProductRecognitionHandler class is responsible for initializing and managing the product
 * recognition process, which involves detecting products and shelves, extracting features, and
 * recognizing products using a Localizer, FeatureExtractor, and Recognizer. This class sets up
 * the necessary components and manages their lifecycle within an Android application context.

 * The ProductRecognitionHandler configures models, assigns an analyzer for image analysis, and
 * provides methods to stop and dispose of resources when they are no longer needed.

 * Usage:
 * - Instantiate the ProductRecognitionHandler with the appropriate context, callback, and image analysis configuration.
 * - The class automatically initializes the Localizer, FeatureExtractor, and Recognizer.
 * - Use getProductRecognitionAnalyzer() to retrieve the current instance of the ProductRecognitionAnalyzer.
 * - Call stop() to terminate the executor service and dispose of the recognition components when finished.

 * Dependencies:
 * - Android Context: Required for resource management and accessing application assets.
 * - ExecutorService: Used for parallel task execution.
 * - ImageAnalysis: Provides the framework for analyzing image data.
 * - Localizer: Detects objects within an image.
 * - FeatureExtractor: Generates feature descriptors for detected objects.
 * - Recognizer: Matches feature descriptors to known products.

 * Exception Handling:
 * - Handles AIVisionSDKLicenseException and generic exceptions during initialization.
 * - Logs any other exceptions encountered during the setup process.

 * Note: Ensure that the appropriate permissions and dependencies are configured
 * in the AndroidManifest and build files to utilize camera and image processing capabilities.
 */
class ProductRecognitionSample(
    private val context: Context,
    private val callback: CameraXLivePreviewActivity,
    private val imageAnalysis: ImageAnalysis
) {
    private val TAG = "ProductRecognitionSample"
    private var localizer: Localizer? = null
    private var featureExtractor: FeatureExtractor? = null
    private var recognizer: Recognizer? = null

    private var localizerInitialized = false
    private var featureExtractorInitialized = false
    private var recognizerInitialized = false
    private val executor = Executors.newFixedThreadPool(3)
    private var productRecognitionSampleAnalyzer: ProductRecognitionSampleAnalyzer? = null
    private val mavenModelName = "product-and-shelf-recognizer"
    init {
        initializeProductRecognition()
    }
    /**
     * Initializes the product recognition components including the Localizer, FeatureExtractor,
     * and Recognizer. This method sets up the necessary components for detecting and recognizing
     * products within image data.
     */
    private fun initializeProductRecognition() {
        try {
            var mStart = System.currentTimeMillis()
            val locSettings = Localizer.Settings(mavenModelName)
            Log.d(TAG, "Shelf Localizer.settings() obj creation time = ${System.currentTimeMillis() - mStart} milli sec")

            mStart = System.currentTimeMillis()
            val feSettings = FeatureExtractor.Settings(mavenModelName)
            Log.d(TAG, "FeatureExtractor.Settings obj creation time = ${System.currentTimeMillis() - mStart} milli sec")

            val rpo = arrayOf(
                InferencerOptions.DSP,
                InferencerOptions.CPU,
                InferencerOptions.GPU
            )

            feSettings.inferencerOptions.runtimeProcessorOrder = rpo
            locSettings.inferencerOptions.runtimeProcessorOrder = rpo
            locSettings.inferencerOptions.defaultDims.height = 640
            locSettings.inferencerOptions.defaultDims.width = 640

            // Ensure that the following files match the ones included in assets
            val indexFilename = "product.index"
            val labelsFilename = "product.txt"
            val toPath = "${context.filesDir}/"
            copyFromAssets(indexFilename, toPath)
            copyFromAssets(labelsFilename, toPath)

            mStart = System.currentTimeMillis()
            val reSettings = Recognizer.SettingsIndex()
            Log.d(TAG, "Recognizer.SettingsIndex() obj creation time = ${System.currentTimeMillis() - mStart} milli sec")

            reSettings.indexFilename = "$toPath$indexFilename"
            reSettings.labelFilename = "$toPath$labelsFilename"

            val mStartLocalizer = System.currentTimeMillis()
            CoroutineScope(executor.asCoroutineDispatcher()).launch {
                try {
                    localizer = Localizer.getLocalizer(locSettings, executor).await()
                    localizerInitialized = true
                    setupAnalyzerIfReady()
                    Log.d(TAG, "Shelf Localizer(locSettings) obj creation / model loading time = ${System.currentTimeMillis() - mStartLocalizer} milli sec")
                } catch (e: AIVisionSDKLicenseException) {
                    Log.e(TAG, "AIVisionSDKLicenseException: Shelf Localizer object creation failed, ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Fatal error: load failed - ${e.message}")
                }
            }

            val mStartFeatureExtractor = System.currentTimeMillis()
            CoroutineScope(executor.asCoroutineDispatcher()).launch {
                try {
                    featureExtractor =
                        FeatureExtractor.getFeatureExtractor(feSettings, executor).await()
                    featureExtractorInitialized = true
                    setupAnalyzerIfReady()
                    Log.d(
                        TAG,
                        "FeatureExtractor() obj creation time = ${System.currentTimeMillis() - mStartFeatureExtractor} milli sec"
                    )
                } catch (e: AIVisionSDKLicenseException) {
                    Log.e(
                        TAG,
                        "AIVisionSDKLicenseException: Feature Extractor object creation failed, ${e.message}"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Fatal error: decoder creation failed - ${e.message}")
                }
            }

                val mStartRecognizer = System.currentTimeMillis()
                CoroutineScope(executor.asCoroutineDispatcher()).launch {
                    try {
                        recognizer = Recognizer.getRecognizer(reSettings, executor).await()
                        recognizerInitialized = true
                        setupAnalyzerIfReady()
                        Log.d(TAG, "Recognizer(reSettings) obj creation time = ${System.currentTimeMillis() - mStartRecognizer} milli sec")
                    } catch (e: Exception) {
                        Log.e(TAG, "Fatal error: recognizer creation failed - ${e.message}")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Fatal error: load failed - ${e.message}")
            }
        }

        /**
         * Sets up the ProductRecognitionAnalyzer if all components (localizer, feature extractor, recognizer) are initialized.
         */
        private fun setupAnalyzerIfReady() {
            if (localizerInitialized && featureExtractorInitialized && recognizerInitialized) {
                productRecognitionSampleAnalyzer = ProductRecognitionSampleAnalyzer(callback, localizer, featureExtractor, recognizer)
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), productRecognitionSampleAnalyzer!!)
            }
        }
    /**
     * Copies files from the assets folder to the specified path.
     *
     * @param filename The name of the file to copy.
     * @param toPath The destination path.
     */
    private fun copyFromAssets(filename: String, toPath: String) {
        val bufferSize = 8192
        try {
            context.assets.open(filename).use { stream ->
                Files.newOutputStream(Paths.get("$toPath$filename")).use { fos ->
                    BufferedOutputStream(fos).use { output ->
                        val data = ByteArray(bufferSize)
                        var count: Int
                        while (stream.read(data).also { count = it } != -1) {
                            output.write(data, 0, count)
                        }
                        output.flush()
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error in copy from assets ${e.message}")
        }
    }
    /**
     * Retrieves the current instance of the ProductRecognitionAnalyzer.
     *
     * @return The ProductRecognitionAnalyzer instance, or null if not yet initialized.
     */
    fun getProductRecognitionAnalyzer(): ProductRecognitionSampleAnalyzer? {
        return productRecognitionSampleAnalyzer
    }

    /**
     * Stops the executor service and disposes of the localizer, feature extractor, and recognizer,
     * releasing any resources held. This method should be called when product recognition is no
     * longer needed.
     */
    fun stop() {
        executor.shutdownNow()
        localizer?.let {
            it.dispose()
            Log.d(TAG, "Localizer is disposed")
            localizer = null
        }
        featureExtractor?.let {
            it.dispose()
            Log.d(TAG, "Feature extractor is disposed")
            featureExtractor = null
        }
        recognizer?.let {
            it.dispose()
            Log.d(TAG, "Recognizer is disposed")
            recognizer = null
        }

    }
}

