// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.detectors.textocrsample;

import android.content.Context;
import android.util.Log;

import androidx.camera.core.ImageAnalysis;
import androidx.core.content.ContextCompat;

import com.zebra.ai.vision.detector.InferencerOptions;
import com.zebra.ai.vision.detector.TextOCR;
import com.zebra.aisuite_quickstart.java.detectors.productrecognition.ProductRecognitionAnalyzer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OCRHandler {
    private static final String TAG = "OCRHandler";
    private TextOCR textOCR; // For live preview
    private TextOCR captureOCR; // For capture mode
    private final ExecutorService executor;
    private final ExecutorService captureExecutor = Executors.newSingleThreadExecutor();
    private final Context context;
    private final TextOCRAnalyzer.DetectionCallback callback;
    private final ImageAnalysis imageAnalysis;
    private TextOCRAnalyzer ocrAnalyzer;
    private final String mavenModelName = "text-ocr-recognizer";
    private final ModelLoadingCallback loadingCallback;

    // Model input sizes
    private static final int LIVE_PREVIEW_SIZE = 640;
    private static final int CAPTURE_SIZE = 1280; // Higher resolution for capture

    /**
     * Callback interface for model loading completion
     */
    public interface ModelLoadingCallback {
        void onLoadingComplete(boolean success);
    }

    public OCRHandler(Context context, TextOCRAnalyzer.DetectionCallback callback, ImageAnalysis imageAnalysis, ModelLoadingCallback loadingCallback) {
        this.context = context;
        this.callback = callback;
        this.executor = Executors.newSingleThreadExecutor();
        this.imageAnalysis = imageAnalysis;
        this.loadingCallback = loadingCallback;
        initializeTextOCR();
        initializeCaptureOCR();
    }

    private void initializeTextOCR() {
        try {
            // Initialize live preview OCR with smaller input size
            TextOCR.Settings liveOCRSettings = createOCRSettings(LIVE_PREVIEW_SIZE);
            createTextOCRWithFallback(liveOCRSettings);
        } catch (Exception e) {
            if (loadingCallback != null) {
                loadingCallback.onLoadingComplete(false);
            }
            Log.e(TAG, "Fatal error: load failed - " + e.getMessage());
        }
    }

    /**
     * Initializes the capture OCR with higher resolution settings
     */
    public void initializeCaptureOCR() {
        try {
            TextOCR.Settings captureOCRSettings = createOCRSettings(CAPTURE_SIZE);
            createCaptureOCRWithFallback(captureOCRSettings);
        } catch (Exception ex) {
            if (loadingCallback != null) {
                loadingCallback.onLoadingComplete(false);
            }
            Log.e(TAG, "Capture OCR initialization failed: " + ex.getMessage());
        }
    }

    /**
     * Creates OCR settings with specified input size
     */
    private TextOCR.Settings createOCRSettings(int inputSize) {
        TextOCR.Settings textOCRSettings = new TextOCR.Settings(mavenModelName);

        Integer[] rpo = new Integer[3];
        rpo[0] = InferencerOptions.DSP;
        rpo[1] = InferencerOptions.CPU;
        rpo[2] = InferencerOptions.GPU;

        textOCRSettings.detectionInferencerOptions.runtimeProcessorOrder = rpo;
        textOCRSettings.recognitionInferencerOptions.runtimeProcessorOrder = rpo;
        textOCRSettings.detectionInferencerOptions.defaultDims.height = inputSize;
        textOCRSettings.detectionInferencerOptions.defaultDims.width = inputSize;


        return textOCRSettings;
    }

    private void createTextOCRWithFallback(TextOCR.Settings textOCRSettings) {
        long m_Start = System.currentTimeMillis();
        TextOCR.getTextOCR(textOCRSettings, executor).thenAccept(OCRInstance -> {
            textOCR = OCRInstance;
            if(captureOCR!=null) {
                if (loadingCallback != null) {
                    loadingCallback.onLoadingComplete(true);
                }
                attachAnalysisAfterModelLoading();
            }

            Log.d(TAG, "TextOCR() obj creation / model loading time = " + (System.currentTimeMillis() - m_Start) + " milli sec and input size: " + textOCRSettings.detectionInferencerOptions.defaultDims.width);
        }).exceptionally(e -> {

            if (loadingCallback != null) {
                loadingCallback.onLoadingComplete(false);
            }
            Log.e(TAG, "Fatal error: TextOCR creation failed - " + e.getMessage());
            return null;
        });
    }

    private void createCaptureOCRWithFallback(TextOCR.Settings textOCRSettings) {
        long m_Start = System.currentTimeMillis();
        TextOCR.getTextOCR(textOCRSettings, captureExecutor).thenAccept(OCRInstance -> {
            captureOCR = OCRInstance;
            if(textOCR!=null) {
                if (loadingCallback != null) {
                    loadingCallback.onLoadingComplete(true);
                }
                attachAnalysisAfterModelLoading();
            }
            Log.d(TAG, "Capture TextOCR created in " + (System.currentTimeMillis() - m_Start) + " ms");
        }).exceptionally(e -> {
            if (loadingCallback != null) {
                loadingCallback.onLoadingComplete(false);
            }
            Log.e(TAG, "Capture OCR creation failed: " + e.getMessage());
            return null;
        });
    }


    /**
     * Stops the executor service and disposes of both TextOCR instances
     */
    public void stop() {
        executor.shutdownNow();
        captureExecutor.shutdownNow();
        if (textOCR != null) {
            textOCR.dispose();
            Log.v(TAG, "Live preview OCR is disposed");
            textOCR = null;
        }
        if (captureOCR != null) {
            captureOCR.dispose();
            Log.v(TAG, "Capture OCR is disposed");
            captureOCR = null;
        }
    }

    public TextOCRAnalyzer getOCRAnalyzer() {
        return ocrAnalyzer;
    }

    public TextOCR getCaptureOCR() {
        return captureOCR;
    }

    public void attachAnalysisAfterModelLoading(){
        ocrAnalyzer = new TextOCRAnalyzer(callback, textOCR);
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), ocrAnalyzer);
    }
}