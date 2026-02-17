package im.ilyk.ccoptimizer.core.billing

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class CycleInferenceEngineImplTest {
    
    private val engine = CycleInferenceEngineImpl()
    private val pst = ZoneId.of("America/Los_Angeles")
    
    @Test
    fun `empty transactions returns null close day with zero confidence`() {
        val result = engine.infer(emptyList(), null, pst, LocalDate.of(2024, 1, 15))
        
        assertNull(result.closeDay)
        assertEquals(Cadence.MONTHLY, result.cadence)
        assertEquals(0.0, result.confidence)
        assertNull(result.nextCloseDate)
        assertTrue(result.candidateDays.isEmpty())
        assertTrue(result.signalWeights.isEmpty())
    }
    
    @Test
    fun `irregular months clamp close day to valid range`() {
        // Generate transactions with inferred close day = 31
        val transactions = generateTransactionsWithCloseDay(31, 6) // 6 months of data
        
        val result = engine.infer(transactions, null, pst, LocalDate.of(2024, 2, 15))
        
        // February should clamp to 28/29
        assertNotNull(result.closeDay)
        assertTrue(result.closeDay!! <= 29)
        assertTrue(result.confidence >= 0.75)
    }
    
    @Test
    fun `dst transitions are stable with LocalDate normalization`() {
        // Spring forward: March 10, 2024 (2 AM -> 3 AM)
        val springForward = LocalDate.of(2024, 3, 10)
        val transactions = generateTransactionsWithCloseDay(10, 3)
        
        val result = engine.infer(transactions, null, pst, springForward)
        
        assertNotNull(result.closeDay)
        assertEquals(10, result.closeDay)
        assertTrue(result.confidence > 0.7)
    }
    
    @Test
    fun `new card with minimal data returns low confidence`() {
        // Only 6-8 weeks of data
        val transactions = generateTransactionsWithCloseDay(15, 2) // 2 months
        
        val result = engine.infer(transactions, null, pst, LocalDate.of(2024, 3, 15))
        
        assertTrue(result.confidence <= 0.35)
        // Should still attempt to detect pattern
        assertNotNull(result.closeDay)
    }
    
    @Test
    fun `interest markers dominate inference`() {
        val closeDay = 15
        val transactions = mutableListOf<Transaction>()
        
        // Add regular transactions
        repeat(3) { month ->
            repeat(10) { day ->
                transactions.add(createTransaction(
                    LocalDate.of(2024, 1 + month, 1 + day),
                    amount = 50.0,
                    description = "Purchase"
                ))
            }
        }
        
        // Add strong interest markers on close day
        repeat(3) { month ->
            transactions.add(createTransaction(
                LocalDate.of(2024, 1 + month, closeDay),
                amount = 25.0,
                description = "Interest Charge",
                isInterest = true
            ))
        }
        
        val result = engine.infer(transactions, null, pst, LocalDate.of(2024, 4, 15))
        
        assertEquals(closeDay, result.closeDay)
        assertTrue(result.confidence > 0.8)
        assertTrue(result.signalWeights["H1_statement_markers"]!! > 0)
    }
    
    @Test
    fun `variable close date chooses earlier with lowered confidence`() {
        val transactions = mutableListOf<Transaction>()
        
        // Alternating close dates: 14 and 15
        repeat(6) { month ->
            val closeDay = if (month % 2 == 0) 14 else 15
            transactions.add(createTransaction(
                LocalDate.of(2024, 1 + month, closeDay),
                amount = 30.0,
                description = "Statement Interest",
                isInterest = true
            ))
        }
        
        val result = engine.infer(transactions, null, pst, LocalDate.of(2024, 7, 15))
        
        // Should choose 14 (earlier) and have lowered confidence
        assertEquals(14, result.closeDay)
        assertTrue(result.confidence < 0.9) // Lower confidence due to variability
    }
    
    @Test
    fun `payment cadence back-inference works correctly`() {
        val transactions = mutableListOf<Transaction>()
        val closeDay = 15
        val dueDay = 10 // 25 days after close
        
        // Add payments around due date for 3 months
        repeat(3) { month ->
            transactions.add(createTransaction(
                LocalDate.of(2024, 1 + month, dueDay),
                amount = -500.0,
                description = "Payment",
                isPayment = true
            ))
        }
        
        val result = engine.infer(transactions, null, pst, LocalDate.of(2024, 4, 15))
        
        // Should detect close day from payment pattern
        assertNotNull(result.closeDay)
        assertTrue(result.signalWeights["H3_payment_back_infer"]!! > 0)
    }
    
    @Test
    fun `balance drops enhance inference when available`() {
        val transactions = generateTransactionsWithCloseDay(15, 3)
        val balances = listOf(
            DailyBalance(LocalDate.of(2024, 1, 14), 1000.0),
            DailyBalance(LocalDate.of(2024, 1, 15), 100.0), // Large drop
            DailyBalance(LocalDate.of(2024, 2, 14), 1000.0),
            DailyBalance(LocalDate.of(2024, 2, 15), 100.0),
        )
        
        val result = engine.infer(transactions, balances, pst, LocalDate.of(2024, 3, 15))
        
        assertTrue(result.signalWeights["H2_balance_drop"]!! > 0)
        assertTrue(result.confidence > 0.7)
    }
    
    @Test
    fun `cadence detection works for different patterns`() {
        // Monthly cadence
        val monthlyTransactions = generateTransactionsWithCloseDay(15, 6)
        val monthlyResult = engine.infer(monthlyTransactions, null, pst, LocalDate.of(2024, 7, 15))
        assertEquals(Cadence.MONTHLY, monthlyResult.cadence)
        
        // Biweekly cadence (simulated)
        val biweeklyTransactions = generateBiweeklyTransactions(3)
        val biweeklyResult = engine.infer(biweeklyTransactions, null, pst, LocalDate.of(2024, 2, 15))
        assertEquals(Cadence.BIWEEKLY, biweeklyResult.cadence)
    }
    
    @Test
    fun `confidence calculation considers multiple factors`() {
        val transactions = mutableListOf<Transaction>()
        
        // Create strong signal on day 15
        repeat(3) { month ->
            transactions.add(createTransaction(
                LocalDate.of(2024, 1 + month, 15),
                amount = 25.0,
                description = "Interest Charge",
                isInterest = true
            ))
        }
        
        val result = engine.infer(transactions, null, pst, LocalDate.of(2024, 4, 15))
        
        assertTrue(result.confidence > 0.5)
        assertTrue(result.confidence <= 1.0)
        assertEquals(15, result.closeDay)
    }
    
    @Test
    fun `next close date calculation works correctly`() {
        val transactions = generateTransactionsWithCloseDay(15, 3)
        val result = engine.infer(transactions, null, pst, LocalDate.of(2024, 4, 15))
        
        assertNotNull(result.nextCloseDate)
        assertTrue(result.nextCloseDate!!.dayOfMonth == 15 || result.nextCloseDate!!.dayOfMonth <= 15)
    }
    
    // Helper methods
    
    private fun generateTransactionsWithCloseDay(closeDay: Int, months: Int): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        
        repeat(months) { month ->
            // Add statement marker on close day
            transactions.add(createTransaction(
                LocalDate.of(2024, 1 + month, closeDay),
                amount = 25.0,
                description = "Interest Charge",
                isInterest = true
            ))
            
            // Add some regular transactions throughout the month
            repeat(10) { day ->
                if (day != closeDay - 1) {
                    transactions.add(createTransaction(
                        LocalDate.of(2024, 1 + month, 1 + day),
                        amount = 50.0,
                        description = "Purchase"
                    ))
                }
            }
        }
        
        return transactions
    }
    
    private fun generateBiweeklyTransactions(cycles: Int): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        
        repeat(cycles) { cycle ->
            val baseDate = LocalDate.of(2024, 1, 1).plusDays(cycle * 14L)
            transactions.add(createTransaction(
                baseDate,
                amount = 25.0,
                description = "Statement Interest",
                isInterest = true
            ))
        }
        
        return transactions
    }
    
    private fun createTransaction(
        date: LocalDate,
        amount: Double,
        description: String,
        isInterest: Boolean = false,
        isPayment: Boolean = false
    ): Transaction {
        return Transaction(
            id = "${date}_${amount}_${description.hashCode()}",
            postedAt = date.atStartOfDay(pst),
            amount = amount,
            description = description,
            category = null,
            isPayment = isPayment,
            isInterest = isInterest,
            isStatementMarker = isInterest
        )
    }
}
