package ie.setu.carkey.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ie.setu.carkey.data.DigitalKey
import ie.setu.carkey.data.EventResult
import ie.setu.carkey.data.Vehicle
import ie.setu.carkey.data.VehicleAction
import ie.setu.carkey.data.VehicleEvent
import ie.setu.carkey.data.login.AuthManager
import ie.setu.carkey.data.repository.FirebaseRepository
import ie.setu.carkey.security.SecurityManager
import ie.setu.carkey.service.BleManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    init {
        loadKeyAndVehicle()
        observeBleConnection()
    }

    private fun loadKeyAndVehicle() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        repository.getKeyForUser(AuthManager.currentUid) { key ->
            if (key == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "No active key found for this account"
                )
                return@getKeyForUser
            }
            _uiState.value = _uiState.value.copy(key = key)
            SecurityManager.hmacKey = key.hmacSecret
            repository.getVehicle(key.vehicleId) { vehicle ->
                _uiState.value = _uiState.value.copy(vehicle = vehicle, isLoading = false)
                Timber.i("Loaded vehicle: ${vehicle?.vehicleId}, key: ${key.keyId}")
            }
        }
    }

    private fun observeBleConnection() {
        viewModelScope.launch {
            BleManager.isConnected.collect { connected ->
                _uiState.value = _uiState.value.copy(bleConnected = connected)
                Timber.i("BLE connection state changed: $connected")
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
        if (!_uiState.value.bleConnected) {
            Timber.w("sendCommand: VACU not connected")
            _uiState.value = _uiState.value.copy(error = "VACU not connected")
            return
        }
        BleManager.sendCommand(command)
        repository.logEvent(
            VehicleEvent(
                logId     = UUID.randomUUID().toString(),
                timestamp = com.google.firebase.Timestamp.now(),
                userId    = AuthManager.currentUid,
                vehicleId = key.vehicleId,
                action    = action,
                result    = EventResult.SUCCESS,
                nonce     = UUID.randomUUID().toString()
            )
        )
        Timber.i("$action command sent for vehicle ${key.vehicleId}")
    }
}
