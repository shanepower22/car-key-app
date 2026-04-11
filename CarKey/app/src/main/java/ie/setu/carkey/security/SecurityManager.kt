package ie.setu.carkey.security

import java.util.UUID

// payload format: {command}:{nonce}:{timestamp}
// e.g. unlock:a1b2c3d4e5f6:1712345678901
object SecurityManager {

    fun buildPayload(command: String): String {
        val nonce = generateNonce()
        val timestamp = System.currentTimeMillis()
        return "$command:$nonce:$timestamp"
    }

    // 32-char hex nonce, unique per command to prevent replay attacks
    fun generateNonce(): String =
        UUID.randomUUID().toString().replace("-", "")

    // returns null if the payload doesn't match the expected format
    fun parsePayload(payload: String): PayloadComponents? {
        val parts = payload.split(":")
        if (parts.size != 3) return null
        val timestamp = parts[2].toLongOrNull() ?: return null
        return PayloadComponents(
            command = parts[0],
            nonce = parts[1],
            timestamp = timestamp
        )
    }

    data class PayloadComponents(
        val command: String,
        val nonce: String,
        val timestamp: Long
    )
}
