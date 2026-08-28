package com.zebra.ai.palletchecker.helpers

import com.zebra.ai.palletchecker.presentation.model.BOX_VALIDATION
import com.zebra.ai.palletchecker.presentation.model.PBoxUIModel

/**
 * Builds a [PalletSpatialMap] from the complete list of snap boxes across all validation states.
 * Uses the grid-assigned [SpatialBoxNode.spatialIndex] as the true, immutable identity of each box.
 * Removes duplicate barcode values from the lookup index to prevent false matches on repeated SKUs.
 * Leverages cross-frame [trackId] for tracking stability, while grid coordinates provide the ground truth spatial context.
 */
object SpatialMapBuilder {

    private const val TAG = "SpatialMapBuilder"

    private const val ROW_SNAP_PX = 40f

    /**
     * Builds the spatial map from all detected snap boxes across every validation state.
     * Uses [forceAuditRequired] to dictate if every box needs wanding (SHELF_CONTINUOUS) or just unverified ones (PALLET_FINITE).
     * Requires [sourceResolution] and [targetViewSize] to process coordinate transformations and enable Tier 2.5 spatial matching.
     */
    fun build(
        snapUiBoxes: List<PBoxUIModel>,
        forceAuditRequired: Boolean = false,
        sourceResolution: androidx.compose.ui.unit.IntSize = androidx.compose.ui.unit.IntSize.Zero,
        targetViewSize: androidx.compose.ui.unit.IntSize = androidx.compose.ui.unit.IntSize.Zero
    ): PalletSpatialMap {
        if (snapUiBoxes.isEmpty()) return PalletSpatialMap.createEmptyObj()

        val sorted = snapUiBoxes
            .filter { it.classId != 3 }
            .sortedWith(compareBy({ it.boundingBox.top }, { it.boundingBox.left }))

        val nodes = mutableListOf<SpatialBoxNode>()
        var currentRow = 0
        var currentCol = 0
        var lastRowTop = sorted.firstOrNull()?.boundingBox?.top ?: 0f

        sorted.forEachIndexed { idx, box ->
            if (idx > 0 && (box.boundingBox.top - lastRowTop) > ROW_SNAP_PX) {
                currentRow++
                currentCol = 0
                lastRowTop = box.boundingBox.top
            }

            val main = box.palletBarcodes
                .firstOrNull { it.isMainBarcode && it.data.isNotEmpty() }


            val allBarcodeValues = box.palletBarcodes
                .filter { it.data.isNotEmpty() }
                .map { it.data }
                .toSet()

            val localizedCount = box.palletBarcodes.size

            val alreadyVerified = !forceAuditRequired && box.validation == BOX_VALIDATION.VERIFIED

            val node = SpatialBoxNode(
                spatialIndex      = idx,
                row               = currentRow,
                col               = currentCol,
                boundingBox       = box.boundingBox,
                trackId           = box.trackId,
                stableKey         = box.stableKey,
                hasPrimaryBarcode = main != null,
                primaryBarcodeValue = main?.data ?: "",
                snapBarcodeValues = allBarcodeValues,
                snapValidation    = box.validation,
                snapBarcodeCount  = allBarcodeValues.size,
                snapLocalizedBarcodeCount = localizedCount,
                accumulatedBarcodeValues = allBarcodeValues.toMutableSet(),
                barcodeVoteCounts = allBarcodeValues.associateWith { SNAP_VOTE_SEED }.toMutableMap(),
                isAuditRequired   = forceAuditRequired || box.validation != BOX_VALIDATION.VERIFIED,
                isAuditCompleted  = alreadyVerified,
                wandValidation    = if (alreadyVerified) BOX_VALIDATION.VERIFIED else BOX_VALIDATION.NOT_DETECTED
            )
            nodes.add(node)
            currentCol++
        }

        LOGI(TAG, "Built spatial map: ${nodes.size} nodes, ${currentRow + 1} rows, " +
                "${nodes.count { it.isAuditRequired }} require audit, " +
                "${nodes.count { it.isAuditCompleted }} pre-verified from snap")

        LOGI("SnapDiag", "═══ SpatialMap built: ${nodes.size} nodes ═══")
        nodes.forEach { n ->
            LOGI("SnapDiag", "  Node[${n.spatialIndex}] key='${n.stableKey.hashCode()}' snapVal=${n.snapValidation} " +
                "hasPrimary=${n.hasPrimaryBarcode} primary='${n.primaryBarcodeValue.hashCode()}' " +
                "decodedBc=${n.snapBarcodeCount} localizedBc=${n.snapLocalizedBarcodeCount} " +
                "auditRequired=${n.isAuditRequired} auditCompleted=${n.isAuditCompleted}")
        }

        val byGrid = nodes.associateBy { it.row to it.col }
        val linkedNodes = nodes.map { n ->
            n.copy(
                neighbours = SpatialNeighbours(
                    top         = byGrid[n.row - 1 to n.col]?.spatialIndex,
                    topLeft     = byGrid[n.row - 1 to n.col - 1]?.spatialIndex,
                    topRight    = byGrid[n.row - 1 to n.col + 1]?.spatialIndex,
                    left        = byGrid[n.row     to n.col - 1]?.spatialIndex,
                    right       = byGrid[n.row     to n.col + 1]?.spatialIndex,
                    bottomLeft  = byGrid[n.row + 1 to n.col - 1]?.spatialIndex,
                    bottom      = byGrid[n.row + 1 to n.col]?.spatialIndex,
                    bottomRight = byGrid[n.row + 1 to n.col + 1]?.spatialIndex
                )
            )
        }

        val byTrackId = linkedNodes
            .filter { it.trackId >= 0 }
            .associateBy { it.trackId }

        val barcodeValueCounts = mutableMapOf<String, Int>()
        linkedNodes.forEach { node ->
            if (node.primaryBarcodeValue.isNotEmpty()) {
                barcodeValueCounts[node.primaryBarcodeValue] =
                    (barcodeValueCounts[node.primaryBarcodeValue] ?: 0) + 1
            }
            if (node.stableKey.isNotEmpty() && node.stableKey != node.primaryBarcodeValue) {
                barcodeValueCounts[node.stableKey] =
                    (barcodeValueCounts[node.stableKey] ?: 0) + 1
            }
        }

        val byBarcode = buildMap<String, SpatialBoxNode> {
            linkedNodes.forEach { node ->
                if (node.primaryBarcodeValue.isNotEmpty() &&
                    barcodeValueCounts[node.primaryBarcodeValue] == 1) {
                    put(node.primaryBarcodeValue, node)
                }
                if (node.stableKey.isNotEmpty() &&
                    barcodeValueCounts[node.stableKey] == 1) {
                    put(node.stableKey, node)
                }
            }
        }

        val ambiguousCount = barcodeValueCounts.count { it.value > 1 }
        if (ambiguousCount > 0) {
            LOGI(TAG, "WARNING: $ambiguousCount barcode value(s) appear on multiple boxes — " +
                    "excluded from lookup. These boxes will resolve via trackId or grid position.")
        }

        val nodesWithoutPrimary = linkedNodes.filter { !it.hasPrimaryBarcode }
        if (nodesWithoutPrimary.isNotEmpty()) {
            LOGI(TAG, "⚠️ WARNING: ${nodesWithoutPrimary.size} node(s) have NO primary barcode:")
            nodesWithoutPrimary.take(5).forEach { node ->
                LOGI(TAG, "  - Node[${node.spatialIndex}] stableKey='${node.stableKey.hashCode()}' " +
                        "validation=${node.snapValidation} (will use spatial matching if trackId resets)")
            }
            if (nodesWithoutPrimary.size > 5) {
                LOGI(TAG, "  ... and ${nodesWithoutPrimary.size - 5} more")
            }
        }

        val rowCount = (linkedNodes.maxOfOrNull { it.row } ?: 0) + 1
        val colCount = (linkedNodes.maxOfOrNull { it.col } ?: 0) + 1

        return PalletSpatialMap(
            nodes            = linkedNodes.toMutableList(),
            nodeByTrackId    = byTrackId.toMutableMap(),
            nodeByBarcode    = byBarcode.toMutableMap(),
            rowCount         = rowCount,
            colCount         = colCount,
            sourceResolution = sourceResolution,
            targetViewSize   = targetViewSize
        )
    }
}

