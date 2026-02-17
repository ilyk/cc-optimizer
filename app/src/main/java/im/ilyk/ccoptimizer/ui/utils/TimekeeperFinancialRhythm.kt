package im.ilyk.ccoptimizer.ui.utils

import im.ilyk.ccoptimizer.data.models.CreditCard
import im.ilyk.ccoptimizer.data.models.PaymentRecommendation
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.*

/**
 * Timekeeper Financial Rhythm Calculations
 * Advanced algorithms for calculating financial health, optimization scores, and cash flow rhythm
 */

/**
 * Calculate overall financial rhythm score (0.0 to 1.0)
 * Higher scores indicate better financial synchronization
 */
fun calculateFinancialRhythm(
    recommendations: List<PaymentRecommendation>,
    cards: List<CreditCard>
): Float {
    if (recommendations.isEmpty() && cards.isEmpty()) return 0.5f
    
    val totalCards = cards.size
    val optimizedCards = recommendations.size // All recommendations are considered optimized
    val verifiedCards = cards.count { it.isDateVerified }
    
    // Base rhythm from optimization ratio
    val optimizationRatio = if (totalCards > 0) optimizedCards.toFloat() / totalCards else 0f
    
    // Bonus for verified cards (more reliable data)
    val verificationBonus = if (totalCards > 0) verifiedCards.toFloat() / totalCards * 0.2f else 0f
    
    // Penalty for cards needing input
    val inputPenalty = if (totalCards > 0) {
        val cardsNeedingInput = cards.count { !it.isDateVerified && it.inferenceConfidence?.let { conf -> conf < 0.6 } ?: true }
        cardsNeedingInput.toFloat() / totalCards * 0.3f
    } else 0f
    
    return (optimizationRatio + verificationBonus - inputPenalty).coerceIn(0f, 1f)
}

/**
 * Calculate cycle health score (0 to 100)
 * Represents how well all payment cycles are aligned
 */
fun calculateCycleHealthScore(
    recommendations: List<PaymentRecommendation>
): Int {
    if (recommendations.isEmpty()) return 0
    
    val totalRecommendations = recommendations.size
    val optimalRecommendations = recommendations.size // All recommendations are considered optimal
    
    // Base score from optimization ratio
    val baseScore = (optimalRecommendations.toFloat() / totalRecommendations * 100).toInt()
    
    // Bonus for consistent timing patterns
    val timingBonus = calculateTimingConsistencyBonus(recommendations)
    
    // Bonus for interest savings
    val savingsBonus = calculateSavingsBonus(recommendations)
    
    return (baseScore + timingBonus + savingsBonus).coerceIn(0, 100)
}

/**
 * Calculate timing consistency bonus (0-10 points)
 * Rewards cards with similar payment patterns
 */
private fun calculateTimingConsistencyBonus(
    recommendations: List<PaymentRecommendation>
): Int {
    if (recommendations.size < 2) return 0
    
    val paymentDates = recommendations.map { it.recommendedPaymentDate.dayOfMonth }
    val dateVariance = calculateVariance(paymentDates.map { it.toFloat() })
    
    // Lower variance = more consistent = higher bonus
    return when {
        dateVariance < 5 -> 10  // Very consistent (within 5 days)
        dateVariance < 10 -> 7  // Moderately consistent
        dateVariance < 15 -> 4  // Somewhat consistent
        else -> 0  // Inconsistent
    }
}

/**
 * Calculate savings bonus (0-15 points)
 * Rewards significant interest savings
 */
private fun calculateSavingsBonus(
    recommendations: List<PaymentRecommendation>
): Int {
    val totalSavings = recommendations.sumOf { it.recommendedAmount }
    
    return when {
        totalSavings > 500 -> 15  // High savings
        totalSavings > 200 -> 10  // Medium savings
        totalSavings > 50 -> 5    // Low savings
        else -> 0  // Minimal savings
    }
}

/**
 * Calculate next payment urgency (days until next payment)
 */
fun calculateNextPaymentUrgency(
    recommendations: List<PaymentRecommendation>
): Int {
    if (recommendations.isEmpty()) return 0
    
    val today = LocalDate.now()
    return recommendations.minOfOrNull { 
        ChronoUnit.DAYS.between(today, it.recommendedPaymentDate).toInt()
    } ?: 0
}

/**
 * Calculate cash flow velocity (0.0 to 1.0)
 * Higher values indicate faster, more efficient cash flow
 */
fun calculateCashFlowVelocity(
    recommendations: List<PaymentRecommendation>,
    cards: List<CreditCard>
): Float {
    if (recommendations.isEmpty()) return 0.5f
    
    val totalBalance = cards.sumOf { it.currentBalance }
    val totalRecommended = recommendations.sumOf { it.recommendedAmount }
    
    if (totalBalance <= 0) return 0.5f
    
    // Velocity based on payment ratio and timing
    val paymentRatio = (totalRecommended / totalBalance).toFloat().coerceIn(0f, 1f)
    val timingEfficiency = calculateTimingEfficiency(recommendations)
    
    return (paymentRatio * 0.7f + timingEfficiency * 0.3f).coerceIn(0f, 1f)
}

