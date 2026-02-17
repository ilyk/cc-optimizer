package im.ilyk.ccoptimizer.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import im.ilyk.ccoptimizer.data.models.Insight
import im.ilyk.ccoptimizer.ui.components.TimekeeperAnimations
import im.ilyk.ccoptimizer.ui.theme.TimekeeperTheme

/**
 * Enhanced Insights screen with floating cards and animated indicators
 */
@Composable
fun TimekeeperInsightsEnhanced(
    insights: List<Insight>,
    onInsightClick: (Insight) -> Unit,
    modifier: Modifier = Modifier
) {
    var isRefreshing by remember { mutableStateOf(false) }
    
    TimekeeperAnimations.DataRefreshAnimation(isRefreshing = isRefreshing) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Enhanced header with mini Timekeeper ring
            TimekeeperInsightsHeader(
                isRefreshing = isRefreshing,
                onRefresh = { isRefreshing = true }
            )
            
            // Floating insight cards
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(insights) { insight ->
                    TimekeeperInsightCard(
                        insight = insight,
                        onClick = { onInsightClick(insight) }
                    )
                }
            }
        }
    }
}

/**
 * Enhanced header with mini Timekeeper ring
 */
@Composable
private fun TimekeeperInsightsHeader(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    TimekeeperTheme.TimekeeperCard(
        modifier = modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mini Timekeeper ring
            TimekeeperMiniRing(
                isAnimating = isRefreshing,
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Insights that anticipate your rhythm",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Intelligent analysis of your financial patterns",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(
                onClick = onRefresh,
                enabled = !isRefreshing
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh insights",
                    tint = if (isRefreshing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Mini Timekeeper ring for header
 */
@Composable
private fun TimekeeperMiniRing(
    isAnimating: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mini_ring")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isAnimating) 2000 else 8000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
        val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        
        androidx.compose.foundation.Canvas(modifier = modifier) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = minOf(size.width, size.height) / 2 - 4.dp.toPx()
            val strokeWidth = 3.dp.toPx()
            
            // Background ring
            drawCircle(
                color = outlineColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )
        
        // Animated progress ring
        drawContext.canvas.rotate(rotation, center.x, center.y)
        drawArc(
            color = Color(0xFF5865F2),
            startAngle = 0f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(
                center.x - radius,
                center.y - radius
            ),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

/**
 * Floating insight card with animated ring indicator
 */
@Composable
private fun TimekeeperInsightCard(
    insight: Insight,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TimekeeperAnimations.FloatingCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated ring indicator
            TimekeeperRingIndicator(
                status = getInsightStatus(insight),
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = getInsightDescription(insight),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (insight.savingsPotential > 0) {
                    Text(
                        text = "Saving potential: $${insight.savingsPotential}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium
                    )
                }
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
 * Animated ring indicator with color-coded states
 */
@Composable
private fun TimekeeperRingIndicator(
    status: InsightStatus,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ring_indicator")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = minOf(size.width, size.height) / 2 - 4.dp.toPx()
            val strokeWidth = 3.dp.toPx()
            
            // Background ring
            drawCircle(
                color = outlineColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )
            
            // Status ring with pulse
            val statusColor = when (status) {
                InsightStatus.Optimal -> Color(0xFF4CAF50)
                InsightStatus.Stable -> Color(0xFF2196F3)
                InsightStatus.AdjustmentNeeded -> Color(0xFF8E44AD)
            }
            
            drawCircle(
                color = statusColor.copy(alpha = pulseAlpha),
                radius = radius * pulseScale,
                center = center,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Get insight status based on content
 */
private fun getInsightStatus(insight: Insight): InsightStatus {
    return when {
        insight.savingsPotential > 50 -> InsightStatus.Optimal
        insight.savingsPotential > 0 -> InsightStatus.Stable
        else -> InsightStatus.AdjustmentNeeded
    }
}

/**
 * Get insight description with brand tone
 */
private fun getInsightDescription(insight: Insight): String {
    return when {
        insight.savingsPotential > 50 -> "Rhythm stable. No intervention required."
        insight.savingsPotential > 0 -> "Minor misalignment detected — saving potential: $${insight.savingsPotential}"
        else -> "Flow disrupted — let's realign your timing"
    }
}

/**
 * Insight status enum
 */
enum class InsightStatus {
    Optimal,
    Stable,
    AdjustmentNeeded
}
