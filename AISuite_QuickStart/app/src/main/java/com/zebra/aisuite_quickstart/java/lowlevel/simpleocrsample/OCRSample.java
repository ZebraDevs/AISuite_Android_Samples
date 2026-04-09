// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.lowlevel.simpleocrsample;

import android.content.Context;
import android.util.Log;

import androidx.camera.core.ImageAnalysis;
import androidx.core.content.ContextCompat;

import com.zebra.ai.vision.detector.AIVisionSDKLicenseException;
import com.zebra.ai.vision.detector.InferencerOptions;
import com.zebra.ai.vision.detector.TextOCR;
import com.zebra.aisuite_quickstart.java.lowlevel.simplebarcodesample.BarcodeSample;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The OCRSample class implements the OCRAnalyzer.DetectionCallback interface and is responsible
 * for setting up and managing the OCR (Optical Character Recognition) process. It uses the TextOCR
 * engine to recognize text in images and provides a mechanism to handle the results of OCR detection.
 *
 * This class initializes the TextOCR component, configures the analysis process, and manages the lifecycle
 * of the OCR engine within an Android application context.
 *
 * Usage:
 * - Instantiate the OCRSample with the appropriate context, callback, and image analysis configuration.
 * - Call initializeTextOCR() to set up the TextOCR engine for text detection and recognition.
 * - Use getTextOCR() to retrieve the current instance of the TextOCR engine.
 * - Call stop() to dispose of the TextOCR engine and release resources when finished.
 *
 * Dependencies:
 * - Android Context: Required for resource management and executing tasks on the main thread.
 * - ExecutorService: Used for asynchronous task execution.
 * - ImageAnalysis: Provides the framework for analyzing image data.
 * - TextOCR: Handles the detection and recognition of text within images.
 *
 * Exception Handling:
 * - Handles AIVisionSDKLicenseException and generic exceptions during initialization.
 * - Logs any other exceptions encountered during the setup process.
 *
 * Note: Ensure that the appropriate permissions and dependencies are configured
 * in the AndroidManifest and build files to utilize camera and image processing capabilities.
 */
public class OCRSample {
    private static final String TAG = "OCRSample";
    private TextOCR textOCR;
    private final ExecutorService executor;
    private final Context context;
    private final OCRAnalyzer.DetectionCallback callback;
    private final ImageAnalysis imageAnalysis;
    private OCRAnalyzer ocrAnalyzer;
    private final String mavenModelName = "text-ocr-recognizer";
    private final ModelLoadingCallback loadingCallback;

    /**
     * Callback interface for model loading completion
     */
    public interface ModelLoadingCallback {
        void onLoadingComplete(boolean success);
    }

    /**
     * Constructs a new OCRSample with the specified context, callback, and image analysis configuration.
     *
     * @param context The Android context for resource management.
     * @param callback The callback for handling OCR text detection results.
     * @param imageAnalysis The image analysis configuration for processing image data.
     */
    public OCRSample(Context context, OCRAnalyzer.DetectionCallback callback, ImageAnalysis imageAnalysis, ModelLoadingCallback loadingCallback) {
        this.context = context;
        this.callback = callback;
        this.executor = Executors.newSingleThreadExecutor();
        this.imageAnalysis = imageAnalysis;
        this.loadingCallback = loadingCallback;
        initializeTextOCR();
    }

    /**
     * Initializes the TextOCR engine with predefined settings for text detection and recognition.
     * This method sets up the necessary components for analyzing and recognizing text from image data.
     */
    private void initializeTextOCR() {
        try {
            TextOCR.Settings textOCRSettings = new TextOCR.Settings(mavenModelName);
            Integer[] rpo = new Integer[3];
            rpo[0] = InferencerOptions.DSP;
            rpo[1] = InferencerOptions.CPU;
            rpo[2] = InferencerOptions.GPU;

            textOCRSettings.detectionInferencerOptions.runtimeProcessorOrder = rpo;
            textOCRSettings.recognitionInferencerOptions.runtimeProcessorOrder = rpo;
            textOCRSettings.detectionInferencerOptions.defaultDims.height = 640;
            textOCRSettings.detectionInferencerOptions.defaultDims.width = 640;

            long m_Start = System.currentTimeMillis();
            TextOCR.getTextOCR(textOCRSettings, executor).thenAccept(OCRInstance -> {
                textOCR = OCRInstance;
                Log.d(TAG, "TextOCR() obj creation / model loading time = " + (System.currentTimeMillis() - m_Start) + " milli sec");

                // Notify successful loading
                if (loadingCallback != null) {
                    loadingCallback.onLoadingComplete(true);
                }
                 ocrAnalyzer = new OCRAnalyzer(callback, textOCR);
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), ocrAnalyzer);
            }).exceptionally(e -> {
                if (e instanceof AIVisionSDKLicenseException) {
                    Log.e(TAG, "AIVisionSDKLicenseException: TextOCR object creation failed, " + e.getMessage());
                } else {
                    Log.e(TAG, "Fatal error: TextOCR creation failed - " + e.getMessage());
                }
                // Notify failed loading
                if (loadingCallback != null) {
                    loadingCallback.onLoadingComplete(false);
                }
                return null;
            });
        } catch (Exception e) {
            // Notify failed loading
            if (loadingCallback != null) {
                loadingCallback.onLoadingComplete(false);
            }
            Log.e(TAG, "Fatal error: load failed - " + e.getMessage());
        }
    }

    /**
     * Disposes of the TextOCR engine, releasing any resources held. This method should be
     * called when OCR processing is no longer needed.
     */
    public void stop() {
        if (textOCR != null) {
            textOCR.dispose();
            Log.d(TAG, "OCR is disposed");
            textOCR = null;
        }
    }

    /**
     * Retrieves the current instance of the TextOCRAnalyzer.
     *
     * @return The TextOCRAnalyzer instance, or null if not yet initialized.
     */
    public OCRAnalyzer getOCRAnalyzer() {
        return ocrAnalyzer;
    }

}
