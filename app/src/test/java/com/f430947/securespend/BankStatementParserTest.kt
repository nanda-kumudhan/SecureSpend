package com.f430947.securespend

import com.f430947.securespend.ui.parseTransactions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the bank-statement transaction parsing logic in BankStatementScreen.
 *
 * These tests run on the JVM (no device required) because [parseTransactions] is a
 * pure function with no Android dependencies.
 */
class BankStatementParserTest {

    @Test
    fun parseTransactions_slashDateAndAmount_returnsTransaction() {
        val text = "01/01/2024  AMAZON PRIME        £9.99"
        val results = parseTransactions(text)
        assertEquals(1, results.size)
        assertEquals("2024-01-01", results[0].date)
        assertEquals(9.99, results[0].amount, 0.001)
    }

    @Test
    fun parseTransactions_isoDateFormat_returnsTransaction() {
        val text = "2024-03-15  Supermarket        45.00"
        val results = parseTransactions(text)
        assertEquals(1, results.size)
        assertEquals("2024-03-15", results[0].date)
        assertEquals(45.00, results[0].amount, 0.001)
    }

    @Test
    fun parseTransactions_monthNameDate_returnsTransaction() {
        val text = "5 Feb 2024   Netflix Subscription   £14.99"
        val results = parseTransactions(text)
        assertEquals(1, results.size)
        assertEquals("2024-02-05", results[0].date)
        assertEquals(14.99, results[0].amount, 0.001)
    }

    @Test
    fun parseTransactions_twoYearDate_returnsTransaction() {
        val text = "12/06/24  Coffee Shop   2.80"
        val results = parseTransactions(text)
        assertEquals(1, results.size)
        assertEquals("2024-06-12", results[0].date)
        assertEquals(2.80, results[0].amount, 0.001)
    }

    @Test
    fun parseTransactions_multipleLines_returnsMultipleTransactions() {
        val text = """
            01/01/2024  Amazon         £9.99
            02/01/2024  Tesco          £34.50
            03/01/2024  Netflix        £15.99
        """.trimIndent()
        val results = parseTransactions(text)
        assertEquals(3, results.size)
    }

    @Test
    fun parseTransactions_noDateLines_returnsEmpty() {
        val text = "No dates here, just some text and maybe 1.99"
        val results = parseTransactions(text)
        assertTrue(results.isEmpty())
    }

    @Test
    fun parseTransactions_headerLinesSkipped_notIncludedInResults() {
        val text = """
            Account Statement
            Opening Balance: 1000.00
            01/02/2024  Coffee   2.50
        """.trimIndent()
        // Only the transaction line should be returned
        val results = parseTransactions(text)
        assertEquals(1, results.size)
        assertEquals(2.50, results[0].amount, 0.001)
    }
}
