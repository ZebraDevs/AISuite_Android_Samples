package com.zebra.ai.palletchecker.presentation.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zebra.ai.palletchecker.domain.model.BarcodeConfig
import com.zebra.ai.palletchecker.domain.model.FIELD_TYPE
import com.zebra.ai.palletchecker.helpers.BARCODE_VOTE_THRESHOLD
import com.zebra.ai.palletchecker.helpers.LOGD
import com.zebra.ai.palletchecker.helpers.LOGE
import com.zebra.ai.palletchecker.helpers.LOGI
import com.zebra.ai.palletchecker.helpers.ModelsStorage
import com.zebra.ai.palletchecker.helpers.PalletProcessHelper
import com.zebra.ai.palletchecker.helpers.SNAP_VOTE_SEED
import com.zebra.ai.palletchecker.helpers.SpatialBoxNode
import com.zebra.ai.palletchecker.helpers.SpatialMapBuilder
import com.zebra.ai.palletchecker.presentation.model.BOX_VALIDATION
import com.zebra.ai.palletchecker.presentation.model.PBoxUIModel
import com.zebra.ai.palletchecker.presentation.model.PalletBarcode
import com.zebra.ai.palletchecker.presentation.model.PalletBox
import com.zebra.ai.palletchecker.presentation.model.PalletSession
import com.zebra.ai.palletchecker.presentation.model.SessionMode
import com.zebra.ai.vision.detector.AIVisionSDK
import com.zebra.ai.vision.detector.AIVisionSDKException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.Locale

/** State emitted by the wand session countdown timer. */
data class WandTimerState(
    /** Total duration of the wand session in seconds. */
    val totalSeconds: Int = WAND_SESSION_DURATION_SECONDS,
    /** Seconds remaining; counts down from totalSeconds to 0. */
    val remainingSeconds: Int = WAND_SESSION_DURATION_SECONDS,
    /** True once the timer has reached 0. */
    val isExpired: Boolean = false
) {
    val progress: Float
        get() = if (totalSeconds == 0) 0f else remainingSeconds.toFloat() / totalSeconds.toFloat()
}

/** Default wand session duration in seconds. */
const val WAND_SESSION_DURATION_SECONDS = 30

class MainViewModel(val app: Application) : AndroidViewModel(app) {

    private val TAG = "MVM"
    var modelInitState = MutableStateFlow(false)
    private var mPalletHelper: PalletProcessHelper? = null

    private var palletSession: MutableStateFlow<PalletSession> = MutableStateFlow(PalletSession())
    private var cachedPalletBox: HashMap<String, PBoxUIModel> = hashMapOf()
    private var _palletResults = MutableStateFlow<List<PalletBox>>(emptyList())
    private var palletResults: StateFlow<List<PalletBox>> = _palletResults
    private var isCaptureImageResults = false

    private val auditTargets = linkedMapOf<String, AuditStatus>()

    /**
     * Single source of truth for per-box validation shown in the PiP thumbnail.
     * Key = stableKey of the spatial node. Value = current best BOX_VALIDATION.
     * Only ever upgraded — never downgraded.
     */
    private val _pipValidationState = MutableStateFlow<Map<String, BOX_VALIDATION>>(emptyMap())
    val pipValidationState: StateFlow<Map<String, BOX_VALIDATION>> = _pipValidationState

    /** Wand session countdown timer state. */
    private val _wandTimerState = MutableStateFlow(WandTimerState())
    val wandTimerState: StateFlow<WandTimerState> = _wandTimerState
    private var timerJob: Job? = null

    /**
     * Starts the wand session countdown timer.
     * Call when entering wand mode. Ticks every second.
     * When it reaches 0 it sets [WandTimerState.isExpired] = true.
     * Also used to RESET the timer in SHELF_CONTINUOUS mode on every new box discovery.
     */
    fun startWandTimer(durationSeconds: Int = WAND_SESSION_DURATION_SECONDS) {
        cancelWandTimer()
        _wandTimerState.value = WandTimerState(totalSeconds = durationSeconds, remainingSeconds = durationSeconds)
        timerJob = viewModelScope.launch {
            for (remaining in durationSeconds downTo 0) {
                _wandTimerState.value = _wandTimerState.value.copy(remainingSeconds = remaining)
                if (remaining == 0) {
                    _wandTimerState.value = _wandTimerState.value.copy(isExpired = true)
                    break
                }
                delay(1_000L)
            }
        }
        LOGD(TAG, "Wand timer started/reset: ${durationSeconds}s")
    }

    /** Cancels an in-progress wand timer without marking it expired. */
    fun cancelWandTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    /**
     * Called when the wand timer expires before all boxes are resolved.
     * Finalises the session leaving unresolved yellow boxes as PARTIAL_DETECTION.
     */
    fun expireWandSession() {
        cancelWandTimer()
        finaliseWandSession()
        LOGD(TAG, "Wand session expired — finalised with unresolved yellows preserved")
    }

    init {
        viewModelScope.launch {
            initSDK()
        }
    }

    fun initSDK() {
        try {
            val init = AIVisionSDK.getInstance(app).init()
        } catch (e: AIVisionSDKException) {
            LOGE(TAG, "SDK Initialize Exception")
        }
    }

    fun initModels(model: () -> SettingsViewModel) {
        viewModelScope.launch {
            disposeModels()
            LOGD(TAG, "Model Init before modelsInit")
            ModelsStorage.initializeAllModels({ model().settings.value }, { model().wandSettings.value })
            LOGD(TAG, "Model Init after modelsInit")
        }
        mPalletHelper = PalletProcessHelper(viewModelScope)
        observeModelInitState()
        observePalletResults()
    }

    private fun disposeModels() {
        modelInitState.value = false
        if (ModelsStorage.isAlreadyAvailable()) {
            ModelsStorage.stop()
        }
    }

    private fun observeModelInitState() {
        viewModelScope.launch {
            ModelsStorage.modelInitiate.collectLatest { state ->
                modelInitState.value = state
            }
        }
    }

    private fun observePalletResults() {
        viewModelScope.launch {
            mPalletHelper?.getResults()?.collectLatest { boxes ->
                if (isCaptureImageResults) {
                    capturePalletResultsToSession(boxes)
                    isCaptureImageResults = false
                }
                _palletResults.value = capToExpectedBoxes(boxes)
            }
        }
    }

    fun getPalletResults() = palletResults
    fun getPalletSession() = palletSession

    fun captureImageToSession(lastCaptureImage: Bitmap?, lastCaptureImgRotation: Int = 0) {
        palletSession.value = palletSession.value.copy(
            storedPalletCaptureImage = lastCaptureImage,
            storedImageRotation = lastCaptureImgRotation
        )
    }

    private fun capturePalletResultsToSession(list: List<PalletBox>) {
        val capped = capToExpectedBoxes(list)
        palletSession.value = palletSession.value.copy(storedPalletDetails = capped)
    }

    /**
     * Caps a list of PalletBox to at most [PalletSession.expectedBoxes] product boxes.
     * Pallet-base entries (classId == 3) are excluded from the count and always retained.
     * If expectedBoxes is 0 (not yet set), the list is returned unchanged.
     */
    private fun capToExpectedBoxes(list: List<PalletBox>): List<PalletBox> {
        val limit = palletSession.value.expectedBoxes
        if (limit <= 0) return list
        val baseBoxes = list.filter { it.classId == PalletProcessHelper.PALLET_BASE_CLASS_ID }
        val productBoxes = list.filter { it.classId != PalletProcessHelper.PALLET_BASE_CLASS_ID }
        return if (productBoxes.size <= limit) list
        else baseBoxes + productBoxes.take(limit)
    }

    fun processCapturedImage() {
        palletSession.value.storedPalletCaptureImage?.let {
            mPalletHelper?.analyzePallet(it)
        }
    }

    /**
     * Result of a single snap attempt.
     * @param allPrimaryFound True when every detected product box has a decoded primary barcode.
     * @param productBoxCount Number of product boxes detected (excluding pallet base).
     * @param primaryFoundCount Number of product boxes that have a decoded primary (SKU) barcode.
     * @param results The processed PalletBox list from this snap.
     */
    data class SnapAttemptResult(
        val allPrimaryFound: Boolean,
        val productBoxCount: Int,
        val primaryFoundCount: Int,
        val results: List<PalletBox>
    )

