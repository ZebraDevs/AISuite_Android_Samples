// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.detectors.productrecognition;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.zebra.ai.vision.detector.AIVisionSDKException;
import com.zebra.ai.vision.detector.BBox;
import com.zebra.ai.vision.detector.ImageData;
import com.zebra.ai.vision.detector.ModuleRecognizer;
import com.zebra.ai.vision.entity.Entity;
import com.zebra.ai.vision.entity.ShelfEntity;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * The ProductRecognitionAnalyzer class implements the ImageAnalysis.Analyzer interface and is
 * responsible for analyzing image frames to detect and recognize products. It integrates module recognizer for matching and
 * identify products.
 *
 * This class is designed to be used within an Android application as part of a camera-based
 * product recognition solution. It processes image data asynchronously and returns recognition
 * results through a callback interface.
 *
 * Usage:
 * - Instantiate the ProductRecognitionAnalyzer with the appropriate callback, localizer, feature extractor, and recognizer.
 * - Implement the DetectionCallback interface to handle recognition results.
 * - The analyze(ImageProxy) method is called by the camera framework to process image frames.
 * - Call stopAnalyzing() to stop the analysis process and release resources.
 *
 * Dependencies:
 * - Android ImageProxy: Provides access to image data from the camera.
 * - ExecutorService: Used for asynchronous task execution.
 * - ModuleRecognizer
 *
 * Concurrency:
 * - Uses a single-threaded executor to ensure that image analysis tasks are processed sequentially.
 * - Manages concurrency with flags to control analysis state and termination.
 *
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
    }

    private static final String TAG = "ProductRecognitionAnalyzer";
    private boolean isAnalyzing = true;
    private final DetectionCallback callback;
    private BBox[] detections, products;
    private volatile boolean isStopped = false;
    private final ExecutorService executorService;
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
        Log.e(TAG, "analyze() called");
        if(productRecognizer == null){
            Log.d(TAG, "Module recognizer is null");
            image.close();
            return;
        }
        if (!isAnalyzing || isStopped) {
            Log.d(TAG, "Analyzer is stopped or already analyzing. Closing image.");
            image.close();
            return;
        }

        isAnalyzing = false;

        Log.d(TAG, "Converting ImageProxy to Bitmap...");
        ImageData imageData = ImageData.fromImageProxy(image);

        Log.d(TAG, "Calling moduleRecognizer.process...");
        long start = System.currentTimeMillis();
        try {
            productRecognizer.process(imageData)
                    .thenAccept(entityList -> {
                        long end = System.currentTimeMillis();
                        long inferenceTime = end - start;
                        Log.d(TAG, "Inference Time: " + inferenceTime);
                        int shelfCount = 0;
                        if (entityList != null) {
                            for (Entity entity : entityList) {
                                if (entity instanceof ShelfEntity) {
                                    shelfCount++;
                                }
                            }
                        }
                        Log.d(TAG, "process() completed. Shelves found: " + shelfCount);
                        if (!isStopped && callback != null) {
                            Log.d(TAG, "Invoking callback.onRecognitionResult");
                            callback.onRecognitionResult(entityList);
                        }
                        Log.d(TAG, "Image closed, ready for next frame.");
                    })
                    .exceptionally(ex -> {
                        Log.e(TAG, "Error in shelf recognition: " + ex.getMessage(), ex);
                        return null;
                    });
            image.close();
            isAnalyzing = true;
        } catch (AIVisionSDKException e) {
            Log.e(TAG, Objects.requireNonNull(e.getMessage()));
            isAnalyzing = true;
            image.close();
        }
    }


    /**
     * Stops the analysis process and terminates any ongoing tasks. This method should be
     * called to release resources and halt image analysis when it is no longer required.
     */

    public void stopAnalyzing() {
        Log.d(TAG, "stopAnalyzing() called. Shutting down executor.");
        isStopped = true;
        executorService.shutdownNow();
    }
}