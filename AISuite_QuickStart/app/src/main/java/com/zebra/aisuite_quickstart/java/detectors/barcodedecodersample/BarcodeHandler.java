// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.detectors.barcodedecodersample;

import android.content.Context;
import android.util.Log;

import androidx.camera.core.ImageAnalysis;
import androidx.core.content.ContextCompat;

import com.zebra.ai.vision.detector.AIVisionSDKException;
import com.zebra.ai.vision.detector.AIVisionSDKLicenseException;
import com.zebra.ai.vision.detector.BarcodeDecoder;
import com.zebra.ai.vision.detector.InferencerOptions;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The BarcodeHandler class is responsible for setting up and managing the barcode
 * decoding process using a BarcodeDecoder and BarcodeAnalyzer. This class initializes
 * the necessary components for barcode detection and manages the lifecycle of the
 * decoding process within an Android application context.
 *
 * The BarcodeHandler sets up the decoder, assigns an analyzer for image analysis,
 * and provides methods to stop and dispose of resources when they are no longer needed.
 *
 * Usage:
 * - Instantiate the BarcodeHandler with the appropriate context, callback, and image analysis configuration.
 * - Call initializeBarcodeDecoder() to set up the barcode decoder and analyzer.
 * - Use getBarcodeAnalyzer() to retrieve the current instance of the BarcodeAnalyzer.
 * - Call stop() to terminate the executor service and dispose of the decoder when finished.
 *
 * Dependencies:
 * - Android Context: Required for managing resources and executing tasks on the main thread.
 * - ExecutorService: Used for asynchronous task execution.
 * - ImageAnalysis: Provides the framework for analyzing image data.
 * - BarcodeDecoder: Handles the decoding of barcode symbologies.
 * - BarcodeAnalyzer: Analyzes images to detect barcodes and report results via a callback.
 *
 * Exception Handling:
 * - Handles AIVisionSDKLicenseException and AIVisionSDKException during decoder initialization.
 * - Logs any other exceptions encountered during the setup process.
 *
 * Note: Ensure that the appropriate permissions and dependencies are configured
 * in the AndroidManifest and build files to utilize camera and image processing capabilities.
 */
public class BarcodeHandler {
    private static final String TAG = "BarcodeHandler";
    private BarcodeDecoder barcodeDecoder;
    private final ExecutorService executor;
    private final Context context;
    private BarcodeAnalyzer barcodeAnalyzer;
    private final BarcodeAnalyzer.DetectionCallback callback;
    private final ImageAnalysis imageAnalysis;
    private String mavenModelName = "barcode-localizer";

    /**
     * Constructs a new BarcodeHandler with the specified context, callback, and image analysis configuration.
     *
     * @param context The Android context for resource management.
     * @param callback The callback for handling detection results.
     * @param imageAnalysis The image analysis configuration for processing image data.
     */
    public BarcodeHandler(Context context, BarcodeAnalyzer.DetectionCallback callback, ImageAnalysis imageAnalysis) {
        this.context = context;
        this.callback = callback;
        this.executor = Executors.newSingleThreadExecutor();
        this.imageAnalysis = imageAnalysis;
        initializeBarcodeDecoder();
    }

    /**
     * Initializes the BarcodeDecoder with predefined settings for barcode symbologies
     * and detection parameters. This method sets up the necessary components for analyzing
     * and decoding barcodes from image data.
     */
    public void initializeBarcodeDecoder() {
        try {
            BarcodeDecoder.Settings decoderSettings = new BarcodeDecoder.Settings(mavenModelName);
            Integer[] rpo = new Integer[1];
            rpo[0] = InferencerOptions.DSP;

            decoderSettings.Symbology.CODE39.enable(true);
            decoderSettings.Symbology.CODE128.enable(true);

            decoderSettings.detectorSetting.inferencerOptions.runtimeProcessorOrder = rpo;
            decoderSettings.detectorSetting.inferencerOptions.defaultDims.height = 640;
            decoderSettings.detectorSetting.inferencerOptions.defaultDims.width = 640;

            long m_Start = System.currentTimeMillis();
            BarcodeDecoder.getBarcodeDecoder(decoderSettings, executor).thenAccept(decoderInstance -> {
                barcodeDecoder = decoderInstance;
                barcodeAnalyzer = new BarcodeAnalyzer(callback, barcodeDecoder);
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), barcodeAnalyzer);
                Log.d(TAG, "BarcodeDecoder() obj creation time =" + (System.currentTimeMillis() - m_Start) + " milli sec");
            }).exceptionally(e -> {
                if (e instanceof AIVisionSDKLicenseException) {
                    Log.e(TAG, "AIVisionSDKLicenseException: Barcode Decoder object creation failed, " + e.getMessage());
                } else {
                    Log.e(TAG, "Fatal error: decoder creation failed - " + e.getMessage());
                }
                return null;
            });
        } catch (AIVisionSDKException ex) {
            Log.e(TAG, "Model Loading: Barcode decoder returned with exception " + ex.getMessage());
        }
    }

    /**
     * Stops the executor service and disposes of the BarcodeDecoder, releasing any resources held.
     * This method should be called when barcode detection is no longer needed.
     */
    public void stop() {
        executor.shutdownNow();
        if (barcodeDecoder != null) {
            barcodeDecoder.dispose();
            Log.d(TAG, "Barcode decoder is disposed");
            barcodeDecoder = null;
        }
    }

    /**
     * Retrieves the current instance of the BarcodeAnalyzer.
     *
     * @return The BarcodeAnalyzer instance, or null if not yet initialized.
     */
    public BarcodeAnalyzer getBarcodeAnalyzer() {
        return barcodeAnalyzer;
    }
}
