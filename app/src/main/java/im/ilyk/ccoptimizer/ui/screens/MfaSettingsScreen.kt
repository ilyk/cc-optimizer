package im.ilyk.ccoptimizer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import im.ilyk.ccoptimizer.R
import im.ilyk.ccoptimizer.security.MfaAction
import im.ilyk.ccoptimizer.security.MfaManager
import im.ilyk.ccoptimizer.security.SecurityLevel

/**
 * MFA Settings Screen
 * 
 * Allows users to:
 * - Enable/disable biometric authentication
 * - Register hardware security keys
 * - Configure passkeys
 * - Set security levels per action
 * - View tutorial again
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MfaSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val mfaManager = remember { MfaManager.getInstance(context) }
    
    val mfaConfig by mfaManager.mfaConfig.collectAsState()
    var showTutorial by remember { mutableStateOf(false) }
    var showActionSettings by remember { mutableStateOf(false) }
    var selectedAction by remember { mutableStateOf<MfaAction?>(null) }
    
    if (showTutorial) {
        MfaOnboardingScreen(
            onComplete = { showTutorial = false },
            onSkip = { showTutorial = false }
        )
        return
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Multi-Factor Authentication") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (mfaConfig.isAnyMfaEnabled())
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (mfaConfig.isAnyMfaEnabled())
                                Icons.Default.Shield
                            else
                                Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        
                        Column {
                            Text(
                                text = if (mfaConfig.isAnyMfaEnabled())
                                    "MFA Active"
                                else
                                    "MFA Not Configured",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (mfaConfig.isAnyMfaEnabled())
                                    "Enabled: ${mfaConfig.getEnabledMethodsDescription()}"
                                else
                                    "Your account is protected by password only",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            
            // Authentication Methods Section
            Text(
                text = stringResource(R.string.mfa_settings_auth_methods),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            // Biometric
            BiometricSettingCard(
                mfaManager = mfaManager,
                activity = activity,
                enabled = mfaConfig.biometricEnabled,
                onToggle = { mfaManager.enableBiometric(it) }
            )
            
            // Hardware Keys
            HardwareKeySettingCard(
                enabled = mfaConfig.hardwareKeyEnabled,
                onToggle = { /* Enable hardware key */ }
            )
            
            // Passkeys
            PasskeySettingCard(
                enabled = mfaConfig.passkeyEnabled,
                onToggle = { /* Enable passkey */ }
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Security Levels Section
            Text(
                text = stringResource(R.string.mfa_settings_security_levels),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = stringResource(R.string.mfa_settings_security_levels_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Action Security Levels
            MfaAction.values().forEach { action ->
                ActionSecurityCard(
                    action = action,
                    currentLevel = mfaConfig.securityLevels[action] ?: action.defaultSecurityLevel,
                    onClick = {
                        selectedAction = action
                        showActionSettings = true
                    }
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Tutorial Button
            OutlinedButton(
                onClick = { showTutorial = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Info, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Setup Tutorial Again")
            }
        }
    }
    
    // Action Security Level Dialog
    if (showActionSettings && selectedAction != null) {
        SecurityLevelDialog(
            action = selectedAction!!,
            currentLevel = mfaConfig.securityLevels[selectedAction] ?: selectedAction!!.defaultSecurityLevel,
            onDismiss = { showActionSettings = false },
            onSelect = { level ->
                mfaManager.setActionSecurityLevel(selectedAction!!, level)
                showActionSettings = false
            }
        )
    }
}

@Composable
private fun BiometricSettingCard(
    mfaManager: MfaManager,
    activity: FragmentActivity?,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val biometricAvailable = mfaManager.isBiometricAvailable()
    val canUse = biometricAvailable == im.ilyk.ccoptimizer.security.BiometricAvailability.AVAILABLE
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Column {
                        Text(
                            text = stringResource(R.string.mfa_settings_biometric_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(if (canUse) R.string.mfa_settings_biometric_type else R.string.mfa_settings_not_available),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Switch(
                    checked = enabled,
                    enabled = canUse,
                    onCheckedChange = { newEnabled ->
                        if (newEnabled && activity != null) {
                            // Verify with biometric before enabling
                            mfaManager.authenticateWithBiometric(
                                activity = activity,
                                action = MfaAction.CHANGE_SECURITY_SETTINGS,
                                onSuccess = { onToggle(true) },
                                onError = { /* Show error */ }
                            )
                        } else {
                            onToggle(false)
                        }
                    }
                )
            }
            
            if (!canUse) {
                Text(
                    text = when (biometricAvailable) {
                        im.ilyk.ccoptimizer.security.BiometricAvailability.NOT_ENROLLED ->
                            "⚠️ No biometrics enrolled. Go to Settings > Security to add fingerprint or face."
                        im.ilyk.ccoptimizer.security.BiometricAvailability.NO_HARDWARE ->
                            "Your device doesn't support biometric authentication"
                        else ->
                            "Biometric authentication is currently unavailable"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun HardwareKeySettingCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Column {
                        Text(
                            text = stringResource(R.string.mfa_settings_hardware_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.mfa_settings_hardware_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle
                )
            }
            
            if (enabled) {
                Button(
                    onClick = { /* Register hardware key */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Register Hardware Key")
                }
            }
        }
    }
}

@Composable
private fun PasskeySettingCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Password,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Column {
                        Text(
                            text = stringResource(R.string.mfa_settings_passkey_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.mfa_settings_passkey_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle
                )
            }
            
            if (enabled) {
                Button(
                    onClick = { /* Create passkey */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Passkey")
                }
            }
        }
    }
}

@Composable
private fun ActionSecurityCard(
    action: MfaAction,
    currentLevel: SecurityLevel,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = action.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = getSecurityLevelDescription(currentLevel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = getSecurityLevelIcon(currentLevel),
                contentDescription = null,
                tint = getSecurityLevelColor(currentLevel)
            )
        }
    }
}

@Composable
private fun SecurityLevelDialog(
    action: MfaAction,
    currentLevel: SecurityLevel,
    onDismiss: () -> Unit,
    onSelect: (SecurityLevel) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = action.displayName)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.mfa_settings_choose_level),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                SecurityLevel.values().forEach { level ->
                    SecurityLevelOption(
                        level = level,
                        selected = level == currentLevel,
                        onClick = { onSelect(level) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SecurityLevelOption(
    level: SecurityLevel,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getSecurityLevelIcon(level),
                    contentDescription = null,
                    tint = if (selected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Column {
                    Text(
                        text = getSecurityLevelTitle(level),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = getSecurityLevelDescription(level),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (selected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun getSecurityLevelTitle(level: SecurityLevel): String {
    return when (level) {
        SecurityLevel.NONE -> "None"
        SecurityLevel.BIOMETRIC -> "Biometric"
        SecurityLevel.HARDWARE_KEY -> "Hardware Key"
        SecurityLevel.BIOMETRIC_AND_HARDWARE -> "Biometric + Hardware Key"
        SecurityLevel.PASSKEY -> "Passkey"
    }
}

private fun getSecurityLevelDescription(level: SecurityLevel): String {
    return when (level) {
        SecurityLevel.NONE -> "No additional authentication"
        SecurityLevel.BIOMETRIC -> "Fingerprint or face required"
        SecurityLevel.HARDWARE_KEY -> "FIDO2 hardware key required"
        SecurityLevel.BIOMETRIC_AND_HARDWARE -> "Both biometric and hardware key required"
        SecurityLevel.PASSKEY -> "Passkey authentication required"
    }
}

@Composable
private fun getSecurityLevelIcon(level: SecurityLevel): androidx.compose.ui.graphics.vector.ImageVector {
    return when (level) {
        SecurityLevel.NONE -> Icons.Default.LockOpen
        SecurityLevel.BIOMETRIC -> Icons.Default.Fingerprint
        SecurityLevel.HARDWARE_KEY -> Icons.Default.Key
        SecurityLevel.BIOMETRIC_AND_HARDWARE -> Icons.Default.Security
        SecurityLevel.PASSKEY -> Icons.Default.Password
    }
}

@Composable
private fun getSecurityLevelColor(level: SecurityLevel): androidx.compose.ui.graphics.Color {
    return when (level) {
        SecurityLevel.NONE -> MaterialTheme.colorScheme.error
        SecurityLevel.BIOMETRIC -> MaterialTheme.colorScheme.primary
        SecurityLevel.HARDWARE_KEY -> MaterialTheme.colorScheme.tertiary
        SecurityLevel.BIOMETRIC_AND_HARDWARE -> MaterialTheme.colorScheme.secondary
        SecurityLevel.PASSKEY -> MaterialTheme.colorScheme.primary
    }
}

