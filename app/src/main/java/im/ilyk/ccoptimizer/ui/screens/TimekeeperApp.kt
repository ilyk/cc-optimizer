package im.ilyk.ccoptimizer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.hilt.navigation.compose.hiltViewModel
import im.ilyk.ccoptimizer.data.models.*
import im.ilyk.ccoptimizer.ui.navigation.TimekeeperBottomNavigation
import im.ilyk.ccoptimizer.ui.navigation.TimekeeperAppBar
import im.ilyk.ccoptimizer.ui.viewmodel.OptimizationViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Main Timekeeper App Container
 * Orchestrates all screens with Timekeeper design system
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimekeeperApp(
    viewModel: OptimizationViewModel = hiltViewModel(),
    onCardClick: (CreditCard) -> Unit = {},
    onConnectPlaid: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Collect state from ViewModel
    val cards by viewModel.cards.collectAsState()
    val paymentPlan by viewModel.paymentPlan.collectAsState()
    val scenarioComparison by viewModel.scenarioComparison.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var currentScreen by remember { mutableStateOf("dashboard") }
    var autoOptimize by remember { mutableStateOf(false) }

    // Convert payment plan to recommendations for existing UI
    val recommendations = paymentPlan?.scheduledPayments?.mapNotNull { payment: ScheduledPayment ->
        val card = cards.find { it.id == payment.cardId }
        if (card != null) {
            PaymentRecommendation(
                creditCard = card,
                recommendedPaymentDate = payment.dueDate,
                recommendedAmount = payment.amount,
                statementDueDate = card.statementDueDate ?: payment.dueDate,
                daysUntilPayment = java.time.temporal.ChronoUnit.DAYS.between(
                    java.time.LocalDate.now(),
                    payment.dueDate
                ).toInt(),
                reasoning = if (payment.isCompleted) "Payment completed." else "Payment scheduled."
            )
        } else null
    } ?: emptyList()

    val cardsNeedingInput = cards.filter { !it.isDateVerified }

    Scaffold(
        topBar = {
            TimekeeperAppBar(
                title = when (currentScreen) {
                    "dashboard" -> "Dashboard"
                    "optimize" -> "Optimize"
                    "insights" -> "Insights"
                    "profile" -> "Profile"
                    else -> "Credit Card Optimizer"
                },
                subtitle = when (currentScreen) {
                    "dashboard" -> "Financial rhythm overview"
                    "optimize" -> "Bring your payments into sync"
                    "insights" -> "Insights that anticipate your rhythm"
                    "profile" -> "Settings & preferences"
                    else -> null
                },
                balance = if (currentScreen == "dashboard") {
                    val total = recommendations.sumOf { it.recommendedAmount }
                    "$${String.format("%.2f", total)}"
                } else null
            )
        },
        bottomBar = {
            TimekeeperBottomNavigation(
                currentRoute = currentScreen,
                onNavigate = { screen -> currentScreen = screen }
            )
        },
        containerColor = Color.Transparent,
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Show error if present
            val currentError = error
            if (currentError != null) {
                LaunchedEffect(currentError) {
                    // Show error toast or snackbar
                    // For now, we'll just clear the error after a delay
                    kotlinx.coroutines.delay(3000)
                    viewModel.clearError()
                }
            }
            when (currentScreen) {
                "dashboard" -> {
                    TimekeeperDashboard(
                        recommendations = recommendations,
                        cardsNeedingInput = cardsNeedingInput,
                        isRefreshing = isLoading,
                        onRefresh = { viewModel.refresh() },
                        onCardClick = onCardClick,
                        onOptimizeClick = { currentScreen = "optimize" },
                        onAddCardClick = onConnectPlaid,
                        onInsightsClick = { currentScreen = "insights" }
                    )
                }

                "optimize" -> {
                    TimekeeperOptimizeScreen(
                        currentScore = calculateOptimizationScore(recommendations),
                        predictedSavings = paymentPlan?.totalSavings ?: 0.0,
                        daysRemaining = calculateDaysRemaining(recommendations),
                        isOptimizing = isLoading,
                        onRecalculateFlow = {
                            viewModel.recalculatePlan()
                        },
                        onCompareScenarios = {
                            viewModel.compareScenarios()
                            currentScreen = "insights"
                        },
                        onToggleAutoOptimize = { enabled ->
                            autoOptimize = enabled
                            // TODO: Implement auto-optimization background job
                        }
                    )
                }

                "insights" -> {
                    TimekeeperInsightsScreen(
                        insights = generateInsights(recommendations, cardsNeedingInput),
                        cashFlowRhythm = calculateCashFlowRhythm(recommendations),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                "profile" -> {
                    TimekeeperProfileScreen(
                        userEmail = "user@example.com",
                        isTwoFactorEnabled = true,
                        encryptionStatus = "End-to-end encrypted",
                        onThemeChange = { theme ->
                            // Handle theme change
                        },
                        onSecuritySettings = {
                            // Navigate to security settings
                        },
                        onAboutTimekeeper = {
                            // Show Timekeeper story
                        }
                    )
                }
            }
        }
    }
}

// Helper functions for calculations
private fun calculateOptimizationScore(recommendations: List<PaymentRecommendation>): Int {
    if (recommendations.isEmpty()) return 0

    val today = java.time.LocalDate.now()
    val totalCards = recommendations.size
    var optimizedCards = 0

    recommendations.forEach { recommendation ->
        val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, recommendation.recommendedPaymentDate).toInt()
        // Consider a card optimized if payment is within 3 days of statement date
        if (daysUntil <= 3 && daysUntil >= 0) {
            optimizedCards++
        }
    }

    return ((optimizedCards.toFloat() / totalCards) * 100).toInt().coerceIn(0, 100)
}

