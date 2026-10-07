package com.simpleinvoice.app.repository

import com.simpleinvoice.app.data.db.BusinessProfileDao
import com.simpleinvoice.app.data.db.PaymentAccountDao
import com.simpleinvoice.app.data.model.BusinessProfile
import com.simpleinvoice.app.data.model.PaymentAccount
import kotlinx.coroutines.flow.Flow

class BusinessProfileRepository(
    private val dao: BusinessProfileDao,
    private val paymentAccountDao: PaymentAccountDao
) {
    fun observe(): Flow<BusinessProfile?> = dao.observe()

    suspend fun get(): BusinessProfile = dao.get() ?: BusinessProfile()

    suspend fun save(profile: BusinessProfile) = dao.upsert(profile.copy(id = BusinessProfile.SINGLETON_ID))

    fun observePaymentAccounts(): Flow<List<PaymentAccount>> = paymentAccountDao.observeAll()

    suspend fun getPaymentAccounts(): List<PaymentAccount> = paymentAccountDao.getAll()

    /** Saves bank details for each currency; entirely blank entries are removed. */
    suspend fun savePaymentAccounts(accounts: Collection<PaymentAccount>) {
        accounts.forEach { account ->
            if (account.isBlank) paymentAccountDao.delete(account.currencyCode) else paymentAccountDao.upsert(account)
        }
    }
}
