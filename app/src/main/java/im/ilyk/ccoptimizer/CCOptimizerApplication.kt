package im.ilyk.ccoptimizer

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import im.ilyk.ccoptimizer.core.logging.RedactingTree
import im.ilyk.ccoptimizer.notifications.NotificationChannels
import im.ilyk.ccoptimizer.workers.NotificationWorker
import im.ilyk.ccoptimizer.workers.PeriodicSyncWorker
import timber.log.Timber
import javax.inject.Inject

/**
 * Application class with Hilt dependency injection and privacy-preserving logging
 */
@HiltAndroidApp
class CCOptimizerApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    override fun onCreate() {
        super.onCreate()
        
        // Set up privacy-preserving logging
        setupLogging()
        
        Timber.i("CC Optimizer starting (version ${BuildConfig.VERSION_NAME})")
        
        // Create notification channels
        NotificationChannels.ensureCreated(this)
        
        // Schedule periodic background sync
        PeriodicSyncWorker.schedule(this)
        
        // Schedule daily notification planning
        NotificationWorker.schedule(this)
        
        Timber.i("Application initialization complete")
    }
    
    /**
     * Configure Timber with PII redaction.
     * In debug: RedactingTree that strips sensitive data
     * In release: No logging tree (no logs at all)
     */
    private fun setupLogging() {
        if (BuildConfig.DEBUG) {
            // Debug builds: plant RedactingTree to strip PII from logs
            Timber.plant(RedactingTree())
            Timber.d("Timber initialized with PII redaction")
        } else {
            // Release builds: No logging at all to prevent any data leakage
            // Timber calls will be no-ops
            Timber.d("Release build: Timber logging disabled")
        }
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
