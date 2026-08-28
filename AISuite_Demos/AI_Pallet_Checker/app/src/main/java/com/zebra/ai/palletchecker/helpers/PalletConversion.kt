package com.zebra.ai.palletchecker.helpers

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import androidx.core.text.isDigitsOnly
import com.zebra.ai.palletchecker.domain.model.BarcodeConfig
import com.zebra.ai.palletchecker.domain.model.FIELD_TYPE
import com.zebra.ai.palletchecker.presentation.model.BOX_VALIDATION
import com.zebra.ai.palletchecker.presentation.model.PBarcodeUIModel
import com.zebra.ai.palletchecker.presentation.model.PBoxUIModel
import com.zebra.ai.palletchecker.presentation.model.PLabelUIModel
import com.zebra.ai.palletchecker.presentation.model.PalletBarcode
import com.zebra.ai.palletchecker.presentation.model.PalletBox
import kotlin.math.roundToInt


private val TAG = "PalletConv"
private const val QTY_BARCODE_IDENTIFIER = "%"
private const val QTY_START_IDENTIFIER = "%92"

/**
 * Uniformly scales a [Rect] to correct coordinate overshoot caused by ImageAnalysis and Preview resolution mismatches.
 * Scales edges by `(PreviewRes / AnalysisRes)` to fix the SDK's Preview-based coordinate transformation.
 */
private fun Rect.scale(factor: Float): Rect =
    Rect(left * factor, top * factor, right * factor, bottom * factor)
private const val QTY_END_IDENTIFIER = "."

private data class TransformKey(
    val imageW: Int,
    val imageH: Int,
    val previewW: Int,
    val previewH: Int,
    val rotation: Int,
    val scaleType: ScaleType
)

/**
 * Scale type for mapping bounding boxes.
 * FILL_CROP  – matches PreviewView.ScaleType.FILL_CENTER (live camera): uses maxOf(scaleX, scaleY)
 * FIT_CENTER – matches ContentScale.Fit (static result image): uses minOf(scaleX, scaleY)
 */
enum class ScaleType { FILL_CROP, FIT_CENTER }

private data class TransformParams(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float
)

private val transformCache = hashMapOf<TransformKey, TransformParams>()

/** Clear cached transform params – call when camera restarts or preview size changes. */
fun clearTransformCache() {
    transformCache.clear()
}

/**
 * Maps a bounding box from image-coordinate space to preview/view-coordinate space.
 *
 * @param imageProxySize  The size of the source image (width × height) in image-coordinate space.
 * @param previewSize     The size of the target view (width × height) in screen pixels.
 * @param boundingBox     The bounding box in image coordinates to transform.
 * @param rotationDegrees The clockwise rotation applied by the camera sensor (0, 90, 180, 270).
 * @param scaleType       How the image fills the preview: FILL_CROP for live camera (PreviewView),
 *                        FIT_CENTER for static images (e.g. result screen).
 */
fun mapBoundsToPreview(
    imageProxySize: IntSize,
    previewSize: IntSize,
    boundingBox: Rect,
    rotationDegrees: Int,
    scaleType: ScaleType = ScaleType.FILL_CROP
): Rect {
    if (imageProxySize.width == 0 || imageProxySize.height == 0 ||
        previewSize.width == 0 || previewSize.height == 0
    ) {
        return boundingBox
    }

    val cacheKey = TransformKey(
        imageProxySize.width,
        imageProxySize.height,
        previewSize.width,
        previewSize.height,
        rotationDegrees,
        scaleType
    )

    val params = transformCache.getOrPut(cacheKey) {
        val scaleX = previewSize.width.toFloat() / imageProxySize.width
        val scaleY = previewSize.height.toFloat() / imageProxySize.height

        val scale = when (scaleType) {
            ScaleType.FILL_CROP -> maxOf(scaleX, scaleY)
            ScaleType.FIT_CENTER -> minOf(scaleX, scaleY)
        }

        val offsetX = (previewSize.width - imageProxySize.width * scale) / 2f
        val offsetY = (previewSize.height - imageProxySize.height * scale) / 2f
        TransformParams(scale = scale, offsetX = offsetX, offsetY = offsetY)
    }

    val left = boundingBox.left * params.scale + params.offsetX
    val top = boundingBox.top * params.scale + params.offsetY
    val right = boundingBox.right * params.scale + params.offsetX
    val bottom = boundingBox.bottom * params.scale + params.offsetY

    return when (rotationDegrees) {
        90 -> Rect(
            left = previewSize.width - bottom,
            top = left,
            right = previewSize.width - top,
            bottom = right
        )

        180 -> Rect(
            left = previewSize.width - right,
            top = previewSize.height - bottom,
            right = previewSize.width - left,
            bottom = previewSize.height - top
        )

        270 -> Rect(
            left = top,
            top = previewSize.height - right,
            right = bottom,
            bottom = previewSize.height - left
        )

        else -> Rect(left, top, right, bottom)
    }
}

