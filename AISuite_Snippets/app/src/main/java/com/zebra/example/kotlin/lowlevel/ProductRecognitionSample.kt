// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.example.kotlin.lowlevel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy
import com.zebra.ai.vision.detector.AIVisionSDKException
import com.zebra.ai.vision.detector.AIVisionSDKLicenseException
import com.zebra.ai.vision.detector.BBox
import com.zebra.ai.vision.detector.FeatureExtractor
import com.zebra.ai.vision.detector.InferencerOptions
import com.zebra.ai.vision.detector.Localizer
import com.zebra.ai.vision.detector.Recognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

/**
 * ProductRecognitionSample is a Kotlin class that demonstrates the use of AI models to recognize products
 * within images. It utilizes a localizer to detect potential products and shelves, a feature extractor to
 * generate descriptors, and a recognizer to identify products based on similarity thresholds.
 *
 * @param context The Android context used to access application resources and assets.
 */
class ProductRecognitionSample(private val context: Context) {

    // Tag used for logging purposes
    private val TAG = "ProductRecognitionSample"

    // Instances of AI models used for product recognition
    private var localizer: Localizer? = null
    private var featureExtractor: FeatureExtractor? = null
    private var recognizer: Recognizer? = null

    // Executor service for handling asynchronous operations
    private val executor = Executors.newSingleThreadExecutor()

    // Flags indicating whether each model has been initialized
    private var localizerInitialized = false
    private var featureExtractorInitialized = false
    private var recognizerInitialized = false

    // Similarity threshold for product recognition
    private val SIMILARITY_THRESHOLD = 0.65f

    // Arrays to hold detected bounding boxes and products
    private var detections: Array<BBox>? = null
    private var products: Array<BBox>? = null
    private val mavenModelName = "product-and-shelf-recognizer"

    /**
     * Initializes the ProductRecognitionSample by setting up the localizer, feature extractor,
     * and recognizer models.
     */
    init {
        initializeProductRecognition()
    }

    /**
     * Configures and initializes the AI models required for product recognition. Sets up the localizer,
     * feature extractor, and recognizer with appropriate settings and loads index files from the application's assets.
     */
    private fun initializeProductRecognition() {
        try {
            var mStart = System.currentTimeMillis()

            // Initialize Localizer settings
            val locSettings = Localizer.Settings(mavenModelName)
            Log.d(TAG, "Shelf Localizer.settings() obj creation time = ${System.currentTimeMillis() - mStart} milli sec")

            mStart = System.currentTimeMillis()

            // Initialize FeatureExtractor settings
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

            // Copy index and label files from assets
            val indexFilename = "product.index"
            val labelsFilename = "product.txt"
            val toPath = "${context.filesDir}/"
            copyFromAssets(indexFilename, toPath)
            copyFromAssets(labelsFilename, toPath)

            mStart = System.currentTimeMillis()

            // Initialize Recognizer settings
            val reSettings = Recognizer.SettingsIndex()
            Log.d(TAG, "Recognizer.SettingsIndex() obj creation time = ${System.currentTimeMillis() - mStart} milli sec")

            reSettings.indexFilename = "$toPath$indexFilename"
            reSettings.labelFilename = "$toPath$labelsFilename"

            // Initialize Localizer asynchronously
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

            // Initialize FeatureExtractor asynchronously
            val mStartFeatureExtractor = System.currentTimeMillis()
            CoroutineScope(executor.asCoroutineDispatcher()).launch {
                try {
                    featureExtractor = FeatureExtractor.getFeatureExtractor(feSettings, executor).await()
                    featureExtractorInitialized = true
                    setupAnalyzerIfReady()
                    Log.d(TAG, "FeatureExtractor() obj creation time = ${System.currentTimeMillis() - mStartFeatureExtractor} milli sec")
                } catch (e: AIVisionSDKLicenseException) {
                    Log.e(TAG, "AIVisionSDKLicenseException: Feature Extractor object creation failed, ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Fatal error: decoder creation failed - ${e.message}")
                }
            }

            // Initialize Recognizer asynchronously
            val mStartRecognizer = System.currentTimeMillis()
            CoroutineScope(executor.asCoroutineDispatcher()).launch {
                try {
                    recognizer = Recognizer.getRecognizer(reSettings, executor).await()
                    recognizerInitialized = true
                    setupAnalyzerIfReady()
                    Log.d(TAG, "Recognizer(reSettings) obj creation time = ${System.currentTimeMillis() - mStartRecognizer} milli sec")
                } catch (e: AIVisionSDKLicenseException) {
                    Log.e(TAG, "AIVisionSDKLicenseException: Recognizer object creation failed, ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Fatal error: recognizer creation failed - ${e.message}")
                }
            }

        } catch (e: Exception) {
            Log.e("Test", "Fatal error: load failed - ${e.message}")
        }
    }

