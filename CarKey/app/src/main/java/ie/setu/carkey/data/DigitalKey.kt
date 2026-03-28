package ie.setu.carkey.data

import com.google.firebase.Timestamp

data class DigitalKey(
    val keyId: String = "",
    val vehicleId: String = "",
    val userId: String = "",
    val status: KeyStatus = KeyStatus.ACTIVE,
    val expiryDate: Timestamp? = null,
    val permissions: List<String> = emptyList()
) {
    fun isUsable(): Boolean {
        if (status != KeyStatus.ACTIVE) return false
        val expiry = expiryDate ?: return true
        return expiry.toDate().after(java.util.Date())
    }
}

enum class KeyStatus { ACTIVE, REVOKED, EXPIRED }
