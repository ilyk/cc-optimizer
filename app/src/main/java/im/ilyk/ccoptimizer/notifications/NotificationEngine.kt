package im.ilyk.ccoptimizer.notifications

import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Notification types
 */
enum class NotifType {
    CloseWindow,
    DueSoon,
    ActionRequired
}

/**
 * Notification plan for a single notification
 */
data class NotifPlan(
    val cardId: String,
    val type: NotifType,
    val fireAt: ZonedDateTime,
    val title: String,
    val body: String,
    val requestCode: Int
)

/**
 * Plans notifications based on cycle inference results
 */
interface NotificationPlanner {
    /**
     * Generate notification plans for all cards
     */
    suspend fun planAll(): List<NotifPlan>
}

/**
 * Schedules notifications using Android AlarmManager
 */
interface NotificationScheduler {
    /**
     * Schedule notifications
     */
    suspend fun schedule(plans: List<NotifPlan>)
    
    /**
     * Cancel all scheduled notifications
     */
    suspend fun cancelAll()
    
    /**
     * Cancel notifications for a specific card
     */
    suspend fun cancelForCard(cardId: String)
}
