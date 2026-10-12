package com.craiglambie.invoicemaker

import android.app.Application
import com.craiglambie.invoicemaker.ads.AdManager
import com.craiglambie.invoicemaker.backup.BackupManager
import com.craiglambie.invoicemaker.plan.PlanRepository
import com.craiglambie.invoicemaker.data.db.AppDatabase
import com.craiglambie.invoicemaker.repository.BusinessProfileRepository
import com.craiglambie.invoicemaker.repository.ClientRepository
import com.craiglambie.invoicemaker.repository.InvoiceRepository
import com.craiglambie.invoicemaker.repository.SavedItemRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class SimpleInvoiceApp : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var businessProfileRepository: BusinessProfileRepository
        private set
    lateinit var clientRepository: ClientRepository
        private set
    lateinit var invoiceRepository: InvoiceRepository
        private set
    lateinit var savedItemRepository: SavedItemRepository
        private set
    lateinit var backupManager: BackupManager
        private set
    lateinit var planRepository: PlanRepository
        private set
    lateinit var adManager: AdManager
        private set

    /** Lives as long as the process; used for work that must outlive any screen, like backups. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.get(this)
        businessProfileRepository = BusinessProfileRepository(database.businessProfileDao(), database.paymentAccountDao())
        clientRepository = ClientRepository(database.clientDao())
        invoiceRepository = InvoiceRepository(database, database.invoiceDao(), database.invoiceLineItemDao())
        savedItemRepository = SavedItemRepository(database.savedItemDao())
        planRepository = PlanRepository(this, invoiceRepository)
        backupManager = BackupManager(
            this, database, invoiceRepository, businessProfileRepository, appScope,
            autoBackupAllowed = { planRepository.tier.value.canAutoBackup }
        )
        adManager = AdManager(this, appScope)
        adManager.initialize()
        backupManager.startWatching()
    }
}
