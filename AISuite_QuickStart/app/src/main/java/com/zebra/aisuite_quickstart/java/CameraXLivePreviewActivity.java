// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java;


import android.content.SharedPreferences;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.zebra.ai.vision.analyzer.tracking.EntityTrackerAnalyzer;
import com.zebra.ai.vision.detector.BBox;
import com.zebra.ai.vision.detector.BarcodeDecoder;
import com.zebra.ai.vision.detector.Recognizer;
import com.zebra.ai.vision.detector.Word;
import com.zebra.ai.vision.entity.BarcodeEntity;
import com.zebra.ai.vision.entity.Entity;
import com.zebra.ai.vision.entity.ParagraphEntity;
import com.zebra.ai.vision.viewfinder.EntityViewController;
import com.zebra.aisuite_quickstart.CameraXViewModel;
import com.zebra.aisuite_quickstart.R;
import com.zebra.aisuite_quickstart.databinding.ActivityCameraXlivePreviewBinding;
import com.zebra.aisuite_quickstart.filtertracker.FilterDialog;
import com.zebra.aisuite_quickstart.java.analyzers.tracker.Tracker;
import com.zebra.aisuite_quickstart.java.camera.CameraManager;
import com.zebra.aisuite_quickstart.java.detectors.barcodedecodersample.BarcodeAnalyzer;
import com.zebra.aisuite_quickstart.java.detectors.barcodedecodersample.BarcodeHandler;
import com.zebra.aisuite_quickstart.java.detectors.textocrsample.OCRHandler;
import com.zebra.aisuite_quickstart.java.detectors.textocrsample.TextOCRAnalyzer;
import com.zebra.aisuite_quickstart.java.handlers.BoundingBoxMapper;
import com.zebra.aisuite_quickstart.java.handlers.DetectionResultHandler;
import com.zebra.aisuite_quickstart.java.handlers.UIHandler;
import com.zebra.aisuite_quickstart.java.detectors.productrecognition.ProductRecognitionAnalyzer;
import com.zebra.aisuite_quickstart.java.detectors.productrecognition.ProductRecognitionHandler;
import com.zebra.aisuite_quickstart.java.lowlevel.productrecognitionsample.ProductRecognitionSample;
import com.zebra.aisuite_quickstart.java.lowlevel.productrecognitionsample.ProductRecognitionSampleAnalyzer;
import com.zebra.aisuite_quickstart.java.lowlevel.simplebarcodesample.BarcodeSample;
import com.zebra.aisuite_quickstart.java.lowlevel.simplebarcodesample.BarcodeSampleAnalyzer;
import com.zebra.aisuite_quickstart.java.lowlevel.simpleocrsample.OCRAnalyzer;
import com.zebra.aisuite_quickstart.java.lowlevel.simpleocrsample.OCRSample;
import com.zebra.aisuite_quickstart.java.viewfinder.EntityBarcodeTracker;
import com.zebra.aisuite_quickstart.java.viewfinder.EntityViewGraphic;
import com.zebra.aisuite_quickstart.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
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
 * - BarcodeHandler, OCRHandler, ProductRecognitionHandler, Tracker, EntityBarcodeTracker:
 * Classes that handle specific detection and recognition tasks.
 * - ActivityCameraXLivePreviewBinding: Used for view binding to access UI components.
 * - GraphicOverlay: Custom view for rendering graphical overlays on camera preview.
 * - ExecutorService: Used for asynchronous task execution.
 * <p>
 * Exception Handling:
 * - Handles exceptions during analyzer setup and model disposal.
 * <p>
 * Note: Ensure that the appropriate permissions are configured in the AndroidManifest to utilize camera capabilities.
 */
public class CameraXLivePreviewActivity extends AppCompatActivity implements BarcodeAnalyzer.DetectionCallback, TextOCRAnalyzer.DetectionCallback, ProductRecognitionAnalyzer.DetectionCallback, Tracker.DetectionCallback, EntityBarcodeTracker.DetectionCallback, BarcodeSampleAnalyzer.SampleBarcodeDetectionCallback, OCRAnalyzer.DetectionCallback, ProductRecognitionSampleAnalyzer.SampleDetectionCallback {

