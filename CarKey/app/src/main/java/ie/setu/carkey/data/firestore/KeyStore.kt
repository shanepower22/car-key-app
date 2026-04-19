package ie.setu.carkey.data.firestore

import com.google.firebase.firestore.FirebaseFirestore
import ie.setu.carkey.data.DigitalKey
import timber.log.Timber

object KeyStore {
    private val db = FirebaseFirestore.getInstance()

    fun getKeyForUser(userId: String, onResult: (DigitalKey?) -> Unit) {
        db.collection("digitalKeys")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "ACTIVE")
            .limit(1)
            .get()
            .addOnSuccessListener { docs ->
                val key = docs.documents.firstOrNull()?.toObject(DigitalKey::class.java)
                Timber.i("Key fetched for user $userId: ${key?.keyId}")
                onResult(key)
            }
            .addOnFailureListener {
                Timber.e("Failed to fetch key: ${it.message}")
                onResult(null)
            }
    }

    fun getAllKeys(onResult: (List<DigitalKey>) -> Unit) {
        db.collection("digitalKeys").get()
            .addOnSuccessListener { docs ->
                val keys = docs.mapNotNull { it.toObject(DigitalKey::class.java) }
                Timber.i("Fetched ${keys.size} keys")
                onResult(keys)
            }
            .addOnFailureListener {
                Timber.e("Failed to fetch keys: ${it.message}")
                onResult(emptyList())
            }
    }

    fun revokeKey(keyId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        db.collection("digitalKeys").document(keyId)
            .update("status", "REVOKED")
            .addOnSuccessListener {
                Timber.i("Key revoked: $keyId")
                onSuccess()
            }
            .addOnFailureListener {
                Timber.e("Failed to revoke key: ${it.message}")
                onError(it.message ?: "Failed to revoke key")
            }
    }

    fun assignKey(key: DigitalKey, onSuccess: () -> Unit, onError: (String) -> Unit) {
        db.collection("digitalKeys").document(key.keyId)
            .set(key)
            .addOnSuccessListener {
                Timber.i("Key assigned: ${key.keyId} to user ${key.userId}")
                onSuccess()
            }
            .addOnFailureListener {
                Timber.e("Failed to assign key: ${it.message}")
                onError(it.message ?: "Failed to assign key")
            }
    }
}
