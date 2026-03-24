package com.zebra.aisuite_quickstart.java.detectors.warehouselocalizer;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.zebra.aisuite_quickstart.GraphicOverlay;

import java.util.ArrayList;
import java.util.List;

public class WareHouseLocalizerGraphic extends GraphicOverlay.Graphic {
    private final Paint boxPaint;
    private final List<Rect> boundingBoxes = new ArrayList<>();
    public WareHouseLocalizerGraphic(GraphicOverlay overlay, List<Rect> boxes) {
        super(overlay);
        overlay.clear();

        // Initialize the paint for drawing bounding boxes
        boxPaint = new Paint();
        boxPaint.setColor(Color.GREEN);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(6f);

        boundingBoxes.clear();

        // Populate bounding boxes if provided
        if (boxes != null) {
            boundingBoxes.addAll(boxes);
        }



        // Trigger a redraw of the overlay
        postInvalidate();
    }
    @Override
    public void draw(Canvas canvas) {
        // Draw bounding boxes
        for (Rect rect : boundingBoxes) {
            canvas.drawRect(rect, boxPaint);
        }

    }
}