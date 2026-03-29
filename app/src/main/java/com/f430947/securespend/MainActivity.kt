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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.f430947.securespend.notifications.NotificationHelper
import com.f430947.securespend.ui.AddExpenseScreen
import com.f430947.securespend.ui.DashboardScreen
import com.f430947.securespend.ui.ExpenseViewModel
import com.f430947.securespend.ui.theme.SecureSpendTheme

/**
 * Entry point of the app. Sets up:
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
            SecureSpendTheme {
                val navController = rememberNavController()
                val expenses by viewModel.allExpenses.collectAsState()

                // Trigger a notification whenever the daily total exceeds the spending limit
                LaunchedEffect(expenses) {
                    val todayTotal = viewModel.getTodayTotal(expenses)
                    if (todayTotal > ExpenseViewModel.DAILY_SPENDING_LIMIT) {
                        NotificationHelper.sendBudgetLimitNotification(
                            this@MainActivity, todayTotal, ExpenseViewModel.DAILY_SPENDING_LIMIT
                        )
                    }
                }

                // NavHost handles navigation between screens (L3)
                NavHost(navController = navController, startDestination = "dashboard") {
                    composable("dashboard") {
                        DashboardScreen(viewModel = viewModel, navController = navController)
                    }
                    composable("add_expense") {
                        AddExpenseScreen(viewModel = viewModel, navController = navController)
                    }
                }
            }
        }
    }
}
