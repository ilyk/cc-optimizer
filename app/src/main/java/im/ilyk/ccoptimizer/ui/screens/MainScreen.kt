package im.ilyk.ccoptimizer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import im.ilyk.ccoptimizer.BuildConfig
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import im.ilyk.ccoptimizer.R
import im.ilyk.ccoptimizer.data.models.PaymentRecommendation
import androidx.hilt.navigation.compose.hiltViewModel
import im.ilyk.ccoptimizer.ui.components.*
import im.ilyk.ccoptimizer.ui.viewmodel.CardDetailViewModel
import im.ilyk.ccoptimizer.ui.viewmodel.ExportViewModel
import im.ilyk.ccoptimizer.ui.viewmodel.MainViewModel
import im.ilyk.ccoptimizer.ui.viewmodel.UiState
import java.time.format.DateTimeFormatter
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    cardDetailViewModel: CardDetailViewModel = hiltViewModel(),
    exportViewModel: ExportViewModel = hiltViewModel(),
    onConnectPlaid: () -> Unit,
    onNavigateToCalendar: (() -> Unit)? = null,
    onNavigateToPrivacySettings: (() -> Unit)? = null,
    onNavigateToTestCenter: (() -> Unit)? = null,
    onResetOnboarding: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var selectedCard by remember { mutableStateOf<im.ilyk.ccoptimizer.data.models.CreditCard?>(null) }
    var cardToDelete by remember { mutableStateOf<im.ilyk.ccoptimizer.data.models.CreditCard?>(null) }
    var cardForDetail by remember { mutableStateOf<im.ilyk.ccoptimizer.data.models.CreditCard?>(null) }
    
    Scaffold(
        topBar = {
            ModernTopBar(
                onConnectBank = onConnectPlaid,
                onOpenCalendar = onNavigateToCalendar,
                onOpenPrivacySettings = onNavigateToPrivacySettings,
                onOpenTestCenter = onNavigateToTestCenter,
                onResetOnboarding = onResetOnboarding,
                showConnectButton = uiState is UiState.Success,
                exportViewModel = exportViewModel
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is UiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.loading_connecting),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                }
                
                is UiState.LoadingWithCards -> {
                    LoadingWithCardsView(
                        loadingCards = state.loadingCards,
                        totalCards = state.totalCards,
                        currentStep = state.currentStep
                    )
                }
                
                is UiState.NotConnected -> {
                    NotConnectedView(onConnectPlaid)
                }
                
                is UiState.Success -> {
                    GroupedRecommendationsView(
                        recommendations = state.recommendations,
                        cardsNeedingInput = state.cardsNeedingInput,
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refreshRecommendations() },
                        onSetStatementDate = { card -> selectedCard = card },
                        onDeleteCard = { card -> cardToDelete = card },
                        onEditCard = { /* TODO: Implement edit functionality */ },
                        onInfoCard = { card -> cardForDetail = card }
                    )
                }
                
                is UiState.Error -> {
                    ErrorView(
                        message = state.message,
                        onRetry = { viewModel.loadCreditCards() }
                    )
                }
            }
        }
    }
    
    // Show statement date input dialog when a card is selected
    selectedCard?.let { card ->
        StatementDateInputDialog(
            card = card,
            onDismiss = { selectedCard = null },
            onConfirm = { statementDay ->
                viewModel.setStatementDate(card.id, statementDay)
                selectedCard = null
            }
        )
    }
    
    // Show delete confirmation dialog
    DeleteCardDialog(
        card = cardToDelete,
        onConfirmDelete = { card ->
            // TODO: Implement delete functionality in viewModel
            cardToDelete = null
        },
        onDismiss = { cardToDelete = null }
    )
    
    // Show card detail screen with payment marking
    cardForDetail?.let { originalCard ->
        // Find the updated card from current state (in case it was refreshed)
        val updatedCard = if (uiState is UiState.Success) {
            val state = uiState as UiState.Success
            // Extract cards from recommendations
            val allCards = state.recommendations.map { it.creditCard } + state.cardsNeedingInput
            allCards.find { it.id == originalCard.id } ?: originalCard
        } else {
            originalCard
        }
        
        CardDetailScreen(
            card = updatedCard,  // Use updated card with latest data
            viewModel = cardDetailViewModel,
            onDismiss = { cardForDetail = null },
            onPaymentSuccess = {
                // Refresh card data from backend after payment is marked/unmarked
                viewModel.refreshRecommendations()
            }
        )
    }
}

