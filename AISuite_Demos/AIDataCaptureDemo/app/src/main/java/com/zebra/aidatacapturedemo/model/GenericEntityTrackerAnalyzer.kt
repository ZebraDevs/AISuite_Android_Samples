// Copyright (c) 2024-2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.aidatacapturedemo.model

import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.zebra.ai.vision.analyzer.tracking.EntityTrackerAnalyzer
import com.zebra.ai.vision.detector.BarcodeDecoder
import com.zebra.ai.vision.detector.ComplexBBox
import com.zebra.ai.vision.detector.Detector
import com.zebra.ai.vision.detector.ModuleRecognizer
import com.zebra.ai.vision.detector.TextOCR
import com.zebra.ai.vision.entity.BarcodeEntity
import com.zebra.ai.vision.entity.Entity
import com.zebra.ai.vision.entity.LabelEntity
import com.zebra.ai.vision.entity.ParagraphEntity
import com.zebra.ai.vision.entity.ProductEntity
import com.zebra.ai.vision.entity.ShelfEntity
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.data.DetectionLevel
import com.zebra.aidatacapturedemo.data.ModuleData
import com.zebra.aidatacapturedemo.data.ResultData
import com.zebra.aidatacapturedemo.data.UsecaseState
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * [GenericEntityTrackerAnalyzer] class is used to detect & Track barcodes, ocr and shelf data
 * found on the Camera Live Preview
 *
 * @param uiState - Used to read all the UI Current State
 * @param viewModel - Used to write any UI State Changes via [AIDataCaptureDemoViewModel]
 */
