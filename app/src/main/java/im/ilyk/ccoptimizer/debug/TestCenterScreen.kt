package im.ilyk.ccoptimizer.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

// Divider alias for compatibility
@Composable
private fun HorizontalDivider() = Divider()

/**
 * Test Center screen for debugging cycle inference and database
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TestCenterScreen(
    viewModel: TestCenterViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test Center") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Datasets section
            item {
                Text(
                    text = "Test Datasets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { viewModel.seedTypicalMonthly() }) {
                        Text("Monthly (15th)")
                    }
                    Button(onClick = { viewModel.seedIrregular31() }) {
                        Text("Irregular (31st)")
                    }
                    Button(onClick = { viewModel.seedNewCardLowHistory() }) {
                        Text("New Card")
                    }
                    Button(onClick = { viewModel.seedInterestMarker() }) {
                        Text("Interest Markers")
                    }
                }
            }
            
            item { HorizontalDivider() }
            
            // Sync section
            item {
                Text(
                    text = "Sync (Story 2.3)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.syncNow(force = false) },
                        enabled = !ui.isSyncing && ui.canSyncNow,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Text(if (ui.isSyncing) "Syncing..." else "Sync Now")
                    }
                    Button(
                        onClick = { viewModel.syncNow(force = true) },
                        enabled = !ui.isSyncing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Force Sync")
                    }
                }
            }
            
            item {
                val lastSync = ui.lastSyncTime
                if (lastSync != null) {
                    Text(
                        text = "Last sync: ${formatTimestamp(lastSync)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            item { HorizontalDivider() }
            
            // Actions section
            item {
                Text(
                    text = "Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.recompute() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Recompute Cycles")
                    }
                    OutlinedButton(onClick = { viewModel.pruneCycles() }) {
                        Text("Prune Cycles")
                    }
                    OutlinedButton(onClick = { viewModel.showDbPath() }) {
                        Text("Show DB Path")
                    }
                    OutlinedButton(onClick = { viewModel.clearAllData() }) {
                        Text("Clear Data")
                    }
                }
            }
            
            item { HorizontalDivider() }
            
            // Latest cycles section
            item {
                Text(
                    text = "Latest Cycle per Card",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (ui.cards.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "No cards yet. Seed some test data to get started.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(ui.cards) { card ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = card.displayName ?: card.cardId,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val cycle = ui.latestByCard[card.cardId]
                            if (cycle == null) {
                                Text(
                                    text = "No cycle computed yet",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "Close Day: ${cycle.closeDay ?: "N/A"}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Cadence: ${cycle.cadence}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Confidence: ${"%.2f".format(cycle.confidence * 100)}%",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Next Close: ${cycle.nextCloseDate ?: "N/A"}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
            
            item { HorizontalDivider() }
            
            // Explainability section
            item {
                Text(
                    text = "Explainability",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                val explain = ui.explain
                if (explain == null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "Run 'Recompute Cycles' to see signal analysis",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Top Signals:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            explain.topSignals.forEach { (signal, weight) ->
                                Text(
                                    text = "  • $signal: ${"%.2f".format(weight)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = "Candidate Days:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            val sortedDays = explain.candidateDays.entries
                                .sortedByDescending { it.value }
                                .take(5)
                            
                            sortedDays.forEach { (day, weight) ->
                                Text(
                                    text = "  • Day $day: ${"%.2f".format(weight)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
            
            item { HorizontalDivider() }
            
            // Status section
            item {
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Text(
                        text = ui.status ?: "Idle",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

/**
 * Format timestamp for display
 */
private fun formatTimestamp(epochMs: Long): String {
    val duration = System.currentTimeMillis() - epochMs
    val minutes = duration / 60_000
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hour${if (hours > 1) "s" else ""} ago"
        else -> "$days day${if (days > 1) "s" else ""} ago"
    }
}
