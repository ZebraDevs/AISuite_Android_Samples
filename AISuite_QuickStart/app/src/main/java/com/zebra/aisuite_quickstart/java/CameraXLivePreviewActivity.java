// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.camera2.CameraMetadata;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.lifecycle.ViewModelProvider;

import com.zebra.ai.vision.analyzer.tracking.EntityTrackerAnalyzer;
import com.zebra.ai.vision.detector.BBox;
import com.zebra.ai.vision.detector.BarcodeDecoder;
import com.zebra.ai.vision.detector.ComplexBBox;
import com.zebra.ai.vision.detector.Recognizer;
import com.zebra.ai.vision.entity.BarcodeEntity;
import com.zebra.ai.vision.entity.Entity;
import com.zebra.ai.vision.entity.LineEntity;
import com.zebra.ai.vision.entity.ParagraphEntity;
import com.zebra.ai.vision.detector.Line;
import com.zebra.ai.vision.detector.Word;
import com.zebra.ai.vision.entity.WordEntity;
import com.zebra.ai.vision.viewfinder.EntityViewController;
import com.zebra.aisuite_quickstart.CameraXViewModel;
import com.zebra.aisuite_quickstart.R;
import com.zebra.aisuite_quickstart.databinding.ActivityCameraXlivePreviewBinding;
import com.zebra.aisuite_quickstart.filtertracker.FilterDialog;
import com.zebra.aisuite_quickstart.filtertracker.FilterItem;
import com.zebra.aisuite_quickstart.filtertracker.FilterType;
import com.zebra.aisuite_quickstart.java.analyzers.barcodetracker.BarcodeTracker;
import com.zebra.aisuite_quickstart.java.analyzers.barcodetracker.BarcodeTrackerGraphic;
import com.zebra.aisuite_quickstart.java.detectors.barcodedecodersample.BarcodeAnalyzer;
import com.zebra.aisuite_quickstart.java.detectors.barcodedecodersample.BarcodeGraphic;
import com.zebra.aisuite_quickstart.java.detectors.barcodedecodersample.BarcodeHandler;
import com.zebra.aisuite_quickstart.java.detectors.textocrsample.OCRGraphic;
import com.zebra.aisuite_quickstart.java.detectors.textocrsample.OCRHandler;
import com.zebra.aisuite_quickstart.java.detectors.textocrsample.TextOCRAnalyzer;
import com.zebra.aisuite_quickstart.java.lowlevel.productrecognitionsample.ProductRecognitionAnalyzer;
import com.zebra.aisuite_quickstart.java.lowlevel.productrecognitionsample.ProductRecognitionGraphic;
import com.zebra.aisuite_quickstart.java.lowlevel.productrecognitionsample.ProductRecognitionHandler;
import com.zebra.aisuite_quickstart.java.lowlevel.simplebarcodesample.BarcodeSample;
import com.zebra.aisuite_quickstart.java.lowlevel.simplebarcodesample.BarcodeSampleAnalyzer;
import com.zebra.aisuite_quickstart.java.lowlevel.simpleocrsample.OCRAnalyzer;
import com.zebra.aisuite_quickstart.java.lowlevel.simpleocrsample.OCRSample;
import com.zebra.aisuite_quickstart.java.viewfinder.EntityBarcodeTracker;
import com.zebra.aisuite_quickstart.java.viewfinder.EntityViewGraphic;
import com.zebra.aisuite_quickstart.utils.CameraUtil;
import com.zebra.aisuite_quickstart.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The CameraXLivePreviewActivity class is an Android activity that demonstrates the use of CameraX
 * for live camera preview and integrates multiple detection and recognition capabilities, including
 * barcode detection, text OCR, product recognition, and entity tracking.
 * <p>
 * This class provides functionality for switching between different models and use cases, handling
 * camera lifecycle, managing image analysis, and updating the UI based on detection results.
 * <p>
 * Usage:
 * - This activity is started as part of the application to demonstrate CameraX functionalities.
 * - It binds and unbinds camera use cases based on user selection and manages their lifecycle.
 * - It provides a spinner UI to select between different detection models and dynamically adapts
 * the camera preview and analysis based on the selected model.
 * <p>
 * Dependencies:
 * - CameraX: Provides camera lifecycle and use case management.
 * - BarcodeHandler, OCRHandler, ProductRecognitionHandler, BarcodeTracker, EntityBarcodeTracker:
 * Classes that handle specific detection and recognition tasks.
 * - ActivityCameraXlivePreviewBinding: Used for view binding to access UI components.
 * - GraphicOverlay: Custom view for rendering graphical overlays on camera preview.
 * - ExecutorService: Used for asynchronous task execution.
 * <p>
 * Exception Handling:
 * - Handles exceptions during analyzer setup and model disposal.
 * <p>
 * Note: Ensure that the appropriate permissions are configured in the AndroidManifest to utilize camera capabilities.
 */
public class CameraXLivePreviewActivity extends AppCompatActivity implements BarcodeAnalyzer.DetectionCallback, TextOCRAnalyzer.DetectionCallback, ProductRecognitionAnalyzer.DetectionCallback, BarcodeTracker.DetectionCallback, EntityBarcodeTracker.DetectionCallback, BarcodeSampleAnalyzer.SampleBarcodeDetectionCallback, OCRAnalyzer.DetectionCallback {

