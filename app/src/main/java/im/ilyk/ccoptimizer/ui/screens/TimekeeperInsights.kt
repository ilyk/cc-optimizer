package im.ilyk.ccoptimizer.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import im.ilyk.ccoptimizer.ui.theme.NumericTypography
import im.ilyk.ccoptimizer.ui.theme.TimekeeperTheme

/**
 * Timekeeper Insights Screen - Financial rhythm and predictive analytics
 */
@Composable
fun TimekeeperInsightsScreen(
    insights: List<InsightData>,
    cashFlowRhythm: Float = 0.75f, // 0.0 to 1.0, where 1.0 is optimal rhythm
    modifier: Modifier = Modifier
) {
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
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Enhanced Financial Rhythm Header
            EnhancedFinancialRhythmCard(
                rhythm = cashFlowRhythm,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )
            
            // Floating Insights List with enhanced cards
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(insights) { insight ->
                    EnhancedInsightCard(insight = insight)
                }
            }
        }
    }
}

/**
 * Financial Rhythm Card - Mini Timekeeper ring showing cash-flow rhythm
 */
@Composable
private fun FinancialRhythmCard(
    rhythm: Float,
    modifier: Modifier = Modifier
) {
    // Rhythm pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "rhythm_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (2000 / rhythm).toInt().coerceAtLeast(1000),
                easing = EaseInOut
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (2000 / rhythm).toInt().coerceAtLeast(1000),
                easing = EaseInOut
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rhythm visualization
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(60.dp)
            ) {
                // Get colors outside Canvas
                val outlineVariant = MaterialTheme.colorScheme.outlineVariant
                val primaryColor = MaterialTheme.colorScheme.primary
                val secondaryColor = MaterialTheme.colorScheme.secondary
                
                // Background ring
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val strokeWidth = 6.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val center = Offset(size.width / 2, size.height / 2)
                    
                    drawCircle(
                        color = outlineVariant,
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeWidth)
                    )
                    
                    // Rhythm progress
                    val sweepAngle = rhythm * 360f
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                primaryColor,
                                secondaryColor
                            )
                        ),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                
                // Pulsing center
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .scale(pulseScale)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Rhythm description
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Financial Rhythm",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                val rhythmDescription = when {
                    rhythm >= 0.9f -> "Perfectly synchronized. All cycles aligned."
                    rhythm >= 0.7f -> "Well balanced. Minor optimizations available."
                    rhythm >= 0.5f -> "Moderate rhythm. Some timing adjustments needed."
                    else -> "Rhythm needs attention. Significant optimization required."
                }
                
                Text(
                    text = rhythmDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Insight Card with gradient and narrative
 */
@Composable
private fun InsightCard(
    insight: InsightData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = insight.gradient,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Icon(
                    imageVector = insight.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = insight.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = insight.narrative,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    
                    if (insight.value != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = insight.value,
                            style = NumericTypography.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    }
                }
                
                // Trend indicator
                if (insight.trend != null) {
                    Icon(
                        imageVector = insight.trend,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Insight Data Model
 */
data class InsightData(
    val title: String,
    val narrative: String,
    val value: String? = null,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val gradient: Brush,
    val trend: androidx.compose.ui.graphics.vector.ImageVector? = null
)

/**
 * Enhanced Financial Rhythm Card with floating design
 */
@Composable
private fun EnhancedFinancialRhythmCard(
    rhythm: Float,
    modifier: Modifier = Modifier
) {
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
            // Enhanced mini Timekeeper ring with pulse
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(60.dp)
            ) {
                // Pulsing ring indicator
                TimekeeperTheme.TimekeeperPulse(
                    modifier = Modifier.size(60.dp),
                    isActive = rhythm > 0.7f
                )
                
                // Mini rhythm ring
                val outlineVariant = MaterialTheme.colorScheme.outlineVariant
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 4.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val center = Offset(size.width / 2, size.height / 2)
                    
                    // Background circle
                    drawCircle(
                        color = outlineVariant,
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeWidth)
                    )
                    
                    // Rhythm arc
                    val sweepAngle = rhythm * 360f
                    drawArc(
                        brush = when {
                            rhythm >= 0.8f -> Brush.sweepGradient(listOf(
                                Color(0xFF4CAF50), // Green for optimal
                                Color(0xFF8BC34A)
                            ))
                            rhythm >= 0.6f -> Brush.sweepGradient(listOf(
                                Color(0xFF2196F3), // Blue for stable
                                Color(0xFF64B5F6)
                            ))
                            else -> Brush.sweepGradient(listOf(
                                Color(0xFF9C27B0), // Violet for adjustment needed
                                Color(0xFFBA68C8)
                            ))
                        },
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Rhythm insights with improved text tone
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Financial Rhythm",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                val rhythmText = when {
                    rhythm >= 0.8f -> "Rhythm stable. No intervention required."
                    rhythm >= 0.6f -> "Minor misalignment detected — saving potential: $18."
                    else -> "Flow disrupted — let's realign your timing."
                }
                
                Text(
                    text = rhythmText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Enhanced Insight Card with floating design and pulsing indicators
 */
@Composable
private fun EnhancedInsightCard(
    insight: InsightData,
    modifier: Modifier = Modifier
) {
    TimekeeperTheme.TimekeeperCard(
        modifier = modifier,
        onClick = { /* Handle insight click */ }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pulsing ring indicator based on insight state
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp)
            ) {
                TimekeeperTheme.TimekeeperPulse(
                    modifier = Modifier.size(48.dp),
                    isActive = true
                )
                
                Icon(
                    imageVector = insight.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Insight content with enhanced text tone
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = insight.narrative,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (insight.value != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = insight.value,
                        style = NumericTypography.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Trend indicator
            if (insight.trend != null) {
                Icon(
                    imageVector = insight.trend,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Sample insights data
 */
fun getSampleInsights(): List<InsightData> {
    return listOf(
        InsightData(
            title = "Interest Saved",
            narrative = "Delaying Card A by 2 days saves $17 in interest.",
            value = "$127.50",
            icon = Icons.Default.Savings,
            gradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF3BA55D), // Emerald Flow
                    Color(0xFF2E7D32)
                )
            ),
            trend = Icons.Default.TrendingUp
        ),
        InsightData(
            title = "Payment Timing Impact",
            narrative = "Optimizing payment dates reduces interest by 23%.",
            value = "23%",
            icon = Icons.Default.Schedule,
            gradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF8E44AD), // Violet
                    Color(0xFF6A1B9A)
                )
            ),
            trend = Icons.Default.TrendingDown
        ),
        InsightData(
            title = "Spending Velocity",
            narrative = "Your spending pattern shows optimal cash flow timing.",
            value = "Optimal",
            icon = Icons.Default.Speed,
            gradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFC7D0E0), // Silver Mist
                    Color(0xFF9CA3AF)
                )
            ),
            trend = Icons.Default.TrendingUp
        ),
        InsightData(
            title = "Cycle Alignment",
            narrative = "All payment cycles are perfectly synchronized.",
            value = "100%",
            icon = Icons.Default.Sync,
            gradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF5865F2), // Electric Blue
                    Color(0xFF3B82F6)
                )
            ),
            trend = Icons.Default.CheckCircle
        )
    )
}
