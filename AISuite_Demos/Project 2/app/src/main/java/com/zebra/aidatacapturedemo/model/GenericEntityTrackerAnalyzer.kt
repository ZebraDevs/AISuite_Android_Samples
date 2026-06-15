// Copyright (c) 2024-2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.aidatacapturedemo.model

import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.zebra.ai.vision.analyzer.tracking.EntityTrackerAnalyzer
import com.zebra.ai.vision.detector.BarcodeDecoder
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
import com.zebra.aidatacapturedemo.data.AdvancedFilterOption
import com.zebra.aidatacapturedemo.data.DetectionLevel
import com.zebra.aidatacapturedemo.data.ModuleData
import com.zebra.aidatacapturedemo.data.OcrRegularFilterOption
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
class GenericEntityTrackerAnalyzer(val uiState: StateFlow<AIDataCaptureDemoUiState>, val viewModel: AIDataCaptureDemoViewModel) {

    private lateinit var mActivityLifecycle: Lifecycle
    private val TAG = "GenericEntityTrackerAnalyzer"
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
    private var detectors: MutableList<Detector<out List<out Entity>>> = mutableListOf()

    fun addDecoder(detector : Detector<out List<out Entity>>){
        detectors.add(detector)
    }

    fun setupEntityTrackerAnalyzer(myLifecycle: Lifecycle): EntityTrackerAnalyzer {
        mActivityLifecycle = myLifecycle

        val entityTrackerAnalyzer = when(uiState.value.usecaseSelected){
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
        mActivityLifecycle.coroutineScope.launch(Dispatchers.Main) {
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
                } else if ( detector is TextOCR) {
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

                                        val rect = Rect(minX.toInt(), minY.toInt(), maxX.toInt(), maxY.toInt())
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
                } else if (detector is ModuleRecognizer) {
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
                    viewModel.updateModuleRecognitionResult(ModuleData(shelves, labels, products))
                }
                else {
                    Log.e(TAG, "handleEntities => Unknown detector type found = $detector ")
                }
            }
        }
    }

    private fun handleEntitiesOcrBarcodeFilter(result: EntityTrackerAnalyzer.Result) {
        mActivityLifecycle.coroutineScope.launch(Dispatchers.Main) {
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
                    if (!uiState.value.ocrBarcodeFindSettings.feedbackSettings.showDetectedBarcode){
                        rectList.retainAll { it.text.isNotBlank() }
                    }

                    viewModel.updateBarcodeResultData(
                        results = FilterUtils.getBarcodeFilteredResultData(
                            uiState = uiState.value,
                            outputBarcodeResultData = rectList
                        )
                    )


                } else if ( detector is TextOCR) {
                    val returnEntityList = result.getValue(detector)
                    val outputOCRResultData = mutableListOf<ResultData>()

                    if ((uiState.value.ocrFilterData.selectedRegularFilterOption == OcrRegularFilterOption.REGEX && uiState.value.ocrFilterData.selectedRegexFilterData.detectionLevel == DetectionLevel.LINE) ||
                        (uiState.value.ocrFilterData.selectedRegularFilterOption == OcrRegularFilterOption.ADVANCED) &&
                        uiState.value.ocrFilterData.selectedAdvancedFilterOptionList.contains(
                            AdvancedFilterOption.CHARACTER_MATCH) &&
                        uiState.value.ocrFilterData.selectedCharacterMatchFilterData.detectionLevel == DetectionLevel.LINE){

                        // Level.LINE_LEVEL results must be displayed
                        returnEntityList?.forEach { entity ->
                            if (entity != null) {
                                val paragraphEntity = entity as ParagraphEntity
                                val lines = paragraphEntity.lines
                                for (line in lines) {
                                    val bbox = line.complexBBox

                                    if (bbox != null && bbox.x != null && bbox.y != null && bbox.x.size == 4 && bbox.y.size == 4) {
                                        val minX = bbox.x[0]
                                        val maxX = bbox.x[2]
                                        val minY = bbox.y[0]
                                        val maxY = bbox.y[2]

                                        val rect = Rect(minX.toInt(), minY.toInt(), maxX.toInt(), maxY.toInt())
                                        val decodedValue = line.text
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
                    }else{
                        // Level.WORD_LEVEL results must be displayed
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

                                            val rect = Rect(minX.toInt(), minY.toInt(), maxX.toInt(), maxY.toInt())
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
        }
    }
}