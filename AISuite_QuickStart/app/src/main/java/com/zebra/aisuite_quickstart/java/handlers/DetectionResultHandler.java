// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.handlers;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.zebra.ai.vision.detector.BBox;
import com.zebra.ai.vision.detector.BarcodeDecoder;
import com.zebra.ai.vision.detector.ComplexBBox;
import com.zebra.ai.vision.detector.Recognizer;
import com.zebra.ai.vision.detector.SKUInfo;
import com.zebra.ai.vision.detector.Word;
import com.zebra.ai.vision.entity.BarcodeEntity;
import com.zebra.ai.vision.entity.Entity;
import com.zebra.ai.vision.entity.LabelEntity;
import com.zebra.ai.vision.entity.LineEntity;
import com.zebra.ai.vision.entity.LocalizerEntity;
import com.zebra.ai.vision.entity.ParagraphEntity;
import com.zebra.ai.vision.entity.ProductEntity;
import com.zebra.ai.vision.entity.ShelfEntity;
import com.zebra.ai.vision.entity.WordEntity;
import com.zebra.aisuite_quickstart.java.CameraXLivePreviewActivity;
import com.zebra.aisuite_quickstart.java.analyzers.tracker.TrackerGraphic;
import com.zebra.aisuite_quickstart.java.camera.CameraManager;
import com.zebra.aisuite_quickstart.java.detectors.barcodedecodersample.BarcodeGraphic;
import com.zebra.aisuite_quickstart.java.detectors.productrecognition.ProductRecognitionGraphic;
import com.zebra.aisuite_quickstart.java.detectors.textocrsample.OCRGraphic;
import com.zebra.aisuite_quickstart.java.detectors.warehouselocalizer.WareHouseLocalizerGraphic;
import com.zebra.aisuite_quickstart.java.viewfinder.EntityViewGraphic;
import com.zebra.aisuite_quickstart.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * DetectionResultHandler manages all detection callbacks and handles UI updates
 * for various detection types (barcode, OCR, product recognition, etc.)
 */
public class DetectionResultHandler {
    private static final String TAG = "DetectionResultHandler";

    private final CameraXLivePreviewActivity activity;
    private final BoundingBoxMapper boundingBoxMapper;
    private final float SIMILARITY_THRESHOLD = 0.65f;
    private final CameraManager cameraManager;
    private final List<ShelfEntity> capturedShelves = new ArrayList<>();
    private final List<Rect> capturedShelfOverlayRects = new ArrayList<>();
    private List<Entity> capturedEntities = new ArrayList<>();
    private final List<RectF> capturedShelfViewRects = new ArrayList<>(); // view-space rects for hit-testing on ImageView
    private Matrix imageToOverlayMatrix;
    private final List<ProductEntity> capturedProducts = new ArrayList<>();
    private final List<RectF> capturedProductViewRects = new ArrayList<>();
    private final List<LabelEntity> capturedLabels = new ArrayList<>();
    private final List<RectF> capturedLabelViewRects = new ArrayList<>();

    public DetectionResultHandler(CameraXLivePreviewActivity activity, BoundingBoxMapper boundingBoxMapper, CameraManager cameraManager) {
        this.activity = activity;
        this.boundingBoxMapper = boundingBoxMapper;
        this.cameraManager = cameraManager;
    }

    // Barcode detection result handler
    public void handleBarcodeDetection(List<BarcodeEntity> result) {
        List<Rect> rects = new ArrayList<>();
        List<String> decodedStrings = new ArrayList<>();

        activity.runOnUiThread(() -> {
            activity.getBinding().graphicOverlay.clear();
            if (result != null) {
                for (BarcodeEntity bb : result) {
                    Rect rect = bb.getBoundingBox();
                    if (rect != null) {
                        Log.d(TAG, String.format("Original bbox: %s", rect));
                        Rect overlayRect = boundingBoxMapper.mapBoundingBoxToOverlay(rect);
                        Log.d(TAG, String.format("Mapped bbox: %s", overlayRect));
                        rects.add(overlayRect);
                        decodedStrings.add(bb.getValue());
                    }
                    Log.e(TAG, "Detected entity - Value: " + bb.getValue());
                    Log.e(TAG, "Detected entity - Symbology: " + bb.getSymbology());
                }
                activity.getBinding().graphicOverlay.add(new BarcodeGraphic(activity.getBinding().graphicOverlay, rects, decodedStrings));
            }
        });
    }

    // Legacy barcode detection result handler
    public void handleLegacyBarcodeDetection(BarcodeDecoder.Result[] barcodes) {
        List<Rect> rects = new ArrayList<>();
        List<String> decodedStrings = new ArrayList<>();

        activity.runOnUiThread(() -> {
            activity.getBinding().graphicOverlay.clear();
            for (BarcodeDecoder.Result barcode : barcodes) {
                String decodedString = barcode.value;
                BBox bbox = barcode.bboxData;
                Rect rect = new Rect((int) bbox.xmin, (int) bbox.ymin, (int) bbox.xmax, (int) bbox.ymax);
                Rect overlayRect = boundingBoxMapper.mapBoundingBoxToOverlay(rect);
                rects.add(overlayRect);
                decodedStrings.add(barcode.value);

                Log.d(TAG, "Symbology Type " + barcode.symbologytype);
                Log.d(TAG, "Decoded barcode: " + decodedString);
            }
            activity.getBinding().graphicOverlay.add(new BarcodeGraphic(activity.getBinding().graphicOverlay, rects, decodedStrings));
        });
    }

    // OCR text detection result handler
    public void handleTextOCRDetection(List<ParagraphEntity> list) {
        List<Rect> rects = new ArrayList<>();
        List<String> decodedStrings = new ArrayList<>();

        activity.runOnUiThread(() -> {
            activity.getBinding().graphicOverlay.clear();
            for (ParagraphEntity entity : list) {
                List<LineEntity> lines = entity.getLines();
                for (LineEntity line : lines) {
                    for (WordEntity word : line.getWords()) {
                        ComplexBBox bbox = word.getComplexBBox();
                        if (!word.getText().isEmpty()) {
                            if (bbox != null && bbox.x != null && bbox.y != null &&
                                    bbox.x.length >= 4 && bbox.y.length >= 4) {
                                float minX = Math.min(Math.min(bbox.x[0], bbox.x[1]), Math.min(bbox.x[2], bbox.x[3]));
                                float maxX = Math.max(Math.max(bbox.x[0], bbox.x[1]), Math.max(bbox.x[2], bbox.x[3]));
                                float minY = Math.min(Math.min(bbox.y[0], bbox.y[1]), Math.min(bbox.y[2], bbox.y[3]));
                                float maxY = Math.max(Math.max(bbox.y[0], bbox.y[1]), Math.max(bbox.y[2], bbox.y[3]));
                                Rect rect = new Rect((int) minX, (int) minY, (int) maxX, (int) maxY);
                                Rect overlayRect = boundingBoxMapper.mapBoundingBoxToOverlay(rect);
                                rects.add(overlayRect);
                                decodedStrings.add(word.getText());
                            }
                        }
                    }
                }
            }
            activity.getBinding().graphicOverlay.add(new OCRGraphic(activity.getBinding().graphicOverlay, rects, decodedStrings));
        });
    }

