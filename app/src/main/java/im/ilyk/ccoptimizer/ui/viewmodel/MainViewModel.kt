package im.ilyk.ccoptimizer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import im.ilyk.ccoptimizer.business.PaymentCalculator
import im.ilyk.ccoptimizer.data.local.CacheManager
import im.ilyk.ccoptimizer.data.models.PaymentRecommendation
import im.ilyk.ccoptimizer.data.repository.PlaidRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UiState {
    object Loading : UiState()
    object NotConnected : UiState()
    data class LoadingWithCards(
        val loadingCards: List<im.ilyk.ccoptimizer.data.models.CreditCard>,
        val totalCards: Int,
        val currentStep: String = "Fetching data..."
    ) : UiState()
    data class Success(
        val recommendations: List<PaymentRecommendation>,
        val cardsNeedingInput: List<im.ilyk.ccoptimizer.data.models.CreditCard> = emptyList()
    ) : UiState()
    data class Error(val message: String) : UiState()
}

sealed class RefreshState {
    object Idle : RefreshState()
    object Refreshing : RefreshState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = PlaidRepository(application.applicationContext)
    private val cacheManager = CacheManager.getInstance(application.applicationContext)
    
    // Initialize state with cached cards immediately (synchronous)
    private val _uiState = MutableStateFlow<UiState>(getInitialState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    private val _linkToken = MutableStateFlow<String?>(null)
    val linkToken: StateFlow<String?> = _linkToken.asStateFlow()
    
    init {
        // If we showed cached data, refresh from backend in background
        if (_uiState.value is UiState.Success) {
            viewModelScope.launch {
                loadCreditCards(isRefresh = true)
            }
        } else {
            // No cache, try loading from backend
            checkExistingAccounts()
        }
    }
    
    /**
     * Get initial state from cache (synchronous for instant display)
     */
    private fun getInitialState(): UiState {
        return try {
            val cachedCards = cacheManager.getCachedCreditCardsImmediate()
            if (cachedCards != null && cachedCards.isNotEmpty()) {
                android.util.Log.d("MainViewModel", "Loaded ${cachedCards.size} cards from cache")
                cachedCards.forEach { card ->
                    android.util.Log.d("MainViewModel", "Card: ${card.bankName}, Logo: ${card.logoUrl}")
                }
                val recommendations = PaymentCalculator.calculateRecommendations(cachedCards)
                val cardsNeedingInput = PaymentCalculator.getCardsNeedingInput(cachedCards)
                UiState.Success(recommendations, cardsNeedingInput)
            } else {
                android.util.Log.d("MainViewModel", "No cached cards, showing NotConnected")
                UiState.NotConnected
            }
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Error loading cache: ${e.message}")
            UiState.NotConnected
        }
    }
    
    /**
     * Checks if user already has connected accounts and loads them
     */
    private fun checkExistingAccounts() {
        viewModelScope.launch {
            // Try to load existing credit cards from CACHE ONLY (fast startup)
            repository.getCreditCards(forceRefresh = false).fold(
                onSuccess = { creditCards ->
                    if (creditCards.isEmpty()) {
                        // No accounts found, show connect screen
                        _uiState.value = UiState.NotConnected
                    } else {
                        // Show cached cards immediately (fast startup)
                        val recommendations = PaymentCalculator.calculateRecommendations(creditCards)
                        val cardsNeedingInput = PaymentCalculator.getCardsNeedingInput(creditCards)
                        _uiState.value = UiState.Success(recommendations, cardsNeedingInput)
                        
                        // Refresh from backend in background (updates will reflect automatically)
                        loadCreditCards(isRefresh = true)
                    }
                },
                onFailure = { error ->
                    // If error loading accounts, show connect screen
                    // (could be network error or no accounts)
                    _uiState.value = UiState.NotConnected
                }
            )
        }
    }
    
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
     * Clears the link token after Plaid Link completes or exits
     */
    fun clearLinkToken() {
        _linkToken.value = null
    }
    
    /**
     * Called after Plaid Link success with public token
     */
    fun onPlaidSuccess(publicToken: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            // Exchange public token for access token (stored securely on backend)
            repository.exchangePublicToken(publicToken).fold(
                onSuccess = { itemId ->
                    // Token exchanged successfully, force refresh from backend to get new cards
                    loadCreditCards(isRefresh = true)
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
                        
                        // Always show success state with cards, even if they need input
                        // The UI will prompt users to enter statement dates for cards that need it
                        _uiState.value = UiState.Success(recommendations, cardsNeedingInput)
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

