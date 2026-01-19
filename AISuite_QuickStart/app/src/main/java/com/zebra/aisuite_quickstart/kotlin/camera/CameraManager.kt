// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.camera

import android.content.Context
import android.hardware.camera2.CameraMetadata
import android.util.Log
import android.util.Size
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import com.zebra.aisuite_quickstart.kotlin.CameraXLivePreviewActivity
import com.zebra.aisuite_quickstart.kotlin.handlers.UIHandler
import com.zebra.aisuite_quickstart.utils.CameraUtil

/**
 * CameraManager handles all CameraX related operations including initialization,
 * binding use cases, and lifecycle management.
 */
class CameraManager(private val activity: CameraXLivePreviewActivity, private val context: Context, private val lifecycleOwner: LifecycleOwner) {

    companion object {
        private const val TAG = "CameraManager"
    }

    private val selectedSize = Size(1920, 1080)

    private var camera: Camera? = null
    private var previewUseCase: Preview? = null
    private var analysisUseCase: ImageAnalysis? = null

    private var uiHandler: UIHandler ?= null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var cameraSelector: CameraSelector
    private lateinit var resolutionSelector: ResolutionSelector
    private var isFrontCamera: Boolean = false

    init {
        initializeCameraSelector()
        initializeResolutionSelector()
    }

    private fun initializeCameraSelector() {
        val cameraFacing = CameraUtil.getPreferredCameraFacing(context)
        if (cameraFacing == CameraMetadata.LENS_FACING_BACK) {
            isFrontCamera = false
            cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()
        } else {
            isFrontCamera = true
            cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()
        }
    }

    private fun initializeResolutionSelector() {
        resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(
                AspectRatioStrategy(AspectRatio.RATIO_16_9, AspectRatioStrategy.FALLBACK_RULE_AUTO)
            )
            .setResolutionStrategy(
                ResolutionStrategy(selectedSize, ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
            )
            .build()
    }

    fun setCameraProvider(provider: ProcessCameraProvider) {
        this.cameraProvider = provider
    }

    fun setUIHandler(uiHandler: UIHandler){
        this.uiHandler = uiHandler
    }


    fun bindPreviewAndAnalysis(surfaceProvider: Preview.SurfaceProvider?) {
        if (cameraProvider == null) return

        previewUseCase?.let { cameraProvider?.unbind(it) }
        analysisUseCase?.let { cameraProvider?.unbind(it) }

        activity.binding.graphicOverlay.clear()
        val previewBuilder = Preview.Builder().apply {
            setResolutionSelector(resolutionSelector)
        }

        val display = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
        val rotation = display.defaultDisplay.rotation
        previewBuilder.setTargetRotation(rotation)

        previewUseCase = previewBuilder.build().apply {
            setSurfaceProvider(surfaceProvider)
        }

        analysisUseCase = ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(rotation)
            .build()


        camera = cameraProvider?.bindToLifecycle(lifecycleOwner, cameraSelector, previewUseCase, analysisUseCase)

        if(uiHandler?.isEntityViewFinder() == true) activity.entityViewController?.setCameraController(camera)
    }

    fun unbindAll() {
        cameraProvider?.unbindAll()
        Log.v(TAG, "Camera Unbound")
    }

    fun updateTargetRotation(rotation: Int) {
        analysisUseCase?.targetRotation = rotation
        previewUseCase?.targetRotation = rotation
    }

    fun isFrontCamera(): Boolean = isFrontCamera

    fun getSelectedSize(): Size = selectedSize

    fun getCameraProvider(): ProcessCameraProvider? = cameraProvider

    fun getAnalysisUseCase(): ImageAnalysis? = analysisUseCase
}