package im.ilyk.ccoptimizer.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import im.ilyk.ccoptimizer.ui.theme.NumericTypography
import im.ilyk.ccoptimizer.ui.theme.TimekeeperTheme
import kotlin.math.*

/**
 * Timekeeper Optimize Screen - Interactive dial for optimization control
 */
@Composable
fun TimekeeperOptimizeScreen(
    currentScore: Int = 75,
    predictedSavings: Double = 127.50,
    daysRemaining: Int = 12,
    isOptimizing: Boolean = false,
    onRecalculateFlow: () -> Unit,
    onCompareScenarios: () -> Unit,
    onToggleAutoOptimize: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var autoOptimize by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Simplified Timekeeper Ring System
        Box(
            modifier = Modifier.size(280.dp),
            contentAlignment = Alignment.Center
        ) {
            // Simple Timekeeper pulse animation
            TimekeeperTheme.TimekeeperPulse(
                modifier = Modifier.size(200.dp),
                isActive = isOptimizing
            )
            
            // Center Timekeeper Glyph
            TimekeeperTheme.TimekeeperGlyph(
                modifier = Modifier.size(64.dp),
                isGlowing = isOptimizing
            )
            
            // Score and savings overlay
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 120.dp)
            ) {
                Text(
                    text = "$currentScore%",
                    style = NumericTypography.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "$${String.format("%.2f", predictedSavings)} potential",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (daysRemaining > 0) {
                    Text(
                        text = "$daysRemaining days remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Control Buttons
        OptimizationControls(
            onRecalculateFlow = onRecalculateFlow,
            onCompareScenarios = onCompareScenarios,
            onToggleAutoOptimize = { 
                autoOptimize = !autoOptimize
                onToggleAutoOptimize(autoOptimize)
            },
            isAutoOptimizeEnabled = autoOptimize
        )
    }
}

/**
 * Timekeeper Background Animation - Semi-transparent rotating arcs
 */
@Composable
private fun TimekeeperBackgroundAnimation(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "timekeeper_bg")
    
    val rotation1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation1"
    )
    
    val rotation2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation2"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    // Get colors outside Canvas
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = minOf(size.width, size.height) * 0.4f
        
        // Outer rotating arc
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    primaryColor.copy(alpha = alpha),
                    secondaryColor.copy(alpha = alpha),
                    Color.Transparent
                )
            ),
            startAngle = rotation1,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
        
        // Inner counter-rotating arc
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    tertiaryColor.copy(alpha = alpha * 0.7f),
                    primaryColor.copy(alpha = alpha * 0.7f),
                    Color.Transparent
                )
            ),
            startAngle = rotation2,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(center.x - radius * 0.6f, center.y - radius * 0.6f),
            size = androidx.compose.ui.geometry.Size(radius * 1.2f, radius * 1.2f),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

/**
 * Interactive Optimization Dial
 */
@Composable
private fun InteractiveOptimizationDial(
    score: Int,
    savings: Double,
    daysRemaining: Int,
    isOptimizing: Boolean,
    modifier: Modifier = Modifier
) {
    var dragAngle by remember { mutableStateOf(0f) }
    
    // Optimization animation
    val infiniteTransition = rememberInfiniteTransition(label = "optimization")
    val optimizationRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "optimization_rotation"
    )
    
    // Get colors outside Canvas
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        // Outer ring - Days remaining
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val strokeWidth = 12.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)
            
            // Background circle
            drawCircle(
                color = outlineVariant,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )
            
            // Days remaining progress
            val daysProgress = (daysRemaining / 30f).coerceIn(0f, 1f)
            val sweepAngle = daysProgress * 360f
            
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
        
        // Inner ring - Optimization score
        Canvas(
            modifier = Modifier.size(200.dp)
        ) {
            val strokeWidth = 8.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)
            
            // Background circle
            drawCircle(
                color = outlineVariant,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )
            
            // Score progress
            val scoreProgress = (score / 100f).coerceIn(0f, 1f)
            val sweepAngle = scoreProgress * 360f
            
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        tertiaryColor,
                        primaryColor
                    )
                ),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            
            // Optimization animation overlay
            if (isOptimizing) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.8f),
                            Color.Transparent
                        )
                    ),
                    startAngle = optimizationRotation,
                    sweepAngle = 60f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
        
        // Center content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$score%",
                style = NumericTypography.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = "Optimized",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "$${String.format("%.2f", savings)}",
                style = NumericTypography.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Saved",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Days remaining indicator
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "$daysRemaining days",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Optimization Controls
 */
@Composable
private fun OptimizationControls(
    onRecalculateFlow: () -> Unit,
    onCompareScenarios: () -> Unit,
    onToggleAutoOptimize: () -> Unit,
    isAutoOptimizeEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main CTA Button
        Button(
            onClick = onRecalculateFlow,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Recalculate Flow",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        // Secondary Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCompareScenarios,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Compare,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Compare")
            }
            
            OutlinedButton(
                onClick = onToggleAutoOptimize,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isAutoOptimizeEnabled) 
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) 
                    else Color.Transparent
                )
            ) {
                Icon(
                    imageVector = if (isAutoOptimizeEnabled) Icons.Default.Check else Icons.Default.AutoMode,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (isAutoOptimizeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Auto",
                    color = if (isAutoOptimizeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Enhanced Timekeeper Dial with ring system and center glyph
 */
@Composable
private fun EnhancedTimekeeperDial(
    score: Int,
    savings: Double,
    daysRemaining: Int,
    isOptimizing: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Timekeeper Ring System
        TimekeeperTheme.TimekeeperRingSystem(
            modifier = Modifier.fillMaxSize(),
            isAnimating = isOptimizing,
            syncSpeed = if (isOptimizing) 3f else 1f
        )
        
        // Center Timekeeper Glyph
        TimekeeperTheme.TimekeeperGlyph(
            modifier = Modifier.size(64.dp),
            isGlowing = isOptimizing
        )
        
        // Success ripple effect
        TimekeeperTheme.TimekeeperRipple(
            modifier = Modifier.fillMaxSize(),
            isVisible = isOptimizing && score > 80
        )
        
        // Score and savings overlay
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 120.dp)
        ) {
            Text(
                text = "$score%",
                style = NumericTypography.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = "$${String.format("%.2f", savings)} potential",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (daysRemaining > 0) {
                Text(
                    text = "$daysRemaining days remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
