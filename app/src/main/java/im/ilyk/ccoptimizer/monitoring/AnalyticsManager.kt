package im.ilyk.ccoptimizer.monitoring

import android.content.Context
import android.util.Log
import im.ilyk.ccoptimizer.BuildConfig

/**
 * Configurable Analytics Manager for Android
 * Supports multiple providers based on BuildConfig
 * 
 * To configure: Set in build.gradle.kts
 * buildConfigField("String", "ANALYTICS_PROVIDER", "\"mixpanel\"")
 * buildConfigField("String", "ANALYTICS_KEY", "\"your_key\"")
 */
class AnalyticsManager private constructor(context: Context) {
    
    private val provider: AnalyticsProvider
    private val enabled: Boolean = BuildConfig.ANALYTICS_ENABLED
    
    init {
        provider = when (BuildConfig.ANALYTICS_PROVIDER) {
            "mixpanel" -> MixpanelProvider(context)
            "amplitude" -> AmplitudeProvider(context)
            "firebase" -> FirebaseProvider(context)
            else -> NoOpProvider()
        }
        
        if (enabled) {
            Log.i(TAG, "Analytics initialized with provider: ${BuildConfig.ANALYTICS_PROVIDER}")
        } else {
            Log.i(TAG, "Analytics disabled")
        }
    }
    
    /**
     * Track an event
     */
    fun track(event: String, properties: Map<String, Any> = emptyMap()) {
        if (!enabled) return
        
        try {
            provider.track(event, properties)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to track event: $event", e)
        }
    }
    
    /**
     * Identify a user
     */
    fun identify(userId: String, traits: Map<String, Any> = emptyMap()) {
        if (!enabled) return
        
        try {
            provider.identify(userId, traits)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to identify user: $userId", e)
        }
    }
    
    /**
     * Reset analytics (on logout)
     */
    fun reset() {
        if (!enabled) return
        
        try {
            provider.reset()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset analytics", e)
        }
    }
    
    /**
     * Common events
     */
    object Events {
        // Authentication
        const val USER_REGISTERED = "User Registered"
        const val USER_LOGGED_IN = "User Logged In"
        const val USER_LOGGED_OUT = "User Logged Out"
        
        // Plaid
        const val PLAID_LINK_OPENED = "Plaid Link Opened"
        const val PLAID_LINK_SUCCESS = "Plaid Link Success"
        const val PLAID_LINK_EXIT = "Plaid Link Exit"
        const val PLAID_ACCOUNTS_LOADED = "Plaid Accounts Loaded"
        
        // Cards
        const val CREDIT_CARDS_VIEWED = "Credit Cards Viewed"
        const val CARD_DETAILS_VIEWED = "Card Details Viewed"
        const val STATEMENT_DATE_SET = "Statement Date Set Manually"
        
        // Recommendations
        const val RECOMMENDATIONS_VIEWED = "Recommendations Viewed"
        const val RECOMMENDATION_TAPPED = "Recommendation Tapped"
        
        // App Lifecycle
        const val APP_OPENED = "App Opened"
        const val APP_BACKGROUNDED = "App Backgrounded"
        const val PULL_TO_REFRESH = "Pull to Refresh"
        
        // Performance
        const val CACHE_HIT = "Cache Hit"
        const val CACHE_MISS = "Cache Miss"
        const val API_ERROR = "API Error"
    }
    
    companion object {
        private const val TAG = "AnalyticsManager"
        
        @Volatile
        private var instance: AnalyticsManager? = null
        
        fun getInstance(context: Context): AnalyticsManager {
            return instance ?: synchronized(this) {
                instance ?: AnalyticsManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

/**
 * Analytics provider interface
 */
interface AnalyticsProvider {
    fun track(event: String, properties: Map<String, Any>)
    fun identify(userId: String, traits: Map<String, Any>)
    fun reset()
}

/**
 * No-op provider (when analytics is disabled)
 */
class NoOpProvider : AnalyticsProvider {
    override fun track(event: String, properties: Map<String, Any>) {}
    override fun identify(userId: String, traits: Map<String, Any>) {}
    override fun reset() {}
}

/**
 * Mixpanel provider
 */
class MixpanelProvider(context: Context) : AnalyticsProvider {
    // Note: Add Mixpanel dependency in build.gradle.kts
    // implementation("com.mixpanel.android:mixpanel-android:7.+")
    
    private val mixpanel: Any? = try {
        // Mixpanel.getInstance(context, BuildConfig.ANALYTICS_KEY)
        null // Placeholder - add actual Mixpanel SDK
    } catch (e: Exception) {
        Log.e("MixpanelProvider", "Failed to initialize", e)
        null
    }
    
    override fun track(event: String, properties: Map<String, Any>) {
        // mixpanel?.track(event, JSONObject(properties))
    }
    
    override fun identify(userId: String, traits: Map<String, Any>) {
        // mixpanel?.identify(userId)
        // mixpanel?.people?.set(JSONObject(traits))
    }
    
    override fun reset() {
        // mixpanel?.reset()
    }
}

/**
 * Amplitude provider
 */
class AmplitudeProvider(context: Context) : AnalyticsProvider {
    // Note: Add Amplitude dependency
    // implementation("com.amplitude:android-sdk:2.+")
    
    override fun track(event: String, properties: Map<String, Any>) {
        // Amplitude.getInstance().logEvent(event, JSONObject(properties))
    }
    
    override fun identify(userId: String, traits: Map<String, Any>) {
        // Amplitude.getInstance().userId = userId
    }
    
    override fun reset() {
        // Amplitude.getInstance().setUserId(null)
        // Amplitude.getInstance().regenerateDeviceId()
    }
}

/**
 * Firebase Analytics provider
 */
class FirebaseProvider(context: Context) : AnalyticsProvider {
    // Note: Add Firebase dependency
    // implementation("com.google.firebase:firebase-analytics-ktx")
    
    private val firebaseAnalytics: Any? = try {
        // FirebaseAnalytics.getInstance(context)
        null // Placeholder
    } catch (e: Exception) {
        Log.e("FirebaseProvider", "Failed to initialize", e)
        null
    }
    
    override fun track(event: String, properties: Map<String, Any>) {
        // val bundle = Bundle()
        // properties.forEach { (key, value) ->
        //     when (value) {
        //         is String -> bundle.putString(key, value)
        //         is Int -> bundle.putInt(key, value)
        //         is Long -> bundle.putLong(key, value)
        //         is Double -> bundle.putDouble(key, value)
        //     }
        // }
        // firebaseAnalytics?.logEvent(event, bundle)
    }
    
    override fun identify(userId: String, traits: Map<String, Any>) {
        // firebaseAnalytics?.setUserId(userId)
    }
    
    override fun reset() {
        // firebaseAnalytics?.resetAnalyticsData()
    }
}

