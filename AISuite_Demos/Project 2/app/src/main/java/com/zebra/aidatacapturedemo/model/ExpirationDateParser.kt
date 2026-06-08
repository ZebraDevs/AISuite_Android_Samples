package com.zebra.aidatacapturedemo.model

import com.zebra.aidatacapturedemo.data.ResultData
import java.util.regex.Pattern

/**
 * Utility class to extract expiration dates from OCR text results based on product label analysis rules.
 */
object ExpirationDateParser {

    val KEYWORDS = listOf(
        "EXP", "EXPIRY", "EXPIRES", "EXPIRATION DATE",
        "BEST BEFORE", "BEST BY", "BB",
        "MA", "MFG"
    )

    private val EXP_KEYWORDS = listOf("EXP", "EXPIRY", "EXPIRES", "EXPIRATION DATE")

    // Regex for various date formats:
    // 1. MM/YY or MM/YYYY (e.g. 12/26, 12/2026) - allows optional spaces around separator
    // 2. MM-YY or MM-YYYY (e.g. 12-26, 12-2026)
    // 3. MON YYYY (e.g. JAN 2026)
    // 4. YYYY-MM-DD (e.g. 2026-12-31)
    private const val DATE_PATTERN_STR = 
        """(\d{1,2}\s*[/-]\s*\d{2,4}|(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\s+\d{4}|\d{4}\s*-\s*\d{2}\s*-\s*\d{2})"""

    private val KEYWORD_PATTERN_STR = KEYWORDS.joinToString("|") { Pattern.quote(it) }

    enum class DateStatus {
        GREEN, RED, NONE
    }

    /**
     * Determines if the date in the text is >= 06/2026 (Green) or < 06/2026 (Red).
     */
    fun getDateStatus(ocrText: String): DateStatus {
        val dateRegex = Regex(DATE_PATTERN_STR, RegexOption.IGNORE_CASE)
        val match = dateRegex.find(ocrText) ?: return DateStatus.NONE
        val dateStr = match.value

        try {
            var month = 0
            var year = 0

            // 1. MM/YY or MM/YYYY or MM-YY or MM-YYYY
            val slashRegex = Regex("""(\d{1,2})\s*[/-]\s*(\d{2,4})""")
            val slashMatch = slashRegex.find(dateStr)
            if (slashMatch != null) {
                month = slashMatch.groups[1]?.value?.toInt() ?: 0
                val yearStr = slashMatch.groups[2]?.value ?: ""
                year = if (yearStr.length == 2) 2000 + yearStr.toInt() else yearStr.toInt()
            } else {
                // 2. MON YYYY
                val monRegex = Regex("""(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\s+(\d{4})""", RegexOption.IGNORE_CASE)
                val monMatch = monRegex.find(dateStr)
                if (monMatch != null) {
                    val months = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
                    month = months.indexOf(monMatch.groups[1]?.value?.uppercase()) + 1
                    year = monMatch.groups[2]?.value?.toInt() ?: 0
                } else {
                    // 3. YYYY-MM-DD
                    val isoRegex = Regex("""(\d{4})\s*-\s*(\d{2})\s*-\s*(\d{2})""")
                    val isoMatch = isoRegex.find(dateStr)
                    if (isoMatch != null) {
                        year = isoMatch.groups[1]?.value?.toInt() ?: 0
                        month = isoMatch.groups[2]?.value?.toInt() ?: 0
                    }
                }
            }

            if (year > 2026) return DateStatus.GREEN
            if (year == 2026 && month >= 6) return DateStatus.GREEN
            if (year > 0) return DateStatus.RED

        } catch (e: Exception) {
            // Ignore parsing errors
        }

        return DateStatus.NONE
    }

    /**
     * Extracts the expiration date from the given OCR text.
     * 
     * @param ocrText The full text obtained from OCR.
     * @return The extracted date string, or "Not found" if no matching date is found.
     */
    fun extractExpirationDate(ocrText: String): String {
        if (ocrText.isBlank()) return "Not found"

        // Pattern to find keyword followed by optional separator (., :, or space) and then the date
        val regex = Regex("(?i)($KEYWORD_PATTERN_STR)[.:\\s]*$DATE_PATTERN_STR", RegexOption.IGNORE_CASE)

        val matches = regex.findAll(ocrText)
        val results = mutableListOf<Pair<String, String>>()

        for (match in matches) {
            val keyword = match.groups[1]?.value?.uppercase() ?: ""
            val date = match.groups[2]?.value ?: ""
            if (date.isNotEmpty()) {
                results.add(keyword to date)
            }
        }

        if (results.isEmpty()) return "Not found"

        // "If multiple dates are found, return the one labeled as expiration specifically"
        val expResult = results.find { (kw, _) -> 
            EXP_KEYWORDS.any { kw.contains(it) } 
        }

        val finalDate = expResult?.second ?: results.first().second
        return "The expiration date is $finalDate"
    }

    /**
     * Extracts expiration date from a list of OCR results, handling cases where the keyword
     * and date might be in separate ResultData items.
     */
    fun extractFromResults(results: List<ResultData>): String {
        if (results.isEmpty()) return "Not found"

        val foundDates = mutableListOf<Pair<String, String>>()
        val keywordRegex = Regex("(?i)($KEYWORD_PATTERN_STR)", RegexOption.IGNORE_CASE)
        val dateRegex = Regex(DATE_PATTERN_STR, RegexOption.IGNORE_CASE)
        val combinedRegex = Regex("(?i)($KEYWORD_PATTERN_STR)[.:\\s]*$DATE_PATTERN_STR", RegexOption.IGNORE_CASE)

        for (i in results.indices) {
            val text = results[i].text.trim()
            
            // Case 1: Keyword and Date in the same result item
            val match = combinedRegex.find(text)
            if (match != null) {
                foundDates.add(match.groups[1]!!.value.uppercase() to match.groups[2]!!.value)
                continue
            }
            
            // Case 2: Keyword in this result, Date in the next result (sequential)
            val keywordMatch = keywordRegex.find(text)
            if (keywordMatch != null && i + 1 < results.size) {
                val nextText = results[i+1].text.trim()
                val dateMatch = dateRegex.find(nextText)
                if (dateMatch != null) {
                    foundDates.add(keywordMatch.value.uppercase() to dateMatch.value)
                }
            }
        }

        if (foundDates.isEmpty()) {
            val result = extractExpirationDate(results.joinToString(" ") { it.text })
            return if (result == "Not found") "Not found" else result
        }

        // Filter for EXP keywords specifically if needed, but the current logic handles priority
        val expResult = foundDates.find { (kw, _) -> 
            kw.contains("EXP") 
        }

        val finalDate = expResult?.second ?: foundDates.first().second
        return "The expiration date is $finalDate"
    }
}
