package ie.setu.carkey.data.firestore

import com.google.firebase.firestore.FirebaseFirestore
import ie.setu.carkey.data.VehicleEvent
import timber.log.Timber

object EventStore {
    private val db = FirebaseFirestore.getInstance()

    fun logEvent(
        event: VehicleEvent,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        db.collection("vehicleEvents")
            .add(event)
            .addOnSuccessListener {
                Timber.i("Event logged: ${event.action} on ${event.vehicleId}")
                onSuccess()
            }
            .addOnFailureListener {
                Timber.e("Failed to log event: ${it.message}")
                onError(it.message ?: "Failed to log event")
            }
    }

    fun getAllEvents(onResult: (List<VehicleEvent>) -> Unit) {
        db.collection("vehicleEvents")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { docs ->
                val events = docs.mapNotNull { it.toObject(VehicleEvent::class.java) }
                Timber.i("Fetched ${events.size} events")
                onResult(events)
            }
            .addOnFailureListener {
                Timber.e("Failed to fetch events: ${it.message}")
                onResult(emptyList())
            }
    }
}
