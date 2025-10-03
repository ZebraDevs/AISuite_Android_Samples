// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.example.java.detectors;

import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;

import com.zebra.ai.vision.detector.AIVisionSDKException;
import com.zebra.ai.vision.detector.AIVisionSDKLicenseException;
import com.zebra.ai.vision.detector.BarcodeDecoder;
import com.zebra.ai.vision.detector.ImageData;
import com.zebra.ai.vision.detector.InferencerOptions;
import com.zebra.ai.vision.entity.BarcodeEntity;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * BarcodeSample is a class that demonstrates the initialization and usage of a barcode decoder.
 */
public class BarcodeSample {

    // Tag used for logging
    private final String TAG = "BarcodeSample";

    // BarcodeDecoder instance used to decode barcodes
    private BarcodeDecoder barcodeDecoder;

    // Executor service for asynchronous operations
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private String mavenModelName = "barcode-localizer";

    /**
     * Constructor for BarcodeSample.
     * Initializes the barcode decoder when an instance of the class is created.
     */
    public BarcodeSample() {
        initializeBarcodeDecoder();
    }

    /**
     * Initializes the barcode decoder with specific settings.
     * Configures the decoder to recognize certain barcode symbologies and dimensions.
     */
    private void initializeBarcodeDecoder() {
        try {
            // Create decoder settings
            BarcodeDecoder.Settings decoderSettings = new BarcodeDecoder.Settings(mavenModelName);

            // Define runtime processor order
            Integer[] rpo = new Integer[3];
            rpo[0] = InferencerOptions.DSP;
            rpo[1] = InferencerOptions.CPU;
            rpo[2] = InferencerOptions.GPU;

            // Enable specific symbologies for barcode decoding
            decoderSettings.Symbology.CODE39.enable(true);
            decoderSettings.Symbology.CODE128.enable(true);

            // Set Inferencer options
            decoderSettings.detectorSetting.inferencerOptions.runtimeProcessorOrder = rpo;
            decoderSettings.detectorSetting.inferencerOptions.defaultDims.height = 640;
            decoderSettings.detectorSetting.inferencerOptions.defaultDims.width = 640;

            // Record the start time for profiling
            long m_Start = System.currentTimeMillis();

            // Get barcode decoder asynchronously and handle result or exception
            BarcodeDecoder.getBarcodeDecoder(decoderSettings, executor).thenAccept(decoderInstance -> {
                barcodeDecoder = decoderInstance;
                Log.d(TAG, "BarcodeDecoder() obj creation time = " + (System.currentTimeMillis() - m_Start) + " milli sec");
            }).exceptionally(e -> {
                Log.e(TAG, "Fatal error: decoder creation failed - " + e.getMessage());
                return null;
            });
        } catch (Exception ex) {
            Log.e(TAG, "Model Loading: Barcode decoder returned with exception " + ex.getMessage());
        }
    }

    /**
     * Processes an image using the barcode decoder.
     * Extracts and logs barcode entities detected in the image.
     *
     * @param image The image to be processed, provided as an ImageProxy
     */
    private void processImage(@NonNull ImageProxy image) {
        // Initialize the barcode decoder prior.
        // Attach your custom analyzer to get image proxy from preview view
        try {
            Log.d(TAG, "Starting image analysis");
            barcodeDecoder.process(ImageData.fromImageProxy(image))
                    .thenAccept(result -> {
                        for (BarcodeEntity bb : result) {
                            Rect rect = bb.getBoundingBox();
                            Log.d(TAG, "Detected entity - Value: " + bb.getValue());
                        }
                        image.close();
                    })
                    .exceptionally(e -> {
                        if (e instanceof AIVisionSDKLicenseException) {
                            Log.e(TAG, "AIVisionSDKLicenseException: Barcode Decoder object creation failed, " + e.getMessage());
                        } else {
                            Log.e(TAG, "Fatal error: decoder creation failed - " + e.getMessage());
                        }
                        image.close();
                        return null;
                    });
        } catch (AIVisionSDKException e) {
            Log.e(TAG, Objects.requireNonNull(e.getMessage()));
            image.close();
        }
    }
}
