// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.example.java.analyzers;

import android.graphics.Rect;
import android.util.Log;

import androidx.camera.core.ImageAnalysis;

import com.zebra.ai.vision.analyzer.tracking.EntityTrackerAnalyzer;
import com.zebra.ai.vision.detector.AIVisionSDKLicenseException;
import com.zebra.ai.vision.detector.BarcodeDecoder;
import com.zebra.ai.vision.detector.InferencerOptions;
import com.zebra.ai.vision.entity.BarcodeEntity;
import com.zebra.ai.vision.entity.Entity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * BarcodeTracker is a class that initializes and manages the barcode detection and decoding process.
 * It uses a barcode decoder to detect barcodes and an entity tracker to handle the detected entities.
 */
public class BarcodeTracker {

    // Tag used for logging
    private final String TAG = "BarcodeTracker";

    // BarcodeDecoder instance used to decode barcodes
    private BarcodeDecoder barcodeDecoder;

    // Executor service for asynchronous operations
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private String mavenModelName = "barcode-localizer";

    /**
     * Constructor for BarcodeTracker.
     */
    public BarcodeTracker() {
        initializeBarcodeDecoder();
    }

    /**
     * Initializes the barcode decoder with specific settings and starts the decoding process.
     */
    private void initializeBarcodeDecoder() {
        try {
            // Create decoder settings
            BarcodeDecoder.Settings decoderSettings = new BarcodeDecoder.Settings(mavenModelName);

            // Define runtime processor order
            Integer[] rpo = new Integer[1];
            rpo[0] = InferencerOptions.DSP;

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
                initializeTracker();
                Log.d(TAG, "BarcodeDecoder() obj creation time = " + (System.currentTimeMillis() - m_Start) + " milli sec");
            }).exceptionally(e -> {
                if (e instanceof AIVisionSDKLicenseException) {
                    Log.e(TAG, "AIVisionSDKLicenseException: Barcode Decoder object creation failed, " + e.getMessage());
                } else {
                    Log.e(TAG, "Fatal error: decoder creation failed - " + e.getMessage());
                }
                return null;
            });
        } catch (Exception ex) {
            Log.e(TAG, "Model Loading: Barcode decoder returned with exception " + ex.getMessage());
        }
    }

    /**
     * Initializes the entity tracker analyzer after loading the barcode decoder model.
     */
    private void initializeTracker() {
        // Initialize the entity tracker analyzer with the decoded barcodes
        // EntityTrackerAnalyzer instance used to analyze detected entities
        EntityTrackerAnalyzer entityTrackerAnalyzer = new EntityTrackerAnalyzer(
                List.of(barcodeDecoder),
                ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
                executor,
                this::handleEntities
        );
        // The tracker analyzer should be passed to the preview view as the analyzer
    }

    /**
     * Handles the entities detected by the entity tracker analyzer.
     *
     * @param result The result containing the detected entities
     */
    private void handleEntities(EntityTrackerAnalyzer.Result result) {
        List<? extends Entity> entities = result.getValue(barcodeDecoder);

        if (entities != null) {
            for (Entity entity : entities) {
                if (entity instanceof BarcodeEntity) {
                    BarcodeEntity bEntity = (BarcodeEntity) entity;
                    Rect rect = bEntity.getBoundingBox();
                    if (rect != null) {
                        String hashCode = String.valueOf(bEntity.hashCode());
                        Log.d(TAG, "Tracker UUID: " + hashCode + " Tracker Detected entity - Value: " + bEntity.getValue());
                    }
                }
            }
        }
    }
}
