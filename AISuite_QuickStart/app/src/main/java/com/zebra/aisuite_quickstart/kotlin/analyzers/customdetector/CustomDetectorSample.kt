// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.analyzers.customdetector

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.core.content.ContextCompat
import com.zebra.ai.vision.analyzer.tracking.EntityTrackerAnalyzer
import com.zebra.ai.vision.custommodels.CustomDetector
import com.zebra.ai.vision.detector.BarcodeDecoder
import com.zebra.ai.vision.detector.Detector
import com.zebra.ai.vision.entity.BarcodeEntity
import com.zebra.ai.vision.entity.DetectionEntity
import com.zebra.ai.vision.entity.Entity
import com.zebra.aisuite_quickstart.kotlin.analyzers.customdetector.barcode.BarcodeIntegration
import com.zebra.aisuite_quickstart.kotlin.analyzers.customdetector.mobilenet.TFLiteModelIntegration
import com.zebra.aisuite_quickstart.kotlin.analyzers.customdetector.ocr.OcrIntegration
import com.zebra.aisuite_quickstart.kotlin.analyzers.customdetector.ocr.OcrTextEntity
import com.zebra.aisuite_quickstart.kotlin.analyzers.customdetector.yolo.YoloIntegration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * CustomDetectorSample — demonstrates multiple heterogeneous models registered in one
 * EntityTrackerAnalyzer:
 *
 *   • Zebra BarcodeDecoder (SDK native Detector — no CustomDetector wrapping needed)
 *   • ML Kit Text Recognition (third-party → wrapped via CustomDetector<OcrTextEntity>)
 *   • YOLOv8n ONNX (third-party → wrapped via CustomDetector<DetectionEntity>)
 *   • MobileNet SSD TFLite (third-party → wrapped via CustomDetector<DetectionEntity>)
 *
 * Integration follows the 5-step CustomDetector pattern from the tech doc:
 *   Step 1 — Initialize each model      → see each model's Integration class
 *   Step 2 — Wrap with CustomDetector   → see each model's Integration class  (n/a for BarcodeDecoder)
 *   Step 3 — Register with analyzer     → buildAndSetAnalyzer()
 *   Step 4 — Handle results             → buildAndSetAnalyzer() result callback
 *   Step 5 — Release resources          → stop()
 *
 * Only the models whose IDs appear in selectedIds are initialized. When the filter selection
 * changes the caller disposes this instance and creates a fresh one — matching the Tracker pattern.
 */
