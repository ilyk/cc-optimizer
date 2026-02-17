package im.ilyk.ccoptimizer.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

/**
 * Timekeeper Haptic Feedback System
 * Premium haptic feedback for key interactions
 */

/**
 * Haptic feedback types for different interactions
 */
enum class TimekeeperHapticType {
    LIGHT_TAP,      // Light tap for button presses
    MEDIUM_TAP,     // Medium tap for card interactions
    HEAVY_TAP,      // Heavy tap for important actions
    SUCCESS,        // Success vibration pattern
    ERROR,          // Error vibration pattern
    RHYTHM_PULSE,   // Rhythm-based pulse for Timekeeper
    OPTIMIZATION    // Special pattern for optimization completion
}

/**
 * Timekeeper Haptic Feedback Manager
 * Manages haptic feedback for the Timekeeper Design System
 */
@Composable
fun rememberTimekeeperHaptic(): TimekeeperHapticManager {
    val context = LocalContext.current
    return remember { TimekeeperHapticManager(context) }
}

class TimekeeperHapticManager(private val context: Context) {
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    /**
     * Trigger haptic feedback based on type
     */
    fun triggerHaptic(type: TimekeeperHapticType) {
        if (vibrator?.hasVibrator() != true) return
        
        when (type) {
            TimekeeperHapticType.LIGHT_TAP -> triggerLightTap()
            TimekeeperHapticType.MEDIUM_TAP -> triggerMediumTap()
            TimekeeperHapticType.HEAVY_TAP -> triggerHeavyTap()
            TimekeeperHapticType.SUCCESS -> triggerSuccessPattern()
            TimekeeperHapticType.ERROR -> triggerErrorPattern()
            TimekeeperHapticType.RHYTHM_PULSE -> triggerRhythmPulse()
            TimekeeperHapticType.OPTIMIZATION -> triggerOptimizationPattern()
        }
    }
    
    /**
     * Light tap for subtle interactions
     */
    private fun triggerLightTap() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(50)
        }
    }
    
    /**
     * Medium tap for card interactions
     */
    private fun triggerMediumTap() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(100)
        }
    }
    
    /**
     * Heavy tap for important actions
     */
    private fun triggerHeavyTap() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(150)
        }
    }
    
    /**
     * Success pattern for completed optimizations
     */
    private fun triggerSuccessPattern() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 100, 50, 100, 50, 200)
            val effect = VibrationEffect.createWaveform(pattern, -1)
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            val pattern = longArrayOf(0, 100, 50, 100, 50, 200)
            vibrator?.vibrate(pattern, -1)
        }
    }
    
    /**
     * Error pattern for failed operations
     */
    private fun triggerErrorPattern() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 200, 100, 200, 100, 200)
            val effect = VibrationEffect.createWaveform(pattern, -1)
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            val pattern = longArrayOf(0, 200, 100, 200, 100, 200)
            vibrator?.vibrate(pattern, -1)
        }
    }
    
    /**
     * Rhythm-based pulse for Timekeeper interactions
     */
    private fun triggerRhythmPulse() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 80, 40, 80, 40, 80, 40, 120)
            val effect = VibrationEffect.createWaveform(pattern, -1)
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            val pattern = longArrayOf(0, 80, 40, 80, 40, 80, 40, 120)
            vibrator?.vibrate(pattern, -1)
        }
    }
    
    /**
     * Special pattern for optimization completion
     */
    private fun triggerOptimizationPattern() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 60, 30, 60, 30, 60, 30, 60, 30, 120)
            val effect = VibrationEffect.createWaveform(pattern, -1)
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            val pattern = longArrayOf(0, 60, 30, 60, 30, 60, 30, 60, 30, 120)
            vibrator?.vibrate(pattern, -1)
        }
    }
}

/**
 * Composable extension for easy haptic feedback
 */
@Composable
fun rememberTimekeeperHapticFeedback(): (TimekeeperHapticType) -> Unit {
    val hapticManager = rememberTimekeeperHaptic()
    return { type -> hapticManager.triggerHaptic(type) }
}
