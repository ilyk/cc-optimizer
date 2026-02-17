package im.ilyk.ccoptimizer.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import im.ilyk.ccoptimizer.R
import im.ilyk.ccoptimizer.data.models.CreditCard
import im.ilyk.ccoptimizer.data.models.PaymentRecommendation
import im.ilyk.ccoptimizer.ui.components.TimekeeperCardDesign
import im.ilyk.ccoptimizer.ui.viewmodel.MainViewModel
import im.ilyk.ccoptimizer.ui.viewmodel.UiState
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onConnectPlaid: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isRefreshing)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.home_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { /* menu placeholder */ }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(R.string.home_menu_content_desc)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { viewModel.refreshRecommendations() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is UiState.Loading -> {
                    HomeLoadingState()
                }

                is UiState.NotConnected -> {
                    HomeNotConnectedState(onConnectPlaid = onConnectPlaid)
                }

                is UiState.LoadingWithCards -> {
                    HomeLoadingWithCardsState(
                        currentStep = state.currentStep,
                        loaded = state.loadingCards.size,
                        total = state.totalCards
                    )
                }

                is UiState.Success -> {
                    HomeSuccessContent(
                        recommendations = state.recommendations,
                        cardsNeedingInput = state.cardsNeedingInput
                    )
                }

                is UiState.Error -> {
                    HomeErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadCreditCards(isRefresh = false) }
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Loading state
// ---------------------------------------------------------------------------

@Composable
private fun HomeLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.home_loading_cards),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Loading with cards state
// ---------------------------------------------------------------------------

