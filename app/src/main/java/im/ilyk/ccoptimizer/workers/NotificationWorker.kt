package im.ilyk.ccoptimizer.workers

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Daily worker that recomputes and schedules notifications
 */
class NotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            // TODO: Implement notification logic with Hilt dependency injection
            android.util.Log.i("NotificationWorker", "Notification worker executed")
            
            Result.success(
                workDataOf(
                    "plansScheduled" to 0
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("NotificationWorker", "Notification planning failed", e)
            Result.failure(
                workDataOf("error" to (e.message ?: "Unknown error"))
            )
        }
    }
    
    companion object {
        const val WORK_NAME = "daily_notification_planning"
        
        /**
         * Schedule daily notification planning
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()
            
            // Run daily at 8 AM
            val request = PeriodicWorkRequestBuilder<NotificationWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }
        
        /**
         * Calculate delay until next 8 AM
         */
        private fun calculateInitialDelay(): Long {
            val now = java.time.ZonedDateTime.now()
            var next8am = now.withHour(8).withMinute(0).withSecond(0).withNano(0)
            
            if (now.isAfter(next8am)) {
                next8am = next8am.plusDays(1)
            }
            
            return java.time.Duration.between(now, next8am).toMillis()
        }
    }
}
