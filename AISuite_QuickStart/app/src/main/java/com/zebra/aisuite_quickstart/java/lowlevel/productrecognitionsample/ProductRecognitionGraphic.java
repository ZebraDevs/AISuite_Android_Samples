// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.lowlevel.productrecognitionsample;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import com.zebra.aisuite_quickstart.GraphicOverlay;

import java.util.ArrayList;
import java.util.List;

/**
 * The ProductRecognitionGraphic class extends the GraphicOverlay.Graphic class and is responsible for
 * rendering visual elements on a canvas to represent detected products, shelves, and labels in an image.
 * This includes drawing bounding boxes around these elements and displaying associated text labels.
 *
 * This class utilizes Android's Canvas and Paint classes to perform drawing operations, allowing for
 * the visual representation of product recognition results in an Android application.
 *
 * Usage:
 * - Instantiate the ProductRecognitionGraphic with a reference to the GraphicOverlay, along with lists
 *   of bounding boxes for labels, shelves, and products, and a list of decoded product names.
 * - The draw(Canvas) method is called to render the graphics on the screen.
 *
 * Dependencies:
 * - GraphicOverlay: A custom view component that manages the drawing of multiple graphics
 *   on top of a camera preview or other content.
 * - Android Paint and Canvas classes: Used to perform drawing operations.
 *
 * Note: This class is typically used in conjunction with a product recognition system to
 * visually display the results of the recognition process in an Android application.
 */
public class ProductRecognitionGraphic extends GraphicOverlay.Graphic {
    private final Paint boxPaint;
    private final Paint contentTextPaint;
    private final Paint labelShelfPaint;
    private final Paint shelfPaint;
    private final List<Rect> labelShelfBBoxes = new ArrayList<>();
    private final List<Rect> labelPegBBoxes = new ArrayList<>();
    private final List<Rect> shelfBBoxes = new ArrayList<>();
    private final List<Rect> productBBoxes = new ArrayList<>();
    private final List<String> decodedProducts = new ArrayList<>();

    /**
     * Constructs a new ProductRecognitionGraphic object, initializing the Paint objects used for drawing
     * and preparing the bounding boxes and decoded product names for rendering.
     *
     * @param overlay The GraphicOverlay on which this graphic will be drawn.
    //     * @param labelShelfRects A list of Rect objects representing the bounding boxes of label shelves.
    //     * @param labelPegRects A list of Rect objects representing the bounding boxes of label pegs.
    //     * @param shelfRects A list of Rect objects representing the bounding boxes of shelves.
     * @param productRects A list of Rect objects representing the bounding boxes of recognized products.
     * @param productLabels A list of strings representing the decoded product names.
     */
    public ProductRecognitionGraphic(
            GraphicOverlay overlay,
            List<Rect> shelfRects,
            List<Rect> labelRects,
            List<Rect> productRects,
            List<String> productLabels
    ) {
        super(overlay);

        shelfBBoxes.clear();
        if (shelfRects != null) shelfBBoxes.addAll(shelfRects);

        labelShelfBBoxes.clear();
        if (labelRects != null) labelShelfBBoxes.addAll(labelRects);

        decodedProducts.clear();
        if (productLabels != null) decodedProducts.addAll(productLabels);

        productBBoxes.clear();
        if (productRects != null) productBBoxes.addAll(productRects);


        // Initialize the paint for drawing bounding boxes around products
        boxPaint = new Paint();
        boxPaint.setColor(Color.GREEN);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(6f);

        // Initialize the paint for drawing label shelves
        labelShelfPaint = new Paint();
        labelShelfPaint.setColor(Color.BLUE);
        labelShelfPaint.setStyle(Paint.Style.STROKE);
        labelShelfPaint.setAlpha(255);

        // Initialize the paint for drawing shelves
        shelfPaint = new Paint();
        shelfPaint.setColor(Color.MAGENTA);
        shelfPaint.setStyle(Paint.Style.STROKE);
        shelfPaint.setStrokeWidth(6f);
        shelfPaint.setAlpha(255);

        // Initialize the paint for drawing text
        contentTextPaint = new Paint();
        contentTextPaint.setColor(Color.WHITE);
        contentTextPaint.setAlpha(255);
        contentTextPaint.setTextSize(20F);


        // Trigger a redraw of the overlay
        postInvalidate();
    }

    /**
     * Draws the bounding boxes and text labels for detected products, shelves, and labels on the given Canvas.
     *
     * @param canvas The canvas on which to draw the graphic.
     */
    @Override
    public void draw(Canvas canvas) {
        for (Rect rect : labelShelfBBoxes) {
            canvas.drawRect(rect, labelShelfPaint);
            contentTextPaint.setTextSize(20f);
            canvas.drawText("LabelShelf", rect.left, rect.bottom, contentTextPaint);
        }

        for (Rect rect : labelPegBBoxes) {
            canvas.drawRect(rect, labelShelfPaint);
            contentTextPaint.setTextSize(20f);
            canvas.drawText("LabelPeg", rect.left, rect.bottom, contentTextPaint);
        }

        for (Rect rect : shelfBBoxes) {
            canvas.drawRect(rect, shelfPaint);
            contentTextPaint.setTextSize(30f);
            int centerX = rect.left + (rect.width() / 2);
            int centerY = rect.top + rect.height();
            canvas.drawText("Shelf", centerX, centerY, contentTextPaint);
        }

        for (int i = 0; i < decodedProducts.size(); i++) {
            canvas.drawRect(productBBoxes.get(i), boxPaint);
            getTextSizeWithinBounds(decodedProducts.get(i), productBBoxes.get(i).left, productBBoxes.get(i).top, productBBoxes.get(i).right, productBBoxes.get(i).bottom, contentTextPaint);
            canvas.drawText(
                    decodedProducts.get(i),
                    productBBoxes.get(i).left,
                    productBBoxes.get(i).bottom,
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
        float maxWidth = maxX - minX;
        float maxHeight = maxY - minY;

        float textSize = 100f; // Initial text size
        paint.setTextSize(textSize);

        Rect textBounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), textBounds);

        while ((textBounds.width() > maxWidth || textBounds.height() > maxHeight) && textSize > 0) {
            textSize -= 1; // Decrease the text size
            paint.setTextSize(textSize);
            paint.getTextBounds(text, 0, text.length(), textBounds);
        }

        Log.v("Text and size", text + " " + textSize);
    }
}