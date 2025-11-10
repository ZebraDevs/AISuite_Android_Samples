// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.lowlevel.simplebarcodesample;

import android.content.Context;
import android.util.Log;

import androidx.camera.core.ImageAnalysis;
import androidx.core.content.ContextCompat;

import com.zebra.ai.vision.detector.AIVisionSDKLicenseException;
import com.zebra.ai.vision.detector.BarcodeDecoder;
import com.zebra.ai.vision.detector.InferencerOptions;
import com.zebra.ai.vision.detector.Localizer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The BarcodeSample class implements the BarcodeSampleAnalyzer.SampleBarcodeDetectionCallback
 * interface and is responsible for initializing and managing the barcode detection process.
 * This class utilizes a Localizer and BarcodeDecoder to process image data and detect barcodes.
 *
 * It configures necessary components and manages their lifecycle within an Android application context.
 * The BarcodeSample class provides a mechanism for handling the results of barcode detection through
 * a callback interface.
 *
 * Usage:
 * - Instantiate the BarcodeSample with the appropriate context, callback, and image analysis configuration.
 * - Call initializeBarcodeDecoder() to set up the localizer and decoder for barcode detection.
 * - Use getBarcodeDecoder() to retrieve the current instance of the BarcodeDecoder.
 * - Call stop() to dispose of the BarcodeDecoder and release resources when finished.
 *
 * Dependencies:
 * - Android Context: Required for resource management and executing tasks on the main thread.
 * - ExecutorService: Used for asynchronous task execution.
 * - ImageAnalysis: Provides the framework for analyzing image data.
 * - Localizer: Detects potential regions of interest for barcode decoding.
 * - BarcodeDecoder: Handles the decoding of barcodes from images.
 *
 * Exception Handling:
 * - Handles AIVisionSDKLicenseException and generic exceptions during initialization.
 * - Logs any other exceptions encountered during the setup process.
 *
 * Note: Ensure that the appropriate permissions and dependencies are configured
 * in the AndroidManifest and build files to utilize camera and image processing capabilities.
 */
public class BarcodeSample {
    private static final String TAG = "BarcodeSample";
    private Localizer localizer;
    private BarcodeDecoder barcodeDecoder;
    private final ExecutorService executor;
    private final Context context;
    private BarcodeSampleAnalyzer barcodeAnalyzer;
    private final BarcodeSampleAnalyzer.SampleBarcodeDetectionCallback callback;
    private final ImageAnalysis imageAnalysis;
    private String mavenModelName = "barcode-localizer";

    /**
     * Constructs a new BarcodeSample with the specified context, callback, and image analysis configuration.
     *
     * @param context The Android context for resource management.
     * @param callback The callback for handling barcode detection results.
     * @param imageAnalysis The image analysis configuration for processing image data.
     */
    public BarcodeSample(Context context, BarcodeSampleAnalyzer.SampleBarcodeDetectionCallback callback, ImageAnalysis imageAnalysis) {
        this.context = context;
        this.callback = callback;
        this.executor = Executors.newSingleThreadExecutor();
        this.imageAnalysis = imageAnalysis;
        initializeBarcodeDecoder();
    }

    /**
     * Initializes the Localizer and BarcodeDecoder with predefined settings. This method sets up
     * the necessary components for analyzing and decoding barcodes from image data.
     */
    public void initializeBarcodeDecoder() {
        try {
            long mStart = System.currentTimeMillis();

            Localizer.Settings locSettings = new Localizer.Settings(mavenModelName);
            long diff = System.currentTimeMillis() - mStart;
            Log.d(TAG, "Barcode Localizer.settings() obj creation time =" + diff + " milli sec");

            Integer[] rpo = new Integer[3];
            rpo[0] = InferencerOptions.DSP;
            rpo[1] = InferencerOptions.CPU;
            rpo[2] = InferencerOptions.GPU;

            locSettings.inferencerOptions.runtimeProcessorOrder = rpo;
            locSettings.inferencerOptions.defaultDims.height = 640;
            locSettings.inferencerOptions.defaultDims.width = 640;

            long start = System.currentTimeMillis();
            Localizer.getLocalizer(locSettings, executor).thenAccept(localizerInstance -> {
                localizer = localizerInstance;
                Log.d(TAG, "Barcode Localizer(locSettings) obj creation / model loading time =" + (System.currentTimeMillis() - start) + " milli sec");

            }).exceptionally(e -> {
                if (e instanceof AIVisionSDKLicenseException) {
                    Log.e(TAG, "AIVisionSDKLicenseException: Barcode Localizer object creation failed, " + e.getMessage());
                } else {
                    Log.e(TAG, "Fatal error: load failed - " + e.getMessage());
                }
                return null;
            });

            BarcodeDecoder.Settings decoderSettings = new BarcodeDecoder.Settings(mavenModelName);

            long m_Start = System.currentTimeMillis();
            BarcodeDecoder.getBarcodeDecoder(decoderSettings, executor).thenAccept(decoderInstance -> {
                barcodeDecoder = decoderInstance;
                barcodeAnalyzer = new BarcodeSampleAnalyzer(callback, localizer, barcodeDecoder);
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
        } catch (Exception e) {
            Log.e(TAG, "Fatal error: load failed - " + e.getMessage());
        }
    }

    /**
     * Disposes of the BarcodeDecoder, releasing any resources held. This method should be
     * called when barcode detection is no longer needed.
     */
    public void stop() {
        if (barcodeDecoder != null) {
            barcodeDecoder.dispose();
            Log.d(TAG, "Barcode decoder is disposed");
            barcodeDecoder = null;
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

    public BarcodeSampleAnalyzer getBarcodeAnalyzer(){
        return barcodeAnalyzer;
    }

}

