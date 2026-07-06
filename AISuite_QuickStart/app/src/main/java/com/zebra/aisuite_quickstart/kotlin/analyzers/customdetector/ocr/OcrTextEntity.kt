// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.analyzers.customdetector.ocr

import android.graphics.Matrix
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import com.zebra.ai.vision.entity.DetectionEntity
import kotlin.math.roundToInt

/**
 * DetectionEntity subclass that carries the ML Kit recognized text through the pipeline.
 *
 * Overrides transformWith() so that if COORDINATE_SYSTEM_VIEW_REFERENCED is used,
 * the SDK-applied transform is propagated to both the bounding box and the corner
 * points while preserving the text string.
 */
class OcrTextEntity(
    bbox   : Rect,
    corners: List<Point>,
    val text: String
) : DetectionEntity(bbox, corners) {

    override fun transformWith(transform: Matrix): DetectionEntity {
        val mapped = RectF(boundingBox)
        transform.mapRect(mapped)
        val mappedBox = Rect(
            mapped.left.roundToInt(),
            mapped.top.roundToInt(),
            mapped.right.roundToInt(),
            mapped.bottom.roundToInt()
        )

        val points = FloatArray(corners.size * 2)
        corners.forEachIndexed { i, p ->
            points[i * 2]     = p.x.toFloat()
            points[i * 2 + 1] = p.y.toFloat()
        }
        transform.mapPoints(points)
        val mappedCorners = corners.indices.map { i ->
            Point(points[i * 2].roundToInt(), points[i * 2 + 1].roundToInt())
        }

        return OcrTextEntity(mappedBox, mappedCorners, text)
    }
}
