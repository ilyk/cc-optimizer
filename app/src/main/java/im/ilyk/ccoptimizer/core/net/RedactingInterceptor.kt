package im.ilyk.ccoptimizer.core.net

import im.ilyk.ccoptimizer.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

/**
 * OkHttp interceptor that redacts sensitive data from request and response bodies.
 * Redacts headers and JSON fields containing PII or financial data.
 *
 * Should only be used in debug builds with body logging enabled.
 */
class RedactingInterceptor : Interceptor {

    private val sensitiveHeaders = setOf(
        "Authorization",
        "Plaid-Item",
        "X-Api-Key",
        "Cookie",
        "Set-Cookie",
        "X-Auth-Token",
        "Bearer",
        "API-Key"
    )

    private val sensitiveJsonKeys = setOf(
        // Financial
        "balance",
        "limit",
        "available",
        "statementBalance",
        "minimumPayment",
        "amount",
        "current",
        
        // Card details
        "last4",
        "cardId",
        "cardNumber",
        "cvv",
        "expirationDate",
        "expiration_date",
        
        // Dates (payment patterns)
        "statementDate",
        "dueDate",
        "paymentDate",
        "cycleDate",
        "dob",
        "dateOfBirth",
        "date_of_birth",
        
        // Auth & Tokens
        "authToken",
        "accessToken",
        "access_token",
        "refreshToken",
        "refresh_token",
        "jwt",
        "password",
        "session",
        "sessionId",
        "session_id",
        
        // Plaid
        "plaidAccessToken",
        "plaidItemId",
        "public_token",
        "publicToken",
        "link_token",
        "linkToken",
        "item_id",
        "itemId",
        "account_id",
        "accountId",
        "request_id",
        "requestId",
        
        // User PII
        "email",
        "phone",
        "phoneNumber",
        "phone_number",
        "ssn",
        "socialSecurity",
        "social_security_number",
        "accountNumber",
        "account_number",
        "routingNumber",
        "routing_number",
        "routing",
        "iban",
        "swift",
        
        // Transaction details
        "merchantName",
        "merchant_name",
        "category",
        "location",
        "description",
        "memo",
        
        // Device IDs
        "deviceId",
        "device_id",
        "imei",
        "androidId",
        "android_id"
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        // Log redacted request
        if (BuildConfig.DEBUG) {
            Timber.d("→ ${request.method} ${request.url}")
            
            // Log redacted headers
            request.headers.forEach { (name, value) ->
                val redactedValue = if (sensitiveHeaders.any { it.equals(name, ignoreCase = true) }) {
                    "***REDACTED***"
                } else {
                    value
                }
                Timber.v("  $name: $redactedValue")
            }
            
            // Log redacted body (if present)
            request.body?.let { body ->
                try {
                    val buffer = Buffer()
                    body.writeTo(buffer)
                    val bodyString = buffer.readUtf8()
                    val redactedBody = redactJsonBody(bodyString)
                    Timber.v("  Body: $redactedBody")
                } catch (e: Exception) {
                    Timber.v("  Body: [Cannot read body]")
                }
            }
        }
        
        val response = chain.proceed(request)
        
        // Log redacted response
        if (BuildConfig.DEBUG) {
            Timber.d("← ${response.code} ${request.url}")
            
            // Log redacted headers
            response.headers.forEach { (name, value) ->
                val redactedValue = if (sensitiveHeaders.any { it.equals(name, ignoreCase = true) }) {
                    "***REDACTED***"
                } else {
                    value
                }
                Timber.v("  $name: $redactedValue")
            }
            
            // Log redacted body (if present and successful)
            if (response.isSuccessful) {
                val responseBody = response.body
                if (responseBody != null) {
                    try {
                        val bodyString = responseBody.string()
                        val redactedBody = redactJsonBody(bodyString)
                        Timber.v("  Body: $redactedBody")
                        
                        // Recreate response with original body
                        return response.newBuilder()
                            .body(bodyString.toResponseBody(responseBody.contentType()))
                            .build()
                    } catch (e: Exception) {
                        Timber.v("  Body: [Cannot read body]")
                    }
                }
            }
        }
        
        return response
    }

    /**
     * Redacts sensitive keys from JSON strings.
     */
    private fun redactJsonBody(body: String): String {
        return try {
            when {
                body.trim().startsWith("{") -> {
                    val json = JSONObject(body)
                    redactJsonObject(json).toString(2)
                }
                body.trim().startsWith("[") -> {
                    val json = JSONArray(body)
                    redactJsonArray(json).toString(2)
                }
                else -> body // Not JSON
            }
        } catch (e: Exception) {
            body // Return original if parsing fails
        }
    }

    private fun redactJsonObject(json: JSONObject): JSONObject {
        val redacted = JSONObject()
        json.keys().forEach { key ->
            val value = json.get(key)
            redacted.put(key, when {
                sensitiveJsonKeys.any { it.equals(key, ignoreCase = true) } -> "***"
                value is JSONObject -> redactJsonObject(value)
                value is JSONArray -> redactJsonArray(value)
                else -> value
            })
        }
        return redacted
    }

    private fun redactJsonArray(json: JSONArray): JSONArray {
        val redacted = JSONArray()
        for (i in 0 until json.length()) {
            val value = json.get(i)
            redacted.put(when (value) {
                is JSONObject -> redactJsonObject(value)
                is JSONArray -> redactJsonArray(value)
                else -> value
            })
        }
        return redacted
    }
}

