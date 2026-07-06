// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.analyzers.customdetector.ocr;

import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;

import com.zebra.ai.vision.entity.DetectionEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * DetectionEntity subclass that carries ML Kit recognized text through the pipeline.
 *
 * Overrides transformWith() so that if COORDINATE_SYSTEM_VIEW_REFERENCED is used,
 * the SDK-applied transform is propagated to both the bounding box and the corner
 * points while preserving the text string.
 */
public class OcrTextEntity extends DetectionEntity {

    private final String text;

    public OcrTextEntity(Rect bbox, List<Point> corners, String text) {
        super(bbox, corners);
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public DetectionEntity transformWith(Matrix transform) {
        RectF mapped = new RectF(getBoundingBox());
        transform.mapRect(mapped);
        Rect mappedBox = new Rect(
                Math.round(mapped.left), Math.round(mapped.top),
                Math.round(mapped.right), Math.round(mapped.bottom)
        );

        List<Point> corners = getCorners();
        float[] pts = new float[corners.size() * 2];
        for (int i = 0; i < corners.size(); i++) {
            pts[i * 2]     = corners.get(i).x;
            pts[i * 2 + 1] = corners.get(i).y;
        }
        transform.mapPoints(pts);
        List<Point> mappedCorners = new ArrayList<>(corners.size());
        for (int i = 0; i < corners.size(); i++) {
            mappedCorners.add(new Point(Math.round(pts[i * 2]), Math.round(pts[i * 2 + 1])));
        }
        return new OcrTextEntity(mappedBox, mappedCorners, text);
    }
}
