package com.zebra.aisuite_quickstart.java.detectors.warehouselocalizer;

import android.content.Context;
import android.util.Log;

import androidx.camera.core.ImageAnalysis;
import androidx.core.content.ContextCompat;

import com.zebra.ai.vision.detector.InferencerOptions;
import com.zebra.ai.vision.detector.Localizer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WareHouseLocalizerHandler {
    private static final String TAG = "WareHouseLocalizerHandler";
    private Localizer wareHouseLocalizer;       // For live preview
    private Localizer captureLocalizer;
    private final ExecutorService executor;
    private final ExecutorService captureExecutor = Executors.newSingleThreadExecutor();
    private final Context context;
    private WareHouseAnalyzer wareHouseAnalyzer;
    private final WareHouseAnalyzer.DetectionCallback callback;
    private final ImageAnalysis imageAnalysis;
    private final String mavenModelName = "warehouse-localizer";
    private final ModelLoadingCallback loadingCallback;

    // Model input sizes
    private static final int LIVE_PREVIEW_SIZE = 640;
    private static final int CAPTURE_SIZE = 1280; // Higher resolution for capture

    /**
     * Callback interface for model loading completion
     */
    public interface ModelLoadingCallback {
        void onLoadingComplete(boolean success);
    }

    public WareHouseLocalizerHandler(Context context, WareHouseAnalyzer.DetectionCallback callback, ImageAnalysis imageAnalysis, ModelLoadingCallback loadingCallback) {
        this.context = context;
        this.callback = callback;
        this.executor = Executors.newSingleThreadExecutor();
        this.imageAnalysis = imageAnalysis;
        this.loadingCallback = loadingCallback;

        initializeWareHouseLocalizer();
        initializeCaptureLocalizer();
    }

    public void initializeWareHouseLocalizer() {
        try {
            // Initialize live preview localizer with smaller input size
            Localizer.Settings liveLocalizerSettings = createLocalizerSettings(LIVE_PREVIEW_SIZE);
            createWareHouseLocalizerWithFallback(liveLocalizerSettings);
        } catch (Exception ex) {
            if (loadingCallback != null) {
                loadingCallback.onLoadingComplete(false);
            }
            Log.e(TAG, "Model Loading: WareHouse localizer returned with exception " + ex.getMessage());
        }
    }

    /**
     * Initializes the capture localizer with higher resolution settings
     */
    public void initializeCaptureLocalizer() {
        try {
            Localizer.Settings captureLocalizerSettings = createLocalizerSettings(CAPTURE_SIZE);
            createCaptureLocalizerWithFallback(captureLocalizerSettings);
        } catch (Exception ex) {
            if (loadingCallback != null) {
                loadingCallback.onLoadingComplete(false);
            }
            Log.e(TAG, "Capture localizer initialization failed: " + ex.getMessage());
        }
    }

    /**
     * Creates localizer settings with specified input size
     */
    private Localizer.Settings createLocalizerSettings(int inputSize) {
        Localizer.Settings localizerSettings = new Localizer.Settings(mavenModelName);
        Integer[] rpo = new Integer[3];
        rpo[0] = InferencerOptions.DSP;
        rpo[1] = InferencerOptions.CPU;
        rpo[2] = InferencerOptions.GPU;

        localizerSettings.inferencerOptions.runtimeProcessorOrder = rpo;
        localizerSettings.inferencerOptions.defaultDims.height = inputSize;
        localizerSettings.inferencerOptions.defaultDims.width = inputSize;

        return localizerSettings;
    }

    private void createWareHouseLocalizerWithFallback(Localizer.Settings localizerSettings) {
        long m_Start = System.currentTimeMillis();
        Localizer.getLocalizer(localizerSettings, executor).thenAccept(localizerInstance -> {
            wareHouseLocalizer = localizerInstance;
            if(captureLocalizer!=null) {
                if (loadingCallback != null) {
                    loadingCallback.onLoadingComplete(true);
                }
                attachAnalysisAfterModelLoading();
            }
            Log.d(TAG, "WareHouseLocalizer() obj creation time =" + (System.currentTimeMillis() - m_Start) + " milli sec and input size: " + localizerSettings.inferencerOptions.defaultDims.width);
        }).exceptionally(e -> {
            if (loadingCallback != null) {
                loadingCallback.onLoadingComplete(false);
            }
            Log.e(TAG, "Fatal error: localizer creation failed - " + e.getMessage());
            return null;
        });
    }

    private void createCaptureLocalizerWithFallback(Localizer.Settings localizerSettings) {
        long m_Start = System.currentTimeMillis();
        Localizer.getLocalizer(localizerSettings, captureExecutor).thenAccept(localizerInstance -> {
            captureLocalizer = localizerInstance;
            if(wareHouseLocalizer!=null) {
                if (loadingCallback != null) {
                    loadingCallback.onLoadingComplete(true);
                }
                attachAnalysisAfterModelLoading();
            }
            Log.d(TAG, "Capture WareHouseLocalizer created in " + (System.currentTimeMillis() - m_Start) + " ms");
        }).exceptionally(e -> {
            if (loadingCallback != null) {
                loadingCallback.onLoadingComplete(false);
            }
            Log.e(TAG, "Capture localizer creation failed: " + e.getMessage());
            return null;
        });
    }

    /**
     * Stops the executor service and disposes of both WareHouseLocalizers
     */
    public void stop() {
        executor.shutdownNow();
        captureExecutor.shutdownNow();
        if (wareHouseLocalizer != null) {
            wareHouseLocalizer.dispose();
            Log.d(TAG, "Live preview warehouse localizer disposed");
            wareHouseLocalizer = null;
        }
        if (captureLocalizer != null) {
            captureLocalizer.dispose();
            Log.d(TAG, "Capture warehouse localizer disposed");
            captureLocalizer = null;
        }
    }

    public WareHouseAnalyzer getWareHouseAnalyzer() {
        return wareHouseAnalyzer;
    }

    public Localizer getCaptureLocalizer() {
        return captureLocalizer;
    }

    public void attachAnalysisAfterModelLoading(){
        wareHouseAnalyzer = new WareHouseAnalyzer(callback, wareHouseLocalizer);
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), wareHouseAnalyzer);
    }
}