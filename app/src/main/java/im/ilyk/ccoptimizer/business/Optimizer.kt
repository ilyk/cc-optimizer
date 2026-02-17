package im.ilyk.ccoptimizer.business

import im.ilyk.ccoptimizer.data.models.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

/**
 * Optimization engine interface
 */
interface Optimizer {
    /**
     * Build an optimized payment plan
     * @param input Optimization input parameters
     * @return Optimized payment plan
     */
    fun buildPlan(input: OptimizationInput): PaymentPlan

    /**
     * Compare baseline vs optimized plans
     * @param input Optimization input parameters
     * @return Scenario comparison
     */
    fun comparePlans(input: OptimizationInput): ScenarioComparison
}

/**
 * Greedy optimization algorithm implementation
 * Minimizes interest while respecting constraints
 */
class GreedyOptimizer : Optimizer {

    private val horizonDays = 90

    override fun buildPlan(input: OptimizationInput): PaymentPlan {
        val horizonStart = LocalDate.now()
        val horizonEnd = horizonStart.plusDays(horizonDays.toLong())
        val horizon = enumerateDays(horizonStart, horizonEnd)

        // Use statementDueDate as the due date proxy; default to 30 days from now if null
        val dueByCard = input.cards.associate {
            it.id to (it.statementDueDate ?: horizonStart.plusDays(30))
        }

        val payments = mutableListOf<ScheduledPayment>()
        val balances = input.cards.associate { it.id to it.currentBalance }.toMutableMap()

        for (day in horizon) {
            var budget = getAvailableCash(input.constraints, day)

            // Greedy: pay highest-balance cards first (since we don't have APR)
            val candidates = input.cards
                .filter { (balances[it.id] ?: 0.0) > 0 }
                .sortedByDescending { balances[it.id] ?: 0.0 }

            for (card in candidates) {
                if (budget <= 0) break
                val currentBalance = balances[card.id] ?: 0.0
                if (currentBalance <= 0) continue

                val amount = min(budget, currentBalance)
                if (amount > 0) {
                    payments.add(
                        ScheduledPayment(
                            id = UUID.randomUUID().toString(),
                            cardId = card.id,
                            dueDate = day,
                            amount = amount
                        )
                    )
                    budget -= amount
                    balances[card.id] = max(0.0, currentBalance - amount)
                }
            }
        }

        val totalPaid = payments.sumOf { it.amount }
        val totalBalance = input.cards.sumOf { it.currentBalance }
        val savings = max(0.0, totalBalance - totalPaid) * 0.02 // estimated interest saved

        return PaymentPlan(
            id = UUID.randomUUID().toString(),
            createdAt = java.time.LocalDateTime.now(),
            scheduledPayments = mergeSmall(payments),
            totalInterest = totalBalance * 0.18 / 12, // rough monthly interest estimate
            totalSavings = savings
        )
    }

    override fun comparePlans(input: OptimizationInput): ScenarioComparison {
        val baseline = buildBaselinePlan(input)
        val optimized = buildPlan(input)
        val savings = baseline.totalInterest - optimized.totalInterest
        return ScenarioComparison(
            currentPlan = baseline,
            optimizedPlan = optimized,
            savings = savings,
            interestReduction = savings
        )
    }

    /**
     * Build baseline plan (minimum payments only)
     */
    private fun buildBaselinePlan(input: OptimizationInput): PaymentPlan {
        val today = LocalDate.now()
        val payments = input.cards.map { card ->
            ScheduledPayment(
                id = UUID.randomUUID().toString(),
                cardId = card.id,
                dueDate = card.statementDueDate ?: today.plusDays(30),
                amount = card.currentBalance * 0.02 // 2% minimum payment estimate
            )
        }

        val totalInterest = input.cards.sumOf { it.currentBalance * 0.18 / 12 }

        return PaymentPlan(
            id = UUID.randomUUID().toString(),
            createdAt = java.time.LocalDateTime.now(),
            scheduledPayments = payments,
            totalInterest = totalInterest,
            totalSavings = 0.0
        )
    }

    /**
     * Get available cash for a specific date
     */
    private fun getAvailableCash(constraints: List<CashflowConstraint>, date: LocalDate): Double {
        return constraints.find { it.date == date }?.maxAmount ?: 0.0
    }

    /**
     * Enumerate days in horizon
     */
    private fun enumerateDays(start: LocalDate, end: LocalDate): List<LocalDate> {
        val days = mutableListOf<LocalDate>()
        var current = start
        while (!current.isAfter(end)) {
            days.add(current)
            current = current.plusDays(1)
        }
        return days
    }

    /**
     * Merge small payments to reduce transaction count
     */
    private fun mergeSmall(payments: List<ScheduledPayment>): List<ScheduledPayment> {
        return payments.groupBy { "${it.cardId}-${it.dueDate}" }
            .map { (_, groupPayments) ->
                if (groupPayments.size == 1) {
                    groupPayments.first()
                } else {
                    val totalAmount = groupPayments.sumOf { it.amount }
                    ScheduledPayment(
                        id = UUID.randomUUID().toString(),
                        cardId = groupPayments.first().cardId,
                        dueDate = groupPayments.first().dueDate,
                        amount = totalAmount
                    )
                }
            }
    }
}
