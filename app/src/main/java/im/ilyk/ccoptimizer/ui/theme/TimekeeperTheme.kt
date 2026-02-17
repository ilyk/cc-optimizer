package im.ilyk.ccoptimizer.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Enhanced Timekeeper theme with gradient overlays, background arcs, and visual depth
 */
object TimekeeperTheme {
    
    // Timekeeper Brand Gradient: #273C75 → #5865F2 → #8E44AD
    val TimekeeperGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF273C75),
            Color(0xFF5865F2),
            Color(0xFF8E44AD)
        )
    )
    
    // Subtle gradient for overlays
    val TimekeeperOverlay = Brush.radialGradient(
        colors = listOf(
            Color(0x1A5865F2),
            Color(0x0A273C75),
            Color.Transparent
        )
    )
    
    // Ambient glow colors
    val AmbientGlow = Color(0x1A5865F2)
    val FocusGlow = Color(0x2A5865F2)
    
    /**
     * Creates a Timekeeper background with animated arcs and enhanced depth
     */
    @Composable
    fun TimekeeperBackground(
        modifier: Modifier = Modifier,
        isAnimating: Boolean = false,
        showArcs: Boolean = true,
        content: @Composable () -> Unit = {}
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            // Base gradient overlay with enhanced depth
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TimekeeperOverlay)
            )
            
            // Enhanced background with temporal depth
            if (showArcs) {
                TimekeeperBackgroundArcs(
                    modifier = Modifier.fillMaxSize(),
                    isAnimating = isAnimating
                )
            }
            
            // Subtle breathing animation overlay
            TimekeeperBreathingOverlay(
                modifier = Modifier.fillMaxSize(),
                isAnimating = isAnimating
            )
            
            // Content
            content()
        }
    }
    
    /**
     * Animated background arcs that slowly rotate
     */
    @Composable
    private fun TimekeeperBackgroundArcs(
        modifier: Modifier = Modifier,
        isAnimating: Boolean = false
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "background_arcs")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(60000, easing = LinearEasing), // 1 minute rotation
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )
        
        androidx.compose.foundation.Canvas(modifier = modifier) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = minOf(size.width, size.height) * 0.3f
            
            // Draw subtle arcs
            drawTimekeeperArcs(
                center = center,
                radius = radius,
                rotation = rotation,
                isAnimating = isAnimating
            )
        }
    }
    
    /**
     * Draws the Timekeeper arcs with breathing animation
     */
    private fun DrawScope.drawTimekeeperArcs(
        center: Offset,
        radius: Float,
        rotation: Float,
        isAnimating: Boolean
    ) {
        val strokeWidth = 2.dp.toPx()
        val alpha = if (isAnimating) 0.3f else 0.1f
        
        // Three concentric arcs
        for (i in 0..2) {
            val arcRadius = radius + (i * 40.dp.toPx())
            val arcAlpha = alpha * (1f - i * 0.2f)
            
            rotate(rotation + (i * 30f)) {
                drawArc(
                    color = Color(0xFF5865F2).copy(alpha = arcAlpha),
                    startAngle = 0f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(
                        center.x - arcRadius,
                        center.y - arcRadius
                    ),
                    size = androidx.compose.ui.geometry.Size(arcRadius * 2, arcRadius * 2),
                    style = Stroke(width = strokeWidth)
                )
            }
        }
    }
    
    /**
     * Creates a Timekeeper glow effect
     */
    @Composable
    fun TimekeeperGlow(
        modifier: Modifier = Modifier,
        intensity: Float = 1f,
        color: Color = Color(0xFF5865F2)
    ) {
        Box(
            modifier = modifier
                .blur(8.dp)
                .background(
                    color.copy(alpha = 0.1f * intensity),
                    shape = RoundedCornerShape(16.dp)
                )
        )
    }
    
    /**
     * Enhanced card with Timekeeper styling
     */
    @Composable
    fun TimekeeperCard(
        modifier: Modifier = Modifier,
        onClick: (() -> Unit)? = null,
        content: @Composable ColumnScope.() -> Unit
    ) {
        Card(
            modifier = modifier
                .clickable { onClick?.invoke() }
                .drawWithContent {
                    drawContent()
                    // Add subtle glow
                    drawRect(
                        brush = TimekeeperOverlay,
                        blendMode = BlendMode.Screen
                    )
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
    
    /**
     * Timekeeper pulse animation
     */
    @Composable
    fun TimekeeperPulse(
        modifier: Modifier = Modifier,
        isActive: Boolean = false
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "timekeeper_pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
        
        Box(
            modifier = modifier
                .size(24.dp)
                .background(
                    Color(0xFF5865F2).copy(alpha = if (isActive) alpha else 0.1f),
                    CircleShape
                )
                .graphicsLayer {
                    scaleX = if (isActive) scale else 1f
                    scaleY = if (isActive) scale else 1f
                }
        )
    }
    
    /**
     * Gradient button with Timekeeper styling
     */
    @Composable
    fun TimekeeperButton(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true
    ) {
        Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            contentPadding = PaddingValues(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        brush = if (enabled) TimekeeperGradient else Brush.linearGradient(
                            listOf(Color.Gray, Color.Gray)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = text,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
    
    /**
     * Breathing overlay for subtle ambient animation
     */
    @Composable
    private fun TimekeeperBreathingOverlay(
        modifier: Modifier = Modifier,
        isAnimating: Boolean = false
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "timekeeper_breathing")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.7f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breathing_alpha"
        )
        
        if (isAnimating) {
            Box(
                modifier = modifier
                    .background(
                        Color(0xFF5865F2).copy(alpha = alpha * 0.05f)
                    )
            )
        }
    }
    
    /**
     * Ripple effect for success states
     */
    @Composable
    fun TimekeeperRipple(
        modifier: Modifier = Modifier,
        isVisible: Boolean = false
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "timekeeper_ripple")
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
            label = "scale"
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
            label = "alpha"
        )
        
        if (isVisible) {
            Box(
                modifier = modifier
                    .size(100.dp)
                    .background(
                        Color(0xFF5865F2).copy(alpha = alpha),
                        CircleShape
                    )
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            )
        }
    }
    
    /**
     * Timekeeper ring system for optimize screen
     */
    @Composable
    fun TimekeeperRingSystem(
        modifier: Modifier = Modifier,
        isAnimating: Boolean = false,
        syncSpeed: Float = 1f
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "timekeeper_rings")
        val rotation1 by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween((8000 / maxOf(syncSpeed, 0.1f)).toInt(), easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation1"
        )
        val rotation2 by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -360f,
            animationSpec = infiniteRepeatable(
                animation = tween((6000 / maxOf(syncSpeed, 0.1f)).toInt(), easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation2"
        )
        val rotation3 by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween((10000 / maxOf(syncSpeed, 0.1f)).toInt(), easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation3"
        )
        
        androidx.compose.foundation.Canvas(modifier = modifier) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = minOf(size.width, size.height) * 0.4f
            
            // Three concentric rings with different speeds
            val ringRadii = listOf(radius * 0.7f, radius * 0.85f, radius * 1.0f)
            val rotations = listOf(rotation1, rotation2, rotation3)
            val strokeWidths = listOf(3.dp.toPx(), 2.dp.toPx(), 1.dp.toPx())
            
            ringRadii.forEachIndexed { index, ringRadius ->
                rotate(rotations[index]) {
                    drawArc(
                        color = Color(0xFF5865F2).copy(alpha = 0.3f - index * 0.1f),
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(
                            center.x - ringRadius,
                            center.y - ringRadius
                        ),
                        size = androidx.compose.ui.geometry.Size(ringRadius * 2, ringRadius * 2),
                        style = Stroke(width = strokeWidths[index])
                    )
                }
            }
        }
    }
    
    /**
     * Timekeeper center glyph with glow effect
     */
    @Composable
    fun TimekeeperGlyph(
        modifier: Modifier = Modifier,
        isGlowing: Boolean = false
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "timekeeper_glyph")
        val glowIntensity by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow_intensity"
        )
        
        Box(
            modifier = modifier
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF5865F2).copy(alpha = if (isGlowing) glowIntensity else 0.3f),
                            Color(0xFF8E44AD).copy(alpha = if (isGlowing) glowIntensity * 0.7f else 0.2f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        ) {
            // Timekeeper symbol - simplified geometric representation
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.minDimension * 0.3f
                
                // Draw timekeeper symbol
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = radius,
                    center = center
                )
                
                // Inner lines
                drawLine(
                    color = Color.White,
                    start = Offset(center.x - radius * 0.5f, center.y),
                    end = Offset(center.x + radius * 0.5f, center.y),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = Color.White,
                    start = Offset(center.x, center.y - radius * 0.5f),
                    end = Offset(center.x, center.y + radius * 0.5f),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}