@Composable
private fun HomeLoadingWithCardsState(
    currentStep: String,
    loaded: Int,
    total: Int
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = currentStep,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (total > 0) {
                Text(
                    text = stringResource(R.string.home_loading_cards_progress, loaded, total),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = if (total > 0) loaded.toFloat() / total else 0f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Not connected state
// ---------------------------------------------------------------------------

@Composable
private fun HomeNotConnectedState(onConnectPlaid: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CreditCard,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.home_no_cards_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.home_no_cards_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onConnectPlaid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.home_connect_cards_button))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Error state
// ---------------------------------------------------------------------------

@Composable
private fun HomeErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = stringResource(R.string.home_something_went_wrong),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            OutlinedButton(
                onClick = onRetry
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.button_retry))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Success state — full Monobank-style content
// ---------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeSuccessContent(
    recommendations: List<PaymentRecommendation>,
    cardsNeedingInput: List<CreditCard>
) {
    if (recommendations.isEmpty() && cardsNeedingInput.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.home_no_recommendations),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { recommendations.size.coerceAtLeast(1) })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Banner: cards needing statement date input
        if (cardsNeedingInput.isNotEmpty()) {
            CardsNeedingInputBanner(count = cardsNeedingInput.size)
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (recommendations.isEmpty()) {
            // No recommendations but has cards needing input
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.home_setup_statement_dates),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            return@Column
        }

        // Card pager
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 24.dp),
            pageSpacing = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(216.dp)
        ) { page ->
            val rec = recommendations[page]
            val pageOffset = pagerState.currentPageOffsetFraction

            TimekeeperCardDesign(
                card = rec.creditCard,
                recommendation = rec,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        rotationY = pageOffset * 8f
                    },
                isSelected = pagerState.currentPage == page
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Page indicator dots
        PageIndicatorDots(
            pageCount = recommendations.size,
            currentPage = pagerState.currentPage,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Pay-by section for selected card
        val activeRec = recommendations.getOrNull(pagerState.currentPage)
        if (activeRec != null) {
            PayBySection(
                recommendation = activeRec,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            UtilizationImpactSection(
                card = activeRec.creditCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ---------------------------------------------------------------------------
// Cards-needing-input banner
// ---------------------------------------------------------------------------

@Composable
private fun CardsNeedingInputBanner(count: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = stringResource(
                    R.string.home_cards_need_setup,
                    count,
                    if (count > 1) "s" else "",
                    if (count == 1) "s" else ""
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Page indicator dots
// ---------------------------------------------------------------------------

@Composable
private fun PageIndicatorDots(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isCurrent = index == currentPage
            val dotAlpha by animateFloatAsState(
                targetValue = if (isCurrent) 1f else 0.3f,
                animationSpec = tween(200),
                label = "dot_alpha_$index"
            )
            val dotSize by animateFloatAsState(
                targetValue = if (isCurrent) 8f else 6f,
                animationSpec = tween(200),
                label = "dot_size_$index"
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(dotSize.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = dotAlpha),
                        shape = CircleShape
                    )
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Pay-by section
// ---------------------------------------------------------------------------

@Composable
private fun PayBySection(
    recommendation: PaymentRecommendation,
    modifier: Modifier = Modifier
) {
    val urgencyColor = when {
        recommendation.daysUntilPayment <= 0 -> MaterialTheme.colorScheme.error
        recommendation.daysUntilPayment <= 3 -> Color(0xFFFF9800)
        recommendation.daysUntilPayment <= 7 -> Color(0xFFFFC107)
        else -> Color(0xFF4CAF50)
    }

    val dateLabel = buildString {
        val formatter = DateTimeFormatter.ofPattern("MMM dd")
        append(recommendation.recommendedPaymentDate.format(formatter))
        when {
            recommendation.daysUntilPayment <= 0 -> append(" (overdue)")
            recommendation.daysUntilPayment == 1 -> append(" (tomorrow)")
            else -> append(" (in ${recommendation.daysUntilPayment} days)")
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val snackbarMessage = stringResource(R.string.home_marked_as_paid_snackbar)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section header
            Text(
                text = stringResource(R.string.home_pay_by_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.SemiBold
            )

            // Payment date with urgency color
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(color = urgencyColor, shape = CircleShape)
                )
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = urgencyColor
                )
            }

            // Statement close date
            recommendation.creditCard.statementCloseDate?.let { closeDate ->
                val closeDateFormatted = closeDate.format(DateTimeFormatter.ofPattern("MMM dd"))
                Text(
                    text = stringResource(R.string.home_statement_closes, closeDateFormatted),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Recommended amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.home_recommended_payment),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$${String.format("%,.2f", recommendation.recommendedAmount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Reasoning text
            if (recommendation.reasoning.isNotBlank()) {
                Text(
                    text = recommendation.reasoning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Mark as paid button
            Button(
                onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = snackbarMessage,
                            duration = SnackbarDuration.Short
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = urgencyColor.copy(alpha = if (recommendation.daysUntilPayment <= 3) 1f else 0.85f)
                )
            ) {
                Text(
                    text = stringResource(R.string.home_mark_as_paid),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Utilization impact section
// ---------------------------------------------------------------------------

@Composable
private fun UtilizationImpactSection(
    card: CreditCard,
    modifier: Modifier = Modifier
) {
    if (card.creditLimit <= 0.0) return

    val utilizationBefore = (card.currentBalance / card.creditLimit).coerceIn(0.0, 1.0)
    val utilizationAfter = 0.0

    val beforeColor = when {
        utilizationBefore > 0.30 -> MaterialTheme.colorScheme.error  // red
        utilizationBefore > 0.10 -> Color(0xFFFFC107)  // yellow/amber (no Material token for warning)
        else -> Color(0xFF4CAF50)                       // green (semantic success)
    }

    val animatedBefore by animateFloatAsState(
        targetValue = utilizationBefore.toFloat(),
        animationSpec = tween(800),
        label = "utilization_before"
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.home_utilization_impact),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.SemiBold
            )

            // Before
            UtilizationRow(
                label = stringResource(R.string.home_utilization_before),
                percentage = utilizationBefore.toFloat(),
                animatedProgress = animatedBefore,
                barColor = beforeColor
            )

            // After
            UtilizationRow(
                label = stringResource(R.string.home_utilization_after),
                percentage = utilizationAfter.toFloat(),
                animatedProgress = 0f,
                barColor = Color(0xFF4CAF50)
            )

            // Summary line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                val beforePct = (utilizationBefore * 100).toInt()
                val afterPct = (utilizationAfter * 100).toInt()
                Text(
                    text = "${beforePct}% → ${afterPct}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun UtilizationRow(
    label: String,
    percentage: Float,
    animatedProgress: Float,
    barColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${(percentage * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = barColor
            )
        }
        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    }
}
