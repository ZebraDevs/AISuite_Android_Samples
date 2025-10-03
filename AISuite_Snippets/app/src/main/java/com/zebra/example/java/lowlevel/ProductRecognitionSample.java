// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.example.java.lowlevel;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.camera.core.ImageProxy;

import com.zebra.ai.vision.detector.AIVisionSDKException;
import com.zebra.ai.vision.detector.AIVisionSDKLicenseException;
import com.zebra.ai.vision.detector.BBox;
import com.zebra.ai.vision.detector.FeatureExtractor;
import com.zebra.ai.vision.detector.InferencerOptions;
import com.zebra.ai.vision.detector.InvalidInputException;
import com.zebra.ai.vision.detector.Localizer;
import com.zebra.ai.vision.detector.Recognizer;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ProductRecognitionSample demonstrates the use of AI models to recognize products
 * within images. It utilizes a localizer to detect potential products and shelves,
 * a feature extractor to generate descriptors, and a recognizer to identify products
 * based on similarity thresholds.
 */
public class ProductRecognitionSample {

    // Android context in which the sample operates
    private final Context context;

    // Instances of AI models used for product recognition
    private Localizer localizer;
    private FeatureExtractor featureExtractor;
    private Recognizer recognizer;

    // Logging tag for this class
    private final String TAG = "ProductRecognitionSample";

    // Similarity threshold for product recognition
    private final float SIMILARITY_THRESHOLD = 0.65f;

    // Arrays to hold detected bounding boxes and products
    private BBox[] detections, products;

    // Executor service for handling asynchronous operations
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private String mavenModelName = "product-and-shelf-recognizer";

    /**
     * Constructor for ProductRecognitionSample.
     * Initializes the product recognition models when an instance is created.
     *
     * @param context The application context for accessing assets and resources
     */
    public ProductRecognitionSample(Context context) {
        this.context = context;
        initializeProductRecognition();
    }

    /**
     * Initializes the AI models required for product recognition. Sets up the localizer,
     * feature extractor, and recognizer with appropriate settings and loads index files
     * from the application's assets.
     */
    private void initializeProductRecognition() {
        try {
            // Localizer settings for detecting products and shelves
            Localizer.Settings locSettings = new Localizer.Settings(mavenModelName);
            FeatureExtractor.Settings feSettings = new FeatureExtractor.Settings(mavenModelName);

            // Runtime processor order configuration
            Integer[] rpo = new Integer[3];
            rpo[0] = InferencerOptions.DSP;
            rpo[1] = InferencerOptions.CPU;
            rpo[2] = InferencerOptions.GPU;
            feSettings.inferencerOptions.runtimeProcessorOrder = rpo;
            locSettings.inferencerOptions.runtimeProcessorOrder = rpo;
            locSettings.inferencerOptions.defaultDims.height = 640;
            locSettings.inferencerOptions.defaultDims.width = 640;

            // File paths for index and label files
            String indexFilename = "product.index";
            String labelsFilename = "product.txt";
            String toPath = context.getFilesDir() + "/";

            // Copy index and label files from assets to local storage
            copyFromAssets(indexFilename, toPath);
            copyFromAssets(labelsFilename, toPath);

            // Recognizer settings with paths to index and label files
            Recognizer.SettingsIndex reSettings = new Recognizer.SettingsIndex();
            reSettings.indexFilename = toPath + indexFilename;
            reSettings.labelFilename = toPath + labelsFilename;

            // Initialize localizer, feature extractor, and recognizer asynchronously
            CompletableFuture<Void> localizerFuture = Localizer.getLocalizer(locSettings, executor)
                    .thenAccept(localizerInstance -> localizer = localizerInstance).exceptionally(e -> handleException(e, "Shelf Localizer object creation failed"));

            CompletableFuture<Void> extractorFuture = FeatureExtractor.getFeatureExtractor(feSettings, executor)
                    .thenAccept(featureExtractorInstance -> featureExtractor = featureExtractorInstance).exceptionally(e -> handleException(e, "Feature Extractor object creation failed"));

            CompletableFuture<Void> recognizerFuture = Recognizer.getRecognizer(reSettings, executor)
                    .thenAccept(recognizerInstance -> recognizer = recognizerInstance).exceptionally(e -> handleException(e, "Recognizer object creation failed"));

            // Wait for all models to be initialized
            CompletableFuture.allOf(localizerFuture, extractorFuture, recognizerFuture).join();

        } catch (Exception e) {
            Log.e(TAG, "Fatal error during initialization: " + e.getMessage());
        }
    }

