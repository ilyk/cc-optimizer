package im.ilyk.ccoptimizer.core.billing

/**
 * Configuration for cycle inference algorithm parameters
 * Allows tuning weights and thresholds for different use cases
 */
data class CycleConfig(
    // Heuristic weights
    val statementMarkerWeight: Double = 3.0,
    val paymentResetWeight: Double = 2.0,
    val paymentBackInferenceWeight: Double = 1.5,
    val cycleNettingWeight: Double = 1.25,
    val keywordWeight: Double = 2.25,
    
    // Thresholds
    val paymentPercentile: Double = 0.75,
    val minPaymentAmount: Double = 100.0,
    val balanceDropThreshold: Double = 0.8,
    val netTransitionThreshold: Double = 0.6,
    
    // Due date inference
    val dueMinusCloseOffset: Int = 25, // Days between close and due
    val backInferenceOffsetRange: Int = 3, // ±3 days for payment back-inference
    
    // Confidence factors
    val newCardMaxConfidence: Double = 0.35,
    val minDataMonths: Int = 3,
    val idealDataMonths: Int = 6,
    
    // Cadence detection
    val monthlyIntervalRange: IntRange = 27..32,
    val fourWeekIntervalRange: IntRange = 26..30,
    val biweeklyIntervalRange: IntRange = 12..16
) {
    companion object {
        val DEFAULT = CycleConfig()
        val CONSERVATIVE = CycleConfig(
            statementMarkerWeight = 4.0,
            paymentResetWeight = 2.5,
            newCardMaxConfidence = 0.25,
            minDataMonths = 4
        )
        val AGGRESSIVE = CycleConfig(
            statementMarkerWeight = 2.5,
            paymentBackInferenceWeight = 2.0,
            newCardMaxConfidence = 0.45,
            minDataMonths = 2
        )
    }
}
