// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.handlers

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.ImageView
import com.zebra.ai.vision.detector.BBox
import com.zebra.ai.vision.detector.BarcodeDecoder
import com.zebra.ai.vision.detector.Recognizer
import com.zebra.ai.vision.detector.Word
import com.zebra.ai.vision.entity.BarcodeEntity
import com.zebra.ai.vision.entity.Entity
import com.zebra.ai.vision.entity.LabelEntity
import com.zebra.ai.vision.entity.LocalizerEntity
import com.zebra.ai.vision.entity.ParagraphEntity
import com.zebra.ai.vision.entity.ProductEntity
import com.zebra.ai.vision.entity.ShelfEntity
import com.zebra.aisuite_quickstart.kotlin.CameraXLivePreviewActivity
import com.zebra.aisuite_quickstart.kotlin.analyzers.tracker.TrackerGraphic
import com.zebra.aisuite_quickstart.kotlin.camera.CameraManager
import com.zebra.aisuite_quickstart.kotlin.detectors.barcodedecodersample.BarcodeGraphic
import com.zebra.aisuite_quickstart.kotlin.detectors.productrecognition.ProductRecognitionGraphic
import com.zebra.aisuite_quickstart.kotlin.detectors.textocrsample.OCRGraphic
import com.zebra.aisuite_quickstart.kotlin.detectors.warehouselocalizer.WareHouseLocalizerGraphic
import com.zebra.aisuite_quickstart.kotlin.viewfinder.EntityViewGraphic
import com.zebra.aisuite_quickstart.utils.CommonUtils

