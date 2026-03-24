package com.zebra.aisuite_quickstart.java.detectors.warehouselocalizer;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.zebra.ai.vision.detector.AIVisionSDKException;
import com.zebra.ai.vision.detector.ImageData;
import com.zebra.ai.vision.detector.Localizer;
import com.zebra.ai.vision.entity.LocalizerEntity;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class WareHouseAnalyzer implements ImageAnalysis.Analyzer{
    /**
     * Interface for handling the results of the barcode detection process.
     * Implement this interface to define how detection results are processed.
     */
    public interface DetectionCallback {
        void onWareHouseLocalizerDetectionResult(List<LocalizerEntity> result);
    }
    private static final String TAG = "WareHouseAnalyzer";
    private final WareHouseAnalyzer.DetectionCallback callback;
    private final Localizer localizer;
    private final ExecutorService executorService;
    private volatile boolean isAnalyzing = true;
    private volatile boolean isStopped = false;

    public WareHouseAnalyzer(WareHouseAnalyzer.DetectionCallback callback, Localizer localizer) {
        this.callback = callback;
        this.localizer = localizer;
        this.executorService = Executors.newSingleThreadExecutor();
    }
    @Override
    public void analyze(@NonNull ImageProxy image) {
        if (!isAnalyzing || isStopped) {
            image.close();
            return;
        }

        isAnalyzing = false; // Prevent re-entry

        Future<?> future = executorService.submit(() -> {
            try {
                Log.d(TAG, "Starting image analysis");
                localizer.process(ImageData.fromImageProxy(image))
                        .thenAccept(result -> {
                            if (!isStopped) {
                                if(!result.isEmpty()) callback.onWareHouseLocalizerDetectionResult(result);
                            }
                            image.close();
                            isAnalyzing = true;
                        })
                        .exceptionally(ex -> {
                            Log.e(TAG, "Error in completable future result " + ex.getMessage());
                            image.close();
                            isAnalyzing = true;
                            return null;
                        });
            } catch (AIVisionSDKException e) {
                Log.e(TAG, Objects.requireNonNull(e.getMessage()));
                image.close();
                isAnalyzing = true;
            }
        });
        // Cancel the task if the analyzer is stopped
        if (isStopped) {
            future.cancel(true);
        }
    }
    /**
     * Stops the analysis process and terminates any ongoing tasks. This method should be
     * called to release resources and halt image analysis when it is no longer required.
     */
    public void stopAnalyzing() {
        isStopped = true;
        executorService.shutdownNow(); // Attempt to cancel ongoing tasks
    }
}