package ie.setu.carkey.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

// Instrumented tests for SecurityManager - require Android Keystore
@RunWith(AndroidJUnit4::class)
class SecurityManagerInstrumentedTest {

    @Test
    fun buildPayload_returnsFourColonSeparatedParts() {
        val payload = SecurityManager.buildPayload("unlock")
        assertEquals(4, payload.split(":").size)
    }

    @Test
    fun buildPayload_firstPartMatchesCommand() {
        val payload = SecurityManager.buildPayload("lock")
        assertEquals("lock", payload.split(":")[0])
    }

    @Test
    fun buildPayload_timestampIsValidLong() {
        val parts = SecurityManager.buildPayload("unlock").split(":")
        val timestamp = parts[2].toLongOrNull()
        assertNotNull(timestamp)
        assertTrue(timestamp!! > 0)
    }

    @Test
    fun buildPayload_signatureIsNonEmpty() {
        val parts = SecurityManager.buildPayload("unlock").split(":")
        assertTrue(parts[3].isNotEmpty())
    }

    @Test
    fun parsePayload_roundTrip() {
        val payload = SecurityManager.buildPayload("unlock")
        val result = SecurityManager.parsePayload(payload)
        assertNotNull(result)
        assertEquals("unlock", result!!.command)
        assertEquals(32, result.nonce.length)
        assertTrue(result.timestamp > 0)
    }

    @Test
    fun signEcdsa_producesNonEmptyBase64() {
        val sig = SecurityManager.signEcdsa("unlock:abc:123", SecurityManager.activeUid)
        assertTrue(sig.isNotEmpty())
    }

    @Test
    fun buildPayload_twoCallsHaveDifferentNonces() {
        val nonce1 = SecurityManager.buildPayload("unlock").split(":")[1]
        val nonce2 = SecurityManager.buildPayload("unlock").split(":")[1]
        assertNotEquals(nonce1, nonce2)
    }
}
