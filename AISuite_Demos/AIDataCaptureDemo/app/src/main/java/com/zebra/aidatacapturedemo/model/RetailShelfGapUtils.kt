package com.zebra.aidatacapturedemo.model

import android.util.Log
import com.zebra.ai.vision.detector.BBox

private const val TAG = "RetailShelfGapUtils"
private const val CLS_PRODUCT = 1
private const val CLS_SHELF_LABEL = 2
private const val CLS_PEG_LABEL = 3
private const val CLS_SHELF = 4

private fun BBox.logStr() = "[x:%.0f-%.0f y:%.0f-%.0f prob:%.2f]".format(xmin, xmax, ymin, ymax, prob)

/** Why a candidate gap was (or wasn't) classified as a genuine empty shelf slot. */
enum class GapReason {
    GENUINE_EMPTY_SLOT,
    EDGE_EMPTY_SLOT,
    TOO_NARROW
}

data class ShelfGap(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val genuine: Boolean,
    val reason: GapReason,
    val widthPx: Float,
    val widthRatioToNeighborWidth: Float
)

data class GapDetectionConfig(
    // Below this fraction of the (smaller) neighbor's width, a gap is just normal packaging
    // spacing between two adjacent products, not a missing one.
    //
    // There is deliberately NO upper-bound width check here. It was tried (capping at various
    // multiples of the neighbor width) to distinguish "one missing product" from "several
    // consecutive missed detections," but on-device logs showed a single genuinely-missing item
    // can legitimately produce a ratio anywhere from ~1.2x up to ~2.9x depending on how wide that
    // specific item was relative to its neighbors — the same range previously assumed to mean
    // "multiple items missing." There's no reliable ceiling to separate the two from geometry
    // alone; every value tried just ended up rejecting a real single-item gap on the next shelf.
    val minGapRatioToNeighborWidth: Float = 0.7f,

    // Start/end gaps are anchored against the shelf rail's own cls==4 bounding box instead of a
    // second detected product — the same reference point tried (and rejected) earlier for exactly
    // this reason: the rail box can be detected short of or well past a slot's true edge. A
    // stricter (higher) ratio than between-gaps hedges against that imprecision, at the cost of
    // missing some genuinely-narrow edge gaps that the rail box happens to measure accurately.
    val minEdgeGapRatioToNeighborWidth: Float = 1.0f,

    // Unlike a between-gap (trustworthy detected products on BOTH sides), an edge gap only has one
    // trustworthy side — the other is the shelf box, which can be detected far wider than the row
    // it actually contains (e.g. a handful of small/spurious detections clustered away from the
    // rest of a wide shelf rail produces a start/end "gap" spanning nearly the whole shelf). A
    // between-gap deliberately has no ceiling (see above), but that reasoning doesn't carry over
    // here since only one side is trustworthy. 2.9x is the top of the range on-device logs showed
    // for a genuinely single missing item in the (more trustworthy) between-gap case — used here
    // as the ceiling past which an edge gap is more likely a mis-anchored/spurious row than a real
    // single missing slot.
    val maxEdgeGapRatioToNeighborWidth: Float = 2.9f
)