class GenericEntityTrackerAnalyzer(
    val uiState: StateFlow<AIDataCaptureDemoUiState>,
    val viewModel: AIDataCaptureDemoViewModel
) {

    private lateinit var mActivityLifecycle: Lifecycle
    private val TAG = "GenericEntityTrackerAnalyzer"
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
    private var detectors: MutableList<Detector<out List<out Entity>>> = mutableListOf()

    /** setAnalyzer() can legitimately call this more than once per scan session (it retries
     * until the native model is ready) — without the containment check, the same detector object
     * would be added multiple times, so the SDK ends up processing that detector's work multiple
     * times per frame instead of once. */
    fun addDecoder(detector: Detector<out List<out Entity>>) {
        if (!detectors.contains(detector)) {
            detectors.add(detector)
        }
    }

    fun setupEntityTrackerAnalyzer(myLifecycle: Lifecycle): EntityTrackerAnalyzer {
        mActivityLifecycle = myLifecycle

        val entityTrackerAnalyzer = when (uiState.value.usecaseSelected) {
            UsecaseState.OCRBarcodeFind.value -> {
                EntityTrackerAnalyzer(
                    detectors,
                    ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
                    executorService,
                    ::handleEntitiesOcrBarcodeFilter
                )
            }

            else -> {
                EntityTrackerAnalyzer(
                    detectors,
                    ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
                    executorService,
                    ::handleEntities
                )
            }
        }

        return entityTrackerAnalyzer
    }

    private fun handleEntities(result: EntityTrackerAnalyzer.Result) {
        // Map entities → ResultData on a background dispatcher (not Main) — for OCR at high
        // resolution this per-frame nested-loop mapping was heavy enough to skip frames. The
        // viewModel.updateXxx() calls push to a thread-safe StateFlow, so Main isn't required.
        mActivityLifecycle.coroutineScope.launch(Dispatchers.Default) {
            // This is a bare launch{} with no CoroutineExceptionHandler, so any uncaught
            // exception here (e.g. a null entity field) would crash the app. Guard it.
            try {
                detectors.forEach { detector ->
                    if (detector is BarcodeDecoder) {
                        val returnEntityList = result.getValue(detector)
                        var rectList: MutableList<ResultData> = mutableListOf()
                        returnEntityList?.forEach { entity ->
                            if (entity != null) {
                                val barcodeEntity = entity as BarcodeEntity
                                val value = barcodeEntity.value
                                val rect = barcodeEntity.boundingBox
                                rectList += ResultData(boundingBox = rect, text = value)
                            }
                        }
                        viewModel.updateBarcodeResultData(results = rectList)
                    } else if (detector is TextOCR) {
                        val returnEntityList = result.getValue(detector)
                        val outputOCRResultData = mutableListOf<ResultData>()
                        returnEntityList?.forEach { entity ->
                            if (entity != null) {
                                val paragraphEntity = entity as ParagraphEntity
                                val lines = paragraphEntity.lines
                                for (line in lines) {
                                    for (word in line.words) {
                                        val bbox = word.complexBBox

                                        if (bbox != null && bbox.x != null && bbox.y != null && bbox.x.size == 4 && bbox.y.size == 4) {
                                            val minX = bbox.x[0]
                                            val maxX = bbox.x[2]
                                            val minY = bbox.y[0]
                                            val maxY = bbox.y[2]

                                            val rect = Rect(
                                                minX.toInt(),
                                                minY.toInt(),
                                                maxX.toInt(),
                                                maxY.toInt()
                                            )
                                            val decodedValue = word.text
                                            outputOCRResultData.add(
                                                ResultData(
                                                    boundingBox = rect,
                                                    text = decodedValue
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        viewModel.updateOcrResultData(results = outputOCRResultData)
                        viewModel.captureLatestOcrFrame()
                        viewModel.logOcrFrameResults(
                            returnEntityList?.filterIsInstance<ParagraphEntity>() ?: emptyList(),
                            isLineLevel = false
                        )
                    } else if (detector is ModuleRecognizer) {
                        if (!viewModel.isRetailPreviewAnalysisStopped()) {
                            val returnEntityList = result.getValue(detector)
                            val shelves = mutableListOf<ShelfEntity>()
                            val labels = mutableListOf<LabelEntity>()
                            val products = mutableListOf<ProductEntity>()
                            returnEntityList?.forEach { entity ->
                                when (entity) {
                                    is ShelfEntity -> shelves.add(entity)
                                    is LabelEntity -> labels.add(entity)
                                    is ProductEntity -> products.add(entity)
                                }
                            }
                            viewModel.updateModuleRecognitionResult(
                                ModuleData(
                                    shelves,
                                    labels,
                                    products
                                )
                            )
                        }
                    } else {
                        Log.e(TAG, "handleEntities => Unknown detector type found = $detector ")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "handleEntities failed: ${e.message}", e)
            }
        }
    }

    private fun handleEntitiesOcrBarcodeFilter(result: EntityTrackerAnalyzer.Result) {
        mActivityLifecycle.coroutineScope.launch(Dispatchers.Default) {
            // Same reasoning as handleEntities() — this is a bare launch{} with no
            // CoroutineExceptionHandler, so guard against an uncaught exception crashing the app.
            try {
                detectors.forEach { detector ->
                    if (detector is BarcodeDecoder) {
                        val returnEntityList = result.getValue(detector)
                        var rectList: MutableList<ResultData> = mutableListOf()
                        returnEntityList?.forEach { entity ->
                            if (entity != null) {
                                val barcodeEntity = entity as BarcodeEntity
                                val value = barcodeEntity.value
                                val rect = barcodeEntity.boundingBox
                                rectList += ResultData(boundingBox = rect, text = value)
                            }
                        }

                        // If feedbackSettings.showDetectedBarcode is false -> then don't show the undecoded barcodes on the display
                        if (!uiState.value.ocrBarcodeFindSettings.feedbackSettings.showDetectedBarcode) {
                            rectList.retainAll { it.text.isNotBlank() }
                        }

                        viewModel.updateBarcodeResultData(
                            results = FilterUtils.getBarcodeFilteredResultData(
                                uiState = uiState.value,
                                outputBarcodeResultData = rectList
                            )
                        )


                    } else if (detector is TextOCR) {
                        val returnEntityList = result.getValue(detector)
                        val outputOCRResultData = mutableListOf<ResultData>()
                        val shouldUseLineLevel = run {
                            val selectedIds = uiState.value.ocrFilterData.selectedFieldIds.toSet()
                            uiState.value.ocrFilterData.fieldFilterConfigs.any { cfg ->
                                cfg.fieldId in selectedIds && (
                                        (cfg.regexEnabled && cfg.regexData?.detectionLevel == DetectionLevel.LINE) ||
                                                (cfg.characterMatchEnabled && cfg.characterMatchData?.detectionLevel == DetectionLevel.LINE))
                            }
                        }

                        returnEntityList?.filterNotNull()?.forEach { entity ->
                            val paragraphEntity = entity as ParagraphEntity
                            paragraphEntity.lines?.forEach { line ->
                                if (shouldUseLineLevel) {
                                    bboxToResultData(line.complexBBox, line.text)
                                        ?.let { outputOCRResultData.add(it) }
                                } else {
                                    line.words.forEach { word ->
                                        bboxToResultData(word.complexBBox, word.text)
                                            ?.let { outputOCRResultData.add(it) }
                                    }
                                }
                            }
                        }

                        viewModel.updateOcrResultData(
                            results = FilterUtils.getOcrFilteredResultData(
                                uiState = uiState.value,
                                outputOCRResultData = outputOCRResultData
                            )
                        )
                    } else {
                        Log.e(TAG, "handleEntities => Unknown detector type found = $detector ")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "handleEntitiesOcrBarcodeFilter failed: ${e.message}", e)
            }
        }
    }

    fun deinitialize() {
        executorService.shutdown()
    }

    private fun bboxToResultData(bbox: ComplexBBox?, text: String): ResultData? {
        if (bbox == null || bbox.x == null || bbox.y == null || bbox.x.size < 4 || bbox.y.size < 4)
            return null
        val rect = Rect(
            bbox.x[0].toInt(), bbox.y[0].toInt(),
            bbox.x[2].toInt(), bbox.y[2].toInt()
        )
        return ResultData(boundingBox = rect, text = text)
    }

}
