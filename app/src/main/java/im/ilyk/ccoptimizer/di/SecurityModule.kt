package im.ilyk.ccoptimizer.di

import android.content.Context
import android.os.Build
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import im.ilyk.ccoptimizer.data.api.PlaidApi
import im.ilyk.ccoptimizer.data.services.SecureStore
import im.ilyk.ccoptimizer.security.Fido2Manager
import javax.inject.Singleton

/**
 * Hilt module for security dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideFido2Manager(
        @ApplicationContext context: Context,
        api: PlaidApi,
        secureStore: SecureStore
    ): Fido2Manager? {
        // Only provide Fido2Manager on Android 14+ (API 34+)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Fido2Manager(context, api, secureStore)
        } else {
            null
        }
    }
}
