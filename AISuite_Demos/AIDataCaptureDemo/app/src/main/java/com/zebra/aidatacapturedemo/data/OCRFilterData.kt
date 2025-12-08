package com.zebra.aidatacapturedemo.data

/**
 * Available OCR FilterType options
 */
enum class OCRFilterType {
    SHOW_ALL,
    NUMERIC_CHARACTERS_ONLY,
    ALPHA_CHARACTERS_ONLY,
    ALPHA_NUMERIC_CHARACTERS_ONLY,
    EXACT_MATCH,
    STARTS_WITH,
    CONTAINS,
    REGEX
}

/**
 * Kotlin data class used to communicate OCR Filter detailed information from View to @see OCRReader
 *
 * @property ocrFilterType - Available Filter Type option
 *
 * @property exactMatchStringList - This property holds, the list of Strings entered by user and used only when OCRFilterType.EXACT_MATCH is requested
 *
 * @property startsWithStringList - This property holds, the list of Strings entered by user and used only when OCRFilterType.STARTS_WITH is requested
 *
 * @property containsStringList - This property holds, the list of Strings entered by user and used only when OCRFilterType.CONTAINS is requested
 *
 * @property regexString - This property holds, one regular expression string entered by user and used only when OCRFilterType.REGEX is requested
 *
 * @property numericCharLengthRange - Char Length Range value applicable for OCRFilterType.NUMERIC_CHARACTERS_ONLY
 *
 * @property alphaCharLengthRange - Char Length Range value applicable for OCRFilterType.ALPHA_CHARACTERS_ONLY
 *
 * @property alphaNumericCharLengthRange - Char Length Range value applicable for OCRFilterType.ALPHA_NUMERIC_CHARACTERS_ONLY
 *
 * @property startWithLengthRange - Char Length Range value applicable for OCRFilterType.STARTS_WITH
 *
 * @property containsLengthRange - Char Length Range value applicable for OCRFilterType.CONTAINS
 *
 */
data class OCRFilterData(
    val ocrFilterType: OCRFilterType,
    val exactMatchStringList: List<String> = listOf(),
    val startsWithStringList: List<String> = listOf(),
    val containsStringList: List<String> = listOf(),
    val regexString: String = "",
    var numericCharLengthRange: ClosedFloatingPointRange<Float> = (2f..15f),
    var alphaCharLengthRange: ClosedFloatingPointRange<Float> = (2f..15f),
    var alphaNumericCharLengthRange: ClosedFloatingPointRange<Float> = (2f..15f),
    var startWithLengthRange: ClosedFloatingPointRange<Float> = (2f..15f),
    var containsLengthRange: ClosedFloatingPointRange<Float> = (2f..15f)
)
