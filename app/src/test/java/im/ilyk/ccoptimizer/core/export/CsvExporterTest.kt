package im.ilyk.ccoptimizer.core.export

import im.ilyk.ccoptimizer.data.db.entities.BalanceEntity
import im.ilyk.ccoptimizer.data.db.entities.CardEntity
import im.ilyk.ccoptimizer.data.db.entities.CycleEntity
import im.ilyk.ccoptimizer.data.db.entities.PaymentEntity
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Unit tests for CsvExporter
 */
class CsvExporterTest {
    
    @Test
    fun `export generates valid CSV structure`() {
        // Given
        val cards = listOf(
            CardEntity(
                cardId = "card_1",
                issuer = "Chase",
                last4 = "1234",
                displayName = "Freedom",
                openedAt = LocalDate.of(2023, 1, 1),
                isActive = true
            )
        )
        
        val cycles = listOf(
            CycleEntity(
                id = 1,
                cardId = "card_1",
                closeDay = 15,
                cadence = "MONTHLY",
                confidence = 0.95,
                nextCloseDate = LocalDate.of(2025, 11, 15),
                candidateDaysJson = "{}",
                signalWeightsJson = "{}",
                computedAt = ZonedDateTime.now()
            )
        )
        
        val balances = listOf(
            BalanceEntity(
                id = 1,
                cardId = "card_1",
                date = LocalDate.of(2025, 10, 15),
                balance = 500.50
            )
        )
        
        val payments = listOf(
            PaymentEntity(
                cardId = "card_1",
                postedDate = LocalDate.of(2025, 10, 10),
                amountCents = 25000,
                createdAt = Instant.now(),
                source = "manual"
            )
        )
        
        // When
        val csv = CsvExporter.export(cards, cycles, balances, payments)
        
        // Then
        assertTrue(csv.contains("[Cards]"))
        assertTrue(csv.contains("[Cycles]"))
        assertTrue(csv.contains("[Balances]"))
        assertTrue(csv.contains("[Payments]"))
        
        assertTrue(csv.contains("card_1"))
        assertTrue(csv.contains("Chase"))
        assertTrue(csv.contains("1234"))
        assertTrue(csv.contains("Freedom"))
    }
    
    @Test
    fun `export handles empty lists`() {
        // Given
        val cards = emptyList<CardEntity>()
        val cycles = emptyList<CycleEntity>()
        val balances = emptyList<BalanceEntity>()
        val payments = emptyList<PaymentEntity>()
        
        // When
        val csv = CsvExporter.export(cards, cycles, balances, payments)
        
        // Then
        assertTrue(csv.contains("[Cards]"))
        assertTrue(csv.contains("[Cycles]"))
        assertTrue(csv.contains("[Balances]"))
        assertTrue(csv.contains("[Payments]"))
        
        // Should have headers but no data rows
        val cardLines = csv.split("[Cycles]")[0].split("\n").filter { it.isNotBlank() }
        assertEquals(2, cardLines.size) // Section header + column header
    }
    
    @Test
    fun `export escapes commas in fields`() {
        // Given
        val cards = listOf(
            CardEntity(
                cardId = "card_1",
                issuer = "Bank of America, N.A.",
                last4 = "1234",
                displayName = "Travel Rewards, Premium",
                openedAt = null,
                isActive = true
            )
        )
        
        // When
        val csv = CsvExporter.export(cards, emptyList(), emptyList(), emptyList())
        
        // Then
        assertTrue(csv.contains("\"Bank of America, N.A.\""))
        assertTrue(csv.contains("\"Travel Rewards, Premium\""))
    }
    
    @Test
    fun `export escapes quotes in fields`() {
        // Given
        val cards = listOf(
            CardEntity(
                cardId = "card_1",
                issuer = "Test \"Bank\"",
                last4 = "1234",
                displayName = "Card with \"quotes\"",
                openedAt = null,
                isActive = true
            )
        )
        
        // When
        val csv = CsvExporter.export(cards, emptyList(), emptyList(), emptyList())
        
        // Then
        assertTrue(csv.contains("\"Test \"\"Bank\"\"\""))
        assertTrue(csv.contains("\"Card with \"\"quotes\"\"\""))
    }
    
    @Test
    fun `export formats numbers correctly`() {
        // Given
        val balances = listOf(
            BalanceEntity(
                id = 1,
                cardId = "card_1",
                date = LocalDate.of(2025, 10, 15),
                balance = 1234.56
            )
        )
        
        val cycles = listOf(
            CycleEntity(
                id = 1,
                cardId = "card_1",
                closeDay = 15,
                cadence = "MONTHLY",
                confidence = 0.9567,
                nextCloseDate = null,
                candidateDaysJson = "{}",
                signalWeightsJson = "{}",
                computedAt = ZonedDateTime.now()
            )
        )
        
        // When
        val csv = CsvExporter.export(emptyList(), cycles, balances, emptyList())
        
        // Then
        assertTrue(csv.contains("1234.56"))
        assertTrue(csv.contains("0.9567"))
    }
    
    @Test
    fun `export handles null values`() {
        // Given
        val cards = listOf(
            CardEntity(
                cardId = "card_1",
                issuer = null,
                last4 = null,
                displayName = null,
                openedAt = null,
                isActive = false
            )
        )
        
        // When
        val csv = CsvExporter.export(cards, emptyList(), emptyList(), emptyList())
        
        // Then
        // Should contain empty fields (consecutive commas)
        val cardRow = csv.split("\n").find { it.startsWith("card_1") }
        assertNotNull(cardRow)
        assertTrue(cardRow!!.contains(",,")) // Empty fields
    }
    
    @Test
    fun `generateFilename returns valid format`() {
        // When
        val filename = CsvExporter.generateFilename()
        
        // Then
        assertTrue(filename.startsWith("credit-planner_export_"))
        assertTrue(filename.endsWith(".csv"))
        assertTrue(filename.matches(Regex("credit-planner_export_\\d{8}\\.csv")))
    }
}