/**
 * Computes shelf-gap candidates from raw Localizer BBox output and classifies each as a genuine
 * empty shelf slot or not, with the specific [GapReason] that decided it.
 *
 * Gaps BETWEEN two actually-detected products (a trustworthy reference on both sides) are the
 * primary case and use [GapDetectionConfig.minGapRatioToNeighborWidth].
 *
 * Gaps at the very START (before the first product) or END (after the last product) of a row are
 * also reported, anchored against the shelf rail's own cls==4 bounding box. This exact reference
 * point was tried once before and abandoned: the rail box is sometimes detected short of or well
 * past where a real missing product's slot actually is — reproduced on-device even with a stricter
 * [GapDetectionConfig.minEdgeGapRatioToNeighborWidth]/[GapDetectionConfig.maxEdgeGapRatioToNeighborWidth]:
 * the shelf box can include physical structure (an end-cap/side panel) beyond the last usable peg
 * position, which the ratio thresholds alone can't distinguish from a genuinely empty slot.
 *
 * To fix that, an edge candidate additionally requires a real cls==2 (Shelf Label) or cls==3 (Peg
 * Label) bounding box physically inside the candidate span — see [rowHasLabelInRange]. Every
 * designated product slot has a label mounted to it, whether or not a product currently occupies
 * it (confirmed on-device: a genuinely empty slot still has its price tag/peg label), while the
 * shelf's structural edge beyond the last real slot does not. This makes label presence a ground
 * truth for "a slot exists here" that geometry/ratio thresholds alone can't provide. Tagged with
 * [GapReason.EDGE_EMPTY_SLOT] so callers/future tuning can still treat it as lower-confidence than
 * [GapReason.GENUINE_EMPTY_SLOT] if needed. The raw camera-frame boundary was also tried and is NOT
 * used here — it depends on the shelf's real edge roughly coinciding with the photo's edge, which
 * isn't reliably true.
 *
 * The shelf box is used ONLY for the horizontal (left/right) edge — never for top/bottom. A
 * detected shelf rail can span several physical rows at once (see [clusterIntoRows]'s own doc),
 * so using its full vertical extent inflated edge-gap boxes to cover multiple rows at once in
 * practice; top/bottom are anchored to the one adjacent product's own height instead, same as
 * every between-gap box already does.
 *
 * Geometry alone decides both WHERE a candidate gap is and whether to trust it — a gap's width is
 * compared directly against its own two immediate neighbor products' widths, filtering out only
 * trivially small spacing (packaging gaps). No upper bound, no shelf/row-wide median, no
 * confidence signal — see [GapDetectionConfig] for why.
 */