    private ActivityCameraXlivePreviewBinding binding;
    private final String TAG = "CameraXLivePreviewActivityJava";
    private static final String BARCODE_DETECTION = "Barcode";
    private static final String LEGACY_BARCODE_DETECTION = "Legacy Barcode";
    private static final String TEXT_OCR_DETECTION = "OCR";
    private static final String LEGACY_OCR_DETECTION = "Legacy OCR";
    private static final String ENTITY_ANALYZER = "Tracker";
    private static final String PRODUCT_RECOGNITION = "Product Recognition";
    private static final String ENTITY_VIEW_FINDER = "Entity Viewfinder";
    private final float SIMILARITY_THRESHOLD = 0.65f;
    @Nullable
    private Camera camera;
    @Nullable
    private Preview previewUseCase;
    private ImageAnalysis analysisUseCase;
    ProcessCameraProvider cameraProvider;
    private int imageWidth;
    private int imageHeight;
    private CameraSelector cameraSelector;
    private ResolutionSelector resolutionSelector;
    private final ExecutorService executors = Executors.newFixedThreadPool(3);
    private BarcodeHandler barcodeHandler;
    private OCRHandler ocrHandler;
    private ProductRecognitionHandler productRecognitionHandler;
    private BarcodeTracker barcodeTracker;
    private EntityBarcodeTracker entityBarcodeTracker;
    private BarcodeSample barcodeLegacySample;
    private OCRSample ocrSample;
    private String selectedModel = BARCODE_DETECTION;
    private String previousSelectedModel = "";
    private boolean isSpinnerInitialized = false;
    private static final String STATE_SELECTED_MODEL = "selected_model";
    private boolean isEntityViewFinder = false;
    private EntityViewController entityViewController;
    private EntityViewGraphic entityViewGraphic;
    private boolean isIconStyleEnable = false;
    private final Size selectedSize = new Size(1920, 1080);
    private int initialRotation = Surface.ROTATION_0;
    private DisplayManager displayManager;

    private boolean isFrontCamera = false;
    private DisplayManager.DisplayListener displayListener;

    // Store pending viewfinder resize data to apply once analyzer is ready
    private Matrix pendingTransformMatrix = null;
    private RectF pendingCropRegion = null;
    // Orientation constants for clarity
    private static final int ROTATION_0 = Surface.ROTATION_0;     // 0
    private static final int ROTATION_90 = Surface.ROTATION_90;   // 1
    private static final int ROTATION_180 = Surface.ROTATION_180; // 2
    private static final int ROTATION_270 = Surface.ROTATION_270; // 3

    private SharedPreferences sharedPreferences;
    private ArrayList<FilterItem> filterItems = new ArrayList<>();

