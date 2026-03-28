package ie.setu.carkey.ui.components.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BleStatusBanner(
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (isConnected) MaterialTheme.colorScheme.primaryContainer
                else             MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isConnected) "● VACU Connected" else "● VACU Not Connected",
                style = MaterialTheme.typography.labelMedium,
                color = if (isConnected) MaterialTheme.colorScheme.onPrimaryContainer
                        else             MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
