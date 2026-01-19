// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.analyzers.tracker;

import android.content.Context;
import android.util.Log;

import androidx.camera.core.ImageAnalysis;
import androidx.core.content.ContextCompat;

import com.zebra.ai.vision.analyzer.tracking.EntityTrackerAnalyzer;
import com.zebra.ai.vision.detector.AIVisionSDKLicenseException;
import com.zebra.ai.vision.detector.BarcodeDecoder;
import com.zebra.ai.vision.detector.Detector;
import com.zebra.ai.vision.detector.InferencerOptions;
import com.zebra.ai.vision.detector.ModuleRecognizer;
import com.zebra.ai.vision.detector.TextOCR;
import com.zebra.ai.vision.entity.Entity;
import com.zebra.aisuite_quickstart.filtertracker.FilterDialog;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The Tracker class is responsible for setting up and managing the process
 * of detecting and decoding barcodes from image data. It utilizes the BarcodeDecoder
 * and EntityTrackerAnalyzer to analyze images and extract barcode information.
 * <p>
 * This class is designed to be used within an Android application context, leveraging
 * an ExecutorService for asynchronous operations. It requires a DetectionCallback
 * interface to handle the results of the barcode detection process.
 * <p>
 * Usage:
 * - Instantiate the Tracker with the necessary context, callback, and image analysis configurations.
 * - Call initializeBarcodeDecoder() to set up the barcode decoder with the desired settings.
 * - Use getBarcodeDecoder() to retrieve the current instance of the BarcodeDecoder.
 * - Invoke stop() to dispose of the BarcodeDecoder and release resources.
 * - Invoke stopAnalyzing() to terminate the ExecutorService and stop ongoing analysis tasks.
 * <p>
 * Dependencies:
 * - Android Context: Required for managing resources and executing tasks on the main thread.
 * - ExecutorService: Used for asynchronous task execution.
 * - ImageAnalysis: Provides the framework for analyzing image data.
 * - BarcodeDecoder: Handles the decoding of barcode symbologies.
 * - EntityTrackerAnalyzer: Analyzes images to track and decode barcodes.
 * <p>
 * Exception Handling:
 * - Handles AIVisionSDKLicenseException during decoder initialization.
 * - Logs any other exceptions encountered during the setup process.
 * <p>
 * Note: Ensure that the appropriate permissions and dependencies are configured
 * in the AndroidManifest and build files to utilize camera and image processing capabilities.
 */
public class Tracker {

    /**
     * Interface for handling the results of the barcode detection process.
     * Implement this interface to define how results are processed after detection.
     */
    public interface DetectionCallback {
        void handleEntities(EntityTrackerAnalyzer.Result result);
    }

    private static final String TAG = "Tracker";
    private BarcodeDecoder barcodeDecoder;
    private TextOCR textOCR;
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final Context context;
    private EntityTrackerAnalyzer entityTrackerAnalyzer;
    private final DetectionCallback callback;
    private final ImageAnalysis imageAnalysis;
    private final String mavenModelName = "barcode-localizer";
    private final String mavenOCRModelName = "text-ocr-recognizer";
    private final String mavenProductModelName = "product-and-shelf-recognizer";
    private ModuleRecognizer moduleRecognizer;
    private final List<String> selectedFilterItems;
    private final List<Detector<? extends List<? extends Entity>>> analyzerList = new ArrayList<>();

    /**
     * Constructs a new Tracker with the specified context, callback, and image analysis configuration.
     *
     * @param context       The Android context for resource management.
     * @param callback      The callback for handling detection results.
     * @param imageAnalysis The image analysis configuration for processing image data.
     */
    public Tracker(Context context, DetectionCallback callback, ImageAnalysis imageAnalysis, List<String> filterItems) {
        this.context = context;
        this.callback = callback;
        this.imageAnalysis = imageAnalysis;
        this.selectedFilterItems = filterItems;

        if(!selectedFilterItems.isEmpty()){
            for(String item : selectedFilterItems){
                if(item.equalsIgnoreCase(FilterDialog.BARCODE_TRACKER)){
                    initializeBarcodeDecoder();
                } else if(item.equalsIgnoreCase(FilterDialog.OCR_TRACKER)){
                    initializeTextOCR();
                } else if (item.equalsIgnoreCase(FilterDialog.PRODUCT_AND_SHELF)) {
                    initializeProductRecognition();
                }
            }
        }else{
            Log.d(TAG, "None of the filter selected");
        }


    }

