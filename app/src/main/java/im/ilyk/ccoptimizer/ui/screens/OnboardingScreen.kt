package im.ilyk.ccoptimizer.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Onboarding flow with 5 screens:
 * 1. Problem - "Juggling Multiple Credit Cards?"
 * 2. Secret Timing - The statement close vs due date secret
 * 3. Opportunity - How proper timing can help
 * 4. Calendar - Visualize your payment windows
 * 5. Privacy - Data security reassurance + CTA
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPage by remember { mutableStateOf(0) }
    val totalPages = 5
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Page content with animation
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                slideInHorizontally { width -> width } + fadeIn() togetherWith
                slideOutHorizontally { width -> -width } + fadeOut()
            },
            label = "onboarding_page"
        ) { page ->
            when (page) {
                0 -> ProblemPage()
                1 -> SecretTimingPage()
                2 -> OpportunityPage()
                3 -> CalendarPage()
                4 -> PrivacyPage()
            }
        }
        
        // Bottom navigation
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Page indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(totalPages) { index ->
                    PageIndicator(isActive = index == currentPage)
                }
            }
            
            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentPage > 0) {
                    TextButton(onClick = { currentPage-- }) {
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                
                Button(
                    onClick = {
                        if (currentPage < totalPages - 1) {
                            currentPage++
                        } else {
                            onComplete()
                        }
                    },
                    modifier = Modifier.widthIn(min = 160.dp).heightIn(min = 48.dp)
                ) {
                    Text(
                        text = when {
                            currentPage < totalPages - 1 -> "Next"
                            else -> "Connect Your Cards"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Skip button
            if (currentPage < totalPages - 1) {
                TextButton(onClick = onComplete) {
                    Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun PageIndicator(isActive: Boolean) {
    Box(
        modifier = Modifier
            .size(if (isActive) 24.dp else 8.dp, 8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
    )
}

@Composable
private fun ProblemPage() {
    OnboardingPageLayout(
        icon = Icons.Default.CreditCard,
        title = "Juggling Multiple Credit Cards?",
        description = "Managing payment dates for multiple cards is overwhelming. Miss a payment and your credit score suffers.",
        iconColor = Color(0xFFFF6B6B)
    ) {
        // Animated stressed person illustration
        AnimatedStressIllustration()
    }
}

@Composable
private fun OpportunityPage() {
    OnboardingPageLayout(
        icon = Icons.Default.TrendingUp,
        title = "There's a Better Way",
        description = "Optimize your credit utilization by paying at the right time. It's not just about paying on time—it's about strategic timing.",
        iconColor = Color(0xFF4ECDC4)
    ) {
        // Animated growth chart
        AnimatedGrowthChart()
    }
}

@Composable
private fun SecretTimingPage() {
    OnboardingPageLayout(
        icon = Icons.Default.Schedule,
        title = "The Secret: Statement Close Date",
        description = "Most people only know the due date. The real secret? Pay before your statement closes to minimize reported utilization.",
        iconColor = Color(0xFFFFA726)
    ) {
        // Animated timeline showing close vs due
        AnimatedTimeline()
    }
}

@Composable
private fun CalendarPage() {
    OnboardingPageLayout(
        icon = Icons.Default.CalendarMonth,
        title = "Visualize Your Payment Windows",
        description = "See all your cards' close and due dates in one calendar. Never miss the optimal payment window again.",
        iconColor = Color(0xFF7C4DFF)
    ) {
        // Simple calendar grid - no animations, just visible content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2A2A2A), RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Week 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(7) { col ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(Color(0xFF444444), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFF666666), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (col == 2) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFFFF9800), CircleShape)
                            )
                        } else if (col == 5) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF4CAF50), RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
            }
            
            // Week 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(7) { col ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(Color(0xFF444444), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFF666666), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (col == 1) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFFD32F2F), CircleShape)
                            )
                        } else if (col == 4) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFFFF9800), CircleShape)
                            )
                        }
                    }
                }
            }
            
            // Week 3
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(7) { col ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(Color(0xFF444444), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFF666666), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (col == 3) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF4CAF50), RoundedCornerShape(2.dp))
                            )
                        } else if (col == 6) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFFD32F2F), CircleShape)
                            )
                        }
                    }
                }
            }
            
            // Week 4
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(7) { col ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(Color(0xFF444444), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFF666666), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (col == 0) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFFFF9800), CircleShape)
                            )
                        } else if (col == 5) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF4CAF50), RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyPage() {
    OnboardingPageLayout(
        icon = Icons.Default.Lock,
        title = "Your Data, Your Control",
        description = "Bank-level encryption. No data sharing. Optional self-hosting. Your financial information stays private and secure.",
        iconColor = Color(0xFF26A69A)
    ) {
        // Privacy badges
        PrivacyBadges()
    }
}

