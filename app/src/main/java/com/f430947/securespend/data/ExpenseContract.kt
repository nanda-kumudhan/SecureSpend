package com.f430947.securespend.data

import android.net.Uri

/**
 * Data Contract that defines the public API of the ExpenseContentProvider.
 * External apps (signed with the same key) use these constants to interact with expense data.
 */
object ExpenseContract {

    const val AUTHORITY = "com.f430947.securespend.provider"
    val BASE_CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY")

    object Expenses {
        const val TABLE_NAME = "expenses"
        val CONTENT_URI: Uri = BASE_CONTENT_URI.buildUpon().appendPath(TABLE_NAME).build()

        const val COLUMN_ID = "id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_AMOUNT = "amount"
        const val COLUMN_DATE = "date"
        const val COLUMN_CATEGORY = "category"
    }
}
