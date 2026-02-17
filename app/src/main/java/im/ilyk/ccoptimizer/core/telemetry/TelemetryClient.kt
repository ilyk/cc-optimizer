package im.ilyk.ccoptimizer.core.telemetry

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import im.ilyk.ccoptimizer.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Client for sending minimal, non-PII telemetry to a self-hosted endpoint.
 * Only sends events if:
 * 1. User has explicitly enabled telemetry
 * 2. A valid HTTPS endpoint is configured
 * 3. Sample rate allows this event
 *
 * No third-party SDKs. Plain HTTPS POST to user-provided endpoint.
 */
@Singleton
class TelemetryClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: TelemetryConfig
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    /**
     * Track a screen view event
     */
    suspend fun trackScreen(screenName: String) {
        track(TelemetryEvent.ScreenView(screenName))
    }

    /**
     * Track a user action event
     */
    suspend fun trackAction(actionName: String, metadata: Map<String, String> = emptyMap()) {
        track(TelemetryEvent.Action(actionName, metadata))
    }

    /**
     * Track a generic event
     */
    suspend fun track(event: TelemetryEvent) {
        withContext(Dispatchers.IO) {
            try {
                val cfg = config.getConfig()
                
                // Only send if enabled and configured
                if (!cfg.canSend()) {
                    Timber.v("Telemetry disabled or not configured, skipping event")
                    return@withContext
                }

                // Apply sample rate
                if (Random.nextFloat() > cfg.sampleRate) {
                    Timber.v("Event sampled out (rate: ${cfg.sampleRate})")
                    return@withContext
                }

                // Send event
                sendEvent(cfg.endpointUrl!!, event)
            } catch (e: Exception) {
                // Never crash the app due to telemetry
                Timber.e(e, "Failed to send telemetry event")
            }
        }
    }

    /**
     * Preview what a telemetry event would look like.
     * Used in UI to show user exactly what data is sent.
     */
    fun preview(event: TelemetryEvent): String {
        return buildPayload(event).toString(2)
    }

    /**
     * Test connectivity to the configured endpoint.
     * Returns true if endpoint responds successfully.
     */
    suspend fun testConnection(endpointUrl: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            require(endpointUrl.startsWith("https://")) {
                "Endpoint must use HTTPS"
            }

            val testEvent = TelemetryEvent.Action("connection_test", emptyMap())
            sendEvent(endpointUrl, testEvent)
            
            Result.success("Connection successful")
        } catch (e: Exception) {
            Timber.e(e, "Connection test failed")
            Result.failure(e)
        }
    }

    /**
     * Sends an event to the endpoint via POST.
     */
    private fun sendEvent(endpointUrl: String, event: TelemetryEvent) {
        val payload = buildPayload(event)
        val body = payload.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(endpointUrl)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("User-Agent", "CCOptimizer/${BuildConfig.VERSION_NAME}")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Server returned ${response.code}: ${response.message}")
            }
            Timber.v("Telemetry event sent: ${event.type}")
        }
    }

    /**
     * Builds the minimal JSON payload.
     * Contains ONLY:
     * - event type
     * - timestamp
     * - app version
     * - optional non-PII metadata
     *
     * NO user identifiers, NO financial data, NO PII.
     */
    private fun buildPayload(event: TelemetryEvent): JSONObject {
        return JSONObject().apply {
            put("event", event.type)
            put("timestamp", System.currentTimeMillis())
            put("app_version", BuildConfig.VERSION_NAME)
            
            when (event) {
                is TelemetryEvent.ScreenView -> {
                    put("screen_name", event.screenName)
                }
                is TelemetryEvent.Action -> {
                    put("action_name", event.actionName)
                    if (event.metadata.isNotEmpty()) {
                        put("metadata", JSONObject(event.metadata))
                    }
                }
            }
        }
    }
}

/**
 * Telemetry event types
 */
sealed class TelemetryEvent(val type: String) {
    /**
     * Screen view event
     */
    data class ScreenView(val screenName: String) : TelemetryEvent("screen_view")

    /**
     * User action event
     */
    data class Action(
        val actionName: String,
        val metadata: Map<String, String> = emptyMap()
    ) : TelemetryEvent("action")
}

