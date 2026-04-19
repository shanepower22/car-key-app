package ie.setu.carkey.ui.screens.manager

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ManagerDashboardScreen(
    onNavigateToKeys: () -> Unit,
    onNavigateToEvents: () -> Unit,
    onNavigateToVehicles: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Manager Dashboard", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(8.dp))

        DashboardCard(
            title = "Key Management",
            description = "Assign and revoke digital keys",
            icon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            onClick = onNavigateToKeys
        )

        DashboardCard(
            title = "Event Log",
            description = "View all lock and unlock events",
            icon = { Icon(Icons.Filled.List, contentDescription = null) },
            onClick = onNavigateToEvents
        )

        DashboardCard(
            title = "Vehicles",
            description = "Add and remove fleet vehicles",
            icon = { Icon(Icons.Filled.Build, contentDescription = null) },
            onClick = onNavigateToVehicles
        )

        DashboardCard(
            title = "Users",
            description = "Add drivers and managers",
            icon = { Icon(Icons.Filled.Person, contentDescription = null) },
            onClick = onNavigateToUsers
        )

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Sign Out")
        }
    }
}

@Composable
private fun DashboardCard(
    title: String,
    description: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            icon()
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
