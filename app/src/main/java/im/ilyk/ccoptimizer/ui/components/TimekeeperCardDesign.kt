package im.ilyk.ccoptimizer.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import im.ilyk.ccoptimizer.R
import im.ilyk.ccoptimizer.data.models.CreditCard
import im.ilyk.ccoptimizer.data.models.PaymentRecommendation
import im.ilyk.ccoptimizer.ui.theme.NumericTypography
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.*

/**
 * Enhanced Credit Card Design with Timekeeper animations and micro-interactions
 * Features: Hover effects, pulse animations, gradient overlays, and smooth transitions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimekeeperCardDesign(
    card: CreditCard,
    recommendation: PaymentRecommendation? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    showOptimization: Boolean = true
) {
    // Timekeeper animations
    val infiniteTransition = rememberInfiniteTransition(label = "timekeeper_card")
    
    // Pulse animation for optimization indicator
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    
    // Rotation animation for Timekeeper core
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // Hover/selection animations
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = tween(300, easing = EaseOutCubic),
        label = "scale"
    )
    
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 12.dp else 4.dp,
        animationSpec = tween(300, easing = EaseOutCubic),
        label = "elevation"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .scale(scale)
            .semantics(mergeDescendants = false) {
                contentDescription = "${card.bankName} card ending in ${card.lastFourDigits}"
            },
        shape = RoundedCornerShape(20.dp), // Enhanced corner radius for Timekeeper design
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        onClick = onClick ?: {}
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Enhanced card background with Timekeeper gradients
            TimekeeperCardBackground(
                card = card,
                modifier = Modifier.fillMaxSize()
            )
            
            // Timekeeper optimization overlay
            if (showOptimization && recommendation != null) {
                TimekeeperOptimizationOverlay(
                    recommendation = recommendation,
                    pulseScale = pulseScale,
                    pulseAlpha = pulseAlpha,
                    rotation = rotation,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Card content with enhanced spacing
            TimekeeperCardContent(
                card = card,
                recommendation = recommendation,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            )
        }
    }
}

/**
 * Timekeeper card background with enhanced gradients
 */
@Composable
private fun TimekeeperCardBackground(
    card: CreditCard,
    modifier: Modifier = Modifier
) {
    if (card.cardArtworkUrl != null) {
        // Enhanced artwork with Timekeeper overlay
        AsyncImage(
            model = card.cardArtworkUrl,
            contentDescription = "${card.bankName} card design",
            modifier = modifier,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
        
        // Timekeeper gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f)
                        )
                    )
                )
        )
    } else {
        // Enhanced generated background with Timekeeper gradients
        TimekeeperGeneratedBackground(
            card = card,
            modifier = modifier
        )
    }
}

/**
 * Generated background with Timekeeper design elements
 */
@Composable
private fun TimekeeperGeneratedBackground(
    card: CreditCard,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val backgroundOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "background_offset"
    )
    
    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                    ),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(1000f, 1000f)
                )
            )
    ) {
        // Timekeeper pattern overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = minOf(size.width, size.height) * 0.3f
            
            // Rotating Timekeeper arcs
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.1f),
                        Color.Transparent
                    )
                ),
                startAngle = backgroundOffset * 360f,
                sweepAngle = 120f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Timekeeper optimization overlay with animated elements
 */
@Composable
private fun TimekeeperOptimizationOverlay(
    recommendation: PaymentRecommendation,
    pulseScale: Float,
    pulseAlpha: Float,
    rotation: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopEnd
    ) {
        // Optimization indicator with pulse
        Box(
            modifier = Modifier
                .padding(16.dp)
                .size(40.dp)
                .scale(pulseScale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Rotating Timekeeper core
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .rotate(rotation)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White,
                                MaterialTheme.colorScheme.primary
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }
    }
}

/**
 * Enhanced card content with Timekeeper typography
 */
@Composable
private fun TimekeeperCardContent(
    card: CreditCard,
    recommendation: PaymentRecommendation?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top row: Bank logo and optimization status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Bank logo
            if (card.logoUrl != null) {
                AsyncImage(
                    model = card.logoUrl,
                    contentDescription = "${card.bankName} logo",
                    modifier = Modifier
                        .height(32.dp)
                        .widthIn(max = 120.dp)
                )
            } else {
                Text(
                    text = card.bankName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                )
            }
            
            // Optimization status with Timekeeper styling
            TimekeeperOptimizationStatus(
                confidence = card.inferenceConfidence,
                isVerified = card.isDateVerified,
                hasRecommendation = recommendation != null
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Middle: Card details with enhanced typography
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = card.bankName,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            Text(
                text = stringResource(R.string.card_number_format, card.lastFourDigits),
                style = NumericTypography.copy(
                    fontSize = 18.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                ),
                color = Color.White,
                letterSpacing = 2.sp
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Bottom: Balance and recommendation with Timekeeper styling
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Balance with enhanced display
            TimekeeperBalanceDisplay(
                balance = card.currentBalance,
                manualPayments = card.manualPaymentsTotal
            )
            
            // Recommendation with Timekeeper styling
            TimekeeperRecommendationDisplay(
                recommendation = recommendation,
                dueDate = card.statementDueDate
            )
        }
    }
}

/**
 * Timekeeper optimization status indicator
 */
@Composable
private fun TimekeeperOptimizationStatus(
    confidence: Double?,
    isVerified: Boolean,
    hasRecommendation: Boolean
) {
    val statusInfo = when {
        hasRecommendation -> Triple(
            "Optimized",
            MaterialTheme.colorScheme.primary,
            Color.White
        )
        isVerified -> Triple(
            "Verified",
            Color(0xFF4CAF50),
            Color.White
        )
        confidence != null && confidence < 0.6 -> Triple(
            "Needs Review",
            Color(0xFFFFC107),
            Color.Black
        )
        confidence != null -> Triple(
            "Inferred",
            Color.White.copy(alpha = 0.3f),
            Color.White
        )
        else -> null
    }
    
    statusInfo?.let { (statusText, bgColor, txtColor) ->
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = bgColor,
            contentColor = txtColor
        ) {
            Text(
                text = statusText,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
    }
}

/**
 * Enhanced balance display with Timekeeper styling
 */
@Composable
private fun TimekeeperBalanceDisplay(
    balance: Double,
    manualPayments: Double
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "Balance",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            text = stringResource(R.string.card_balance_format, String.format("%,.2f", balance)),
            style = NumericTypography.copy(
                fontSize = 18.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            ),
            color = Color.White
        )
        
        if (manualPayments > 0) {
            Text(
                text = "Paid: -$${String.format("%,.2f", manualPayments)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
        }
    }
}

/**
 * Enhanced recommendation display with Timekeeper styling
 */
@Composable
private fun TimekeeperRecommendationDisplay(
    recommendation: PaymentRecommendation?,
    dueDate: LocalDate?
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (recommendation != null) {
            Text(
                text = "Pay by",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = recommendation.recommendedPaymentDate.format(
                    DateTimeFormatter.ofPattern("MMM dd")
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.card_amount_format, String.format("%,.2f", recommendation.recommendedAmount)),
                style = NumericTypography.copy(
                    fontSize = 16.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            dueDate?.let { due ->
                Text(
                    text = "Due",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = due.format(DateTimeFormatter.ofPattern("MMM dd")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
        }
    }
}
