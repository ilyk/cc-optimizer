package im.ilyk.ccoptimizer.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import im.ilyk.ccoptimizer.ui.theme.TimekeeperTheme

/**
 * Timekeeper Micro-Animation Framework
 * Implements the timing and feedback system specified in Phase 2
 */
object TimekeeperMicroInteractions {
    
    // Animation timing constants from Phase 2 document
    private const val TAB_CHANGE_DURATION = 300L
    private const val BUTTON_PRESS_DURATION = 250L
    private const val DATA_REFRESH_DURATION = 600L
    private const val SUCCESS_STATE_DURATION = 1200L
    private const val ERROR_DURATION = 500L
    
    /**
     * Tab change animation with soft fade and upward motion
     */
    @Composable
    fun TabChangeAnimation(
        isActive: Boolean,
        content: @Composable () -> Unit
    ) {
        val transition = updateTransition(
            targetState = isActive,
            label = "tab_change"
        )
        
        val alpha by transition.animateFloat(
            transitionSpec = { tween(TAB_CHANGE_DURATION.toInt(), easing = EaseInOut) },
            label = "alpha"
        ) { active ->
            if (active) 1f else 0.7f
        }
        
        val offsetY by transition.animateFloat(
            transitionSpec = { tween(TAB_CHANGE_DURATION.toInt(), easing = EaseInOut) },
            label = "offset_y"
        ) { active ->
            if (active) 0f else 8f
        }
        
        Box(
            modifier = Modifier
                .graphicsLayer {
                    this.alpha = alpha
                    translationY = offsetY
                }
        ) {
            content()
        }
    }
    
    /**
     * Button press animation with gradient pulse
     */
    @Composable
    fun ButtonPressAnimation(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        content: @Composable () -> Unit
    ) {
        val hapticFeedback = LocalHapticFeedback.current
        var isPressed by remember { mutableStateOf(false) }
        
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.95f else 1f,
            animationSpec = tween(BUTTON_PRESS_DURATION.toInt(), easing = EaseInOut),
            label = "button_scale"
        )
        
        val pulseAlpha by animateFloatAsState(
            targetValue = if (isPressed) 1f else 0f,
            animationSpec = tween(BUTTON_PRESS_DURATION.toInt(), easing = EaseOut),
            label = "pulse_alpha"
        )
        
        Box(
            modifier = modifier
                .scale(scale)
                .pointerInput(onClick) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            hapticFeedback.performHapticFeedback(
                                androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                            )
                            tryAwaitRelease()
                            isPressed = false
                        },
                        onTap = {
                            if (enabled) {
                                onClick()
                            }
                        }
                    )
                }
        ) {
            content()
            
            // Gradient pulse overlay
            if (isPressed) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = pulseAlpha }
                ) {
                    TimekeeperTheme.TimekeeperGlow(
                        modifier = Modifier.fillMaxSize(),
                        intensity = pulseAlpha
                    )
                }
            }
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
            targetValue = if (isRefreshing) 3f else 0f,
            animationSpec = if (isRefreshing) {
                infiniteRepeatable(
                    animation = tween(DATA_REFRESH_DURATION.toInt(), easing = EaseOut),
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
            initialValue = 0.8f,
            targetValue = 0f,
            animationSpec = if (isRefreshing) {
                infiniteRepeatable(
                    animation = tween(DATA_REFRESH_DURATION.toInt(), easing = EaseOut),
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
                TimekeeperTheme.TimekeeperRipple(
                    modifier = Modifier.fillMaxSize(),
                    isVisible = true
                )
            }
        }
    }
    
    /**
     * Success state animation with arcs align and outward glow
     */
    @Composable
    fun SuccessStateAnimation(
        isSuccess: Boolean,
        content: @Composable () -> Unit
    ) {
        val transition = updateTransition(
            targetState = isSuccess,
            label = "success_state"
        )
        
        val glowIntensity by transition.animateFloat(
            transitionSpec = { tween(SUCCESS_STATE_DURATION.toInt(), easing = EaseOut) },
            label = "glow_intensity"
        ) { success ->
            if (success) 1f else 0f
        }
        
        val scale by transition.animateFloat(
            transitionSpec = { tween(SUCCESS_STATE_DURATION.toInt(), easing = EaseOut) },
            label = "scale"
        ) { success ->
            if (success) 1.05f else 1f
        }
        
        Box(
            modifier = Modifier
                .scale(scale)
                .graphicsLayer { alpha = 1f }
        ) {
            content()
            
            if (isSuccess) {
                TimekeeperTheme.TimekeeperGlow(
                    modifier = Modifier.fillMaxSize(),
                    intensity = glowIntensity,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
    
    /**
     * Error state animation with subtle desync shake
     */
    @Composable
    fun ErrorStateAnimation(
        isError: Boolean,
        content: @Composable () -> Unit
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "error_shake")
        val shakeOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = if (isError) 4f else 0f,
            animationSpec = if (isError) {
                infiniteRepeatable(
                    animation = tween(ERROR_DURATION.toInt(), easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse
                )
            } else {
                infiniteRepeatable(
                    animation = tween(0, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            },
            label = "shake_offset"
        )
        
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX = shakeOffset
                }
        ) {
            content()
        }
    }
    
    /**
     * Haptic feedback helper
     */
    @Composable
    fun rememberTimekeeperHapticFeedback(): androidx.compose.ui.hapticfeedback.HapticFeedback {
        return LocalHapticFeedback.current
    }
    
    /**
     * Sound feedback helper (placeholder for future implementation)
     */
    @Composable
    fun rememberTimekeeperSoundFeedback(): () -> Unit {
        return remember {
            {
                // TODO: Implement sound feedback with chime_light.wav
                // For now, this is a placeholder
            }
        }
    }
}