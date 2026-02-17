package im.ilyk.ccoptimizer.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import im.ilyk.ccoptimizer.MainActivity
import im.ilyk.ccoptimizer.R

/**
 * Receives alarm broadcasts and displays notifications
 */
class NotificationReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "im.ilyk.ccoptimizer.NOTIFICATION") return
        
        val cardId = intent.getStringExtra("cardId") ?: return
        val typeName = intent.getStringExtra("type") ?: return
        val title = intent.getStringExtra("title") ?: "Credit Card Optimizer"
        val body = intent.getStringExtra("body") ?: "Notification"
        
        val type = try {
            NotifType.valueOf(typeName)
        } catch (e: IllegalArgumentException) {
            return
        }
        
        // Determine channel based on type
        val channelId = when (type) {
            NotifType.CloseWindow -> NotificationChannels.CLOSE_WINDOW
            NotifType.DueSoon -> NotificationChannels.DUE_SOON
            NotifType.ActionRequired -> NotificationChannels.ACTION_REQUIRED
        }
        
        // Create deep link to main activity
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("cardId", cardId)
            putExtra("screen", "card_detail")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            cardId.hashCode(),
            mainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Build notification
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(when (type) {
                NotifType.CloseWindow -> NotificationCompat.PRIORITY_HIGH
                NotifType.DueSoon -> NotificationCompat.PRIORITY_DEFAULT
                NotifType.ActionRequired -> NotificationCompat.PRIORITY_LOW
            })
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        // Generate unique notification ID
        val notificationId = generateNotificationId(cardId, type)
        
        // Show notification
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
    
    /**
     * Generate unique notification ID
     */
    private fun generateNotificationId(cardId: String, type: NotifType): Int {
        return (cardId.hashCode() and 0x00FFFFFF) or (type.ordinal shl 24)
    }
}
