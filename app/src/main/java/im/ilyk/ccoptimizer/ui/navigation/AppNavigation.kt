package im.ilyk.ccoptimizer.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import im.ilyk.ccoptimizer.data.models.CreditCard
import im.ilyk.ccoptimizer.data.models.PaymentRecommendation
import im.ilyk.ccoptimizer.debug.TestCenterScreen
import im.ilyk.ccoptimizer.ui.screens.CalendarScreen
import im.ilyk.ccoptimizer.ui.screens.HomeScreen
import im.ilyk.ccoptimizer.ui.screens.OnboardingScreen
import im.ilyk.ccoptimizer.ui.screens.PrivacySettingsScreen
import im.ilyk.ccoptimizer.ui.screens.SettingsScreen
import im.ilyk.ccoptimizer.ui.screens.TimekeeperChatScreen
import im.ilyk.ccoptimizer.ui.theme.ThemePreferences
import im.ilyk.ccoptimizer.ui.viewmodel.MainViewModel
import im.ilyk.ccoptimizer.ui.viewmodel.UiState
import kotlinx.coroutines.launch

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
 * Bottom navigation tab definitions
 */
private enum class BottomTab(val label: String) {
    HOME("Home"),
    CALENDAR("Calendar"),
    TIMEKEEPER("Timekeeper"),
    SETTINGS("Settings")
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
    var currentScreen by remember {
        mutableStateOf<Screen>(
            if (hasCompletedOnboarding) Screen.Main else Screen.Onboarding
        )
    }

    // Navigation functions
    val navigateToMain = { currentScreen = Screen.Main }
    val navigateToPrivacySettings = { currentScreen = Screen.PrivacySettings }
    val navigateToTestCenter = { currentScreen = Screen.TestCenter }
    val navigateBack = { currentScreen = Screen.Main }

    when (currentScreen) {
        Screen.Onboarding -> {
            OnboardingScreen(
                onComplete = {
                    onOnboardingComplete()
                    navigateToMain()
                }
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

        Screen.Main, Screen.Calendar -> {
            BottomNavScaffold(
                viewModel = viewModel,
                onConnectPlaid = onConnectPlaid,
                initialTab = if (currentScreen == Screen.Calendar) BottomTab.CALENDAR else BottomTab.HOME
            )
        }
    }
}

@Composable
private fun BottomNavScaffold(
    viewModel: MainViewModel,
    onConnectPlaid: () -> Unit,
    initialTab: BottomTab = BottomTab.HOME
) {
    var selectedTab by remember { mutableStateOf(initialTab) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == BottomTab.HOME,
                    onClick = { selectedTab = BottomTab.HOME },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text(BottomTab.HOME.label) }
                )
                NavigationBarItem(
                    selected = selectedTab == BottomTab.CALENDAR,
                    onClick = { selectedTab = BottomTab.CALENDAR },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar") },
                    label = { Text(BottomTab.CALENDAR.label) }
                )
                NavigationBarItem(
                    selected = selectedTab == BottomTab.TIMEKEEPER,
                    onClick = { selectedTab = BottomTab.TIMEKEEPER },
                    icon = { Icon(Icons.Default.SmartToy, contentDescription = "Timekeeper") },
                    label = { Text(BottomTab.TIMEKEEPER.label) }
                )
                NavigationBarItem(
                    selected = selectedTab == BottomTab.SETTINGS,
                    onClick = { selectedTab = BottomTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text(BottomTab.SETTINGS.label) }
                )
            }
        }
    ) { paddingValues ->
        when (selectedTab) {
            BottomTab.HOME -> {
                HomeScreen(
                    viewModel = viewModel,
                    onConnectPlaid = onConnectPlaid,
                    modifier = Modifier.padding(paddingValues)
                )
            }

            BottomTab.CALENDAR -> {
                val uiState by viewModel.uiState.collectAsState()

                val (cards, recommendations) = when (val state = uiState) {
                    is UiState.Success -> {
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
                    onBack = { selectedTab = BottomTab.HOME },
                    modifier = Modifier.padding(paddingValues)
                )
            }

            BottomTab.TIMEKEEPER -> {
                TimekeeperChatScreen(
                    modifier = Modifier.padding(paddingValues)
                )
            }

            BottomTab.SETTINGS -> {
                SettingsTabWrapper(
                    paddingValues = paddingValues
                )
            }
        }
    }
}

@Composable
private fun SettingsTabWrapper(
    paddingValues: PaddingValues
) {
    // ThemePreferences is a singleton; we need a Context to get the instance.
    // Since we're in a Composable, use LocalContext.
    val context = LocalContext.current
    val themePreferences = remember(context) { ThemePreferences.getInstance(context) }
    val currentTheme by themePreferences.themeMode.collectAsState(initial = ThemePreferences.THEME_AUTO)
    val coroutineScope = rememberCoroutineScope()

    SettingsScreen(
        currentTheme = currentTheme,
        onThemeChange = { newTheme ->
            coroutineScope.launch {
                themePreferences.setThemeMode(newTheme)
            }
        },
        onNavigateBack = { /* no-op: settings is a tab, back closes the app */ },
        modifier = Modifier.padding(paddingValues)
    )
}
