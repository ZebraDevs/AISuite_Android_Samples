// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.example.kotlin.detectors

import android.content.Context
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageProxy
import com.zebra.ai.vision.detector.ImageData
import com.zebra.ai.vision.detector.InferencerOptions
import com.zebra.ai.vision.detector.ModuleRecognizer
import com.zebra.ai.vision.entity.LabelEntity
import com.zebra.ai.vision.entity.ProductEntity
import com.zebra.ai.vision.entity.ShelfEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Executors

class ProductRecognitionSample(private val context: Context){

    private val TAG = "ProductRecognitionSample"
    private val executor = Executors.newFixedThreadPool(3)
    private var moduleRecognizer: ModuleRecognizer? = null
    private val mavenModelName = "product-and-shelf-recognizer"
    /**
     * Initializes the ProductRecognitionSample by setting up the module recognizer models.
     */
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
            Log.e(TAG, "Error in copy from assets ${e.message}")
        }
    }
    private fun processImage(image: ImageProxy) {
        try {
            val imageData = ImageData.fromImageProxy(image)
            val start = System.currentTimeMillis()
            moduleRecognizer?.process(imageData)?.thenAccept { entityList ->
                val end = System.currentTimeMillis()
                Log.d(TAG, "Inference Time: ${end - start} ms")
                val shelves = mutableListOf<ShelfEntity>()
                val labels = mutableListOf<LabelEntity>()
                val products = mutableListOf<ProductEntity>()

                for (entity in entityList) {
                    when (entity) {
                        is ShelfEntity -> shelves.add(entity)
                        is LabelEntity -> labels.add(entity)
                        is ProductEntity -> products.add(entity)
                    }
                }

                val shelfRects = mutableListOf<Rect>()
                val labelShelfRects = mutableListOf<Rect>()
                val labelPegRects = mutableListOf<Rect>()
                val productRects = mutableListOf<Rect>()
                val productLabels = mutableListOf<String>()

                // Draw shelves and their labels
                for (shelf in shelves) {
                    shelfRects.add(shelf.boundingBox)
                }

                // Draw all labels (if you want to show all, not just those attached to shelves)
                for (label in labels) {
                    if (label.classId == LabelEntity.ClassId.PEG_LABEL) {
                        labelPegRects.add(label.boundingBox)
                    }
                    if (label.classId == LabelEntity.ClassId.SHELF_LABEL) {
                        labelShelfRects.add(label.boundingBox)
                    }
                }

                // Draw all products
                for (product in products) {
                    productRects.add(product.boundingBox)
                    val topSku = product.topKSKUs?.firstOrNull()?.let { skuInfo ->
                        "${skuInfo.productSKU} (${String.format("%.2f", skuInfo.accuracy)})"
                    } ?: ""
                    productLabels.add(topSku)
                    Log.d(TAG, "SKU=$topSku, Product bbox=${product.boundingBox}")
                }
                image.close()
            }?.exceptionally { ex ->
                Log.e(TAG, "Error in product recognition: ${ex.message}", ex)
                image.close()
                null
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Exception during analyze: ${ex.message}", ex)
            image.close()
        }
    }
}