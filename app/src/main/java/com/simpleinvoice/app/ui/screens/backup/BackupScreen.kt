package com.simpleinvoice.app.ui.screens.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import com.simpleinvoice.app.SimpleInvoiceApp
import com.simpleinvoice.app.plan.AdRules
import com.simpleinvoice.app.util.findActivity
import java.text.DateFormat
import java.time.LocalDate
import java.util.Date
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(onBack: () -> Unit, onOpenPlans: () -> Unit) {
    val context = LocalContext.current
    val app = remember { context.applicationContext as SimpleInvoiceApp }
    val manager = app.backupManager
    val settings by manager.settings.collectAsStateWithLifecycle()
    val tier by app.planRepository.tier.collectAsStateWithLifecycle()
    /** A backup/restore waiting for the free user to accept the ad. */
    var pendingAdAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun afterBackupAd(action: () -> Unit) {
        if (AdRules.popupOnBackup(tier)) pendingAdAction = action else action()
    }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var busy by remember { mutableStateOf(false) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }

    fun run(action: suspend () -> Result<*>, success: String) {
        busy = true
        scope.launch {
            val result = action()
            busy = false
            snackbar.showSnackbar(result.fold({ success }, { it.message ?: "Something went wrong" }))
        }
    }

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) manager.setFolder(uri)
    }
    val createBackupFile = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) run({ manager.backupToFile(uri) }, "Backup saved")
    }
    val pickRestoreFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        restoreUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & restore") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Choose a folder for backups. Pick one that a sync app (Nextcloud, Syncthing, " +
                    "FolderSync…) keeps in sync, and your invoices are safely off the phone.",
                style = MaterialTheme.typography.bodyMedium
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Backup folder", style = MaterialTheme.typography.labelLarge)
                    Text(settings.folderName ?: "Not set", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = { pickFolder.launch(null) }) {
                        Icon(Icons.Filled.Folder, contentDescription = null)
                        Text(if (settings.folderUri == null) " Choose folder" else " Change folder")
                    }
                }
            }

            SwitchRow(
                title = "Automatic backup",
                subtitle = "Back up to the folder a few seconds after any change" +
                    if (tier.canAutoBackup) "" else " (Premium Plus)",
                checked = settings.autoBackup && tier.canAutoBackup,
                enabled = settings.folderUri != null,
                onCheckedChange = { enabled ->
                    if (tier.canAutoBackup) {
                        manager.setAutoBackup(enabled)
                    } else {
                        Toast.makeText(context, "Upgrade to get automated backups", Toast.LENGTH_SHORT).show()
                    }
                }
            )
            SwitchRow(
                title = "Also save invoice PDFs",
                subtitle = "Keep a PDF of every invoice in an \"Invoices\" folder next to the backups",
                checked = settings.exportPdfs,
                enabled = settings.folderUri != null,
                onCheckedChange = manager::setExportPdfs
            )
            var keepText by remember(settings.keepCount) { mutableStateOf(settings.keepCount.toString()) }
            OutlinedTextField(
                value = keepText,
                onValueChange = { value ->
                    if (value.length <= 2 && value.all(Char::isDigit)) {
                        keepText = value
                        value.toIntOrNull()?.takeIf { it > 0 }?.let(manager::setKeepCount)
                    }
                },
                label = { Text("Daily backups to keep") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Text(lastBackupText(settings.lastBackupAt), style = MaterialTheme.typography.bodyMedium)
            settings.lastError?.let {
                Text("Last backup failed: $it", color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    afterBackupAd {
                        if (settings.folderUri != null) {
                            run({ manager.backupToFolder() }, "Backed up to ${settings.folderName ?: "folder"}")
                        } else {
                            createBackupFile.launch("SimpleInvoice-backup-${LocalDate.now()}.db")
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Back up now") }

            Divider()
            Text("Restore", style = MaterialTheme.typography.titleMedium)
            Text(
                "Replace everything in the app with a backup file. Your current data is kept " +
                    "in the app as a safety copy first.",
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedButton(
                onClick = { afterBackupAd { pickRestoreFile.launch(arrayOf("*/*")) } },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Restore from backup…") }
        }
    }

    pendingAdAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingAdAction = null },
            title = { Text("Free mode - Ad supported") },
            text = {
                Text(
                    "Backups are free with a short ad.\n\nUpgrade to Premium to back up and restore " +
                        "without ads, or Premium Plus for automatic backups."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingAdAction = null
                    val activity = context.findActivity()
                    if (activity != null) app.adManager.showInterstitial(activity, action) else action()
                }) { Text("Watch ad") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { pendingAdAction = null; onOpenPlans() }) { Text("See plans") }
                    TextButton(onClick = { pendingAdAction = null }) { Text("Cancel") }
                }
            }
        )
    }

    restoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { restoreUri = null },
            title = { Text("Restore this backup?") },
            text = { Text("All current invoices, clients and settings will be replaced. The app will restart.") },
            confirmButton = {
                TextButton(onClick = {
                    restoreUri = null
                    busy = true
                    scope.launch {
                        manager.restore(uri)
                            .onSuccess { manager.restartApp() }
                            .onFailure {
                                busy = false
                                snackbar.showSnackbar(it.message ?: "Restore failed")
                            }
                    }
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { restoreUri = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

private fun lastBackupText(timestamp: Long?): String =
    if (timestamp == null) {
        "No backups yet"
    } else {
        "Last backup: " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp))
    }
