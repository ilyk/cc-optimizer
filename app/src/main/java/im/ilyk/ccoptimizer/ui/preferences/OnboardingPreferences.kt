package im.ilyk.ccoptimizer.ui.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "onboarding_preferences"
)

class OnboardingPreferences private constructor(context: Context) {
    private val dataStore = context.onboardingDataStore
    
    private object Keys {
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
    }
    
    val hasCompletedOnboarding: Flow<Boolean> = dataStore.data.map { preferences ->
        val value = preferences[Keys.HAS_COMPLETED_ONBOARDING] ?: false
        android.util.Log.d("OnboardingPreferences", "Reading hasCompletedOnboarding: $value")
        value
    }
    
    suspend fun setOnboardingCompleted() {
        android.util.Log.d("OnboardingPreferences", "setOnboardingCompleted called")
        dataStore.edit { preferences ->
            preferences[Keys.HAS_COMPLETED_ONBOARDING] = true
            android.util.Log.d("OnboardingPreferences", "setOnboardingCompleted saved: true")
        }
    }
    
    suspend fun resetOnboarding() {
        dataStore.edit { preferences ->
            preferences[Keys.HAS_COMPLETED_ONBOARDING] = false
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: OnboardingPreferences? = null
        
        fun getInstance(context: Context): OnboardingPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OnboardingPreferences(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}

