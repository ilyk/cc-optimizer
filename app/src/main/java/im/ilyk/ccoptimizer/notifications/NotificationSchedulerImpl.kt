package im.ilyk.ccoptimizer.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import im.ilyk.ccoptimizer.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules notifications using AlarmManager for precise timing
 */
@Singleton
class NotificationSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationScheduler {
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager = NotificationManagerCompat.from(context)
    
    override suspend fun schedule(plans: List<NotifPlan>) {
        for (plan in plans) {
            scheduleSingle(plan)
        }
    }
    
    override suspend fun cancelAll() {
        // Cancel all pending intents
        // In production, you'd track all request codes
        // For now, this is a placeholder
    }
    
    override suspend fun cancelForCard(cardId: String) {
        // Cancel notifications for a specific card
        NotifType.values().forEach { type ->
            val requestCode = generateRequestCode(cardId, type)
            val intent = createNotificationIntent(cardId, type)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            
            pendingIntent?.let { alarmManager.cancel(it) }
        }
    }
    
    /**
     * Schedule a single notification
     */
    private fun scheduleSingle(plan: NotifPlan) {
        val intent = createNotificationIntent(plan.cardId, plan.type).apply {
            putExtra("title", plan.title)
            putExtra("body", plan.body)
            putExtra("cardId", plan.cardId)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            plan.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val triggerAtMillis = plan.fireAt.toInstant().toEpochMilli()
        
        // Use setExactAndAllowWhileIdle for precise timing
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }
    
    /**
     * Create notification intent
     */
    private fun createNotificationIntent(cardId: String, type: NotifType): Intent {
        return Intent(context, NotificationReceiver::class.java).apply {
            action = "im.ilyk.ccoptimizer.NOTIFICATION"
            putExtra("cardId", cardId)
            putExtra("type", type.name)
        }
    }
    
    /**
     * Generate unique request code (same logic as planner)
     */
    private fun generateRequestCode(cardId: String, type: NotifType): Int {
        val cardHash = cardId.hashCode() and 0x00FFFFFF
        val typeCode = type.ordinal and 0xFF
        return (cardHash shl 8) or typeCode
    }
}
