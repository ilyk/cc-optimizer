package im.ilyk.ccoptimizer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import im.ilyk.ccoptimizer.data.models.CreditCard
import im.ilyk.ccoptimizer.data.models.PaymentRecommendation
import im.ilyk.ccoptimizer.ui.components.CalendarView
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    cards: List<CreditCard>,
    recommendations: List<PaymentRecommendation>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showFilterDialog by remember { mutableStateOf(false) }
    var filterNeedsAttention by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Payment Calendar",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Track your payment windows",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, "Filter")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        val filteredCards = if (filterNeedsAttention) {
            // Filter cards that need attention soon (within 7 days)
            val today = LocalDate.now()
            cards.filter { card ->
                card.statementCloseDate?.let { close ->
                    val daysUntilClose = java.time.temporal.ChronoUnit.DAYS.between(today, close)
                    daysUntilClose in 0..7
                } ?: false ||
                card.statementDueDate?.let { due ->
                    val daysUntilDue = java.time.temporal.ChronoUnit.DAYS.between(today, due)
                    daysUntilDue in 0..7
                } ?: false
            }
        } else {
            cards
        }
        
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (filterNeedsAttention && filteredCards.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        text = "No cards need attention in the next 7 days",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                CalendarView(
                    cards = filteredCards,
                    recommendations = recommendations,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
    
    // Filter dialog
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filter Options") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text("Show only cards needing attention")
                        Switch(
                            checked = filterNeedsAttention,
                            onCheckedChange = { filterNeedsAttention = it }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilterDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}

