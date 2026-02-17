package im.ilyk.ccoptimizer.core.inference

import im.ilyk.ccoptimizer.data.db.entities.BalanceEntity
import im.ilyk.ccoptimizer.data.db.entities.CycleEntity
import im.ilyk.ccoptimizer.data.db.entities.PaymentEntity
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.min

/**
 * Represents a single utilization data point for a specific month
 * 
 * @param month The year-month this utilization applies to
 * @param percent The utilization percentage (0.0 to 100.0+)
 */
data class UtilizationPoint(
    val month: YearMonth,
    val percent: Double
)

/**
 * Pure functions for computing credit card utilization
 * 
 * Utilization is calculated as:
 * (Statement Balance / Credit Limit) * 100
 * 
 * Where Statement Balance = Balance at close date - Payments on/before close date
 */
object UtilizationCalc {
    
    /**
     * Compute monthly utilization for a single card
     * 
     * @param month The month to compute utilization for
     * @param dailyBalances Daily balance snapshots (at least covering the close date)
     * @param payments Manual payment records
     * @param cycleModel The card's billing cycle model (contains close day)
     * @param creditLimitCents The card's credit limit in cents
     * @return UtilizationPoint with the computed percentage
     */
    fun perCardMonthly(
        month: YearMonth,
        dailyBalances: List<BalanceEntity>,
        payments: List<PaymentEntity>,
        cycleModel: CycleEntity,
        creditLimitCents: Long
    ): UtilizationPoint {
        val closeDay = cycleModel.closeDay ?: 30
        val closeDate = LocalDate.of(
            month.year,
            month.month,
            min(closeDay, month.lengthOfMonth())
        )
        
        // Find balance at close date (convert to cents)
        val balanceAtClose = dailyBalances
            .find { it.date == closeDate }
            ?.balance
            ?.let { (it * 100).toLong() }
            ?: 0L
        
        // Sum payments on or before close date
        val payBeforeClose = payments
            .filter { !it.postedDate.isAfter(closeDate) }
            .sumOf { it.amountCents }
        
        // Statement balance = balance - payments (cannot be negative)
        val statementBalance = (balanceAtClose - payBeforeClose).coerceAtLeast(0)
        
        // Calculate percentage
        val pct = if (creditLimitCents > 0) {
            100.0 * statementBalance / creditLimitCents
        } else {
            0.0
        }
        
        return UtilizationPoint(month, pct)
    }
    
    /**
     * Compute monthly utilization for multiple cards (total/aggregate)
     * 
     * Weighted by credit limits to get accurate total utilization.
     * 
     * @param month The month to compute utilization for
     * @param cardData List of (balances, payments, cycle, limit) tuples for each card
     * @return UtilizationPoint with the aggregate percentage
     */
    fun totalMonthly(
        month: YearMonth,
        cardData: List<CardUtilizationData>
    ): UtilizationPoint {
        var totalStatementBalance = 0L
        var totalLimit = 0L
        
        for (data in cardData) {
            val point = perCardMonthly(
                month,
                data.balances,
                data.payments,
                data.cycle,
                data.creditLimitCents
            )
            
            // Convert percentage back to absolute balance
            val balance = (point.percent * data.creditLimitCents / 100.0).toLong()
            totalStatementBalance += balance
            totalLimit += data.creditLimitCents
        }
        
        val pct = if (totalLimit > 0) {
            100.0 * totalStatementBalance / totalLimit
        } else {
            0.0
        }
        
        return UtilizationPoint(month, pct)
    }
    
    /**
     * Compute utilization for the last N months
     * 
     * @param currentMonth The current month (defaults to now)
     * @param monthsBack Number of months to look back (inclusive of current)
     * @param balances All balance records for the card
     * @param payments All payment records for the card
     * @param cycle The card's cycle model
     * @param creditLimitCents The card's credit limit in cents
     * @return List of UtilizationPoints, oldest to newest
     */
    fun lastNMonths(
        currentMonth: YearMonth = YearMonth.now(),
        monthsBack: Int = 12,
        balances: List<BalanceEntity>,
        payments: List<PaymentEntity>,
        cycle: CycleEntity,
        creditLimitCents: Long
    ): List<UtilizationPoint> {
        val points = mutableListOf<UtilizationPoint>()
        
        for (i in (monthsBack - 1) downTo 0) {
            val month = currentMonth.minusMonths(i.toLong())
            val point = perCardMonthly(
                month,
                balances,
                payments,
                cycle,
                creditLimitCents
            )
            points.add(point)
        }
        
        return points
    }
}

/**
 * Data holder for computing total utilization across cards
 * 
 * @param balances Daily balance records for this card
 * @param payments Payment records for this card
 * @param cycle Billing cycle model for this card
 * @param creditLimitCents Credit limit in cents
 */
data class CardUtilizationData(
    val balances: List<BalanceEntity>,
    val payments: List<PaymentEntity>,
    val cycle: CycleEntity,
    val creditLimitCents: Long
)


