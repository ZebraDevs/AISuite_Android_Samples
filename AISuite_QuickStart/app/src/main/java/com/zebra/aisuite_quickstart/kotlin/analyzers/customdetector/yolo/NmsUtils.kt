// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.analyzers.customdetector.yolo

/**
 * Non-Maximum Suppression: given parallel lists of [x1, y1, x2, y2] boxes and confidence
 * scores, returns the indices of boxes that survive the IoU cull.
 */
internal fun nms(
    boxes        : List<FloatArray>,
    scores       : FloatArray,
    iouThreshold : Float = 0.45f
): List<Int> {
    val order      = scores.indices.sortedByDescending { scores[it] }
    val suppressed = BooleanArray(scores.size)
    val keep       = mutableListOf<Int>()

    for (i in order) {
        if (suppressed[i]) continue
        keep += i
        for (j in order) {
            if (suppressed[j] || j == i) continue
            if (iou(boxes[i], boxes[j]) > iouThreshold) suppressed[j] = true
        }
    }
    return keep
}

private fun iou(a: FloatArray, b: FloatArray): Float {
    val x1 = maxOf(a[0], b[0])
    val y1 = maxOf(a[1], b[1])
    val x2 = minOf(a[2], b[2])
    val y2 = minOf(a[3], b[3])

    val interArea = (x2 - x1).coerceAtLeast(0f) * (y2 - y1).coerceAtLeast(0f)
    val aArea     = (a[2] - a[0]) * (a[3] - a[1])
    val bArea     = (b[2] - b[0]) * (b[3] - b[1])
    val unionArea = aArea + bArea - interArea
    return if (unionArea <= 0f) 0f else interArea / unionArea
}
