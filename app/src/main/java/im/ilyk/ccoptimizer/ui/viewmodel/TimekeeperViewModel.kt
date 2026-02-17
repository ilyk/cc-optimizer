package im.ilyk.ccoptimizer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import im.ilyk.ccoptimizer.data.models.ChatMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TimekeeperViewModel(application: Application) : AndroidViewModel(application) {

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                role = "assistant",
                content = "I'm Timekeeper, your payment timing assistant. Ask me about your cards, when to pay, or how to optimize your credit utilization."
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(role = "user", content = text.trim())
        _messages.value = _messages.value + userMessage

        viewModelScope.launch {
            _isTyping.value = true
            delay(1500) // Simulate API call delay

            // TODO: Replace with actual API call to POST /api/chat
            val response = generateMockResponse(text)
            _messages.value = _messages.value + response
            _isTyping.value = false
        }
    }

    private fun generateMockResponse(query: String): ChatMessage {
        val lowerQuery = query.lowercase()
        val content = when {
            lowerQuery.contains("when") && lowerQuery.contains("pay") ->
                "Based on your card data, I'd recommend paying before your statement closes. Connect your cards in the Home tab to get personalized payment timing recommendations."
            lowerQuery.contains("utilization") ->
                "Credit utilization is the percentage of your credit limit you're using. Keeping it below 10% before your statement closes can boost your credit score. I can help you time payments to achieve this."
            lowerQuery.contains("help") || lowerQuery.contains("what can you") ->
                "I can help you with:\n• When to pay each card\n• How to minimize utilization\n• Understanding statement close dates\n• Payment prioritization\n\nJust ask me anything about your credit cards!"
            else ->
                "I understand you're asking about \"${query.take(50)}\". Once connected to the backend, I'll be able to give you personalized advice based on your card data. For now, try asking about when to pay or credit utilization."
        }
        return ChatMessage(role = "assistant", content = content)
    }
}
