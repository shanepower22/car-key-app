package ie.setu.carkey.data.login

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import timber.log.Timber

object AuthManager {
    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    val isSignedIn  get() = auth.currentUser != null
    val currentUid  get() = auth.currentUser?.uid   ?: ""
    val currentEmail get() = auth.currentUser?.email ?: ""

    fun login(
        credentials: Credentials,
        onSuccess: (UserRole) -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(credentials.email, credentials.password)
            .addOnSuccessListener {
                Timber.i("Login success: ${credentials.email}")
                fetchRole(onSuccess, onError)
            }
            .addOnFailureListener { e ->
                Timber.e("Login failed: ${e.message}")
                onError(e.message ?: "Login failed")
            }
    }

    fun fetchRole(
        onSuccess: (UserRole) -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return onError("No user signed in")
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val role = runCatching {
                    UserRole.valueOf(doc.getString("role") ?: UserRole.DRIVER.name)
                }.getOrDefault(UserRole.DRIVER)
                Timber.i("Role fetched: $role")
                onSuccess(role)
            }
            .addOnFailureListener { onError(it.message ?: "Failed to fetch role") }
    }

    fun signOut() {
        Timber.i("User signed out")
        auth.signOut()
    }
}
