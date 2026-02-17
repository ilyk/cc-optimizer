package im.ilyk.ccoptimizer.core.billing

import java.time.LocalDate

/**
 * Represents the cadence of a credit card statement cycle
 */
enum class Cadence { 
    MONTHLY, 
    FOUR_WEEK, 
    BIWEEKLY 
}

/**
 * Result of cycle inference analysis containing detected patterns
 * and confidence metrics
 */
data class CycleInferenceResult(
    val closeDay: Int?, // Day of month (1-31) or null if insufficient signal
    val cadence: Cadence,
    val confidence: Double, // 0.0 to 1.0
    val nextCloseDate: LocalDate?,
    val candidateDays: Map<Int, Double>, // Histogram of candidate close days with weights
    val signalWeights: Map<String, Double> // Contribution of each heuristic signal
)
