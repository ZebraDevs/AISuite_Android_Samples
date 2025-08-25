// Copyright (c) 2024-2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.aidatacapturedemo.model

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.zebra.ai.vision.detector.AIVisionSDKException
import com.zebra.ai.vision.detector.ImageData
import com.zebra.ai.vision.detector.InvalidInputException
import com.zebra.ai.vision.detector.TextOCR
import com.zebra.ai.vision.entity.ParagraphEntity
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.data.OCRFilterData
import com.zebra.aidatacapturedemo.data.OCRFilterType
import com.zebra.aidatacapturedemo.data.ResultData
import com.zebra.aidatacapturedemo.data.PROFILING
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
) :
    ImageAnalysis.Analyzer {

    private lateinit var mOCRFilterTypeData: OCRFilterData
    private val TAG = "TextOCRAnalyzer"

    private var textOCR: TextOCR? = null
    private val textOCRSettings = TextOCR.Settings("text-ocr-recognizer")
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
    private var isAnalyzing = true

    init {
        mOCRFilterTypeData = if (uiState.value.usecaseSelected == UsecaseState.OCR.value){
            OCRFilterData(ocrFilterType = OCRFilterType.SHOW_ALL)
        }else{
            when (uiState.value.selectedOcrFilterType) {
                OCRFilterType.SHOW_ALL -> {
                    OCRFilterData(ocrFilterType = OCRFilterType.SHOW_ALL)
                }

                OCRFilterType.NUMERIC_CHARACTERS_ONLY -> {
                    OCRFilterData(
                        ocrFilterType = OCRFilterType.NUMERIC_CHARACTERS_ONLY,
                        charLengthMin = uiState.value.selectedNumericCharSliderValues.start.toInt(),
                        charLengthMax = uiState.value.selectedNumericCharSliderValues.endInclusive.toInt()
                    )
                }

                OCRFilterType.ALPHA_CHARACTERS_ONLY -> {
                    OCRFilterData(
                        ocrFilterType = OCRFilterType.ALPHA_CHARACTERS_ONLY,
                        charLengthMin = uiState.value.selectedAlphaCharSliderValues.start.toInt(),
                        charLengthMax = uiState.value.selectedAlphaCharSliderValues.endInclusive.toInt()
                    )
                }

                OCRFilterType.ALPHA_NUMERIC_CHARACTERS_ONLY -> {
                    OCRFilterData(
                        ocrFilterType = OCRFilterType.ALPHA_NUMERIC_CHARACTERS_ONLY,
                        charLengthMin = uiState.value.selectedAlphaNumericCharSliderValues.start.toInt(),
                        charLengthMax = uiState.value.selectedAlphaNumericCharSliderValues.endInclusive.toInt()
                    )
                }

                OCRFilterType.EXACT_MATCH -> {
                    OCRFilterData(
                        ocrFilterType = OCRFilterType.EXACT_MATCH,
                        exactMatchString = uiState.value.selectedExactMatchString
                    )
                }
            }
        }
    }

    override fun analyze(image: ImageProxy) {
        if (textOCR == null) {
            Log.e(TAG, "OCR is null")
            image.close()
            return
        }
        if (!isAnalyzing) {
            image.close()
            return
        }

        isAnalyzing = false // Set to false to prevent re-entry

        executorService.submit {
            try {
                Log.d(TAG, "Starting image analysis")
                val bitmap = rotateBitmapIfNeeded(image)
                textOCR?.process(ImageData.fromImageProxy(image))
                    ?.thenAccept { result ->
                        onDetectionTextResult(result)
                        image.close()
                    }
            } catch (e: InvalidInputException) {
                Log.e(TAG, e.message ?: "InvalidInputException occurred")
                image.close()
            } catch (e: AIVisionSDKException) {
                Log.e(TAG, e.message ?: "AIVisionSDKException occurred")
                image.close()
            } finally {
                isAnalyzing = true
            }
        }
    }

    fun startAnalyzing() {
        isAnalyzing = true
    }

    fun stopAnalyzing() {
        isAnalyzing = false
    }

    fun initialize() {
        try {
            textOCR?.dispose()
            textOCR = null
            updateModelDemoReady(false)

            configure()

            val mStart = System.currentTimeMillis()
            TextOCR.getTextOCR(textOCRSettings, executorService).thenAccept { ocrInstance ->
                textOCR = ocrInstance
                updateModelDemoReady(true)
                Log.e(
                    PROFILING,
                    "TextOCR() obj creation / model loading time = ${System.currentTimeMillis() - mStart} milli sec"
                )
                Log.i(TAG, "TextOCR creation success")
            }.exceptionally { e ->
                Log.e(TAG, "Fatal error: TextOCR creation failed - ${e.message}")
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

    private fun configure() {
        try {
            if (uiState.value.usecaseSelected == UsecaseState.OCR.value) {
                //Swap the values as the presented index is reverse of what model expects
                val processorOrder = when (uiState.value.textOCRSettings.commonSettings.processorSelectedIndex) {
                    0 -> arrayOf(2)
                    1 -> arrayOf(1)
                    2 -> arrayOf(0)
                    else -> {
                        arrayOf(2)
                    }
                }
                textOCRSettings.detectionInferencerOptions.runtimeProcessorOrder = processorOrder

                textOCRSettings.decodingTotalProbThreshold = 0F

                textOCRSettings.detectionInferencerOptions.defaultDims.width =
                    uiState.value.textOCRSettings.commonSettings.inputSizeSelected
                textOCRSettings.detectionInferencerOptions.defaultDims.height =
                    uiState.value.textOCRSettings.commonSettings.inputSizeSelected
            } else {
                //Swap the values as the presented index is reverse of what model expects
                val processorOrder = when (uiState.value.ocrFindSettings.commonSettings.processorSelectedIndex) {
                    0 -> arrayOf(2)
                    1 -> arrayOf(1)
                    2 -> arrayOf(0)
                    else -> {
                        arrayOf(2)
                    }
                }
                textOCRSettings.detectionInferencerOptions.runtimeProcessorOrder = processorOrder

                textOCRSettings.decodingTotalProbThreshold = 0F

                textOCRSettings.detectionInferencerOptions.defaultDims.width =
                    uiState.value.ocrFindSettings.commonSettings.inputSizeSelected
                textOCRSettings.detectionInferencerOptions.defaultDims.height =
                    uiState.value.ocrFindSettings.commonSettings.inputSizeSelected

                //Detection Parameters
                textOCRSettings.heatmapThreshold = uiState.value.ocrFindSettings.advancedOCRSetting.heatmapThreshold.toFloat()
                textOCRSettings.boxThreshold = uiState.value.ocrFindSettings.advancedOCRSetting.boxThreshold.toFloat()
                textOCRSettings.minBoxArea = uiState.value.ocrFindSettings.advancedOCRSetting.minBoxArea.toInt()
                textOCRSettings.minBoxSize = uiState.value.ocrFindSettings.advancedOCRSetting.minBoxSize.toInt()
                textOCRSettings.unclipRatio = uiState.value.ocrFindSettings.advancedOCRSetting.unclipRatio.toFloat()
                textOCRSettings.minRatioForRotation = uiState.value.ocrFindSettings.advancedOCRSetting.minRatioForRotation.toFloat()

                textOCRSettings.decodingMaxWordCombinations =
                    uiState.value.ocrFindSettings.advancedOCRSetting.maxWordCombinations.toInt()
                textOCRSettings.decodingTopkIgnoreCutoff = uiState.value.ocrFindSettings.advancedOCRSetting.topkIgnoreCutoff.toInt()
                textOCRSettings.decodingTotalProbThreshold =
                    uiState.value.ocrFindSettings.advancedOCRSetting.totalProbabilityThreshold.toFloat()

                // OCR Tiling related
                if (uiState.value.ocrFindSettings.advancedOCRSetting.enableTiling) {
                    textOCRSettings.tiling.enable = true
                    textOCRSettings.tiling.topCorrelationThr =
                        uiState.value.ocrFindSettings.advancedOCRSetting.topCorrelationThreshold.toFloat()
                    textOCRSettings.tiling.mergePointsCutoff =
                        uiState.value.ocrFindSettings.advancedOCRSetting.mergePointsCutoff.toInt()
                    textOCRSettings.tiling.splitMarginFactor =
                        uiState.value.ocrFindSettings.advancedOCRSetting.splitMarginFactor.toFloat()
                    textOCRSettings.tiling.aspectRatioLowerThr =
                        uiState.value.ocrFindSettings.advancedOCRSetting.aspectRatioLowerThreshold.toFloat()
                    textOCRSettings.tiling.aspectRatioUpperThr =
                        uiState.value.ocrFindSettings.advancedOCRSetting.aspectRatioUpperThreshold.toFloat()
                    textOCRSettings.tiling.topkMergedPredictions =
                        uiState.value.ocrFindSettings.advancedOCRSetting.topKMergedPredictions.toInt()
                } else {
                    textOCRSettings.tiling.enable = false
                }
                if (uiState.value.ocrFindSettings.advancedOCRSetting.enableGrouping) {
                    textOCRSettings.grouping.widthDistanceRatio =
                        uiState.value.ocrFindSettings.advancedOCRSetting.widthDistanceRatio.toFloat()
                    textOCRSettings.grouping.heightDistanceRatio =
                        uiState.value.ocrFindSettings.advancedOCRSetting.heightDistanceRatio.toFloat()
                    textOCRSettings.grouping.centerDistanceRatio =
                        uiState.value.ocrFindSettings.advancedOCRSetting.centerDistanceRatio.toFloat()
                    textOCRSettings.grouping.paragraphHeightDistance =
                        uiState.value.ocrFindSettings.advancedOCRSetting.paragraphHeightDistance.toFloat()
                    textOCRSettings.grouping.paragraphHeightRatioThreshold =
                        uiState.value.ocrFindSettings.advancedOCRSetting.paragraphHeightRatioThreshold.toFloat()
                } else {
                    //Reset to default values
                    textOCRSettings.grouping.widthDistanceRatio = 1.5f
                    textOCRSettings.grouping.heightDistanceRatio = 2.0f
                    textOCRSettings.grouping.centerDistanceRatio = 0.6f
                    textOCRSettings.grouping.paragraphHeightDistance = 1.0f
                    textOCRSettings.grouping.paragraphHeightRatioThreshold = 0.3333f
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error: configure failed - ${e.message}")
        }
    }

    private fun rotateBitmapIfNeeded(imageProxy: ImageProxy): Bitmap? {
        try {
            val bitmap = imageProxy.toBitmap()
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            return rotateBitmap(bitmap, rotationDegrees)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting image to bitmap: " + e.message)
            return null
        }
    }

    private fun rotateBitmap(bitmap: Bitmap?, degrees: Int): Bitmap? {
        if (degrees == 0 || bitmap == null) return bitmap

        try {
            val matrix = Matrix()
            matrix.postRotate(degrees.toFloat())
            return Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.getWidth(),
                bitmap.getHeight(),
                matrix,
                true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error rotating bitmap: " + e.message)
            return bitmap
        }
    }

    private fun onDetectionTextResult(list: List<ParagraphEntity>) {
        val outputOCRResultData = mutableListOf<ResultData>()
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
                        val decodedValue = word.decodes[0].content
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

        updateOcrResultData(
            results = TextOCRUtils.toFilteredByType(
                ocrFilterTypeData = mOCRFilterTypeData,
                outputOCRResultData = outputOCRResultData
            )
        )
    }

    fun updateOcrResultData(results: List<ResultData>) {
        viewModel.updateOcrResultData(results = results)
    }

    /**
     * Setter Function to communicate any runtime FilterType option changes.
     *
     * @param ocrFilterTypeData - Selected OCRFilterData option
     *
     * @return null
     */
    fun setOCRFilterType(ocrFilterTypeData: OCRFilterData) {
        mOCRFilterTypeData = ocrFilterTypeData
    }

    private fun updateModelDemoReady(isReady: Boolean) {
        viewModel.updateModelDemoReady(isReady = isReady)
    }
}