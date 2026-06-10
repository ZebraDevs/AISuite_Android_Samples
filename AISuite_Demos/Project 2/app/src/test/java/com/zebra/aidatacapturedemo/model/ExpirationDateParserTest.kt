package com.zebra.aidatacapturedemo.model

import org.junit.Assert.assertEquals
imporat org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class ExpirationDateParserTest {

    @Test
    fun testParseDateVariations() {
        // Test standard formats
        assertEquals("July 2026", ExpirationDateParser.formatWithMonthName("EXP 07/26"))
        assertEquals("July 2026", ExpirationDateParser.formatWithMonthName("BEST BEFORE 07-2026"))
        assertEquals("July 2026", ExpirationDateParser.formatWithMonthName("07.2026"))
        
        // Test formats with spaces (newly supported)
        assertEquals("July 2026", ExpirationDateParser.formatWithMonthName("EXP 07 26"))
        assertEquals("July 2026", ExpirationDateParser.formatWithMonthName("EXP 07 2026"))
        
        // Test month names
        assertEquals("January 2026", ExpirationDateParser.formatWithMonthName("JAN 2026"))
        assertEquals("January 2026", ExpirationDateParser.formatWithMonthName("JAN. 26"))
        assertEquals("February 2027", ExpirationDateParser.formatWithMonthName("FEB 27"))
        
        // Test ISO-like formats
        assertEquals("December 2025", ExpirationDateParser.formatWithMonthName("2025-12-31"))
        assertEquals("December 2025", ExpirationDateParser.formatWithMonthName("2025-12"))
    }

    @Test
    fun testNewKeywords() {
        assertTrue(ExpirationDateParser.formatWithMonthName("BBE 07/26").isNotEmpty())
        assertTrue(ExpirationDateParser.formatWithMonthName("USE BY 07/26").isNotEmpty())
        assertTrue(ExpirationDateParser.formatWithMonthName("UB 07/26").isNotEmpty())
        assertTrue(ExpirationDateParser.formatWithMonthName("ED 07/26").isNotEmpty())
    }

    @Test
    fun testIsDateLike() {
        assertTrue(ExpirationDateParser.isDateLike("EXP"))
        assertTrue(ExpirationDateParser.isDateLike("07/26"))
        assertTrue(ExpirationDateParser.isDateLike("BEST BEFORE"))
        
        // Should be false for common non-date text
        assertFalse(ExpirationDateParser.isDateLike("500ml"))
        assertFalse(ExpirationDateParser.isDateLike("SHAKE WELL"))
        assertFalse(ExpirationDateParser.isDateLike("CONTENTS"))
    }

    @Test
    fun testDateStatus() {
        // Since we now use Calendar.getInstance(), this test is time-dependent.
        // Assuming current date is after June 2024.
        
        // Far future date should be GREEN
        assertEquals(ExpirationDateParser.DateStatus.GREEN, ExpirationDateParser.getDateStatus("EXP 01/2030"))
        
        // Past date should be RED
        assertEquals(ExpirationDateParser.DateStatus.RED, ExpirationDateParser.getDateStatus("EXP 01/2020"))
    }
}
