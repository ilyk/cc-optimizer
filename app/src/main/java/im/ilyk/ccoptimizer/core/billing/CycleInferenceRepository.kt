package im.ilyk.ccoptimizer.core.billing

import java.time.LocalDate
import java.time.ZoneId

/**
 * Repository interface for cycle inference operations
 */
interface CycleInferenceRepository {
    
    /**
     * Get the latest cycle inference result for a card
     */
    suspend fun getCycleInference(cardId: String): CycleInferenceResult?
    
    /**
     * Save cycle inference result for a card
     */
    suspend fun saveCycleInference(cardId: String, result: CycleInferenceResult)
    
    /**
     * Get all cycle inference results
     */
    suspend fun getAllCycleInferences(): Map<String, CycleInferenceResult>
    
    /**
     * Clear cycle inference data for a card
     */
    suspend fun clearCycleInference(cardId: String)
    
    /**
     * Get the next close date for a card
     */
    suspend fun getNextCloseDate(cardId: String): LocalDate?
    
    /**
     * Get cards with close dates approaching (within specified days)
     */
    suspend fun getCardsWithApproachingCloseDates(days: Int): List<String>
}
