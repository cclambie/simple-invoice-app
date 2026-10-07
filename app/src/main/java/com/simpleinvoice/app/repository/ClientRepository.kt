package com.simpleinvoice.app.repository

import com.simpleinvoice.app.data.db.ClientDao
import com.simpleinvoice.app.data.model.Client
import kotlinx.coroutines.flow.Flow

class ClientRepository(private val dao: ClientDao) {
    fun observeAll(): Flow<List<Client>> = dao.observeAll()

    fun observeById(id: Long): Flow<Client?> = dao.observeById(id)

    suspend fun getById(id: Long): Client? = dao.getById(id)

    suspend fun save(client: Client): Long =
        if (client.id == 0L) dao.insert(client) else { dao.update(client); client.id }

    suspend fun delete(client: Client) = dao.delete(client)
}
