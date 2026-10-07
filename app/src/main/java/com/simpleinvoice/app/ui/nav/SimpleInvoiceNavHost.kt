package com.simpleinvoice.app.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.simpleinvoice.app.ui.screens.backup.BackupScreen
import com.simpleinvoice.app.ui.screens.business.BusinessSettingsScreen
import com.simpleinvoice.app.ui.screens.clients.ClientEditScreen
import com.simpleinvoice.app.ui.screens.clients.ClientListScreen
import com.simpleinvoice.app.ui.screens.invoices.InvoiceDetailScreen
import com.simpleinvoice.app.ui.screens.invoices.InvoiceEditScreen
import com.simpleinvoice.app.ui.screens.invoices.InvoiceListScreen
import com.simpleinvoice.app.ui.screens.reports.ReportsScreen

private fun destinationIcon(destination: TopLevelDestination) = when (destination) {
    TopLevelDestination.Invoices -> Icons.Filled.Description
    TopLevelDestination.Clients -> Icons.Filled.People
    TopLevelDestination.Reports -> Icons.Filled.Assessment
    TopLevelDestination.Business -> Icons.Filled.Business
}

@Composable
fun SimpleInvoiceNavHost() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute?.hierarchy?.any { it.route == destination.route } == true,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destinationIcon(destination), contentDescription = destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.INVOICE_LIST,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.INVOICE_LIST) {
                InvoiceListScreen(
                    onAddInvoice = { navController.navigate(Routes.invoiceEdit(0)) },
                    onOpenInvoice = { id -> navController.navigate(Routes.invoiceDetail(id)) }
                )
            }
            composable(
                route = Routes.INVOICE_DETAIL,
                arguments = listOf(navArgument("invoiceId") { type = NavType.LongType })
            ) { backStackEntry ->
                val invoiceId = backStackEntry.arguments?.getLong("invoiceId") ?: 0L
                InvoiceDetailScreen(
                    invoiceId = invoiceId,
                    onEdit = { navController.navigate(Routes.invoiceEdit(invoiceId)) },
                    onBack = { navController.popBackStack() },
                    onDeleted = { navController.popBackStack() }
                )
            }
            composable(
                route = Routes.INVOICE_EDIT,
                arguments = listOf(navArgument("invoiceId") { type = NavType.LongType })
            ) { backStackEntry ->
                val invoiceId = backStackEntry.arguments?.getLong("invoiceId") ?: 0L
                InvoiceEditScreen(
                    invoiceId = invoiceId,
                    onSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() },
                    onAddClient = { navController.navigate(Routes.clientEdit(0)) }
                )
            }
            composable(Routes.CLIENT_LIST) {
                ClientListScreen(
                    onAddClient = { navController.navigate(Routes.clientEdit(0)) },
                    onOpenClient = { id -> navController.navigate(Routes.clientEdit(id)) }
                )
            }
            composable(
                route = Routes.CLIENT_EDIT,
                arguments = listOf(navArgument("clientId") { type = NavType.LongType })
            ) { backStackEntry ->
                val clientId = backStackEntry.arguments?.getLong("clientId") ?: 0L
                ClientEditScreen(
                    clientId = clientId,
                    onSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(Routes.REPORTS) {
                ReportsScreen()
            }
            composable(Routes.BUSINESS_SETTINGS) {
                BusinessSettingsScreen(onOpenBackup = { navController.navigate(Routes.BACKUP) })
            }
            composable(Routes.BACKUP) {
                BackupScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
