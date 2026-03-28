package ie.setu.carkey.data.repository

import ie.setu.carkey.data.DigitalKey
import ie.setu.carkey.data.Vehicle
import ie.setu.carkey.data.VehicleEvent
import ie.setu.carkey.data.firestore.EventStore
import ie.setu.carkey.data.firestore.KeyStore
import ie.setu.carkey.data.firestore.VehicleStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRepository @Inject constructor() {

    fun getKeyForUser(userId: String, onResult: (DigitalKey?) -> Unit) =
        KeyStore.getKeyForUser(userId, onResult)

    fun getVehicle(vehicleId: String, onResult: (Vehicle?) -> Unit) =
        VehicleStore.getVehicle(vehicleId, onResult)

    fun logEvent(event: VehicleEvent, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) =
        EventStore.logEvent(event, onSuccess, onError)
}
