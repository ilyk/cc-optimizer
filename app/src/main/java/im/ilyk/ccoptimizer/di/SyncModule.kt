package im.ilyk.ccoptimizer.di

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import im.ilyk.ccoptimizer.BuildConfig
import im.ilyk.ccoptimizer.data.db.AppDatabase
import im.ilyk.ccoptimizer.data.repository.CycleRepositoryImpl
import im.ilyk.ccoptimizer.data.sync.*
import im.ilyk.ccoptimizer.security.SecureTokenStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.time.Clock
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/**
 * Hilt module for sync dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object SyncModule {
    
    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideSecureTokenStorage(@ApplicationContext context: Context): SecureTokenStorage {
        return SecureTokenStorage(context)
    }
    
    @Provides
    @Singleton
    fun provideSyncPrefs(@ApplicationContext context: Context): SyncPrefs {
        return SyncPrefs(context)
    }
    
    @Provides
    @Singleton
    fun provideRateLimitPrefs(@ApplicationContext context: Context): RateLimitPrefs {
        return RateLimitPrefs(context, Clock.systemUTC())
    }
    
    @Provides
    @Singleton
    fun provideSelfHostSyncProvider(
        moshi: Moshi,
        tokenStorage: SecureTokenStorage
    ): SelfHostSyncProvider {
        // Auth token provider gets JWT from secure storage
        val authTokenProvider: suspend () -> String = {
            tokenStorage.getJwtToken() ?: throw IllegalStateException("No auth token available. Please login first.")
        }
        
        return SelfHostSyncProvider(
            baseUrl = BuildConfig.BACKEND_URL,
            authTokenProvider = authTokenProvider,
            moshi = moshi
        )
    }
    
    @Provides
    @Singleton
    fun provideSyncProvider(
        selfHostProvider: SelfHostSyncProvider,
        rateLimitPrefs: RateLimitPrefs
    ): SyncProvider {
        // Wrap SelfHostSyncProvider with rate limiting
        return RateLimitedSyncProvider(
            delegate = selfHostProvider,
            rateLimitPrefs = rateLimitPrefs,
            clock = Clock.systemUTC()
        )
    }
    
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher {
        return Dispatchers.IO
    }
    
    @Provides
    @Singleton
    fun provideSyncRepository(
        db: AppDatabase,
        provider: SyncProvider,
        cycleRepository: CycleRepositoryImpl,
        prefs: SyncPrefs,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): SyncRepository {
        return SyncRepositoryImpl(
            db = db,
            provider = provider,
            cycleRepository = cycleRepository,
            prefs = prefs,
            ioDispatcher = ioDispatcher
        )
    }
}
