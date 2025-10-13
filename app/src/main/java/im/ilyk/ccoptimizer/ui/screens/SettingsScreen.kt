package im.ilyk.ccoptimizer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import im.ilyk.ccoptimizer.R
import im.ilyk.ccoptimizer.ui.theme.ThemeMode
import im.ilyk.ccoptimizer.ui.theme.ThemePreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: String,
    onThemeChange: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.content_desc_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Theme Section
            ThemeSection(
                currentTheme = currentTheme,
                onThemeChange = onThemeChange
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Future sections
            // - Notification preferences
            // - Account management
            // - About
        }
    }
}

@Composable
private fun ThemeSection(
    currentTheme: String,
    onThemeChange: (String) -> Unit
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = stringResource(R.string.settings_appearance),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Theme selector
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showThemeDialog = true },
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.settings_theme),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = when (ThemeMode.fromValue(currentTheme)) {
                            ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                            ThemeMode.DARK -> stringResource(R.string.theme_dark)
                            ThemeMode.AUTO -> stringResource(R.string.theme_auto)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = when (currentTheme) {
                        ThemePreferences.THEME_LIGHT -> "☀️"
                        ThemePreferences.THEME_DARK -> "🌙"
                        else -> "🌓"
                    },
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
    
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = currentTheme,
            onThemeSelected = { theme ->
                onThemeChange(theme)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }
}

@Composable
private fun ThemeSelectionDialog(
    currentTheme: String,
    onThemeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_choose)) },
        text = {
            Column {
                ThemeMode.values().forEach { mode ->
                    ThemeOption(
                        mode = mode,
                        isSelected = currentTheme == mode.value,
                        onSelect = { onThemeSelected(mode.value) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_done))
            }
        }
    )
}

@Composable
private fun ThemeOption(
    mode: ThemeMode,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when (mode) {
                        ThemeMode.LIGHT -> "☀️"
                        ThemeMode.DARK -> "🌙"
                        ThemeMode.AUTO -> "🌓"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(end = 16.dp)
                )
                
                Column {
                    Text(
                        text = when (mode) {
                            ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                            ThemeMode.DARK -> stringResource(R.string.theme_dark)
                            ThemeMode.AUTO -> stringResource(R.string.theme_auto)
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = when (mode) {
                            ThemeMode.LIGHT -> stringResource(R.string.theme_light_description)
                            ThemeMode.DARK -> stringResource(R.string.theme_dark_description)
                            ThemeMode.AUTO -> stringResource(R.string.theme_auto_description)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.content_desc_selected),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

