// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.analyzers.customdetector.ocr

import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.zebra.ai.vision.custommodels.CustomDetector

/**
 * Steps 1+2: Initialize ML Kit TextRecognizer and wrap it in a CustomDetector.
 *
 * The TextRecognizer is created internally and owned by the returned CustomDetector —
 * calling CustomDetector.close() will auto-close it.
 */
object OcrIntegration {

    const val MODEL_ID = "ML Kit OCR"

    private const val TAG = "OcrIntegration"

    fun create(): CustomDetector<OcrTextEntity> {
        Log.d(TAG, "Initializing ML Kit OCR…")

        // Step 1: Initialize
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        // Step 2: Wrap with CustomDetector
        val detector = CustomDetector.create<_, OcrTextEntity>(recognizer, MODEL_ID) { model, imageData ->
            val frame = imageData.getBitmap()
            val bitmap = if (frame.config == Bitmap.Config.HARDWARE)
                frame.copy(Bitmap.Config.ARGB_8888, false) else frame
            val visionText = Tasks.await(model.process(InputImage.fromBitmap(bitmap, 0)))
            val entities = visionText.textBlocks.mapNotNull { block ->
                val box = block.boundingBox ?: return@mapNotNull null
                OcrTextEntity(box, block.cornerPoints?.toList() ?: emptyList(), block.text)
            }
            Log.v(TAG, "  ML Kit OCR: ${entities.size} text block(s)")
            entities
        }

        Log.d(TAG, "ML Kit OCR detector ready")
        return detector
    }
}
