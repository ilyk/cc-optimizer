package im.ilyk.ccoptimizer.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Theme preferences manager
 * Supports: Light, Dark, and Auto (system default)
 */
class ThemePreferences(private val context: Context) {
    
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_AUTO = "auto" // Default
        
        @Volatile
        private var instance: ThemePreferences? = null
        
        fun getInstance(context: Context): ThemePreferences {
            return instance ?: synchronized(this) {
                instance ?: ThemePreferences(context.applicationContext).also { instance = it }
            }
        }
    }
    
    /**
     * Get current theme mode as Flow
     */
    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE_KEY] ?: THEME_AUTO // Auto is default
    }
    
    /**
     * Set theme mode
     */
    suspend fun setThemeMode(mode: String) {
        if (mode !in listOf(THEME_LIGHT, THEME_DARK, THEME_AUTO)) {
            throw IllegalArgumentException("Invalid theme mode: $mode")
        }
        
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode
        }
    }
    
    /**
     * Get theme mode synchronously (for initial setup)
     */
    suspend fun getThemeMode(): String {
        return context.dataStore.data.map { it[THEME_MODE_KEY] ?: THEME_AUTO }.first()
    }
}

/**
 * Theme mode enum for type safety
 */
enum class ThemeMode(val value: String, val displayName: String) {
    LIGHT(ThemePreferences.THEME_LIGHT, "Light"),
    DARK(ThemePreferences.THEME_DARK, "Dark"),
    AUTO(ThemePreferences.THEME_AUTO, "Auto (System)");
    
    companion object {
        fun fromValue(value: String): ThemeMode {
            return values().find { it.value == value } ?: AUTO
        }
    }
}