    // Legacy OCR detection result handler
    public void handleLegacyTextOCRDetection(Word[] words) {
        List<Rect> rects = new ArrayList<>();
        List<String> decodedStrings = new ArrayList<>();

        activity.runOnUiThread(() -> {
            activity.getBinding().graphicOverlay.clear();
            for (Word word : words) {
                if (word.decodes.length > 0) {
                    ComplexBBox bbox = word.bbox;
                    if (bbox != null && bbox.x != null && bbox.y != null &&
                            bbox.x.length >= 3 && bbox.y.length >= 3) {
                        float minX = bbox.x[0], maxX = bbox.x[2], minY = bbox.y[0], maxY = bbox.y[2];
                        Rect rect = new Rect((int) minX, (int) minY, (int) maxX, (int) maxY);
                        Rect overlayRect = boundingBoxMapper.mapBoundingBoxToOverlay(rect);
                        rects.add(overlayRect);
                        decodedStrings.add(word.decodes[0].content);
                    }
                }
            }
            activity.getBinding().graphicOverlay.add(new OCRGraphic(activity.getBinding().graphicOverlay, rects, decodedStrings));
        });
    }

    public void handleLegacyProductRecognitionResult(BBox[] detections, BBox[] products, Recognizer.Recognition[] recognitions) {
        activity.runOnUiThread(() -> {
            activity.getBinding().graphicOverlay.clear();
            if (detections != null) {
                List<Rect> labelShelfRects = new ArrayList<>();
                List<Rect> labelPegRects = new ArrayList<>();
                List<Rect> shelfRects = new ArrayList<>();
                List<Rect> recognizedRects = new ArrayList<>();
                List<String> decodedStrings = new ArrayList<>();
                List<Rect> barcodeRects = new ArrayList<>();
                List<String> barcodeTexts = new ArrayList<>();

                BBox[] labelShelfObjects = Arrays.stream(detections).filter(x -> x.cls == 2).toArray(BBox[]::new);
                BBox[] labelPegObjects = Arrays.stream(detections).filter(x -> x.cls == 3).toArray(BBox[]::new);
                BBox[] shelfObjects = Arrays.stream(detections).filter(x -> x.cls == 4).toArray(BBox[]::new);

                for (BBox bBox : labelShelfObjects) {
                    Rect rect = new Rect((int) bBox.xmin, (int) bBox.ymin, (int) bBox.xmax, (int) bBox.ymax);
                    Rect overlayRect = boundingBoxMapper.mapBoundingBoxToOverlay(rect);
                    labelShelfRects.add(overlayRect);
                }

                for (BBox bBox : labelPegObjects) {
                    Rect rect = new Rect((int) bBox.xmin, (int) bBox.ymin, (int) bBox.xmax, (int) bBox.ymax);
                    Rect overlayRect = boundingBoxMapper.mapBoundingBoxToOverlay(rect);
                    labelPegRects.add(overlayRect);
                }

                for (BBox bBox : shelfObjects) {
                    Rect rect = new Rect((int) bBox.xmin, (int) bBox.ymin, (int) bBox.xmax, (int) bBox.ymax);
                    Rect overlayRect = boundingBoxMapper.mapBoundingBoxToOverlay(rect);
                    shelfRects.add(overlayRect);
                }

                if (recognitions.length == 0) {
                    decodedStrings.add("No products found");
                    recognizedRects.add(new Rect(250, 250, 0, 0));
                } else {
                    Log.v(TAG, "products length :" + products.length + " recognitions length: " + recognitions.length);
                    for (int i = 0; i < products.length; i++) {
                        if (recognitions[i].similarity[0] > SIMILARITY_THRESHOLD) {
                            BBox bBox = products[i];
                            Rect rect = new Rect((int) bBox.xmin, (int) bBox.ymin, (int) bBox.xmax, (int) bBox.ymax);
                            Rect overlayRect = boundingBoxMapper.mapBoundingBoxToOverlay(rect);
                            recognizedRects.add(overlayRect);
                            decodedStrings.add(recognitions[i].sku[0]);
                        }
                    }
                }

                activity.getBinding().graphicOverlay.add(new ProductRecognitionGraphic(activity.getBinding().graphicOverlay, labelShelfRects, labelPegRects, shelfRects, recognizedRects, decodedStrings, barcodeRects, barcodeTexts));
            }
        });
    }

    // Product recognition detection result handler
    public void handleDetectionRecognitionResult(List<Entity> result) {
        Log.d(TAG, "Inside On Shelf RecognitionResult (flat hierarchy)");
        activity.runOnUiThread(() -> {
            activity.getBinding().graphicOverlay.clear();
            if (result == null) return;

            List<ShelfEntity> shelves = new ArrayList<>();
            List<LabelEntity> labels = new ArrayList<>();
            List<ProductEntity> products = new ArrayList<>();
            for (Entity entity : result) {
                if (entity instanceof ShelfEntity) {
                    shelves.add((ShelfEntity) entity);
                } else if (entity instanceof LabelEntity) {
                    labels.add((LabelEntity) entity);
                } else if (entity instanceof ProductEntity) {
                    products.add((ProductEntity) entity);
                }
            }

            List<Rect> shelfRects = new ArrayList<>();
            List<Rect> labelPegRects = new ArrayList<>();
            List<Rect> labelShelfRects = new ArrayList<>();
            List<Rect> productRects = new ArrayList<>();
            List<String> productLabels = new ArrayList<>();
            List<Rect> barcodeRects = new ArrayList<>();
            List<String> barcodeTexts = new ArrayList<>();
                    for (int i = 0; i < shelves.size(); i++) {
                        ShelfEntity shelf = shelves.get(i);
                        Rect shelfRect = boundingBoxMapper.mapBoundingBoxToOverlay(shelf.getBoundingBox());
                        shelfRects.add(shelfRect);

                        // Use ShelfEntity associations
                        List<LabelEntity> shelfLabels = shelf.getLabels();
                        if (shelfLabels != null) {
                            for (LabelEntity label : shelfLabels) {
                                Rect lsr = boundingBoxMapper.mapBoundingBoxToOverlay(label.getBoundingBox());
                                labelShelfRects.add(lsr);

                                List<BarcodeEntity> barcodes = label.getBarcodes();
                                Log.d(TAG, "Barcodes size: " + barcodes.size());
                                if (!barcodes.isEmpty()) {
                                    for (BarcodeEntity barcode : barcodes) {
                                        Rect barcodeRect = barcode.getBoundingBox();
                                        Log.d(TAG, "Detected entity - Value: " + barcode.getValue());
                                        Log.d(TAG, "Detected entity - Symbology: " + barcode.getSymbology());
                                        Rect barRect = boundingBoxMapper.mapBoundingBoxToOverlay(barcodeRect);
                                        barcodeRects.add(barRect);
                                        barcodeTexts.add(barcode.getValue());
                                    }
                                }
                            }
                        }
                    }

            // Draw all products (regardless of shelf assignment)
            for (ProductEntity product : products) {
                Rect prodRect = boundingBoxMapper.mapBoundingBoxToOverlay(product.getBoundingBox());
                productRects.add(prodRect);
                String topSku = "";
                if (product.getAccuracy() >= SIMILARITY_THRESHOLD) {
                    List<SKUInfo> skuInfos = product.getTopKSKUs();
                    if (skuInfos != null && !skuInfos.isEmpty()) {
                        topSku = skuInfos.get(0).getProductSKU();
                    }
                }
                productLabels.add(topSku);

                Log.d(TAG, String.format(
                        "SKU=%s, Product bbox=%s",
                        topSku, prodRect
                ));
            }

            activity.getBinding().graphicOverlay.add(new ProductRecognitionGraphic(activity.getBinding().graphicOverlay, labelShelfRects, labelPegRects, shelfRects, productRects, productLabels, barcodeRects,
                    barcodeTexts)
            );
        });
    }

