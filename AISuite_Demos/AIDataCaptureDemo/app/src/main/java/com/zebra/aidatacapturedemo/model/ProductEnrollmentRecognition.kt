// Copyright (c) 2024-2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.aidatacapturedemo.model

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.zebra.ai.vision.detector.AIVisionSDKException
import com.zebra.ai.vision.detector.BBox
import com.zebra.ai.vision.detector.FeatureExtractor
import com.zebra.ai.vision.detector.FeatureStorage
import com.zebra.ai.vision.detector.InvalidInputException
import com.zebra.ai.vision.detector.Localizer
import com.zebra.ai.vision.detector.Recognizer
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.data.ProductData
import com.zebra.aidatacapturedemo.data.toProductData
import com.zebra.aidatacapturedemo.model.FileUtils.Companion.databaseFile
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.nio.file.Paths
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.time.TimeSource

/**
 * [ProductEnrollmentRecognition] class is used to perform the product recognition on the Camera Live Preview.
 * It uses the Localizer to detect shelves, labels, peg labels, products which generates
 * boundingboxes for the detections.
 * FeatureExtractor is used to extract features from product detection bounding boxes.
 * Recognizer does a semantic searches to locate matching descriptors, and
 * finds the best fit from feature vectors.
 * FeatureStorage is used save feature descriptors, which is used in conjunction with feature extractor,
 * to enroll new products for product recognition.
 * It  provides the methods to initialize, deinitialize, execute,
 * deleteProductDB, applyProductDB and enrollProductIndex.
 * @param uiState - Used to read all the UI Current State
 * @param viewModel - Used to write any UI State Changes via [AIDataCaptureDemoViewModel]
 * @param cacheDir - App Cache Directory Path required for loading the Product.db to enroll & Recognize Retail Products
 */

