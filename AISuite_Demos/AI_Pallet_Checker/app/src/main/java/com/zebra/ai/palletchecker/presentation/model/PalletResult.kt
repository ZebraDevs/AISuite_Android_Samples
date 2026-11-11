package com.zebra.ai.palletchecker.presentation.model

private val TAG = "PalletResult"
enum class BOX_VALIDATION {
    VERIFIED,
    MISMATCH_QTY,
    PARTIAL_DETECTION,
    NOT_DETECTED

}

data class PalletBarcode(val boundingBox: androidx.compose.ui.geometry.Rect,val data:String="", val symbology:Int,
                         val isMainBarcode:Boolean = false, val isQtyBarcodes: Boolean = false, val angle:Double =0.0)
data class PalletLabel(val boundingBox: androidx.compose.ui.geometry.Rect, val barcodes:List<PalletBarcode> = emptyList(), val isQtyLabel: Boolean = false)
data class PalletBox(val id: Int = 0,
                     val boundingBox: androidx.compose.ui.geometry.Rect,
                     val barcodeList: List<PalletBarcode> = emptyList(),
                     val labelsList: List<PalletLabel> = emptyList(),
                     val barcodeInLables: List<Int> = emptyList(),
                     val isLeftAligned: Boolean = false,
                     val classId: Int = 1,
                     val validation: BOX_VALIDATION = BOX_VALIDATION.NOT_DETECTED,
                     val trackId: Long = -1L,
                     val stableKey: String = ""
                     )


data class PBarcodeUIModel(
    val data: String,
    val boundingBox: androidx.compose.ui.geometry.Rect,
    val symbology: Int,
    val isMainBarcode: Boolean = false,
    val isQtyBarcode: Boolean = false,
    val angle:Double =0.0
)

data class PLabelUIModel(val boundingBox: androidx.compose.ui.geometry.Rect, val barcodes: List<PBarcodeUIModel>, val isQtyLabel: Boolean = false)
data class PBoxUIModel(
    val id: Int,
    val boundingBox: androidx.compose.ui.geometry.Rect,
    val palletLabels: List<PLabelUIModel>,
    val palletBarcodes: List<PBarcodeUIModel>,
    val expectedQty : Int,
    val detectedQty: Int,
    val qtyList:List<Int> = emptyList(),
    val labelWithBarcodeCount :Int,
    val classId: Int = 1,
    val validation: BOX_VALIDATION = BOX_VALIDATION.NOT_DETECTED,
    val isLeftAligned: Boolean = false,
    val trackId: Long = -1L,
    val stableKey: String = ""
)

