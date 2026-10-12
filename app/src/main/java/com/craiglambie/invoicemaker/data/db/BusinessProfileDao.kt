package com.craiglambie.invoicemaker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.craiglambie.invoicemaker.data.model.BusinessProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessProfileDao {
    @Query("SELECT * FROM business_profile WHERE id = ${BusinessProfile.SINGLETON_ID} LIMIT 1")
    fun observe(): Flow<BusinessProfile?>

    @Query("SELECT * FROM business_profile WHERE id = ${BusinessProfile.SINGLETON_ID} LIMIT 1")
    suspend fun get(): BusinessProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: BusinessProfile)
}
