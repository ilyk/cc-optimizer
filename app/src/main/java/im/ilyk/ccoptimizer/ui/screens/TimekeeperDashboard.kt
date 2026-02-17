package im.ilyk.ccoptimizer.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import im.ilyk.ccoptimizer.data.models.CreditCard
import im.ilyk.ccoptimizer.data.models.PaymentRecommendation
import im.ilyk.ccoptimizer.ui.theme.NumericTypography
import im.ilyk.ccoptimizer.ui.theme.TimekeeperTheme
import kotlin.math.cos
import kotlin.math.sin

/**
 * Timekeeper Dashboard - Overview of financial health and rhythm
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimekeeperDashboard(
    recommendations: List<PaymentRecommendation>,
    cardsNeedingInput: List<CreditCard>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onCardClick: (CreditCard) -> Unit,
    onOptimizeClick: () -> Unit,
    onAddCardClick: () -> Unit,
    onInsightsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allCards = (recommendations.map { it.creditCard } + cardsNeedingInput).distinctBy { it.id }
    val cycleHealthScore = calculateCycleHealthScore(recommendations)
    val nextPaymentDays = calculateNextPaymentDays(recommendations)
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Timekeeper background with breathing animation
        TimekeeperTheme.TimekeeperBackground(
            modifier = Modifier.fillMaxSize(),
            isAnimating = true,
            showArcs = false
        )
        
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Cycle Health Card with Timekeeper pulse
            CycleHealthCard(
                score = cycleHealthScore,
                nextPaymentDays = nextPaymentDays,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        
        // Credit Cards List
        if (allCards.isNotEmpty()) {
            Text(
                text = "Your Cards",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(200.dp)
            ) {
                items(allCards) { card ->
                    TimekeeperCardPreview(
                        card = card,
                        recommendation = recommendations.find { it.creditCard.id == card.id },
                        onClick = { onCardClick(card) }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Quick Actions
        QuickActionsGrid(
            onOptimizeClick = onOptimizeClick,
            onAddCardClick = onAddCardClick,
            onInsightsClick = onInsightsClick,
            modifier = Modifier.padding(16.dp)
        )
        
            Spacer(modifier = Modifier.height(80.dp)) // Space for bottom navigation
        }
    }
}

/**
 * Cycle Health Card with Timekeeper animation
 */
@Composable
private fun CycleHealthCard(
    score: Int,
    nextPaymentDays: Int,
    modifier: Modifier = Modifier
) {
    // Timekeeper rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "timekeeper_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    TimekeeperTheme.TimekeeperCard(
        modifier = modifier,
        onClick = null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timekeeper Score Ring with pulse animation
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                // Timekeeper pulse animation behind the ring
                TimekeeperTheme.TimekeeperPulse(
                    modifier = Modifier.size(80.dp),
                    isActive = true
                )
                // Get colors outside Canvas
                val outlineVariant = MaterialTheme.colorScheme.outlineVariant
                val primaryColor = MaterialTheme.colorScheme.primary
                val secondaryColor = MaterialTheme.colorScheme.secondary
                val tertiaryColor = MaterialTheme.colorScheme.tertiary
                
                // Background ring
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val strokeWidth = 8.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                    
                    // Background circle
                    drawCircle(
                        color = outlineVariant,
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeWidth)
                    )
                    
                    // Progress arc
                    val sweepAngle = (score / 100f) * 360f
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                primaryColor,
                                secondaryColor,
                                tertiaryColor
                            )
                        ),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(
                            center.x - radius,
                            center.y - radius
                        ),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                
                // Timekeeper core animation
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(rotation)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            ),
                            shape = CircleShape
                        )
                )
                
                // Score text
                Text(
                    text = "$score%",
                    style = NumericTypography.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Health insights
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Cycle Health",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                val insightText = when {
                    score >= 90 -> "Flow optimal — all cycles perfectly aligned."
                    score >= 70 -> "Flow slightly off-beat — next cycle ready for sync."
                    score >= 50 -> "Flow disrupted — let's realign your timing."
                    else -> "Flow disrupted — let's realign your timing."
                }
                
                Text(
                    text = insightText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (nextPaymentDays > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Next payment in $nextPaymentDays days",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Timekeeper Card Preview - horizontal scrollable cards
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimekeeperCardPreview(
    card: CreditCard,
    recommendation: PaymentRecommendation?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(160.dp)
            .height(120.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Bank name and network
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = card.bankName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                
                if (recommendation != null) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                CircleShape
                            )
                    )
                }
            }
            
            // Card number
            Text(
                text = "•••• ${card.lastFourDigits}",
                style = NumericTypography.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Balance and recommendation
            Column {
                Text(
                    text = "$${String.format("%,.0f", card.currentBalance)}",
                    style = NumericTypography.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (recommendation != null) {
                    Text(
                        text = "Pay by ${recommendation.recommendedPaymentDate.dayOfMonth}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Quick Actions Grid
 */
@Composable
private fun QuickActionsGrid(
    onOptimizeClick: () -> Unit,
    onAddCardClick: () -> Unit,
    onInsightsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = "Bring your payments into sync",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                icon = Icons.Default.Tune,
                label = "Optimize",
                onClick = onOptimizeClick,
                modifier = Modifier.weight(1f)
            )
            
            QuickActionButton(
                icon = Icons.Default.Add,
                label = "Add Card",
                onClick = onAddCardClick,
                modifier = Modifier.weight(1f)
            )
            
            QuickActionButton(
                icon = Icons.Default.Analytics,
                label = "Insights",
                onClick = onInsightsClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Helper functions
private fun calculateCycleHealthScore(recommendations: List<PaymentRecommendation>): Int {
    if (recommendations.isEmpty()) return 0
    
    val today = java.time.LocalDate.now()
    val totalCards = recommendations.size
    var optimizedCards = 0
    
    recommendations.forEach { recommendation ->
        val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, recommendation.recommendedPaymentDate).toInt()
        // Consider a card optimized if payment is within 3 days of statement date
        if (daysUntil <= 3 && daysUntil >= 0) {
            optimizedCards++
        }
    }
    
    return ((optimizedCards.toFloat() / totalCards) * 100).toInt().coerceIn(0, 100)
}

private fun calculateNextPaymentDays(recommendations: List<PaymentRecommendation>): Int {
    if (recommendations.isEmpty()) return 0
    val today = java.time.LocalDate.now()
    return recommendations.minOfOrNull { 
        java.time.temporal.ChronoUnit.DAYS.between(today, it.recommendedPaymentDate).toInt()
    } ?: 0
}
