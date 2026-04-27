package ie.setu.carkey.data.firestore

import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import ie.setu.carkey.data.login.UserModel
import ie.setu.carkey.data.login.UserRole
import timber.log.Timber

object UserStore {
    private val db = FirebaseFirestore.getInstance()

    fun updatePublicKey(
        uid: String,
        publicKey: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        db.collection("users").document(uid)
            .update("publicKey", publicKey)
            .addOnSuccessListener {
                Timber.i("Public key registered for user $uid")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Timber.e("Failed to update public key: ${e.message}")
                onError(e.message ?: "Failed to register public key")
            }
    }

    fun getUserPublicKey(uid: String, onResult: (String?) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val key = doc.getString("publicKey")
                onResult(if (key.isNullOrBlank()) null else key)
            }
            .addOnFailureListener {
                Timber.e("Failed to fetch public key: ${it.message}")
                onResult(null)
            }
    }

    fun getAllUsers(onResult: (List<UserModel>) -> Unit) {
        db.collection("users").get()
            .addOnSuccessListener { docs ->
                val users = docs.mapNotNull { doc ->
                    doc.toObject(UserModel::class.java)?.let { u ->
                        if (u.uid.isBlank()) u.copy(uid = doc.id) else u
                    }
                }
                Timber.i("Fetched ${users.size} users")
                onResult(users)
            }
            .addOnFailureListener {
                Timber.e("Failed to fetch users: ${it.message}")
                onResult(emptyList())
            }
    }

    // Uses a secondary FirebaseApp instance so the manager's session is not affected
    fun addUser(
        email: String,
        password: String,
        name: String,
        role: UserRole,
        onSuccess: (UserModel) -> Unit,
        onError: (String) -> Unit
    ) {
        val secondaryApp = try {
            FirebaseApp.getInstance("user_creation")
        } catch (e: IllegalStateException) {
            val options = FirebaseApp.getInstance().options
            FirebaseApp.initializeApp(
                FirebaseApp.getInstance().applicationContext,
                FirebaseOptions.Builder()
                    .setApiKey(options.apiKey)
                    .setApplicationId(options.applicationId)
                    .setProjectId(options.projectId)
                    .build(),
                "user_creation"
            )
        }

        val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)
        secondaryAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: run {
                    onError("User created but UID missing")
                    return@addOnSuccessListener
                }
                val userModel = UserModel(uid = uid, email = email, name = name, role = role.name)
                db.collection("users").document(uid).set(userModel)
                    .addOnSuccessListener {
                        Timber.i("User added: $email ($role)")
                        secondaryAuth.signOut()
                        onSuccess(userModel)
                    }
                    .addOnFailureListener { e ->
                        Timber.e("Firestore write failed for new user: ${e.message}")
                        onError(e.message ?: "Failed to save user profile")
                    }
            }
            .addOnFailureListener { e ->
                Timber.e("Auth user creation failed: ${e.message}")
                onError(e.message ?: "Failed to create user")
            }
    }
}
