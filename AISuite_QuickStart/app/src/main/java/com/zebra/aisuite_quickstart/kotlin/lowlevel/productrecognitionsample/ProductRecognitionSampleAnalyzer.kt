// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.lowlevel.productrecognitionsample


import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.zebra.ai.vision.detector.AIVisionSDKException
import com.zebra.ai.vision.detector.BBox
import com.zebra.ai.vision.detector.FeatureExtractor
import com.zebra.ai.vision.detector.Localizer
import com.zebra.ai.vision.detector.Recognizer
import com.zebra.aisuite_quickstart.utils.CommonUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * The ProductRecognitionAnalyzer class implements the ImageAnalysis.Analyzer interface and is
 * responsible for analyzing image frames to detect and recognize products. It integrates multiple
 * components including a Localizer for object detection, a FeatureExtractor for generating
 * descriptors, and a Recognizer for matching and identifying products.

 * This class is designed to be used within an Android application as part of a camera-based
 * product recognition solution. It processes image data asynchronously and returns recognition
 * results through a callback interface.

 * Usage:
 * - Instantiate the ProductRecognitionAnalyzer with the appropriate callback, localizer, feature extractor, and recognizer.
 * - Implement the DetectionCallback interface to handle recognition results.
 * - The analyze(ImageProxy) method is called by the camera framework to process image frames.
 * - Call stop() to terminate ongoing analysis and release resources.

 * Dependencies:
 * - Android ImageProxy: Provides access to image data from the camera.
 * - CoroutineScope: Used for asynchronous task execution, leveraging Kotlin coroutines.
 * - Localizer: Detects objects within an image.
 * - FeatureExtractor: Generates feature descriptors for detected objects.
 * - Recognizer: Matches feature descriptors to known products.

 * Concurrency:
 * - Uses a CoroutineScope to ensure that image analysis tasks are processed asynchronously.
 * - Manages concurrency with flags to control analysis state and prevent re-entry.

 * Note: Ensure that the appropriate permissions and dependencies are configured
 * in the AndroidManifest and build files to utilize camera and image processing capabilities.
 */
class ProductRecognitionSampleAnalyzer(
    private val callback: SampleDetectionCallback,
    private val localizer: Localizer?,
    private val featureExtractor: FeatureExtractor?,
    private val recognizer: Recognizer?
) : ImageAnalysis.Analyzer {

    /**
     * Interface for handling the results of the product recognition process.
     * Implement this interface to define how recognition results are processed.
     */
    interface SampleDetectionCallback {
        fun onDetectionRecognitionResult(
            detections: Array<BBox>,
            products: Array<BBox>,
            recognitions: Array<Recognizer.Recognition>
        )
    }

    private val TAG = "ProductRecognitionSampleAnalyzer"
    private var isAnalyzing = true
    private var detections: Array<BBox>? = null
    private var products: Array<BBox>? = null

    private val job = Job()
    private var isStopped = false
    private val executorService: ExecutorService = Executors.newFixedThreadPool(3)
    private val scope = CoroutineScope(Dispatchers.IO + job)

    /**
     * Analyzes the given image to perform product recognition. This method is called by the camera
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
                val bitmap: Bitmap = CommonUtils.rotateBitmapIfNeeded(image)
                val futureResultBBox = localizer!!.detect(bitmap, executorService)
                futureResultBBox?.thenCompose { bBoxes ->
                    detections = bBoxes
                    products = bBoxes?.filter { it.cls == 1 }?.toTypedArray()
                    Log.d(TAG, "Products size = ${products?.size ?: 0}")

                    if (detections != null && detections!!.isNotEmpty()) {
                        featureExtractor?.generateDescriptors(products!!, bitmap, executorService)
                    } else {
                        CompletableFuture.completedFuture(null)
                    }
                }?.thenCompose { descriptor ->
                    if (descriptor != null && detections!!.isNotEmpty()) {
                        recognizer?.findRecognitions(descriptor, executorService)
                    } else {
                        CompletableFuture.completedFuture(null)
                    }
                }?.thenAccept { recognitions ->
                    recognitions?.let {
                        Log.d(TAG, "Products recognitions length" + recognitions.size)
                        if (!isStopped) callback.onDetectionRecognitionResult(
                            detections!!,
                            products!!,
                            it
                        )
                    }
                    isAnalyzing = true
                    image.close()
                }?.exceptionally { ex ->
                    isAnalyzing = true
                    image.close()
                    Log.d(TAG, "Exception occurred: ${ex.message}")
                    null
                }
            } catch (ex: AIVisionSDKException) {
                isAnalyzing = true
                image.close()
                Log.e(TAG, "Error during image processing: ${ex.message}")
            }
        }

    }

    /**
     * Stops the analysis process and cancels the coroutine job. This method should be
     * called to release resources and halt image analysis when it is no longer required.
     */
    fun stopAnalyzing() {
        isStopped = true
        job.cancel()
        executorService.shutdownNow()
    }
}

