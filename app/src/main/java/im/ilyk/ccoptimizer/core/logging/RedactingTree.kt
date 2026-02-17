package im.ilyk.ccoptimizer.core.logging

import android.util.Log
import timber.log.Timber

/**
 * Timber tree that redacts sensitive information from logs.
 * Strips PII, financial data, and authentication tokens.
 *
 * Use only in debug builds; production should avoid body logging entirely.
 */
class RedactingTree : Timber.DebugTree() {

    private val redactionPatterns = listOf(
        // Credit card related
        "cardId" to "***CARD_ID***",
        "last4" to "****",
        "balance" to "***BALANCE***",
        "limit" to "***LIMIT***",
        "available" to "***AVAILABLE***",
        "statementBalance" to "***STATEMENT_BALANCE***",
        "minimumPayment" to "***MIN_PAYMENT***",
        
        // Dates (potential PII for payment patterns)
        "statementDate" to "***DATE***",
        "dueDate" to "***DATE***",
        "paymentDate" to "***DATE***",
        "cycleDate" to "***DATE***",
        "dob" to "***DOB***",
        "dateOfBirth" to "***DOB***",
        
        // Authentication & Tokens
        "authToken" to "***TOKEN***",
        "accessToken" to "***TOKEN***",
        "access_token" to "***TOKEN***",
        "refreshToken" to "***TOKEN***",
        "refresh_token" to "***TOKEN***",
        "Authorization" to "***AUTH***",
        "Bearer" to "***BEARER***",
        "jwt" to "***JWT***",
        "session" to "***SESSION***",
        "sessionId" to "***SESSION***",
        "session_id" to "***SESSION***",
        
        // Plaid specific
        "plaidAccessToken" to "***PLAID_TOKEN***",
        "plaidItemId" to "***ITEM_ID***",
        "Plaid-Item" to "***PLAID_ITEM***",
        "public_token" to "***PUBLIC_TOKEN***",
        "publicToken" to "***PUBLIC_TOKEN***",
        "link_token" to "***LINK_TOKEN***",
        "linkToken" to "***LINK_TOKEN***",
        "item_id" to "***ITEM_ID***",
        "itemId" to "***ITEM_ID***",
        "account_id" to "***ACCOUNT_ID***",
        "accountId" to "***ACCOUNT_ID***",
        "request_id" to "***REQUEST_ID***",
        "requestId" to "***REQUEST_ID***",
        
        // API keys & Cookies
        "apiKey" to "***API_KEY***",
        "api_key" to "***API_KEY***",
        "X-Api-Key" to "***API_KEY***",
        "Cookie" to "***COOKIE***",
        "Set-Cookie" to "***COOKIE***",
        
        // User PII
        "email" to "***EMAIL***",
        "phone" to "***PHONE***",
        "phoneNumber" to "***PHONE***",
        "phone_number" to "***PHONE***",
        "ssn" to "***SSN***",
        "socialSecurity" to "***SSN***",
        "social_security_number" to "***SSN***",
        "accountNumber" to "***ACCOUNT***",
        "account_number" to "***ACCOUNT***",
        "routingNumber" to "***ROUTING***",
        "routing_number" to "***ROUTING***",
        "routing" to "***ROUTING***",
        "iban" to "***IBAN***",
        "swift" to "***SWIFT***",
        
        // Transaction details
        "merchantName" to "***MERCHANT***",
        "merchant_name" to "***MERCHANT***",
        "amount" to "***AMOUNT***",
        "category" to "***CATEGORY***",
        "description" to "***DESCRIPTION***",
        "memo" to "***MEMO***",
        
        // Device identifiers
        "deviceId" to "***DEVICE_ID***",
        "device_id" to "***DEVICE_ID***",
        "imei" to "***IMEI***",
        "androidId" to "***ANDROID_ID***"
    )

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val redactedMessage = redact(message)
        super.log(priority, tag, redactedMessage, t)
    }

    /**
     * Redacts sensitive patterns from log messages.
     * Uses both key-value pattern matching and regex patterns.
     */
    private fun redact(message: String): String {
        var redacted = message

        // Redact JSON key-value pairs
        redactionPatterns.forEach { (key, replacement) ->
            // Match: "key":"value" or 'key':'value' or key=value
            val jsonPattern = """"$key"\s*:\s*"[^"]*"""".toRegex(RegexOption.IGNORE_CASE)
            redacted = redacted.replace(jsonPattern, "\"$key\":\"$replacement\"")

            val jsonSingleQuotePattern = """'$key'\s*:\s*'[^']*'""".toRegex(RegexOption.IGNORE_CASE)
            redacted = redacted.replace(jsonSingleQuotePattern, "'$key':'$replacement'")

            val keyValuePattern = """$key\s*=\s*\S+""".toRegex(RegexOption.IGNORE_CASE)
            redacted = redacted.replace(keyValuePattern, "$key=$replacement")
        }

        // Redact credit card numbers (PAN: 13-19 digits with optional spaces/dashes)
        // Match patterns like: 4532123456789010, 4532-1234-5678-9010, 4532 1234 5678 9010
        val cardNumberPattern = """\b\d{4}[\s\-]?\d{4}[\s\-]?\d{4}[\s\-]?\d{4,7}\b""".toRegex()
        redacted = redacted.replace(cardNumberPattern, "****-****-****-****")
        
        // Also catch raw PAN-like sequences (13-19 consecutive digits)
        val rawPanPattern = """\b\d{13,19}\b""".toRegex()
        redacted = redacted.replace(rawPanPattern, "****-****-****-****")

        // Redact email addresses
        val emailPattern = """\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b""".toRegex()
        redacted = redacted.replace(emailPattern, "***EMAIL***")

        // Redact phone numbers (various formats)
        val phonePattern = """\b(\+?\d{1,3}[\s.-]?)?\(?\d{3}\)?[\s.-]?\d{3}[\s.-]?\d{4}\b""".toRegex()
        redacted = redacted.replace(phonePattern, "***PHONE***")

        // Redact SSN
        val ssnPattern = """\b\d{3}-\d{2}-\d{4}\b""".toRegex()
        redacted = redacted.replace(ssnPattern, "***-**-****")

        return redacted
    }
}

