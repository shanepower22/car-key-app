package ie.setu.carkey.security

import android.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// payload format: {command}:{nonce}:{timestamp}:{hmac_signature}
// e.g. unlock:a1b2c3d4:1712345678901:Xyz123==
// HMAC secret is provisioned per-key by the manager and stored in Firestore.
// HomeViewModel loads it from the active DigitalKey and sets it here before any command is sent.
object SecurityManager {

    // set by HomeViewModel once the active key is loaded from Firestore
    var hmacKey: String = ""

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
        val keyBytes = hmacKey.toByteArray(Charsets.UTF_8)
        val secretKey = SecretKeySpec(keyBytes, "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKey)
        val bytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    data class PayloadComponents(
        val command: String,
        val nonce: String,
        val timestamp: Long
    )
}
