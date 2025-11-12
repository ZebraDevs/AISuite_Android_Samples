// Copyright (c) 2024-2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.aidatacapturedemo.model

import com.zebra.aidatacapturedemo.data.OCRFilterData
import com.zebra.aidatacapturedemo.data.OCRFilterType
import com.zebra.aidatacapturedemo.data.ResultData
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

class TextOCRUtils {
    companion object {
        /**
         * Helper function used to filter the OCR Results based on the
         * requested ocrSearchType Param using pre-build regular express
         *
         * @param ocrFilterData - Selected OCRSearchType option
         * @param outputOCRResultData - Total Array list of OCR Result
         *
         * @return List of OcrResultData based on the requested OCRSearchType Filter
         */
        fun toFilteredByType(
            ocrFilterData: OCRFilterData,
            outputOCRResultData: MutableList<ResultData>
        ): List<ResultData> {

            val filteredOCRResultData = mutableListOf<ResultData>()

            when (ocrFilterData.ocrFilterType) {

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
                            d.text.length in (ocrFilterData.numericCharLengthRange.start.toInt()..ocrFilterData.numericCharLengthRange.endInclusive.toInt())
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
                        "^[a-zA-Z]{${ocrFilterData.alphaCharLengthRange.start.toInt()},${ocrFilterData.alphaCharLengthRange.endInclusive.toInt()}}\$".toRegex()
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
                        "^[a-zA-Z0-9]{${ocrFilterData.alphaNumericCharLengthRange.start.toInt()},${ocrFilterData.alphaNumericCharLengthRange.endInclusive.toInt()}}\$".toRegex()
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
                        ocrFilterData.exactMatchStringList.forEach { exactMatchString ->
                            if (exactMatchString.equals(d.text, ignoreCase = true)) {
                                filteredOCRResultData += ResultData(
                                    boundingBox = d.boundingBox,
                                    text = d.text
                                )
                            }
                        }
                    }
                }

                OCRFilterType.STARTS_WITH -> {
                    if (ocrFilterData.startsWithString.isNotBlank()) {
                        for (d in outputOCRResultData) {
                            if (d.text.startsWith(
                                    ocrFilterData.startsWithString,
                                    ignoreCase = true
                                )
                            ) {
                                filteredOCRResultData += ResultData(
                                    boundingBox = d.boundingBox,
                                    text = d.text
                                )
                            }
                        }
                    }
                }

                OCRFilterType.CONTAINS -> {
                    if (ocrFilterData.containsString.isNotBlank()) {
                        for (d in outputOCRResultData) {
                            if (d.text.contains(ocrFilterData.containsString, ignoreCase = true)) {
                                filteredOCRResultData += ResultData(
                                    boundingBox = d.boundingBox,
                                    text = d.text
                                )
                            }
                        }
                    }
                }


                OCRFilterType.REGEX -> {
                    if (ocrFilterData.regexString.isNotBlank()) {
                        validateRegexSyntax(ocrFilterData.regexString)?.let { regexPattern ->
                            for (d in outputOCRResultData) {
                                if (regexPattern.matcher(d.text).matches()) {
                                    filteredOCRResultData += ResultData(
                                        boundingBox = d.boundingBox,
                                        text = d.text
                                    )
                                }
                            }
                        }
                    }
                }
            }

            return filteredOCRResultData
        }

        fun validateRegexSyntax(regexString: String): Pattern? {

            // replace if any '\\' found on the regex with '\' as sometime user may get this online suggestion
            val userInputString = regexString.replace(
                "\\\\",
                "\\"
            )

            return try {
                Pattern.compile(userInputString)
            } catch (e: PatternSyntaxException) {
                null
            }
        }
    }

}
