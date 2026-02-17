package im.ilyk.ccoptimizer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import im.ilyk.ccoptimizer.data.models.*
import im.ilyk.ccoptimizer.ui.viewmodel.OptimizationViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Demo screen showing the optimization system in action
 * This demonstrates the functional integration with real data
 */
@Composable
fun OptimizationDemoScreen(
    viewModel: OptimizationViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    // Collect state from ViewModel
    val cards by viewModel.cards.collectAsState()
    val paymentPlan by viewModel.paymentPlan.collectAsState()
    val scenarioComparison by viewModel.scenarioComparison.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Optimization Demo",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Error display
        val currentError = error
        if (currentError != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Error: $currentError",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Loading state
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Cards section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Credit Cards (${cards.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                cards.forEach { card: CreditCard ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = card.bankName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Balance: \$${String.format("%.2f", card.currentBalance)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Card: ${card.name}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            val dueDate = card.statementDueDate
                            if (dueDate != null) {
                                Text(
                                    text = "Due: ${dueDate.format(DateTimeFormatter.ofPattern("MMM dd"))}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }

        // Payment Plan section
        val currentPlan = paymentPlan
        if (currentPlan != null) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Optimized Payment Plan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Total Interest: \$${String.format("%.2f", currentPlan.totalInterest)}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "Total Savings vs Baseline: \$${String.format("%.2f", currentPlan.totalSavings)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Scheduled Payments:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    currentPlan.scheduledPayments.take(5).forEach { payment: ScheduledPayment ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Card ${payment.cardId.takeLast(4)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = payment.dueDate.format(DateTimeFormatter.ofPattern("MMM dd")),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Column(
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "\$${String.format("%.2f", payment.amount)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (payment.isCompleted) "Completed" else "Pending",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }

                    if (currentPlan.scheduledPayments.size > 5) {
                        Text(
                            text = "... and ${currentPlan.scheduledPayments.size - 5} more payments",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Scenario Comparison section
        val currentComparison = scenarioComparison
        if (currentComparison != null) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Scenario Comparison",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Current",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "\$${String.format("%.2f", currentComparison.currentPlan.totalInterest)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Optimized",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "\$${String.format("%.2f", currentComparison.optimizedPlan.totalInterest)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.refresh() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Refresh Data")
            }

            Button(
                onClick = { viewModel.recalculatePlan() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Recalculate")
            }

            Button(
                onClick = { viewModel.compareScenarios() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Compare")
            }
        }
    }
}
