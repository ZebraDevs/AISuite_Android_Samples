// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.detectors.textocrsample;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.zebra.ai.vision.detector.AIVisionSDKException;
import com.zebra.ai.vision.detector.ImageData;
import com.zebra.ai.vision.detector.TextOCR;
import com.zebra.ai.vision.entity.ParagraphEntity;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/**
 * The TextOCRAnalyzer class implements the ImageAnalysis.Analyzer interface and is
 * responsible for analyzing image frames to perform optical character recognition (OCR)
 * using the TextOCR engine. It processes image data asynchronously and returns recognized
 * text results through a callback interface.
 *
 * This class is designed to be used within an Android application, typically as part of
 * a camera-based OCR solution. It provides a callback mechanism to return OCR results to
 * the caller and manages the analysis lifecycle to ensure efficient processing.
 *
 * Usage:
 * - Instantiate the TextOCRAnalyzer with a DetectionCallback and a TextOCR instance.
 * - Implement the DetectionCallback interface to handle OCR results.
 * - The analyze(ImageProxy) method is called by the camera framework to process image frames.
 * - Call stopAnalyzing() to stop the analysis process and release resources.
 *
 * Dependencies:
 * - Android ImageProxy: Provides access to image data from the camera.
 * - ExecutorService: Used for asynchronous task execution.
 * - TextOCR: Handles the OCR processing to recognize text in images.
 *
 * Concurrency:
 * - Uses a single-threaded executor to ensure that image analysis tasks are processed sequentially.
 * - Manages concurrency with flags to control analysis state and termination.
 *
 * Note: Ensure that the appropriate permissions and dependencies are configured
 * in the AndroidManifest and build files to utilize camera and image processing capabilities.
 */
public class TextOCRAnalyzer implements ImageAnalysis.Analyzer {

    /**
     * Interface for handling the results of the OCR detection process.
     * Implement this interface to define how OCR results are processed.
     */
    public interface DetectionCallback {
        void onDetectionTextResult(List<ParagraphEntity> list);
    }

    private static final String TAG = "TextOCRAnalyzer";
    private final DetectionCallback callback;
    private final TextOCR textOCR;
    private final ExecutorService executorService;
    private boolean isAnalyzing = true;
    private volatile boolean isStopped = false;

    /**
     * Constructs a new TextOCRAnalyzer with the specified callback and TextOCR instance.
     *
     * @param callback The callback for handling OCR results.
     * @param textOCR The TextOCR engine used to process image data.
     */
    public TextOCRAnalyzer(DetectionCallback callback, TextOCR textOCR) {
        this.callback = callback;
        this.executorService = Executors.newSingleThreadExecutor();
        this.textOCR = textOCR;
    }

    /**
     * Analyzes the given image to perform OCR. This method is called by the camera
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

                textOCR.process(ImageData.fromImageProxy(image))
                        .thenAccept(result -> {
                            if (!isStopped) {
                                callback.onDetectionTextResult(result);
                            }
                            isAnalyzing = true;
                            image.close();
                        })
                        .exceptionally(ex -> {
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
