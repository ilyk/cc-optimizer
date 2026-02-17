package im.ilyk.ccoptimizer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import im.ilyk.ccoptimizer.data.models.CreditCard
import im.ilyk.ccoptimizer.data.models.PaymentRecommendation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableCard(
    card: CreditCard,
    recommendation: PaymentRecommendation? = null,
    onDelete: (CreditCard) -> Unit,
    onEdit: (CreditCard) -> Unit,
    onInfo: (CreditCard) -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableStateOf(0f) }
    var isHorizontalSwipe by remember { mutableStateOf(false) }
    var totalDragDistance by remember { mutableStateOf(0f) }
    var swipeDetectionEnabled by remember { mutableStateOf(false) }
    var dragStartTime by remember { mutableStateOf(0L) }
    var previousOffsetState by remember { mutableStateOf(0f) } // Track previous resting position
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val view = LocalView.current
    var hasProvidedHaptic by remember { mutableStateOf(false) }
    
    // Get card width for calculating button sizes
    var cardWidthPx by remember { mutableStateOf(0f) }
    
    // Animation for smooth reset
    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = tween(durationMillis = 300),
        label = "cardOffset"
    )
    
    // Dynamic thresholds based on card width (1/5 of card width per button)
    val buttonWidthPx = if (cardWidthPx > 0) cardWidthPx / 5 else with(density) { 80.dp.toPx() }
    val buttonWidthDp = with(density) { buttonWidthPx.toDp() }
    val leftSwipeLimit = -buttonWidthPx // Left swipe: reveal 1 button (delete)
    val rightSwipeLimit = buttonWidthPx * 2 // Right swipe: reveal 2 buttons (info + edit)
    val swipeDelayMs = 100L // Delay before enabling swipe detection (< 150ms requirement)
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .semantics {
                contentDescription = "${card.bankName} credit card ending in ${card.lastFourDigits}, " +
                    "balance ${String.format("%.2f", card.currentBalance)} dollars. " +
                    "Swipe left to delete, swipe right for info and edit options."
                // Custom actions for accessibility
                customActions = listOf(
                    CustomAccessibilityAction("Delete card") {
                        onDelete(card)
                        true
                    },
                    CustomAccessibilityAction("Edit card") {
                        onEdit(card)
                        true
                    },
                    CustomAccessibilityAction("View card info") {
                        onInfo(card)
                        true
                    }
                )
            }
    ) {
        // Black background with rounded corners (8dp grid: 2x8=16dp)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
        )
        
        // Info button (blue) - leftmost, revealed on right swipe
        // All corners rounded (16dp) - right corners hidden under card
        Box(
            modifier = Modifier
                .width(buttonWidthDp + 30.dp)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF2196F3))
                .semantics(mergeDescendants = true) {
                    contentDescription = "View information for ${card.bankName} card ending in ${card.lastFourDigits}"
                    role = Role.Button
                }
                .pointerInput(Unit) {
                    detectTapGestures {
                        onInfo(card)
                        offsetX = 0f
                        previousOffsetState = 0f
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null, // Described by parent
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "INFO",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // Edit button (green) - second from left, revealed on right swipe
        // All corners rounded (16dp) - right corners hidden under card
        Box(
            modifier = Modifier
                .width(buttonWidthDp + 30.dp)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .offset(x = buttonWidthDp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF4CAF50))
                .semantics(mergeDescendants = true) {
                    contentDescription = "Edit ${card.bankName} card ending in ${card.lastFourDigits}"
                    role = Role.Button
                }
                .pointerInput(Unit) {
                    detectTapGestures {
                        onEdit(card)
                        offsetX = 0f
                        previousOffsetState = 0f
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null, // Described by parent
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "EDIT",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // Delete button (red) - rightmost, revealed on left swipe
        // All corners rounded (16dp) - left corners hidden under card
        Box(
            modifier = Modifier
                .width(buttonWidthDp + 30.dp)
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFEF5350))
                .semantics(mergeDescendants = true) {
                    contentDescription = "Delete ${card.bankName} card ending in ${card.lastFourDigits}"
                    role = Role.Button
                }
                .pointerInput(Unit) {
                    detectTapGestures {
                        onDelete(card)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null, // Described by parent
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "DELETE",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // Main card
        CreditCardDesign(
            card = card,
            recommendation = recommendation,
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = with(density) { animatedOffset.toDp() })
                .clip(RoundedCornerShape(16.dp))  // 8dp grid: 2x8=16dp
                .onSizeChanged { size ->
                    cardWidthPx = size.width.toFloat()
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            // Wait for initial down event
                            val down = awaitFirstDown(requireUnconsumed = false)
                            
                            // Reset swipe detection state
                            isHorizontalSwipe = false
                            var isVerticalScroll = false
                            totalDragDistance = 0f
                            swipeDetectionEnabled = false
                            dragStartTime = System.currentTimeMillis()
                            hasProvidedHaptic = false
                            
                            val startPosition = down.position
                            var lastPosition = startPosition
                            
                            // Track pointer movements
                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.first()
                                
                                if (change.pressed) {
                                    val currentPosition = change.position
                                    val totalDelta = currentPosition - startPosition
                                    val incrementalDelta = currentPosition - lastPosition
                                    
                                    // Check if enough time has passed
                                    val elapsedTime = System.currentTimeMillis() - dragStartTime
                                    if (elapsedTime > swipeDelayMs) {
                                        swipeDetectionEnabled = true
                                    }
                                    
                                    // Determine gesture direction once
                                    if (swipeDetectionEnabled && !isHorizontalSwipe && !isVerticalScroll) {
                                        val horizontalDistance = kotlin.math.abs(totalDelta.x)
                                        val verticalDistance = kotlin.math.abs(totalDelta.y)
                                        
                                        // Need significant movement to determine direction
                                        if (horizontalDistance > 20f || verticalDistance > 20f) {
                                            if (horizontalDistance > verticalDistance * 1.5f) {
                                                // Horizontal swipe detected - provide haptic feedback
                                                isHorizontalSwipe = true
                                                if (!hasProvidedHaptic) {
                                                    view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                                                    hasProvidedHaptic = true
                                                }
                                            } else {
                                                // Vertical scroll detected - disable swipe for this gesture
                                                isVerticalScroll = true
                                            }
                                        }
                                    }
                                    
                                    // Only update offset if it's a horizontal swipe (not vertical scroll)
                                    if (isHorizontalSwipe && !isVerticalScroll) {
                                        // Follow finger directly, but constrain based on starting position
                                        val newOffset = previousOffsetState + totalDelta.x
                                        
                                        // Prevent sliding from left to right without returning to center
                                        val constrainedOffset = when {
                                            // Starting from left (showing delete)
                                            previousOffsetState < -5f -> {
                                                // Can only move right (toward center or beyond)
                                                newOffset.coerceIn(leftSwipeLimit, 0f)
                                            }
                                            // Starting from right (showing info/edit)
                                            previousOffsetState > 5f -> {
                                                // Can only move left (toward center or beyond)
                                                newOffset.coerceIn(0f, rightSwipeLimit)
                                            }
                                            // Starting from center
                                            else -> {
                                                // Can move either direction
                                                newOffset.coerceIn(leftSwipeLimit, rightSwipeLimit)
                                            }
                                        }
                                        
                                        offsetX = constrainedOffset
                                        change.consume()
                                    }
                                    
                                    lastPosition = currentPosition
                                }
                            } while (change.pressed)
                            
                            // Handle release - snap to show buttons or reset
                            if (isHorizontalSwipe) {
                                // Determine if we should snap to show buttons or reset
                                val threshold = buttonWidthPx / 2 // Need to drag at least half a button width
                                
                                when {
                                    offsetX < -threshold -> {
                                        // Snap to show delete button - haptic feedback
                                        view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                                        offsetX = leftSwipeLimit
                                        previousOffsetState = leftSwipeLimit
                                    }
                                    offsetX > threshold -> {
                                        // Snap to show info/edit buttons - haptic feedback
                                        view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                                        offsetX = rightSwipeLimit
                                        previousOffsetState = rightSwipeLimit
                                    }
                                    else -> {
                                        // Reset to center
                                        offsetX = 0f
                                        previousOffsetState = 0f
                                    }
                                }
                            } else {
                                // Reset if this was vertical scrolling
                                offsetX = 0f
                                previousOffsetState = 0f
                            }
                        }
                    }
                }
        )
    }
}
