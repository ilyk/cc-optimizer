package im.ilyk.ccoptimizer.core.telemetry

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Configuration for self-hosted telemetry.
 * All analytics are OPT-IN and default to OFF.
 * No third-party SDKs; only user-provided endpoints.
 */
@Singleton
class TelemetryConfig @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "telemetry_config")

    /**
     * Whether telemetry is enabled (default: false)
     */
    val isEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ENABLED] ?: false
    }

    /**
     * User-provided self-hosted endpoint URL (must be HTTPS)
     */
    val endpointUrl: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_ENDPOINT_URL]
    }

    /**
     * Sample rate (0.0 to 1.0, default 0.0 when disabled)
     */
    val sampleRate: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_SAMPLE_RATE] ?: 0f
    }

    /**
     * Get current configuration as a snapshot
     */
    suspend fun getConfig(): Config {
        var config = Config()
        context.dataStore.data.map { prefs ->
            config = Config(
                enabled = prefs[KEY_ENABLED] ?: false,
                endpointUrl = prefs[KEY_ENDPOINT_URL],
                sampleRate = prefs[KEY_SAMPLE_RATE] ?: 0f
            )
        }.collect {}
        return config
    }

    /**
     * Enable telemetry with a self-hosted endpoint
     */
    suspend fun enable(endpointUrl: String, sampleRate: Float = 1f) {
        require(endpointUrl.startsWith("https://")) {
            "Telemetry endpoint must use HTTPS"
        }
        require(sampleRate in 0f..1f) {
            "Sample rate must be between 0.0 and 1.0"
        }
        
        // Strict validation: no localhost or private IPs in release builds
        if (!im.ilyk.ccoptimizer.BuildConfig.DEBUG) {
            val host = extractHost(endpointUrl)
            require(!isPrivateOrLocalhost(host)) {
                "Telemetry endpoint cannot be localhost or private IP in release builds"
            }
        }

        context.dataStore.edit { prefs ->
            prefs[KEY_ENABLED] = true
            prefs[KEY_ENDPOINT_URL] = endpointUrl
            prefs[KEY_SAMPLE_RATE] = sampleRate
        }
    }
    
    /**
     * Extract host from URL
     */
    private fun extractHost(url: String): String {
        return url.replace("https://", "")
            .replace("http://", "")
            .split("/")[0]
            .split(":")[0] // Remove port
    }
    
    /**
     * Check if host is localhost or private IP
     */
    private fun isPrivateOrLocalhost(host: String): Boolean {
        return host == "localhost" ||
               host == "127.0.0.1" ||
               host.startsWith("10.") ||
               host.startsWith("192.168.") ||
               host.startsWith("172.16.") ||
               host.startsWith("172.17.") ||
               host.startsWith("172.18.") ||
               host.startsWith("172.19.") ||
               host.startsWith("172.20.") ||
               host.startsWith("172.21.") ||
               host.startsWith("172.22.") ||
               host.startsWith("172.23.") ||
               host.startsWith("172.24.") ||
               host.startsWith("172.25.") ||
               host.startsWith("172.26.") ||
               host.startsWith("172.27.") ||
               host.startsWith("172.28.") ||
               host.startsWith("172.29.") ||
               host.startsWith("172.30.") ||
               host.startsWith("172.31.")
    }

    /**
     * Disable telemetry
     */
    suspend fun disable() {
        context.dataStore.edit { prefs ->
            prefs[KEY_ENABLED] = false
            prefs[KEY_SAMPLE_RATE] = 0f
        }
    }

    /**
     * Update endpoint URL (validates HTTPS)
     */
    suspend fun updateEndpoint(url: String) {
        require(url.startsWith("https://")) {
            "Telemetry endpoint must use HTTPS"
        }
        context.dataStore.edit { prefs ->
            prefs[KEY_ENDPOINT_URL] = url
        }
    }

    /**
     * Update sample rate
     */
    suspend fun updateSampleRate(rate: Float) {
        require(rate in 0f..1f) {
            "Sample rate must be between 0.0 and 1.0"
        }
        context.dataStore.edit { prefs ->
            prefs[KEY_SAMPLE_RATE] = rate
        }
    }

    data class Config(
        val enabled: Boolean = false,
        val endpointUrl: String? = null,
        val sampleRate: Float = 0f
    ) {
        /**
         * Check if telemetry can send events
         */
        fun canSend(): Boolean {
            return enabled && !endpointUrl.isNullOrBlank() && sampleRate > 0f
        }
    }

    companion object {
        private val KEY_ENABLED = booleanPreferencesKey("telemetry_enabled")
        private val KEY_ENDPOINT_URL = stringPreferencesKey("telemetry_endpoint_url")
        private val KEY_SAMPLE_RATE = floatPreferencesKey("telemetry_sample_rate")
    }
}

