// Copyright (c) 2024-2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.aidatacapturedemo.data

import android.graphics.Bitmap
import com.zebra.ai.vision.detector.BBox
import com.zebra.aidatacapturedemo.model.FileUtils
import com.zebra.aidatacapturedemo.ui.view.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

val PROFILING = "Profiling"

enum class UsecaseState(val value: String) {
    Main("None"),
    Barcode("Barcode Recognizer"),
    OCR("Text/OCR Recognizer"),
    Retail("Product & Shelf Localizer"),
    OCRFind("OCR Find"),
    Product("Product & Shelf Recognizer")
}

data class BarcodeSymbology(
    var australian_postal :Boolean =  false,
    var aztec :Boolean =  true,
    var canadian_postal :Boolean =  false,
    var chinese_2of5 :Boolean =  false,
    var codabar :Boolean =  true,
    var code11 :Boolean =  false,
    var code39 :Boolean =  true,
    var code93 :Boolean =  false,
    var code128 :Boolean =  true,
    var composite_ab :Boolean =  false,
    var composite_c :Boolean =  false,
    var d2of5 :Boolean =  false,
    var datamatrix :Boolean =  true,
    var dotcode :Boolean =  false,
    var dutch_postal :Boolean =  false,
    var ean_8 :Boolean =  true,
    var ean_13 :Boolean =  true,
    var finnish_postal_4s :Boolean =  false,
    var grid_matrix :Boolean =  false,
    var gs1_databar :Boolean =  true,
    var gs1_databar_expanded :Boolean =  true,
    var gs1_databar_lim :Boolean =  false,
    var gs1_datamatrix :Boolean =  false,
    var gs1_qrcode :Boolean =  false,
    var hanxin :Boolean =  false,
    var i2of5 :Boolean =  false,
    var japanese_postal :Boolean =  false,
    var korean_3of5 :Boolean =  false,
    var mailmark :Boolean =  true,
    var matrix_2of5	 :Boolean =  false,
    var maxicode :Boolean =  true,
    var micropdf :Boolean =  false,
    var microqr :Boolean =  false,
    var msi :Boolean =  false,
    var pdf417 :Boolean =  true,
    var qrcode :Boolean =  true,
    var tlc39 :Boolean =  false,
    var trioptic39 :Boolean =  false,
    var uk_postal :Boolean =  false,
    var upc_a :Boolean =  true,
    var upce0 :Boolean =  true,
    var upce1 :Boolean =  false,
    var usplanet :Boolean =  false,
    var uspostnet :Boolean =  false,
    var us4state :Boolean =  false,
    var us4state_fics :Boolean =  false,
)

data class CommonSettings(
    var processorSelectedIndex: Int = 0,
    var resolutionSelectedIndex: Int = 1,
    var inputSizeSelected: Int = 1280,
)

data class BarcodeSettings(
    var commonSettings: CommonSettings = CommonSettings(),
    var barcodeSymbology: BarcodeSymbology = BarcodeSymbology()
) {
    fun isEquals(other: BarcodeSettings): Boolean {
        return commonSettings.processorSelectedIndex == other.commonSettings.processorSelectedIndex &&
                commonSettings.resolutionSelectedIndex == other.commonSettings.resolutionSelectedIndex &&
                commonSettings.inputSizeSelected == other.commonSettings.inputSizeSelected &&
                barcodeSymbology == other.barcodeSymbology
    }
}

data class OcrFindSettings(
    var commonSettings: CommonSettings = CommonSettings()
)

data class AdvancedOCRSetting(
    // Detection Parameters
    var heatmapThreshold :String =  0.5f.toString(),
    var boxThreshold :String =  0.85f.toString(),
    var minBoxArea :String =  "10",
    var minBoxSize :String =  "1",
    var unclipRatio :String =  1.5f.toString(),
    var minRatioForRotation :String =  1.5f.toString(),
    // Recognition Parameters
    var maxWordCombinations :String =  10.toString(),
    var totalProbabilityThreshold :String =  0.8999f.toString(),
    var topkIgnoreCutoff :String =  4.toString(),

    // Tiling Related
    var enableTiling : Boolean =  false,
    var topCorrelationThreshold :String =  0.0f.toString(),
    var mergePointsCutoff :String =  5.0f.toString(),
    var splitMarginFactor :String =  0.1f.toString(),
    var aspectRatioLowerThreshold :String =  10.0f.toString(),
    var aspectRatioUpperThreshold :String =  40.toString(),
    var topKMergedPredictions :String =  5.0f.toString(),

    //Grouping Related
    var enableGrouping : Boolean =   false,
    var widthDistanceRatio :String =   1.5f.toString(),
    var heightDistanceRatio :String =  2.0f.toString(),
    var centerDistanceRatio :String =  0.6f.toString(),
    var paragraphHeightDistance :String =  1.0f.toString(),
    var paragraphHeightRatioThreshold :String =  0.3333f.toString()
)

data class TextOcrSettings(
    var commonSettings: CommonSettings = CommonSettings(),
    var advancedOCRSetting: AdvancedOCRSetting = AdvancedOCRSetting()
)

data class RetailShelfSettings(
    var commonSettings: CommonSettings = CommonSettings(),
)
data class ProductRecognitionSettings(
    var commonSettings: CommonSettings = CommonSettings(),
)
/**
 * AIDataCaptureDemoUiState class used to store UI state data
 * This is used to save data from updated by UI as well as Model
 */
data class AIDataCaptureDemoUiState(
    // UI --> Model
    var usecaseSelected: String = UsecaseState.Main.value,
    var activeScreen: Screen = Screen.Start,
    var zoomLevel: Float = 1.0f,
    val appBarTitle: String = "",
    val toastMessage: String? = null,

    // Settings
    var barcodeSettings : BarcodeSettings = FileUtils.loadBarcodeSettings(),
    var textOCRSettings : TextOcrSettings = FileUtils.loadOCRSettings(),
    var ocrFindSettings: OcrFindSettings = FileUtils.loadAdvancedOCRSettings(),
    var retailShelfSettings : RetailShelfSettings = FileUtils.loadRetailShelfSettings(),
    var productRecognitionSettings: ProductRecognitionSettings = FileUtils.loadProductRecognitionSettings(),

    // Model --> UI
    var modelDemoReady: Boolean = false,
    var isCameraReady: Boolean = false,
    var cameraError: String? = null,
    var isProductEnrollmentCompleted: Boolean = false,
    var currentBitmap: Bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888),
    var captureBitmap: Bitmap? = null,
    var bboxes: Array<BBox?> = arrayOf(),
    var productResults: MutableList<ProductData> = mutableListOf(),
    val ocrResults: List<ResultData> = listOf(),
    var barcodeResults: List<ResultData> = listOf(),

    var selectedOCRFilterData: OCRFilterData = OCRFilterData(ocrFilterType = OCRFilterType.SHOW_ALL)
)