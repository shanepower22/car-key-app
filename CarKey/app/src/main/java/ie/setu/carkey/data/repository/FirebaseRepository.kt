package ie.setu.carkey.data.repository

import ie.setu.carkey.data.DigitalKey
import ie.setu.carkey.data.Vehicle
import ie.setu.carkey.data.VehicleEvent
import ie.setu.carkey.data.firestore.EventStore
import ie.setu.carkey.data.firestore.KeyStore
import ie.setu.carkey.data.firestore.UserStore
import ie.setu.carkey.data.firestore.VehicleStore
import ie.setu.carkey.data.login.UserModel
import ie.setu.carkey.data.login.UserRole
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRepository @Inject constructor() {

    // driver
    fun getKeyForUser(userId: String, onResult: (DigitalKey?) -> Unit) =
        KeyStore.getKeyForUser(userId, onResult)

    fun getVehicle(vehicleId: String, onResult: (Vehicle?) -> Unit) =
        VehicleStore.getVehicle(vehicleId, onResult)

    fun logEvent(event: VehicleEvent, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) =
        EventStore.logEvent(event, onSuccess, onError)

    fun updatePublicKey(uid: String, publicKey: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) =
        UserStore.updatePublicKey(uid, publicKey, onSuccess, onError)

    // manager
    fun getAllUsers(onResult: (List<UserModel>) -> Unit) =
        UserStore.getAllUsers(onResult)

    fun addUser(
        email: String,
        password: String,
        name: String,
        role: UserRole,
        onSuccess: (UserModel) -> Unit,
        onError: (String) -> Unit
    ) = UserStore.addUser(email, password, name, role, onSuccess, onError)

    fun getAllKeys(onResult: (List<DigitalKey>) -> Unit) =
        KeyStore.getAllKeys(onResult)

    fun getAllVehicles(onResult: (List<Vehicle>) -> Unit) =
        VehicleStore.getAllVehicles(onResult)

    fun getAllEvents(onResult: (List<VehicleEvent>) -> Unit) =
        EventStore.getAllEvents(onResult)

    fun revokeKey(keyId: String, onSuccess: () -> Unit, onError: (String) -> Unit) =
        KeyStore.revokeKey(keyId, onSuccess, onError)

    fun assignKey(key: DigitalKey, onSuccess: () -> Unit, onError: (String) -> Unit) =
        KeyStore.assignKey(key, onSuccess, onError)

    fun addVehicle(vehicle: Vehicle, onSuccess: () -> Unit, onError: (String) -> Unit) =
        VehicleStore.addVehicle(vehicle, onSuccess, onError)

    fun deleteVehicle(vehicleId: String, onSuccess: () -> Unit, onError: (String) -> Unit) =
        VehicleStore.deleteVehicle(vehicleId, onSuccess, onError)
}