fun PalletBox.toPalletBoxUiModel(
    resolution: IntSize,
    imageViewSize: IntSize,
    mappedBarcodes: Map<String, Int>,
    isFromAnalyzer: Boolean = false,
    scaleType: ScaleType = ScaleType.FILL_CROP,
    viewRefScale: Float = 1f
): PBoxUIModel {

    LOGD(
        TAG,
        "Pallet Box Class ID : $classId ${resolution} ${imageViewSize} ${isFromAnalyzer} scaleType=$scaleType viewRefScale=$viewRefScale"
    )

    // Avoid double-transforming preview-space coordinates already returned in VIEW_REFERENCED.
    // Instead, apply only viewRefScale to correct coordinate mismatches when Analysis and Preview resolutions differ.
    fun mapRect(rect: Rect): Rect {
        return if (isFromAnalyzer) {
            if (viewRefScale != 1f) rect.scale(viewRefScale)
            else rect
        } else {
            mapBoundsToPreview(resolution, imageViewSize, rect, 0, scaleType)
        }
    }

    val boxRect = mapRect(this.boundingBox)

    val bList = this.barcodeList.map { data ->
        val rect = mapRect(data.boundingBox)
        PBarcodeUIModel(
            data.data,
            rect,
            data.symbology,
            isMainBarcode = data.isMainBarcode,
            isQtyBarcode = data.isQtyBarcodes,
            angle = data.angle
        )
    }

    val llist = this.labelsList.map { data ->
        val overlayRect = mapRect(data.boundingBox)
        val matchedBarcodes =
            this.barcodeList.filter { data.boundingBox.toRect().contains(it.boundingBox.toRect()) }
                .map { bar ->
                    val rect = mapRect(bar.boundingBox)
                    PBarcodeUIModel(
                        bar.data,
                        rect,
                        bar.symbology,
                        isMainBarcode = bar.isMainBarcode,
                        isQtyBarcode = bar.isQtyBarcodes,
                        bar.angle
                    )
                }
        PLabelUIModel(overlayRect, matchedBarcodes)
    }


    val validQtyLabelsBarcodes = bList.filter {
        it.isQtyBarcode && it.data.isNotEmpty()
    }
    val qtyLabelsDetectedNotDecode = bList.filter {
        it.isQtyBarcode && it.data.isEmpty()
    }
    val validBigLabels = bList.filter { it.isMainBarcode }

    var expectedQty = 0
    var detectedQty = 0
    var statusOfBox = BOX_VALIDATION.NOT_DETECTED
    var qtyList: List<Int> = emptyList()
    if (validBigLabels.isNotEmpty()) {
        if (validBigLabels.size == 1) {

            val qty =
                if (mappedBarcodes.contains(validBigLabels[0].data)) mappedBarcodes[validBigLabels[0].data]
                    ?: -1 else -1

            if (qty != -1) {
                expectedQty = qty

                qtyList = validQtyLabelsBarcodes.mapNotNull { qtyBarcode ->

                    extractQtyFromData(qtyBarcode.data)?.let { return@mapNotNull it }
                    if (mappedBarcodes.contains(qtyBarcode.data)) {
                        return@mapNotNull mappedBarcodes[qtyBarcode.data]
                    }
                    if (qtyBarcode.data.isDigitsOnly()) qtyBarcode.data.toInt() else null
                }

                detectedQty = qtyList.sum()
                if (expectedQty != 0 && detectedQty == expectedQty) {
                    statusOfBox = BOX_VALIDATION.VERIFIED
                } else {
                    statusOfBox = if (detectedQty > 0) {
                        if (qtyLabelsDetectedNotDecode.isNotEmpty()) BOX_VALIDATION.PARTIAL_DETECTION else BOX_VALIDATION.MISMATCH_QTY
                    } else {
                        BOX_VALIDATION.PARTIAL_DETECTION
                    }
                }
            }

        } else {
            statusOfBox = BOX_VALIDATION.NOT_DETECTED
        }
    }

    return PBoxUIModel(
        id,
        boxRect,
        llist,
        bList,
        expectedQty,
        detectedQty,
        qtyList,
        llist.count { it.barcodes.isNotEmpty() },
        classId,
        statusOfBox,
        isLeftAligned,
        trackId,
        stableKey
    )
}


