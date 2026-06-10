package com.zebra.aidatacapturedemo.model

import com.zebra.aidatacapturedemo.data.ResultData
import java.util.regex.Pattern

/**
 * Utility class to extract expiration dates from OCR text results based on product label analysis rules.
 */
object ExpirationDateParser {

    private val KEYWORDS = listOf(
        "EXP", "EXPIRY", "EXPIRES", "EXPIRATION DATE",
        "BEST BEFORE", "BEST BY", "BB",
        "MA", "MFG"
    )

    private val EXP_KEYWORDS = listOf("EXP", "EXPIRY", "EXPIRES", "EXPIRATION DATE")

    // Regex for various date formats as requested:
    // 1. MM/YY or MM/YYYY (e.g. 12/26, 12/2026)
    // 2. MM-YY or MM-YYYY (e.g. 12-26, 12-2026)
    // 3. MON YYYY (e.g. JAN 2026)
    // 4. YYYY-MM-DD (e.g. 2026-12-31)
    private const val DATE_PATTERN_STR = 
        """(\b\d{1,2}[/-]\d{2,4}\b|\b(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\s+\d{4}\b|\b\d{4}-\d{2}-\d{2}\b)"""

    private val KEYWORD_PATTERN_STR = KEYWORDS.joinToString("|") { Pattern.quote(it) }

    /**
     * Extracts the expiration date from the given OCR text.
     * 
     * @param ocrText The full text obtained from OCR.
     * @return The extracted date string, or "Not found" if no matching date is found.
     */
    fun extractExpirationDate(ocrText: String): String {
        if (ocrText.isBlank()) return "Not found"

        // Pattern to find keyword followed by optional separator (: or space) and then the date
        val regex = Regex("(?i)($KEYWORD_PATTERN_STR)[:\\s]*$DATE_PATTERN_STR", RegexOption.IGNORE_CASE)

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

        return expResult?.second ?: results.first().second
    }

    /**
     * Extracts expiration date from a list of OCR results, handling cases where the keyword
     * and date might be in separate ResultData items.
     *
     * @param results List of ResultData objects from OCR analysis.
     * @return The extracted date string, or "Not found".
     */
    fun extractFromResults(results: List<ResultData>): String {
        if (results.isEmpty()) return "Not found"

        val foundDates = mutableListOf<Pair<String, String>>()
        val keywordRegex = Regex("(?i)($KEYWORD_PATTERN_STR)", RegexOption.IGNORE_CASE)
        val dateRegex = Regex(DATE_PATTERN_STR, RegexOption.IGNORE_CASE)
        val combinedRegex = Regex("(?i)($KEYWORD_PATTERN_STR)[:\\s]*$DATE_PATTERN_STR", RegexOption.IGNORE_CASE)

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
            // Last resort: search in the joined string
            return extractExpirationDate(results.joinToString(" ") { it.text })
        }

        val expResult = foundDates.find { (kw, _) -> 
            EXP_KEYWORDS.any { kw.contains(it) } 
        }

        return expResult?.second ?: foundDates.first().second
    }
}
