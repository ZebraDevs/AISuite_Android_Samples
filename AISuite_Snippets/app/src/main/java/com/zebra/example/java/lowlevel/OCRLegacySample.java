// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.example.java.lowlevel;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;

import com.zebra.ai.vision.detector.AIVisionSDKLicenseException;
import com.zebra.ai.vision.detector.InferencerOptions;
import com.zebra.ai.vision.detector.InvalidInputException;
import com.zebra.ai.vision.detector.TextOCR;
import com.zebra.ai.vision.internal.detector.Paragraph;
import com.zebra.ai.vision.internal.detector.Word;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * OCRLegacySample demonstrates how to use Optical Character Recognition (OCR)
 * to extract text from images using the TextOCR API. It initializes the OCR model
 * and processes images to recognize text, either grouping into paragraphs or detecting individual words.
 */
public class OCRLegacySample {

    // Tag used for logging purposes
    private static final String TAG = "OCRLegacySample";

    // Instance of TextOCR used for text recognition
    private TextOCR textOCR;

    // Executor service for handling asynchronous operations
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Flag to enable or disable grouping of detected text into paragraphs
    private final boolean enableGrouping = false;
    private String mavenModelName = "text-ocr-recognizer";

    /**
     * Constructor for OCRLegacySample.
     * Automatically initializes the TextOCR instance upon creation of an OCRLegacySample object.
     */
    public OCRLegacySample() {
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
            Integer[] rpo = new Integer[1];
            rpo[0] = InferencerOptions.DSP;

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
     * Either groups text into paragraphs or detects individual words, based on the enableGrouping flag.
     *
     * @param imageProxy The image to be processed, provided as an ImageProxy
     */
    private void processImage(@NonNull ImageProxy imageProxy) {
        // Ensure TextOCR is initialized before processing the image
        try {
            Bitmap bitmap = imageProxy.toBitmap();

            if (enableGrouping) {
                // Group detected text into paragraphs
                CompletableFuture<Paragraph[]> futureTextParagraph;
                try {
                    futureTextParagraph = textOCR.detectParagraphs(bitmap, executor);
                } catch (InvalidInputException e) {
                    imageProxy.close();
                    throw new RuntimeException(e);
                }

                // Handle detected paragraphs
                futureTextParagraph.thenAccept(res -> {
                    for (Paragraph paragraph : res) {
                        String paragraphText = paragraph.toString();
                        Log.d(TAG, "Paragraph Text: " + paragraphText);
                    }
                    imageProxy.close();
                }).exceptionally(ex -> {
                    Log.e(TAG, "In enable grouping Exception occurred: " + ex.getMessage());
                    imageProxy.close();
                    return null;
                });

            } else {
                // Detect individual words
                CompletableFuture<Word[]> futureTextWords;
                try {
                    futureTextWords = textOCR.detectWords(bitmap, executor);
                } catch (InvalidInputException e) {
                    imageProxy.close();
                    throw new RuntimeException(e);
                }

                // Handle detected words
                futureTextWords.thenAccept(words -> {
                    Log.e("Async", "Got the detect data. invoking callback");
                    imageProxy.close();
                }).exceptionally(ex -> {
                    Log.e(TAG, "In non enable grouping Exception occurred: " + ex.getMessage());
                    imageProxy.close();
                    return null;
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception while processing image: " + e.getMessage());
            imageProxy.close();
        }
    }
}