    /**
     * Processes a snap bitmap and checks whether all product boxes have their primary
     * (PRODUCT_SKU) barcode decoded.
     *
     * When [previousResults] is non-null, barcodes decoded in earlier snap attempts are
     * merged into the current results via centroid-proximity matching. This means each
     * subsequent snap builds on the cumulative knowledge of all prior snaps, so even if
     * any single snap fails to decode a particular barcode, a prior snap that succeeded
     * for that barcode will fill it in.
     *
     * @param bitmap The captured high-res snap bitmap.
     * @param previousResults Results from earlier snap attempts (for merging). Null on first attempt.
     * @return SnapAttemptResult with completeness info and processed boxes.
     */
    suspend fun processSnapAndCheckComplete(
        bitmap: Bitmap,
        previousResults: List<PalletBox>?
    ): SnapAttemptResult {
        val helper = mPalletHelper ?: return SnapAttemptResult(false, 0, 0, emptyList())

        var results = helper.analyzePalletSuspend(bitmap)

        if (previousResults != null && previousResults.isNotEmpty()) {
            results = mergeSnapBarcodes(results, previousResults, bitmap.width, bitmap.height)
        }

        val skuPrefix = helper.skuPrefix
        results = results.map { box ->
            if (box.classId == PalletProcessHelper.PALLET_BASE_CLASS_ID) return@map box


            val primaryBarcode = if (skuPrefix.isNotEmpty()) {
                box.barcodeList.firstOrNull { it.data.isNotEmpty() && it.data.startsWith(skuPrefix) }
            } else {
                box.barcodeList.firstOrNull { it.isMainBarcode && it.data.isNotEmpty() }
            }

            if (primaryBarcode == null) return@map box

            val updatedBarcodes = box.barcodeList.map { barcode ->
                barcode.copy(isMainBarcode = barcode.data.isNotEmpty() && barcode.data == primaryBarcode.data)
            }

            val newStableKey = primaryBarcode.data.takeIf { it.isNotEmpty() }
                ?: box.stableKey

            box.copy(
                barcodeList = updatedBarcodes,
                stableKey = newStableKey
            )
        }

        val productResults = results.filter { it.classId != PalletProcessHelper.PALLET_BASE_CLASS_ID }
        LOGI("SnapDiag", "═══ Post-merge snap summary: ${productResults.size} product boxes (skuPrefix='$skuPrefix') ═══")
        productResults.forEachIndexed { idx, box ->
            val decoded = box.barcodeList.filter { it.data.isNotEmpty() }
            val empty = box.barcodeList.count { it.data.isEmpty() }
            val mainBc = box.barcodeList.firstOrNull { it.isMainBarcode && it.data.isNotEmpty() }
            val hasPrimary = mainBc != null

            val dupes = decoded.groupBy { it.data }.filter { it.value.size > 1 }
            LOGI("SnapDiag", "  Box[${idx+1}] stableKey='${box.stableKey.hashCode()}' hasPrimary=$hasPrimary primary='${mainBc?.data?.hashCode() ?: ""}' decoded=${decoded.size} empty=$empty total=${box.barcodeList.size}")
            if (dupes.isNotEmpty()) {
                dupes.forEach { (data, list) ->
                    LOGI("SnapDiag", "    ⚠ DUPLICATE barcode '${data.hashCode()}' appears ${list.size}x in this box")
                }
            }
            decoded.forEach { bc ->
                LOGI("SnapDiag", "    bc='${bc.data.hashCode()}' isMain=${bc.isMainBarcode} isQty=${bc.isQtyBarcodes}")
            }
        }

        val capped = capToExpectedBoxes(results)

        isCaptureImageResults = false
        palletSession.value = palletSession.value.copy(storedPalletDetails = capped)
        _palletResults.value = capped

        val productBoxes = capped.filter { it.classId != PalletProcessHelper.PALLET_BASE_CLASS_ID }
        val primaryFoundCount = productBoxes.count { box ->
            box.barcodeList.any { barcode ->
                barcode.data.isNotEmpty() && (
                    skuPrefix.isEmpty() || barcode.data.startsWith(skuPrefix)
                )
            }
        }

        val allFound = productBoxes.isNotEmpty() && primaryFoundCount == productBoxes.size
        LOGI(TAG, "SnapCheck: attempt result — ${primaryFoundCount}/${productBoxes.size} boxes have primary barcode (allFound=$allFound, skuPrefix='$skuPrefix')")
        return SnapAttemptResult(allFound, productBoxes.size, primaryFoundCount, capped)
    }

    /**
     * Merges barcodes from [previousResults] into [currentResults] by matching boxes
     * that occupy the same spatial position (centroid proximity in normalized [0,1] space).
     *
     * For each box in [currentResults] that has an unresolved (empty) barcode, finds the
     * closest box in [previousResults] and copies over any decoded barcodes that the
     * current snap missed.
     */
    private fun mergeSnapBarcodes(
        currentResults: List<PalletBox>,
        previousResults: List<PalletBox>,
        imageWidth: Int,
        imageHeight: Int
    ): List<PalletBox> {
        if (previousResults.isEmpty()) return currentResults
        val w = imageWidth.toFloat().coerceAtLeast(1f)
        val h = imageHeight.toFloat().coerceAtLeast(1f)

        data class PrevEntry(val box: PalletBox, val cx: Float, val cy: Float)
        val prevEntries = previousResults
            .filter { it.classId != PalletProcessHelper.PALLET_BASE_CLASS_ID }
            .map { PrevEntry(it, it.boundingBox.center.x / w, it.boundingBox.center.y / h) }

        if (prevEntries.isEmpty()) return currentResults

        val maxNormDistSq = 0.12f * 0.12f

        return currentResults.map { box ->
            if (box.classId == PalletProcessHelper.PALLET_BASE_CLASS_ID) return@map box

            val cx = box.boundingBox.center.x / w
            val cy = box.boundingBox.center.y / h

            var bestPrev: PrevEntry? = null
            var bestDistSq = maxNormDistSq
            for (prev in prevEntries) {
                val dx = cx - prev.cx
                val dy = cy - prev.cy
                val distSq = dx * dx + dy * dy
                if (distSq < bestDistSq) {
                    bestDistSq = distSq
                    bestPrev = prev
                }
            }

            if (bestPrev == null) return@map box

            val currentData = box.barcodeList.mapNotNull { it.data.takeIf { d -> d.isNotEmpty() } }.toMutableSet()
            val newBarcodes = bestPrev.box.barcodeList.filter { prevBarcode ->
                prevBarcode.data.isNotEmpty() && prevBarcode.data !in currentData
            }

            if (newBarcodes.isEmpty()) return@map box

            val mergedList = box.barcodeList.toMutableList()

            val usedPrevIndices = mutableSetOf<Int>()

            for (i in mergedList.indices) {
                if (mergedList[i].data.isNotEmpty()) continue
                val snapCx = mergedList[i].boundingBox.center.x / w
                val snapCy = mergedList[i].boundingBox.center.y / h
                var closestPrevIdx = -1
                var closestPrevBarcode: PalletBarcode? = null
                var closestDist = 0.10f * 0.10f
                for ((pi, prevBarcode) in bestPrev.box.barcodeList.withIndex()) {
                    if (pi in usedPrevIndices) continue
                    if (prevBarcode.data.isEmpty()) continue
                    if (prevBarcode.data in currentData) continue
                    val pdx = snapCx - prevBarcode.boundingBox.center.x / w
                    val pdy = snapCy - prevBarcode.boundingBox.center.y / h
                    val pdist = pdx * pdx + pdy * pdy
                    if (pdist < closestDist) {
                        closestDist = pdist
                        closestPrevBarcode = prevBarcode
                        closestPrevIdx = pi
                    }
                }
                if (closestPrevBarcode != null) {
                    mergedList[i] = mergedList[i].copy(
                        data = closestPrevBarcode.data,
                        isMainBarcode = closestPrevBarcode.isMainBarcode
                    )
                    usedPrevIndices.add(closestPrevIdx)
                    currentData.add(closestPrevBarcode.data)     // prevent same value in another slot
                    LOGI(TAG, "SnapMerge: filled barcode '${closestPrevBarcode.data}' from previous snap (isMain=${closestPrevBarcode.isMainBarcode})")
                }
            }


            val alreadyMergedData = mergedList.mapNotNull { it.data.takeIf { d -> d.isNotEmpty() } }.toSet()
            val additionalBarcodes = newBarcodes.filter { it.data !in alreadyMergedData }


            val finalList = mergedList + additionalBarcodes
            val finalDecoded = finalList.filter { it.data.isNotEmpty() }.map { it.data }
            val dupes = finalDecoded.groupBy { it }.filter { it.value.size > 1 }
            LOGI("SnapDiag", "SnapMerge Box[${box.id}] key='${box.stableKey.hashCode()}': slotFills=${mergedList.count { it.data.isNotEmpty() } - box.barcodeList.count { it.data.isNotEmpty() }}, appended=${additionalBarcodes.size}, finalDecoded=${finalDecoded.size}/${finalList.size}")
            if (dupes.isNotEmpty()) {
                dupes.forEach { (data, list) ->
                    LOGI("SnapDiag", "  ⚠ DUPLICATE after merge: '$data' appears ${list.size}x")
                }
            }

            box.copy(barcodeList = finalList)
        }
    }

    fun getCachedPalletBoxes() = cachedPalletBox