class ProductEnrollmentRecognition(
    val uiState: StateFlow<AIDataCaptureDemoUiState>,
    val viewModel: AIDataCaptureDemoViewModel,
    private val cacheDir: String
) : ImageAnalysis.Analyzer {

    private var mIsStopPreviewAnalysisRequested: Boolean = false
    private val TAG = "ProductEnrollmentRecognition"

    private var localizer: Localizer? = null
    private var extractor: FeatureExtractor? = null
    private var featureStorage: FeatureStorage? = null
    private var recognizer: Recognizer? = null
    private val job = Job()
    private val executorService: ExecutorService = Executors.newFixedThreadPool(4)
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var isAnalyzing = true

    /**
     * ProductEnrollmentRecognition workflow takes the input image and runs it through the
     * localizer that generates boundingboxes for the shelf, label, and product detections.
     * We use the product detection boundingboxes, which is passed along with the input image
     * to extract features using the feature extractor. Feature extractor generates feature
     * descriptos which is used to perform semantic search using the recognizer to find
     * the best fitting product from the database.
     * We use only products with greater than 0.8 confidence from product recognition.
     */
    override fun analyze(image: ImageProxy) {
        if (uiState.value.modelDemoReady == false) {
            Log.e(TAG, "ProductEnrollmentRecognition init in progress")
            image.close()
            return
        }
        if (!isAnalyzing || mIsStopPreviewAnalysisRequested) {
            image.close()
            return
        }

        isAnalyzing = false // Set to false to prevent re-entry

        scope.launch {
            try {
                Log.d(TAG, "Starting image analysis")
                val bitmap = rotateBitmapIfNeeded(image)!!
                execute(bitmap)
                image.close()
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

    /**
     * This function is used to initialize the various components that are used to
     * accomplish product recognition, namely localizer, feature extractor and
     * product recognition. feature storage in is used to save new products decriptors.
     */
    fun initialize() {
        deinitialize()
        checkUpdateModelDemoReady(false)
        initializeLocalizer()
        initFeatureStorage()
        initFeatureExtractor()
        initProductRecognition()
    }

    /**
     * To deinitialize the ProductEnrollmentRecognition, we need to dispose the localizer,
     * feature extractor, feature storage and recognizer.
     */
    fun deinitialize() {
        localizer?.dispose()
        localizer = null
        recognizer?.dispose()
        recognizer = null
        featureStorage?.dispose()
        featureStorage = null
        extractor?.dispose()
        extractor = null
    }

    /**
     * ProductRecognition workflow takes the input image and runs it through the
     * localizer that generates boundingboxes for the shelf, label, and product detections.
     * WE use the product detection boundingboxes, which is passed along with the input image
     * to extract features using the feature extractor. Feature extractor generates feature
     * descriptos which is used to perform semantic search using the recognizer to find
     * the best fitting product from the database.
     * We use only products with greater than 0.8 confidence from product recognition.
     */
    fun execute(bitmap: Bitmap) {
        if (bitmap != null) {
            val bboxes = executeRetailShelfLocalization(bitmap)
            if (bboxes != null) {
                executeProductRecognition(bitmap, bboxes)
            }
        }
    }

    fun rotateBitmapIfNeeded(imageProxy: ImageProxy): Bitmap? {
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

    /**
     * To initialize the RetailShelfLocalizer, we need to set the
     * model name, processor type (CPU, GPU, DSP), and
     * dimensions of the input image
     */
    private fun initializeLocalizer() {
        Log.i(TAG, "initializeLocalizer")
        localizer?.dispose()
        localizer = null

        val locSettings = Localizer.Settings("product-and-shelf-recognizer")

        //Swap the values as the presented index is reverse of what model expects
        val processorOrder = when (uiState.value.productRecognitionSettings.commonSettings.processorSelectedIndex) {
            0 -> arrayOf(2)
            1 -> arrayOf(1)
            2 -> arrayOf(0)
            else -> { arrayOf(2) }
        }

        locSettings.inferencerOptions.runtimeProcessorOrder = processorOrder

        locSettings.inferencerOptions.defaultDims.width = uiState.value.productRecognitionSettings.commonSettings.inputSizeSelected
        locSettings.inferencerOptions.defaultDims.height = uiState.value.productRecognitionSettings.commonSettings.inputSizeSelected

        try {
            Localizer.getLocalizer(locSettings, executorService)
                .thenAccept { localizerInstance: Localizer ->
                    localizer = localizerInstance
                    checkUpdateModelDemoReady(true)
                    Log.i(TAG, "Localizer init Success")
                }.exceptionally { e: Throwable ->
                    Log.e(TAG, "Localizer init Failed -> " + e.message)
                    null
                }
        } catch (e: IOException) {
            Log.e(TAG, "Localizer init Failed -> " + e.message)
        }
    }

    /**
     * To initialize the FeatureExtractor, we need to set the
     * model name, processor type (CPU, GPU, DSP)
     */
    private fun initFeatureExtractor() {
        Log.i(TAG, "initFeatureExtractor")
        extractor?.dispose()
        extractor = null

        val extractorSettings =
            FeatureExtractor.Settings("product-and-shelf-recognizer")

        //Swap the values as the presented index is reverse of what model expects
        val processorOrder = when (uiState.value.productRecognitionSettings.commonSettings.processorSelectedIndex) {
            0 -> arrayOf(2)
            1 -> arrayOf(1)
            2 -> arrayOf(0)
            else -> { arrayOf(2) }
        }
        extractorSettings.inferencerOptions.runtimeProcessorOrder = processorOrder

        try {
            FeatureExtractor.getFeatureExtractor(extractorSettings, executorService)
                .thenAccept { extractorInstance: FeatureExtractor ->
                    extractor = extractorInstance
                    checkUpdateModelDemoReady(true)
                    Log.i(TAG, "Feature Extractor init Success")
                }.exceptionally { e: Throwable ->
                    Log.e(TAG, "Feature Extractor init Failed -> " + e.message)
                    null
                }
        } catch (e: IOException) {
            Log.e(TAG, "Feature Extractor Failed -> " + e.message)
        }
    }

    /**
     * To initialize the FeatureStorage, we need to set the
     * database file path where the features are stored and max update N.
     * We could potentially use this database file initialize a FeatureExtractor
     * to enroll more products for recognition
     */
    private fun initFeatureStorage() {
        Log.i(TAG, "initFeatureStorage")

        featureStorage?.dispose()
        featureStorage = null

        val dataBaseFile = Paths.get(cacheDir, databaseFile).toString()
        val featureStorageSettings = FeatureStorage.Settings(dataBaseFile)
        featureStorageSettings.maxUpdateN = 5

        try {
            FeatureStorage.getFeatureStorage(featureStorageSettings, executorService)
                .thenAccept { storageInstance: FeatureStorage ->
                    featureStorage = storageInstance
                    checkUpdateModelDemoReady(true)
                    Log.i(TAG, "Feature Storage init Success")
                }.exceptionally { e: Throwable ->
                    Log.e(TAG, "Feature Storage init Failed -> " + e.message)
                    null
                }
        } catch (e: IOException) {
            Log.e(TAG, "Feature Storage init Failed -> " + e.message)
        } catch (e: RuntimeException) {
            Log.e(TAG, "DB empty -> " + e.message)
        }
    }

    //
    /** To initialize the Recognizer, we need to set the database file path
     *  and index dimensions
     */
    private fun initProductRecognition() {
        Log.i(TAG, "initProductEnrollmentRecognition")
        recognizer?.dispose()
        recognizer = null

        val recognizerSettings = Recognizer.SettingsDb()
        val indexDimensions = 768
        recognizerSettings.dbSource = Paths.get(cacheDir, databaseFile).toString()
        recognizerSettings.indexDimensions = indexDimensions

        try {
            Recognizer.getRecognizer(recognizerSettings, executorService)
                .thenAccept { recognizerInstance: Recognizer ->
                    recognizer = recognizerInstance
                    checkUpdateModelDemoReady(true)
                    Log.i(TAG, "Recognizer init Success")
                }.exceptionally { e: Throwable ->
                    Log.e(TAG, "Recognizer init Failed -> " + e.message)
                    null
                }
        } catch (e: IOException) {
            Log.e(TAG, "Recognizer init Failed -> " + e.message)
        } catch (e: RuntimeException) {
            checkUpdateModelDemoReady(true)
            Log.e(TAG, "DB empty -> " + e.message)
        }
    }

    /**
     * This function is used to delete the product database file. We need to reinitialize
     * FeatureStorage and Recognizer with new empty database file.
     * This will result in no products being recognized.
     */
    fun deleteProductDB() {
        Log.i(TAG, "deleteProductDB")
        FileUtils.deleteProductDBFile()
        initFeatureStorage()
        initProductRecognition()
    }

    /**
     * This function is used to apply the product database file.
     * We need to reinitialize FeatureStorage and Recognizer with the new
     * database file. This will result in the products included in the database
     * being recognized.
     */
    fun applyProductDB() {
        Log.i(TAG, "applyProductDB")
        initFeatureStorage()
        initProductRecognition()
    }

    /**
     * This function is used to enroll the products, using their product decriptors
     * into the database. We need to extract the features from the product detection
     * bounding boxes and add them to the feature storage.
     * We then reinitialize the product recognition to include the new products.
     */
    fun enrollProductIndex(productDataList: List<ProductData>) {
        Log.i(TAG, "enrollProducts")
        if (productDataList.size == 0) {
            return
        }
        Log.i(TAG, "Num Products - ${productDataList.size}")
        for (product in productDataList) {
            if (product.text.isNotEmpty()) {
                val arrayOfDescriptor =
                    extractor?.generateSingleDescriptor(product.crop, executorService)?.get()
                featureStorage!!.addDescriptors(product.text, arrayOfDescriptor, true)
            }
        }
        initProductRecognition()
    }

    /**
     * This function is used to execute the retail shelf localization.
     * Localizer generates boundingboxes for the shelf, shelf labels, peg labels,
     * and product detections.
     */
    private fun executeRetailShelfLocalization(bitmap: Bitmap?): Array<BBox>? {
        Log.i(TAG, "executeRetailShelfLocalization")
        Log.i(TAG, "Image Width = " + bitmap?.width.toString())
        Log.i(TAG, "Image Height = " + bitmap?.height.toString())
        val timeSource = TimeSource.Monotonic
        val mark = timeSource.markNow()
        val result = localizer?.detect(bitmap, executorService)?.get()
        val elapsed = timeSource.markNow() - mark
        updateRetailShelfDetectionResult(result)
        return result
    }

    /**
     * This function is used to execute the product recognition.
     * We use the product detection boundingboxes, which is passed along with the
     * input image to extract features using the feature extractor.
     * Feature extractor generates feature descriptors which is used to perform
     * semantic search using the recognizer to find the best fitting product
     * from the database.
     * We use only products with greater than 0.8 confidence from product recognition.
     */
    private fun executeProductRecognition(bitmap: Bitmap?, bboxes: Array<BBox>) {
        Log.i(TAG, "executeProductRecognition")
        if (recognizer == null) {
            return
        } // empty db

        val timeSource = TimeSource.Monotonic
        val mark = timeSource.markNow()

        val products: Array<BBox> = bboxes.filter { it.cls == 1 }.toTypedArray()
        Log.i(TAG, "Products - ${products.size}")

        val descriptors = extractor?.generateDescriptors(products, bitmap, executorService)?.get()

        val elapsed = timeSource.markNow() - mark
        Log.d(TAG, "Extractor - ${elapsed}")
        descriptors?.let {
            val mark2 = timeSource.markNow()
            val recognitions = recognizer?.findRecognitions(descriptors, executorService)?.get()
            Log.i(TAG, "Recognitions - ${recognitions?.size}")

            recognitions?.let { it1 ->
                toProductData(
                    bitmap!!, products,
                    it1
                )
            }?.let { it2 ->
                updateProductResults(it2)
            }
            val elapsed2 = timeSource.markNow() - mark2
            Log.d(TAG, "Recognizer - ${elapsed2}")
        }
    }

    private fun checkUpdateModelDemoReady(ready: Boolean) {
        if (ready == false) {
            viewModel.updateModelDemoReady(isReady = false)
        } else {
            if (localizer != null && featureStorage != null && extractor != null) {
                viewModel.updateModelDemoReady(isReady = ready)
            }
        }
    }

    fun updateProductResults(results: MutableList<ProductData>) {
        viewModel.updateProductRecognitionResult(results = results)
    }

    fun updateCaptureBitmap(bitmap: Bitmap) {
        viewModel.updateCaptureBitmap(bitmap)
    }

    private fun updateRetailShelfDetectionResult(result: Array<BBox?>?) {
        viewModel.updateRetailShelfDetectionResult(results = result)
    }

    fun stopPreviewAnalysis() {
        mIsStopPreviewAnalysisRequested = true
    }

    fun executeHighRes(highResBitmap: Bitmap) {
        while (!isAnalyzing) {
        }
        execute(bitmap = highResBitmap)
    }

    fun startPreviewAnalysis() {
        mIsStopPreviewAnalysisRequested = false
    }
}
