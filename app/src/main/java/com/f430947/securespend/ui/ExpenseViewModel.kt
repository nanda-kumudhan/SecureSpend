package com.f430947.securespend.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.f430947.securespend.data.Expense
import com.f430947.securespend.data.ExpenseDatabase
import com.f430947.securespend.data.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    /** SharedPreferences for persisting app settings such as theme. */
    private val prefs = application.getSharedPreferences("secure_spend_prefs", Context.MODE_PRIVATE)

    /** Theme mode: "SYSTEM", "LIGHT", or "DARK". Persisted across app restarts. */
    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    companion object {
        /** Daily spending limit that triggers a budget notification. */
        const val DAILY_SPENDING_LIMIT = 50.0
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        /** Ordered list of all expense categories available throughout the app. */
        val CATEGORIES = listOf("Food", "Transport", "Shopping", "Entertainment", "Bills", "Other")
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

    /** Removes every expense from the database. Called from the Settings screen. */
    fun deleteAllExpenses() {
        viewModelScope.launch { repository.deleteAll() }
    }

    /** Persists the chosen theme mode and updates the UI-observable StateFlow. */
    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _themeMode.value = mode
    }

    /** Returns the sum of all expenses recorded for today. */
    fun getTodayTotal(expenses: List<Expense>): Double {
        val today = LocalDate.now().format(DATE_FORMATTER)
        return expenses.filter { it.date == today }.sumOf { it.amount }
    }

    /**
     * Returns a map of category name to total amount spent in that category,
     * sorted by total descending. Used to display a spending breakdown.
     */
    fun getCategoryTotals(expenses: List<Expense>): Map<String, Double> {
        return expenses
            .groupBy { it.category.ifBlank { "Other" } }
            .mapValues { (_, items) -> items.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
            .associate { it.key to it.value }
    }
}
