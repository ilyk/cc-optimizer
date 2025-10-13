package im.ilyk.ccoptimizer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark color scheme - polished for OLED
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),           // Lighter blue for dark backgrounds
    secondary = Color(0xFF80CBC4),         // Teal accent
    tertiary = Color(0xFFCE93D8),          // Purple accent
    background = Color(0xFF121212),        // Pure black for OLED
    surface = Color(0xFF1E1E1E),           // Slightly elevated
    surfaceVariant = Color(0xFF2C2C2C),    // Cards, chips
    onPrimary = Color(0xFF003258),         // Dark text on primary
    onSecondary = Color(0xFF00251A),       // Dark text on secondary
    onTertiary = Color(0xFF3E2346),        // Dark text on tertiary
    onBackground = Color(0xFFE1E2E5),      // Light text on dark bg
    onSurface = Color(0xFFE1E2E5),         // Light text on surfaces
    error = Color(0xFFCF6679),             // Error red
    onError = Color(0xFF000000)            // Black text on error
)

// Light color scheme - Material 3 with brand colors
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),           // Blue
    secondary = Color(0xFF00796B),         // Teal
    tertiary = Color(0xFF7B1FA2),          // Purple
    background = Color(0xFFFCFCFC),        // Off-white
    surface = Color(0xFFFFFFFF),           // Pure white
    surfaceVariant = Color(0xFFF5F5F5),    // Cards, chips
    onPrimary = Color.White,               // White text on primary
    onSecondary = Color.White,             // White text on secondary
    onTertiary = Color.White,              // White text on tertiary
    onBackground = Color(0xFF1A1C1E),      // Dark text on light bg
    onSurface = Color(0xFF1A1C1E),         // Dark text on surfaces
    error = Color(0xFFB00020),             // Error red
    onError = Color.White                  // White text on error
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
    
    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

