package im.ilyk.ccoptimizer.core.inference

import im.ilyk.ccoptimizer.data.db.entities.BalanceEntity
import im.ilyk.ccoptimizer.data.db.entities.CycleEntity
import im.ilyk.ccoptimizer.data.db.entities.PaymentEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime

/**
 * Unit tests for UtilizationCalc
 */
class UtilizationCalcTest {
    
    @Test
    fun `perCardMonthly calculates utilization correctly`() {
        // Given: $500 balance, $5000 limit = 10% utilization
        val month = YearMonth.of(2025, 10)
        val closeDay = 15
        val closeDate = LocalDate.of(2025, 10, 15)
        
        val balances = listOf(
            BalanceEntity(
                cardId = "card_123",
                date = closeDate,
                balance = 500.0 // $500
            )
        )
        
        val payments = emptyList<PaymentEntity>()
        
        val cycle = CycleEntity(
            cardId = "card_123",
            closeDay = closeDay,
            cadence = "MONTHLY",
            confidence = 0.95,
            nextCloseDate = closeDate,
            candidateDaysJson = "{}",
            signalWeightsJson = "{}",
            computedAt = ZonedDateTime.now()
        )
        
        val creditLimitCents = 500000L // $5000
        
        // When
        val result = UtilizationCalc.perCardMonthly(
            month, balances, payments, cycle, creditLimitCents
        )
        
        // Then
        assertEquals(month, result.month)
        assertEquals(10.0, result.percent, 0.01)
    }
    
    @Test
    fun `perCardMonthly subtracts payments before close date`() {
        // Given: $1000 balance, $300 payment = $700 statement balance
        // $700 / $5000 = 14%
        val month = YearMonth.of(2025, 10)
        val closeDay = 15
        val closeDate = LocalDate.of(2025, 10, 15)
        
        val balances = listOf(
            BalanceEntity(
                cardId = "card_123",
                date = closeDate,
                balance = 1000.0
            )
        )
        
        val payments = listOf(
            PaymentEntity(
                cardId = "card_123",
                postedDate = LocalDate.of(2025, 10, 10), // before close
                amountCents = 30000L, // $300
                createdAt = Instant.now(),
                source = "manual"
            )
        )
        
        val cycle = CycleEntity(
            cardId = "card_123",
            closeDay = closeDay,
            cadence = "MONTHLY",
            confidence = 0.95,
            nextCloseDate = closeDate,
            candidateDaysJson = "{}",
            signalWeightsJson = "{}",
            computedAt = ZonedDateTime.now()
        )
        
        val creditLimitCents = 500000L
        
        // When
        val result = UtilizationCalc.perCardMonthly(
            month, balances, payments, cycle, creditLimitCents
        )
        
        // Then
        assertEquals(14.0, result.percent, 0.01)
    }
    
    @Test
    fun `perCardMonthly ignores payments after close date`() {
        // Given: $1000 balance, $300 payment AFTER close = still $1000 statement balance
        val month = YearMonth.of(2025, 10)
        val closeDay = 15
        val closeDate = LocalDate.of(2025, 10, 15)
        
        val balances = listOf(
            BalanceEntity(
                cardId = "card_123",
                date = closeDate,
                balance = 1000.0
            )
        )
        
        val payments = listOf(
            PaymentEntity(
                cardId = "card_123",
                postedDate = LocalDate.of(2025, 10, 20), // AFTER close
                amountCents = 30000L,
                createdAt = Instant.now(),
                source = "manual"
            )
        )
        
        val cycle = CycleEntity(
            cardId = "card_123",
            closeDay = closeDay,
            cadence = "MONTHLY",
            confidence = 0.95,
            nextCloseDate = closeDate,
            candidateDaysJson = "{}",
            signalWeightsJson = "{}",
            computedAt = ZonedDateTime.now()
        )
        
        val creditLimitCents = 500000L
        
        // When
        val result = UtilizationCalc.perCardMonthly(
            month, balances, payments, cycle, creditLimitCents
        )
        
        // Then
        // $1000 / $5000 = 20%
        assertEquals(20.0, result.percent, 0.01)
    }
    
    @Test
    fun `perCardMonthly includes payments on close date`() {
        // Given: payment on exact close date should be counted
        val month = YearMonth.of(2025, 10)
        val closeDay = 15
        val closeDate = LocalDate.of(2025, 10, 15)
        
        val balances = listOf(
            BalanceEntity(
                cardId = "card_123",
                date = closeDate,
                balance = 1000.0
            )
        )
        
        val payments = listOf(
            PaymentEntity(
                cardId = "card_123",
                postedDate = closeDate, // ON close date
                amountCents = 50000L, // $500
                createdAt = Instant.now(),
                source = "manual"
            )
        )
        
        val cycle = CycleEntity(
            cardId = "card_123",
            closeDay = closeDay,
            cadence = "MONTHLY",
            confidence = 0.95,
            nextCloseDate = closeDate,
            candidateDaysJson = "{}",
            signalWeightsJson = "{}",
            computedAt = ZonedDateTime.now()
        )
        
        val creditLimitCents = 500000L
        
        // When
        val result = UtilizationCalc.perCardMonthly(
            month, balances, payments, cycle, creditLimitCents
        )
        
        // Then
        // ($1000 - $500) / $5000 = 10%
        assertEquals(10.0, result.percent, 0.01)
    }
    
