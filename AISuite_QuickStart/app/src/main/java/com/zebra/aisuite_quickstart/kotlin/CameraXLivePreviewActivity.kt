// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin

import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.common.util.concurrent.ListenableFuture
import com.zebra.ai.vision.analyzer.tracking.EntityTrackerAnalyzer
import com.zebra.ai.vision.detector.BBox
import com.zebra.ai.vision.detector.BarcodeDecoder
import com.zebra.ai.vision.detector.Recognizer
import com.zebra.ai.vision.entity.BarcodeEntity
import com.zebra.ai.vision.entity.Entity
import com.zebra.ai.vision.entity.ParagraphEntity
import com.zebra.ai.vision.viewfinder.EntityViewController
import com.zebra.ai.vision.viewfinder.listners.EntityClickListener
import com.zebra.aisuite_quickstart.CameraXViewModel
import com.zebra.aisuite_quickstart.R
import com.zebra.aisuite_quickstart.databinding.ActivityCameraXlivePreviewBinding
import com.zebra.aisuite_quickstart.kotlin.analyzers.barcodetracker.BarcodeTracker
import com.zebra.aisuite_quickstart.kotlin.analyzers.barcodetracker.BarcodeTrackerGraphic
import com.zebra.aisuite_quickstart.kotlin.viewfinder.EntityBarcodeTracker
import com.zebra.aisuite_quickstart.kotlin.detectors.barcodedecodersample.BarcodeAnalyzer
import com.zebra.aisuite_quickstart.kotlin.detectors.barcodedecodersample.BarcodeGraphic
import com.zebra.aisuite_quickstart.kotlin.detectors.barcodedecodersample.BarcodeHandler
import com.zebra.aisuite_quickstart.kotlin.viewfinder.EntityViewGraphic
import com.zebra.aisuite_quickstart.kotlin.detectors.textocrsample.OCRGraphic
import com.zebra.aisuite_quickstart.kotlin.detectors.textocrsample.OCRHandler
import com.zebra.aisuite_quickstart.kotlin.detectors.textocrsample.TextOCRAnalyzer
import com.zebra.aisuite_quickstart.kotlin.lowlevel.productrecognitionsample.ProductRecognitionAnalyzer
import com.zebra.aisuite_quickstart.kotlin.lowlevel.productrecognitionsample.ProductRecognitionGraphic
import com.zebra.aisuite_quickstart.kotlin.lowlevel.productrecognitionsample.ProductRecognitionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max

/**
 * The CameraXLivePreviewActivity class is an Android activity that demonstrates the use of CameraX
 * for live camera preview and integrates multiple detection and recognition capabilities, including
 * barcode detection, text OCR, product recognition, and entity tracking.

 * This class provides functionality for switching between different models and use cases, handling
 * camera lifecycle, managing image analysis, and updating the UI based on detection results.

 * Usage:
 * - This activity is started as part of the application to demonstrate CameraX functionalities.
 * - It binds and unbinds camera use cases based on user selection and manages their lifecycle.
 * - It provides a spinner UI to select between different detection models and dynamically adapts
 *   the camera preview and analysis based on the selected model.

 * Dependencies:
 * - CameraX: Provides camera lifecycle and use case management.
 * - BarcodeHandler, OCRHandler, ProductRecognitionHandler, BarcodeTracker, EntityBarcodeTracker:
 *   Classes that handle specific detection and recognition tasks.
 * - ActivityCameraXlivePreviewBinding: Used for view binding to access UI components.
 * - GraphicOverlay: Custom view for rendering graphical overlays on camera preview.
 * - ExecutorService: Used for asynchronous task execution.

 * Exception Handling:
 * - Handles exceptions during analyzer setup and model disposal.

 * Note: Ensure that the appropriate permissions are configured in the AndroidManifest to utilize camera capabilities.
 */
