package im.ilyk.ccoptimizer.debug

import im.ilyk.ccoptimizer.core.billing.DailyBalance
import im.ilyk.ccoptimizer.core.billing.Transaction
import im.ilyk.ccoptimizer.data.db.AppDatabase
import im.ilyk.ccoptimizer.data.db.entities.BalanceEntity
import im.ilyk.ccoptimizer.data.db.entities.CardEntity
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds test data for the Test Center
 */
@Singleton
class TestDataSeeder @Inject constructor(
    private val db: AppDatabase
) {
    
    private val transactionCache = mutableMapOf<String, List<Transaction>>()
    private val balanceCache = mutableMapOf<String, List<DailyBalance>>()
    
    /**
     * Seed typical monthly card with close day 15
     */
    suspend fun seedMonthly(closeDay: Int = 15) {
        val cardId = "test_monthly_$closeDay"
        val card = CardEntity(
            cardId = cardId,
            issuer = "Test Bank",
            last4 = "1234",
            displayName = "Test Monthly (Close: $closeDay)",
            openedAt = LocalDate.now().minusMonths(6),
            isActive = true
        )
        
        db.cardDao().upsert(card)
        
        // Generate 6 months of transactions with clear pattern
        val transactions = mutableListOf<Transaction>()
        repeat(6) { month ->
            val baseDate = LocalDate.now().minusMonths(5L - month)
            
            // Statement marker on close day
            transactions.add(
                Transaction(
                    id = "stmt_${cardId}_$month",
                    postedAt = baseDate.withDayOfMonth(closeDay).atStartOfDay(ZoneId.systemDefault()),
                    amount = 25.0,
                    description = "Interest Charge",
                    category = "Interest",
                    isInterest = true,
                    isStatementMarker = true
                )
            )
            
            // Large payment a few days after close
            transactions.add(
                Transaction(
                    id = "pmt_${cardId}_$month",
                    postedAt = baseDate.withDayOfMonth(closeDay).plusDays(5).atStartOfDay(ZoneId.systemDefault()),
                    amount = -500.0,
                    description = "Payment - Thank You",
                    category = "Payment",
                    isPayment = true
                )
            )
            
            // Random purchases
            repeat(10) { day ->
                if (day != closeDay - 1) {
                    transactions.add(
                        Transaction(
                            id = "tx_${cardId}_${month}_$day",
                            postedAt = baseDate.plusDays(day.toLong()).atStartOfDay(ZoneId.systemDefault()),
                            amount = 50.0 + (day * 5),
                            description = "Purchase at Store #$day",
                            category = "Shopping"
                        )
                    )
                }
            }
        }
        
        transactionCache[cardId] = transactions
        
        // Generate daily balances for the last 90 days
        val balanceEntities = mutableListOf<BalanceEntity>()
        val dailyBalances = mutableListOf<DailyBalance>()
        var currentBalance = 1500.0 // Starting balance
        
        for (daysAgo in 90 downTo 0) {
            val date = LocalDate.now().minusDays(daysAgo.toLong())
            
            // Fluctuate balance slightly each day
            currentBalance += (-50..100).random()
            currentBalance = currentBalance.coerceIn(100.0, 3000.0)
            
            balanceEntities.add(
                BalanceEntity(
                    cardId = cardId,
                    date = date,
                    balance = currentBalance
                )
            )
            
            dailyBalances.add(
                DailyBalance(
                    date = date,
                    balance = currentBalance
                )
            )
        }
        
        // Insert balance entities into database
        db.balanceDao().upsertAll(balanceEntities)
        
        balanceCache[cardId] = dailyBalances
    }
    
    /**
     * Seed card with irregular close day 31 (tests month-end clamping)
     */
    suspend fun seedIrregular31() {
        val cardId = "test_irregular_31"
        val card = CardEntity(
            cardId = cardId,
            issuer = "Test Bank",
            last4 = "3131",
            displayName = "Test Irregular (Close: 31)",
            openedAt = LocalDate.now().minusMonths(6),
            isActive = true
        )
        
        db.cardDao().upsert(card)
        
        val transactions = mutableListOf<Transaction>()
        repeat(6) { month ->
            val baseDate = LocalDate.now().minusMonths(5L - month)
            val lastDay = baseDate.lengthOfMonth()
            
            // Statement marker on last day of month
            transactions.add(
                Transaction(
                    id = "stmt_${cardId}_$month",
                    postedAt = baseDate.withDayOfMonth(lastDay).atStartOfDay(ZoneId.systemDefault()),
                    amount = 30.0,
                    description = "Statement Interest",
                    category = "Interest",
                    isInterest = true,
                    isStatementMarker = true
                )
            )
        }
        
        transactionCache[cardId] = transactions
        balanceCache[cardId] = emptyList()
    }
    
    /**
     * Seed new card with only 2 months of history (low confidence)
     */
    suspend fun seedNewCardLowHistory() {
        val cardId = "test_new_card"
        val card = CardEntity(
            cardId = cardId,
            issuer = "Test Bank",
            last4 = "9999",
            displayName = "Test New Card (2 months)",
            openedAt = LocalDate.now().minusMonths(2),
            isActive = true
        )
        
        db.cardDao().upsert(card)
        
        val transactions = mutableListOf<Transaction>()
        repeat(2) { month ->
            val baseDate = LocalDate.now().minusMonths(1L - month)
            
            transactions.add(
                Transaction(
                    id = "stmt_${cardId}_$month",
                    postedAt = baseDate.withDayOfMonth(10).atStartOfDay(ZoneId.systemDefault()),
                    amount = 20.0,
                    description = "Interest",
                    category = "Interest",
                    isInterest = true
                )
            )
            
            repeat(5) { day ->
                transactions.add(
                    Transaction(
                        id = "tx_${cardId}_${month}_$day",
                        postedAt = baseDate.plusDays(day.toLong()).atStartOfDay(ZoneId.systemDefault()),
                        amount = 30.0,
                        description = "Purchase",
                        category = "Shopping"
                    )
                )
            }
        }
        
        transactionCache[cardId] = transactions
        balanceCache[cardId] = emptyList()
    }
    
    /**
     * Seed card with strong interest marker signals
     */
    suspend fun seedInterestMarker() {
        val cardId = "test_interest_marker"
        val card = CardEntity(
            cardId = cardId,
            issuer = "Test Bank",
            last4 = "7777",
            displayName = "Test Interest Markers (Close: 20)",
            openedAt = LocalDate.now().minusMonths(4),
            isActive = true
        )
        
        db.cardDao().upsert(card)
        
        val transactions = mutableListOf<Transaction>()
        repeat(4) { month ->
            val baseDate = LocalDate.now().minusMonths(3L - month)
            
            // Strong interest marker on day 20
            transactions.add(
                Transaction(
                    id = "stmt_${cardId}_$month",
                    postedAt = baseDate.withDayOfMonth(20).atStartOfDay(ZoneId.systemDefault()),
                    amount = 35.0,
                    description = "FINANCE CHARGE - PREVIOUS BALANCE",
                    category = "Interest",
                    isInterest = true,
                    isStatementMarker = true
                )
            )
            
            // Add some noise transactions
            repeat(8) { day ->
                transactions.add(
                    Transaction(
                        id = "tx_${cardId}_${month}_$day",
                        postedAt = baseDate.plusDays(day.toLong()).atStartOfDay(ZoneId.systemDefault()),
                        amount = 40.0 + (day * 3),
                        description = "Store Purchase",
                        category = "Shopping"
                    )
                )
            }
        }
        
        transactionCache[cardId] = transactions
        balanceCache[cardId] = emptyList()
    }
    
    /**
     * Load cached transactions for a card
     */
    fun loadTransactions(cardId: String): List<Transaction> {
        return transactionCache[cardId] ?: emptyList()
    }
    
    /**
     * Load cached balances for a card
     */
    fun loadBalances(cardId: String): List<DailyBalance> {
        return balanceCache[cardId] ?: emptyList()
    }
    
    /**
     * Clear all test data
     */
    suspend fun clearAllTestData() {
        // Delete all test cards
        transactionCache.keys.forEach { cardId ->
            db.cardDao().deleteById(cardId)
        }
        transactionCache.clear()
        balanceCache.clear()
    }
}
