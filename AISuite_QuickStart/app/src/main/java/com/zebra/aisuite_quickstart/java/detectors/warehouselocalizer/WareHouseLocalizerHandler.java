package com.zebra.aisuite_quickstart.java.detectors.warehouselocalizer;

import android.content.Context;
import android.util.Log;

import androidx.camera.core.ImageAnalysis;
import androidx.core.content.ContextCompat;

import com.zebra.ai.vision.detector.AIVisionSDK;
import com.zebra.ai.vision.detector.InferencerOptions;
import com.zebra.ai.vision.detector.Localizer;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WareHouseLocalizerHandler {
    private static final String TAG = "WareHouseLocalizerHandler";
    private Localizer localizer;
    private final ExecutorService executor;
    private final Context context;
    private WareHouseAnalyzer wareHouseAnalyzer;
    private final WareHouseAnalyzer.DetectionCallback callback;
    private final ImageAnalysis imageAnalysis;
    private String mavenModelName = "warehouse-localizer";

    public WareHouseLocalizerHandler(Context context, WareHouseAnalyzer.DetectionCallback callback, ImageAnalysis imageAnalysis) {
        this.context = context;
        this.callback = callback;
        this.executor = Executors.newSingleThreadExecutor();
        this.imageAnalysis = imageAnalysis;

        initializeLocalizer();
    }

    private void initializeLocalizer() {
        try {
            long mStart = System.currentTimeMillis();
            Localizer.Settings locSettings = new Localizer.Settings(mavenModelName);
            long diff = System.currentTimeMillis() - mStart;
            Log.e("Profiling", "WareHouse Localizer.settings() obj creation time =" + diff + " milli sec");

            Integer[] rpo = new Integer[3];
            rpo[0] = InferencerOptions.DSP;
            rpo[1] = InferencerOptions.CPU;
            rpo[2] = InferencerOptions.GPU;

            locSettings.inferencerOptions.runtimeProcessorOrder = rpo;
            locSettings.inferencerOptions.defaultDims.height = 640;
            locSettings.inferencerOptions.defaultDims.width = 640;

            long start = System.currentTimeMillis();
            Localizer.getLocalizer(locSettings, executor).thenAccept(localizerInstance -> {
                localizer = localizerInstance;
                Log.e("Profiling", "WareHouse Localizer(locSettings) obj creation / model loading time =" + (System.currentTimeMillis() - start) + " milli sec");
                String[] classes = localizer.getSupportedClasses();
                List<String> list = Arrays.asList(classes);
                Log.e(TAG, "classes=" + list);
                wareHouseAnalyzer = new WareHouseAnalyzer(callback, localizer);
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), wareHouseAnalyzer);
            }).exceptionally(e -> {
                Log.e(TAG,"Error occurred :"+e.getMessage());
                return null;
            });
            Log.e("Profiling", "WareHouse Localizer model archive info=" + AIVisionSDK.getInstance(context).getModelArchiveInfo( "warehouse-localizer"));

        } catch (Exception e) {
            Log.e(TAG, "Fatal error: load failed - " + e.getMessage());
        }
    }

    public void stop(){
        if(localizer!=null){
            localizer.dispose();
            localizer = null;
        }
    }

    public WareHouseAnalyzer getWareHouseAnalyzer(){
        return wareHouseAnalyzer;
    }
}