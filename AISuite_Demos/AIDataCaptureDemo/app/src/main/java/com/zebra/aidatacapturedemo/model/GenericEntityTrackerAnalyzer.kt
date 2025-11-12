// Copyright (c) 2024-2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.aidatacapturedemo.model

import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.zebra.ai.vision.analyzer.tracking.EntityTrackerAnalyzer
import com.zebra.ai.vision.detector.AIVisionSDKException
import com.zebra.ai.vision.detector.BarcodeDecoder
import com.zebra.ai.vision.detector.Detector
import com.zebra.ai.vision.detector.TextOCR
import com.zebra.ai.vision.entity.BarcodeEntity
import com.zebra.ai.vision.entity.Entity
import com.zebra.ai.vision.entity.ParagraphEntity
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.data.OCRFilterData
import com.zebra.aidatacapturedemo.data.OCRFilterType
import com.zebra.aidatacapturedemo.data.ResultData
import com.zebra.aidatacapturedemo.data.PROFILING
import com.zebra.aidatacapturedemo.data.UsecaseState
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * [GenericEntityTrackerAnalyzer] class is used to detect & Track barcodes found on the Camera Live Preview
 *
 * @param uiState - Used to read all the UI Current State
 * @param viewModel - Used to write any UI State Changes via [AIDataCaptureDemoViewModel]
 */
class GenericEntityTrackerAnalyzer(val uiState: StateFlow<AIDataCaptureDemoUiState>, val viewModel: AIDataCaptureDemoViewModel) {

    private lateinit var mActivityLifecycle: Lifecycle
    private val TAG = "GenericEntityTrackerAnalyzer"
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
    private var detectors: MutableList<Detector<out List<out Entity>>> = mutableListOf()

    private lateinit var ocrFilterTypeData: OCRFilterData


    fun addDecoder(detector : Detector<out List<out Entity>>){
        detectors.add(detector)
    }

    fun setupEntityTrackerAnalyzer(myLifecycle: Lifecycle): EntityTrackerAnalyzer {
        mActivityLifecycle = myLifecycle

        val entityTrackerAnalyzer = EntityTrackerAnalyzer(
            detectors,
            ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
            executorService,
            ::handleEntities
        )
        return entityTrackerAnalyzer
    }

    private fun handleEntities(result: EntityTrackerAnalyzer.Result) {
        mActivityLifecycle.coroutineScope.launch(Dispatchers.Main) {
            detectors.forEach { detector ->
                if (detector is BarcodeDecoder) {
                    val returnEntityList = result.getValue(detector)
                    var rectList: List<ResultData> = mutableListOf()
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
                    viewModel.updateOcrResultData(
                        results = TextOCRUtils.toFilteredByType(
                            ocrFilterData = uiState.value.selectedOCRFilterData,
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