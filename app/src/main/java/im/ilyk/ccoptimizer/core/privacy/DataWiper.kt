package im.ilyk.ccoptimizer.core.privacy

import android.app.NotificationManager
import android.content.Context
import androidx.work.WorkManager
import coil.Coil
import dagger.hilt.android.qualifiers.ApplicationContext
import im.ilyk.ccoptimizer.core.security.SecurityPreferences
import im.ilyk.ccoptimizer.core.security.UserPreferences
import im.ilyk.ccoptimizer.data.db.AppDatabase
import im.ilyk.ccoptimizer.data.local.CacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles complete deletion of all local user data.
 * Wipes database, encrypted preferences, caches, and work manager jobs.
 */
@Singleton
class DataWiper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    @UserPreferences private val userPrefs: android.content.SharedPreferences,
    @SecurityPreferences private val securityPrefs: android.content.SharedPreferences,
    private val cacheManager: CacheManager
) {

    /**
     * Wipes all local data including:
     * - Room database (all tables)
     * - Encrypted shared preferences (both user and security)
     * - App cache directory
     * - Coil image cache
     * - WorkManager jobs
     *
     * Returns Result with success/failure and optional error message.
     */
    suspend fun wipeAll(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Timber.w("Starting complete data wipe...")

            // 1. Clear all Room database tables
            wipeDatabase()

            // 2. Clear encrypted preferences
            wipeEncryptedPreferences()

            // 3. Clear app cache
            wipeCache()

            // 4. Clear Coil image cache
            wipeImageCache()

            // 5. Cancel all WorkManager jobs
            wipeWorkManagerJobs()

            // 6. Clear any temporary files
            wipeTempFiles()

            Timber.w("Data wipe completed successfully")
            
            // Verify wipe was successful
            val verified = verifyWipe()
            if (!verified) {
                Timber.e("Post-wipe verification failed - some data may remain")
                return@withContext Result.failure(Exception("Wipe verification failed"))
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to wipe data")
            Result.failure(e)
        }
    }

    /**
     * Clears all tables in the Room database.
     */
    private suspend fun wipeDatabase() {
        try {
            database.clearAllTables()
            Timber.d("Database cleared")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear database")
            throw e
        }
    }

    /**
     * Clears all encrypted shared preferences.
     */
    private suspend fun wipeEncryptedPreferences() {
        try {
            userPrefs.edit().clear().apply()
            securityPrefs.edit().clear().apply()
            Timber.d("Encrypted preferences cleared")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear encrypted preferences")
            throw e
        }
    }

    /**
     * Clears app cache directory.
     */
    private suspend fun wipeCache() {
        try {
            cacheManager.clearCache()
            
            // Also clear standard cache directory
            context.cacheDir.deleteRecursively()
            context.cacheDir.mkdirs()
            
            // Clear code cache (if exists, Android 5+)
            context.codeCacheDir?.let {
                it.deleteRecursively()
                it.mkdirs()
            }
            
            Timber.d("Cache cleared")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear cache")
            throw e
        }
    }

    /**
     * Clears Coil image cache (both memory and disk).
     */
    private suspend fun wipeImageCache() {
        try {
            val imageLoader = Coil.imageLoader(context)
            // Clear memory cache
            imageLoader.memoryCache?.clear()
            // Clear disk cache
            imageLoader.diskCache?.clear()
            Timber.d("Image cache cleared (memory + disk)")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear image cache")
            // Non-critical, continue
        }
    }

    /**
     * Cancels all WorkManager jobs and clears notification channels.
     */
    private suspend fun wipeWorkManagerJobs() {
        try {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelAllWork()
            workManager.pruneWork() // Remove completed jobs
            Timber.d("WorkManager jobs cancelled")
            
            // Cancel all notifications
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancelAll()
            Timber.d("All notifications cancelled")
        } catch (e: Exception) {
            Timber.e(e, "Failed to cancel WorkManager jobs or notifications")
            // Non-critical, continue
        }
    }

    /**
     * Clears temporary files in the app's files directory.
     */
    private suspend fun wipeTempFiles() {
        try {
            // Clear any temp files created for exports, ICS files, etc.
            val filesDir = context.filesDir
            filesDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("temp_") || 
                    file.name.endsWith(".tmp") ||
                    file.name.endsWith(".ics")) {
                    file.delete()
                }
            }
            
            // Clear external cache if available
            context.externalCacheDir?.deleteRecursively()
            
            Timber.d("Temp files cleared")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear temp files")
            // Non-critical, continue
        }
    }

    /**
     * Verifies that the wipe was successful.
     * Returns true if all data appears to be cleared.
     */
    suspend fun verifyWipe(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check database is empty
            val cardCount = database.cardDao().getAllActive().count()
            if (cardCount > 0) return@withContext false

            // Check preferences are empty
            if (userPrefs.all.isNotEmpty()) return@withContext false
            if (securityPrefs.all.isNotEmpty()) return@withContext false

            // Check cache is minimal
            val cacheSize = context.cacheDir.walkTopDown().sumOf { it.length() }
            if (cacheSize > 1024 * 100) return@withContext false // More than 100KB

            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to verify wipe")
            false
        }
    }
}

