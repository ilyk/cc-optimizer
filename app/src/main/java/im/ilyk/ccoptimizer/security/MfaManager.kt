package im.ilyk.ccoptimizer.security

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Multi-Factor Authentication Manager
 * 
 * Supports multiple authentication methods:
 * - Biometric (fingerprint, face)
 * - FIDO2/WebAuthn (Yubikey, hardware keys)
 * - Passkeys (platform authenticator)
 * 
 * Security levels are configurable per action:
 * - NONE: No additional auth required
 * - BIOMETRIC: Fingerprint or face required
 * - HARDWARE_KEY: FIDO2 hardware key required
 * - BIOMETRIC_AND_HARDWARE: Both required
 * - PASSKEY: Passkey authentication
 */
class MfaManager(private val context: Context) {
    
    private val preferences = context.getSharedPreferences("mfa_prefs", Context.MODE_PRIVATE)
    private val biometricManager = BiometricManager.from(context)
    
    private val _mfaConfig = MutableStateFlow(loadMfaConfig())
    val mfaConfig: StateFlow<MfaConfig> = _mfaConfig.asStateFlow()
    
    /**
     * Check if biometric authentication is available on this device
     */
    fun isBiometricAvailable(): BiometricAvailability {
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> 
                BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                BiometricAvailability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                BiometricAvailability.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                BiometricAvailability.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                BiometricAvailability.SECURITY_UPDATE_REQUIRED
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED ->
                BiometricAvailability.UNSUPPORTED
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN ->
                BiometricAvailability.UNKNOWN
            else -> BiometricAvailability.UNKNOWN
        }
    }
    
    /**
     * Check if device biometric can be used
     */
    fun canUseBiometric(): Boolean {
        return isBiometricAvailable() == BiometricAvailability.AVAILABLE
    }
    
    /**
     * Authenticate with biometrics
     */
    fun authenticateWithBiometric(
        activity: FragmentActivity,
        action: MfaAction,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit = {}
    ) {
        if (!canUseBiometric()) {
            onError("Biometric authentication not available")
            return
        }
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(action.biometricTitle)
            .setSubtitle(action.biometricSubtitle)
            .setDescription(action.biometricDescription)
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setConfirmationRequired(action.requireConfirmation)
            .build()
        
        val biometricPrompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        onCancel()
                    } else {
                        onError(errString.toString())
                    }
                }
                
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Don't call onError here - user can retry
                }
            }
        )
        
        biometricPrompt.authenticate(promptInfo)
    }
    
    /**
     * Enable biometric authentication for user
     */
    fun enableBiometric(enabled: Boolean) {
        preferences.edit().putBoolean(PREF_BIOMETRIC_ENABLED, enabled).apply()
        _mfaConfig.value = _mfaConfig.value.copy(biometricEnabled = enabled)
    }
    
    /**
     * Check if biometric is enabled for user
     */
    fun isBiometricEnabled(): Boolean {
        return preferences.getBoolean(PREF_BIOMETRIC_ENABLED, false)
    }
    
    /**
     * Set security level for a specific action
     */
    fun setActionSecurityLevel(action: MfaAction, level: SecurityLevel) {
        preferences.edit().putString("security_${action.key}", level.name).apply()
        _mfaConfig.value = loadMfaConfig()
    }
    
    /**
     * Get required security level for an action
     */
    fun getRequiredSecurityLevel(action: MfaAction): SecurityLevel {
        val levelName = preferences.getString("security_${action.key}", null)
            ?: action.defaultSecurityLevel.name
        return SecurityLevel.valueOf(levelName)
    }
    
    /**
     * Check if user meets security requirements for an action
     */
    suspend fun checkSecurityRequirements(
        activity: FragmentActivity,
        action: MfaAction,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val requiredLevel = getRequiredSecurityLevel(action)
        
        when (requiredLevel) {
            SecurityLevel.NONE -> {
                // No additional auth required
                onSuccess()
            }
            SecurityLevel.BIOMETRIC -> {
                // Require biometric only
                authenticateWithBiometric(activity, action, onSuccess, onError)
            }
            SecurityLevel.HARDWARE_KEY -> {
                // Require FIDO2 hardware key
                authenticateWithHardwareKey(activity, action, onSuccess, onError)
            }
            SecurityLevel.BIOMETRIC_AND_HARDWARE -> {
                // Require both biometric and hardware key
                authenticateWithBiometric(activity, action, 
                    onSuccess = {
                        authenticateWithHardwareKey(activity, action, onSuccess, onError)
                    },
                    onError = onError
                )
            }
            SecurityLevel.PASSKEY -> {
                // Require passkey authentication
                authenticateWithPasskey(activity, action, onSuccess, onError)
            }
        }
    }
    
    /**
     * Authenticate with FIDO2 hardware key (Yubikey, etc.)
     */
    private fun authenticateWithHardwareKey(
        activity: FragmentActivity,
        action: MfaAction,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // FIDO2/WebAuthn implementation
        // This requires Credential Manager API (Android 14+) or FIDO2 API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Use Credential Manager for passkeys and FIDO2
            authenticateWithCredentialManager(activity, action, onSuccess, onError)
        } else {
            // Fallback for older Android versions
            onError("Hardware key authentication requires Android 14+")
        }
    }
    
    /**
     * Authenticate with passkey
     */
    private fun authenticateWithPasskey(
        activity: FragmentActivity,
        action: MfaAction,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            authenticateWithCredentialManager(activity, action, onSuccess, onError)
        } else {
            onError("Passkey authentication requires Android 14+")
        }
    }
    
    /**
     * Authenticate using Credential Manager (Android 14+)
     * Supports passkeys and FIDO2
     */
    private fun authenticateWithCredentialManager(
        activity: FragmentActivity,
        action: MfaAction,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Implementation will use androidx.credentials:credentials
        // This is a placeholder for the full implementation
        onError("Credential Manager not yet implemented")
    }
    
    /**
     * Load MFA configuration from preferences
     */
    private fun loadMfaConfig(): MfaConfig {
        return MfaConfig(
            biometricEnabled = preferences.getBoolean(PREF_BIOMETRIC_ENABLED, false),
            hardwareKeyEnabled = preferences.getBoolean(PREF_HARDWARE_KEY_ENABLED, false),
            passkeyEnabled = preferences.getBoolean(PREF_PASSKEY_ENABLED, false),
            securityLevels = MfaAction.values().associate { action ->
                action to getRequiredSecurityLevel(action)
            }
        )
    }
    
    /**
     * Show onboarding tutorial for MFA setup
     */
    fun shouldShowMfaOnboarding(): Boolean {
        return preferences.getBoolean(PREF_SHOW_MFA_ONBOARDING, true)
    }
    
    /**
     * Mark MFA onboarding as completed
     */
    fun completeMfaOnboarding() {
        preferences.edit().putBoolean(PREF_SHOW_MFA_ONBOARDING, false).apply()
    }
    
    companion object {
        private const val PREF_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val PREF_HARDWARE_KEY_ENABLED = "hardware_key_enabled"
        private const val PREF_PASSKEY_ENABLED = "passkey_enabled"
        private const val PREF_SHOW_MFA_ONBOARDING = "show_mfa_onboarding"
        
        @Volatile
        private var INSTANCE: MfaManager? = null
        
        fun getInstance(context: Context): MfaManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MfaManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

/**
 * Biometric availability status
 */
enum class BiometricAvailability {
    AVAILABLE,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    NOT_ENROLLED,
    SECURITY_UPDATE_REQUIRED,
    UNSUPPORTED,
    UNKNOWN
}

/**
 * Security levels for different actions
 */
enum class SecurityLevel {
    NONE,                    // No additional auth
    BIOMETRIC,              // Fingerprint or face
    HARDWARE_KEY,           // FIDO2/Yubikey
    BIOMETRIC_AND_HARDWARE, // Both biometric + hardware key
    PASSKEY                 // Passkey authentication
}

/**
 * Actions that can require MFA
 */
enum class MfaAction(
    val key: String,
    val displayName: String,
    val defaultSecurityLevel: SecurityLevel,
    val biometricTitle: String,
    val biometricSubtitle: String,
    val biometricDescription: String,
    val requireConfirmation: Boolean = false
) {
    APP_UNLOCK(
        key = "app_unlock",
        displayName = "Unlock App After Minimization",
        defaultSecurityLevel = SecurityLevel.BIOMETRIC,
        biometricTitle = "Unlock App",
        biometricSubtitle = "Verify your identity",
        biometricDescription = "Use biometric to unlock the app",
        requireConfirmation = false
    ),
    
    LOGIN(
        key = "login",
        displayName = "Login to Account",
        defaultSecurityLevel = SecurityLevel.NONE,
        biometricTitle = "Login",
        biometricSubtitle = "Authenticate to login",
        biometricDescription = "Verify your identity to login",
        requireConfirmation = false
    ),
    
    CONNECT_BANK(
        key = "connect_bank",
        displayName = "Connect Bank Account",
        defaultSecurityLevel = SecurityLevel.BIOMETRIC,
        biometricTitle = "Connect Bank",
        biometricSubtitle = "Connecting to your bank",
        biometricDescription = "Verify identity to connect your bank account via Plaid",
        requireConfirmation = true
    ),
    
    ADD_CARD(
        key = "add_card",
        displayName = "Add Credit Card",
        defaultSecurityLevel = SecurityLevel.BIOMETRIC_AND_HARDWARE,
        biometricTitle = "Add Card",
        biometricSubtitle = "Adding credit card",
        biometricDescription = "Verify identity to add a new credit card",
        requireConfirmation = true
    ),
    
    REMOVE_CARD(
        key = "remove_card",
        displayName = "Remove Credit Card",
        defaultSecurityLevel = SecurityLevel.BIOMETRIC,
        biometricTitle = "Remove Card",
        biometricSubtitle = "Removing credit card",
        biometricDescription = "Verify identity to remove this credit card",
        requireConfirmation = true
    ),
    
    VIEW_SENSITIVE_DATA(
        key = "view_sensitive",
        displayName = "View Sensitive Data",
        defaultSecurityLevel = SecurityLevel.BIOMETRIC,
        biometricTitle = "View Data",
        biometricSubtitle = "Accessing sensitive information",
        biometricDescription = "Verify identity to view sensitive financial data",
        requireConfirmation = false
    ),
    
    CHANGE_SECURITY_SETTINGS(
        key = "change_security",
        displayName = "Change Security Settings",
        defaultSecurityLevel = SecurityLevel.BIOMETRIC_AND_HARDWARE,
        biometricTitle = "Security Settings",
        biometricSubtitle = "Modifying security configuration",
        biometricDescription = "Verify identity to change security settings",
        requireConfirmation = true
    ),
    
    EXPORT_DATA(
        key = "export_data",
        displayName = "Export Data",
        defaultSecurityLevel = SecurityLevel.BIOMETRIC_AND_HARDWARE,
        biometricTitle = "Export Data",
        biometricSubtitle = "Exporting financial data",
        biometricDescription = "Verify identity to export your data",
        requireConfirmation = true
    )
}

/**
 * MFA configuration
 */
data class MfaConfig(
    val biometricEnabled: Boolean = false,
    val hardwareKeyEnabled: Boolean = false,
    val passkeyEnabled: Boolean = false,
    val securityLevels: Map<MfaAction, SecurityLevel> = emptyMap()
) {
    /**
     * Check if any MFA method is enabled
     */
    fun isAnyMfaEnabled(): Boolean {
        return biometricEnabled || hardwareKeyEnabled || passkeyEnabled
    }
    
    /**
     * Get human-readable description of enabled methods
     */
    fun getEnabledMethodsDescription(): String {
        val methods = buildList {
            if (biometricEnabled) add("Biometric")
            if (hardwareKeyEnabled) add("Hardware Key")
            if (passkeyEnabled) add("Passkey")
        }
        return when {
            methods.isEmpty() -> "None"
            methods.size == 1 -> methods[0]
            else -> methods.dropLast(1).joinToString(", ") + " and " + methods.last()
        }
    }
}

/**
 * Result of MFA authentication attempt
 */
sealed class MfaResult {
    object Success : MfaResult()
    data class Error(val message: String) : MfaResult()
    object Cancelled : MfaResult()
    data class PartialSuccess(val completedSteps: List<String>) : MfaResult()
}

