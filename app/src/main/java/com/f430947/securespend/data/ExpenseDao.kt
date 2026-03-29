package com.f430947.securespend.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) providing all database operations for Expense records.
 * Async (suspend/Flow) methods are used by the ViewModel; sync methods by the ContentProvider.
 */
@Dao
interface ExpenseDao {

    // --- Async methods used by the ViewModel via coroutines ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense): Long

    @Delete
    suspend fun delete(expense: Expense)

    @Query("SELECT * FROM expenses ORDER BY id DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    // --- Synchronous methods used by the ContentProvider ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSync(expense: Expense): Long

    @Delete
    fun deleteSync(expense: Expense)

    @Query("SELECT * FROM expenses ORDER BY id DESC")
    fun getAllExpensesSync(): List<Expense>

    @Query("SELECT * FROM expenses WHERE id = :id")
    fun getExpenseById(id: Long): Expense?

    @Query("SELECT * FROM expenses WHERE date = :date")
    fun getExpensesByDate(date: String): List<Expense>
}
