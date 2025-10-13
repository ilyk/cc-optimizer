package im.ilyk.ccoptimizer.utils

import kotlinx.coroutines.delay
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Retry helper with exponential backoff for Android
 * Handles network errors gracefully
 */
object RetryHelper {
    
    /**
     * Retry a suspending function with exponential backoff
     */
    suspend fun <T> retry(
        maxAttempts: Int = 3,
        initialDelayMs: Long = 1000,
        maxDelayMs: Long = 10000,
        backoffMultiplier: Double = 2.0,
        shouldRetry: (Throwable) -> Boolean = ::isRetryableError,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMs
        var lastException: Throwable? = null

        repeat(maxAttempts) { attempt ->
            try {
                return block()
            } catch (e: Throwable) {
                lastException = e

                // Don't retry if error is not retryable or this was last attempt
                if (!shouldRetry(e) || attempt == maxAttempts - 1) {
                    throw e
                }

                // Add jitter (±30%)
                val jitter = (currentDelay * 0.3 * (Math.random() - 0.5)).toLong()
                val delayWithJitter = (currentDelay + jitter).coerceIn(0, maxDelayMs)

                android.util.Log.w(
                    "RetryHelper",
                    "Attempt ${attempt + 1}/$maxAttempts failed: ${e.message}. " +
                    "Retrying in ${delayWithJitter}ms..."
                )

                delay(delayWithJitter)

                // Exponential backoff for next attempt
                currentDelay = (currentDelay * backoffMultiplier).toLong().coerceAtMost(maxDelayMs)
            }
        }

        throw lastException ?: Exception("Retry failed with unknown error")
    }

    /**
     * Check if error should be retried
     */
    fun isRetryableError(error: Throwable): Boolean {
        return when (error) {
            // Network errors
            is IOException -> true
            is SocketTimeoutException -> true
            is UnknownHostException -> true
            
            // HTTP errors (check status code)
            else -> {
                // Check if it's a retrofit HTTP exception
                val message = error.message ?: ""
                when {
                    message.contains("HTTP 408") -> true // Request Timeout
                    message.contains("HTTP 429") -> true // Too Many Requests
                    message.contains("HTTP 500") -> true // Internal Server Error
                    message.contains("HTTP 502") -> true // Bad Gateway
                    message.contains("HTTP 503") -> true // Service Unavailable
                    message.contains("HTTP 504") -> true // Gateway Timeout
                    else -> false
                }
            }
        }
    }

    /**
     * Retry with custom retry predicate
     */
    suspend fun <T> retryWhen(
        maxAttempts: Int = 3,
        predicate: (Throwable) -> Boolean,
        block: suspend () -> T
    ): T {
        return retry(
            maxAttempts = maxAttempts,
            shouldRetry = predicate,
            block = block
        )
    }
}

