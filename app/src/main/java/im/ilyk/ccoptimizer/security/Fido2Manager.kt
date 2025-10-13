package im.ilyk.ccoptimizer.security

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * FIDO2/WebAuthn Manager
 * 
 * Supports:
 * - FIDO2 hardware keys (Yubikey, Google Titan, etc.)
 * - Platform authenticators (passkeys)
 * - WebAuthn standard
 * 
 * Requires Android 14+ (API 34+) for Credential Manager API
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class Fido2Manager(private val context: Context) {
    
    private val credentialManager = CredentialManager.create(context)
    private val preferences = context.getSharedPreferences("fido2_prefs", Context.MODE_PRIVATE)
    
    /**
     * Register a new FIDO2 credential (hardware key or passkey)
     */
    suspend fun registerCredential(
        activity: FragmentActivity,
        userId: String,
        userName: String,
        userDisplayName: String,
        requireUserVerification: Boolean = true,
        requireResidentKey: Boolean = false
    ): Result<String> = withContext(Dispatchers.Main) {
        try {
            // Generate challenge from backend
            val challenge = generateChallenge()
            
            // Create registration request
            val requestJson = buildRegistrationRequest(
                userId = userId,
                userName = userName,
                userDisplayName = userDisplayName,
                challenge = challenge,
                requireUserVerification = requireUserVerification,
                requireResidentKey = requireResidentKey
            )
            
            val request = CreatePublicKeyCredentialRequest(requestJson)
            
            // Show system UI for credential creation
            val result = credentialManager.createCredential(
                request = request,
                activity = activity
            )
            
            when (result) {
                is CreatePublicKeyCredentialResponse -> {
                    // Extract credential ID and public key
                    val credentialId = extractCredentialId(result.registrationResponseJson)
                    
                    // Save credential ID
                    saveCredentialId(credentialId)
                    
                    // Send to backend for verification
                    verifyRegistrationWithBackend(result.registrationResponseJson)
                    
                    Result.success(credentialId)
                }
                else -> {
                    Result.failure(Exception("Unknown credential type"))
                }
            }
        } catch (e: CreateCredentialException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Authenticate with existing FIDO2 credential
     */
    suspend fun authenticate(
        activity: FragmentActivity,
        userId: String
    ): Result<String> = withContext(Dispatchers.Main) {
        try {
            // Generate challenge from backend
            val challenge = generateChallenge()
            
            // Get saved credential IDs
            val credentialIds = getSavedCredentialIds()
            
            if (credentialIds.isEmpty()) {
                return@withContext Result.failure(Exception("No credentials registered"))
            }
            
            // Build authentication request
            val requestJson = buildAuthenticationRequest(
                challenge = challenge,
                credentialIds = credentialIds
            )
            
            val publicKeyOption = GetPublicKeyCredentialOption(requestJson)
            val getCredRequest = GetCredentialRequest(listOf(publicKeyOption))
            
            // Show system UI for authentication
            val result = credentialManager.getCredential(
                request = getCredRequest,
                activity = activity
            )
            
            when (val credential = result.credential) {
                is PublicKeyCredential -> {
                    // Verify signature with backend
                    verifyAuthenticationWithBackend(credential.authenticationResponseJson)
                    
                    Result.success("Authentication successful")
                }
                else -> {
                    Result.failure(Exception("Unknown credential type"))
                }
            }
        } catch (e: GetCredentialException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Build FIDO2 registration request (WebAuthn format)
     */
    private fun buildRegistrationRequest(
        userId: String,
        userName: String,
        userDisplayName: String,
        challenge: String,
        requireUserVerification: Boolean,
        requireResidentKey: Boolean
    ): String {
        return JSONObject().apply {
            // Relying Party (your app)
            put("rp", JSONObject().apply {
                put("id", "ccoptimizer.ilyk.im")  // Your domain
                put("name", "Credit Card Optimizer")
            })
            
            // User info
            put("user", JSONObject().apply {
                put("id", userId)
                put("name", userName)
                put("displayName", userDisplayName)
            })
            
            // Challenge (from backend)
            put("challenge", challenge)
            
            // Supported algorithms
            put("pubKeyCredParams", org.json.JSONArray().apply {
                // ES256 (ECDSA with SHA-256)
                put(JSONObject().apply {
                    put("type", "public-key")
                    put("alg", -7)
                })
                // RS256 (RSA with SHA-256)
                put(JSONObject().apply {
                    put("type", "public-key")
                    put("alg", -257)
                })
            })
            
            // Authenticator selection
            put("authenticatorSelection", JSONObject().apply {
                put("authenticatorAttachment", if (requireResidentKey) "platform" else "cross-platform")
                put("requireResidentKey", requireResidentKey)
                put("residentKey", if (requireResidentKey) "required" else "discouraged")
                put("userVerification", if (requireUserVerification) "required" else "preferred")
            })
            
            // Timeout (60 seconds)
            put("timeout", 60000)
            
            // Attestation
            put("attestation", "none")  // or "direct" for hardware attestation
        }.toString()
    }
    
    /**
     * Build FIDO2 authentication request
     */
    private fun buildAuthenticationRequest(
        challenge: String,
        credentialIds: List<String>
    ): String {
        return JSONObject().apply {
            put("challenge", challenge)
            put("rpId", "ccoptimizer.ilyk.im")
            put("timeout", 60000)
            put("userVerification", "required")
            
            // Allow credentials
            put("allowCredentials", org.json.JSONArray().apply {
                credentialIds.forEach { id ->
                    put(JSONObject().apply {
                        put("type", "public-key")
                        put("id", id)
                    })
                }
            })
        }.toString()
    }
    
    /**
     * Generate authentication challenge (should come from backend)
     */
    private suspend fun generateChallenge(): String {
        // TODO: Get challenge from backend API
        // For now, generate random challenge
        val bytes = ByteArray(32)
        java.security.SecureRandom().nextBytes(bytes)
        return android.util.Base64.encodeToString(bytes, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
    }
    
    /**
     * Extract credential ID from registration response
     */
    private fun extractCredentialId(responseJson: String): String {
        val json = JSONObject(responseJson)
        return json.optString("id", "")
    }
    
    /**
     * Save credential ID for future authentication
     */
    private fun saveCredentialId(credentialId: String) {
        val existing = getSavedCredentialIds().toMutableSet()
        existing.add(credentialId)
        preferences.edit()
            .putStringSet(PREF_CREDENTIAL_IDS, existing)
            .apply()
    }
    
    /**
     * Get saved credential IDs
     */
    private fun getSavedCredentialIds(): List<String> {
        return preferences.getStringSet(PREF_CREDENTIAL_IDS, emptySet())?.toList() ?: emptyList()
    }
    
    /**
     * Verify registration with backend
     */
    private suspend fun verifyRegistrationWithBackend(responseJson: String) {
        // TODO: Send to backend for verification
        // Backend should verify:
        // - Challenge matches
        // - Signature is valid
        // - Origin matches
        // - Store public key
    }
    
    /**
     * Verify authentication with backend
     */
    private suspend fun verifyAuthenticationWithBackend(responseJson: String) {
        // TODO: Send to backend for verification
        // Backend should verify:
        // - Challenge matches
        // - Signature is valid using stored public key
        // - Counter is incremented (prevents replay)
    }
    
    /**
     * Check if user has registered any FIDO2 credentials
     */
    fun hasRegisteredCredentials(): Boolean {
        return getSavedCredentialIds().isNotEmpty()
    }
    
    /**
     * Remove all FIDO2 credentials
     */
    fun removeAllCredentials() {
        preferences.edit()
            .remove(PREF_CREDENTIAL_IDS)
            .apply()
    }
    
    /**
     * Check if FIDO2 is supported on this device
     */
    fun isSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    }
    
    companion object {
        private const val PREF_CREDENTIAL_IDS = "fido2_credential_ids"
        
        @Volatile
        private var INSTANCE: Fido2Manager? = null
        
        fun getInstance(context: Context): Fido2Manager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Fido2Manager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

