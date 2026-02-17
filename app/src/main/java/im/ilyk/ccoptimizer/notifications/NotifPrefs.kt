package im.ilyk.ccoptimizer.notifications

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

private val Context.notifDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "notification_preferences"
)

/**
 * Quiet hours configuration
 */
data class QuietHours(
    val start: LocalTime, // e.g., 22:00
    val end: LocalTime    // e.g., 07:00
)

/**
 * DataStore-based preferences for notification configuration
 */
@Singleton
class NotifPrefs @Inject constructor(
    context: Context
) {
    private val dataStore = context.notifDataStore
    
    companion object {
        private val KEY_BUFFER_CLOSE_DAYS = intPreferencesKey("buffer_close_days")
        private val KEY_BUFFER_DUE_DAYS = intPreferencesKey("buffer_due_days")
        private val KEY_QUIET_START_HOUR = intPreferencesKey("quiet_start_hour")
        private val KEY_QUIET_START_MINUTE = intPreferencesKey("quiet_start_minute")
        private val KEY_QUIET_END_HOUR = intPreferencesKey("quiet_end_hour")
        private val KEY_QUIET_END_MINUTE = intPreferencesKey("quiet_end_minute")
        
        // Defaults
        private const val DEFAULT_BUFFER_CLOSE_DAYS = 3
        private const val DEFAULT_BUFFER_DUE_DAYS = 10
        private val DEFAULT_QUIET_START = LocalTime.of(22, 0) // 10 PM
        private val DEFAULT_QUIET_END = LocalTime.of(7, 0)    // 7 AM
    }
    
    /**
     * Buffer days before statement close to send notification
     */
    val bufferCloseDays: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_BUFFER_CLOSE_DAYS] ?: DEFAULT_BUFFER_CLOSE_DAYS
    }
    
    /**
     * Buffer days before payment due to send notification
     */
    val bufferDueDays: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_BUFFER_DUE_DAYS] ?: DEFAULT_BUFFER_DUE_DAYS
    }
    
    /**
     * Quiet hours configuration (no notifications during this time)
     */
    val quietHours: Flow<QuietHours> = dataStore.data.map { prefs ->
        val startHour = prefs[KEY_QUIET_START_HOUR] ?: DEFAULT_QUIET_START.hour
        val startMinute = prefs[KEY_QUIET_START_MINUTE] ?: DEFAULT_QUIET_START.minute
        val endHour = prefs[KEY_QUIET_END_HOUR] ?: DEFAULT_QUIET_END.hour
        val endMinute = prefs[KEY_QUIET_END_MINUTE] ?: DEFAULT_QUIET_END.minute
        
        QuietHours(
            start = LocalTime.of(startHour, startMinute),
            end = LocalTime.of(endHour, endMinute)
        )
    }
    
    /**
     * Set buffer days for close and due notifications
     */
    suspend fun setBuffers(closeDays: Int, dueDays: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_BUFFER_CLOSE_DAYS] = closeDays.coerceIn(1, 14)
            prefs[KEY_BUFFER_DUE_DAYS] = dueDays.coerceIn(1, 30)
        }
    }
    
    /**
     * Set quiet hours
     */
    suspend fun setQuietHours(quietHours: QuietHours) {
        dataStore.edit { prefs ->
            prefs[KEY_QUIET_START_HOUR] = quietHours.start.hour
            prefs[KEY_QUIET_START_MINUTE] = quietHours.start.minute
            prefs[KEY_QUIET_END_HOUR] = quietHours.end.hour
            prefs[KEY_QUIET_END_MINUTE] = quietHours.end.minute
        }
    }
    
    /**
     * Get current buffer settings
     */
    suspend fun getBufferCloseDays(): Int {
        return dataStore.data.map { it[KEY_BUFFER_CLOSE_DAYS] ?: DEFAULT_BUFFER_CLOSE_DAYS }.first()
    }
    
    suspend fun getBufferDueDays(): Int {
        return dataStore.data.map { it[KEY_BUFFER_DUE_DAYS] ?: DEFAULT_BUFFER_DUE_DAYS }.first()
    }
    
    suspend fun getQuietHours(): QuietHours {
        return dataStore.data.map { prefs ->
            val startHour = prefs[KEY_QUIET_START_HOUR] ?: DEFAULT_QUIET_START.hour
            val startMinute = prefs[KEY_QUIET_START_MINUTE] ?: DEFAULT_QUIET_START.minute
            val endHour = prefs[KEY_QUIET_END_HOUR] ?: DEFAULT_QUIET_END.hour
            val endMinute = prefs[KEY_QUIET_END_MINUTE] ?: DEFAULT_QUIET_END.minute
            
            QuietHours(
                start = LocalTime.of(startHour, startMinute),
                end = LocalTime.of(endHour, endMinute)
            )
        }.first()
    }
    
    /**
     * Clear all notification preferences
     */
    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}
