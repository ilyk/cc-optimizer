package im.ilyk.ccoptimizer.core.logging

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for RedactingTree to ensure PII and sensitive data is properly redacted.
 */
class RedactingTreeTest {

    private lateinit var redactingTree: RedactingTree

    @Before
    fun setup() {
        redactingTree = RedactingTree()
    }

    @Test
    fun `redacts credit card numbers`() {
        val message = "Card number: 4532-1234-5678-9010"
        val result = invokeRedact(message)
        
        assertFalse(result.contains("4532"))
        assertTrue(result.contains("****-****-****-****"))
    }

    @Test
    fun `redacts JSON auth tokens`() {
        val message = """{"authToken":"sk_live_abc123xyz"}"""
        val result = invokeRedact(message)
        
        assertFalse(result.contains("sk_live_abc123xyz"))
        assertTrue(result.contains("***TOKEN***"))
    }

    @Test
    fun `redacts balance and financial amounts`() {
        val message = """{"cardId":"123","balance":1250.50,"limit":5000}"""
        val result = invokeRedact(message)
        
        assertFalse(result.contains("1250.50"))
        assertFalse(result.contains("5000"))
        assertTrue(result.contains("***BALANCE***"))
        assertTrue(result.contains("***LIMIT***"))
    }

    @Test
    fun `redacts email addresses`() {
        val message = "User email: john.doe@example.com"
        val result = invokeRedact(message)
        
        assertFalse(result.contains("john.doe@example.com"))
        assertTrue(result.contains("***EMAIL***"))
    }

    @Test
    fun `redacts phone numbers`() {
        val message = "Phone: +1 (555) 123-4567"
        val result = invokeRedact(message)
        
        assertFalse(result.contains("555"))
        assertTrue(result.contains("***PHONE***"))
    }

    @Test
    fun `redacts SSN`() {
        val message = "SSN: 123-45-6789"
        val result = invokeRedact(message)
        
        assertFalse(result.contains("123-45-6789"))
        assertTrue(result.contains("***-**-****"))
    }

    @Test
    fun `redacts Plaid tokens`() {
        val message = """{"plaidAccessToken":"access-sandbox-abc123"}"""
        val result = invokeRedact(message)
        
        assertFalse(result.contains("access-sandbox-abc123"))
        assertTrue(result.contains("***PLAID_TOKEN***"))
    }

    @Test
    fun `redacts Authorization header`() {
        val message = "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
        val result = invokeRedact(message)
        
        assertFalse(result.contains("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"))
    }

    @Test
    fun `preserves non-sensitive data`() {
        val message = "Card issuer: Chase, Statement day: 15"
        val result = invokeRedact(message)
        
        // Should preserve issuer name
        assertTrue(result.contains("Chase"))
        // But redact date field
        assertTrue(result.contains("***DATE***"))
    }

    @Test
    fun `handles empty string`() {
        val result = invokeRedact("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `handles string with no sensitive data`() {
        val message = "Hello world, no sensitive data here"
        val result = invokeRedact(message)
        assertTrue(result.contains("Hello world"))
    }

    /**
     * Helper to invoke the private redact method via reflection
     * (or we could make it internal/public for testing)
     */
    private fun invokeRedact(message: String): String {
        val method = RedactingTree::class.java.getDeclaredMethod("redact", String::class.java)
        method.isAccessible = true
        return method.invoke(redactingTree, message) as String
    }
}

