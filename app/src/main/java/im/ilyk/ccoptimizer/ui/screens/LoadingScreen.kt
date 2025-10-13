package im.ilyk.ccoptimizer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import im.ilyk.ccoptimizer.data.models.CreditCard
import im.ilyk.ccoptimizer.ui.components.AnalyzingCard
import im.ilyk.ccoptimizer.ui.components.LoadingCardSkeleton

@Composable
fun LoadingWithCardsView(
    loadingCards: List<CreditCard>,
    totalCards: Int,
    currentStep: String
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Progress Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Analyzing Your Cards",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentStep,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                if (totalCards > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Progress bar
                    LinearProgressIndicator(
                        progress = loadingCards.size.toFloat() / totalCards.toFloat(),
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "${loadingCards.size} of $totalCards cards analyzed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }
        }
        
        // Loading cards list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (loadingCards.isEmpty()) {
                // Show skeleton loaders while fetching initial data
                items(3) {
                    LoadingCardSkeleton()
                }
            } else {
                // Show analyzing cards with animation
                items(loadingCards) { card ->
                    AnalyzingCard(
                        cardName = card.name,
                        cardMask = card.lastFourDigits
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "💡 Using AI to detect your statement closing dates\nThis may take a few moments...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

