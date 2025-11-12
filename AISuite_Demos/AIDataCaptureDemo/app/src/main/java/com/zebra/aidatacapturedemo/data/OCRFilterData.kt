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
 * @property exactMatchStringList - Filter String used only when OCRFilterType.EXACT_MATCH is requested
 *
 * @property numericCharLengthRange - Char Length Range value applicable for OCRFilterType.NUMERIC_CHARACTERS_ONLY
 *
 * @property alphaCharLengthRange - Char Length Range value applicable for OCRFilterType.ALPHA_CHARACTERS_ONLY
 *
 * @property alphaNumericCharLengthRange - Char Length Range value applicable for OCRFilterType.ALPHA_NUMERIC_CHARACTERS_ONLY
 *
 */
data class OCRFilterData(
    val ocrFilterType: OCRFilterType,
    val exactMatchStringList: List<String> = listOf(),
    val startsWithString: String = "",
    val containsString: String = "",
    val regexString: String = "",
    var numericCharLengthRange: ClosedFloatingPointRange<Float> = (2f..15f),
    var alphaCharLengthRange: ClosedFloatingPointRange<Float> = (2f..15f),
    var alphaNumericCharLengthRange: ClosedFloatingPointRange<Float> = (2f..15f)
)
