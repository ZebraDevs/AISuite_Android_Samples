package com.zebra.aidatacapturedemo.model

import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.data.BarcodeFieldFilterConfig
import com.zebra.aidatacapturedemo.data.CharacterMatchData
import com.zebra.aidatacapturedemo.data.CharacterMatchFilterOption
import com.zebra.aidatacapturedemo.data.CharacterTypeFilterOption
import com.zebra.aidatacapturedemo.data.OcrFieldFilterConfig
import com.zebra.aidatacapturedemo.data.ResultData
import com.zebra.aidatacapturedemo.ui.view.FilterMatchType
import com.zebra.aidatacapturedemo.ui.view.RegexConstant
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import kotlin.random.Random

/**
 * FilterUtils is a utility class that provides functions to filter OCR and Barcode result data
 * based on user-selected criteria in the UI state.
 */
object FilterUtils {

    fun generateFieldId(title: String): String {
        val slug = title.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
        return "${slug}_${Random.nextInt(1000, 9999)}"
    }

    fun validateRegexSyntax(regexString: String): Pattern? {
        val userInputString = regexString.replace("\\\\", "\\")
        return try {
            Pattern.compile(userInputString)
        } catch (_: PatternSyntaxException) {
            null
        }
    }

    /**
     * Field-scoped OCR filter evaluation.
     * A result passes if it matches at least one configured field rule-set.
     */
    fun getOcrFilteredResultData(
        uiState: AIDataCaptureDemoUiState,
        outputOCRResultData: List<ResultData>
    ): List<ResultData> {
        val selectedIds = uiState.ocrFilterData.selectedFieldIds.toSet()
        if (selectedIds.isEmpty()) return outputOCRResultData

        val fieldConfigs = uiState.ocrFilterData.fieldFilterConfigs
            .filter { it.fieldId in selectedIds }
        if (fieldConfigs.isEmpty()) return outputOCRResultData

        val accepted = mutableListOf<ResultData>()
        for (result in outputOCRResultData) {
            val text = result.text
            if (text.isBlank()) continue

            // OR across field configs (match any configured field)
            val matchedAnyField = fieldConfigs.any { cfg ->
                evaluateOcrFieldConfig(text, cfg)
            }

            if (matchedAnyField) accepted.add(result)
        }

        return accepted
    }

    /**
     * Field-scoped Barcode filter evaluation.
     * A result passes if it matches at least one configured field rule-set.
     */
    fun getBarcodeFilteredResultData(
        uiState: AIDataCaptureDemoUiState,
        outputBarcodeResultData: List<ResultData>
    ): List<ResultData> {
        val selectedIds =
            uiState.barcodeFilterData.selectedFieldIds.toSet()
        if (selectedIds.isEmpty()) return outputBarcodeResultData

        val fieldConfigs = uiState.barcodeFilterData.fieldFilterConfigs
            .filter { it.fieldId in selectedIds }
        if (fieldConfigs.isEmpty()) return outputBarcodeResultData

        val accepted = mutableListOf<ResultData>()
        for (result in outputBarcodeResultData) {
            val text = result.text
            if (text.isBlank()) continue

            // OR across field configs (match any configured field)
            val matchedAnyField = fieldConfigs.any { cfg ->
                var anyEnabled = false
                var anyFilterPassed = false

                // CHARACTER MATCH FILTER
                if (cfg.characterMatchEnabled && !anyFilterPassed) {
                    anyEnabled = true
                    val cm = cfg.characterMatchData
                    val charMatchPass = cm != null &&
                            matchesCharacterMatch(text, cm)
                    if (charMatchPass) anyFilterPassed = true
                }

                // STRING LENGTH FILTER
                if (cfg.stringLengthEnabled && !anyFilterPassed) {
                    anyEnabled = true
                    val range = cfg.stringLengthRange
                    val stringLengthPass = range != null &&
                            text.length.toFloat() in range
                    if (stringLengthPass) anyFilterPassed = true
                }

                // If no filters enabled for this field, don't treat it as a match.
                anyEnabled && anyFilterPassed
            }

            if (matchedAnyField) accepted.add(result)
        }

        return accepted
    }

