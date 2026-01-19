// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.example.java.analyzers;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;

import androidx.camera.core.ImageAnalysis;

import com.zebra.ai.vision.analyzer.tracking.EntityTrackerAnalyzer;
import com.zebra.ai.vision.detector.AIVisionSDKLicenseException;
import com.zebra.ai.vision.detector.BarcodeDecoder;
import com.zebra.ai.vision.detector.ComplexBBox;
import com.zebra.ai.vision.detector.Detector;
import com.zebra.ai.vision.detector.InferencerOptions;
import com.zebra.ai.vision.detector.ModuleRecognizer;
import com.zebra.ai.vision.detector.TextOCR;
import com.zebra.ai.vision.entity.BarcodeEntity;
import com.zebra.ai.vision.entity.Entity;
import com.zebra.ai.vision.entity.LabelEntity;
import com.zebra.ai.vision.entity.LineEntity;
import com.zebra.ai.vision.entity.ParagraphEntity;
import com.zebra.ai.vision.entity.ProductEntity;
import com.zebra.ai.vision.entity.ShelfEntity;
import com.zebra.ai.vision.entity.WordEntity;

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
 * Tracker is a class that facilitates the detection and tracking of barcodes
 * using a barcode decoder as a detector, tracking of texts using text OCR as detector, tracking of shelf and products using module recognizer as detector and an entity tracker analyzer. It initializes the necessary
 * components and processes image data to identify respective things.
 */
public class Tracker {

    // Tag used for logging
    private final String TAG = "Tracker";
    private final Context context;

    // BarcodeDecoder instance used to decode barcodes
    private BarcodeDecoder barcodeDecoder;
    // TextOCR instance used to detect texts
    private TextOCR textOCR;
    private ModuleRecognizer moduleRecognizer;

    // Executor service for asynchronous operations
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final String mavenModelName = "barcode-localizer";
    private final String mavenOCRModelName = "text-ocr-recognizer";
    private final String mavenProductModelName = "product-and-shelf-recognizer";

    /**
     * Constructor for Tracker.
     */
    public Tracker(Context context) {
        this.context = context;
        // We can pass multiple detectors as a list to the entity tracker analyzer
        initializeBarcodeDecoder(); // if we need to pass only barcode decoder as detector
        //    initializeTextOCR(); // if we need to pass only text OCR as detector
        //   initializeModuleRecognizer(); // if we need to pass only module recognizer as detector
    }

    /**
     * Initializes the barcode decoder with specific settings and starts the decoding process.
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
                initializeTracker(List.of(barcodeDecoder));
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
                initializeTracker(List.of(textOCR));
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
     * Initializes the ModuleRecognizer with product recognition enabled.
     */
    private void initializeModuleRecognizer() {
        try {
            // Copy assets
            String indexFilename = "product.index";
            String labelsFilename = "product.txt";
            String toPath = context.getFilesDir() + "/";
            copyFromAssets(indexFilename, toPath);
            copyFromAssets(labelsFilename, toPath);

            // Create settings with base model
            ModuleRecognizer.Settings settings = new ModuleRecognizer.Settings(mavenProductModelName);

            // Configure InferencerOptions
            settings.inferencerOptions.runtimeProcessorOrder = new Integer[]{
                    InferencerOptions.DSP,
                    InferencerOptions.CPU,
                    InferencerOptions.GPU
            };
            settings.inferencerOptions.defaultDims.height = 640;
            settings.inferencerOptions.defaultDims.width = 640;

            // Enable product recognition with the same model and recognition data
            settings.enableProductRecognitionWithIndex(
                    mavenProductModelName,
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
                        initializeTracker(List.of(moduleRecognizer));

                    })
                    .exceptionally(throwable -> {
                        Log.e(TAG, "Failed to initialize ModuleRecognizer: " + throwable.getMessage());
                        return null;
                    });

        } catch (Exception e) {
            Log.e(TAG, "Fatal error during initialization: " + e.getMessage());
        }
    }

    /**
     * Initializes the entity tracker analyzer after loading the barcode decoder model.
     */
    private void initializeTracker(List<Detector<? extends List<? extends Entity>>> detectors) {
        // Initialize the entity tracker analyzer with the decoded barcodes
        // EntityTrackerAnalyzer instance used to analyze detected entities
        EntityTrackerAnalyzer entityTrackerAnalyzer = new EntityTrackerAnalyzer(
                detectors,
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
        List<? extends Entity> ocrEntities = result.getValue(textOCR);
        List<? extends Entity> moduleEntities = result.getValue(moduleRecognizer);

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
        if (ocrEntities != null) {
            for (Entity entity : ocrEntities) {
                if (entity instanceof ParagraphEntity) {
                    ParagraphEntity pEntity = (ParagraphEntity) entity;
                    Log.i(TAG, "Paragraph Entity detected" + pEntity);
                    List<LineEntity> lineEntities = pEntity.getLines();
                    Log.i(TAG, "Lines detected" + lineEntities.size());
                    for (LineEntity lineEntity : lineEntities) {

                        for (WordEntity wordEntity : lineEntity.getWords()) {
                            ComplexBBox bbox = wordEntity.getComplexBBox();
                            String text = wordEntity.getText();
                        }
                    }
                }
            }
        }
        if (moduleEntities != null) {
            List<ShelfEntity> shelves = new ArrayList<>();
            List<LabelEntity> labels = new ArrayList<>();
            List<ProductEntity> products = new ArrayList<>();
            for (Entity entity : moduleEntities) {
                if (entity instanceof ShelfEntity) {
                    shelves.add((ShelfEntity) entity);
                } else if (entity instanceof LabelEntity) {
                    labels.add((LabelEntity) entity);
                } else if (entity instanceof ProductEntity) {
                    products.add((ProductEntity) entity);
                }
            }

            for (ShelfEntity shelf : shelves) {
                Rect shelfRect = shelf.getBoundingBox();
            }
            // Draw all labels (if you want to show all, not just those attached to shelves)
            for (LabelEntity label : labels) {
                if (label.getClassId() == LabelEntity.ClassId.PEG_LABEL) {
                    Rect labelRect = label.getBoundingBox();
                }
                if (label.getClassId() == LabelEntity.ClassId.SHELF_LABEL) {
                    Rect labelShelfRect = label.getBoundingBox();
                }
            }
            for (ProductEntity product : products) {
                Rect prodRect = product.getBoundingBox();
            }

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
     * Stops and disposes of the BarcodeDecoder,textOCR releasing any resources held.
     * This method should be called when detectors are no longer needed.
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
}