class DetectionResultHandler(
    private val activity: CameraXLivePreviewActivity,
    private val boundingBoxMapper: BoundingBoxMapper,
    private val cameraManager: CameraManager
) {
    companion object {
        private const val TAG = "DetectionResultHandler"
        private const val SIMILARITY_THRESHOLD = 0.65f
    }

    private val capturedShelves = mutableListOf<ShelfEntity>()
    private val capturedShelfOverlayRects = mutableListOf<Rect>()
    private var capturedEntities = mutableListOf<Entity>()
    private val capturedShelfViewRects = mutableListOf<RectF>()
    private var imageToOverlayMatrix: Matrix? = null
    private val capturedProducts = mutableListOf<ProductEntity>()
    private val capturedProductViewRects = mutableListOf<RectF>()
    private val capturedLabels = mutableListOf<LabelEntity>()
    private val capturedLabelViewRects = mutableListOf<RectF>()

    fun handleBarcodeDetection(result: List<BarcodeEntity>?) {
        val rects = mutableListOf<Rect>()
        val decodedStrings = mutableListOf<String>()

        activity.runOnUiThread {
            activity.binding.graphicOverlay.clear()
            result?.forEach { bb ->
                val rect = bb.boundingBox
                rect?.let {
                    Log.d(TAG, "Original bbox: $rect")
                    val overlayRect = boundingBoxMapper.mapBoundingBoxToOverlay(rect)
                    Log.d(TAG, "Mapped bbox: $overlayRect")
                    rects.add(overlayRect)
                    decodedStrings.add(bb.value)
                    Log.e(TAG, "Detected entity - Value: ${bb.value}")
                    Log.e(TAG, "Detected entity - Symbology: ${bb.symbology}")
                }
            }
            activity.binding.graphicOverlay.add(
                BarcodeGraphic(activity.binding.graphicOverlay, rects, decodedStrings)
            )
        }
    }

    fun handleLegacyBarcodeDetection(barcodes: Array<BarcodeDecoder.Result>) {
        val rects = mutableListOf<Rect>()
        val decodedStrings = mutableListOf<String>()

        activity.runOnUiThread {
            activity.binding.graphicOverlay.clear()
            barcodes.forEach { barcode ->
                val rect = Rect(
                    barcode.bboxData.xmin.toInt(),
                    barcode.bboxData.ymin.toInt(),
                    barcode.bboxData.xmax.toInt(),
                    barcode.bboxData.ymax.toInt()
                )
                val overlayRect = boundingBoxMapper.mapBoundingBoxToOverlay(rect)
                rects.add(overlayRect)
                decodedStrings.add(barcode.value)
                Log.d(TAG, "Symbology Type: ${barcode.symbologytype}")
                Log.d(TAG, "Decoded barcode: ${barcode.value}")
            }
            activity.binding.graphicOverlay.add(
                BarcodeGraphic(activity.binding.graphicOverlay, rects, decodedStrings)
            )
        }
    }

    fun handleTextOCRDetection(list: List<ParagraphEntity>?) {
        val rects = mutableListOf<Rect>()
        val decodedStrings = mutableListOf<String>()

        activity.runOnUiThread {
            activity.binding.graphicOverlay.clear()
            if (list != null) {
                for (entity in list) {
                    val lines = entity.lines
                    for (line in lines) {
                        for (word in line.words) {
                            if (word.text.isNotEmpty()) {
                                val bbox = word.complexBBox
                                if (bbox != null && bbox.x.size >= 3 && bbox.y.size >= 3) {
                                    val rect = Rect(
                                        bbox.x[0].toInt(),
                                        bbox.y[0].toInt(),
                                        bbox.x[2].toInt(),
                                        bbox.y[2].toInt()
                                    )
                                    val overlayRect =
                                        boundingBoxMapper.mapBoundingBoxToOverlay(rect)
                                    rects.add(overlayRect)
                                    decodedStrings.add(word.text)
                                }
                            }
                        }
                    }
                }
                activity.binding.graphicOverlay.add(
                    OCRGraphic(activity.binding.graphicOverlay, rects, decodedStrings)
                )
            }
        }
    }

    // Legacy OCR detection result handler
    fun handleLegacyTextOCRDetection(words: Array<Word>) {
        val rects = mutableListOf<Rect>()
        val decodedStrings = mutableListOf<String>()

        activity.runOnUiThread {
            activity.binding.graphicOverlay.clear()
            for (word in words) {
                if (word.decodes.isNotEmpty()) {
                    val bbox = word.bbox
                    if (bbox != null && bbox.x != null && bbox.y != null &&
                        bbox.x.size >= 3 && bbox.y.size >= 3
                    ) {
                        val minX = bbox.x[0]
                        val maxX = bbox.x[2]
                        val minY = bbox.y[0]
                        val maxY = bbox.y[2]
                        val rect = Rect(minX.toInt(), minY.toInt(), maxX.toInt(), maxY.toInt())
                        val overlayRect = boundingBoxMapper.mapBoundingBoxToOverlay(rect)
                        rects.add(overlayRect)
                        decodedStrings.add(word.decodes[0].content)
                    }
                }
            }
            activity.binding.graphicOverlay.add(
                OCRGraphic(activity.binding.graphicOverlay, rects, decodedStrings)
            )
        }
    }

    fun handleLegacyDetectionRecognitionResult(
        detections: Array<BBox>?,
        products: Array<BBox>,
        recognitions: Array<Recognizer.Recognition>
    ) {
        activity.runOnUiThread {
            activity.binding.graphicOverlay.clear()
            detections?.let {
                val labelShelfRects = mutableListOf<Rect>()
                val labelPegRects = mutableListOf<Rect>()
                val shelfRects = mutableListOf<Rect>()
                val recognizedRects = mutableListOf<Rect>()
                val decodedStrings = mutableListOf<String>()
                val labelShelfObjects = detections.filter { it.cls == 2 }
                val labelPegObjects = detections.filter { it.cls == 3 }
                val shelfObjects = detections.filter { it.cls == 4 }
                val barcodeRects = mutableListOf<Rect>()
                val barcodeTexts = mutableListOf<String>()
                for (bBox in labelShelfObjects) {
                    val rect = Rect(
                        bBox.xmin.toInt(),
                        bBox.ymin.toInt(),
                        bBox.xmax.toInt(),
                        bBox.ymax.toInt()
                    )
                    val overlayRect = boundingBoxMapper.mapBoundingBoxToOverlay(rect)
                    labelShelfRects.add(overlayRect)
                }
                for (bBox in labelPegObjects) {
                    val rect = Rect(
                        bBox.xmin.toInt(),
                        bBox.ymin.toInt(),
                        bBox.xmax.toInt(),
                        bBox.ymax.toInt()
                    )
                    val overlayRect = boundingBoxMapper.mapBoundingBoxToOverlay(rect)
                    labelPegRects.add(overlayRect)
                }

                for (bBox in shelfObjects) {
                    val rect = Rect(
                        bBox.xmin.toInt(),
                        bBox.ymin.toInt(),
                        bBox.xmax.toInt(),
                        bBox.ymax.toInt()
                    )
                    val overlayRect = boundingBoxMapper.mapBoundingBoxToOverlay(rect)
                    shelfRects.add(overlayRect)
                }
                // Draw all products
                if (recognitions.isEmpty()) {
                    decodedStrings.add("No products found")
                    recognizedRects.add(Rect(250, 250, 0, 0))
                } else {
                    Log.v(
                        TAG,
                        "products length: ${products.size} recognitions length: ${recognitions.size}"
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
                            val overlayRect = boundingBoxMapper.mapBoundingBoxToOverlay(rect)
                            recognizedRects.add(overlayRect)
                            decodedStrings.add(recognitions[i].sku[0])
                        }
                    }
                }

                activity.binding.graphicOverlay.add(
                    ProductRecognitionGraphic(
                        activity.binding.graphicOverlay,
                        labelShelfRects,
                        labelPegRects,
                        shelfRects,
                        recognizedRects,
                        decodedStrings,
                        barcodeRects,
                        barcodeTexts
                    )
                )
            }
        }
    }

    // Product recognition detection result handler
    fun handleDetectionRecognitionResult(result: List<Entity>?) {
        Log.d(TAG, "Inside On Shelf RecognitionResult (flat hierarchy)")
        activity.runOnUiThread {
            activity.binding.graphicOverlay.clear()
            if (result == null) return@runOnUiThread

            val shelves = mutableListOf<com.zebra.ai.vision.entity.ShelfEntity>()
            val labels = mutableListOf<com.zebra.ai.vision.entity.LabelEntity>()
            val products = mutableListOf<com.zebra.ai.vision.entity.ProductEntity>()

            for (entity in result) {
                when (entity) {
                    is com.zebra.ai.vision.entity.ShelfEntity -> shelves.add(entity)
                    is com.zebra.ai.vision.entity.LabelEntity -> labels.add(entity)
                    is com.zebra.ai.vision.entity.ProductEntity -> products.add(entity)
                }
            }

            val shelfRects = mutableListOf<Rect>()
            val labelShelfRects = mutableListOf<Rect>()
            val labelPegRects = mutableListOf<Rect>()
            val productRects = mutableListOf<Rect>()
            val productLabels = mutableListOf<String>()
            val barcodeRects = mutableListOf<Rect>()
            val barcodeTexts = mutableListOf<String>()

            // Draw shelves and their labels
            for (shelf in shelves) {
                val shelfRect = boundingBoxMapper.mapBoundingBoxToOverlay(shelf.boundingBox)
                shelfRects.add(shelfRect)
            }


            // Draw all labels (if you want to show all, not just those attached to shelves)
            for (label in labels) {
                if (label.classId == LabelEntity.ClassId.PEG_LABEL) {
                    val labelRect: Rect =
                        boundingBoxMapper.mapBoundingBoxToOverlay(label.boundingBox)
                    labelPegRects.add(labelRect)
                }
                if (label.classId == LabelEntity.ClassId.SHELF_LABEL) {
                    val labelRect: Rect =
                        boundingBoxMapper.mapBoundingBoxToOverlay(label.boundingBox)
                    labelShelfRects.add(labelRect)
                }
                val barcodes = label.getBarcodes()
                Log.d(TAG, "Barcodes size: " + barcodes.size)
                if (!barcodes.isEmpty()) {
                    for (barcode in barcodes) {
                        val barcodeRect = barcode.getBoundingBox()
                        Log.d(
                            TAG,
                            "Detected entity - Value: " + barcode.getValue()
                        )
                        Log.d(
                            TAG,
                            "Detected entity - Symbology: " + barcode.getSymbology()
                        )
                        val barRect = boundingBoxMapper.mapBoundingBoxToOverlay(barcodeRect)
                        barcodeRects.add(barRect)
                        barcodeTexts.add(barcode.getValue())
                    }
                }
            }

            // Draw all products
            for (product in products) {
                val prodRect = boundingBoxMapper.mapBoundingBoxToOverlay(product.boundingBox)
                productRects.add(prodRect)
                val topSku = if (product.accuracy >= SIMILARITY_THRESHOLD) {
                    product.topKSKUs?.firstOrNull()?.let { skuInfo ->
                        "${skuInfo.productSKU}"
                    } ?: ""
                } else ""
                productLabels.add(topSku)
                Log.d(TAG, "SKU=$topSku, Product bbox=$prodRect")
            }

            activity.binding.graphicOverlay.add(
                ProductRecognitionGraphic(
                    activity.binding.graphicOverlay,
                    labelShelfRects,
                    labelPegRects,
                    shelfRects,
                    productRects,
                    productLabels,
                    barcodeRects,
                    barcodeTexts
                )
            )
        }
    }

    fun handleEntityTrackerDetection(
        barcodeEntities: List<Entity>?,
        ocrEntities: List<Entity>?,
        moduleEntities: List<Entity>?
    ) {
        val barcodeRects = mutableListOf<Rect>()
        val barcodeStrings = mutableListOf<String>()
        val ocrRects = mutableListOf<Rect>()
        val ocrStrings = mutableListOf<String>()

        val shelves = mutableListOf<com.zebra.ai.vision.entity.ShelfEntity>()
        val labels = mutableListOf<com.zebra.ai.vision.entity.LabelEntity>()
        val products = mutableListOf<com.zebra.ai.vision.entity.ProductEntity>()
        val shelfRects = mutableListOf<Rect>()
        val labelShelfRects = mutableListOf<Rect>()
        val labelPegRects = mutableListOf<Rect>()
        val productRects = mutableListOf<Rect>()
        val productLabels = mutableListOf<String>()



        activity.runOnUiThread {
            activity.binding.graphicOverlay.clear()
            barcodeEntities?.forEach { entity ->
                if (entity is BarcodeEntity) {
                    val rect = entity.boundingBox
                    rect?.let {
                            val overlayRect = boundingBoxMapper.mapBoundingBoxToOverlay(rect)
                            barcodeRects.add(overlayRect)
                            val hashCode = entity.hashCode().toString().takeLast(4)
                            if (entity.value.trim().isNotEmpty()) {
                                barcodeStrings.add("$hashCode:${entity.value}")
                            }
                            else{
                                barcodeStrings.add("")
                            }
                            Log.d(
                                TAG,
                                "Tracker UUID: $hashCode Detected entity - Value: ${entity.value}"
                            )

                    }
                }
            }
            ocrEntities?.forEach { entity ->
                if (entity is ParagraphEntity) {
                    val lines = entity.lines
                    for (line in lines) {
                        for (word in line.words) {
                            val bbox = word.complexBBox

                            if (bbox != null && bbox.x != null && bbox.y != null && bbox.x.size >= 3 && bbox.y.size >= 3) {
                                val minX = bbox.x[0]
                                val maxX = bbox.x[2]
                                val minY = bbox.y[0]
                                val maxY = bbox.y[2]

                                val rect =
                                    Rect(minX.toInt(), minY.toInt(), maxX.toInt(), maxY.toInt())
                                val overlayRect = boundingBoxMapper.mapBoundingBoxToOverlay(rect)
                                val decodedValue = word.text
                                ocrRects.add(overlayRect)
                                ocrStrings.add(decodedValue)
                            }
                        }
                    }
                    Log.d(TAG, "Detected OCR entity - Text: ${entity.text}")
                }

            }

            moduleEntities?.forEach { entity ->
                when (entity) {
                    is com.zebra.ai.vision.entity.ShelfEntity -> shelves.add(entity)
                    is LabelEntity -> labels.add(entity)
                    is com.zebra.ai.vision.entity.ProductEntity -> products.add(entity)
                }
            }

            // Draw shelves and their labels
            for (shelf in shelves) {
                val shelfRect = boundingBoxMapper.mapBoundingBoxToOverlay(shelf.boundingBox)
                shelfRects.add(shelfRect)
            }
            // Draw all labels (if you want to show all, not just those attached to shelves)
            for (label in labels) {
                if (label.classId == LabelEntity.ClassId.PEG_LABEL) {
                    val labelRect: Rect =
                        boundingBoxMapper.mapBoundingBoxToOverlay(label.boundingBox)
                    labelPegRects.add(labelRect)
                }
                if (label.classId == LabelEntity.ClassId.SHELF_LABEL) {
                    val labelRect: Rect =
                        boundingBoxMapper.mapBoundingBoxToOverlay(label.boundingBox)
                    labelShelfRects.add(labelRect)
                }
            }
            // Draw all products
            for (product in products) {
                val prodRect = boundingBoxMapper.mapBoundingBoxToOverlay(product.boundingBox)
                productRects.add(prodRect)
                val topSku = if (product.accuracy >= SIMILARITY_THRESHOLD) {
                    product.topKSKUs?.firstOrNull()?.let { skuInfo ->
                        "${skuInfo.productSKU}"
                    } ?: ""
                } else ""
                productLabels.add(topSku)
                Log.d(TAG, "SKU=$topSku, Product bbox=$prodRect")
            }

            // Add graphics for barcodes
            if (barcodeRects.isNotEmpty()) {
                activity.binding.graphicOverlay.add(
                    TrackerGraphic(activity.binding.graphicOverlay, barcodeRects, barcodeStrings)
                )
            }

            // Add graphics for OCR
            if (ocrRects.isNotEmpty()) {
                activity.binding.graphicOverlay.add(
                    OCRGraphic(
                        activity.binding.graphicOverlay,
                        ocrRects,
                        ocrStrings
                    )
                )
            }
            if (shelfRects.isNotEmpty() || labelShelfRects.isNotEmpty() || labelPegRects.isNotEmpty() || productRects.isNotEmpty()) {
                activity.binding.graphicOverlay.add(
                    ProductRecognitionGraphic(
                        activity.binding.graphicOverlay,
                        labelShelfRects,
                        labelPegRects,
                        shelfRects,
                        productRects,
                        productLabels,
                        null,
                        null
                    )
                )
            }
        }
    }

    fun handleEntityViewFinderDetection(
        entities: List<Entity>?,
        entityViewGraphic: EntityViewGraphic
    ) {
        activity.runOnUiThread {
            entityViewGraphic.clear()
            entities?.forEach { entity ->
                if (entity is BarcodeEntity) {
                    val rect = entity.boundingBox
                    rect?.let {
                        Log.d(TAG, "Adding entity to view - Value: ${entity.value}, BBox: $rect")
                        entityViewGraphic.addEntity(entity)
                    } ?: Log.w(TAG, "Entity has null bounding box - Value: ${entity.value}")
                }
            }
            entityViewGraphic.render()
        }
    }

    fun handleWareHouseLocalizerDetectionResult(result: List<LocalizerEntity>) {
        val rects = mutableListOf<Rect>()

        activity.runOnUiThread {
            activity.binding.graphicOverlay.clear()
            result.forEach { bb ->
                val rect = bb.boundingBox
                rect?.let {
                    Log.d(TAG, "Original bbox: $rect")
                    val overlayRect = boundingBoxMapper.mapBoundingBoxToOverlay(rect)
                    Log.d(TAG, "Mapped bbox: $overlayRect")
                    rects.add(overlayRect)
                }
            }
            activity.binding.graphicOverlay.add(
                WareHouseLocalizerGraphic(
                    activity.binding.graphicOverlay,
                    rects
                )
            )
        }
    }

    fun handleImageCaptureBarcodeResult(entities: List<BarcodeEntity>?) {
        if (activity.capturedBitmap == null || entities == null) {
            Log.w(TAG, "Cannot overlay: bitmap or entities null")
            return
        }

        val annotated = activity.capturedBitmap!!.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(annotated)

        // Extract barcodes and bounding boxes
        val rects = mutableListOf<Rect>()
        val decodedStrings = mutableListOf<String>()

        for (entity in entities) {
            val rect = entity.boundingBox
            if (rect != null) {
                rects.add(rect)
                decodedStrings.add(entity.value)
            }
        }

        // Draw barcodes using BarcodeGraphic style (matching preview)
        val rectPaint = Paint().apply {
            color = Color.GREEN
            style = Paint.Style.STROKE
            strokeWidth = 16f
        }

        val textPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 60f
            alpha = 255
        }

        val backgroundPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            strokeWidth = 6f
        }

        val contentPadding = 40

        for (i in rects.indices) {
            val rect = rects[i]
            val text = decodedStrings[i]

            // Draw bounding box
            canvas.drawRect(rect, rectPaint)

            // Draw text with white background (matching preview style)
            if (text.isNotEmpty()) {
                val textWidth = textPaint.measureText(text).toInt()

                // Calculate background rectangle position and size
                val contentRect = Rect(
                    rect.left,
                    rect.bottom + contentPadding / 2,
                    rect.left + textWidth + contentPadding * 2,
                    rect.bottom + textPaint.textSize.toInt() + contentPadding
                )

                // Draw white background rectangle
                canvas.drawRect(contentRect, backgroundPaint)

                // Draw dark gray text
                canvas.drawText(
                    text,
                    (rect.left + contentPadding).toFloat(),
                    (rect.bottom + contentPadding * 2).toFloat(),
                    textPaint
                )
            }
        }

        activity.runOnUiThread {
            activity.binding.capturedImageView.setImageBitmap(annotated)
            Log.d(TAG, "Overlayed ${rects.size} barcode detections")
        }
    }

    fun handleImageCaptureTextResult(entities: List<ParagraphEntity>?) {
        if (activity.capturedBitmap == null || entities == null) {
            Log.w(TAG, "Cannot overlay: frozenFrame=${cameraManager.imageCapture}")
            return
        }

        val annotated = activity.capturedBitmap!!.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(annotated)
        Log.d(TAG, "Drawing detections on canvas")

        // Extract text and bounding boxes
        val rects = mutableListOf<Rect>()
        val decodedStrings = mutableListOf<String>()

        for (entity in entities) {
            val lines = entity.lines
            for (line in lines) {
                for (word in line.words) {
                    val bbox = word.complexBBox
                    if (word.text.isNotEmpty()) {
                        if (bbox != null && bbox.x != null && bbox.y != null &&
                            bbox.x.size >= 3 && bbox.y.size >= 3
                        ) {
                            val minX = bbox.x[0]
                            val maxX = bbox.x[2]
                            val minY = bbox.y[0]
                            val maxY = bbox.y[2]
                            val rect = Rect(minX.toInt(), minY.toInt(), maxX.toInt(), maxY.toInt())
                            rects.add(rect)
                            decodedStrings.add(word.text)
                        }
                    }
                }
            }
        }

        // Draw rectangles and text on canvas
        val rectPaint = Paint().apply {
            color = Color.GREEN
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 60f
            alpha = 255
        }

        for (i in rects.indices) {
            val rect = rects[i]
            val text = decodedStrings[i]

            // Draw bounding box
            canvas.drawRect(rect, rectPaint)

            // Draw text above the box
            CommonUtils.getTextSizeWithinBounds(
                canvas, text,
                rect.left.toFloat(), rect.top.toFloat(),
                rect.right.toFloat(), rect.bottom.toFloat(), textPaint
            )
        }

        activity.runOnUiThread {
            activity.binding.capturedImageView.setImageBitmap(annotated)
            Log.d(TAG, "Overlayed ${rects.size} detections")
        }
    }

    fun handleImageCaptureRecognitionResult(entities: List<Entity>?) {
        val currentCapture = activity.capturedBitmap
        if (currentCapture == null || entities == null) {
            Log.w(TAG, "Cannot overlay: frozenFrame=$currentCapture")
            return
        }

        val annotated = currentCapture.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(annotated)

        val shelfPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.YELLOW
            style = Paint.Style.STROKE
            strokeWidth = 8f
            alpha = 255
        }

        val labelShelfPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLUE
            style = Paint.Style.STROKE
            strokeWidth = 6f
            alpha = 255
        }

        val labelPegPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.MAGENTA
            style = Paint.Style.STROKE
            strokeWidth = 6f
            alpha = 255
        }

        val productPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 255, 0)
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }

        val barcodePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        barcodePaint.setColor(Color.RED)
        barcodePaint.setStyle(Paint.Style.STROKE)
        barcodePaint.setStrokeWidth(6f)
        barcodePaint.setAlpha(255)


        val shelves = mutableListOf<ShelfEntity>()
        val labels = mutableListOf<LabelEntity>()
        val products = mutableListOf<ProductEntity>()


        for (entity in entities) {
            when (entity) {
                is ShelfEntity -> shelves.add(entity)
                is LabelEntity -> labels.add(entity)
                is ProductEntity -> products.add(entity)
            }
        }

        // Draw shelves first
        for (shelf in shelves) {
            val shelfRect = boundingBoxMapper.mapBoundingBoxToOverlay(shelf.boundingBox)
            canvas.drawRect(shelfRect, shelfPaint)
        }

        // Draw all labels (if you want to show all, not just those attached to shelves)
        for (label in labels) {
            if (label.classId == LabelEntity.ClassId.PEG_LABEL) {
                val labelRect: Rect = boundingBoxMapper.mapBoundingBoxToOverlay(label.boundingBox)
                canvas.drawRect(labelRect, labelPegPaint)
            }
            if (label.classId == LabelEntity.ClassId.SHELF_LABEL) {
                val labelRect: Rect =
                    boundingBoxMapper.mapBoundingBoxToOverlay(label.boundingBox)
                canvas.drawRect(labelRect, labelShelfPaint)
            }
            val barcodes = label.getBarcodes()
            if (barcodes != null) {
                for (barcode in barcodes) {
                    val barcodeBBox = barcode.getBoundingBox()
                    if (barcodeBBox != null) {
                        val barcodeRect = boundingBoxMapper.mapBoundingBoxToOverlay(barcodeBBox)
                        canvas.drawRect(barcodeRect, barcodePaint)
                    }
                }
            }
        }

        // Draw products and SKU text
        for (product in products) {
            val prodRect = boundingBoxMapper.mapBoundingBoxToOverlay(product.boundingBox)
            canvas.drawRect(prodRect, productPaint)

        }

        activity.runOnUiThread {
            activity.binding.capturedImageView.setImageBitmap(annotated)
            Log.d(TAG, "Overlayed recognition detections on captured bitmap")
        }
    }

    fun overlayShelfAssociations(shelf: ShelfEntity) {
        val currentCapture = activity.capturedBitmap
        if (currentCapture == null) {
            Log.w(TAG, "overlayShelfAssociations: no captured bitmap")
            return
        }
        val annotated = currentCapture.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(annotated)

        val shelfPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 8f
            color = Color.YELLOW
        }

        val labelShelfPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = Color.CYAN
        }

        val labelPegPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = Color.MAGENTA
        }

        val productPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 8f
            color = Color.GREEN
        }

        val barcodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            strokeWidth = 6f
            color = Color.BLUE
        }

        val barTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 60f
            style = Paint.Style.FILL_AND_STROKE
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 42f
            style = Paint.Style.FILL
        }

        // Draw shelf bbox
        shelf.boundingBox?.let {
            canvas.drawRect(it, shelfPaint)
        }

        val labels = shelf.labels
        if (labels != null) {
            for (l in labels) {
                if (l.boundingBox == null) continue
                if (l.classId == LabelEntity.ClassId.SHELF_LABEL) {
                    canvas.drawRect(l.boundingBox, labelShelfPaint)
                } else if (l.classId == LabelEntity.ClassId.PEG_LABEL) {
                    canvas.drawRect(l.boundingBox, labelPegPaint)
                }

                // Draw barcodes for label
                val entityRect = l.boundingBox
                val barcodes = l.barcodes
                if (barcodes != null) {
                    for (barcode in barcodes) {
                        val barcodeRect = barcode.boundingBox

                        val barcodeText = barcode.value
                        val barTextBounds = Rect()
                        barTextPaint.getTextBounds(
                            barcodeText,
                            0,
                            barcodeText.length,
                            barTextBounds
                        )
                        val barTextLeft = barcodeRect.left
                        val barTextBottom = entityRect.bottom + 30
                        val barTextRight = barTextLeft + barTextBounds.width()
                        val barTextTop = barTextBottom - barTextBounds.height()

                        val barTextBgRect =
                            Rect(barTextLeft, barTextTop, barTextRight, barTextBottom)
                        canvas.drawRect(barTextBgRect, barcodePaint)
                        canvas.drawText(
                            barcodeText,
                            barTextLeft.toFloat(),
                            barTextBottom.toFloat(),
                            barTextPaint
                        )
                    }
                }
            }
        }

        val products = shelf.products
        if (products != null) {
            for (p in products) {
                if (p.boundingBox == null) continue
                canvas.drawRect(p.boundingBox, productPaint)
                var sku = ""
                val topK = p.topKSKUs
                if (topK != null && topK.isNotEmpty() && topK[0] != null) {
                    sku = topK[0].productSKU.toString()
                }
                if (sku.isNotEmpty()) {
                    val r = p.boundingBox
                    canvas.drawText(
                        sku,
                        (r.left + 8).toFloat(),
                        maxOf(0, r.top - 10).toFloat(),
                        textPaint
                    )
                }
            }
        }

        activity.runOnUiThread {
            activity.binding.capturedImageView.setImageBitmap(annotated)
            Log.d(TAG, "Overlayed tapped shelf associations on captured bitmap")
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun enableShelfTapOnCapturedBitmap(
        result: List<Entity>?,
        capturedBitmap: Bitmap?,
        imageView: ImageView?
    ) {
        Log.d(TAG, "Enable shelf-tap on captured bitmap")
        activity.runOnUiThread {
            if (result == null || result.isEmpty() || capturedBitmap == null || imageView == null) {
                Log.w(TAG, "Missing result/bitmap/imageView")
                return@runOnUiThread
            }

            // Keep entities and collect shelves
            capturedEntities = ArrayList(result)
            capturedShelves.clear()
            capturedProducts.clear()
            capturedLabels.clear()

            for (e in capturedEntities) {
                when (e) {
                    is ShelfEntity -> capturedShelves.add(e)
                    is ProductEntity -> capturedProducts.add(e)
                    is LabelEntity -> capturedLabels.add(e)
                }
            }
            capturedShelves.sortBy { it.boundingBox.top }
            Log.d(TAG, "Shelves collected: ${capturedShelves.size}")
            Log.d(TAG, "Products collected: ${capturedProducts.size}")
            Log.d(TAG, "Labels collected: ${capturedLabels.size}")

            // Precompute ImageView-space rects for hit-testing on the bitmap view
            capturedShelfViewRects.clear()
            for (i in capturedShelves.indices) {
                val shelf = capturedShelves[i]
                val shelfImgRect = shelf.boundingBox
                val vf = RectF(shelfImgRect)
                val im = Matrix(imageView.imageMatrix)
                im.mapRect(vf)
                capturedShelfViewRects.add(vf)
                Log.d(
                    TAG, "Shelf[$i] imageRect=${shelfImgRect.toShortString()}" +
                            " | viewRect=(${vf.left},${vf.top},${vf.right},${vf.bottom})"
                )
            }
            capturedProductViewRects.clear()
            for (product in capturedProducts) {
                val productImgRect = product.boundingBox
                val vf = RectF(productImgRect)
                val im = Matrix(imageView.imageMatrix)
                im.mapRect(vf)
                capturedProductViewRects.add(vf)
            }
            capturedLabelViewRects.clear()
            for (label in capturedLabels) {
                val labelImgRect = label.boundingBox
                val vf = RectF(labelImgRect)
                val im = Matrix(imageView.imageMatrix)
                im.mapRect(vf)
                capturedLabelViewRects.add(vf)
            }

            // Initial full render
            handleDetectionRecognitionResult(result)
            Log.d(TAG, "Initial full render done for captured bitmap")

            // Tap listener on the bitmap view (ImageView)
            imageView.isClickable = true
            val touchSlop = ViewConfiguration.get(imageView.context).scaledTouchSlop
            val tapTimeout = ViewConfiguration.getTapTimeout().toLong()

            imageView.setOnTouchListener(object : View.OnTouchListener {
                var downX = 0f
                var downY = 0f
                var downTime = 0L
                var movedOutside = false

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            downX = event.x
                            downY = event.y
                            downTime = System.currentTimeMillis()
                            movedOutside = false
                            Log.d(TAG, "ACTION_DOWN @ ($downX,$downY)")
                            v.isPressed = true
                            return true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            val dx = event.x - downX
                            val dy = event.y - downY
                            if (Math.hypot(dx.toDouble(), dy.toDouble()) > touchSlop) movedOutside =
                                true
                            return true
                        }

                        MotionEvent.ACTION_UP -> {
                            v.isPressed = false
                            val duration = System.currentTimeMillis() - downTime
                            val isTap = !movedOutside && duration <= tapTimeout
                            val upX = event.x
                            val upY = event.y
                            Log.d(
                                TAG,
                                "ACTION_UP @ ($upX,$upY), isTap=$isTap, movedOutside=$movedOutside, duration=${duration}ms"
                            )

                            if (isTap) {
                                Log.d(TAG, "Tap detected, checking shelves...")
                                for (i in capturedShelfViewRects.indices) {
                                    val viewRect = capturedShelfViewRects[i]
                                    if (viewRect.contains(upX, upY)) {
                                        val tappedShelf = capturedShelves[i]
                                        val imgRect = tappedShelf.boundingBox
                                        Log.d(
                                            TAG,
                                            "Tap is on shelf[$i] imageRect=${imgRect.toShortString()}" +
                                                    " | viewRect=(${viewRect.left},${viewRect.top},${viewRect.right},${viewRect.bottom})"
                                        )
                                        overlayShelfAssociations(tappedShelf)
                                        v.performClick()
                                        return true
                                    }
                                }
                                Log.d(TAG, "Tap detected but not on any shelf")
                                handleDetectionRecognitionResult(result)
                                activity.binding.graphicOverlay.clear()
                                v.performClick()
                                return true
                            }
                            return true
                        }

                        MotionEvent.ACTION_CANCEL -> {
                            v.isPressed = false
                            Log.d(TAG, "ACTION_CANCEL")
                            return true
                        }
                    }
                    return false
                }
            })

            // Ensure overlay stays above
            activity.binding.graphicOverlay.bringToFront()
        }
    }

    /**
     * Unified handler for capture mode that processes barcode, OCR, and module recognition results
     * and draws them directly on a canvas (for captured image)
     */
    fun handleCaptureEntityTrackerDetection(
        barcodeEntities: List<Entity>?,
        ocrEntities: List<Entity>?,
        moduleEntities: List<Entity>?
    ) {
        if (activity.capturedBitmap == null) {
            Log.w(TAG, "Cannot overlay: bitmap or entities null")
            return
        }

        val annotated = activity.capturedBitmap!!.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(annotated)

        // Paint configurations for barcodes
        val barcodePaint = Paint().apply {
            color = Color.GREEN
            style = Paint.Style.STROKE
            strokeWidth = 12f
            isAntiAlias = true
        }

        val barcodeTextPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 60f
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val barcodeBackgroundPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        // Paint configurations for OCR
        val ocrPaint = Paint().apply {
            color = Color.YELLOW
            style = Paint.Style.STROKE
            strokeWidth = 6f
            isAntiAlias = true
        }

        val ocrTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 60f
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val ocrBackgroundPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        // Paint configurations for module recognition
        val shelfPaint = Paint().apply {
            color = Color.BLUE
            style = Paint.Style.STROKE
            strokeWidth = 6f
            isAntiAlias = true
        }

        val labelShelfPaint = Paint().apply {
            color = Color.CYAN
            style = Paint.Style.STROKE
            strokeWidth = 6f
            isAntiAlias = true
        }

        val labelPegPaint = Paint().apply {
            color = Color.MAGENTA
            style = Paint.Style.STROKE
            strokeWidth = 6f
            isAntiAlias = true
        }

        val productPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 6f
            isAntiAlias = true
        }

        val productTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 48f
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val productBackgroundPaint = Paint().apply {
            color = Color.argb(180, 0, 0, 0)
            style = Paint.Style.FILL
        }

        // Process barcode entities from capture
        if (barcodeEntities != null) {
            for (entity in barcodeEntities) {
                if (entity is BarcodeEntity) {
                    val rect = entity.boundingBox

                    if (rect != null) {
                        Log.d(TAG, "Capture - Drawing barcode: ${entity.value}, bbox: $rect")
                        val overlayRect = boundingBoxMapper.mapBoundingBoxToOverlay(rect)

                        // Draw bounding box
                        canvas.drawRect(
                            overlayRect.left.toFloat(),
                            overlayRect.top.toFloat(),
                            overlayRect.right.toFloat(),
                            overlayRect.bottom.toFloat(),
                            barcodePaint
                        )

                        // Draw text with background
                        val text = entity.value
                        val textBounds = Rect()
                        barcodeTextPaint.getTextBounds(text, 0, text.length, textBounds)

                        val textX = overlayRect.left.toFloat()
                        val textY = overlayRect.top - 10f

                        // Draw background rectangle for text
                        canvas.drawRect(
                            textX,
                            textY - textBounds.height() - 5f,
                            textX + textBounds.width() + 10f,
                            textY + 5f,
                            barcodeBackgroundPaint
                        )

                        // Draw text
                        canvas.drawText(text, textX + 5f, textY, barcodeTextPaint)
                    }
                }
            }
        }

        // Process OCR entities from capture
        if (ocrEntities != null) {
            val rects = mutableListOf<Rect>()
            val decodedStrings = mutableListOf<String>()
            for (entity in ocrEntities) {
                if (entity is ParagraphEntity) {
                    val lines = entity.lines

                    for (line in lines) {
                        val words = line.words

                        for (word in words) {
                            if (word.text.isNotEmpty()) {
                                val bbox = word.complexBBox

                                if (bbox != null && bbox.x != null && bbox.y != null &&
                                    bbox.x.size >= 3 && bbox.y.size >= 3
                                ) {
                                    val minX = bbox.x[0]
                                    val maxX = bbox.x[2]
                                    val minY = bbox.y[0]
                                    val maxY = bbox.y[2]

                                    val rect =
                                        Rect(minX.toInt(), minY.toInt(), maxX.toInt(), maxY.toInt())
                                    Log.d(TAG, "Capture - Drawing OCR: ${word.text}, bbox: $rect")
                                    val overlayRect =
                                        boundingBoxMapper.mapBoundingBoxToOverlay(rect)
                                    rects.add(overlayRect)
                                    decodedStrings.add(word.text)
                                }
                            }
                        }
                    }
                }
            }
            for (i in rects.indices) {
                val rect = rects[i]
                val text = decodedStrings[i]

                // Draw bounding box
                canvas.drawRect(rect, ocrPaint)

                // Draw text above the box
                CommonUtils.getTextSizeWithinBounds(
                    canvas, text,
                    rect.left.toFloat(), rect.top.toFloat(),
                    rect.right.toFloat(), rect.bottom.toFloat(), ocrTextPaint
                )
            }
        }

        // Process module recognition entities from capture
        val shelves = mutableListOf<ShelfEntity>()
        val labels = mutableListOf<LabelEntity>()
        val products = mutableListOf<ProductEntity>()

        if (moduleEntities != null) {
            for (entity in moduleEntities) {
                when (entity) {
                    is ShelfEntity -> shelves.add(entity)
                    is LabelEntity -> labels.add(entity)
                    is ProductEntity -> products.add(entity)
                }
            }
        }

        // Draw shelves
        for (shelf in shelves) {
            Log.d(TAG, "Capture - Drawing shelf, bbox: ${shelf.boundingBox}")
            val shelfRect = boundingBoxMapper.mapBoundingBoxToOverlay(shelf.boundingBox)
            canvas.drawRect(
                shelfRect.left.toFloat(),
                shelfRect.top.toFloat(),
                shelfRect.right.toFloat(),
                shelfRect.bottom.toFloat(),
                shelfPaint
            )
        }

        // Draw labels
        for (label in labels) {
            if (label.classId == LabelEntity.ClassId.PEG_LABEL) {
                Log.d(TAG, "Capture - Drawing peg label, bbox: ${label.boundingBox}")
                val labelRect = boundingBoxMapper.mapBoundingBoxToOverlay(label.boundingBox)
                canvas.drawRect(
                    labelRect.left.toFloat(),
                    labelRect.top.toFloat(),
                    labelRect.right.toFloat(),
                    labelRect.bottom.toFloat(),
                    labelPegPaint
                )
            }
            if (label.classId == LabelEntity.ClassId.SHELF_LABEL) {
                Log.d(TAG, "Capture - Drawing shelf label, bbox: ${label.boundingBox}")
                val labelRect = boundingBoxMapper.mapBoundingBoxToOverlay(label.boundingBox)
                canvas.drawRect(
                    labelRect.left.toFloat(),
                    labelRect.top.toFloat(),
                    labelRect.right.toFloat(),
                    labelRect.bottom.toFloat(),
                    labelShelfPaint
                )
            }
        }

        // Draw products
        for (product in products) {
            Log.d(TAG, "Capture - Drawing product, bbox: ${product.boundingBox}")
            val prodRect = boundingBoxMapper.mapBoundingBoxToOverlay(product.boundingBox)

            // Draw bounding box
            canvas.drawRect(
                prodRect.left.toFloat(),
                prodRect.top.toFloat(),
                prodRect.right.toFloat(),
                prodRect.bottom.toFloat(),
                productPaint
            )

            // Draw SKU text with background
            var topSku = ""
            if (product.topKSKUs != null && product.topKSKUs.isNotEmpty()) {
                val skuInfo = product.topKSKUs[0]
                topSku = "${skuInfo.productSKU}"
            }

            if (topSku.isNotEmpty()) {
                val textBounds = Rect()
                productTextPaint.getTextBounds(topSku, 0, topSku.length, textBounds)

                val textX = prodRect.left.toFloat()
                val textY = prodRect.top - 10f

                // Draw background rectangle for text
                canvas.drawRect(
                    textX,
                    textY - textBounds.height() - 5f,
                    textX + textBounds.width() + 10f,
                    textY + 5f,
                    productBackgroundPaint
                )

                // Draw text
                canvas.drawText(topSku, textX + 5f, textY, productTextPaint)
            }
        }

        Log.d(TAG, "Capture - Entity tracker detection completed and drawn on canvas")
        Log.d(
            TAG, "Capture - Statistics: barcodes=${barcodeEntities?.size ?: 0}" +
                    ", OCR entities=${ocrEntities?.size ?: 0}" +
                    ", shelves=${shelves.size}, labels=${labels.size}, products=${products.size}"
        )
        activity.runOnUiThread {
            activity.binding.capturedImageView.setImageBitmap(annotated)
        }
    }

    fun handleImageCaptureWareHouseResult(entities: List<LocalizerEntity>?) {
        if (activity.capturedBitmap == null || entities == null) {
            Log.w(TAG, "Cannot overlay: bitmap or entities null")
            return
        }

        val annotated = activity.capturedBitmap!!.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(annotated)

        // Draw using WareHouseLocalizerGraphic style (matching preview)
        val rectPaint = Paint().apply {
            color = Color.GREEN
            style = Paint.Style.STROKE
            strokeWidth = 16f
        }

        for (entity in entities) {
            val rect = entity.boundingBox
            if (rect != null) {
                Log.d(TAG, "Original bbox: $rect")
                val overlayRect = boundingBoxMapper.mapBoundingBoxToOverlay(rect)
                Log.d(TAG, "Mapped bbox: $overlayRect")
                canvas.drawRect(overlayRect, rectPaint)
            }
        }
        activity.runOnUiThread {
            activity.binding.capturedImageView.setImageBitmap(annotated)
        }
    }

    fun clearCaptureTapListener() {
        activity.runOnUiThread(Runnable {
            // Remove tap listener from capturedImageView
            activity.binding.capturedImageView.setOnTouchListener(null)
            activity.binding.capturedImageView.setClickable(false)

            // Clear cached data
            capturedEntities.clear()
            capturedShelves.clear()
            capturedProducts.clear()
            capturedLabels.clear()
            capturedShelfViewRects.clear()
            capturedProductViewRects.clear()
            capturedLabelViewRects.clear()
        })
    }
}