private fun calculateDaysRemaining(recommendations: List<PaymentRecommendation>): Int {
    if (recommendations.isEmpty()) return 0
    val today = java.time.LocalDate.now()
    return recommendations.minOfOrNull {
        java.time.temporal.ChronoUnit.DAYS.between(today, it.recommendedPaymentDate).toInt()
    } ?: 0
}

private fun calculateCashFlowRhythm(recommendations: List<PaymentRecommendation>): Float {
    if (recommendations.isEmpty()) return 0.5f

    val today = java.time.LocalDate.now()
    val totalCards = recommendations.size
    var wellTimedCards = 0

    recommendations.forEach { recommendation ->
        val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, recommendation.recommendedPaymentDate).toInt()
        // Consider well-timed if payment is 1-2 days before statement
        if (daysUntil in 1..2) {
            wellTimedCards++
        }
    }

    return (wellTimedCards.toFloat() / totalCards).coerceIn(0f, 1f)
}

private fun generateInsights(recommendations: List<PaymentRecommendation>, cardsNeedingInput: List<CreditCard>): List<InsightData> {
    val insights = mutableListOf<InsightData>()

    // Insight 1: Payment timing
    if (recommendations.isNotEmpty()) {
        val today = java.time.LocalDate.now()
        val urgentPayments = recommendations.count {
            java.time.temporal.ChronoUnit.DAYS.between(today, it.recommendedPaymentDate).toInt() <= 3
        }

        insights.add(
            InsightData(
                title = "Payment Timing",
                narrative = if (urgentPayments > 0) {
                    "$urgentPayments payments need attention within 3 days"
                } else {
                    "All payments are well-timed"
                },
                value = if (urgentPayments > 0) urgentPayments.toString() else null,
                icon = androidx.compose.material.icons.Icons.Default.Schedule,
                gradient = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF5865F2),
                        Color(0xFF8E44AD)
                    )
                ),
                trend = if (urgentPayments == 0) androidx.compose.material.icons.Icons.Default.CheckCircle else androidx.compose.material.icons.Icons.Default.Warning
            )
        )
    }

    // Insight 2: Savings potential
    val totalSavings = recommendations.sumOf { it.recommendedAmount }
    insights.add(
        InsightData(
            title = "Savings Potential",
            narrative = if (totalSavings > 0) {
                "$${String.format("%.2f", totalSavings)} in potential interest savings"
            } else {
                "No immediate savings opportunities"
            },
            value = if (totalSavings > 0) "$${String.format("%.2f", totalSavings)}" else null,
            icon = androidx.compose.material.icons.Icons.Default.Savings,
            gradient = androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(
                    Color(0xFF3BA55D),
                    Color(0xFF2E7D32)
                )
            ),
            trend = if (totalSavings > 100) androidx.compose.material.icons.Icons.Default.TrendingUp else androidx.compose.material.icons.Icons.Default.TrendingDown
        )
    )

    // Insight 3: Cards needing input
    if (cardsNeedingInput.isNotEmpty()) {
        insights.add(
            InsightData(
                title = "Data Quality",
                narrative = "${cardsNeedingInput.size} cards need statement date verification",
                value = cardsNeedingInput.size.toString(),
                icon = androidx.compose.material.icons.Icons.Default.Warning,
                gradient = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFF9800),
                        Color(0xFFE65100)
                    )
                ),
                trend = androidx.compose.material.icons.Icons.Default.Info
            )
        )
    }

    return insights
}
