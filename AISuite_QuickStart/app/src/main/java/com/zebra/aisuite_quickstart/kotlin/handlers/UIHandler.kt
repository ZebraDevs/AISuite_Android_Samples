// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.handlers

import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.annotation.NonNull
import com.zebra.aisuite_quickstart.filtertracker.FilterDialog
import com.zebra.aisuite_quickstart.kotlin.CameraXLivePreviewActivity
import com.zebra.aisuite_quickstart.kotlin.camera.CameraManager
import androidx.core.content.edit
import androidx.core.view.isVisible

/**
 * UIHandler manages UI-related operations including spinner setup,
 * overlay management, and orientation changes.
 */
class UIHandler(
    private val activity: CameraXLivePreviewActivity,
    private val cameraManager: CameraManager,
    private val sharedPreferences: SharedPreferences?,
) {
    companion object {
        private const val TAG = "UIHandler"

        // Model constants
        private const val BARCODE_DETECTION = "Barcode"
        private const val LEGACY_BARCODE_DETECTION = "Legacy Barcode"
        private const val TEXT_OCR_DETECTION = "OCR"
        private const val LEGACY_OCR_DETECTION = "Legacy OCR"
        private const val ENTITY_ANALYZER = "Tracker"
        private const val PRODUCT_RECOGNITION = "Product Recognition"
        private const val ENTITY_VIEW_FINDER = "Entity Viewfinder"
    }

    private var selectedModel: String = BARCODE_DETECTION
    private var isEntityViewFinder: Boolean = false
    var isSpinnerInitialized = false

    fun setupSpinner() {
        val dataAdapter = getStringArrayAdapter()
        activity.binding.spinner.adapter = dataAdapter
        activity.binding.spinner.onItemSelectedListener = createSpinnerListener()
        setUpTrackerFilter()
    }

    fun setUpTrackerFilter() {
        activity.binding.trackerFilter.setOnClickListener {
            val dialog = FilterDialog(activity)
            dialog.setCallback { filters ->
                sharedPreferences?.edit {

                    for (option in filters) {
                        putBoolean(option.title, option.isChecked)
                    }
                }
                activity.clearGraphicOverlay()
                activity.stopAnalyzing()
                cameraManager.unbindAll()
                activity.disposeModels()
                cameraManager.bindPreviewAndAnalysis(activity.getPreviewSurfaceProvider())
                activity.bindAnalysisUseCase()

            }
            dialog.show()
        }
    }

    @NonNull
    private fun getStringArrayAdapter(): ArrayAdapter<String> {
        val options = listOf(
            BARCODE_DETECTION,
            TEXT_OCR_DETECTION,
            PRODUCT_RECOGNITION,
            ENTITY_ANALYZER,
            ENTITY_VIEW_FINDER
            // LEGACY_BARCODE_DETECTION, // Uncomment to use barcode legacy option
            // LEGACY_OCR_DETECTION // Uncomment to use OCR legacy option
        )

        return ArrayAdapter(
            activity,
            com.zebra.aisuite_quickstart.R.layout.spinner_style,
            options
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun createSpinnerListener(): AdapterView.OnItemSelectedListener {
        return object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>, view: View?, pos: Int, id: Long) {
                selectedModel = adapterView.getItemAtPosition(pos).toString()
                isEntityViewFinder = selectedModel == ENTITY_VIEW_FINDER

                Log.e(TAG, "Selected option is $selectedModel")
                activity.binding.trackerFilter.isVisible = TextUtils.equals(selectedModel, ENTITY_ANALYZER)
                // Lock orientation when Entity Viewfinder is selected
                if (isEntityViewFinder) {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
                    Log.d(TAG, "Orientation locked for Entity Viewfinder mode")
                } else {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    Log.d(TAG, "Orientation unlocked for $selectedModel mode")
                }
                // Clear overlays and rebind the camera
                if (!isSpinnerInitialized) {
                    isSpinnerInitialized = true
                } else {
                    activity.clearGraphicOverlay()
                    activity.stopAnalyzing()
                    cameraManager.unbindAll()
                    activity.disposeModels()
                    cameraManager.bindPreviewAndAnalysis(activity.getPreviewSurfaceProvider())
                    activity.bindAnalysisUseCase()
                }
            }

            override fun onNothingSelected(adapterView: AdapterView<*>) {
                // No action needed
            }
        }
    }

    fun updatePreviewVisibility(isEntityViewFinder: Boolean) {
        activity.binding.previewView.visibility = if (isEntityViewFinder) View.GONE else View.VISIBLE
        activity.binding.entityView.visibility = if (isEntityViewFinder) View.VISIBLE else View.GONE
    }

    fun getSelectedModel(): String = selectedModel

    fun isEntityViewFinder(): Boolean = isEntityViewFinder
}