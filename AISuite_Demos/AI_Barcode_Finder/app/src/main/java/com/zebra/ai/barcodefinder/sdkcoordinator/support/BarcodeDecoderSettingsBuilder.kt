package com.zebra.ai.barcodefinder.sdkcoordinator.support

import com.zebra.ai.barcodefinder.sdkcoordinator.enums.ProcessorType
import com.zebra.ai.barcodefinder.sdkcoordinator.model.BarcodeSymbology
import com.zebra.ai.vision.detector.BarcodeDecoder

class  BarcodeDecoderSettingsBuilder {
    private val settings = BarcodeDecoder.Settings("barcode-localizer")

    // Configure symbologies
    fun configureSymbologies(barcodeSymbology: BarcodeSymbology): BarcodeDecoderSettingsBuilder {
        settings.Symbology?.let { symbology ->
            with(barcodeSymbology) {
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
        return this
    }

    // Configure processor type
    fun configureProcessorType(processorType: ProcessorType): BarcodeDecoderSettingsBuilder {
        settings.detectorSetting?.inferencerOptions?.let { inferencerOptions ->
            inferencerOptions.runtimeProcessorOrder = when (processorType) {
                ProcessorType.DSP -> arrayOf(2)
                ProcessorType.GPU -> arrayOf(1)
                ProcessorType.CPU -> arrayOf(0)
                ProcessorType.AUTO -> arrayOf(2, 0, 1)
            }
        }
        return this
    }

    // Configure model input dimensions
    fun configureModelInput(inputWidth: Int, inputHeight: Int): BarcodeDecoderSettingsBuilder {
        settings.detectorSetting?.inferencerOptions?.defaultDims?.let { dynamicDims ->
            dynamicDims.width = inputWidth
            dynamicDims.height = inputHeight
        }
        return this
    }

    // Build the settings object
    fun build(): BarcodeDecoder.Settings {
        return settings
    }
}