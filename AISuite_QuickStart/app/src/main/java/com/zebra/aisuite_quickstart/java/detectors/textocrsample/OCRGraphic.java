// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.detectors.textocrsample;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import com.zebra.aisuite_quickstart.GraphicOverlay;

import java.util.ArrayList;
import java.util.List;
import static java.lang.Math.abs;

/**
 * The OCRGraphic class extends the GraphicOverlay.Graphic class and is responsible for
 * rendering visual elements on a canvas to represent detected text from an OCR (Optical
 * Character Recognition) process. This includes drawing bounding boxes around detected
 * text areas and displaying the recognized text within these boxes.
 *
 * This class uses the Android Canvas and Paint classes to perform drawing operations,
 * allowing for the visual representation of text detection results in an Android application.
 *
 * Usage:
 * - Instantiate the OCRGraphic with a reference to the GraphicOverlay, along with lists
 *   of bounding boxes and recognized text strings.
 * - The draw(Canvas) method is called to render the graphics on the screen.
 *
 * Dependencies:
 * - GraphicOverlay: A custom view component that manages the drawing of multiple graphics
 *   on top of a camera preview or other content.
 * - Android Paint and Canvas classes: Used to perform drawing operations.
 *
 * Note: This class is typically used in conjunction with an OCR detection system to
 * visually display the results of the detection process in an Android application.
 */
public class OCRGraphic extends GraphicOverlay.Graphic {
    private final Paint boxPaint;
    private final Paint contentTextPaint;
    private final List<Rect> boundingBoxes = new ArrayList<>();
    private final List<String> decodedValues = new ArrayList<>();

    /**
     * Constructs a new OCRGraphic object, initializing the Paint objects used for drawing
     * and preparing the bounding boxes and recognized text for rendering.
     *
     * @param overlay The GraphicOverlay on which this graphic will be drawn.
     * @param boxes A list of Rect objects representing the bounding boxes of detected text.
     * @param decodedStrings A list of strings representing the recognized content of each text area.
     */
    public OCRGraphic(GraphicOverlay overlay, List<Rect> boxes, List<String> decodedStrings) {
        super(overlay);
     //   overlay.clear();

        // Initialize the paint for drawing bounding boxes
        boxPaint = new Paint();
        boxPaint.setColor(Color.GREEN);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(6f);

        // Initialize the paint for drawing text
        contentTextPaint = new Paint();
        contentTextPaint.setColor(Color.WHITE);
        contentTextPaint.setAlpha(255);
        contentTextPaint.setTextSize(36F);
        boundingBoxes.clear();

        // Populate bounding boxes if provided
        if (boxes != null) {
            boundingBoxes.addAll(boxes);
        }

        // Populate decoded values if provided
        decodedValues.clear();
        if (decodedStrings != null) {
            decodedValues.addAll(decodedStrings);
        }

        // Trigger a redraw of the overlay
        postInvalidate();
    }

    /**
     * Draws the bounding boxes and recognized text on the given Canvas.
     *
     * @param canvas The canvas on which to draw the graphic.
     */
    @Override
    public void draw(Canvas canvas) {
        // Draw bounding boxes
        for (Rect rect : boundingBoxes) {
            canvas.drawRect(rect, boxPaint);
        }

        // Draw the text content of the OCR
        for (int i = 0; i < decodedValues.size(); i++) {
            getTextSizeWithinBounds(decodedValues.get(i), boundingBoxes.get(i).left, boundingBoxes.get(i).top, boundingBoxes.get(i).right, boundingBoxes.get(i).bottom, contentTextPaint);
            canvas.drawText(
                    decodedValues.get(i),
                    boundingBoxes.get(i).left,
                    boundingBoxes.get(i).bottom,
                    contentTextPaint
            );
        }
    }

    /**
     * Adjusts the text size to ensure that it fits within the specified bounds.
     *
     * @param text The text to be drawn.
     * @param minX The minimum x-coordinate of the bounding area.
     * @param minY The minimum y-coordinate of the bounding area.
     * @param maxX The maximum x-coordinate of the bounding area.
     * @param maxY The maximum y-coordinate of the bounding area.
     * @param paint The Paint object used for drawing the text.
     */
    private void getTextSizeWithinBounds(String text, float minX, float minY, float maxX, float maxY, Paint paint) {
        // Define the maximum width and height the text should fit into
        float maxWidth = abs(maxX - minX);
        float maxHeight = abs(maxY - minY);

        // Start with a reasonable text size
        float textSize = 100f; // Initial text size
        paint.setTextSize(textSize);

        // Create a Rect to store text bounds
        Rect textBounds = new Rect();

        // Measure text and adjust size
        paint.getTextBounds(text, 0, text.length(), textBounds);

        while ((textBounds.width() > maxWidth || textBounds.height() > maxHeight) && textSize > 0) {
            textSize -= 1; // Decrease the text size
            paint.setTextSize(textSize);
            paint.getTextBounds(text, 0, text.length(), textBounds);
        }

        Log.v("Text and size", text + " " + textSize);
    }
}