    /**
     * Returns which filter type matched for OCR result
     */
    fun getOcrMatchingFilterType(
        text: String,
        cfg: OcrFieldFilterConfig
    ): FilterMatchType? {
        if (text.isBlank()) return null

        var anyEnabled = false
        var matchedFilter: FilterMatchType? = null

        // REGEX FILTER
        if (cfg.regexEnabled && matchedFilter == null) {
            anyEnabled = true
            val regexData = cfg.regexData
            val regexPass = if (regexData == null) {
                false
            } else {
                val patterns = mutableListOf<String>()
                if (regexData.regexDefaultString.isNotBlank()) {
                    patterns.add(regexData.regexDefaultString)
                }

                patterns.addAll(regexData.regexAdditionalStringList.filter {
                    it.isNotBlank()
                })

                if (patterns.isEmpty()) {
                    false
                } else {
                    patterns.any { pattern ->

                        validateRegexSyntax(pattern)?.matcher(text)?.matches() == true
                    }
                }
            }
            if (regexPass) matchedFilter = FilterMatchType.REGEX
        }

        // CHARACTER MATCH FILTER
        if (cfg.characterMatchEnabled && matchedFilter == null) {
            anyEnabled = true
            val cm = cfg.characterMatchData
            val charMatchPass = cm != null && matchesCharacterMatch(
                text,
                cm
            )
            if (charMatchPass) matchedFilter =
                FilterMatchType.CHARACTER_MATCH
        }

        // STRING LENGTH FILTER
        if (cfg.stringLengthEnabled && matchedFilter == null) {
            anyEnabled = true
            val range = cfg.stringLengthRange
            val stringLengthPass = range != null && text.length.toFloat() in range
            if (stringLengthPass) matchedFilter = FilterMatchType.STRING_LENGTH
        }

        // CHARACTER TYPE FILTER
        if (cfg.characterTypeEnabled && matchedFilter == null) {
            anyEnabled = true
            val options = cfg.characterTypeOptions
            val charTypePass = validateCharacterType(text, options)
            if (charTypePass) matchedFilter = FilterMatchType.CHARACTER_TYPE
        }

        return if (anyEnabled && matchedFilter != null) matchedFilter
        else null
    }

    /**
     * Returns which filter type matched for Barcode result
     */
    fun getBarcodeMatchingFilterType(
        text: String,
        cfg: BarcodeFieldFilterConfig
    ): FilterMatchType? {
        if (text.isBlank()) return null

        var anyEnabled = false
        var matchedFilter: FilterMatchType? = null

        // CHARACTER MATCH FILTER
        if (cfg.characterMatchEnabled && matchedFilter == null) {
            anyEnabled = true
            val cm = cfg.characterMatchData
            val charMatchPass = cm != null && matchesCharacterMatch(
                text,
                cm
            )
            if (charMatchPass) matchedFilter = FilterMatchType.CHARACTER_MATCH
        }

        // STRING LENGTH FILTER
        if (cfg.stringLengthEnabled && matchedFilter == null) {
            anyEnabled = true
            val range = cfg.stringLengthRange
            val stringLengthPass = range != null && text.length.toFloat() in range
            if (stringLengthPass) matchedFilter = FilterMatchType.STRING_LENGTH
        }

        return if (anyEnabled && matchedFilter != null) matchedFilter
        else null
    }

    /**
     * OCR field-level evaluation with enabled flags.
     */
    private fun evaluateOcrFieldConfig(
        text: String,
        cfg: OcrFieldFilterConfig
    ): Boolean {
        var anyEnabled = false
        var anyFilterPassed = false

        // REGEX FILTER
        if (cfg.regexEnabled) {
            anyEnabled = true
            val regexData = cfg.regexData
            val regexPass = if (regexData == null) false else {
                val patterns = mutableListOf<String>()
                if (regexData.regexDefaultString.isNotBlank()) {
                    patterns.add(regexData.regexDefaultString)
                }

                patterns.addAll(regexData.regexAdditionalStringList.filter {
                    it.isNotBlank()
                })

                if (patterns.isEmpty()) false else patterns.any { pattern ->
                    validateRegexSyntax(pattern)
                        ?.matcher(text)?.matches() == true
                }
            }
            if (regexPass) anyFilterPassed = true
        }

        // CHARACTER MATCH FILTER
        if (cfg.characterMatchEnabled && !anyFilterPassed) {
            anyEnabled = true
            val cm = cfg.characterMatchData
            val charMatchPass = cm != null && matchesCharacterMatch(
                text,
                cm
            )
            if (charMatchPass) anyFilterPassed = true
        }

        // STRING LENGTH FILTER
        if (cfg.stringLengthEnabled && !anyFilterPassed) {
            anyEnabled = true
            val range = cfg.stringLengthRange
            val stringLengthPass = range != null && text.length.toFloat() in range
            if (stringLengthPass) anyFilterPassed = true
        }

        // CHARACTER TYPE FILTER
        if (cfg.characterTypeEnabled && !anyFilterPassed) {
            anyEnabled = true
            val options = cfg.characterTypeOptions
            val charTypePass = validateCharacterType(text, options)
            if (charTypePass) anyFilterPassed = true
        }

        return anyEnabled && anyFilterPassed
    }

