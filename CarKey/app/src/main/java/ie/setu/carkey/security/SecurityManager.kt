package ie.setu.carkey.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// payload format: {command}:{nonce}:{timestamp}:{signature}
// signature is ECDSA P-256 with SHA-256, signed by a hardware-backed private key
// stored in the Android Keystore. The corresponding public key is registered
// against the user's profile in Firestore for VACU-side verification.
object SecurityManager {

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS_PREFIX = "gokey_signing_key_"


    var hmacKey: String = ""

    // set by HomeViewModel after the user signs in - used to look up the
    // correct key in the Android Keystore.
    var activeUid: String = ""

    fun buildPayload(command: String): String {
        val nonce = generateNonce()
        val timestamp = System.currentTimeMillis()
        val data = "$command:$nonce:$timestamp"
        val signature = signEcdsa(data, activeUid)
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

    // ECDSA / Keystore

    private fun keyAliasFor(uid: String): String = KEY_ALIAS_PREFIX + uid

    // returns the existing key pair for this user, or generates a new one bound
    // to the device hardware-backed Keystore.
    private fun ensureKeyPair(uid: String): KeyPair {
        val alias = keyAliasFor(uid)
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

        if (keyStore.containsAlias(alias)) {
            val privateKey = keyStore.getKey(alias, null) as PrivateKey
            val publicKey = keyStore.getCertificate(alias).publicKey
            return KeyPair(publicKey, privateKey)
        }

        val generator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            KEYSTORE_PROVIDER
        )
        generator.initialize(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build()
        )
        return generator.generateKeyPair()
    }

    // X.509 SubjectPublicKeyInfo encoded, base64 NO_WRAP
    fun getPublicKeyBase64(uid: String): String {
        val keyPair = ensureKeyPair(uid)
        return Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
    }

    // sign data with user's private key
    fun signEcdsa(data: String, uid: String): String {
        val keyPair = ensureKeyPair(uid)
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(keyPair.private)
        signature.update(data.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(signature.sign(), Base64.NO_WRAP)
    }
}
