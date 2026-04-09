package com.zebra.aisuite_quickstart.kotlin.detectors.warehouselocalizer

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.zebra.ai.vision.detector.AIVisionSDKException
import com.zebra.ai.vision.detector.ImageData
import com.zebra.ai.vision.detector.Localizer
import com.zebra.ai.vision.entity.LocalizerEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class WareHouseAnalyzer(
    private val callback: DetectionCallback,
    private val localizer: Localizer?
) : ImageAnalysis.Analyzer {

    /**
     * Interface for handling the results of the barcode detection process.
     * Implement this interface to define how detection results are processed.
     */
    interface DetectionCallback {
        fun onLocalizerDetectionResult(result: List<LocalizerEntity>)
        fun onCaptureWareHouseDetectionResult(entities: List<LocalizerEntity>)
    }

    private val TAG = "WareHouseAnalyzer"
    private var isAnalyzing = true
    private var job = Job()

    private var isStopped = false

    // Create a CoroutineScope with the IO dispatcher and the Job
    private var scope = CoroutineScope(Dispatchers.IO + job)
    /**
     * Analyzes the given image to detect pallets. This method is called by the camera
     * framework to process image frames asynchronously.
     *
     * @param image The image frame to analyze.
     */
    override fun analyze(image: ImageProxy) {
        if (!isAnalyzing || isStopped) {
            image.close()
            return
        }
        isAnalyzing = false // Prevent re-entry

        scope.launch {
            try {
                Log.d(TAG, "Starting image analysis")
                val result = processImageAsync(image)
                withContext(Dispatchers.Main) {
                    if (!isStopped) callback.onLocalizerDetectionResult(result)
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Error during image processing: ${ex.message}")
            } finally {
                isAnalyzing = true
                image.close() // Ensure image is closed
            }
        }
    }


    /**
     * Processes the image asynchronously using the BarcodeDecoder.
     *
     * @param image The ImageProxy containing the image data to process.
     * @return A list of BarcodeEntity objects representing the detected barcodes.
     */
    private suspend fun processImageAsync(image: ImageProxy): List<LocalizerEntity> {
        return suspendCancellableCoroutine { cont ->
            try {
                localizer?.process(ImageData.fromImageProxy(image))
                    ?.thenAccept { result ->
                        cont.resume(result) // Resume the coroutine with the result
                    }
                    ?.exceptionally { ex ->
                        cont.resumeWithException(ex) // Resume with exception
                        null
                    }
            } catch (e: AIVisionSDKException) {
                cont.resumeWithException(e)
            }
        }
    }

    /**
     * Processes a captured image using the provided capture localizer and delivers
     * results via the onCaptureWareHouseDetectionResult callback.
     *
     * @param image The ImageProxy containing the captured image data to process.
     * @param captureLocalizer The Localizer instance to use for capture processing.
     */
    fun processImage(image: ImageProxy, captureLocalizer: Localizer) {
        val captureScope = CoroutineScope(Dispatchers.IO + Job())
        captureScope.launch {
            try {
                Log.d(TAG, "Starting image capture analysis")
                val result = suspendCancellableCoroutine<List<LocalizerEntity>> { cont ->
                    try {
                        captureLocalizer.process(ImageData.fromImageProxy(image))
                            .thenAccept { result ->
                                cont.resume(result)
                            }
                            .exceptionally { ex ->
                                cont.resumeWithException(ex)
                                null
                            }
                    } catch (e: AIVisionSDKException) {
                        cont.resumeWithException(e)
                    }
                }
                withContext(Dispatchers.Main) {
                    callback.onCaptureWareHouseDetectionResult(result)
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Error in capture image processing: ${ex.message}")
            } finally {
                image.close()
                isAnalyzing = true
            }
        }
    }

    /**
     * Stops the analysis process and cancels the coroutine job. This method should be
     * called to release resources and halt image analysis when it is no longer required.
     */
    fun stop() {
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