package im.ilyk.ccoptimizer.calendar

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import im.ilyk.ccoptimizer.data.db.dao.CardDao
import im.ilyk.ccoptimizer.data.db.dao.CycleDao
import java.io.File
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for ICS calendar export functionality
 */
@Singleton
class IcsExportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cardDao: CardDao,
    private val cycleDao: CycleDao,
    private val icsGenerator: IcsGenerator = IcsGenerator(),
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    
    /**
     * Generate ICS file for all cards and return file path
     */
    suspend fun generateIcsFile(): File {
        val cards = cardDao.getAllActive()
        val cycles = cards.associate { card ->
            card.cardId to cycleDao.latest(card.cardId)
        }.filterValues { it != null }.mapValues { it.value!! }
        
        val icsContent = icsGenerator.generate(cards, cycles, zoneId)
        
        // Write to cache directory
        val file = File(context.cacheDir, "payment_schedule.ics")
        file.writeText(icsContent)
        
        return file
    }
    
    /**
     * Generate ICS for a single card with recurring events
     */
    suspend fun generateIcsFileForCard(cardId: String, monthsAhead: Int = 12): File? {
        val card = cardDao.get(cardId) ?: return null
        val cycle = cycleDao.latest(cardId) ?: return null
        
        val icsContent = icsGenerator.generateRecurring(card, cycle, monthsAhead, zoneId)
        
        if (icsContent.isEmpty()) return null
        
        val file = File(context.cacheDir, "payment_schedule_${card.last4}.ics")
        file.writeText(icsContent)
        
        return file
    }
    
    /**
     * Create share intent for ICS file
     */
    fun createShareIntent(file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/calendar"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Credit Card Payment Schedule")
            putExtra(Intent.EXTRA_TEXT, "Payment schedule for your credit cards")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    
    /**
     * Export and share ICS file
     */
    suspend fun exportAndShare(): Intent? {
        val file = generateIcsFile()
        return createShareIntent(file)
    }
    
    /**
     * Export ICS for a single card
     */
    suspend fun exportCardAndShare(cardId: String): Intent? {
        val file = generateIcsFileForCard(cardId) ?: return null
        return createShareIntent(file)
    }
}