    // Entity tracker detection result handler
    public void handleEntityTrackerDetection(List<? extends Entity> barcodeEntities, List<? extends Entity> ocrEntities, List<? extends Entity> moduleEntities) {
        List<Rect> barcodeRects = new ArrayList<>();
        List<String> barcodeStrings = new ArrayList<>();
        List<Rect> ocrRects = new ArrayList<>();
        List<String> ocrStrings = new ArrayList<>();
        List<Rect> shelfRects = new ArrayList<>();
        List<Rect> labelPegRects = new ArrayList<>();
        List<Rect> labelShelfRects = new ArrayList<>();
        List<Rect> productRects = new ArrayList<>();
        List<String> productLabels = new ArrayList<>();


        activity.runOnUiThread(() -> {
            activity.getBinding().graphicOverlay.clear();
            if (barcodeEntities != null) {
                for (Entity entity : barcodeEntities) {
                    if (entity instanceof BarcodeEntity) {
                        BarcodeEntity bEntity = (BarcodeEntity) entity;
                        Rect rect = bEntity.getBoundingBox();
                        if (rect != null) {
                            Rect overlayRect = boundingBoxMapper.mapBoundingBoxToOverlay(rect);
                            barcodeRects.add(overlayRect);
                            String hashCode = String.valueOf(bEntity.hashCode());
                            // Ensure the string has at least 4 characters
                            if (hashCode.length() >= 4) {
                                // Get the last four digits
                                hashCode = hashCode.substring(hashCode.length() - 4);

                            }
                            if(!bEntity.getValue().trim().isEmpty()) {
                                barcodeStrings.add(hashCode + ":" + bEntity.getValue());
                            }
                            else{
                                barcodeStrings.add("");
                            }
                            Log.d(TAG, "Tracker UUID: " + hashCode + " Tracker Detected entity - Value: " + bEntity.getValue());
                        }
                    }
                }
            }
            if (ocrEntities != null) {
                for (Entity entity : ocrEntities) {
                    if (entity instanceof ParagraphEntity) {
                        ParagraphEntity pEntity = (ParagraphEntity) entity;
                        Log.i(TAG, "Paragraph Entity detected" + pEntity);
                        List<LineEntity> lineEntities = pEntity.getLines();
                        Log.i(TAG, "Lines detected" + lineEntities.size());
                        for (LineEntity lineEntity : lineEntities) {

                            for (WordEntity wordEntity : lineEntity.getWords()) {
                                ComplexBBox bbox = wordEntity.getComplexBBox();

                                if (bbox != null && bbox.x != null && bbox.y != null && bbox.x.length >= 4 && bbox.y.length >= 4) {
                                    float minX = Math.min(Math.min(bbox.x[0], bbox.x[1]), Math.min(bbox.x[2], bbox.x[3]));
                                    float maxX = Math.max(Math.max(bbox.x[0], bbox.x[1]), Math.max(bbox.x[2], bbox.x[3]));
                                    float minY = Math.min(Math.min(bbox.y[0], bbox.y[1]), Math.min(bbox.y[2], bbox.y[3]));
                                    float maxY = Math.max(Math.max(bbox.y[0], bbox.y[1]), Math.max(bbox.y[2], bbox.y[3]));

                                    Rect rect = new Rect((int) minX, (int) minY, (int) maxX, (int) maxY);
                                    Rect overlayRect = boundingBoxMapper.mapBoundingBoxToOverlay(rect);
                                    String decodedValue = wordEntity.getText();
                                    ocrRects.add(overlayRect);
                                    ocrStrings.add(decodedValue);
                                }
                            }
                        }
                    }

                }
            }
            if (moduleEntities != null) {
                List<ShelfEntity> shelves = new ArrayList<>();
                List<LabelEntity> labels = new ArrayList<>();
                List<ProductEntity> products = new ArrayList<>();
                for (Entity entity : moduleEntities) {
                    if (entity instanceof ShelfEntity) {
                        shelves.add((ShelfEntity) entity);
                    } else if (entity instanceof LabelEntity) {
                        labels.add((LabelEntity) entity);
                    } else if (entity instanceof ProductEntity) {
                        products.add((ProductEntity) entity);
                    }
                }

                for (ShelfEntity shelf : shelves) {
                    Rect shelfRect = boundingBoxMapper.mapBoundingBoxToOverlay(shelf.getBoundingBox());
                    shelfRects.add(shelfRect);
                }
                // Draw all labels (if you want to show all, not just those attached to shelves)
                for (LabelEntity label : labels) {
                    if (label.getClassId() == LabelEntity.ClassId.PEG_LABEL) {
                        Rect labelRect = boundingBoxMapper.mapBoundingBoxToOverlay(label.getBoundingBox());
                        labelPegRects.add(labelRect);
                    }
                    if (label.getClassId() == LabelEntity.ClassId.SHELF_LABEL) {
                        Rect labelRect = boundingBoxMapper.mapBoundingBoxToOverlay(label.getBoundingBox());
                        labelShelfRects.add(labelRect);
                    }
                }
                for (ProductEntity product : products) {
                    Rect prodRect = boundingBoxMapper.mapBoundingBoxToOverlay(product.getBoundingBox());
                    productRects.add(prodRect);
                    String topSku = "";
                    if (product.getAccuracy() >= SIMILARITY_THRESHOLD) {
                        List<SKUInfo> skuInfos = product.getTopKSKUs();
                        if (skuInfos != null && !skuInfos.isEmpty()) {
                            topSku = skuInfos.get(0).getProductSKU();
                        }
                    }
                    productLabels.add(topSku);
                }

                // Add the product recognition overlay
                activity.getBinding().graphicOverlay.add(
                        new ProductRecognitionGraphic(
                                activity.getBinding().graphicOverlay,
                                labelShelfRects,
                                labelPegRects,
                                shelfRects,
                                productRects,
                                productLabels,
                                null,
                                null
                        )
                );
            }
            if (!barcodeRects.isEmpty())
                activity.getBinding().graphicOverlay.add(new TrackerGraphic(activity.getBinding().graphicOverlay, barcodeRects, barcodeStrings));
            if (!ocrRects.isEmpty())
                activity.getBinding().graphicOverlay.add(new OCRGraphic(activity.getBinding().graphicOverlay, ocrRects, ocrStrings));
            if (!shelfRects.isEmpty() || !labelShelfRects.isEmpty() || !labelPegRects.isEmpty() || !productRects.isEmpty() || !productLabels.isEmpty())
                activity.getBinding().graphicOverlay.add(new ProductRecognitionGraphic(activity.getBinding().graphicOverlay, labelShelfRects, labelPegRects, shelfRects, productRects, productLabels, null, null));
        });
    }

