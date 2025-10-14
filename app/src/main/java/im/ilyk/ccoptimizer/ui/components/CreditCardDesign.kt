package im.ilyk.ccoptimizer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import im.ilyk.ccoptimizer.R
import im.ilyk.ccoptimizer.data.models.CreditCard
import im.ilyk.ccoptimizer.data.models.PaymentRecommendation
import java.time.format.DateTimeFormatter

/**
 * Credit card design component with real bank branding
 * Similar to Google Pay, Apple Pay card appearance
 */
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
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onClick ?: {}
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background gradient using bank's primary color
            CardBackgroundGradient(
                primaryColor = card.primaryColor,
                cardNetwork = card.cardNetwork
            )
            
            // Card content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top row: Bank logo and payment status
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
                    
                    // Payment urgency badge
                    recommendation?.let { rec ->
                        PaymentUrgencyBadge(rec.daysUntilPayment)
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Middle: Card number (stylized)
                Text(
                    text = stringResource(R.string.card_number_format, card.lastFourDigits),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Bottom row: Card details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Left: Card name and balance
                    Column {
                        Text(
                            text = card.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.95f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.card_balance_format, String.format("%,.2f", card.currentBalance)),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    
                    // Right: Card network logo or recommendation
                    recommendation?.let { rec ->
                        PaymentInfo(rec)
                    } ?: run {
                        CardNetworkBadge(card.cardNetwork)
                    }
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
    // Parse bank's primary color or use fallback based on network
    val colors = try {
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
                else -> listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.primaryContainer
                )
            }
        }
    } catch (e: Exception) {
        // Fallback to Material colors if color parsing fails
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer
        )
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

