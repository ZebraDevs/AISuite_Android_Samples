// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.detectors.productrecognition;

import android.content.Context;
import android.util.Log;

import androidx.camera.core.ImageAnalysis;
import androidx.core.content.ContextCompat;

import com.zebra.ai.vision.analyzer.tracking.EntityTrackerAnalyzer;
import com.zebra.ai.vision.detector.BarcodeDecoder;
import com.zebra.ai.vision.detector.EntityType;
import com.zebra.ai.vision.detector.InferencerOptions;
import com.zebra.ai.vision.detector.ModuleRecognizer;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The ProductRecognitionHandler class manages the product recognition process using ModuleRecognizer.
 */
public class ProductRecognitionHandler {
    private static final String TAG = "ProductRecognitionHandler";
    private final ExecutorService executor;
    private final ExecutorService captureExecutor = Executors.newFixedThreadPool(3);
    private final Context context;
    private final ImageAnalysis imageAnalysis;
    private ProductRecognitionAnalyzer analyzer;
    private ModuleRecognizer moduleRecognizer; // For live preview
    private ModuleRecognizer captureRecognizer; // For capture mode
    private final String mavenModelName = "product-and-shelf-recognizer";
    private final String barcodeMavenModelName = "barcode-localizer";
    private final ModelLoadingCallback loadingCallback;
    private final ProductRecognitionAnalyzer.DetectionCallback callback;

    // Model input sizes
    private static final int LIVE_PREVIEW_SIZE = 640;
    private static final int CAPTURE_SIZE = 1280; // Higher resolution for capture
    String indexFilename = "product.index";
    String labelsFilename = "product.txt";
    String toPath;

    /**
     * Callback interface for model loading completion
     */
    public interface ModelLoadingCallback {
        void onLoadingComplete(boolean success);
    }

    /**
     * Constructs a new ProductRecognitionHandler.
     */
    public ProductRecognitionHandler(Context context, ProductRecognitionAnalyzer.DetectionCallback callback, ImageAnalysis imageAnalysis, ModelLoadingCallback loadingCallback) {
        this.context = context;
        this.callback = callback;
        this.imageAnalysis = imageAnalysis;
        this.executor = Executors.newFixedThreadPool(3);
        this.loadingCallback = loadingCallback;
        toPath = context.getFilesDir() + "/";
        copyFromAssets(indexFilename, toPath);
        copyFromAssets(labelsFilename, toPath);
        initializeModuleRecognizer();
        initializeCaptureRecognizer();
    }

    /**
     * Initializes the ModuleRecognizer with product recognition enabled.
     */
    private void initializeModuleRecognizer() {
        try {

            // Create settings for live preview
            ModuleRecognizer.Settings liveRecognizerSettings = createRecognizerSettings(LIVE_PREVIEW_SIZE, toPath, indexFilename, labelsFilename);

            // Call the helper function to create the recognizer with fallback logic
            createModuleRecognizerWithFallback(liveRecognizerSettings);

        } catch (Exception e) {
            // Notify failed loading
            if (loadingCallback != null) {
                loadingCallback.onLoadingComplete(false);
            }
            Log.e(TAG, "Fatal error during initialization setup: " + e.getMessage());
        }
    }

    /**
     * Initializes the capture recognizer with higher resolution settings
     */
    public void initializeCaptureRecognizer() {
        try {

            // Create settings for capture
            ModuleRecognizer.Settings captureRecognizerSettings = createRecognizerSettings(CAPTURE_SIZE, toPath, indexFilename, labelsFilename);
            createCaptureRecognizerWithFallback(captureRecognizerSettings);
        } catch (Exception ex) {
            if (loadingCallback != null) {
                loadingCallback.onLoadingComplete(false);
            }
            Log.e(TAG, "Capture recognizer initialization failed: " + ex.getMessage());
        }
    }

