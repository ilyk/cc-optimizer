package im.ilyk.ccoptimizer.core.billing

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Demo class to test the cycle inference engine
 */
object CycleInferenceDemo {
    
    fun runDemo() {
        println("🔄 Cycle Inference Engine Demo")
        println("=" * 50)
        
        val engine = CycleInferenceEngineFactory.createDefault()
        val pst = ZoneId.of("America/Los_Angeles")
        
        // Create sample transactions with clear pattern
        val transactions = createSampleTransactions()
        
        val result = engine.infer(transactions, null, pst, LocalDate.of(2024, 3, 15))
        
        println("📊 Inference Results:")
        println("Close Day: ${result.closeDay}")
        println("Cadence: ${result.cadence}")
        println("Confidence: ${String.format("%.2f", result.confidence * 100)}%")
        println("Next Close Date: ${result.nextCloseDate}")
        
        println("\n🎯 Candidate Days (top 5):")
        result.candidateDays.entries
            .sortedByDescending { it.value }
            .take(5)
            .forEach { (day, weight) ->
                println("  Day $day: ${String.format("%.2f", weight)}")
            }
        
        println("\n📈 Signal Weights:")
        result.signalWeights.forEach { (signal, weight) ->
            println("  $signal: ${String.format("%.2f", weight)}")
        }
        
        // Test edge cases
        println("\n🧪 Edge Case Tests:")
        testEdgeCases(engine, pst)
    }
    
    private fun createSampleTransactions(): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        
        // Create 6 months of data with close day 15
        repeat(6) { month ->
            val baseDate = LocalDate.of(2024, 1 + month, 1)
            
            // Statement marker on day 15
            transactions.add(Transaction(
                id = "stmt_${month}_15",
                postedAt = baseDate.plusDays(14).atStartOfDay(ZoneId.of("America/Los_Angeles")),
                amount = 25.0,
                description = "Interest Charge",
                category = "Interest",
                isInterest = true,
                isStatementMarker = true
            ))
            
            // Large payment on day 10 (25 days after close)
            transactions.add(Transaction(
                id = "payment_${month}_10",
                postedAt = baseDate.plusDays(9).atStartOfDay(ZoneId.of("America/Los_Angeles")),
                amount = -500.0,
                description = "Payment",
                category = "Payment",
                isPayment = true
            ))
            
            // Some purchases throughout the month
            repeat(15) { day ->
                if (day != 14 && day != 9) { // Not on close day or payment day
                    transactions.add(Transaction(
                        id = "purchase_${month}_${day}",
                        postedAt = baseDate.plusDays(day.toLong()).atStartOfDay(ZoneId.of("America/Los_Angeles")),
                        amount = 50.0 + (day * 2),
                        description = "Purchase at Store",
                        category = "Shopping"
                    ))
                }
            }
        }
        
        return transactions
    }
    
    private fun testEdgeCases(engine: CycleInferenceEngine, tz: ZoneId) {
        // Test 1: Empty transactions
        val emptyResult = engine.infer(emptyList(), null, tz)
        println("  Empty transactions: Close Day = ${emptyResult.closeDay}, Confidence = ${String.format("%.2f", emptyResult.confidence)}")
        
        // Test 2: New card with minimal data
        val newCardTransactions = createSampleTransactions().take(20) // Only 20 transactions
        val newCardResult = engine.infer(newCardTransactions, null, tz)
        println("  New card (minimal data): Close Day = ${newCardResult.closeDay}, Confidence = ${String.format("%.2f", newCardResult.confidence)}")
        
        // Test 3: Variable close dates
        val variableTransactions = createVariableCloseTransactions()
        val variableResult = engine.infer(variableTransactions, null, tz)
        println("  Variable close dates: Close Day = ${variableResult.closeDay}, Confidence = ${String.format("%.2f", variableResult.confidence)}")
    }
    
    private fun createVariableCloseTransactions(): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        
        // Alternating close dates: 14 and 15
        repeat(6) { month ->
            val closeDay = if (month % 2 == 0) 14 else 15
            transactions.add(Transaction(
                id = "stmt_${month}_${closeDay}",
                postedAt = LocalDate.of(2024, 1 + month, closeDay).atStartOfDay(ZoneId.of("America/Los_Angeles")),
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

private operator fun String.times(n: Int): String = this.repeat(n)
