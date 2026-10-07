package com.simpleinvoice.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.simpleinvoice.app.data.model.SavedItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedItemDao {
    @Query("SELECT * FROM saved_items ORDER BY description COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<SavedItem>>

    @Query("SELECT * FROM saved_items WHERE description = :description COLLATE NOCASE LIMIT 1")
    suspend fun findByDescription(description: String): SavedItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: SavedItem): Long

    @Delete
    suspend fun delete(item: SavedItem)
}
