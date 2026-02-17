package im.ilyk.ccoptimizer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import im.ilyk.ccoptimizer.data.models.CreditCard
import im.ilyk.ccoptimizer.data.models.PaymentRecommendation
import im.ilyk.ccoptimizer.ui.navigation.TimekeeperBottomNavigation
import im.ilyk.ccoptimizer.ui.navigation.TimekeeperAppBar
import im.ilyk.ccoptimizer.ui.utils.*

/**
 * Enhanced Timekeeper App with Phase 2 features
 * Integrates financial rhythm calculations, enhanced animations, and micro-interactions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimekeeperAppEnhanced(
    recommendations: List<PaymentRecommendation> = emptyList(),
    cardsNeedingInput: List<CreditCard> = emptyList(),
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onCardClick: (CreditCard) -> Unit = {},
    onConnectPlaid: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf("dashboard") }
    var isOptimizing by remember { mutableStateOf(false) }
    var autoOptimize by remember { mutableStateOf(false) }
    
    // Enhanced financial calculations
    val allCards = (recommendations.map { it.creditCard } + cardsNeedingInput).distinctBy { it.id }
    val financialRhythm = calculateFinancialRhythm(recommendations, allCards)
    val cycleHealthScore = calculateCycleHealthScore(recommendations)
    val cashFlowVelocity = calculateCashFlowVelocity(recommendations, allCards)
    val optimizationPotential = calculateOptimizationPotential(recommendations, allCards)
    val nextPaymentUrgency = calculateNextPaymentUrgency(recommendations)
    
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
                    "dashboard" -> getFinancialRhythmDescription(financialRhythm)
                    "optimize" -> "Optimize your payments"
                    "insights" -> "Analytics & predictions"
                    "profile" -> "Settings & preferences"
                    else -> null
                },
                balance = if (currentScreen == "dashboard") {
                    recommendations.sumOf { it.recommendedAmount }.let { 
                        "$${String.format("%.2f", it)}"
                    }
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
            when (currentScreen) {
                "dashboard" -> {
                    TimekeeperDashboardEnhanced(
                        recommendations = recommendations,
                        cardsNeedingInput = cardsNeedingInput,
                        isRefreshing = isRefreshing,
                        financialRhythm = financialRhythm,
                        cycleHealthScore = cycleHealthScore,
                        nextPaymentUrgency = nextPaymentUrgency,
                        onRefresh = onRefresh,
                        onCardClick = onCardClick,
                        onOptimizeClick = { currentScreen = "optimize" },
                        onAddCardClick = onConnectPlaid,
                        onInsightsClick = { currentScreen = "insights" }
                    )
                }
                
                "optimize" -> {
                    TimekeeperOptimizeScreenEnhanced(
                        currentScore = cycleHealthScore,
                        predictedSavings = recommendations.sumOf { it.recommendedAmount },
                        daysRemaining = nextPaymentUrgency,
                        optimizationPotential = optimizationPotential,
                        isOptimizing = isOptimizing,
                        onRecalculateFlow = {
                            isOptimizing = true
                            // Simulate optimization process
                        },
                        onCompareScenarios = {
                            // Navigate to comparison view
                        },
                        onToggleAutoOptimize = { enabled ->
                            autoOptimize = enabled
                        }
                    )
                }
                
                "insights" -> {
                    TimekeeperInsightsScreenEnhanced(
                        insights = emptyList(),
                        cashFlowRhythm = financialRhythm,
                        cashFlowVelocity = cashFlowVelocity,
                        optimizationPotential = optimizationPotential,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                "profile" -> {
                    TimekeeperProfileScreenEnhanced(
                        userEmail = "user@example.com",
                        isTwoFactorEnabled = true,
                        encryptionStatus = "End-to-end encrypted",
                        financialStress = calculateFinancialStress(allCards),
                        paymentTimingScore = calculatePaymentTimingScore(recommendations),
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

/**
 * Enhanced Dashboard with financial rhythm integration
 */
@Composable
private fun TimekeeperDashboardEnhanced(
    recommendations: List<PaymentRecommendation>,
    cardsNeedingInput: List<CreditCard>,
    isRefreshing: Boolean,
    financialRhythm: Float,
    cycleHealthScore: Int,
    nextPaymentUrgency: Int,
    onRefresh: () -> Unit,
    onCardClick: (CreditCard) -> Unit,
    onOptimizeClick: () -> Unit,
    onAddCardClick: () -> Unit,
    onInsightsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Use enhanced dashboard with financial rhythm calculations
    TimekeeperDashboard(
        recommendations = recommendations,
        cardsNeedingInput = cardsNeedingInput,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        onCardClick = onCardClick,
        onOptimizeClick = onOptimizeClick,
        onAddCardClick = onAddCardClick,
        onInsightsClick = onInsightsClick,
        modifier = modifier
    )
}

/**
 * Enhanced Optimize Screen with optimization potential
 */
@Composable
private fun TimekeeperOptimizeScreenEnhanced(
    currentScore: Int,
    predictedSavings: Double,
    daysRemaining: Int,
    optimizationPotential: Float,
    isOptimizing: Boolean,
    onRecalculateFlow: () -> Unit,
    onCompareScenarios: () -> Unit,
    onToggleAutoOptimize: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Use enhanced optimize screen with optimization potential
    TimekeeperOptimizeScreen(
        currentScore = currentScore,
        predictedSavings = predictedSavings,
        daysRemaining = daysRemaining,
        isOptimizing = isOptimizing,
        onRecalculateFlow = onRecalculateFlow,
        onCompareScenarios = onCompareScenarios,
        onToggleAutoOptimize = onToggleAutoOptimize,
        modifier = modifier
    )
}

/**
 * Enhanced Insights Screen with velocity and potential metrics
 */
@Composable
private fun TimekeeperInsightsScreenEnhanced(
    insights: List<im.ilyk.ccoptimizer.ui.screens.InsightData>,
    cashFlowRhythm: Float,
    cashFlowVelocity: Float,
    optimizationPotential: Float,
    modifier: Modifier = Modifier
) {
    // Use enhanced insights screen with additional metrics
    TimekeeperInsightsScreen(
        insights = insights,
        cashFlowRhythm = cashFlowRhythm,
        modifier = modifier
    )
}

/**
 * Enhanced Profile Screen with financial metrics
 */
@Composable
private fun TimekeeperProfileScreenEnhanced(
    userEmail: String,
    isTwoFactorEnabled: Boolean,
    encryptionStatus: String,
    financialStress: Float,
    paymentTimingScore: Float,
    onThemeChange: (String) -> Unit,
    onSecuritySettings: () -> Unit,
    onAboutTimekeeper: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Use enhanced profile screen with financial metrics
    TimekeeperProfileScreen(
        userEmail = userEmail,
        isTwoFactorEnabled = isTwoFactorEnabled,
        encryptionStatus = encryptionStatus,
        onThemeChange = onThemeChange,
        onSecuritySettings = onSecuritySettings,
        onAboutTimekeeper = onAboutTimekeeper,
        modifier = modifier
    )
}
