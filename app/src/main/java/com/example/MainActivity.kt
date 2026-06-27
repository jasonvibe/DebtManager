package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.Loan
import com.example.ui.screens.AddEditLoanScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoanDetailScreen
import com.example.ui.screens.LoansScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.LoanViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: LoanViewModel by viewModels {
        LoanViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel)
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "仪表盘", Icons.Default.Dashboard)
    object Loans : Screen("loans", "台账", Icons.Default.FormatListBulleted)
    object Settings : Screen("settings", "设置", Icons.Default.Settings)
}

@Composable
fun MainAppScreen(viewModel: LoanViewModel) {
    val navController = rememberNavController()

    val loans by viewModel.loans.collectAsStateWithLifecycle()
    val repaymentPlans by viewModel.repaymentPlans.collectAsStateWithLifecycle()
    val syncRecords by viewModel.syncRecords.collectAsStateWithLifecycle()

    val jianguoUser by viewModel.jianguoUser.collectAsStateWithLifecycle()
    val jianguoPass by viewModel.jianguoPass.collectAsStateWithLifecycle()
    val jianguoUrl by viewModel.jianguoUrl.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Determine if the current route is a top-level tab to show Bottom Bar
    val isTopLevelRoute = currentDestination?.route in listOf(
        Screen.Dashboard.route,
        Screen.Loans.route,
        Screen.Settings.route
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (isTopLevelRoute) {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    val tabs = listOf(Screen.Dashboard, Screen.Loans, Screen.Settings)
                    tabs.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            modifier = Modifier.testTag("nav_tab_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Tab 1: Dashboard
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    loans = loans,
                    repaymentPlans = repaymentPlans,
                    onAddLoanClick = {
                        navController.navigate("add_edit_loan")
                    },
                    onSyncClick = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onLoanClick = { loanId ->
                        navController.navigate("loan_detail/$loanId")
                    }
                )
            }

            // Tab 2: Loans List
            composable(Screen.Loans.route) {
                LoansScreen(
                    loans = loans,
                    repaymentPlans = repaymentPlans,
                    onLoanClick = { loanId ->
                        navController.navigate("loan_detail/$loanId")
                    },
                    onAddLoanClick = {
                        navController.navigate("add_edit_loan")
                    }
                )
            }

            // Tab 3: Settings
            composable(Screen.Settings.route) {
                SettingsScreen(
                    jianguoUser = jianguoUser,
                    jianguoPass = jianguoPass,
                    jianguoUrl = jianguoUrl,
                    lastSyncTime = lastSyncTime,
                    syncStatus = syncStatus,
                    syncRecords = syncRecords,
                    onSaveCredentials = { u, p, url ->
                        viewModel.updateCredentials(u, p, url)
                    },
                    onTestConnection = {
                        viewModel.testConnection { }
                    },
                    onBackupClick = {
                        viewModel.backupToCloud()
                    },
                    onRestoreClick = {
                        viewModel.restoreFromCloud()
                    },
                    onResetStatus = {
                        viewModel.resetSyncStatus()
                    }
                )
            }

            // Sub-Screen: Loan Detail
            composable(
                route = "loan_detail/{loanId}",
                arguments = listOf(navArgument("loanId") { type = NavType.LongType })
            ) { backStackEntry ->
                val loanId = backStackEntry.arguments?.getLong("loanId") ?: 0L
                val currentLoan = loans.find { it.id == loanId }
                val currentPlans = repaymentPlans.filter { it.loanId == loanId }

                LoanDetailScreen(
                    loan = currentLoan,
                    repaymentPlans = currentPlans,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onDeleteClick = {
                        currentLoan?.let { viewModel.deleteLoan(it) }
                        navController.popBackStack()
                    },
                    onEditClick = {
                        navController.navigate("add_edit_loan?loanId=$loanId")
                    },
                    onUpdatePlanStatus = { plan, status, date, amount ->
                        viewModel.updateRepaymentPlanStatus(plan, status, date, amount)
                    }
                )
            }

            // Sub-Screen: Add / Edit Loan
            composable(
                route = "add_edit_loan?loanId={loanId}",
                arguments = listOf(
                    navArgument("loanId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val loanIdStr = backStackEntry.arguments?.getString("loanId")
                val loanId = loanIdStr?.toLongOrNull()
                val currentLoan = loans.find { it.id == loanId }

                AddEditLoanScreen(
                    loan = currentLoan,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onSaveClick = { borrowerName, principal, loanDate, totalPeriods, repaymentMethod, totalInterest, note, loanSource, repaymentDay, monthlyRepaymentAmount ->
                        if (isEditMode(currentLoan)) {
                            // In edit mode we can update the loan fields
                            currentLoan?.let {
                                val updatedLoan = it.copy(
                                    borrowerName = borrowerName,
                                    principal = principal,
                                    loanDate = loanDate,
                                    totalPeriods = totalPeriods,
                                    repaymentMethod = repaymentMethod,
                                    totalInterest = totalInterest,
                                    note = note,
                                    loanSource = loanSource,
                                    repaymentDay = repaymentDay,
                                    monthlyRepaymentAmount = monthlyRepaymentAmount,
                                    updatedAt = System.currentTimeMillis()
                                )
                                viewModel.updateLoanWithRegeneratedPlans(updatedLoan)
                            }
                        } else {
                            viewModel.addLoan(borrowerName, principal, loanDate, totalPeriods, repaymentMethod, totalInterest, note, loanSource, repaymentDay, monthlyRepaymentAmount)
                        }
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

private fun isEditMode(loan: Loan?): Boolean {
    return loan != null
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
