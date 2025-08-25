// Copyright (c) 2024-2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.aidatacapturedemo.model

import com.zebra.aidatacapturedemo.data.OCRFilterData
import com.zebra.aidatacapturedemo.data.OCRFilterType
import com.zebra.aidatacapturedemo.data.ResultData

class TextOCRUtils {
    companion object {
        /**
         * Helper function used to filter the OCR Results based on the
         * requested ocrSearchType Param using pre-build regular express
         *
         * @param ocrFilterTypeData - Selected OCRSearchType option
         * @param outputOCRResultData - Total Array list of OCR Result
         *
         * @return List of OcrResultData based on the requested OCRSearchType Filter
         */
        fun toFilteredByType(
            ocrFilterTypeData: OCRFilterData,
            outputOCRResultData: MutableList<ResultData>
        ): List<ResultData> {

            val filteredOCRResultData = mutableListOf<ResultData>()

            when (ocrFilterTypeData.ocrFilterType) {

                // The output is not filtered
                OCRFilterType.SHOW_ALL -> {
                    val regex = "(.*?)".toRegex()
                    for (d in outputOCRResultData) {
                        if (regex.matches(d.text)) {
                            filteredOCRResultData += ResultData(
                                boundingBox = d.boundingBox,
                                text = d.text
                            )
                        }
                    }
                }
                // The output is limited to numeric characters of specified size range only
                OCRFilterType.NUMERIC_CHARACTERS_ONLY -> {
                    val regex = "^[+-]?\\d*\\.?\\d+\$".toRegex()
                    for (d in outputOCRResultData) {
                        if (regex.matches(d.text) &&
                            d.text.length in (ocrFilterTypeData.charLengthMin!!..ocrFilterTypeData.charLengthMax!!)
                        ) {
                            filteredOCRResultData += ResultData(
                                boundingBox = d.boundingBox,
                                text = d.text
                            )
                        }
                    }
                }
                // The output is limited to alpha characters of specified size range only
                OCRFilterType.ALPHA_CHARACTERS_ONLY -> {
                    val regex =
                        "^[a-zA-Z]{${ocrFilterTypeData.charLengthMin},${ocrFilterTypeData.charLengthMax}}\$".toRegex()
                    for (d in outputOCRResultData) {
                        if (regex.matches(d.text)) {
                            filteredOCRResultData += ResultData(
                                boundingBox = d.boundingBox,
                                text = d.text
                            )
                        }
                    }
                }
                // The output is limited to alpha numeric characters of specified size range only
                OCRFilterType.ALPHA_NUMERIC_CHARACTERS_ONLY -> {
                    val regex =
                        "^[a-zA-Z0-9]{${ocrFilterTypeData.charLengthMin},${ocrFilterTypeData.charLengthMax}}\$".toRegex()
                    for (d in outputOCRResultData) {
                        if (regex.matches(d.text)) {
                            filteredOCRResultData += ResultData(
                                boundingBox = d.boundingBox,
                                text = d.text
                            )
                        }
                    }
                }
                // The output is limited to only the text that exact matches the given pattern.
                OCRFilterType.EXACT_MATCH -> {
                    for (d in outputOCRResultData) {
                        if (ocrFilterTypeData.exactMatchString!! == d.text) {
                            filteredOCRResultData += ResultData(
                                boundingBox = d.boundingBox,
                                text = d.text
                            )
                        }
                    }
                }
            }

            return filteredOCRResultData
        }
    }

}