/**
 * Calculate timing efficiency (0.0 to 1.0)
 * Measures how well payment dates are distributed
 */
private fun calculateTimingEfficiency(
    recommendations: List<PaymentRecommendation>
): Float {
    if (recommendations.size < 2) return 1f
    
    val paymentDates = recommendations.map { it.recommendedPaymentDate }
    val today = LocalDate.now()
    
    // Calculate spread of payment dates
    val daysFromToday = paymentDates.map { ChronoUnit.DAYS.between(today, it).toInt() }
    val minDays = daysFromToday.minOrNull() ?: 0
    val maxDays = daysFromToday.maxOrNull() ?: 0
    val spread = maxDays - minDays
    
    // Optimal spread is 7-14 days (not too clustered, not too spread out)
    return when {
        spread in 7..14 -> 1.0f      // Perfect spread
        spread in 5..20 -> 0.8f    // Good spread
        spread in 3..30 -> 0.6f    // Acceptable spread
        else -> 0.4f               // Poor spread
    }
}

/**
 * Calculate optimization potential (0.0 to 1.0)
 * Indicates how much improvement is possible
 */
fun calculateOptimizationPotential(
    recommendations: List<PaymentRecommendation>,
    cards: List<CreditCard>
): Float {
    if (recommendations.isEmpty()) return 0.8f // High potential if no optimization yet
    
    val currentScore = calculateCycleHealthScore(recommendations)
    val maxPossibleScore = 100
    
    // Potential is inverse of current score
    return (1f - currentScore.toFloat() / maxPossibleScore).coerceIn(0f, 1f)
}

/**
 * Calculate interest savings potential
 */
fun calculateInterestSavingsPotential(
    recommendations: List<PaymentRecommendation>
): Double {
    return recommendations.sumOf { it.recommendedAmount }
}

/**
 * Calculate financial stress level (0.0 to 1.0)
 * Higher values indicate more financial stress
 */
fun calculateFinancialStress(
    cards: List<CreditCard>
): Float {
    if (cards.isEmpty()) return 0f
    
    val totalBalance = cards.sumOf { it.currentBalance }
    val totalLimit = cards.sumOf { it.creditLimit }
    val utilizationRatio = if (totalLimit > 0) (totalBalance / totalLimit).toFloat() else 0f
    
    // Stress factors
    val highUtilization = if (utilizationRatio > 0.8f) 0.3f else 0f
    val manyCards = if (cards.size > 5) 0.2f else 0f
    val highBalances = if (totalBalance > 10000.0) 0.2f else 0f
    
    return (utilizationRatio * 0.5f + highUtilization + manyCards + highBalances).coerceIn(0f, 1f)
}

/**
 * Calculate payment timing score (0.0 to 1.0)
 * Measures how well payment dates are optimized
 */
fun calculatePaymentTimingScore(
    recommendations: List<PaymentRecommendation>
): Float {
    if (recommendations.isEmpty()) return 0.5f
    
    val today = LocalDate.now()
    val optimalDays = 7 // Optimal payment window
    
    val timingScores = recommendations.map { rec ->
        val daysUntil = ChronoUnit.DAYS.between(today, rec.recommendedPaymentDate).toInt()
        val daysDiff = abs(daysUntil - optimalDays)
        
        // Score decreases as days differ from optimal
        when {
            daysDiff <= 1 -> 1.0f      // Perfect timing
            daysDiff <= 3 -> 0.8f     // Good timing
            daysDiff <= 7 -> 0.6f     // Acceptable timing
            else -> 0.3f              // Poor timing
        }
    }
    
    return timingScores.average().toFloat()
}

/**
 * Calculate variance of a list of numbers
 */
private fun calculateVariance(numbers: List<Float>): Float {
    if (numbers.isEmpty()) return 0f
    
    val mean = numbers.average().toFloat()
    val squaredDiffs = numbers.map { (it - mean).pow(2) }
    return squaredDiffs.average().toFloat()
}

/**
 * Get financial rhythm description based on score
 */
fun getFinancialRhythmDescription(score: Float): String {
    return when {
        score >= 0.9f -> "Perfectly synchronized. All cycles aligned."
        score >= 0.7f -> "Well balanced. Minor optimizations available."
        score >= 0.5f -> "Moderate rhythm. Some timing adjustments needed."
        score >= 0.3f -> "Rhythm needs attention. Significant optimization required."
        else -> "Critical optimization needed. Financial flow disrupted."
    }
}

/**
 * Get cycle health description based on score
 */
fun getCycleHealthDescription(score: Int): String {
    return when {
        score >= 90 -> "All cycles aligned. Flow optimal."
        score >= 70 -> "Mostly aligned. Minor optimizations available."
        score >= 50 -> "Some cycles need attention."
        score >= 30 -> "Cycles need significant optimization."
        else -> "Critical cycle misalignment detected."
    }
}

/**
 * Get optimization urgency level
 */
fun getOptimizationUrgency(score: Int): String {
    return when {
        score >= 80 -> "Low"      // Well optimized
        score >= 60 -> "Medium"   // Some optimization needed
        score >= 40 -> "High"     // Significant optimization needed
        else -> "Critical"        // Urgent optimization required
    }
}
