package im.ilyk.ccoptimizer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import im.ilyk.ccoptimizer.business.PaymentCalculator
import im.ilyk.ccoptimizer.data.models.PaymentRecommendation
import im.ilyk.ccoptimizer.data.repository.PlaidRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UiState {
    object Loading : UiState()
    object NotConnected : UiState()
    data class Loading With(
        val loadingCards: List<com.ccoptimizer.app.data.models.CreditCard>,
        val totalCards: Int,
        val currentStep: String = "Fetching data..."
    ) : UiState()
    data class Success(
        val recommendations: List<PaymentRecommendation>,
        val cardsNeedingInput: List<com.ccoptimizer.app.data.models.CreditCard> = emptyList()
    ) : UiState()
    data class Error(val message: String) : UiState()
}

sealed class RefreshState {
    object Idle : RefreshState()
    object Refreshing : RefreshState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = PlaidRepository(application.applicationContext)
    
    private val _uiState = MutableStateFlow<UiState>(UiState.NotConnected)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    private val _linkToken = MutableStateFlow<String?>(null)
    val linkToken: StateFlow<String?> = _linkToken.asStateFlow()
    
    /**
     * Initiates Plaid Link flow by creating a link token
     */
    fun initiatePlaidLink() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            repository.createLinkToken().fold(
                onSuccess = { token ->
                    _linkToken.value = token
                },
                onFailure = { error ->
                    _uiState.value = UiState.Error("Failed to initialize Plaid: ${error.message}")
                }
            )
        }
    }
    
    /**
     * Called after Plaid Link success with public token
     */
    fun onPlaidSuccess(publicToken: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            // Exchange public token for access token
            repository.exchangePublicToken(publicToken).fold(
                onSuccess = { accessToken ->
                    // Fetch credit cards and calculate recommendations
                    loadCreditCards()
                },
                onFailure = { error ->
                    _uiState.value = UiState.Error("Failed to connect: ${error.message}")
                }
            )
        }
    }
    
    /**
     * Loads credit cards and calculates payment recommendations
     */
    fun loadCreditCards(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _isRefreshing.value = true
            } else {
                _uiState.value = UiState.Loading
            }
            
            // Force refresh when explicitly refreshing, use cache otherwise
            repository.getCreditCards(forceRefresh = isRefresh).fold(
                onSuccess = { creditCards ->
                    if (creditCards.isEmpty()) {
                        _uiState.value = UiState.Error("No credit cards found")
                    } else {
                        // Get recommendations for cards with known statement dates
                        val recommendations = PaymentCalculator.calculateRecommendations(creditCards)
                        
                        // Get cards that need user input for statement dates
                        val cardsNeedingInput = PaymentCalculator.getCardsNeedingInput(creditCards)
                        
                        if (recommendations.isEmpty() && cardsNeedingInput.isNotEmpty()) {
                            _uiState.value = UiState.Error(
                                "Statement dates could not be determined automatically. " +
                                "${cardsNeedingInput.size} card(s) need manual date input."
                            )
                        } else {
                            _uiState.value = UiState.Success(recommendations, cardsNeedingInput)
                        }
                    }
                },
                onFailure = { error ->
                    if (isRefresh) {
                        // Keep current state on refresh error
                        _isRefreshing.value = false
                    } else {
                        _uiState.value = UiState.Error("Failed to load cards: ${error.message}")
                    }
                }
            )
            
            if (isRefresh) {
                _isRefreshing.value = false
            }
        }
    }
    
    fun refreshRecommendations() {
        loadCreditCards(isRefresh = true)
    }
    
    /**
     * Set statement date for a card manually
     */
    fun setStatementDate(cardId: String, statementDay: Int) {
        viewModelScope.launch {
            // Save to local cache
            repository.saveStatementDateOverride(cardId, statementDay)
            // Refresh to show updated recommendations
            loadCreditCards(isRefresh = true)
        }
    }
}

