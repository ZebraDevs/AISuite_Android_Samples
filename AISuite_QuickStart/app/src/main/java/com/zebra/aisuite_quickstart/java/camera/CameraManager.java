// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.camera;

import android.content.Context;
import android.hardware.camera2.CameraMetadata;
import android.util.Log;
import android.util.Size;
import android.view.Display;

import androidx.annotation.Nullable;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraProvider;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.lifecycle.LifecycleOwner;

import com.zebra.aisuite_quickstart.java.CameraXLivePreviewActivity;
import com.zebra.aisuite_quickstart.java.handlers.UIHandler;
import com.zebra.aisuite_quickstart.utils.CameraUtil;

/**
 * CameraManager handles all CameraX related operations including initialization,
 * binding use cases, and lifecycle management.
 */
public class CameraManager {
    private static final String TAG = "CameraManager";

    private final Context context;
    private final LifecycleOwner lifecycleOwner;
    private final Size selectedSize = new Size(1920, 1080);

    @Nullable
    private Camera camera;
    @Nullable
    private Preview previewUseCase;
    private ImageAnalysis analysisUseCase;
    public ProcessCameraProvider cameraProvider;
    private CameraSelector cameraSelector;
    private ResolutionSelector resolutionSelector;
    private boolean isFrontCamera = false;
    private UIHandler uiHandler;
    private CameraXLivePreviewActivity activity;

    public CameraManager(CameraXLivePreviewActivity activity, Context context, LifecycleOwner lifecycleOwner) {
        this.context = context;
        this.activity = activity;
        this.lifecycleOwner = lifecycleOwner;
        initializeCameraSelector();
        initializeResolutionSelector();
    }

    private void initializeCameraSelector() {
        int cameraFacing = CameraUtil.getPreferredCameraFacing(context);
        if (cameraFacing == CameraMetadata.LENS_FACING_BACK) {
            isFrontCamera = false;
            cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build();
        } else {
            isFrontCamera = true;
            cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build();
        }
    }

    // Setter to inject the UIHandler instance
    public void setUIHandler(UIHandler uiHandler) {
        this.uiHandler = uiHandler;
    }

    private void initializeResolutionSelector() {
        resolutionSelector = new ResolutionSelector.Builder()
                .setAspectRatioStrategy(
                        new AspectRatioStrategy(AspectRatio.RATIO_16_9,
                                AspectRatioStrategy.FALLBACK_RULE_AUTO)
                )
                .setResolutionStrategy(
                        new ResolutionStrategy(selectedSize,
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
                ).build();
    }

    public void setCameraProvider(ProcessCameraProvider provider) {
        this.cameraProvider = provider;
    }

    public void bindPreviewAndAnalysis(Preview.SurfaceProvider surfaceProvider) {
        if (cameraProvider == null) {
            return;
        }
        if (previewUseCase != null) {
            cameraProvider.unbind(previewUseCase);
        }
        if(analysisUseCase!= null){
            cameraProvider.unbind(analysisUseCase);
        }
        activity.getBinding().graphicOverlay.clear();

        Preview.Builder previewBuilder = new Preview.Builder();
        previewBuilder.setResolutionSelector(resolutionSelector);

        Display display = context.getSystemService(android.view.WindowManager.class)
                .getDefaultDisplay();
        int rotation = display.getRotation();
        previewBuilder.setTargetRotation(rotation);

        previewUseCase = previewBuilder.build();
        previewUseCase.setSurfaceProvider(surfaceProvider);

        analysisUseCase = new ImageAnalysis.Builder()
                .setResolutionSelector(resolutionSelector)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetRotation(rotation)
                .build();

        camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector,
                previewUseCase, analysisUseCase);
        if(uiHandler.isEntityViewFinder()) activity.getEntityViewController().setCameraController(camera);
    }

    public void unbindAll() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            Log.v(TAG, "Camera Unbound");
        }
    }

    public void updateTargetRotation(int rotation) {
        if (analysisUseCase != null) {
            analysisUseCase.setTargetRotation(rotation);
        }
        if (previewUseCase != null) {
            previewUseCase.setTargetRotation(rotation);
        }
    }

    public boolean isFrontCamera() {
        return isFrontCamera;
    }

    public Size getSelectedSize() {
        return selectedSize;
    }
    public CameraProvider getCameraProvider(){
        return cameraProvider;
    }
    public ImageAnalysis getAnalysisUseCase(){
        return analysisUseCase;
    }
}
