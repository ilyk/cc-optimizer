package im.ilyk.ccoptimizer.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import im.ilyk.ccoptimizer.data.models.CreditCard
import im.ilyk.ccoptimizer.data.models.PaymentRecommendation
import im.ilyk.ccoptimizer.ui.components.TimekeeperAnimations
import im.ilyk.ccoptimizer.ui.theme.TimekeeperTheme
import im.ilyk.ccoptimizer.ui.utils.calculateCycleHealthScore

/**
 * Enhanced Dashboard with Timekeeper pulse, animations, and brand microcopy
 */
@Composable
fun TimekeeperDashboardEnhanced(
    creditCards: List<CreditCard>,
    recommendations: List<PaymentRecommendation>,
    onCardClick: (CreditCard) -> Unit,
    onOptimizeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isAnimating by remember { mutableStateOf(false) }
    var showRipple by remember { mutableStateOf(false) }
    
    // Calculate cycle health with enhanced scoring
    val cycleHealthScore = calculateCycleHealthScore(recommendations).toFloat() / 100f
    val healthStatus = when {
        cycleHealthScore >= 0.8f -> "Flow perfectly synced"
        cycleHealthScore >= 0.6f -> "Flow slightly off-beat — next cycle ready for sync"
        cycleHealthScore >= 0.4f -> "Rhythm needs alignment"
        else -> "Flow disrupted — let's realign your timing"
    }
    
    // Trigger animations when score changes
    LaunchedEffect(cycleHealthScore) {
        isAnimating = true
        showRipple = true
        kotlinx.coroutines.delay(800)
        isAnimating = false
        kotlinx.coroutines.delay(400)
        showRipple = false
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Enhanced header with Timekeeper background
        TimekeeperTheme.TimekeeperBackground(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            isAnimating = isAnimating
        )
        
        // Cycle Health Card with pulse animation
        TimekeeperCycleHealthCardEnhanced(
            score = cycleHealthScore,
            status = healthStatus,
            isAnimating = isAnimating,
            showRipple = showRipple,
            modifier = Modifier.padding(16.dp)
        )
        
        // Enhanced credit cards list
        TimekeeperCreditCardsListEnhanced(
            cards = creditCards,
            onCardClick = onCardClick,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        // Quick actions with enhanced styling
        TimekeeperQuickActions(
            onOptimizeClick = onOptimizeClick,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/**
 * Enhanced Cycle Health Card with Timekeeper pulse and animations
 */
@Composable
private fun TimekeeperCycleHealthCardEnhanced(
    score: Float,
    status: String,
    isAnimating: Boolean,
    showRipple: Boolean,
    modifier: Modifier = Modifier
) {
    TimekeeperAnimations.SuccessAnimation(isSuccess = showRipple) {
        TimekeeperTheme.TimekeeperCard(
            modifier = modifier,
            content = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Timekeeper pulse indicator
                    TimekeeperTheme.TimekeeperPulse(
                        isActive = true,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Cycle Health",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Animated progress ring
                    TimekeeperProgressRingEnhanced(
                        progress = score,
                        isAnimating = isAnimating,
                        modifier = Modifier.size(60.dp)
                    )
                }
            }
        )
    }
}

/**
 * Enhanced progress ring with smooth animations
 */
@Composable
private fun TimekeeperProgressRingEnhanced(
    progress: Float,
    isAnimating: Boolean,
    modifier: Modifier = Modifier
) {
    TimekeeperAnimations.ProgressRingAnimation(
        progress = progress,
        isAnimating = isAnimating
    ) {
        val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        val onSurfaceColor = MaterialTheme.colorScheme.onSurface
        
        androidx.compose.foundation.Canvas(modifier = modifier) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = minOf(size.width, size.height) / 2 - 8.dp.toPx()
            val strokeWidth = 6.dp.toPx()
            
            // Background ring
            drawCircle(
                color = outlineColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )
            
            // Progress arc
            val sweepAngle = progress * 360f
            drawArc(
                color = when {
                    progress >= 0.8f -> Color(0xFF4CAF50)
                    progress >= 0.6f -> Color(0xFF2196F3)
                    progress >= 0.4f -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                },
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(
                    center.x - radius,
                    center.y - radius
                ),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            
            // Center text
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = onSurfaceColor.toArgb()
                    textSize = 12.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                drawText(
                    "${(progress * 100).toInt()}%",
                    center.x,
                    center.y + paint.textSize / 3,
                    paint
                )
            }
        }
    }
}

/**
 * Enhanced credit cards list with floating cards
 */
@Composable
private fun TimekeeperCreditCardsListEnhanced(
    cards: List<CreditCard>,
    onCardClick: (CreditCard) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Your Cards",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(cards) { card ->
                TimekeeperCreditCardEnhanced(
                    card = card,
                    onClick = { onCardClick(card) }
                )
            }
        }
    }
}

/**
 * Enhanced credit card with Timekeeper styling
 */
@Composable
private fun TimekeeperCreditCardEnhanced(
    card: CreditCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TimekeeperAnimations.FloatingCard(
        modifier = modifier.width(280.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Card icon with gradient background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        TimekeeperTheme.TimekeeperGradient,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = card.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "•••• ${card.lastFourDigits}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "$${String.format("%.2f", card.currentBalance)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Enhanced quick actions with gradient buttons
 */
@Composable
private fun TimekeeperQuickActions(
    onOptimizeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TimekeeperAnimations.InteractiveButton(
                text = "Bring your payments into sync",
                onClick = onOptimizeClick,
                modifier = Modifier.weight(1f)
            )
            
            TimekeeperAnimations.InteractiveButton(
                text = "Insights that anticipate your rhythm",
                onClick = { /* Navigate to insights */ },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
