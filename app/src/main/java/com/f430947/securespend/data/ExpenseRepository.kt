package com.f430947.securespend.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository providing a clean API over the Room DAO for the ViewModel.
 * Follows the Layered Architecture pattern (L4).
 */
class ExpenseRepository(private val dao: ExpenseDao) {

    val allExpenses: Flow<List<Expense>> = dao.getAllExpenses()

    suspend fun insert(expense: Expense) = dao.insert(expense)

    suspend fun delete(expense: Expense) = dao.delete(expense)

    suspend fun deleteAll() = dao.deleteAll()
}