@Composable
private fun OnboardingPageLayout(
    icon: ImageVector,
    title: String,
    description: String,
    iconColor: Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 180.dp), // Bottom padding for nav
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        // Icon with animation
        var iconVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(100)
            iconVisible = true
        }
        
        AnimatedVisibility(
            visible = iconVisible,
            enter = scaleIn() + fadeIn()
        ) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.1f),
                modifier = Modifier.size(100.dp) // Reduced from 120dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(56.dp) // Reduced from 64dp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Title with animation
        var textVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(300)
            textVisible = true
        }
        
        AnimatedVisibility(
            visible = textVisible,
            enter = slideInVertically { it / 2 } + fadeIn()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 24.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Custom content
        content()
        
        // Push content up to prevent bottom nav overlap
        Spacer(modifier = Modifier.weight(1f, fill = false))
    }
}

@Composable
private fun AnimatedStressIllustration() {
    // Simple animated illustration of multiple cards
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        repeat(3) { index ->
            var cardVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(500L + index * 200L)
                cardVisible = true
            }
            
            AnimatedVisibility(
                visible = cardVisible,
                enter = slideInVertically { it } + fadeIn()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = listOf(
                        Color(0xFFEF5350),
                        Color(0xFFEC407A),
                        Color(0xFFAB47BC)
                    )[index],
                    modifier = Modifier
                        .width(100.dp)
                        .height(60.dp)
                ) {}
            }
        }
    }
}

@Composable
private fun AnimatedGrowthChart() {
    // Animated bar chart showing improvement
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(horizontal = 32.dp)
    ) {
        listOf(0.4f, 0.6f, 0.8f, 1f).forEachIndexed { index, height ->
            var barVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(500L + index * 200L)
                barVisible = true
            }
            
            val animatedHeight by animateFloatAsState(
                targetValue = if (barVisible) height else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "bar_height"
            )
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(animatedHeight)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun AnimatedTimeline() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        TimelineItem(
            label = "Statement Close",
            sublabel = "Pay before this date",
            color = Color(0xFFFF9800),
            delay = 500L
        )
        TimelineItem(
            label = "Optimal Window",
            sublabel = "Utilization reported",
            color = Color(0xFF4CAF50),
            delay = 700L
        )
        TimelineItem(
            label = "Payment Due",
            sublabel = "Avoid late fees",
            color = Color(0xFFD32F2F),
            delay = 900L
        )
    }
}

@Composable
private fun TimelineItem(
    label: String,
    sublabel: String,
    color: Color,
    delay: Long
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delay)
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally { -it } + fadeIn()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = sublabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AnimatedCalendarPreview() {
    // Simple, highly visible calendar grid
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                Color(0xFF1A1A1A), // Dark background to make it visible
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Calendar event patterns: close (orange), pay (green), due (red)
            val eventPattern = listOf(
                null, null, "close", null, null, "pay", null,  // Week 1
                null, "due", null, null, "close", null, null,   // Week 2
                null, null, null, "pay", null, null, "due",     // Week 3
                "close", null, null, null, null, "pay", null    // Week 4
            )
            
            repeat(4) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(7) { col ->
                        val index = row * 7 + col
                        val eventType = if (index < eventPattern.size) eventPattern[index] else null
                        
                        var cellVisible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            delay(400L + index * 25L)
                            cellVisible = true
                        }
                        
                        AnimatedVisibility(
                            visible = cellVisible,
                            enter = scaleIn(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200))
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF333333)) // Dark gray cells
                                    .border(
                                        width = 1.dp,
                                        color = Color(0xFF666666), // Light gray borders
                                        shape = RoundedCornerShape(6.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                // Event indicator dot - make them bigger and more visible
                                when (eventType) {
                                    "close" -> Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF9800)) // Orange
                                    )
                                    "pay" -> Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color(0xFF4CAF50)) // Green
                                    )
                                    "due" -> Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFD32F2F)) // Red
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyBadges() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        listOf(
            "🔒 Bank-Level Encryption" to 500L,
            "🚫 No Data Sharing" to 700L,
            "🏠 Self-Hosting Option" to 900L,
            "✅ GDPR Compliant" to 1100L
        ).forEach { (badge, delay) ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(delay)
                visible = true
            }
            
            AnimatedVisibility(
                visible = visible,
                enter = slideInHorizontally { it } + fadeIn()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = badge,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

