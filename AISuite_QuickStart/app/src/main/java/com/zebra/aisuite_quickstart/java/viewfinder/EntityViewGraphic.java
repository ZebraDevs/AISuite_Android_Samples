// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.viewfinder;

import android.graphics.Color;

import com.zebra.ai.vision.entity.BarcodeEntity;
import com.zebra.ai.vision.viewfinder.EntityViewController;
import com.zebra.ai.vision.viewfinder.pen.BoundingBoxDrawPen;
import com.zebra.ai.vision.viewfinder.pen.IconDrawPen;
import com.zebra.ai.vision.viewfinder.pen.IconStyle;
import com.zebra.ai.vision.viewfinder.pen.StylePen;

/**
 * The EntityViewGraphic class is responsible for rendering visual representations of barcode entities
 * on an entity view controller. It provides options to render entities using either bounding box styles
 * or icon styles, depending on the configuration. This class facilitates the display of detection and
 * decoding results in a user interface.

 * The class allows toggling between different rendering styles for detected barcodes, either showing
 * bounding boxes or icons, and provides methods to clear and render the graphics on the view controller.

 * Usage:
 * - Instantiate the EntityViewGraphic with an EntityViewController.
 * - Use addEntity() to add barcode entities for rendering.
 * - Call enableIconPen() to toggle between bounding box and icon rendering styles.
 * - Call clear() to remove all graphics from the view.
 * - Call render() to redraw the graphics on the view.

 * Dependencies:
 * - EntityViewController: Manages the addition and rendering of graphical elements.
 * - StylePen: Defines the style used for drawing bounding boxes or icons.
 * - BarcodeEntity: Represents the barcode entity to be displayed.

 * Note: This class is typically used in a barcode detection and visualization system to provide
 * user feedback through graphical representation of detection and decoding results.
 */
public class EntityViewGraphic {

    private final EntityViewController mViewController;
    private boolean isEnableIconStyle = false;
    private final StylePen detectBoxPen = new BoundingBoxDrawPen.Builder()
            .setStroke(5f, Color.rgb(255, 255, 0))
            .build();

    private final StylePen decodeBoxPen = new BoundingBoxDrawPen.Builder()
            .setStroke(5f, Color.rgb(0, 255, 0))
            .build();

    private final StylePen detectIconPen = new IconDrawPen.Builder().setIconStyle(IconStyle.DETECTION_ONLY)
            .build();

    private final StylePen decodeIconPen = new IconDrawPen.Builder().setIconStyle(IconStyle.SUCCESSFUL_DECODE)
            .build();

    /**
     * Constructs a new EntityViewGraphic with the specified EntityViewController.
     *
     * @param controller The EntityViewController responsible for managing graphical rendering.
     */
    public EntityViewGraphic(EntityViewController controller) {
        mViewController = controller;
    }

    /**
     * Clears all graphical elements from the view controller.
     */
    public void clear() {
        mViewController.removeAll();
    }

    /**
     * Renders the graphical elements on the view controller.
     */
    public void render() {
        mViewController.render();
    }

    /**
     * Enables or disables the icon style rendering mode.
     *
     * @param isEnable True to enable icon style rendering, false to use bounding box style.
     */
    public void enableIconPen(boolean isEnable) {
        isEnableIconStyle = isEnable;
    }

    /**
     * Adds a barcode entity to the view controller for rendering. The rendering style depends on
     * whether icon style is enabled or not.
     *
     * @param bEntity The BarcodeEntity to be added and rendered.
     */
    public void addEntity(BarcodeEntity bEntity) {
        if (isEnableIconStyle) {
            createAndDrawIconPen(bEntity);
        } else {
            createAndDrawBBoxPen(bEntity);
        }
    }

    /**
     * Creates and draws a bounding box style pen for the given barcode entity.
     *
     * @param bEntity The BarcodeEntity to be rendered.
     */
    private void createAndDrawBBoxPen(BarcodeEntity bEntity) {
        boolean hasValue = bEntity.getValue() != null && !bEntity.getValue().isEmpty();
        mViewController.add(bEntity, hasValue ? decodeBoxPen : detectBoxPen);
    }

    /**
     * Creates and draws an icon style pen for the given barcode entity.
     *
     * @param bEntity The BarcodeEntity to be rendered.
     */
    private void createAndDrawIconPen(BarcodeEntity bEntity) {
        boolean hasValue = bEntity.getValue() != null && !bEntity.getValue().isEmpty();
        mViewController.add(bEntity, hasValue ? decodeIconPen : detectIconPen);
    }
}

