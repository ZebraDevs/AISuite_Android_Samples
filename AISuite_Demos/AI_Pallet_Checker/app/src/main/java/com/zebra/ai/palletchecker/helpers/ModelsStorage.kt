package com.zebra.ai.palletchecker.helpers

import android.util.Log
import androidx.compose.ui.unit.IntSize
import com.zebra.ai.palletchecker.domain.model.AppSettings
import com.zebra.ai.palletchecker.domain.model.BarcodeSymbology
import com.zebra.ai.vision.detector.AIVisionSDKException
import com.zebra.ai.vision.detector.BarcodeDecoder
import com.zebra.ai.vision.detector.InferencerOptions
import com.zebra.ai.vision.detector.Localizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.future.await
import java.io.IOException

object ModelsStorage {

    private val TAG = "ModelsStorage"
    private val PALLET_LOCALIZER_INPUT_SIZE = IntSize(832,832)
    private val BARCODE_MODEL_NAME = "barcode-decoder"
    private val PALLET_AND_BOX_MODEL_NAME = "pallet-and-box-localizer"
    private var barcodeDecoder: BarcodeDecoder? = null
    private var palletAndBoxLocalizer: Localizer? = null
    private var configBarcodeLocalizer: BarcodeDecoder? = null
    var modelInitiate = MutableStateFlow(false)
    private var snapModeSettings : (() -> AppSettings)? = null
    private var wandModeSettings : (() -> AppSettings)? = null

    suspend fun initializeAllModels(snapModeSettings: (() -> AppSettings), wandModeSettings: (() -> AppSettings)) {
        this.snapModeSettings = snapModeSettings
        this.wandModeSettings = wandModeSettings
        LOGV(TAG, "Initialize All Models ${snapModeSettings.invoke().modelInput.width} X ${snapModeSettings.invoke().modelInput.height}")
        initializeBarcodeDecoder()
        IntializeBarcodeForConfigure()
        initializePalletAndBoxLocalizer()

        if ( barcodeDecoder != null && palletAndBoxLocalizer != null) {
            modelInitiate.value = true
        } else {
            modelInitiate.value = false
        }

    }

    suspend fun IntializeBarcodeForConfigure() {
        try {
            val modelWidth = wandModeSettings?.invoke()?.modelInput?.width ?: 640
            val modelHeight = wandModeSettings?.invoke()?.modelInput?.height ?: 640

            LOGV(
                TAG,
                "Start Config initializeBarcodeDecoder: modelInput=${modelWidth}x${modelHeight}, " +
                "resolution=${wandModeSettings?.invoke()?.resolution?.width}x${wandModeSettings?.invoke()?.resolution?.height}"
            )

            val decoderSettings: BarcodeDecoder.Settings =
                BarcodeDecoder.Settings(
                    BARCODE_MODEL_NAME,
                )
            decoderSettings.enableAIBarcodeDecode = wandModeSettings?.invoke()?.enableAIbarcodeDecode ?: true
            configureSymbology(decoderSettings, wandModeSettings?.invoke()?.barcodeSymbology)

            val rpo = arrayOfNulls<Int>(3)
            rpo[0] = InferencerOptions.DSP
            rpo[1] = InferencerOptions.CPU
            rpo[2] = InferencerOptions.GPU

            decoderSettings.detectorSetting.inferencerOptions.runtimeProcessorOrder = rpo
            decoderSettings.detectorSetting.inferencerOptions.defaultDims.height = modelHeight
            decoderSettings.detectorSetting.inferencerOptions.defaultDims.width = modelWidth

            LOGD(TAG, "Start getConfigBarcodeDecoder with dims: ${modelWidth}x${modelHeight}")
            val decoderInstance =
                BarcodeDecoder.getBarcodeDecoder(decoderSettings, Dispatchers.IO.asExecutor())
                    .await()
            configBarcodeLocalizer = decoderInstance
            LOGD(TAG, "ConfigBarcodeDecoder initialized successfully")

        } catch (ex: AIVisionSDKException) {
            LOGE(TAG, "Model Loading:Config  Barcode decoder returned with exception " + ex.message)
        }
    }


    suspend fun initializeBarcodeDecoder() {
        try {
            LOGD(
                TAG,
                "Start initializeBarcodeDecoder ${snapModeSettings?.invoke()?.modelInput?.width} X ${snapModeSettings?.invoke()?.modelInput?.height}"
            )
            var inferenceSize = IntSize(640, 640)
            snapModeSettings?.let {
                inferenceSize = IntSize(it().modelInput.width, it().modelInput.height)
            }

            val decoderSettings: BarcodeDecoder.Settings =
                BarcodeDecoder.Settings(
                    BARCODE_MODEL_NAME,
                )

            decoderSettings.enableAIBarcodeDecode = snapModeSettings?.invoke()?.enableAIbarcodeDecode ?: true
            configureSymbology(decoderSettings, snapModeSettings?.invoke()?.barcodeSymbology)

            val rpo = arrayOfNulls<Int>(3)
            rpo[0] = InferencerOptions.DSP
            rpo[1] = InferencerOptions.CPU
            rpo[2] = InferencerOptions.GPU

            decoderSettings.detectorSetting.inferencerOptions.runtimeProcessorOrder = rpo
            decoderSettings.detectorSetting.inferencerOptions.defaultDims.height =
                inferenceSize.height
            decoderSettings.detectorSetting.inferencerOptions.defaultDims.width =
                inferenceSize.width

            val decoderInstance =
                BarcodeDecoder.getBarcodeDecoder(decoderSettings, Dispatchers.IO.asExecutor())
                    .await()
            barcodeDecoder = decoderInstance
            LOGD(TAG, "BarcodeDecoder initialized successfully")

        } catch (ex: AIVisionSDKException) {
            LOGE(TAG, "Model Loading: Barcode decoder returned with exception " + ex.message)
        }
    }

