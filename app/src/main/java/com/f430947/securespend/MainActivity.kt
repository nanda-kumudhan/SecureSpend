package com.f430947.securespend

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.f430947.securespend.notifications.NotificationHelper
import com.f430947.securespend.ui.AddExpenseScreen
import com.f430947.securespend.ui.BankStatementScreen
import com.f430947.securespend.ui.DashboardScreen
import com.f430947.securespend.ui.ExpenseViewModel
import com.f430947.securespend.ui.ReceiptScannerScreen
import com.f430947.securespend.ui.SettingsScreen
import com.f430947.securespend.ui.theme.SecureSpendTheme

/**
 * Entry point of the app. Sets up:
 * - Bottom NavigationBar with Dashboard, Bank Statement, and Settings destinations
 * - Navigation Component with NavHost (L3)
 * - Notification channel and runtime POST_NOTIFICATIONS permission (L4, L5)
 * - Budget-exceeded notification trigger (L5)
 *
 * The ViewModel is created here and survives configuration changes (e.g. screen rotation), so
 * expense data is never lost when the user rotates the device (L6).
 */
class MainActivity : ComponentActivity() {

    // viewModels() delegate ensures the ViewModel survives screen rotation (L6)
    private val viewModel: ExpenseViewModel by viewModels()

    // Runtime permission launcher for POST_NOTIFICATIONS (Android 13+)
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled silently; app degrades gracefully without it */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register notification channel (required for API 26+)
        NotificationHelper.createNotificationChannel(this)

        // Request POST_NOTIFICATIONS permission only when needed (L4: responsible permissions)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()

            SecureSpendTheme(themeMode = themeMode) {
                val navController = rememberNavController()
                val expenses by viewModel.allExpenses.collectAsState()
                val currentRoute by navController.currentBackStackEntryAsState()

                // Trigger a notification whenever the daily total exceeds the spending limit
                LaunchedEffect(expenses) {
                    val todayTotal = viewModel.getTodayTotal(expenses)
                    if (todayTotal > ExpenseViewModel.DAILY_SPENDING_LIMIT) {
                        NotificationHelper.sendBudgetLimitNotification(
                            this@MainActivity, todayTotal, ExpenseViewModel.DAILY_SPENDING_LIMIT
                        )
                    }
                }

                // Bottom nav is only visible on the three main destination screens
                val bottomNavRoutes = setOf("dashboard", "bank_statement", "settings")
                val showBottomBar = currentRoute?.destination?.route in bottomNavRoutes

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = currentRoute?.destination?.route == "dashboard",
                                    onClick = {
                                        navController.navigate("dashboard") {
                                            popUpTo("dashboard") { inclusive = true }
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                                    label = { Text("Dashboard") }
                                )
                                NavigationBarItem(
                                    selected = currentRoute?.destination?.route == "bank_statement",
                                    onClick = {
                                        navController.navigate("bank_statement") {
                                            popUpTo("dashboard") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.AccountBalance, contentDescription = "Bank Statement") },
                                    label = { Text("Bank") }
                                )
                                NavigationBarItem(
                                    selected = currentRoute?.destination?.route == "settings",
                                    onClick = {
                                        navController.navigate("settings") {
                                            popUpTo("dashboard") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                    label = { Text("Settings") }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    // NavHost handles navigation between screens (L3)
                    NavHost(
                        navController = navController,
                        startDestination = "dashboard",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("dashboard") {
                            DashboardScreen(viewModel = viewModel, navController = navController)
                        }
                        composable("add_expense") {
                            AddExpenseScreen(viewModel = viewModel, navController = navController)
                        }
                        composable("receipt_scanner") {
                            ReceiptScannerScreen(navController = navController)
                        }
                        composable("bank_statement") {
                            BankStatementScreen(viewModel = viewModel)
                        }
                        composable("settings") {
                            SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
