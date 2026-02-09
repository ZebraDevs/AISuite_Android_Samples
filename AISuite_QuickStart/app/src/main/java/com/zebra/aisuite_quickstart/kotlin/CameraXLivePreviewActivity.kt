// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin

import android.content.SharedPreferences
import android.graphics.Matrix
import android.graphics.RectF
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Display
import android.view.Surface
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.zebra.ai.vision.analyzer.tracking.EntityTrackerAnalyzer
import com.zebra.ai.vision.detector.BBox
import com.zebra.ai.vision.detector.BarcodeDecoder
import com.zebra.ai.vision.detector.Recognizer
import com.zebra.ai.vision.detector.Word
import com.zebra.ai.vision.entity.BarcodeEntity
import com.zebra.ai.vision.entity.Entity
import com.zebra.ai.vision.entity.ParagraphEntity
import com.zebra.ai.vision.viewfinder.EntityViewController
import com.zebra.ai.vision.viewfinder.listners.EntityViewResizeSpecs
import com.zebra.aisuite_quickstart.CameraXViewModel
import com.zebra.aisuite_quickstart.R
import com.zebra.aisuite_quickstart.databinding.ActivityCameraXlivePreviewBinding
import com.zebra.aisuite_quickstart.filtertracker.FilterDialog
import com.zebra.aisuite_quickstart.kotlin.analyzers.tracker.Tracker
import com.zebra.aisuite_quickstart.kotlin.camera.CameraManager
import com.zebra.aisuite_quickstart.kotlin.detectors.barcodedecodersample.BarcodeAnalyzer
import com.zebra.aisuite_quickstart.kotlin.detectors.barcodedecodersample.BarcodeHandler
import com.zebra.aisuite_quickstart.kotlin.detectors.productrecognition.ProductRecognitionHandler
import com.zebra.aisuite_quickstart.kotlin.detectors.textocrsample.OCRHandler
import com.zebra.aisuite_quickstart.kotlin.detectors.textocrsample.TextOCRAnalyzer
import com.zebra.aisuite_quickstart.kotlin.handlers.BoundingBoxMapper
import com.zebra.aisuite_quickstart.kotlin.handlers.DetectionResultHandler
import com.zebra.aisuite_quickstart.kotlin.handlers.UIHandler
import com.zebra.aisuite_quickstart.kotlin.detectors.productrecognition.ProductRecognitionAnalyzer
import com.zebra.aisuite_quickstart.kotlin.lowlevel.productrecognitionsample.ProductRecognitionSample
import com.zebra.aisuite_quickstart.kotlin.lowlevel.productrecognitionsample.ProductRecognitionSampleAnalyzer
import com.zebra.aisuite_quickstart.kotlin.lowlevel.simplebarcodesample.BarcodeSample
import com.zebra.aisuite_quickstart.kotlin.lowlevel.simplebarcodesample.BarcodeSampleAnalyzer
import com.zebra.aisuite_quickstart.kotlin.lowlevel.simpleocrsample.OCRAnalyzer
import com.zebra.aisuite_quickstart.kotlin.lowlevel.simpleocrsample.OCRSample
import com.zebra.aisuite_quickstart.kotlin.viewfinder.EntityBarcodeTracker
import com.zebra.aisuite_quickstart.kotlin.viewfinder.EntityViewGraphic
import com.zebra.aisuite_quickstart.utils.CommonUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraXLivePreviewActivity : AppCompatActivity(), BarcodeAnalyzer.DetectionCallback,
    TextOCRAnalyzer.DetectionCallback, ProductRecognitionAnalyzer.DetectionCallback,
    Tracker.DetectionCallback,
    EntityBarcodeTracker.DetectionCallback, BarcodeSampleAnalyzer.SampleBarcodeDetectionCallback,
    OCRAnalyzer.DetectionCallback, ProductRecognitionSampleAnalyzer.SampleDetectionCallback {

    lateinit var binding: ActivityCameraXlivePreviewBinding
    private val tag = "CameraXLivePreviewActivityKotlin"
    private var analysisUseCase: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var cameraManager: CameraManager
    private lateinit var boundingBoxMapper: BoundingBoxMapper
    private lateinit var detectionHandler: DetectionResultHandler
    private lateinit var uiHandler: UIHandler
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0
    private val executors: ExecutorService = Executors.newFixedThreadPool(3)

    private var barcodeHandler: BarcodeHandler? = null
    private var ocrHandler: OCRHandler? = null
    private var ocrSample: OCRSample? = null
    private var productRecognitionHandler: ProductRecognitionHandler? = null
    private var productRecognitionSample: ProductRecognitionSample? = null
    private var tracker: Tracker? = null
    private var barcodeLegacySample: BarcodeSample? = null
    private var entityBarcodeTracker: EntityBarcodeTracker? = null
    private var selectedModel = BARCODE_DETECTION
    private val stateSelectedModel = "selected_model"
    private var previousSelectedModel = ""
    var entityViewController: EntityViewController? = null
    private var entityViewGraphic: EntityViewGraphic? = null
    private var isIconStyleEnable = false
    private var initialRotation = Surface.ROTATION_0
    private var displayManager: DisplayManager? = null
    private var displayListener: DisplayManager.DisplayListener? = null
    private var selectedSize: Size = Size(1920, 1080)

    // Orientation constants for clarity
    val ROTATION_0: Int = Surface.ROTATION_0 // 0
    val ROTATION_180: Int = Surface.ROTATION_180 // 2

    // Store pending viewfinder resize data to apply once analyzer is ready
    private var pendingTransformMatrix: Matrix? = null
    private var pendingCropRegion: RectF? = null
    private var sharedPreferences: SharedPreferences? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCameraXlivePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.camx_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences(CommonUtils.PREFS_NAME_KOTLIN, MODE_PRIVATE)

        initializeComponents()

        // Restore UI state if needed
        if (savedInstanceState != null) {
            uiHandler = UIHandler(this, cameraManager, sharedPreferences)
        }

        // Setup UI and camera
        uiHandler.setupSpinner()
        setupDisplayRotationListener()
        initEntityView()
        setupCamera()
    }

    private fun initializeComponents() {
        cameraManager = CameraManager(this, this, this)
        boundingBoxMapper = BoundingBoxMapper(this, this)
        detectionHandler = DetectionResultHandler(this, boundingBoxMapper)
        uiHandler = UIHandler(this, cameraManager, sharedPreferences)
        cameraManager.setUIHandler(uiHandler)

        val selectedSize = cameraManager.getSelectedSize()
        imageWidth = selectedSize.height
        imageHeight = selectedSize.width

        boundingBoxMapper.setImageDimensions(imageWidth, imageHeight)
        boundingBoxMapper.setFrontCamera(cameraManager.isFrontCamera())
    }

    private fun setupCamera() {
        val cameraXViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[CameraXViewModel::class.java]

        cameraXViewModel.processCameraProvider.observe(this) { provider ->
            cameraManager.setCameraProvider(provider)
            Log.v(tag, "Binding all camera use cases")
            bindAllCameraUseCases()
        }
    }

    private fun initEntityView() {
        entityViewController = EntityViewController(binding.entityView, this)
        entityViewController!!.registerEntityClickListener { _: Entity? ->
            isIconStyleEnable = !isIconStyleEnable
            entityViewGraphic!!.enableIconPen(isIconStyleEnable)
        }

        entityViewController!!.registerViewfinderResizeListener { entityViewResizeSpecs: EntityViewResizeSpecs? ->
            if (entityBarcodeTracker != null && entityBarcodeTracker!!.getEntityTrackerAnalyzer() != null) {
                // Analyzer is ready, apply the transform immediately
                entityBarcodeTracker!!.getEntityTrackerAnalyzer()!!
                    .updateTransform(entityViewResizeSpecs!!.sensorToViewMatrix)
                entityBarcodeTracker!!.getEntityTrackerAnalyzer()!!
                    .setCropRect(entityViewResizeSpecs.viewfinderFOVCropRegion)

                // Clear any pending data
                pendingTransformMatrix = null
                pendingCropRegion = null
                Log.d(tag, "Applied viewfinder resize specs immediately")
            } else {
                // Analyzer not ready yet, extract and store the actual VALUES

                try {
                    pendingTransformMatrix = Matrix(entityViewResizeSpecs!!.sensorToViewMatrix)
                    pendingCropRegion = RectF(entityViewResizeSpecs.viewfinderFOVCropRegion)
                    Log.d(tag, "Stored pending viewfinder resize data for later application")
                } catch (e: java.lang.Exception) {
                    Log.e(tag, "Failed to extract resize spec values", e)
                    pendingTransformMatrix = null
                    pendingCropRegion = null
                }
            }
        }

        entityViewGraphic = EntityViewGraphic(entityViewController!!)
    }

    private fun setupDisplayRotationListener() {
        displayManager = getSystemService(DISPLAY_SERVICE) as DisplayManager
        displayListener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {}
            override fun onDisplayRemoved(displayId: Int) {}
            override fun onDisplayChanged(displayId: Int) {
                val defaultDisplay: Display? = windowManager.defaultDisplay
                val newRotation = defaultDisplay?.rotation ?: return
                if (newRotation == initialRotation) return

                runOnUiThread {
                    initialRotation = newRotation
                    cameraManager.updateTargetRotation(newRotation)
                    boundingBoxMapper.setInitialRotation(initialRotation)

                    val selectedSize = cameraManager.getSelectedSize()
                    if (initialRotation == Surface.ROTATION_0 || initialRotation == Surface.ROTATION_180) {
                        imageWidth = selectedSize.height
                        imageHeight = selectedSize.width
                    } else {
                        imageWidth = selectedSize.width
                        imageHeight = selectedSize.height
                    }
                    boundingBoxMapper.setImageDimensions(imageWidth, imageHeight)
                    Log.i(
                        tag,
                        "Display changed: rotation=$newRotation, imageWidth=$imageWidth, imageHeight=$imageHeight"
                    )
                }
            }
        }
        displayManager?.registerDisplayListener(displayListener, null)
    }

    private fun bindAllCameraUseCases() {
        cameraManager.let {
            cameraManager.unbindAll()
            bindPreviewUseCase()
            bindAnalysisUseCase()
        }
    }

    private fun bindPreviewUseCase() {
        cameraManager.bindPreviewAndAnalysis(getPreviewSurfaceProvider())
    }

    fun clearGraphicOverlay() {
        runOnUiThread {
            binding.graphicOverlay.clear()
            entityViewGraphic?.clear()
        }
    }

    private fun applyPendingResizeSpecs() {
        if (pendingTransformMatrix != null && entityBarcodeTracker?.getEntityTrackerAnalyzer() != null) {
            entityBarcodeTracker!!.getEntityTrackerAnalyzer()!!
                .updateTransform(pendingTransformMatrix!!)
            entityBarcodeTracker!!.getEntityTrackerAnalyzer()!!.setCropRect(pendingCropRegion!!)
            Log.d(tag, "Applied pending viewfinder resize specs")
            pendingTransformMatrix = null // Clear pending specs after applying
            pendingCropRegion = null
        }
    }

    override fun onEntityBarcodeTrackerReady() {
        Log.d(tag, "EntityBarcodeTracker is ready, applying pending configurations")
        applyPendingResizeSpecs()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(stateSelectedModel, selectedModel)
    }

    fun bindAnalysisUseCase() {
        if (cameraManager.getCameraProvider() == null) {
            return
        }
        selectedModel = uiHandler.getSelectedModel()
        analysisUseCase = cameraManager.getAnalysisUseCase()
        previousSelectedModel = selectedModel
        val filterItems = FilterDialog.trackerArray
        val selectedFilterItems: MutableList<String?> = ArrayList()
        for (filterItem in filterItems) {
            val defaultValue = filterItem.equals(FilterDialog.BARCODE_TRACKER, ignoreCase = true)
            val isChecked = sharedPreferences!!.getBoolean(filterItem, defaultValue)
            if (isChecked) selectedFilterItems.add(filterItem)
        }

        try {
            when (selectedModel) {
                BARCODE_DETECTION -> {
                    Log.i(tag, "Using Barcode Decoder")
                    executors.execute {
                        barcodeHandler =
                            BarcodeHandler(this, this@CameraXLivePreviewActivity, analysisUseCase!!)
                    }
                }

                TEXT_OCR_DETECTION -> {
                    Log.i(tag, "Using Text OCR")
                    executors.execute {
                        ocrHandler =
                            OCRHandler(this, this@CameraXLivePreviewActivity, analysisUseCase!!)
                    }
                }

                ENTITY_VIEW_FINDER -> {
                    Log.i(tag, "Using Entity View Analyzer")
                    executors.execute {
                        analysisUseCase?.let {
                            entityBarcodeTracker =
                                EntityBarcodeTracker(this, this, it)
                        }
                    }
                }

                ENTITY_ANALYZER -> {
                    Log.i(tag, "Using Entity Analyzer")
                    executors.execute {
                        analysisUseCase?.let { tracker = Tracker(this, this, it, selectedFilterItems) }
                    }
                }

                PRODUCT_RECOGNITION -> {
                    Log.i(tag, "Using Product Recognition")
                    executors.execute {
                            productRecognitionHandler = ProductRecognitionHandler(this, this, analysisUseCase!!)
                    }
                }

                LEGACY_BARCODE_DETECTION -> {
                    Log.i(tag, "Using Legacy Barcode Decoder")
                    executors.execute {
                        barcodeLegacySample =
                            BarcodeSample(this, this@CameraXLivePreviewActivity, analysisUseCase!!)
                    }
                }

                LEGACY_OCR_DETECTION -> {
                    Log.i(tag, "Using Legacy Text OCR")
                    executors.execute {
                        ocrSample =
                            OCRSample(this, this@CameraXLivePreviewActivity, analysisUseCase!!)

                    }
                }
                LEGACY_PRODUCT_RECOGNITION -> {
                    Log.i(tag, "Using Product Recognition")
                    executors.execute {
                        productRecognitionSample =
                            ProductRecognitionSample(this, this@CameraXLivePreviewActivity, analysisUseCase!!)
                    }
                }

                else -> throw IllegalStateException("Invalid model name")
            }
        } catch (e: Exception) {
            Log.e(tag, "Cannot create model for: $selectedModel", e)
            return
        }
    }

    public override fun onResume() {
        super.onResume()
        Log.v(tag, "OnResume called")
        clearGraphicOverlay()

        val currentRotation = binding.previewView.display?.rotation ?: Surface.ROTATION_0
        if (currentRotation != initialRotation) {
            Log.d(
                tag,
                "Rotation changed during pause, updating initialRotation from $initialRotation to $currentRotation"
            )
            initialRotation = currentRotation
            cameraManager.updateTargetRotation(initialRotation)
            boundingBoxMapper.setInitialRotation(initialRotation)

            val selectedSize = cameraManager.getSelectedSize()
            if (initialRotation == ROTATION_0 || initialRotation == ROTATION_180) {
                imageWidth = selectedSize.height
                imageHeight = selectedSize.width
            } else {
                imageWidth = selectedSize.width
                imageHeight = selectedSize.height
            }
            boundingBoxMapper.setImageDimensions(imageWidth, imageHeight)
            Log.d(tag, "Updated imageWidth=$imageWidth, imageHeight=$imageHeight")
        }
        if (uiHandler.isSpinnerInitialized) bindAllCameraUseCases()
    }

    fun stopAnalyzing() {
        try {
            when (previousSelectedModel) {
                BARCODE_DETECTION -> {
                    Log.i(tag, "Stopping the barcode analyzer")
                    barcodeHandler?.getBarcodeAnalyzer()?.stop()
                }

                TEXT_OCR_DETECTION -> {
                    Log.i(tag, "Stopping the ocr analyzer")
                    ocrHandler?.getOCRAnalyzer()?.stop()
                }

                ENTITY_VIEW_FINDER -> {
                    Log.i(tag, "Stopping the entity tracker")
                    entityBarcodeTracker?.stopAnalyzing()
                }

                ENTITY_ANALYZER -> {
                    Log.i(tag, "Stopping the entity tracker")
                    tracker?.stopAnalyzing()
                }

                PRODUCT_RECOGNITION -> {
                    Log.i(tag, "Stopping the recognition analyzer")
                    productRecognitionHandler?.getProductRecognitionAnalyzer()?.stopAnalyzing()
                }

                LEGACY_BARCODE_DETECTION -> {
                    Log.i(tag, "Stopping the legacy barcode analyzer")
                    barcodeLegacySample?.getBarcodeAnalyzer()?.stop()
                }

                LEGACY_OCR_DETECTION -> {
                    Log.i(tag, "Stopping the legacy ocr analyzer")
                    ocrSample?.getOCRAnalyzer()?.stop()
                }

                LEGACY_PRODUCT_RECOGNITION -> {
                    Log.i(tag, "Stopping the legacy product recognition analyzer")
                    productRecognitionSample?.getProductRecognitionAnalyzer()?.stopAnalyzing()
                }

                else -> Log.e(tag, "Invalid stop analyzer option")
            }
        } catch (e: java.lang.Exception) {
            Log.e(tag, "Can not stop the analyzer : $previousSelectedModel", e)
        }
    }

    override fun onDetectionResult(result: List<BarcodeEntity>) {
        detectionHandler.handleBarcodeDetection(result)
    }

    override fun onDetectionTextResult(list: List<ParagraphEntity>) {
        detectionHandler.handleTextOCRDetection(list)
    }

    override fun handleEntities(result: EntityTrackerAnalyzer.Result) {

            val barcodeEntities = tracker?.getBarcodeDecoder()?.let { result.getValue(it) }
            val ocrEntities = tracker?.getTextOCR()?.let { result.getValue(it) }
            val moduleEntities = tracker?.getModuleRecognizer()?.let { result.getValue(it) }
            detectionHandler.handleEntityTrackerDetection(barcodeEntities, ocrEntities, moduleEntities)

    }

    override fun handleEntitiesForEntityView(result: EntityTrackerAnalyzer.Result) {
        Log.d(tag, "Handle View Entity - Result received")
        applyPendingResizeSpecs()
        lifecycleScope.launch(Dispatchers.Main) {
            val entities = if (entityBarcodeTracker?.getBarcodeDecoder() != null) {
                result.getValue(entityBarcodeTracker!!.getBarcodeDecoder()!!)
            } else {
                Log.w(tag, "EntityBarcodeTracker or decoder is null - tracker: ${entityBarcodeTracker != null}, decoder: ${entityBarcodeTracker?.getBarcodeDecoder() != null}")
                null
            }
            Log.d(tag, "EntityBarcodeTracker decoder available, entities count: ${entities?.size ?: "null"}")
            if (entityViewGraphic != null) {
                entityViewGraphic?.clear()
            } else {
                Log.w(tag, "EntityViewGraphic is null")
            }
            if (entities != null && entityViewGraphic != null) {
                Log.d(tag, "Processing ${entities.size} entities for entity view")
                detectionHandler.handleEntityViewFinderDetection(
                    entities as MutableList<Entity?> as List<Entity>?,
                    entityViewGraphic!!
                )
                Log.d(tag, "Rendered entities on entity view")
            } else {
                Log.w(tag, "No entities to process for entity view")
            }
        }
    }

    override fun onRecognitionResult(result: List<Entity>?) {
        detectionHandler.handleDetectionRecognitionResult(result)
    }

    override fun onDetectionResult(list: Array<BarcodeDecoder.Result>) {
        detectionHandler.handleLegacyBarcodeDetection(list)
    }

    override fun onDetectionTextResult(list: Array<Word>) {
        detectionHandler.handleLegacyTextOCRDetection(list)
    }

    fun disposeModels() {
        try {
            when (previousSelectedModel) {
                BARCODE_DETECTION -> {
                    Log.i(tag, "Disposing the barcode analyzer")
                    barcodeHandler?.stop()
                }

                TEXT_OCR_DETECTION -> {
                    Log.i(tag, "Disposing the ocr analyzer")
                    ocrHandler?.stop()
                }

                ENTITY_VIEW_FINDER -> {
                    Log.i(tag, "Disposing the entity View tracker analyzer")
                    entityBarcodeTracker?.stop()
                    entityBarcodeTracker = null
                }

                ENTITY_ANALYZER -> {
                    Log.i(tag, "Disposing the entity tracker analyzer")
                    tracker?.stop()
                }

                PRODUCT_RECOGNITION -> {
                    Log.i(tag, "Disposing the recognition analyzer")
                    productRecognitionHandler?.stop()
                }

                LEGACY_BARCODE_DETECTION -> {
                    Log.i(tag, "Disposing the legacy barcode analyzer")
                    barcodeLegacySample?.stop()
                }

                LEGACY_OCR_DETECTION -> {
                    Log.i(tag, "Disposing the legacy ocr analyzer")
                    ocrSample?.stop()
                }

                LEGACY_PRODUCT_RECOGNITION -> {
                    Log.i(tag, "Disposing the legacy product recognition analyzer")
                    productRecognitionSample?.stop()
                }

                else -> Log.e(tag, "Invalid selected option")
            }
        } catch (e: java.lang.Exception) {
            Log.e(tag, "Can not dispose the analyzer : $previousSelectedModel", e)
        }
    }

    companion object {
        private const val BARCODE_DETECTION = "Barcode"
        private const val TEXT_OCR_DETECTION = "OCR"
        private const val ENTITY_ANALYZER = "Tracker"
        private const val PRODUCT_RECOGNITION = "Product Recognition"
        private const val ENTITY_VIEW_FINDER = "Entity Viewfinder"
        private const val LEGACY_BARCODE_DETECTION = "Legacy Barcode"
        private const val LEGACY_OCR_DETECTION = "Legacy OCR"
        private const val LEGACY_PRODUCT_RECOGNITION = "Legacy Product Recognition"

    }

    fun getPreviewSurfaceProvider(): Preview.SurfaceProvider {
        uiHandler.updatePreviewVisibility(uiHandler.isEntityViewFinder())
        if (uiHandler.isEntityViewFinder()) {
            return entityViewController!!.surfaceProvider
        }
        return binding.previewView.getSurfaceProvider()
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    public override fun onPause() {
        super.onPause()
        Log.v(tag, "onPause called")
        clearGraphicOverlay()
        stopAnalyzing()
        cameraManager.unbindAll()
        disposeModels()
    }

    override fun onDestroy() {
        displayListener?.let {
            displayManager?.unregisterDisplayListener(it)
        }
        super.onDestroy()
    }

    override fun onDetectionRecognitionResult(
        detections: Array<BBox>,
        products: Array<BBox>,
        recognitions: Array<Recognizer.Recognition>
    ) {
    detectionHandler.handleLegacyDetectionRecognitionResult(detections, products, recognitions)
    }
}