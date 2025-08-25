// Copyright (c) 2024-2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.aidatacapturedemo.model

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.zebra.ai.vision.analyzer.tracking.EntityTrackerAnalyzer
import com.zebra.ai.vision.detector.BarcodeDecoder
import com.zebra.ai.vision.entity.BarcodeEntity
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.data.ResultData
import com.zebra.aidatacapturedemo.data.PROFILING
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * [BarcodeEntityTrackerAnalyzer] class is used to detect & Track barcodes found on the Camera Live Preview
 *
 * @param uiState - Used to read all the UI Current State
 * @param viewModel - Used to write any UI State Changes via [AIDataCaptureDemoViewModel]
 */
class BarcodeEntityTrackerAnalyzer(
    val uiState: StateFlow<AIDataCaptureDemoUiState>,
    val viewModel: AIDataCaptureDemoViewModel
) {

    private lateinit var mActivityLifecycle: Lifecycle
    private val TAG = "BarcodeEntityTrackerAnalyzer"
    private var barcodeDecoder: BarcodeDecoder? = null
    private val decoderSettings = BarcodeDecoder.Settings("barcode-localizer")
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()

    /**
     * To initialize the BarcodeEntityTrackerAnalyzer
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
                        "BarcodeEntityTrackerAnalyzer obj creation / model loading time = ${System.currentTimeMillis() - mStart} milli sec"
                    )
                    Log.i(TAG, "BarcodeEntityTrackerAnalyzer init Success")
                }.exceptionally { e: Throwable ->
                Log.e(TAG, "BarcodeEntityTrackerAnalyzer init Failed -> " + e.message)
                null
            }
        } catch (ex: IOException) {
            Log.e(TAG, "getBarcodeDecoder init Failed -> " + ex.message)
        }
    }

    fun setupEntityTrackerAnalyzer(myLifecycle: Lifecycle): EntityTrackerAnalyzer {
        mActivityLifecycle = myLifecycle

        val entityTrackerAnalyzer = EntityTrackerAnalyzer(
            listOf(barcodeDecoder),
            ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
            executorService,
            ::handleEntities
        )

        return entityTrackerAnalyzer
    }

    /**
     * To deinitialize the BarcodeEntityTrackerAnalyzer, we need to dispose the localizer
     */
    fun deinitialize() {
        barcodeDecoder?.dispose()
        barcodeDecoder = null
    }

    private fun configure() {
        try {
            //Swap the values as the presented index is reverse of what model expects
            val processorOrder = when (uiState.value.barcodeSettings.commonSettings.processorSelectedIndex) {
                0 -> arrayOf(2)
                1 -> arrayOf(1)
                2 -> arrayOf(0)
                else -> { arrayOf(2) }
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

    private fun handleEntities(result: EntityTrackerAnalyzer.Result) {
        mActivityLifecycle.coroutineScope.launch(Dispatchers.Main) {

            val returnEntityList = result.getValue(barcodeDecoder!!)
            var rectList : List<ResultData> = mutableListOf()
            returnEntityList?.forEach { entity ->
                if (entity != null){
                    val barcodeEntity = entity as BarcodeEntity
                    val value = barcodeEntity.value
                    val rect = barcodeEntity.boundingBox
                    rectList += ResultData(boundingBox = rect, text = value)
                }
            }
            viewModel.updateBarcodeResultData(results = rectList)
        }
    }

    private fun updateModelDemoReady(isReady: Boolean) {
        viewModel.updateModelDemoReady(isReady = isReady)
    }
}