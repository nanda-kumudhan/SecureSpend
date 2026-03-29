package com.f430947.securespend.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.f430947.securespend.data.Expense
import com.f430947.securespend.data.ExpenseDatabase
import com.f430947.securespend.data.ExpenseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * ViewModel that holds UI state and mediates between the Repository and the UI.
 * Extending AndroidViewModel ensures the data survives screen rotation (L6).
 */
class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository

    /**
     * StateFlow of all expenses observed by the UI.
     * The data survives configuration changes because it lives in the ViewModel.
     */
    val allExpenses: StateFlow<List<Expense>>

    companion object {
        /** Daily spending limit that triggers a budget notification. */
        const val DAILY_SPENDING_LIMIT = 50.0
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    init {
        val db = ExpenseDatabase.getDatabase(application)
        repository = ExpenseRepository(db.expenseDao())
        allExpenses = repository.allExpenses
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun insertExpense(expense: Expense) {
        viewModelScope.launch { repository.insert(expense) }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch { repository.delete(expense) }
    }

    /** Returns the sum of all expenses recorded for today. */
    fun getTodayTotal(expenses: List<Expense>): Double {
        val today = LocalDate.now().format(DATE_FORMATTER)
        return expenses.filter { it.date == today }.sumOf { it.amount }
    }
}
