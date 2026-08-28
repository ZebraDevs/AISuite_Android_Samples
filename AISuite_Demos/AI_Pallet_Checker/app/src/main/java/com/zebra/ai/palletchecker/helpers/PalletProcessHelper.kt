package com.zebra.ai.palletchecker.helpers

import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toComposeRect
import com.zebra.ai.palletchecker.presentation.model.PalletBarcode
import com.zebra.ai.palletchecker.presentation.model.PalletBox
import com.zebra.ai.vision.analyzer.tracking.EntityTrackerAnalyzer
import com.zebra.ai.vision.detector.ImageData
import com.zebra.ai.vision.entity.BarcodeEntity
import com.zebra.ai.vision.entity.LocalizerEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

class PalletProcessHelper(
    private val scope: CoroutineScope
) {
    private val TAG = "PalletHlpr"
    private val executors = Executors.newSingleThreadExecutor()

    companion object {
        const val PALLET_BASE_CLASS_ID = 3
    }

    private var resultFlow = MutableSharedFlow<List<PalletBox>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val frameGateLock = Any()
    private var isFrameProcessing = false
    private var pendingFrame: Pair<ImageProxy, Boolean>? = null

    private var nextTrackId = 1L
    private var previousTrackRects = mutableMapOf<Long, Rect>()
    private var trackedPrimaryBarcode = mutableMapOf<Long, String>()

    var skuPrefix: String = ""

    fun getResults() = resultFlow

    fun analyze(image: ImageProxy, isLowResBarcode: Boolean = false) {
        synchronized(frameGateLock) {
            if (isFrameProcessing) {
                pendingFrame?.first?.close()
                pendingFrame = image to isLowResBarcode
                return
            }
            isFrameProcessing = true
        }
        processFrame(image, isLowResBarcode)
    }

    private fun processFrame(image: ImageProxy, isLowResBarcode: Boolean) {
        val startMs = System.currentTimeMillis()
        scope.launch(Dispatchers.Default) {
            try {
                val palletBoxList = mutableListOf<PalletBox>()
                val barcodeList = mutableListOf<PalletBarcode>()

                val imageData = ImageData.fromImageProxy(image)

                val barcodeProcessJob = async {
                    val barcodeDecoder = if (isLowResBarcode) ModelsStorage.getConfigBarcodeDecoder() else ModelsStorage.getBarcodeDecoder()
                    val result = barcodeDecoder?.process(imageData)?.await()
                    var decodedCount = 0
                    var emptyCount = 0

                    result?.forEach {
                        val value = it.value ?: ""
                        if (value.isNotEmpty()) decodedCount++ else emptyCount++
                        barcodeList.add(PalletBarcode(it.boundingBox.toComposeRect(), value, it.symbology))
                    }
                    // Debug logging for barcode decoding diagnostics
                    if ((result?.size ?: 0) > 0) {
                        LOGD(TAG, "ProcessFrame: isLowRes=$isLowResBarcode, total=${result?.size}, decoded=$decodedCount, empty=$emptyCount")
                    }
                }

                val palletProcessJob = async {
                    val result = ModelsStorage.getPalletAndBoxLocalizer()?.process(imageData, Dispatchers.IO.asExecutor())?.await()
                    result?.forEach {
                        palletBoxList.add(PalletBox(classId = it.classId, boundingBox = it.boundingBox.toComposeRect()))
                    }
                }

                awaitAll(barcodeProcessJob, palletProcessJob)

                LOGD(TAG, "PERFSTATS Decode Pallet and Barcode Time : ${System.currentTimeMillis() - startMs}ms | barcodes=${barcodeList.size}, boxes=${palletBoxList.size}")

                var list = convertResultToPallet(barcodeList, palletBoxList)
                list = applyDeterministicIdentity(list)
                resultFlow.tryEmit(list)
                LOGD(TAG,"Total Process Time  : ${System.currentTimeMillis() - startMs}")
            } catch (t: Throwable) {
                LOGE(TAG, "Frame processing error: ${t.message}")
            } finally {
                image.close()
                processPendingFrameIfAny()
            }
        }
    }

    private fun processPendingFrameIfAny() {
        val nextFrame: Pair<ImageProxy, Boolean>?
        synchronized(frameGateLock) {
            nextFrame = pendingFrame
            pendingFrame = null
            if (nextFrame == null) {
                isFrameProcessing = false
            }
        }
        nextFrame?.let { processFrame(it.first, it.second) }
    }

    fun analyzePallet(image: Bitmap, rotationDegrees: Int = 0) {
        scope.launch(Dispatchers.Default) {
            val list = analyzePalletSuspend(image, rotationDegrees)
            resultFlow.tryEmit(list)
        }
    }

    /**
     * Suspend variant of [analyzePallet] that returns the processed box list directly.
     *
     * Used by the multi-snap retry loop in MainViewModel so it can inspect the results
     * (check if all product boxes have a primary barcode) before deciding whether to
     * re-snap or proceed to the results screen.
     *
     * Also supplements unresolved barcodes from the live-preview cache, identical to
     * the fire-and-forget [analyzePallet].
     */
    suspend fun analyzePalletSuspend(image: Bitmap, rotationDegrees: Int = 0): List<PalletBox> {
        val startMs = System.currentTimeMillis()
        val palletBoxList = mutableListOf<PalletBox>()
        val barcodeList = mutableListOf<PalletBarcode>()
        val imageData = ImageData.fromBitmap(image, rotationDegrees)

        val barcodeProcessJob = scope.async(Dispatchers.Default) {
            val result = ModelsStorage.getBarcodeDecoder()?.process(imageData)?.await()
            result?.forEach {
                barcodeList.add(PalletBarcode(it.boundingBox.toComposeRect(), it.value, it.symbology))
            }
        }
        val palletProcessJob = scope.async(Dispatchers.Default) {
            val result = ModelsStorage.getPalletAndBoxLocalizer()?.process(imageData, Dispatchers.IO.asExecutor())?.await()
            result?.forEach {
                palletBoxList.add(PalletBox(classId = it.classId, boundingBox = it.boundingBox.toComposeRect()))
            }
        }

        awaitAll(barcodeProcessJob, palletProcessJob)
        LOGD(TAG, "PERFSTATS Decode Pallet and Barcode Time : ${System.currentTimeMillis() - startMs}ms | barcodes=${barcodeList.size}, decoded=${barcodeList.count { it.data.isNotEmpty() }}, boxes=${palletBoxList.size}")

        var list = convertResultToPallet(barcodeList, palletBoxList)
        list = applyDeterministicIdentity(list)
        LOGD(TAG, "Total Process Time  : ${System.currentTimeMillis() - startMs}")
        return list
    }

    private fun convertResultToPallet(
        barcodes: List<PalletBarcode>,
        boxes: List<PalletBox>,
        keepEmptyBoxes: Boolean = false
    ): MutableList<PalletBox> {
        var counter = 0
        val sortedBox = getSortedPallet(boxes)

        val barcodeRects = barcodes.map { it.boundingBox.toRect() }

        val decodedBarcodes = barcodes.count { it.data.isNotEmpty() }
        val productBoxes = boxes.count { it.classId != PALLET_BASE_CLASS_ID }
        LOGI(TAG, "convertResultToPallet: barcodes=${barcodes.size} (decoded=$decodedBarcodes), boxes=${boxes.size} (products=$productBoxes), keepEmpty=$keepEmptyBoxes")

        val palletBoxes = sortedBox.mapNotNull { bx ->
            if (bx.classId == PALLET_BASE_CLASS_ID) {
                return@mapNotNull PalletBox(boundingBox = bx.boundingBox, classId = bx.classId)
            }

            val boxRect = bx.boundingBox.toRect()

            val barcodeInBox = barcodes.filterIndexed { i, _ -> boxRect.contains(barcodeRects[i]) }

            val decodedInBox = barcodeInBox.count { it.data.isNotEmpty() }
            if (barcodeInBox.isNotEmpty() || keepEmptyBoxes) {
                LOGI(TAG, "  Box[${counter+1}]: ${barcodeInBox.size} barcodes inside ($decodedInBox decoded)")
            }

            when {
                barcodeInBox.isNotEmpty() -> {
                    counter++
                    PalletBox(counter, bx.boundingBox, barcodeInBox, emptyList(), emptyList(), classId = bx.classId)
                }
                keepEmptyBoxes -> {
                    counter++
                    PalletBox(counter, bx.boundingBox, emptyList(), emptyList(), emptyList(), classId = bx.classId)
                }
                else -> null
            }
        }
        return palletBoxes.toMutableList()
    }

    private fun applyDeterministicIdentity(input: List<PalletBox>): MutableList<PalletBox> {
        if (input.isEmpty()) {
            synchronized(this) {
                previousTrackRects.clear()
            }
            return mutableListOf()
        }

        val usedIds = hashSetOf<Long>()
        val previousRectSnapshot = synchronized(this) {
            previousTrackRects.toMap()
        }

        val currentRects = mutableMapOf<Long, Rect>()

        val updated = input.map { box ->
            val matchedTrackId = findBestTrackId(box.boundingBox, previousRectSnapshot, usedIds)
            val trackId = matchedTrackId ?: nextTrackId++
            usedIds.add(trackId)

            val stableBarcodeList = stabilizeMainBarcodeForTrack(trackId, box.barcodeList)
            val stableKey = stableBarcodeList.firstOrNull { it.isMainBarcode }?.data?.takeIf { it.isNotEmpty() }
                ?: "track-$trackId"

            currentRects[trackId] = box.boundingBox
            box.copy(
                id = trackId.toInt(),
                barcodeList = stableBarcodeList,
                trackId = trackId,
                stableKey = stableKey
            )
        }

        synchronized(this) {
            previousTrackRects = currentRects
        }
        return updated.toMutableList()
    }

    /**
     * Stabilizes the primary (PRODUCT_SKU) barcode for a given trackId across frames.
     *
     * Selection strategy (in priority order):
     *   1. Previously tracked value — if it still appears in the current frame's barcodes
     *   2. Config-criteria match — first decoded barcode whose value starts with [skuPrefix]
     *   3. No primary — if no barcode matches the SKU prefix, none is marked as main
     *
     * The legacy width/position heuristic (45%/55%) has been removed. Primary barcode
     * identification now relies exclusively on the user-configured PRODUCT_SKU prefix.
     */
    private fun stabilizeMainBarcodeForTrack(trackId: Long, barcodes: List<PalletBarcode>): List<PalletBarcode> {
        if (barcodes.isEmpty()) return barcodes

        val trackedValue = trackedPrimaryBarcode[trackId]

        val currentDecodedValues = barcodes.filter { it.data.isNotEmpty() }.map { it.data }.toSet()
        if (!trackedValue.isNullOrEmpty() && trackedValue !in currentDecodedValues) {
            trackedPrimaryBarcode.remove(trackId)
        }

        val validTrackedValue = trackedPrimaryBarcode[trackId]
        val selected: PalletBarcode? = when {
            !validTrackedValue.isNullOrEmpty() ->
                barcodes.firstOrNull { it.data == validTrackedValue }
            skuPrefix.isNotEmpty() ->
                barcodes.firstOrNull { it.data.isNotEmpty() && it.data.startsWith(skuPrefix) }
            else -> null
        }

        val selectedData = selected?.data
        if (!selectedData.isNullOrEmpty()) {
            trackedPrimaryBarcode[trackId] = selectedData
        }

        return barcodes.map { bar ->
            bar.copy(
                isMainBarcode = !selectedData.isNullOrEmpty() && bar.data == selectedData,
                isQtyBarcodes = false,
                angle = 0.0
            )
        }
    }

    fun getSortedPallet(boxes: List<PalletBox>): List<PalletBox> {
        return boxes.sortedWith(Comparator { r1, r2 ->
            if (kotlin.math.abs(r1.boundingBox.top - r2.boundingBox.top) <= 40) {
                r1.boundingBox.left.compareTo(r2.boundingBox.left)
            } else {
                r1.boundingBox.top.compareTo(r2.boundingBox.top)
            }
        })
    }

    private fun findBestTrackId(current: Rect, previous: Map<Long, Rect>, usedIds: Set<Long> = emptySet()): Long? {
        var bestId: Long? = null
        var bestIou = 0f
        for ((id, oldRect) in previous) {
            if (id in usedIds) continue
            val iou = intersectionOverUnion(current, oldRect)
            if (iou > bestIou) {
                bestIou = iou
                bestId = id
            }
        }
        return if (bestIou >= 0.3f) bestId else null
    }

    private fun intersectionOverUnion(a: Rect, b: Rect): Float {
        val left = max(a.left, b.left)
        val top = max(a.top, b.top)
        val right = min(a.right, b.right)
        val bottom = min(a.bottom, b.bottom)

        if (right <= left || bottom <= top) return 0f

        val inter = (right - left) * (bottom - top)
        val areaA = a.width * a.height
        val areaB = b.width * b.height
        val union = areaA + areaB - inter

        return if (union <= 0f) 0f else inter / union
    }

    fun clear() {
        resultFlow.tryEmit(emptyList())
        previousTrackRects.clear()
        trackedPrimaryBarcode.clear()
        nextTrackId = 1L
        synchronized(frameGateLock) {
            pendingFrame?.first?.close()
            pendingFrame = null
            isFrameProcessing = false
        }
    }

    fun createEntityPalletAnalyzer(): EntityTrackerAnalyzer {
        val palletAndBoxLocalizer = ModelsStorage.getPalletAndBoxLocalizer()
        val barcode = ModelsStorage.getConfigBarcodeDecoder()

        return EntityTrackerAnalyzer(
            listOfNotNull(palletAndBoxLocalizer, barcode),
            ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
            executors,
            { entities ->
                val palletBoxList = mutableListOf<PalletBox>()
                val barcodeList = mutableListOf<PalletBarcode>()

                barcode?.let { barcodeDetector ->
                    val barcodeEntities = entities.getValue(barcodeDetector)
                    val totalBarcodes = barcodeEntities?.size ?: 0
                    var decodedCount = 0
                    var emptyCount = 0

                    barcodeEntities?.forEach {
                        if (it is BarcodeEntity) {
                            val value = it.value ?: ""
                            if (value.isNotEmpty()) decodedCount++ else emptyCount++
                            barcodeList.add(PalletBarcode(it.boundingBox.toComposeRect(), value, it.symbology))
                        }
                    }

                    if (totalBarcodes > 0) {
                        LOGI(TAG, "PERFSTATS Wanding: totalbarcodes =$totalBarcodes, decoded=$decodedCount, empty=$emptyCount")
                    }
                }

                palletAndBoxLocalizer?.let { palletAndBoxDetector ->
                    entities.getValue(palletAndBoxDetector)?.forEach {
                        if (it is LocalizerEntity) {
                            palletBoxList.add(PalletBox(classId = it.classId, boundingBox = it.boundingBox.toComposeRect()))
                        }
                    }
                }

                var list = convertResultToPallet(barcodeList, palletBoxList, keepEmptyBoxes = true)
                list = applyDeterministicIdentity(list)
                resultFlow.tryEmit(list)
            }
        )
    }
}
