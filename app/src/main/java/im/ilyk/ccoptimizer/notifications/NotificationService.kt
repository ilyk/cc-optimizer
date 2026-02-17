package im.ilyk.ccoptimizer.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import im.ilyk.ccoptimizer.MainActivity
import im.ilyk.ccoptimizer.R
import im.ilyk.ccoptimizer.data.models.PaymentRecommendation
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Manages notifications for payment reminders
 * Only sends if permission is granted
 */
class NotificationService(private val context: Context) {
    
    private val notificationManager = NotificationManagerCompat.from(context)
    
    companion object {
        private const val CHANNEL_ID = "payment_reminders"
        private const val CHANNEL_NAME = "Payment Reminders"
        private const val CHANNEL_DESCRIPTION = "Notifications for upcoming credit card payments"
        
        private const val NOTIFICATION_ID_BASE = 1000
        
        @Volatile
        private var instance: NotificationService? = null
        
        fun getInstance(context: Context): NotificationService {
            return instance ?: synchronized(this) {
                instance ?: NotificationService(context.applicationContext).also { instance = it }
            }
        }
    }
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Create notification channel (Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }
            
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Send payment reminder notification
     */
    fun sendPaymentReminder(recommendation: PaymentRecommendation) {
        if (!hasNotificationPermission()) {
            android.util.Log.w("NotificationService", "No notification permission")
            return
        }
        
        val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), recommendation.recommendedPaymentDate)
        
        val title = when {
            daysUntil <= 0 -> "Payment Due Today!"
            daysUntil == 1L -> "Payment Due Tomorrow"
            else -> "Payment Due in $daysUntil Days"
        }
        
        val message = buildString {
            append("${recommendation.creditCard.name} ")
            append("(${recommendation.creditCard.lastFourDigits})\n")
            append("Amount: $${String.format("%.2f", recommendation.recommendedAmount)}\n")
            append("Due: ${recommendation.recommendedPaymentDate.format(DateTimeFormatter.ofPattern("MMM dd"))}")
        }
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_recommendations", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_BASE + recommendation.creditCard.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // You'll need to add this icon
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        try {
            notificationManager.notify(
                NOTIFICATION_ID_BASE + recommendation.creditCard.hashCode(),
                notification
            )
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationService", "Failed to send notification", e)
        }
    }
    
    /**
     * Schedule all payment reminders
     */
    fun schedulePaymentReminders(recommendations: List<PaymentRecommendation>) {
        recommendations.forEach { recommendation ->
            val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), recommendation.recommendedPaymentDate)
            
            // Schedule notifications at different intervals
            when {
                daysUntil >= 7 -> {
                    // 7 days before
                    scheduleNotification(recommendation, daysUntil = 7)
                    // 3 days before
                    scheduleNotification(recommendation, daysUntil = 3)
                    // 1 day before
                    scheduleNotification(recommendation, daysUntil = 1)
                    // On the day
                    scheduleNotification(recommendation, daysUntil = 0)
                }
                daysUntil >= 3 -> {
                    scheduleNotification(recommendation, daysUntil = 3)
                    scheduleNotification(recommendation, daysUntil = 1)
                    scheduleNotification(recommendation, daysUntil = 0)
                }
                daysUntil >= 1 -> {
                    scheduleNotification(recommendation, daysUntil = 1)
                    scheduleNotification(recommendation, daysUntil = 0)
                }
                daysUntil == 0L -> {
                    scheduleNotification(recommendation, daysUntil = 0)
                }
            }
        }
    }
    
    /**
     * Schedule notification for specific days before payment
     */
    private fun scheduleNotification(recommendation: PaymentRecommendation, daysUntil: Long) {
        val notificationDate = recommendation.recommendedPaymentDate.minusDays(daysUntil)
        val now = LocalDate.now()

        if (notificationDate.isBefore(now)) {
            return // Don't schedule past notifications
        }

        // Schedule notification using WorkManager
        im.ilyk.ccoptimizer.workers.PaymentReminderWorker.schedule(
            context,
            recommendation,
            notificationDate
        )
    }
    
    /**
     * Cancel all notifications for a card
     */
    fun cancelNotifications(cardId: String) {
        // Cancel active notification
        notificationManager.cancel(NOTIFICATION_ID_BASE + cardId.hashCode())

        // Cancel scheduled work for this card
        im.ilyk.ccoptimizer.workers.PaymentReminderWorker.cancelForCard(context, cardId)
    }

    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications() {
        // Cancel all active notifications
        notificationManager.cancelAll()

        // Cancel all scheduled work
        im.ilyk.ccoptimizer.workers.PaymentReminderWorker.cancelAll(context)
    }
    
    /**
     * Check if has notification permission
     */
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationManager.areNotificationsEnabled()
        } else {
            true
        }
    }
}

