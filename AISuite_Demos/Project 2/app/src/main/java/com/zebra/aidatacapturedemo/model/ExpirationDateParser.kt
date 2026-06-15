package com.zebra.aidatacapturedemo.model

import com.zebra.aidatacapturedemo.data.ResultData
import java.util.Calendar
import java.util.regex.Pattern

/**
 * Utility class to extract expiration dates from OCR text results based on product label analysis rules.
 */
object ExpirationDateParser {

    val KEYWORDS = listOf("EXP")
    val LOT_KEYWORDS = listOf("LOT")

    private val FULL_MONTH_NAMES = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    // Expanded date pattern to support spaces and dots
    const val DATE_PATTERN_STR =
        """(\d{1,2}\s*[./\-\s]\s*\d{2,4}|\d{4}\s*[./\-\s]\s*\d{1,2}|(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\.?\s*\d{2,4}|\d{4}\s*-\s*\d{2}\s*-\s*\d{2})"""

    const val LOT_PATTERN_STR = """([A-Z0-9]{4,15})"""

    val KEYWORD_PATTERN_STR = KEYWORDS.joinToString("|") { Pattern.quote(it) }
    val LOT_KEYWORD_PATTERN_STR = LOT_KEYWORDS.joinToString("|") { Pattern.quote(it) }

    enum class DateStatus {
        GREEN, YELLOW, RED, NONE
    }

