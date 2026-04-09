// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.detectors.productrecognition

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.zebra.ai.vision.detector.ImageData
import com.zebra.ai.vision.detector.ModuleRecognizer
import com.zebra.ai.vision.entity.Entity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ProductRecognitionAnalyzer(
    private val callback: DetectionCallback,
    private val moduleRecognizer: ModuleRecognizer?
) : ImageAnalysis.Analyzer {

    interface DetectionCallback {
        fun onRecognitionResult(result: List<Entity>?)
        fun onCaptureRecognitionResult(result: List<Entity>?)
    }

    private val TAG = "ProductRecognitionAnalyzer"
    private var isAnalyzing = true
    private var isStopped = false
    private var job = Job()
    private var scope = CoroutineScope(Dispatchers.IO + job)

    override fun analyze(image: ImageProxy) {
        if (moduleRecognizer == null) {
            Log.d(TAG, "moduleRecognizer is null")
            image.close()
            return
        }
        if (!isAnalyzing || isStopped) {
            image.close()
            return
        }
        isAnalyzing = false

        scope.launch {
            try {
                val imageData = ImageData.fromImageProxy(image)
                val start = System.currentTimeMillis()
                moduleRecognizer.process(imageData).thenAccept { entityList ->
                    val end = System.currentTimeMillis()
                    Log.d(TAG, "Inference Time: ${end - start} ms")
                    if (!isStopped) {
                        callback.onRecognitionResult(entityList)
                    }
                    image.close()
                    isAnalyzing = true
                }.exceptionally { ex ->
                    Log.e(TAG, "Error in product recognition: ${ex.message}", ex)
                    image.close()
                    isAnalyzing = true
                    null
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Exception during analyze: ${ex.message}", ex)
                image.close()
                isAnalyzing = true
            }
        }
    }

    /**
     * Processes a captured image using the provided capture recognizer and delivers
     * results via the onCaptureRecognitionResult callback.
     *
     * @param image The ImageProxy containing the captured image data to process.
     * @param captureRecognizer The ModuleRecognizer instance to use for capture processing.
     */
    fun processImage(image: ImageProxy, captureRecognizer: ModuleRecognizer) {
        val captureScope = CoroutineScope(Dispatchers.IO + Job())
        captureScope.launch {
            try {
                Log.d(TAG, "Starting image capture recognition analysis")
                val result = suspendCancellableCoroutine<List<Entity>> { cont ->
                    try {
                        val imageData = ImageData.fromImageProxy(image)
                        captureRecognizer.process(imageData)
                            .thenAccept { entityList ->
                                cont.resume(entityList)
                            }
                            .exceptionally { ex ->
                                cont.resumeWithException(ex)
                                null
                            }
                    } catch (e: Exception) {
                        cont.resumeWithException(e)
                    }
                }
                withContext(Dispatchers.Main) {
                    callback.onCaptureRecognitionResult(result)
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Error in capture recognition processing: ${ex.message}", ex)
            } finally {
                image.close()
                isAnalyzing = true
            }
        }
    }

    /**
     * Stops the analysis process and cancels the coroutine job.
     */
    fun stopAnalyzing() {
        isStopped = true
        job.cancel()
    }

    /**
     * Starts or restarts the analysis process. This method resets the stopped state
     * and creates a new coroutine scope for processing.
     */
    fun startAnalyzing() {
        Log.d(TAG, "startAnalyzing() called.")
        isStopped = false
        job = Job()
        scope = CoroutineScope(Dispatchers.IO + job)
    }

}