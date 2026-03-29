package com.f430947.securespend

import com.f430947.securespend.ui.extractTotalAmount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for the receipt amount-extraction logic in ReceiptScannerScreen.
 *
 * These tests run on the JVM (no device required) because [extractTotalAmount] is a
 * pure function with no Android dependencies.
 */
class ReceiptParserTest {

    @Test
    fun extractTotalAmount_withExplicitTotalLine_returnsCorrectAmount() {
        val text = """
            Coffee      1.50
            Sandwich    3.25
            Total:      4.75
        """.trimIndent()
        val result = extractTotalAmount(text)
        assertNotNull("Expected a detected amount", result)
        assertEquals(4.75, result!!, 0.001)
    }

    @Test
    fun extractTotalAmount_withCurrencySymbol_returnsCorrectAmount() {
        val text = """
            Item A     £2.00
            TOTAL      £9.99
        """.trimIndent()
        val result = extractTotalAmount(text)
        assertNotNull("Expected a detected amount", result)
        assertEquals(9.99, result!!, 0.001)
    }

    @Test
    fun extractTotalAmount_withCommaDecimalSeparator_returnsCorrectAmount() {
        val text = "Grand Total: 12,50"
        val result = extractTotalAmount(text)
        assertNotNull("Expected a detected amount", result)
        assertEquals(12.50, result!!, 0.001)
    }

    @Test
    fun extractTotalAmount_noTotalLine_returnsLargestAmount() {
        val text = """
            Item A    1.00
            Item B    3.50
            Item C    2.00
        """.trimIndent()
        // Falls back to largest amount heuristic
        val result = extractTotalAmount(text)
        assertNotNull("Expected a detected amount", result)
        assertEquals(3.50, result!!, 0.001)
    }

    @Test
    fun extractTotalAmount_noAmountsInText_returnsNull() {
        val text = "Thank you for shopping with us!"
        assertNull(extractTotalAmount(text))
    }
}
