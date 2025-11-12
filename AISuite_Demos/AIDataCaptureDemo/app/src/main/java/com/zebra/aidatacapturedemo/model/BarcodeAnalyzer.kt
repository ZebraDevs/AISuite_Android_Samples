// Copyright (c) 2024-2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.aidatacapturedemo.model

import android.util.Log
import androidx.lifecycle.Lifecycle
import com.zebra.ai.vision.detector.BarcodeDecoder
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.data.PROFILING
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * [BarcodeAnalyzer] class is used to detect & Track barcodes found on the Camera Live Preview
 *
 * @param uiState - Used to read all the UI Current State
 * @param viewModel - Used to write any UI State Changes via [AIDataCaptureDemoViewModel]
 */
class BarcodeAnalyzer(
    val uiState: StateFlow<AIDataCaptureDemoUiState>,
    val viewModel: AIDataCaptureDemoViewModel) {

    private lateinit var mActivityLifecycle: Lifecycle
    private val TAG = "BarcodeAnalyzer"
    private var barcodeDecoder: BarcodeDecoder? = null
    private val decoderSettings = BarcodeDecoder.Settings("barcode-localizer")
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()

    /**
     * To initialize the BarcodeAnalyzer
     */
    fun initialize() {
        barcodeDecoder?.dispose()
        barcodeDecoder = null
        updateModelDemoReady(false)
        try {
            configure()

            val mStart = System.currentTimeMillis()
            BarcodeDecoder.getBarcodeDecoder(decoderSettings, executorService)
                .thenAccept { barcodeDecoderInstance: BarcodeDecoder ->
                    barcodeDecoder = barcodeDecoderInstance
                    updateModelDemoReady(true)
                    Log.e(
                        PROFILING,
                        "BarcodeAnalyzer obj creation / model loading time = ${System.currentTimeMillis() - mStart} milli sec"
                    )
                    Log.i(TAG, "BarcodeAnalyzer init Success")
                }.exceptionally { e: Throwable ->
                    Log.e(TAG, "BarcodeAnalyzer init Failed -> " + e.message)
                    if (e.message?.contains("Given runtimes are not available") == true ||
                        e.message?.contains("Initialize barcodeDecoder due to SNPE exception") == true
                    ) {
                        viewModel.updateToastMessage(message = "Selected inference type is not supported on this device. Switching to Auto-select for optimal performance.")
                        viewModel.updateSelectedProcessor(0) //Auto-Select
                        viewModel.saveSettings()
                        initialize()
                    }
                    null
                }
        } catch (ex: IOException) {
            Log.e(TAG, "getBarcodeDecoder init Failed -> " + ex.message)
        }
    }

    /**
     * To deinitialize the BarcodeAnalyzer, we need to dispose the localizer
     */
    fun deinitialize() {
        barcodeDecoder?.dispose()
        barcodeDecoder = null
    }
    fun getDetector() : BarcodeDecoder? {
        return barcodeDecoder
    }
    private fun configure() {
        try {
            //Swap the values as the presented index is reverse of what model expects
            val processorOrder = when (uiState.value.barcodeSettings.commonSettings.processorSelectedIndex) {
                0 -> arrayOf(2, 0, 1) // AUTO
                1 -> arrayOf(2) // DSP
                2 -> arrayOf(1) // GPU
                3 -> arrayOf(0) //CPU
                else -> {
                    arrayOf(2, 0, 1)
                }
            }
            decoderSettings.detectorSetting.inferencerOptions.runtimeProcessorOrder = processorOrder

            decoderSettings.detectorSetting.inferencerOptions.defaultDims.width =
                uiState.value.barcodeSettings.commonSettings.inputSizeSelected
            decoderSettings.detectorSetting.inferencerOptions.defaultDims.height =
                uiState.value.barcodeSettings.commonSettings.inputSizeSelected

            decoderSettings.Symbology.AUSTRALIAN_POSTAL.enable(uiState.value.barcodeSettings.barcodeSymbology.australian_postal)
            decoderSettings.Symbology.AZTEC.enable(uiState.value.barcodeSettings.barcodeSymbology.aztec)
            decoderSettings.Symbology.CANADIAN_POSTAL.enable(uiState.value.barcodeSettings.barcodeSymbology.canadian_postal)
            decoderSettings.Symbology.CHINESE_2OF5.enable(uiState.value.barcodeSettings.barcodeSymbology.chinese_2of5)
            decoderSettings.Symbology.CODABAR.enable(uiState.value.barcodeSettings.barcodeSymbology.codabar)
            decoderSettings.Symbology.CODE11.enable(uiState.value.barcodeSettings.barcodeSymbology.code11)
            decoderSettings.Symbology.CODE39.enable(uiState.value.barcodeSettings.barcodeSymbology.code39)
            decoderSettings.Symbology.CODE93.enable(uiState.value.barcodeSettings.barcodeSymbology.code93)
            decoderSettings.Symbology.CODE128.enable(uiState.value.barcodeSettings.barcodeSymbology.code128)
            decoderSettings.Symbology.COMPOSITE_AB.enable(uiState.value.barcodeSettings.barcodeSymbology.composite_ab)
            decoderSettings.Symbology.COMPOSITE_C.enable(uiState.value.barcodeSettings.barcodeSymbology.composite_c)
            decoderSettings.Symbology.D2OF5.enable(uiState.value.barcodeSettings.barcodeSymbology.d2of5)
            decoderSettings.Symbology.DATAMATRIX.enable(uiState.value.barcodeSettings.barcodeSymbology.datamatrix)
            decoderSettings.Symbology.DOTCODE.enable(uiState.value.barcodeSettings.barcodeSymbology.dotcode)
            decoderSettings.Symbology.DUTCH_POSTAL.enable(uiState.value.barcodeSettings.barcodeSymbology.dutch_postal)
            decoderSettings.Symbology.EAN8.enable(uiState.value.barcodeSettings.barcodeSymbology.ean_8)
            decoderSettings.Symbology.EAN13.enable(uiState.value.barcodeSettings.barcodeSymbology.ean_13)
            decoderSettings.Symbology.FINNISH_POSTAL_4S.enable(uiState.value.barcodeSettings.barcodeSymbology.finnish_postal_4s)
            decoderSettings.Symbology.GRID_MATRIX.enable(uiState.value.barcodeSettings.barcodeSymbology.grid_matrix)
            decoderSettings.Symbology.GS1_DATABAR.enable(uiState.value.barcodeSettings.barcodeSymbology.gs1_databar)
            decoderSettings.Symbology.GS1_DATABAR_EXPANDED.enable(uiState.value.barcodeSettings.barcodeSymbology.gs1_databar_expanded)
            decoderSettings.Symbology.GS1_DATABAR_LIM.enable(uiState.value.barcodeSettings.barcodeSymbology.gs1_databar_lim)
            decoderSettings.Symbology.GS1_DATAMATRIX.enable(uiState.value.barcodeSettings.barcodeSymbology.gs1_datamatrix)
            decoderSettings.Symbology.GS1_QRCODE.enable(uiState.value.barcodeSettings.barcodeSymbology.gs1_qrcode)
            decoderSettings.Symbology.HANXIN.enable(uiState.value.barcodeSettings.barcodeSymbology.hanxin)
            decoderSettings.Symbology.I2OF5.enable(uiState.value.barcodeSettings.barcodeSymbology.i2of5)
            decoderSettings.Symbology.JAPANESE_POSTAL.enable(uiState.value.barcodeSettings.barcodeSymbology.japanese_postal)
            decoderSettings.Symbology.KOREAN_3OF5.enable(uiState.value.barcodeSettings.barcodeSymbology.korean_3of5)
            decoderSettings.Symbology.MAILMARK.enable(uiState.value.barcodeSettings.barcodeSymbology.mailmark)
            decoderSettings.Symbology.MATRIX_2OF5.enable(uiState.value.barcodeSettings.barcodeSymbology.matrix_2of5)
            decoderSettings.Symbology.MAXICODE.enable(uiState.value.barcodeSettings.barcodeSymbology.maxicode)
            decoderSettings.Symbology.MICROPDF.enable(uiState.value.barcodeSettings.barcodeSymbology.micropdf)
            decoderSettings.Symbology.MICROQR.enable(uiState.value.barcodeSettings.barcodeSymbology.microqr)
            decoderSettings.Symbology.MSI.enable(uiState.value.barcodeSettings.barcodeSymbology.msi)
            decoderSettings.Symbology.PDF417.enable(uiState.value.barcodeSettings.barcodeSymbology.pdf417)
            decoderSettings.Symbology.QRCODE.enable(uiState.value.barcodeSettings.barcodeSymbology.qrcode)
            decoderSettings.Symbology.TLC39.enable(uiState.value.barcodeSettings.barcodeSymbology.tlc39)
            decoderSettings.Symbology.TRIOPTIC39.enable(uiState.value.barcodeSettings.barcodeSymbology.trioptic39)
            decoderSettings.Symbology.UK_POSTAL.enable(uiState.value.barcodeSettings.barcodeSymbology.uk_postal)
            decoderSettings.Symbology.UPCA.enable(uiState.value.barcodeSettings.barcodeSymbology.upc_a)
            decoderSettings.Symbology.UPCE0.enable(uiState.value.barcodeSettings.barcodeSymbology.upce0)
            decoderSettings.Symbology.UPCE1.enable(uiState.value.barcodeSettings.barcodeSymbology.upce1)
            decoderSettings.Symbology.USPLANET.enable(uiState.value.barcodeSettings.barcodeSymbology.usplanet)
            decoderSettings.Symbology.USPOSTNET.enable(uiState.value.barcodeSettings.barcodeSymbology.uspostnet)
            decoderSettings.Symbology.US4STATE.enable(uiState.value.barcodeSettings.barcodeSymbology.us4state)
            decoderSettings.Symbology.US4STATE_FICS.enable(uiState.value.barcodeSettings.barcodeSymbology.us4state_fics)
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error: configure failed - ${e.message}")
        }
    }

    private fun updateModelDemoReady(isReady: Boolean) {
        viewModel.updateModelDemoReady(isReady = isReady)
    }
}