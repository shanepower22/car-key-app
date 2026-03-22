data class DigitalKey(
    val keyId: String = "",
    val vehicleId: String = "",
    val userId: String = "",
    val status: KeyStatus = KeyStatus.ACTIVE,
    val expiryDate: com.google.firebase.Timestamp? = null,
    val permissions: List<String> = emptyList()
)

enum class KeyStatus {
    ACTIVE,    // Key is valid and can be used
    REVOKED,   // Key was explicitly revoked by a manager
    EXPIRED    // Key passed its expiry date
}