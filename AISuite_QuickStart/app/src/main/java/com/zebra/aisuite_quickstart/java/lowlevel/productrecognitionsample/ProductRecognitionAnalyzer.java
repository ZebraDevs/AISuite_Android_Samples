// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.lowlevel.productrecognitionsample;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.zebra.ai.vision.detector.AIVisionSDKException;
import com.zebra.ai.vision.detector.BBox;
import com.zebra.ai.vision.detector.FeatureExtractor;
import com.zebra.ai.vision.detector.InvalidInputException;
import com.zebra.ai.vision.detector.Localizer;
import com.zebra.ai.vision.detector.Recognizer;
import com.zebra.aisuite_quickstart.utils.CommonUtils;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * The ProductRecognitionAnalyzer class implements the ImageAnalysis.Analyzer interface and is
 * responsible for analyzing image frames to detect and recognize products. It integrates multiple
 * components including a localizer for object detection, a feature extractor for generating
 * descriptors, and a recognizer for matching and identifying products.
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
 * - Localizer: Detects objects within an image.
 * - FeatureExtractor: Generates feature descriptors for detected objects.
 * - Recognizer: Matches feature descriptors to known products.
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
        void onDetectionRecognitionResult(BBox[] detections, BBox[] products, Recognizer.Recognition[] recognitions);
    }

    private static final String TAG = "ProductRecognitionAnalyzer";
    private boolean isAnalyzing = true;
    private final DetectionCallback callback;
    private final Localizer localizer;
    private final FeatureExtractor featureExtractor;
    private final Recognizer recognizer;
    private BBox[] detections, products;
    private volatile boolean isStopped = false;
    private final ExecutorService executorService;

    /**
     * Constructs a new ProductRecognitionAnalyzer with the specified callback, localizer,
     * feature extractor, and recognizer.
     *
     * @param callback The callback for handling recognition results.
     * @param localizer The localizer used for detecting objects within images.
     * @param featureExtractor The feature extractor used for generating descriptors.
     * @param recognizer The recognizer used for matching and identifying products.
     */
    public ProductRecognitionAnalyzer(DetectionCallback callback, Localizer localizer, FeatureExtractor featureExtractor, Recognizer recognizer) {
        this.callback = callback;
        this.localizer = localizer;
        this.featureExtractor = featureExtractor;
        this.recognizer = recognizer;
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
        if (!isAnalyzing || isStopped) {
            image.close();
            return;
        }
        isAnalyzing = false; // Prevent re-entry

        Future<?> future = executorService.submit(() -> {
            try {
                Log.d(TAG, "Starting image analysis");
                Bitmap bitmap = CommonUtils.rotateBitmapIfNeeded(image);
                CompletableFuture<BBox[]> futureResultBBox = localizer.detect(bitmap, executorService);

                futureResultBBox.thenCompose(bBoxes -> {
                    detections = bBoxes;
                    products = Arrays.stream(bBoxes).filter(x -> x.cls == 1).toArray(BBox[]::new);
                    Log.d(TAG, "Products size =" + products.length + " detections " + detections.length);

                    if (detections != null && detections.length > 0) {
                        try {
                            return featureExtractor.generateDescriptors(products, bitmap, executorService);
                        } catch (InvalidInputException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        return CompletableFuture.completedFuture(null);
                    }
                }).thenCompose(descriptor -> {
                    if (descriptor != null && detections.length > 0) {
                        try {
                            return recognizer.findRecognitions(descriptor, executorService);
                        } catch (InvalidInputException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        return CompletableFuture.completedFuture(null);
                    }
                }).thenAccept(recognitions -> {
                    if (recognitions != null) {
                        Log.d(TAG, "Products recognitions " + recognitions.length);
                        if (!isStopped) callback.onDetectionRecognitionResult(detections, products, recognitions);
                    }
                    image.close();
                    isAnalyzing = true;
                }).exceptionally(ex -> {
                    Log.e(TAG, "Error in completable future result " + ex.getMessage());
                    image.close();
                    isAnalyzing = true;
                    return null;
                });

            } catch (AIVisionSDKException e) {
                Log.e(TAG, "Exception occurred: " + e.getMessage());
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

