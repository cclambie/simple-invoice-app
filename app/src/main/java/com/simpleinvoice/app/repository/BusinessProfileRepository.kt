package com.simpleinvoice.app.repository

import com.simpleinvoice.app.data.db.BusinessProfileDao
import com.simpleinvoice.app.data.model.BusinessProfile
import kotlinx.coroutines.flow.Flow

class BusinessProfileRepository(private val dao: BusinessProfileDao) {
    fun observe(): Flow<BusinessProfile?> = dao.observe()

    suspend fun get(): BusinessProfile = dao.get() ?: BusinessProfile()

    suspend fun save(profile: BusinessProfile) = dao.upsert(profile.copy(id = BusinessProfile.SINGLETON_ID))
}
