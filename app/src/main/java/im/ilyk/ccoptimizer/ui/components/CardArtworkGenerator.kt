package im.ilyk.ccoptimizer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import im.ilyk.ccoptimizer.data.models.CreditCard

/**
 * Generates beautiful card designs based on bank information
 * Similar to what you see in digital wallets like Apple Pay/Google Pay
 * 
 * NOTE: In Plaid sandbox, we only get basic institution logos and primary colors.
 * In production, banks provide full card artwork images with custom designs,
 * patterns, and high-resolution branding. This generator creates realistic
 * fallback designs that simulate what production cards would look like.
 */
@Composable
fun CardArtworkGenerator(
    card: CreditCard,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Enhanced gradient backgrounds with bank-specific designs
        CardArtworkBackground(
            bankName = card.bankName,
            primaryColor = card.primaryColor,
            cardNetwork = card.cardNetwork
        )
        
        // Bank logo overlay
        if (card.logoUrl != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                // Bank logo would go here if we had AsyncImage
                // For now, we'll use enhanced gradients
            }
        }
        
        // Card network badge
        CardNetworkBadge(
            cardNetwork = card.cardNetwork,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )
        
        // Subtle pattern overlay for premium look
        CardPatternOverlay(
            bankName = card.bankName,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun CardArtworkBackground(
    bankName: String,
    primaryColor: String?,
    cardNetwork: String?
) {
    val colors = when {
        primaryColor != null -> {
            try {
                val baseColor = Color(android.graphics.Color.parseColor(primaryColor))
                listOf(
                    baseColor,
                    baseColor.copy(alpha = 0.8f),
                    baseColor.copy(alpha = 0.6f)
                )
            } catch (e: Exception) {
                getBankSpecificColors(bankName, cardNetwork)
            }
        }
        else -> getBankSpecificColors(bankName, cardNetwork)
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Base gradient background
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
        
        // Add subtle card texture/pattern overlay (removed circle gradient)
        // CardTextureOverlay(
        //     bankName = bankName,
        //     cardNetwork = cardNetwork,
        //     modifier = Modifier.fillMaxSize()
        // )
        
        // Add subtle shine/reflection effect
        CardShineEffect(
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun getBankSpecificColors(bankName: String, cardNetwork: String?): List<Color> {
    return when {
        // Major Banks - More realistic color schemes
        bankName.contains("Chase", ignoreCase = true) -> listOf(
            Color(0xFF1E3A8A), Color(0xFF1E40AF), Color(0xFF1D4ED8)
        )
        bankName.contains("American Express", ignoreCase = true) -> listOf(
            Color(0xFF1E40AF), Color(0xFF2563EB), Color(0xFF3B82F6)
        )
        bankName.contains("Capital One", ignoreCase = true) -> listOf(
            Color(0xFFDC2626), Color(0xFFEF4444), Color(0xFFF87171)
        )
        bankName.contains("Discover", ignoreCase = true) -> listOf(
            Color(0xFFEA580C), Color(0xFFF97316), Color(0xFFFB923C)
        )
        bankName.contains("Wells Fargo", ignoreCase = true) -> listOf(
            Color(0xFFDC2626), Color(0xFFEF4444), Color(0xFFFCA5A5)
        )
        bankName.contains("Bank of America", ignoreCase = true) -> listOf(
            Color(0xFFDC2626), Color(0xFFEF4444), Color(0xFFFCA5A5)
        )
        bankName.contains("Citi", ignoreCase = true) -> listOf(
            Color(0xFF0284C7), Color(0xFF0EA5E9), Color(0xFF38BDF8)
        )
        bankName.contains("US Bank", ignoreCase = true) -> listOf(
            Color(0xFF1E40AF), Color(0xFF2563EB), Color(0xFF3B82F6)
        )
        bankName.contains("SoFi", ignoreCase = true) -> listOf(
            Color(0xFF059669), Color(0xFF10B981), Color(0xFF34D399)
        )
        bankName.contains("TD Bank", ignoreCase = true) -> listOf(
            Color(0xFF7C2D12), Color(0xFF92400E), Color(0xFFB45309)
        )
        bankName.contains("PNC", ignoreCase = true) -> listOf(
            Color(0xFF1F2937), Color(0xFF374151), Color(0xFF4B5563)
        )
        bankName.contains("Fifth Third", ignoreCase = true) -> listOf(
            Color(0xFF7C2D12), Color(0xFF92400E), Color(0xFFB45309)
        )
        bankName.contains("Regions", ignoreCase = true) -> listOf(
            Color(0xFF1E40AF), Color(0xFF2563EB), Color(0xFF3B82F6)
        )
        bankName.contains("BB&T", ignoreCase = true) -> listOf(
            Color(0xFF1E40AF), Color(0xFF2563EB), Color(0xFF3B82F6)
        )
        bankName.contains("SunTrust", ignoreCase = true) -> listOf(
            Color(0xFF1E40AF), Color(0xFF2563EB), Color(0xFF3B82F6)
        )
        // Credit Unions
        bankName.contains("Navy Federal", ignoreCase = true) -> listOf(
            Color(0xFF1E40AF), Color(0xFF2563EB), Color(0xFF3B82F6)
        )
        bankName.contains("PenFed", ignoreCase = true) -> listOf(
            Color(0xFF7C2D12), Color(0xFF92400E), Color(0xFFB45309)
        )
        // Store Cards
        bankName.contains("Home Depot", ignoreCase = true) -> listOf(
            Color(0xFFDC2626), Color(0xFFEF4444), Color(0xFFF87171)
        )
        bankName.contains("Target", ignoreCase = true) -> listOf(
            Color(0xFFDC2626), Color(0xFFEF4444), Color(0xFFF87171)
        )
        bankName.contains("Amazon", ignoreCase = true) -> listOf(
            Color(0xFFF59E0B), Color(0xFFFBBF24), Color(0xFFFCD34D)
        )
        // Fintech
        bankName.contains("Chime", ignoreCase = true) -> listOf(
            Color(0xFF059669), Color(0xFF10B981), Color(0xFF34D399)
        )
        bankName.contains("Varo", ignoreCase = true) -> listOf(
            Color(0xFF7C3AED), Color(0xFF8B5CF6), Color(0xFFA78BFA)
        )
        // Fallback to card network colors
        else -> when (cardNetwork?.lowercase()) {
            "visa" -> listOf(Color(0xFF1E40AF), Color(0xFF2563EB), Color(0xFF3B82F6))
            "mastercard" -> listOf(Color(0xFFDC2626), Color(0xFFEF4444), Color(0xFFF87171))
            "american express", "amex" -> listOf(Color(0xFF1E40AF), Color(0xFF2563EB), Color(0xFF3B82F6))
            "discover" -> listOf(Color(0xFFEA580C), Color(0xFFF97316), Color(0xFFFB923C))
            else -> listOf(Color(0xFF6366F1), Color(0xFF4F46E5), Color(0xFF3730A3))
        }
    }
}

@Composable
private fun CardNetworkBadge(
    cardNetwork: String?,
    modifier: Modifier = Modifier
) {
    val networkColor = when (cardNetwork?.lowercase()) {
        "visa" -> Color.White
        "mastercard" -> Color.White
        "american express", "amex" -> Color.White
        "discover" -> Color.White
        else -> Color.White.copy(alpha = 0.8f)
    }
    
    val networkText = when (cardNetwork?.lowercase()) {
        "visa" -> "VISA"
        "mastercard" -> "Mastercard"
        "american express", "amex" -> "AMEX"
        "discover" -> "Discover"
        else -> cardNetwork?.uppercase() ?: ""
    }
    
    if (networkText.isNotEmpty()) {
        Text(
            text = networkText,
            color = networkColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = modifier
        )
    }
}

@Composable
private fun CardTextureOverlay(
    bankName: String,
    cardNetwork: String?,
    modifier: Modifier = Modifier
) {
    // Add subtle texture patterns based on bank and card network
    val textureColor = when {
        bankName.contains("Chase", ignoreCase = true) -> Color.White.copy(alpha = 0.12f)
        bankName.contains("American Express", ignoreCase = true) -> Color.White.copy(alpha = 0.18f)
        bankName.contains("Capital One", ignoreCase = true) -> Color.White.copy(alpha = 0.10f)
        bankName.contains("Discover", ignoreCase = true) -> Color.White.copy(alpha = 0.15f)
        bankName.contains("Wells Fargo", ignoreCase = true) -> Color.White.copy(alpha = 0.08f)
        else -> Color.White.copy(alpha = 0.06f)
    }
    
    // Create a subtle texture pattern
    Box(
        modifier = modifier.background(
            Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    textureColor.copy(alpha = 0.3f),
                    textureColor,
                    textureColor.copy(alpha = 0.5f),
                    Color.Transparent
                ),
                radius = 600f
            )
        )
    )
}

@Composable
private fun CardShineEffect(
    modifier: Modifier = Modifier
) {
    // Add a subtle shine/reflection effect to simulate card material
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.15f),
                    Color.Transparent,
                    Color.Transparent,
                    Color.White.copy(alpha = 0.05f)
                ),
                start = androidx.compose.ui.geometry.Offset(-200f, -200f),
                end = androidx.compose.ui.geometry.Offset(400f, 400f)
            )
        )
    )
}

@Composable
private fun CardPatternOverlay(
    bankName: String,
    modifier: Modifier = Modifier
) {
    // Legacy function - keeping for compatibility
    CardTextureOverlay(bankName, null, modifier)
}
