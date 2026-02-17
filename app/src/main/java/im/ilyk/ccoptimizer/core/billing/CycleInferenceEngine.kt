package im.ilyk.ccoptimizer.core.billing

import java.time.LocalDate
import java.time.ZoneId

/**
 * Interface for detecting credit card statement cycles from transaction data
 */
interface CycleInferenceEngine {
    /**
     * Infer statement cycle patterns from transaction history
     * 
     * @param transactions List of transactions sorted by postedAt
     * @param dailyBalances Optional daily balance snapshots for enhanced analysis
     * @param tz Timezone for date normalization (defaults to system timezone)
     * @param today Current date for calculating next close date
     * @return CycleInferenceResult with detected patterns and confidence
     */
    fun infer(
        transactions: List<Transaction>,
        dailyBalances: List<DailyBalance>? = null,
        tz: ZoneId = ZoneId.systemDefault(),
        today: LocalDate = LocalDate.now(tz)
    ): CycleInferenceResult
}
