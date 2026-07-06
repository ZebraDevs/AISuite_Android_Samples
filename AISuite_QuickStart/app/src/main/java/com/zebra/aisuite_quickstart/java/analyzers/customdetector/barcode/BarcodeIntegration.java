// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.analyzers.customdetector.barcode;

import android.util.Log;

import com.zebra.ai.vision.detector.BarcodeDecoder;
import com.zebra.ai.vision.detector.InferencerOptions;

import java.util.concurrent.ExecutorService;

/**
 * Step 1: Initialize — Zebra BarcodeDecoder.
 *
 * BarcodeDecoder implements the SDK's Detector interface directly, so Step 2
 * (CustomDetector wrapping) is not needed. It can be added to EntityTrackerAnalyzer
 * alongside CustomDetector instances without any additional wrapping.
 */
public class BarcodeIntegration {

    public static final String MODEL_ID = "Barcode Decoder";

    private static final String TAG              = "BarcodeIntegration";
    private static final String MODEL_NAME       = "barcode-decoder";

    public static BarcodeDecoder create(ExecutorService executor) throws Exception {
        Log.d(TAG, "Initializing BarcodeDecoder…");

        // Step 1: Initialize
        BarcodeDecoder.Settings settings = new BarcodeDecoder.Settings(MODEL_NAME);
        Integer[] rpo = {InferencerOptions.DSP, InferencerOptions.CPU, InferencerOptions.GPU};
        settings.Symbology.CODE39.enable(true);
        settings.Symbology.CODE93.enable(true);
        settings.Symbology.CODE128.enable(true);
        settings.Symbology.CODABAR.enable(true);
        settings.Symbology.EAN8.enable(true);
        settings.Symbology.EAN13.enable(true);
        settings.Symbology.UPCA.enable(true);
        settings.Symbology.UPCE0.enable(true);
        settings.Symbology.I2OF5.enable(true);
        settings.Symbology.QRCODE.enable(true);
        settings.Symbology.DATAMATRIX.enable(true);
        settings.Symbology.PDF417.enable(true);
        settings.detectorSetting.inferencerOptions.defaultDims.width  = 640;
        settings.detectorSetting.inferencerOptions.defaultDims.height = 640;
        settings.detectorSetting.inferencerOptions.runtimeProcessorOrder = rpo;
        settings.enableAIBarcodeDecode = true;

        Log.d(TAG, "BarcodeDecoder settings — enableAIBarcodeDecode=true"
                + " dims=640x640"
                + " symbologies=[CODE39, CODE93, CODE128, CODABAR, EAN8, EAN13, UPCA, UPCE0, I2OF5, QRCODE, DATAMATRIX, PDF417]"
                + " rpo=[DSP, CPU, GPU]");

        BarcodeDecoder decoder = BarcodeDecoder.getBarcodeDecoder(settings, executor).get();
        Log.d(TAG, "BarcodeDecoder ready");
        return decoder;
    }
}
