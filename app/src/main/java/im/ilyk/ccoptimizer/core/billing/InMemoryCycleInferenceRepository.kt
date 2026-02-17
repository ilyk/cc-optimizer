package im.ilyk.ccoptimizer.core.billing

import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory implementation of CycleInferenceRepository for testing and development
 */
class InMemoryCycleInferenceRepository : CycleInferenceRepository {
    
    private val inferenceResults = ConcurrentHashMap<String, CycleInferenceResult>()
    
    override suspend fun getCycleInference(cardId: String): CycleInferenceResult? {
        return inferenceResults[cardId]
    }
    
    override suspend fun saveCycleInference(cardId: String, result: CycleInferenceResult) {
        inferenceResults[cardId] = result
    }
    
    override suspend fun getAllCycleInferences(): Map<String, CycleInferenceResult> {
        return inferenceResults.toMap()
    }
    
    override suspend fun clearCycleInference(cardId: String) {
        inferenceResults.remove(cardId)
    }
    
    override suspend fun getNextCloseDate(cardId: String): LocalDate? {
        return inferenceResults[cardId]?.nextCloseDate
    }
    
    override suspend fun getCardsWithApproachingCloseDates(days: Int): List<String> {
        val today = LocalDate.now()
        val cutoffDate = today.plusDays(days.toLong())
        
        return inferenceResults.entries
            .filter { (_, result) ->
                result.nextCloseDate?.let { closeDate ->
                    closeDate.isAfter(today) && closeDate.isBefore(cutoffDate) || closeDate.isEqual(cutoffDate)
                } ?: false
            }
            .map { it.key }
    }
}
