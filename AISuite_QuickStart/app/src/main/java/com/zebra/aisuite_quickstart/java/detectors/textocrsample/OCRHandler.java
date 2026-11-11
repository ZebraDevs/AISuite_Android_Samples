// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.detectors.textocrsample;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import com.zebra.aisuite_quickstart.utils.AppLog;

import androidx.camera.core.ImageAnalysis;
import androidx.core.content.ContextCompat;

import com.zebra.ai.vision.detector.InferencerOptions;
import com.zebra.ai.vision.detector.TextOCR;
import com.zebra.aisuite_quickstart.utils.CommonUtils;

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
    private static final int CAPTURE_SIZE = 1280; // Higher resolution for capture
    private final SharedPreferences sharedPreferences;
    private boolean isPickListEnabled = false;

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
        this.sharedPreferences = context.getSharedPreferences(CommonUtils.SETTINGS_PREFS, MODE_PRIVATE);
        isPickListEnabled = sharedPreferences.getBoolean(CommonUtils.PREF_TEXT_OCR_PICKLIST_ENABLED, false);
        initializeTextOCR();
        initializeCaptureOCR();
    }

    private void initializeTextOCR() {
        try {
            // Initialize live preview OCR with selected model resolution
            TextOCR.Settings liveOCRSettings = createLiveOCRSettings();
            createTextOCR(liveOCRSettings);
        } catch (Exception e) {
            if (loadingCallback != null) {
                loadingCallback.onLoadingComplete(false);
            }
            AppLog.e(TAG, "Fatal error: load failed - " + e.getMessage());
        }
    }

    /**
     * Initializes the capture OCR with higher resolution settings
     */
    public void initializeCaptureOCR() {
        try {
            TextOCR.Settings captureOCRSettings = createCaptureOCRSettings();
            createCaptureOCR(captureOCRSettings);
        } catch (Exception ex) {
            if (loadingCallback != null) {
                loadingCallback.onLoadingComplete(false);
            }
            AppLog.e(TAG, "Capture OCR initialization failed: " + ex.getMessage());
        }
    }

    /**
     * Creates live preview OCR settings using the model resolution selected in Settings
     */
    private TextOCR.Settings createLiveOCRSettings() {
        TextOCR.Settings textOCRSettings = createBaseOCRSettings();
        textOCRSettings.detectionInferencerOptions.setModelResolution(CommonUtils.getModelResolution(sharedPreferences));
        return textOCRSettings;
    }

    /**
     * Creates capture OCR settings with specified input size
     */
    private TextOCR.Settings createCaptureOCRSettings() {
        TextOCR.Settings textOCRSettings = createBaseOCRSettings();
        textOCRSettings.detectionInferencerOptions.setModelResolution(new InferencerOptions.Resolution(1280,1280));
        return textOCRSettings;
    }

    private TextOCR.Settings createBaseOCRSettings() {
        TextOCR.Settings textOCRSettings = new TextOCR.Settings(mavenModelName);

        Integer[] rpo = new Integer[3];
        rpo[0] = InferencerOptions.DSP;
        rpo[1] = InferencerOptions.CPU;
        rpo[2] = InferencerOptions.GPU;

        textOCRSettings.detectionInferencerOptions.runtimeProcessorOrder = rpo;
        textOCRSettings.recognitionInferencerOptions.runtimeProcessorOrder = rpo;
        textOCRSettings.unclipRatio = 0.6f;
        textOCRSettings.picklistConfig.setOCRPicklist(isPickListEnabled);

        return textOCRSettings;
    }

    private void createTextOCR(TextOCR.Settings textOCRSettings) {
        long m_Start = System.currentTimeMillis();
        TextOCR.getTextOCR(textOCRSettings, executor).thenAccept(OCRInstance -> {
            textOCR = OCRInstance;
            if (captureOCR != null) {
                if (loadingCallback != null) {
                    loadingCallback.onLoadingComplete(true);
                }
                attachAnalysisAfterModelLoading();
            }

            AppLog.i(TAG, "TextOCR model loading time: " + (System.currentTimeMillis() - m_Start) + " milli sec and model resolution: " + CommonUtils.getModelResolution(sharedPreferences));
        }).exceptionally(e -> {
            if (loadingCallback != null) {
                loadingCallback.onLoadingComplete(false);
            }
            AppLog.e(TAG, "TextOCR model loading failed - " + e.getMessage());
            return null;
        });
    }

    private void createCaptureOCR(TextOCR.Settings textOCRSettings) {
        long m_Start = System.currentTimeMillis();
        TextOCR.getTextOCR(textOCRSettings, captureExecutor).thenAccept(OCRInstance -> {
            captureOCR = OCRInstance;
            if (textOCR != null) {
                if (loadingCallback != null) {
                    loadingCallback.onLoadingComplete(true);
                }
                attachAnalysisAfterModelLoading();
            }
            AppLog.i(TAG, "Capture TextOCR model loading time: " + (System.currentTimeMillis() - m_Start) + " milli sec and input size: "+ textOCRSettings.detectionInferencerOptions.defaultDims.width);
        }).exceptionally(e -> {
            if (loadingCallback != null) {
                loadingCallback.onLoadingComplete(false);
            }
            AppLog.e(TAG, "Capture TextOCR model loading failed: " + e.getMessage());
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
            AppLog.i(TAG, "Live preview OCR is disposed");
            textOCR = null;
        }
        if (captureOCR != null) {
            captureOCR.dispose();
            AppLog.i(TAG, "Capture OCR is disposed");
            captureOCR = null;
        }
    }

    public TextOCRAnalyzer getOCRAnalyzer() {
        return ocrAnalyzer;
    }

    public TextOCR getCaptureOCR() {
        return captureOCR;
    }

    public void attachAnalysisAfterModelLoading() {
        ocrAnalyzer = new TextOCRAnalyzer(callback, textOCR);
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), ocrAnalyzer);
    }
}