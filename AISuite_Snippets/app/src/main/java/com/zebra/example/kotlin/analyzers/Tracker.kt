// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.example.kotlin.analyzers

import android.content.Context
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageAnalysis
import com.zebra.ai.vision.analyzer.tracking.EntityTrackerAnalyzer
import com.zebra.ai.vision.detector.AIVisionSDKLicenseException
import com.zebra.ai.vision.detector.BarcodeDecoder
import com.zebra.ai.vision.detector.Detector
import com.zebra.ai.vision.detector.InferencerOptions
import com.zebra.ai.vision.detector.ModuleRecognizer
import com.zebra.ai.vision.detector.TextOCR
import com.zebra.ai.vision.entity.BarcodeEntity
import com.zebra.ai.vision.entity.Entity
import com.zebra.ai.vision.entity.LabelEntity
import com.zebra.ai.vision.entity.ParagraphEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Tracker is a class that facilitates the detection and tracking of barcodes
 * using a barcode decoder as a detector, tracking of texts using text OCR as detector, tracking of shelf and products using module recognizer as detector and an entity tracker analyzer. It initializes the necessary
 * components and processes image data to identify respective things.
 */
class Tracker(private val context: Context) {

    // Tag used for logging purposes
    private val TAG = "Tracker"

    // Instance of BarcodeDecoder used for decoding barcodes
    private var barcodeDecoder: BarcodeDecoder? = null
    private var textOCR: TextOCR? = null
    private var moduleRecognizer: ModuleRecognizer? = null

    // Instance of EntityTrackerAnalyzer used for analyzing tracked entities
    private var entityTrackerAnalyzer: EntityTrackerAnalyzer? = null

    // Executor service for handling asynchronous operations
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mavenModelName = "barcode-localizer"
    private val mavenOCRModelName = "text-ocr-recognizer"
    private val mavenProductModelName = "product-and-shelf-recognizer"

    /**
     * Initializes the Tracker by setting up the barcode decoder.
     */
    init {
        // We can pass multiple detectors as a list to the entity tracker analyzer
        initializeBarcodeDecoder() // if we need to pass only barcode decoder as a detector
        //    initializeTextOCR() // if we need to pass only text OCR as a detector
        //   initializeModuleRecognizer() // if we need to pass only module recognizer as a detector
    }

    /**
     * Configures and initializes the barcode decoder with specific settings.
     * The decoder is set up to detect specific symbologies and dimensions.
     */
    private fun initializeBarcodeDecoder() {
        try {
            // Create settings for the barcode decoder
            val decoderSettings = BarcodeDecoder.Settings(mavenModelName)

            // Define runtime processor order
            val rpo = arrayOf(
                InferencerOptions.DSP,
                InferencerOptions.CPU,
                InferencerOptions.GPU
            )

            // Enable specific symbologies for barcode decoding
            decoderSettings.Symbology.CODE39.enable(true)
            decoderSettings.Symbology.CODE128.enable(true)

            // Set inferencer options
            decoderSettings.detectorSetting.inferencerOptions.runtimeProcessorOrder = rpo
            decoderSettings.detectorSetting.inferencerOptions.defaultDims.height = 640
            decoderSettings.detectorSetting.inferencerOptions.defaultDims.width = 640

            // Record the start time for profiling
            val mStart = System.currentTimeMillis()

            // Asynchronously get the barcode decoder and handle the result or any exceptions
            BarcodeDecoder.getBarcodeDecoder(decoderSettings, executor)
                .thenAccept { decoderInstance: BarcodeDecoder? ->
                    barcodeDecoder = decoderInstance
                    initializeTracker(listOfNotNull(barcodeDecoder))
                    Log.d(
                        TAG,
                        "BarcodeDecoder() obj creation time = " + (System.currentTimeMillis() - mStart) + " milli sec"
                    )
                }.exceptionally { e: Throwable? ->
                    Log.e(TAG, "Fatal error: decoder creation failed - " + e?.message)
                    null
                }
        } catch (e: AIVisionSDKLicenseException) {
            Log.e(TAG, "AIVisionSDKLicenseException: Barcode Decoder object creation failed, ${e.message}")
        } catch (ex: Exception) {
            Log.e(TAG, "Model Loading: Barcode decoder returned with exception " + ex.message)
        }
    }

