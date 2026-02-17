package im.ilyk.ccoptimizer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Timekeeper Dark Color Scheme - Deep Cosmos with Indigo gradients
private val TimekeeperDarkColorScheme = darkColorScheme(
    primary = Color(0xFF5865F2),           // Electric Blue - primary action
    secondary = Color(0xFF8E44AD),          // Violet - secondary actions
    tertiary = Color(0xFF273C75),          // Indigo - tertiary elements
    background = Color(0xFF0B0E14),         // Deep Cosmos - main dark canvas
    surface = Color(0xFF1A1D29),            // Elevated surfaces
    surfaceVariant = Color(0xFF2A2D3A),     // Cards, chips, elevated content
    onPrimary = Color.White,                // White text on primary
    onSecondary = Color.White,              // White text on secondary
    onTertiary = Color.White,               // White text on tertiary
    onBackground = Color(0xFFFFFFFF),       // Pure white text on dark bg
    onSurface = Color(0xFFE1E2E5),          // Light text on surfaces
    onSurfaceVariant = Color(0xFFA0AEC0),   // Secondary text
    error = Color(0xFFA83247),              // Deep Crimson - errors
    onError = Color.White,                  // White text on error
    outline = Color(0xFFC7D0E0),            // Silver Mist - borders, accents
    outlineVariant = Color(0xFF3A3D4A)      // Subtle borders
)

// Timekeeper Light Color Scheme - Clean with brand gradients
private val TimekeeperLightColorScheme = lightColorScheme(
    primary = Color(0xFF5865F2),           // Electric Blue
    secondary = Color(0xFF8E44AD),         // Violet
    tertiary = Color(0xFF273C75),          // Indigo
    background = Color(0xFFF8FAFC),        // Clean light background
    surface = Color(0xFFFFFFFF),           // Pure white surfaces
    surfaceVariant = Color(0xFFF1F5F9),    // Light cards, chips
    onPrimary = Color.White,               // White text on primary
    onSecondary = Color.White,              // White text on secondary
    onTertiary = Color.White,               // White text on tertiary
    onBackground = Color(0xFF1A1C1E),      // Dark text on light bg
    onSurface = Color(0xFF1A1C1E),          // Dark text on surfaces
    onSurfaceVariant = Color(0xFF64748B),   // Secondary text
    error = Color(0xFFA83247),             // Deep Crimson
    onError = Color.White,                  // White text on error
    outline = Color(0xFFC7D0E0),            // Silver Mist
    outlineVariant = Color(0xFFE2E8F0)      // Light borders
)

/**
 * App theme with support for Light, Dark, and Auto modes
 * 
 * @param themeMode: "light", "dark", or "auto" (default: "auto")
 * @param content: Composable content
 */
@Composable
fun CreditCardOptimizerTheme(
    themeMode: String = ThemePreferences.THEME_AUTO,
    content: @Composable () -> Unit
) {
    val systemInDarkTheme = isSystemInDarkTheme()
    
    // Determine if dark theme should be used
    val useDarkTheme = when (themeMode) {
        ThemePreferences.THEME_LIGHT -> false
        ThemePreferences.THEME_DARK -> true
        ThemePreferences.THEME_AUTO -> systemInDarkTheme // Follow system
        else -> systemInDarkTheme // Default to auto
    }
    
    val colorScheme = if (useDarkTheme) TimekeeperDarkColorScheme else TimekeeperLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

