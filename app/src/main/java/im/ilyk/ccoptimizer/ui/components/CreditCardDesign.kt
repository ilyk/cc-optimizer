package im.ilyk.ccoptimizer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import im.ilyk.ccoptimizer.R
import im.ilyk.ccoptimizer.data.models.CreditCard
import im.ilyk.ccoptimizer.data.models.PaymentRecommendation
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Credit card design component with real bank branding
 * Similar to Google Pay, Apple Pay card appearance
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardDesign(
    card: CreditCard,
    recommendation: PaymentRecommendation? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(16.dp),  // Standardized corner radius (8dp grid: 2x8)
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onClick ?: {}
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Full card artwork (if available) or gradient background
            if (card.cardArtworkUrl != null) {
                // Display full card artwork image
                AsyncImage(
                    model = card.cardArtworkUrl,
                    contentDescription = "${card.bankName} card design",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                // Dynamic scrim overlay for WCAG AA contrast (4.5:1)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.4f),  // Top: darker for logo/indicator
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.6f)   // Bottom: darker for text
                                )
                            )
                        )
                )
            } else {
                // Enhanced fallback: Generated card artwork based on bank branding
                CardArtworkGenerator(
                    card = card,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Card content with 8dp grid spacing
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),  // 8dp grid: 16dp base padding
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top row: Bank logo and confidence indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Bank logo (from Plaid CDN)
                    if (card.logoUrl != null) {
                        AsyncImage(
                            model = card.logoUrl,
                            contentDescription = "${card.bankName} logo",
                            modifier = Modifier
                                .height(32.dp)
                                .widthIn(max = 120.dp)
                        )
                    } else {
                        // Fallback: Bank name
                        Text(
                            text = card.bankName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                    
                    // Confidence/Verification indicator
                    ConfidenceIndicator(
                        confidence = card.inferenceConfidence,
                        isVerified = card.isDateVerified
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Middle: Card number and issuer
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = card.bankName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = stringResource(R.string.card_number_format, card.lastFourDigits),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Bottom: Balance and close/due dates
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Left: Balance breakdown
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        // Always show bank balance
                        Text(
                            text = "Balance",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = stringResource(R.string.card_balance_format, String.format("%,.2f", card.currentBalance)),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        
                        // Show manual payments if any
                        if (card.manualPaymentsTotal > 0) {
                            Text(
                                text = "Paid: -$${String.format("%,.2f", card.manualPaymentsTotal)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            )
                        }
                    }
                    
                    // Right: Close/Due dates
                    StatementDatesDisplay(
                        closeDate = card.statementCloseDate,
                        dueDate = card.statementDueDate,
                        recommendation = recommendation
                    )
                }
            }
        }
    }
}

@Composable
private fun CardBackgroundGradient(
    primaryColor: String?,
    cardNetwork: String?
) {
    // Material theme colors for fallback
    val fallbackPrimary = MaterialTheme.colorScheme.primary
    val fallbackPrimaryContainer = MaterialTheme.colorScheme.primaryContainer
    
    // Parse bank's primary color or use fallback based on network
    val colors = remember(primaryColor, cardNetwork) {
        try {
            if (primaryColor != null) {
                val baseColor = Color(android.graphics.Color.parseColor(primaryColor))
                listOf(
                    baseColor,
                    baseColor.copy(alpha = 0.8f),
                    baseColor.copy(alpha = 0.6f)
                )
            } else {
                // Fallback gradient based on card network
                when (cardNetwork?.lowercase()) {
                    "visa" -> listOf(Color(0xFF1434CB), Color(0xFF0D2580))
                    "mastercard" -> listOf(Color(0xFFEB001B), Color(0xFFB80016))
                    "american express", "amex" -> listOf(Color(0xFF006FCF), Color(0xFF004A8F))
                    "discover" -> listOf(Color(0xFFFF6000), Color(0xFFCC4D00))
                    else -> listOf(fallbackPrimary, fallbackPrimaryContainer)
                }
            }
        } catch (e: Exception) {
            // Fallback to Material colors if color parsing fails
            listOf(fallbackPrimary, fallbackPrimaryContainer)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = colors,
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(1000f, 1000f)
                )
            )
    )
}

@Composable
private fun PaymentUrgencyBadge(daysUntil: Int) {
    val (text, backgroundColor) = when {
        daysUntil <= 0 -> "DUE TODAY!" to Color(0xFFD32F2F)
        daysUntil == 1 -> "DUE TOMORROW" to Color(0xFFF57C00)
        daysUntil <= 3 -> "$daysUntil DAYS" to Color(0xFFFBC02D)
        else -> "$daysUntil DAYS" to Color.White.copy(alpha = 0.3f)
    }
    
    Surface(
        shape = MaterialTheme.shapes.small,
        color = backgroundColor,
        contentColor = Color.White
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}

@Composable
private fun PaymentInfo(recommendation: PaymentRecommendation) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = recommendation.recommendedPaymentDate.format(
                DateTimeFormatter.ofPattern("MMM dd")
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
        Text(
            text = stringResource(R.string.card_amount_format, String.format("%,.2f", recommendation.recommendedAmount)),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}

@Composable
private fun CardNetworkBadge(network: String?) {
    network?.let {
        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = Color.White.copy(alpha = 0.2f)
        ) {
            Text(
                text = network.uppercase(),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
    }
}

/**
 * Confidence indicator for statement date inference
 * Shows amber warning if confidence < 0.6, green checkmark if verified
 */
@Composable
private fun ConfidenceIndicator(
    confidence: Double?,
    isVerified: Boolean
) {
    val indicatorData = when {
        isVerified -> Triple(
            "Verified",
            Color(0xFF4CAF50),  // Green - WCAG AA compliant
            Color.White
        )
        confidence != null && confidence < 0.6 -> Triple(
            "Needs confirm",
            Color(0xFFFFC107),  // Amber - WCAG AA compliant
            Color.Black
        )
        confidence != null -> Triple(
            "Inferred",
            Color.White.copy(alpha = 0.3f),
            Color.White
        )
        else -> null
    }
    
    indicatorData?.let { (text, backgroundColor, textColor) ->
        Surface(
            shape = MaterialTheme.shapes.small,
            color = backgroundColor,
            contentColor = textColor
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
    }
}

/**
 * Displays statement close and due dates
 */
@Composable
private fun StatementDatesDisplay(
    closeDate: LocalDate?,
    dueDate: LocalDate?,
    recommendation: PaymentRecommendation?
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Display close date
        closeDate?.let { close ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Closes",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = close.format(DateTimeFormatter.ofPattern("MMM dd")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
        }
        
        // Display due date or recommendation
        if (recommendation != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
            }
        } else {
            dueDate?.let { due ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
}

