package ie.setu.carkey.ui.screens.manager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.Timestamp
import ie.setu.carkey.data.DigitalKey
import ie.setu.carkey.data.KeyStatus
import ie.setu.carkey.ui.viewmodel.ManagerViewModel
import java.util.Date

@Composable
fun KeysScreen(
    viewModel: ManagerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showAssignDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.successMessage, state.error) {
        // messages are shown via snackbar below; clear after showing
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAssignDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Assign Key")
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    Text("Digital Keys", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(state.keys) { key ->
                    KeyItem(
                        key = key,
                        onRevoke = { viewModel.revokeKey(key.keyId) }
                    )
                }
            }
        }
    }

    if (showAssignDialog) {
        AssignKeyDialog(
            users = state.users,
            vehicles = state.vehicles,
            onDismiss = { showAssignDialog = false },
            onConfirm = { userId, vehicleId ->
                // expiry = 30 days from now
                val expiry = Timestamp(Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000))
                viewModel.assignKey(userId, vehicleId, expiry)
                showAssignDialog = false
            }
        )
    }

    state.successMessage?.let { msg ->
        LaunchedEffect(msg) {
            viewModel.clearMessage()
        }
    }
    state.error?.let {
        LaunchedEffect(it) {
            viewModel.clearMessage()
        }
    }
}

@Composable
private fun KeyItem(key: DigitalKey, onRevoke: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("User: ${key.userId}", style = MaterialTheme.typography.bodyMedium)
                    Text("Vehicle: ${key.vehicleId}", style = MaterialTheme.typography.bodySmall)
                    Text("Key: ${key.keyId.take(8)}...", style = MaterialTheme.typography.bodySmall)
                }
                StatusChip(key.status)
            }
            if (key.status == KeyStatus.ACTIVE) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onRevoke,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Revoke")
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: KeyStatus) {
    val color = when (status) {
        KeyStatus.ACTIVE  -> MaterialTheme.colorScheme.primary
        KeyStatus.REVOKED -> MaterialTheme.colorScheme.error
        KeyStatus.EXPIRED -> MaterialTheme.colorScheme.outline
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status.name,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun AssignKeyDialog(
    users: List<ie.setu.carkey.data.login.UserModel>,
    vehicles: List<ie.setu.carkey.data.Vehicle>,
    onDismiss: () -> Unit,
    onConfirm: (userId: String, vehicleId: String) -> Unit
) {
    var selectedUserId by remember { mutableStateOf(users.firstOrNull()?.uid ?: "") }
    var selectedVehicleId by remember { mutableStateOf(vehicles.firstOrNull()?.vehicleId ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign Key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("User ID")
                DropdownSelector(
                    options = users.map { it.uid to (it.name.ifBlank { it.email }) },
                    selected = selectedUserId,
                    onSelect = { selectedUserId = it }
                )
                Text("Vehicle")
                DropdownSelector(
                    options = vehicles.map { it.vehicleId to "${it.make} ${it.model}" },
                    selected = selectedVehicleId,
                    onSelect = { selectedVehicleId = it }
                )
                Text(
                    "Expiry will be set to 30 days from now.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedUserId, selectedVehicleId) },
                enabled = selectedUserId.isNotBlank() && selectedVehicleId.isNotBlank()
            ) {
                Text("Assign")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DropdownSelector(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = options.firstOrNull { it.first == selected }?.second ?: selected

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(label.ifBlank { "Select..." })
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, display) ->
                DropdownMenuItem(
                    text = { Text(display) },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    }
                )
            }
        }
    }
}
