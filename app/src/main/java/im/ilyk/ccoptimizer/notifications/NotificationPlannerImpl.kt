package im.ilyk.ccoptimizer.notifications

import im.ilyk.ccoptimizer.data.db.dao.CardDao
import im.ilyk.ccoptimizer.data.db.dao.CycleDao
import kotlinx.coroutines.flow.first
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of notification planner
 * 
 * Computes notification fire times based on:
 * - Next close dates from cycle inference
 * - Buffer days from preferences
 * - Quiet hours configuration
 * - Confidence thresholds
 */
@Singleton
class NotificationPlannerImpl @Inject constructor(
    private val cardDao: CardDao,
    private val cycleDao: CycleDao,
    private val notifPrefs: NotifPrefs,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : NotificationPlanner {
    
    companion object {
        private const val DUE_OFFSET_DAYS = 25 // Days between close and due (from Story 2.1)
        private const val LOW_CONFIDENCE_THRESHOLD = 0.6
    }
    
    override suspend fun planAll(): List<NotifPlan> {
        val plans = mutableListOf<NotifPlan>()
        
        // Get all active cards
        val cards = cardDao.getAllActive()
        
        // Get buffer settings
        val bufferCloseDays = notifPrefs.getBufferCloseDays()
        val bufferDueDays = notifPrefs.getBufferDueDays()
        val quietHours = notifPrefs.getQuietHours()
        
        val now = ZonedDateTime.now(zoneId)
        
        for (card in cards) {
            val cycle = cycleDao.latest(card.cardId) ?: continue
            
            // Check if we need action required notification
            if (cycle.closeDay == null || cycle.confidence < LOW_CONFIDENCE_THRESHOLD) {
                val actionRequiredPlan = NotifPlan(
                    cardId = card.cardId,
                    type = NotifType.ActionRequired,
                    fireAt = now.plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0),
                    title = "Help Improve Accuracy",
                    body = "Please confirm the statement close date for ${card.displayName ?: "your card"}",
                    requestCode = generateRequestCode(card.cardId, NotifType.ActionRequired)
                )
                plans.add(adjustForQuietHours(actionRequiredPlan, quietHours))
                continue // Skip other notifications if we need confirmation
            }
            
            val nextCloseDate = cycle.nextCloseDate ?: continue
            
            // Plan close window notification
            val closeWindowAt = nextCloseDate.minusDays(bufferCloseDays.toLong())
                .atTime(9, 0) // 9 AM
                .atZone(zoneId)
            
            if (closeWindowAt.isAfter(now)) {
                val closeWindowPlan = NotifPlan(
                    cardId = card.cardId,
                    type = NotifType.CloseWindow,
                    fireAt = closeWindowAt,
                    title = "Statement Closes Soon",
                    body = "${card.displayName ?: "Your card"} statement closes on ${nextCloseDate}. Pay now to reduce utilization!",
                    requestCode = generateRequestCode(card.cardId, NotifType.CloseWindow)
                )
                plans.add(adjustForQuietHours(closeWindowPlan, quietHours))
            }
            
            // Plan due soon notification
            val dueDate = nextCloseDate.plusDays(DUE_OFFSET_DAYS.toLong())
            val dueSoonAt = dueDate.minusDays(bufferDueDays.toLong())
                .atTime(10, 0) // 10 AM
                .atZone(zoneId)
            
            if (dueSoonAt.isAfter(now)) {
                val dueSoonPlan = NotifPlan(
                    cardId = card.cardId,
                    type = NotifType.DueSoon,
                    fireAt = dueSoonAt,
                    title = "Payment Due Soon",
                    body = "${card.displayName ?: "Your card"} payment is due on ${dueDate}. Don't forget to pay!",
                    requestCode = generateRequestCode(card.cardId, NotifType.DueSoon)
                )
                plans.add(adjustForQuietHours(dueSoonPlan, quietHours))
            }
        }
        
        return plans
    }
    
    /**
     * Adjust notification time to be outside quiet hours
     */
    private fun adjustForQuietHours(plan: NotifPlan, quietHours: QuietHours): NotifPlan {
        val fireTime = plan.fireAt.toLocalTime()
        
        // Check if fire time falls within quiet hours
        val inQuietHours = if (quietHours.start.isBefore(quietHours.end)) {
            // Normal case: 22:00 - 07:00
            fireTime.isAfter(quietHours.start) || fireTime.isBefore(quietHours.end)
        } else {
            // Crosses midnight: 23:00 - 01:00
            fireTime.isAfter(quietHours.start) && fireTime.isBefore(quietHours.end)
        }
        
        if (!inQuietHours) {
            return plan // No adjustment needed
        }
        
        // Move to end of quiet hours
        val adjustedFireAt = plan.fireAt
            .withHour(quietHours.end.hour)
            .withMinute(quietHours.end.minute)
            .withSecond(0)
            .withNano(0)
        
        return plan.copy(fireAt = adjustedFireAt)
    }
    
    /**
     * Generate unique request code for AlarmManager
     */
    private fun generateRequestCode(cardId: String, type: NotifType): Int {
        // Combine card ID hash with type ordinal for uniqueness
        val cardHash = cardId.hashCode() and 0x00FFFFFF // 24 bits
        val typeCode = type.ordinal and 0xFF // 8 bits
        return (cardHash shl 8) or typeCode
    }
}