fun PalletBox.toPalletBoxUiModel(
    resolution: IntSize,
    imageViewSize: IntSize,
    isFromAnalyzer: Boolean = false,
    scaleType: ScaleType = ScaleType.FILL_CROP,
    appConfig: List<BarcodeConfig> = emptyList(),
    viewRefScale: Float = 1f
): PBoxUIModel {

    fun mapRect(rect: Rect): Rect {
        return if (isFromAnalyzer) {
            if (viewRefScale != 1f) rect.scale(viewRefScale)
            else rect
        } else {
            mapBoundsToPreview(resolution, imageViewSize, rect, 0, scaleType)
        }
    }

    val boxRect = mapRect(this.boundingBox)
    val selectedConfig = appConfig.filter { it.isSelected }

    val configMap    = linkedMapOf<String, PalletBarcode>()
    val usedBarcodes = mutableSetOf<String>()

    val barcodeConfigMatches = this.barcodeList.map { data ->
        var matchedConfig: BarcodeConfig? = null
        if (data.data.isNotEmpty()) {
            val candidate = isVerified(data, appConfig = selectedConfig)
            if (candidate != null
                && !configMap.containsKey(candidate.type)   // field not yet claimed
                && !usedBarcodes.contains(data.data)) {     // barcode not yet used
                configMap[candidate.type] = data
                usedBarcodes.add(data.data)
                matchedConfig = candidate
            }
        }
        Pair(data, matchedConfig)
    }

    val identityBarcode: PalletBarcode? = configMap[FIELD_TYPE.PRODUCT_SKU.name]

    val bList = barcodeConfigMatches.map { (data, matchedConfig) ->
        val isMain = identityBarcode != null && data == identityBarcode
        val isQty  = matchedConfig != null && matchedConfig.type == FIELD_TYPE.QTY.name
        PBarcodeUIModel(
            data          = data.data,
            boundingBox   = mapRect(data.boundingBox),
            symbology     = data.symbology,
            isMainBarcode = isMain,
            isQtyBarcode  = isQty
        )
    }

    val configNeeded  = selectedConfig.size
    val configMatched = configMap.size
    val totalDetected = bList.size
    val totalDecoded  = bList.count { it.data.isNotEmpty() }

    val statusOfBox = when {
        configNeeded == 0 ->
            BOX_VALIDATION.NOT_DETECTED

        configMatched == configNeeded ->
            BOX_VALIDATION.VERIFIED

        totalDecoded < totalDetected && configMatched < configNeeded ->
            BOX_VALIDATION.PARTIAL_DETECTION
        totalDecoded >= totalDetected && configMatched < configNeeded ->
            BOX_VALIDATION.PARTIAL_DETECTION
        else ->
            BOX_VALIDATION.NOT_DETECTED
    }

    LOGI(TAG, "=== Box $id | $statusOfBox | configMatched=$configMatched/$configNeeded | decoded=$totalDecoded/$totalDetected | stableKey=${stableKey.hashCode()}")

    if (statusOfBox == BOX_VALIDATION.MISMATCH_QTY || statusOfBox == BOX_VALIDATION.PARTIAL_DETECTION) {
        val allBarcodeData = this.barcodeList.map { "'${it.data}'" }
        val decodedBarcodeData = this.barcodeList.filter { it.data.isNotEmpty() }.map { "'${it.data}'" }
        val emptySlots = this.barcodeList.count { it.data.isEmpty() }
        LOGI("SnapDiag", "┌─── BOX $id [$statusOfBox] stableKey='${stableKey.hashCode()}' ───")
        LOGI("SnapDiag", "│ configNeeded=$configNeeded configMatched=$configMatched")
        LOGI("SnapDiag", "│ totalDetected(localized)=$totalDetected totalDecoded=$totalDecoded emptySlots=$emptySlots")
        LOGI("SnapDiag", "│ allBarcodes=${allBarcodeData.hashCode()}")
        LOGI("SnapDiag", "│ decodedOnly=${decodedBarcodeData.hashCode()}")
        selectedConfig.forEach { cfg ->
            val matched = configMap[cfg.type]
            LOGI("SnapDiag", "│ field=${cfg.type} regex='${cfg.regex}' → matched=${matched?.data?.hashCode() ?: "NONE"}")
        }
        LOGI("SnapDiag", "│ WHY YELLOW: ${if (totalDecoded >= totalDetected) "decoded($totalDecoded)>=detected($totalDetected) but configMatched($configMatched)<configNeeded($configNeeded) — deferred to wand" else "decoded($totalDecoded)<detected($totalDetected)"}")
        LOGI("SnapDiag", "└────────────────────────────────────────────")
    }
    selectedConfig.forEach { cfg ->
        val matched = configMap[cfg.type]
        LOGI(TAG, "  field=${cfg.type} regex='${cfg.regex}' → matched=${matched?.data?.hashCode() ?: "NONE"}")
    }

    return PBoxUIModel(
        id,
        boxRect,
        emptyList(),
        bList,
        0,
        0,
        emptyList(),
        0,
        classId,
        statusOfBox,
        isLeftAligned,
        trackId,
        stableKey          // always preserve the canonical key set by PalletProcessHelper
    )
}

