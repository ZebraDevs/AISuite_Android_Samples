// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.analyzers.tracker;

import android.content.Context;
import android.util.Log;

import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.core.content.ContextCompat;

import com.zebra.ai.vision.analyzer.tracking.EntityTrackerAnalyzer;
import com.zebra.ai.vision.detector.AIVisionSDKException;
import com.zebra.ai.vision.detector.AIVisionSDKLicenseException;
import com.zebra.ai.vision.detector.BarcodeDecoder;
import com.zebra.ai.vision.detector.Detector;
import com.zebra.ai.vision.detector.ImageData;
import com.zebra.ai.vision.detector.InferencerOptions;
import com.zebra.ai.vision.detector.ModuleRecognizer;
import com.zebra.ai.vision.detector.TextOCR;
import com.zebra.ai.vision.entity.Entity;
import com.zebra.aisuite_quickstart.filtertracker.FilterDialog;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The Tracker class is responsible for setting up and managing the process
 * of detecting and decoding barcodes from image data. It utilizes the BarcodeDecoder
 * and EntityTrackerAnalyzer to analyze images and extract barcode information.
 * <p>
 * This class is designed to be used within an Android application context, leveraging
 * an ExecutorService for asynchronous operations. It requires a DetectionCallback
 * interface to handle the results of the barcode detection process.
 * <p>
 * Usage:
 * - Instantiate the Tracker with the necessary context, callback, and image analysis configurations.
 * - Call initializeBarcodeDecoder() to set up the barcode decoder with the desired settings.
 * - Use getBarcodeDecoder() to retrieve the current instance of the BarcodeDecoder.
 * - Invoke stop() to dispose of the BarcodeDecoder and release resources.
 * - Invoke stopAnalyzing() to terminate the ExecutorService and stop ongoing analysis tasks.
 * <p>
 * Dependencies:
 * - Android Context: Required for managing resources and executing tasks on the main thread.
 * - ExecutorService: Used for asynchronous task execution.
 * - ImageAnalysis: Provides the framework for analyzing image data.
 * - BarcodeDecoder: Handles the decoding of barcode symbologies.
 * - EntityTrackerAnalyzer: Analyzes images to track and decode barcodes.
 * <p>
 * Exception Handling:
 * - Handles AIVisionSDKLicenseException during decoder initialization.
 * - Logs any other exceptions encountered during the setup process.
 * <p>
 * Note: Ensure that the appropriate permissions and dependencies are configured
 * in the AndroidManifest and build files to utilize camera and image processing capabilities.
 */
public class Tracker {

