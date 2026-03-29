package com.f430947.securespend.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a single expense record stored in the local database.
 */
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val date: String,       // ISO format: yyyy-MM-dd
    val category: String
)