    private fun configureSymbology(decoderSettings: BarcodeDecoder.Settings, barcodeSymbology: BarcodeSymbology?) {
        decoderSettings.Symbology?.let { symbology ->
            barcodeSymbology?.let {
                with(it) {
                    symbology.AUSTRALIAN_POSTAL.enable(australianPostal)
                    symbology.AZTEC.enable(aztec)
                    symbology.CANADIAN_POSTAL.enable(canadianPostal)
                    symbology.CHINESE_2OF5.enable(chinese2of5)
                    symbology.CODABAR.enable(codabar)
                    symbology.CODE11.enable(code11)
                    symbology.CODE39.enable(code39)
                    symbology.CODE93.enable(code93)
                    symbology.CODE128.enable(code128)
                    symbology.COMPOSITE_AB.enable(compositeAB)
                    symbology.COMPOSITE_C.enable(compositeC)
                    symbology.D2OF5.enable(d2of5)
                    symbology.DATAMATRIX.enable(datamatrix)
                    symbology.DOTCODE.enable(dotcode)
                    symbology.DUTCH_POSTAL.enable(dutchPostal)
                    symbology.EAN8.enable(ean8)
                    symbology.EAN13.enable(ean13)
                    symbology.FINNISH_POSTAL_4S.enable(finnishPostal4s)
                    symbology.GRID_MATRIX.enable(gridMatrix)
                    symbology.GS1_DATABAR.enable(gs1Databar)
                    symbology.GS1_DATABAR_EXPANDED.enable(gs1DatabarExpanded)
                    symbology.GS1_DATABAR_LIM.enable(gs1DatabarLim)
                    symbology.GS1_DATAMATRIX.enable(gs1Datamatrix)
                    symbology.GS1_QRCODE.enable(gs1Qrcode)
                    symbology.HANXIN.enable(hanxin)
                    symbology.I2OF5.enable(i2of5)
                    symbology.JAPANESE_POSTAL.enable(japanesePostal)
                    symbology.KOREAN_3OF5.enable(korean3of5)
                    symbology.MAILMARK.enable(mailmark)
                    symbology.MATRIX_2OF5.enable(matrix2of5)
                    symbology.MAXICODE.enable(maxicode)
                    symbology.MICROPDF.enable(micropdf)
                    symbology.MICROQR.enable(microqr)
                    symbology.MSI.enable(msi)
                    symbology.PDF417.enable(pdf417)
                    symbology.QRCODE.enable(qrcode)
                    symbology.TLC39.enable(tlc39)
                    symbology.TRIOPTIC39.enable(trioptic39)
                    symbology.UK_POSTAL.enable(ukPostal)
                    symbology.UPCA.enable(upcA)
                    symbology.UPCE0.enable(upcE)
                    symbology.UPCE1.enable(upce1)
                    symbology.USPLANET.enable(usplanet)
                    symbology.USPOSTNET.enable(uspostnet)
                    symbology.US4STATE.enable(us4state)
                    symbology.US4STATE_FICS.enable(us4stateFics)
                }
            }

        }

    }


    private suspend fun initializePalletAndBoxLocalizer() {

        LOGD(
            TAG,
            "initializePalletAndBoxLocalizer ${wandModeSettings?.invoke()?.modelInput?.width} X ${wandModeSettings?.invoke()?.modelInput?.height}"
        )
        palletAndBoxLocalizer?.dispose()
        palletAndBoxLocalizer = null

        val inferenceSize = PALLET_LOCALIZER_INPUT_SIZE
        val locSettings = Localizer.Settings(PALLET_AND_BOX_MODEL_NAME)

        //Swap the values as the presented index is reverse of what model expects
        val processorOrder = arrayOf(2, 0, 1)

        locSettings.inferencerOptions.runtimeProcessorOrder = processorOrder

        locSettings.inferencerOptions.defaultDims.width = inferenceSize.width
        locSettings.inferencerOptions.defaultDims.height = inferenceSize.height

        try {
            palletAndBoxLocalizer = Localizer.getLocalizer(locSettings, Dispatchers.IO.asExecutor())
                .exceptionally { e: Throwable ->
                    LOGE(TAG, "Pallet and Box Localizer init Failed -> " + e.message)
                    null
                }.await()
        } catch (e: IOException) {
            LOGE(TAG, "Pallet and Box Localizer init Failed -> " + e.message)
        }
    }


    fun stop() {
        LOGD(TAG, "Dispose All Models")
        barcodeDecoder?.dispose()
        barcodeDecoder = null

        palletAndBoxLocalizer?.dispose()
        palletAndBoxLocalizer = null

        configBarcodeLocalizer?.dispose()
        configBarcodeLocalizer = null
        modelInitiate.value = false
    }
    fun getBarcodeDecoder() = barcodeDecoder
    fun getPalletAndBoxLocalizer() = palletAndBoxLocalizer
    fun getConfigBarcodeDecoder() = configBarcodeLocalizer

    fun isAlreadyAvailable() =
        (barcodeDecoder != null && palletAndBoxLocalizer != null)
}