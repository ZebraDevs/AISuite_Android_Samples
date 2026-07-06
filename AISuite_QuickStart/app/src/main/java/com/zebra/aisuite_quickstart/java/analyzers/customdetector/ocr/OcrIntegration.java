// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.analyzers.customdetector.ocr;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.zebra.ai.vision.custommodels.CustomDetector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Steps 1+2: Initialize ML Kit TextRecognizer and wrap it in a CustomDetector.
 *
 * The TextRecognizer is created internally and owned by the returned CustomDetector —
 * calling CustomDetector.close() will auto-close it.
 */
public class OcrIntegration {

    public static final String MODEL_ID = "ML Kit OCR";

    private static final String TAG = "OcrIntegration";

    public static CustomDetector<OcrTextEntity> create() {
        Log.d(TAG, "Initializing ML Kit OCR…");

        // Step 1: Initialize
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        // Step 2: Wrap with CustomDetector
        CustomDetector<OcrTextEntity> detector = CustomDetector.create(recognizer, MODEL_ID,
                (model, imageData) -> {
                    Bitmap frame = imageData.getBitmap();
                    Bitmap bitmap = (frame.getConfig() == Bitmap.Config.HARDWARE)
                            ? frame.copy(Bitmap.Config.ARGB_8888, false) : frame;
                    com.google.mlkit.vision.text.Text visionText;
                    try {
                        visionText = Tasks.await(model.process(InputImage.fromBitmap(bitmap, 0)));
                    } catch (Exception e) {
                        throw new RuntimeException("ML Kit OCR failed", e);
                    }
                    List<OcrTextEntity> result = new ArrayList<>();
                    for (com.google.mlkit.vision.text.Text.TextBlock block : visionText.getTextBlocks()) {
                        if (block.getBoundingBox() == null) continue;
                        result.add(new OcrTextEntity(
                                block.getBoundingBox(),
                                block.getCornerPoints() != null
                                        ? Arrays.asList(block.getCornerPoints())
                                        : new ArrayList<>(),
                                block.getText()
                        ));
                    }
                    Log.v(TAG, "  ML Kit OCR: " + result.size() + " text block(s)");
                    return result;
                });

        Log.d(TAG, "ML Kit OCR detector ready");
        return detector;
    }
}