    private ActivityCameraXlivePreviewBinding binding;
    private final String TAG = "CameraXLivePreviewActivityJava";
    private static final String BARCODE_DETECTION = "Barcode";
    private static final String LEGACY_BARCODE_DETECTION = "Legacy Barcode";
    private static final String TEXT_OCR_DETECTION = "OCR";
    private static final String LEGACY_OCR_DETECTION = "Legacy OCR";
    private static final String ENTITY_ANALYZER = "Tracker";
    private static final String PRODUCT_RECOGNITION = "Product Recognition";
    private static final String LEGACY_PRODUCT_RECOGNITION = "Legacy Product Recognition";
    private static final String ENTITY_VIEW_FINDER = "Entity Viewfinder";
    private ImageAnalysis analysisUseCase;
    private int imageWidth;
    private int imageHeight;
    private final ExecutorService executors = Executors.newFixedThreadPool(3);
    private BarcodeHandler barcodeHandler;
    private OCRHandler ocrHandler;
    private ProductRecognitionHandler productRecognitionHandler;
    private ProductRecognitionSample productRecognitionSample;
    private Tracker tracker;
    private EntityBarcodeTracker entityBarcodeTracker;
    private BarcodeSample barcodeLegacySample;
    private OCRSample ocrSample;
    private String selectedModel = BARCODE_DETECTION;
    private String previousSelectedModel = "";
    private static final String STATE_SELECTED_MODEL = "selected_model";
    private EntityViewController entityViewController;
    private EntityViewGraphic entityViewGraphic;
    private boolean isIconStyleEnable = false;
    private final Size selectedSize = new Size(1920, 1080);
    private int initialRotation = Surface.ROTATION_0;
    private DisplayManager displayManager;
    private DisplayManager.DisplayListener displayListener;

    // Store pending viewfinder resize data to apply once analyzer is ready
    private Matrix pendingTransformMatrix = null;
    private RectF pendingCropRegion = null;
    // Orientation constants for clarity
    private static final int ROTATION_0 = Surface.ROTATION_0;     // 0
    private static final int ROTATION_180 = Surface.ROTATION_180; // 2
    private SharedPreferences sharedPreferences;
    private CameraManager cameraManager;
    private DetectionResultHandler detectionHandler;
    private UIHandler uiHandler;
    private BoundingBoxMapper boundingBoxMapper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getSharedPreferences(CommonUtils.PREFS_NAME, MODE_PRIVATE);
        // Initialize components
        initializeComponents();

        if (savedInstanceState != null) {
            uiHandler = new UIHandler(this, cameraManager, sharedPreferences); // Reinitialize with saved state if needed
        }

        binding = ActivityCameraXlivePreviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.camx_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Setup UI
        uiHandler.setupSpinner();
        displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        registerDisplayRotationListener();
        initEntityView();