    /**
     * Initializes the TextOCR with predefined settings for text detection and recognition.
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
                Log.d(TAG, "TextOCR() obj creation / model loading time = ${System.currentTimeMillis() - startTime} milli sec")
                initializeTracker(listOfNotNull(textOCR))
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
     * Initialize ModuleRecognizer with product recognition enabled
     */
    private fun initializeModuleRecognizer() {
        CoroutineScope(executor.asCoroutineDispatcher()).launch {
            try {
                Log.i(TAG, "Initializing ModuleRecognizer")

                // Copy assets
                val indexFilename = "product.index"
                val labelsFilename = "product.txt"
                val toPath = "${context.filesDir}/"
                copyFromAssets(indexFilename, toPath)
                copyFromAssets(labelsFilename, toPath)

                // Create settings with base model (localization)
                val settings = ModuleRecognizer.Settings(mavenProductModelName)

                // Configure InferencerOptions
                val rpo = arrayOf(
                    InferencerOptions.DSP,
                    InferencerOptions.CPU,
                    InferencerOptions.GPU
                )
                settings.inferencerOptions.runtimeProcessorOrder = rpo
                settings.inferencerOptions.defaultDims.height = 640
                settings.inferencerOptions.defaultDims.width = 640

                // Enable product recognition with the same model and recognition data
                settings.enableProductRecognitionWithIndex(
                    mavenProductModelName,
                    "$toPath$indexFilename",
                    "$toPath$labelsFilename"
                )

                // Initialize ModuleRecognizer
                val startTime = System.currentTimeMillis()
                moduleRecognizer = ModuleRecognizer.getModuleRecognizer(settings, executor).await()
                val creationTime = System.currentTimeMillis() - startTime

                Log.d(TAG, "ModuleRecognizer Creation Time: ${creationTime}ms")
                Log.i(TAG, "ModuleRecognizer instance created successfully")
                initializeTracker(listOfNotNull(moduleRecognizer))

            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize ModuleRecognizer: ${e.message}")
                e.printStackTrace()
            }
        }
    }

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
            Log.e(TAG, "Error copying from assets: ${e.message}")
        }
    }

    /**
     * Initializes the entity tracker analyzer after the barcode decoder model is loaded.
     * This analyzer is responsible for tracking detected barcodes in the image data.
     */
    private fun initializeTracker(detectors: List<Detector<out MutableList<out Entity>>>) {
        // Initialize the entity tracker analyzer with the decoded barcodes
        entityTrackerAnalyzer = EntityTrackerAnalyzer(
            detectors,
            ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
            executor,
            ::handleEntities
        )
        // Pass this tracker analyzer to the preview view as your image analyzer
    }

    /**
     * Handles the entities detected by the entity tracker analyzer.
     * Logs details of each detected barcode entity, including its UUID and value.
     *
     * @param result The result containing the detected entities
     */
    private fun handleEntities(result: EntityTrackerAnalyzer.Result) {
        val barcodeEntities = barcodeDecoder?.let { result.getValue(it) }
        val ocrEntities = textOCR?.let { result.getValue(it) }
        val moduleEntities = moduleRecognizer?.let { result.getValue(it) }

        if (barcodeEntities != null) {
            for (entity in barcodeEntities) {
                if (entity is BarcodeEntity) {
                    val rect = entity.boundingBox
                    if (rect != null) {
                        val hashCode = entity.hashCode().toString()
                        Log.d(
                            TAG,
                            "Tracker UUID: $hashCode Tracker Detected entity - Value: ${entity.value}"
                        )
                    }
                }
            }
        }
        ocrEntities?.forEach { entity ->
            if (entity is ParagraphEntity) {
                val lines = entity.lines
                for (line in lines) {
                    for (word in line.words) {
                        val bbox = word.complexBBox
                        Log.d(TAG, "Detected OCR entity - Text: ${word.text}")
                    }
                }
            }

        }
        val shelves = mutableListOf<com.zebra.ai.vision.entity.ShelfEntity>()
        val labels = mutableListOf<LabelEntity>()
        val products = mutableListOf<com.zebra.ai.vision.entity.ProductEntity>()
        moduleEntities?.forEach { entity ->
            when (entity) {
                is com.zebra.ai.vision.entity.ShelfEntity -> shelves.add(entity)
                is LabelEntity -> labels.add(entity)
                is com.zebra.ai.vision.entity.ProductEntity -> products.add(entity)
            }
            for (shelf in shelves) {
                val shelfRect = shelf.boundingBox
            }
            for (label in labels) {
                if (label.classId == LabelEntity.ClassId.PEG_LABEL) {
                    val pegRect: Rect = label.boundingBox

                }
                if (label.classId == LabelEntity.ClassId.SHELF_LABEL) {
                    val labelShelfRect: Rect = label.boundingBox
                }
            }
            for (product in products) {
                val prodRect = product.boundingBox
            }
        }

    }

    fun stop() {
        barcodeDecoder?.let {
            it.dispose()
            Log.d(TAG, "Barcode decoder is disposed")
            barcodeDecoder = null
        }
        textOCR?.let {
            it.dispose()
            Log.d(TAG, "TextOCR is disposed")
            textOCR = null
        }
        moduleRecognizer?.let {
            it.dispose()
            Log.d(TAG, "Module Recognizer is disposed")
            moduleRecognizer = null
        }
    }
}
