package im.ilyk.ccoptimizer.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Centralized cryptography module for the application.
 * Provides MasterKey and EncryptedSharedPreferences instances.
 */
@Module
@InstallIn(SingletonComponent::class)
object CryptoModule {

    /**
     * Provides the application's MasterKey using AES-GCM with Android Keystore.
     * The key is generated on first access and persists in the Keystore.
     */
    @Provides
    @Singleton
    fun provideMasterKey(@ApplicationContext context: Context): MasterKey {
        return MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    /**
     * Provides encrypted shared preferences for user settings.
     * Stores: theme preferences, notification settings, telemetry config, etc.
     */
    @Provides
    @Singleton
    @UserPreferences
    fun provideUserEncryptedPreferences(
        @ApplicationContext context: Context,
        masterKey: MasterKey
    ): SharedPreferences {
        return EncryptedSharedPreferences.create(
            context,
            "user_preferences_encrypted",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Provides encrypted shared preferences for security-sensitive data.
     * Stores: tokens, API keys, MFA secrets, etc.
     * Already provided via SecureTokenStorage but exposed for injection.
     */
    @Provides
    @Singleton
    @SecurityPreferences
    fun provideSecurityEncryptedPreferences(
        @ApplicationContext context: Context,
        masterKey: MasterKey
    ): SharedPreferences {
        return EncryptedSharedPreferences.create(
            context,
            "secure_token_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}

/**
 * Qualifier for user preferences (non-sensitive settings)
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UserPreferences

/**
 * Qualifier for security preferences (tokens, keys)
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SecurityPreferences