private fun isVerified(
    data: PalletBarcode,
    appConfig: List<BarcodeConfig> = emptyList()
): BarcodeConfig? {
    if (data.data.isNotEmpty() && appConfig.isNotEmpty()) {
        val list = appConfig.filter {
            val type = FIELD_TYPE.valueOf(it.type)
            LOGI(TAG,"Box Match  : ${it.regex} ${data.data?.hashCode()}  ${data.data.startsWith(it.regex)}  ${((it.regex.isEmpty() && data.data.startsWith(type.startWith)))}")

            it.isSelected && (
                (it.regex.isEmpty() && data.data.startsWith(type.startWith))
                ||
                (it.regex.isNotEmpty() && data.data.startsWith(it.regex))
            )
        }
        return if (list.isNotEmpty()) list[0] else null
    }
    return null
}

fun Rect.toRect(): android.graphics.Rect {
    return android.graphics.Rect(
        this.left.roundToInt(),
        this.top.roundToInt(),
        this.right.roundToInt(),
        this.bottom.roundToInt()
    )
}

fun extractQtyFromData(barcode: String): Int? {

    if (barcode.isEmpty() || !barcode.startsWith(QTY_BARCODE_IDENTIFIER)) return null
    val qtyText = barcode.substringAfter(QTY_START_IDENTIFIER).substringBefore(QTY_END_IDENTIFIER)
    return if (qtyText.isDigitsOnly()) {
        LOGD(TAG, "Extracted Qty : $qtyText")
        qtyText.toInt()
    } else null
}