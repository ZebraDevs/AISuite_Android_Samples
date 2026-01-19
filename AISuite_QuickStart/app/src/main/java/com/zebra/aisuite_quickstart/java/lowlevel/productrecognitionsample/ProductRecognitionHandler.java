// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.lowlevel.productrecognitionsample;

import android.content.Context;
import android.util.Log;

import androidx.camera.core.ImageAnalysis;
import androidx.core.content.ContextCompat;

import com.zebra.ai.vision.detector.InferencerOptions;
import com.zebra.ai.vision.detector.ModuleRecognizer;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The ProductRecognitionHandler class manages the product recognition process using ModuleRecognizer.
 */
public class ProductRecognitionHandler {
    private static final String TAG = "ProductRecognitionHandler";
    private final ExecutorService executor;
    private final Context context;
    private final ImageAnalysis imageAnalysis;
    private ProductRecognitionAnalyzer analyzer;
    private ModuleRecognizer moduleRecognizer;
    private final String mavenModelName = "product-and-shelf-recognizer";

    /**
     * Constructs a new ProductRecognitionHandler.
     */
    public ProductRecognitionHandler(Context context, ProductRecognitionAnalyzer.DetectionCallback callback, ImageAnalysis imageAnalysis) {
        this.context = context;
        this.imageAnalysis = imageAnalysis;
        this.executor = Executors.newFixedThreadPool(3);
        initializeModuleRecognizer(callback);
    }

    /**
     * Initializes the ModuleRecognizer with product recognition enabled.
     */
    private void initializeModuleRecognizer(ProductRecognitionAnalyzer.DetectionCallback callback) {
        try {
            // Copy assets
            String indexFilename = "product.index";
            String labelsFilename = "product.txt";
            String toPath = context.getFilesDir() + "/";
            copyFromAssets(indexFilename, toPath);
            copyFromAssets(labelsFilename, toPath);

            // Create settings with base model
            ModuleRecognizer.Settings settings = new ModuleRecognizer.Settings(mavenModelName);

            // Configure InferencerOptions
            Integer[] rpo = new Integer[]{
                    InferencerOptions.DSP,
                    InferencerOptions.CPU,
                    InferencerOptions.GPU
            };
            settings.inferencerOptions.runtimeProcessorOrder = rpo;
            settings.inferencerOptions.defaultDims.height = 640;
            settings.inferencerOptions.defaultDims.width = 640;

            // Enable product recognition with the same model and recognition data
            settings.enableProductRecognitionWithIndex(
                    mavenModelName,
                    toPath + indexFilename,
                    toPath + labelsFilename
            );

            // Initialize ModuleRecognizer
            long startTime = System.currentTimeMillis();
            ModuleRecognizer.getModuleRecognizer(settings, executor)
                    .thenAccept(recognizerInstance -> {
                        long creationTime = System.currentTimeMillis() - startTime;
                        Log.d(TAG, "ModuleRecognizer Creation Time: " + creationTime + "ms");
                        Log.i(TAG, "ModuleRecognizer instance created successfully");

                        moduleRecognizer = recognizerInstance;
                        analyzer = new ProductRecognitionAnalyzer(callback, moduleRecognizer);
                        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), analyzer);
                    })
                    .exceptionally(throwable -> {
                        Log.e(TAG, "Failed to initialize ModuleRecognizer: " + throwable.getMessage());
                        throwable.printStackTrace();
                        return null;
                    });

        } catch (Exception e) {
            Log.e(TAG, "Fatal error during initialization: " + e.getMessage());
            e.printStackTrace();
        }
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
     * Stops the executor service and disposes of the ModuleRecognizer.
     */
    public void stop() {
        executor.shutdownNow();
        if (moduleRecognizer != null) {
            moduleRecognizer.dispose();
            Log.d(TAG, "ModuleRecognizer disposed");
            moduleRecognizer = null;
        }
    }
}