    /**
     * Returns true when the audit session is complete.
     *
     * PALLET_FINITE:
     *   - Session ends when ALL audit-required nodes (registered in auditTargets) are COMPLETED.
     *   - auditTargets is never empty in a real wand session because updatePartialDetections
     *     only enters wand mode when there are non-VERIFIED snap boxes.
     *   - Edge case: if somehow auditTargets is empty (zero pending boxes), we still require
     *     at least expectedBoxes total boxes to have been seen during snap before declaring done.
     *
     * SHELF_CONTINUOUS:
     *   - Session ends when the count of VALIDATED (audit-completed) nodes equals expectedBoxes.
     *   - Nodes are added dynamically; simply being discovered (having a primary barcode) is NOT
     *     sufficient — the node must have been wanded and reached a final state (GREEN or RED).
     *   - This prevents premature exit when snap pre-populated some nodes with SKU barcodes.
     */
    fun isAuditDone(expectedBoxes: Int): Boolean {
        val mode = palletSession.value.sessionMode
        return if (mode == SessionMode.SHELF_CONTINUOUS) {

            val completedCount = palletSession.value.spatialMap?.nodes
                ?.count { it.isAuditCompleted && (it.wandValidation == BOX_VALIDATION.VERIFIED || it.wandValidation == BOX_VALIDATION.MISMATCH_QTY) } ?: 0
            LOGI(TAG, "isAuditDone SHELF: completedCount=$completedCount / expectedBoxes=$expectedBoxes")
            completedCount >= expectedBoxes
        } else {
            if (auditTargets.isEmpty()) {
                val snapBoxCount = palletSession.value.storedPalletDetails
                    .count { it.classId != PalletProcessHelper.PALLET_BASE_CLASS_ID }
                LOGI(TAG, "isAuditDone PALLET(empty targets): snapBoxCount=$snapBoxCount / expectedBoxes=$expectedBoxes")
                return snapBoxCount >= expectedBoxes
            }

            val spatialMap = palletSession.value.spatialMap
            if (spatialMap != null) {
                for ((key, status) in auditTargets) {
                    if (status == AuditStatus.COMPLETED) continue
                    val node = spatialMap.nodes.find { it.stableKey == key }
                    if (node != null && node.isAuditCompleted) {
                        auditTargets[key] = AuditStatus.COMPLETED
                        LOGI(TAG, "isAuditDone SYNC: '$key' node.isAuditCompleted=true but auditTarget was $status → COMPLETED")
                    }
                }
            }

            val allComplete = auditTargets.values.all { it == AuditStatus.COMPLETED }


            val spatialCompleted = spatialMap?.nodes
                ?.count { it.isAuditCompleted && (it.wandValidation == BOX_VALIDATION.VERIFIED || it.wandValidation == BOX_VALIDATION.MISMATCH_QTY) }
                ?: 0

            val snapVerified = spatialMap?.nodes
                ?.count { node ->
                    !node.isAuditRequired
                        && node.snapValidation == BOX_VALIDATION.VERIFIED
                        && !(node.isAuditCompleted && (node.wandValidation == BOX_VALIDATION.VERIFIED || node.wandValidation == BOX_VALIDATION.MISMATCH_QTY))
                }
                ?: 0
            val totalResolved = spatialCompleted + snapVerified

            LOGI(TAG, "isAuditDone PALLET: allComplete=$allComplete auditTargets=${auditTargets.size}, " +
                    "spatialCompleted=$spatialCompleted snapVerified=$snapVerified totalResolved=$totalResolved expectedBoxes=$expectedBoxes")

            LOGI("WandDiag", "isAuditDone check: allComplete=$allComplete totalResolved=$totalResolved expected=$expectedBoxes")
            auditTargets.forEach { (key, status) ->
                val node = spatialMap?.nodes?.find { it.stableKey == key }
                LOGI("WandDiag", "  target='$key' status=$status node=${node?.let { "idx=${it.spatialIndex} completed=${it.isAuditCompleted} wandVal=${it.wandValidation} snapVal=${it.snapValidation}" } ?: "NOT_FOUND"}")
            }

            allComplete || totalResolved >= expectedBoxes
        }
    }

    /**
     * Scans [newBarcodes] for a value matching the PRODUCT_SKU config prefix.
     * If found, registers the SKU value in [nodeByBarcode] so future tier-1 resolution
     * finds this node correctly even though the node's original stableKey is "track-N".
     *
     * IMPORTANT: We ONLY update the spatial map's barcode index here.
     * We do NOT create new entries in [auditTargets] or [_pipValidationState] —
     * those remain keyed on the node's immutable [stableKey] ("track-N") and are
     * updated by [updatePipState] using that same key. Creating duplicate entries
     * under a promoted key would leave dangling PENDING targets that never get
     * marked COMPLETED, blocking [isAuditDone].
     */
    private fun tryPromotePrimaryBarcode(
        node: SpatialBoxNode,
        newBarcodes: Set<String>,
        appConfig: List<BarcodeConfig>
    ) {
        val skuConfig = appConfig.filter { it.isSelected }
            .firstOrNull { it.type == FIELD_TYPE.PRODUCT_SKU.name } ?: return

        val skuBarcode = newBarcodes.firstOrNull { barcode ->
            if (skuConfig.regex.isNotEmpty()) barcode.startsWith(skuConfig.regex)
            else barcode.startsWith(FIELD_TYPE.PRODUCT_SKU.startWith)
        } ?: return

        val spatialMap = palletSession.value.spatialMap ?: return
        if (spatialMap.nodeByBarcode.containsKey(skuBarcode)) return

        spatialMap.nodeByBarcode[skuBarcode] = node
        spatialMap.nodeByBarcode["barcode:$skuBarcode"] = node

        LOGI(TAG, "Tier-1 promoted: node[${node.spatialIndex}] stableKey='${node.stableKey}' → barcode index added for '$skuBarcode'")
    }

    /**
     * Adds a new spatial node dynamically during SHELF_CONTINUOUS wand mode when a
     * previously unseen unique primary barcode is found.
     *
     * Scans ALL decoded barcodes on the live box for a PRODUCT_SKU config match —
     * not just the isMainBarcode flag, which is set by a spatial heuristic in
     * PalletProcessHelper and may pick the wrong barcode.
     *
     * @param box Live wand-frame box
     * @param appConfig Current barcode config (used to identify the SKU barcode)
     * @return true if a new node was added, false if this SKU was already registered
     */
    fun addShelfNodeFromWand(box: PBoxUIModel, appConfig: List<BarcodeConfig> = emptyList()): Boolean {
        val map = palletSession.value.spatialMap ?: return false

        val skuConfig = appConfig.filter { it.isSelected }
            .firstOrNull { it.type == FIELD_TYPE.PRODUCT_SKU.name }

        val primaryBarcode: String? = if (skuConfig != null) {
            box.palletBarcodes.firstOrNull { pb ->
                pb.data.isNotEmpty() && (
                    if (skuConfig.regex.isNotEmpty()) pb.data.startsWith(skuConfig.regex)
                    else pb.data.startsWith(FIELD_TYPE.PRODUCT_SKU.startWith)
                )
            }?.data
        } else {
            box.palletBarcodes.firstOrNull { it.isMainBarcode && it.data.isNotEmpty() }?.data
        }

        if (primaryBarcode.isNullOrEmpty()) return false

        val correctedBox = box.copy(
            palletBarcodes = box.palletBarcodes.map { pb ->
                pb.copy(isMainBarcode = (pb.data == primaryBarcode))
            }
        )

        val newNode = map.addShelfNode(correctedBox, appConfig) ?: return false

        auditTargets[newNode.stableKey] = AuditStatus.PENDING

        val newNodeBarcodes = correctedBox.palletBarcodes
            .filter { it.data.isNotEmpty() }
            .map { it.data }
            .toSet()

        val updatedDetails = palletSession.value.storedPalletDetails.toMutableList()
        val orphanedSnapBoxes = updatedDetails.filter { existingBox ->
            val hasNoPrimary = existingBox.barcodeList.none { it.isMainBarcode && it.data.isNotEmpty() }
            if (!hasNoPrimary) return@filter false

            val existsInSpatialMap = map.nodes.any { it.stableKey == existingBox.stableKey }
            if (existsInSpatialMap) return@filter false

            val existingBarcodes = existingBox.barcodeList
                .filter { it.data.isNotEmpty() }
                .map { it.data }
                .toSet()
            val hasOverlappingBarcodes = existingBarcodes.intersect(newNodeBarcodes).isNotEmpty()
            val hasMatchingTrackId = box.trackId >= 0 && existingBox.trackId == box.trackId

            hasOverlappingBarcodes || hasMatchingTrackId
        }

        if (orphanedSnapBoxes.isNotEmpty()) {
            val orphanedKeys = orphanedSnapBoxes.map { it.stableKey }.toSet()
            updatedDetails.removeAll { it.stableKey in orphanedKeys }

            val currentPip = _pipValidationState.value.toMutableMap()
            orphanedKeys.forEach { key ->
                currentPip.remove(key)
                auditTargets.remove(key)
            }
            _pipValidationState.value = currentPip

            LOGI(TAG, "Shelf: removed ${orphanedSnapBoxes.size} orphaned snap box(es): ${orphanedKeys.hashCode()}")
        }

        val currentPip = _pipValidationState.value
        _pipValidationState.value = currentPip + (newNode.stableKey to BOX_VALIDATION.NOT_DETECTED)

        val newPalletBox = PalletBox(
            id = newNode.spatialIndex,
            boundingBox = newNode.boundingBox,
            barcodeList = correctedBox.palletBarcodes.map { pb ->
                PalletBarcode(
                    boundingBox = pb.boundingBox,
                    data = pb.data,
                    symbology = pb.symbology,
                    isMainBarcode = pb.isMainBarcode,
                    isQtyBarcodes = pb.isQtyBarcode,
                    angle = pb.angle
                )
            },
            classId = box.classId,
            validation = BOX_VALIDATION.NOT_DETECTED,
            trackId = box.trackId,
            stableKey = newNode.stableKey
        )
        updatedDetails.add(newPalletBox)
        palletSession.value = palletSession.value.copy(storedPalletDetails = updatedDetails)

        LOGI(TAG, "Shelf: added new node index=${newNode.spatialIndex} barcode=${primaryBarcode.hashCode()}")
        return true
    }

