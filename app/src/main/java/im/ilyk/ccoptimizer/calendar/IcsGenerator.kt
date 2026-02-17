package im.ilyk.ccoptimizer.calendar

import im.ilyk.ccoptimizer.data.db.entities.CardEntity
import im.ilyk.ccoptimizer.data.db.entities.CycleEntity
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Generates ICS (iCalendar) files for credit card payment reminders
 */
class IcsGenerator {
    
    companion object {
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
        private val DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
    }
    
    /**
     * Generate ICS file content for all cards
     */
    fun generate(
        cards: List<CardEntity>,
        cycles: Map<String, CycleEntity>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        val builder = StringBuilder()
        
        // ICS header
        builder.appendLine("BEGIN:VCALENDAR")
        builder.appendLine("VERSION:2.0")
        builder.appendLine("PRODID:-//Credit Card Optimizer//Payment Calendar//EN")
        builder.appendLine("CALSCALE:GREGORIAN")
        builder.appendLine("METHOD:PUBLISH")
        builder.appendLine("X-WR-CALNAME:Credit Card Payment Schedule")
        builder.appendLine("X-WR-CALDESC:Statement close dates and payment due dates")
        builder.appendLine("X-WR-TIMEZONE:${zoneId.id}")
        
        // Generate events for each card
        for (card in cards) {
            val cycle = cycles[card.cardId] ?: continue
            val closeDay = cycle.closeDay ?: continue
            val nextCloseDate = cycle.nextCloseDate ?: continue
            
            // Add close date event
            builder.append(generateCloseEvent(card, nextCloseDate, zoneId))
            
            // Add due date event (25 days after close)
            val dueDate = nextCloseDate.plusDays(25)
            builder.append(generateDueEvent(card, dueDate, zoneId))
        }
        
        // ICS footer
        builder.appendLine("END:VCALENDAR")
        
        return builder.toString()
    }
    
    /**
     * Generate statement close event
     */
    private fun generateCloseEvent(
        card: CardEntity,
        closeDate: LocalDate,
        zoneId: ZoneId
    ): String {
        val uid = UUID.nameUUIDFromBytes("${card.cardId}_close_${closeDate}".toByteArray())
        val now = java.time.ZonedDateTime.now(zoneId)
        
        return buildString {
            appendLine("BEGIN:VEVENT")
            appendLine("UID:$uid")
            appendLine("DTSTAMP:${now.format(DATETIME_FORMAT)}")
            appendLine("DTSTART;VALUE=DATE:${closeDate.format(DATE_FORMAT)}")
            appendLine("SUMMARY:${card.displayName ?: "Card"} Statement Closes")
            appendLine("DESCRIPTION:Statement closes for ${card.displayName ?: card.cardId}. Pay before this date to reduce reported utilization.")
            appendLine("STATUS:CONFIRMED")
            appendLine("TRANSP:TRANSPARENT")
            appendLine("CATEGORIES:PAYMENT,CREDIT_CARD")
            appendLine("CLASS:PRIVATE")
            appendLine("END:VEVENT")
        }
    }
    
    /**
     * Generate payment due event
     */
    private fun generateDueEvent(
        card: CardEntity,
        dueDate: LocalDate,
        zoneId: ZoneId
    ): String {
        val uid = UUID.nameUUIDFromBytes("${card.cardId}_due_${dueDate}".toByteArray())
        val now = java.time.ZonedDateTime.now(zoneId)
        
        return buildString {
            appendLine("BEGIN:VEVENT")
            appendLine("UID:$uid")
            appendLine("DTSTAMP:${now.format(DATETIME_FORMAT)}")
            appendLine("DTSTART;VALUE=DATE:${dueDate.format(DATE_FORMAT)}")
            appendLine("SUMMARY:${card.displayName ?: "Card"} Payment Due")
            appendLine("DESCRIPTION:Payment due for ${card.displayName ?: card.cardId}. Avoid late fees!")
            appendLine("STATUS:CONFIRMED")
            appendLine("TRANSP:TRANSPARENT")
            appendLine("CATEGORIES:PAYMENT,CREDIT_CARD")
            appendLine("CLASS:PRIVATE")
            appendLine("BEGIN:VALARM")
            appendLine("TRIGGER:-PT2H") // Reminder 2 hours before
            appendLine("ACTION:DISPLAY")
            appendLine("DESCRIPTION:Payment due soon")
            appendLine("END:VALARM")
            appendLine("END:VEVENT")
        }
    }
    
    /**
     * Generate ICS for a single card with recurring events
     */
    fun generateRecurring(
        card: CardEntity,
        cycle: CycleEntity,
        monthsAhead: Int = 12,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        val builder = StringBuilder()
        
        builder.appendLine("BEGIN:VCALENDAR")
        builder.appendLine("VERSION:2.0")
        builder.appendLine("PRODID:-//Credit Card Optimizer//Payment Calendar//EN")
        builder.appendLine("CALSCALE:GREGORIAN")
        
        val closeDay = cycle.closeDay ?: return ""
        var currentDate = cycle.nextCloseDate ?: LocalDate.now().withDayOfMonth(closeDay)
        
        // Generate events for next N months
        repeat(monthsAhead) {
            builder.append(generateCloseEvent(card, currentDate, zoneId))
            builder.append(generateDueEvent(card, currentDate.plusDays(25), zoneId))
            
            // Advance to next month
            currentDate = currentDate.plusMonths(1)
                .withDayOfMonth(minOf(closeDay, currentDate.plusMonths(1).lengthOfMonth()))
        }
        
        builder.appendLine("END:VCALENDAR")
        
        return builder.toString()
    }
}