@Composable
fun NotConnectedView(onConnect: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.not_connected_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.not_connected_description),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onConnect,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(stringResource(R.string.button_connect_plaid), fontSize = 18.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupedRecommendationsView(
    recommendations: List<PaymentRecommendation>,
    cardsNeedingInput: List<im.ilyk.ccoptimizer.data.models.CreditCard>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onSetStatementDate: (im.ilyk.ccoptimizer.data.models.CreditCard) -> Unit,
    onDeleteCard: (im.ilyk.ccoptimizer.data.models.CreditCard) -> Unit,
    onEditCard: (im.ilyk.ccoptimizer.data.models.CreditCard) -> Unit,
    onInfoCard: (im.ilyk.ccoptimizer.data.models.CreditCard) -> Unit
) {
    // Group cards by bank
    val allCards = (recommendations.map { it.creditCard } + cardsNeedingInput).distinctBy { it.id }
    val bankGroups = allCards.groupBy { it.bankName }.map { (bankName, cards) ->
        val logo = cards.firstOrNull()?.logoUrl
        android.util.Log.d("BankGroup", "Bank: $bankName, Logo: $logo")
        BankGroup(
            bankName = bankName,
            logoUrl = logo,
            cards = cards,
            recommendations = recommendations.filter { rec -> cards.any { card -> card.id == rec.creditCard.id } }
        )
    }.sortedBy { it.bankName }

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing),
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        BankGroupedCards(
            bankGroups = bankGroups,
            onDeleteCard = onDeleteCard,
            onEditCard = onEditCard,
            onInfoCard = onInfoCard
        )
    }
}

@Composable
fun RecommendationsView(
    recommendations: List<PaymentRecommendation>,
    cardsNeedingInput: List<im.ilyk.ccoptimizer.data.models.CreditCard>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onSetStatementDate: (im.ilyk.ccoptimizer.data.models.CreditCard) -> Unit
) {
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Summary Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.main_recommendations_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.main_cards_count, recommendations.size) +
                        if (cardsNeedingInput.isNotEmpty()) " • ${cardsNeedingInput.size} need setup" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
        
        // Recommendations List with Pull-to-Refresh
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            // Cards needing statement date input
            if (cardsNeedingInput.isNotEmpty()) {
                item {
                    CardsNeedingInputSection(
                        cards = cardsNeedingInput,
                        onSetStatementDate = onSetStatementDate
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            // Payment recommendations
            items(recommendations) { recommendation ->
                PaymentRecommendationCard(recommendation)
            }
            
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.main_refresh_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun CardsNeedingInputSection(
    cards: List<im.ilyk.ccoptimizer.data.models.CreditCard>,
    onSetStatementDate: (im.ilyk.ccoptimizer.data.models.CreditCard) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF9C4) // Light yellow/warning color
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.main_setup_required_icon),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.main_setup_required_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = stringResource(R.string.main_setup_required_desc, cards.size),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            cards.forEach { card ->
                CreditCardDesign(
                    card = card,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    onSetStatementDate(card)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.main_setup_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

@Composable
fun PaymentRecommendationCard(recommendation: PaymentRecommendation) {
    // Use the beautiful CreditCardDesign component instead of plain card
    CreditCardDesign(
        card = recommendation.creditCard,
        recommendation = recommendation,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Optional: Handle card tap to show more details
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.error_something_wrong),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernTopBar(
    onConnectBank: () -> Unit,
    onOpenCalendar: (() -> Unit)?,
    onOpenPrivacySettings: (() -> Unit)?,
    onOpenTestCenter: (() -> Unit)?,
    onResetOnboarding: (() -> Unit)?,
    showConnectButton: Boolean,
    exportViewModel: ExportViewModel? = null
) {
    CenterAlignedTopAppBar(
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Credit Card Optimizer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Maximize your cashback",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        },
        actions = {
            // Debug: Reset onboarding button
            if (onResetOnboarding != null) {
                IconButton(
                    onClick = onResetOnboarding,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Reset Onboarding (Debug)",
                        tint = Color.Red
                    )
                }
            }
            
            // Test Center button (Debug only)
            if (onOpenTestCenter != null && BuildConfig.DEBUG) {
                IconButton(
                    onClick = onOpenTestCenter,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Test Center (Debug)",
                        tint = Color(0xFFFF9800) // Orange for debug
                    )
                }
            }
            
            // Privacy Settings button
            if (onOpenPrivacySettings != null) {
                IconButton(
                    onClick = onOpenPrivacySettings,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Privacy & Security",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Export button
            if (showConnectButton && exportViewModel != null) {
                ExportButton(viewModel = exportViewModel)
            }
            
            // Calendar button
            if (showConnectButton && onOpenCalendar != null) {
                IconButton(
                    onClick = onOpenCalendar,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Open Calendar",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Connect bank button
            if (showConnectButton) {
                IconButton(
                    onClick = onConnectBank,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(48.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Connect Bank",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

