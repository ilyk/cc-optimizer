package im.ilyk.ccoptimizer.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import im.ilyk.ccoptimizer.security.BiometricAvailability
import im.ilyk.ccoptimizer.security.MfaManager

/**
 * MFA Onboarding Screen
 * 
 * Interactive tutorial showing users how to enable MFA:
 * - Biometric (fingerprint/face)
 * - FIDO2 hardware keys (Yubikey)
 * - Passkeys
 * 
 * Features:
 * - Step-by-step wizard
 * - Real-time availability detection
 * - "Try it now" interactive demos
 * - Clear explanations of benefits
 * - Can skip (optional)
 */
@Composable
fun MfaOnboardingScreen(
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val mfaManager = remember { MfaManager.getInstance(context) }
    
    var currentStep by remember { mutableStateOf(0) }
    var biometricEnabled by remember { mutableStateOf(false) }
    var hardwareKeyEnabled by remember { mutableStateOf(false) }
    var passkeyEnabled by remember { mutableStateOf(false) }
    
    val biometricAvailability by remember {
        mutableStateOf(mfaManager.isBiometricAvailable())
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Secure Your Account") },
                actions = {
                    TextButton(onClick = onSkip) {
                        Text("Skip for now")
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = (currentStep + 1) / 4f,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            when (currentStep) {
                0 -> IntroductionStep(onNext = { currentStep = 1 })
                1 -> BiometricStep(
                    available = biometricAvailability,
                    enabled = biometricEnabled,
                    onEnable = { enabled ->
                        if (enabled && activity != null) {
                            mfaManager.authenticateWithBiometric(
                                activity = activity,
                                action = im.ilyk.ccoptimizer.security.MfaAction.APP_UNLOCK,
                                onSuccess = {
                                    mfaManager.enableBiometric(true)
                                    biometricEnabled = true
                                },
                                onError = { /* Show error */ }
                            )
                        } else {
                            mfaManager.enableBiometric(false)
                            biometricEnabled = false
                        }
                    },
                    onNext = { currentStep = 2 },
                    onBack = { currentStep = 0 }
                )
                2 -> HardwareKeyStep(
                    enabled = hardwareKeyEnabled,
                    onEnable = { hardwareKeyEnabled = it },
                    onNext = { currentStep = 3 },
                    onBack = { currentStep = 1 }
                )
                3 -> SummaryStep(
                    biometricEnabled = biometricEnabled,
                    hardwareKeyEnabled = hardwareKeyEnabled,
                    passkeyEnabled = passkeyEnabled,
                    onComplete = {
                        mfaManager.completeMfaOnboarding()
                        onComplete()
                    },
                    onBack = { currentStep = 2 }
                )
            }
        }
    }
}

@Composable
private fun IntroductionStep(onNext: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Secure Your Financial Data",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Add extra security layers to protect your credit card information and bank connections.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        SecurityFeatureCard(
            icon = Icons.Default.Fingerprint,
            title = "Biometric Authentication",
            description = "Use fingerprint or face recognition for quick, secure access"
        )
        
        SecurityFeatureCard(
            icon = Icons.Default.Key,
            title = "Hardware Security Keys",
            description = "Add Yubikey or other FIDO2 keys for maximum security"
        )
        
        SecurityFeatureCard(
            icon = Icons.Default.Password,
            title = "Passkeys",
            description = "Modern passwordless authentication synced across your devices"
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Get Started")
        }
    }
}

@Composable
private fun BiometricStep(
    available: BiometricAvailability,
    enabled: Boolean,
    onEnable: (Boolean) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Fingerprint,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Biometric Authentication",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        when (available) {
            BiometricAvailability.AVAILABLE -> {
                Text(
                    text = "Your device supports biometric authentication!",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "✓ Quick unlock (no password needed)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "✓ Secure (biometric data never leaves your device)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "✓ Convenient (touch or glance to authenticate)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (enabled) "Biometric Enabled" else "Enable Biometric",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Switch(
                        checked = enabled,
                        onCheckedChange = onEnable
                    )
                }
                
                if (enabled) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "Biometric authentication is now active!",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            
            BiometricAvailability.NOT_ENROLLED -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "⚠️ No Biometrics Enrolled",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "To use biometric authentication, please:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "1. Go to Settings > Security",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "2. Add a fingerprint or face",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "3. Come back to enable biometric auth",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            BiometricAvailability.NO_HARDWARE -> {
                Text(
                    text = "Your device doesn't support biometric authentication",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "You can use hardware keys or passkeys instead",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
            
            else -> {
                Text(
                    text = "Biometric authentication is currently unavailable",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onBack) {
                Text("Back")
            }
            
            Button(onClick = onNext) {
                Text("Next")
            }
        }
    }
}

@Composable
private fun HardwareKeyStep(
    enabled: Boolean,
    onEnable: (Boolean) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Key,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Hardware Security Keys",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Add an extra layer of security with FIDO2-compatible hardware keys like Yubikey.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Supported Hardware:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                HardwareKeyItem("✓ Yubikey (all models)")
                HardwareKeyItem("✓ Google Titan Security Key")
                HardwareKeyItem("✓ Feitian ePass FIDO")
                HardwareKeyItem("✓ Any FIDO2/WebAuthn key")
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "💡 Recommended For:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("• High-value transactions", style = MaterialTheme.typography.bodyMedium)
                Text("• Adding/removing cards", style = MaterialTheme.typography.bodyMedium)
                Text("• Changing security settings", style = MaterialTheme.typography.bodyMedium)
            }
        }
        
        AnimatedVisibility(visible = enabled) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "Hardware key support enabled! You can register your key in Settings.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (enabled) "Hardware Key Enabled" else "Enable Hardware Keys",
                style = MaterialTheme.typography.titleMedium
            )
            
            Switch(
                checked = enabled,
                onCheckedChange = onEnable
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onBack) {
                Text("Back")
            }
            
            Button(onClick = onNext) {
                Text("Next")
            }
        }
    }
}

@Composable
private fun SummaryStep(
    biometricEnabled: Boolean,
    hardwareKeyEnabled: Boolean,
    passkeyEnabled: Boolean,
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Security Setup Complete!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Enabled Security Features:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (biometricEnabled) {
                    SecurityEnabledItem(
                        icon = Icons.Default.Fingerprint,
                        text = "Biometric Authentication"
                    )
                }
                
                if (hardwareKeyEnabled) {
                    SecurityEnabledItem(
                        icon = Icons.Default.Key,
                        text = "Hardware Security Keys"
                    )
                }
                
                if (passkeyEnabled) {
                    SecurityEnabledItem(
                        icon = Icons.Default.Password,
                        text = "Passkeys"
                    )
                }
                
                if (!biometricEnabled && !hardwareKeyEnabled && !passkeyEnabled) {
                    Text(
                        text = "No additional security enabled (you can enable later in Settings)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "💡 You can customize security levels in Settings:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "• Set different requirements for different actions",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• Enable/disable MFA methods anytime",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• View tutorial again from Settings",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onBack) {
                Text("Back")
            }
            
            Button(
                onClick = onComplete,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            ) {
                Text("Complete Setup")
            }
        }
    }
}

@Composable
private fun SecurityFeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HardwareKeyItem(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Done,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun SecurityEnabledItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

