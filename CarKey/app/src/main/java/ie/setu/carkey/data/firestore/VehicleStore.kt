package ie.setu.carkey.data.firestore

import com.google.firebase.firestore.FirebaseFirestore
import ie.setu.carkey.data.Vehicle
import timber.log.Timber

object VehicleStore {
    private val db = FirebaseFirestore.getInstance()

    fun getVehicle(vehicleId: String, onResult: (Vehicle?) -> Unit) {
        db.collection("vehicles").document(vehicleId).get()
            .addOnSuccessListener { doc ->
                val vehicle = doc.toObject(Vehicle::class.java)
                Timber.i("Vehicle fetched: ${vehicle?.vehicleId}")
                onResult(vehicle)
            }
            .addOnFailureListener {
                Timber.e("Failed to fetch vehicle: ${it.message}")
                onResult(null)
            }
    }

    fun getAllVehicles(onResult: (List<Vehicle>) -> Unit) {
        db.collection("vehicles").get()
            .addOnSuccessListener { docs ->
                val vehicles = docs.mapNotNull { it.toObject(Vehicle::class.java) }
                Timber.i("Fetched ${vehicles.size} vehicles")
                onResult(vehicles)
            }
            .addOnFailureListener {
                Timber.e("Failed to fetch vehicles: ${it.message}")
                onResult(emptyList())
            }
    }

    fun addVehicle(vehicle: Vehicle, onSuccess: () -> Unit, onError: (String) -> Unit) {
        db.collection("vehicles").document(vehicle.vehicleId)
            .set(vehicle)
            .addOnSuccessListener {
                Timber.i("Vehicle added: ${vehicle.vehicleId}")
                onSuccess()
            }
            .addOnFailureListener {
                Timber.e("Failed to add vehicle: ${it.message}")
                onError(it.message ?: "Failed to add vehicle")
            }
    }

    fun deleteVehicle(vehicleId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        db.collection("vehicles").document(vehicleId)
            .delete()
            .addOnSuccessListener {
                Timber.i("Vehicle deleted: $vehicleId")
                onSuccess()
            }
            .addOnFailureListener {
                Timber.e("Failed to delete vehicle: ${it.message}")
                onError(it.message ?: "Failed to delete vehicle")
            }
    }
}
