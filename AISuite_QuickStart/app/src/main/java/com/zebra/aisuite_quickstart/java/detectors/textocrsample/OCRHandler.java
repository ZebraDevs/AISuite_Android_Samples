// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.detectors.textocrsample;

import android.content.Context;
import android.util.Log;

import androidx.camera.core.ImageAnalysis;
import androidx.core.content.ContextCompat;

import com.zebra.ai.vision.detector.AIVisionSDKLicenseException;
import com.zebra.ai.vision.detector.InferencerOptions;
import com.zebra.ai.vision.detector.TextOCR;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The OCRHandler class is responsible for setting up and managing the optical character
 * recognition (OCR) process using a TextOCR and TextOCRAnalyzer. This class initializes
 * the necessary components for text detection and recognition, and manages the lifecycle
 * of the OCR process within an Android application context.
 *
 * The OCRHandler configures the OCR model, assigns an analyzer for image analysis, and
 * provides methods to stop and dispose of resources when they are no longer needed.
 *
 * Usage:
 * - Instantiate the OCRHandler with the appropriate context, callback, and image analysis configuration.
 * - Call initializeTextOCR() to set up the OCR engine and analyzer.
 * - Use getOCRAnalyzer() to retrieve the current instance of the TextOCRAnalyzer.
 * - Call stop() to terminate the executor service and dispose of the TextOCR when finished.
 *
 * Dependencies:
 * - Android Context: Required for managing resources and executing tasks on the main thread.
 * - ExecutorService: Used for asynchronous task execution.
 * - ImageAnalysis: Provides the framework for analyzing image data.
 * - TextOCR: Handles the OCR processing to recognize text in images.
 * - TextOCRAnalyzer: Analyzes images to detect text and report results via a callback.
 *
 * Exception Handling:
 * - Handles AIVisionSDKLicenseException and generic exceptions during OCR initialization.
 * - Logs any other exceptions encountered during the setup process.
 *
 * Note: Ensure that the appropriate permissions and dependencies are configured
 * in the AndroidManifest and build files to utilize camera and image processing capabilities.
 */
public class OCRHandler {
    private static final String TAG = "OCRHandler";
    private TextOCR textOCR;
    private final ExecutorService executor;
    private final Context context;
    private final TextOCRAnalyzer.DetectionCallback callback;
    private final ImageAnalysis imageAnalysis;
    private TextOCRAnalyzer ocrAnalyzer;
    private String mavenModelName = "text-ocr-recognizer";

    /**
     * Constructs a new OCRHandler with the specified context, callback, and image analysis configuration.
     *
     * @param context The Android context for resource management.
     * @param callback The callback for handling detection results.
     * @param imageAnalysis The image analysis configuration for processing image data.
     */
    public OCRHandler(Context context, TextOCRAnalyzer.DetectionCallback callback, ImageAnalysis imageAnalysis) {
        this.context = context;
        this.callback = callback;
        this.executor = Executors.newSingleThreadExecutor();
        this.imageAnalysis = imageAnalysis;
        initializeTextOCR();
    }

    /**
     * Initializes the TextOCR with predefined settings for text detection and recognition.
     * This method sets up the necessary components for analyzing and recognizing text from
     * image data.
     */
    private void initializeTextOCR() {
        try {
            TextOCR.Settings textOCRSettings = new TextOCR.Settings(mavenModelName);

            Integer[] rpo = new Integer[1];
            rpo[0] = InferencerOptions.DSP;

            textOCRSettings.detectionInferencerOptions.runtimeProcessorOrder = rpo;
            textOCRSettings.recognitionInferencerOptions.runtimeProcessorOrder = rpo;
            textOCRSettings.detectionInferencerOptions.defaultDims.height = 640;
            textOCRSettings.detectionInferencerOptions.defaultDims.width = 640;

            long m_Start = System.currentTimeMillis();
            TextOCR.getTextOCR(textOCRSettings, executor).thenAccept(OCRInstance -> {
                textOCR = OCRInstance;
                ocrAnalyzer = new TextOCRAnalyzer(callback, textOCR);
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), ocrAnalyzer);

                Log.d(TAG, "TextOCR() obj creation / model loading time = " + (System.currentTimeMillis() - m_Start) + " milli sec");
            }).exceptionally(e -> {
                if (e instanceof AIVisionSDKLicenseException) {
                    Log.e(TAG, "AIVisionSDKLicenseException: TextOCR object creation failed, " + e.getMessage());
                } else {
                    Log.e(TAG, "Fatal error: TextOCR creation failed - " + e.getMessage());
                }
                return null;
            });
        } catch (Exception e) {
            Log.e(TAG, "Fatal error: load failed - " + e.getMessage());
        }
    }

    /**
     * Stops the executor service and disposes of the TextOCR, releasing any resources held.
     * This method should be called when text recognition is no longer needed.
     */
    public void stop() {
        executor.shutdownNow();
        if (textOCR != null) {
            textOCR.dispose();
            Log.v(TAG, "OCR is disposed ");
            textOCR = null;
        }
    }

    /**
     * Retrieves the current instance of the TextOCRAnalyzer.
     *
     * @return The TextOCRAnalyzer instance, or null if not yet initialized.
     */
    public TextOCRAnalyzer getOCRAnalyzer() {
        return ocrAnalyzer;
    }
}
