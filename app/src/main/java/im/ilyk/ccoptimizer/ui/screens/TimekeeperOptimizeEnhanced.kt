package im.ilyk.ccoptimizer.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import im.ilyk.ccoptimizer.ui.components.TimekeeperAnimations
import im.ilyk.ccoptimizer.ui.components.rememberTimekeeperHapticFeedback
import im.ilyk.ccoptimizer.ui.components.rememberTimekeeperSoundFeedback
import im.ilyk.ccoptimizer.ui.theme.TimekeeperTheme
import kotlinx.coroutines.delay

/**
 * Enhanced Optimize screen with Timekeeper ring system and interactions
 */
@Composable
fun TimekeeperOptimizeEnhanced(
    onRecalculateClick: () -> Unit,
    onCompareClick: () -> Unit,
    onAutoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isCalculating by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var showRipple by remember { mutableStateOf(false) }
    
    // Haptic feedback
    val hapticFeedback = rememberTimekeeperHapticFeedback()
    val soundFeedback = rememberTimekeeperSoundFeedback()
    
    // Handle calculation simulation
    LaunchedEffect(isCalculating) {
        if (isCalculating) {
            delay(2000)
            isCalculating = false
            isSuccess = true
            delay(1000)
            isSuccess = false
            showRipple = false
        }
    }
    
    // Enhanced background with darker contrast
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F7)) // Darker background as specified
    ) {
        // Timekeeper background with enhanced animation
        TimekeeperTheme.TimekeeperBackground(
            modifier = Modifier.fillMaxSize(),
            isAnimating = isCalculating
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Timekeeper Ring System
            TimekeeperRingSystem(
                isCalculating = isCalculating,
                isSuccess = isSuccess,
                showRipple = showRipple,
                modifier = Modifier.size(300.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Enhanced action buttons
            TimekeeperActionButtons(
                onRecalculateClick = {
                    isCalculating = true
                    showRipple = true
                    hapticFeedback(im.ilyk.ccoptimizer.ui.components.TimekeeperHapticType.MEDIUM_TAP)
                    soundFeedback(im.ilyk.ccoptimizer.ui.components.TimekeeperSoundType.SUCCESS_CHIME)
                    onRecalculateClick()
                },
                onCompareClick = onCompareClick,
                onAutoClick = onAutoClick,
                isCalculating = isCalculating
            )
        }
    }
}

/**
 * Timekeeper Ring System with three concentric arcs
 */
@Composable
private fun TimekeeperRingSystem(
    isCalculating: Boolean,
    isSuccess: Boolean,
    showRipple: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "timekeeper_rings")
    
    // Different rotation speeds for each ring
    val ring1Rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isCalculating) 3000 else 15000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1_rotation"
    )
    
    val ring2Rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isCalculating) 2000 else 12000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2_rotation"
    )
    
    val ring3Rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isCalculating) 4000 else 18000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring3_rotation"
    )
    
    // Timekeeper glyph glow
    val glyphGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isCalculating || isSuccess) 1f else 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glyph_glow"
    )
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = minOf(size.width, size.height) / 2 - 20.dp.toPx()
            
            // Draw three concentric rings
            drawTimekeeperRings(
                center = center,
                maxRadius = maxRadius,
                ring1Rotation = ring1Rotation,
                ring2Rotation = ring2Rotation,
                ring3Rotation = ring3Rotation,
                isCalculating = isCalculating,
                isSuccess = isSuccess
            )
            
            // Draw Timekeeper glyph in center
            drawTimekeeperGlyph(
                center = center,
                glowIntensity = glyphGlow,
                isSuccess = isSuccess
            )
        }
        
        // Ripple effect for success
        TimekeeperAnimations.GradientRipple(
            isVisible = showRipple,
            modifier = Modifier.size(200.dp)
        )
    }
}

/**
 * Draws the Timekeeper rings with different speeds and colors
 */
private fun DrawScope.drawTimekeeperRings(
    center: Offset,
    maxRadius: Float,
    ring1Rotation: Float,
    ring2Rotation: Float,
    ring3Rotation: Float,
    isCalculating: Boolean,
    isSuccess: Boolean
) {
    val strokeWidth = 4.dp.toPx()
    
        // Ring 1 - Outer ring
        drawContext.canvas.rotate(ring1Rotation, center.x, center.y)
        drawArc(
            color = if (isSuccess) Color(0xFF4CAF50) else Color(0xFF5865F2),
            startAngle = 0f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = Offset(
                center.x - maxRadius,
                center.y - maxRadius
            ),
            size = androidx.compose.ui.geometry.Size(maxRadius * 2, maxRadius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    
    // Ring 2 - Middle ring
    val middleRadius = maxRadius * 0.7f
    drawContext.canvas.rotate(ring2Rotation, center.x, center.y)
    drawArc(
        color = if (isSuccess) Color(0xFF4CAF50) else Color(0xFF8E44AD),
        startAngle = 45f,
        sweepAngle = 270f,
        useCenter = false,
        topLeft = Offset(
            center.x - middleRadius,
            center.y - middleRadius
        ),
        size = androidx.compose.ui.geometry.Size(middleRadius * 2, middleRadius * 2),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
    
    // Ring 3 - Inner ring
    val innerRadius = maxRadius * 0.4f
    drawContext.canvas.rotate(ring3Rotation, center.x, center.y)
    drawArc(
        color = if (isSuccess) Color(0xFF4CAF50) else Color(0xFF273C75),
        startAngle = 90f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(
            center.x - innerRadius,
            center.y - innerRadius
        ),
        size = androidx.compose.ui.geometry.Size(innerRadius * 2, innerRadius * 2),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}

/**
 * Draws the Timekeeper glyph with glow effect
 */
private fun DrawScope.drawTimekeeperGlyph(
    center: Offset,
    glowIntensity: Float,
    isSuccess: Boolean
) {
    val glyphSize = 24.dp.toPx()
    val glowColor = if (isSuccess) Color(0xFF4CAF50) else Color(0xFF5865F2)
    
    // Glow effect
    drawCircle(
        color = glowColor.copy(alpha = glowIntensity * 0.3f),
        radius = glyphSize * 2,
        center = center,
        blendMode = BlendMode.Screen
    )
    
    // Main glyph (simplified Timekeeper symbol)
    drawCircle(
        color = glowColor.copy(alpha = glowIntensity),
        radius = glyphSize,
        center = center
    )
    
    // Inner details
    drawCircle(
        color = Color.White.copy(alpha = glowIntensity),
        radius = glyphSize * 0.3f,
        center = center
    )
}

/**
 * Enhanced action buttons with gradient styling and hover states
 */
@Composable
private fun TimekeeperActionButtons(
    onRecalculateClick: () -> Unit,
    onCompareClick: () -> Unit,
    onAutoClick: () -> Unit,
    isCalculating: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main recalculate button with enhanced styling
        TimekeeperAnimations.InteractiveButton(
            text = if (isCalculating) "Syncing your rhythm..." else "Recalculate Flow",
            onClick = onRecalculateClick,
            enabled = !isCalculating,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Secondary action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TimekeeperAnimations.InteractiveButton(
                text = "Compare",
                onClick = onCompareClick,
                modifier = Modifier.weight(1f)
            )
            
            TimekeeperAnimations.InteractiveButton(
                text = "Auto",
                onClick = onAutoClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