class CustomDetectorSample(
    private val context        : Context,
    private val callback       : DetectionCallback,
    private val imageAnalysis  : ImageAnalysis,
    selectedIds                : List<String>,
    private val loadingCallback: ModelLoadingCallback
) {

    companion object {
        /** Ordered list shown in the filter dialog — assembled from each Integration class. */
        @JvmField
        val MODEL_IDS = arrayOf(
            BarcodeIntegration.MODEL_ID,
            OcrIntegration.MODEL_ID,
            YoloIntegration.MODEL_ID,
            TFLiteModelIntegration.MODEL_ID
        )

        private const val TAG = "CustomDetectorSampleKt"
    }

    // ── Callbacks ─────────────────────────────────────────────────────────────────────────────

    interface DetectionCallback {
        fun handleCustomDetectionEntities(
            barcodeEntities   : List<BarcodeEntity>,
            ocrEntities       : List<OcrTextEntity>,
            yoloEntities      : List<DetectionEntity>,
            mobileNetEntities : List<DetectionEntity>
        )
    }

    interface ModelLoadingCallback {
        fun onLoadingComplete(success: Boolean)
    }

    // ── Internal state ────────────────────────────────────────────────────────────────────────

    private val executor: ExecutorService = Executors.newFixedThreadPool(4)

    /**
     * Incremented on every reRegister() call. Each EntityTrackerAnalyzer result callback
     * captures the generation value at construction time and discards its result if the
     * generation has advanced by the time the callback fires — preventing stale in-flight
     * results from overwriting a cleared overlay after a filter change.
     */
    private val analyzerGeneration = AtomicInteger(0)

    private var barcodeDecoder : BarcodeDecoder? = null
    private var mlkitDetector  : CustomDetector<OcrTextEntity>? = null
    private var yoloDetector   : CustomDetector<DetectionEntity>? = null
    private var mobileNetDetector : CustomDetector<DetectionEntity>? = null

    init {
        executor.submit {
            try {
                initAllDetectors(selectedIds)
                loadingCallback.onLoadingComplete(true)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize custom detectors: ${e.message}", e)
                loadingCallback.onLoadingComplete(false)
            }
        }
    }

    // ── Steps 1+2: Initialize and wrap (delegated to per-model Integration classes) ──────────

    private fun initAllDetectors(selectedIds: List<String>) {
        Log.i(TAG, "initAllDetectors() — selectedIds=$selectedIds")
        val t0 = System.currentTimeMillis()

        if (BarcodeIntegration.MODEL_ID      in selectedIds) barcodeDecoder    = BarcodeIntegration.create(executor)
        if (OcrIntegration.MODEL_ID          in selectedIds) mlkitDetector     = OcrIntegration.create()
        if (YoloIntegration.MODEL_ID         in selectedIds) yoloDetector      = YoloIntegration.create(context)
        if (TFLiteModelIntegration.MODEL_ID in selectedIds) mobileNetDetector = TFLiteModelIntegration.create(context)

        Log.i(TAG, "Selected detectors initialized in ${System.currentTimeMillis() - t0} ms")
        buildAndSetAnalyzer(selectedIds)
    }

    // ── Steps 3+4: Register with EntityTrackerAnalyzer and handle results ────────────────────

    private fun buildAndSetAnalyzer(selectedIds: List<String>) {
        // Snapshot the current generation. The result callback below captures this value and
        // compares it against analyzerGeneration at dispatch time to drop stale results.
        val generation = analyzerGeneration.get()

        // Step 3: Register — assemble selected detectors into the analyzer
        val detectorList = mutableListOf<Detector<out MutableList<out Entity>>>()

        if (BarcodeIntegration.MODEL_ID      in selectedIds) barcodeDecoder?.let    { detectorList.add(it) }
        if (OcrIntegration.MODEL_ID          in selectedIds) mlkitDetector?.let     { detectorList.add(it) }
        if (YoloIntegration.MODEL_ID         in selectedIds) yoloDetector?.let      { detectorList.add(it) }
        if (TFLiteModelIntegration.MODEL_ID in selectedIds) mobileNetDetector?.let { detectorList.add(it) }

        if (detectorList.isEmpty()) {
            Log.w(TAG, "No detectors selected — analyzer cleared")
            callback.handleCustomDetectionEntities(emptyList(), emptyList(), emptyList(), emptyList())
            return
        }

        val analyzer = EntityTrackerAnalyzer(
            detectorList,
            ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
            executor
        ) { result ->
            // Guard: discard result if a newer reRegister() has been called since this
            // analyzer was created. This prevents in-flight frames from a superseded
            // analyzer from overwriting the overlay after a filter change.
            val currentGeneration = analyzerGeneration.get()
            if (currentGeneration != generation) {
                Log.w(TAG, "Discarding stale result — analyzer generation=$generation current=$currentGeneration")
                return@EntityTrackerAnalyzer
            }

            // Step 4: Handle results — retrieve per-detector entity lists
            val barcodes = barcodeDecoder?.let {
                result.getValue(it)?.filterIsInstance<BarcodeEntity>()
            } ?: emptyList()
            val ocr = mlkitDetector?.let {
                result.getValue(it)?.filterIsInstance<OcrTextEntity>()
            } ?: emptyList()
            val yolo = yoloDetector?.let {
                result.getValue(it)?.filterIsInstance<DetectionEntity>()
            } ?: emptyList()
            val mobileNet = mobileNetDetector?.let {
                result.getValue(it)?.filterIsInstance<DetectionEntity>()
            } ?: emptyList()

            Log.v(TAG, "Results — barcodes=${barcodes.size} ocr=${ocr.size} yolo=${yolo.size} mobileNet=${mobileNet.size}")
            callback.handleCustomDetectionEntities(barcodes, ocr, yolo, mobileNet)
        }

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), analyzer)
        Log.d(TAG, "EntityTrackerAnalyzer set — detectors=${detectorList.size} ids=$selectedIds")
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────────────────────

    fun startAnalyzing() {
        Log.d(TAG, "startAnalyzing()")
    }

    fun stopAnalyzing() {
        Log.d(TAG, "stopAnalyzing()")
        executor.shutdownNow()
    }

    fun stop() {
        Log.d(TAG, "stop() — disposing all detectors")

        // Step 5: Release resources
        barcodeDecoder?.dispose();    barcodeDecoder    = null
        mlkitDetector?.close();       mlkitDetector     = null
        yoloDetector?.close();        yoloDetector      = null
        mobileNetDetector?.close();   mobileNetDetector = null
        if (!executor.isShutdown) executor.shutdownNow()
    }
}
