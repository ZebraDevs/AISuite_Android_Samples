package com.zebra.aisuite_quickstart.kotlin.handlers

import android.graphics.Rect
import android.util.Log
import com.zebra.ai.vision.detector.BBox
import com.zebra.ai.vision.detector.BarcodeDecoder
import com.zebra.ai.vision.detector.Recognizer
import com.zebra.ai.vision.detector.Word
import com.zebra.ai.vision.entity.BarcodeEntity
import com.zebra.ai.vision.entity.Entity
import com.zebra.ai.vision.entity.ParagraphEntity
import com.zebra.aisuite_quickstart.kotlin.CameraXLivePreviewActivity
import com.zebra.aisuite_quickstart.kotlin.analyzers.tracker.TrackerGraphic
import com.zebra.aisuite_quickstart.kotlin.detectors.barcodedecodersample.BarcodeGraphic
import com.zebra.aisuite_quickstart.kotlin.detectors.textocrsample.OCRGraphic
import com.zebra.aisuite_quickstart.kotlin.lowlevel.productrecognitionsample.ProductRecognitionGraphic
import com.zebra.aisuite_quickstart.kotlin.viewfinder.EntityViewGraphic

class DetectionResultHandler(
    private val activity: CameraXLivePreviewActivity,
    private val boundingBoxMapper: BoundingBoxMapper
) {
    companion object {
        private const val TAG = "DetectionResultHandler"
        private const val SIMILARITY_THRESHOLD = 0.65f
    }

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

    // Product recognition detection result handler
    fun handleDetectionRecognitionResult(
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

                for (bBox in labelShelfObjects) {
                    val rect = Rect(bBox.xmin.toInt(), bBox.ymin.toInt(), bBox.xmax.toInt(), bBox.ymax.toInt())
                    val overlayRect = boundingBoxMapper.mapBoundingBoxToOverlay(rect)
                    labelShelfRects.add(overlayRect)
                }

                for (bBox in labelPegObjects) {
                    val rect = Rect(bBox.xmin.toInt(), bBox.ymin.toInt(), bBox.xmax.toInt(), bBox.ymax.toInt())
                    val overlayRect = boundingBoxMapper.mapBoundingBoxToOverlay(rect)
                    labelPegRects.add(overlayRect)
                }

                for (bBox in shelfObjects) {
                    val rect = Rect(bBox.xmin.toInt(), bBox.ymin.toInt(), bBox.xmax.toInt(), bBox.ymax.toInt())
                    val overlayRect = boundingBoxMapper.mapBoundingBoxToOverlay(rect)
                    shelfRects.add(overlayRect)
                }

                if (recognitions.isEmpty()) {
                    decodedStrings.add("No products found")
                    recognizedRects.add(Rect(250, 250, 0, 0))
                } else {
                    Log.v(TAG, "products length: ${products.size} recognitions length: ${recognitions.size}")
                    for (i in products.indices) {
                        if (recognitions[i].similarity[0] > SIMILARITY_THRESHOLD) {
                            val bBox = products[i]
                            val rect = Rect(bBox.xmin.toInt(), bBox.ymin.toInt(), bBox.xmax.toInt(), bBox.ymax.toInt())
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
                        decodedStrings
                    )
                )
            }
        }
    }

    fun handleEntityTrackerDetection(barcodeEntities: List<Entity>?, ocrEntities: List<Entity>?) {
        val barcodeRects = mutableListOf<Rect>()
        val barcodeStrings = mutableListOf<String>()
        val ocrRects = mutableListOf<Rect>()
        val ocrStrings = mutableListOf<String>()

        activity.runOnUiThread {
            activity.binding.graphicOverlay.clear()
            barcodeEntities?.forEach { entity ->
                if (entity is BarcodeEntity) {
                    val rect = entity.boundingBox
                    rect?.let {
                        val overlayRect = boundingBoxMapper.mapBoundingBoxToOverlay(rect)
                        barcodeRects.add(overlayRect)
                        val hashCode = entity.hashCode().toString().takeLast(4)
                        barcodeStrings.add("$hashCode:${entity.value}")
                        Log.d(TAG, "Tracker UUID: $hashCode Detected entity - Value: ${entity.value}")
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

                                    val rect = Rect(minX.toInt(), minY.toInt(), maxX.toInt(), maxY.toInt())
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
            // Add graphics for barcodes
            if (barcodeRects.isNotEmpty()) {
                activity.binding.graphicOverlay.add(TrackerGraphic(activity.binding.graphicOverlay, barcodeRects, barcodeStrings)
                )
            }

            // Add graphics for OCR
            if (ocrRects.isNotEmpty()) {
                activity.binding.graphicOverlay.add(OCRGraphic(activity.binding.graphicOverlay, ocrRects, ocrStrings))
            }
        }
    }

    fun handleEntityViewFinderDetection(entities: List<Entity>?, entityViewGraphic: EntityViewGraphic) {
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
}
