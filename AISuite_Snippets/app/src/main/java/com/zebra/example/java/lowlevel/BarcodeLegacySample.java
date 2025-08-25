// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.example.java.lowlevel;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;

import com.zebra.ai.vision.detector.AIVisionSDKException;
import com.zebra.ai.vision.detector.AIVisionSDKLicenseException;
import com.zebra.ai.vision.detector.AIVisionSDKSNPEException;
import com.zebra.ai.vision.detector.BBox;
import com.zebra.ai.vision.detector.BarcodeDecoder;
import com.zebra.ai.vision.detector.InferencerOptions;
import com.zebra.ai.vision.detector.InvalidInputException;
import com.zebra.ai.vision.detector.Localizer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * BarcodeLegacySample demonstrates the use of a barcode decoder and localizer
 * to detect and decode barcodes from images. It sets up the necessary models
 * and processes images by first localizing barcodes and then decoding them.
 */
public class BarcodeLegacySample {

    // Tag used for logging purposes
    private final String TAG = "BarcodeSample";

    // Instance of BarcodeDecoder used for decoding barcodes
    private BarcodeDecoder barcodeDecoder;

    // Instance of Localizer used for detecting barcode positions
    private Localizer localizer;

    // Executor service for handling asynchronous operations
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private String mavenModelName = "barcode-localizer";

    /**
     * Constructor for BarcodeLegacySample.
     * Initializes the barcode and localizer models when an instance is created.
     */
    public BarcodeLegacySample() {
        initialiseModels();
    }

    /**
     * Initializes the models required for barcode localization and decoding.
     * Sets up the Localizer and BarcodeDecoder with appropriate settings.
     */
    private void initialiseModels() {
        try {
            long mStart = System.currentTimeMillis();

            // Initialize Localizer with settings
            Localizer.Settings locSettings = new Localizer.Settings(mavenModelName);
            long diff = System.currentTimeMillis() - mStart;
            Log.d(TAG, "Barcode Localizer.settings() obj creation time = " + diff + " milli sec");

            Integer[] rpo = new Integer[1];
            rpo[0] = InferencerOptions.DSP;

            locSettings.inferencerOptions.runtimeProcessorOrder = rpo;
            locSettings.inferencerOptions.defaultDims.height = 640;
            locSettings.inferencerOptions.defaultDims.width = 640;

            long start = System.currentTimeMillis();
            Localizer.getLocalizer(locSettings, executor).thenAccept(localizerInstance -> {
                localizer = localizerInstance;
                Log.d(TAG, "Barcode Localizer(locSettings) obj creation / model loading time = " + (System.currentTimeMillis() - start) + " milli sec");
            }).exceptionally(e -> handleException(e, "Barcode Localizer object creation failed"));

            // Initialize BarcodeDecoder with settings
            BarcodeDecoder.Settings decoderSettings = new BarcodeDecoder.Settings(mavenModelName);
            decoderSettings.Symbology.CODE39.enable(true);
            decoderSettings.Symbology.CODE128.enable(true);
            decoderSettings.detectorSetting.inferencerOptions.runtimeProcessorOrder = rpo;
            decoderSettings.detectorSetting.inferencerOptions.defaultDims.height = 640;
            decoderSettings.detectorSetting.inferencerOptions.defaultDims.width = 640;
            decoderSettings.enableLocalization = false;

            long m_Start = System.currentTimeMillis();
            BarcodeDecoder.getBarcodeDecoder(decoderSettings, executor).thenAccept(decoderInstance -> {
                barcodeDecoder = decoderInstance;
                Log.d(TAG, "BarcodeDecoder() obj creation time = " + (System.currentTimeMillis() - m_Start) + " milli sec");
            }).exceptionally(e -> handleException(e, "Barcode Decoder object creation failed"));

        } catch (Exception e) {
            Log.e(TAG, "Fatal error: load failed - " + e.getMessage());
        }
    }

    /**
     * Processes an image by first detecting barcodes with the Localizer,
     * then decoding them using the BarcodeDecoder.
     *
     * @param imageProxy The image to be processed, provided as an ImageProxy
     */
    private void processImage(@NonNull ImageProxy imageProxy) {
        // Convert ImageProxy to Bitmap for processing
        try {
            Bitmap bitmap = imageProxy.toBitmap();
            CompletableFuture<BBox[]> futureResult = localizer.detect(bitmap, executor);

            // Chain barcode decoding after detection
            futureResult.thenCompose(bBoxes -> {
                Log.d(TAG, "Detections: " + bBoxes.length);
                // Proceed to barcode decoding
                try {
                    return barcodeDecoder.decode(bitmap, bBoxes, executor);
                } catch (InvalidInputException e) {
                    imageProxy.close();
                    throw new RuntimeException(e);
                }
            }).thenAccept(barcodes -> {
                for (BarcodeDecoder.Result barcode : barcodes) {
                    String decodedString = barcode.value;
                    BBox decoded_bbox = barcode.bboxData;
                    Log.d(TAG, "Decoded barcode: " + decodedString);
                }
                imageProxy.close();
            }).exceptionally(ex -> {
                Log.e(TAG, "Error in completable future result " + ex.getMessage());
                imageProxy.close();
                return null;
            });

        } catch (Exception e) {
            Log.e(TAG, "Exception in decoding or on drawing canvas " + e.getMessage());
            imageProxy.close();
        }
    }

    /**
     * Handles exceptions during model creation and processing, logging the appropriate error messages.
     *
     * @param e The exception that occurred
     * @param message A custom message describing the context of the error
     * @return Always returns null for use in exceptionally() callback
     */
    private Void handleException(Throwable e, String message) {
        if (e instanceof AIVisionSDKLicenseException) {
            Log.e(TAG, "AIVisionSDKLicenseException: " + message + ", " + e.getMessage());
        } else if (e instanceof AIVisionSDKSNPEException) {
            Log.e(TAG, "AIVisionSDKSNPEException: " + message + ", " + e.getMessage());
        } else if (e instanceof InvalidInputException) {
            Log.e(TAG, "InvalidInputException: " + message + ", " + e.getMessage());
        } else if (e instanceof AIVisionSDKException) {
            Log.e(TAG, "AIVisionSDKException: " + message + ", " + e.getMessage());
        } else {
            Log.e(TAG, "Unhandled exception: " + message + ", " + e.getMessage());
        }
        return null;
    }
}
