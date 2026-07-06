// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.analyzers.customdetector.yolo;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.zebra.ai.vision.custommodels.CustomDetector;
import com.zebra.ai.vision.entity.DetectionEntity;

import java.util.List;

/**
 * Steps 1+2: Initialize YOLOv8n ONNX model and wrap it in a CustomDetector.
 *
 * YoloOnnxModel implements AutoCloseable, so calling CustomDetector.close()
 * will auto-close the underlying ONNX session.
 */
public class YoloIntegration {

    public static final String MODEL_ID = "YOLO Object Detection";

    private static final String TAG = "YoloIntegration";

    public static CustomDetector<DetectionEntity> create(Context context) throws Exception {
        Log.d(TAG, "Initializing YOLOv8n ONNX…");

        // Step 1: Initialize
        YoloOnnxModel model = new YoloOnnxModel(context);

        // Step 2: Wrap with CustomDetector
        CustomDetector<DetectionEntity> detector = CustomDetector.create(model, MODEL_ID,
                (m, imageData) -> {
                    try {
                        List<DetectionEntity> entities = Tasks.await(
                                m.process(InputImage.fromBitmap(imageData.getBitmap(), 0)));
                        Log.v(TAG, "  YOLO: " + entities.size() + " detection(s)");
                        return entities;
                    } catch (Exception e) {
                        throw new RuntimeException("YOLO inference failed", e);
                    }
                });

        Log.d(TAG, "YOLOv8n ONNX detector ready");
        return detector;
    }
}