    // Entity view finder detection result handler
    public void handleEntityViewFinderDetection(List<Entity> entities, EntityViewGraphic entityViewGraphic) {
        activity.runOnUiThread(() -> {
            entityViewGraphic.clear();

            for (Entity entity : entities) {
                if (entity instanceof BarcodeEntity) {
                    BarcodeEntity bEntity = (BarcodeEntity) entity;
                    Rect rect = bEntity.getBoundingBox();
                    if (rect != null) {
                        Log.d(TAG, "Adding entity to view - Value: " + bEntity.getValue() + ", BBox: " + rect);
                        entityViewGraphic.addEntity(bEntity);
                    } else {
                        Log.w(TAG, "Entity has null bounding box - Value: " + bEntity.getValue());
                    }
                }
            }
            entityViewGraphic.render();
        });
    }
    public void handleImageCaptureBarcodeResult(List<BarcodeEntity> entities) {
        if (activity.getCapturedBitmap() == null || entities == null) {
            Log.w(TAG, "Cannot overlay: bitmap or entities null");
            return;
        }

        Bitmap annotated = activity.getCapturedBitmap().copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(annotated);

        // Extract barcodes and bounding boxes (same logic as handleBarcodeDetection)
        List<Rect> rects = new ArrayList<>();
        List<String> decodedStrings = new ArrayList<>();

        for (BarcodeEntity entity : entities) {
            Rect rect = entity.getBoundingBox();
            if (rect != null) {
                rects.add(rect);
                decodedStrings.add(entity.getValue());
            }
        }

        // Draw barcodes using BarcodeGraphic style (matching preview)
        Paint rectPaint = new Paint();
        rectPaint.setColor(Color.GREEN);
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(16f);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.DKGRAY); // Dark gray text like preview
        textPaint.setTextSize(60f);
        textPaint.setAlpha(255);

        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.WHITE); // White background like preview
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setStrokeWidth(6f);

        int contentPadding = 40;

        for (int i = 0; i < rects.size(); i++) {
            Rect rect = rects.get(i);
            String text = decodedStrings.get(i);

            // Draw bounding box
            canvas.drawRect(rect, rectPaint);

            // Draw text with white background (matching preview style)
            if (text != null && !text.isEmpty()) {
                int textWidth = (int) textPaint.measureText(text);

                // Calculate background rectangle position and size (matching BarcodeGraphic)
                Rect contentRect = new Rect(
                        rect.left,
                        rect.bottom + contentPadding / 2,
                        rect.left + textWidth + contentPadding * 2,
                        rect.bottom + (int) textPaint.getTextSize() + contentPadding
                );

                // Draw white background rectangle
                canvas.drawRect(contentRect, backgroundPaint);

                // Draw dark gray text
                canvas.drawText(text, rect.left + contentPadding , rect.bottom + contentPadding * 2, textPaint);
             //   CommonUtils.getTextSizeWithinBounds(canvas, text, rect.left, rect.top, rect.right, rect.bottom, textPaint);
            }
        }

        activity.runOnUiThread(() -> {
            activity.getBinding().capturedImageView.setImageBitmap(annotated);
            Log.d(TAG, "Overlayed " + rects.size() + " barcode detections");
        });
    }
    public void handleImageCaptureTextResult(List<ParagraphEntity> entities) {
        if (activity.getCapturedBitmap() == null || entities == null) {
            Log.w(TAG, "Cannot overlay: frozenFrame=" + cameraManager.getImageCapture());
            return;
        }

        Bitmap annotated = activity.getCapturedBitmap().copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(annotated);
        Log.d(TAG, "Drawing detections on canvas");
        // Extract text and bounding boxes (same logic as handleTextOCRDetection)
        List<Rect> rects = new ArrayList<>();
        List<String> decodedStrings = new ArrayList<>();

        for (ParagraphEntity entity : entities) {
            List<LineEntity> lines = entity.getLines();
            for (LineEntity line : lines) {
                for (WordEntity word : line.getWords()) {
                    ComplexBBox bbox = word.getComplexBBox();
                    if (!word.getText().isEmpty()) {
                        if (bbox != null && bbox.x != null && bbox.y != null &&
                                bbox.x.length >= 3 && bbox.y.length >= 3) {
                            float minX = bbox.x[0], maxX = bbox.x[2], minY = bbox.y[0], maxY = bbox.y[2];
                            Rect rect = new Rect((int) minX, (int) minY, (int) maxX, (int) maxY);
                            // Note: For captured image, you may not need boundingBoxMapper transformation
                            // If the image coordinates match the detection coordinates directly
                            rects.add(rect);
                            decodedStrings.add(word.getText());
                        }
                    }
                }
            }
        }

        // Draw rectangles and text on canvas
        Paint rectPaint = new Paint();
        rectPaint.setColor(Color.GREEN); // Green color
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(6f);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(60f);
        textPaint.setAlpha(255);
        //textPaint.setAntiAlias(true);

        for (int i = 0; i < rects.size(); i++) {
            Rect rect = rects.get(i);
            String text = decodedStrings.get(i);

            // Draw bounding box
            canvas.drawRect(rect, rectPaint);

            // Draw text above the box
            CommonUtils.getTextSizeWithinBounds(canvas, text, rect.left, rect.top, rect.right, rect.bottom, textPaint);
            //  canvas.drawText(text, rect.left, rect.top - 5, textPaint);
        }

        activity.runOnUiThread(() -> {
            activity.getBinding().capturedImageView.setImageBitmap(annotated);
            Log.d(TAG, "Overlayed " + rects.size() + " detections");
        });
    }

    public void handleImageCaptureRecognitionResult(List<Entity> entities) {
        Bitmap currentCapture = activity.getCapturedBitmap();
        if (currentCapture == null || entities == null) {
            Log.w(TAG, "Cannot overlay: frozenFrame=" + currentCapture);
            return;
        }

        Bitmap annotated = currentCapture.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(annotated);

        Paint productPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        productPaint.setStyle(Paint.Style.STROKE);
        productPaint.setStrokeWidth(8f);
        productPaint.setColor(Color.rgb(0, 255, 0));

        Paint shelfPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shelfPaint.setColor(Color.YELLOW);
        shelfPaint.setStyle(Paint.Style.STROKE);
        shelfPaint.setStrokeWidth(8f);
        shelfPaint.setAlpha(255);

        Paint labelShelfPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelShelfPaint.setColor(Color.BLUE);
        labelShelfPaint.setStyle(Paint.Style.STROKE);
        labelShelfPaint.setStrokeWidth(6f);
        labelShelfPaint.setAlpha(255);

        Paint barcodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barcodePaint.setColor(Color.RED);
        barcodePaint.setStyle(Paint.Style.STROKE);
        barcodePaint.setStrokeWidth(6f);
        barcodePaint.setAlpha(255);

        List<ShelfEntity> shelves = new ArrayList<>();
        List<ProductEntity> products = new ArrayList<>();

        for (Entity entity : entities) {
            if (entity instanceof ShelfEntity) {
                shelves.add((ShelfEntity) entity);
            } else if (entity instanceof ProductEntity) {
                products.add((ProductEntity) entity);
            }
        }

        // Draw shelves, associated labels, and associated barcodes first
        for (ShelfEntity shelf : shelves) {
            Rect shelfBBox = shelf.getBoundingBox();
            if (shelfBBox != null) {
                Rect shelfRect = boundingBoxMapper.mapBoundingBoxToOverlay(shelfBBox);
                canvas.drawRect(shelfRect, shelfPaint);
            }

            List<LabelEntity> shelfLabels = shelf.getLabels();
            if (shelfLabels != null) {
                for (LabelEntity label : shelfLabels) {
                    Rect labelBBox = label.getBoundingBox();
                    if (labelBBox != null) {
                        Rect labelRect = boundingBoxMapper.mapBoundingBoxToOverlay(labelBBox);
                        canvas.drawRect(labelRect, labelShelfPaint);
                    }

                    List<BarcodeEntity> barcodes = label.getBarcodes();
                    if (barcodes != null) {
                        for (BarcodeEntity barcode : barcodes) {
                            Rect barcodeBBox = barcode.getBoundingBox();
                            if (barcodeBBox != null) {
                                Rect barcodeRect = boundingBoxMapper.mapBoundingBoxToOverlay(barcodeBBox);
                                canvas.drawRect(barcodeRect, barcodePaint);
                            }
                        }
                    }
                }
            }
        }

        // Draw products after shelves/labels/barcodes
        for (ProductEntity product : products) {
            Rect productBBox = product.getBoundingBox();
            if (productBBox != null) {
                Rect productRect = boundingBoxMapper.mapBoundingBoxToOverlay(productBBox);
                canvas.drawRect(productRect, productPaint);
            }
        }

        activity.runOnUiThread(() -> {
            activity.getBinding().capturedImageView.setImageBitmap(annotated);
            Log.d(TAG, "Overlay recognition detections on captured bitmap");
        });
    }


    public void overlayShelfAssociations(@NonNull ShelfEntity shelf) {
        Bitmap currentCapture = activity.getCapturedBitmap();
        if (currentCapture == null) {
            Log.w(TAG, "overlayShelfAssociations: no captured bitmap");
            return;
        }
        Bitmap annotated = currentCapture.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(annotated);

        Paint shelfPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shelfPaint.setStyle(Paint.Style.STROKE);
        shelfPaint.setStrokeWidth(8f);
        shelfPaint.setColor(Color.YELLOW);

        Paint labelShelfPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelShelfPaint.setStyle(Paint.Style.STROKE);
        labelShelfPaint.setStrokeWidth(6f);
        labelShelfPaint.setColor(Color.CYAN);

        Paint labelPegPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPegPaint.setStyle(Paint.Style.STROKE);
        labelPegPaint.setStrokeWidth(6f);
        labelPegPaint.setColor(Color.MAGENTA);

        Paint productPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        productPaint.setStyle(Paint.Style.STROKE);
        productPaint.setStrokeWidth(8f);
        productPaint.setColor(Color.GREEN);

        Paint barcodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barcodePaint.setStyle(Paint.Style.FILL);
        barcodePaint.setStrokeWidth(6f);
        barcodePaint.setColor(Color.BLUE);

        Paint barTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barTextPaint.setColor(Color.WHITE);
        barTextPaint.setTextSize(60f);
        barTextPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(42f);
        textPaint.setStyle(Paint.Style.FILL);

        // Draw shelf bbox
        if (shelf.getBoundingBox() != null) {
            canvas.drawRect(shelf.getBoundingBox(), shelfPaint);
        }

        List<LabelEntity> labels = shelf.getLabels();
        if (labels != null) {
            for (LabelEntity l : labels) {
                if (l.getBoundingBox() == null) continue;
                if (l.getClassId() == LabelEntity.ClassId.SHELF_LABEL) {
                    canvas.drawRect(l.getBoundingBox(), labelShelfPaint);
                } else if (l.getClassId() == LabelEntity.ClassId.PEG_LABEL) {
                    canvas.drawRect(l.getBoundingBox(), labelPegPaint);
                }

                // Draw barcodes for label
                Rect entityRect = l.getBoundingBox();
                List<BarcodeEntity> barcodes = l.getBarcodes();
                if (barcodes != null) {
                    for (BarcodeEntity barcode : barcodes) {
                        Rect barcodeRect = barcode.getBoundingBox();

                        String barcodeText = barcode.getValue();
                        Rect barTextBounds = new Rect();
                        barTextPaint.getTextBounds(barcodeText, 0, barcodeText.length(), barTextBounds);
                        int barTextLeft = barcodeRect.left;
                        int barTextBottom = entityRect.bottom + 30;
                        int barTextRight = barTextLeft + barTextBounds.width();
                        int barTextTop = barTextBottom - barTextBounds.height();

                        Rect barTextBgRect = new Rect(barTextLeft, barTextTop, barTextRight, barTextBottom);
                        canvas.drawRect(barTextBgRect, barcodePaint); // Use a fill paint for background
                        canvas.drawText(barcodeText, barTextLeft, barTextBottom, barTextPaint);
                    }
                }

            }
        }

        List<ProductEntity> products = shelf.getProducts();
        if (products != null) {
            for (ProductEntity p : products) {
                if (p.getBoundingBox() == null) continue;
                canvas.drawRect(p.getBoundingBox(), productPaint);
                String sku = "";
                List<SKUInfo> topK = p.getTopKSKUs();
                if (topK != null && !topK.isEmpty() && topK.get(0) != null) {
                    sku = String.valueOf(topK.get(0).getProductSKU());
                }
                if (!sku.isEmpty()) {
                    Rect r = p.getBoundingBox();
                    canvas.drawText(sku, r.left + 8, Math.max(0, r.top - 10), textPaint);
                }

            }
        }



        activity.runOnUiThread(() -> {
            activity.getBinding().capturedImageView.setImageBitmap(annotated);
            Log.d(TAG, "Overlayed tapped shelf associations on captured bitmap");
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    public void enableShelfTapOnCapturedBitmap(List<Entity> result, Bitmap capturedBitmap, ImageView imageView) {
        Log.d(TAG, "Enable shelf-tap on captured bitmap");
        activity.runOnUiThread(() -> {

            if (result == null || result.isEmpty() || capturedBitmap == null || imageView == null) {
                Log.w(TAG, "Missing result/bitmap/imageView");
                return;
            }

            // Keep entities and collect shelves
            capturedEntities = new ArrayList<>(result);
            capturedShelves.clear();
            capturedProducts.clear();
            capturedLabels.clear();

            for (Entity e : capturedEntities) {
                if (e instanceof ShelfEntity) capturedShelves.add((ShelfEntity) e);
                if (e instanceof ProductEntity) capturedProducts.add((ProductEntity) e);
                if (e instanceof LabelEntity) capturedLabels.add((LabelEntity) e);
            }
            capturedShelves.sort(java.util.Comparator.comparingInt(s -> s.getBoundingBox().top));
            Log.d(TAG, "Shelves collected: " + capturedShelves.size());
            Log.d(TAG, "Products collected: " + capturedProducts.size());
            Log.d(TAG, "Labels collected: " + capturedLabels.size());

            // Precompute ImageView-space rects for hit-testing on the bitmap view
            capturedShelfViewRects.clear();
            for (int i = 0; i < capturedShelves.size(); i++) {
                ShelfEntity shelf = capturedShelves.get(i);
                Rect shelfImgRect = shelf.getBoundingBox();
                RectF vf = new RectF(shelfImgRect);
                Matrix im = new Matrix(imageView.getImageMatrix());
                im.mapRect(vf);
                capturedShelfViewRects.add(vf);
                Log.d(TAG, "Shelf[" + i + "] imageRect=" + shelfImgRect.toShortString()
                        + " | viewRect=(" + vf.left + "," + vf.top + "," + vf.right + "," + vf.bottom + ")");
            }
            capturedProductViewRects.clear();
            for (ProductEntity product : capturedProducts) {
                Rect productImgRect = product.getBoundingBox();
                RectF vf = new RectF(productImgRect);
                Matrix im = new Matrix(imageView.getImageMatrix());
                im.mapRect(vf);
                capturedProductViewRects.add(vf);
            }
            capturedLabelViewRects.clear();
            for (LabelEntity label : capturedLabels) {
                Rect labelImgRect = label.getBoundingBox();
                RectF vf = new RectF(labelImgRect);
                Matrix im = new Matrix(imageView.getImageMatrix());
                im.mapRect(vf);
                capturedLabelViewRects.add(vf);
            }

            // Initial full render (you can comment this out if you only want overlays on tap)
            handleDetectionRecognitionResult(result);
            Log.d(TAG, "Initial full render done for captured bitmap");

            // Tap listener on the bitmap view (ImageView)
            imageView.setClickable(true);
            final int touchSlop = ViewConfiguration.get(imageView.getContext()).getScaledTouchSlop();
            final long tapTimeout = ViewConfiguration.getTapTimeout();

            imageView.setOnTouchListener(new View.OnTouchListener() {
                float downX, downY; long downTime; boolean movedOutside;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            downX = event.getX(); downY = event.getY();
                            downTime = System.currentTimeMillis(); movedOutside = false;
                            Log.d(TAG, "ACTION_DOWN @ (" + downX + "," + downY + ")");
                            v.setPressed(true);
                            return true;
                        case MotionEvent.ACTION_MOVE: {
                            float dx = event.getX() - downX, dy = event.getY() - downY;
                            if (Math.hypot(dx, dy) > touchSlop) movedOutside = true;
                            return true;
                        }
                        case MotionEvent.ACTION_UP: {
                            v.setPressed(false);
                            long duration = System.currentTimeMillis() - downTime;
                            boolean isTap = !movedOutside && duration <= tapTimeout;
                            float upX = event.getX(), upY = event.getY();
                            Log.d(TAG, "ACTION_UP @ (" + upX + "," + upY + "), isTap=" + isTap + ", movedOutside=" + movedOutside + ", duration=" + duration + "ms");

                            if (isTap) {
                                Log.d(TAG, "Tap detected, checking shelves...");
                                for (int i = 0; i < capturedShelfViewRects.size(); i++) {
                                    RectF viewRect = capturedShelfViewRects.get(i);
                                    if (viewRect != null && viewRect.contains(upX, upY)) {
                                        ShelfEntity tappedShelf = capturedShelves.get(i);
                                        Rect imgRect = tappedShelf.getBoundingBox();
                                        Log.d(TAG, "Tap is on shelf[" + i + "] imageRect=" + imgRect.toShortString()
                                                + " | viewRect=(" + viewRect.left + "," + viewRect.top + "," + viewRect.right + "," + viewRect.bottom + ")");
//                                        logShelfAssociations(tappedShelf);
                                        overlayShelfAssociations(tappedShelf);
                                        v.performClick();
                                        return true;
                                    }
                                }
                                Log.d(TAG, "Tap detected but not on any shelf");
                                handleDetectionRecognitionResult(result);
                                activity.getBinding().graphicOverlay.clear();
                                v.performClick();
                                return true;
                            }
                            return true;
                        }
                        case MotionEvent.ACTION_CANCEL:
                            v.setPressed(false);
                            Log.d(TAG, "ACTION_CANCEL");
                            return true;
                    }
                    return false;
                }
            });

            // Ensure overlay stays above
            activity.getBinding().graphicOverlay.bringToFront();
        });
    }



    /**
     * Unified handler for capture mode that processes barcode, OCR, and module recognition results
     * and draws them directly on a canvas (for captured image)
     */
    public void handleCaptureEntityTrackerDetection(List<? extends Entity> barcodeEntities, List<? extends Entity> ocrEntities, List<? extends Entity> moduleEntities) {
        if (activity.getCapturedBitmap() == null ) {
            Log.w(TAG, "Cannot overlay: bitmap or entities null");
            return;
        }

        Bitmap annotated = activity.getCapturedBitmap().copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(annotated);

        // Paint configurations for barcodes
        Paint barcodePaint = new Paint();
        barcodePaint.setColor(Color.GREEN);
        barcodePaint.setStyle(Paint.Style.STROKE);
        barcodePaint.setStrokeWidth(12f);
        barcodePaint.setAntiAlias(true);

        Paint barcodeTextPaint = new Paint();
        barcodeTextPaint.setColor(Color.DKGRAY);
        barcodeTextPaint.setTextSize(60f);
        barcodeTextPaint.setStyle(Paint.Style.FILL);
        barcodeTextPaint.setAntiAlias(true);

        Paint barcodeBackgroundPaint = new Paint();
        barcodeBackgroundPaint.setColor(Color.WHITE);
        barcodeBackgroundPaint.setStyle(Paint.Style.FILL);

        // Paint configurations for OCR
        Paint ocrPaint = new Paint();
        ocrPaint.setColor(Color.YELLOW);
        ocrPaint.setStyle(Paint.Style.STROKE);
        ocrPaint.setStrokeWidth(6f);
        ocrPaint.setAntiAlias(true);

        Paint ocrTextPaint = new Paint();
        ocrTextPaint.setColor(Color.WHITE);
        ocrTextPaint.setTextSize(60f);
        ocrTextPaint.setStyle(Paint.Style.FILL);
        ocrTextPaint.setAntiAlias(true);

        Paint ocrBackgroundPaint = new Paint();
        ocrBackgroundPaint.setColor(Color.WHITE);
        ocrBackgroundPaint.setStyle(Paint.Style.FILL);

        // Paint configurations for module recognition
        Paint shelfPaint = new Paint();
        shelfPaint.setColor(Color.BLUE);
        shelfPaint.setStyle(Paint.Style.STROKE);
        shelfPaint.setStrokeWidth(6f);
        shelfPaint.setAntiAlias(true);

        Paint labelShelfPaint = new Paint();
        labelShelfPaint.setColor(Color.CYAN);
        labelShelfPaint.setStyle(Paint.Style.STROKE);
        labelShelfPaint.setStrokeWidth(6f);
        labelShelfPaint.setAntiAlias(true);

        Paint labelPegPaint = new Paint();
        labelPegPaint.setColor(Color.MAGENTA);
        labelPegPaint.setStyle(Paint.Style.STROKE);
        labelPegPaint.setStrokeWidth(6f);
        labelPegPaint.setAntiAlias(true);

        Paint productPaint = new Paint();
        productPaint.setColor(Color.RED);
        productPaint.setStyle(Paint.Style.STROKE);
        productPaint.setStrokeWidth(6f);
        productPaint.setAntiAlias(true);

        Paint productTextPaint = new Paint();
        productTextPaint.setColor(Color.WHITE);
        productTextPaint.setTextSize(48f);
        productTextPaint.setStyle(Paint.Style.FILL);
        productTextPaint.setAntiAlias(true);

        Paint productBackgroundPaint = new Paint();
        productBackgroundPaint.setColor(Color.argb(180, 0, 0, 0));
        productBackgroundPaint.setStyle(Paint.Style.FILL);

        // Process barcode entities from capture
        if (barcodeEntities != null) {
            for (Entity entity : barcodeEntities) {
                if (entity instanceof BarcodeEntity) {
                    BarcodeEntity barcodeEntity = (BarcodeEntity) entity;
                    Rect rect = barcodeEntity.getBoundingBox();

                    if (rect != null) {
                        Log.d(TAG, "Capture - Drawing barcode: " + barcodeEntity.getValue() + ", bbox: " + rect);
                        Rect overlayRect = boundingBoxMapper.mapBoundingBoxToOverlay(rect);

                        // Draw bounding box
                        canvas.drawRect(
                                overlayRect.left,
                                overlayRect.top,
                                overlayRect.right,
                                overlayRect.bottom,
                                barcodePaint
                        );

                        // Draw text with background
                        String text = barcodeEntity.getValue();
                        Rect textBounds = new Rect();
                        barcodeTextPaint.getTextBounds(text, 0, text.length(), textBounds);

                        float textX = overlayRect.left;
                        float textY = overlayRect.top - 10f;

                        // Draw background rectangle for text
                        canvas.drawRect(
                                textX,
                                textY - textBounds.height() - 5f,
                                textX + textBounds.width() + 10f,
                                textY + 5f,
                                barcodeBackgroundPaint
                        );

                        // Draw text
                        canvas.drawText(text, textX + 5f, textY, barcodeTextPaint);
                    }
                }
            }
        }

        // Process OCR entities from capture
        if (ocrEntities != null) {
            // Extract barcodes and bounding boxes (same logic as handleBarcodeDetection)
            List<Rect> rects = new ArrayList<>();
            List<String> decodedStrings = new ArrayList<>();
            for (Entity entity : ocrEntities) {
                if (entity instanceof ParagraphEntity) {
                    ParagraphEntity paragraph = (ParagraphEntity) entity;
                    List<LineEntity> lines = paragraph.getLines();

                    for (LineEntity line : lines) {
                        List<WordEntity> words = line.getWords();

                        for (WordEntity word : words) {
                            if (!word.getText().isEmpty()) {
                                ComplexBBox bbox = word.getComplexBBox();

                                if (bbox != null && bbox.x != null && bbox.y != null &&
                                        bbox.x.length >= 3 && bbox.y.length >= 3) {
                                    float minX = bbox.x[0], maxX = bbox.x[2], minY = bbox.y[0], maxY = bbox.y[2];

                                    Rect rect = new Rect((int) minX, (int) minY, (int) maxX, (int) maxY);
                                    Log.d(TAG, "Capture - Drawing OCR: " + word.getText() + ", bbox: " + rect);
                                    Rect overlayRect = boundingBoxMapper.mapBoundingBoxToOverlay(rect);
                                    rects.add(overlayRect);
                                    decodedStrings.add(word.getText());

                                }
                            }
                        }
                    }
                }
            }
            for (int i = 0; i < rects.size(); i++) {
                Rect rect = rects.get(i);
                String text = decodedStrings.get(i);

                // Draw bounding box
                canvas.drawRect(rect, ocrPaint);

                // Draw text above the box
                CommonUtils.getTextSizeWithinBounds(canvas, text, rect.left, rect.top, rect.right, rect.bottom, ocrTextPaint);
                //  canvas.drawText(text, rect.left, rect.top - 5, textPaint);
            }
        }

        // Process module recognition entities from capture
        List<ShelfEntity> shelves = new ArrayList<>();
        List<LabelEntity> labels = new ArrayList<>();
        List<ProductEntity> products = new ArrayList<>();

        if (moduleEntities != null) {
            for (Entity entity : moduleEntities) {
                if (entity instanceof ShelfEntity) {
                    shelves.add((ShelfEntity) entity);
                } else if (entity instanceof LabelEntity) {
                    labels.add((LabelEntity) entity);
                } else if (entity instanceof ProductEntity) {
                    products.add((ProductEntity) entity);
                }
            }
        }

        // Draw shelves
        for (ShelfEntity shelf : shelves) {
            Log.d(TAG, "Capture - Drawing shelf, bbox: " + shelf.getBoundingBox());
            Rect shelfRect = boundingBoxMapper.mapBoundingBoxToOverlay(shelf.getBoundingBox());
            canvas.drawRect(
                    shelfRect.left,
                    shelfRect.top,
                    shelfRect.right,
                    shelfRect.bottom,
                    shelfPaint
            );
        }

        // Draw labels
        for (LabelEntity label : labels) {
            if (label.getClassId() == LabelEntity.ClassId.PEG_LABEL) {
                Log.d(TAG, "Capture - Drawing peg label, bbox: " + label.getBoundingBox());
                Rect labelRect = boundingBoxMapper.mapBoundingBoxToOverlay(label.getBoundingBox());
                canvas.drawRect(
                        labelRect.left,
                        labelRect.top,
                        labelRect.right,
                        labelRect.bottom,
                        labelPegPaint
                );
            }
            if (label.getClassId() == LabelEntity.ClassId.SHELF_LABEL) {
                Log.d(TAG, "Capture - Drawing shelf label, bbox: " + label.getBoundingBox());
                Rect labelRect = boundingBoxMapper.mapBoundingBoxToOverlay(label.getBoundingBox());
                canvas.drawRect(
                        labelRect.left,
                        labelRect.top,
                        labelRect.right,
                        labelRect.bottom,
                        labelShelfPaint
                );
            }
        }

        // Draw products
        for (ProductEntity product : products) {
            Log.d(TAG, "Capture - Drawing product, bbox: " + product.getBoundingBox());
            Rect prodRect = boundingBoxMapper.mapBoundingBoxToOverlay(product.getBoundingBox());

            // Draw bounding box
            canvas.drawRect(
                    prodRect.left,
                    prodRect.top,
                    prodRect.right,
                    prodRect.bottom,
                    productPaint
            );

            // Draw SKU text with background
            String topSku = "";
            if (product.getTopKSKUs() != null && !product.getTopKSKUs().isEmpty()) {
                SKUInfo skuInfo = product.getTopKSKUs().get(0);
               
                topSku = skuInfo.getProductSKU();
            }

            if (!topSku.isEmpty()) {
                Rect textBounds = new Rect();
                productTextPaint.getTextBounds(topSku, 0, topSku.length(), textBounds);

                float textX = prodRect.left;
                float textY = prodRect.top - 10f;

                // Draw background rectangle for text
                canvas.drawRect(
                        textX,
                        textY - textBounds.height() - 5f,
                        textX + textBounds.width() + 10f,
                        textY + 5f,
                        productBackgroundPaint
                );

                // Draw text
                canvas.drawText(topSku, textX + 5f, textY, productTextPaint);
            }
        }

        Log.d(TAG, "Capture - Entity tracker detection completed and drawn on canvas");
        Log.d(TAG, "Capture - Statistics: barcodes=" + (barcodeEntities != null ? barcodeEntities.size() : 0) +
                ", OCR entities=" + (ocrEntities != null ? ocrEntities.size() : 0) +
                ", shelves=" + shelves.size() + ", labels=" + labels.size() + ", products=" + products.size());
        activity.runOnUiThread(() -> {
            activity.getBinding().capturedImageView.setImageBitmap(annotated);

        });
    }
    public void handleWareHouseLocalizerDetectionResult(List<LocalizerEntity> result) {
        List<Rect> rects = new ArrayList<>();

        activity.runOnUiThread(() -> {
            activity.getBinding().graphicOverlay.clear();
            if (result != null) {
                for (LocalizerEntity entity : result) {
                    Rect rect = entity.getBoundingBox();
                    if (rect != null) {
                        Log.d(TAG, String.format("Original bbox: %s", rect));
                        Rect overlayRect = boundingBoxMapper.mapBoundingBoxToOverlay(rect);
                        Log.d(TAG, String.format("Mapped bbox: %s", overlayRect));
                        rects.add(overlayRect);
                    }
                }
                activity.getBinding().graphicOverlay.add(new WareHouseLocalizerGraphic(activity.getBinding().graphicOverlay, rects));
            }
        });
    }

    public void handleImageCaptureWareHouseResult(List<LocalizerEntity> entities) {
        if (activity.getCapturedBitmap() == null || entities == null) {
            Log.w(TAG, "Cannot overlay: bitmap or entities null");
            return;
        }

        Bitmap annotated = activity.getCapturedBitmap().copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(annotated);

        // Draw barcodes using BarcodeGraphic style (matching preview)
        Paint rectPaint = new Paint();
        rectPaint.setColor(Color.GREEN);
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(16f);

        for (LocalizerEntity entity : entities) {
            Rect rect = entity.getBoundingBox();
            if (rect != null) {
                Log.d(TAG, String.format("Original bbox: %s", rect));
                Rect overlayRect = boundingBoxMapper.mapBoundingBoxToOverlay(rect);
                Log.d(TAG, String.format("Mapped bbox: %s", overlayRect));
                canvas.drawRect(overlayRect, rectPaint);
            }
        }
        activity.runOnUiThread(() -> {
            activity.getBinding().capturedImageView.setImageBitmap(annotated);
        });
    }

    /**
     * Clears the tap listener and cached shelf/product/label data for captured image.
     * Call this when switching modes or returning to live preview.
     */
    public void clearCaptureTapListener() {
        activity.runOnUiThread(() -> {
            // Remove tap listener from capturedImageView
            activity.getBinding().capturedImageView.setOnTouchListener(null);
            activity.getBinding().capturedImageView.setClickable(false);

            // Clear cached data
            capturedEntities.clear();
            capturedShelves.clear();
            capturedProducts.clear();
            capturedLabels.clear();
            capturedShelfViewRects.clear();
            capturedProductViewRects.clear();
            capturedLabelViewRects.clear();
        });
    }
}
