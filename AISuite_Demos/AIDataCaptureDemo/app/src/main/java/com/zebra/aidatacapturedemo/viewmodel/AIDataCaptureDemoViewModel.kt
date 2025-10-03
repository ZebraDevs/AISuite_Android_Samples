// Copyright (c) 2024-2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.aidatacapturedemo.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import android.util.Size
import android.view.ScaleGestureDetector
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getString
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import com.google.common.util.concurrent.ListenableFuture
import com.zebra.ai.vision.detector.AIVisionSDK
import com.zebra.ai.vision.detector.BBox
import com.zebra.ai.vision.detector.InvalidInputException
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.data.BarcodeSettings
import com.zebra.aidatacapturedemo.data.CommonSettings
import com.zebra.aidatacapturedemo.data.OCRFilterData
import com.zebra.aidatacapturedemo.data.OcrFindSettings
import com.zebra.aidatacapturedemo.data.ProductData
import com.zebra.aidatacapturedemo.data.ProductRecognitionSettings
import com.zebra.aidatacapturedemo.data.ResultData
import com.zebra.aidatacapturedemo.data.RetailShelfSettings
import com.zebra.aidatacapturedemo.data.TextOcrSettings
import com.zebra.aidatacapturedemo.data.UsecaseState
import com.zebra.aidatacapturedemo.model.BarcodeEntityTrackerAnalyzer
import com.zebra.aidatacapturedemo.model.FileUtils
import com.zebra.aidatacapturedemo.model.FileUtils.Companion.databaseFile
import com.zebra.aidatacapturedemo.model.FileUtils.Companion.mCacheDir
import com.zebra.aidatacapturedemo.model.ProductEnrollmentRecognition
import com.zebra.aidatacapturedemo.model.RetailShelfAnalyzer
import com.zebra.aidatacapturedemo.model.TextOCRAnalyzer
import com.zebra.aidatacapturedemo.ui.view.Screen
import com.zebra.aidatacapturedemo.ui.view.Variables.highResolutionCaptureImageSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "AIDataCaptureDemoViewModel"

/**
 * ViewModel class for the AIDataCaptureDemo
 * Initializes AIVisionSDK, before using its components and all the models
 */