    /**
     * Adds a new spatial node dynamically during PALLET_FINITE wand mode when a
     * previously unseen unique primary barcode is discovered.
     *
     * This extends the spatial map to handle boxes that were:
     *  - Hidden/occluded during snap phase (behind other boxes)
     *  - Missed by snap detection (edge of frame, poor lighting)
     *  - Added to pallet after snap was taken
     *
     * Unlike SHELF_CONTINUOUS mode, this does NOT add the box to storedPalletDetails,
     * so it won't appear in the PIP overlay (which shows snap-phase ground truth).
     * The dynamic box will appear in the FINAL RESULTS screen after wand completes.
     *
     * Includes robust duplicate detection:
     *  - Primary barcode uniqueness check
     *  - Spatial overlap check (IoU > 0.5) to avoid re-adding same box from different angles
     *  - Orphaned snap-box cleanup (yellow boxes that now have a primary barcode)
     *
     * @param box Live wand-frame box
     * @param appConfig Current barcode config (used to identify the SKU barcode)
     * @return true if a new node was added, false if this SKU was already registered or overlaps with existing node
     */
    fun addPalletFiniteNodeFromWand(box: PBoxUIModel, appConfig: List<BarcodeConfig> = emptyList()): Boolean {
        val map = palletSession.value.spatialMap ?: return false

        val skuConfig = appConfig.filter { it.isSelected }
            .firstOrNull { it.type == FIELD_TYPE.PRODUCT_SKU.name }

        val primaryBarcode: String? = if (skuConfig != null) {
            box.palletBarcodes.firstOrNull { pb ->
                pb.data.isNotEmpty() && (
                    if (skuConfig.regex.isNotEmpty()) pb.data.startsWith(skuConfig.regex)
                    else pb.data.startsWith(FIELD_TYPE.PRODUCT_SKU.startWith)
                )
            }?.data
        } else {
            box.palletBarcodes.firstOrNull { it.isMainBarcode && it.data.isNotEmpty() }?.data
        }

        if (primaryBarcode.isNullOrEmpty()) return false

        if (map.nodeByBarcode.containsKey(primaryBarcode) ||
            map.nodeByBarcode.containsKey("barcode:$primaryBarcode")) {
            return false
        }

        val unbarcodedCandidates = map.nodes.filter { existingNode ->
            existingNode.isAuditRequired && !existingNode.isAuditCompleted && !existingNode.hasPrimaryBarcode
        }
        val existingUnbarcoded = if (unbarcodedCandidates.isNotEmpty()) {
            val liveSourceSize = androidx.compose.ui.unit.IntSize(
                box.boundingBox.width.toInt().coerceAtLeast(1),
                box.boundingBox.height.toInt().coerceAtLeast(1)
            )
            var bestNode: SpatialBoxNode? = null
            var bestIoU = 0f
            for (candidate in unbarcodedCandidates) {
                val iou = if (map.sourceResolution.width > 0 && liveSourceSize.width > 0) {
                    map.computeNormalizedIoU(
                        box.boundingBox, liveSourceSize,
                        candidate.boundingBox, map.sourceResolution
                    )
                } else {
                    map.computeIoU(box.boundingBox, candidate.boundingBox)
                }
                if (iou > bestIoU) {
                    bestIoU = iou
                    bestNode = candidate
                }
            }

            if (bestIoU >= 0.1f) {
                LOGI(TAG, "PALLET_FINITE: best unbarcoded match node[${bestNode?.spatialIndex}] IoU=${String.format(
                    Locale.US, "%.2f", bestIoU)}")
                bestNode
            } else if (unbarcodedCandidates.size == 1) {
                LOGI(TAG, "PALLET_FINITE: single unbarcoded candidate node[${unbarcodedCandidates[0].spatialIndex}], promoting despite low IoU=$bestIoU")
                unbarcodedCandidates[0]
            } else {
                LOGI(TAG, "PALLET_FINITE: ${unbarcodedCandidates.size} unbarcoded candidates but best IoU=$bestIoU < 0.1, skipping promotion")
                null
            }
        } else null

        if (existingUnbarcoded != null) {
            map.nodeByBarcode[primaryBarcode] = existingUnbarcoded
            map.nodeByBarcode["barcode:$primaryBarcode"] = existingUnbarcoded
            existingUnbarcoded.accumulatedBarcodeValues.add(primaryBarcode)
            existingUnbarcoded.barcodeVoteCounts[primaryBarcode] =
                (existingUnbarcoded.barcodeVoteCounts[primaryBarcode] ?: 0) + SNAP_VOTE_SEED
            LOGI(TAG, "PALLET_FINITE: promoted existing node[${existingUnbarcoded.spatialIndex}] " +
                    "key='${existingUnbarcoded.stableKey}' with barcode '$primaryBarcode' (no duplicate created)")
            return false
        }

        val liveSourceSize = androidx.compose.ui.unit.IntSize(
            box.boundingBox.width.toInt(),
            box.boundingBox.height.toInt()
        )

        val hasOverlap = map.nodes.any { existingNode ->
            if (!existingNode.isAuditRequired || existingNode.isAuditCompleted) return@any false

            val iou = if (map.sourceResolution.width > 0 && liveSourceSize.width > 0) {
                map.computeNormalizedIoU(
                    box.boundingBox, liveSourceSize,
                    existingNode.boundingBox, map.sourceResolution
                )
            } else {
                map.computeIoU(box.boundingBox, existingNode.boundingBox)
            }
            iou > 0.5f
        }

        if (hasOverlap) {
            LOGI(TAG, "PALLET_FINITE: barcode '$primaryBarcode' overlaps with existing node, skipping")
            return false
        }

        val correctedBox = box.copy(
            palletBarcodes = box.palletBarcodes.map { pb ->
                pb.copy(isMainBarcode = (pb.data == primaryBarcode))
            }
        )

        val newNode = map.addShelfNode(correctedBox, appConfig) ?: return false

        auditTargets[newNode.stableKey] = AuditStatus.PENDING

        val currentPip = _pipValidationState.value
        _pipValidationState.value = currentPip + (newNode.stableKey to BOX_VALIDATION.NOT_DETECTED)

        val newNodeBarcodes = correctedBox.palletBarcodes
            .filter { it.data.isNotEmpty() }
            .map { it.data }
            .toSet()

        val updatedDetails = palletSession.value.storedPalletDetails.toMutableList()
        val orphanedSnapBoxes = updatedDetails.filter { existingBox ->
            val hasNoPrimary = existingBox.barcodeList.none { it.isMainBarcode && it.data.isNotEmpty() }
            if (!hasNoPrimary) return@filter false

            val spatialNode = map.nodes.find { it.stableKey == existingBox.stableKey }
            if (spatialNode != null && spatialNode.hasPrimaryBarcode) return@filter false

            val existingBarcodes = existingBox.barcodeList
                .filter { it.data.isNotEmpty() }
                .map { it.data }
                .toSet()
            val hasOverlappingBarcodes = existingBarcodes.intersect(newNodeBarcodes).isNotEmpty()
            val hasMatchingTrackId = box.trackId >= 0 && existingBox.trackId == box.trackId

            hasOverlappingBarcodes || hasMatchingTrackId
        }

        if (orphanedSnapBoxes.isNotEmpty()) {
            val orphanedKeys = orphanedSnapBoxes.map { it.stableKey }.toSet()
            updatedDetails.removeAll { it.stableKey in orphanedKeys }

            val currentPipUpdate = _pipValidationState.value.toMutableMap()
            orphanedKeys.forEach { key ->
                currentPipUpdate.remove(key)
                auditTargets.remove(key)
            }
            _pipValidationState.value = currentPipUpdate

            LOGI(TAG, "PALLET_FINITE: removed ${orphanedSnapBoxes.size} orphaned snap box(es): ${orphanedKeys}")
            palletSession.value = palletSession.value.copy(storedPalletDetails = updatedDetails)
        }

