// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.lowlevel.productrecognitionsample;

import android.content.Context;
import android.util.Log;

import androidx.camera.core.ImageAnalysis;
import androidx.core.content.ContextCompat;

import com.zebra.ai.vision.detector.AIVisionSDKLicenseException;
import com.zebra.ai.vision.detector.FeatureExtractor;
import com.zebra.ai.vision.detector.InferencerOptions;
import com.zebra.ai.vision.detector.Localizer;
import com.zebra.ai.vision.detector.Recognizer;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductRecognitionSample {
    private static final String TAG = "ProductRecognitionSample";
    private Localizer localizer;
    private FeatureExtractor featureExtractor;
    private Recognizer recognizer;
    private final ExecutorService executor;
    private final Context context;
    private final ImageAnalysis imageAnalysis;
    private boolean localizerInitialized = false;
    private boolean featureExtractorInitialized = false;
    private boolean recognizerInitialized = false;
    private ProductRecognitionSampleAnalyzer analyzer;
    private final String mavenModelName = "product-and-shelf-recognizer";

    /**
     * Constructs a new ProductRecognitionSample with the specified context, callback, and image analysis configuration.
     *
     * @param context       The Android context for resource management.
     * @param callback      The callback for handling recognition results.
     * @param imageAnalysis The image analysis configuration for processing image data.
     *                      Constructs a new ProductRecognitionHandler.
     */
    public ProductRecognitionSample(Context context, ProductRecognitionSampleAnalyzer.SampleDetectionCallback callback, ImageAnalysis imageAnalysis) {
        this.context = context;
        this.imageAnalysis = imageAnalysis;
        this.executor = Executors.newFixedThreadPool(3); // Create a thread pool for parallel execution
        initializeProductRecognition(callback);
    }

    /**
     * Initializes the product recognition components including the localizer, feature extractor,
     * and recognizer. This method sets up the necessary components for detecting and recognizing
     * products within image data.
     *
     * @param callback The callback for handling recognition results.
     */
    private void initializeProductRecognition(ProductRecognitionSampleAnalyzer.SampleDetectionCallback callback) {
        try {
            Localizer.Settings locSettings = new Localizer.Settings(mavenModelName);
            FeatureExtractor.Settings feSettings = new FeatureExtractor.Settings(mavenModelName);

            Integer[] rpo = new Integer[3];
            rpo[0] = InferencerOptions.DSP;
            rpo[1] = InferencerOptions.CPU;
            rpo[2] = InferencerOptions.GPU;
            feSettings.inferencerOptions.runtimeProcessorOrder = rpo;
            locSettings.inferencerOptions.runtimeProcessorOrder = rpo;
            locSettings.inferencerOptions.defaultDims.height = 640;
            locSettings.inferencerOptions.defaultDims.width = 640;
            // Copy assets
            String indexFilename = "product.index";
            String labelsFilename = "product.txt";
            String toPath = context.getFilesDir() + "/";
            copyFromAssets(indexFilename, toPath);
            copyFromAssets(labelsFilename, toPath);

            Recognizer.SettingsIndex reSettings = new Recognizer.SettingsIndex();
            reSettings.indexFilename = toPath + indexFilename;
            reSettings.labelFilename = toPath + labelsFilename;
            long m_Start = System.currentTimeMillis();

            CompletableFuture<Void> localizerFuture = Localizer.getLocalizer(locSettings, executor)
                    .thenAccept(localizerInstance -> {
                        Log.d(TAG, "Shelf Localizer(locSettings) obj creation / model loading time =" + (System.currentTimeMillis() - m_Start) + " milli sec");
                        localizerInitialized = true;
                        localizer = localizerInstance;
                        tryInitializeProductRecognition(callback);
                    }).exceptionally(e -> {
                        if (e instanceof AIVisionSDKLicenseException) {
                            Log.e(TAG, "AIVisionSDKLicenseException: Shelf Localizer object creation failed, " + e.getMessage());
                        } else {
                            Log.e(TAG, "Localizer load failed: " + e.getMessage());
                        }
                        return null;
                    });

            long mstart = System.currentTimeMillis();
            CompletableFuture<Void> extractorFuture = FeatureExtractor.getFeatureExtractor(feSettings, executor)
                    .thenAccept(featureExtractorInstance -> {
                        Log.d(TAG, "FeatureExtractor() obj creation time =" + (System.currentTimeMillis() - mstart) + " milli sec");
                        featureExtractorInitialized = true;
                        featureExtractor = featureExtractorInstance;
                        tryInitializeProductRecognition(callback);
                    }).exceptionally(e -> {
                        if (e instanceof AIVisionSDKLicenseException) {
                            Log.e(TAG, "AIVisionSDKLicenseException: Feature Extractor object creation failed, " + e.getMessage());
                        } else {
                            Log.e(TAG, "FeatureExtractor creation failed: " + e.getMessage());
                        }
                        return null;
                    });

            long mStartRecognizer = System.currentTimeMillis();
            CompletableFuture<Void> recognizerFuture = Recognizer.getRecognizer(reSettings, executor)
                    .thenAccept(recognizerInstance -> {
                        Log.d(TAG, "Recognizer(reSettings) obj creation time =" + (System.currentTimeMillis() - mStartRecognizer) + " milli sec");
                        recognizerInitialized = true;
                        recognizer = recognizerInstance;
                        tryInitializeProductRecognition(callback);
                    }).exceptionally(e -> {
                        Log.e(TAG, "Recognizer creation failed: " + e.getMessage());
                        return null;
                    });

            CompletableFuture.allOf(localizerFuture, extractorFuture, recognizerFuture).join();

        } catch (Exception e) {
            Log.e(TAG, "Fatal error during initialization: " + e.getMessage());
        }
    }

    /**
     * Attempts to initialize the ProductRecognitionAnalyzer once all components are initialized.
     *
     * @param callback The callback for handling recognition results.
     */
    private synchronized void tryInitializeProductRecognition(ProductRecognitionSampleAnalyzer.SampleDetectionCallback callback) {
        if (localizerInitialized && featureExtractorInitialized && recognizerInitialized) {
            analyzer = new ProductRecognitionSampleAnalyzer(callback, localizer, featureExtractor, recognizer);
            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), analyzer);
        }
    }

    /**
     * Copies files from the assets folder to the specified path.
     *
     * @param filename The name of the file to copy.
     * @param toPath   The destination path.
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
            Log.e(TAG, "Error in copy from assets: " + e.getMessage());
        }
    }

    public ProductRecognitionSampleAnalyzer getProductRecognitionSampleAnalyzer() {
        return analyzer;
    }

    public void stop() {
        executor.shutdownNow();
        if (localizer != null) {
            localizer.dispose();
            Log.d(TAG, "Localizer is disposed");
            localizer = null;
        }
        if (featureExtractor != null) {
            featureExtractor.dispose();
            Log.d(TAG, "Feature extractor is disposed");
            featureExtractor = null;
        }
        if (recognizer != null) {
            recognizer.dispose();
            Log.d(TAG, "Recognizer is disposed");
            recognizer = null;
        }
    }
}

