package im.ilyk.ccoptimizer.calendar

import im.ilyk.ccoptimizer.data.db.entities.CardEntity
import im.ilyk.ccoptimizer.data.db.entities.CycleEntity
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class IcsGeneratorTest {
    
    private val generator = IcsGenerator()
    private val pst = ZoneId.of("America/Los_Angeles")
    
    @Test
    fun `generate creates valid ICS header`() {
        val ics = generator.generate(emptyList(), emptyMap(), pst)
        
        assertTrue(ics.contains("BEGIN:VCALENDAR"))
        assertTrue(ics.contains("VERSION:2.0"))
        assertTrue(ics.contains("END:VCALENDAR"))
        assertTrue(ics.contains("PRODID"))
    }
    
    @Test
    fun `generate includes card close date event`() {
        val card = CardEntity("card1", "Chase", "1234", "Chase Sapphire", null, true)
        val cycle = CycleEntity(
            cardId = "card1",
            closeDay = 15,
            cadence = "MONTHLY",
            confidence = 0.85,
            nextCloseDate = LocalDate.of(2024, 4, 15),
            candidateDaysJson = "{}",
            signalWeightsJson = "{}",
            computedAt = ZonedDateTime.now()
        )
        
        val ics = generator.generate(listOf(card), mapOf("card1" to cycle), pst)
        
        assertTrue(ics.contains("BEGIN:VEVENT"))
        assertTrue(ics.contains("Statement Closes"))
        assertTrue(ics.contains("20240415")) // Date in YYYYMMDD format
        assertTrue(ics.contains("END:VEVENT"))
    }
    
    @Test
    fun `generate includes payment due date event`() {
        val card = CardEntity("card1", "Chase", "1234", "Chase Sapphire", null, true)
        val cycle = CycleEntity(
            cardId = "card1",
            closeDay = 15,
            cadence = "MONTHLY",
            confidence = 0.85,
            nextCloseDate = LocalDate.of(2024, 4, 15),
            candidateDaysJson = "{}",
            signalWeightsJson = "{}",
            computedAt = ZonedDateTime.now()
        )
        
        val ics = generator.generate(listOf(card), mapOf("card1" to cycle), pst)
        
        assertTrue(ics.contains("Payment Due"))
        // Due date should be 25 days after close
        assertTrue(ics.contains("20240510")) // April 15 + 25 days = May 10
    }
    
    @Test
    fun `generate handles multiple cards`() {
        val cards = listOf(
            CardEntity("card1", "Chase", "1111", "Card 1", null, true),
            CardEntity("card2", "Citi", "2222", "Card 2", null, true)
        )
        
        val cycles = mapOf(
            "card1" to CycleEntity(
                cardId = "card1",
                closeDay = 15,
                cadence = "MONTHLY",
                confidence = 0.85,
                nextCloseDate = LocalDate.of(2024, 4, 15),
                candidateDaysJson = "{}",
                signalWeightsJson = "{}",
                computedAt = ZonedDateTime.now()
            ),
            "card2" to CycleEntity(
                cardId = "card2",
                closeDay = 20,
                cadence = "MONTHLY",
                confidence = 0.90,
                nextCloseDate = LocalDate.of(2024, 4, 20),
                candidateDaysJson = "{}",
                signalWeightsJson = "{}",
                computedAt = ZonedDateTime.now()
            )
        )
        
        val ics = generator.generate(cards, cycles, pst)
        
        // Should have events for both cards
        val eventCount = ics.split("BEGIN:VEVENT").size - 1
        assertEquals(4, eventCount) // 2 cards × (close + due) = 4 events
    }
    
    @Test
    fun `generate skips cards without cycle data`() {
        val card = CardEntity("card1", "Chase", "1234", "Chase Sapphire", null, true)
        
        val ics = generator.generate(listOf(card), emptyMap(), pst)
        
        // Should have header/footer but no events
        assertTrue(ics.contains("BEGIN:VCALENDAR"))
        assertTrue(ics.contains("END:VCALENDAR"))
        assertFalse(ics.contains("BEGIN:VEVENT"))
    }
    
    @Test
    fun `generate skips cards with null close day`() {
        val card = CardEntity("card1", "Chase", "1234", "Chase Sapphire", null, true)
        val cycle = CycleEntity(
            cardId = "card1",
            closeDay = null, // No close day
            cadence = "MONTHLY",
            confidence = 0.3,
            nextCloseDate = null,
            candidateDaysJson = "{}",
            signalWeightsJson = "{}",
            computedAt = ZonedDateTime.now()
        )
        
        val ics = generator.generate(listOf(card), mapOf("card1" to cycle), pst)
        
        assertFalse(ics.contains("BEGIN:VEVENT"))
    }
    
    @Test
    fun `generateRecurring creates events for future months`() {
        val card = CardEntity("card1", "Chase", "1234", "Chase Sapphire", null, true)
        val cycle = CycleEntity(
            cardId = "card1",
            closeDay = 15,
            cadence = "MONTHLY",
            confidence = 0.85,
            nextCloseDate = LocalDate.of(2024, 4, 15),
            candidateDaysJson = "{}",
            signalWeightsJson = "{}",
            computedAt = ZonedDateTime.now()
        )
        
        val ics = generator.generateRecurring(card, cycle, monthsAhead = 6, pst)
        
        // Should have 6 months × 2 events (close + due) = 12 events
        val eventCount = ics.split("BEGIN:VEVENT").size - 1
        assertEquals(12, eventCount)
    }
    
    @Test
    fun `ics includes VALARM for due events`() {
        val card = CardEntity("card1", "Chase", "1234", "Chase Sapphire", null, true)
        val cycle = CycleEntity(
            cardId = "card1",
            closeDay = 15,
            cadence = "MONTHLY",
            confidence = 0.85,
            nextCloseDate = LocalDate.of(2024, 4, 15),
            candidateDaysJson = "{}",
            signalWeightsJson = "{}",
            computedAt = ZonedDateTime.now()
        )
        
        val ics = generator.generate(listOf(card), mapOf("card1" to cycle), pst)
        
        // Due date events should have alarms
        assertTrue(ics.contains("BEGIN:VALARM"))
        assertTrue(ics.contains("TRIGGER:-PT2H")) // 2 hours before
        assertTrue(ics.contains("END:VALARM"))
    }
    
    @Test
    fun `ics events have unique UIDs`() {
        val card = CardEntity("card1", "Chase", "1234", "Chase Sapphire", null, true)
        val cycle = CycleEntity(
            cardId = "card1",
            closeDay = 15,
            cadence = "MONTHLY",
            confidence = 0.85,
            nextCloseDate = LocalDate.of(2024, 4, 15),
            candidateDaysJson = "{}",
            signalWeightsJson = "{}",
            computedAt = ZonedDateTime.now()
        )
        
        val ics = generator.generate(listOf(card), mapOf("card1" to cycle), pst)
        
        val uidCount = ics.split("UID:").size - 1
        assertTrue(uidCount >= 2) // At least close + due events
        
        // Extract UIDs
        val uids = ics.lines()
            .filter { it.startsWith("UID:") }
            .map { it.substring(4) }
        
        // All should be unique
        assertEquals(uids.size, uids.toSet().size)
    }
}
