package im.ilyk.ccoptimizer.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import im.ilyk.ccoptimizer.data.models.CreditCard
import im.ilyk.ccoptimizer.data.models.PaymentRecommendation
import im.ilyk.ccoptimizer.debug.TestCenterScreen
import im.ilyk.ccoptimizer.ui.screens.CalendarScreen
import im.ilyk.ccoptimizer.ui.screens.MainScreen
import im.ilyk.ccoptimizer.ui.screens.OnboardingScreen
import im.ilyk.ccoptimizer.ui.screens.PrivacySettingsScreen
import im.ilyk.ccoptimizer.ui.screens.TimekeeperIntegrationExample
import im.ilyk.ccoptimizer.ui.viewmodel.MainViewModel

/**
 * Navigation destinations for the app
 */
sealed class Screen {
    object Onboarding : Screen()
    object Main : Screen()
    object Calendar : Screen()
    object PrivacySettings : Screen()
    object TestCenter : Screen()
}

/**
 * Main navigation container that manages screen transitions
 */
@Composable
fun AppNavigation(
    viewModel: MainViewModel,
    onConnectPlaid: () -> Unit,
    hasCompletedOnboarding: Boolean,
    onOnboardingComplete: () -> Unit,
    onResetOnboarding: (() -> Unit)? = null
) {
    // Track current screen (using remember instead of rememberSaveable because Screen is a sealed class)
    var currentScreen by remember { 
        mutableStateOf<Screen>(
            if (hasCompletedOnboarding) Screen.Main else Screen.Onboarding
        )
    }
    
    // Navigation functions
    val navigateToMain = { currentScreen = Screen.Main }
    val navigateToCalendar = { currentScreen = Screen.Calendar }
    val navigateToPrivacySettings = { currentScreen = Screen.PrivacySettings }
    val navigateToTestCenter = { currentScreen = Screen.TestCenter }
    val navigateBack = { currentScreen = Screen.Main }
    
    // Render current screen
    when (currentScreen) {
        Screen.Onboarding -> {
            OnboardingScreen(
                onComplete = {
                    onOnboardingComplete()
                    navigateToMain()
                }
            )
        }
        
        Screen.Main -> {
            // Phase 1: Core Integration - Replace MainScreen with Timekeeper system
            TimekeeperIntegrationExample(
                viewModel = viewModel,
                onConnectPlaid = onConnectPlaid
            )
        }
        
        Screen.Calendar -> {
            val uiState by viewModel.uiState.collectAsState()
            
            // Extract cards and recommendations from UI state
            val (cards, recommendations) = when (val state = uiState) {
                is im.ilyk.ccoptimizer.ui.viewmodel.UiState.Success -> {
                    Pair(
                        state.recommendations.map { it.creditCard } + state.cardsNeedingInput,
                        state.recommendations
                    )
                }
                else -> Pair(emptyList<CreditCard>(), emptyList<PaymentRecommendation>())
            }
            
            CalendarScreen(
                cards = cards,
                recommendations = recommendations,
                onBack = navigateBack
            )
        }
        
        Screen.PrivacySettings -> {
            PrivacySettingsScreen(
                onNavigateBack = navigateBack
            )
        }
        
        Screen.TestCenter -> {
            TestCenterScreen()
        }
    }
}

