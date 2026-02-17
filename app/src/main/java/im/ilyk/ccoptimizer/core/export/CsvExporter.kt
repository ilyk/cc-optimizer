package im.ilyk.ccoptimizer.core.export

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import im.ilyk.ccoptimizer.data.db.entities.BalanceEntity
import im.ilyk.ccoptimizer.data.db.entities.CardEntity
import im.ilyk.ccoptimizer.data.db.entities.CycleEntity
import im.ilyk.ccoptimizer.data.db.entities.PaymentEntity
import java.time.format.DateTimeFormatter

/**
 * CSV Exporter for credit card data
 * 
 * Exports cards, cycles, balances, and payments to RFC4180 compliant CSV format.
 * Organizes data into sections with headers.
 */
object CsvExporter {
    
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val dateTimeFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME
    
    /**
     * Export all data to a single CSV file with multiple sections
     * 
     * @param cards List of card entities
     * @param cycles List of cycle inference entities
     * @param balances List of daily balance entities
     * @param payments List of payment entities
     * @return CSV string with all data
     */
    fun export(
        cards: List<CardEntity>,
        cycles: List<CycleEntity>,
        balances: List<BalanceEntity>,
        payments: List<PaymentEntity>
    ): String {
        val csv = StringBuilder()
        
        // Section 1: Cards
        csv.appendLine("[Cards]")
        csv.appendLine(csvRow(listOf("cardId", "issuer", "last4", "displayName", "openedAt", "isActive")))
        cards.forEach { card ->
            csv.appendLine(csvRow(listOf(
                card.cardId,
                card.issuer ?: "",
                card.last4 ?: "",
                card.displayName ?: "",
                card.openedAt?.format(dateFormatter) ?: "",
                card.isActive.toString()
            )))
        }
        csv.appendLine()
        
        // Section 2: Cycles
        csv.appendLine("[Cycles]")
        csv.appendLine(csvRow(listOf(
            "id", "cardId", "closeDay", "cadence", "confidence", "nextCloseDate", "computedAt"
        )))
        cycles.forEach { cycle ->
            csv.appendLine(csvRow(listOf(
                cycle.id.toString(),
                cycle.cardId,
                cycle.closeDay?.toString() ?: "",
                cycle.cadence,
                String.format("%.4f", cycle.confidence),
                cycle.nextCloseDate?.format(dateFormatter) ?: "",
                cycle.computedAt.format(dateTimeFormatter)
            )))
        }
        csv.appendLine()
        
        // Section 3: Balances
        csv.appendLine("[Balances]")
        csv.appendLine(csvRow(listOf("id", "cardId", "date", "balance")))
        balances.forEach { balance ->
            csv.appendLine(csvRow(listOf(
                balance.id.toString(),
                balance.cardId,
                balance.date.format(dateFormatter),
                String.format("%.2f", balance.balance)
            )))
        }
        csv.appendLine()
        
        // Section 4: Payments
        csv.appendLine("[Payments]")
        csv.appendLine(csvRow(listOf("cardId", "postedDate", "amountCents", "createdAt", "source")))
        payments.forEach { payment ->
            csv.appendLine(csvRow(listOf(
                payment.cardId,
                payment.postedDate.format(dateFormatter),
                payment.amountCents.toString(),
                payment.createdAt.toString(),
                payment.source
            )))
        }
        
        return csv.toString()
    }
    
    /**
     * Format a list of values as a CSV row
     * Escapes quotes and wraps fields containing commas, quotes, or newlines
     * 
     * @param values List of values to format
     * @return CSV-formatted row string
     */
    private fun csvRow(values: List<String>): String {
        return values.joinToString(",") { value ->
            escapeCsvField(value)
        }
    }
    
    /**
     * Escape a CSV field value per RFC4180
     * 
     * @param field The field value to escape
     * @return Escaped field value
     */
    private fun escapeCsvField(field: String): String {
        // If field contains comma, quote, or newline, wrap in quotes
        // and escape internal quotes by doubling them
        return if (field.contains(',') || field.contains('"') || field.contains('\n')) {
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }
    }
    
    /**
     * Generate a filename for the export
     * 
     * @return Filename in format: credit-planner_export_YYYYMMDD.csv
     */
    fun generateFilename(): String {
        val now = java.time.LocalDate.now()
        val dateStr = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        return "credit-planner_export_$dateStr.csv"
    }
}


