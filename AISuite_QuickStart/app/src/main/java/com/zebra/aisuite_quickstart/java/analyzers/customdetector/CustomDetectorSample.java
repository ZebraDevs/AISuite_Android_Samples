// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.analyzers.customdetector;

import android.content.Context;
import android.util.Log;

import androidx.camera.core.ImageAnalysis;
import androidx.core.content.ContextCompat;

import com.zebra.ai.vision.analyzer.tracking.EntityTrackerAnalyzer;
import com.zebra.ai.vision.custommodels.CustomDetector;
import com.zebra.ai.vision.detector.BarcodeDecoder;
import com.zebra.ai.vision.detector.Detector;
import com.zebra.ai.vision.entity.BarcodeEntity;
import com.zebra.ai.vision.entity.DetectionEntity;
import com.zebra.ai.vision.entity.Entity;
import com.zebra.aisuite_quickstart.java.analyzers.customdetector.barcode.BarcodeIntegration;
import com.zebra.aisuite_quickstart.java.analyzers.customdetector.mobilenet.TFLiteModelIntegration;
import com.zebra.aisuite_quickstart.java.analyzers.customdetector.ocr.OcrIntegration;
import com.zebra.aisuite_quickstart.java.analyzers.customdetector.ocr.OcrTextEntity;
import com.zebra.aisuite_quickstart.java.analyzers.customdetector.yolo.YoloIntegration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

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
 *   Step 4 — Handle results             → handleEntities() result callback
 *   Step 5 — Release resources          → stop()
 *
 * Only the models whose IDs appear in selectedIds are initialized. When the filter selection
 * changes the caller disposes this instance and creates a fresh one — matching the Tracker pattern.
 */
public class CustomDetectorSample {

    /** Ordered list shown in the filter dialog — assembled from each Integration class. */
    public static final String[] MODEL_IDS = {
        BarcodeIntegration.MODEL_ID,
        OcrIntegration.MODEL_ID,
        YoloIntegration.MODEL_ID,
        TFLiteModelIntegration.MODEL_ID
    };

    private static final String TAG = "CustomDetectorSampleJava";

    // ── Callback interfaces ───────────────────────────────────────────────────────────────────

    public interface DetectionCallback {
        void handleCustomDetectionEntities(
            List<BarcodeEntity>    barcodeEntities,
            List<OcrTextEntity>    ocrEntities,
            List<DetectionEntity>  yoloEntities,
            List<DetectionEntity>  mobileNetEntities
        );
    }

    public interface ModelLoadingCallback {
        void onLoadingComplete(boolean success);
    }

    // ── Internal state ────────────────────────────────────────────────────────────────────────

    private final Context               context;
    private final DetectionCallback     callback;
    private final ImageAnalysis         imageAnalysis;
    private final ModelLoadingCallback  loadingCallback;
    private final ExecutorService       executor = Executors.newFixedThreadPool(4);

    /**
     * Incremented on every reRegister() call. Each EntityTrackerAnalyzer result callback
     * captures the generation value at construction time and discards its result if the
     * generation has advanced by the time the callback fires — preventing stale in-flight
     * results from overwriting a cleared overlay after a filter change.
     */
    private final AtomicInteger analyzerGeneration = new AtomicInteger(0);

    private volatile BarcodeDecoder                       barcodeDecoder;
    private volatile CustomDetector<OcrTextEntity>        mlkitDetector;
    private volatile CustomDetector<DetectionEntity>      yoloDetector;
    private volatile CustomDetector<DetectionEntity>      mobileNetDetector;

    public CustomDetectorSample(
            Context context,
            DetectionCallback callback,
            ImageAnalysis imageAnalysis,
            List<String> selectedIds,
            ModelLoadingCallback loadingCallback) {

        this.context         = context;
        this.callback        = callback;
        this.imageAnalysis   = imageAnalysis;
        this.loadingCallback = loadingCallback;

        executor.submit(() -> {
            try {
                initAllDetectors(selectedIds);
                loadingCallback.onLoadingComplete(true);
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize custom detectors: " + e.getMessage(), e);
                loadingCallback.onLoadingComplete(false);
            }
        });
    }

    // ── Steps 1+2: Initialize and wrap (delegated to per-model Integration classes) ──────────

    private void initAllDetectors(List<String> selectedIds) throws Exception {
        Log.i(TAG, "initAllDetectors() — selectedIds=" + selectedIds);
        long t0 = System.currentTimeMillis();

        if (selectedIds.contains(BarcodeIntegration.MODEL_ID))      barcodeDecoder    = BarcodeIntegration.create(executor);
        if (selectedIds.contains(OcrIntegration.MODEL_ID))          mlkitDetector     = OcrIntegration.create();
        if (selectedIds.contains(YoloIntegration.MODEL_ID))         yoloDetector      = YoloIntegration.create(context);
        if (selectedIds.contains(TFLiteModelIntegration.MODEL_ID)) mobileNetDetector = TFLiteModelIntegration.create(context);

        Log.i(TAG, "Selected detectors initialized in " + (System.currentTimeMillis() - t0) + " ms");
        buildAndSetAnalyzer(selectedIds);
    }

