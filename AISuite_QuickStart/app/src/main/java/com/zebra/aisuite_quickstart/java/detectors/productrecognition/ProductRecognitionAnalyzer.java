// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.detectors.productrecognition;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.zebra.ai.vision.detector.AIVisionSDKException;
import com.zebra.ai.vision.detector.ImageData;
import com.zebra.ai.vision.detector.ModuleRecognizer;
import com.zebra.ai.vision.entity.Entity;
import com.zebra.ai.vision.entity.ShelfEntity;
import com.zebra.aisuite_quickstart.utils.AppLog;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * The ProductRecognitionAnalyzer class implements the ImageAnalysis.Analyzer interface and is
 * responsible for analyzing image frames to detect and recognize products. It integrates module recognizer for matching and
 * identify products.
 * <p>
 * This class is designed to be used within an Android application as part of a camera-based
 * product recognition solution. It processes image data asynchronously and returns recognition
 * results through a callback interface.
 * <p>
 * Usage:
 * - Instantiate the ProductRecognitionAnalyzer with the appropriate callback, localizer, feature extractor, and recognizer.
 * - Implement the DetectionCallback interface to handle recognition results.
 * - The analyze(ImageProxy) method is called by the camera framework to process image frames.
 * - Call stopAnalyzing() to stop the analysis process and release resources.
 * <p>
 * Dependencies:
 * - Android ImageProxy: Provides access to image data from the camera.
 * - ExecutorService: Used for asynchronous task execution.
 * - ModuleRecognizer
 * <p>
 * Concurrency:
 * - Uses a single-threaded executor to ensure that image analysis tasks are processed sequentially.
 * - Manages concurrency with flags to control analysis state and termination.
 * <p>
 * Note: Ensure that the appropriate permissions and dependencies are configured
 * in the AndroidManifest and build files to utilize camera and image processing capabilities.
 */
public class ProductRecognitionAnalyzer implements ImageAnalysis.Analyzer {

    /**
     * Interface for handling the results of the product recognition process.
     * Implement this interface to define how recognition results are processed.
     */
    public interface DetectionCallback {
        void onRecognitionResult(List<Entity> result);

        void onCaptureRecognitionResult(List<Entity> result);
    }

    private static final String TAG = "ProductRecognitionAnalyzer";
    private final AtomicBoolean isAnalyzing = new AtomicBoolean(true);
    private final DetectionCallback callback;
    private volatile boolean isStopped = false;
    private ExecutorService executorService;
    private final ModuleRecognizer productRecognizer;


    public ProductRecognitionAnalyzer(DetectionCallback callback, ModuleRecognizer productRecognizer) {
        this.callback = callback;
        this.productRecognizer = productRecognizer;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Analyzes the given image to perform product recognition. This method is called by the camera
     * framework to process image frames asynchronously.
     *
     * @param image The image frame to analyze.
     */
    @Override
    public void analyze(@NonNull ImageProxy image) {
        if (!isAnalyzing.compareAndSet(true, false) || isStopped) {
            image.close();
            return;
        }

        ImageData imageData = ImageData.fromImageProxy(image);

        long start = System.currentTimeMillis();
        AppLog.v(TAG, "Starting image analysis");
        executorService.execute(() -> {
            try {
                productRecognizer.process(imageData)
                        .thenAccept(entityList -> {
                            long end = System.currentTimeMillis();
                            long inferenceTime = end - start;
                            AppLog.d(TAG, "Inference Time: " + inferenceTime);
                            if (!isStopped && callback != null) {
                                callback.onRecognitionResult(entityList);
                            }
                            image.close();
                            isAnalyzing.set(true);
                        })
                        .exceptionally(ex -> {
                            AppLog.e(TAG, "Error in shelf recognition: " + ex.getMessage());
                            image.close();
                            isAnalyzing.set(true);
                            return null;
                        });

            } catch (Exception e) {
                AppLog.e(TAG, "Error running product recognition "+ e.getMessage());
                isAnalyzing.set(true);
                image.close();
            }
        });
    }

    /**
     * Process image for capture mode using a different recognizer
     *
     * @param image             The captured image to process
     * @param captureRecognizer The high-resolution recognizer for capture mode
     */
    public void processImage(ImageProxy image, ModuleRecognizer captureRecognizer) {
        try {
            AppLog.v(TAG, "Starting image capture analysis");
            captureRecognizer.process(ImageData.fromImageProxy(image))
                    .thenAccept(result -> {
                        callback.onCaptureRecognitionResult(result);
                        image.close();
                    }).exceptionally(ex -> {
                        AppLog.e(TAG, "Error in completable future result " + ex.getMessage());
                        image.close();
                        return null;
                    });
        } catch (Exception e) {
            AppLog.e(TAG, Objects.requireNonNull(e.getMessage()));
            image.close();
        }
    }


    /**
     * Stops the analysis process and terminates any ongoing tasks. This method should be
     * called to release resources and halt image analysis when it is no longer required.
     */

    public void stopAnalyzing() {
        AppLog.i(TAG, "stopAnalyzing() called. Shutting down executor.");
        isStopped = true;
        executorService.shutdownNow();
    }

    public void startAnalyzing() {
        AppLog.i(TAG, "startAnalyzing() called. ");
        isStopped = false;
        isAnalyzing.set(true);
        executorService = Executors.newSingleThreadExecutor();
    }
}