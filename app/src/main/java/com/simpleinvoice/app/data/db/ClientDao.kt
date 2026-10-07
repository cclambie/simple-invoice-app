package com.simpleinvoice.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.simpleinvoice.app.data.model.Client
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun getById(id: Long): Client?

    @Query("SELECT * FROM clients WHERE id = :id")
    fun observeById(id: Long): Flow<Client?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(client: Client): Long

    @Update
    suspend fun update(client: Client)

    @Delete
    suspend fun delete(client: Client)
}
