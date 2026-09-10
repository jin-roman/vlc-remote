package com.vlcsync.remote.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vlcsync.remote.data.ConnectionState
import com.vlcsync.remote.data.VlcDevice
import com.vlcsync.remote.data.normalizeAddress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var devices by remember { mutableStateOf<List<VlcDevice>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("VLC Sync Remote") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Добавить устройство") },
                onClick = { showAddDialog = true }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(devices) { device ->
                DeviceCard(device)
            }
        }
    }

    if (showAddDialog) {
        AddDeviceDialog(
            onDismiss = { showAddDialog = false },
            onDeviceAdded = { device ->
                devices = devices + device
                showAddDialog = false
            }
        )
    }
}

@Composable
fun DeviceCard(device: VlcDevice) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("${device.host}:${device.port}")
            Text("Статус: ${device.connectionState}")

            device.status?.let { status ->
                Spacer(modifier = Modifier.height(8.dp))
                Text("Позиция: ${status.positionMs / 1000}с / ${status.durationMs / 1000}с")
                Text("Медиа: ${status.mediaTitle ?: "Неизвестно"}")

                if (status.durationMs > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { status.positionMs.toFloat() / status.durationMs },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun AddDeviceDialog(
    onDismiss: () -> Unit,
    onDeviceAdded: (VlcDevice) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить устройство") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя устройства") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Адрес (192.168.1.100:8080)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val normalized = normalizeAddress(address)
                    if (normalized != null && name.isNotBlank()) {
                        val parts = normalized.removePrefix("https://").split(":")
                        onDeviceAdded(
                            VlcDevice(
                                id = java.util.UUID.randomUUID().toString(),
                                name = name,
                                host = parts[0],
                                port = parts[1].toInt(),
                                connectionState = ConnectionState.AUTH_NEEDED
                            )
                        )
                    }
                }
            ) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}