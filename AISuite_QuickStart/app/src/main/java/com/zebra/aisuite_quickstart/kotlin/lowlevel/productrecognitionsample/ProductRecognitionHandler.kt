// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.lowlevel.productrecognitionsample

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.core.content.ContextCompat
import com.zebra.ai.vision.detector.InferencerOptions
import com.zebra.ai.vision.detector.ModuleRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Executors

class ProductRecognitionHandler(
    private val context: Context,
    private val callback: ProductRecognitionAnalyzer.DetectionCallback,
    private val imageAnalysis: ImageAnalysis
) {
    private val TAG = "ProductRecognitionHandler"
    private val executor = Executors.newFixedThreadPool(3)
    private var productRecognitionAnalyzer: ProductRecognitionAnalyzer? = null
    private var moduleRecognizer: ModuleRecognizer? = null
    private val mavenModelName = "product-and-shelf-recognizer"

    init {
        initializeModuleRecognizer()
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
                val settings = ModuleRecognizer.Settings(mavenModelName)

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
                    mavenModelName,
                    "$toPath$indexFilename",
                    "$toPath$labelsFilename"
                )

                // Initialize ModuleRecognizer
                val startTime = System.currentTimeMillis()
                moduleRecognizer = ModuleRecognizer.getModuleRecognizer(settings, executor).await()
                val creationTime = System.currentTimeMillis() - startTime

                Log.d(TAG, "ModuleRecognizer Creation Time: ${creationTime}ms")
                Log.i(TAG, "ModuleRecognizer instance created successfully")

                // Set up analyzer
                productRecognitionAnalyzer = ProductRecognitionAnalyzer(callback, moduleRecognizer)
                imageAnalysis.setAnalyzer(
                    ContextCompat.getMainExecutor(context),
                    productRecognitionAnalyzer!!
                )

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

    fun getProductRecognitionAnalyzer(): ProductRecognitionAnalyzer? {
        return productRecognitionAnalyzer
    }

    fun stop() {
        executor.shutdownNow()
        productRecognitionAnalyzer?.stopAnalyzing()
        moduleRecognizer?.dispose()
        Log.d(TAG, "ModuleRecognizer disposed")
        moduleRecognizer = null
    }
}