package im.ilyk.ccoptimizer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import im.ilyk.ccoptimizer.business.Optimizer
import im.ilyk.ccoptimizer.data.models.*
import im.ilyk.ccoptimizer.data.repository.CardsRepository
import im.ilyk.ccoptimizer.data.services.CashflowService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ViewModel for optimization functionality
 * Manages optimization state and provides reactive data streams
 */
@Singleton
class OptimizationViewModel @Inject constructor(
    private val cardsRepository: CardsRepository,
    private val cashflowService: CashflowService,
    private val optimizer: Optimizer
) : ViewModel() {
    
    // State flows for reactive UI
    private val _cards = MutableStateFlow<List<CreditCard>>(emptyList())
    val cards: StateFlow<List<CreditCard>> = _cards.asStateFlow()
    
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()
    
    private val _cashflow = MutableStateFlow<List<CashflowConstraint>>(emptyList())
    val cashflow: StateFlow<List<CashflowConstraint>> = _cashflow.asStateFlow()
    
    private val _optimizationInput = MutableStateFlow<OptimizationInput?>(null)
    val optimizationInput: StateFlow<OptimizationInput?> = _optimizationInput.asStateFlow()
    
    private val _paymentPlan = MutableStateFlow<PaymentPlan?>(null)
    val paymentPlan: StateFlow<PaymentPlan?> = _paymentPlan.asStateFlow()
    
    private val _scenarioComparison = MutableStateFlow<ScenarioComparison?>(null)
    val scenarioComparison: StateFlow<ScenarioComparison?> = _scenarioComparison.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        // Watch cards from repository
        viewModelScope.launch {
            cardsRepository.watchCards().collect { cardList ->
                _cards.value = cardList
                updateOptimizationInput()
            }
        }
    }
    
    /**
     * Refresh all data from sources
     */
    fun refresh() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                // Sync cards
                val syncedCards = cardsRepository.syncCards()
                _cards.value = syncedCards
                
                // Fetch transactions for all cards
                val allTransactions = mutableListOf<Transaction>()
                for (card in syncedCards) {
                    val cardTransactions = cardsRepository.syncTxns(card.id)
                    allTransactions.addAll(cardTransactions)
                }
                _transactions.value = allTransactions
                
                // Fetch cashflow forecast
                val now = LocalDate.now()
                val forecast = cashflowService.forecastUserCashflow(
                    from = now,
                    to = now.plusDays(90)
                )
                _cashflow.value = forecast
                
                updateOptimizationInput()
                
            } catch (e: Exception) {
                _error.value = "Failed to refresh data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Recalculate optimization plan
     */
    fun recalculatePlan() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val input = _optimizationInput.value
                if (input != null) {
                    val plan = optimizer.buildPlan(input)
                    _paymentPlan.value = plan
                }
                
            } catch (e: Exception) {
                _error.value = "Failed to calculate plan: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Compare scenarios (baseline vs optimized)
     */
    fun compareScenarios() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val input = _optimizationInput.value
                if (input != null) {
                    val comparison = optimizer.comparePlans(input)
                    _scenarioComparison.value = comparison
                }
                
            } catch (e: Exception) {
                _error.value = "Failed to compare scenarios: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Update optimization input when data changes
     */
    private fun updateOptimizationInput() {
        val cards = _cards.value
        val transactions = _transactions.value
        val cashflow = _cashflow.value
        
        if (cards.isNotEmpty()) {
            val now = LocalDate.now()
            val input = OptimizationInput(
                cards = cards,
                availableCash = cashflow.sumOf { it.maxAmount },
                constraints = cashflow
            )
            _optimizationInput.value = input
            
            // Auto-recalculate plan when input changes
            recalculatePlan()
        }
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }
}
