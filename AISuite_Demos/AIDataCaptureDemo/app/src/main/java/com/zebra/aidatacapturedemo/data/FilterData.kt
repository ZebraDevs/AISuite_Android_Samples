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

enum class FilterMenuAction {
    NONE,
    SELECT_ALL,
    DESELECT_ALL,
    DELETE,
    RESET
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
    var fieldList: MutableList<FilterField> = mutableListOf(),
    var fieldFilterConfigs: MutableList<OcrFieldFilterConfig> =
        mutableListOf(),
    var selectedFieldIds: MutableList<String> = mutableListOf()
)

data class BarcodeFilterData(
    var fieldList: MutableList<FilterField> = mutableListOf(),
    var fieldFilterConfigs: MutableList<BarcodeFieldFilterConfig> =
        mutableListOf(),
    var selectedFieldIds: MutableList<String> = mutableListOf()
)


data class FilterField(
    val id: String,
    val title: String,
    val isDefault: Boolean = true
)

sealed class FieldFilterConfig {
    abstract val characterMatchData: CharacterMatchData?
    abstract val stringLengthRange: ClosedFloatingPointRange<Float>?
}

data class OcrFieldFilterConfig(
    var fieldId: String? = null,
    var fieldTitle: String? = null,
    var regexData: RegexData? = null,
    var characterTypeOptions: MutableList<CharacterTypeFilterOption> = mutableListOf(),
    override var characterMatchData: CharacterMatchData? = null,
    override var stringLengthRange: ClosedFloatingPointRange<Float>? = null,
    var regexEnabled: Boolean = false,
    var characterMatchEnabled: Boolean = false,
    var characterTypeEnabled: Boolean = false,
    var stringLengthEnabled: Boolean = false
) : FieldFilterConfig()

data class BarcodeFieldFilterConfig(
    var fieldId: String? = null,
    var fieldTitle: String? = null,
    override var characterMatchData: CharacterMatchData? = null,
    override var stringLengthRange: ClosedFloatingPointRange<Float>? = null,
    var characterMatchEnabled: Boolean = false,
    var stringLengthEnabled: Boolean = false
) : FieldFilterConfig()

object DefaultValues {
    val CHARACTER_MATCH_FILTER_OPTION = CharacterMatchFilterOption.STARTS_WITH
    val OCR_DETECTION_LEVEL = DetectionLevel.WORD
    const val STRING_LENGTH_RANGE_MIN_VALUE = 2f
    const val STRING_LENGTH_RANGE_MAX_VALUE = 15f
    const val DEFAULT_REGEX_STRING = ""
}