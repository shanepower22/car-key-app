package ie.setu.carkey.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ie.setu.carkey.data.login.AuthManager
import ie.setu.carkey.data.login.Credentials
import ie.setu.carkey.data.login.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val isSignedIn get() = AuthManager.isSignedIn

    fun login(credentials: Credentials, onSuccess: (UserRole) -> Unit) {
        _uiState.value = AuthUiState(isLoading = true)
        AuthManager.login(
            credentials = credentials,
            onSuccess = { role ->
                _uiState.value = AuthUiState()
                onSuccess(role)
            },
            onError = { msg ->
                _uiState.value = AuthUiState(error = msg)
            }
        )
    }

    fun resolveRole(onSuccess: (UserRole) -> Unit) {
        AuthManager.fetchRole(onSuccess) { msg ->
            _uiState.value = AuthUiState(error = msg)
        }
    }

    fun signOut() = AuthManager.signOut()
}
