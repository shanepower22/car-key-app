package ie.setu.carkey.data

import com.google.firebase.Timestamp

enum class VehicleAction { UNLOCK, LOCK, UNKNOWN }
enum class EventResult   { SUCCESS, FAILURE }

data class VehicleEvent(
    val logId: String = "",
    val timestamp: Timestamp? = null,
    val userId: String = "",
    val vehicleId: String = "",
    val action: VehicleAction = VehicleAction.UNLOCK,
    val result: EventResult = EventResult.SUCCESS,
    val nonce: String = ""
)
