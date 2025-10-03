// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.example.kotlin.analyzers

import android.util.Log
import androidx.camera.core.ImageAnalysis
import com.zebra.ai.vision.analyzer.tracking.EntityTrackerAnalyzer
import com.zebra.ai.vision.detector.AIVisionSDKLicenseException
import com.zebra.ai.vision.detector.BarcodeDecoder
import com.zebra.ai.vision.detector.InferencerOptions
import com.zebra.ai.vision.entity.BarcodeEntity
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * BarcodeTracker is a class that facilitates the detection and tracking of barcodes
 * using a barcode decoder and an entity tracker analyzer. It initializes the necessary
 * components and processes image data to identify barcodes.
 */
class BarcodeTracker {

    // Tag used for logging purposes
    private val TAG = "BarcodeTracker"

    // Instance of BarcodeDecoder used for decoding barcodes
    private var barcodeDecoder: BarcodeDecoder? = null

    // Instance of EntityTrackerAnalyzer used for analyzing tracked entities
    private var entityTrackerAnalyzer: EntityTrackerAnalyzer? = null

    // Executor service for handling asynchronous operations
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mavenModelName = "barcode-localizer"

    /**
     * Initializes the BarcodeTracker by setting up the barcode decoder.
     */
    init {
        initializeBarcodeDecoder()
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
                    initializeTracker()
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
     * Initializes the entity tracker analyzer after the barcode decoder model is loaded.
     * This analyzer is responsible for tracking detected barcodes in the image data.
     */
    private fun initializeTracker() {
        // Initialize the entity tracker analyzer with the decoded barcodes
        entityTrackerAnalyzer = EntityTrackerAnalyzer(
            listOf(barcodeDecoder),
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
        val entities = result.getValue(barcodeDecoder!!)

        if (entities != null) {
            for (entity in entities) {
                if (entity is BarcodeEntity) {
                    val bEntity = entity
                    val rect = bEntity.boundingBox
                    if (rect != null) {
                        val hashCode = bEntity.hashCode().toString()
                        Log.d(
                            TAG,
                            "Tracker UUID: $hashCode Tracker Detected entity - Value: ${bEntity.value}"
                        )
                    }
                }
            }
        }
    }
}
