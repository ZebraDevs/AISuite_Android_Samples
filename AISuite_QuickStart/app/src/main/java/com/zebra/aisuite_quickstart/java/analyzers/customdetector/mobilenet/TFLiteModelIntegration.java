// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.analyzers.customdetector.mobilenet;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.zebra.ai.vision.custommodels.CustomDetector;
import com.zebra.ai.vision.entity.DetectionEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Steps 1+2: Initialize MobileNet SSD TFLite model and wrap it in a CustomDetector.
 *
 * TFLiteModel implements AutoCloseable, so calling CustomDetector.close()
 * will auto-close the underlying TFLite interpreter.
 *
 * Model: SSD MobileNet V1 COCO (quantized), Apache-2.0 license.
 * Detects 90 COCO object classes (person, car, dog, …) at 300×300 input.
 */
public class TFLiteModelIntegration {

    public static final String MODEL_ID = "MobileNet SSD";

    private static final String TAG = "TFLiteModelIntegration";

    public static CustomDetector<DetectionEntity> create(Context context) throws IOException {
        Log.d(TAG, "Initializing MobileNet SSD object detector…");

        // Step 1: Initialize
        TFLiteModel model = new TFLiteModel(context);

        // Step 2: Wrap with CustomDetector
        CustomDetector<DetectionEntity> detector = CustomDetector.create(model, MODEL_ID,
                (m, imageData) -> {
                    try {
                        List<DetectionEntity> entities = Tasks.await(
                                m.process(InputImage.fromBitmap(imageData.getBitmap(), 0)));
                        Log.v(TAG, "  MobileNet SSD: " + entities.size() + " detection(s)");
                        return entities;
                    } catch (Exception e) {
                        throw new RuntimeException("MobileNet SSD inference failed", e);
                    }
                });

        Log.d(TAG, "MobileNet SSD object detector ready");
        return detector;
    }
}
