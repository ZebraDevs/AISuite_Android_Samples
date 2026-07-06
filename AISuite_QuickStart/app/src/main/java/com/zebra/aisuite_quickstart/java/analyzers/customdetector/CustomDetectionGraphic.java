// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.analyzers.customdetector;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.zebra.aisuite_quickstart.GraphicOverlay;

import java.util.List;

/**
 * CustomDetectionGraphic renders bounding boxes for all four custom detector model outputs.
 * Each model type is drawn in a distinct color so results are visually distinct:
 *
 *   Cyan   — Barcode Decoder
 *   Yellow — ML Kit OCR
 *   Green  — YOLO Object Detection
 *   Red    — MobileNet SSD Object Detection
 *
 * All bounding boxes must already be mapped to overlay coordinates by the caller via
 * BoundingBoxMapper.mapBoundingBoxToOverlay() before being passed to this graphic.
 */
public class CustomDetectionGraphic extends GraphicOverlay.Graphic {

    private final List<Rect>   barcodeRects;
    private final List<String> barcodeLabels;
    private final List<Rect>   ocrRects;
    private final List<String> ocrLabels;
    private final List<Rect>   yoloRects;
    private final List<Rect>   mobileNetRects;

    private final Paint strokePaint;
    private final Paint labelPaint;

    public CustomDetectionGraphic(
            GraphicOverlay overlay,
            List<Rect>   barcodeRects,
            List<String> barcodeLabels,
            List<Rect>   ocrRects,
            List<String> ocrLabels,
            List<Rect>   yoloRects,
            List<Rect>   mobileNetRects) {
        super(overlay);
        this.barcodeRects    = barcodeRects;
        this.barcodeLabels   = barcodeLabels;
        this.ocrRects        = ocrRects;
        this.ocrLabels       = ocrLabels;
        this.yoloRects       = yoloRects;
        this.mobileNetRects  = mobileNetRects;

        strokePaint = new Paint();
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(5f);
        strokePaint.setAntiAlias(true);

        labelPaint = new Paint();
        labelPaint.setTextSize(36f);
        labelPaint.setAntiAlias(true);
        labelPaint.setFakeBoldText(true);
    }

    @Override
    public void draw(Canvas canvas) {
        for (int i = 0; i < barcodeRects.size(); i++) {
            Rect box = barcodeRects.get(i);
            drawBox(canvas, box, Color.CYAN);
            String label = i < barcodeLabels.size() ? barcodeLabels.get(i) : "";
            if (!label.isEmpty()) {
                drawLabel(canvas, label, box, Color.CYAN);
            }
        }
        for (int i = 0; i < ocrRects.size(); i++) {
            Rect box = ocrRects.get(i);
            drawBox(canvas, box, Color.YELLOW);
            String label = i < ocrLabels.size() ? ocrLabels.get(i) : "";
            if (!label.isEmpty()) {
                drawLabel(canvas, label, box, Color.YELLOW);
            }
        }
        for (Rect box : yoloRects) {
            drawBox(canvas, box, Color.GREEN);
        }
        for (Rect box : mobileNetRects) {
            drawBox(canvas, box, Color.RED);
        }
    }

    private void drawBox(Canvas canvas, Rect box, int color) {
        strokePaint.setColor(color);
        canvas.drawRect(box, strokePaint);
    }

    private void drawLabel(Canvas canvas, String text, Rect box, int color) {
        labelPaint.setColor(color);
        String label = text.length() > 20 ? text.substring(0, 20) + "…" : text;
        canvas.drawText(label, box.left, box.top - 8f, labelPaint);
    }
}
