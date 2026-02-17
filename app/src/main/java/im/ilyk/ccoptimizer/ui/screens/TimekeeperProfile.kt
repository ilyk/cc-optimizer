package im.ilyk.ccoptimizer.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import im.ilyk.ccoptimizer.ui.theme.TimekeeperTheme

/**
 * Timekeeper Profile/Settings Screen - Trust and transparency
 */
@Composable
fun TimekeeperProfileScreen(
    userEmail: String = "user@example.com",
    isTwoFactorEnabled: Boolean = true,
    encryptionStatus: String = "End-to-end encrypted",
    onThemeChange: (String) -> Unit = {},
    onSecuritySettings: () -> Unit = {},
    onAboutTimekeeper: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showTimekeeperModal by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Enhanced profile header with gradient background
        EnhancedProfileHeader(
            email = userEmail,
            onThemeChange = onThemeChange,
            modifier = Modifier.fillMaxWidth()
        )
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Meet The Timekeeper card
            item {
                MeetTimekeeperCard(
                    onClick = { showTimekeeperModal = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        
        // Security Section
        item {
            SettingsSection(
                title = "Security",
                items = listOf(
                    SettingsItem(
                        icon = Icons.Default.Security,
                        title = "Two-Factor Authentication",
                        subtitle = if (isTwoFactorEnabled) "Enabled" else "Disabled",
                        onClick = onSecuritySettings,
                        trailing = if (isTwoFactorEnabled) Icons.Default.Check else Icons.Default.ArrowForward
                    ),
                    SettingsItem(
                        icon = Icons.Default.Lock,
                        title = "Data Encryption",
                        subtitle = encryptionStatus,
                        onClick = { },
                        trailing = Icons.Default.Check
                    )
                )
            )
        }
        
        // Theme Customization
        item {
            ThemeCustomizationSection(
                onThemeChange = onThemeChange
            )
        }
        
        // About The Timekeeper
        item {
            AboutTimekeeperSection(
                onLearnMore = onAboutTimekeeper
            )
        }
        
        // App Info
        item {
            AppInfoSection()
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
 * Profile Header with user info
 */
@Composable
private fun ProfileHeader(
    email: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = email.first().uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // User info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Credit Card Optimizer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Settings Section with animated dividers
 */
@Composable
private fun SettingsSection(
    title: String,
    items: List<SettingsItem>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    SettingsItemRow(
                        item = item,
                        showDivider = index < items.size - 1
                    )
                }
            }
        }
    }
}

/**
 * Settings Item Row
 */
@Composable
private fun SettingsItemRow(
    item: SettingsItem,
    showDivider: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                if (item.subtitle != null) {
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Trailing icon
            Icon(
                imageVector = item.trailing,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        
        // Animated divider
        if (showDivider) {
            TimekeeperDivider()
        }
    }
}

/**
 * Theme Customization Section
 */
@Composable
private fun ThemeCustomizationSection(
    onThemeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTheme by remember { mutableStateOf("dark") }
    
    Column(
        modifier = modifier
    ) {
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                val themes = listOf(
                    "Light" to Icons.Default.LightMode,
                    "Dark" to Icons.Default.DarkMode,
                    "Auto" to Icons.Default.AutoMode
                )
                
                themes.forEach { (theme, icon) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = theme,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        
                        RadioButton(
                            selected = selectedTheme.lowercase() == theme.lowercase(),
                            onClick = { 
                                selectedTheme = theme.lowercase()
                                onThemeChange(theme.lowercase())
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * About The Timekeeper Section
 */
@Composable
private fun AboutTimekeeperSection(
    onLearnMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Timekeeper animation
    val infiniteTransition = rememberInfiniteTransition(label = "timekeeper_about")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Timekeeper symbol
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .rotate(rotation)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Meet The Timekeeper",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "We protect your data like time itself — every second matters.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = onLearnMore,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Learn More")
            }
        }
    }
}

/**
 * App Info Section
 */
@Composable
private fun AppInfoSection(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Credit Card Optimizer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Version 1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Timekeeper Divider with animation
 */
@Composable
private fun TimekeeperDivider(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "divider")
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
            .fillMaxWidth()
            .height(1.dp)
            .background(
                MaterialTheme.colorScheme.outline.copy(alpha = alpha)
            )
    )
}

/**
 * Settings Item Data Class
 */
data class SettingsItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val subtitle: String? = null,
    val onClick: () -> Unit,
    val trailing: androidx.compose.ui.graphics.vector.ImageVector
)

/**
 * Enhanced Profile Header with gradient background
 */
@Composable
private fun EnhancedProfileHeader(
    email: String,
    onThemeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                brush = TimekeeperTheme.TimekeeperGradient,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            )
            .padding(24.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    onClick = { onThemeChange("dark") }
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
                        .size(60.dp)
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
                        modifier = Modifier.size(30.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = "Timekeeper User",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

/**
 * Meet The Timekeeper Card
 */
@Composable
private fun MeetTimekeeperCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TimekeeperTheme.TimekeeperCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timekeeper symbol
            TimekeeperTheme.TimekeeperGlyph(
                modifier = Modifier.size(48.dp),
                isGlowing = true
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Meet The Timekeeper",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "The Timekeeper aligns every cycle — your financial rhythm, perfectly timed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Learn more",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Timekeeper Modal with animated introduction
 */
@Composable
private fun TimekeeperModal(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        TimekeeperTheme.TimekeeperCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            onClick = null
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated Timekeeper glyph
                TimekeeperTheme.TimekeeperGlyph(
                    modifier = Modifier.size(80.dp),
                    isGlowing = true
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "The Timekeeper",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "The Timekeeper aligns every cycle — your financial rhythm, perfectly timed. Through advanced algorithms and intuitive design, we ensure your payments flow in perfect harmony with your financial goals.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue")
                }
            }
        }
    }
}