    private ModuleRecognizer.Settings createRecognizerSettings(int inputSize, String toPath, String indexFilename, String labelsFilename) {
        // Create settings with base model
        ModuleRecognizer.Settings settings = new ModuleRecognizer.Settings(mavenModelName);

        // Configure InferencerOptions
        settings.inferencerOptions.runtimeProcessorOrder = new Integer[]{
                InferencerOptions.DSP,
                InferencerOptions.CPU,
                InferencerOptions.GPU
        };
        settings.inferencerOptions.defaultDims.height = inputSize;
        settings.inferencerOptions.defaultDims.width = inputSize;

        // Enable product recognition with the same model and recognition data
        settings.enableProductRecognitionWithIndex(
                mavenModelName,
                toPath + indexFilename,
                toPath + labelsFilename
        );

        BarcodeDecoder.Settings labelBarcodeSettings = new BarcodeDecoder.Settings(barcodeMavenModelName);
        Map<EntityType, BarcodeDecoder.Settings> barcodeSettingsMap = new HashMap<>();
        barcodeSettingsMap.put(EntityType.LABEL, labelBarcodeSettings);
        settings.enableBarcodeRecognition(barcodeSettingsMap);

        return settings;
    }

    private void createModuleRecognizerWithFallback(ModuleRecognizer.Settings settings) {
        long startTime = System.currentTimeMillis();
        ModuleRecognizer.getModuleRecognizer(settings, executor)
                .thenAccept(recognizerInstance -> {
                    moduleRecognizer = recognizerInstance;
                    if(captureRecognizer!=null) {
                        if (loadingCallback != null) {
                            loadingCallback.onLoadingComplete(true);
                        }
                        attachAnalysisAfterModelLoading();
                    }
                    long creationTime = System.currentTimeMillis() - startTime;
                    Log.d(TAG, "ModuleRecognizer Creation Time: " + creationTime + "ms and input size: " + settings.inferencerOptions.defaultDims.width);
                })
                .exceptionally(throwable -> {
                    // Notify failed loading
                    if (loadingCallback != null) {
                        loadingCallback.onLoadingComplete(false);
                    }
                    Log.e(TAG, "Failed to initialize ModuleRecognizer: " + throwable.getMessage());
                    return null;
                });
    }

    private void createCaptureRecognizerWithFallback(ModuleRecognizer.Settings decoderSettings) {
        long m_Start = System.currentTimeMillis();
        ModuleRecognizer.getModuleRecognizer(decoderSettings, captureExecutor).thenAccept(decoderInstance -> {
            captureRecognizer = decoderInstance;
            if(moduleRecognizer!=null) {
                if (loadingCallback != null) {
                    loadingCallback.onLoadingComplete(true);
                }
                attachAnalysisAfterModelLoading();
            }
            Log.d(TAG, "Capture ModuleRecognizer created in " + (System.currentTimeMillis() - m_Start) + " ms");
        }).exceptionally(e -> {
            // Notify failed loading
            if (loadingCallback != null) {
                loadingCallback.onLoadingComplete(false);
            }
            Log.e(TAG, "Capture recognizer creation failed: " + e.getMessage());
            return null;
        });
    }


    /**
     * Copies files from the assets folder to the specified path.
     */
    private void copyFromAssets(String filename, String toPath) {
        final int bufferSize = 8192;
        try (InputStream stream = context.getAssets().open(filename);
             OutputStream fos = Files.newOutputStream(Paths.get(toPath + filename));
             BufferedOutputStream output = new BufferedOutputStream(fos)) {
            byte[] data = new byte[bufferSize];
            int count;
            while ((count = stream.read(data)) != -1) {
                output.write(data, 0, count);
            }
            output.flush();
        } catch (IOException e) {
            Log.e(TAG, "Error copying from assets: " + e.getMessage());
        }
    }

    /**
     * Retrieves the current instance of the ProductRecognitionAnalyzer.
     */
    public ProductRecognitionAnalyzer getProductRecognitionAnalyzer() {
        return analyzer;
    }

    /**
     * Retrieves the capture instance of the ModuleRecognizer.
     */
    public ModuleRecognizer getCaptureRecognizer() {
        return captureRecognizer;
    }

    /**
     * Stops the executor service and disposes of the ModuleRecognizer.
     */
    public void stop() {
        executor.shutdownNow();
        captureExecutor.shutdownNow();
        if (moduleRecognizer != null) {
            moduleRecognizer.dispose();
            Log.d(TAG, "ModuleRecognizer disposed");
            moduleRecognizer = null;
        }
        if (captureRecognizer != null) {
            captureRecognizer.dispose();
            Log.d(TAG, "Capture module recognizer disposed");
            captureRecognizer = null;
        }
    }

    public void attachAnalysisAfterModelLoading(){
        analyzer = new ProductRecognitionAnalyzer(callback, moduleRecognizer);
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), analyzer);
    }
}