    /**
     * Defines the preferred order of processors for AI inference operations.
     * Returns an array prioritizing DSP, then CPU, then GPU for optimal performance.
     *
     * @return Array of InferencerOptions constants representing processor priority order
     */
    private Integer[] getProcessorOrder() {
        return new Integer[]{InferencerOptions.DSP, InferencerOptions.CPU, InferencerOptions.GPU};
    }

    /**
     * Configuring Inferencer options based on settings instance type.
     */
    private void configureInferencerOptions(Object settings) {
        Integer[] rpo = getProcessorOrder();
        if (settings instanceof BarcodeDecoder.Settings) {
            BarcodeDecoder.Settings decoderSettings = (BarcodeDecoder.Settings) settings;
            decoderSettings.Symbology.CODE39.enable(true);
            decoderSettings.Symbology.CODE128.enable(true);
            decoderSettings.detectorSetting.inferencerOptions.runtimeProcessorOrder = rpo;
            decoderSettings.detectorSetting.inferencerOptions.defaultDims.height = 640;
            decoderSettings.detectorSetting.inferencerOptions.defaultDims.width = 640;
        } else if (settings instanceof TextOCR.Settings) {
            TextOCR.Settings textOCRSettings = (TextOCR.Settings) settings;
            textOCRSettings.detectionInferencerOptions.runtimeProcessorOrder = rpo;
            textOCRSettings.recognitionInferencerOptions.runtimeProcessorOrder = rpo;
            textOCRSettings.detectionInferencerOptions.defaultDims.height = 640;
            textOCRSettings.detectionInferencerOptions.defaultDims.width = 640;
        }
    }

    /**
     * Initializes the BarcodeDecoder with predefined settings for barcode symbologies
     * and detection parameters. This method sets up the necessary components for analyzing
     * and decoding barcodes from image data.
     */
    public void initializeBarcodeDecoder() {
        try {
            BarcodeDecoder.Settings settings = new BarcodeDecoder.Settings(mavenModelName);
            configureInferencerOptions(settings);

            long startTime = System.currentTimeMillis();
            BarcodeDecoder.getBarcodeDecoder(settings, executor).thenAccept(decoder -> {
                barcodeDecoder = decoder;
                createAnalyzer(List.of(barcodeDecoder));
                Log.d(TAG, "BarcodeDecoder creation time: " + (System.currentTimeMillis() - startTime) + "ms");
            }).exceptionally(this::handleException);
        } catch (Exception ex) {
            Log.e(TAG, "BarcodeDecoder initialization failed: " + ex.getMessage());
        }
    }

    /**
     * Initializes the TextOCR with predefined settings for text detection and recognition.
     * This method sets up the necessary components for analyzing and recognizing text from
     * image data.
     */
    private void initializeTextOCR() {
        try {
            TextOCR.Settings settings = new TextOCR.Settings(mavenOCRModelName);
            configureInferencerOptions(settings);

            long startTime = System.currentTimeMillis();
            TextOCR.getTextOCR(settings, executor).thenAccept(ocr -> {
                textOCR = ocr;
                createAnalyzer(List.of(textOCR));
                Log.d(TAG, "TextOCR creation time: " + (System.currentTimeMillis() - startTime) + "ms");
            }).exceptionally(this::handleException);
        } catch (Exception e) {
            Log.e(TAG, "TextOCR initialization failed: " + e.getMessage());
        }
    }

