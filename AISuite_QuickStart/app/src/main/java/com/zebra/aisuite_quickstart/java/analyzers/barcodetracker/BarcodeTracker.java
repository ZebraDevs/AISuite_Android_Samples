// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.analyzers.barcodetracker;

import android.content.Context;
import android.util.Log;

import androidx.camera.core.ImageAnalysis;
import androidx.core.content.ContextCompat;

import com.zebra.ai.vision.analyzer.tracking.EntityTrackerAnalyzer;
import com.zebra.ai.vision.detector.AIVisionSDKLicenseException;
import com.zebra.ai.vision.detector.BarcodeDecoder;
import com.zebra.ai.vision.detector.InferencerOptions;
import com.zebra.ai.vision.detector.TextOCR;
import com.zebra.aisuite_quickstart.filtertracker.FilterType;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The BarcodeTracker class is responsible for setting up and managing the process
 * of detecting and decoding barcodes from image data. It utilizes the BarcodeDecoder
 * and EntityTrackerAnalyzer to analyze images and extract barcode information.
 *
 * This class is designed to be used within an Android application context, leveraging
 * an ExecutorService for asynchronous operations. It requires a DetectionCallback
 * interface to handle the results of the barcode detection process.
 *
 * Usage:
 * - Instantiate the BarcodeTracker with the necessary context, callback, and image analysis configurations.
 * - Call initializeBarcodeDecoder() to set up the barcode decoder with the desired settings.
 * - Use getBarcodeDecoder() to retrieve the current instance of the BarcodeDecoder.
 * - Invoke stop() to dispose of the BarcodeDecoder and release resources.
 * - Invoke stopAnalyzing() to terminate the ExecutorService and stop ongoing analysis tasks.
 *
 * Dependencies:
 * - Android Context: Required for managing resources and executing tasks on the main thread.
 * - ExecutorService: Used for asynchronous task execution.
 * - ImageAnalysis: Provides the framework for analyzing image data.
 * - BarcodeDecoder: Handles the decoding of barcode symbologies.
 * - EntityTrackerAnalyzer: Analyzes images to track and decode barcodes.
 *
 * Exception Handling:
 * - Handles AIVisionSDKLicenseException during decoder initialization.
 * - Logs any other exceptions encountered during the setup process.
 *
 * Note: Ensure that the appropriate permissions and dependencies are configured
 * in the AndroidManifest and build files to utilize camera and image processing capabilities.
 */
public class BarcodeTracker {

    /**
     * Interface for handling the results of the barcode detection process.
     * Implement this interface to define how results are processed after detection.
     */
    public interface DetectionCallback {
        void handleEntities(EntityTrackerAnalyzer.Result result);
    }

    private static final String TAG = "BarcodeTracker";
    private BarcodeDecoder barcodeDecoder;
    private TextOCR textOCR;
    private final ExecutorService executor;
    private final Context context;
    private EntityTrackerAnalyzer entityTrackerAnalyzer;
    private final DetectionCallback callback;
    private final ImageAnalysis imageAnalysis;
    private String mavenModelName = "barcode-localizer";
    private String mavenOCRModelName = "text-ocr-recognizer";
    private boolean barcodeInitialized = false;
    private boolean ocrInitialized = false;
    private FilterType filterType;

    /**
     * Constructs a new BarcodeTracker with the specified context, callback, and image analysis configuration.
     *
     * @param context The Android context for resource management.
     * @param callback The callback for handling detection results.
     * @param imageAnalysis The image analysis configuration for processing image data.
     */
    public BarcodeTracker(Context context, DetectionCallback callback, ImageAnalysis imageAnalysis, FilterType filterType) {
        this.context = context;
        this.callback = callback;
        this.executor = Executors.newSingleThreadExecutor();
        this.imageAnalysis = imageAnalysis;
        this.filterType = filterType;

       if(filterType.equals(FilterType.BOTH)) initializeBarcodeDecoderOCR();
       if(filterType.equals(FilterType.BARCODE)) initializeBarcodeDecoder();
       if(filterType.equals(FilterType.OCR)) initializeTextOCR();
    }

