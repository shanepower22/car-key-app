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
}
