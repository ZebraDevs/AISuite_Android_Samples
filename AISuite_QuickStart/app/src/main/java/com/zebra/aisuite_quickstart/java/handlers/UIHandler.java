// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.handlers;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static com.zebra.aisuite_quickstart.utils.CommonUtils.WAREHOUSE_LOCALIZER;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;

import com.zebra.aisuite_quickstart.filtertracker.FilterDialog;
import com.zebra.aisuite_quickstart.filtertracker.FilterItem;
import com.zebra.aisuite_quickstart.java.CameraXLivePreviewActivity;
import com.zebra.aisuite_quickstart.java.camera.CameraManager;
import com.zebra.aisuite_quickstart.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * UIHandler manages UI-related operations including spinner setup,
 * overlay management, and orientation changes.
 */
public class UIHandler {
    private static final String TAG = "UIHandler";

    private final CameraXLivePreviewActivity activity;
    private final CameraManager cameraManager;

    // Model constants
    public static final String BARCODE_DETECTION = "Barcode";
    private static final String LEGACY_BARCODE_DETECTION = "Legacy Barcode";
    public static final String TEXT_OCR_DETECTION = "OCR";
    private static final String LEGACY_OCR_DETECTION = "Legacy OCR";
    public static final String ENTITY_ANALYZER = "Tracker";
    public static final String PRODUCT_RECOGNITION = "Product Recognition";
    private static final String LEGACY_PRODUCT_RECOGNITION = "Legacy Product Recognition";
    private static final String ENTITY_VIEW_FINDER = "Entity Viewfinder";

    private String selectedModel = BARCODE_DETECTION;
    private final SharedPreferences sharedPreferences;
    private boolean isEntityViewFinder = false;
    public boolean isSpinnerInitialized = false;
    private ImageCapture imageCapture;
    public boolean isInCaptureMode = false;
    private Bitmap currentCapture;
    private final ExecutorService executors = Executors.newFixedThreadPool(2);

    public UIHandler(CameraXLivePreviewActivity activity, CameraManager cameraManager, SharedPreferences sharedPreferences) {
        this.activity = activity;
        this.sharedPreferences = sharedPreferences;
        this.cameraManager = cameraManager;
    }

    public void setupSpinner() {
        ArrayAdapter<String> dataAdapter = getStringArrayAdapter();
        activity.getBinding().spinner.setAdapter(dataAdapter);
        activity.getBinding().spinner.setOnItemSelectedListener(createSpinnerListener());
        setupFilterButton();
    }

    @NonNull
    private ArrayAdapter<String> getStringArrayAdapter() {
        List<String> options = new ArrayList<>();
        options.add(BARCODE_DETECTION);
        options.add(TEXT_OCR_DETECTION);
        options.add(PRODUCT_RECOGNITION);
        options.add(ENTITY_ANALYZER);
        options.add(ENTITY_VIEW_FINDER);
        options.add(WAREHOUSE_LOCALIZER);
        // options.add(LEGACY_BARCODE_DETECTION); // uncomment to use barcode legacy option
        // options.add(LEGACY_OCR_DETECTION); // uncomment to use ocr legacy option
       // options.add(LEGACY_PRODUCT_RECOGNITION); // Uncomment to use Product recognition legacy option

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(activity, com.zebra.aisuite_quickstart.R.layout.spinner_style, options);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return dataAdapter;
    }

