// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.example.java.detectors;

import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;

import com.zebra.ai.vision.detector.AIVisionSDKException;
import com.zebra.ai.vision.detector.AIVisionSDKLicenseException;
import com.zebra.ai.vision.detector.ComplexBBox;
import com.zebra.ai.vision.detector.ImageData;
import com.zebra.ai.vision.detector.InferencerOptions;
import com.zebra.ai.vision.detector.TextOCR;
import com.zebra.ai.vision.entity.LineEntity;
import com.zebra.ai.vision.entity.ParagraphEntity;
import com.zebra.ai.vision.entity.WordEntity;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * OCRSample is a class that demonstrates the use of Optical Character Recognition (OCR)
 * to extract text from images using the TextOCR API. It initializes the OCR settings,
 * processes images.
 */
public class OCRSample {

    // Tag used for logging
    private static final String TAG = "OCRSample";

    // TextOCR instance used for text recognition
    private TextOCR textOCR;

    // Executor service for asynchronous operations
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private String mavenModelName = "text-ocr-recognizer";

    /**
     * Constructor for OCRSample.
     * Automatically initializes the TextOCR instance upon creation of an OCRSample object.
     */
    public OCRSample() {
        initializeTextOCR();
    }

    /**
     * Initializes the TextOCR with specific settings for text detection and recognition.
     * Configures the runtime processor order and default dimensions for the OCR model.
     */
    private void initializeTextOCR() {
        try {
            // Create TextOCR settings with the specified model name
            TextOCR.Settings textOCRSettings = new TextOCR.Settings(mavenModelName);

            // Define runtime processor order
            Integer[] rpo = new Integer[3];
            rpo[0] = InferencerOptions.DSP;
            rpo[1] = InferencerOptions.CPU;
            rpo[2] = InferencerOptions.GPU;

            // Apply runtime processor order and default dimensions to detection and recognition settings
            textOCRSettings.detectionInferencerOptions.runtimeProcessorOrder = rpo;
            textOCRSettings.recognitionInferencerOptions.runtimeProcessorOrder = rpo;
            textOCRSettings.detectionInferencerOptions.defaultDims.height = 640;
            textOCRSettings.detectionInferencerOptions.defaultDims.width = 640;

            // Record the start time for profiling
            long m_Start = System.currentTimeMillis();

            // Get TextOCR asynchronously and handle the result or any exceptions
            TextOCR.getTextOCR(textOCRSettings, executor).thenAccept(OCRInstance -> {
                textOCR = OCRInstance;
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
     * Processes an image using the TextOCR instance to recognize and extract text.
     * Logs recognized text along with its bounding box coordinates.
     *
     * @param image The image to be processed, provided as an ImageProxy
     */
    private void processImage(@NonNull ImageProxy image) {
        // Ensure TextOCR is initialized before processing the image
        try {
            Log.d(TAG, "Starting image analysis");

            // Process the image asynchronously and handle the result or exceptions
            textOCR.process(ImageData.fromImageProxy(image))
                    .thenAccept(result -> {
                        for (ParagraphEntity entity : result) {
                            List<LineEntity> lines = entity.getLines();
                            for (LineEntity line : lines) {
                                for (WordEntity word : line.getWords()) {
                                    ComplexBBox bbox = word.getComplexBBox();
                                    if (bbox != null && bbox.x != null && bbox.y != null && bbox.x.length >= 3 && bbox.y.length >= 3) {
                                        float minX = bbox.x[0], maxX = bbox.x[2], minY = bbox.y[0], maxY = bbox.y[2];
                                        Rect rect = new Rect((int) minX, (int) minY, (int) maxX, (int) maxY);
                                        String decodedValue = word.getText();
                                        Log.d(TAG, "Decoded value: " + decodedValue);
                                    }
                                }
                            }
                        }
                        image.close();
                    })
                    .exceptionally(ex -> {
                        Log.e(TAG, "Error in completable future result " + ex.getMessage());
                        // Close the Image Proxy object to release resources
                        image.close();
                        return null;
                    });
        } catch (AIVisionSDKException e) {
            Log.e(TAG, Objects.requireNonNull(e.getMessage()));
            image.close();
        }
    }
}