class AIDataCaptureDemoViewModel(
    private val cacheDir: String,
    private val context: Context,
    private val assetManager: AssetManager
) : ViewModel() {

    // Used to set up a link between the Model and UI View.
    private val _uiState = MutableStateFlow(AIDataCaptureDemoUiState())
    val uiState: StateFlow<AIDataCaptureDemoUiState> = _uiState.asStateFlow()

    private var executor: Executor? = null

    // Used to set up a link between the Camera and UI View.
    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    val surfaceRequest: StateFlow<SurfaceRequest?> = _surfaceRequest

    private lateinit var resolutionSelector: ResolutionSelector
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var analysisUseCase: ImageAnalysis? = null
    private lateinit var imageCaptureResolutionSelector: ResolutionSelector
    private var imageCapture : ImageCapture? = null

    private var ocrAnalyzer: TextOCRAnalyzer? = null
    private var retailShelfAnalyzer: RetailShelfAnalyzer? = null
    private var barcodeEntityTrackerAnalyzer: BarcodeEntityTrackerAnalyzer? = null
    private var productEnrollmentRecognition: ProductEnrollmentRecognition? = null

    companion object {
        fun factory() = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                AIDataCaptureDemoViewModel(
                    application.filesDir.absolutePath,
                    application as Context,
                    application.assets
                )
            }
        }
    }

    init {
        executor = Dispatchers.Default.asExecutor()

        val isInitDone = AIVisionSDK.getInstance(context).init()
        Log.i(TAG, "AI Vision SDK Init ret = $isInitDone")

        // Get the SDK version
        val sdkVersion = AIVisionSDK.getInstance(context).sdkVersion
        Log.i(TAG, "AI Vision SDK Version = $sdkVersion")
    }

    /**
     * This function is used to initialize the model based on the selected index
     */
    fun initModel() {
        CoroutineScope(executor!!.asCoroutineDispatcher()).launch {

            when (uiState.value.usecaseSelected) {
                UsecaseState.Barcode.value -> {
                    barcodeEntityTrackerAnalyzer = BarcodeEntityTrackerAnalyzer(
                        uiState = uiState,
                        viewModel = this@AIDataCaptureDemoViewModel
                    )
                    barcodeEntityTrackerAnalyzer?.initialize()
                }

                UsecaseState.Retail.value -> {
                    retailShelfAnalyzer = RetailShelfAnalyzer(
                        uiState = uiState,
                        viewModel = this@AIDataCaptureDemoViewModel
                    )
                    retailShelfAnalyzer?.initialize()
                }

                UsecaseState.Product.value -> {
                    productEnrollmentRecognition =
                        ProductEnrollmentRecognition(
                            uiState = uiState,
                            viewModel = this@AIDataCaptureDemoViewModel,
                            cacheDir = context.filesDir.absolutePath
                        )
                    productEnrollmentRecognition?.initialize()
                }

                UsecaseState.OCRFind.value,
                UsecaseState.OCR.value -> {
                    ocrAnalyzer = TextOCRAnalyzer(
                        uiState = uiState,
                        viewModel = this@AIDataCaptureDemoViewModel
                    )
                    ocrAnalyzer?.initialize()
                }
            }
        }
    }

    /**
     * This function is used to de-initialize the model based on the selected value
     */
    fun deinitModel() {
        when (uiState.value.usecaseSelected) {
            UsecaseState.Barcode.value -> {
                barcodeEntityTrackerAnalyzer?.deinitialize()
                barcodeEntityTrackerAnalyzer = null
            }

            UsecaseState.Retail.value -> {
                retailShelfAnalyzer?.deinitialize()
                retailShelfAnalyzer = null
            }

            UsecaseState.Product.value -> {
                productEnrollmentRecognition?.deinitialize()
                productEnrollmentRecognition = null
            }

            UsecaseState.OCRFind.value,
            UsecaseState.OCR.value -> {
                ocrAnalyzer?.deinitialize()
                ocrAnalyzer = null
            }
        }
    }

    /**
     * This function is used to start processing
     */
    fun startProcessing() {
        when (uiState.value.usecaseSelected) {
            UsecaseState.Barcode.value -> {

            }

            UsecaseState.Retail.value -> {
                retailShelfAnalyzer?.startAnalyzing()
            }

            UsecaseState.Product.value -> {
                productEnrollmentRecognition?.startAnalyzing()
            }

            UsecaseState.OCRFind.value,
            UsecaseState.OCR.value -> {
                ocrAnalyzer?.startAnalyzing()
            }
        }
    }

    /**
     * This function is used to stop processing
     */
    fun stopProcessing() {
        when (uiState.value.usecaseSelected) {
            UsecaseState.Barcode.value -> {

            }

            UsecaseState.Retail.value -> {
                retailShelfAnalyzer?.stopAnalyzing()
            }

            UsecaseState.Product.value -> {
                productEnrollmentRecognition?.stopAnalyzing()
            }

            UsecaseState.OCRFind.value,
            UsecaseState.OCR.value -> {
                ocrAnalyzer?.stopAnalyzing()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public fun setupCameraController(
        previewView: PreviewView,
        cameraPreviewResolution: Size,
        lifecycleOwner: LifecycleOwner,
        activityLifecycle: Lifecycle
    ) {
        cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()

                resolutionSelector = ResolutionSelector.Builder()
                    .setAspectRatioStrategy(
                        AspectRatioStrategy(
                            AspectRatio.RATIO_16_9,
                            AspectRatioStrategy.FALLBACK_RULE_NONE
                        )
                    )
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            cameraPreviewResolution,
                            ResolutionStrategy.FALLBACK_RULE_NONE
                        )
                    ).build()

                imageCaptureResolutionSelector = ResolutionSelector.Builder()
                    .setAspectRatioStrategy(
                        AspectRatioStrategy(
                            AspectRatio.RATIO_16_9,
                            AspectRatioStrategy.FALLBACK_RULE_NONE
                        )
                    )
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(
                                highResolutionCaptureImageSize.width,
                                highResolutionCaptureImageSize.height
                            ),
                            ResolutionStrategy.FALLBACK_RULE_NONE
                        )
                    ).build()

                imageCapture = ImageCapture.Builder()
                    .setResolutionSelector(imageCaptureResolutionSelector)
                    .setCaptureMode(CAPTURE_MODE_MAXIMIZE_QUALITY).build()

                analysisUseCase = ImageAnalysis.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val previewUsecase = Preview.Builder().build()

                // Set the appropriate analyzer based on the selectedUsecase
                setAnalyzer(activityLifecycle)
                cameraProvider?.unbindAll()
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    previewUsecase,
                    imageCapture,
                    analysisUseCase
                )
                previewUsecase.setSurfaceProvider(previewView.surfaceProvider)

                //Pinch to Zoom handling
                val scaleGestureDetector = ScaleGestureDetector(
                    context,
                    object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                        override fun onScale(detector: ScaleGestureDetector): Boolean {
                            val cameraControl = camera?.cameraControl // Get CameraControl instance
                            val cameraInfo = camera?.cameraInfo // Get CameraInfo instance

                            if (cameraControl != null && cameraInfo != null) {
                                val currentZoomRatio = cameraInfo.zoomState.value?.zoomRatio ?: 1.0f
                                val newZoomRatio = currentZoomRatio * detector.scaleFactor

                                // Clamp the new zoom ratio within the camera's supported range
                                val minZoomRatio = cameraInfo.zoomState.value?.minZoomRatio ?: 1.0f
                                val maxZoomRatio = cameraInfo.zoomState.value?.maxZoomRatio ?: 1.0f
                                val clampedZoomRatio =
                                    newZoomRatio.coerceIn(minZoomRatio, maxZoomRatio)

                                setZoom(clampedZoomRatio)
                            }
                            return true
                        }
                    })

                previewView.setOnTouchListener { _, event ->
                    scaleGestureDetector.onTouchEvent(event)
                    true // Indicate that the event was consumed
                }

            } catch (e: Exception) {
                Log.e(TAG, " Exception while setting up the camera : ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Turn on torch
     */
    fun enableTorch(enabled: Boolean) {
        if (camera?.cameraInfo?.hasFlashUnit() == true) {
            camera?.cameraControl?.enableTorch(enabled)
        }
    }

    /**
     * Set Zoom Value
     */
    fun setZoom(zoomValue: Float) {
        _uiState.update { currentState ->
            currentState.copy(
                zoomLevel = zoomValue
            )
        }
        camera?.cameraControl?.setZoomRatio(uiState.value.zoomLevel)
    }

    /**
     * Update the selected usecase
     */
    fun updateSelectedUsecase(usecase: String) {
        _uiState.update { currentState ->
            currentState.copy(
                usecaseSelected = usecase
            )
        }
    }

    /**
     * Update the selected processor
     */
    fun updateSelectedProcessor(index: Int) {
        val updatedSelectedProcessorIndex = when (uiState.value.usecaseSelected) {
            UsecaseState.Barcode.value -> {
                val currentProcessorSelectedIndex = _uiState.value.barcodeSettings.commonSettings
                currentProcessorSelectedIndex.copy(processorSelectedIndex = index)
            }
            UsecaseState.Retail.value -> {
                val currentProcessorSelectedIndex = _uiState.value.retailShelfSettings.commonSettings
                currentProcessorSelectedIndex.copy(processorSelectedIndex = index)
            }

            UsecaseState.Product.value -> {
                val currentProcessorSelectedIndex = _uiState.value.productRecognitionSettings.commonSettings
                currentProcessorSelectedIndex.copy(processorSelectedIndex = index)
            }

            UsecaseState.OCRFind.value -> {
                val currentProcessorSelectedIndex = _uiState.value.ocrFindSettings.commonSettings
                currentProcessorSelectedIndex.copy(processorSelectedIndex = index)
            }
            UsecaseState.OCR.value -> {
                val currentProcessorSelectedIndex = _uiState.value.textOCRSettings.commonSettings
                currentProcessorSelectedIndex.copy(processorSelectedIndex = index)
            }
            else -> {
                0
            }
        }
        when (uiState.value.usecaseSelected) {
            UsecaseState.Barcode.value -> _uiState.value.barcodeSettings.commonSettings = updatedSelectedProcessorIndex as CommonSettings
            UsecaseState.Retail.value -> _uiState.value.retailShelfSettings.commonSettings = updatedSelectedProcessorIndex as CommonSettings
            UsecaseState.Product.value -> _uiState.value.productRecognitionSettings.commonSettings = updatedSelectedProcessorIndex as CommonSettings
            UsecaseState.OCRFind.value -> _uiState.value.ocrFindSettings.commonSettings = updatedSelectedProcessorIndex as CommonSettings
            UsecaseState.OCR.value -> _uiState.value.textOCRSettings.commonSettings = updatedSelectedProcessorIndex as CommonSettings
        }
        if ((uiState.value.usecaseSelected == UsecaseState.OCRFind.value) || (uiState.value.usecaseSelected == UsecaseState.OCR.value)) {
            updateSelectedDimensions(0)
        }
    }

    /**
     * Update the selected dimensions
     */
    fun updateSelectedDimensions(index: Int) {
        var dimension = when (index) {
            0 -> 640
            1 -> 1280
            2 -> 1600
            3 -> 2560
            else -> throw InvalidInputException(
                "Invalid dimension selection ${index}"
            )
        }

        val updatedInputSize = when (uiState.value.usecaseSelected) {
            UsecaseState.Barcode.value -> {
                val currentInputSizeSelected = _uiState.value.barcodeSettings.commonSettings
                currentInputSizeSelected.copy(inputSizeSelected = dimension)
            }
            UsecaseState.Retail.value -> {
                val currentInputSizeSelected = _uiState.value.retailShelfSettings.commonSettings
                currentInputSizeSelected.copy(inputSizeSelected = dimension)
            }

            UsecaseState.Product.value -> {
                val currentInputSizeSelected = _uiState.value.productRecognitionSettings.commonSettings
                currentInputSizeSelected.copy(inputSizeSelected = dimension)
            }

            UsecaseState.OCRFind.value -> {
                val currentInputSizeSelected = _uiState.value.ocrFindSettings.commonSettings
                if(currentInputSizeSelected.processorSelectedIndex == 2){
                    dimension = 640
                }
                currentInputSizeSelected.copy(inputSizeSelected = dimension)
            }
            UsecaseState.OCR.value -> {
                val currentInputSizeSelected = _uiState.value.textOCRSettings.commonSettings
                if(currentInputSizeSelected.processorSelectedIndex == 2){
                    dimension = 640
                }
                currentInputSizeSelected.copy(inputSizeSelected = dimension)
            }
            else -> {
                1280
            }
        }
        when (uiState.value.usecaseSelected) {
            UsecaseState.Barcode.value -> _uiState.value.barcodeSettings.commonSettings = updatedInputSize as CommonSettings
            UsecaseState.Retail.value -> _uiState.value.retailShelfSettings.commonSettings = updatedInputSize as CommonSettings
            UsecaseState.Product.value -> _uiState.value.productRecognitionSettings.commonSettings = updatedInputSize as CommonSettings
            UsecaseState.OCRFind.value -> _uiState.value.ocrFindSettings.commonSettings = updatedInputSize as CommonSettings
            UsecaseState.OCR.value -> _uiState.value.textOCRSettings.commonSettings = updatedInputSize as CommonSettings
        }
    }

    fun updateSelectedResolution(index: Int) {
        val updatedResolution = when (uiState.value.usecaseSelected) {
            UsecaseState.Barcode.value -> {
                val currentResolutionSelectedIndex = _uiState.value.barcodeSettings.commonSettings
                currentResolutionSelectedIndex.copy(resolutionSelectedIndex = index)
            }
            UsecaseState.Retail.value -> {
                val currentResolutionSelectedIndex = _uiState.value.retailShelfSettings.commonSettings
                currentResolutionSelectedIndex.copy(resolutionSelectedIndex = index)
            }

            UsecaseState.Product.value -> {
                val currentResolutionSelectedIndex = _uiState.value.productRecognitionSettings.commonSettings
                currentResolutionSelectedIndex.copy(resolutionSelectedIndex = index)
            }

            UsecaseState.OCRFind.value -> {
                val currentResolutionSelectedIndex = _uiState.value.ocrFindSettings.commonSettings
                currentResolutionSelectedIndex.copy(resolutionSelectedIndex = index)
            }
            UsecaseState.OCR.value -> {
                val currentResolutionSelectedIndex = _uiState.value.textOCRSettings.commonSettings
                currentResolutionSelectedIndex.copy(resolutionSelectedIndex = index)
            }
            else -> {
                1
            }
        }
        when (uiState.value.usecaseSelected) {
            UsecaseState.Barcode.value -> _uiState.value.barcodeSettings.commonSettings = updatedResolution as CommonSettings
            UsecaseState.Retail.value -> _uiState.value.retailShelfSettings.commonSettings = updatedResolution as CommonSettings
            UsecaseState.Product.value -> _uiState.value.productRecognitionSettings.commonSettings = updatedResolution as CommonSettings
            UsecaseState.OCRFind.value -> _uiState.value.ocrFindSettings.commonSettings = updatedResolution as CommonSettings
            UsecaseState.OCR.value -> _uiState.value.textOCRSettings.commonSettings = updatedResolution as CommonSettings
        }
    }

    fun getSelectedResolution() : Int? {
        val currentResolutionSelectedIndex = when (uiState.value.usecaseSelected) {
            UsecaseState.Barcode.value -> {
                _uiState.value.barcodeSettings.commonSettings.resolutionSelectedIndex
            }
            UsecaseState.Retail.value -> {
                _uiState.value.retailShelfSettings.commonSettings.resolutionSelectedIndex
            }

            UsecaseState.Product.value -> {
                _uiState.value.productRecognitionSettings.commonSettings.resolutionSelectedIndex
            }

            UsecaseState.OCRFind.value -> {
                _uiState.value.ocrFindSettings.commonSettings.resolutionSelectedIndex
            }
            UsecaseState.OCR.value -> {
                _uiState.value.textOCRSettings.commonSettings.resolutionSelectedIndex
            }
            else -> {
                null
            }
        }
        return currentResolutionSelectedIndex
    }
    fun getProcessorSelectedIndex() : Int? {
        val currentProcessorSelectedIndex = when (uiState.value.usecaseSelected) {
            UsecaseState.Barcode.value -> {
                _uiState.value.barcodeSettings.commonSettings.processorSelectedIndex
            }
            UsecaseState.Retail.value -> {
                _uiState.value.retailShelfSettings.commonSettings.processorSelectedIndex
            }

            UsecaseState.Product.value -> {
                _uiState.value.productRecognitionSettings.commonSettings.processorSelectedIndex
            }

            UsecaseState.OCRFind.value -> {
                _uiState.value.ocrFindSettings.commonSettings.processorSelectedIndex
            }
            UsecaseState.OCR.value -> {
                _uiState.value.textOCRSettings.commonSettings.processorSelectedIndex
            }
            else -> {
                null
            }
        }
        return currentProcessorSelectedIndex
    }

    fun getInputSizeSelected() : Int? {
        val currentInputSizeSelected = when (uiState.value.usecaseSelected) {
            UsecaseState.Barcode.value -> {
                _uiState.value.barcodeSettings.commonSettings.inputSizeSelected
            }
            UsecaseState.Retail.value -> {
                _uiState.value.retailShelfSettings.commonSettings.inputSizeSelected
            }

            UsecaseState.Product.value -> {
                _uiState.value.productRecognitionSettings.commonSettings.inputSizeSelected
            }

            UsecaseState.OCRFind.value -> {
                _uiState.value.ocrFindSettings.commonSettings.inputSizeSelected
            }
            UsecaseState.OCR.value -> {
                _uiState.value.textOCRSettings.commonSettings.inputSizeSelected
            }
            else -> {
                null
            }
        }
        return currentInputSizeSelected
    }

    /**
     * Update the barcode symbologies
     */
    fun updateBarcodeSymbology(name: String, enabled: Boolean) {
        val currentSymbology = _uiState.value.barcodeSettings.barcodeSymbology
        val updatedSymbology = when (name) {
            getString(context, R.string.australian_postal) -> currentSymbology.copy( australian_postal = enabled)


            getString(context, R.string.aztec) -> {
                currentSymbology.copy(
                    aztec = enabled
                )
            }

            getString(context, R.string.canadian_postal) -> {
                currentSymbology.copy(
                    canadian_postal = enabled
                )
            }

            getString(context, R.string.chinese_2of5) -> {
                currentSymbology.copy(
                    chinese_2of5 = enabled
                )
            }

            getString(context, R.string.codabar) -> {
                currentSymbology.copy(
                    codabar = enabled
                )
            }

            getString(context, R.string.code11) -> {
                currentSymbology.copy(
                    code11 = enabled
                )

            }

            getString(context, R.string.code39) -> {

                currentSymbology.copy(
                    code39 = enabled
                )

            }

            getString(context, R.string.code93) -> {

                currentSymbology.copy(
                    code93 = enabled
                )

            }

            getString(context, R.string.code128) -> {

                currentSymbology.copy(
                    code128 = enabled
                )

            }

            getString(context, R.string.composite_ab) -> {

                currentSymbology.copy(
                    composite_ab = enabled
                )

            }

            getString(context, R.string.composite_c) -> {

                currentSymbology.copy(
                    composite_c = enabled
                )

            }

            getString(context, R.string.d2of5) -> {

                currentSymbology.copy(
                    d2of5 = enabled
                )

            }

            getString(context, R.string.datamatrix) -> {

                currentSymbology.copy(
                    datamatrix = enabled
                )

            }

            getString(context, R.string.dotcode) -> {

                currentSymbology.copy(
                    dotcode = enabled
                )

            }

            getString(context, R.string.dutch_postal) -> {

                currentSymbology.copy(
                    dutch_postal = enabled
                )

            }

            getString(context, R.string.ean_8) -> {

                currentSymbology.copy(
                    ean_8 = enabled
                )

            }

            getString(context, R.string.ean_13) -> {

                currentSymbology.copy(
                    ean_13 = enabled
                )

            }

            getString(context, R.string.finnish_postal_4s) -> {

                currentSymbology.copy(
                    finnish_postal_4s = enabled
                )

            }

            getString(context, R.string.grid_matrix) -> {

                currentSymbology.copy(
                    grid_matrix = enabled
                )

            }

            getString(context, R.string.gs1_databar) -> {

                currentSymbology.copy(
                    gs1_databar = enabled
                )

            }

            getString(context, R.string.gs1_databar_expanded) -> {

                currentSymbology.copy(
                    gs1_databar_expanded = enabled
                )

            }

            getString(context, R.string.gs1_databar_lim) -> {

                currentSymbology.copy(
                    gs1_databar_lim = enabled
                )

            }

            getString(context, R.string.gs1_datamatrix) -> {

                currentSymbology.copy(
                    gs1_datamatrix = enabled
                )
            }

            getString(context, R.string.gs1_qrcode) -> {

                currentSymbology.copy(
                    gs1_qrcode = enabled
                )

            }

            getString(context, R.string.hanxin) -> {

                currentSymbology.copy(
                    hanxin = enabled
                )

            }

            getString(context, R.string.i2of5) -> {

                currentSymbology.copy(
                    i2of5 = enabled
                )

            }

            getString(context, R.string.japanese_postal) -> {

                currentSymbology.copy(
                    japanese_postal = enabled
                )

            }

            getString(context, R.string.korean_3of5) -> {

                currentSymbology.copy(
                    korean_3of5 = enabled
                )

            }

            getString(context, R.string.mailmark) -> {

                currentSymbology.copy(
                    mailmark = enabled
                )
            }

            getString(context, R.string.matrix_2of5) -> {

                currentSymbology.copy(
                    matrix_2of5 = enabled
                )

            }

            getString(context, R.string.maxicode) -> {

                currentSymbology.copy(
                    maxicode = enabled
                )

            }

            getString(context, R.string.micropdf) -> {

                currentSymbology.copy(
                    micropdf = enabled
                )

            }

            getString(context, R.string.microqr) -> {

                currentSymbology.copy(
                    microqr = enabled
                )

            }

            getString(context, R.string.msi) -> {

                currentSymbology.copy(
                    msi = enabled
                )

            }

            getString(context, R.string.pdf417) -> {

                currentSymbology.copy(
                    pdf417 = enabled
                )

            }

            getString(context, R.string.qrcode) -> {

                currentSymbology.copy(
                    qrcode = enabled
                )

            }

            getString(context, R.string.tlc39) -> {

                currentSymbology.copy(
                    tlc39 = enabled
                )

            }

            getString(context, R.string.trioptic39) -> {

                currentSymbology.copy(
                    trioptic39 = enabled
                )

            }

            getString(context, R.string.uk_postal) -> {

                currentSymbology.copy(
                    uk_postal = enabled
                )

            }

            getString(context, R.string.upc_a) -> {

                currentSymbology.copy(
                    upc_a = enabled
                )

            }

            getString(context, R.string.upce0) -> {

                currentSymbology.copy(
                    upce0 = enabled
                )

            }

            getString(context, R.string.upce1) -> {

                currentSymbology.copy(
                    upce1 = enabled
                )
            }

            getString(context, R.string.usplanet) -> {

                currentSymbology.copy(
                    usplanet = enabled
                )

            }

            getString(context, R.string.uspostnet) -> {

                currentSymbology.copy(
                    uspostnet = enabled
                )

            }

            getString(context, R.string.us4state) -> {

                currentSymbology.copy(
                    us4state = enabled
                )

            }

            getString(context, R.string.us4state_fics) -> {

                currentSymbology.copy(
                    us4state_fics = enabled
                )

            }

            else -> {
                currentSymbology
            }
        }
        _uiState.value.barcodeSettings.barcodeSymbology = updatedSymbology
    }

    /**
     * Update the current bitmap used for processing by the models
     */
    fun updateBitmap(bitmap: Bitmap, rotation: Int) {
        val matrix: Matrix = Matrix()
        matrix.postRotate(rotation.toFloat())
        val rotatedBitmap =
            Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )

        _uiState.update { currentState ->
            currentState.copy(
                currentBitmap = rotatedBitmap
            )
        }
    }

    suspend fun takePicture(): Bitmap = suspendCancellableCoroutine { continuation ->
        executor?.let { cameraExecutor ->
            imageCapture!!.takePicture(
                cameraExecutor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val highResBitmap: Bitmap =
                            productEnrollmentRecognition?.rotateBitmapIfNeeded(imageProxy = image)!!
                        image.close()
                        continuation.resume(highResBitmap)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        continuation.resumeWithException(exception)
                    }
                })
        }
    }

    private fun setAnalyzer(activityLifecycle: Lifecycle) {
        when (uiState.value.usecaseSelected) {
            UsecaseState.Barcode.value -> {
                barcodeEntityTrackerAnalyzer?.let {
                    val analyzer = it.setupEntityTrackerAnalyzer(activityLifecycle)
                    analysisUseCase?.setAnalyzer(executor!!, analyzer)
                }
            }

            UsecaseState.Retail.value -> {
                retailShelfAnalyzer?.let {
                    analysisUseCase?.setAnalyzer(executor!!, it)
                }
            }

            UsecaseState.Product.value -> {
                productEnrollmentRecognition?.let {
                    analysisUseCase?.setAnalyzer(executor!!, it)
                }
            }

            UsecaseState.OCRFind.value,
            UsecaseState.OCR.value -> {
                ocrAnalyzer?.let {
                    analysisUseCase?.setAnalyzer(executor!!, it)
                }
            }
        }
    }

    fun setOCRFilterType(ocrFilterTypeData: OCRFilterData) {
        ocrAnalyzer?.setOCRFilterType(ocrFilterTypeData = ocrFilterTypeData)
    }

    fun updateAppBarTitle(title: String) {
        _uiState.update { currentState ->
            currentState.copy(
                appBarTitle = title
            )
        }
    }

    fun updateOCRTextFieldValues(name: String, value: String) {
        val advancedOCRSetting = _uiState.value.textOCRSettings.advancedOCRSetting
        val updatedOCRSetting =
        when (name) {
            getString(context, R.string.heatmap_threshold) -> {
                advancedOCRSetting.copy(
                        heatmapThreshold = value
                    )
            }

            getString(context, R.string.box_threshold) -> {
                    advancedOCRSetting.copy(
                        boxThreshold = value
                    )
            }

            getString(context, R.string.min_box_area) -> {
                    advancedOCRSetting.copy(
                        minBoxArea = value
                    )
            }

            getString(context, R.string.min_box_size) -> {
                    advancedOCRSetting.copy(
                        minBoxSize = value
                    )
            }

            getString(context, R.string.unclip_ratio) -> {
                    advancedOCRSetting.copy(
                        unclipRatio = value
                    )
            }

            getString(context, R.string.min_ratio_for_rotation) -> {
                    advancedOCRSetting.copy(
                        minRatioForRotation = value
                    )
            }

            getString(context, R.string.character_confidence_threshold) -> {
                    advancedOCRSetting.copy(
                        maxWordCombinations = value
                    )
            }

            getString(context, R.string.max_word_combinations) -> {
                    advancedOCRSetting.copy(
                        maxWordCombinations = value
                    )
            }

            getString(context, R.string.topk_ignore_cutoff) -> {
                    advancedOCRSetting.copy(
                        topkIgnoreCutoff = value
                    )
            }

            getString(context, R.string.topk_ignore_cutoff) -> {
                    advancedOCRSetting.copy(
                        topkIgnoreCutoff = value
                    )
            }

            getString(context, R.string.total_probability_threshold) -> {
                    advancedOCRSetting.copy(
                        totalProbabilityThreshold = value
                    )
            }

            getString(context, R.string.width_distance_ratio) -> {
                    advancedOCRSetting.copy(
                        widthDistanceRatio = value
                    )
            }

            getString(context, R.string.height_distance_ratio) -> {
                    advancedOCRSetting.copy(
                        heightDistanceRatio = value
                    )
            }

            getString(context, R.string.center_distance_ratio) -> {
                    advancedOCRSetting.copy(
                        centerDistanceRatio = value
                    )
            }

            getString(context, R.string.paragraph_height_distance) -> {
                    advancedOCRSetting.copy(
                        paragraphHeightDistance = value
                    )
            }

            getString(context, R.string.paragraph_height_ratio_threshold) -> {
                    advancedOCRSetting.copy(
                        paragraphHeightRatioThreshold = value
                    )
            }

            getString(context, R.string.top_correlation_threshold) -> {
                    advancedOCRSetting.copy(
                        topCorrelationThreshold = value
                    )
            }

            getString(context, R.string.merge_points_cutoff) -> {
                    advancedOCRSetting.copy(
                        mergePointsCutoff = value
                    )
            }

            getString(context, R.string.split_margin_factor) -> {
                    advancedOCRSetting.copy(
                        splitMarginFactor = value
                    )
            }

            getString(context, R.string.aspect_ratio_lower_threshold) -> {
                    advancedOCRSetting.copy(
                        aspectRatioLowerThreshold = value
                    )
            }

            getString(context, R.string.aspect_ratio_upper_threshold) -> {
                    advancedOCRSetting.copy(
                        aspectRatioUpperThreshold = value
                    )
            }

            getString(context, R.string.topK_merged_predictions) -> {
                    advancedOCRSetting.copy(
                        topKMergedPredictions = value
                    )
            }

            else -> {
                advancedOCRSetting
            }
        }
        _uiState.value.textOCRSettings.advancedOCRSetting = updatedOCRSetting
    }

    fun updateOCRSwitchOptions(name: String, enabled: Boolean) {
        val advancedOCRSetting = _uiState.value.textOCRSettings.advancedOCRSetting
        val updatedOCRSetting =
            when (name) {
            getString(context, R.string.enable_tiling) -> {
                advancedOCRSetting.copy(
                        enableTiling = enabled
                    )
            }

            getString(context, R.string.enable_grouping) -> {
                advancedOCRSetting.copy(
                        enableGrouping = enabled
                    )
            }
            else -> {
                advancedOCRSetting
            }
        }
        _uiState.value.textOCRSettings.advancedOCRSetting = updatedOCRSetting
    }

    fun applySettings() {
        deinitModel()
        saveSettings()
        initModel()
    }

    fun saveSettings() {
        when (uiState.value.usecaseSelected) {
            UsecaseState.Barcode.value -> {
                FileUtils.saveBarcodeSettings(uiState.value.barcodeSettings)
            }

            UsecaseState.Retail.value -> {
                FileUtils.saveRetailShelfSettings(uiState.value.retailShelfSettings)
            }
            UsecaseState.Product.value -> {
                FileUtils.saveProductRecognitionSettings(uiState.value.productRecognitionSettings)
            }

            UsecaseState.OCRFind.value -> {
                FileUtils.saveOCRFindSettings(uiState.value.ocrFindSettings)
            }
            UsecaseState.OCR.value -> {
                FileUtils.saveOCRSettings(uiState.value.textOCRSettings)
            }

            UsecaseState.Main.value -> {

            }
        }
    }
    fun restoreDefaultSettings() {
        when (_uiState.value.usecaseSelected) {
            UsecaseState.Barcode.value -> {
                _uiState.value.barcodeSettings = BarcodeSettings()
            }

            UsecaseState.Retail.value -> {
                _uiState.value.retailShelfSettings = RetailShelfSettings()
            }
            UsecaseState.Product.value -> {
                _uiState.value.productRecognitionSettings = ProductRecognitionSettings()
            }

            UsecaseState.OCRFind.value -> {
                _uiState.value.ocrFindSettings = OcrFindSettings()
            }
            UsecaseState.OCR.value -> {
                _uiState.value.textOCRSettings = TextOcrSettings()
            }

            UsecaseState.Main.value -> {

            }
        }
    }

    fun getString(resId: Int): String {
        return getString(context, resId)
    }

    /**
     * This function is used to load a new product database into the
     * production recognition pipeline
     */
    fun loadProductIndex(uri: Uri) {
        val productDBFile = File(mCacheDir, databaseFile)
        FileUtils.saveFile(uri, productDBFile.toUri())
        productEnrollmentRecognition?.applyProductDB()
    }

    /**
     * This function is used to delete the product data from the
     * production recognition pipeline
     */
    fun deleteProductIndex() {
        productEnrollmentRecognition?.deleteProductDB()
    }

    /**
     * This function is used to add product data into the
     * existing product database used by the production recognition pipeline
     */
    fun enrollProductIndex() {
        if (uiState.value.bboxes.size == 0) {
            return
        }
        productEnrollmentRecognition?.enrollProductIndex(uiState.value.productResults)
    }

    fun updateModelDemoReady(isReady: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                modelDemoReady = isReady
            )
        }
    }

    fun updateProductEnrollmentState(state: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                isProductEnrollmentCompleted = state
            )
        }
    }

    fun updateBarcodeResultData(results: List<ResultData>) {
        _uiState.update { it ->
            it.copy(
                barcodeResults = results
            )
        }
    }

    fun updateRetailShelfDetectionResult(results: Array<BBox?>?) {
        val bBoxesResult = results ?: arrayOf()

        _uiState.update { currentState ->
            currentState.copy(
                bboxes = bBoxesResult
            )
        }
    }

    fun updateProductRecognitionResult(results: MutableList<ProductData>?) {
        val productRecognitionResult = results ?: mutableListOf()
        _uiState.update { productResults ->
            productResults.copy(
                productResults = productRecognitionResult
            )
        }
    }

    fun updateCaptureBitmap(bitmap: Bitmap) {
        _uiState.update { productResults ->
            productResults.copy(
                captureBitmap = bitmap
            )
        }
    }

    fun updateOcrResultData(results: List<ResultData>?) {
        val ocrResults = results ?: listOf()
//        _uiState.value.ocrResults.clear()
        _uiState.update { textResults ->
            textResults.copy(
                ocrResults = ocrResults
            )
        }
    }

    fun loadInputStreamFromAsset(fileName: String): String {
        try {
            val inputStream = assetManager.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val stringBuilder = StringBuilder()
            var line: String = reader.readLine()
            while (line != null) {
                stringBuilder.append(line)
                line = reader.readLine()
            }
            val htmlString = stringBuilder.toString();
            return htmlString

        } catch (e: IOException) {
            e.printStackTrace()
            return ""
        }
    }

    fun handleBackButton(navController: NavController) {
        val currentScreen = uiState.value.activeScreen

        if (currentScreen == Screen.DemoStart) {
            deinitModel()
            updateSelectedUsecase(UsecaseState.Main.value)
            updateAppBarTitle(context.getString(R.string.app_name))
        } else if (currentScreen == Screen.DemoSetting) {
            applySettings()
        } else if (currentScreen == Screen.Capture) {
            if (uiState.value.usecaseSelected == UsecaseState.Product.value) {
                // clear all the previous results
                updateProductRecognitionResult(results = null)
                updateRetailShelfDetectionResult(results = null)
                updateProductEnrollmentState(state = false)
                startPreviewAnalysis()
                startProcessing()
            }
        }
        setZoom(1.0f)
        navController.navigateUp()
    }

    fun toast(toastString: String) {
        Toast.makeText(context, toastString, Toast.LENGTH_LONG).show()
    }

    fun updateActiveScreenData(activeScreen: Screen) {
        _uiState.update { uiStateData ->
            uiStateData.copy(
                activeScreen = activeScreen
            )
        }
    }

    fun stopPreviewAnalysis() {
        productEnrollmentRecognition!!.stopPreviewAnalysis()
    }

    fun startPreviewAnalysis() {
        productEnrollmentRecognition!!.startPreviewAnalysis()
    }

    fun executeHighRes(highResBitmap: Bitmap) {
        productEnrollmentRecognition!!.executeHighRes(highResBitmap)
    }

    fun updateToastMessage(message: String?) {
        _uiState.update { uiStateData ->
            uiStateData.copy(
                toastMessage = message
            )
        }
    }
}
