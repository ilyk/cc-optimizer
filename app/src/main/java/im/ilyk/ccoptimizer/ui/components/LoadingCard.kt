package im.ilyk.ccoptimizer.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import im.ilyk.ccoptimizer.R

@Composable
fun LoadingCardSkeleton() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Shimmer rectangles for card name and number
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(20.dp)
                    .alpha(alpha)
                    .background(
                        Color.Gray.copy(alpha = 0.3f),
                        RoundedCornerShape(4.dp)
                    )
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(14.dp)
                    .alpha(alpha)
                    .background(
                        Color.Gray.copy(alpha = 0.2f),
                        RoundedCornerShape(4.dp)
                    )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Shimmer for payment info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(40.dp)
                        .alpha(alpha)
                        .background(
                            Color.Gray.copy(alpha = 0.3f),
                            RoundedCornerShape(4.dp)
                        )
                )
                
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(40.dp)
                        .alpha(alpha)
                        .background(
                            Color.Gray.copy(alpha = 0.3f),
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }
    }
}

@Composable
fun AnalyzingCard(cardName: String, cardMask: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "analyzing")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )
    
    val dotCount = ((progress * 3).toInt() % 4)
    val dots = ".".repeat(dotCount)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = cardName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.loading_card_mask, cardMask),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Divider()
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Analyzing status with animated dots
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.loading_ai_emoji),
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.loading_analyzing_patterns, dots),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress description
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.small
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = when (dotCount) {
                            0 -> "Fetching transaction history..."
                            1 -> "Running AI analysis..."
                            2 -> "Detecting patterns..."
                            else -> "Almost done..."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun ProcessingCard(cardName: String, cardMask: String, status: String = "Processing") {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = cardName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "•••• $cardMask",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

