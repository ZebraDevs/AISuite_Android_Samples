// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.viewfinder;

import android.content.Context;
import android.util.Log;

import androidx.camera.core.ImageAnalysis;
import androidx.core.content.ContextCompat;

import com.zebra.ai.vision.analyzer.tracking.EntityTrackerAnalyzer;
import com.zebra.ai.vision.detector.AIVisionSDKLicenseException;
import com.zebra.ai.vision.detector.BarcodeDecoder;
import com.zebra.ai.vision.detector.InferencerOptions;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The EntityBarcodeTracker class is responsible for managing the detection and decoding
 * of barcodes using a BarcodeDecoder and EntityTrackerAnalyzer. This class is designed
 * to work within an Android context and facilitates asynchronous operations for barcode
 * detection in a view-referenced coordinate system.
 *
 * This class initializes and configures a barcode decoder, processes image analysis through
 * an executor service, and provides callback mechanisms to handle detection results and
 * notify when the tracker is ready for use.
 *
 * Usage:
 * - Instantiate the EntityBarcodeTracker with the necessary context, callback, and image analysis configurations.
 * - Call initializeBarcodeDecoder() to set up the barcode decoder and begin processing images.
 * - Use getBarcodeDecoder() and getEntityTrackerAnalyzer() to retrieve instances of the decoder and analyzer.
 * - Implement the DetectionCallback interface to handle detection results and track readiness.
 * - Call stop() to dispose of the BarcodeDecoder and release resources when finished.
 * - Call stopAnalyzing() to terminate the executor service and stop ongoing analysis tasks.
 *
 * Dependencies:
 * - Android Context: Required for resource management and executing tasks on the main thread.
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
public class EntityBarcodeTracker {

    /**
     * Interface for handling the results of the barcode detection process and notifying when the tracker is ready.
     * Implement this interface to define how results are processed and readiness is handled.
     */
    public interface DetectionCallback {
        void handleEntitiesForEntityView(EntityTrackerAnalyzer.Result result);

        // Method to notify when the tracker is ready for use.
        default void onEntityBarcodeTrackerReady() {}
    }

    private static final String TAG = "EntityBarcodeTracker";
    private BarcodeDecoder barcodeDecoder;
    private final ExecutorService executor;
    private final Context context;
    private EntityTrackerAnalyzer entityTrackerAnalyzer;
    private final DetectionCallback callback;
    private final ImageAnalysis imageAnalysis;
    private String mavenModelName = "barcode-localizer";

    /**
     * Constructs a new EntityBarcodeTracker with the specified context, callback, and image analysis configuration.
     *
     * @param context The Android context for resource management.
     * @param callback The callback for handling detection results and tracker readiness.
     * @param imageAnalysis The image analysis configuration for processing image data.
     */
    public EntityBarcodeTracker(Context context, DetectionCallback callback, ImageAnalysis imageAnalysis) {
        this.context = context;
        this.callback = callback;
        this.executor = Executors.newSingleThreadExecutor();
        this.imageAnalysis = imageAnalysis;
        initializeBarcodeDecoder();
    }

    /**
     * Initializes the BarcodeDecoder with predefined settings for barcode symbologies
     * and detection parameters. This method sets up the necessary components for analyzing
     * and decoding barcodes from image data. Notifies the callback when the tracker is ready.
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
                        ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED,
                        executor,
                        this::handleEntities
                );
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), entityTrackerAnalyzer);
                Log.d(TAG, "Entity Tracker BarcodeDecoder() obj creation time =" + (System.currentTimeMillis() - m_Start) + " milli sec");

                // Notify that the tracker is ready
                callback.onEntityBarcodeTrackerReady();
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
     * Retrieves the current instance of the BarcodeDecoder.
     *
     * @return The BarcodeDecoder instance, or null if not yet initialized.
     */
    public BarcodeDecoder getBarcodeDecoder() {
        return barcodeDecoder;
    }

    /**
     * Retrieves the current instance of the EntityTrackerAnalyzer.
     *
     * @return The EntityTrackerAnalyzer instance, or null if not yet initialized.
     */
    public EntityTrackerAnalyzer getEntityTrackerAnalyzer() {
        return entityTrackerAnalyzer;
    }

    /**
     * Handles the results of the barcode detection by invoking the callback with the result.
     *
     * @param result The result of the barcode detection process.
     */
    private void handleEntities(EntityTrackerAnalyzer.Result result) {
        callback.handleEntitiesForEntityView(result);
    }

    /**
     * Stops and disposes of the BarcodeDecoder, releasing any resources held.
     * This method should be called when barcode detection is no longer needed.
     */
    public void stop() {
        if (barcodeDecoder != null) {
            barcodeDecoder.dispose();
            Log.d(TAG, "Barcode decoder is disposed");
            barcodeDecoder = null;
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