package im.ilyk.ccoptimizer.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*

/**
 * Timekeeper Lottie Animations
 * Premium animations for the Timekeeper symbol and key interactions
 */

/**
 * Timekeeper Symbol Animation
 * The core Timekeeper symbol with rotating arcs and pulsing center
 */
@Composable
fun TimekeeperSymbolAnimation(
    isActive: Boolean = true,
    size: Int = 120,
    modifier: Modifier = Modifier
) {
    // Create a programmatic Lottie animation for the Timekeeper symbol
    // Since we don't have a Lottie JSON file, we'll create a custom animation
    val infiniteTransition = rememberInfiniteTransition(label = "timekeeper_symbol")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Box(
        modifier = modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer rotating ring
        TimekeeperRotatingRing(
            rotation = rotation,
            alpha = alpha,
            size = size,
            modifier = Modifier.fillMaxSize()
        )
        
        // Inner pulsing center
        TimekeeperPulsingCenter(
            pulse = pulse,
            alpha = alpha,
            size = size / 3,
            modifier = Modifier.size((size / 3).dp)
        )
    }
}

/**
 * Timekeeper Success Animation
 * Celebration animation for successful optimizations
 */
@Composable
fun TimekeeperSuccessAnimation(
    isVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "success_animation")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseOutBack),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )
    
    if (isVisible) {
        Box(
            modifier = modifier
                .scale(scale)
                .alpha(alpha),
            contentAlignment = Alignment.Center
        ) {
            // Success checkmark animation
            TimekeeperCheckmarkAnimation()
        }
    }
}

/**
 * Timekeeper Loading Animation
 * Sophisticated loading animation with Timekeeper branding
 */
@Composable
fun TimekeeperLoadingAnimation(
    isLoading: Boolean = true,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_animation")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    if (isLoading) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            TimekeeperRotatingRing(
                rotation = rotation,
                alpha = alpha,
                size = 80,
                modifier = Modifier.size(80.dp)
            )
        }
    }
}

/**
 * Timekeeper Pulse Animation
 * Subtle pulse animation for interactive elements
 */
@Composable
fun TimekeeperPulseAnimation(
    isPulsing: Boolean = true,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_animation")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    if (isPulsing) {
        Box(
            modifier = modifier
                .scale(scale)
                .alpha(alpha)
        ) {
            // Content goes here
        }
    }
}

/**
 * Timekeeper Rotating Ring
 * Custom rotating ring animation for Timekeeper symbol
 */
@Composable
private fun TimekeeperRotatingRing(
    rotation: Float,
    alpha: Float,
    size: Int,
    modifier: Modifier = Modifier
) {
    // This would be implemented with Canvas drawing
    // For now, we'll use a placeholder
    Box(
        modifier = modifier
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        // Placeholder for rotating ring
        Surface(
            modifier = Modifier.size((size * 0.8).dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        ) {}
    }
}

/**
 * Timekeeper Pulsing Center
 * Pulsing center dot for Timekeeper symbol
 */
@Composable
private fun TimekeeperPulsingCenter(
    pulse: Float,
    alpha: Float,
    size: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .scale(pulse)
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(size.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = MaterialTheme.colorScheme.primary
        ) {}
    }
}

/**
 * Timekeeper Checkmark Animation
 * Animated checkmark for success states
 */
@Composable
private fun TimekeeperCheckmarkAnimation() {
    // Placeholder for checkmark animation
    Surface(
        modifier = Modifier.size(40.dp),
        shape = androidx.compose.foundation.shape.CircleShape,
        color = MaterialTheme.colorScheme.primary
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            // Checkmark icon would go here
        }
    }
}
