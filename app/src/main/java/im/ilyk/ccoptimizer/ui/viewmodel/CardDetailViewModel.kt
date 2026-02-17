package im.ilyk.ccoptimizer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import im.ilyk.ccoptimizer.data.db.dao.BalanceDao
import im.ilyk.ccoptimizer.data.db.dao.CycleDao
import im.ilyk.ccoptimizer.data.db.entities.PaymentEntity
import im.ilyk.ccoptimizer.data.repository.PaymentRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * ViewModel for card detail screen
 * 
 * Manages manual payment marking and utilization display
 */
@HiltViewModel
class CardDetailViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val balanceDao: BalanceDao,
    private val cycleDao: CycleDao
) : ViewModel() {
    
    private val _currentCardId = MutableStateFlow<String?>(null)
    
    /**
     * Observe payments for the current card
     */
    val payments: StateFlow<List<PaymentEntity>> = _currentCardId
        .filterNotNull()
        .flatMapLatest { cardId ->
            paymentRepository.observe(cardId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    private val _paymentOperation = MutableStateFlow<PaymentOperationState>(PaymentOperationState.Idle)
    val paymentOperation: StateFlow<PaymentOperationState> = _paymentOperation.asStateFlow()
    
    /**
     * Set the current card to display details for
     * Also syncs payments from backend to clean up any orphaned local data
     */
    fun setCard(cardId: String) {
        _currentCardId.value = cardId
        
        // Sync payments from backend to ensure we show accurate data
        viewModelScope.launch {
            try {
                paymentRepository.syncFromBackend(cardId)
                android.util.Log.d("CardDetailViewModel", "Synced payments from backend for card $cardId")
            } catch (e: Exception) {
                android.util.Log.w("CardDetailViewModel", "Failed to sync payments: ${e.message}")
                // Don't show error to user - local data is okay
            }
        }
    }
    
    /**
     * Mark a payment as paid
     * 
     * @param cardId The card ID
     * @param date The payment date
     * @param amountCents The payment amount in cents
     */
    fun markPaid(cardId: String, date: LocalDate, amountCents: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _paymentOperation.value = PaymentOperationState.Loading
            try {
                android.util.Log.d("CardDetailViewModel", "Marking payment: cardId=$cardId, date=$date, amount=$amountCents")
                paymentRepository.markPaid(cardId, date, amountCents)
                android.util.Log.d("CardDetailViewModel", "Payment marked successfully")
                _paymentOperation.value = PaymentOperationState.Success("Payment marked successfully!")
                
                // Notify parent to refresh card data
                onSuccess()
            } catch (e: Exception) {
                android.util.Log.e("CardDetailViewModel", "Error marking payment", e)
                val errorMsg = when {
                    e.message?.contains("FOREIGN KEY constraint failed") == true -> 
                        "Card not found in database. Please sync your cards first."
                    e.message?.contains("UNIQUE constraint failed") == true -> 
                        "This payment has already been recorded."
                    else -> "Error: ${e.message ?: "Unknown error"}"
                }
                _paymentOperation.value = PaymentOperationState.Error(errorMsg)
            } finally {
                // Reset to idle after a brief delay
                kotlinx.coroutines.delay(3000)
                _paymentOperation.value = PaymentOperationState.Idle
            }
        }
    }
    
    /**
     * Remove a payment record
     * 
     * @param cardId The card ID
     * @param date The payment date
     * @param amountCents The payment amount in cents
     */
    fun unmarkPaid(cardId: String, date: LocalDate, amountCents: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _paymentOperation.value = PaymentOperationState.Loading
            try {
                paymentRepository.unmark(cardId, date, amountCents)
                _paymentOperation.value = PaymentOperationState.Success("Payment removed")
                
                // Notify parent to refresh card data
                onSuccess()
            } catch (e: Exception) {
                _paymentOperation.value = PaymentOperationState.Error(e.message ?: "Unknown error")
            } finally {
                kotlinx.coroutines.delay(2000)
                _paymentOperation.value = PaymentOperationState.Idle
            }
        }
    }
}

/**
 * State for payment operations (mark/unmark)
 */
sealed class PaymentOperationState {
    object Idle : PaymentOperationState()
    object Loading : PaymentOperationState()
    data class Success(val message: String) : PaymentOperationState()
    data class Error(val message: String) : PaymentOperationState()
}


