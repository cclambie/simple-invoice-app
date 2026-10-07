package com.simpleinvoice.app.backup

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.room.InvalidationTracker
import com.simpleinvoice.app.data.db.AppDatabase
import com.simpleinvoice.app.data.db.DATABASE_NAME
import com.simpleinvoice.app.data.db.DATABASE_VERSION
import com.simpleinvoice.app.repository.BusinessProfileRepository
import com.simpleinvoice.app.repository.InvoiceRepository
import com.simpleinvoice.app.util.pdfFileName
import com.simpleinvoice.app.util.writeInvoicePdf
import java.io.File
import java.io.OutputStream
import java.time.LocalDate
import kotlin.system.exitProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class BackupSettings(
    /** Folder chosen with the system folder picker (a document tree URI), if any. */
    val folderUri: Uri? = null,
    val folderName: String? = null,
    val autoBackup: Boolean = false,
    /** Also write a PDF of every invoice into an "Invoices" subfolder. */
    val exportPdfs: Boolean = false,
    val keepCount: Int = DEFAULT_KEEP_COUNT,
    val lastBackupAt: Long? = null,
    val lastError: String? = null
) {
    companion object {
        const val DEFAULT_KEEP_COUNT = 7
    }
}

/**
 * Copies the database to a user-chosen folder (which can be synced by Nextcloud, Syncthing,
 * FolderSync, etc.), and restores it from a backup file.
 *
 * Folder backups are one file per day ("SimpleInvoice-backup-2026-10-07.db"), overwritten
 * through the day, keeping the newest [BackupSettings.keepCount].
 */
class BackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val invoiceRepository: InvoiceRepository,
    private val businessProfileRepository: BusinessProfileRepository,
    private val scope: CoroutineScope,
    /** Automatic backup is a paid feature; checked on every change so a downgrade takes effect. */
    private val autoBackupAllowed: () -> Boolean
) {
    private val prefs = context.getSharedPreferences("backup", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<BackupSettings> = _settings.asStateFlow()

    private val backupMutex = Mutex()
    private var pendingAutoBackup: Job? = null

    /** Starts watching the database so changes trigger an automatic backup. */
    fun startWatching() {
        scope.launch(Dispatchers.IO) {
            database.invalidationTracker.addObserver(object : InvalidationTracker.Observer(WATCHED_TABLES) {
                override fun onInvalidated(tables: Set<String>) = scheduleAutoBackup()
            })
        }
    }

    /** Debounces bursts of edits into a single backup a few seconds after the last change. */
    private fun scheduleAutoBackup() {
        val current = _settings.value
        if (!current.autoBackup || current.folderUri == null || !autoBackupAllowed()) return
        pendingAutoBackup?.cancel()
        pendingAutoBackup = scope.launch(Dispatchers.IO) {
            delay(AUTO_BACKUP_DELAY_MS)
            backupToFolder()
        }
    }

    fun setFolder(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        _settings.value.folderUri?.takeIf { it != uri }?.let { old ->
            runCatching { context.contentResolver.releasePersistableUriPermission(old, flags) }
        }
        context.contentResolver.takePersistableUriPermission(uri, flags)
        val name = DocumentFile.fromTreeUri(context, uri)?.name
        prefs.edit().putString(KEY_FOLDER, uri.toString()).putString(KEY_FOLDER_NAME, name).apply()
        refresh()
    }

    fun setAutoBackup(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO, enabled).apply()
        refresh()
        if (enabled) scheduleAutoBackup()
    }

    fun setExportPdfs(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PDFS, enabled).apply()
        refresh()
    }

    fun setKeepCount(count: Int) {
        prefs.edit().putInt(KEY_KEEP, count.coerceIn(1, 99)).apply()
        refresh()
    }

    /** Writes today's backup (and PDFs, if enabled) to the chosen folder and prunes old backups. */
    suspend fun backupToFolder(): Result<String> = withContext(Dispatchers.IO) {
        backupMutex.withLock {
            runCatching {
                val current = _settings.value
                val folder = current.folderUri?.let { DocumentFile.fromTreeUri(context, it) }
                    ?.takeIf { it.canWrite() }
                    ?: error("Backup folder is not available. Choose the folder again.")

                val name = "$FILE_PREFIX${LocalDate.now()}$FILE_SUFFIX"
                val file = folder.findFile(name) ?: folder.createFile("application/octet-stream", name)
                    ?: error("Couldn't create $name in the backup folder.")
                openOutput(file.uri).use(::writeSnapshot)

                folder.listFiles()
                    .filter { it.name.orEmpty().startsWith(FILE_PREFIX) && it.name.orEmpty().endsWith(FILE_SUFFIX) }
                    .sortedByDescending { it.name }
                    .drop(current.keepCount)
                    .forEach { it.delete() }

                if (current.exportPdfs) exportPdfs(folder)
                name
            }.also(::recordResult)
        }
    }

    /** Writes a one-off backup to a file the user picked (e.g. on Google Drive). */
    suspend fun backupToFile(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        backupMutex.withLock {
            runCatching { openOutput(uri).use(::writeSnapshot) }.also(::recordResult)
        }
    }

    /**
     * Replaces all data with the backup at [uri]. A copy of the current data is kept in the app's
     * private storage first. On success the caller should call [restartApp].
     */
    suspend fun restore(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        backupMutex.withLock {
            runCatching {
                val incoming = File(context.cacheDir, "restore.db")
                (context.contentResolver.openInputStream(uri) ?: error("Couldn't open the backup file."))
                    .use { input -> incoming.outputStream().use { input.copyTo(it) } }
                try {
                    validateBackup(incoming)
                    File(context.filesDir, "pre-restore.db").outputStream().use(::writeSnapshot)

                    val dbFile = context.getDatabasePath(DATABASE_NAME)
                    database.close()
                    File(dbFile.path + "-wal").delete()
                    File(dbFile.path + "-shm").delete()
                    incoming.copyTo(dbFile, overwrite = true)
                } finally {
                    incoming.delete()
                }
                Unit
            }
        }
    }

    /** Relaunches the app so the restored database is opened (and migrated if it's older). */
    fun restartApp() {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        context.startActivity(Intent.makeRestartActivityTask(launch.component))
        exitProcess(0)
    }

    private fun validateBackup(file: File) {
        val notABackup = "That file isn't a Simple Invoice backup."
        try {
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                db.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'invoices'", null)
                    .use { require(it.moveToFirst()) { notABackup } }
                require(db.version <= DATABASE_VERSION) {
                    "That backup was made by a newer version of the app. Update the app, then restore."
                }
            }
        } catch (e: SQLiteException) {
            throw IllegalArgumentException(notABackup, e)
        }
    }

    /**
     * Copies the database file while holding a write transaction, so no other write can change
     * it mid-copy. The WAL is checkpointed first so the main file holds all committed data.
     */
    private fun writeSnapshot(out: OutputStream) {
        val db = database.openHelper.writableDatabase
        db.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
        db.beginTransaction()
        try {
            File(db.path ?: error("Database path unknown")).inputStream().use { it.copyTo(out) }
        } finally {
            db.endTransaction()
        }
    }

    private suspend fun exportPdfs(folder: DocumentFile) {
        val dir = folder.findFile(PDF_DIR)?.takeIf { it.isDirectory } ?: folder.createDirectory(PDF_DIR)
            ?: error("Couldn't create the $PDF_DIR folder.")
        val existing = dir.listFiles().associateBy { it.name }
        val business = businessProfileRepository.get()
        val accounts = businessProfileRepository.getPaymentAccounts().associateBy { it.currencyCode }
        invoiceRepository.getAllWithDetails().forEach { details ->
            val name = details.pdfFileName()
            val file = existing[name] ?: dir.createFile("application/pdf", name) ?: return@forEach
            openOutput(file.uri).use { writeInvoicePdf(details, business, accounts[details.currencyCode], it) }
        }
    }

    private fun openOutput(uri: Uri): OutputStream =
        context.contentResolver.openOutputStream(uri, "wt") ?: error("Couldn't write to the backup location.")

    private fun recordResult(result: Result<*>) {
        result.onSuccess {
            prefs.edit().putLong(KEY_LAST, System.currentTimeMillis()).remove(KEY_ERROR).apply()
        }.onFailure {
            prefs.edit().putString(KEY_ERROR, it.message ?: it.javaClass.simpleName).apply()
        }
        refresh()
    }

    private fun refresh() {
        _settings.value = readSettings()
    }

    private fun readSettings() = BackupSettings(
        folderUri = prefs.getString(KEY_FOLDER, null)?.let(Uri::parse),
        folderName = prefs.getString(KEY_FOLDER_NAME, null),
        autoBackup = prefs.getBoolean(KEY_AUTO, false),
        exportPdfs = prefs.getBoolean(KEY_PDFS, false),
        keepCount = prefs.getInt(KEY_KEEP, BackupSettings.DEFAULT_KEEP_COUNT),
        lastBackupAt = prefs.getLong(KEY_LAST, 0L).takeIf { it > 0 },
        lastError = prefs.getString(KEY_ERROR, null)
    )

    private companion object {
        val WATCHED_TABLES = arrayOf(
            "invoices", "invoice_line_items", "clients", "business_profile", "saved_items", "payment_accounts"
        )
        const val AUTO_BACKUP_DELAY_MS = 5_000L
        const val FILE_PREFIX = "SimpleInvoice-backup-"
        const val FILE_SUFFIX = ".db"
        const val PDF_DIR = "Invoices"

        const val KEY_FOLDER = "folder_uri"
        const val KEY_FOLDER_NAME = "folder_name"
        const val KEY_AUTO = "auto_backup"
        const val KEY_PDFS = "export_pdfs"
        const val KEY_KEEP = "keep_count"
        const val KEY_LAST = "last_backup_at"
        const val KEY_ERROR = "last_error"
    }
}
