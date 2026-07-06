// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.analyzers.customdetector.mobilenet

import android.content.Context
import android.util.Log
import com.zebra.ai.vision.custommodels.CustomDetector
import com.zebra.ai.vision.entity.DetectionEntity

/**
 * Steps 1+2: Initialize MobileNet SSD TFLite model and wrap it in a CustomDetector.
 *
 * TFLiteModel implements AutoCloseable, so calling CustomDetector.close()
 * will auto-close the underlying TFLite interpreter.
 *
 * Model: SSD MobileNet V1 COCO (quantized), Apache-2.0 license.
 * Detects 90 COCO object classes (person, car, dog, …) at 300×300 input.
 */
object TFLiteModelIntegration {

    const val MODEL_ID = "MobileNet SSD"

    private const val TAG = "TFLiteModelIntegration"

    fun create(context: Context): CustomDetector<DetectionEntity> {
        Log.d(TAG, "Initializing MobileNet SSD object detector…")

        // Step 1: Initialize
        val model = TFLiteModel(context)

        // Step 2: Wrap with CustomDetector
        val detector = CustomDetector.create(model, MODEL_ID) { m, imageData ->
            val entities = m.detect(imageData.getBitmap())
                .map { rect -> DetectionEntity(rect, emptyList()) }
            Log.v(TAG, "  MobileNet SSD: ${entities.size} detection(s)")
            entities
        }

        Log.d(TAG, "MobileNet SSD object detector ready")
        return detector
    }
}
