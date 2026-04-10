package com.zebra.aidatacapturedemo.model

import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.data.AdvancedFilterOption
import com.zebra.aidatacapturedemo.data.CharacterMatchFilterOption
import com.zebra.aidatacapturedemo.data.CharacterTypeFilterOption
import com.zebra.aidatacapturedemo.data.OcrRegularFilterOption
import com.zebra.aidatacapturedemo.data.ResultData
import com.zebra.aidatacapturedemo.ui.view.RegexConstant
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * FilterUtils is a utility class that provides functions to filter OCR and Barcode result data
 * based on user-selected criteria in the UI state.
 */
class FilterUtils {
    companion object {
        fun getOcrFilteredResultData(
            uiState: AIDataCaptureDemoUiState,
            outputOCRResultData: MutableList<ResultData>
        ): List<ResultData> {

            val filteredOCRResultData = mutableListOf<ResultData>()

            when (uiState.ocrFilterData.selectedRegularFilterOption) {
                OcrRegularFilterOption.UNFILTERED -> {
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

                OcrRegularFilterOption.REGEX -> {
                    // This list holds all the possible regex patterns which includes default + additional regex strings
                    val regexPatternList: MutableList<Pattern> = mutableListOf()

                    // The following validation block of code is useful for regex validation.
                    val regexStringDefault =
                        uiState.ocrFilterData.selectedRegexFilterData.regexDefaultString
                    if (regexStringDefault.isNotBlank()) {

                        // add default regex string
                        validateRegexSyntax(regexStringDefault)?.let { pattern ->
                            regexPatternList.add(pattern)
                        }

                        // add additional regex string(s)
                        uiState.ocrFilterData.selectedRegexFilterData.regexAdditionalStringList.forEach { additionalRegexString ->
                            if (additionalRegexString.isNotBlank()) {
                                validateRegexSyntax(additionalRegexString)?.let { pattern ->
                                    regexPatternList.add(pattern)
                                }
                            }
                        }
                    }

                    if (regexPatternList.isNotEmpty()) {
                        outputOCRResultData.forEach { resultData ->
                            var isRegexMatchedFound = false

                            run outerLoop@{
                                regexPatternList.forEach { regexPattern ->
                                    if (regexPattern.matcher(resultData.text).matches()) {
                                        isRegexMatchedFound = true
                                        return@outerLoop // skip the other additional regex
                                    }
                                }
                            }

                            if (isRegexMatchedFound) {
                                filteredOCRResultData += ResultData(
                                    boundingBox = resultData.boundingBox,
                                    text = resultData.text
                                )
                            }
                        }
                    }
                }

                OcrRegularFilterOption.ADVANCED -> {
                    outputOCRResultData.forEach { resultData ->
                        var isAdvancedMatchedFound = true

                        run outerLoop@{
                            uiState.ocrFilterData.selectedAdvancedFilterOptionList.forEach continueNextIteration@{ advancedFilterType ->
                                when (advancedFilterType) {
                                    AdvancedFilterOption.CHARACTER_TYPE -> {
                                        val ocrCharacterTypeFilterOptionListSize =
                                            uiState.ocrFilterData.selectedCharacterTypeFilterOptionList.size
                                        if (ocrCharacterTypeFilterOptionListSize > 0) {

                                            // If Select All is selected, skip any further check, because it is considered as wildcard
                                            if (uiState.ocrFilterData.selectedCharacterTypeFilterOptionList.contains(
                                                    CharacterTypeFilterOption.SELECT_ALL
                                                )
                                            ) {
                                                // skip other checks
                                            } else {

                                                if (ocrCharacterTypeFilterOptionListSize == 1) {
                                                    if (uiState.ocrFilterData.selectedCharacterTypeFilterOptionList.contains(
                                                            CharacterTypeFilterOption.ALPHA
                                                        )
                                                    ) {
                                                        if (RegexConstant.ALPHA_ONLY.matches(
                                                                resultData.text
                                                            )
                                                        ) {
                                                            return@continueNextIteration // continue next iteration, CHARACTER_TYPE condition success
                                                        } else {
                                                            isAdvancedMatchedFound = false
                                                            return@outerLoop // break the loop, CHARACTER_TYPE condition failed and skip this result
                                                        }
                                                    }

                                                    if (uiState.ocrFilterData.selectedCharacterTypeFilterOptionList.contains(
                                                            CharacterTypeFilterOption.NUMERIC
                                                        )
                                                    ) {
                                                        if (RegexConstant.NUMERIC_ONLY.matches(
                                                                resultData.text
                                                            )
                                                        ) {
                                                            return@continueNextIteration
                                                        } else {
                                                            isAdvancedMatchedFound = false
                                                            return@outerLoop
                                                        }
                                                    }

                                                    if (uiState.ocrFilterData.selectedCharacterTypeFilterOptionList.contains(
                                                            CharacterTypeFilterOption.INCLUDE_SPECIAL_CHARACTERS
                                                        )
                                                    ) {
                                                        if (RegexConstant.SPECIAL_CHARACTERS_ONLY.matches(
                                                                resultData.text
                                                            )
                                                        ) {
                                                            return@continueNextIteration
                                                        } else {
                                                            isAdvancedMatchedFound = false
                                                            return@outerLoop
                                                        }
                                                    }
                                                } else { // ocrCharacterTypeFilterOptionListSize == 2
                                                    // hybrid selection found

                                                    if (uiState.ocrFilterData.selectedCharacterTypeFilterOptionList.contains(
                                                            CharacterTypeFilterOption.ALPHA
                                                        ) &&
                                                        uiState.ocrFilterData.selectedCharacterTypeFilterOptionList.contains(
                                                            CharacterTypeFilterOption.NUMERIC
                                                        )
                                                    ) {
                                                        if (RegexConstant.ALPHA_AND_NUMERIC_ONLY.matches(
                                                                resultData.text
                                                            )
                                                        ) {
                                                            return@continueNextIteration
                                                        } else {
                                                            isAdvancedMatchedFound = false
                                                            return@outerLoop
                                                        }
                                                    } else if (uiState.ocrFilterData.selectedCharacterTypeFilterOptionList.contains(
                                                            CharacterTypeFilterOption.ALPHA
                                                        ) &&
                                                        uiState.ocrFilterData.selectedCharacterTypeFilterOptionList.contains(
                                                            CharacterTypeFilterOption.INCLUDE_SPECIAL_CHARACTERS
                                                        )
                                                    ) {
                                                        if (RegexConstant.ALPHA_AND_SPECIAL_CHARACTERS_ONLY.matches(
                                                                resultData.text
                                                            )
                                                        ) {
                                                            return@continueNextIteration
                                                        } else {
                                                            isAdvancedMatchedFound = false
                                                            return@outerLoop
                                                        }
                                                    } else {
                                                        if (RegexConstant.NUMERIC_AND_SPECIAL_CHARACTERS_ONLY.matches(
                                                                resultData.text
                                                            )
                                                        ) {
                                                            return@continueNextIteration
                                                        } else {
                                                            isAdvancedMatchedFound = false
                                                            return@outerLoop
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // perform more validation if character match is selected
                                    AdvancedFilterOption.CHARACTER_MATCH -> {
                                        when (uiState.ocrFilterData.selectedCharacterMatchFilterData.type) {
                                            CharacterMatchFilterOption.STARTS_WITH -> {
                                                val areAllStringsNotBlank =
                                                    uiState.ocrFilterData.selectedCharacterMatchFilterData.startsWithStringList.all { it.isNotBlank() }
                                                if (areAllStringsNotBlank) {
                                                    uiState.ocrFilterData.selectedCharacterMatchFilterData.startsWithStringList.forEach { startsWithString ->
                                                        if (resultData.text.startsWith(
                                                                startsWithString,
                                                                ignoreCase = true
                                                            )
                                                        ) {
                                                            return@continueNextIteration
                                                        }
                                                    }
                                                }
                                                isAdvancedMatchedFound = false
                                                return@outerLoop
                                            }

                                            CharacterMatchFilterOption.CONTAINS -> {
                                                val areAllStringsNotBlank =
                                                    uiState.ocrFilterData.selectedCharacterMatchFilterData.containsStringList.all { it.isNotBlank() }
                                                if (areAllStringsNotBlank) {
                                                    uiState.ocrFilterData.selectedCharacterMatchFilterData.containsStringList.forEach { containString ->
                                                        if (resultData.text.contains(
                                                                containString,
                                                                ignoreCase = true
                                                            )
                                                        ) {
                                                            return@continueNextIteration
                                                        }
                                                    }
                                                }
                                                isAdvancedMatchedFound = false
                                                return@outerLoop
                                            }

                                            CharacterMatchFilterOption.EXACT_MATCH -> {
                                                val areAllStringsNotBlank =
                                                    uiState.ocrFilterData.selectedCharacterMatchFilterData.exactMatchStringList.all { it.isNotBlank() }
                                                if (areAllStringsNotBlank) {

                                                    uiState.ocrFilterData.selectedCharacterMatchFilterData.exactMatchStringList.forEach { exactMatchString ->
                                                        if (resultData.text.equals(
                                                                exactMatchString,
                                                                ignoreCase = true
                                                            )
                                                        ) {
                                                            return@continueNextIteration
                                                        }
                                                    }
                                                }
                                                isAdvancedMatchedFound = false
                                                return@outerLoop
                                            }
                                        }
                                    }

                                    AdvancedFilterOption.STRING_LENGTH -> {

                                        if (resultData.text.length in uiState.ocrFilterData.selectedStringLengthRange.start.toInt()..uiState.ocrFilterData.selectedStringLengthRange.endInclusive.toInt()) {
                                            return@continueNextIteration
                                        } else {
                                            isAdvancedMatchedFound = false
                                            return@outerLoop
                                        }
                                    }
                                }
                            }
                        }

                        if (isAdvancedMatchedFound) {
                            filteredOCRResultData += ResultData(
                                boundingBox = resultData.boundingBox,
                                text = resultData.text
                            )
                        }
                    }
                }

            }

            return filteredOCRResultData
        }

        fun getBarcodeFilteredResultData(
            uiState: AIDataCaptureDemoUiState,
            outputBarcodeResultData: MutableList<ResultData>
        ): List<ResultData> {

            val filteredBarcodeResultData = mutableListOf<ResultData>()

            outputBarcodeResultData.forEach { resultData ->
                var isBarcodeMatchedFound = true

                run outerLoop@{
                    uiState.barcodeFilterData.selectedAdvancedFilterOptionList.forEach continueNextIteration@{ advancedFilterType ->

                        when (advancedFilterType) {
                            AdvancedFilterOption.CHARACTER_MATCH -> {
                                when (uiState.barcodeFilterData.selectedCharacterMatchFilterData.type) {
                                    CharacterMatchFilterOption.STARTS_WITH -> {
                                        val areAllStringsNotBlank =
                                            uiState.barcodeFilterData.selectedCharacterMatchFilterData.startsWithStringList.all { it.isNotBlank() }
                                        if (areAllStringsNotBlank) {
                                            uiState.barcodeFilterData.selectedCharacterMatchFilterData.startsWithStringList.forEach { startsWithString ->
                                                if (resultData.text.startsWith(
                                                        startsWithString,
                                                        ignoreCase = true
                                                    )
                                                ) {
                                                    return@continueNextIteration
                                                }
                                            }
                                        }
                                        isBarcodeMatchedFound = false
                                        return@outerLoop
                                    }

                                    CharacterMatchFilterOption.CONTAINS -> {
                                        val areAllStringsNotBlank =
                                            uiState.barcodeFilterData.selectedCharacterMatchFilterData.containsStringList.all { it.isNotBlank() }
                                        if (areAllStringsNotBlank) {
                                            uiState.barcodeFilterData.selectedCharacterMatchFilterData.containsStringList.forEach { containString ->
                                                if (resultData.text.contains(
                                                        containString,
                                                        ignoreCase = true
                                                    )
                                                ) {
                                                    return@continueNextIteration
                                                }
                                            }
                                        }
                                        isBarcodeMatchedFound = false
                                        return@outerLoop
                                    }

                                    CharacterMatchFilterOption.EXACT_MATCH -> {
                                        val areAllStringsNotBlank =
                                            uiState.barcodeFilterData.selectedCharacterMatchFilterData.exactMatchStringList.all { it.isNotBlank() }
                                        if (areAllStringsNotBlank) {
                                            uiState.barcodeFilterData.selectedCharacterMatchFilterData.exactMatchStringList.forEach { exactMatchString ->
                                                if (resultData.text.equals(
                                                        exactMatchString,
                                                        ignoreCase = true
                                                    )
                                                ) {
                                                    return@continueNextIteration
                                                }
                                            }
                                        }
                                        isBarcodeMatchedFound = false
                                        return@outerLoop
                                    }
                                }
                            }

                            AdvancedFilterOption.STRING_LENGTH -> {
                                if (resultData.text.length in uiState.barcodeFilterData.selectedStringLengthRange.start.toInt()..uiState.barcodeFilterData.selectedStringLengthRange.endInclusive.toInt()) {
                                    return@continueNextIteration
                                } else {
                                    isBarcodeMatchedFound = false
                                    return@outerLoop
                                }
                            }

                            else -> {
                                TODO("Unhandled barcode filter received = $advancedFilterType")
                            }
                        }
                    }
                }

                if (isBarcodeMatchedFound) {
                    filteredBarcodeResultData += ResultData(
                        boundingBox = resultData.boundingBox,
                        text = resultData.text
                    )
                }
            }

            return filteredBarcodeResultData
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