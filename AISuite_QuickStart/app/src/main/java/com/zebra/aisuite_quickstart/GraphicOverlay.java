// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * The GraphicOverlay class is a custom view designed to manage and render graphical elements
 * overlaid on top of another view, such as a camera preview. This class maintains a list of
 * graphics to be drawn and provides methods to add, clear, and render these graphics on a canvas.

 * The GraphicOverlay is intended to be used within an Android application for scenarios where
 * visual elements need to be dynamically drawn over a base view, such as in augmented reality
 * or camera-based applications.

 * Usage:
 * - Instantiate the GraphicOverlay as part of your view hierarchy in XML or programmatically.
 * - Use add() to add Graphic objects to be rendered.
 * - Use clear() to remove all graphics from the overlay.
 * - Override the Graphic.draw() method to define custom drawing behavior for each graphic.

 * Dependencies:
 * - Android View: Provides the base functionality for custom views.
 * - Canvas: Used for drawing operations.

 * Thread Safety:
 * - Uses synchronization to ensure thread-safe access to the list of graphics.

 * Note: Ensure that the appropriate permissions and dependencies are configured
 * in the AndroidManifest and build files to utilize camera and image processing capabilities.
 */
public class GraphicOverlay extends View {
    private final Object lock = new Object();
    private final List<Graphic> graphics = new ArrayList<>();

    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Adds a graphic to the overlay.
     *
     * @param graphic The graphic object to be added.
     */
    public void add(Graphic graphic) {
        synchronized (lock) {
            graphics.add(graphic);
        }
        postInvalidate();
    }

    /**
     * Removes all graphics from the overlay.
     */
    public void clear() {
        synchronized (lock) {
            graphics.clear();
        }
        postInvalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        synchronized (lock) {
            for (Graphic graphic : graphics) {
                graphic.draw(canvas);
            }
        }
    }

    /**
     * Base class for a custom graphics object to be rendered within the graphic overlay.
     * Extend this class to implement custom drawing behavior for each graphic.
     */
    public abstract static class Graphic {
        private final GraphicOverlay overlay;

        public Graphic(GraphicOverlay overlay) {
            this.overlay = overlay;
        }

        /**
         * Draws the graphic on the given canvas.
         *
         * @param canvas The canvas on which to draw the graphic.
         */
        public abstract void draw(Canvas canvas);

        /**
         * Invalidates the overlay, causing it to be redrawn.
         */
        protected void postInvalidate() {
            overlay.postInvalidate();
        }
    }
}

