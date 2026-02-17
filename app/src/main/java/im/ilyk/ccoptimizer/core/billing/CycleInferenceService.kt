package im.ilyk.ccoptimizer.core.billing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.ZoneId

/**
 * Service that orchestrates cycle inference operations
 */
class CycleInferenceService(
    private val engine: CycleInferenceEngine,
    private val repository: CycleInferenceRepository
) {
    
    private val _inferenceResults = MutableStateFlow<Map<String, CycleInferenceResult>>(emptyMap())
    val inferenceResults: StateFlow<Map<String, CycleInferenceResult>> = _inferenceResults.asStateFlow()
    
    /**
     * Perform cycle inference for a card with given transaction data
     */
    suspend fun inferCycleForCard(
        cardId: String,
        transactions: List<Transaction>,
        dailyBalances: List<DailyBalance>? = null,
        tz: ZoneId = ZoneId.systemDefault(),
        today: LocalDate = LocalDate.now(tz)
    ): CycleInferenceResult {
        val result = engine.infer(transactions, dailyBalances, tz, today)
        
        // Save to repository
        repository.saveCycleInference(cardId, result)
        
        // Update state flow
        val currentResults = _inferenceResults.value.toMutableMap()
        currentResults[cardId] = result
        _inferenceResults.value = currentResults
        
        return result
    }
    
    /**
     * Get cycle inference result for a card
     */
    suspend fun getCycleInference(cardId: String): CycleInferenceResult? {
        return repository.getCycleInference(cardId)
    }
    
    /**
     * Get all cycle inference results
     */
    suspend fun getAllCycleInferences(): Map<String, CycleInferenceResult> {
        val results = repository.getAllCycleInferences()
        _inferenceResults.value = results
        return results
    }
    
    /**
     * Get cards with approaching close dates
     */
    suspend fun getCardsWithApproachingCloseDates(days: Int = 7): List<String> {
        return repository.getCardsWithApproachingCloseDates(days)
    }
    
    /**
     * Clear cycle inference data for a card
     */
    suspend fun clearCycleInference(cardId: String) {
        repository.clearCycleInference(cardId)
        
        val currentResults = _inferenceResults.value.toMutableMap()
        currentResults.remove(cardId)
        _inferenceResults.value = currentResults
    }
    
    /**
     * Refresh all cycle inferences (useful after sync)
     */
    suspend fun refreshAllInferences(
        cardTransactions: Map<String, List<Transaction>>,
        cardBalances: Map<String, List<DailyBalance>> = emptyMap(),
        tz: ZoneId = ZoneId.systemDefault()
    ) {
        val results = mutableMapOf<String, CycleInferenceResult>()
        
        for ((cardId, transactions) in cardTransactions) {
            val balances = cardBalances[cardId]
            val result = engine.infer(transactions, balances, tz)
            repository.saveCycleInference(cardId, result)
            results[cardId] = result
        }
        
        _inferenceResults.value = results
    }
}