fun detectShelfGaps(
    bboxes: List<BBox>,
    config: GapDetectionConfig = GapDetectionConfig()
): List<ShelfGap> {
    val shelves = bboxes.filter { it.cls == CLS_SHELF }
    val products = bboxes.filter { it.cls == CLS_PRODUCT }
    val labels = bboxes.filter { it.cls == CLS_SHELF_LABEL || it.cls == CLS_PEG_LABEL }
    Log.d(TAG, "detectShelfGaps: ${shelves.size} shelves, ${products.size} products, ${labels.size} labels")
    shelves.forEachIndexed { i, s -> Log.d(TAG, "  shelf[$i] ${s.logStr()}") }
    products.forEachIndexed { i, p -> Log.d(TAG, "  product[$i] ${p.logStr()}") }
    if (shelves.isEmpty() || products.isEmpty()) {
        Log.d(TAG, "  no shelves or no products -> 0 gaps")
        return emptyList()
    }

    val productsByShelf = products.groupBy { product -> nearestShelfBelow(product, shelves) }
    productsByShelf.forEach { (shelf, ps) ->
        Log.d(
            TAG,
            "  grouped -> shelf=${shelf?.logStr() ?: "NONE (no qualifying shelf below)"} " +
                    "products=${ps.sortedBy { it.xmin }.joinToString { it.logStr() }}"
        )
    }

    val gaps = mutableListOf<ShelfGap>()

    shelves.forEach { shelf ->
        val shelfProducts = productsByShelf[shelf]
        if (shelfProducts == null) {
            Log.d(TAG, "  shelf ${shelf.logStr()} has no grouped products -> skip")
            return@forEach
        }

        // nearestShelfBelow only finds the closest shelf rail below a product — on a pegboard or
        // multi-row display, several distinct vertical rows can all share the SAME shelf rail
        // underneath them and land in this one group. Sorting that flat by xmin treats products
        // from different rows as if they were side-by-side, producing nonsense (often negative)
        // widths and corrupting the median. Row-cluster first so gaps are only ever computed
        // between products that are actually on the same physical row.
        val rows = clusterIntoRows(shelfProducts)
        Log.d(TAG, "  shelf ${shelf.logStr()} -> ${rows.size} row(s): ${rows.map { it.size }}")

        rows.forEach { row ->
            val rowProducts = row.sortedBy { it.xmin }
            if (rowProducts.isEmpty()) return@forEach

            val rowTop = rowProducts.minOf { it.ymin }
            val rowBottom = rowProducts.maxOf { it.ymax }

            val firstBox = rowProducts.first()
            val firstWidth = firstBox.xmax - firstBox.xmin
            val startGapPx = firstBox.xmin - shelf.xmin
            if (startGapPx >= firstWidth * config.minEdgeGapRatioToNeighborWidth &&
                startGapPx <= firstWidth * config.maxEdgeGapRatioToNeighborWidth &&
                rowHasLabelInRange(labels, rowTop, rowBottom, shelf.xmin, firstBox.xmin)
            ) {
                Log.d(
                    TAG,
                    "    start candidate shelf=${shelf.logStr()} first=${firstBox.logStr()} " +
                            "widthPx=$startGapPx ratio=${startGapPx / firstWidth} -> ${GapReason.EDGE_EMPTY_SLOT}"
                )
                gaps += ShelfGap(
                    left = shelf.xmin,
                    top = firstBox.ymin,
                    right = firstBox.xmin,
                    bottom = firstBox.ymax,
                    genuine = true,
                    reason = GapReason.EDGE_EMPTY_SLOT,
                    widthPx = startGapPx,
                    widthRatioToNeighborWidth = startGapPx / firstWidth
                )
            }

            val lastBox = rowProducts.last()
            val lastWidth = lastBox.xmax - lastBox.xmin
            val endGapPx = shelf.xmax - lastBox.xmax
            if (endGapPx >= lastWidth * config.minEdgeGapRatioToNeighborWidth &&
                endGapPx <= lastWidth * config.maxEdgeGapRatioToNeighborWidth &&
                rowHasLabelInRange(labels, rowTop, rowBottom, lastBox.xmax, shelf.xmax)
            ) {
                Log.d(
                    TAG,
                    "    end candidate shelf=${shelf.logStr()} last=${lastBox.logStr()} " +
                            "widthPx=$endGapPx ratio=${endGapPx / lastWidth} -> ${GapReason.EDGE_EMPTY_SLOT}"
                )
                gaps += ShelfGap(
                    left = lastBox.xmax,
                    top = lastBox.ymin,
                    right = shelf.xmax,
                    bottom = lastBox.ymax,
                    genuine = true,
                    reason = GapReason.EDGE_EMPTY_SLOT,
                    widthPx = endGapPx,
                    widthRatioToNeighborWidth = endGapPx / lastWidth
                )
            }

            if (rowProducts.size < 2) return@forEach

            for (i in 0 until rowProducts.size - 1) {
                val leftBox = rowProducts[i]
                val rightBox = rowProducts[i + 1]
                val leftWidth = leftBox.xmax - leftBox.xmin
                val rightWidth = rightBox.xmax - rightBox.xmin
                val minNeighborWidth = minOf(leftWidth, rightWidth)
                val maxNeighborWidth = maxOf(leftWidth, rightWidth)
                val widthPx = rightBox.xmin - leftBox.xmax
                val widthRatio = widthPx / maxNeighborWidth

                val reason = when {
                    widthPx < minNeighborWidth * config.minGapRatioToNeighborWidth -> GapReason.TOO_NARROW
                    else -> GapReason.GENUINE_EMPTY_SLOT
                }
                Log.d(
                    TAG,
                    "    candidate[$i] left=${leftBox.logStr()} right=${rightBox.logStr()} " +
                            "widthPx=$widthPx ratio=$widthRatio -> $reason"
                )

                gaps += ShelfGap(
                    left = leftBox.xmax,
                    top = minOf(leftBox.ymin, rightBox.ymin),
                    right = rightBox.xmin,
                    bottom = maxOf(leftBox.ymax, rightBox.ymax),
                    genuine = reason == GapReason.GENUINE_EMPTY_SLOT,
                    reason = reason,
                    widthPx = widthPx,
                    widthRatioToNeighborWidth = widthRatio
                )
            }
        }
    }
    Log.d(TAG, "detectShelfGaps: total candidates=${gaps.size} genuine=${gaps.count { it.genuine }}")
    return gaps
}

