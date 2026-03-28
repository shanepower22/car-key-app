package ie.setu.carkey.ui.components.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ie.setu.carkey.data.DigitalKey
import ie.setu.carkey.data.KeyStatus
import ie.setu.carkey.data.Vehicle

@Composable
fun VehicleCard(
    vehicle: Vehicle?,
    digitalKey: DigitalKey?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (vehicle != null) "${vehicle.make} ${vehicle.model}"
                       else "No vehicle assigned",
                style = MaterialTheme.typography.titleMedium
            )
            if (vehicle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = vehicle.registrationPlate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(10.dp))
            val (label, color) = when (digitalKey?.status) {
                KeyStatus.ACTIVE  -> "Key Active"  to MaterialTheme.colorScheme.primary
                KeyStatus.REVOKED -> "Key Revoked" to MaterialTheme.colorScheme.error
                KeyStatus.EXPIRED -> "Key Expired" to MaterialTheme.colorScheme.error
                null              -> "No Key"      to MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}
