package im.ilyk.ccoptimizer.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat

/**
 * Notification channel definitions and management
 */
object NotificationChannels {
    
    const val CLOSE_WINDOW = "close_window"
    const val DUE_SOON = "due_soon"
    const val ACTION_REQUIRED = "action_required"
    
    /**
     * Ensure all notification channels are created
     */
    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = NotificationManagerCompat.from(context)
            
            val channels = listOf(
                NotificationChannel(
                    CLOSE_WINDOW,
                    "Statement Close Window",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts when your statement is about to close (optimal payment window)"
                    enableVibration(true)
                    enableLights(true)
                },
                
                NotificationChannel(
                    DUE_SOON,
                    "Payment Due Soon",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Reminders when payments are due soon"
                    enableVibration(true)
                },
                
                NotificationChannel(
                    ACTION_REQUIRED,
                    "Action Required",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Help improve accuracy by confirming statement dates"
                }
            )
            
            channels.forEach { channel ->
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
    
    /**
     * Check if a channel is enabled
     */
    fun isChannelEnabled(context: Context, channelId: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = NotificationManagerCompat.from(context)
            val channel = notificationManager.getNotificationChannel(channelId)
            return channel?.importance != NotificationManager.IMPORTANCE_NONE
        }
        return true // Pre-O always enabled
    }
    
    /**
     * Check if notifications are enabled globally
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}
