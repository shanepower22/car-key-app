package ie.setu.carkey.ui.screens.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ie.setu.carkey.data.login.Credentials
import ie.setu.carkey.data.login.UserRole
import ie.setu.carkey.ui.components.login.LoginField
import ie.setu.carkey.ui.components.login.PasswordField
import ie.setu.carkey.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: (UserRole) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var credentials by remember { mutableStateOf(Credentials()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "GoKey",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Sign in to your GoKey account",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        LoginField(
            value = credentials.email,
            onValueChange = { credentials = credentials.copy(email = it) }
        )

        Spacer(Modifier.height(12.dp))

        PasswordField(
            value = credentials.password,
            onValueChange = { credentials = credentials.copy(password = it) }
        )

        uiState.error?.let { msg ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { viewModel.login(credentials, onLoginSuccess) },
            modifier = Modifier.fillMaxWidth(),
            enabled = credentials.isNotEmpty() && !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Sign In")
            }
        }
    }
}