    private fun getCurrentYearAndMonth(): Pair<Int, Int> {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.YEAR) to (cal.get(Calendar.MONTH) + 1)
    }

    private fun parseDate(ocrText: String): Pair<Int, Int>? {
        FULL_MONTH_NAMES.forEachIndexed { index, monthName ->
            val formattedRegex = Regex("$monthName\\s+(\\d{4})", RegexOption.IGNORE_CASE)
            val formattedMatch = formattedRegex.find(ocrText)
            if (formattedMatch != null) {
                val year = formattedMatch.groups[1]?.value?.toInt() ?: 0
                if (year in 2020..2045) return (index + 1) to year
            }
        }

        // Strip prefixes to evaluate date strings cleanly
        val cleanOcrText = ocrText.replace("The Expiration Date is: ", "", ignoreCase = true)
        val dateRegex = Regex(DATE_PATTERN_STR, RegexOption.IGNORE_CASE)
        val match = dateRegex.find(cleanOcrText) ?: return null
        val dateStr = match.value

        try {
            // 1. Check for YYYY/MM, YYYY.MM, or YYYY-MM structures first
            val yyyyMmRegex = Regex("""(\d{4})\s*[./\-\s]\s*(\d{1,2})""")
            val yyyyMmMatch = yyyyMmRegex.find(dateStr)
            if (yyyyMmMatch != null) {
                val year = yyyyMmMatch.groups[1]?.value?.toInt() ?: 0
                val month = yyyyMmMatch.groups[2]?.value?.toInt() ?: 0
                if (year in 2020..2045 && month in 1..12) return month to year
            }

            // 2. Standard MM/YY or MM/YYYY or MM-YY or MM-YYYY or MM.YY
            val slashRegex = Regex("""(\d{1,2})\s*[./\-\s]\s*(\d{2,4})""")
            val slashMatch = slashRegex.find(dateStr)
            if (slashMatch != null) {
                val month = slashMatch.groups[1]?.value?.toInt() ?: 0
                val yearStr = slashMatch.groups[2]?.value ?: ""
                val year = if (yearStr.length == 2) 2000 + yearStr.toInt() else yearStr.toInt()

                if (year in 2020..2045 && month in 1..12) return month to year
            }

            // 3. MON YYYY or MON YY
            val monRegex = Regex("""(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\.?\s*(\d{2,4})""", RegexOption.IGNORE_CASE)
            val monMatch = monRegex.find(dateStr)
            if (monMatch != null) {
                val months = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
                val month = months.indexOf(monMatch.groups[1]?.value?.uppercase()) + 1
                val yearStr = monMatch.groups[2]?.value ?: ""

                val year = if (yearStr.length == 2) 2000 + yearStr.toInt() else yearStr.toInt()
                if (year in 2020..2045 && month in 1..12) return month to year
            }

            // 4. YYYY-MM-DD
            val isoRegex = Regex("""(\d{4})\s*-\s*(\d{2})\s*-\s*(\d{2})""")
            val isoMatch = isoRegex.find(dateStr)
            if (isoMatch != null) {
                val year = isoMatch.groups[1]?.value?.toInt() ?: 0
                val month = isoMatch.groups[2]?.value?.toInt() ?: 0

                if (year in 2020..2045 && month in 1..12) return month to year
            } else {
                val shortIsoRegex = Regex("""(\d{4})\s*-\s*(\d{2})""")
                val shortIsoMatch = shortIsoRegex.find(dateStr)
                if (shortIsoMatch != null) {
                    val year = shortIsoMatch.groups[1]?.value?.toInt() ?: 0
                    val month = shortIsoMatch.groups[2]?.value?.toInt() ?: 0

                    if (year in 2020..2045 && month in 1..12) return month to year
                }
            }
        } catch (e: Exception) {
            // Ignore parsing errors
        }
        return null
    }

    fun formatWithMonthName(ocrText: String): String {
        val parsed = parseDate(ocrText) ?: return ""
        val (month, year) = parsed
        if (month in 1..12) {
            return "${FULL_MONTH_NAMES[month - 1]} $year"
        }
        return ""
    }

    fun isDateLike(text: String): Boolean {
        val textUpper = text.uppercase()
        // Only consider it a date if it contains one of the expiration keywords
        return KEYWORDS.any { textUpper.contains(it) }
    }

    fun isLotLike(text: String): Boolean {
        val textUpper = text.uppercase()
        return LOT_KEYWORDS.any { textUpper.contains(it) }
    }

    fun extractLotNumber(ocrText: String): String {
        if (ocrText.isBlank()) return ""
        val regex = Regex("(?i)($LOT_KEYWORD_PATTERN_STR)[.:\\s]*$LOT_PATTERN_STR", RegexOption.IGNORE_CASE)
        val match = regex.find(ocrText)
        val value = match?.groups?.get(2)?.value ?: ""
        if (value.isNotEmpty()) return "LOT: $value"
        return ""
    }

    fun extractAllLotNumbersFromResults(results: List<ResultData>): List<String> {
        if (results.isEmpty()) return emptyList()
        val foundLots = mutableSetOf<String>()
        val combinedRegex = Regex("(?i)($LOT_KEYWORD_PATTERN_STR)[.:\\s]*$LOT_PATTERN_STR", RegexOption.IGNORE_CASE)

        for (item in results) {
            val matches = combinedRegex.findAll(item.text)
            for (match in matches) {
                val value = match.groups[2]?.value ?: ""
                if (value.isNotEmpty()) {
                    foundLots.add("LOT: $value")
                }
            }
        }
        return foundLots.toList()
    }

    fun getDateStatus(ocrText: String): DateStatus {
        val parsed = parseDate(ocrText) ?: return DateStatus.NONE
        val (month, year) = parsed
        val (currentYear, currentMonth) = getCurrentYearAndMonth()

        if (year > currentYear) return DateStatus.GREEN
        if (year == currentYear) {
            return when {
                month > currentMonth + 1 -> DateStatus.GREEN
                month >= currentMonth -> DateStatus.YELLOW
                else -> DateStatus.RED
            }
        }
        return DateStatus.RED
    }

    fun getMonthsUntilExpiration(ocrText: String): Int {
        val parsed = parseDate(ocrText) ?: return 0
        val (month, year) = parsed
        val (currentYear, currentMonth) = getCurrentYearAndMonth()

        return (year - currentYear) * 12 + (month - currentMonth)
    }

    fun extractExpirationDate(ocrText: String): String {
        if (ocrText.isBlank()) return "Not found"
        val regex = Regex("(?i)($KEYWORD_PATTERN_STR)[.:\\s]*$DATE_PATTERN_STR", RegexOption.IGNORE_CASE)
        val matches = regex.findAll(ocrText)
        val results = mutableListOf<String>()

        for (match in matches) {
            val date = formatWithMonthName(match.value)
            if (date.isNotEmpty()) {
                results.add("The Expiration Date is: $date")
            }
        }

        if (results.isEmpty()) return "Not found"
        return results.first()
    }

    fun extractAllFormattedFromResults(results: List<ResultData>): List<String> {
        if (results.isEmpty()) return emptyList()
        val foundDates = mutableSetOf<String>()
        val combinedRegex = Regex("(?i)($KEYWORD_PATTERN_STR)[.:\\s]*$DATE_PATTERN_STR", RegexOption.IGNORE_CASE)

        for (item in results) {
            val matches = combinedRegex.findAll(item.text)
            for (match in matches) {
                val cleanDate = formatWithMonthName(match.value)
                if (cleanDate.isNotEmpty()) {
                    foundDates.add("The Expiration Date is: $cleanDate")
                }
            }
        }
        return foundDates.toList()
    }

    fun extractAllFromResults(results: List<ResultData>): List<String> {
        if (results.isEmpty()) return emptyList()
        val foundDates = mutableSetOf<String>()
        // Only look for dates that are preceded by a valid keyword (EXP, BEST BY, etc.)
        val combinedRegex = Regex("(?i)($KEYWORD_PATTERN_STR)[.:\\s]*$DATE_PATTERN_STR", RegexOption.IGNORE_CASE)

        for (item in results) {
            val matchesWithKeyword = combinedRegex.findAll(item.text)
            for (match in matchesWithKeyword) {
                val rawDate = match.value
                val cleanDate = formatWithMonthName(rawDate)
                if (cleanDate.isNotEmpty()) {
                    foundDates.add("The Expiration Date is: $cleanDate")
                }
            }
        }
        return foundDates.toList()
    }
}