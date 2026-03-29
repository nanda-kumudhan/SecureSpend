package com.f430947.securespend.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.f430947.securespend.data.Expense

/**
 * Dashboard screen: the main screen of the app.
 *
 * Features:
 * - TopAppBar with a Share button (ShareSheet via Intent.ACTION_SEND)
 * - Spending summary card with today's total and all-time total
 * - Category filter chips to narrow the expense list
 * - FloatingActionButton to navigate to the Add Expense screen
 * - LazyColumn for performant list rendering (L2 best practice)
 * - Adaptive two-pane layout on tablets (screen width > 600 dp) (L5)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ExpenseViewModel,
    navController: NavController
) {
    val expenses by viewModel.allExpenses.collectAsState()
    val context = LocalContext.current
    val isWideScreen = LocalConfiguration.current.screenWidthDp > 600

    var selectedExpense by remember { mutableStateOf<Expense?>(null) }
    // Persisted across rotation: which category filter chip is active ("All" = no filter)
    var activeCategory by rememberSaveable { mutableStateOf("All") }

    val todayTotal = viewModel.getTodayTotal(expenses)
    val allTimeTotal = expenses.sumOf { it.amount }

    // Categories that actually appear in the current expense list (plus "All")
    val availableCategories = listOf("All") +
            expenses.map { it.category.ifBlank { "Other" } }.distinct().sorted()

    // Filter expenses by selected category
    val filteredExpenses = if (activeCategory == "All") expenses
    else expenses.filter { it.category.ifBlank { "Other" } == activeCategory }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SecureSpend") },
                actions = {
                    IconButton(onClick = { shareReport(context, expenses, todayTotal) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share Report")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_expense") }) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { paddingValues ->
        if (isWideScreen) {
            // Adaptive layout: two-pane list/detail for tablets (L5)
            Row(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                Column(modifier = Modifier.weight(1f)) {
                    SpendingSummaryCard(todayTotal = todayTotal, allTimeTotal = allTimeTotal)
                    CategoryFilterRow(
                        categories = availableCategories,
                        activeCategory = activeCategory,
                        onCategorySelected = { activeCategory = it }
                    )
                    ExpenseList(
                        expenses = filteredExpenses,
                        onExpenseClick = { selectedExpense = it },
                        onDeleteClick = { viewModel.deleteExpense(it) }
                    )
                }
                VerticalDivider(modifier = Modifier.fillMaxHeight().width(1.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(16.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    if (selectedExpense != null) {
                        ExpenseDetailPanel(expense = selectedExpense!!)
                    } else {
                        Text(
                            "Select an expense to see details",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        } else {
            // Phone layout: single-pane with summary + filter + list
            Column(modifier = Modifier.padding(paddingValues)) {
                SpendingSummaryCard(todayTotal = todayTotal, allTimeTotal = allTimeTotal)
                CategoryFilterRow(
                    categories = availableCategories,
                    activeCategory = activeCategory,
                    onCategorySelected = { activeCategory = it }
                )
                ExpenseList(
                    expenses = filteredExpenses,
                    onExpenseClick = { selectedExpense = it },
                    onDeleteClick = { viewModel.deleteExpense(it) }
                )
            }
        }
    }
}

/** Summary card showing today's spending and the all-time total. */
@Composable
fun SpendingSummaryCard(todayTotal: Double, allTimeTotal: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Today", style = MaterialTheme.typography.labelMedium)
                Text(
                    "£%.2f".format(todayTotal),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("All-time", style = MaterialTheme.typography.labelMedium)
                Text(
                    "£%.2f".format(allTimeTotal),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/** Horizontally scrollable row of [FilterChip]s for each expense category. */
@Composable
fun CategoryFilterRow(
    categories: List<String>,
    activeCategory: String,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { cat ->
            FilterChip(
                selected = cat == activeCategory,
                onClick = { onCategorySelected(cat) },
                label = { Text(cat) }
            )
        }
    }
}

@Composable
fun ExpenseList(
    expenses: List<Expense>,
    onExpenseClick: (Expense) -> Unit,
    onDeleteClick: (Expense) -> Unit,
    modifier: Modifier = Modifier
) {
    if (expenses.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No expenses yet. Tap + to add one.", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(expenses, key = { it.id }) { expense ->
                ExpenseCard(
                    expense = expense,
                    onClick = { onExpenseClick(expense) },
                    onDeleteClick = { onDeleteClick(expense) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseCard(
    expense: Expense,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${expense.category} • ${expense.date}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = "£%.2f".format(expense.amount),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Delete expense")
            }
        }
    }
}

@Composable
fun ExpenseDetailPanel(expense: Expense) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Expense Detail",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        HorizontalDivider()
        DetailRow("Title", expense.title)
        DetailRow("Amount", "£%.2f".format(expense.amount))
        DetailRow("Category", expense.category)
        DetailRow("Date", expense.date)
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(80.dp)
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Launches the system ShareSheet to share a plain-text budget report. */
private fun shareReport(context: Context, expenses: List<Expense>, todayTotal: Double) {
    val summary = buildString {
        appendLine("SecureSpend Budget Report")
        appendLine("========================")
        appendLine("Today's spending: £%.2f".format(todayTotal))
        appendLine()
        appendLine("Recent Expenses:")
        expenses.take(10).forEach { expense ->
            appendLine(
                "• ${expense.title}: £%.2f (${expense.category}, ${expense.date})".format(expense.amount)
            )
        }
        appendLine()
        appendLine("Total all-time: £%.2f".format(expenses.sumOf { it.amount }))
    }
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "SecureSpend Budget Report")
        putExtra(Intent.EXTRA_TEXT, summary)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Budget Report"))
}

