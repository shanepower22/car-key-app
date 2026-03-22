data class VehicleEvent(
    val logId: String = "",
    val timestamp: com.google.firebase.Timestamp? = null,
    val userId: String = "",
    val vehicleId: String = "",
    val action: VehicleAction = VehicleAction.UNLOCK,
    val result: EventResult = EventResult.SUCCESS,
    val nonce: String = ""
)

enum class VehicleAction { UNLOCK, LOCK }
enum class EventResult  { SUCCESS, FAILURE }