    public void processImage(ImageProxy image) {
        try {
            ImageData imageData = ImageData.fromImageProxy(image);
            List<CompletableFuture<?>> futures = new ArrayList<>();

            final List<? extends Entity>[] barcodeResult = new List[]{null};
            final List<? extends Entity>[] ocrResult = new List[]{null};
            final List<? extends Entity>[] moduleResult = new List[]{null};

            for (String item : selectedFilterItems) {
                if (item.equalsIgnoreCase(FilterDialog.BARCODE_TRACKER) && captureBarcodeDecoder != null) {
                    futures.add(captureBarcodeDecoder.process(imageData)
                            .thenAccept(result -> barcodeResult[0] = result));
                } else if (item.equalsIgnoreCase(FilterDialog.OCR_TRACKER) && captureOcr != null) {
                    futures.add(captureOcr.process(imageData)
                            .thenAccept(result -> ocrResult[0] = result));
                } else if (item.equalsIgnoreCase(FilterDialog.PRODUCT_AND_SHELF) && captureModuleRecognizer != null) {
                    futures.add(captureModuleRecognizer.process(imageData)
                            .thenAccept(result -> moduleResult[0] = result));
                }
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenRun(() -> callback.handleCaptureFrameEntities(barcodeResult[0], ocrResult[0], moduleResult[0]))
                    .exceptionally(e -> {
                        Log.e(TAG, "Error processing capture image: " + e.getMessage());
                        return null;
                    });
        } catch (AIVisionSDKException e) {
            Log.e(TAG, "Error in processImage: " + e.getMessage());
        } finally {
            image.close();
        }
    }

    public void startAnalyzing() {
        Log.d(TAG, "startAnalyzing() called. ");
        executor = Executors.newFixedThreadPool(3);
        entityTrackerAnalyzer = new EntityTrackerAnalyzer(analyzerList, ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL, executor, this::handleEntities);
    }

    /**
     * Interface for handling the results of the barcode detection process.
     * Implement this interface to define how results are processed after detection.
     */
    public interface DetectionCallback {
        void handleEntities(EntityTrackerAnalyzer.Result result);

        void handleCaptureFrameEntities(List<? extends Entity> barcodeEntities, List<? extends Entity> ocrEntities, List<? extends Entity> moduleEntities);
    }

    private static final String TAG = "Tracker";
    private BarcodeDecoder barcodeDecoder;
    private TextOCR textOCR;
    private ExecutorService executor = Executors.newFixedThreadPool(3);
    private final ExecutorService captureExecutor = Executors.newFixedThreadPool(3);
    private final Context context;
    private EntityTrackerAnalyzer entityTrackerAnalyzer;
    private final DetectionCallback callback;
    private final ImageAnalysis imageAnalysis;
    private final String mavenModelName = "barcode-localizer";
    private final String mavenOCRModelName = "text-ocr-recognizer";
    private final String mavenProductModelName = "product-and-shelf-recognizer";
    private ModuleRecognizer moduleRecognizer;
    private final List<String> selectedFilterItems;
    private final List<Detector<? extends List<? extends Entity>>> analyzerList = new ArrayList<>();
    private final List<Detector<? extends List<? extends Entity>>> captureAnalyzerList = new ArrayList<>();
    private final ModelLoadingCallback loadingCallback;
    // Model input sizes
    private static final int LIVE_PREVIEW_SIZE = 640;
    private static final int CAPTURE_SIZE = 1280; // Higher resolution for capture
    // Capture instances
    private BarcodeDecoder captureBarcodeDecoder;
    private TextOCR captureOcr;
    private ModuleRecognizer captureModuleRecognizer;

    private boolean captureModelsLoaded = false;
    private boolean modelsLoaded = false;

    /**
     * Callback interface for model loading completion
     */
    public interface ModelLoadingCallback {
        void onLoadingComplete(boolean success);
    }

    public interface CaptureTrackerCallback {
        void onCaptureTrackerReady(boolean modelLoaded);
    }

    /**
     * Constructs a new Tracker with the specified context, callback, and image analysis configuration.
     *
     * @param context       The Android context for resource management.
     * @param callback      The callback for handling detection results.
     * @param imageAnalysis The image analysis configuration for processing image data.
     */
    public Tracker(Context context, DetectionCallback callback, ImageAnalysis imageAnalysis, List<String> filterItems, ModelLoadingCallback loadingCallback) {
        this.context = context;
        this.callback = callback;
        this.imageAnalysis = imageAnalysis;
        this.selectedFilterItems = filterItems;
        this.loadingCallback = loadingCallback;

        if (!selectedFilterItems.isEmpty()) {
            for (String item : selectedFilterItems) {
                if (item.equalsIgnoreCase(FilterDialog.BARCODE_TRACKER)) {
                    initializeBarcodeDecoder();
                    initializeCaptureBarcodeDecoder();
                } else if (item.equalsIgnoreCase(FilterDialog.OCR_TRACKER)) {
                    initializeTextOCR();
                    initializeCaptureOcr();
                } else if (item.equalsIgnoreCase(FilterDialog.PRODUCT_AND_SHELF)) {
                    initializeModuleRecognizer();
                    initializeCaptureModuleRecognizer();
                }
            }
        } else {
            if (loadingCallback != null) {
                loadingCallback.onLoadingComplete(false);
            }
            Log.d(TAG, "None of the filter selected");
        }

    }


    /**
     * Initializes the BarcodeDecoder with predefined settings for barcode symbologies
     * and detection parameters. This method sets up the necessary components for analyzing
     * and decoding barcodes from image data.
     */
    public void initializeBarcodeDecoder() {
        try {
            // Initialize live preview decoder with smaller input size
            BarcodeDecoder.Settings liveDecoderSettings = createBarcodeDecoderSettings(LIVE_PREVIEW_SIZE);
            // Call the helper function to create the decoder with fallback logic
            createBarcodeDecoderWithFallback(liveDecoderSettings);

        } catch (AIVisionSDKException ex) {
            // Notify failed loading
            if (loadingCallback != null) {
                loadingCallback.onLoadingComplete(false);
            }
            Log.e(TAG, "Model Loading: Barcode decoder returned with exception " + ex.getMessage());
        }
    }

    private void createBarcodeDecoderWithFallback(BarcodeDecoder.Settings decoderSettings) {
        long m_Start = System.currentTimeMillis();
        BarcodeDecoder.getBarcodeDecoder(decoderSettings, executor).thenAccept(decoderInstance -> {
            barcodeDecoder = decoderInstance;
            createAnalyzer(List.of(barcodeDecoder));
            Log.d(TAG, "BarcodeDecoder() obj creation time =" + (System.currentTimeMillis() - m_Start) + " milli sec");
        }).exceptionally(e -> {
            if (e instanceof AIVisionSDKLicenseException) {
                // Notify failed loading
                if (loadingCallback != null) {
                    loadingCallback.onLoadingComplete(false);
                }
                Log.e(TAG, "AIVisionSDKLicenseException: Barcode Decoder object creation failed, " + e.getMessage());
            } else {
                // Notify failed loading
                if (loadingCallback != null) {
                    loadingCallback.onLoadingComplete(false);
                }
                Log.e(TAG, "Fatal error: decoder creation failed - " + e.getMessage());
            }
            return null;
        });
    }

    /**
     * Initializes the TextOCR with predefined settings for text detection and recognition.
     * This method sets up the necessary components for analyzing and recognizing text from
     * image data.
     */
    private void initializeTextOCR() {
        try {
            // Initialize live preview OCR with smaller input size
            TextOCR.Settings liveOCRSettings = createTextOCRSettings(LIVE_PREVIEW_SIZE);
            // Call the helper function to create the TextOCR instance with fallback logic
            createTextOCRWithFallback(liveOCRSettings);

        } catch (Exception e) {
            // Notify failed loading
            if (loadingCallback != null) {
                loadingCallback.onLoadingComplete(false);
            }
            Log.e(TAG, "Fatal error: load failed - " + e.getMessage());
        }
    }

    private void createTextOCRWithFallback(TextOCR.Settings textOCRSettings) {
        long m_Start = System.currentTimeMillis();
        TextOCR.getTextOCR(textOCRSettings, executor).thenAccept(OCRInstance -> {
            textOCR = OCRInstance;
            createAnalyzer(List.of(textOCR));
            Log.d(TAG, "TextOCR() obj creation / model loading time = " + (System.currentTimeMillis() - m_Start) + " milli sec");
        }).exceptionally(e -> {
            if (e instanceof AIVisionSDKLicenseException) {
                // Notify failed loading
                if (loadingCallback != null) {
                    loadingCallback.onLoadingComplete(false);
                }
                Log.e(TAG, "AIVisionSDKLicenseException: TextOCR object creation failed, " + e.getMessage());
            } else {
                // Notify failed loading
                if (loadingCallback != null) {
                    loadingCallback.onLoadingComplete(false);
                }
                Log.e(TAG, "Fatal error: TextOCR creation failed - " + e.getMessage());
            }
            return null;
        });
    }

    private void initializeModuleRecognizer() {
        try {
            // Create settings for live preview
            ModuleRecognizer.Settings liveRecognizerSettings = createModuleRecognizerSettings(LIVE_PREVIEW_SIZE);            // Call the helper function to create the recognizer with fallback logic
            createModuleRecognizerWithFallback(liveRecognizerSettings);

        } catch (Exception e) {
            // Notify failed loading
            if (loadingCallback != null) {
                loadingCallback.onLoadingComplete(false);
            }
            Log.e(TAG, "Fatal error during initialization setup: " + e.getMessage());
        }
    }

    private void createModuleRecognizerWithFallback(ModuleRecognizer.Settings settings) {
        long startTime = System.currentTimeMillis();
        ModuleRecognizer.getModuleRecognizer(settings, executor).thenAccept(recognizerInstance -> {
            long creationTime = System.currentTimeMillis() - startTime;
            Log.d(TAG, "ModuleRecognizer Creation Time: " + creationTime + "ms");

            moduleRecognizer = recognizerInstance;
            // Use EntityTrackerAnalyzer with moduleRecognizer as a Detector
            createAnalyzer(List.of(moduleRecognizer));
        }).exceptionally(throwable -> {
            // Notify failed loading
            if (loadingCallback != null) {
                loadingCallback.onLoadingComplete(false);
            }
            Log.e(TAG, "Failed to initialize ModuleRecognizer: " + throwable.getMessage());
            return null;
        });
    }

    /**
     * Creates barcode decoder settings with specified input size
     */
    private BarcodeDecoder.Settings createBarcodeDecoderSettings(int inputSize) {
        BarcodeDecoder.Settings decoderSettings = new BarcodeDecoder.Settings(mavenModelName);
        Integer[] rpo = new Integer[3];
        rpo[0] = InferencerOptions.DSP;
        rpo[1] = InferencerOptions.CPU;
        rpo[2] = InferencerOptions.GPU;

        decoderSettings.Symbology.CODE39.enable(true);
        decoderSettings.Symbology.CODE128.enable(true);

        decoderSettings.detectorSetting.inferencerOptions.runtimeProcessorOrder = rpo;
        decoderSettings.detectorSetting.inferencerOptions.defaultDims.height = inputSize;
        decoderSettings.detectorSetting.inferencerOptions.defaultDims.width = inputSize;

        return decoderSettings;
    }


    private TextOCR.Settings createTextOCRSettings(int inputSize) {
        TextOCR.Settings textOCRSettings = new TextOCR.Settings(mavenOCRModelName);
        Integer[] rpo = new Integer[3];
        rpo[0] = InferencerOptions.DSP;
        rpo[1] = InferencerOptions.CPU;
        rpo[2] = InferencerOptions.GPU;

        textOCRSettings.detectionInferencerOptions.runtimeProcessorOrder = rpo;
        textOCRSettings.recognitionInferencerOptions.runtimeProcessorOrder = rpo;
        textOCRSettings.detectionInferencerOptions.defaultDims.height = inputSize;
        textOCRSettings.detectionInferencerOptions.defaultDims.width = inputSize;


        return textOCRSettings;
    }

    /**
     * Creates module recognizer settings with specified input size
     */
    private ModuleRecognizer.Settings createModuleRecognizerSettings(int inputSize) {
        // Copy assets
        String indexFilename = "product.index";
        String labelsFilename = "product.txt";
        String toPath = context.getFilesDir() + "/";
        copyFromAssets(indexFilename, toPath);
        copyFromAssets(labelsFilename, toPath);
        // Create settings with base model
        ModuleRecognizer.Settings settings = new ModuleRecognizer.Settings(mavenProductModelName);

        // Configure InferencerOptions
        settings.inferencerOptions.runtimeProcessorOrder = new Integer[]{InferencerOptions.DSP, InferencerOptions.CPU, InferencerOptions.GPU};
        settings.inferencerOptions.defaultDims.height = inputSize;
        settings.inferencerOptions.defaultDims.width = inputSize;

        // Enable product recognition with the same model and recognition data
        settings.enableProductRecognitionWithIndex(mavenProductModelName, toPath + indexFilename, toPath + labelsFilename);

        return settings;
    }


    /**
     * Initializes the capture barcode decoder with higher resolution settings
     */
    public void initializeCaptureBarcodeDecoder() {
        try {
            BarcodeDecoder.Settings captureDecoderSettings = createBarcodeDecoderSettings(CAPTURE_SIZE);
            createCaptureBarcodeDecoderWithFallback(captureDecoderSettings);
        } catch (Exception ex) {
            if (loadingCallback != null) {
                loadingCallback.onLoadingComplete(false);
            }
            Log.e(TAG, "Capture barcode decoder initialization failed: " + ex.getMessage());
        }
    }

    private void createCaptureBarcodeDecoderWithFallback(BarcodeDecoder.Settings decoderSettings) {
        long startTime = System.currentTimeMillis();
        BarcodeDecoder.getBarcodeDecoder(decoderSettings, captureExecutor).thenAccept(decoderInstance -> {
            captureBarcodeDecoder = decoderInstance;
            createCaptureAnalyzer(List.of(captureBarcodeDecoder));
            Log.d(TAG, "Capture BarcodeDecoder() obj creation time =" + (System.currentTimeMillis() - startTime) + " milli sec");
        }).exceptionally(e -> {
            if (loadingCallback != null) {
                loadingCallback.onLoadingComplete(false);
            }
            Log.e(TAG, "Capture barcode decoder creation failed: " + e.getMessage());
            return null;
        });
    }

    /**
     * Initializes the capture OCR scanner with higher resolution settings
     */
    public void initializeCaptureOcr() {
        try {
            TextOCR.Settings captureOcrSettings = createTextOCRSettings(CAPTURE_SIZE);
            createCaptureTextOCRWithFallback(captureOcrSettings);
        } catch (Exception ex) {
            if (loadingCallback != null) {
                loadingCallback.onLoadingComplete(false);
            }
            Log.e(TAG, "Capture OCR scanner initialization failed: " + ex.getMessage());
        }
    }

    private void createCaptureTextOCRWithFallback(TextOCR.Settings ocrSettings) {
        long startTime = System.currentTimeMillis();
        TextOCR.getTextOCR(ocrSettings, captureExecutor).thenAccept(ocrInstance -> {
            captureOcr = ocrInstance;
            createCaptureAnalyzer(List.of(captureOcr));
            Log.d(TAG, "TextOCR() obj creation / model loading time = " + (System.currentTimeMillis() - startTime) + " milli sec");
        }).exceptionally(e -> {
            if (loadingCallback != null) {
                loadingCallback.onLoadingComplete(false);
            }
            Log.e(TAG, "Capture OCR scanner creation failed: " + e.getMessage());
            return null;
        });
    }

    /**
     * Initializes the capture module recognizer with higher resolution settings
     */
    public void initializeCaptureModuleRecognizer() {
        try {
            ModuleRecognizer.Settings captureModuleSettings = createModuleRecognizerSettings(CAPTURE_SIZE);
            createCaptureModuleRecognizerWithFallback(captureModuleSettings);
        } catch (Exception ex) {
            if (loadingCallback != null) {
                loadingCallback.onLoadingComplete(false);
            }
            Log.e(TAG, "Capture module recognizer initialization failed: " + ex.getMessage());
        }
    }

    private void createCaptureModuleRecognizerWithFallback(ModuleRecognizer.Settings moduleSettings) {
        long startTime = System.currentTimeMillis();
        ModuleRecognizer.getModuleRecognizer(moduleSettings, captureExecutor).thenAccept(moduleInstance -> {
            captureModuleRecognizer = moduleInstance;
            long creationTime = System.currentTimeMillis() - startTime;
            createCaptureAnalyzer(List.of(captureModuleRecognizer));
            Log.d(TAG, "Capture ModuleRecognizer Creation Time: " + creationTime + "ms");
        }).exceptionally(e -> {
            if (loadingCallback != null) {
                loadingCallback.onLoadingComplete(false);
            }
            Log.e(TAG, "Capture module recognizer creation failed: " + e.getMessage());
            return null;
        });
    }


    /**
     * Copies files from the assets folder to the specified path.
     *
     * @param filename The name of the file to copy.
     * @param toPath   The destination path.
     */
    private void copyFromAssets(String filename, String toPath) {
        final int bufferSize = 8192;
        try (InputStream stream = context.getAssets().open(filename); OutputStream fos = Files.newOutputStream(Paths.get(toPath + filename)); BufferedOutputStream output = new BufferedOutputStream(fos)) {
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
     * Retrieves the current instance of the ProductRecognitionAnalyzer.
     *
     * @return The ProductRecognitionAnalyzer instance, or null if not yet initialized.
     */
    public ModuleRecognizer getModuleRecognizer() {
        return moduleRecognizer;
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
     * Retrieves the current instance of the TextOCR.
     *
     * @return The TextOCR instance, or null if not yet initialized.
     */
    public TextOCR getTextOCR() {
        return textOCR;
    }

    /**
     * Handles the results of the barcode detection by invoking the callback with the result.
     *
     * @param result The result of the barcode detection process.
     */
    private void handleEntities(EntityTrackerAnalyzer.Result result) {
        callback.handleEntities(result);
    }


    /**
     * Creates and configures an EntityTrackerAnalyzer with the provided analyzers.
     * Sets up the analyzer with original coordinate system and assigns it to the
     * image analysis pipeline using the main executor.
     *
     * @param analyzers List of detector instances (BarcodeDecoder, TextOCR, etc.)
     */
    private synchronized void createAnalyzer(List<Detector<? extends List<? extends Entity>>>
                                                     analyzers) {
        analyzerList.add(analyzers.get(0));
        if (selectedFilterItems.size() == analyzerList.size()) {
            modelsLoaded = true;
            if (captureModelsLoaded) {
                if (loadingCallback != null) {
                    loadingCallback.onLoadingComplete(true);
                }
                attachAnalysisAfterModelLoading();
            }

        }
    }

    private synchronized void createCaptureAnalyzer(List<Detector<? extends List<? extends Entity>>>
                                                            analyzers) {
        captureAnalyzerList.add(analyzers.get(0));
        if (selectedFilterItems.size() == captureAnalyzerList.size()) {
            if(modelsLoaded) {
                if (loadingCallback != null) {
                    loadingCallback.onLoadingComplete(true);
                }
                attachAnalysisAfterModelLoading();
            }
            captureModelsLoaded = true;

        }
    }

    /**
     * Handles exceptions during component initialization.
     * Differentiates between license errors and other fatal errors,
     * providing appropriate error messages for debugging.
     *
     * @param component Name of the component that failed to initialize
     * @param e         Exception that occurred during initialization
     */
    private Void handleException(Throwable e) {
        String message = e instanceof AIVisionSDKLicenseException ? "License error: " + e.getMessage() : "Fatal error: " + e.getMessage();
        Log.e(TAG, message);
        return null;
    }

    /**
     * Stops and disposes of the BarcodeDecoder, TextOCR, and ModuleRecognizer, releasing any resources held.
     * This method should be called when barcode detection is no longer needed.
     */
    public void stop() {
        if (captureExecutor != null) {
            captureExecutor.shutdownNow();
        }
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
        // Dispose capture instances
        if (captureBarcodeDecoder != null) {
            captureBarcodeDecoder.dispose();
            Log.d(TAG, "Capture barcode decoder disposed");
            captureBarcodeDecoder = null;
        }
        if (captureOcr != null) {
            captureOcr.dispose();
            Log.d(TAG, "Capture OCR scanner disposed");
            captureOcr = null;
        }
        if (captureModuleRecognizer != null) {
            captureModuleRecognizer.dispose();
            Log.d(TAG, "Capture module recognizer disposed");
            captureModuleRecognizer = null;
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

    public boolean getCapturedTrackerAnalyzer() {
        return captureModelsLoaded;
    }

    public EntityTrackerAnalyzer getEntityTrackerAnalyzer() {
        return entityTrackerAnalyzer;
    }

    public void attachAnalysisAfterModelLoading(){
        entityTrackerAnalyzer = new EntityTrackerAnalyzer(analyzerList, ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL, executor, this::handleEntities);
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), entityTrackerAnalyzer);
    }


}
