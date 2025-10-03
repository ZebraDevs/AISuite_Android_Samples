// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.lowlevel.simpleocrsample;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.zebra.ai.vision.detector.InvalidInputException;
import com.zebra.ai.vision.detector.TextOCR;
import com.zebra.ai.vision.internal.detector.Word;
import com.zebra.aisuite_quickstart.utils.CommonUtils;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * The OCRAnalyzer class implements the ImageAnalysis.Analyzer interface and is responsible
 * for analyzing image frames to perform Optical Character Recognition (OCR). It utilizes
 * the TextOCR engine to detect and process text within the image.
 *
 * This class is designed to be used within an Android application as part of a camera-based
 * OCR solution. It processes image data asynchronously and returns recognized text results
 * through a callback interface.
 *
 * Usage:
 * - Instantiate the OCRAnalyzer with the appropriate callback and TextOCR instance.
 * - Implement the DetectionCallback interface to handle OCR results.
 * - The analyze(ImageProxy) method is called by the camera framework to process image frames.
 *
 * Dependencies:
 * - Android ImageProxy: Provides access to image data from the camera.
 * - ExecutorService: Used for asynchronous task execution.
 * - TextOCR: Processes the bitmap to detect text words.
 *
 * Concurrency:
 * - Uses a single-threaded executor to ensure that image analysis tasks are processed sequentially.
 * - Manages concurrency with a flag to control analysis state and prevent re-entry.
 *
 * Note: Ensure that the appropriate permissions and dependencies are configured
 * in the AndroidManifest and build files to utilize camera and image processing capabilities.
 */
public class OCRAnalyzer implements ImageAnalysis.Analyzer {

    /**
     * Interface for handling the results of the OCR detection process.
     * Implement this interface to define how OCR results are processed.
     */
    public interface DetectionCallback {
        void onDetectionTextResult(Word[] list);
    }

    private static final String TAG = "TextOCRAnalyzer";
    private final OCRAnalyzer.DetectionCallback callback;
    private final TextOCR textOCR;
    private final ExecutorService executorService;
    private boolean isAnalyzing = true;
    private volatile boolean isStopped = false;

    /**
     * Constructs a new OCRAnalyzer with the specified callback and TextOCR instance.
     *
     * @param callback The callback for handling OCR text detection results.
     * @param textOCR The TextOCR engine used to process image data and detect text.
     */
    public OCRAnalyzer(OCRAnalyzer.DetectionCallback callback, TextOCR textOCR) {
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

        if (!isAnalyzing) {
            image.close();
            return;
        }

        isAnalyzing = false; // Set to false to prevent re-entry

        Future<?> future = executorService.submit(() -> {
            try {
                Log.d(TAG, "Starting image analysis");
                Bitmap bitmap = CommonUtils.rotateBitmapIfNeeded(image);

                CompletableFuture<Word[]> futureTextWords = textOCR.detectWords(bitmap, executorService);

                futureTextWords.thenAccept(words -> {
                    callback.onDetectionTextResult(words);
                    isAnalyzing = true;
                    image.close();
                }).exceptionally(ex -> {
                    Log.e(TAG, "In non-enable grouping Exception occurred: " + ex.getMessage());
                    isAnalyzing = true;
                    image.close();
                    return null;
                });
            } catch (InvalidInputException e) {
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
