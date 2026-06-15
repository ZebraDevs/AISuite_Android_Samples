package com.zebra.aidatacapturedemo.data

/* * FilterData.kt is a data class that defines the structure for filter settings used in the
OCR-Barcode Find Usecase Demo. It includes various enums and data classes to represent
different filter options for OCR and barcode recognition. The filter settings allow users to
customize the detection process by applying regular expressions, character type filters,
character match filters, and string length filters.
This structured approach helps manage the complexity of filter configurations and provides a
clear way to store and access filter-related data.
 */
enum class FilterType(val value: String) {
    NONE(value = "None"),
    OCR_FILTER(value = "OcrFilter"),
    BARCODE_FILTER(value = "BarcodeFilter")
}

enum class OcrRegularFilterOption {
    UNFILTERED,
    REGEX,
    ADVANCED
}

enum class AdvancedFilterOption {
    CHARACTER_TYPE,
    CHARACTER_MATCH,
    STRING_LENGTH
}

enum class CharacterTypeFilterOption {
    SELECT_ALL,
    ALPHA,
    NUMERIC,
    INCLUDE_SPECIAL_CHARACTERS
}

enum class CharacterMatchFilterOption {
    STARTS_WITH,
    CONTAINS,
    EXACT_MATCH
}

enum class DetectionLevel {
    WORD,
    LINE
}

data class RegexData(
    var detectionLevel: DetectionLevel = DefaultValues.OCR_DETECTION_LEVEL,
    var regexAdditionalStringList: MutableList<String> = mutableListOf(),
    var regexDefaultString: String = DefaultValues.DEFAULT_REGEX_STRING
)

data class CharacterMatchData(
    var type: CharacterMatchFilterOption = DefaultValues.CHARACTER_MATCH_FILTER_OPTION,
    var detectionLevel: DetectionLevel = DefaultValues.OCR_DETECTION_LEVEL,
    var startsWithStringList: List<String> = listOf(),
    var containsStringList: List<String> = listOf(),
    var exactMatchStringList: List<String> = listOf()
)

data class OcrFilterData(
    var selectedRegularFilterOption: OcrRegularFilterOption = DefaultValues.OCR_REGULAR_FILTER_OPTION,
    var selectedAdvancedFilterOptionList: MutableList<AdvancedFilterOption> = mutableListOf(),
    var selectedRegexFilterData: RegexData = RegexData(),
    var selectedCharacterTypeFilterOptionList: MutableList<CharacterTypeFilterOption> = mutableListOf(),
    var selectedCharacterMatchFilterData: CharacterMatchData = CharacterMatchData(),
    var selectedStringLengthRange: ClosedFloatingPointRange<Float> = (DefaultValues.STRING_LENGTH_RANGE_MIN_VALUE ..DefaultValues.STRING_LENGTH_RANGE_MAX_VALUE)
)

data class BarcodeFilterData(
    var selectedAdvancedFilterOptionList: MutableList<AdvancedFilterOption> = mutableListOf(),
    var selectedCharacterMatchFilterData: CharacterMatchData = CharacterMatchData(),
    var selectedStringLengthRange: ClosedFloatingPointRange<Float> = (DefaultValues.STRING_LENGTH_RANGE_MIN_VALUE ..DefaultValues.STRING_LENGTH_RANGE_MAX_VALUE)
)

object DefaultValues {
    val OCR_REGULAR_FILTER_OPTION = OcrRegularFilterOption.UNFILTERED
    val CHARACTER_MATCH_FILTER_OPTION = CharacterMatchFilterOption.STARTS_WITH
    val OCR_DETECTION_LEVEL = DetectionLevel.WORD
    const val STRING_LENGTH_RANGE_MIN_VALUE = 2f
    const val STRING_LENGTH_RANGE_MAX_VALUE = 15f
    const val DEFAULT_REGEX_STRING = ""
}