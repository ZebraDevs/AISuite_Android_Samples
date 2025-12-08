package com.zebra.aisuite_quickstart.java.handlers;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import com.zebra.aisuite_quickstart.filtertracker.FilterDialog;
import com.zebra.aisuite_quickstart.filtertracker.FilterItem;
import com.zebra.aisuite_quickstart.java.CameraXLivePreviewActivity;
import com.zebra.aisuite_quickstart.java.camera.CameraManager;

import java.util.ArrayList;
import java.util.List;

/**
 * UIHandler manages UI-related operations including spinner setup,
 * overlay management, and orientation changes.
 */
public class UIHandler {
    private static final String TAG = "UIHandler";

    private final CameraXLivePreviewActivity activity;
    private final CameraManager cameraManager;

    // Model constants
    private static final String BARCODE_DETECTION = "Barcode";
    private static final String LEGACY_BARCODE_DETECTION = "Legacy Barcode";
    private static final String TEXT_OCR_DETECTION = "OCR";
    private static final String LEGACY_OCR_DETECTION = "Legacy OCR";
    private static final String ENTITY_ANALYZER = "Tracker";
    private static final String PRODUCT_RECOGNITION = "Product Recognition";
    private static final String ENTITY_VIEW_FINDER = "Entity Viewfinder";

    private String selectedModel = BARCODE_DETECTION;
    private SharedPreferences sharedPreferences;
    private boolean isEntityViewFinder = false;

    public boolean isSpinnerInitialized = false;

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
        options.add(ENTITY_ANALYZER);
        options.add(PRODUCT_RECOGNITION);
        options.add(ENTITY_VIEW_FINDER);
        // options.add(LEGACY_BARCODE_DETECTION); // uncomment to use barcode legacy option
        // options.add(LEGACY_OCR_DETECTION); // uncomment to use ocr legacy option

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

}