    /**
     * Copies files from the application's assets to a specified local path.
     *
     * @param filename The name of the file to copy
     * @param toPath The destination path for the copied file
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
     * Processes an image using the initialized AI models to detect and recognize products.
     * The image is first analyzed for bounding boxes, then descriptors are generated for products,
     * and finally, recognitions are found based on these descriptors.
     *
     * @param imageProxy The image to be processed, provided as an ImageProxy
     */
    private void processImage(ImageProxy imageProxy) {
        try {
            Log.d(TAG, "Starting image analysis");
            Bitmap bitmap = imageProxy.toBitmap();

            // Detect bounding boxes in the image using the  shelf localizer
            CompletableFuture<BBox[]> futureResultBBox = localizer.detect(bitmap, executor);
            futureResultBBox.thenCompose(bBoxes -> {
                detections = bBoxes;
                //Detected objects are categorized using class IDs:
                // - Class ID 1 corresponds to products.
                products = Arrays.stream(bBoxes).filter(x -> x.cls == 1).toArray(BBox[]::new);
                Log.d(TAG, "Products size = " + products.length + " detections " + bBoxes.length);

                // Generate descriptors for detected products
                if (bBoxes.length > 0) {
                    try {
                        return featureExtractor.generateDescriptors(products, bitmap, executor);
                    } catch (InvalidInputException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    return CompletableFuture.completedFuture(null);
                }
            }).thenCompose(descriptor -> {
                // Find recognitions based on generated descriptors
                if (descriptor != null && detections.length > 0) {
                    try {
                        return recognizer.findRecognitions(descriptor, executor);
                    } catch (InvalidInputException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    return CompletableFuture.completedFuture(null);
                }
            }).thenAccept(recognitions -> {
                // Log recognized products if any are found
                if (recognitions != null) {
                    Log.e(TAG, "Products recognitions " + recognitions.length);
                    //Detected objects are categorized using class IDs:
                    // - Class ID 2 corresponds to label shelf.
                    // - Class ID 3 corresponds to label peg.
                    // - Class ID 4 corresponds to Shelf.
                    BBox[] labelShelfObjects = Arrays.stream(detections).filter(x -> x.cls == 2).toArray(BBox[]::new);
                    BBox[] labelPegObjects = Arrays.stream(detections).filter(x -> x.cls == 3).toArray(BBox[]::new);
                    BBox[] shelfObjects = Arrays.stream(detections).filter(x -> x.cls == 4).toArray(BBox[]::new);
                    if (recognitions.length > 0) {
                        for (int i = 0; i < products.length; i++) {
                            if (recognitions[i].similarity[0] > SIMILARITY_THRESHOLD) {
                                BBox bBox = products[i];
                                Log.d(TAG, "product id: " + recognitions[i].sku[0]);
                            }
                        }
                    }
                }
                imageProxy.close();
            }).exceptionally(ex -> {
                Log.e(TAG, "Error in completable future result " + ex.getMessage());
                imageProxy.close();
                return null;
            });

        } catch (AIVisionSDKException e) {
            Log.e(TAG, "Exception occurred: " + e.getMessage());
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
        } else {
            Log.e(TAG, message + ": " + e.getMessage());
        }
        return null;
    }
}