        LOGI(TAG, "PALLET_FINITE: added dynamic node index=${newNode.spatialIndex} barcode=$primaryBarcode (no PIP update)")
        return true
    }

    /**
     * Finalizes the wand session by persisting all upgraded [pipValidationState] entries back to [storedPalletDetails] for proper UI rendering.
     * Keeps the complete barcode list intact across snap and wand phases, rather than filtering flags like isMainBarcode or isQtyBarcode.
     * Allows the results screen to correctly re-derive SKU and QTY classification flags using current config-criteria matching.
     * Call this immediately before navigating to the results screen after the audit completes.
     */
    fun finaliseWandSession() {
        val pipMap = _pipValidationState.value
        val spatialMap = palletSession.value.spatialMap

        val updated = palletSession.value.storedPalletDetails.map { box ->

            val primaryBc = box.barcodeList.firstOrNull { it.isMainBarcode && it.data.isNotEmpty() }?.data
            val resolved = pipMap[box.stableKey]
                ?: primaryBc?.let { pipMap[it] ?: pipMap["barcode:$it"] }
                ?: box.barcodeList
                    .filter { it.data.isNotEmpty() }
                    .firstNotNullOfOrNull { pipMap[it.data] ?: pipMap["barcode:${it.data}"] }

            val node = spatialMap?.nodes?.find { it.stableKey == box.stableKey }
                ?: primaryBc?.let { bc ->
                    spatialMap?.nodeByBarcode?.get(bc)
                        ?: spatialMap?.nodeByBarcode?.get("barcode:$bc")
                }
                ?: box.barcodeList
                    .filter { it.data.isNotEmpty() }
                    .firstNotNullOfOrNull { bc ->
                        spatialMap?.nodeByBarcode?.get(bc.data)
                            ?: spatialMap?.nodeByBarcode?.get("barcode:${bc.data}")
                    }

            if (node != null) {
                LOGI(TAG, "finaliseWandSession: box '${box.stableKey.hashCode()}' → node[${node.spatialIndex}] key='${node.stableKey.hashCode()}' wandVal=${node.wandValidation}")
            }

            val isShelfMode = palletSession.value.sessionMode == SessionMode.SHELF_CONTINUOUS
            val voteThreshold = if (isShelfMode) 1 else BARCODE_VOTE_THRESHOLD
            val confidentBarcodes = if (node != null && spatialMap != null) {
                spatialMap.getConfidentBarcodes(node, voteThreshold)
            } else {
                null
            }

            val mergedBarcodeList = if (node != null && node.wandBarcodes.isNotEmpty()) {
                val existingData = box.barcodeList.map { it.data }.toSet()
                val newWandBarcodes = node.wandBarcodes
                    .filter { it.data.isNotEmpty() && it.data !in existingData }
                    .filter { pb ->
                        confidentBarcodes == null || pb.data in confidentBarcodes
                    }
                    .map { pb ->
                        PalletBarcode(
                            boundingBox = pb.boundingBox,
                            data = pb.data,
                            symbology = pb.symbology,
                            isMainBarcode = pb.isMainBarcode,
                            isQtyBarcodes = pb.isQtyBarcode,
                            angle = pb.angle
                        )
                    }
                if (newWandBarcodes.isEmpty()) box.barcodeList
                else box.barcodeList + newWandBarcodes
            } else {
                box.barcodeList
            }

            val needsUpdate = resolved != null || mergedBarcodeList !== box.barcodeList
            if (needsUpdate) {
                box.copy(
                    validation  = resolved ?: box.validation,
                    barcodeList = mergedBarcodeList
                )
            } else {
                box
            }
        }

        val deduped = updated
            .associateBy { it.stableKey.removePrefix("barcode:").ifEmpty { "id-${it.id}" } }
            .values.toMutableList()

        var hasUnmappedDynamicBoxes = false

        if (spatialMap != null) {
            val existingKeys = deduped.map { it.stableKey.removePrefix("barcode:") }.toSet()

            val dynamicNodes = spatialMap.nodes.filter { node ->
                val normalizedNodeKey = node.stableKey.removePrefix("barcode:")
                node.isAuditCompleted && normalizedNodeKey !in existingKeys
                    && (node.wandValidation == BOX_VALIDATION.VERIFIED || node.wandValidation == BOX_VALIDATION.MISMATCH_QTY)
            }

            val claimedSnapIndices = mutableSetOf<Int>()

            for (node in dynamicNodes) {
                val primaryBc = node.primaryBarcodeValue
                val nodeKeyBare = node.stableKey.removePrefix("barcode:")
                val alreadyCovered = primaryBc.isNotEmpty() && deduped.any { box ->
                    val keyBare = box.stableKey.removePrefix("barcode:")
                    if (keyBare == nodeKeyBare) return@any true
                    box.barcodeList.any { it.data == primaryBc && it.isMainBarcode }
                }
                if (alreadyCovered) {
                    LOGI(TAG, "finaliseWandSession: skipping dynamic node '${node.stableKey}' — primary '$primaryBc' already covered")
                    continue
                }

                val wandBarcodes = node.wandBarcodes.map { pb ->
                    PalletBarcode(
                        boundingBox = pb.boundingBox,
                        data = pb.data,
                        symbology = pb.symbology,
                        isMainBarcode = pb.isMainBarcode,
                        isQtyBarcodes = pb.isQtyBarcode,
                        angle = pb.angle
                    )
                }

                val dynamicBarcodeValues = wandBarcodes
                    .filter { it.data.isNotEmpty() }
                    .map { it.data }
                    .toSet()

                var matchingSnapIdx = -1
                for (i in deduped.indices) {
                    if (i in claimedSnapIndices) continue
                    val snapBox = deduped[i]

                    val hasNoPrimary = snapBox.barcodeList.none { it.isMainBarcode && it.data.isNotEmpty() }
                    if (!hasNoPrimary) continue

                    val snapBarcodeValues = snapBox.barcodeList
                        .filter { it.data.isNotEmpty() }
                        .map { it.data }
                        .toSet()

                    val hasOverlap = snapBarcodeValues.intersect(dynamicBarcodeValues).isNotEmpty()
                    val hasNoBarcodes = snapBarcodeValues.isEmpty()

                    if (hasOverlap || hasNoBarcodes) {
                        matchingSnapIdx = i
                        break
                    }
                }

                if (matchingSnapIdx >= 0) {
                    claimedSnapIndices.add(matchingSnapIdx)
                    val snapBox = deduped[matchingSnapIdx]
                    val mergedBarcodes = snapBox.barcodeList.toMutableList()

                    val existingData = mergedBarcodes.map { it.data }.toSet()
                    val newBarcodes = wandBarcodes.filter { it.data.isNotEmpty() && it.data !in existingData }
                    mergedBarcodes.addAll(newBarcodes)

                    deduped[matchingSnapIdx] = snapBox.copy(
                        validation = node.wandValidation,
                        barcodeList = mergedBarcodes
                    )
                    LOGI(TAG, "finaliseWandSession: updated snap box '${snapBox.stableKey}' in-place with wand data from node[${node.spatialIndex}] key='${node.stableKey}'")
                } else {
                    hasUnmappedDynamicBoxes = true
                    val dynamicBox = PalletBox(
                        id = node.spatialIndex,
                        boundingBox = node.boundingBox,
                        barcodeList = wandBarcodes,
                        classId = 1,
                        validation = node.wandValidation,
                        trackId = node.trackId,
                        stableKey = node.stableKey
                    )
                    deduped.add(dynamicBox)
                    LOGI(TAG, "finaliseWandSession: added UNMAPPED dynamic node[${node.spatialIndex}] key='${node.stableKey}' (overlay will be unreliable)")
                }
            }

            val staleTrackEntries = deduped.filter { box ->
                box.stableKey.startsWith("track-") &&
                    box.barcodeList.none { it.isMainBarcode && it.data.isNotEmpty() } &&
                    box.validation != BOX_VALIDATION.VERIFIED &&
                    box.validation != BOX_VALIDATION.MISMATCH_QTY
            }
            val expectedLimit = palletSession.value.expectedBoxes
            val productBoxCount = deduped.count { it.classId != PalletProcessHelper.PALLET_BASE_CLASS_ID }
            val afterRemovalCount = productBoxCount - staleTrackEntries.size

            if (staleTrackEntries.isNotEmpty() && expectedLimit > 0 && afterRemovalCount >= expectedLimit) {
                val staleKeys = staleTrackEntries.map { it.stableKey }.toSet()
                deduped.removeAll { it.stableKey in staleKeys }
                LOGI(TAG, "finaliseWandSession: removed ${staleKeys.size} stale track-N entries " +
                        "(remaining products=$afterRemovalCount >= expected=$expectedLimit): $staleKeys")
            } else if (staleTrackEntries.isNotEmpty()) {
                LOGI(TAG, "finaliseWandSession: keeping ${staleTrackEntries.size} unresolved track-N entries " +
                        "(removing would leave $afterRemovalCount < expected=$expectedLimit): ${staleTrackEntries.map { it.stableKey }}")
            }
        }

        val productCountAfterDedup = deduped.count { it.classId != PalletProcessHelper.PALLET_BASE_CLASS_ID }
        val limit = palletSession.value.expectedBoxes
        if (spatialMap != null && limit > 0 && productCountAfterDedup < limit) {
            val existingPrimaries = deduped.flatMap { box ->
                box.barcodeList.filter { it.data.isNotEmpty() }.map { it.data }
            }.toSet()
            val existingStableKeys = deduped.map { it.stableKey.removePrefix("barcode:") }.toSet()

            val missingNodes = spatialMap.nodes.filter { node ->
                val normalizedKey = node.stableKey.removePrefix("barcode:")
                node.isAuditCompleted
                    && (node.wandValidation == BOX_VALIDATION.VERIFIED || node.wandValidation == BOX_VALIDATION.MISMATCH_QTY)
                    && normalizedKey !in existingStableKeys
                    && (node.primaryBarcodeValue.isEmpty() || node.primaryBarcodeValue !in existingPrimaries)
            }

            for (node in missingNodes) {
                if (deduped.count { it.classId != PalletProcessHelper.PALLET_BASE_CLASS_ID } >= limit) break
                val nodeBarcodes = node.wandBarcodes.filter { it.data.isNotEmpty() }.map { it.data }.toSet()
                val alreadyCoveredByBarcode = deduped.any { box ->
                    box.barcodeList.any { it.data.isNotEmpty() && it.data in nodeBarcodes }
                }
                if (alreadyCoveredByBarcode) continue

                val recoveredBox = PalletBox(
                    id = node.spatialIndex,
                    boundingBox = node.boundingBox,
                    barcodeList = node.wandBarcodes.map { pb ->
                        PalletBarcode(
                            boundingBox = pb.boundingBox,
                            data = pb.data,
                            symbology = pb.symbology,
                            isMainBarcode = pb.isMainBarcode,
                            isQtyBarcodes = pb.isQtyBarcode,
                            angle = pb.angle
                        )
                    },
                    classId = 1,
                    validation = node.wandValidation,
                    trackId = node.trackId,
                    stableKey = node.stableKey
                )
                deduped.add(recoveredBox)
                LOGI(TAG, "finaliseWandSession BACKSTOP: recovered missing node[${node.spatialIndex}] key='${node.stableKey}' val=${node.wandValidation}")
            }
        }

        val capped = capToExpectedBoxes(deduped)
        palletSession.value = palletSession.value.copy(
            storedPalletDetails = capped,
            hasUnmappedWandBoxes = hasUnmappedDynamicBoxes
        )
        _palletResults.value = capped
        LOGI(TAG, "finaliseWandSession: ${pipMap.size} resolved, ${updated.size}→${deduped.size} boxes after dedup, ${capped.size} after cap, unmappedDynamic=$hasUnmappedDynamicBoxes")


        LOGI("WandDiag", "═══ finaliseWandSession: ${capped.size} boxes in final results ═══")
        capped.forEachIndexed { idx, box ->
            val primary = box.barcodeList.firstOrNull { it.isMainBarcode && it.data.isNotEmpty() }?.data ?: ""
            val decodedCount = box.barcodeList.count { it.data.isNotEmpty() }
            LOGI("WandDiag", "  Result[${idx+1}] key='${box.stableKey.hashCode()}' val=${box.validation} primary='${primary.hashCode()}' decoded=$decodedCount/${box.barcodeList.size}")
        }
    }

    private fun getAuditKey(box: PBoxUIModel): String {
        val main = box.palletBarcodes.firstOrNull { it.isMainBarcode && it.data.isNotEmpty() }?.data
        return if (!main.isNullOrEmpty()) {
            "barcode:$main"
        } else {
            "track:${box.trackId.takeIf { it >= 0 } ?: box.id.toLong()}"
        }
    }

    fun findTotalQtyForPallets(appConfig: List<BarcodeConfig>): Int {
        val qtyConfig = appConfig.filter { it.isSelected }
            .firstOrNull { it.type == FIELD_TYPE.QTY.name }
        qtyConfig?.let {

        }
        val totalQty = qtyConfig?.let { configTxt ->
            palletSession.value.storedPalletDetails.map {
                val list = it.barcodeList.mapNotNull { b ->
                    if (b.data.isNotEmpty() && b.data.startsWith(configTxt.regex, false)) {
                        val subStr = b.data.replace(configTxt.regex, "")
                        subStr.toIntOrNull()
                    } else null
                }
                list.sum()
            }.sum()
        } ?: palletSession.value.storedPalletDetails.size
        return totalQty
    }

    fun clearSession() {
        cachedPalletBox.clear()
        auditTargets.clear()
        _pipValidationState.value = emptyMap()
        cancelWandTimer()
        _wandTimerState.value = WandTimerState()
        mPalletHelper?.clear()
        palletSession.value = PalletSession()
    }

    fun startSession(expectedBoxes: Int = 0) {
        clearSession()
        palletSession.value = palletSession.value.copy(expectedBoxes = expectedBoxes)
        isCaptureImageResults = true
    }

    /**
     * Updates the PRODUCT_SKU prefix used by PalletProcessHelper to identify the
     * primary barcode on each box during frame processing.
     *
     * Must be called whenever the config changes or before a new session begins.
     * @param appConfig The current barcode config list
     */
    fun updateSkuPrefix(appConfig: List<BarcodeConfig>) {
        val skuConfig = appConfig.filter { it.isSelected }
            .firstOrNull { it.type == FIELD_TYPE.PRODUCT_SKU.name }
        val prefix = when {
            skuConfig == null -> ""
            skuConfig.regex.isNotEmpty() -> skuConfig.regex
            else -> FIELD_TYPE.PRODUCT_SKU.startWith
        }
        mPalletHelper?.skuPrefix = prefix
        LOGI(TAG, "SKU prefix updated to: '$prefix'")
    }

    /**
     * Seeds [_pipValidationState] from the COMPLETE set of snap-validated UI boxes.
     * Must be called before [updatePartialDetections].
     */
    fun seedSnapValidations(allSnapBoxes: List<PBoxUIModel>) {
        val seed = allSnapBoxes
            .filter { it.classId != PalletProcessHelper.PALLET_BASE_CLASS_ID }
            .associate { it.stableKey to it.validation }
        _pipValidationState.value = seed
        LOGI(TAG, "seedSnapValidations: seeded ${seed.size} boxes")
    }

    /**
     * Builds the spatial map and determines the session mode.
     *
     * PALLET_FINITE   — boxes seen >= expectedBoxes AND pipEnabled=true → full map from snap, PIP shown.
     * SHELF_CONTINUOUS — boxes seen < expectedBoxes OR pipEnabled=false → sparse/empty initial map,
     *                    PIP hidden, nodes added dynamically during wand as new primary barcodes are found.
     *
     * When [pipEnabled] is false (user has disabled PIP in settings), the mode is always forced to
     * SHELF_CONTINUOUS regardless of the snap box count. This means the workflow has no hard dependency
     * on the pre-made Box snap generated map — nodes/boxes are added to the list dynamically based on
     * the Primary unique barcode, exactly as in the standard SHELF_CONTINUOUS flow.
     *
     * @param allSnapBoxes All boxes detected in snap (product boxes only, no pallet base)
     * @param expectedBoxes The "Qty of Boxes to Audit" entered by the user
     * @param sourceResolution Source image resolution from snap phase (for coordinate normalization)
     * @param targetViewSize Target screen/view size from snap phase (for coordinate normalization)
     * @param pipEnabled Whether PIP (Picture-in-Picture) thumbnail is enabled in settings.
     *                   When false, always uses SHELF_CONTINUOUS mode (no snap-map dependency).
     */
    fun updatePartialDetections(
        allSnapBoxes: List<PBoxUIModel>,
        expectedBoxes: Int,
        sourceResolution: androidx.compose.ui.unit.IntSize = androidx.compose.ui.unit.IntSize.Zero,
        targetViewSize: androidx.compose.ui.unit.IntSize = androidx.compose.ui.unit.IntSize.Zero,
        pipEnabled: Boolean = true
    ) {
        palletSession.value = palletSession.value.copy(partialDetection = allSnapBoxes)

        val productBoxes = allSnapBoxes.filter { it.classId != PalletProcessHelper.PALLET_BASE_CLASS_ID }
        val productBoxCount = productBoxes.size

        val missingPrimaryCount = productBoxes.count { box ->
            box.palletBarcodes.none { it.isMainBarcode && it.data.isNotEmpty() }
        }


        val isHybrid = pipEnabled && productBoxCount >= expectedBoxes && missingPrimaryCount > 1

        val mode = when {
            !pipEnabled -> SessionMode.SHELF_CONTINUOUS
            isHybrid -> SessionMode.SHELF_CONTINUOUS
            productBoxCount >= expectedBoxes -> SessionMode.PALLET_FINITE
            else -> SessionMode.SHELF_CONTINUOUS
        }

        LOGI(TAG, "Session mode: $mode (boxesSeen=$productBoxCount, expectedBoxes=$expectedBoxes, " +
                "pipEnabled=$pipEnabled, missingPrimary=$missingPrimaryCount, isHybrid=$isHybrid)")

        val greenIn = productBoxes.count { it.validation == BOX_VALIDATION.VERIFIED }
        val yellowIn = productBoxes.count { it.validation == BOX_VALIDATION.PARTIAL_DETECTION }
        val redIn = productBoxes.count { it.validation == BOX_VALIDATION.MISMATCH_QTY }
        val otherIn = productBoxCount - greenIn - yellowIn - redIn
        LOGI("SnapDiag", "updatePartialDetections INPUT: $productBoxCount product boxes " +
                "(green=$greenIn yellow=$yellowIn red=$redIn other=$otherIn) → mode=$mode")
        if (isHybrid) {
            LOGI("WandDiag", "═══ HYBRID WAND MODE: $missingPrimaryCount boxes missing primary barcode → " +
                    "SHELF_CONTINUOUS with ${productBoxCount - missingPrimaryCount} green snap boxes carried forward ═══")
        }

        val spatialMap = when {
            mode == SessionMode.PALLET_FINITE -> {
                SpatialMapBuilder.build(allSnapBoxes, sourceResolution = sourceResolution, targetViewSize = targetViewSize)
            }
            isHybrid -> {
                val greenSnapBoxes = allSnapBoxes.filter {
                    it.classId != PalletProcessHelper.PALLET_BASE_CLASS_ID &&
                    it.validation == BOX_VALIDATION.VERIFIED
                }
                LOGI(TAG, "Hybrid: seeding map with ${greenSnapBoxes.size} GREEN snap boxes")
                SpatialMapBuilder.build(greenSnapBoxes, forceAuditRequired = false, sourceResolution = sourceResolution, targetViewSize = targetViewSize)
            }
            else -> {
                // SHELF_CONTINUOUS: seed the map with all snap boxes that have a primary barcode.
                // Use forceAuditRequired = false so already-VERIFIED (green) snap boxes are
                // correctly carried forward as isAuditCompleted=true / wandValidation=VERIFIED.
                // Only unverified boxes will be marked as requiring wand attention.
                val snapWithPrimary = allSnapBoxes.filter {
                    it.palletBarcodes.any { b -> b.isMainBarcode && b.data.isNotEmpty() }
                }
                SpatialMapBuilder.build(snapWithPrimary, forceAuditRequired = false, sourceResolution = sourceResolution, targetViewSize = targetViewSize)
            }
        }

        palletSession.value = palletSession.value.copy(
            spatialMap = spatialMap,
            sessionMode = mode,
            expectedBoxes = expectedBoxes,
            isHybridWand = isHybrid
        )

        auditTargets.clear()
        spatialMap.getAuditRequiredNodes().forEach { node ->
            auditTargets[node.stableKey] = AuditStatus.PENDING
        }

        LOGI(TAG, "Spatial map: ${spatialMap.nodes.size} nodes, " +
                "${spatialMap.getAuditRequiredNodes().size} require wand attention, mode=$mode, " +
                "sourceRes=${sourceResolution.width}×${sourceResolution.height}, targetView=${targetViewSize.width}×${targetViewSize.height}")

        val greenNodes = spatialMap.nodes.count { it.isAuditCompleted && it.wandValidation == BOX_VALIDATION.VERIFIED }
        val pendingNodes = spatialMap.nodes.count { !it.isAuditCompleted || it.wandValidation != BOX_VALIDATION.VERIFIED }
        LOGI("SnapDiag", "updatePartialDetections OUTPUT: spatialMap has ${spatialMap.nodes.size} nodes " +
                "(preVerified=$greenNodes pending=$pendingNodes) — " +
                if (greenNodes != greenIn) "⚠ GREEN COUNT MISMATCH: input=$greenIn output=$greenNodes" else "✓ green counts match")
    }

    /** Backward-compat overload for callers that don't yet pass expectedBoxes. */
    fun updatePartialDetections(allSnapBoxes: List<PBoxUIModel>) {
        updatePartialDetections(allSnapBoxes, expectedBoxes = allSnapBoxes.size)
    }

    /**
     * Resolves a live wand-frame box to its spatial map node using 3-tier lookup.
     */
    fun resolveWandBoxToNode(liveBox: PBoxUIModel): SpatialBoxNode? =
        palletSession.value.spatialMap?.resolveNode(liveBox)

    /**
     * Returns the "better" of two validation states — never allows downgrading.
     *
     * Only VERIFIED is a true terminal state that cannot be changed.
     * MISMATCH_QTY (red) can be upgraded to VERIFIED when the user subsequently
     * wands over the box and all required barcodes are decoded — this allows
     * recovery from premature red marking caused by partial views.
     */
    private fun upgradedValidation(existing: BOX_VALIDATION?, incoming: BOX_VALIDATION): BOX_VALIDATION {
        if (existing == null) return incoming

        if (existing == BOX_VALIDATION.VERIFIED) return existing
        val rank = mapOf(
            BOX_VALIDATION.NOT_DETECTED to 0,
            BOX_VALIDATION.PARTIAL_DETECTION to 1,
            BOX_VALIDATION.MISMATCH_QTY to 2,
            BOX_VALIDATION.VERIFIED to 3
        )
        return if ((rank[incoming] ?: 0) > (rank[existing] ?: 0)) incoming else existing
    }

    /**
     * Incremental wand processing for a matched spatial node.
     */
    fun updateWandNodeIncrementally(
        node: SpatialBoxNode,
        liveBox: PBoxUIModel,
        liveLocalizedCount: Int,
        appConfig: List<BarcodeConfig>
    ): BOX_VALIDATION? {

        if (node.isAuditCompleted) {
            val liveBarcodeCount = liveBox.palletBarcodes.count { it.data.isNotEmpty() }
            val canReopen = node.wandValidation == BOX_VALIDATION.MISMATCH_QTY && liveBarcodeCount > 0
            if (!canReopen) return null
            node.isAuditCompleted = false
            LOGI(TAG, "Node[${node.spatialIndex}]: reopening MISMATCH node — live decoded $liveBarcodeCount barcode(s)")
        }

        val spatialMap = palletSession.value.spatialMap

        val newBarcodes = liveBox.palletBarcodes
            .filter { it.data.isNotEmpty() }
            .map { it.data }
            .toSet()

        val safeBarcodes = if (spatialMap != null) {
            newBarcodes.filter { barcode ->
                !spatialMap.isPrimaryOfOtherNode(barcode, node)
            }.toSet()
        } else {
            newBarcodes
        }

        safeBarcodes.forEach { barcode ->
            node.accumulatedBarcodeValues.add(barcode)
            node.barcodeVoteCounts[barcode] = (node.barcodeVoteCounts[barcode] ?: 0) + 1
        }

        if (safeBarcodes.isNotEmpty()) {
            val existingValues = node.wandBarcodes.map { it.data }.toSet()
            val newWandBarcodes = liveBox.palletBarcodes.filter {
                it.data.isNotEmpty() && it.data !in existingValues
                    && it.data in safeBarcodes
            }
            node.wandBarcodes = node.wandBarcodes + newWandBarcodes

            if (!node.hasPrimaryBarcode || node.primaryBarcodeValue.isEmpty()) {
                tryPromotePrimaryBarcode(node, safeBarcodes, appConfig)
            }
        }

        node.currentWandLocalizedCount = maxOf(node.currentWandLocalizedCount, liveLocalizedCount)

        val validationResult = computeValidationFromCriteria(node, appConfig)
        val liveValidation = validationResult.first
        val configMatched = validationResult.second
        val configNeeded = validationResult.third

        val totalLocalized = maxOf(node.snapLocalizedBarcodeCount, node.currentWandLocalizedCount)
        val totalDecoded = node.accumulatedBarcodeValues.size

        LOGI(TAG, "WandIncremental[${node.spatialIndex}]: accumulated=$totalDecoded, configMatched=$configMatched/$configNeeded, localized=$totalLocalized")

        val hasFullView = node.currentWandLocalizedCount >= node.snapLocalizedBarcodeCount

        val confidentBarcodes = palletSession.value.spatialMap?.getConfidentBarcodes(node, if (palletSession.value.sessionMode == SessionMode.SHELF_CONTINUOUS) 1 else BARCODE_VOTE_THRESHOLD) ?: emptySet()
        LOGI("WandDiag", "┌─── Node[${node.spatialIndex}] key='${node.stableKey.hashCode()}' snapVal=${node.snapValidation} ───")
        LOGI("WandDiag", "│ liveBarcodes=${newBarcodes.map { "'${it.hashCode()}'" }}")
        LOGI("WandDiag", "│ safeBarcodes(afterDedup)=${safeBarcodes.map { "'${it.hashCode()}'" }}")
        LOGI("WandDiag", "│ accumulated=${node.accumulatedBarcodeValues.map { "'${it.hashCode()}'" }}")
        LOGI("WandDiag", "│ votes=${node.barcodeVoteCounts.map { Pair(it.key.hashCode(),it.value) }}")
        LOGI("WandDiag", "│ confidentBarcodes=${confidentBarcodes.hashCode()}")
        LOGI("WandDiag", "│ configMatched=$configMatched/$configNeeded liveValidation=$liveValidation")
        LOGI("WandDiag", "│ snapLocalized=${node.snapLocalizedBarcodeCount} wandLocalized=${node.currentWandLocalizedCount} hasFullView=$hasFullView")
        LOGI("WandDiag", "│ hasPrimary=${node.hasPrimaryBarcode} primaryVal='${node.primaryBarcodeValue.hashCode()}'")
        LOGI("WandDiag", "└────────────────────────────────────────────")

        return when {
            liveValidation == BOX_VALIDATION.VERIFIED -> {
                LOGI("WandDiag", "  → DECISION: VERIFIED (liveValidation==VERIFIED)")
                node.wandValidation = BOX_VALIDATION.VERIFIED
                node.isAuditCompleted = true
                updatePipState(node, BOX_VALIDATION.VERIFIED)
                BOX_VALIDATION.VERIFIED
            }
            node.snapValidation == BOX_VALIDATION.VERIFIED -> {
                LOGI("WandDiag", "  → DECISION: VERIFIED (snapValidation==VERIFIED)")
                node.wandValidation = BOX_VALIDATION.VERIFIED
                node.isAuditCompleted = true
                updatePipState(node, BOX_VALIDATION.VERIFIED)
                BOX_VALIDATION.VERIFIED
            }

            liveValidation == BOX_VALIDATION.MISMATCH_QTY && hasFullView -> {
                LOGI("WandDiag", "  → DECISION: MISMATCH_QTY suppressed → PARTIAL_DETECTION (live=MISMATCH+hasFullView, kept yellow)")
                node.wandValidation = BOX_VALIDATION.PARTIAL_DETECTION
                updatePipState(node, BOX_VALIDATION.PARTIAL_DETECTION)
                BOX_VALIDATION.PARTIAL_DETECTION
            }
            node.snapValidation == BOX_VALIDATION.MISMATCH_QTY && totalDecoded >= node.snapBarcodeCount && hasFullView && totalDecoded > 0 -> {
                LOGI("WandDiag", "  → DECISION: MISMATCH_QTY suppressed → PARTIAL_DETECTION (snap=MISMATCH confirmed by wand, decoded=$totalDecoded>=snapBc=${node.snapBarcodeCount}, kept yellow)")
                node.wandValidation = BOX_VALIDATION.PARTIAL_DETECTION
                updatePipState(node, BOX_VALIDATION.PARTIAL_DETECTION)
                BOX_VALIDATION.PARTIAL_DETECTION
            }
            else -> {
                LOGI("WandDiag", "  → DECISION: PARTIAL_DETECTION (fallthrough: liveVal=$liveValidation hasFullView=$hasFullView snapVal=${node.snapValidation})")
                node.wandValidation = BOX_VALIDATION.PARTIAL_DETECTION
                updatePipState(node, BOX_VALIDATION.PARTIAL_DETECTION)
                BOX_VALIDATION.PARTIAL_DETECTION
            }
        }
    }

    private fun computeValidationFromCriteria(
        node: SpatialBoxNode,
        appConfig: List<BarcodeConfig>
    ): Triple<BOX_VALIDATION, Int, Int> {
        val spatialMap = palletSession.value.spatialMap
        val isShelfMode = palletSession.value.sessionMode == SessionMode.SHELF_CONTINUOUS
        val voteThreshold = if (isShelfMode) 1 else BARCODE_VOTE_THRESHOLD
        val barcodes = if (spatialMap != null) {
            spatialMap.getConfidentBarcodes(node, voteThreshold)
        } else {
            node.accumulatedBarcodeValues
        }
        val selectedConfig = appConfig.filter { it.isSelected }

        if (selectedConfig.isEmpty()) {
            return Triple(BOX_VALIDATION.NOT_DETECTED, 0, 0)
        }
        if (barcodes.isEmpty()) {
            return Triple(BOX_VALIDATION.NOT_DETECTED, 0, selectedConfig.size)
        }

        val configMatched = mutableMapOf<String, String>()
        val usedBarcodes = mutableSetOf<String>()

        for (config in selectedConfig) {
            val fieldType = try { FIELD_TYPE.valueOf(config.type) } catch (e: Exception) { continue }
            val matchingBarcode = barcodes.firstOrNull { barcode ->
                barcode !in usedBarcodes && isBarcodeMatchingCriteria(barcode, config, fieldType)
            }
            if (matchingBarcode != null) {
                configMatched[config.type] = matchingBarcode
                usedBarcodes.add(matchingBarcode)
            }
        }

        val configNeeded = selectedConfig.size
        val matched = configMatched.size
        val totalLocalized = maxOf(node.snapLocalizedBarcodeCount, node.currentWandLocalizedCount)
        val totalDecoded = barcodes.size

        val validation = when {
            configNeeded == 0 -> BOX_VALIDATION.NOT_DETECTED
            matched == configNeeded -> BOX_VALIDATION.VERIFIED
            totalDecoded < totalLocalized -> BOX_VALIDATION.PARTIAL_DETECTION
            else -> BOX_VALIDATION.MISMATCH_QTY
        }

        if (validation == BOX_VALIDATION.MISMATCH_QTY || validation == BOX_VALIDATION.PARTIAL_DETECTION) {
            LOGI("WandDiag", "  computeCriteria[${node.spatialIndex}]: confidentBarcodes=$barcodes matched=$matched/$configNeeded decoded=$totalDecoded localized=$totalLocalized → $validation")
            selectedConfig.forEach { cfg ->
                LOGI("WandDiag", "    field=${cfg.type} regex='${cfg.regex}' → ${configMatched[cfg.type] ?: "NONE"}")
            }
        }

        return Triple(validation, matched, configNeeded)
    }

    private fun isBarcodeMatchingCriteria(barcode: String, config: BarcodeConfig, fieldType: FIELD_TYPE): Boolean {
        return when {
            config.regex.isNotEmpty() -> barcode.startsWith(config.regex)
            else -> barcode.startsWith(fieldType.startWith)
        }
    }

    private fun updatePipState(node: SpatialBoxNode, validation: BOX_VALIDATION) {
        val current = _pipValidationState.value
        val existing = current[node.stableKey]
        val upgraded = upgradedValidation(existing, validation)

        var updates = current
        if (upgraded != existing) {
            updates = updates + (node.stableKey to upgraded)
            LOGI(TAG, "PipState UPDATE: node[${node.spatialIndex}] key='${node.stableKey.hashCode()}' " +
                    "existing=$existing → upgraded=$upgraded (incoming=$validation)")
        } else {
            LOGI(TAG, "PipState NO-CHANGE: node[${node.spatialIndex}] key='${node.stableKey.hashCode()}' " +
                    "stays at $existing (incoming=$validation was not an upgrade)")
        }

        val crossKeys = mutableSetOf<String>()
        if (node.primaryBarcodeValue.isNotEmpty()) {
            crossKeys.add(node.primaryBarcodeValue)
            crossKeys.add("barcode:${node.primaryBarcodeValue}")
        }

        val spatialMap = palletSession.value.spatialMap
        if (spatialMap != null) {
            for ((key, mappedNode) in spatialMap.nodeByBarcode) {
                if (mappedNode.spatialIndex == node.spatialIndex && key != node.stableKey) {
                    crossKeys.add(key)
                }
            }
        }
        for (altKey in crossKeys) {
            if (altKey != node.stableKey) {
                val altExisting = updates[altKey]
                val altUpgraded = upgradedValidation(altExisting, upgraded)
                if (altUpgraded != altExisting) {
                    updates = updates + (altKey to altUpgraded)
                    LOGI(TAG, "PipState CROSS-KEY: '${altKey.hashCode()}' $altExisting → $altUpgraded")
                }
            }
        }

        if (updates !== current) {
            _pipValidationState.value = updates
        }

        val status = when (validation) {
            BOX_VALIDATION.VERIFIED, BOX_VALIDATION.MISMATCH_QTY -> AuditStatus.COMPLETED
            else -> AuditStatus.IN_PROGRESS
        }
        val previousStatus = auditTargets[node.stableKey]
        auditTargets[node.stableKey] = status

        if (previousStatus != status) {
            LOGI(TAG, "AuditTarget UPDATE: key='${node.stableKey.hashCode()}' $previousStatus → $status")
        }
    }

    /**
     * Returns true if the wand session has completed at least one audit node.
     */
    fun hasWandCompleted(): Boolean =
        palletSession.value.spatialMap?.nodes?.any { it.isAuditCompleted } == true

    override fun onCleared() {
        LOGD(TAG, "Main VM OnCleared")
        ModelsStorage.stop()
        super.onCleared()
    }

    fun clearCachedPalletBoxes() {
        cachedPalletBox.clear()
    }
}

