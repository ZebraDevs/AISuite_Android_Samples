package com.zebra.ai.barcodefinder.domain.proxy

import android.util.Log
import com.zebra.ai.barcodefinder.domain.enums.ProcessorType
import com.zebra.ai.barcodefinder.domain.model.AppSettings
import com.zebra.ai.vision.detector.BarcodeDecoder
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

/**
 * A class to handle BarcodeDecoder initialization and configuration.
 */
class BarcodeDecoderProxy {

    private val TAG = "BarcodeDecoderProxy"

    /**
     * Initializes and returns a BarcodeDecoder instance based on the provided settings.
     *
     * @param settings The settings for the BarcodeDecoder.
     * @param executor The executor for BarcodeDecoder operations.
     * @return A CompletableFuture for BarcodeDecoder, allowing async handling.
     */
    fun createBarcodeDecoder(
        settings: BarcodeDecoder.Settings,
        executor: Executor
    ): CompletableFuture<BarcodeDecoder> {
        return BarcodeDecoder.getBarcodeDecoder(settings, executor)
            .thenApply { decoderInstance ->
                Log.d(TAG, "BarcodeDecoder initialized successfully.")
                decoderInstance
            }.exceptionally { ex ->
                Log.e(TAG, "Failed to initialize BarcodeDecoder: ${ex.message}", ex)
                throw ex
            }
    }

    /**
     * Configures the BarcodeDecoder settings based on the provided application settings.
     *
     * @param appSettings Application-specific settings to configure the decoder.
     * @return Configured BarcodeDecoder.Settings instance.
     */
    fun configureSettings(appSettings: AppSettings): BarcodeDecoder.Settings {
        val modelName = "barcode-localizer"
        val settings = BarcodeDecoder.Settings(modelName)

        try {
            // Configure symbology settings
            settings.Symbology?.let { symbology ->
                with(appSettings.barcodeSymbology) {
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

            // Configure processor type
            settings.detectorSetting?.inferencerOptions?.let { inferencerOptions ->
                val processorOrder = when (appSettings.processorType) {
                    ProcessorType.DSP -> arrayOf(2)
                    ProcessorType.GPU -> arrayOf(1)
                    ProcessorType.CPU -> arrayOf(0)
                    ProcessorType.AUTO -> arrayOf(2,0,1)
                }
                inferencerOptions.runtimeProcessorOrder = processorOrder.map { Integer.valueOf(it) }.toTypedArray()

                Log.d(
                    TAG,
                    "Applied processor type: ${appSettings.processorType.displayNameResId} (${appSettings.processorType.identifierResId})"
                )

                inferencerOptions.defaultDims?.let { dynamicDims ->
                    val inputWidth = appSettings.modelInput.width
                    val inputHeight = appSettings.modelInput.height
                    dynamicDims.width = inputWidth
                    dynamicDims.height = inputHeight
                    Log.d(
                        TAG,
                        "Applied model input: ${appSettings.modelInput.displayNameResId} (${inputWidth}x${inputHeight})"
                    )
                }
            }
            Log.d(TAG, "BarcodeDecoder settings configured successfully.")
        } catch (ex: Exception) {
            Log.w(TAG, "Failed to configure BarcodeDecoder settings: ${ex.message}", ex)
        }

        return settings
    }

}