package com.f430947.securespend.data

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

/**
 * ContentProvider that exposes expense data to other authorised applications.
 *
 * Security: access is restricted by the custom "signature" permission
 * (com.f430947.securespend.EXPENSE_DATA) declared in AndroidManifest.xml, ensuring only
 * applications signed by the same developer key can read or write expense data (L7).
 */
class ExpenseContentProvider : ContentProvider() {

    private lateinit var db: ExpenseDatabase

    companion object {
        private const val EXPENSES = 1
        private const val EXPENSE_ID = 2

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(ExpenseContract.AUTHORITY, ExpenseContract.Expenses.TABLE_NAME, EXPENSES)
            addURI(
                ExpenseContract.AUTHORITY,
                "${ExpenseContract.Expenses.TABLE_NAME}/#",
                EXPENSE_ID
            )
        }
    }

    override fun onCreate(): Boolean {
        db = ExpenseDatabase.getDatabase(context!!)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor {
        val columns = arrayOf(
            ExpenseContract.Expenses.COLUMN_ID,
            ExpenseContract.Expenses.COLUMN_TITLE,
            ExpenseContract.Expenses.COLUMN_AMOUNT,
            ExpenseContract.Expenses.COLUMN_DATE,
            ExpenseContract.Expenses.COLUMN_CATEGORY
        )
        val cursor = MatrixCursor(columns)
        val dao = db.expenseDao()

        when (uriMatcher.match(uri)) {
            EXPENSES -> dao.getAllExpensesSync().forEach { expense ->
                cursor.addRow(
                    arrayOf(expense.id, expense.title, expense.amount, expense.date, expense.category)
                )
            }
            EXPENSE_ID -> {
                val id = ContentUris.parseId(uri)
                dao.getExpenseById(id)?.let { expense ->
                    cursor.addRow(
                        arrayOf(expense.id, expense.title, expense.amount, expense.date, expense.category)
                    )
                }
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }

        cursor.setNotificationUri(context!!.contentResolver, uri)
        return cursor
    }

    override fun getType(uri: Uri): String = when (uriMatcher.match(uri)) {
        EXPENSES -> "vnd.android.cursor.dir/${ExpenseContract.AUTHORITY}.${ExpenseContract.Expenses.TABLE_NAME}"
        EXPENSE_ID -> "vnd.android.cursor.item/${ExpenseContract.AUTHORITY}.${ExpenseContract.Expenses.TABLE_NAME}"
        else -> throw IllegalArgumentException("Unknown URI: $uri")
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (uriMatcher.match(uri) != EXPENSES) {
            throw IllegalArgumentException("Invalid URI for insert: $uri")
        }
        val expense = Expense(
            title = values?.getAsString(ExpenseContract.Expenses.COLUMN_TITLE) ?: "",
            amount = values?.getAsDouble(ExpenseContract.Expenses.COLUMN_AMOUNT) ?: 0.0,
            date = values?.getAsString(ExpenseContract.Expenses.COLUMN_DATE) ?: "",
            category = values?.getAsString(ExpenseContract.Expenses.COLUMN_CATEGORY) ?: ""
        )
        val id = db.expenseDao().insertSync(expense)
        context!!.contentResolver.notifyChange(uri, null)
        return ContentUris.withAppendedId(uri, id)
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        if (uriMatcher.match(uri) != EXPENSE_ID) {
            throw IllegalArgumentException("Invalid URI for delete: $uri")
        }
        val id = ContentUris.parseId(uri)
        val expense = db.expenseDao().getExpenseById(id) ?: return 0
        db.expenseDao().deleteSync(expense)
        context!!.contentResolver.notifyChange(uri, null)
        return 1
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int = 0 // Update not implemented for this version
}
