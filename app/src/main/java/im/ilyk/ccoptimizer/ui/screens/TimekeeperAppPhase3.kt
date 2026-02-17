package im.ilyk.ccoptimizer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import im.ilyk.ccoptimizer.data.models.CreditCard
import im.ilyk.ccoptimizer.data.models.PaymentRecommendation
import im.ilyk.ccoptimizer.ui.components.*
import im.ilyk.ccoptimizer.ui.navigation.TimekeeperBottomNavigation
import im.ilyk.ccoptimizer.ui.utils.*

/**
 * Timekeeper App Phase 3 - Advanced Features
 * Integrates Lottie animations, haptic feedback, and sound cues
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimekeeperAppPhase3(
    recommendations: List<PaymentRecommendation>,
    cardsNeedingInput: List<CreditCard>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onCardClick: (CreditCard) -> Unit,
    onConnectPlaid: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf("dashboard") }
    
    // Phase 3: Advanced feedback systems
    val hapticFeedback = rememberTimekeeperHapticFeedback()
    val soundFeedback = rememberTimekeeperSoundFeedback()
    val combinedFeedback = rememberTimekeeperFeedback()
    
    // Financial rhythm calculations
    val financialRhythm = calculateFinancialRhythm(recommendations, cardsNeedingInput)
    val cashFlowVelocity = calculateCashFlowVelocity(recommendations, cardsNeedingInput)
    val optimizationPotential = calculateOptimizationPotential(recommendations, cardsNeedingInput)
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar with Phase 3 enhancements
        TopAppBar(
            title = { Text("Credit Optimizer") },
            actions = {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(
                        onClick = {
                            hapticFeedback(TimekeeperHapticType.MEDIUM_TAP)
                            soundFeedback(TimekeeperSoundType.BUTTON_TAP)
                            onRefresh()
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        // Main content with Phase 3 features
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (selectedTab) {
                "dashboard" -> {
                    TimekeeperDashboardPhase3(
                        recommendations = recommendations,
                        cardsNeedingInput = cardsNeedingInput,
                        financialRhythm = financialRhythm,
                        onCardClick = { card ->
                            hapticFeedback(TimekeeperHapticType.MEDIUM_TAP)
                            soundFeedback(TimekeeperSoundType.CARD_SELECT)
                            onCardClick(card)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                "optimize" -> {
                    TimekeeperOptimizePhase3(
                        recommendations = recommendations,
                        onOptimize = {
                            hapticFeedback(TimekeeperHapticType.OPTIMIZATION)
                            soundFeedback(TimekeeperSoundType.OPTIMIZATION_DONE)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                "insights" -> {
                    TimekeeperInsightsPhase3(
                        insights = emptyList(), // Would be populated with real insights
                        cashFlowRhythm = financialRhythm,
                        cashFlowVelocity = cashFlowVelocity,
                        optimizationPotential = optimizationPotential,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                "profile" -> {
                    TimekeeperProfilePhase3(
                        onConnectPlaid = {
                            hapticFeedback(TimekeeperHapticType.HEAVY_TAP)
                            soundFeedback(TimekeeperSoundType.BUTTON_TAP)
                            onConnectPlaid()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        
        // Timekeeper Bottom Navigation with Phase 3 enhancements
        TimekeeperBottomNavigation(
            currentRoute = selectedTab,
            onNavigate = { tab ->
                hapticFeedback(TimekeeperHapticType.LIGHT_TAP)
                soundFeedback(TimekeeperSoundType.BUTTON_TAP)
                selectedTab = tab
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Enhanced Dashboard with Phase 3 features
 */
@Composable
private fun TimekeeperDashboardPhase3(
    recommendations: List<PaymentRecommendation>,
    cardsNeedingInput: List<CreditCard>,
    financialRhythm: Float,
    onCardClick: (CreditCard) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Timekeeper Symbol Animation
        TimekeeperSymbolAnimation(
            isActive = true,
            size = 100,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        )
        
        // Cycle Health Card with enhanced animations
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Cycle Health: ${(financialRhythm * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Financial Rhythm: ${String.format("%.1f", financialRhythm)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // Credit Cards List with haptic feedback
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(cardsNeedingInput) { card ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onCardClick(card) }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = card.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Balance: $${card.currentBalance}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Enhanced Optimize screen with Phase 3 features
 */
@Composable
private fun TimekeeperOptimizePhase3(
    recommendations: List<PaymentRecommendation>,
    onOptimize: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Timekeeper Background Animation
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
        )
        
        // Interactive Optimization Dial with enhanced feedback
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Optimization Score: ${calculateOptimizationScore(recommendations)}%",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Total Savings: $${String.format("%.2f", calculateTotalSavings(recommendations))}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Days Remaining: ${calculateDaysRemaining(recommendations)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = onOptimize,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Optimize Now")
                }
            }
        }
    }
}

/**
 * Enhanced Insights screen with Phase 3 features
 */
@Composable
private fun TimekeeperInsightsPhase3(
    insights: List<Any>, // Would be proper insight data
    cashFlowRhythm: Float,
    cashFlowVelocity: Float,
    optimizationPotential: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Timekeeper Success Animation
        TimekeeperSuccessAnimation(
            isVisible = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        )
        
        // Enhanced insights content would go here
        Text(
            text = "Enhanced Insights with Phase 3 Features",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

/**
 * Enhanced Profile screen with Phase 3 features
 */
@Composable
private fun TimekeeperProfilePhase3(
    onConnectPlaid: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Timekeeper Loading Animation
        TimekeeperLoadingAnimation(
            isLoading = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        )
        
        // Enhanced profile content would go here
        Text(
            text = "Enhanced Profile with Phase 3 Features",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}


// Helper functions for calculations
private fun calculateOptimizationScore(recommendations: List<PaymentRecommendation>): Int {
    if (recommendations.isEmpty()) return 0
    return (recommendations.size * 100 / recommendations.size)
}

private fun calculateTotalSavings(recommendations: List<PaymentRecommendation>): Double {
    return recommendations.sumOf { it.recommendedAmount }
}

private fun calculateDaysRemaining(recommendations: List<PaymentRecommendation>): Int {
    if (recommendations.isEmpty()) return 0
    val today = java.time.LocalDate.now()
    return recommendations.minOfOrNull { 
        java.time.temporal.ChronoUnit.DAYS.between(today, it.recommendedPaymentDate).toInt()
    } ?: 0
}
