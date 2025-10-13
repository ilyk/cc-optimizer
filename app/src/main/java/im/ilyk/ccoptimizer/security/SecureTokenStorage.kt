package im.ilyk.ccoptimizer.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure storage for sensitive tokens using EncryptedSharedPreferences
 * Uses AES256-GCM encryption via Android Keystore
 */
class SecureTokenStorage(context: Context) {
    
    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    /**
     * Store JWT token securely
     */
    fun storeJwtToken(token: String) {
        encryptedPrefs.edit()
            .putString(KEY_JWT_TOKEN, token)
            .putLong(KEY_JWT_STORED_AT, System.currentTimeMillis())
            .apply()
    }
    
    /**
     * Retrieve JWT token
     * Returns null if token doesn't exist or has expired
     */
    fun getJwtToken(): String? {
        val token = encryptedPrefs.getString(KEY_JWT_TOKEN, null) ?: return null
        val storedAt = encryptedPrefs.getLong(KEY_JWT_STORED_AT, 0)
        
        // Check if token expired (7 days)
        val tokenAge = System.currentTimeMillis() - storedAt
        if (tokenAge > TOKEN_EXPIRY_MS) {
            clearJwtToken()
            return null
        }
        
        return token
    }
    
    /**
     * Clear JWT token
     */
    fun clearJwtToken() {
        encryptedPrefs.edit()
            .remove(KEY_JWT_TOKEN)
            .remove(KEY_JWT_STORED_AT)
            .apply()
    }
    
    /**
     * Store Plaid access token securely
     */
    fun storePlaidAccessToken(token: String) {
        encryptedPrefs.edit()
            .putString(KEY_PLAID_TOKEN, token)
            .putLong(KEY_PLAID_STORED_AT, System.currentTimeMillis())
            .apply()
    }
    
    /**
     * Retrieve Plaid access token
     * Returns null if token doesn't exist or has expired
     */
    fun getPlaidAccessToken(): String? {
        val token = encryptedPrefs.getString(KEY_PLAID_TOKEN, null) ?: return null
        val storedAt = encryptedPrefs.getLong(KEY_PLAID_STORED_AT, 0)
        
        // Plaid tokens don't expire, but check if stored recently (30 days)
        val tokenAge = System.currentTimeMillis() - storedAt
        if (tokenAge > PLAID_TOKEN_MAX_AGE_MS) {
            clearPlaidAccessToken()
            return null
        }
        
        return token
    }
    
    /**
     * Clear Plaid access token
     */
    fun clearPlaidAccessToken() {
        encryptedPrefs.edit()
            .remove(KEY_PLAID_TOKEN)
            .remove(KEY_PLAID_STORED_AT)
            .apply()
    }
    
    /**
     * Clear all tokens (logout)
     */
    fun clearAll() {
        encryptedPrefs.edit().clear().apply()
    }
    
    /**
     * Check if user has valid JWT token
     */
    fun isLoggedIn(): Boolean {
        return getJwtToken() != null
    }
    
    companion object {
        private const val PREFS_NAME = "secure_token_prefs"
        private const val KEY_JWT_TOKEN = "jwt_token"
        private const val KEY_JWT_STORED_AT = "jwt_stored_at"
        private const val KEY_PLAID_TOKEN = "plaid_token"
        private const val KEY_PLAID_STORED_AT = "plaid_stored_at"
        
        // 7 days in milliseconds
        private const val TOKEN_EXPIRY_MS = 7L * 24 * 60 * 60 * 1000
        
        // 30 days in milliseconds
        private const val PLAID_TOKEN_MAX_AGE_MS = 30L * 24 * 60 * 60 * 1000
    }
}

