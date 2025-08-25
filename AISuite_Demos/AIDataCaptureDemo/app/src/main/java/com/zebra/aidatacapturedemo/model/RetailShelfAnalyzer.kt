// Copyright (c) 2024-2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.aidatacapturedemo.model

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.zebra.ai.vision.detector.AIVisionSDKException
import com.zebra.ai.vision.detector.BBox
import com.zebra.ai.vision.detector.ImageData
import com.zebra.ai.vision.detector.InvalidInputException
import com.zebra.ai.vision.detector.Localizer
import com.zebra.ai.vision.entity.LocalizerEntity
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * [RetailShelfAnalyzer] class is used to detect all the SHELF LABELS, PEG LABELS, PRODUCTS & SHELVES of the Retail Store
 * Shelves found on the Camera Live Preview.
 *
 * @param uiState - Used to read all the UI Current State
 * @param viewModel - Used to write any UI State Changes via [AIDataCaptureDemoViewModel]
 */
class RetailShelfAnalyzer(
    val uiState: StateFlow<AIDataCaptureDemoUiState>,
    val viewModel: AIDataCaptureDemoViewModel
) :
    ImageAnalysis.Analyzer {

    private val TAG = "RetailShelfAnalyzer"
    private var localizer: Localizer? = null
    private val locSettings = Localizer.Settings("product-and-shelf-recognizer")
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
    private var isAnalyzing = true

    /**
     * This function is used to execute the retail shelf localization.
     * Localizer generates boundingboxes for the shelf, shelf labels, peg labels,
     * and product detections.
     */
    override fun analyze(image: ImageProxy) {
        if (localizer == null) {
            Log.e(TAG, "RetailShelfAnalyzer is null")
            image.close()
            return
        }
        if (!isAnalyzing) {
            image.close()
            return
        }

        isAnalyzing = false // Set to false to prevent re-entry

        executorService.submit {
            try {
                Log.d(TAG, "Starting image analysis")
                localizer?.process(ImageData.fromImageProxy(image))
                    ?.thenAccept { result ->
                        updateRetailShelfDetectionResultUsingProcess(result)
                        image.close()
                    }
            } catch (e: InvalidInputException) {
                Log.e(TAG, e.message ?: "InvalidInputException occurred")
                image.close()
            } catch (e: AIVisionSDKException) {
                Log.e(TAG, e.message ?: "AIVisionSDKException occurred")
                image.close()
            } finally {
                isAnalyzing = true
            }
        }
    }

    fun startAnalyzing() {
        isAnalyzing = true
    }

    fun stopAnalyzing() {
        isAnalyzing = false
    }

    fun initialize() {
        try {
            localizer?.dispose()
            localizer = null
            updateModelDemoReady(false)

            configure()

            try {
                Localizer.getLocalizer(locSettings, executorService)
                    .thenAccept { localizerInstance: Localizer ->
                        localizer = localizerInstance
                        updateModelDemoReady(true)
                        Log.i(TAG, "Localizer init Success")
                    }.exceptionally { e: Throwable ->
                        Log.e(TAG, "Localizer init Failed -> " + e.message)
                        null
                    }
            } catch (e: IOException) {
                Log.e(TAG, "Localizer init Failed -> " + e.message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error: load failed - ${e.message}")
        }
    }

    private fun configure() {
        try {

            //Swap the values as the presented index is reverse of what model expects
            val processorOrder = when (uiState.value.retailShelfSettings.commonSettings.processorSelectedIndex) {
                0 -> arrayOf(2)
                1 -> arrayOf(1)
                2 -> arrayOf(0)
                else -> { arrayOf(2) }
            }
            locSettings.inferencerOptions.runtimeProcessorOrder = processorOrder

            locSettings.inferencerOptions.defaultDims.width = uiState.value.retailShelfSettings.commonSettings.inputSizeSelected
            locSettings.inferencerOptions.defaultDims.height = uiState.value.retailShelfSettings.commonSettings.inputSizeSelected
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error: configure failed - ${e.message}")
        }
    }

    fun deinitialize() {
        localizer?.dispose()
        localizer = null
    }

    private fun rotateBitmapIfNeeded(imageProxy: ImageProxy): Bitmap? {
        try {
            val bitmap = imageProxy.toBitmap()
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            return rotateBitmap(bitmap, rotationDegrees)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting image to bitmap: " + e.message)
            return null
        }
    }

    private fun rotateBitmap(bitmap: Bitmap?, degrees: Int): Bitmap? {
        if (degrees == 0 || bitmap == null) return bitmap

        try {
            val matrix = Matrix()
            matrix.postRotate(degrees.toFloat())
            return Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.getWidth(),
                bitmap.getHeight(),
                matrix,
                true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error rotating bitmap: " + e.message)
            return bitmap
        }
    }

    private fun updateRetailShelfDetectionResultUsingProcess(localizerEntities: List<LocalizerEntity>) {
        var bBoxesResult : Array<BBox?> =  arrayOf()
        localizerEntities.forEach { localizerEntity ->
            localizerEntity.boundingBox?.let {
                val bBox = BBox()
                bBox.cls = localizerEntity.classId
                bBox.prob = localizerEntity.accuracy
                bBox.xmin = it.left.toFloat()
                bBox.ymin = it.top.toFloat()
                bBox.xmax = it.right.toFloat()
                bBox.ymax = it.bottom.toFloat()
                bBoxesResult += bBox
            }
        }
        updateRetailShelfDetectionResultUsingDetect(result = bBoxesResult)
    }

    private fun updateRetailShelfDetectionResultUsingDetect(result: Array<BBox?>?) {
        viewModel.updateRetailShelfDetectionResult(results = result)
    }

    private fun updateModelDemoReady(isReady: Boolean) {
        viewModel.updateModelDemoReady(isReady = isReady)
    }
}