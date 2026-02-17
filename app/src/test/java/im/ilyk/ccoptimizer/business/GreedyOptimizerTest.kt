package im.ilyk.ccoptimizer.business

import im.ilyk.ccoptimizer.data.models.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for GreedyOptimizer
 */
class GreedyOptimizerTest {
    
    private val optimizer = GreedyOptimizer()
    
    @Test
    fun `buildPlan should generate payments for all cards`() {
        // Given
        val cards = listOf(
            createTestCard("card1", 1000.0, 0.1999, LocalDate.now().plusDays(25)),
            createTestCard("card2", 2000.0, 0.2299, LocalDate.now().plusDays(30))
        )
        val input = createTestInput(cards)
        
        // When
        val plan = optimizer.buildPlan(input)
        
        // Then
        assertTrue(plan.payments.isNotEmpty())
        assertEquals(2, plan.payments.map { it.cardId }.distinct().size)
    }
    
    @Test
    fun `buildPlan should prioritize high APR cards`() {
        // Given
        val highAprCard = createTestCard("high-apr", 1000.0, 0.2999, LocalDate.now().plusDays(25))
        val lowAprCard = createTestCard("low-apr", 1000.0, 0.0999, LocalDate.now().plusDays(25))
        val cards = listOf(highAprCard, lowAprCard)
        val input = createTestInput(cards, availableCash = 500.0)
        
        // When
        val plan = optimizer.buildPlan(input)
        
        // Then
        val highAprPayments = plan.payments.filter { it.cardId == "high-apr" }
        val lowAprPayments = plan.payments.filter { it.cardId == "low-apr" }
        
        assertTrue(highAprPayments.sumOf { it.amount } > lowAprPayments.sumOf { it.amount })
    }
    
    @Test
    fun `buildPlan should respect minimum payments on due dates`() {
        // Given
        val card = createTestCard("card1", 1000.0, 0.1999, LocalDate.now().plusDays(5))
        val input = createTestInput(listOf(card), availableCash = 100.0)
        
        // When
        val plan = optimizer.buildPlan(input)
        
        // Then
        val dueDatePayments = plan.payments.filter { 
            it.cardId == "card1" && it.date == card.dueDate 
        }
        assertTrue(dueDatePayments.any { it.isMinDue })
        assertTrue(dueDatePayments.any { it.amount >= card.minPayment })
    }
    
    @Test
    fun `buildPlan should respect cashflow constraints`() {
        // Given
        val card = createTestCard("card1", 1000.0, 0.1999, LocalDate.now().plusDays(25))
        val cashflow = listOf(
            CashflowConstraint(LocalDate.now(), 100.0),
            CashflowConstraint(LocalDate.now().plusDays(1), 200.0)
        )
        val input = OptimizationInput(
            cards = listOf(card),
            txns = emptyList(),
            cashflow = cashflow,
            horizonStart = LocalDate.now(),
            horizonEnd = LocalDate.now().plusDays(30),
            respectIssuerAutopay = true,
            preferBalanceSmoothing = true,
            minimizeInterest = true
        )
        
        // When
        val plan = optimizer.buildPlan(input)
        
        // Then
        val totalPaymentsToday = plan.payments
            .filter { it.date == LocalDate.now() }
            .sumOf { it.amount }
        assertTrue(totalPaymentsToday <= 100.0)
    }
    
    @Test
    fun `comparePlans should show baseline vs optimized`() {
        // Given
        val cards = listOf(
            createTestCard("card1", 1000.0, 0.1999, LocalDate.now().plusDays(25))
        )
        val input = createTestInput(cards)
        
        // When
        val comparison = optimizer.comparePlans(input)
        
        // Then
        assertNotNull(comparison.baseline)
        assertNotNull(comparison.optimized)
        assertTrue(comparison.optimized.totalInterestProjected <= comparison.baseline.totalInterestProjected)
    }
    
    @Test
    fun `buildPlan should handle empty input gracefully`() {
        // Given
        val input = OptimizationInput(
            cards = emptyList(),
            txns = emptyList(),
            cashflow = emptyList(),
            horizonStart = LocalDate.now(),
            horizonEnd = LocalDate.now().plusDays(30),
            respectIssuerAutopay = true,
            preferBalanceSmoothing = true,
            minimizeInterest = true
        )
        
        // When
        val plan = optimizer.buildPlan(input)
        
        // Then
        assertTrue(plan.payments.isEmpty())
        assertEquals(0.0, plan.totalInterestProjected)
    }
    
    @Test
    fun `buildPlan should merge small payments`() {
        // Given
        val card = createTestCard("card1", 1000.0, 0.1999, LocalDate.now().plusDays(25))
        val input = createTestInput(listOf(card), availableCash = 50.0)
        
        // When
        val plan = optimizer.buildPlan(input)
        
        // Then
        // Should not have too many small payments for the same card on the same day
        val paymentsByCardAndDate = plan.payments.groupBy { "${it.cardId}-${it.date}" }
        assertTrue(paymentsByCardAndDate.values.all { it.size <= 1 })
    }
    
    // Helper methods
    private fun createTestCard(
        id: String,
        balance: Double,
        apr: Double,
        dueDate: LocalDate
    ): CreditCard {
        return CreditCard(
            id = id,
            externalId = "external-$id",
            issuer = "Test Bank",
            network = CardNetwork.VISA,
            balance = balance,
            minPayment = balance * 0.02, // 2% minimum
            creditLimit = balance * 2,
            aprPurchase = apr,
            statementClose = dueDate.minusDays(21),
            dueDate = dueDate,
            utilization = balance / (balance * 2)
        )
    }
    
    private fun createTestInput(
        cards: List<CreditCard>,
        availableCash: Double = 1000.0
    ): OptimizationInput {
        val now = LocalDate.now()
        val cashflow = (0..30).map { days ->
            CashflowConstraint(
                date = now.plusDays(days.toLong()),
                available = availableCash
            )
        }
        
        return OptimizationInput(
            cards = cards,
            txns = emptyList(),
            cashflow = cashflow,
            horizonStart = now,
            horizonEnd = now.plusDays(30),
            respectIssuerAutopay = true,
            preferBalanceSmoothing = true,
            minimizeInterest = true
        )
    }
}