    /**
     * Sets up the analyzer if all models are initialized successfully.
     */
    private fun setupAnalyzerIfReady() {
        if (localizerInitialized && featureExtractorInitialized && recognizerInitialized) {
            Log.d(TAG, "Models are loaded successfully")
        }
    }

    /**
     * Copies files from the application's assets to a specified local path.
     *
     * @param filename The name of the file to copy
     * @param toPath The destination path for the copied file
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
     * Processes an image using the initialized AI models to detect and recognize products.
     * The image is first analyzed for bounding boxes, then descriptors are generated for products,
     * and finally, recognitions are found based on these descriptors.
     *
     * @param image The image to be processed, provided as an ImageProxy
     */
    private fun processImage(image: ImageProxy) {
        try {
            Log.d(TAG, "Starting image analysis")
            val bitmap: Bitmap = image.toBitmap()
            val futureResultBBox = localizer?.detect(bitmap, executor)
            futureResultBBox?.thenCompose { bBoxes ->
                detections = bBoxes
                //Detected objects are categorized using class IDs:
                // - Class ID 1 corresponds to products.
                products = bBoxes?.filter { it.cls == 1 }?.toTypedArray()
                Log.e(TAG, "Products size = ${products?.size ?: 0}")
                // Generate descriptors for detected products
                if (detections != null && detections!!.isNotEmpty()) {
                    featureExtractor?.generateDescriptors(products!!, bitmap, executor)
                } else {
                    CompletableFuture.completedFuture(null)
                }
            }?.thenCompose { descriptor ->
                // Find recognitions based on generated descriptors
                if (descriptor != null && detections!!.isNotEmpty()) {
                    recognizer?.findRecognitions(descriptor, executor)
                } else {
                    CompletableFuture.completedFuture(null)
                }
            }?.thenAccept { recognitions ->
                recognitions?.let {
                    Log.e(TAG, "Products recognitions length" + recognitions.size)
                    //Detected objects are categorized using class IDs:
                    // - Class ID 2 corresponds to label shelf.
                    // - Class ID 3 corresponds to label peg.
                    // - Class ID 4 corresponds to Shelf.
                    val labelShelfObjects = detections?.filter { it.cls == 2 }
                    val labelPegObjects = detections?.filter { it.cls == 3 }
                    val shelfObjects = detections?.filter { it.cls == 4 }
                    if (recognitions.size > 0) {
                        for (i in products!!.indices) {
                            if (recognitions[i].similarity[0] > SIMILARITY_THRESHOLD) {
                                val bBox: BBox? = products!![i]
                                Log.d(TAG, "product id: " + recognitions[i].sku[0])
                            }
                        }
                    }
                    image.close()
                }
            }?.exceptionally { ex ->
                Log.e(TAG, "Error during image processing: ${ex.message}")
                image.close()
                null
            }
        } catch (ex: AIVisionSDKException) {
            image.close()
            Log.e(TAG, "Error during image processing: ${ex.message}")
        }
    }
}
