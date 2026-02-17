package im.ilyk.ccoptimizer.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import im.ilyk.ccoptimizer.ui.components.TimekeeperAnimations
import im.ilyk.ccoptimizer.ui.components.TimekeeperSymbolAnimation
import im.ilyk.ccoptimizer.ui.theme.TimekeeperTheme
import kotlinx.coroutines.delay

/**
 * Enhanced Profile screen with gradient header and Timekeeper modal
 */
@Composable
fun TimekeeperProfileEnhanced(
    onThemeToggle: () -> Unit,
    onSecurityClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showTimekeeperModal by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Enhanced profile header with gradient
        TimekeeperProfileHeader(
            onThemeToggle = onThemeToggle,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Profile content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Meet The Timekeeper card
                TimekeeperMeetCard(
                    onClick = { showTimekeeperModal = true }
                )
            }
            
            item {
                // Settings section
                TimekeeperSettingsSection(
                    onSecurityClick = onSecurityClick,
                    onNotificationsClick = onNotificationsClick
                )
            }
        }
    }
    
    // Timekeeper modal
    if (showTimekeeperModal) {
        TimekeeperModal(
            onDismiss = { showTimekeeperModal = false }
        )
    }
}

/**
 * Enhanced profile header with gradient background
 */
@Composable
private fun TimekeeperProfileHeader(
    onThemeToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isGlowAnimating by remember { mutableStateOf(false) }
    
    // Trigger glow animation
    LaunchedEffect(Unit) {
        isGlowAnimating = true
    }
    
    Box(
        modifier = modifier
            .height(200.dp)
            .background(
                TimekeeperTheme.TimekeeperGradient,
                RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            )
    ) {
        // Glow animation overlay
        if (isGlowAnimating) {
            TimekeeperTheme.TimekeeperGlow(
                modifier = Modifier.fillMaxSize(),
                intensity = 0.3f,
                color = Color(0xFF5865F2)
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top row with theme toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                IconButton(
                    onClick = onThemeToggle
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Toggle theme",
                        tint = Color.White
                    )
                }
            }
            
            // Profile info
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile avatar with Timekeeper styling
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = "Your Financial Rhythm",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    
                    Text(
                        text = "Perfectly timed, always in sync",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

/**
 * Meet The Timekeeper card with animation
 */
@Composable
private fun TimekeeperMeetCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TimekeeperAnimations.FloatingCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timekeeper symbol with animation
            TimekeeperSymbolAnimated(
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Meet The Timekeeper",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = "Discover the intelligence behind your financial rhythm",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Learn more",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Animated Timekeeper symbol
 */
@Composable
private fun TimekeeperSymbolAnimated(
    modifier: Modifier = Modifier
) {
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
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
        androidx.compose.foundation.Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = minOf(size.width, size.height) / 2 - 4.dp.toPx()
        
        // Rotating outer ring
        drawContext.canvas.rotate(rotation, center.x, center.y)
        drawCircle(
            color = Color(0xFF5865F2),
            radius = radius,
            center = center,
            style = Stroke(width = 3.dp.toPx())
        )
        
        // Pulsing center
        drawCircle(
            color = Color(0xFF8E44AD),
            radius = radius * 0.3f * scale,
            center = center
        )
    }
}

/**
 * Settings section with enhanced styling
 */
@Composable
private fun TimekeeperSettingsSection(
    onSecurityClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        TimekeeperSettingItem(
            icon = Icons.Default.Security,
            title = "Security",
            subtitle = "Manage your account security",
            onClick = onSecurityClick
        )
        
        TimekeeperSettingItem(
            icon = Icons.Default.Notifications,
            title = "Notifications",
            subtitle = "Control your rhythm alerts",
            onClick = onNotificationsClick
        )
        
        TimekeeperSettingItem(
            icon = Icons.Default.Palette,
            title = "Theme",
            subtitle = "Customize your experience",
            onClick = { /* Theme toggle handled in header */ }
        )
    }
}

/**
 * Enhanced setting item with micro-transitions
 */
@Composable
private fun TimekeeperSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    
    // Reset button state after animation
    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(100)
            isPressed = false
        }
    }
    
    TimekeeperAnimations.ButtonPressAnimation(isPressed = isPressed) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clickable {
                    isPressed = true
                    onClick()
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Timekeeper modal with Lottie animation
 */
@Composable
private fun TimekeeperModal(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        TimekeeperTheme.TimekeeperCard(
            modifier = modifier.width(320.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Timekeeper Lottie animation
                TimekeeperSymbolAnimation(
                    isActive = true,
                    size = 120,
                    modifier = Modifier.size(120.dp)
                )
                
                Text(
                    text = "The Timekeeper",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "The Timekeeper aligns every cycle — your financial rhythm, perfectly timed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TimekeeperTheme.TimekeeperButton(
                    text = "Understand",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
