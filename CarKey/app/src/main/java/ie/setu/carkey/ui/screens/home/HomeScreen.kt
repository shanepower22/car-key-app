package ie.setu.carkey.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ie.setu.carkey.ui.components.home.BleStatusBanner
import ie.setu.carkey.ui.components.home.VehicleCard
import ie.setu.carkey.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onSignOut: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BleStatusBanner(isConnected = uiState.bleConnected)

        Spacer(Modifier.height(16.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else {
            VehicleCard(vehicle = uiState.vehicle, digitalKey = uiState.key)
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.unlock() },
                modifier = Modifier.weight(1f),
                enabled = uiState.key?.isUsable() == true && uiState.bleConnected
            ) { Text("Unlock") }

            OutlinedButton(
                onClick = { viewModel.lock() },
                modifier = Modifier.weight(1f),
                enabled = uiState.key?.isUsable() == true && uiState.bleConnected
            ) { Text("Lock") }
        }

        uiState.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.weight(1f))

        TextButton(onClick = onSignOut) { Text("Sign Out") }
    }
}