    public void initializeProductRecognition() {
        Log.e(TAG, "Initializing ModuleRecognizer for EntityTrackerAnalyzer");

        String iFilename = "product.index";
        String lFilename = "product.txt";
        String toPath = context.getFilesDir() + "/";
        copyFromAssets(iFilename, toPath);
        copyFromAssets(lFilename, toPath);
        String indexFilename = toPath + iFilename;
        String labelsFilename = toPath + lFilename;
        ModuleRecognizer.Settings settings;
        try {
            settings = new ModuleRecognizer.Settings(mavenProductModelName);
        } catch (Exception e) {
            Log.e(TAG, "Error "+e.getMessage());
            return;
        }
        Integer[] rpo = new Integer[]{
                InferencerOptions.DSP,
                InferencerOptions.CPU,
                InferencerOptions.GPU
        };
        settings.inferencerOptions.runtimeProcessorOrder = rpo;
        settings.inferencerOptions.defaultDims.height = 640;
        settings.inferencerOptions.defaultDims.width = 640;
        try {
            settings.enableProductRecognitionWithIndex(
                    mavenProductModelName,
                    indexFilename,
                    labelsFilename
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        long startTime = System.currentTimeMillis();
        ModuleRecognizer.getModuleRecognizer(settings, executor)
                .thenAccept(recognizerInstance -> {
                    Log.e(TAG, "ModuleRecognizer instance created");
                    moduleRecognizer = recognizerInstance;
                    // Use EntityTrackerAnalyzer with moduleRecognizer as a Detector
                    createAnalyzer(List.of(moduleRecognizer));
                    Log.d(TAG, "Module Recognition creation time: " + (System.currentTimeMillis() - startTime) + "ms");
                })
                .exceptionally(e -> {
                    Log.e(TAG, "Failed to create ModuleRecognizer: " + e.getMessage());
                    return null;
                });
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
    public ModuleRecognizer getModuleRecognizer() {
        return moduleRecognizer;
    }

    /**
     * Retrieves the current instance of the BarcodeDecoder.
     *
     * @return The BarcodeDecoder instance, or null if not yet initialized.
     */
    public BarcodeDecoder getBarcodeDecoder() {
        return barcodeDecoder;
    }

    /**
     * Retrieves the current instance of the TextOCR.
     *
     * @return The TextOCR instance, or null if not yet initialized.
     */
    public TextOCR getTextOCR() {
        return textOCR;
    }

    /**
     * Handles the results of the barcode detection by invoking the callback with the result.
     *
     * @param result The result of the barcode detection process.
     */
    private void handleEntities(EntityTrackerAnalyzer.Result result) {
        callback.handleEntities(result);
    }

    /**
     * Creates and configures an EntityTrackerAnalyzer with the provided analyzers.
     * Sets up the analyzer with original coordinate system and assigns it to the
     * image analysis pipeline using the main executor.
     *
     * @param analyzers List of detector instances (BarcodeDecoder, TextOCR, etc.)
     */
    private synchronized void createAnalyzer(List<Detector<? extends List<? extends Entity>>> analyzers) {
        analyzerList.add(analyzers.get(0));
        if(selectedFilterItems.size() == analyzerList.size()) {
            entityTrackerAnalyzer = new EntityTrackerAnalyzer(
                    analyzerList,
                    ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
                    executor,
                    this::handleEntities
            );
            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), entityTrackerAnalyzer);
        }
    }

    /**
     * Handles exceptions during component initialization.
     * Differentiates between license errors and other fatal errors,
     * providing appropriate error messages for debugging.
     *
     * @param component Name of the component that failed to initialize
     * @param e         Exception that occurred during initialization
     */
    private Void handleException(Throwable e) {
        String message = e instanceof AIVisionSDKLicenseException ?
                "License error: " + e.getMessage() :
                "Fatal error: " + e.getMessage();
        Log.e(TAG, message);
        return null;
    }

    /**
     * Stops and disposes of the BarcodeDecoder,textOCR releasing any resources held.
     * This method should be called when barcode detection is no longer needed.
     */
    public void stop() {
        if (barcodeDecoder != null) {
            barcodeDecoder.dispose();
            Log.d(TAG, "Barcode decoder is disposed");
            barcodeDecoder = null;
        }
        if (textOCR != null) {
            textOCR.dispose();
            Log.d(TAG, "TextOCR is disposed");
            textOCR = null;
        }
        if (moduleRecognizer != null) {
            moduleRecognizer.dispose();
            Log.d(TAG, "Module Recognizer is disposed");
            moduleRecognizer = null;
        }
    }

    /**
     * Stops the ExecutorService, terminating any ongoing analysis tasks.
     * This method should be called to clean up resources when analysis is no longer required.
     */
    public void stopAnalyzing() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

}
