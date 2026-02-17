package im.ilyk.ccoptimizer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import im.ilyk.ccoptimizer.data.models.CreditCard
import im.ilyk.ccoptimizer.data.models.PaymentRecommendation
import im.ilyk.ccoptimizer.ui.viewmodel.MainViewModel
import im.ilyk.ccoptimizer.ui.viewmodel.UiState

/**
 * Timekeeper Integration Example
 * Shows how to integrate the new Timekeeper design system with existing app logic
 * Now includes Phase 2 enhancements: financial rhythm calculations and micro-interactions
 */
@Composable
fun TimekeeperIntegrationExample(
    viewModel: MainViewModel,
    onConnectPlaid: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
    // Extract data from existing UI state
    val (recommendations, cardsNeedingInput) = when (val state = uiState) {
        is UiState.Success -> {
            state.recommendations to state.cardsNeedingInput
        }
        else -> {
            emptyList<PaymentRecommendation>() to emptyList<CreditCard>()
        }
    }
    
    // Use the enhanced Timekeeper app with Phase 2 features
    TimekeeperAppEnhanced(
        recommendations = recommendations,
        cardsNeedingInput = cardsNeedingInput,
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshRecommendations() },
        onCardClick = { card ->
            // Handle card click - could navigate to detail screen
        },
        onConnectPlaid = onConnectPlaid,
        modifier = modifier
    )
}

/**
 * Migration Guide for Existing MainScreen
 * 
 * To migrate from the current MainScreen to the new Timekeeper design:
 * 
 * 1. Replace MainScreen with TimekeeperIntegrationExample in your navigation
 * 2. The new system automatically handles all existing UI states:
 *    - Loading states
 *    - Error states  
 *    - Success states with recommendations
 *    - Cards needing input
 * 
 * 3. All existing functionality is preserved:
 *    - Plaid connection
 *    - Card management
 *    - Payment recommendations
 *    - Refresh functionality
 * 
 * 4. New features added:
 *    - Timekeeper animations and micro-interactions
 *    - Enhanced visual hierarchy
 *    - Improved navigation with bottom tabs
 *    - Financial rhythm visualization
 *    - Cycle health scoring
 *    - Interactive optimization dial
 * 
 * Example usage in AppNavigation.kt:
 * 
 * ```kotlin
 * Screen.Main -> {
 *     TimekeeperIntegrationExample(
 *         viewModel = viewModel,
 *         onConnectPlaid = onConnectPlaid
 *     )
 * }
 * ```
 */