    /**
     * Initializes the BarcodeDecoder with predefined settings for barcode symbologies
     * and detection parameters. This method sets up the necessary components for analyzing
     * and decoding barcodes from image data.
     */
    public void initializeBarcodeDecoder() {
        try {
            BarcodeDecoder.Settings decoderSettings = new BarcodeDecoder.Settings(mavenModelName);

            Integer[] rpo = new Integer[3];
            rpo[0] = InferencerOptions.DSP;
            rpo[1] = InferencerOptions.CPU;
            rpo[2] = InferencerOptions.GPU;

            decoderSettings.Symbology.CODE39.enable(true);
            decoderSettings.Symbology.CODE128.enable(true);

            decoderSettings.detectorSetting.inferencerOptions.runtimeProcessorOrder = rpo;
            decoderSettings.detectorSetting.inferencerOptions.defaultDims.height = 640;
            decoderSettings.detectorSetting.inferencerOptions.defaultDims.width = 640;

            long m_Start = System.currentTimeMillis();
            BarcodeDecoder.getBarcodeDecoder(decoderSettings, executor).thenAccept(decoderInstance -> {
                barcodeDecoder = decoderInstance;
                entityTrackerAnalyzer = new EntityTrackerAnalyzer(
                        List.of(barcodeDecoder),
                        ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
                        executor,
                        this::handleEntities
                );
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), entityTrackerAnalyzer);
                Log.d(TAG, "Entity Tracker BarcodeDecoder() obj creation time =" + (System.currentTimeMillis() - m_Start) + " milli sec");
            }).exceptionally(e -> {
                if (e instanceof AIVisionSDKLicenseException) {
                    Log.e(TAG, "AIVisionSDKLicenseException: Barcode Decoder object creation failed, " + e.getMessage());
                } else {
                    Log.e(TAG, "Fatal error: decoder creation failed - " + e.getMessage());
                }
                return null;
            });
        } catch (Exception ex) {
            Log.e(TAG, "Model Loading: Entity Tracker Barcode decoder returned with exception " + ex.getMessage());
        }
    }
    /**
     * Initializes the TextOCR with predefined settings for text detection and recognition.
     * This method sets up the necessary components for analyzing and recognizing text from
     * image data.
     */
    private void initializeTextOCR() {
        try {
            TextOCR.Settings textOCRSettings = new TextOCR.Settings(mavenOCRModelName);

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
                entityTrackerAnalyzer = new EntityTrackerAnalyzer(
                        List.of(textOCR),
                        ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
                        executor,
                        this::handleEntities
                );
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), entityTrackerAnalyzer);

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
     * Initializes the BarcodeDecoder and OCR with predefined settings for barcode symbologies
     * and detection parameters. This method sets up the necessary components for analyzing
     * and decoding barcodes from image data.
     */
    public void initializeBarcodeDecoderOCR() {
        try {
            BarcodeDecoder.Settings decoderSettings = new BarcodeDecoder.Settings(mavenModelName);
            TextOCR.Settings textOCRSettings = new TextOCR.Settings(mavenOCRModelName);

            Integer[] rpo = new Integer[3];
            rpo[0] = InferencerOptions.DSP;
            rpo[1] = InferencerOptions.CPU;
            rpo[2] = InferencerOptions.GPU;

            decoderSettings.Symbology.CODE39.enable(true);
            decoderSettings.Symbology.CODE128.enable(true);

            decoderSettings.detectorSetting.inferencerOptions.runtimeProcessorOrder = rpo;
            decoderSettings.detectorSetting.inferencerOptions.defaultDims.height = 640;
            decoderSettings.detectorSetting.inferencerOptions.defaultDims.width = 640;

            textOCRSettings.detectionInferencerOptions.runtimeProcessorOrder = rpo;
            textOCRSettings.recognitionInferencerOptions.runtimeProcessorOrder = rpo;
            textOCRSettings.detectionInferencerOptions.defaultDims.height = 640;
            textOCRSettings.detectionInferencerOptions.defaultDims.width = 640;

            long m_Start = System.currentTimeMillis();
            BarcodeDecoder.getBarcodeDecoder(decoderSettings, executor).thenAccept(decoderInstance -> {
                barcodeDecoder = decoderInstance;
                barcodeInitialized = true;
                tryInitializeTracker();
                Log.d(TAG, "Entity Tracker BarcodeDecoder() obj creation time =" + (System.currentTimeMillis() - m_Start) + " milli sec");
            }).exceptionally(e -> {
                if (e instanceof AIVisionSDKLicenseException) {
                    Log.e(TAG, "AIVisionSDKLicenseException: Barcode Decoder object creation failed, " + e.getMessage());
                } else {
                    Log.e(TAG, "Fatal error: decoder creation failed - " + e.getMessage());
                }
                return null;
            });
            long mStart = System.currentTimeMillis();
            TextOCR.getTextOCR(textOCRSettings, executor).thenAccept(OCRInstance -> {
                textOCR = OCRInstance;
                ocrInitialized = true;
                tryInitializeTracker();
                Log.d(TAG, "TextOCR() obj creation / model loading time = " + (System.currentTimeMillis() - mStart) + " milli sec");
            }).exceptionally(e -> {
                if (e instanceof AIVisionSDKLicenseException) {
                    Log.e(TAG, "AIVisionSDKLicenseException: TextOCR object creation failed, " + e.getMessage());
                } else {
                    Log.e(TAG, "Fatal error: TextOCR creation failed - " + e.getMessage());
                }
                return null;
            });
        } catch (Exception ex) {
            Log.e(TAG, "Model Loading: Entity Tracker Barcode decoder returned with exception " + ex.getMessage());
        }
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
     * Attempts to initialize the Tracker once all components are initialized.
     */
    private synchronized void tryInitializeTracker() {
        if (barcodeInitialized && ocrInitialized) {
                entityTrackerAnalyzer = new EntityTrackerAnalyzer(
                        List.of(barcodeDecoder, textOCR),
                        ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
                        executor,
                        this::handleEntities
                );
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), entityTrackerAnalyzer);
        }
    }

    /**
     * Stops and disposes of the BarcodeDecoder,textOCR releasing any resources held.
     * This method should be called when barcode detection is no longer needed.
     */
    public void stop() {
        if (barcodeDecoder != null) {
            barcodeDecoder.dispose();
            Log.d(TAG, "Barcode decoder is disposed");
            barcodeDecoder =null;
        }
        if (textOCR != null) {
            textOCR.dispose();
            Log.d(TAG, "TextOCR is disposed");
            textOCR = null;
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
