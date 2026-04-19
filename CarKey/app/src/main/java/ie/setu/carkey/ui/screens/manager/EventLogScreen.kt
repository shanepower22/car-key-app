package ie.setu.carkey.ui.screens.manager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ie.setu.carkey.data.EventResult
import ie.setu.carkey.data.VehicleAction
import ie.setu.carkey.data.VehicleEvent
import ie.setu.carkey.ui.viewmodel.ManagerViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EventLogScreen(
    viewModel: ManagerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadEvents()
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text("Event Log", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.events.isEmpty()) {
            item {
                Text("No events recorded yet.", style = MaterialTheme.typography.bodyMedium)
            }
        }

        items(state.events) { event ->
            EventItem(event)
        }
    }
}

@Composable
private fun EventItem(event: VehicleEvent) {
    val formatter = remember { SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault()) }
    val timeStr = event.timestamp?.toDate()?.let { formatter.format(it) } ?: "Unknown time"
    val actionColor = if (event.action == VehicleAction.UNLOCK)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.secondary
    val resultColor = if (event.result == EventResult.SUCCESS)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.error

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(timeStr, style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Vehicle: ${event.vehicleId}", style = MaterialTheme.typography.bodyMedium)
                Text("User: ${event.userId}", style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    event.action.name,
                    color = actionColor,
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    event.result.name,
                    color = resultColor,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
