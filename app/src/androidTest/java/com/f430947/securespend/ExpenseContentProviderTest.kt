package com.f430947.securespend

import android.content.ContentValues
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.f430947.securespend.data.ExpenseContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for the ExpenseContentProvider (L7).
 *
 * Uses InstrumentationRegistry.getInstrumentation().targetContext.contentResolver to insert a
 * fake expense via the ContentProvider and then queries it back, verifying that the Contract
 * constants and Provider implementation work correctly together.
 */
@RunWith(AndroidJUnit4::class)
class ExpenseContentProviderTest {

    private val contentResolver =
        InstrumentationRegistry.getInstrumentation().targetContext.contentResolver

    @Test
    fun insertAndQueryExpense_returnsInsertedRecord() {
        // Arrange: build expense values using the Contract constants
        val values = ContentValues().apply {
            put(ExpenseContract.Expenses.COLUMN_TITLE, "Instrumented Test Expense")
            put(ExpenseContract.Expenses.COLUMN_AMOUNT, 9.99)
            put(ExpenseContract.Expenses.COLUMN_DATE, "2024-06-01")
            put(ExpenseContract.Expenses.COLUMN_CATEGORY, "Food")
        }

        // Act: insert via the ContentProvider
        val insertedUri = contentResolver.insert(ExpenseContract.Expenses.CONTENT_URI, values)
        assertNotNull("Inserted URI must not be null", insertedUri)

        // Act: query all expenses back
        val cursor = contentResolver.query(
            ExpenseContract.Expenses.CONTENT_URI,
            null,
            null,
            null,
            null
        )
        assertNotNull("Query cursor must not be null", cursor)

        cursor!!.use {
            assertTrue("Result set must contain at least one row", it.moveToFirst())

            val titleIdx = it.getColumnIndex(ExpenseContract.Expenses.COLUMN_TITLE)
            val amountIdx = it.getColumnIndex(ExpenseContract.Expenses.COLUMN_AMOUNT)
            val categoryIdx = it.getColumnIndex(ExpenseContract.Expenses.COLUMN_CATEGORY)

            // Find the row we just inserted
            var found = false
            do {
                if (it.getString(titleIdx) == "Instrumented Test Expense") {
                    found = true
                    assertEquals(9.99, it.getDouble(amountIdx), 0.01)
                    assertEquals("Food", it.getString(categoryIdx))
                    break
                }
            } while (it.moveToNext())

            assertTrue("Inserted expense must be retrievable via ContentProvider query", found)
        }
    }

    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.f430947.securespend", appContext.packageName)
    }
}
