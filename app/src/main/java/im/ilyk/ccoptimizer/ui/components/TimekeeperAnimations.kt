package im.ilyk.ccoptimizer.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import im.ilyk.ccoptimizer.ui.theme.TimekeeperTheme
import kotlinx.coroutines.delay

/**
 * Enhanced micro-animations for Timekeeper UI polish
 */
object TimekeeperAnimations {
    
    /**
     * Tab change animation with soft fade and upward motion
     */
    @Composable
    fun TabChangeAnimation(
        isVisible: Boolean,
        content: @Composable () -> Unit
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = tween(300, easing = EaseOutCubic)
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { -it / 3 },
                animationSpec = tween(300, easing = EaseInCubic)
            ) + fadeOut(animationSpec = tween(300))
        ) {
            content()
        }
    }
    
    /**
     * Button press animation with gradient pulse
     */
    @Composable
    fun ButtonPressAnimation(
        isPressed: Boolean,
        content: @Composable () -> Unit
    ) {
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.95f else 1f,
            animationSpec = tween(250, easing = EaseOutCubic),
            label = "button_scale"
        )
        
        val alpha by animateFloatAsState(
            targetValue = if (isPressed) 0.8f else 1f,
            animationSpec = tween(250, easing = EaseOutCubic),
            label = "button_alpha"
        )
        
        Box(
            modifier = Modifier
                .scale(scale)
                .alpha(alpha)
        ) {
            content()
        }
    }
    
    /**
     * Data refresh animation with ripple wave
     */
    @Composable
    fun DataRefreshAnimation(
        isRefreshing: Boolean,
        content: @Composable () -> Unit
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "data_refresh")
        val rippleScale by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = if (isRefreshing) 2f else 0f,
            animationSpec = if (isRefreshing) {
                infiniteRepeatable(
                    animation = tween(600, easing = EaseOut),
                    repeatMode = RepeatMode.Restart
                )
            } else {
                infiniteRepeatable(
                    animation = tween(0, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            },
            label = "ripple_scale"
        )
        val rippleAlpha by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 0f,
            animationSpec = if (isRefreshing) {
                infiniteRepeatable(
                    animation = tween(600, easing = EaseOut),
                    repeatMode = RepeatMode.Restart
                )
            } else {
                infiniteRepeatable(
                    animation = tween(0, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            },
            label = "ripple_alpha"
        )
        
        Box {
            content()
            
            if (isRefreshing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color(0xFF5865F2).copy(alpha = rippleAlpha),
                            CircleShape
                        )
                        .scale(rippleScale)
                )
            }
        }
    }
    
    /**
     * Success state animation with arcs align and outward glow
     */
    @Composable
    fun SuccessAnimation(
        isSuccess: Boolean,
        content: @Composable () -> Unit
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "success_animation")
        val glowScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = if (isSuccess) 1.5f else 1f,
            animationSpec = if (isSuccess) {
                infiniteRepeatable(
                    animation = tween(1200, easing = EaseOut),
                    repeatMode = RepeatMode.Reverse
                )
            } else {
                infiniteRepeatable(
                    animation = tween(0, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            },
            label = "glow_scale"
        )
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = if (isSuccess) 0.8f else 0.3f,
            animationSpec = if (isSuccess) {
                infiniteRepeatable(
                    animation = tween(1200, easing = EaseOut),
                    repeatMode = RepeatMode.Reverse
                )
            } else {
                infiniteRepeatable(
                    animation = tween(0, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            },
            label = "glow_alpha"
        )
        
        Box {
            content()
            
            if (isSuccess) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color(0xFF4CAF50).copy(alpha = glowAlpha),
                            CircleShape
                        )
                        .scale(glowScale)
                        .blur(4.dp)
                )
            }
        }
    }
    
    /**
     * Error animation with subtle desync shake
     */
    @Composable
    fun ErrorAnimation(
        hasError: Boolean,
        content: @Composable () -> Unit
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "error_animation")
        val shakeX by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = if (hasError) 4f else 0f,
            animationSpec = if (hasError) {
                infiniteRepeatable(
                    animation = tween(500, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse
                )
            } else {
                infiniteRepeatable(
                    animation = tween(0, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            },
            label = "shake_x"
        )
        
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX = shakeX
                }
        ) {
            content()
        }
    }
    
    /**
     * Progress ring animation
     */
    @Composable
    fun ProgressRingAnimation(
        progress: Float,
        isAnimating: Boolean = false,
        content: @Composable () -> Unit
    ) {
        val animatedProgress by animateFloatAsState(
            targetValue = progress,
            animationSpec = tween(800, easing = EaseOutCubic),
            label = "progress"
        )
        
        val pulseScale by animateFloatAsState(
            targetValue = if (isAnimating) 1.1f else 1f,
            animationSpec = tween(400, easing = EaseInOut),
            label = "pulse_scale"
        )
        
        Box(
            modifier = Modifier
                .scale(pulseScale)
        ) {
            content()
        }
    }
    
    /**
     * Breathing animation for Timekeeper pulse
     */
    @Composable
    fun BreathingAnimation(
        isActive: Boolean = true,
        content: @Composable () -> Unit
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "breathing")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breathing_scale"
        )
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breathing_alpha"
        )
        
        Box(
            modifier = Modifier
                .scale(if (isActive) scale else 1f)
                .alpha(if (isActive) alpha else 0.1f)
        ) {
            content()
        }
    }
    
    /**
     * Interactive button with enhanced feedback
     */
    @Composable
    fun InteractiveButton(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        isPressed: Boolean = false
    ) {
        var isPressedState by remember { mutableStateOf(false) }
        
        // Reset button state after animation
        LaunchedEffect(isPressedState) {
            if (isPressedState) {
                delay(250)
                isPressedState = false
            }
        }
        
        ButtonPressAnimation(isPressed = isPressedState || isPressed) {
            TimekeeperTheme.TimekeeperButton(
                text = text,
                onClick = {
                    isPressedState = true
                    onClick()
                },
                modifier = modifier
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressedState = true
                                tryAwaitRelease()
                                isPressedState = false
                            }
                        )
                    },
                enabled = enabled
            )
        }
    }
    
    /**
     * Floating card with depth and shadow
     */
    @Composable
    fun FloatingCard(
        modifier: Modifier = Modifier,
        onClick: (() -> Unit)? = null,
        content: @Composable ColumnScope.() -> Unit
    ) {
        val elevation by animateDpAsState(
            targetValue = if (onClick != null) 12.dp else 8.dp,
            animationSpec = tween(300, easing = EaseOutCubic),
            label = "card_elevation"
        )
        
        Card(
            modifier = modifier
                .clickable { onClick?.invoke() },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
    
    /**
     * Gradient ripple effect
     */
    @Composable
    fun GradientRipple(
        isVisible: Boolean = false,
        modifier: Modifier = Modifier
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "gradient_ripple")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = if (isVisible) 3f else 0f,
            animationSpec = if (isVisible) {
                infiniteRepeatable(
                    animation = tween(1200, easing = EaseOut),
                    repeatMode = RepeatMode.Restart
                )
            } else {
                infiniteRepeatable(
                    animation = tween(0, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            },
            label = "ripple_scale"
        )
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 0f,
            animationSpec = if (isVisible) {
                infiniteRepeatable(
                    animation = tween(1200, easing = EaseOut),
                    repeatMode = RepeatMode.Restart
                )
            } else {
                infiniteRepeatable(
                    animation = tween(0, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            },
            label = "ripple_alpha"
        )
        
        if (isVisible) {
            Box(
                modifier = modifier
                    .size(100.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF5865F2).copy(alpha = alpha),
                                Color(0xFF8E44AD).copy(alpha = alpha * 0.5f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
                    .scale(scale)
            )
        }
    }
}