        // Setup camera
        setupCamera();

    }
    private void initializeComponents() {
        cameraManager = new CameraManager(this,this, this);
        boundingBoxMapper = new BoundingBoxMapper(this, this);
        detectionHandler = new DetectionResultHandler(this, boundingBoxMapper);
        uiHandler = new UIHandler(this, cameraManager, sharedPreferences);
        cameraManager.setUIHandler(uiHandler);

        // Initialize dimensions
        Size selectedSize = cameraManager.getSelectedSize();
        imageWidth = selectedSize.getHeight();
        imageHeight = selectedSize.getWidth();
        boundingBoxMapper.setImageDimensions(imageWidth, imageHeight);
        boundingBoxMapper.setFrontCamera(cameraManager.isFrontCamera());
    }

    private void setupCamera() {
        new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()))
                .get(CameraXViewModel.class)
                .getProcessCameraProvider()
                .observe(this, provider -> {
                    cameraManager.setCameraProvider(provider);
                    Log.v(TAG, "Binding all camera use cases");
                    bindAllCameraUseCases();
                });
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
                    pendingTransformMatrix = new android.graphics.Matrix(entityViewResizeSpecs.getSensorToViewMatrix());
                    pendingCropRegion = new android.graphics.RectF(entityViewResizeSpecs.getViewfinderFOVCropRegion());
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
        if (cameraManager.getCameraProvider() != null) {
            // As required by CameraX API, unbinds all use cases before trying to re-bind any of them.
            cameraManager.unbindAll();
            bindPreviewUseCase();
            bindAnalysisUseCase();
        }
    }

    public void stopAnalyzing() {
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
                    if (tracker != null) {
                        tracker.stopAnalyzing();
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
                case LEGACY_PRODUCT_RECOGNITION:
                    Log.i(TAG, "Stopping the legacy product recognition analyzer");
                    if (productRecognitionSample != null && productRecognitionSample.getProductRecognitionSampleAnalyzer() != null) {
                        productRecognitionSample.getProductRecognitionSampleAnalyzer().stopAnalyzing();
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
                    if (tracker != null) {
                        tracker.stop();
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
                case LEGACY_PRODUCT_RECOGNITION:
                    Log.i(TAG, "Disposing the legacy product recognition analyzer");
                    if (productRecognitionSample != null) {
                        productRecognitionSample.stop();
                    }
                    break;
                default:
                    Log.e(TAG, "Invalid selected option: " + previousSelectedModel);
            }
        } catch (Exception e) {
            Log.e(TAG, "Can not dispose the analyzer: " + previousSelectedModel, e);
        }
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putString(STATE_SELECTED_MODEL, selectedModel);
    }

    // Handles barcode detection results and updates the graphical overlay
    @Override
    public void onDetectionResult(List<BarcodeEntity> result) {
        detectionHandler.handleBarcodeDetection(result);
    }

    // Handles text OCR detection results and updates the graphical overlay
    @Override
    public void onDetectionTextResult(List<ParagraphEntity> list) {
        detectionHandler.handleTextOCRDetection(list);
    }

    // Handles entity tracking results and updates the graphical overlay
    @Override
    public synchronized void handleEntities(EntityTrackerAnalyzer.Result result) {
        List<? extends Entity> barcodeEntities;
        List<? extends Entity> ocrEntities;
        List<? extends Entity> moduleEntities;

        if (tracker.getBarcodeDecoder() != null) {
            barcodeEntities = result.getValue(tracker.getBarcodeDecoder());
        } else {
            barcodeEntities = null;
        }

        if (tracker.getTextOCR() != null) {
            ocrEntities = result.getValue(tracker.getTextOCR());
        } else {
            ocrEntities = null;
        }

        if (tracker.getModuleRecognizer() != null) {
            moduleEntities = result.getValue(tracker.getModuleRecognizer());
        } else {
            moduleEntities = null;
        }
        detectionHandler.handleEntityTrackerDetection(barcodeEntities, ocrEntities, moduleEntities);

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

        if (entities != null && entityViewGraphic != null) {
            Log.d(TAG, "Processing " + entities.size() + " entities for entity view");
            detectionHandler.handleEntityViewFinderDetection((List<Entity>) entities, entityViewGraphic);
            Log.d(TAG, "Rendered entities on entity view");
        } else {
            Log.w(TAG, "No entities to process for entity view");
        }
    }

    // Handles product recognition results and updates the graphical overlay
    @Override
    public void onRecognitionResult(List<Entity> result) {
        detectionHandler.handleDetectionRecognitionResult(result);
    }

    /**
     * Callback method invoked when barcode detection results are available.
     *
     * @param barcodes An array of BarcodeDecoder.Result representing detected barcodes.
     */
    @Override
    public void onDetectionResult(BarcodeDecoder.Result[] barcodes) {
        detectionHandler.handleLegacyBarcodeDetection(barcodes);
    }

    /**
     * Callback method invoked when OCR text detection results are available.
     *
     * @param words An array of Word objects representing detected words.
     */
    @Override
    public void onDetectionTextResult(Word[] words) {
        detectionHandler.handleLegacyTextOCRDetection(words);

    }

    // Handles product recognition results using legacy api and updates the graphical overlay
    @Override
    public void onDetectionRecognitionResult(BBox[] detections, BBox[] products, Recognizer.Recognition[] recognitions) {
        detectionHandler.handleLegacyProductRecognitionResult(detections, products, recognitions);
    }

    public void bindAnalysisUseCase() {
        if (cameraManager.getCameraProvider() == null) {
            return;
        }
        selectedModel = uiHandler.getSelectedModel();
        analysisUseCase = cameraManager.getAnalysisUseCase();
        previousSelectedModel = selectedModel;
        String[] filterItems = FilterDialog.trackerArray;
        List<String> selectedFilterItems = new ArrayList<>();
        for (String filterItem : filterItems) {
            boolean defaultValue = filterItem.equalsIgnoreCase(FilterDialog.BARCODE_TRACKER);
            boolean isChecked = sharedPreferences.getBoolean(filterItem, defaultValue);
            if (isChecked) selectedFilterItems.add(filterItem);
        }

        try {
            switch (selectedModel) {
                case BARCODE_DETECTION:
                    Log.i(TAG, "Using Barcode Decoder");
                    executors.execute(() -> barcodeHandler = new BarcodeHandler(this, this, analysisUseCase));
                    break;
                case TEXT_OCR_DETECTION:
                    Log.i(TAG, "Using Text OCR");
                    executors.execute(() -> ocrHandler = new OCRHandler(this, this, analysisUseCase));
                    break;
                case ENTITY_VIEW_FINDER:
                    Log.i(TAG, "Using Entity View Analyzer");
                    executors.execute(() -> entityBarcodeTracker = new EntityBarcodeTracker(this, this, analysisUseCase));
                    break;
                case ENTITY_ANALYZER:
                    Log.i(TAG, "Using Entity Analyzer");
                    executors.execute(() -> {
                        try {
                            tracker = new Tracker(this, this, analysisUseCase, selectedFilterItems);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });

                    break;
                case PRODUCT_RECOGNITION:
                    Log.i(TAG, "Using Product Recognition");
                    executors.execute(() -> productRecognitionHandler = new ProductRecognitionHandler(CameraXLivePreviewActivity.this, CameraXLivePreviewActivity.this, analysisUseCase));
                    break;

                case LEGACY_BARCODE_DETECTION:
                    Log.i(TAG, "Using Legacy Barcode Detection");
                    executors.execute(() -> barcodeLegacySample = new BarcodeSample(this, this, analysisUseCase));

                    break;
                case LEGACY_OCR_DETECTION:
                    Log.i(TAG, "Using Legacy Text OCR");
                    executors.execute(() -> ocrSample = new OCRSample(this, this, analysisUseCase));
                    break;
                case LEGACY_PRODUCT_RECOGNITION:
                    Log.i(TAG, "Using Legacy Product Recognition");
                    executors.execute(() -> productRecognitionSample = new ProductRecognitionSample(CameraXLivePreviewActivity.this, CameraXLivePreviewActivity.this, analysisUseCase) );
                    break;
                default:
                    throw new IllegalStateException("Invalid model name");
            }
        } catch (Exception e) {
            Log.e(TAG, "Can not create model for : " + selectedModel, e);
        }
    }


    private void bindPreviewUseCase() {
        cameraManager.bindPreviewAndAnalysis(getPreviewSurfaceProvider());
    }

    // Getters for components
    public ActivityCameraXlivePreviewBinding getBinding() {
        return binding;
    }
    public void clearGraphicOverlay() {
        runOnUiThread(() -> {
            binding.graphicOverlay.clear();
            if (entityViewGraphic != null) entityViewGraphic.clear();
        });
    }

    public Preview.SurfaceProvider getPreviewSurfaceProvider() {
        uiHandler.updatePreviewVisibility(uiHandler.isEntityViewFinder());
        if (uiHandler.isEntityViewFinder()){
            return entityViewController.getSurfaceProvider();
        }
        return binding.previewView.getSurfaceProvider();
    }

    public void onBackPressed() {
        super.onBackPressed();
    }

    public void onResume() {
        super.onResume();
        Log.v(TAG, "OnResume called");
        clearGraphicOverlay();

        int currentRotation = getWindowManager().getDefaultDisplay().getRotation();
        if (currentRotation != initialRotation) {
            Log.d(TAG, "Rotation changed during pause, updating initialRotation from " + initialRotation + " to " + currentRotation);
            initialRotation = currentRotation;
            if(boundingBoxMapper!=null) boundingBoxMapper.setInitialRotation(initialRotation);
            if (cameraManager != null) cameraManager.updateTargetRotation(currentRotation);
            // check if the device rotation is changes when suspended (0-> 0°, 2 -> 180°)
            if (initialRotation == ROTATION_0 || initialRotation == ROTATION_180) {
                imageWidth = selectedSize.getHeight();
                imageHeight = selectedSize.getWidth();
            } else {
                imageWidth = selectedSize.getWidth();
                imageHeight = selectedSize.getHeight();
            }
            boundingBoxMapper.setImageDimensions(imageWidth, imageHeight);
            Log.d(TAG, "Updated imageWidth=" + imageWidth + ", imageHeight=" + imageHeight);
        }
        if (uiHandler.isSpinnerInitialized) bindAllCameraUseCases();
    }

    public void onPause() {
        clearGraphicOverlay();
        stopAnalyzing();
        cameraManager.unbindAll();
        disposeModels();
        super.onPause();
        Log.v(TAG, "onPause called");
    }

    @Override
    protected void onDestroy() {
        if (displayManager != null && displayListener != null) {
            displayManager.unregisterDisplayListener(displayListener);
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
                    if (cameraManager != null) cameraManager.updateTargetRotation(newRotation);
                    if (boundingBoxMapper!=null){
                        boundingBoxMapper.setInitialRotation(initialRotation);
                    }

                    if (initialRotation == ROTATION_0 || initialRotation == ROTATION_180) {
                        imageWidth = selectedSize.getHeight();
                        imageHeight = selectedSize.getWidth();
                    } else {
                        imageWidth = selectedSize.getWidth();
                        imageHeight = selectedSize.getHeight();
                    }
                    boundingBoxMapper.setImageDimensions(imageWidth, imageHeight);
                    Log.i(TAG, "Display changed, updated targetRotation and dimensions: rotation=" + newRotation + ", imageWidth=" + imageWidth + ", imageHeight=" + imageHeight);
                });
            }
        };
        displayManager.registerDisplayListener(displayListener, null);
    }

    public EntityViewController getEntityViewController(){
        return entityViewController;
    }

}