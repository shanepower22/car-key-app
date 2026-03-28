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
}
