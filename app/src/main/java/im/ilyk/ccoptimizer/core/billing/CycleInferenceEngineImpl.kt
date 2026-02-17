package im.ilyk.ccoptimizer.core.billing

import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Implementation of cycle inference engine using weighted heuristic signals
 * 
 * Target: ≥85% accuracy within ±2 days, ≥90% unit test coverage
 */
class CycleInferenceEngineImpl(
    private val config: CycleConfig = CycleConfig.DEFAULT
) : CycleInferenceEngine {

    override fun infer(
        transactions: List<Transaction>,
        dailyBalances: List<DailyBalance>?,
        tz: ZoneId,
        today: LocalDate
    ): CycleInferenceResult {
        if (transactions.isEmpty()) {
            return CycleInferenceResult(
                closeDay = null,
                cadence = Cadence.MONTHLY,
                confidence = 0.0,
                nextCloseDate = null,
                candidateDays = emptyMap(),
                signalWeights = emptyMap()
            )
        }

        val txByDate = transactions.groupBy { it.postedAt.withZoneSameInstant(tz).toLocalDate() }
        val daysHistogram = mutableMapOf<Int, Double>()
        val signals = mutableMapOf<String, Double>()

        // H1: Statement markers & keywords
        val markerWeight = config.statementMarkerWeight
        for ((date, txs) in txByDate) {
            val hasMarker = txs.any { 
                it.isStatementMarker || 
                it.isInterest || 
                hasStatementKeyword(it.description) 
            }
            if (hasMarker) {
                vote(daysHistogram, date.dayOfMonth, markerWeight)
                accumulate(signals, "H1_statement_markers", markerWeight)
            }
        }

        // H2: Payment-window reset
        val resetWeight = config.paymentResetWeight
        val orderedDates = txByDate.keys.sorted()
        val dailyNet = orderedDates.associateWith { d -> txByDate[d]!!.sumOf { it.amount } }
        
        // Detect large payments followed by balance reset
        for (d in orderedDates) {
            val dayTx = txByDate[d]!!
            val bigPayment = dayTx.any { 
                it.isPayment && abs(it.amount) >= estimatePaymentThreshold(transactions) 
            }
            if (bigPayment) {
                val prev = d.minusDays(1)
                vote(daysHistogram, prev.dayOfMonth, resetWeight)
                accumulate(signals, "H2_payment_reset", resetWeight)
            }
        }

        // H2: Enhanced with balance drops if available
        if (!dailyBalances.isNullOrEmpty()) {
            val byDate = dailyBalances.associateBy { it.date }
            for (i in 1 until dailyBalances.size) {
                val prev = dailyBalances[i - 1]
                val cur = dailyBalances[i]
                val drop = prev.balance - cur.balance
                if (drop > 0 && drop >= estimatePaymentThreshold(transactions) * config.balanceDropThreshold) {
                    val close = cur.date.minusDays(1)
                    vote(daysHistogram, close.dayOfMonth, resetWeight * 1.1)
                    accumulate(signals, "H2_balance_drop", resetWeight * 1.1)
                }
            }
        }

        // H3: Payment cadence back-inference
        val backInferWeight = config.paymentBackInferenceWeight
        val paymentDates = orderedDates.filter { d -> 
            txByDate[d]!!.any { it.isPayment || it.amount < 0 } 
        }
        val clusters = clusterDatesByMonthDay(paymentDates)
        for (centroid in clusters) {
            for (offset in -config.backInferenceOffsetRange..config.backInferenceOffsetRange) {
                val hypotheticalClose = centroid.minusDays(config.dueMinusCloseOffset.toLong() + offset)
                vote(daysHistogram, hypotheticalClose.dayOfMonth, 
                    backInferWeight * (1.0 - abs(offset) / 7.0))
            }
            accumulate(signals, "H3_payment_back_infer", backInferWeight)
        }

        // H4: Cycle netting transitions
        val netWeight = config.cycleNettingWeight
        val running = mutableListOf<Pair<LocalDate, Double>>()
        var rollingSum = 0.0
        for (d in orderedDates) {
            rollingSum += dailyNet[d] ?: 0.0
            running += d to rollingSum
        }
        
        for (i in 1 until running.size) {
            val (dPrev, sumPrev) = running[i - 1]
            val (dCur, sumCur) = running[i]
            if (sumPrev > sumCur && (sumPrev - sumCur) >= estimatePaymentThreshold(transactions) * config.netTransitionThreshold) {
                val close = dCur.minusDays(1)
                vote(daysHistogram, close.dayOfMonth, netWeight)
                accumulate(signals, "H4_net_transition", netWeight)
            }
        }

        // H5: Description keywords
        val keywordWeight = config.keywordWeight
        for ((date, txs) in txByDate) {
            val hasKeywords = txs.any { hasStatementKeyword(it.description) }
            if (hasKeywords) {
                vote(daysHistogram, date.dayOfMonth, keywordWeight)
                accumulate(signals, "H5_keywords", keywordWeight)
            }
        }

        // Aggregate & choose close day
        val totalWeight = daysHistogram.values.sum().coerceAtLeast(1e-9)
        val best = daysHistogram.maxByOrNull { it.value }
        val closeDay = best?.key

        // Cadence detection via boundary intervals
        val boundaryDates = estimateCycleBoundaries(orderedDates, daysHistogram)
        val cadence = detectCadence(boundaryDates)

        // Calculate confidence
        val confidence = if (closeDay == null) {
            0.0
        } else {
            val baseConfidence = (best!!.value / totalWeight).coerceIn(0.0, 1.0)
            val supportConfidence = confidenceFromSupport(daysHistogram, closeDay)
            val cadenceConf = cadenceConfidence(cadence)
            
            // Apply new card penalty if insufficient data
            val dataMonths = transactions.map { it.postedAt.withZoneSameInstant(tz).toLocalDate() }
                .distinct().sorted().let { dates ->
                    if (dates.size < 2) 0.0
                    else ChronoUnit.DAYS.between(dates.first(), dates.last()) / 30.0
                }
            
            val newCardPenalty = if (dataMonths < config.minDataMonths) {
                config.newCardMaxConfidence
            } else {
                1.0
            }
            
            (baseConfidence * supportConfidence * cadenceConf * newCardPenalty).coerceIn(0.0, 1.0)
        }

        // Calculate next close date
        val lastBoundary = boundaryDates.maxOrNull()
        val nextCloseDate = lastBoundary?.let { advanceNext(it, closeDay!!, cadence) }

        return CycleInferenceResult(
            closeDay = closeDay,
            cadence = cadence,
            confidence = confidence,
            nextCloseDate = nextCloseDate,
            candidateDays = daysHistogram.toSortedMap(),
            signalWeights = signals.toSortedMap()
        )
    }

    private fun vote(hist: MutableMap<Int, Double>, day: Int, weight: Double) {
        val d = day.coerceIn(1, 31)
        hist[d] = (hist[d] ?: 0.0) + weight
    }

    private fun accumulate(map: MutableMap<String, Double>, key: String, weight: Double) {
        map[key] = (map[key] ?: 0.0) + weight
    }

    private fun hasStatementKeyword(desc: String): Boolean {
        val d = desc.lowercase()
        return listOf(
            "statement", "interest", "finance charge", "previous balance", 
            "bill cycle", "statement balance", "minimum payment"
        ).any { it in d }
    }

    private fun estimatePaymentThreshold(transactions: List<Transaction>): Double {
        val payments = transactions
            .filter { it.isPayment || it.amount < 0 }
            .map { abs(it.amount) }
        
        if (payments.isEmpty()) return 100.0
        
        val p75 = percentile(payments, config.paymentPercentile)
        return maxOf(config.minPaymentAmount, p75)
    }

    private fun percentile(values: List<Double>, p: Double): Double {
        if (values.isEmpty()) return 0.0
        
        val v = values.sorted()
        val idx = (p * (v.size - 1)).coerceIn(0.0, (v.size - 1).toDouble())
        val lo = v[idx.toInt()]
        val hi = v[minOf(idx.toInt() + 1, v.lastIndex)]
        val frac = idx - idx.toInt()
        return lo + (hi - lo) * frac
    }

    private fun clusterDatesByMonthDay(dates: List<LocalDate>): List<LocalDate> {
        if (dates.isEmpty()) return emptyList()
        
        val byDay = dates.groupBy { it.dayOfMonth }
        val top = byDay.entries
            .sortedByDescending { it.value.size }
            .take(2)
            .map { (dom, ds) ->
                // Centroid = most recent occurrence on that day of month
                ds.maxOrNull()!!.withDayOfMonth(dom)
            }
        return top
    }

    private fun estimateCycleBoundaries(
        orderedDates: List<LocalDate>, 
        hist: Map<Int, Double>
    ): List<LocalDate> {
        val peaks = hist.entries
            .sortedByDescending { it.value }
            .take(2)
            .map { it.key }
        
        val boundaries = mutableListOf<LocalDate>()
        for (d in orderedDates) {
            if (d.dayOfMonth in peaks) boundaries += d
        }
        return boundaries.distinct().sorted()
    }

    private fun detectCadence(boundaries: List<LocalDate>): Cadence {
        if (boundaries.size < 3) return Cadence.MONTHLY
        
        val intervals = boundaries.zipWithNext { a, b -> 
            ChronoUnit.DAYS.between(a, b).toInt() 
        }
        val median = intervals.sorted()[intervals.size / 2]
        
        return when {
            median in config.monthlyIntervalRange -> Cadence.MONTHLY
            median in config.fourWeekIntervalRange -> Cadence.FOUR_WEEK // Bias toward monthly
            median in config.biweeklyIntervalRange -> Cadence.BIWEEKLY
            else -> Cadence.MONTHLY
        }
    }

    private fun confidenceFromSupport(hist: Map<Int, Double>, chosen: Int): Double {
        val total = hist.values.sum().coerceAtLeast(1e-9)
        val top = hist[chosen] ?: 0.0
        val next = hist.entries
            .filter { it.key != chosen }
            .maxByOrNull { it.value }
            ?.value ?: 0.0
        
        val margin = (top - next).coerceAtLeast(0.0) / total
        val concentration = top / total
        return (0.5 * margin + 0.5 * concentration).coerceIn(0.0, 1.0)
    }

    private fun cadenceConfidence(c: Cadence): Double = when (c) {
        Cadence.MONTHLY -> 1.0
        Cadence.FOUR_WEEK -> 0.9
        Cadence.BIWEEKLY -> 0.85
    }

    private fun advanceNext(lastBoundary: LocalDate, closeDay: Int, cadence: Cadence): LocalDate {
        return when (cadence) {
            Cadence.MONTHLY -> {
                val nextMonth = lastBoundary.plusMonths(1)
                val maxDay = nextMonth.lengthOfMonth()
                nextMonth.withDayOfMonth(minOf(closeDay, maxDay))
            }
            Cadence.FOUR_WEEK -> lastBoundary.plusDays(28)
            Cadence.BIWEEKLY -> lastBoundary.plusDays(14)
        }
    }
}
