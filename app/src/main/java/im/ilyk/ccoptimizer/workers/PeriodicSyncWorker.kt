package im.ilyk.ccoptimizer.workers

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Periodic background sync worker (runs every 12 hours)
 */
class PeriodicSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            // TODO: Implement sync logic with Hilt dependency injection
            android.util.Log.i("PeriodicSyncWorker", "Periodic sync executed")
            
            // Return success with metrics
            Result.success(
                workDataOf(
                    "durationMs" to 0L,
                    "cards" to 0,
                    "transactions" to 0,
                    "balances" to 0,
                    "fromCache" to false
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("PeriodicSyncWorker", "Sync failed", e)
            Result.failure(
                workDataOf("error" to (e.message ?: "Unknown error"))
            )
        }
    }
    
    companion object {
        const val WORK_NAME = "periodic_sync"
        
        /**
         * Schedule periodic sync every 12 hours
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
            
            val request = PeriodicWorkRequestBuilder<PeriodicSyncWorker>(
                repeatInterval = 12,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    backoffPolicy = BackoffPolicy.EXPONENTIAL,
                    backoffDelay = 30,
                    timeUnit = TimeUnit.SECONDS
                )
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }
    }
}
