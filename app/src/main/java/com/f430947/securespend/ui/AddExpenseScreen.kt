package com.f430947.securespend.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.f430947.securespend.data.Expense
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Screen for adding a new expense.
 *
 * All TextField states use rememberSaveable so that user input is preserved across
 * screen rotations, satisfying the lifecycle-aware requirement (L6).
 *
 * A "Scan Receipt" button navigates to ReceiptScannerScreen, which uses ML Kit to OCR
 * a photo and return the detected total via the back-stack SavedStateHandle (L8).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    viewModel: ExpenseViewModel,
    navController: NavController
) {
    // rememberSaveable persists state across configuration changes (e.g. screen rotation)
    var title by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable {
        mutableStateOf(LocalDate.now().format(ExpenseViewModel.DATE_FORMATTER))
    }
    var titleError by rememberSaveable { mutableStateOf(false) }
    var amountError by rememberSaveable { mutableStateOf(false) }
    var dateError by rememberSaveable { mutableStateOf(false) }
    var categoryExpanded by rememberSaveable { mutableStateOf(false) }

    val categories = listOf("Food", "Transport", "Shopping", "Entertainment", "Bills", "Other")

    // Observe the scanned amount returned by ReceiptScannerScreen via SavedStateHandle
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val scannedAmount by savedStateHandle
        ?.getStateFlow<Double?>("scanned_amount", null)
        ?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(scannedAmount) {
        scannedAmount?.let { scanned ->
            amount = String.format(Locale.US, "%.2f", scanned)
            amountError = false
            savedStateHandle?.remove<Double>("scanned_amount")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Expense") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; titleError = false },
                label = { Text("Title") },
                isError = titleError,
                supportingText = if (titleError) ({ Text("Title is required") }) else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it; amountError = false },
                label = { Text("Amount (£)") },
                isError = amountError,
                supportingText = if (amountError) ({ Text("Enter a valid positive amount") }) else null,                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            // Scan Receipt button: navigate to ReceiptScannerScreen to auto-fill the amount
            OutlinedButton(
                onClick = { navController.navigate("receipt_scanner") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Scan Receipt")
            }

            // Category drop-down
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = { category = cat; categoryExpanded = false }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = date,
                onValueChange = { date = it; dateError = false },
                label = { Text("Date (yyyy-MM-dd)") },
                isError = dateError,
                supportingText = if (dateError) ({ Text("Enter a valid date (yyyy-MM-dd)") }) else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = {
                    titleError = title.isBlank()
                    val parsedAmount = amount.toDoubleOrNull()
                    amountError = parsedAmount == null || parsedAmount <= 0
                    dateError = try {
                        LocalDate.parse(date.trim(), ExpenseViewModel.DATE_FORMATTER)
                        false
                    } catch (_: DateTimeParseException) {
                        true
                    }
                    if (!titleError && !amountError && !dateError) {
                        viewModel.insertExpense(
                            Expense(
                                title = title.trim(),
                                amount = parsedAmount!!,
                                date = date.trim(),
                                category = category.ifBlank { "Other" }
                            )
                        )
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Expense")
            }
        }
    }
}
