package ie.setu.carkey.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import android.util.Base64
import java.util.UUID
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

// payload format: {command}:{nonce}:{timestamp}:{hmac_signature}
// e.g. unlock:a1b2c3d4:1712345678901:Xyz123==
// HMAC key is generated once and stored in Android Keystore
object SecurityManager {

    private const val KEY_ALIAS = "carkey_hmac_key"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"

    fun buildPayload(command: String): String {
        val nonce = generateNonce()
        val timestamp = System.currentTimeMillis()
        val data = "$command:$nonce:$timestamp"
        val signature = sign(data)
        return "$data:$signature"
    }

    // 32-char hex nonce, unique per command to prevent replay attacks
    fun generateNonce(): String =
        UUID.randomUUID().toString().replace("-", "")

    // returns null if format is invalid or signature verification fails
    fun parsePayload(payload: String): PayloadComponents? {
        val parts = payload.split(":")
        if (parts.size != 4) return null
        val timestamp = parts[2].toLongOrNull() ?: return null
        val data = "${parts[0]}:${parts[1]}:${parts[2]}"
        if (!verifySignature(data, parts[3])) return null
        return PayloadComponents(
            command = parts[0],
            nonce = parts[1],
            timestamp = timestamp
        )
    }

    fun verifySignature(data: String, signature: String): Boolean {
        return try {
            sign(data) == signature
        } catch (e: Exception) {
            false
        }
    }

    private fun sign(data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(getOrCreateKey())
        val bytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        val keyGen = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_HMAC_SHA256,
            KEYSTORE_PROVIDER
        )
        keyGen.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN)
                .build()
        )
        return keyGen.generateKey()
    }

    data class PayloadComponents(
        val command: String,
        val nonce: String,
        val timestamp: Long
    )
}
