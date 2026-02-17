package im.ilyk.ccoptimizer.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import im.ilyk.ccoptimizer.data.models.CreditCard
import im.ilyk.ccoptimizer.data.models.Insight
import im.ilyk.ccoptimizer.data.models.PaymentRecommendation
import im.ilyk.ccoptimizer.ui.components.TimekeeperAnimations
import im.ilyk.ccoptimizer.ui.navigation.TimekeeperBottomNavigation
import im.ilyk.ccoptimizer.ui.theme.TimekeeperTheme

/**
 * Polished Timekeeper App with all Phase 2 enhancements
 * Implements the complete UI/UX polish from ui_polish_phase2.md
 */
@Composable
fun TimekeeperAppPolished(
    creditCards: List<CreditCard> = emptyList(),
    recommendations: List<PaymentRecommendation> = emptyList(),
    insights: List<Insight> = emptyList(),
    onCardClick: (CreditCard) -> Unit = {},
    onOptimizeClick: () -> Unit = {},
    onInsightClick: (Insight) -> Unit = {},
    onThemeToggle: () -> Unit = {},
    onSecurityClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentRoute by remember { mutableStateOf("dashboard") }
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Enhanced background with Timekeeper theme
    TimekeeperTheme.TimekeeperBackground(
        modifier = Modifier.fillMaxSize(),
        isAnimating = isRefreshing
    )
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Enhanced top app bar
        TimekeeperAppBar(
            currentRoute = currentRoute,
            isRefreshing = isRefreshing,
            onRefresh = { isRefreshing = true }
        )
        
        // Main content with tab change animations
        TimekeeperAnimations.TabChangeAnimation(
            isVisible = true
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentRoute) {
                    "dashboard" -> {
                        TimekeeperDashboardEnhanced(
                            creditCards = creditCards,
                            recommendations = recommendations,
                            onCardClick = onCardClick,
                            onOptimizeClick = onOptimizeClick
                        )
                    }
                    "optimize" -> {
                        TimekeeperOptimizeEnhanced(
                            onRecalculateClick = onOptimizeClick,
                            onCompareClick = { /* Handle compare */ },
                            onAutoClick = { /* Handle auto */ }
                        )
                    }
                    "insights" -> {
                        TimekeeperInsightsEnhanced(
                            insights = insights,
                            onInsightClick = onInsightClick
                        )
                    }
                    "profile" -> {
                        TimekeeperProfileEnhanced(
                            onThemeToggle = onThemeToggle,
                            onSecurityClick = onSecurityClick,
                            onNotificationsClick = onNotificationsClick
                        )
                    }
                }
            }
        }
        
        // Enhanced bottom navigation
        TimekeeperBottomNavigation(
            currentRoute = currentRoute,
            onNavigate = { route -> currentRoute = route }
        )
    }
}

/**
 * Enhanced app bar with Timekeeper styling
 */
@Composable
private fun TimekeeperAppBar(
    currentRoute: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = when (currentRoute) {
        "dashboard" -> "Your Financial Rhythm"
        "optimize" -> "Bring your payments into sync"
        "insights" -> "Insights that anticipate your rhythm"
        "profile" -> "Your Profile"
        else -> "Timekeeper"
    }
    
        @OptIn(ExperimentalMaterial3Api::class)
        TopAppBar(
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
        actions = {
            if (currentRoute in listOf("dashboard", "insights")) {
                IconButton(
                    onClick = onRefresh,
                    enabled = !isRefreshing
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = if (isRefreshing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
    )
}

/**
 * Enhanced navigation with micro-interactions
 */
@Composable
fun TimekeeperBottomNavigationEnhanced(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val navigationItems = listOf(
        Triple("dashboard", Icons.Default.Home, "Dashboard"),
        Triple("optimize", Icons.Default.Tune, "Optimize"),
        Triple("insights", Icons.Default.Analytics, "Insights"),
        Triple("profile", Icons.Default.Person, "Profile")
    )
    
    @OptIn(ExperimentalMaterial3Api::class)
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    ) {
        navigationItems.forEach { (route, icon, label) ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label
                    )
                },
                label = { Text(label) },
                selected = currentRoute == route,
                onClick = { onNavigate(route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

/**
 * Enhanced loading state with Timekeeper animations
 */
@Composable
fun TimekeeperLoadingState(
    message: String = "Syncing your rhythm...",
    modifier: Modifier = Modifier
) {
    TimekeeperAnimations.DataRefreshAnimation(isRefreshing = true) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Timekeeper pulse animation
            TimekeeperTheme.TimekeeperPulse(
                isActive = true,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Enhanced error state with Timekeeper styling
 */
@Composable
fun TimekeeperErrorState(
    message: String = "Flow disrupted — let's realign your timing",
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    TimekeeperAnimations.ErrorAnimation(hasError = true) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            TimekeeperTheme.TimekeeperButton(
                text = "Retry",
                onClick = onRetry
            )
        }
    }
}

/**
 * Enhanced success state with ripple animation
 */
@Composable
fun TimekeeperSuccessState(
    message: String = "Flow perfectly synced",
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    TimekeeperAnimations.SuccessAnimation(isSuccess = true) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Success icon with glow
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(64.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            TimekeeperTheme.TimekeeperButton(
                text = "Continue",
                onClick = onContinue
            )
        }
    }
}
