// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.detectors.barcodedecodersample;

import android.content.Context;
import android.util.Log;

import androidx.camera.core.ImageAnalysis;
import androidx.core.content.ContextCompat;

import com.zebra.ai.vision.detector.BarcodeDecoder;
import com.zebra.ai.vision.detector.InferencerOptions;
import com.zebra.aisuite_quickstart.java.detectors.textocrsample.TextOCRAnalyzer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BarcodeHandler {
    private static final String TAG = "BarcodeHandler";
    private BarcodeDecoder barcodeDecoder; // For live preview
    private BarcodeDecoder captureDecoder; // For capture mode
    private final ExecutorService executor;
    private final ExecutorService captureExecutor = Executors.newSingleThreadExecutor();
    private final Context context;
    private BarcodeAnalyzer barcodeAnalyzer;
    private final BarcodeAnalyzer.DetectionCallback callback;
    private final ImageAnalysis imageAnalysis;
    private final String mavenModelName = "barcode-localizer";
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

    public BarcodeHandler(Context context, BarcodeAnalyzer.DetectionCallback callback, ImageAnalysis imageAnalysis, ModelLoadingCallback loadingCallback) {
        this.context = context;
        this.callback = callback;
        this.executor = Executors.newSingleThreadExecutor();
        this.imageAnalysis = imageAnalysis;
        this.loadingCallback = loadingCallback;
        initializeBarcodeDecoder();
        initializeCaptureDecoder();
    }

    public void initializeBarcodeDecoder() {
        try {
            // Initialize live preview decoder with smaller input size
            BarcodeDecoder.Settings liveDecoderSettings = createDecoderSettings(LIVE_PREVIEW_SIZE);
            createBarcodeDecoderWithFallback(liveDecoderSettings);
        } catch (Exception ex) {
            if (loadingCallback != null) {
                loadingCallback.onLoadingComplete(false);
            }
            Log.e(TAG, "Model Loading: Barcode decoder returned with exception " + ex.getMessage());
        }
    }

    /**
     * Initializes the capture decoder with higher resolution settings
     */
    public void initializeCaptureDecoder() {
        try {
            BarcodeDecoder.Settings captureDecoderSettings = createDecoderSettings(CAPTURE_SIZE);
            createCaptureDecoderWithFallback(captureDecoderSettings);
        } catch (Exception ex) {
            if (loadingCallback != null) {
                loadingCallback.onLoadingComplete(false);
            }
            Log.e(TAG, "Capture decoder initialization failed: " + ex.getMessage());
        }
    }

    /**
     * Creates decoder settings with specified input size
     */
    private BarcodeDecoder.Settings createDecoderSettings(int inputSize) {
        BarcodeDecoder.Settings decoderSettings = new BarcodeDecoder.Settings(mavenModelName);
        Integer[] rpo = new Integer[3];
        rpo[0] = InferencerOptions.DSP;
        rpo[1] = InferencerOptions.CPU;
        rpo[2] = InferencerOptions.GPU;

        decoderSettings.Symbology.CODE39.enable(true);
        decoderSettings.Symbology.CODE128.enable(true);

        decoderSettings.detectorSetting.inferencerOptions.runtimeProcessorOrder = rpo;
        decoderSettings.detectorSetting.inferencerOptions.defaultDims.height = inputSize;
        decoderSettings.detectorSetting.inferencerOptions.defaultDims.width = inputSize;

        return decoderSettings;
    }

    private void createBarcodeDecoderWithFallback(BarcodeDecoder.Settings decoderSettings) {
        long m_Start = System.currentTimeMillis();
        BarcodeDecoder.getBarcodeDecoder(decoderSettings, executor).thenAccept(decoderInstance -> {
            barcodeDecoder = decoderInstance;
            if(captureDecoder!=null) {
                if (loadingCallback != null) {
                    loadingCallback.onLoadingComplete(true);
                }
                attachAnalysisAfterModelLoading();
            }
            Log.d(TAG, "BarcodeDecoder() obj creation time =" + (System.currentTimeMillis() - m_Start) + " milli sec and input size: " + decoderSettings.detectorSetting.inferencerOptions.defaultDims.width);
        }).exceptionally(e -> {
            if (loadingCallback != null) {
                loadingCallback.onLoadingComplete(false);
            }
            Log.e(TAG, "Fatal error: decoder creation failed - " + e.getMessage());
            return null;
        });
    }

    private void createCaptureDecoderWithFallback(BarcodeDecoder.Settings decoderSettings) {
        long m_Start = System.currentTimeMillis();
        BarcodeDecoder.getBarcodeDecoder(decoderSettings, captureExecutor).thenAccept(decoderInstance -> {
            captureDecoder = decoderInstance;
            if(barcodeDecoder!=null) {
                if (loadingCallback != null) {
                    loadingCallback.onLoadingComplete(true);
                }
                attachAnalysisAfterModelLoading();
            }
            Log.d(TAG, "Capture BarcodeDecoder created in " + (System.currentTimeMillis() - m_Start) + " ms");
        }).exceptionally(e -> {
            if (loadingCallback != null) {
                loadingCallback.onLoadingComplete(false);
            }
            Log.e(TAG, "Capture decoder creation failed: " + e.getMessage());
            return null;
        });
    }

    /**
     * Stops the executor service and disposes of both BarcodeDecoders
     */
    public void stop() {
        executor.shutdownNow();
        captureExecutor.shutdownNow();
        if (barcodeDecoder != null) {
            barcodeDecoder.dispose();
            Log.d(TAG, "Live preview barcode decoder disposed");
            barcodeDecoder = null;
        }
        if (captureDecoder != null) {
            captureDecoder.dispose();
            Log.d(TAG, "Capture barcode decoder disposed");
            captureDecoder = null;
        }
    }

    public BarcodeAnalyzer getBarcodeAnalyzer() {
        return barcodeAnalyzer;
    }

    public BarcodeDecoder getCaptureDecoder() {
        return captureDecoder;
    }

    public void attachAnalysisAfterModelLoading(){
        barcodeAnalyzer = new BarcodeAnalyzer(callback, barcodeDecoder);
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), barcodeAnalyzer);
    }
}