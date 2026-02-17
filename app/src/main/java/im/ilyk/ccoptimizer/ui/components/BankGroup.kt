package im.ilyk.ccoptimizer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import im.ilyk.ccoptimizer.data.models.CreditCard
import im.ilyk.ccoptimizer.data.models.PaymentRecommendation

data class BankGroup(
    val bankName: String,
    val logoUrl: String?,
    val cards: List<CreditCard>,
    val recommendations: List<PaymentRecommendation>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankGroupedCards(
    bankGroups: List<BankGroup>,
    onDeleteCard: (CreditCard) -> Unit,
    onEditCard: (CreditCard) -> Unit,
    onInfoCard: (CreditCard) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(bankGroups) { bankGroup ->
            BankGroupSection(
                bankGroup = bankGroup,
                onDeleteCard = onDeleteCard,
                onEditCard = onEditCard,
                onInfoCard = onInfoCard
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BankGroupSection(
    bankGroup: BankGroup,
    onDeleteCard: (CreditCard) -> Unit,
    onEditCard: (CreditCard) -> Unit,
    onInfoCard: (CreditCard) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    
    Column {
        // Bank header (clickable to expand/collapse)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Expand/collapse icon
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 8.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Bank logo (fixed width to ensure consistent alignment)
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(40.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (bankGroup.logoUrl != null) {
                    AsyncImage(
                        model = bankGroup.logoUrl,
                        contentDescription = "${bankGroup.bankName} logo",
                        modifier = Modifier
                            .height(40.dp)
                            .widthIn(max = 100.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Bank name
            Text(
                text = bankGroup.bankName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Card count badge
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "${bankGroup.cards.size} card${if (bankGroup.cards.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        
        // Cards for this bank (collapsible)
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                bankGroup.cards.forEach { card ->
                    val recommendation = bankGroup.recommendations.find { it.creditCard.id == card.id }
                    
                    SwipeableCard(
                        card = card,
                        recommendation = recommendation,
                        onDelete = onDeleteCard,
                        onEdit = onEditCard,
                        onInfo = onInfoCard,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
