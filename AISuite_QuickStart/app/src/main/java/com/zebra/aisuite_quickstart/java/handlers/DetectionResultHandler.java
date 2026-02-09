// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.handlers;

import android.graphics.Rect;
import android.util.Log;

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
import com.zebra.ai.vision.entity.ParagraphEntity;
import com.zebra.ai.vision.entity.ProductEntity;
import com.zebra.ai.vision.entity.ShelfEntity;
import com.zebra.ai.vision.entity.WordEntity;
import com.zebra.aisuite_quickstart.java.CameraXLivePreviewActivity;
import com.zebra.aisuite_quickstart.java.analyzers.tracker.TrackerGraphic;
import com.zebra.aisuite_quickstart.java.detectors.barcodedecodersample.BarcodeGraphic;
import com.zebra.aisuite_quickstart.java.detectors.productrecognition.ProductRecognitionGraphic;
import com.zebra.aisuite_quickstart.java.detectors.textocrsample.OCRGraphic;
import com.zebra.aisuite_quickstart.java.viewfinder.EntityViewGraphic;

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

    public DetectionResultHandler(CameraXLivePreviewActivity activity, BoundingBoxMapper boundingBoxMapper) {
        this.activity = activity;
        this.boundingBoxMapper = boundingBoxMapper;
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
                                    bbox.x.length >= 3 && bbox.y.length >= 3) {
                                float minX = bbox.x[0], maxX = bbox.x[2], minY = bbox.y[0], maxY = bbox.y[2];
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

                activity.getBinding().graphicOverlay.add(new ProductRecognitionGraphic(activity.getBinding().graphicOverlay, labelShelfRects, labelPegRects, shelfRects, recognizedRects, decodedStrings));
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

            // Draw shelves and their labels
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

            // Draw all products (regardless of shelf assignment)
            for (ProductEntity product : products) {
                Rect prodRect = boundingBoxMapper.mapBoundingBoxToOverlay(product.getBoundingBox());
                productRects.add(prodRect);
                String topSku = "";
                List<SKUInfo> skuInfos = product.getTopKSKUs();
                if (skuInfos != null && !skuInfos.isEmpty()) {
                    topSku = skuInfos.get(0).getProductSKU();
                }
                productLabels.add(topSku);

                Log.d(TAG, String.format(
                        "SKU=%s, Product bbox=%s",
                        topSku, prodRect
                ));
            }

            activity.getBinding().graphicOverlay.add(new ProductRecognitionGraphic(activity.getBinding().graphicOverlay, labelShelfRects, labelPegRects, shelfRects, productRects, productLabels)
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
                            barcodeStrings.add(hashCode + ":" + bEntity.getValue());
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

                                if (bbox != null && bbox.x != null && bbox.y != null && bbox.x.length >= 3 && bbox.y.length >= 3) {
                                    float minX = bbox.x[0], maxX = bbox.x[2], minY = bbox.y[0], maxY = bbox.y[2];

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
                    List<SKUInfo> skuInfos = product.getTopKSKUs();
                    if (skuInfos != null && !skuInfos.isEmpty()) {
                        SKUInfo topSKU = skuInfos.get(0);
                        topSku = topSKU.getProductSKU() + " (" + String.format("%.2f", topSKU.getAccuracy()) + ")";
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
                                productLabels
                        )
                );
            }
            if (!barcodeRects.isEmpty())
                activity.getBinding().graphicOverlay.add(new TrackerGraphic(activity.getBinding().graphicOverlay, barcodeRects, barcodeStrings));
            if (!ocrRects.isEmpty())
                activity.getBinding().graphicOverlay.add(new OCRGraphic(activity.getBinding().graphicOverlay, ocrRects, ocrStrings));
            if (!shelfRects.isEmpty() || !labelShelfRects.isEmpty() || !labelPegRects.isEmpty() || !productRects.isEmpty() || !productLabels.isEmpty())
                activity.getBinding().graphicOverlay.add(new ProductRecognitionGraphic(activity.getBinding().graphicOverlay, labelShelfRects, labelPegRects, shelfRects, productRects, productLabels));
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
}