    private fun matchesCharacterMatch(value: String, data: CharacterMatchData): Boolean {
        return when (data.type) {
            CharacterMatchFilterOption.STARTS_WITH ->
                data.startsWithStringList.any { token ->
                    token.isNotBlank() && value.startsWith(
                        token,
                        ignoreCase = true
                    )
                }

            CharacterMatchFilterOption.CONTAINS ->
                data.containsStringList.any { token ->
                    token.isNotBlank() && value.contains(
                        token,
                        ignoreCase = true
                    )
                }

            CharacterMatchFilterOption.EXACT_MATCH ->
                data.exactMatchStringList.any { token ->
                    token.isNotBlank() && value.equals(
                        token,
                        ignoreCase = true
                    )
                }
        }
    }

    private fun validateCharacterType(
        text: String,
        options: List<CharacterTypeFilterOption>
    ): Boolean {
        if (options.isEmpty()) return true
        if (text.isBlank()) return false

        if (options.contains(CharacterTypeFilterOption.SELECT_ALL)) return true

        val hasAlpha = options.contains(CharacterTypeFilterOption.ALPHA)
        val hasNumeric = options.contains(CharacterTypeFilterOption.NUMERIC)
        val hasSpecial = options.contains(CharacterTypeFilterOption.INCLUDE_SPECIAL_CHARACTERS)

        if (hasAlpha && !hasNumeric && !hasSpecial) {
            return RegexConstant.ALPHA_ONLY.matches(text)
        }
        if (!hasAlpha && hasNumeric && !hasSpecial) {
            return RegexConstant.NUMERIC_ONLY.matches(text)
        }
        if (!hasAlpha && !hasNumeric && hasSpecial) {
            return RegexConstant.SPECIAL_CHARACTERS_ONLY.matches(text)
        }
        if (hasAlpha && hasNumeric && !hasSpecial) {
            return RegexConstant.ALPHA_AND_NUMERIC_ONLY.matches(text)
        }
        if (hasAlpha && !hasNumeric && hasSpecial) {
            return RegexConstant.ALPHA_AND_SPECIAL_CHARACTERS_ONLY.matches(text)
        }
        if (!hasAlpha && hasNumeric && hasSpecial) {
            return RegexConstant.NUMERIC_AND_SPECIAL_CHARACTERS_ONLY.matches(text)
        }

        return hasAlpha && hasNumeric && hasSpecial
    }

    fun matchesOcrFieldResult(
        text: String,
        cfg: OcrFieldFilterConfig
    ): Boolean {
        if (text.isBlank()) return false
        return evaluateOcrFieldConfig(text, cfg)
    }

    fun matchesBarcodeFieldResult(
        text: String,
        cfg: BarcodeFieldFilterConfig
    ): Boolean {
        if (text.isBlank()) return false

        var anyEnabled = false
        var anyFilterPassed = false

        // CHARACTER MATCH FILTER
        if (cfg.characterMatchEnabled && !anyFilterPassed) {
            anyEnabled = true
            val cm = cfg.characterMatchData
            val charMatchPass = cm != null && matchesCharacterMatch(
                text,
                cm
            )
            if (charMatchPass) anyFilterPassed = true
        }

        // STRING LENGTH FILTER
        if (cfg.stringLengthEnabled && !anyFilterPassed) {
            anyEnabled = true
            val range = cfg.stringLengthRange
            val stringLengthPass = range != null && text.length.toFloat() in range
            if (stringLengthPass) anyFilterPassed = true
        }

        return anyEnabled && anyFilterPassed
    }

    fun getOrCreateOcrFieldConfig(
        uiState: AIDataCaptureDemoUiState,
        fieldId: String,
        fieldTitle: String
    ): OcrFieldFilterConfig {
        return uiState.ocrFilterData.fieldFilterConfigs.firstOrNull { it.fieldId == fieldId }
            ?: OcrFieldFilterConfig(
                fieldId = fieldId,
                fieldTitle = fieldTitle
            )
    }

    fun getOrCreateBarcodeFieldConfig(
        uiState: AIDataCaptureDemoUiState,
        fieldId: String,
        fieldTitle: String
    ): BarcodeFieldFilterConfig {
        return uiState.barcodeFilterData.fieldFilterConfigs.firstOrNull { it.fieldId == fieldId }
            ?: BarcodeFieldFilterConfig(
                fieldId = fieldId,
                fieldTitle = fieldTitle,
            )
    }
}