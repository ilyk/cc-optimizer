package im.ilyk.ccoptimizer.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import im.ilyk.ccoptimizer.core.billing.CycleInferenceEngine
import im.ilyk.ccoptimizer.data.db.AppDatabase
import im.ilyk.ccoptimizer.data.db.entities.CardEntity
import im.ilyk.ccoptimizer.data.db.entities.CycleEntity
import im.ilyk.ccoptimizer.data.repository.CycleRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Test Center debug screen
 */
@HiltViewModel
class TestCenterViewModel @Inject constructor(
    private val db: AppDatabase,
    private val cycleRepository: CycleRepositoryImpl,
    private val engine: CycleInferenceEngine,
    private val seeder: TestDataSeeder,
    private val syncRepository: im.ilyk.ccoptimizer.data.sync.SyncRepository,
    private val syncPrefs: im.ilyk.ccoptimizer.data.sync.SyncPrefs
) : ViewModel() {
    
    data class Explain(
        val candidateDays: Map<Int, Double>,
        val topSignals: List<Pair<String, Double>>
    )
    
    data class Ui(
        val cards: List<CardEntity> = emptyList(),
        val latestByCard: Map<String, CycleEntity?> = emptyMap(),
        val explain: Explain? = null,
        val status: String? = null,
        val isSyncing: Boolean = false,
        val lastSyncTime: Long? = null,
        val canSyncNow: Boolean = true
    )
    
    private val _ui = MutableStateFlow(Ui())
    val ui: StateFlow<Ui> = _ui
    
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    private val intDoubleMapType = Types.newParameterizedType(
        Map::class.java,
        Integer::class.java,
        java.lang.Double::class.java
    )
    
    private val stringDoubleMapType = Types.newParameterizedType(
        Map::class.java,
        String::class.java,
        java.lang.Double::class.java
    )
    
    private val candidateAdapter: JsonAdapter<Map<Int, Double>> = moshi.adapter(intDoubleMapType)
    private val signalsAdapter: JsonAdapter<Map<String, Double>> = moshi.adapter(stringDoubleMapType)
    
    init {
        refresh()
        updateSyncStatus()
    }
    
    // Sync functions
    fun syncNow(force: Boolean = false) = viewModelScope.launch {
        _ui.update { it.copy(isSyncing = true, status = "Syncing...") }
        
        try {
            val result = syncRepository.syncAll(force = force)
            
            if (result.fromCache) {
                _ui.update { it.copy(
                    isSyncing = false,
                    status = "Rate-limited. Try again later or use Force Sync."
                )}
            } else {
                _ui.update { it.copy(
                    isSyncing = false,
                    status = "Synced ${result.cards} card(s), ${result.transactions} tx, ${result.balances} balances in ${result.durationMs}ms"
                )}
            }
            
            refresh()
            updateSyncStatus()
        } catch (e: Exception) {
            _ui.update { it.copy(
                isSyncing = false,
                status = "Sync failed: ${e.message}"
            )}
        }
    }
    
    // Seeding functions
    fun seedTypicalMonthly() = launchAndToast("Seeded monthly card (close day 15)") {
        seeder.seedMonthly(closeDay = 15)
    }
    
    fun seedIrregular31() = launchAndToast("Seeded irregular card (close day 31)") {
        seeder.seedIrregular31()
    }
    
    fun seedNewCardLowHistory() = launchAndToast("Seeded new card (2 months history)") {
        seeder.seedNewCardLowHistory()
    }
    
    fun seedInterestMarker() = launchAndToast("Seeded card with strong interest markers") {
        seeder.seedInterestMarker()
    }
    
    // Actions
    fun recompute() = viewModelScope.launch {
        _ui.update { it.copy(status = "Recomputing cycles...") }
        
        try {
            val cards = db.cardDao().observeAll().first()
            
            if (cards.isEmpty()) {
                _ui.update { it.copy(status = "No cards found. Seed some test data first.") }
                return@launch
            }
            
            var computedCount = 0
            cards.forEach { card ->
                val transactions = seeder.loadTransactions(card.cardId)
                val balances = seeder.loadBalances(card.cardId)
                
                if (transactions.isNotEmpty()) {
                    cycleRepository.recomputeAndPersist(card.cardId, transactions, balances)
                    computedCount++
                }
            }
            
            refresh()
            
            // Pull last result for explainability
            val latest = cards.firstOrNull()?.let { db.cycleDao().latest(it.cardId) }
            latest?.let {
                val candidateDays = candidateAdapter.fromJson(it.candidateDaysJson) ?: emptyMap()
                val signalWeights = signalsAdapter.fromJson(it.signalWeightsJson) ?: emptyMap()
                
                val topSignals = signalWeights.entries
                    .sortedByDescending { entry -> entry.value }
                    .take(3)
                    .map { it.key to it.value }
                
                _ui.update { ui ->
                    ui.copy(
                        explain = Explain(candidateDays, topSignals),
                        status = "Recomputed $computedCount cycle(s) successfully"
                    )
                }
            } ?: _ui.update { it.copy(status = "Recomputed $computedCount cycle(s), but no results to show") }
            
        } catch (e: Exception) {
            _ui.update { it.copy(status = "Error: ${e.message}") }
        }
    }
    
    fun pruneCycles() = launchAndToast("Pruned old cycles (kept last 10 per card)") {
        val cards = db.cardDao().observeAll().first()
        cards.forEach { card ->
            db.cycleDao().pruneOldCycles(card.cardId, 10)
        }
    }
    
    fun showDbPath() = viewModelScope.launch {
        try {
            val path = db.openHelper.readableDatabase.path
            _ui.update { it.copy(status = "DB Path: $path") }
        } catch (e: Exception) {
            _ui.update { it.copy(status = "Error getting DB path: ${e.message}") }
        }
    }
    
    fun clearAllData() = launchAndToast("Cleared all test data") {
        seeder.clearAllTestData()
        _ui.update { it.copy(explain = null) }
    }
    
    fun simulateWrongKey() = viewModelScope.launch {
        _ui.update {
            it.copy(
                status = "⚠️ To test wrong key: Clear app data, then try to open DB with different key. " +
                        "This is safe in debug builds only."
            )
        }
    }
    
    // Private helpers
    private fun refresh() = viewModelScope.launch {
        try {
            val cards = db.cardDao().observeAll().first()
            val latestCycles = cards.associate { card ->
                card.cardId to db.cycleDao().latest(card.cardId)
            }
            
            _ui.update { it.copy(cards = cards, latestByCard = latestCycles) }
        } catch (e: Exception) {
            _ui.update { it.copy(status = "Error refreshing: ${e.message}") }
        }
    }
    
    private fun launchAndToast(message: String, block: suspend () -> Unit) = viewModelScope.launch {
        try {
            block()
            refresh()
            _ui.update { it.copy(status = message) }
        } catch (e: Exception) {
            _ui.update { it.copy(status = "Error: ${e.message}") }
        }
    }
    
    private fun updateSyncStatus() = viewModelScope.launch {
        try {
            val lastSync = syncPrefs.getLastSyncEpochMs()
            val canSync = syncPrefs.canSyncNow(global = true)
            
            _ui.update { it.copy(
                lastSyncTime = lastSync,
                canSyncNow = canSync
            )}
        } catch (e: Exception) {
            // Ignore sync status errors
        }
    }
}
