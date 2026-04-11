package ie.setu.carkey.security

import org.junit.Assert.*
import org.junit.Test

// Unit tests for SecurityManager
// Tests that require buildPayload/signing are in SecurityManagerInstrumentedTest
class SecurityManagerTest {

    @Test
    fun `generateNonce produces unique values`() {
        val nonces = (1..100).map { SecurityManager.generateNonce() }.toSet()
        assertEquals(100, nonces.size)
    }

    @Test
    fun `generateNonce is 32 alphanumeric characters`() {
        val nonce = SecurityManager.generateNonce()
        assertEquals(32, nonce.length)
        assertTrue(nonce.all { it.isLetterOrDigit() })
    }

    @Test
    fun `parsePayload returns null for too few parts`() {
        assertNull(SecurityManager.parsePayload("unlock:abc123:1712345678901"))
    }

    @Test
    fun `parsePayload returns null for too many parts`() {
        assertNull(SecurityManager.parsePayload("unlock:abc:123:sig:extra"))
    }

    @Test
    fun `parsePayload returns null when timestamp is not a number`() {
        assertNull(SecurityManager.parsePayload("unlock:abc123:notanumber:sig"))
    }

    @Test
    fun `verifySignature returns false for tampered data`() {
        // verifySignature with mismatched data/sig should not throw, just return false
        assertFalse(SecurityManager.verifySignature("unlock:abc:123", "badsignature=="))
    }
}
