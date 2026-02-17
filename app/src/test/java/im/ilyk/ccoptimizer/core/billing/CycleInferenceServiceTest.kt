package im.ilyk.ccoptimizer.core.billing

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class CycleInferenceServiceTest {
    
    private val repository = InMemoryCycleInferenceRepository()
    private val engine = CycleInferenceEngineImpl()
    private val service = CycleInferenceService(engine, repository)
    
    @Test
    fun `infer cycle for card saves result and updates state flow`() = runTest {
        val cardId = "test-card-1"
        val transactions = generateTestTransactions()
        
        val result = service.inferCycleForCard(cardId, transactions)
        
        assertNotNull(result.closeDay)
        assertTrue(result.confidence > 0.0)
        
        // Verify saved in repository
        val savedResult = repository.getCycleInference(cardId)
        assertEquals(result, savedResult)
        
        // Verify state flow updated
        val allResults = service.inferenceResults.value
        assertTrue(allResults.containsKey(cardId))
        assertEquals(result, allResults[cardId])
    }
    
    @Test
    fun `get cycle inference returns saved result`() = runTest {
        val cardId = "test-card-2"
        val transactions = generateTestTransactions()
        
        // First infer and save
        service.inferCycleForCard(cardId, transactions)
        
        // Then retrieve
        val result = service.getCycleInference(cardId)
        
        assertNotNull(result)
        assertNotNull(result?.closeDay)
        assertTrue(result?.confidence!! > 0.0)
    }
    
    @Test
    fun `clear cycle inference removes result`() = runTest {
        val cardId = "test-card-3"
        val transactions = generateTestTransactions()
        
        // First infer and save
        service.inferCycleForCard(cardId, transactions)
        assertNotNull(service.getCycleInference(cardId))
        
        // Then clear
        service.clearCycleInference(cardId)
        
        // Verify removed
        assertNull(service.getCycleInference(cardId))
        assertFalse(service.inferenceResults.value.containsKey(cardId))
    }
    
    @Test
    fun `get cards with approaching close dates works correctly`() = runTest {
        val card1 = "card-1"
        val card2 = "card-2"
        val card3 = "card-3"
        
        // Create transactions with different close dates
        val transactions1 = generateTransactionsWithCloseDay(15, 3)
        val transactions2 = generateTransactionsWithCloseDay(20, 3)
        val transactions3 = generateTransactionsWithCloseDay(5, 3) // Far future
        
        service.inferCycleForCard(card1, transactions1)
        service.inferCycleForCard(card2, transactions2)
        service.inferCycleForCard(card3, transactions3)
        
        // Get cards with close dates in next 10 days
        val approachingCards = service.getCardsWithApproachingCloseDates(10)
        
        // Should include cards with close dates in the next 10 days
        assertTrue(approachingCards.isNotEmpty())
    }
    
    @Test
    fun `refresh all inferences updates all cards`() = runTest {
        val cardTransactions = mapOf(
            "card-1" to generateTestTransactions(),
            "card-2" to generateTestTransactions(),
            "card-3" to generateTestTransactions()
        )
        
        service.refreshAllInferences(cardTransactions)
        
        val allResults = service.getAllCycleInferences()
        assertEquals(3, allResults.size)
        assertTrue(allResults.containsKey("card-1"))
        assertTrue(allResults.containsKey("card-2"))
        assertTrue(allResults.containsKey("card-3"))
    }
    
    // Helper methods
    
    private fun generateTestTransactions(): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        
        // Add 3 months of data with close day 15
        repeat(3) { month ->
            // Statement marker on day 15
            transactions.add(Transaction(
                id = "stmt_${month}_15",
                postedAt = LocalDate.of(2024, 1 + month, 15).atStartOfDay(ZoneId.systemDefault()),
                amount = 25.0,
                description = "Interest Charge",
                category = "Interest",
                isInterest = true,
                isStatementMarker = true
            ))
            
            // Some purchases throughout the month
            repeat(10) { day ->
                if (day != 14) { // Not on close day
                    transactions.add(Transaction(
                        id = "purchase_${month}_${day}",
                        postedAt = LocalDate.of(2024, 1 + month, 1 + day).atStartOfDay(ZoneId.systemDefault()),
                        amount = 50.0,
                        description = "Purchase",
                        category = "Shopping"
                    ))
                }
            }
        }
        
        return transactions
    }
    
    private fun generateTransactionsWithCloseDay(closeDay: Int, months: Int): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        
        repeat(months) { month ->
            transactions.add(Transaction(
                id = "stmt_${month}_${closeDay}",
                postedAt = LocalDate.of(2024, 1 + month, closeDay).atStartOfDay(ZoneId.systemDefault()),
                amount = 25.0,
                description = "Interest Charge",
                category = "Interest",
                isInterest = true,
                isStatementMarker = true
            ))
        }
        
        return transactions
    }
}