    @Test
    fun `perCardMonthly handles zero limit gracefully`() {
        // Given: zero credit limit
        val month = YearMonth.of(2025, 10)
        val closeDate = LocalDate.of(2025, 10, 15)
        
        val balances = listOf(
            BalanceEntity(
                cardId = "card_123",
                date = closeDate,
                balance = 500.0
            )
        )
        
        val cycle = CycleEntity(
            cardId = "card_123",
            closeDay = 15,
            cadence = "MONTHLY",
            confidence = 0.95,
            nextCloseDate = closeDate,
            candidateDaysJson = "{}",
            signalWeightsJson = "{}",
            computedAt = ZonedDateTime.now()
        )
        
        val creditLimitCents = 0L
        
        // When
        val result = UtilizationCalc.perCardMonthly(
            month, balances, emptyList(), cycle, creditLimitCents
        )
        
        // Then
        assertEquals(0.0, result.percent, 0.01)
    }
    
    @Test
    fun `perCardMonthly handles missing balance data`() {
        // Given: no balance on close date
        val month = YearMonth.of(2025, 10)
        val closeDate = LocalDate.of(2025, 10, 15)
        
        val balances = listOf(
            BalanceEntity(
                cardId = "card_123",
                date = LocalDate.of(2025, 10, 10), // different date
                balance = 500.0
            )
        )
        
        val cycle = CycleEntity(
            cardId = "card_123",
            closeDay = 15,
            cadence = "MONTHLY",
            confidence = 0.95,
            nextCloseDate = closeDate,
            candidateDaysJson = "{}",
            signalWeightsJson = "{}",
            computedAt = ZonedDateTime.now()
        )
        
        val creditLimitCents = 500000L
        
        // When
        val result = UtilizationCalc.perCardMonthly(
            month, balances, emptyList(), cycle, creditLimitCents
        )
        
        // Then: should default to 0% when balance not found
        assertEquals(0.0, result.percent, 0.01)
    }
    
    @Test
    fun `perCardMonthly prevents negative utilization`() {
        // Given: payments exceed balance (should floor at 0)
        val month = YearMonth.of(2025, 10)
        val closeDate = LocalDate.of(2025, 10, 15)
        
        val balances = listOf(
            BalanceEntity(
                cardId = "card_123",
                date = closeDate,
                balance = 100.0 // $100
            )
        )
        
        val payments = listOf(
            PaymentEntity(
                cardId = "card_123",
                postedDate = closeDate,
                amountCents = 50000L, // $500 payment (more than balance!)
                createdAt = Instant.now(),
                source = "manual"
            )
        )
        
        val cycle = CycleEntity(
            cardId = "card_123",
            closeDay = 15,
            cadence = "MONTHLY",
            confidence = 0.95,
            nextCloseDate = closeDate,
            candidateDaysJson = "{}",
            signalWeightsJson = "{}",
            computedAt = ZonedDateTime.now()
        )
        
        val creditLimitCents = 500000L
        
        // When
        val result = UtilizationCalc.perCardMonthly(
            month, balances, payments, cycle, creditLimitCents
        )
        
        // Then: should be 0%, not negative
        assertEquals(0.0, result.percent, 0.01)
    }
    
    @Test
    fun `lastNMonths returns correct number of months`() {
        // Given
        val currentMonth = YearMonth.of(2025, 10)
        val closeDate = LocalDate.of(2025, 10, 15)
        
        val balances = listOf(
            BalanceEntity(
                cardId = "card_123",
                date = closeDate,
                balance = 500.0
            )
        )
        
        val cycle = CycleEntity(
            cardId = "card_123",
            closeDay = 15,
            cadence = "MONTHLY",
            confidence = 0.95,
            nextCloseDate = closeDate,
            candidateDaysJson = "{}",
            signalWeightsJson = "{}",
            computedAt = ZonedDateTime.now()
        )
        
        val creditLimitCents = 500000L
        
        // When
        val result = UtilizationCalc.lastNMonths(
            currentMonth, 12, balances, emptyList(), cycle, creditLimitCents
        )
        
        // Then
        assertEquals(12, result.size)
        assertEquals(YearMonth.of(2024, 11), result.first().month)
        assertEquals(YearMonth.of(2025, 10), result.last().month)
    }
    
    @Test
    fun `totalMonthly aggregates multiple cards correctly`() {
        // Given: 
        // Card 1: $500 / $5000 = 10%
        // Card 2: $1000 / $10000 = 10%
        // Total: $1500 / $15000 = 10%
        val month = YearMonth.of(2025, 10)
        val closeDate = LocalDate.of(2025, 10, 15)
        
        val cycle = CycleEntity(
            cardId = "any",
            closeDay = 15,
            cadence = "MONTHLY",
            confidence = 0.95,
            nextCloseDate = closeDate,
            candidateDaysJson = "{}",
            signalWeightsJson = "{}",
            computedAt = ZonedDateTime.now()
        )
        
        val cardData = listOf(
            CardUtilizationData(
                balances = listOf(
                    BalanceEntity(
                        cardId = "card_1",
                        date = closeDate,
                        balance = 500.0
                    )
                ),
                payments = emptyList(),
                cycle = cycle,
                creditLimitCents = 500000L
            ),
            CardUtilizationData(
                balances = listOf(
                    BalanceEntity(
                        cardId = "card_2",
                        date = closeDate,
                        balance = 1000.0
                    )
                ),
                payments = emptyList(),
                cycle = cycle,
                creditLimitCents = 1000000L
            )
        )
        
        // When
        val result = UtilizationCalc.totalMonthly(month, cardData)
        
        // Then
        assertEquals(10.0, result.percent, 0.01)
    }
}


