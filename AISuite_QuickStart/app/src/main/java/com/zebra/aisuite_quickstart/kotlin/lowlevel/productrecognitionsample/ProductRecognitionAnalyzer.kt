// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.lowlevel.productrecognitionsample

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.zebra.ai.vision.detector.ModuleRecognizer
import com.zebra.ai.vision.entity.Entity
import com.zebra.ai.vision.detector.ImageData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ProductRecognitionAnalyzer(
    private val callback: DetectionCallback,
    private val moduleRecognizer: ModuleRecognizer?
) : ImageAnalysis.Analyzer {

    interface DetectionCallback {
        fun onRecognitionResult(result: List<Entity>?)
    }

    private val TAG = "ProductRecognitionAnalyzer"
    private var isAnalyzing = true
    private var isStopped = false
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

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

    fun stopAnalyzing() {
        isStopped = true
        job.cancel()
        executorService.shutdownNow()
    }
}