package com.craiglambie.invoicemaker.repository

import com.craiglambie.invoicemaker.data.db.SavedItemDao
import com.craiglambie.invoicemaker.data.model.SavedItem
import kotlinx.coroutines.flow.Flow

class SavedItemRepository(private val dao: SavedItemDao) {
    fun observeAll(): Flow<List<SavedItem>> = dao.observeAll()

    /** Saves an item, updating the price of an existing item with the same description. */
    suspend fun save(description: String, unitPrice: Double) {
        val existing = dao.findByDescription(description)
        dao.upsert(SavedItem(id = existing?.id ?: 0, description = description, unitPrice = unitPrice))
    }

    suspend fun delete(item: SavedItem) = dao.delete(item)
}
