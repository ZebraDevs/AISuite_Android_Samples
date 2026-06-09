// Copyright (c) 2024-2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.aidatacapturedemo.model

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.zebra.ai.vision.detector.AIVisionSDKException
import com.zebra.ai.vision.detector.ImageData
import com.zebra.ai.vision.detector.InvalidInputException
import com.zebra.ai.vision.detector.TextOCR
import com.zebra.ai.vision.entity.ParagraphEntity
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.data.AdvancedFilterOption
import com.zebra.aidatacapturedemo.data.DetectionLevel
import com.zebra.aidatacapturedemo.data.OcrRegularFilterOption
import com.zebra.aidatacapturedemo.data.PROFILING
import com.zebra.aidatacapturedemo.data.ResultData
import com.zebra.aidatacapturedemo.data.UsecaseState
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * [TextOCRAnalyzer] class is used to detect all the Optical Character Recognition (OCR) found on the Camera Live Preview
 *
 * @param uiState - Used to read all the UI Current State
 * @param viewModel - Used to write any UI State Changes via [AIDataCaptureDemoViewModel]
 */
class TextOCRAnalyzer(
    val uiState: StateFlow<AIDataCaptureDemoUiState>,
    val viewModel: AIDataCaptureDemoViewModel
) {
    private val TAG = "TextOCRAnalyzer"

    private var textOCR: TextOCR? = null
    private val textOCRSettings = TextOCR.Settings("text-ocr-recognizer")
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()

    /**
     * initialize function is used to initialize the TextOCR model with the specified settings
     * and handle any exceptions that may occur during the initialization process.
     * It also updates the UI state to indicate whether the OCR model is ready or not.
     */
    fun initialize() {
        try {
            textOCR?.dispose()
            textOCR = null
            updateOcrModelDemoReady(false)

            configure()

            val mStart = System.currentTimeMillis()
            TextOCR.getTextOCR(textOCRSettings, executorService).thenAccept { ocrInstance ->
                textOCR = ocrInstance
                updateOcrModelDemoReady(true)
                Log.e(
                    PROFILING,
                    "TextOCR() obj creation / model loading time = ${System.currentTimeMillis() - mStart} milli sec"
                )
                Log.i(TAG, "TextOCR creation success")
            }.exceptionally { e ->
                Log.e(TAG, "Fatal error: TextOCR creation failed - ${e.message}")
                if ((e.message?.contains("Given runtimes are not available") == true) ||
                    (e.message?.contains("Error creating SNPE object") == true)) {
                    viewModel.updateToastMessage(message = "Selected inference type is not supported on this device. Switching to Auto-select for optimal performance.")
                    viewModel.updateSelectedProcessor(0) //Auto-Select
                    viewModel.saveSettings()
                    initialize()
                }
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error: load failed - ${e.message}")
        }
    }

    fun deinitialize() {
        textOCR?.dispose()
        textOCR = null
    }

    fun getDetector() : TextOCR? {
        return textOCR
    }

    /** executeHighRes function is used to perform OCR analysis on a high-resolution bitmap image.
     * It submits the analysis task to an executor service, processes the image data, and updates
     * the UI state with the OCR results.
     * The function also handles exceptions that may occur during the analysis process.
     *
     * @param highResBitmap - The high-resolution bitmap image to be analyzed for OCR.
     */
    fun executeHighRes(highResBitmap: Bitmap) {
        executorService.submit {
            try {
                Log.d(TAG, "Starting image analysis")
                val highResImageData: ImageData = ImageData.fromBitmap(highResBitmap, 0)
                textOCR?.process(highResImageData)
                    ?.thenAccept { result ->
                        if ((uiState.value.ocrFilterData.selectedRegularFilterOption == OcrRegularFilterOption.REGEX && uiState.value.ocrFilterData.selectedRegexFilterData.detectionLevel == DetectionLevel.LINE) ||
                            (uiState.value.ocrFilterData.selectedRegularFilterOption == OcrRegularFilterOption.ADVANCED) &&
                            uiState.value.ocrFilterData.selectedAdvancedFilterOptionList.contains(
                                AdvancedFilterOption.CHARACTER_MATCH
                            ) &&
                            uiState.value.ocrFilterData.selectedCharacterMatchFilterData.detectionLevel == DetectionLevel.LINE
                        ) {
                            onDetectionTextResultLineLevel(result)
                        } else {
                            onDetectionTextResultWordLevel(result)
                        }
                    }
            } catch (e: InvalidInputException) {
                Log.e(TAG, e.message ?: "InvalidInputException occurred")
            } catch (e: AIVisionSDKException) {
                Log.e(TAG, e.message ?: "AIVisionSDKException occurred")
            } finally {
                // Optional cleanup
            }
        }
    }

    private fun configure() {
        try {
            if (uiState.value.usecaseSelected == UsecaseState.OCRBarcodeFind.value) {
                val processorOrder =
                    when (uiState.value.ocrBarcodeFindSettings.commonSettings.processorSelectedIndex) {
                        0 -> arrayOf(2, 0, 1) // AUTO
                        1 -> arrayOf(2) // DSP
                        2 -> arrayOf(1) // GPU
                        3 -> arrayOf(0) //CPU
                        else -> arrayOf(2, 0, 1)
                    }
                textOCRSettings.detectionInferencerOptions.runtimeProcessorOrder = processorOrder
                textOCRSettings.recognitionInferencerOptions.runtimeProcessorOrder = processorOrder

                textOCRSettings.decodingTotalProbThreshold = 0F

                textOCRSettings.detectionInferencerOptions.defaultDims.width =
                    uiState.value.ocrBarcodeFindSettings.commonSettings.inputSizeSelected
                textOCRSettings.detectionInferencerOptions.defaultDims.height =
                    uiState.value.ocrBarcodeFindSettings.commonSettings.inputSizeSelected

                // Optimized adjustments for OCR Find pipeline
                textOCRSettings.unclipRatio = 2.0f
                textOCRSettings.minRatioForRotation = 0.1f
            } else {
                val processorOrder =
                    when (uiState.value.textOCRSettings.commonSettings.processorSelectedIndex) {
                        0 -> arrayOf(2, 0, 1) // AUTO
                        1 -> arrayOf(2) // DSP
                        2 -> arrayOf(1) // GPU
                        3 -> arrayOf(0) //CPU
                        else -> arrayOf(2, 0, 1)
                    }
                textOCRSettings.detectionInferencerOptions.runtimeProcessorOrder = processorOrder
                textOCRSettings.recognitionInferencerOptions.runtimeProcessorOrder = processorOrder

                textOCRSettings.decodingTotalProbThreshold = 0F

                textOCRSettings.detectionInferencerOptions.defaultDims.width =
                    uiState.value.textOCRSettings.commonSettings.inputSizeSelected
                textOCRSettings.detectionInferencerOptions.defaultDims.height =
                    uiState.value.textOCRSettings.commonSettings.inputSizeSelected

                // DETECTION PARAMETERS - Tuned for Curved & Vertical Medical Labels
                textOCRSettings.heatmapThreshold =
                    uiState.value.textOCRSettings.advancedOCRSetting.heatmapThreshold.toFloat()
                textOCRSettings.boxThreshold =
                    uiState.value.textOCRSettings.advancedOCRSetting.boxThreshold.toFloat()
                textOCRSettings.minBoxArea =
                    uiState.value.textOCRSettings.advancedOCRSetting.minBoxArea.toInt()
                textOCRSettings.minBoxSize =
                    uiState.value.textOCRSettings.advancedOCRSetting.minBoxSize.toInt()

                // FIXED: Increased unclip margin to capture complete warped text curves near margins
                textOCRSettings.unclipRatio = 2.2f

                // FIXED: Drastically lowered rotation filter limit to process full vertical text frames
                textOCRSettings.minRatioForRotation = 0.05f

                textOCRSettings.decodingMaxWordCombinations =
                    uiState.value.textOCRSettings.advancedOCRSetting.maxWordCombinations.toInt()
                textOCRSettings.decodingTopkIgnoreCutoff =
                    uiState.value.textOCRSettings.advancedOCRSetting.topkIgnoreCutoff.toInt()
                textOCRSettings.decodingTotalProbThreshold =
                    uiState.value.textOCRSettings.advancedOCRSetting.totalProbabilityThreshold.toFloat()

                // OCR Tiling Related
                if (uiState.value.textOCRSettings.advancedOCRSetting.enableTiling) {
                    textOCRSettings.tiling.enable = true
                    textOCRSettings.tiling.topCorrelationThr =
                        uiState.value.textOCRSettings.advancedOCRSetting.topCorrelationThreshold.toFloat()
                    textOCRSettings.tiling.mergePointsCutoff =
                        uiState.value.textOCRSettings.advancedOCRSetting.mergePointsCutoff.toInt()
                    textOCRSettings.tiling.splitMarginFactor =
                        uiState.value.textOCRSettings.advancedOCRSetting.splitMarginFactor.toFloat()
                    textOCRSettings.tiling.aspectRatioLowerThr =
                        uiState.value.textOCRSettings.advancedOCRSetting.aspectRatioLowerThreshold.toFloat()
                    textOCRSettings.tiling.aspectRatioUpperThr =
                        uiState.value.textOCRSettings.advancedOCRSetting.aspectRatioUpperThreshold.toFloat()
                    textOCRSettings.tiling.topkMergedPredictions =
                        uiState.value.textOCRSettings.advancedOCRSetting.topKMergedPredictions.toInt()
                } else {
                    textOCRSettings.tiling.enable = false
                }

                if (uiState.value.textOCRSettings.advancedOCRSetting.enableGrouping) {
                    // FIXED: Expanded spatial thresholds so curved words aren't mistakenly separated
                    textOCRSettings.grouping.widthDistanceRatio = 2.2f
                    textOCRSettings.grouping.heightDistanceRatio = 2.5f
                    textOCRSettings.grouping.centerDistanceRatio = 0.8f
                    textOCRSettings.grouping.paragraphHeightDistance = 1.5f
                    textOCRSettings.grouping.paragraphHeightRatioThreshold = 0.45f
                } else {
                    // Reset to stable defaults optimized for cylindrical label setups
                    textOCRSettings.grouping.widthDistanceRatio = 2.0f
                    textOCRSettings.grouping.heightDistanceRatio = 2.2f
                    textOCRSettings.grouping.centerDistanceRatio = 0.7f
                    textOCRSettings.grouping.paragraphHeightDistance = 1.2f
                    textOCRSettings.grouping.paragraphHeightRatioThreshold = 0.35f
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error: configure failed - ${e.message}")
        }
    }

    private fun updateOcrModelDemoReady(isReady: Boolean) {
        viewModel.updateOcrModelDemoReady(isReady = isReady)
    }

    private fun onDetectionTextResultWordLevel(entityList: List<ParagraphEntity>) {
        val outputOCRResultData = mutableListOf<ResultData>()
        entityList.forEach { entity ->
            val paragraphEntity = entity
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
        viewModel.updateOcrResultData(
            results = FilterUtils.getOcrFilteredResultData(
                uiState = uiState.value,
                outputOCRResultData = outputOCRResultData
            )
        )
    }

    private fun onDetectionTextResultLineLevel(entityList: List<ParagraphEntity>) {
        val outputOCRResultData = mutableListOf<ResultData>()
        entityList.forEach { entity ->
            val paragraphEntity = entity
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
        viewModel.updateOcrResultData(
            results = FilterUtils.getOcrFilteredResultData(
                uiState = uiState.value,
                outputOCRResultData = outputOCRResultData
            )
        )
    }
}