package com.zebra.ai.palletchecker.helpers

import androidx.compose.ui.geometry.Rect
import com.zebra.ai.palletchecker.domain.model.BarcodeConfig
import com.zebra.ai.palletchecker.presentation.model.BOX_VALIDATION
import com.zebra.ai.palletchecker.presentation.model.PBarcodeUIModel
import com.zebra.ai.palletchecker.presentation.model.PBoxUIModel


const val BARCODE_VOTE_THRESHOLD = 2
const val SNAP_VOTE_SEED = 3

/** Stores spatialIndex references to a box's 8-directional spatial neighbors, using null for edges. */
data class SpatialNeighbours(
    val top: Int? = null,
    val topLeft: Int? = null,
    val topRight: Int? = null,
    val left: Int? = null,
    val right: Int? = null,
    val bottomLeft: Int? = null,
    val bottom: Int? = null,
    val bottomRight: Int? = null
)

/**
 * A pallet spatial map node corresponding to a single box detected during the SNAP phase.
 * Uses a unique, grid-assigned [spatialIndex] as its true identity, with barcodes acting as secondary lookup hints.
 */
data class SpatialBoxNode(

    val spatialIndex: Int,
    val row: Int,
    val col: Int,
    val boundingBox: Rect,
    val trackId: Long,
    val stableKey: String,
    val hasPrimaryBarcode: Boolean,
    val primaryBarcodeValue: String,
    val snapBarcodeValues: Set<String> = emptySet(),
    val snapValidation: BOX_VALIDATION,
    val snapBarcodeCount: Int = 0,
    val snapLocalizedBarcodeCount: Int = 0,
    val neighbours: SpatialNeighbours = SpatialNeighbours(),
    var wandValidation: BOX_VALIDATION = BOX_VALIDATION.NOT_DETECTED,
    var wandBarcodes: List<PBarcodeUIModel> = emptyList(),
    var accumulatedBarcodeValues: MutableSet<String> = mutableSetOf(),
    var barcodeVoteCounts: MutableMap<String, Int> = mutableMapOf(),
    var currentWandLocalizedCount: Int = 0,
    var isAuditRequired: Boolean = false,
    var isAuditCompleted: Boolean = false
)

/**
 * Represents the pallet's spatial map, tracking unique boxes dynamically (SHELF_CONTINUOUS) or post-SNAP using grid-assigned [spatialIndex] as true identities.
 * Resolves boxes through a tiered priority hierarchy (Barcode match, Intersection, TrackId, Grid overlap, to Centroid proximity) where barcodes act as lookup hints.
 * Coordinates normalization across SNAP and wand viewports is handled via [sourceResolution] and [targetViewSize] parameters.
 */
