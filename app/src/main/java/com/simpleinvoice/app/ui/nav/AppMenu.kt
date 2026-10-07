package com.simpleinvoice.app.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector

/** Navigates to a route; provided by [SimpleInvoiceNavHost] so any screen can show [AppMenu]. */
val LocalNavigateTo = staticCompositionLocalOf<(String) -> Unit> { {} }

/** The ☰ menu in the top bar of the main screens: every screen, including Plans and Backup. */
@Composable
fun AppMenu() {
    val navigateTo = LocalNavigateTo.current
    var open by remember { mutableStateOf(false) }

    IconButton(onClick = { open = true }) {
        Icon(Icons.Filled.Menu, contentDescription = "Menu")
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        @Composable
        fun item(label: String, icon: ImageVector, route: String) = DropdownMenuItem(
            text = { Text(label) },
            leadingIcon = { Icon(icon, contentDescription = null) },
            onClick = {
                open = false
                navigateTo(route)
            }
        )
        TopLevelDestination.entries.forEach { item(it.label, destinationIcon(it), it.route) }
        Divider()
        item("Plans", Icons.Filled.WorkspacePremium, Routes.PLANS)
        item("Backup & restore", Icons.Filled.Backup, Routes.BACKUP)
    }
}
