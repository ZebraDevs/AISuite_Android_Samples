// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.analyzers.tracker

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.core.content.ContextCompat
import com.zebra.ai.vision.analyzer.tracking.EntityTrackerAnalyzer
import com.zebra.ai.vision.detector.AIVisionSDKLicenseException
import com.zebra.ai.vision.detector.BarcodeDecoder
import com.zebra.ai.vision.detector.Detector
import com.zebra.ai.vision.detector.InferencerOptions
import com.zebra.ai.vision.detector.ModuleRecognizer
import com.zebra.ai.vision.detector.TextOCR
import com.zebra.ai.vision.entity.Entity
import com.zebra.aisuite_quickstart.filtertracker.FilterDialog
import java.io.BufferedOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Tracker(
    private val context: Context,
    private val callback: DetectionCallback,
    private val imageAnalysis: ImageAnalysis,
    private val selectedFilterItems: MutableList<String?>
) {

    interface DetectionCallback {
        fun handleEntities(result: EntityTrackerAnalyzer.Result)
    }

    private val TAG = "Tracker"
    private var barcodeDecoder: BarcodeDecoder? = null
    private var textOCR: TextOCR? = null
    private var moduleRecognizer: ModuleRecognizer? = null
    private val executor: ExecutorService = Executors.newFixedThreadPool(3)
    private var entityTrackerAnalyzer: EntityTrackerAnalyzer? = null
    private val mavenModelName = "barcode-localizer"
    private val mavenOCRModelName = "text-ocr-recognizer"
    private val mavenProductModelName = "product-and-shelf-recognizer"

    private val analyzerList: MutableList<Detector<out MutableList<out Entity?>?>?> = ArrayList()

    init {
        if (!selectedFilterItems.isEmpty()) {
            for (item in selectedFilterItems) {
                if (item.equals(FilterDialog.BARCODE_TRACKER, ignoreCase = true)) {
                    initializeBarcodeDecoder()
                } else if (item.equals(FilterDialog.OCR_TRACKER, ignoreCase = true)) {
                    initializeTextOCR()
                } else if (item.equals(FilterDialog.PRODUCT_AND_SHELF, ignoreCase = true)) {
                    initializeProductRecognition()
                }
            }
        } else {
            Log.d(TAG, "None of the filter selected")
        }
    }

    private fun getProcessorOrder(): Array<Int> =
        arrayOf(InferencerOptions.DSP, InferencerOptions.CPU, InferencerOptions.GPU)

    private fun configureInferencerOptions(settings: Any) {
        val rpo = getProcessorOrder()
        when (settings) {
            is BarcodeDecoder.Settings -> {
                settings.Symbology.CODE39.enable(true)
                settings.Symbology.CODE128.enable(true)
                settings.detectorSetting.inferencerOptions.runtimeProcessorOrder = rpo
                settings.detectorSetting.inferencerOptions.defaultDims.height = 640
                settings.detectorSetting.inferencerOptions.defaultDims.width = 640
            }
            is TextOCR.Settings -> {
                settings.detectionInferencerOptions.runtimeProcessorOrder = rpo
                settings.recognitionInferencerOptions.runtimeProcessorOrder = rpo
                settings.detectionInferencerOptions.defaultDims.height = 640
                settings.detectionInferencerOptions.defaultDims.width = 640
            }
        }
    }

    fun initializeBarcodeDecoder() {
        try {
            val settings = BarcodeDecoder.Settings(mavenModelName)
            configureInferencerOptions(settings)
            val startTime = System.currentTimeMillis()
            BarcodeDecoder.getBarcodeDecoder(settings, executor).thenAccept { decoder ->
                barcodeDecoder = decoder
                createAnalyzer(listOfNotNull(barcodeDecoder))
                Log.d(TAG, "BarcodeDecoder creation time: ${System.currentTimeMillis() - startTime}ms")
            }.exceptionally { handleException(it) }
        } catch (ex: Exception) {
            Log.e(TAG, "BarcodeDecoder initialization failed: ${ex.message}")
        }
    }

    private fun initializeTextOCR() {
        try {
            val settings = TextOCR.Settings(mavenOCRModelName)
            configureInferencerOptions(settings)
            val startTime = System.currentTimeMillis()
            TextOCR.getTextOCR(settings, executor).thenAccept { ocr ->
                textOCR = ocr
                createAnalyzer(listOfNotNull(textOCR))
                Log.d(TAG, "TextOCR creation time: ${System.currentTimeMillis() - startTime}ms")
            }.exceptionally { handleException(it) }
        } catch (e: Exception) {
            Log.e(TAG, "TextOCR initialization failed: ${e.message}")
        }
    }


    fun initializeProductRecognition() {
        Log.e(TAG, "Initializing ModuleRecognizer for EntityTrackerAnalyzer")
        val iFilename = "product.index"
        val lFilename = "product.txt"
        val toPath = context.filesDir.absolutePath + "/"
        copyFromAssets(iFilename, toPath)
        copyFromAssets(lFilename, toPath)
        val indexFilename = toPath + iFilename
        val labelsFilename = toPath + lFilename
        val settings = try {
            ModuleRecognizer.Settings(mavenProductModelName)
        } catch (e: Exception) {
            Log.e(TAG, "Error ${e.message}")
            return
        }
        val rpo = arrayOf(
            InferencerOptions.DSP,
            InferencerOptions.CPU,
            InferencerOptions.GPU
        )
        settings.inferencerOptions.runtimeProcessorOrder = rpo
        settings.inferencerOptions.defaultDims.height = 640
        settings.inferencerOptions.defaultDims.width = 640

        settings.enableProductRecognitionWithIndex(
            mavenProductModelName,
            indexFilename,
            labelsFilename
        )
        val startTime = System.currentTimeMillis()
        ModuleRecognizer.getModuleRecognizer(settings, executor)
            .thenAccept { recognizerInstance ->
                Log.e(TAG, "ModuleRecognizer instance created")
                moduleRecognizer = recognizerInstance
                createAnalyzer(listOfNotNull(moduleRecognizer))
                Log.d(TAG, "Product Recognition creation time: ${System.currentTimeMillis() - startTime}ms")
            }
            .exceptionally {
                Log.e(TAG, "Failed to create ModuleRecognizer: ${it.message}")
                null
            }
    }

    private fun copyFromAssets(filename: String, toPath: String) {
        val bufferSize = 8192
        try {
            context.assets.open(filename).use { stream ->
                Files.newOutputStream(Paths.get(toPath + filename)).use { fos ->
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
            Log.e(TAG, "Error in copy from assets: ${e.message}")
        }
    }

    fun getModuleRecognizer(): ModuleRecognizer? = moduleRecognizer
    fun getBarcodeDecoder(): BarcodeDecoder? = barcodeDecoder
    fun getTextOCR(): TextOCR? = textOCR

    private fun handleEntities(result: EntityTrackerAnalyzer.Result) {
        callback.handleEntities(result)
    }
    @Synchronized
    private fun createAnalyzer(analyzers: List<Detector<out MutableList<out Entity>>>) {
        analyzerList.add(analyzers[0])
        if(analyzerList.size == selectedFilterItems.size) {
            entityTrackerAnalyzer = EntityTrackerAnalyzer(
                analyzerList,
                ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
                executor,
                ::handleEntities
            )
            imageAnalysis.setAnalyzer(
                ContextCompat.getMainExecutor(context),
                entityTrackerAnalyzer!!
            )
        }
    }

    private fun handleException(e: Throwable): Void? {
        val message = if (e is AIVisionSDKLicenseException)
            "License error: ${e.message}"
        else
            "Fatal error: ${e.message}"
        Log.e(TAG, message)
        return null
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

    fun stopAnalyzing() {
        executor.shutdownNow()
    }
}