    private boolean isBarcodeChecked = true;
    private boolean isOCRChecked = false;
    // Create a Set to hold unique barcode values
    Set<String> uniqueBarcodeValues = new HashSet<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            selectedModel = savedInstanceState.getString(STATE_SELECTED_MODEL, BARCODE_DETECTION);
        }

        binding = ActivityCameraXlivePreviewBinding.inflate(getLayoutInflater());
        sharedPreferences = getSharedPreferences(CommonUtils.PREFS_NAME, MODE_PRIVATE);
        int cameraFacing = CameraUtil.getPreferredCameraFacing(this);
        if (cameraFacing == CameraMetadata.LENS_FACING_BACK) {
            // Set up the camera to use the back camera
            isFrontCamera = false;
            cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        } else {
            // Set up the camera to use the front camera
            isFrontCamera = true;
            cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();
        }

        setContentView(binding.getRoot());

        displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        registerDisplayRotationListener();

        resolutionSelector = new ResolutionSelector.Builder()
                .setAspectRatioStrategy(
                        new AspectRatioStrategy(AspectRatio.RATIO_16_9, AspectRatioStrategy.FALLBACK_RULE_AUTO)
                )
                .setResolutionStrategy(
                        new ResolutionStrategy(selectedSize, ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
                ).build();

        new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()))
                .get(CameraXViewModel.class)
                .getProcessCameraProvider()
                .observe(
                        this,
                        provider -> {
                            cameraProvider = provider;
                            Log.v(TAG, "Binding all camera");
                            bindAllCameraUseCases();
                        });

        ArrayAdapter<String> dataAdapter = getStringArrayAdapter();
        // attaching data adapter to spinner
        binding.spinner.setAdapter(dataAdapter);

        binding.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                selectedModel = adapterView.getItemAtPosition(pos).toString();
                isEntityViewFinder = selectedModel.equals(ENTITY_VIEW_FINDER);

                Log.e(TAG, "selected option is " + selectedModel);

                binding.trackerFilter.setVisibility(TextUtils.equals(selectedModel, ENTITY_ANALYZER) ? VISIBLE : GONE);

                // Lock orientation when Entity Viewfinder is selected
                if (isEntityViewFinder) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
                    Log.d(TAG, "Orientation locked for Entity Viewfinder mode");
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    Log.d(TAG, "Orientation unlocked for " + selectedModel + " mode");
                }

                initialRotation = getWindow().getDecorView().getDisplay().getRotation();
                if (initialRotation == ROTATION_0 || initialRotation == ROTATION_180) {
                    imageWidth = selectedSize.getHeight();
                    imageHeight = selectedSize.getWidth();
                } else {
                    imageWidth = selectedSize.getWidth();
                    imageHeight = selectedSize.getHeight();
                }
                Log.d(TAG, "Updated imageWidth=" + imageWidth + ", imageHeight=" + imageHeight);

                if (!isSpinnerInitialized) {
                    isSpinnerInitialized = true;
                } else {
                    if (entityViewGraphic != null)
                        entityViewGraphic.clear();
                    runOnUiThread(() -> binding.graphicOverlay.clear());
                    stopAnalyzing();
                    unBindCameraX();
                    disposeModels();
                    bindPreviewUseCase();
                    bindAnalysisUseCase();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // No action needed
            }
        });

        binding.trackerFilter.setOnClickListener(v -> {
            FilterDialog dialog = new FilterDialog(CameraXLivePreviewActivity.this);
            dialog.setCallback(Option -> {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                isBarcodeChecked = Option.get(0).isChecked();
                isOCRChecked = Option.get(1).isChecked();
                for (FilterItem option : Option) {
                    editor.putBoolean(option.getTitle(), option.isChecked());
                }
                editor.apply();
                binding.graphicOverlay.clear();
                stopAnalyzing();
                unBindCameraX();
                disposeModels();
                bindPreviewUseCase();
                bindAnalysisUseCase();
            });
            dialog.show();
        });

        initEntityView();
    }

    private void initEntityView() {
        entityViewController = new EntityViewController(binding.entityView, this);
        entityViewController.registerEntityClickListener(entity -> {
            isIconStyleEnable = !isIconStyleEnable;
            entityViewGraphic.enableIconPen(isIconStyleEnable);
        });

        entityViewController.registerViewfinderResizeListener(entityViewResizeSpecs -> {
            if (entityBarcodeTracker != null && entityBarcodeTracker.getEntityTrackerAnalyzer() != null) {
                // Analyzer is ready, apply the transform immediately
                entityBarcodeTracker.getEntityTrackerAnalyzer().updateTransform(entityViewResizeSpecs.getSensorToViewMatrix());
                entityBarcodeTracker.getEntityTrackerAnalyzer().setCropRect(entityViewResizeSpecs.getViewfinderFOVCropRegion());

                // Clear any pending data
                pendingTransformMatrix = null;
                pendingCropRegion = null;
                Log.d(TAG, "Applied viewfinder resize specs immediately");
            } else {
                // Analyzer not ready yet, extract and store the actual VALUES

                try {
                    pendingTransformMatrix = new Matrix(entityViewResizeSpecs.getSensorToViewMatrix());
                    pendingCropRegion = new RectF(entityViewResizeSpecs.getViewfinderFOVCropRegion());
                    Log.d(TAG, "Stored pending viewfinder resize data for later application");
                } catch (Exception e) {
                    Log.e(TAG, "Failed to extract resize spec values", e);
                    pendingTransformMatrix = null;
                    pendingCropRegion = null;
                }
            }
        });

        entityViewGraphic = new EntityViewGraphic(entityViewController);
    }

    /**
     * Apply any pending viewfinder resize data to the EntityTrackerAnalyzer
     * This should be called after the analyzer is fully initialized
     */
    private void applyPendingResizeSpecs() {
        if (pendingTransformMatrix != null && pendingCropRegion != null &&
                entityBarcodeTracker != null && entityBarcodeTracker.getEntityTrackerAnalyzer() != null) {
            try {
                // Use our safely stored copies of the values
                entityBarcodeTracker.getEntityTrackerAnalyzer().updateTransform(pendingTransformMatrix);
                entityBarcodeTracker.getEntityTrackerAnalyzer().setCropRect(pendingCropRegion);
                Log.d(TAG, "Applied pending viewfinder resize data from stored values");
            } catch (Exception e) {
                Log.e(TAG, "Failed to apply pending resize data", e);
            } finally {
                // Clear the stored values
                pendingTransformMatrix = null;
                pendingCropRegion = null;
            }
        }
    }

    /**
     * Call this method when EntityBarcodeTracker is fully initialized
     * This ensures proper setup of the analyzer with any pending configurations
     */
    public void onEntityBarcodeTrackerReady() {
        Log.d(TAG, "EntityBarcodeTracker is ready, applying pending configurations");
        applyPendingResizeSpecs();
    }

    private void bindAllCameraUseCases() {
        if (cameraProvider != null) {
            // As required by CameraX API, unbinds all use cases before trying to re-bind any of them.
            cameraProvider.unbindAll();
            bindPreviewUseCase();
            bindAnalysisUseCase();
        }
    }

    private void stopAnalyzing() {
        try {
            switch (previousSelectedModel) {
                case BARCODE_DETECTION:
                    Log.i(TAG, "Stopping the barcode analyzer");
                    if (barcodeHandler != null && barcodeHandler.getBarcodeAnalyzer() != null) {
                        barcodeHandler.getBarcodeAnalyzer().stopAnalyzing();
                    }
                    break;
                case TEXT_OCR_DETECTION:
                    Log.i(TAG, "Stopping the ocr analyzer");
                    if (ocrHandler != null && ocrHandler.getOCRAnalyzer() != null) {
                        ocrHandler.getOCRAnalyzer().stopAnalyzing();
                    }
                    break;
                case ENTITY_VIEW_FINDER:
                    Log.i(TAG, "Stopping the entity view tracker analyzer");
                    if (entityBarcodeTracker != null) {
                        entityBarcodeTracker.stopAnalyzing();
                    }
                    break;
                case ENTITY_ANALYZER:
                    Log.i(TAG, "Stopping the entity tracker analyzer");
                    if (barcodeTracker != null) {
                        barcodeTracker.stopAnalyzing();
                    }
                    break;

                case PRODUCT_RECOGNITION:
                    Log.i(TAG, "Stopping the recognition analyzer");
                    if (productRecognitionHandler != null && productRecognitionHandler.getProductRecognitionAnalyzer() != null) {
                        productRecognitionHandler.getProductRecognitionAnalyzer().stopAnalyzing();
                    }
                    break;
                case LEGACY_BARCODE_DETECTION:
                    Log.i(TAG, "Stopping the barcode legacy analyzer");
                    if (barcodeLegacySample != null && barcodeLegacySample.getBarcodeAnalyzer() != null) {
                        barcodeLegacySample.getBarcodeAnalyzer().stopAnalyzing();
                    }
                    break;
                case LEGACY_OCR_DETECTION:
                    Log.i(TAG, "Stopping the legacy ocr analyzer");
                    if (ocrSample != null && ocrSample.getOCRAnalyzer() != null) {
                        ocrSample.getOCRAnalyzer().stopAnalyzing();
                    }
                    break;
                default:
                    Log.e(TAG, "Invalid selected option: " + previousSelectedModel);
            }
        } catch (Exception e) {
            Log.e(TAG, "Can not stop the analyzer: " + previousSelectedModel, e);
        }
    }

    public void disposeModels() {
        try {
            switch (previousSelectedModel) {
                case BARCODE_DETECTION:
                    Log.i(TAG, "Disposing the barcode analyzer");
                    if (barcodeHandler != null) {
                        barcodeHandler.stop();
                    }
                    break;
                case TEXT_OCR_DETECTION:
                    Log.i(TAG, "Disposing the ocr analyzer");
                    if (ocrHandler != null) {
                        ocrHandler.stop();
                    }
                    break;
                case ENTITY_VIEW_FINDER:
                    Log.i(TAG, "Disposing the entity view tracker analyzer");
                    if (entityBarcodeTracker != null) {
                        entityBarcodeTracker.stop();
                        entityBarcodeTracker = null;
                    }
                    break;
                case ENTITY_ANALYZER:
                    Log.i(TAG, "Disposing the entity tracker analyzer");
                    if (barcodeTracker != null) {
                        barcodeTracker.stop();
                    }
                    break;

                case PRODUCT_RECOGNITION:
                    Log.i(TAG, "Disposing the recognition analyzer");
                    if (productRecognitionHandler != null) {
                        productRecognitionHandler.stop();
                    }
                    break;
                case LEGACY_BARCODE_DETECTION:
                    Log.i(TAG, "Disposing the barcode legacy analyzer");
                    if (barcodeLegacySample != null) {
                        barcodeLegacySample.stop();
                    }
                    break;
                case LEGACY_OCR_DETECTION:
                    Log.i(TAG, "Disposing the legacy ocr analyzer");
                    if (ocrSample != null) {
                        ocrSample.stop();
                    }
                    break;
                default:
                    Log.e(TAG, "Invalid selected option: " + previousSelectedModel);
            }
        } catch (Exception e) {
            Log.e(TAG, "Can not dispose the analyzer: " + previousSelectedModel, e);
        }
    }

    @NonNull
    private ArrayAdapter<String> getStringArrayAdapter() {
        List<String> options = new ArrayList<>();

        options.add(BARCODE_DETECTION);
        options.add(TEXT_OCR_DETECTION);
        options.add(ENTITY_ANALYZER);
        options.add(PRODUCT_RECOGNITION);
        options.add(ENTITY_VIEW_FINDER);
        //options.add(LEGACY_BARCODE_DETECTION); // uncomment to use barcode legacy option
        // options.add(LEGACY_OCR_DETECTION); // uncomment to use ocr legacy option

        // Creating adapter for spinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, R.layout.spinner_style, options);
        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return dataAdapter;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putString(STATE_SELECTED_MODEL, selectedModel);
    }

    private Rect mapBoundingBoxToOverlay(Rect bbox) {
        Display display = getWindowManager().getDefaultDisplay();
        int currentRotation = display.getRotation();

        int relativeRotation = ((currentRotation - initialRotation + 4) % 4);

        int overlayWidth = binding.graphicOverlay.getWidth();
        int overlayHeight = binding.graphicOverlay.getHeight();

        if (overlayWidth == 0 || overlayHeight == 0) {
            return bbox;
        }

        Rect transformedBbox = transformBoundingBoxForRotation(bbox, relativeRotation);

        int effectiveImageWidth = imageWidth;
        int effectiveImageHeight = imageHeight;

        boolean isTablet = isTablet(this.getApplicationContext());
        if ((isTablet && (relativeRotation == ROTATION_0 || relativeRotation == ROTATION_180)) ||
                (!isTablet && (relativeRotation == ROTATION_90 || relativeRotation == ROTATION_270))) {
            effectiveImageWidth = imageHeight;
            effectiveImageHeight = imageWidth;
        }

        float scaleX = (float) overlayWidth / effectiveImageWidth;
        float scaleY = (float) overlayHeight / effectiveImageHeight;
        float scale = Math.max(scaleX, scaleY);

        float offsetX = (overlayWidth - effectiveImageWidth * scale) / 2f;
        float offsetY = (overlayHeight - effectiveImageHeight * scale) / 2f;

        // Handle mirroring for front camera
        if (isFrontCamera) {
            int left = transformedBbox.left;
            int right = transformedBbox.right;

            // Calculate mirrored left and right
            transformedBbox.left = effectiveImageWidth - right;
            transformedBbox.right = effectiveImageWidth - left;
        }

        return new Rect(
                (int) (transformedBbox.left * scale + offsetX),
                (int) (transformedBbox.top * scale + offsetY),
                (int) (transformedBbox.right * scale + offsetX),
                (int) (transformedBbox.bottom * scale + offsetY)
        );
    }


    private Rect transformBoundingBoxForRotation(Rect bbox, int relativeRotation) {
        // relativeRotation values:
        // 0: No rotation (0 degrees)
        // 1: 90 degrees clockwise
        // 2: 180 degrees
        // 3: 270 degrees clockwise
        // These values are calculated based on the difference between current and initial device rotation.
        // The transformation is needed to map the bounding box from the image coordinate system to the display coordinate system.
        switch (relativeRotation) {
            case ROTATION_0: // No transformation needed, image is already aligned
                // No transformation
                return new Rect(bbox);
            case ROTATION_90:
                // 90 degree clockwise rotation: swap x/y and adjust for width
                // left becomes top, top becomes (imageWidth - right), right becomes bottom, bottom becomes (imageWidth - left)
                return new Rect(
                        bbox.top,
                        imageWidth - bbox.right,
                        bbox.bottom,
                        imageWidth - bbox.left
                );
            case ROTATION_180:
                // 180 degree rotation: flip both axes
                // left becomes (imageWidth - right), top becomes (imageHeight - bottom), right becomes (imageWidth - left), bottom becomes (imageHeight - top)
                return new Rect(
                        imageWidth - bbox.right,
                        imageHeight - bbox.bottom,
                        imageWidth - bbox.left,
                        imageHeight - bbox.top
                );
            case ROTATION_270:
                // 270 degree clockwise rotation: swap x/y and adjust for height
                // left becomes (imageHeight - bottom), top becomes left, right becomes (imageHeight - top), bottom becomes right
                return new Rect(
                        imageHeight - bbox.bottom,
                        bbox.left,
                        imageHeight - bbox.top,
                        bbox.right
                );
            default:
                Log.w(TAG, "Unknown relative rotation: " + relativeRotation + ", using original bbox");
                return new Rect(bbox);
        }
    }

    // Handles barcode detection results and updates the graphical overlay
    @Override
    public void onDetectionResult(List<BarcodeEntity> result) {
        List<Rect> rects = new ArrayList<>();
        List<String> decodedStrings = new ArrayList<>();


        runOnUiThread(() -> {
            binding.graphicOverlay.clear();
            if (result != null) {

                for (BarcodeEntity bb : result) {
                    Rect rect = bb.getBoundingBox();
                    if (rect != null) {
                        Log.d(TAG, String.format("Original bbox: %s", rect));
                        Rect overlayRect = mapBoundingBoxToOverlay(rect);
                        Log.d(TAG, String.format("Mapped bbox: %s", overlayRect));
                        rects.add(overlayRect);
                        decodedStrings.add(bb.getValue());
                    }
                    Log.e(TAG, "Detected entity - Value: " + bb.getValue());
                    Log.e(TAG, "Detected entity - Symbology: " + bb.getSymbology());
                }


                binding.graphicOverlay.add(new BarcodeGraphic(binding.graphicOverlay, rects, decodedStrings));
            }
        });

    }

    // Handles text OCR detection results and updates the graphical overlay
    @Override
    public void onDetectionTextResult(List<ParagraphEntity> list) {
        List<Rect> rects = new ArrayList<>();
        List<String> decodedStrings = new ArrayList<>();
        runOnUiThread(() -> {
            binding.graphicOverlay.clear();
            for (ParagraphEntity paragraphEntity : list) {
                List<LineEntity> lineEntities = paragraphEntity.getLines();
                for (LineEntity lineEntity : lineEntities) {
                    for (WordEntity wordEntity : lineEntity.getWords()) {
                        ComplexBBox bbox = wordEntity.getComplexBBox();
                        if(!wordEntity.getText().isEmpty()) {

                            if (bbox != null && bbox.x != null && bbox.y != null && bbox.x.length >= 3 && bbox.y.length >= 3) {
                                float minX = bbox.x[0], maxX = bbox.x[2], minY = bbox.y[0], maxY = bbox.y[2];
                                if(minX > maxX){
                                    float temp = minX;
                                    minX = maxX;
                                    maxX = temp;
                                }
                                if(minY > maxY){
                                    float temp = minY;
                                    minY = maxY;
                                    maxY = temp;
                                }

                                Rect rect = new Rect((int) minX, (int) minY, (int) maxX, (int) maxY);
                                Rect overlayRect = mapBoundingBoxToOverlay(rect);
                                String decodedValue = wordEntity.getText();
                                rects.add(overlayRect);
                                decodedStrings.add(decodedValue);

                            }
                        }
                    }
                }

                binding.graphicOverlay.add(new OCRGraphic(binding.graphicOverlay, rects, decodedStrings));
            }
        });

    }

    // Handles entity tracking results and updates the graphical overlay
    @Override
    public synchronized void handleEntities(EntityTrackerAnalyzer.Result result) {
        List<Rect> barcodeRects = new ArrayList<>();
        List<String> barcodeStrings = new ArrayList<>();
        List<Rect> ocrRects = new ArrayList<>();
        List<String> ocrStrings = new ArrayList<>();
        List<? extends Entity> barcodeEntities;
        List<? extends Entity> ocrEntities;
        if (barcodeTracker.getBarcodeDecoder() != null) {
            barcodeEntities = result.getValue(barcodeTracker.getBarcodeDecoder());
        } else {
            barcodeEntities = null;
        }

        if (barcodeTracker.getTextOCR() != null) {
            ocrEntities = result.getValue(barcodeTracker.getTextOCR());
        } else {
            ocrEntities = null;
        }

        runOnUiThread(() -> {
            binding.graphicOverlay.clear();
            if (barcodeEntities != null) {
                for (Entity entity : barcodeEntities) {
                    if (entity instanceof BarcodeEntity) {
                        BarcodeEntity bEntity = (BarcodeEntity) entity;
                        Rect rect = bEntity.getBoundingBox();
                        if (rect != null) {
                            Rect overlayRect = mapBoundingBoxToOverlay(rect);

                            barcodeRects.add(overlayRect);
                            String hashCode = String.valueOf(bEntity.hashCode());
                            // Ensure the string has at least 4 characters
                            if (hashCode.length() >= 4) {
                                // Get the last four digits
                                hashCode = hashCode.substring(hashCode.length() - 4);

                            }
                            String decodedValue =bEntity.getValue();
                            barcodeStrings.add(hashCode + ":" + decodedValue);
                            // Add the unique value to the Set
                            if(!decodedValue.isEmpty()) uniqueBarcodeValues.add(decodedValue);
                            Log.d(TAG, "Tracker UUID: " + hashCode + " Tracker Detected entity - Value: " + decodedValue);
                        }
                    }

                }
            }
            if (ocrEntities != null) {
                for (Entity entity : ocrEntities) {
                    //count.getAndIncrement();
                    if (entity instanceof ParagraphEntity) {
                        ParagraphEntity pEntity = (ParagraphEntity) entity;
                        Log.i(TAG,"Paragraph Entity detected" +pEntity);
                        List<LineEntity> lineEntities = pEntity.getLines();
                        Log.i(TAG,"Lines detected" +lineEntities.size());
//
                        for (LineEntity lineEntity : lineEntities) {

                            for (WordEntity wordEntity : lineEntity.getWords()) {
                                ComplexBBox bbox = wordEntity.getComplexBBox();

                                if (bbox != null && bbox.x != null && bbox.y != null && bbox.x.length >= 3 && bbox.y.length >= 3) {
                                    float minX = bbox.x[0], maxX = bbox.x[2], minY = bbox.y[0], maxY = bbox.y[2];

                                    Rect rect = new Rect((int) minX, (int) minY, (int) maxX, (int) maxY);
                                    Rect overlayRect = mapBoundingBoxToOverlay(rect);
                                    String decodedValue = wordEntity.getText();
                                    ocrRects.add(overlayRect);
//
                                    ocrStrings.add(decodedValue);
                                }
                            }
                        }
                    }

                }
            }
            if (!barcodeRects.isEmpty())
                binding.graphicOverlay.add(new BarcodeTrackerGraphic(binding.graphicOverlay, barcodeRects, barcodeStrings));
            if (!ocrRects.isEmpty())
                binding.graphicOverlay.add(new OCRGraphic(binding.graphicOverlay, ocrRects, ocrStrings));
        });
    }

    // Handles entities for the entity view tracker and updates the graphical overlay
    @Override
    public void handleEntitiesForEntityView(EntityTrackerAnalyzer.Result result) {
        Log.d(TAG, "Handle View Entity - Result received");

        // Apply any pending resize specs now that the analyzer is ready
        applyPendingResizeSpecs();

        List<? extends Entity> entities = null;
        if (entityBarcodeTracker != null && entityBarcodeTracker.getBarcodeDecoder() != null) {
            entities = result.getValue(entityBarcodeTracker.getBarcodeDecoder());
            Log.d(TAG, "EntityBarcodeTracker decoder available, entities count: " + (entities != null ? entities.size() : "null"));
        } else {
            Log.w(TAG, "EntityBarcodeTracker or decoder is null - tracker: " + (entityBarcodeTracker != null) + ", decoder: " + (entityBarcodeTracker != null && entityBarcodeTracker.getBarcodeDecoder() != null));
        }

        if (entityViewGraphic != null) {
            entityViewGraphic.clear();
        } else {
            Log.w(TAG, "EntityViewGraphic is null");
        }

        if (entities != null) {
            Log.d(TAG, "Processing " + entities.size() + " entities for entity view");
            for (Entity entity : entities) {
                if (entity instanceof BarcodeEntity) {
                    BarcodeEntity bEntity = (BarcodeEntity) entity;
                    Rect rect = bEntity.getBoundingBox();
                    if (rect != null) {
                        Log.d(TAG, "Adding entity to view - Value: " + bEntity.getValue() + ", BBox: " + rect);
                        entityViewGraphic.addEntity(bEntity);
                    } else {
                        Log.w(TAG, "Entity has null bounding box - Value: " + bEntity.getValue());
                    }
                }
            }
            entityViewGraphic.render();
            Log.d(TAG, "Rendered entities on entity view");
        } else {
            Log.w(TAG, "No entities to process for entity view");
        }
    }

    // Handles product recognition results and updates the graphical overlay
    @Override
    public void onDetectionRecognitionResult(BBox[] detections, BBox[] products, Recognizer.Recognition[] recognitions) {
        runOnUiThread(() -> {

            binding.graphicOverlay.clear();
            if (detections != null) {
                List<Rect> labelShelfRects = new ArrayList<>();
                List<Rect> labelPegRects = new ArrayList<>();
                List<Rect> shelfRects = new ArrayList<>();
                List<Rect> recognizedRects = new ArrayList<>();
                List<String> decodedStrings = new ArrayList<>();
                BBox[] labelShelfObjects = Arrays.stream(detections).filter(x -> x.cls == 2).toArray(BBox[]::new);
                BBox[] labelPegObjects = Arrays.stream(detections).filter(x -> x.cls == 3).toArray(BBox[]::new);
                BBox[] shelfObjects = Arrays.stream(detections).filter(x -> x.cls == 4).toArray(BBox[]::new);
                for (BBox bBox : labelShelfObjects) {
                    Rect rect = new Rect((int) bBox.xmin, (int) bBox.ymin, (int) bBox.xmax, (int) bBox.ymax);
                    Rect overlayRect = mapBoundingBoxToOverlay(rect);
                    labelShelfRects.add(overlayRect);
                }
                for (BBox bBox : labelPegObjects) {
                    Rect rect = new Rect((int) bBox.xmin, (int) bBox.ymin, (int) bBox.xmax, (int) bBox.ymax);
                    Rect overlayRect = mapBoundingBoxToOverlay(rect);
                    labelPegRects.add(overlayRect);
                }
                for (BBox bBox : shelfObjects) {
                    Rect rect = new Rect((int) bBox.xmin, (int) bBox.ymin, (int) bBox.xmax, (int) bBox.ymax);
                    Rect overlayRect = mapBoundingBoxToOverlay(rect);
                    shelfRects.add(overlayRect);
                }
                if (recognitions.length == 0) {
                    decodedStrings.add("No products found");
                    recognizedRects.add(new Rect(250, 250, 0, 0));
                } else {
                    Log.v(TAG, "products length :" + products.length + " recognitions length: " + recognitions.length);
                    for (int i = 0; i < products.length; i++) {
                        if (recognitions[i].similarity[0] > SIMILARITY_THRESHOLD) {
                            BBox bBox = products[i];
                            Rect rect = new Rect((int) bBox.xmin, (int) bBox.ymin, (int) bBox.xmax, (int) bBox.ymax);
                            Rect overlayRect = mapBoundingBoxToOverlay(rect);
                            recognizedRects.add(overlayRect);
                            decodedStrings.add(recognitions[i].sku[0]);
                        }
                    }
                }
                binding.graphicOverlay.add(new ProductRecognitionGraphic(binding.graphicOverlay, labelShelfRects, labelPegRects, shelfRects, recognizedRects, decodedStrings));
            }
        });
    }

    private void bindAnalysisUseCase() {
        if (cameraProvider == null) {
            return;
        }
        previousSelectedModel = selectedModel;
        isBarcodeChecked =sharedPreferences.getBoolean(FilterDialog.BARCODE_TRACKER, true);
        isOCRChecked =sharedPreferences.getBoolean(FilterDialog.OCR_TRACKER, false);

        try {
            switch (selectedModel) {
                case BARCODE_DETECTION:
                    Log.i(TAG, "Using Barcode Decoder");
                    executors.execute(() -> {
                        barcodeHandler = new BarcodeHandler(this, this, analysisUseCase);
                    });

                    break;
                case TEXT_OCR_DETECTION:
                    Log.i(TAG, "Using Text OCR");
                    executors.execute(() -> {
                        ocrHandler = new OCRHandler(this, this, analysisUseCase);

                    });
                    break;
                case ENTITY_VIEW_FINDER:
                    Log.i(TAG, "Using Entity View Analyzer");
                    executors.execute(() -> {
                        entityBarcodeTracker = new EntityBarcodeTracker(this, this, analysisUseCase);
                    });
                    break;
                case ENTITY_ANALYZER:
                    Log.i(TAG, "Using Entity Analyzer");
                    executors.execute(() -> {
                        barcodeTracker = new BarcodeTracker(this, this, analysisUseCase, FilterType.getFilterType(isBarcodeChecked, isOCRChecked));
                    });

                    break;
                case PRODUCT_RECOGNITION:
                    Log.i(TAG, "Using Product Recognition");
                    executors.execute(() -> {
                        productRecognitionHandler = new ProductRecognitionHandler(CameraXLivePreviewActivity.this, CameraXLivePreviewActivity.this, analysisUseCase);
                    });

                    break;
                case LEGACY_BARCODE_DETECTION:
                    Log.i(TAG, "Using Legacy Barcode Detection");
                    executors.execute(() -> {
                        barcodeLegacySample = new BarcodeSample(this, this, analysisUseCase);
                    });

                    break;
                case LEGACY_OCR_DETECTION:
                    Log.i(TAG, "Using Legacy Text OCR");
                    executors.execute(() -> {
                        ocrSample = new OCRSample(this, this, analysisUseCase);
                    });
                    break;
                default:
                    throw new IllegalStateException("Invalid model name");
            }
        } catch (Exception e) {
            Log.e(TAG, "Can not create model for : " + selectedModel, e);
            return;
        }
        cameraProvider.bindToLifecycle(/* lifecycleOwner= */ this, cameraSelector, analysisUseCase);
    }


    private void bindPreviewUseCase() {
        if (cameraProvider == null) {
            return;
        }
        if (previewUseCase != null) {
            binding.graphicOverlay.clear();
            cameraProvider.unbind(previewUseCase);
        }
        if (analysisUseCase != null) {
            cameraProvider.unbind(analysisUseCase);
        }

        Preview.Builder builder = new Preview.Builder();

        builder.setResolutionSelector(resolutionSelector);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        builder.setTargetRotation(rotation);

        analysisUseCase = new ImageAnalysis.Builder()
                .setResolutionSelector(resolutionSelector)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetRotation(rotation)
                .build();

        previewUseCase = builder.build();
        binding.previewView.setVisibility(isEntityViewFinder ? GONE : VISIBLE);
        binding.entityView.setVisibility(isEntityViewFinder ? VISIBLE : GONE);
        if (isEntityViewFinder) {
            previewUseCase.setSurfaceProvider(entityViewController.getSurfaceProvider());
        } else {
            previewUseCase.setSurfaceProvider(binding.previewView.getSurfaceProvider());
        }
        camera = cameraProvider.bindToLifecycle(/* lifecycleOwner= */ this, cameraSelector, previewUseCase, analysisUseCase);
        if (isEntityViewFinder) {
            entityViewController.setCameraController(camera);
        }
    }

    public void onBackPressed() {
        super.onBackPressed();
    }

    public void onResume() {
        super.onResume();
        Log.v(TAG, "OnResume called");

        int currentRotation = getWindowManager().getDefaultDisplay().getRotation();
        if (currentRotation != initialRotation) {
            Log.d(TAG, "Rotation changed during pause, updating initialRotation from " + initialRotation + " to " + currentRotation);
            initialRotation = currentRotation;
            if (analysisUseCase != null) analysisUseCase.setTargetRotation(currentRotation);
            // check if the device rotation is changes when suspended (0-> 0°, 2 -> 180°)
            if (initialRotation == ROTATION_0 || initialRotation == ROTATION_180) {
                imageWidth = selectedSize.getHeight();
                imageHeight = selectedSize.getWidth();
            } else {
                imageWidth = selectedSize.getWidth();
                imageHeight = selectedSize.getHeight();
            }
            Log.d(TAG, "Updated imageWidth=" + imageWidth + ", imageHeight=" + imageHeight);
        }
        if (isSpinnerInitialized) bindAllCameraUseCases();
    }

    public void onPause() {
        super.onPause();
        Log.v(TAG, "onPause called");
        stopAnalyzing();
        unBindCameraX();
        disposeModels();
    }

    @Override
    protected void onDestroy() {
        if (displayManager != null && displayListener != null) {
            displayManager.unregisterDisplayListener(displayListener);
        }
        // Iterate through the uniqueBarcodeValues Set and log each value
        Log.d(TAG, "Total barcodes size " + uniqueBarcodeValues.size());
        for (String value : uniqueBarcodeValues) {
            Log.d(TAG, "Unique Barcode Value: " + value);
        }
        super.onDestroy();

    }

    private void registerDisplayRotationListener() {
        if (displayManager == null) return;
        displayListener = new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayAdded(int displayId) {
            }

            @Override
            public void onDisplayRemoved(int displayId) {
            }

            @Override
            public void onDisplayChanged(int displayId) {
                Display defaultDisplay = getWindowManager().getDefaultDisplay();
                if (defaultDisplay == null || defaultDisplay.getDisplayId() != displayId) return;
                final int newRotation = defaultDisplay.getRotation();
                if (newRotation == initialRotation) return;
                runOnUiThread(() -> {
                    initialRotation = newRotation;
                    if (analysisUseCase != null) analysisUseCase.setTargetRotation(newRotation);

                    if (initialRotation == ROTATION_0 || initialRotation == ROTATION_180) {
                        imageWidth = selectedSize.getHeight();
                        imageHeight = selectedSize.getWidth();
                    } else {
                        imageWidth = selectedSize.getWidth();
                        imageHeight = selectedSize.getHeight();
                    }
                    Log.i(TAG, "Display changed, updated targetRotation and dimensions: rotation=" + newRotation + ", imageWidth=" + imageWidth + ", imageHeight=" + imageHeight);
                });
            }
        };
        displayManager.registerDisplayListener(displayListener, null);
    }

    private void unBindCameraX() {
        if (cameraProvider != null) {
            // As required by CameraX API, unbinds all use cases before trying to re-bind any of them.
            cameraProvider.unbindAll();
            Log.v(TAG, "Camera Unbounded");
        }
    }

    /**
     * Determines if the current device is a tablet based on screen size configuration.
     * Useful for adjusting bounding box scaling between normal Android devices and tablets.
     */
    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    /**
     * Callback method invoked when barcode detection results are available.
     *
     * @param barcodes An array of BarcodeDecoder.Result representing detected barcodes.
     */
    @Override
    public void onDetectionResult(BarcodeDecoder.Result[] barcodes) {
        List<Rect> rects = new ArrayList<>();
        List<String> decodedStrings = new ArrayList<>();

        runOnUiThread(() -> {
            binding.graphicOverlay.clear();
            for (BarcodeDecoder.Result barcode : barcodes) {
                String decodedString = barcode.value;
                BBox bbox = barcode.bboxData;
                Rect rect = new Rect((int) bbox.xmin, (int) bbox.ymin, (int) bbox.xmax, (int) bbox.ymax);
                Rect overlayRect = mapBoundingBoxToOverlay(rect);
                rects.add(overlayRect);
                decodedStrings.add(barcode.value);

                Log.d(TAG, "Symbology Type " + barcode.symbologytype);
                Log.d(TAG, "Decoded barcode: " + decodedString);
            }
            binding.graphicOverlay.add(new BarcodeGraphic(binding.graphicOverlay, rects, decodedStrings));
        });
    }

    /**
     * Callback method invoked when OCR text detection results are available.
     *
     * @param words An array of Word objects representing detected words.
     */
    @Override
    public void onDetectionTextResult(Word[] words) {
        List<Rect> rects = new ArrayList<>();
        List<String> decodedStrings = new ArrayList<>();
        runOnUiThread(() -> {
            binding.graphicOverlay.clear();
            for (Word word : words) {
                // Append each word's content followed by a newline
                if (word.decodes.length > 0) {
                    ComplexBBox bbox = word.bbox;

                    if (bbox != null && bbox.x != null && bbox.y != null && bbox.x.length >= 3 && bbox.y.length >= 3) {
                        float minX = bbox.x[0], maxX = bbox.x[2], minY = bbox.y[0], maxY = bbox.y[2];

                        Rect rect = new Rect((int) minX, (int) minY, (int) maxX, (int) maxY);
                        Rect overlayRect = mapBoundingBoxToOverlay(rect);
                        String decodedValue = word.decodes[0].content;
                        rects.add(overlayRect);
                        decodedStrings.add(decodedValue);

                    }
                }
            }
            binding.graphicOverlay.add(new OCRGraphic(binding.graphicOverlay, rects, decodedStrings));

        });

    }
}
