// Copyright (c) 2024-2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.aidatacapturedemo.model

import android.util.Log
import com.zebra.ai.vision.detector.BBox
import com.zebra.ai.vision.detector.BarcodeDecoder
import com.zebra.ai.vision.detector.EntityType
import com.zebra.ai.vision.detector.ModuleRecognizer
import com.zebra.ai.vision.entity.LocalizerEntity
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.model.FileUtils.Companion.databaseFile
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.nio.file.Paths
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
    val viewModel: AIDataCaptureDemoViewModel,
    private val cacheDir: String
) {

    private val TAG = "RetailShelfAnalyzer"
    private var moduleRecognizer: ModuleRecognizer? = null
    private val mavenBarcodeModelName = "barcode-localizer"
    private val mavenOCRModelName = "text-ocr-recognizer"
    private val mavenProductModelName = "product-and-shelf-recognizer"
    private val moduleRecognizerSettings = ModuleRecognizer.Settings(mavenProductModelName)
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()

    /**
     * initialize function is used to initialize the ModuleRecognizer for Retail Shelf Analyzer
     * use case. It configures the model settings based on the current UI state and creates an
     * instance of ModuleRecognizer. If the initialization fails due to unsupported inference type
     * or missing product data, it updates the UI with appropriate messages and
     * takes corrective actions.
     */
    fun initialize() {
        Log.e(TAG, "Initializing ModuleRecognizer for EntityTrackerAnalyzer")

        moduleRecognizer?.dispose()
        moduleRecognizer = null
        updateRetailShelfModelDemoReady(false)

        configure()

        val startTime = System.currentTimeMillis()
        try {
            ModuleRecognizer.getModuleRecognizer(moduleRecognizerSettings, executorService)
                .thenAccept { recognizerInstance ->
                    Log.e(TAG, "ModuleRecognizer instance created")
                    moduleRecognizer = recognizerInstance
                    updateRetailShelfModelDemoReady(true)
                    Log.d(TAG, "Product Recognition creation time: ${System.currentTimeMillis() - startTime} ms")
                }.exceptionally {  e: Throwable ->
                    Log.e(TAG, "ModuleRecognizer init Failed -> " + e.message)
                    if (e.message?.contains("Given runtimes are not available") == true ||
                        e.message?.contains("Initialize barcodeDecoder due to SNPE exception") == true
                    ) {
                        viewModel.updateToastMessage(message = "Selected inference type is not supported on this device. Switching to Auto-select for optimal performance.")
                        viewModel.updateSelectedProcessor(0) //Auto-Select
                        viewModel.saveSettings()
                        initialize()
                    } else if ((e.message?.contains("No DB product data available to build a search index!") == true) ||
                        (e.message?.contains("Cannot open DB file") == true)) {
                        viewModel.updateToastMessage(message = "No products enrolled. Enroll products using Product & Shelf Enrollment")
                    }
                    null
                }
        } catch (e: IOException) {
            Log.e(TAG, "ModuleRecognizer init Failed -> " + e.message)
        }
    }

    private fun configure() {
        try {
            val dataBaseFile = Paths.get(cacheDir, databaseFile).toString()
            //Swap the values as the presented index is reverse of what model expects
            val processorOrder =
                when (uiState.value.retailShelfSettings.commonSettings.processorSelectedIndex) {
                    0 -> arrayOf(2, 0, 1) // AUTO
                    1 -> arrayOf(2) // DSP
                    2 -> arrayOf(1) // GPU
                    3 -> arrayOf(0) //CPU
                    else -> {
                        arrayOf(2, 0, 1)
                    }
                }
            moduleRecognizerSettings.inferencerOptions.runtimeProcessorOrder = processorOrder
            moduleRecognizerSettings.inferencerOptions.defaultDims.width =
                uiState.value.retailShelfSettings.commonSettings.inputSizeSelected
            moduleRecognizerSettings.inferencerOptions.defaultDims.height =
                uiState.value.retailShelfSettings.commonSettings.inputSizeSelected

            val labelBarcodeSettings: BarcodeDecoder.Settings = BarcodeDecoder.Settings(mavenBarcodeModelName)
            val barcodeSettingsMap: MutableMap<EntityType?, BarcodeDecoder.Settings?> = HashMap()
            barcodeSettingsMap[EntityType.LABEL] = labelBarcodeSettings
            moduleRecognizerSettings.enableBarcodeRecognition(barcodeSettingsMap)

            moduleRecognizerSettings.enableProductRecognitionWithDb(
                mavenProductModelName,
                dataBaseFile
            )
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error: configure failed - ${e.message}")
        }
    }

    fun deinitialize() {
        moduleRecognizer?.dispose()
        moduleRecognizer = null
    }

    fun getDetector(): ModuleRecognizer? {
        return moduleRecognizer
    }

    private fun updateRetailShelfModelDemoReady(isReady: Boolean) {
        viewModel.updateRetailShelfModelDemoReady(isReady = isReady)
    }
}