data class PalletSpatialMap(
    val nodes: MutableList<SpatialBoxNode>,
    val nodeByTrackId: MutableMap<Long, SpatialBoxNode>,
    val nodeByBarcode: MutableMap<String, SpatialBoxNode>,
    val rowCount: Int,
    val colCount: Int,
    val capturedAtMs: Long = System.currentTimeMillis(),
    val sourceResolution: androidx.compose.ui.unit.IntSize = androidx.compose.ui.unit.IntSize.Zero,
    val targetViewSize: androidx.compose.ui.unit.IntSize = androidx.compose.ui.unit.IntSize.Zero
) {
    /**
     * Adds a new node in SHELF_CONTINUOUS mode when a previously unseen, unique primary barcode is discovered.
     * Excludes primary barcodes already registered to existing nodes to prevent cross-contamination from adjacent boxes.
     * @return The newly created node, or null if the primary barcode is already registered.
     */
    fun addShelfNode(box: PBoxUIModel, appConfig: List<BarcodeConfig> = emptyList()): SpatialBoxNode? {
        val primaryBarcode = box.palletBarcodes.firstOrNull { it.isMainBarcode && it.data.isNotEmpty() }?.data
            ?: return null

        if (nodeByBarcode.containsKey(primaryBarcode)) return null

        val nextIndex = nodes.size
        val allBarcodeValues = box.palletBarcodes
            .filter { it.data.isNotEmpty() }
            .map { it.data }
            .filter { value ->
                if (value == primaryBarcode) return@filter true
                val existingOwner = nodeByBarcode[value] ?: nodeByBarcode["barcode:$value"]
                existingOwner == null || existingOwner.primaryBarcodeValue != value
            }
            .toSet()

        val node = SpatialBoxNode(
            spatialIndex           = nextIndex,
            row                    = nextIndex,
            col                    = 0,
            boundingBox            = box.boundingBox,
            trackId                = box.trackId,
            stableKey              = "barcode:$primaryBarcode",
            hasPrimaryBarcode      = true,
            primaryBarcodeValue    = primaryBarcode,
            snapBarcodeValues      = allBarcodeValues,
            snapValidation         = BOX_VALIDATION.NOT_DETECTED,
            snapBarcodeCount       = allBarcodeValues.size,
            snapLocalizedBarcodeCount = box.palletBarcodes.size,
            accumulatedBarcodeValues  = allBarcodeValues.toMutableSet(),
            barcodeVoteCounts         = allBarcodeValues.associateWith { SNAP_VOTE_SEED }.toMutableMap(),
            isAuditRequired        = true,
            isAuditCompleted       = false
        )

        nodes.add(node)
        nodeByBarcode[primaryBarcode] = node
        nodeByBarcode["barcode:$primaryBarcode"] = node
        if (box.trackId >= 0) nodeByTrackId[box.trackId] = node
        return node
    }

    /** Returns all nodes that still require wand-mode attention. */
    fun getAuditRequiredNodes(): List<SpatialBoxNode> =
        nodes.filter { it.isAuditRequired && !it.isAuditCompleted }

    /**
     * Returns true if [barcodeValue] is the PRIMARY barcode of a DIFFERENT node
     * (not [excludeNode]). Used to prevent cross-contamination during wand accumulation.
     */
    fun isPrimaryOfOtherNode(barcodeValue: String, excludeNode: SpatialBoxNode): Boolean {
        val ownerNode = nodeByBarcode[barcodeValue] ?: nodeByBarcode["barcode:$barcodeValue"]
        return ownerNode != null && ownerNode.spatialIndex != excludeNode.spatialIndex
                && ownerNode.primaryBarcodeValue == barcodeValue
    }

    /**
     * Returns barcodes meeting the vote [threshold], defaulting to [BARCODE_VOTE_THRESHOLD].
     * For SHELF_CONTINUOUS, a threshold of 1 is recommended to skip multi-frame validation for immediate close-up decodes.
     */
    fun getConfidentBarcodes(node: SpatialBoxNode, threshold: Int = BARCODE_VOTE_THRESHOLD): Set<String> {
        return node.accumulatedBarcodeValues.filter { barcode ->
            (node.barcodeVoteCounts[barcode] ?: 0) >= threshold
        }.toSet()
    }

    /**
     * Resolves a live wand-frame box to its spatial map node using BARCODE-ONLY resolution:
     *  1. Unique barcode match — barcode appears in nodeByBarcode index
     *  2. Multi-barcode intersection — if live frame has multiple barcodes, find node containing ALL
     *
     * All spatial/tracking-based tiers (trackId, centroid proximity) have been removed
     * as they are unreliable and can misassociate data to the wrong box.
     * The only reliable identity for a physical box is its decoded barcode.
     */
    fun resolveNode(liveBox: PBoxUIModel): SpatialBoxNode? {
        val candidateKeys = buildList {
            liveBox.palletBarcodes
                .filter { it.data.isNotEmpty() }
                .forEach { add(it.data) }
            if (liveBox.stableKey.isNotEmpty()) add(liveBox.stableKey)
        }
        for (key in candidateKeys) {
            nodeByBarcode[key]?.let { return it }
        }


        val liveBarcodesSet = liveBox.palletBarcodes
            .filter { it.data.isNotEmpty() }
            .map { it.data }
            .toSet()
        if (liveBarcodesSet.isNotEmpty()) {
            val matchingNodes = nodes.filter { node ->
                liveBarcodesSet.all { barcode -> barcode in node.snapBarcodeValues }
            }
            if (matchingNodes.size == 1) {
                return matchingNodes.first()
            }

            if (matchingNodes.isNotEmpty()) {
                val auditPending = matchingNodes.filter { it.isAuditRequired && !it.isAuditCompleted }
                if (auditPending.size == 1) {
                    return auditPending.first()
                }
            }
        }

        return null
    }

    /**
     * Computes IoU (Intersection over Union) between two bounding boxes.
     * Public accessor for external duplicate detection (e.g., addPalletFiniteNodeFromWand).
     */
    fun computeIoU(a: Rect, b: Rect): Float {
        val left = maxOf(a.left, b.left)
        val top = maxOf(a.top, b.top)
        val right = minOf(a.right, b.right)
        val bottom = minOf(a.bottom, b.bottom)

        if (right <= left || bottom <= top) return 0f

        val intersection = (right - left) * (bottom - top)
        val aArea = a.width * a.height
        val bArea = b.width * b.height
        val union = aArea + bArea - intersection

        return if (union <= 0f) 0f else intersection / union
    }

    /**
     * Computes IoU after normalizing both boxes to [0,1]×[0,1] coordinate space.
     * Public accessor for external coordinate-space aware duplicate detection.
     */
    fun computeNormalizedIoU(
        liveBox: Rect,
        liveSourceSize: androidx.compose.ui.unit.IntSize,
        nodeBox: Rect,
        nodeSourceSize: androidx.compose.ui.unit.IntSize
    ): Float {
        val liveNorm = Rect(
            left = liveBox.left / liveSourceSize.width,
            top = liveBox.top / liveSourceSize.height,
            right = liveBox.right / liveSourceSize.width,
            bottom = liveBox.bottom / liveSourceSize.height
        )

        val nodeNorm = Rect(
            left = nodeBox.left / nodeSourceSize.width,
            top = nodeBox.top / nodeSourceSize.height,
            right = nodeBox.right / nodeSourceSize.width,
            bottom = nodeBox.bottom / nodeSourceSize.height
        )

        return computeIoU(liveNorm, nodeNorm)
    }

    companion object {
        fun createEmptyObj() = PalletSpatialMap(
            nodes            = mutableListOf(),
            nodeByTrackId    = mutableMapOf(),
            nodeByBarcode    = mutableMapOf(),
            rowCount         = 0,
            colCount         = 0,
            sourceResolution = androidx.compose.ui.unit.IntSize.Zero,
            targetViewSize   = androidx.compose.ui.unit.IntSize.Zero
        )
    }
}