/**
 * True if at least one shelf/peg [labels] entry sits inside [left]..[right] and is vertically
 * plausible for this row (within one row-height of it — generous, since a peg label dangles at
 * roughly hook height while a shelf-edge price tag sits below the whole row on the rail).
 *
 * This is the ground-truth check that lets an edge candidate be trusted: every designated product
 * slot has a label mounted to it, occupied or not, but the shelf's own structural edge (an
 * end-cap/side panel beyond the last real slot) does not — so "no label in this span" means
 * "not a real slot," regardless of how the width ratio checks out.
 */
private fun rowHasLabelInRange(
    labels: List<BBox>,
    rowTop: Float,
    rowBottom: Float,
    left: Float,
    right: Float
): Boolean {
    val rowHeight = rowBottom - rowTop
    return labels.any { label ->
        val labelCenterX = (label.xmin + label.xmax) / 2f
        val verticallyNearRow = label.ymin >= rowTop - rowHeight && label.ymin <= rowBottom + rowHeight * 2f
        labelCenterX in left..right && verticallyNearRow
    }
}

/**
 * Clusters [products] into rows by vertical position: sorted by y-center, a product joins the
 * current row if its y-center is within half a typical product height of that row's running
 * average y-center, otherwise it starts a new row. Comparing against the row's running average
 * (not just the previous product) tolerates the normal jitter of pegboard-hook heights within one
 * real row while still separating genuinely distinct rows sharing the same shelf rail below them.
 */
private fun clusterIntoRows(products: List<BBox>): List<List<BBox>> {
    if (products.isEmpty()) return emptyList()
    val medianHeight = products.map { it.ymax - it.ymin }.sorted().let { it[it.size / 2] }
    val rowThreshold = medianHeight * 0.5f

    val sortedByYCenter = products.sortedBy { (it.ymin + it.ymax) / 2f }
    val rows = mutableListOf<MutableList<BBox>>()
    var currentRow = mutableListOf(sortedByYCenter.first())
    var rowYCenterSum = (sortedByYCenter.first().ymin + sortedByYCenter.first().ymax) / 2f

    for (i in 1 until sortedByYCenter.size) {
        val product = sortedByYCenter[i]
        val productYCenter = (product.ymin + product.ymax) / 2f
        val rowAvgYCenter = rowYCenterSum / currentRow.size
        if (productYCenter - rowAvgYCenter <= rowThreshold) {
            currentRow.add(product)
            rowYCenterSum += productYCenter
        } else {
            rows.add(currentRow)
            currentRow = mutableListOf(product)
            rowYCenterSum = productYCenter
        }
    }
    rows.add(currentRow)
    return rows
}

/**
 * Picks the shelf rail closest below this product (smallest gap between the product's bottom
 * edge and the shelf rail's top edge), among shelves the product horizontally overlaps enough
 * to plausibly belong to. Requires the shelf to actually be at/below the product —using an
 * unsigned distance here previously let a product get matched to a shelf rail from a completely
 * different row above it when the correct rail underneath wasn't cleanly detected,producing
 * gap boxes that span multiple shelf rows. Returns null if no shelf qualifies.
 */
private fun nearestShelfBelow(product: BBox, shelves: List<BBox>): BBox? {
    val tolerance = (product.ymax - product.ymin) * 0.5f
    return shelves
        .filter { shelf ->
            val overlapLeft = maxOf(product.xmin, shelf.xmin)
            val overlapRight = minOf(product.xmax, shelf.xmax)
            val horizontalOverlap = (overlapRight - overlapLeft).coerceAtLeast(0f)
            val overlapsHorizontally = horizontalOverlap > (product.xmax -
                    product.xmin) * 0.3f
            val isBelowProduct = shelf.ymin >= product.ymax - tolerance
            overlapsHorizontally && isBelowProduct
        }
        .minByOrNull { shelf -> shelf.ymin - product.ymax }
}
