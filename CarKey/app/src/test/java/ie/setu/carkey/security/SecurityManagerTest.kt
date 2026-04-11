package ie.setu.carkey.security

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for SecurityManager — V-Model Phase 5 (Unit Test)
 *
 * Tests verify:
 *  - Payload structure matches {command}:{nonce}:{timestamp}
 *  - Nonces are unique across calls (replay prevention — FR-06)
 *  - parsePayload correctly reconstructs components
 *  - parsePayload returns null for malformed input
 */
class SecurityManagerTest {

    @Test
    fun `buildPayload returns three colon-separated parts`() {
        val payload = SecurityManager.buildPayload("unlock")
        val parts = payload.split(":")
        assertEquals(3, parts.size)
    }

    @Test
    fun `buildPayload first part matches command`() {
        val payload = SecurityManager.buildPayload("lock")
        val command = payload.split(":")[0]
        assertEquals("lock", command)
    }

    @Test
    fun `buildPayload timestamp is a valid long`() {
        val payload = SecurityManager.buildPayload("unlock")
        val timestamp = payload.split(":")[2].toLongOrNull()
        assertNotNull(timestamp)
        assertTrue(timestamp!! > 0)
    }

    @Test
    fun `generateNonce produces unique values`() {
        val nonces = (1..100).map { SecurityManager.generateNonce() }.toSet()
        assertEquals(100, nonces.size)
    }

    @Test
    fun `generateNonce is 32 hex characters`() {
        val nonce = SecurityManager.generateNonce()
        assertEquals(32, nonce.length)
        assertTrue(nonce.all { it.isLetterOrDigit() })
    }

    @Test
    fun `parsePayload returns correct components`() {
        val payload = "unlock:abc123:1712345678901"
        val result = SecurityManager.parsePayload(payload)
        assertNotNull(result)
        assertEquals("unlock", result!!.command)
        assertEquals("abc123", result.nonce)
        assertEquals(1712345678901L, result.timestamp)
    }

    @Test
    fun `parsePayload returns null for too few parts`() {
        assertNull(SecurityManager.parsePayload("unlock:abc123"))
    }

    @Test
    fun `parsePayload returns null for too many parts`() {
        assertNull(SecurityManager.parsePayload("unlock:abc:123:extra"))
    }

    @Test
    fun `parsePayload returns null when timestamp is not a number`() {
        assertNull(SecurityManager.parsePayload("unlock:abc123:notanumber"))
    }

    @Test
    fun `buildPayload round-trips through parsePayload`() {
        val payload = SecurityManager.buildPayload("lock")
        val result = SecurityManager.parsePayload(payload)
        assertNotNull(result)
        assertEquals("lock", result!!.command)
        assertEquals(32, result.nonce.length)
        assertTrue(result.timestamp > 0)
    }
}
