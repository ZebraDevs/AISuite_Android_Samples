// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.lowlevel.simplebarcodesample;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.zebra.ai.vision.detector.AIVisionSDKException;
import com.zebra.ai.vision.detector.BBox;
import com.zebra.ai.vision.detector.BarcodeDecoder;
import com.zebra.ai.vision.detector.InvalidInputException;
import com.zebra.ai.vision.detector.Localizer;
import com.zebra.aisuite_quickstart.utils.CommonUtils;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * The BarcodeSampleAnalyzer class implements the ImageAnalysis.Analyzer interface and is
 * responsible for analyzing image frames to detect and decode barcodes. It utilizes a Localizer
 * to identify potential regions of interest and a BarcodeDecoder to decode barcodes within those
 * regions.
 *
 * This class is designed to be used within an Android application as part of a camera-based barcode
 * scanning solution. It processes image data asynchronously and returns detection results through
 * a callback interface.
 *
 * Usage:
 * - Instantiate the BarcodeSampleAnalyzer with the appropriate callback, localizer, and barcode decoder.
 * - Implement the SampleBarcodeDetectionCallback interface to handle barcode detection results.
 * - The analyze(ImageProxy) method is called by the camera framework to process image frames.
 *
 * Dependencies:
 * - Android ImageProxy: Provides access to image data from the camera.
 * - ExecutorService: Used for asynchronous task execution.
 * - Localizer: Detects potential regions of interest for barcode decoding.
 * - BarcodeDecoder: Decodes barcodes from images.
 *
 * Concurrency:
 * - Uses a single-threaded executor to ensure that image analysis tasks are processed sequentially.
 * - Manages concurrency with a flag to control analysis state and prevent re-entry.
 *
 * Note: Ensure that the appropriate permissions and dependencies are configured
 * in the AndroidManifest and build files to utilize camera and image processing capabilities.
 */
public class BarcodeSampleAnalyzer implements ImageAnalysis.Analyzer {

    /**
     * Interface for handling the results of the barcode detection process.
     * Implement this interface to define how detection results are processed.
     */
    public interface SampleBarcodeDetectionCallback {
        void onDetectionResult(BarcodeDecoder.Result[] list);
    }

    private static final String TAG = "BarcodeAnalyzer";
    private boolean isAnalyzing = true;
    private final BarcodeSampleAnalyzer.SampleBarcodeDetectionCallback callback;
    private BarcodeDecoder barcodeDecoder;
    private BBox[] detections;
    private Localizer localizer;
    private final ExecutorService executorService;
    private volatile boolean isStopped = false;

    /**
     * Constructs a new BarcodeSampleAnalyzer with the specified callback, localizer, and barcode decoder.
     *
     * @param callback The callback for handling barcode detection results.
     * @param localizer The localizer used to detect potential regions of interest.
     * @param barcodeDecoder The barcode decoder used to decode barcodes within detected regions.
     */
    public BarcodeSampleAnalyzer(BarcodeSampleAnalyzer.SampleBarcodeDetectionCallback callback, Localizer localizer, BarcodeDecoder barcodeDecoder) {
        this.callback = callback;
        this.localizer = localizer;
        this.barcodeDecoder = barcodeDecoder;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Analyzes the given image to detect and decode barcodes. This method is called by the camera
     * framework to process image frames asynchronously.
     *
     * @param image The image frame to analyze.
     */
    @Override
    public void analyze(@NonNull ImageProxy image) {
        if (!isAnalyzing || isStopped) {
            image.close();
            return;
        }
        isAnalyzing = false; // Prevent re-entry

        Future<?> future = executorService.submit(() -> {
            try {
                Bitmap bitmap = CommonUtils.rotateBitmapIfNeeded(image);
                CompletableFuture<BBox[]> futureResult = localizer.detect(bitmap, executorService);
                Log.d(TAG, "Starting image analysis");
                futureResult.thenCompose(bBoxes -> {
                    detections = bBoxes;
                    // Proceed to barcode decoding
                    try {
                        return barcodeDecoder.decode(bitmap, bBoxes, executorService);
                    } catch (InvalidInputException e) {
                        throw new RuntimeException(e);
                    }
                }).thenAccept(barcodes -> {
                    callback.onDetectionResult(barcodes);
                    isAnalyzing = true;
                    image.close();
                }).exceptionally(ex -> {
                    Log.e(TAG, "Error in completable future result " + ex.getMessage());
                    isAnalyzing = true;
                    image.close();
                    return null;
                });

            } catch (AIVisionSDKException e) {
                Log.e(TAG, Objects.requireNonNull(e.getMessage()));
                isAnalyzing = true;
                image.close();
            }
        });
        // Cancel the task if the analyzer is stopped
        if (isStopped) {
            future.cancel(true);
        }
    }
    /**
     * Stops the analysis process and terminates any ongoing tasks. This method should be
     * called to release resources and halt image analysis when it is no longer required.
     */
    public void stopAnalyzing() {
        isStopped = true;
        executorService.shutdownNow(); // Attempt to cancel ongoing tasks
    }
}