    // ── Steps 3+4: Register with EntityTrackerAnalyzer and handle results ────────────────────

    private void buildAndSetAnalyzer(List<String> selectedIds) {

        // Step 3: Register — assemble selected detectors into the analyzer
        List<Detector<? extends List<? extends Entity>>> detectorList = new ArrayList<>();

        if (selectedIds.contains(BarcodeIntegration.MODEL_ID)      && barcodeDecoder    != null) detectorList.add(barcodeDecoder);
        if (selectedIds.contains(OcrIntegration.MODEL_ID)          && mlkitDetector     != null) detectorList.add(mlkitDetector);
        if (selectedIds.contains(YoloIntegration.MODEL_ID)         && yoloDetector      != null) detectorList.add(yoloDetector);
        if (selectedIds.contains(TFLiteModelIntegration.MODEL_ID) && mobileNetDetector != null) detectorList.add(mobileNetDetector);

        if (detectorList.isEmpty()) {
            Log.w(TAG, "No detectors selected — analyzer cleared");
            callback.handleCustomDetectionEntities(
                    Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyList());
            return;
        }

        final BarcodeDecoder                       bd = barcodeDecoder;
        final CustomDetector<OcrTextEntity>        md = mlkitDetector;
        final CustomDetector<DetectionEntity>      yd = yoloDetector;
        final CustomDetector<DetectionEntity>      nd = mobileNetDetector;

        EntityTrackerAnalyzer analyzer = new EntityTrackerAnalyzer(
                detectorList,
                ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
                executor,
                result -> handleEntities(result, bd, md, yd, nd)
        );

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), analyzer);
        Log.d(TAG, "EntityTrackerAnalyzer set — detectors=" + detectorList.size() + " ids=" + selectedIds);
    }

    // ── Step 4: Handle results ────────────────────────────────────────────────────────────────

    private void handleEntities(
            EntityTrackerAnalyzer.Result result,
            BarcodeDecoder                       bd,
            CustomDetector<OcrTextEntity>        md,
            CustomDetector<DetectionEntity>      yd,
            CustomDetector<DetectionEntity>      nd) {

        List<BarcodeEntity>   barcodes  = new ArrayList<>();
        List<OcrTextEntity>   ocr       = new ArrayList<>();
        List<DetectionEntity> yolo      = new ArrayList<>();
        List<DetectionEntity> mobileNet = new ArrayList<>();

        if (bd != null) {
            List<? extends Entity> r = result.getValue(bd);
            if (r != null) for (Entity e : r) if (e instanceof BarcodeEntity) barcodes.add((BarcodeEntity) e);
        }
        if (md != null) {
            List<? extends Entity> r = result.getValue(md);
            if (r != null) for (Entity e : r) if (e instanceof OcrTextEntity) ocr.add((OcrTextEntity) e);
        }
        if (yd != null) {
            List<? extends Entity> r = result.getValue(yd);
            if (r != null) for (Entity e : r) if (e instanceof DetectionEntity && !(e instanceof OcrTextEntity)) yolo.add((DetectionEntity) e);
        }
        if (nd != null) {
            List<? extends Entity> r = result.getValue(nd);
            if (r != null) for (Entity e : r) if (e instanceof DetectionEntity && !(e instanceof OcrTextEntity)) mobileNet.add((DetectionEntity) e);
        }

        Log.v(TAG, "Results — barcodes=" + barcodes.size() +
                " ocr=" + ocr.size() + " yolo=" + yolo.size() + " mobileNet=" + mobileNet.size());
        callback.handleCustomDetectionEntities(barcodes, ocr, yolo, mobileNet);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────────────────────

    public void startAnalyzing() {
        Log.d(TAG, "startAnalyzing()");
    }

    public void stopAnalyzing() {
        Log.d(TAG, "stopAnalyzing()");
        executor.shutdownNow();
    }

    public void stop() {
        Log.d(TAG, "stop() — disposing all detectors");

        // Step 5: Release resources
        if (barcodeDecoder    != null) { barcodeDecoder.dispose();    barcodeDecoder    = null; }
        if (mlkitDetector     != null) { mlkitDetector.close();      mlkitDetector     = null; }
        if (yoloDetector      != null) { yoloDetector.close();       yoloDetector      = null; }
        if (mobileNetDetector != null) { mobileNetDetector.close();  mobileNetDetector = null; }
        if (!executor.isShutdown()) executor.shutdownNow();
    }
}
