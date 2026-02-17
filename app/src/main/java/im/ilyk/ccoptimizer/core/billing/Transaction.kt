package im.ilyk.ccoptimizer.core.billing

import java.time.ZonedDateTime

/**
 * Represents a credit card transaction with all relevant metadata
 * for cycle inference analysis.
 */
data class Transaction(
    val id: String,
    val postedAt: ZonedDateTime,
    val amount: Double, // purchases > 0, payments < 0 by convention
    val description: String,
    val category: String?,
    val isPayment: Boolean = false,
    val isFee: Boolean = false,
    val isInterest: Boolean = false,
    val isStatementMarker: Boolean = false,
    val balanceAfter: Double? = null,
)

/**
 * Daily balance snapshot for enhanced cycle inference
 */
data class DailyBalance(
    val date: java.time.LocalDate,
    val balance: Double
)
