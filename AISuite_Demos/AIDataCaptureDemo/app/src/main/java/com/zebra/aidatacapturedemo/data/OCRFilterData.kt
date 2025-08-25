package com.zebra.aidatacapturedemo.data

/**
 * Available FilterType options
 */
enum class OCRFilterType {
    SHOW_ALL,
    NUMERIC_CHARACTERS_ONLY,
    ALPHA_CHARACTERS_ONLY,
    ALPHA_NUMERIC_CHARACTERS_ONLY,
    EXACT_MATCH
}
/**
 * Kotlin data class used to communicate OCR Filter detailed information from View to @see OCRReader
 *
 * @property ocrFilterType - Available Filter Type option
 *
 * @property exactMatchString - Filter String used only when OCRFilterType.EXACT_MATCH is requested
 *
 * @property charLengthMin - Filter String filtered Character minimum length.
 * This is applicable for all OCRFilterType except OCRFilterType.SHOW_ALL and OCRFilterType.EXACT_MATCH.
 *
 * @property charLengthMax - Filter String filtered Character maximum length.
 *  This is applicable for all OCRFilterType except OCRFilterType.SHOW_ALL and OCRFilterType.EXACT_MATCH.
 *
 */
data class OCRFilterData(
    val ocrFilterType: OCRFilterType,
    val exactMatchString: String? = null,
    val charLengthMin: Int? = null,
    val charLengthMax: Int? = null
)
