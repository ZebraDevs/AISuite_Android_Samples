// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.handlers

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.core.content.edit
import androidx.core.view.isVisible
import com.zebra.aisuite_quickstart.filtertracker.FilterDialog
import com.zebra.aisuite_quickstart.kotlin.CameraXLivePreviewActivity
import com.zebra.aisuite_quickstart.kotlin.camera.CameraManager
import com.zebra.aisuite_quickstart.utils.CommonUtils
import com.zebra.aisuite_quickstart.utils.CommonUtils.WAREHOUSE_LOCALIZER
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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
        private const val LEGACY_PRODUCT_RECOGNITION = "Legacy Product Recognition"
        private const val ENTITY_VIEW_FINDER = "Entity Viewfinder"
    }

    var selectedModel: String = BARCODE_DETECTION
    var isEntityViewFinder: Boolean = false
    var isSpinnerInitialized = false

    private var imageCapture: ImageCapture? = null
    var isInCaptureMode: Boolean = false
    private var currentCapture: Bitmap? = null
    private val executors: ExecutorService = Executors.newFixedThreadPool(2)

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
                activity.isModelLoaded = false
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
            ENTITY_VIEW_FINDER,
            WAREHOUSE_LOCALIZER,
            // LEGACY_BARCODE_DETECTION, // Uncomment to use barcode legacy option
            // LEGACY_OCR_DETECTION // Uncomment to use OCR legacy option
            //LEGACY_PRODUCT_RECOGNITION // Uncomment to use Product recognition legacy option
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
                activity.isModelLoaded = false

                // Show capture layout for supported models
                if (selectedModel.equals(BARCODE_DETECTION, ignoreCase = true) ||
                    selectedModel.equals(TEXT_OCR_DETECTION, ignoreCase = true) ||
                    selectedModel.equals(PRODUCT_RECOGNITION, ignoreCase = true) ||
                    selectedModel.equals(ENTITY_ANALYZER, ignoreCase = true) ||
                    selectedModel.equals(WAREHOUSE_LOCALIZER, ignoreCase = true)
                ) {
                    initializeCaptureFeature()
                    activity.binding.captureLayout.visibility = View.VISIBLE
                } else {
                    activity.binding.captureLayout.visibility = View.GONE
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
    private fun initializeCaptureFeature() {
        activity.binding.captureButton.setOnClickListener {
            if (selectedModel.equals(BARCODE_DETECTION, ignoreCase = true) ||
                selectedModel.equals(TEXT_OCR_DETECTION, ignoreCase = true) ||
                selectedModel.equals(PRODUCT_RECOGNITION, ignoreCase = true) ||
                selectedModel.equals(ENTITY_ANALYZER, ignoreCase = true) ||
                selectedModel.equals(WAREHOUSE_LOCALIZER, ignoreCase = true)
            ) {
                if (activity.isModelLoaded) {
                    activity.binding.captureLayout.visibility = View.GONE
                    imageCapture = cameraManager.imageCapture
                    activity.stopAnalyzing()
                    activity.binding.control.visibility = View.GONE

                    executors.submit {
                        when {
                            selectedModel.equals(BARCODE_DETECTION, ignoreCase = true) ->
                                initializeCaptureDecoderAndCapture()
                            selectedModel.equals(TEXT_OCR_DETECTION, ignoreCase = true) ->
                                initializeCaptureOCRAndCapture()
                            selectedModel.equals(PRODUCT_RECOGNITION, ignoreCase = true) ->
                                initializeCaptureRecognitionAndCapture()
                            selectedModel.equals(ENTITY_ANALYZER, ignoreCase = true) ->
                                initializeCaptureTracker()
                            selectedModel.equals(WAREHOUSE_LOCALIZER, ignoreCase = true) ->
                                initializeCaptureWareHouseLocalizer()
                        }
                    }
                } else {
                    Toast.makeText(activity, "Model is not loaded", Toast.LENGTH_SHORT).show()
                }
            }
        }

        activity.binding.backToLiveButton.setOnClickListener {
            activity.binding.control.visibility = View.VISIBLE
            returnToLivePreview()
        }
    }

    private fun initializeCaptureDecoderAndCapture() {
        activity.initializeCaptureDecoder(::performCapture)
    }

    private fun initializeCaptureOCRAndCapture() {
        activity.initializeCaptureOCR(::performCapture)
    }

    private fun initializeCaptureRecognitionAndCapture() {
        activity.initializeCaptureRecognition(::performCapture)
    }

    private fun initializeCaptureTracker() {
        activity.initializeCaptureTracker(::performCapture)
    }

    private fun initializeCaptureWareHouseLocalizer() {
        activity.initializeCaptureWareHouse(::performCapture)
    }

    private fun performCapture() {
        if (imageCapture == null || isInCaptureMode) return
        Log.d(TAG, "Performing capture")

        imageCapture!!.takePicture(executors, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                try {
                    Log.e(TAG, "Image Dims w x h = ${imageProxy.width} x ${imageProxy.height}")
                    Log.e(TAG, "Selected model: $selectedModel")
                    currentCapture = CommonUtils.rotateBitmapIfNeeded(imageProxy)
                    activity.runOnUiThread {
                        switchToCaptureMode()
                        activity.binding.capturedImageView.setImageBitmap(currentCapture)
                    }
                    when {
                        selectedModel.equals(TEXT_OCR_DETECTION, ignoreCase = true) ->
                            activity.processCaptureOCR(imageProxy)
                        selectedModel.equals(BARCODE_DETECTION, ignoreCase = true) ->
                            activity.processBarcodeWithCaptureDecoder(imageProxy)
                        selectedModel.equals(PRODUCT_RECOGNITION, ignoreCase = true) ->
                            activity.processCaptureRecognition(imageProxy)
                        selectedModel.equals(ENTITY_ANALYZER, ignoreCase = true) ->
                            activity.processCaptureTracker(imageProxy)
                        selectedModel.equals(WAREHOUSE_LOCALIZER, ignoreCase = true) ->
                            activity.processCaptureWareHouseLocalizer(imageProxy)
                    }
                    Log.d(TAG, "bitmap captured")
                } catch (e: Exception) {
                    Log.e(TAG, "Exception occurred while capturing: ${e.message}")
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Capture operation failed", exception)
            }
        })
    }

    @SuppressLint("ResourceAsColor")
    private fun returnToLivePreview() {
        activity.runOnUiThread {
            isInCaptureMode = false
            currentCapture = null

            activity.clearGraphicOverlay()

            activity.binding.capturedImageView.visibility = View.GONE
            activity.binding.capturedImageView.setImageBitmap(null)
            activity.binding.backToLiveButton.visibility = View.GONE
            activity.binding.trackerFilter.isVisible = TextUtils.equals(selectedModel, ENTITY_ANALYZER)
            activity.binding.previewView.visibility = View.VISIBLE
            activity.binding.previewView.setBackgroundColor(android.R.color.black)
            activity.binding.graphicOverlay.visibility = View.VISIBLE
            activity.binding.captureLayout.visibility = View.VISIBLE
        }

        cameraManager.unbindAll()
        cameraManager.bindPreviewAndAnalysis(activity.getPreviewSurfaceProvider())

        when {
            selectedModel.equals(TEXT_OCR_DETECTION, ignoreCase = true) ->
                activity.setOCRAnalysis()

            selectedModel.equals(BARCODE_DETECTION, ignoreCase = true) ->
                activity.setBarcodeAnalysis()

            selectedModel.equals(PRODUCT_RECOGNITION, ignoreCase = true) -> {
                    activity.clearCaptureListener()
                    activity.setRecognitionAnalysis()
            }
            selectedModel.equals(ENTITY_ANALYZER, ignoreCase = true) ->
                activity.setTrackerAnalysis()
            selectedModel.equals(WAREHOUSE_LOCALIZER, ignoreCase = true) ->
                activity.setWareHouseAnalysis()
        }
    }

    private fun switchToCaptureMode() {
        activity.runOnUiThread {
            isInCaptureMode = true

            cameraManager.unbindImageAnalysis()

            activity.binding.previewView.visibility = View.GONE
            activity.binding.graphicOverlay.visibility = View.GONE
            activity.binding.captureLayout.visibility = View.GONE

            activity.binding.capturedImageView.visibility = View.VISIBLE
            activity.binding.backToLiveButton.visibility = View.VISIBLE
            activity.binding.backToLiveButton.bringToFront()
        }
    }


    fun updatePreviewVisibility(isEntityViewFinder: Boolean) {
        activity.binding.previewView.visibility = if (isEntityViewFinder) View.GONE else View.VISIBLE
        activity.binding.entityView.visibility = if (isEntityViewFinder) View.VISIBLE else View.GONE
    }

    /**
     * Enable or disable the spinner and change its appearance
     * @param enabled true to enable spinner, false to disable
     */
    fun setSpinnerEnabled(enabled: Boolean) {
        activity.runOnUiThread {
            activity.binding.spinner.let { spinner ->
                spinner.isEnabled = enabled
                spinner.alpha = if (enabled) 1.0f else 0.5f

            }
            activity.binding.trackerFilter.isEnabled = enabled
            activity.binding.trackerFilter.alpha = if (enabled) 1.0f else 0.5f

            activity.binding.captureButton.isEnabled = enabled
            activity.binding.captureButton.alpha = if (enabled) 1.0f else 0.5f
        }
    }
}