class CameraXLivePreviewActivity : AppCompatActivity(), BarcodeAnalyzer.DetectionCallback,
    TextOCRAnalyzer.DetectionCallback, ProductRecognitionAnalyzer.DetectionCallback, EntityBarcodeTracker.DetectionCallback {

    private lateinit var binding: ActivityCameraXlivePreviewBinding
    private val tag = "CameraXLivePreviewActivityKotlin"
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var camera: Camera? = null
    private var previewUseCase: Preview? = null
    private var analysisUseCase: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0
    private val SIMILARITY_THRESHOLD = 0.65f
    private val lensFacing = CameraSelector.LENS_FACING_BACK
    private lateinit var cameraSelector: CameraSelector

    private lateinit var resolutionSelector: ResolutionSelector
    private val executors: ExecutorService = Executors.newFixedThreadPool(3)

    private var barcodeHandler: BarcodeHandler? = null
    private var ocrHandler: OCRHandler? = null
    private var productRecognitionHandler: ProductRecognitionHandler? = null
    private var barcodeTracker: BarcodeTracker? = null
    private var entityBarcodeTracker: EntityBarcodeTracker? = null
    private var selectedModel = BARCODE_DETECTION
    private lateinit var barcodeDecoder: BarcodeDecoder
    private val stateSelectedModel = "selected_model"
    private var previousSelectedModel = ""
    private var isEntityViewFinder = false
    private var entityViewController: EntityViewController? = null
    private var entityViewGraphic: EntityViewGraphic? = null
    private var isIconStyleEnable = false
    private var initialRotation = Surface.ROTATION_0
    private var selectedSize: Size = Size(1920, 1080)
    private var isSpinnerInitialized = false

    // Store pending viewfinder resize data to apply once analyzer is ready
    private var pendingTransformMatrix: android.graphics.Matrix? = null
    private var pendingCropRegion: android.graphics.RectF? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            selectedModel = savedInstanceState.getString(stateSelectedModel, BARCODE_DETECTION) ?: BARCODE_DETECTION
        }

        binding = ActivityCameraXlivePreviewBinding.inflate(layoutInflater)
        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        setContentView(binding.root)

        resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(
                AspectRatioStrategy(
                    AspectRatio.RATIO_16_9,
                    AspectRatioStrategy.FALLBACK_RULE_AUTO
                )
            )
            .setResolutionStrategy(
                ResolutionStrategy(
                    selectedSize,
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            ).build()

        ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))
            .get(CameraXViewModel::class.java)
            .processCameraProvider
            .observe(
                this,
                Observer { provider: ProcessCameraProvider? ->
                    cameraProvider = provider
                    bindAllCameraUseCases()
                }
            )

        val options = listOf(
            BARCODE_DETECTION, TEXT_OCR_DETECTION, ENTITY_ANALYZER, PRODUCT_RECOGNITION, ENTITY_VIEW_FINDER
        )

        // Creating adapter for spinner
        val dataAdapter = ArrayAdapter(this, R.layout.spinner_style, options)
        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // attaching data adapter to spinner
        binding.spinner.adapter = dataAdapter
        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>, view: View?, pos: Int, l: Long) {
                selectedModel = adapterView.getItemAtPosition(pos).toString()
                isEntityViewFinder = selectedModel == ENTITY_VIEW_FINDER
                Log.e(tag, "selected option is $selectedModel")

                // Lock orientation when Entity Viewfinder is selected
                if (isEntityViewFinder) {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
                    Log.d(tag, "Orientation locked for Entity Viewfinder mode")
                } else {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    Log.d(tag, "Orientation unlocked for $selectedModel mode")
                }

                initialRotation = window?.decorView?.display?.rotation ?: Surface.ROTATION_0
                if (initialRotation == 0 || initialRotation == 2) {
                    imageWidth = selectedSize.height
                    imageHeight = selectedSize.width
                } else {
                    imageWidth = selectedSize.width
                    imageHeight = selectedSize.height
                }
                Log.d(tag, "Updated imageWidth= $imageWidth , imageHeight=  $imageHeight")

                if (!isSpinnerInitialized) {
                    isSpinnerInitialized = true
                    return
                }
                entityViewGraphic?.clear()
                binding.graphicOverlay.clear()

                stopAnalyzing()
                unBindCameraX()
                disposeModels()
                bindPreviewUseCase()
                bindAnalysisUseCase()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>) {}
        }

        entityViewController = EntityViewController(binding.entityView, this)
        entityViewController?.registerEntityClickListener(object : EntityClickListener {
            override fun onEntityClicked(entity: Entity?) {
                isIconStyleEnable = !isIconStyleEnable
                entityViewGraphic?.enableIconPen(isIconStyleEnable)
            }
        })

        entityViewController?.registerViewfinderResizeListener { entityViewResizeSpecs ->
            if (entityBarcodeTracker?.getEntityTrackerAnalyzer() != null) {
                // Analyzer is ready, apply the transform immediately
                entityBarcodeTracker!!.getEntityTrackerAnalyzer()!!.updateTransform(entityViewResizeSpecs.sensorToViewMatrix)
                entityBarcodeTracker!!.getEntityTrackerAnalyzer()!!.setCropRect(entityViewResizeSpecs.viewfinderFOVCropRegion)

                // Clear any pending data
                pendingTransformMatrix = null
                pendingCropRegion = null
                Log.d(tag, "Applied viewfinder resize specs immediately")
            } else {
                // Analyzer not ready yet
                try {
                    pendingTransformMatrix = android.graphics.Matrix(entityViewResizeSpecs.sensorToViewMatrix)
                    pendingCropRegion = android.graphics.RectF(entityViewResizeSpecs.viewfinderFOVCropRegion)
                    Log.d(tag, "Stored pending viewfinder resize data for later application")
                } catch (e: Exception) {
                    Log.e(tag, "Failed to extract resize spec values", e)
                    pendingTransformMatrix = null
                    pendingCropRegion = null
                }
            }
        }

        entityViewController?.let { controller ->
            entityViewGraphic = EntityViewGraphic(controller)
        }
    }

    /**
     * Apply any pending viewfinder resize specs to the EntityTrackerAnalyzer
     * This should be called after the analyzer is fully initialized
     */
    private fun applyPendingResizeSpecs() {
        if (pendingTransformMatrix != null && entityBarcodeTracker?.getEntityTrackerAnalyzer() != null) {
            entityBarcodeTracker!!.getEntityTrackerAnalyzer()!!.updateTransform(pendingTransformMatrix!!)
            entityBarcodeTracker!!.getEntityTrackerAnalyzer()!!.setCropRect(pendingCropRegion!!)
            Log.d(tag, "Applied pending viewfinder resize specs")
            pendingTransformMatrix = null // Clear pending specs after applying
            pendingCropRegion = null
        }
    }

    /**
     * Call this method when EntityBarcodeTracker is fully initialized
     * This ensures proper setup of the analyzer with any pending configurations
     */
    override fun onEntityBarcodeTrackerReady() {
        Log.d(tag, "EntityBarcodeTracker is ready, applying pending configurations")
        applyPendingResizeSpecs()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(stateSelectedModel, selectedModel)
    }

    private fun mapBoundingBoxToOverlay(bbox: Rect): Rect {
        val display = windowManager.defaultDisplay
        val currentRotation = display.rotation

        val relativeRotation = ((currentRotation - initialRotation + 4) % 4)

        val overlayWidth = binding.graphicOverlay.width
        val overlayHeight = binding.graphicOverlay.height

        if (overlayWidth == 0 || overlayHeight == 0) {
            return bbox
        }

        val transformedBbox = transformBoundingBoxForRotation(bbox, relativeRotation)

        var effectiveImageWidth = imageWidth
        var effectiveImageHeight = imageHeight

        if (relativeRotation == 1 || relativeRotation == 3) {
            effectiveImageWidth = imageHeight
            effectiveImageHeight = imageWidth
        }

        val scaleX: Float = overlayWidth.toFloat() / effectiveImageWidth
        val scaleY: Float = overlayHeight.toFloat() / effectiveImageHeight
        val scale = max(scaleX, scaleY)

        val offsetX: Float = (overlayWidth - effectiveImageWidth * scale) / 2f
        val offsetY: Float = (overlayHeight - effectiveImageHeight * scale) / 2f

        return Rect(
            (transformedBbox.left * scale + offsetX).toInt(),
            (transformedBbox.top * scale + offsetY).toInt(),
            (transformedBbox.right * scale + offsetX).toInt(),
            (transformedBbox.bottom * scale + offsetY).toInt()
        )
    }

    private fun transformBoundingBoxForRotation(bbox: Rect, relativeRotation: Int): Rect {
        // relativeRotation values:
        // 0: No rotation (0 degrees)
        // 1: 90 degrees clockwise
        // 2: 180 degrees
        // 3: 270 degrees clockwise
        // These values are calculated based on the difference between current and initial device rotation.
        // The transformation is needed to map the bounding box from the image coordinate system to the display coordinate system.
        when (relativeRotation) {
            0 -> // No transformation needed, image is already aligned
                return Rect(bbox)

            1 -> // 90 degree clockwise rotation: swap x/y and adjust for width
                // left becomes top, top becomes (imageWidth - right), right becomes bottom, bottom becomes (imageWidth - left)
                return Rect(
                    bbox.top,
                    imageWidth - bbox.right,
                    bbox.bottom,
                    imageWidth - bbox.left
                )

            2 -> // 180 degree rotation: flip both axes
                // left becomes (imageWidth - right), top becomes (imageHeight - bottom), right becomes (imageWidth - left), bottom becomes (imageHeight - top)
                return Rect(
                    imageWidth - bbox.right,
                    imageHeight - bbox.bottom,
                    imageWidth - bbox.left,
                    imageHeight - bbox.top
                )

            3 -> // 270 degree clockwise rotation: swap x/y and adjust for height
                // left becomes (imageHeight - bottom), top becomes left, right becomes (imageHeight - top), bottom becomes right
                return Rect(
                    imageHeight - bbox.bottom,
                    bbox.left,
                    imageHeight - bbox.top,
                    bbox.right
                )
            else -> {
                Log.w(tag, "Unknown relative rotation: $relativeRotation, using original bbox")
                return Rect(bbox)
            }
        }
    }

    // Handles barcode detection results and updates the graphical overlay
    override fun onDetectionResult(result: List<BarcodeEntity>) {
        // Launch a coroutine on the IO dispatcher for background processing
        lifecycleScope.launch(Dispatchers.Main) {
            val rects = mutableListOf<Rect>()
            val decodedStrings = mutableListOf<String>()
            binding.graphicOverlay.clear()

            // Perform calculations on a background thread
            result.forEach { bb ->
                val rect = bb.boundingBox
                if (rect != null) {
                    val overlayRect = mapBoundingBoxToOverlay(rect)
                    rects.add(overlayRect)
                    decodedStrings.add(bb.value)
                }
                Log.e(tag, "Detected entity - Value: ${bb.value}")
                Log.e(tag, "Detected entity - Symbology: ${bb.symbology}")
            }

            binding.graphicOverlay.add(
                BarcodeGraphic(
                    binding.graphicOverlay,
                    rects,
                    decodedStrings
                )
            )

        }
    }

    // Handles text OCR detection results and updates the graphical overlay
    override fun onDetectionTextResult(list: List<ParagraphEntity>) {
        // Use lifecycleScope to launch a coroutine
        lifecycleScope.launch(Dispatchers.Main) {
            val rects = mutableListOf<Rect>()
            val decodedStrings = mutableListOf<String>()
            binding.graphicOverlay.clear()
            for (entity in list) {
                val lines = entity.textParagraph.lines
                for (line in lines) {
                    for (word in line.words) {
                        val bbox = word.bbox

                        if (bbox != null && bbox.x != null && bbox.y != null && bbox.x.size >= 3 && bbox.y.size >= 3) {
                            val minX = bbox.x[0]
                            val maxX = bbox.x[2]
                            val minY = bbox.y[0]
                            val maxY = bbox.y[2]

                            val rect = Rect(minX.toInt(), minY.toInt(), maxX.toInt(), maxY.toInt())
                            val overlayRect = mapBoundingBoxToOverlay(rect)
                            val decodedValue = word.decodes[0].content
                            rects.add(overlayRect)
                            decodedStrings.add(decodedValue)
                        }
                    }
                }

                binding.graphicOverlay.add(
                    OCRGraphic(
                        binding.graphicOverlay,
                        rects,
                        decodedStrings
                    )
                )
            }
        }
    }


    private fun bindAnalysisUseCase() {
        if (cameraProvider == null) {
            return
        }

        previousSelectedModel = selectedModel

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
                        analysisUseCase?.let { entityBarcodeTracker =
                            EntityBarcodeTracker(this, this, it)
                        }
                    }
                }

                ENTITY_ANALYZER -> {
                    Log.i(tag, "Using Entity Analyzer")
                    executors.execute {
                        analysisUseCase?.let { barcodeTracker = BarcodeTracker(this, this, it) }
                    }
                }

                PRODUCT_RECOGNITION -> {
                    Log.i(tag, "Using Product Recognition")
                    executors.execute {
                        productRecognitionHandler =
                            ProductRecognitionHandler(this, this, analysisUseCase!!)

                    }
                }

                else -> throw IllegalStateException("Invalid model name")
            }
        } catch (e: Exception) {
            Log.e(tag, "Cannot create model for: $selectedModel", e)
            return
        }
        camera = cameraProvider?.bindToLifecycle(this, cameraSelector, analysisUseCase!!)
    }


    public override fun onResume() {
        super.onResume()
        Log.v(tag, "OnResume called")

        val currentRotation = binding.previewView.display?.rotation ?: Surface.ROTATION_0
        if (currentRotation != initialRotation) {
            Log.d(tag, "Rotation changed during pause, updating initialRotation from $initialRotation to $currentRotation"
            )
            initialRotation = currentRotation

            // check if the device rotation is changes when suspended (0-> 0°, 2 -> 270°)
            if (initialRotation == 0 || initialRotation == 2) {
                imageWidth = selectedSize.height
                imageHeight = selectedSize.width
            } else {
                imageWidth = selectedSize.width
                imageHeight = selectedSize.height
            }
            Log.d(tag, "Updated imageWidth=$imageWidth, imageHeight=$imageHeight")
        }
       if(isSpinnerInitialized) bindAllCameraUseCases()
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }
    public override fun onPause() {
        super.onPause()
        Log.v(tag, "onPause called")
        stopAnalyzing()
        unBindCameraX()
        disposeModels()
    }

    override fun onDestroy() {

        super.onDestroy()
    }

    private fun stopAnalyzing() {
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
                ENTITY_VIEW_FINDER ->{
                    Log.i(tag, "Stopping the entity tracker")
                    entityBarcodeTracker?.stopAnalyzing()
                }
                ENTITY_ANALYZER ->{
                    Log.i(tag, "Stopping the entity tracker")
                    barcodeTracker?.stopAnalyzing()
                }
                PRODUCT_RECOGNITION -> {
                    Log.i(tag, "Stopping the recognition analyzer")
                    productRecognitionHandler?.getProductRecognitionAnalyzer()?.stop()
                }

                else -> Log.e(tag,"Invalid stop analyzer option")
            }
        } catch (e: java.lang.Exception) {
            Log.e(tag, "Can not stop the analyzer : $previousSelectedModel", e)
        }
    }

    private fun unBindCameraX() {
        cameraProvider?.let { provider ->
            // As required by CameraX API, unbinds all use cases before trying to re-bind any of them.
            provider.unbindAll()
            Log.v(tag, "Camera Unbounded")
        }
    }

    // Handles entity tracking results and updates the graphical overlay
    fun handleEntities(result: EntityTrackerAnalyzer.Result) {
        // Use lifecycleScope to launch a coroutine
        lifecycleScope.launch(Dispatchers.Main) {
            val rects = mutableListOf<Rect>()
            val decodedStrings = mutableListOf<String>()
            val entities =barcodeTracker?.getBarcodeDecoder()?.let { result.getValue(it) }
            binding.graphicOverlay.clear()

            if (entities != null) {
                for (entity in entities) {
                    if (entity is BarcodeEntity) {
                        val bEntity = entity
                        val rect = bEntity.boundingBox
                        if (rect != null) {
                            val overlayRect = mapBoundingBoxToOverlay(rect)
                            rects.add(overlayRect)
                            var hashCode = bEntity.hashCode().toString()

                            // Ensure the string has at least 4 characters
                            if (hashCode.length >= 4) {
                                // Get the last four digits
                                hashCode = hashCode.substring(hashCode.length - 4)
                            }
                            decodedStrings.add("$hashCode:${bEntity.value}")
                        }

                        Log.d(tag, "Detected entity - Value: ${bEntity.value}")
                    }
                }
            }


            binding.graphicOverlay.add(
                BarcodeTrackerGraphic(
                    binding.graphicOverlay,
                    rects,
                    decodedStrings
                )
            )
        }
    }

    // Handles entities for the entity view tracker and updates the graphical overlay
    override fun handleEntitiesForEntityView(result: EntityTrackerAnalyzer.Result) {
        Log.d(tag, "Handle View Entity - Result received")

        // Apply any pending resize specs now that the analyzer is ready
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
                entityViewGraphic!!.clear()
            } else {
                Log.w(tag, "EntityViewGraphic is null")
            }

            if (entities != null) {
                Log.d(tag, "Processing ${entities.size} entities for entity view")
                for (entity in entities) {
                    if (entity is BarcodeEntity) {
                        val bEntity = entity
                        val rect = bEntity.boundingBox
                        if (rect != null) {
                            Log.d(tag, "Adding entity to view - Value: ${bEntity.value}, BBox: $rect")
                            entityViewGraphic?.addEntity(bEntity)
                        } else {
                            Log.w(tag, "Entity has null bounding box - Value: ${bEntity.value}")
                        }
                    }
                }
                entityViewGraphic?.render()
                Log.d(tag, "Rendered entities on entity view")
            } else {
                Log.w(tag, "No entities to process for entity view")
            }
        }
    }

    // Handles product recognition results and updates the graphical overlay
    override fun onDetectionRecognitionResult(
        detections: Array<BBox>,
        products: Array<BBox>,
        recognitions: Array<Recognizer.Recognition>
    ) {

        lifecycleScope.launch(Dispatchers.Main) {
            binding.graphicOverlay.clear()
            val labelShelfRects = mutableListOf<Rect>()
            val labelPegRects = mutableListOf<Rect>()
            val shelfRects = mutableListOf<Rect>()
            val recognizedRects = mutableListOf<Rect>()
            val decodedStrings = mutableListOf<String>()


            val labelShelfObjects = detections.filter { it.cls == 2 }
            val labelPegObjects = detections.filter { it.cls == 3 }
            val shelfObjects = detections.filter { it.cls == 4 }

            for (bBox in labelShelfObjects) {
                val rect =
                    Rect(bBox.xmin.toInt(), bBox.ymin.toInt(), bBox.xmax.toInt(), bBox.ymax.toInt())
                val overlayRect = mapBoundingBoxToOverlay(rect)
                labelShelfRects.add(overlayRect)
            }

            for (bBox in labelPegObjects) {
                val rect =
                    Rect(bBox.xmin.toInt(), bBox.ymin.toInt(), bBox.xmax.toInt(), bBox.ymax.toInt())
                val overlayRect = mapBoundingBoxToOverlay(rect)
                labelPegRects.add(overlayRect)
            }

            for (bBox in shelfObjects) {
                val rect =
                    Rect(bBox.xmin.toInt(), bBox.ymin.toInt(), bBox.xmax.toInt(), bBox.ymax.toInt())
                val overlayRect = mapBoundingBoxToOverlay(rect)
                shelfRects.add(overlayRect)
            }

            if (recognitions.isEmpty()) {
                decodedStrings.add("No products found")
                recognizedRects.add(Rect(250, 250, 0, 0))
            } else {
                Log.v(
                    tag,
                    "products length :" + products.size + " recognitions length: " + recognitions.size
                )

                for (i in products.indices) {
                    if (recognitions[i].similarity[0] > SIMILARITY_THRESHOLD) {
                        val bBox = products[i]
                        val rect = Rect(
                            bBox.xmin.toInt(),
                            bBox.ymin.toInt(),
                            bBox.xmax.toInt(),
                            bBox.ymax.toInt()
                        )
                        val overlayRect = mapBoundingBoxToOverlay(rect)
                        recognizedRects.add(overlayRect)
                        decodedStrings.add(recognitions[i].sku[0])
                    }
                }
            }

            binding.graphicOverlay.add(
                ProductRecognitionGraphic(
                    binding.graphicOverlay,
                    labelShelfRects,
                    labelPegRects,
                    shelfRects,
                    recognizedRects,
                    decodedStrings
                )
            )
        }
    }
    private fun disposeModels() {
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
                    barcodeTracker?.stop()
                }

                PRODUCT_RECOGNITION -> {
                    Log.i(tag, "Disposing the recognition analyzer")
                    productRecognitionHandler?.stop()
                }

                else -> Log.e(tag, "Invalid selected option")
            }
        } catch (e: java.lang.Exception) {
            Log.e(tag, "Can not dispose the analyzer : $previousSelectedModel", e)
        }
    }
    private fun bindAllCameraUseCases() {
        if (cameraProvider != null) {
            // As required by CameraX API, unbinds all use cases before trying to re-bind any of them.
            cameraProvider!!.unbindAll()
            bindPreviewUseCase()
            bindAnalysisUseCase()
        }
    }
    private fun bindPreviewUseCase() {
        if (cameraProvider == null) {
            return
        }
        if (previewUseCase != null) {
            binding.graphicOverlay.clear()
            cameraProvider?.unbind(previewUseCase)
        }
        if (analysisUseCase != null) {
            cameraProvider?.unbind(analysisUseCase)
        }

        val builder = Preview.Builder()

        builder.setResolutionSelector(resolutionSelector)

        analysisUseCase = ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        previewUseCase = builder.build()
        binding.previewView.visibility = if (isEntityViewFinder) View.GONE else View.VISIBLE
        binding.entityView.visibility = if (isEntityViewFinder) View.VISIBLE else View.GONE

        if (isEntityViewFinder) {
            previewUseCase?.surfaceProvider = entityViewController?.surfaceProvider
        } else {
            previewUseCase?.surfaceProvider = binding.previewView.getSurfaceProvider()
        }
        camera = cameraProvider?.bindToLifecycle( /* lifecycleOwner= */this, cameraSelector, previewUseCase, analysisUseCase)

        if (isEntityViewFinder) {
            entityViewController?.setCameraController(camera)
        }
    }

    companion object {
        private const val BARCODE_DETECTION = "Barcode"
        private const val TEXT_OCR_DETECTION = "OCR"
        private const val ENTITY_ANALYZER = "Tracker"
        private const val PRODUCT_RECOGNITION = "Product Recognition"
        private const val ENTITY_VIEW_FINDER = "Entity Viewfinder"
    }
}
