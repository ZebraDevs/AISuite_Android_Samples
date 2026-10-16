package com.zebra.aidatacapturedemo.ui.view

/**
 * Configuration for the minimum-size guard applied to bounding-box overlays.
 *
 * This guard is only used by OCR overlay functions — barcode overlay functions
 * do not call [expandBBoxIfNeeded] at all, so there is no need for an on/off toggle.
 *
 * @property minSizePx    The minimum dimension (width or height) in canvas pixels.
 *                        Boxes smaller than this are expanded for legibility.
 * @property preserveAspectRatio If `true`, both dimensions are scaled proportionally
 *                        so the box's aspect ratio is kept intact. If `false`, only
 *                        the undersized dimension is expanded (legacy behaviour).
 */
data class MinSizeGuardConfig(
    val minSizePx: Float = 20f,
    val preserveAspectRatio: Boolean = true
)

/**
 * Holds the four edges of a bounding box in canvas-pixel coordinates.
 */
data class CanvasBBox(
    var left: Float,
    var top: Float,
    var right: Float,
    var bottom: Float
) {
    val width get() = right - left
    val height get() = bottom - top
}

/**
 * Expands a [CanvasBBox] according to the given [MinSizeGuardConfig].
 *
 * - When [MinSizeGuardConfig.preserveAspectRatio] is `true`, both dimensions are
 *   scaled by the same factor (derived from the smallest undersized dimension)
 *   so the box's shape stays the same.
 * - Otherwise, only the undersized dimension(s) are padded independently
 *   (legacy per-dimension behaviour).
 *
 * The result is clamped to `[0, canvasWidth]` × `[0, canvasHeight]`.
 *
 * @param bbox          The original bounding box in canvas coordinates.
 * @param config        Expansion rules.
 * @param canvasWidth   Maximum x-extent for clamping.
 * @param canvasHeight  Maximum y-extent for clamping.
 * @return A new [CanvasBBox] that satisfies the minimum-size constraints.
 */
fun expandBBoxIfNeeded(
    bbox: CanvasBBox,
    config: MinSizeGuardConfig,
    canvasWidth: Float,
    canvasHeight: Float
): CanvasBBox {

    val w = bbox.width
    val h = bbox.height
    val min = config.minSizePx

    // Both dimensions already meet the minimum — nothing to do
    if (w >= min && h >= min) return bbox

    val result = bbox.copy()

    if (config.preserveAspectRatio) {
        // Determine the scale factor needed so the smallest dimension reaches `min`.
        // Apply that same factor to both dimensions to keep aspect ratio intact.
        val scaleW = if (w > 0f && w < min) min / w else 1f
        val scaleH = if (h > 0f && h < min) min / h else 1f
        val scale = maxOf(scaleW, scaleH)

        val newW = w * scale
        val newH = h * scale

        // Expand symmetrically from the centre
        val cx = (bbox.left + bbox.right) / 2f
        val cy = (bbox.top + bbox.bottom) / 2f

        result.left = (cx - newW / 2f).coerceAtLeast(0f)
        result.right = (cx + newW / 2f).coerceAtMost(canvasWidth)
        result.top = (cy - newH / 2f).coerceAtLeast(0f)
        result.bottom = (cy + newH / 2f).coerceAtMost(canvasHeight)
    } else {
        // Legacy: pad each undersized dimension independently
        if (h < min) {
            val pad = (min - h) / 2f
            result.top = (bbox.top - pad).coerceAtLeast(0f)
            result.bottom = (bbox.bottom + pad).coerceAtMost(canvasHeight)
        }
        if (w < min) {
            val pad = (min - w) / 2f
            result.left = (bbox.left - pad).coerceAtLeast(0f)
            result.right = (bbox.right + pad).coerceAtMost(canvasWidth)
        }
    }

    return result
}

