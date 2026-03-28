package ie.setu.carkey.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ie.setu.carkey.data.DigitalKey
import ie.setu.carkey.data.EventResult
import ie.setu.carkey.data.Vehicle
import ie.setu.carkey.data.VehicleAction
import ie.setu.carkey.data.VehicleEvent
import ie.setu.carkey.data.login.AuthManager
import ie.setu.carkey.data.repository.FirebaseRepository
import ie.setu.carkey.service.BleManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

data class HomeUiState(
    val vehicle: Vehicle? = null,
    val key: DigitalKey? = null,
    val bleConnected: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: FirebaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { loadKeyAndVehicle() }

    private fun loadKeyAndVehicle() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        repository.getKeyForUser(AuthManager.currentUid) { key ->
            if (key == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "No active key found")
                return@getKeyForUser
            }
            _uiState.value = _uiState.value.copy(key = key)
            repository.getVehicle(key.vehicleId) { vehicle ->
                _uiState.value = _uiState.value.copy(vehicle = vehicle, isLoading = false)
            }
        }
    }

    fun unlock() = sendCommand("unlock", VehicleAction.UNLOCK)
    fun lock()   = sendCommand("lock",   VehicleAction.LOCK)

    private fun sendCommand(command: String, action: VehicleAction) {
        val key = _uiState.value.key ?: run {
            Timber.w("sendCommand: no active key")
            return
        }
        BleManager.sendCommand(command)
        repository.logEvent(
            VehicleEvent(
                logId     = UUID.randomUUID().toString(),
                userId    = AuthManager.currentUid,
                vehicleId = key.vehicleId,
                action    = action,
                result    = EventResult.SUCCESS,
                nonce     = UUID.randomUUID().toString()
            )
        )
    }
}
