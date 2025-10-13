# Dark Mode

Material 3 dynamic theming with Light, Dark, and Auto modes.

## Overview

The app supports three theme modes:
- **Light**: Always light theme
- **Dark**: Always dark theme (OLED-optimized)
- **Auto**: Follows system preference

## Implementation

### Theme System

```kotlin
@Composable
fun CreditCardOptimizerTheme(
    themeMode: Int = ThemePreferences.THEME_AUTO,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemePreferences.THEME_LIGHT -> false
        ThemePreferences.THEME_DARK -> true
        ThemePreferences.THEME_AUTO -> isSystemInDarkTheme()
        else -> isSystemInDarkTheme()
    }
    
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

### Color Schemes

**Light Theme**:
```kotlin
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),        // Blue
    secondary = Color(0xFF388E3C),      // Green
    tertiary = Color(0xFFFFA000),       // Amber
    background = Color(0xFFFFFBFE),     // Off-white
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)
```

**Dark Theme** (OLED-optimized):
```kotlin
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF64B5F6),        // Light Blue
    secondary = Color(0xFF81C784),      // Light Green
    tertiary = Color(0xFFFFCA28),       // Light Amber
    background = Color(0xFF000000),     // True black (OLED)
    surface = Color(0xFF121212),        // Near black
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5)
)
```

### Theme Persistence

```kotlin
class ThemePreferences private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("theme", Context.MODE_PRIVATE)
    
    private val _themeMode = MutableStateFlow(
        prefs.getInt(KEY_THEME_MODE, THEME_AUTO)
    )
    val themeMode: StateFlow<Int> = _themeMode.asStateFlow()
    
    fun setThemeMode(mode: Int) {
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
        _themeMode.value = mode
    }
    
    companion object {
        const val THEME_LIGHT = 0
        const val THEME_DARK = 1
        const val THEME_AUTO = 2
    }
}
```

### Settings UI

```kotlin
@Composable
fun ThemeSelector() {
    val themePreferences = ThemePreferences.getInstance(LocalContext.current)
    val currentTheme by themePreferences.themeMode.collectAsState()
    
    Column {
        Text("Theme", style = MaterialTheme.typography.titleMedium)
        
        RadioButton(
            selected = currentTheme == ThemePreferences.THEME_LIGHT,
            onClick = { themePreferences.setThemeMode(ThemePreferences.THEME_LIGHT) },
            label = "Light"
        )
        
        RadioButton(
            selected = currentTheme == ThemePreferences.THEME_DARK,
            onClick = { themePreferences.setThemeMode(ThemePreferences.THEME_DARK) },
            label = "Dark"
        )
        
        RadioButton(
            selected = currentTheme == ThemePreferences.THEME_AUTO,
            onClick = { themePreferences.setThemeMode(ThemePreferences.THEME_AUTO) },
            label = "Auto (System)"
        )
    }
}
```

## OLED Optimization

Dark theme uses true black (#000000) for:
- **Power savings** on OLED screens
- **Better contrast** for readability
- **Reduced eye strain** in dark environments

## Dynamic Colors

Material 3 supports dynamic colors from wallpaper (Android 12+):

```kotlin
val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

val colorScheme = when {
    dynamicColor && darkTheme -> {
        dynamicDarkColorScheme(LocalContext.current)
    }
    dynamicColor && !darkTheme -> {
        dynamicLightColorScheme(LocalContext.current)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
}
```

## Testing Themes

**Manual testing**:
1. Open Settings
2. Change theme mode
3. Verify all screens
4. Check text contrast
5. Verify colors

**Automated testing**:
```kotlin
@Test
fun testDarkTheme() {
    composeTestRule.setContent {
        CreditCardOptimizerTheme(themeMode = ThemePreferences.THEME_DARK) {
            MainScreen()
        }
    }
    
    // Verify dark background
    composeTestRule.onNodeWithTag("background")
        .assertBackgroundColor(Color(0xFF000000))
}
```

---

**See Also**:
- [Material 3 Design](https://m3.material.io/)
- [UI Components](../ui/components.md)