data class BarcodeQuantityResult(
    val rect: androidx.compose.ui.geometry.Rect,
    val barcode: String,
    val symbology: Int,
    val isDetected: Boolean = false,
    val qty: Int = 0
)

/**
 * Enum representing all navigation destinations (screens) in the pallet checker app.
 * Each value corresponds to a distinct UI screen or modal that can be shown to the user.
 * Used for navigation logic, screen transitions, and UI state management.
 *
 * Screens:
 * - Home: Main dashboard screen
 * - Settings: General settings screen
 * - SettingsResolution: Camera resolution selection
 * - SettingsModelInputSize: Model input size selection
 * - SettingsInference: Processor type selection
 * - SettingsBarcodeSymbology: Barcode symbology selection
 * - Configure: Demo configuration screen
 * - About: About/info screen
 * - EULA: End User License Agreement screen
 * - Finder: Barcode finder/scan screen
 * - Result: Result summary screen
 */
@Serializable
sealed class ScreenState {
    @Serializable
    data object HOME : ScreenState()
    @Serializable
    data class FINDER(val processType: String) : ScreenState()
    @Serializable
    data object RESULT : ScreenState()
    @Serializable
    data object CONFIGURE : ScreenState()
    @Serializable
    data object MODEL_INPUT : ScreenState()
    @Serializable
    data object RESOLUTION : ScreenState()
    @Serializable
    data object SYMBOLOGY : ScreenState()
    @Serializable
    data object INFERENCE : ScreenState()
    @Serializable
    data object SETTINGS : ScreenState()
    @Serializable
    data object EULA : ScreenState()
    @Serializable
    data object ABOUT : ScreenState()
}

enum class AuditStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED
}
