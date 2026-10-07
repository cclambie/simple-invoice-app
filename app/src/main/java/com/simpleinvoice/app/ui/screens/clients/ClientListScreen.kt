package com.simpleinvoice.app.ui.screens.clients

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simpleinvoice.app.data.model.Client
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientListScreen(
    onAddClient: () -> Unit,
    onOpenClient: (Long) -> Unit,
    viewModel: ClientListViewModel = viewModel(factory = ClientListViewModel.Factory)
) {
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Clients") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClient) {
                Icon(Icons.Filled.Add, contentDescription = "Add client")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (clients.isEmpty()) {
            EmptyClientsMessage(padding)
        } else {
            LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
                items(clients, key = { it.id }) { client ->
                    ClientRow(
                        client = client,
                        onClick = { onOpenClient(client.id) },
                        onDelete = {
                            viewModel.deleteClient(
                                client,
                                onBlocked = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Can't delete ${client.name}: they have invoices"
                                        )
                                    }
                                },
                                onDeleted = {}
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyClientsMessage(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Text("No clients yet. Tap + to add one or import from your contacts.")
    }
}

@Composable
private fun ClientRow(client: Client, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        ListItem(
            headlineContent = { Text(client.name) },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (client.email.isNotBlank()) Text(client.email)
                    if (client.phone.isNotBlank()) Text(client.phone)
                }
            },
            trailingContent = {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete client")
                }
            },
            modifier = Modifier.clickable(onClick = onClick)
        )
    }
}
