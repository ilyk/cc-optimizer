package im.ilyk.ccoptimizer.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import im.ilyk.ccoptimizer.core.export.CsvExporter
import im.ilyk.ccoptimizer.data.db.dao.BalanceDao
import im.ilyk.ccoptimizer.data.db.dao.CardDao
import im.ilyk.ccoptimizer.data.db.dao.CycleDao
import im.ilyk.ccoptimizer.data.db.dao.PaymentDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for data export functionality
 */
@HiltViewModel
class ExportViewModel @Inject constructor(
    private val cardDao: CardDao,
    private val cycleDao: CycleDao,
    private val balanceDao: BalanceDao,
    private val paymentDao: PaymentDao
) : ViewModel() {
    
    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()
    
    /**
     * Export data to CSV and share via system share sheet
     * 
     * @param context Application context for file I/O and sharing
     */
    fun exportToCsv(context: Context) {
        viewModelScope.launch {
            _exportState.value = ExportState.Exporting
            
            try {
                // Gather all data from database
                val cards = withContext(Dispatchers.IO) {
                    cardDao.getAllCards()
                }
                
                // Get cycles for all cards
                val cycles = withContext(Dispatchers.IO) {
                    cards.flatMap { card ->
                        cycleDao.getAllByCard(card.cardId)
                    }
                }
                
                // Get balances for all cards
                val balances = withContext(Dispatchers.IO) {
                    cards.flatMap { card ->
                        balanceDao.getByCardAndDateRange(
                            card.cardId,
                            java.time.LocalDate.now().minusYears(2),
                            java.time.LocalDate.now()
                        )
                    }
                }
                
                // Get payments for all cards
                val payments = withContext(Dispatchers.IO) {
                    cards.flatMap { card ->
                        paymentDao.byCard(card.cardId)
                    }
                }
                
                // Generate CSV
                val csvContent = CsvExporter.export(cards, cycles, balances, payments)
                
                // Write to cache directory
                val filename = CsvExporter.generateFilename()
                val cacheDir = context.cacheDir
                val file = File(cacheDir, filename)
                
                withContext(Dispatchers.IO) {
                    file.writeText(csvContent)
                }
                
                // Get file URI for sharing
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                
                _exportState.value = ExportState.Success(uri, filename)
                
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Export failed")
            }
        }
    }
    
    /**
     * Create a share intent for the exported file
     * 
     * @param context Application context
     * @param uri File URI to share
     * @return Share intent
     */
    fun createShareIntent(context: Context, uri: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Credit Card Data Export")
            putExtra(Intent.EXTRA_TEXT, "Exported credit card data from Credit Card Optimizer")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    
    /**
     * Reset export state to idle
     */
    fun resetState() {
        _exportState.value = ExportState.Idle
    }
}

/**
 * Export operation state
 */
sealed class ExportState {
    object Idle : ExportState()
    object Exporting : ExportState()
    data class Success(val uri: Uri, val filename: String) : ExportState()
    data class Error(val message: String) : ExportState()
}


