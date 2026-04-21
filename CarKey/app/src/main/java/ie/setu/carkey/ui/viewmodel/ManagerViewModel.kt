package ie.setu.carkey.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import ie.setu.carkey.data.DigitalKey
import ie.setu.carkey.data.KeyStatus
import ie.setu.carkey.data.Vehicle
import ie.setu.carkey.data.VehicleEvent
import ie.setu.carkey.data.login.UserModel
import ie.setu.carkey.data.login.UserRole
import ie.setu.carkey.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

data class ManagerUiState(
    val users: List<UserModel> = emptyList(),
    val keys: List<DigitalKey> = emptyList(),
    val vehicles: List<Vehicle> = emptyList(),
    val events: List<VehicleEvent> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ManagerViewModel @Inject constructor(
    private val repository: FirebaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManagerUiState())
    val uiState: StateFlow<ManagerUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        var done = 0
        fun checkDone() { if (++done == 3) _uiState.value = _uiState.value.copy(isLoading = false) }
        repository.getAllUsers   { users    -> _uiState.value = _uiState.value.copy(users = users);       checkDone() }
        repository.getAllVehicles{ vehicles -> _uiState.value = _uiState.value.copy(vehicles = vehicles); checkDone() }
        repository.getAllKeys    { keys     -> _uiState.value = _uiState.value.copy(keys = keys);         checkDone() }
    }

    fun loadEvents() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        repository.getAllEvents { events ->
            _uiState.value = _uiState.value.copy(events = events, isLoading = false)
        }
    }

    fun revokeKey(keyId: String) {
        repository.revokeKey(
            keyId = keyId,
            onSuccess = {
                Timber.i("Key revoked: $keyId")
                _uiState.value = _uiState.value.copy(
                    successMessage = "Key revoked",
                    keys = _uiState.value.keys.map { key ->
                        if (key.keyId == keyId) key.copy(status = KeyStatus.REVOKED) else key
                    }
                )
            },
            onError = { msg ->
                _uiState.value = _uiState.value.copy(error = msg)
            }
        )
    }

    fun assignKey(userId: String, vehicleId: String, expiryTimestamp: Timestamp) {
        val key = DigitalKey(
            keyId = UUID.randomUUID().toString(),
            userId = userId,
            vehicleId = vehicleId,
            status = KeyStatus.ACTIVE,
            expiryDate = expiryTimestamp,
            hmacSecret = UUID.randomUUID().toString().replace("-", "")
        )
        repository.assignKey(
            key = key,
            onSuccess = {
                Timber.i("Key assigned to $userId for vehicle $vehicleId")
                _uiState.value = _uiState.value.copy(
                    successMessage = "Key assigned",
                    keys = _uiState.value.keys + key
                )
            },
            onError = { msg ->
                _uiState.value = _uiState.value.copy(error = msg)
            }
        )
    }

    fun addUser(email: String, password: String, name: String, role: UserRole) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        repository.addUser(
            email = email,
            password = password,
            name = name,
            role = role,
            onSuccess = { user ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "User ${user.email} created",
                    users = _uiState.value.users + user
                )
            },
            onError = { msg ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = msg)
            }
        )
    }

    fun addVehicle(make: String, model: String, plate: String) {
        val vehicle = Vehicle(
            vehicleId = UUID.randomUUID().toString(),
            make = make,
            model = model,
            registrationPlate = plate
        )
        repository.addVehicle(
            vehicle = vehicle,
            onSuccess = {
                Timber.i("Vehicle added: ${vehicle.vehicleId}")
                _uiState.value = _uiState.value.copy(
                    successMessage = "Vehicle added",
                    vehicles = _uiState.value.vehicles + vehicle
                )
            },
            onError = { msg ->
                _uiState.value = _uiState.value.copy(error = msg)
            }
        )
    }

    fun deleteVehicle(vehicleId: String) {
        repository.deleteVehicle(
            vehicleId = vehicleId,
            onSuccess = {
                Timber.i("Vehicle deleted: $vehicleId")
                _uiState.value = _uiState.value.copy(
                    successMessage = "Vehicle deleted",
                    vehicles = _uiState.value.vehicles.filter { it.vehicleId != vehicleId }
                )
            },
            onError = { msg ->
                _uiState.value = _uiState.value.copy(error = msg)
            }
        )
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }
}
