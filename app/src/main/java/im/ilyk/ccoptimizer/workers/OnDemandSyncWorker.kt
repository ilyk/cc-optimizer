package im.ilyk.ccoptimizer.workers

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * On-demand sync worker triggered by user action
 */
class OnDemandSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    
    override suspend fun doWork(): Result {
        val force = inputData.getBoolean(KEY_FORCE, false)
        
        return try {
            // TODO: Implement sync logic with Hilt dependency injection
            android.util.Log.i("OnDemandSyncWorker", "On-demand sync executed, force=$force")
            
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
            android.util.Log.e("OnDemandSyncWorker", "Sync failed", e)
            Result.failure(
                workDataOf("error" to (e.message ?: "Unknown error"))
            )
        }
    }
    
    companion object {
        const val WORK_NAME = "ondemand_sync"
        const val KEY_FORCE = "force"
        
        /**
         * Trigger an on-demand sync
         */
        fun trigger(context: Context, force: Boolean = false) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val request = OneTimeWorkRequestBuilder<OnDemandSyncWorker>()
                .setInputData(
                    workDataOf(KEY_FORCE to force)
                )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    backoffPolicy = BackoffPolicy.EXPONENTIAL,
                    backoffDelay = 10,
                    timeUnit = TimeUnit.SECONDS
                )
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.KEEP,
                    request
                )
        }
    }
}
