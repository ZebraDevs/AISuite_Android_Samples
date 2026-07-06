// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.analyzers.customdetector.yolo

import android.content.Context
import android.util.Log
import com.zebra.ai.vision.custommodels.CustomDetector
import com.zebra.ai.vision.entity.DetectionEntity

/**
 * Steps 1+2: Initialize YOLOv8n ONNX model and wrap it in a CustomDetector.
 *
 * YoloOnnxModel implements AutoCloseable, so calling CustomDetector.close()
 * will auto-close the underlying ONNX session.
 */
object YoloIntegration {

    const val MODEL_ID = "YOLO Object Detection"

    private const val TAG = "YoloIntegration"

    fun create(context: Context): CustomDetector<DetectionEntity> {
        Log.d(TAG, "Initializing YOLOv8n ONNX…")

        // Step 1: Initialize
        val model = YoloOnnxModel(context)

        // Step 2: Wrap with CustomDetector
        val detector = CustomDetector.create(model, MODEL_ID) { m, imageData ->
            val entities = m.detect(imageData.getBitmap())
                .map { rect -> DetectionEntity(rect, emptyList()) }
            Log.v(TAG, "  YOLO: ${entities.size} detection(s)")
            entities
        }

        Log.d(TAG, "YOLOv8n ONNX detector ready")
        return detector
    }
}
