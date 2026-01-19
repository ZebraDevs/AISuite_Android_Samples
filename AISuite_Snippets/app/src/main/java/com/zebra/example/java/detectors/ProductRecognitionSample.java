// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.example.java.detectors;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;

import androidx.camera.core.ImageProxy;

import com.zebra.ai.vision.detector.ImageData;
import com.zebra.ai.vision.detector.InferencerOptions;
import com.zebra.ai.vision.detector.ModuleRecognizer;
import com.zebra.ai.vision.detector.SKUInfo;
import com.zebra.ai.vision.entity.Entity;
import com.zebra.ai.vision.entity.LabelEntity;
import com.zebra.ai.vision.entity.ProductEntity;
import com.zebra.ai.vision.entity.ShelfEntity;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductRecognitionSample {
    // Android context in which the sample operates
    private final Context context;

    private final String TAG = "ProductRecognitionSample";

    private ModuleRecognizer moduleRecognizer;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final String mavenModelName = "product-and-shelf-recognizer";

    /**
     * Constructor for ProductRecognitionSample.
     * Initializes the product recognition models when an instance is created.
     *
     * @param context The application context for accessing assets and resources
     */
    public ProductRecognitionSample(Context context) {
        this.context = context;
        initializeModuleRecognizer();
    }

    /**
     * Initializes the ModuleRecognizer with product recognition enabled.
     */
    private void initializeModuleRecognizer() {
        try {
            // Copy assets
            String indexFilename = "product.index";
            String labelsFilename = "product.txt";
            String toPath = context.getFilesDir() + "/";
            copyFromAssets(indexFilename, toPath);
            copyFromAssets(labelsFilename, toPath);

            // Create settings with base model
            ModuleRecognizer.Settings settings = new ModuleRecognizer.Settings(mavenModelName);

            // Configure InferencerOptions
            settings.inferencerOptions.runtimeProcessorOrder = new Integer[]{
                    InferencerOptions.DSP,
                    InferencerOptions.CPU,
                    InferencerOptions.GPU
            };
            settings.inferencerOptions.defaultDims.height = 640;
            settings.inferencerOptions.defaultDims.width = 640;

            // Enable product recognition with the same model and recognition data
            settings.enableProductRecognitionWithIndex(
                    mavenModelName,
                    toPath + indexFilename,
                    toPath + labelsFilename
            );

            // Initialize ModuleRecognizer
            long startTime = System.currentTimeMillis();
            ModuleRecognizer.getModuleRecognizer(settings, executor)
                    .thenAccept(recognizerInstance -> {
                        long creationTime = System.currentTimeMillis() - startTime;
                        Log.d(TAG, "ModuleRecognizer Creation Time: " + creationTime + "ms");
                        Log.i(TAG, "ModuleRecognizer instance created successfully");

                        moduleRecognizer = recognizerInstance;

                    })
                    .exceptionally(throwable -> {
                        Log.e(TAG, "Failed to initialize ModuleRecognizer: " + throwable.getMessage());
                        return null;
                    });

        } catch (Exception e) {
            Log.e(TAG, "Fatal error during initialization: " + e.getMessage());
        }
    }

    /**
     * Copies files from the assets folder to the specified path.
     */
    private void copyFromAssets(String filename, String toPath) {
        final int bufferSize = 8192;
        try (InputStream stream = context.getAssets().open(filename);
             OutputStream fos = Files.newOutputStream(Paths.get(toPath + filename));
             BufferedOutputStream output = new BufferedOutputStream(fos)) {
            byte[] data = new byte[bufferSize];
            int count;
            while ((count = stream.read(data)) != -1) {
                output.write(data, 0, count);
            }
            output.flush();
        } catch (IOException e) {
            Log.e(TAG, "Error copying from assets: " + e.getMessage());
        }
    }

    private void processImage(ImageProxy image) {
        Log.d(TAG, "Converting ImageProxy to Bitmap...");
        ImageData imageData = ImageData.fromImageProxy(image);

        Log.d(TAG, "Calling moduleRecognizer.process...");
        long start = System.currentTimeMillis();
        moduleRecognizer.process(imageData)
                .thenAccept(entityList -> {
                    long end = System.currentTimeMillis();
                    long inferenceTime = end - start;
                    Log.d(TAG, "Inference Time: " + inferenceTime);
                    List<ShelfEntity> shelves = new ArrayList<>();
                    List<LabelEntity> labels = new ArrayList<>();
                    List<ProductEntity> products = new ArrayList<>();
                    for (Entity entity : entityList) {
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
                        shelfRects.add(shelf.getBoundingBox());
                    }

                    // Draw all labels (if you want to show all, not just those attached to shelves)
                    for (LabelEntity label : labels) {
                        if (label.getClassId() == LabelEntity.ClassId.PEG_LABEL) {
                            labelPegRects.add(label.getBoundingBox());
                        }
                        if (label.getClassId() == LabelEntity.ClassId.SHELF_LABEL) {
                            labelShelfRects.add(label.getBoundingBox());
                        }

                    }

                    // Draw all products (regardless of shelf assignment)
                    for (ProductEntity product : products) {
                        productRects.add(product.getBoundingBox());
                        String topSku = "";
                        List<SKUInfo> skuInfos = product.getTopKSKUs();
                        if (skuInfos != null && !skuInfos.isEmpty()) {
                            topSku = skuInfos.get(0).getProductSKU();
                        }
                        productLabels.add(topSku);

                        Log.d(TAG, String.format(
                                "SKU=%s, Product bbox=%s",
                                topSku, product.getBoundingBox()
                        ));
                    }

                    image.close();
                    Log.d(TAG, "Image closed, ready for next frame.");
                })
                .exceptionally(ex -> {
                    Log.e(TAG, "Error in shelf recognition: " + ex.getMessage(), ex);
                    image.close();
                    return null;
                });
    }
}
