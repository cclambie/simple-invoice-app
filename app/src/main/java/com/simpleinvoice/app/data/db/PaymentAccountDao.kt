package com.simpleinvoice.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.simpleinvoice.app.data.model.PaymentAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentAccountDao {
    @Query("SELECT * FROM payment_accounts ORDER BY currencyCode")
    fun observeAll(): Flow<List<PaymentAccount>>

    @Query("SELECT * FROM payment_accounts ORDER BY currencyCode")
    suspend fun getAll(): List<PaymentAccount>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: PaymentAccount)

    @Query("DELETE FROM payment_accounts WHERE currencyCode = :currencyCode")
    suspend fun delete(currencyCode: String)
}