    private AdapterView.OnItemSelectedListener createSpinnerListener() {
        return new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                selectedModel = adapterView.getItemAtPosition(pos).toString();
                isEntityViewFinder = selectedModel.equals(ENTITY_VIEW_FINDER);

                Log.e(TAG, "selected option is " + selectedModel);
                activity.getBinding().trackerFilter.setVisibility(TextUtils.equals(selectedModel, ENTITY_ANALYZER) ? VISIBLE : GONE);

                // Lock orientation when Entity Viewfinder is selected
                if (isEntityViewFinder) {
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
                    Log.d(TAG, "Orientation locked for Entity Viewfinder mode");
                } else {
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    Log.d(TAG, "Orientation unlocked for " + selectedModel + " mode");
                }
                activity.setModelLoaded(false);
                if (selectedModel.equalsIgnoreCase(BARCODE_DETECTION) || selectedModel.equalsIgnoreCase(TEXT_OCR_DETECTION) || selectedModel.equalsIgnoreCase(PRODUCT_RECOGNITION) || selectedModel.equalsIgnoreCase(ENTITY_ANALYZER) || selectedModel.equalsIgnoreCase(WAREHOUSE_LOCALIZER)) {
                    initializeCaptureFeature();
                    activity.getBinding().captureLayout.setVisibility(VISIBLE);
                }  else {
                    activity.getBinding().captureLayout.setVisibility(GONE);
                }

                if (!isSpinnerInitialized) {
                    isSpinnerInitialized = true;
                } else {
                    activity.clearGraphicOverlay();
                    activity.stopAnalyzing();
                    cameraManager.unbindAll();
                    activity.disposeModels();
                    cameraManager.bindPreviewAndAnalysis(activity.getPreviewSurfaceProvider());
                    activity.bindAnalysisUseCase();
                }
                // Clear overlays and rebind camera

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // No action needed
            }
        };
    }

    private void setupFilterButton() {
        activity.getBinding().trackerFilter.setOnClickListener(v -> {
            FilterDialog dialog = new FilterDialog(activity);
            dialog.setCallback(options -> {
                SharedPreferences.Editor editor = sharedPreferences.edit();

                for (FilterItem option : options) {
                    editor.putBoolean(option.getTitle(), option.isChecked());
                }
                editor.apply();
                activity.setModelLoaded(false);

                activity.clearGraphicOverlay();
                activity.stopAnalyzing();
                cameraManager.unbindAll();
                activity.disposeModels();
                cameraManager.bindPreviewAndAnalysis(activity.getPreviewSurfaceProvider());
                activity.bindAnalysisUseCase();
            });
            dialog.show();
        });
    }

    public void updatePreviewVisibility(boolean isEntityViewFinder) {
        activity.getBinding().previewView.setVisibility(isEntityViewFinder ? GONE : VISIBLE);
        activity.getBinding().entityView.setVisibility(isEntityViewFinder ? VISIBLE : GONE);
    }

    public String getSelectedModel() {
        return selectedModel;
    }

    public boolean isEntityViewFinder() {
        return isEntityViewFinder;
    }
    public void setSpinnerEnabled(boolean enabled) {
        if (activity != null && activity.getBinding() != null) {
            activity.runOnUiThread(() -> {
                activity.getBinding().spinner.setEnabled(enabled);
                activity.getBinding().spinner.setAlpha(enabled ? 1.0f : 0.5f);

                activity.getBinding().trackerFilter.setEnabled(enabled);
                activity.getBinding().trackerFilter.setAlpha(enabled ? 1.0f : 0.5f);

                activity.getBinding().captureButton.setEnabled(enabled);
                activity.getBinding().captureButton.setAlpha(enabled ? 1.0f : 0.5f);
            });
        }
    }
    private void initializeCaptureFeature() {
        activity.getBinding().captureButton.setOnClickListener(v -> {
            if(selectedModel.equalsIgnoreCase(BARCODE_DETECTION) || selectedModel.equalsIgnoreCase(TEXT_OCR_DETECTION) || selectedModel.equalsIgnoreCase(PRODUCT_RECOGNITION) || selectedModel.equalsIgnoreCase(ENTITY_ANALYZER) || selectedModel.equalsIgnoreCase(WAREHOUSE_LOCALIZER)) {
                if(activity.isModelLoaded()) {
                    activity.getBinding().captureLayout.setVisibility(GONE);
                    imageCapture = cameraManager.getImageCapture();
                    activity.stopAnalyzing();
                    activity.getBinding().control.setVisibility(GONE);

                    // Submit capture initialization to background thread
                    executors.submit(() -> {
                        if (selectedModel.equalsIgnoreCase(BARCODE_DETECTION)) {
                            initializeCaptureDecoderAndCapture();
                        } else if (selectedModel.equalsIgnoreCase(TEXT_OCR_DETECTION)) {
                            initializeCaptureOCRAndCapture();
                        } else if (selectedModel.equalsIgnoreCase(PRODUCT_RECOGNITION)) {
                            initializeCaptureRecognitionAndCapture();
                        } else if (selectedModel.equalsIgnoreCase(ENTITY_ANALYZER)) {
                            initializeCaptureTracker();
                        } else if (selectedModel.equalsIgnoreCase(WAREHOUSE_LOCALIZER)) {
                            initializeCaptureWareHouseLocalizer();
                        }
                    });
                }else{
                    Toast.makeText(activity, "Model is not loaded", Toast.LENGTH_SHORT).show();
                }
            }
        });
        activity.getBinding().backToLiveButton.setOnClickListener(v -> {
            try {
                activity.getBinding().control.setVisibility(VISIBLE);
                returnToLivePreview();
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void initializeCaptureWareHouseLocalizer() {
        activity.initializeCaptureWareHouse(this::performCapture);
    }

    private void initializeCaptureOCRAndCapture() {
        activity.initializeCaptureOCR(this::performCapture);
    }

    private void initializeCaptureDecoderAndCapture() {
        activity.initializeCaptureDecoder(this::performCapture);
    }

    private void initializeCaptureRecognitionAndCapture() {
        activity.initializeCaptureRecognition(this::performCapture);
    }

    private void initializeCaptureTracker() {
        activity.initializeCaptureTracker(this::performCapture);
    }

    private void performCapture() {
        if (imageCapture == null || isInCaptureMode) return;
        Log.d(TAG, "Performing capture");
        imageCapture.takePicture(executors, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                try {
                    Log.e(TAG, "Image DIms w x h="+ imageProxy.getWidth() + " x " + imageProxy.getHeight());
                    Log.e(TAG, "Selected model" + selectedModel);
                    currentCapture = CommonUtils.rotateBitmapIfNeeded(imageProxy);
                    activity.runOnUiThread(() -> {
                        switchToCaptureMode();
                        activity.getBinding().capturedImageView.setImageBitmap(currentCapture);
                    });
                    if(selectedModel.equalsIgnoreCase(TEXT_OCR_DETECTION)) {
                        activity.processCaptureOCR(imageProxy);
                    }else if(selectedModel.equalsIgnoreCase(BARCODE_DETECTION)){
                        activity.processBarcodeWithCaptureDecoder(imageProxy);
                    }else if(selectedModel.equalsIgnoreCase(PRODUCT_RECOGNITION)){
                        activity.processCaptureRecognition(imageProxy);
                    } else if(selectedModel.equalsIgnoreCase(ENTITY_ANALYZER)){
                        activity.processCaptureTracker(imageProxy);
                    }else if(selectedModel.equalsIgnoreCase(WAREHOUSE_LOCALIZER)){
                        activity.processCaptureWareHouseLocalizer(imageProxy);
                    }

                    Log.d(TAG, "bitmap captured");
                } catch (Exception e) {
                    Log.e(TAG,"Exception occurred while capturing "+e.getMessage());
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "Capture operation failed", exception);
            }
        });
    }

    @SuppressLint("ResourceAsColor")
    private void returnToLivePreview() throws ExecutionException, InterruptedException {
        activity.runOnUiThread(() -> {
            isInCaptureMode = false;
            currentCapture = null;

            // Clear graphic overlay to remove any previous detection results
            activity.clearGraphicOverlay();

            activity.getBinding().capturedImageView.setVisibility(View.GONE);
            activity.getBinding().capturedImageView.setImageBitmap(null);
            activity.getBinding().backToLiveButton.setVisibility(View.GONE);

            activity.getBinding().trackerFilter.setVisibility(TextUtils.equals(selectedModel, ENTITY_ANALYZER) ? VISIBLE : GONE);
            activity.getBinding().previewView.setVisibility(View.VISIBLE);
            activity.getBinding().previewView.setBackgroundColor(android.R.color.black);
            activity.getBinding().graphicOverlay.setVisibility(View.VISIBLE);
            activity.getBinding().captureLayout.setVisibility(View.VISIBLE);

        });
        cameraManager.unbindAll();
        cameraManager.bindPreviewAndAnalysis(activity.getPreviewSurfaceProvider());
        if(selectedModel.equalsIgnoreCase(TEXT_OCR_DETECTION)) {
            activity.setOCRAnalysis();
        }else if(selectedModel.equalsIgnoreCase(BARCODE_DETECTION)){
            activity.setBarcodeAnalysis();
        } else if(selectedModel.equalsIgnoreCase(PRODUCT_RECOGNITION)){
            activity.clearCaptureListener();
            activity.setRecognitionAnalysis();
        } else if(selectedModel.equalsIgnoreCase(ENTITY_ANALYZER)){
            activity.setTrackerAnalysis();
        } else if(selectedModel.equalsIgnoreCase(WAREHOUSE_LOCALIZER)){
            activity.setWareHouseAnalysis();
        }
    }

    private void switchToCaptureMode() {
        activity.runOnUiThread(() -> {
            isInCaptureMode = true;

            cameraManager.unbindImageAnalysis();

            activity.getBinding().previewView.setVisibility(View.GONE);
            activity.getBinding().graphicOverlay.setVisibility(View.GONE);
            activity.getBinding().captureLayout.setVisibility(View.GONE);

            activity.getBinding().capturedImageView.setVisibility(View.VISIBLE);
            activity.getBinding().backToLiveButton.setVisibility(View.VISIBLE);
            // Ensure the button is drawn on top of the ImageView
            activity.getBinding().backToLiveButton.bringToFront();
        });
    }

}