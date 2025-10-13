package im.ilyk.ccoptimizer.business

import im.ilyk.ccoptimizer.data.models.BankInstitution
import im.ilyk.ccoptimizer.data.models.CreditCard
import im.ilyk.ccoptimizer.data.models.PaymentRecommendation
import im.ilyk.ccoptimizer.utils.BusinessDayCalculator
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

object PaymentCalculator {
    
    /**
     * Calculates payment recommendations for all credit cards
     * Filters out cards that don't have a statement day (need user input)
     */
    fun calculateRecommendations(creditCards: List<CreditCard>): List<PaymentRecommendation> {
        return creditCards
            .filter { it.statementGenerationDay != null } // Only process cards with known statement dates
            .mapNotNull { card ->
                try {
                    calculateRecommendation(card)
                } catch (e: Exception) {
                    null // Skip cards that can't be calculated
                }
            }
            .sortedBy { it.recommendedPaymentDate }
    }
    
    /**
     * Get credit cards that need statement date input from user
     */
    fun getCardsNeedingInput(creditCards: List<CreditCard>): List<CreditCard> {
        return creditCards.filter { it.needsStatementDateInput }
    }
    
    /**
     * Calculates payment recommendation for a single credit card
     * 
     * Logic:
     * 1. Find the next statement generation date
     * 2. Calculate when payment needs to hit the account (day before statement)
     * 3. Account for bank processing time
     * 4. Account for weekends and holidays
     * 5. Recommend payment amount (current balance)
     * 
     * @throws IllegalArgumentException if statement generation day is null
     */
    fun calculateRecommendation(creditCard: CreditCard): PaymentRecommendation {
        val today = LocalDate.now()
        
        // Ensure we have a statement day - should never be null here due to filtering, but check anyway
        val statementDay = creditCard.statementGenerationDay 
            ?: throw IllegalArgumentException("Statement generation day is required for ${creditCard.name}")
        
        // Find the next statement generation date
        val nextStatementDate = getNextStatementDate(today, statementDay)
        
        // Payment should hit the account 1 day before statement generation
        val targetPaymentHitDate = nextStatementDate.minusDays(1)
        
        // Calculate when to initiate payment based on bank processing time
        val processingDays = creditCard.institution.processingDays
        val recommendedPaymentDate = BusinessDayCalculator.getBusinessDaysBefore(
            targetPaymentHitDate, 
            processingDays
        )
        
        // Ensure we don't recommend a date in the past
        val finalRecommendedDate = if (recommendedPaymentDate.isBefore(today)) {
            today
        } else {
            recommendedPaymentDate
        }
        
        // Calculate recommended amount (pay full balance to keep statement at $0)
        val recommendedAmount = creditCard.currentBalance
        
        val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, finalRecommendedDate).toInt()
        
        val reasoning = buildReasoningString(
            creditCard,
            nextStatementDate,
            targetPaymentHitDate,
            processingDays,
            daysUntil
        )
        
        return PaymentRecommendation(
            creditCard = creditCard,
            recommendedPaymentDate = finalRecommendedDate,
            recommendedAmount = recommendedAmount,
            statementDueDate = nextStatementDate,
            daysUntilPayment = daysUntil,
            reasoning = reasoning
        )
    }
    
    /**
     * Gets the next statement generation date based on the day of month
     * Handles month-end edge cases (e.g., day 31 in February becomes day 28/29)
     */
    private fun getNextStatementDate(fromDate: LocalDate, statementDay: Int): LocalDate {
        // Validate statement day is in valid range
        require(statementDay in 1..31) { "Statement day must be between 1 and 31, got: $statementDay" }
        
        // Calculate day for current month (accounting for shorter months)
        val dayThisMonth = minOf(statementDay, fromDate.lengthOfMonth())
        val currentMonth = fromDate.withDayOfMonth(dayThisMonth)
        
        return if (currentMonth.isAfter(fromDate)) {
            // Statement day hasn't occurred yet this month
            currentMonth
        } else {
            // Statement day already passed, use next month
            val nextMonth = fromDate.plusMonths(1)
            val dayNextMonth = minOf(statementDay, nextMonth.lengthOfMonth())
            nextMonth.withDayOfMonth(dayNextMonth)
        }
    }
    
    private fun buildReasoningString(
        card: CreditCard,
        statementDate: LocalDate,
        targetHitDate: LocalDate,
        processingDays: Int,
        daysUntil: Int
    ): String {
        return buildString {
            append("Statement closes on ${statementDate}. ")
            append("Payment must hit account by ${targetHitDate}. ")
            
            if (processingDays == 0) {
                append("${card.institution.displayName} processes instantly. ")
            } else {
                append("${card.institution.displayName} takes $processingDays business day(s) to process. ")
            }
            
            if (daysUntil == 0) {
                append("Payment should be made TODAY!")
            } else if (daysUntil == 1) {
                append("Payment should be made tomorrow.")
            } else {
                append("You have $daysUntil days to make the payment.")
            }
        }
    }
}

