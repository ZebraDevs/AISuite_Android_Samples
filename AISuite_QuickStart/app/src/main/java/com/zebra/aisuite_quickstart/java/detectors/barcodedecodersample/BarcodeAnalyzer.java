// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.detectors.barcodedecodersample;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.zebra.ai.vision.detector.AIVisionSDKException;
import com.zebra.ai.vision.detector.BarcodeDecoder;
import com.zebra.ai.vision.detector.ImageData;
import com.zebra.ai.vision.entity.BarcodeEntity;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * The BarcodeAnalyzer class implements the ImageAnalysis.Analyzer interface and is
 * responsible for analyzing image frames to detect barcodes. It utilizes a BarcodeDecoder
 * to process the image data and execute the detection logic asynchronously.
 *
 * This class is designed to be used within the context of an Android application, typically
 * as part of a camera-based barcode scanning solution. It provides a callback interface to
 * return detection results to the caller.
 *
 * Usage:
 * - Instantiate the BarcodeAnalyzer with a DetectionCallback and a BarcodeDecoder.
 * - Implement the DetectionCallback interface to handle detection results.
 * - The analyze(ImageProxy) method is called by the camera framework to process image frames.
 * - Call stopAnalyzing() to stop the analysis process and release resources.
 *
 * Dependencies:
 * - Android ImageProxy: Provides access to image data from the camera.
 * - ExecutorService: Used for asynchronous task execution.
 * - BarcodeDecoder: Handles the decoding of barcode data from images.
 *
 * Concurrency:
 * - Uses a single-threaded executor to ensure that image analysis tasks are processed sequentially.
 * - Manages concurrency with volatile flags to control analysis state and termination.
 *
 * Note: Ensure that the appropriate permissions and dependencies are configured
 * in the AndroidManifest and build files to utilize camera and image processing capabilities.
 */
public class BarcodeAnalyzer implements ImageAnalysis.Analyzer {

    /**
     * Interface for handling the results of the barcode detection process.
     * Implement this interface to define how detection results are processed.
     */
    public interface DetectionCallback {
        void onDetectionResult(List<BarcodeEntity> list);
    }

    private static final String TAG = "BarcodeAnalyzer";
    private final DetectionCallback callback;
    private final BarcodeDecoder barcodeDecoder;
    private final ExecutorService executorService;
    private volatile boolean isAnalyzing = true;
    private volatile boolean isStopped = false;

    /**
     * Constructs a new BarcodeAnalyzer with the specified callback and barcode decoder.
     *
     * @param callback The callback for handling detection results.
     * @param barcodeDecoder The barcode decoder used to process image data.
     */
    public BarcodeAnalyzer(DetectionCallback callback, BarcodeDecoder barcodeDecoder) {
        this.callback = callback;
        this.barcodeDecoder = barcodeDecoder;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Analyzes the given image to detect barcodes. This method is called by the camera
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
                Log.d(TAG, "Starting image analysis");
                barcodeDecoder.process(ImageData.fromImageProxy(image))
                        .thenAccept(result -> {
                            if (!isStopped) {
                                callback.onDetectionResult(result);
                            }
                        })
                        .exceptionally(ex -> {
                            Log.e(TAG, "Error in completable future result " + ex.getMessage());
                            return null;
                        });
                image.close();
                isAnalyzing = true;
            } catch (AIVisionSDKException e) {
                Log.e(TAG, Objects.requireNonNull(e.getMessage()));
                image.close();
                isAnalyzing = true;
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
