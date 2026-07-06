// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.analyzers.customdetector.barcode

import android.util.Log
import com.zebra.ai.vision.detector.BarcodeDecoder
import com.zebra.ai.vision.detector.InferencerOptions
import java.util.concurrent.ExecutorService

/**
 * Step 1: Initialize — Zebra BarcodeDecoder.
 *
 * BarcodeDecoder implements the SDK's Detector interface directly, so Step 2
 * (CustomDetector wrapping) is not needed. It can be added to EntityTrackerAnalyzer
 * alongside CustomDetector instances without any additional wrapping.
 */
object BarcodeIntegration {

    const val MODEL_ID = "Barcode Decoder"

    private const val TAG        = "BarcodeIntegration"
    private const val MODEL_NAME = "barcode-decoder"

    fun create(executor: ExecutorService): BarcodeDecoder {
        Log.d(TAG, "Initializing BarcodeDecoder…")

        // Step 1: Initialize
        val settings = BarcodeDecoder.Settings(MODEL_NAME).apply {
            Symbology?.let { s ->
                s.CODE39.enable(true)
                s.CODE93.enable(true)
                s.CODE128.enable(true)
                s.CODABAR.enable(true)
                s.EAN8.enable(true)
                s.EAN13.enable(true)
                s.UPCA.enable(true)
                s.UPCE0.enable(true)
                s.I2OF5.enable(true)
                s.QRCODE.enable(true)
                s.DATAMATRIX.enable(true)
                s.PDF417.enable(true)
            }
            detectorSetting?.inferencerOptions?.apply {
                defaultDims.width  = 640
                defaultDims.height = 640
                runtimeProcessorOrder = arrayOf(
                    InferencerOptions.DSP, InferencerOptions.CPU, InferencerOptions.GPU
                )
            }
            enableAIBarcodeDecode = true
        }

        Log.d(TAG, "BarcodeDecoder settings — enableAIBarcodeDecode=true" +
                " dims=640x640" +
                " symbologies=[CODE39, CODE93, CODE128, CODABAR, EAN8, EAN13, UPCA, UPCE0, I2OF5, QRCODE, DATAMATRIX, PDF417]" +
                " rpo=[DSP, CPU, GPU]")

        val decoder = BarcodeDecoder.getBarcodeDecoder(settings, executor).get()
        Log.d(TAG, "BarcodeDecoder ready")
        return decoder
    }
}
