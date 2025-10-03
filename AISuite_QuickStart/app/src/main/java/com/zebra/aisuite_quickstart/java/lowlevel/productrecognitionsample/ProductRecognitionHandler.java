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

/**
 * The ProductRecognitionHandler class is responsible for initializing and managing the product
 * recognition process, which involves detecting products and shelves, extracting features, and
 * recognizing products using a localizer, feature extractor, and recognizer. This class sets up
 * the necessary components and manages their lifecycle within an Android application context.
 *
 * The ProductRecognitionHandler configures models, assigns an analyzer for image analysis, and
 * provides methods to stop and dispose of resources when they are no longer needed.
 *
 * Usage:
 * - Instantiate the ProductRecognitionHandler with the appropriate context, callback, and image analysis configuration.
 * - Call initializeProductRecognition() to set up the product recognition components.
 * - Use getProductRecognitionAnalyzer() to retrieve the current instance of the ProductRecognitionAnalyzer.
 * - Call stop() to terminate the executor service and dispose of the recognition components when finished.
 *
 * Dependencies:
 * - Android Context: Required for resource management and accessing application assets.
 * - ExecutorService: Used for parallel task execution.
 * - ImageAnalysis: Provides the framework for analyzing image data.
 * - Localizer: Detects objects within an image.
 * - FeatureExtractor: Generates feature descriptors for detected objects.
 * - Recognizer: Matches feature descriptors to known products.
 *
 * Exception Handling:
 * - Handles AIVisionSDKLicenseException and generic exceptions during initialization.
 * - Logs any other exceptions encountered during the setup process.
 *
 * Note: Ensure that the appropriate permissions and dependencies are configured
 * in the AndroidManifest and build files to utilize camera and image processing capabilities.
 */
public class ProductRecognitionHandler {
    private static final String TAG = "ProductRecognitionHandler";
    private Localizer localizer;
    private FeatureExtractor featureExtractor;
    private Recognizer recognizer;
    private final ExecutorService executor;
    private final Context context;
    private final ImageAnalysis imageAnalysis;
    private boolean localizerInitialized = false;
    private boolean featureExtractorInitialized = false;
    private boolean recognizerInitialized = false;
    private ProductRecognitionAnalyzer analyzer;
    private String mavenModelName = "product-and-shelf-recognizer";

    /**
     * Constructs a new ProductRecognitionHandler with the specified context, callback, and image analysis configuration.
     *
     * @param context The Android context for resource management.
     * @param callback The callback for handling recognition results.
     * @param imageAnalysis The image analysis configuration for processing image data.
     */
    public ProductRecognitionHandler(Context context, ProductRecognitionAnalyzer.DetectionCallback callback, ImageAnalysis imageAnalysis) {
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
    private void initializeProductRecognition(ProductRecognitionAnalyzer.DetectionCallback callback) {
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
    private synchronized void tryInitializeProductRecognition(ProductRecognitionAnalyzer.DetectionCallback callback) {
        if (localizerInitialized && featureExtractorInitialized && recognizerInitialized) {
            analyzer = new ProductRecognitionAnalyzer(callback, localizer, featureExtractor, recognizer);
            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), analyzer);
        }
    }

    /**
     * Copies files from the assets folder to the specified path.
     *
     * @param filename The name of the file to copy.
     * @param toPath The destination path.
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

    /**
     * Retrieves the current instance of the ProductRecognitionAnalyzer.
     *
     * @return The ProductRecognitionAnalyzer instance, or null if not yet initialized.
     */
    public ProductRecognitionAnalyzer getProductRecognitionAnalyzer() {
        return analyzer;
    }

    /**
     * Stops the executor service and disposes of the localizer, feature extractor, and recognizer,
     * releasing any resources held. This method should be called when product recognition is no
     * longer needed.
     */
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

