package im.ilyk.ccoptimizer.security

import android.content.Context
import android.os.Build
import android.util.Base64
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
import com.google.gson.JsonObject
import dagger.hilt.android.qualifiers.ApplicationContext
import im.ilyk.ccoptimizer.data.api.PlaidApi
import im.ilyk.ccoptimizer.data.models.AuthenticateRequest
import im.ilyk.ccoptimizer.data.models.ChallengeRequest
import im.ilyk.ccoptimizer.data.models.ChallengeResponse
import im.ilyk.ccoptimizer.data.models.MfaException
import im.ilyk.ccoptimizer.data.models.RegisterCredentialRequest
import im.ilyk.ccoptimizer.data.services.SecureStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

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
@Singleton
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class Fido2Manager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: PlaidApi,
    private val secureStore: SecureStore
) {

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
            // Generate challenge from backend for registration
            val challengeResponse = generateChallengeResponse("registration")

            // Create registration request using data from challenge response
            val requestJson = buildRegistrationRequest(
                userId = challengeResponse.userId,  // Use base64-encoded userId from backend
                userName = challengeResponse.userName,
                userDisplayName = userDisplayName,
                challenge = challengeResponse.challenge,
                rpId = challengeResponse.rpId,  // Use rpId from backend
                requireUserVerification = requireUserVerification,
                requireResidentKey = requireResidentKey
            )
            
            val request = CreatePublicKeyCredentialRequest(requestJson)
            
            // Show system UI for credential creation
            val result = credentialManager.createCredential(
                context = activity,
                request = request
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
            // Generate challenge from backend for authentication
            val challengeResponse = generateChallengeResponse("authentication")

            // Get saved credential IDs
            val credentialIds = getSavedCredentialIds()

            if (credentialIds.isEmpty()) {
                return@withContext Result.failure(Exception("No credentials registered"))
            }

            // Build authentication request using data from challenge response
            val requestJson = buildAuthenticationRequest(
                challenge = challengeResponse.challenge,
                rpId = challengeResponse.rpId,  // Use rpId from backend
                credentialIds = credentialIds
            )
            
            val publicKeyOption = GetPublicKeyCredentialOption(requestJson)
            val getCredRequest = GetCredentialRequest(listOf(publicKeyOption))
            
            // Show system UI for authentication
            val result = credentialManager.getCredential(
                context = activity,
                request = getCredRequest
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
        rpId: String,
        requireUserVerification: Boolean,
        requireResidentKey: Boolean
    ): String {
        return JSONObject().apply {
            // Relying Party (your app)
            put("rp", JSONObject().apply {
                put("id", rpId)  // Use rpId from backend challenge response
                put("name", "Credit Card Optimizer")
            })

            // User info
            put("user", JSONObject().apply {
                put("id", userId)  // Already base64url-encoded by backend
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
        rpId: String,
        credentialIds: List<String>
    ): String {
        return JSONObject().apply {
            put("challenge", challenge)
            put("rpId", rpId)  // Use rpId from backend challenge response
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
     * Generate authentication challenge from backend
     * Returns the full challenge response containing challenge, rpId, userId, etc.
     *
     * @param challengeType Either "registration" or "authentication"
     */
    private suspend fun generateChallengeResponse(challengeType: String): ChallengeResponse = withContext(Dispatchers.IO) {
        try {
            val authToken = getAuthToken()
            val request = ChallengeRequest(challengeType = challengeType)
            val response = api.getMfaChallenge("Bearer $authToken", request)

            if (!response.isSuccessful) {
                Timber.e("Failed to get challenge: ${response.code()} - ${response.message()}")
                throw MfaException("Failed to get challenge from server: ${response.code()}")
            }

            val challengeResponse = response.body()
                ?: throw MfaException("Empty challenge response from server")

            Timber.d("Received $challengeType challenge from backend for user: ${challengeResponse.userId}, rpId: ${challengeResponse.rpId}")
            challengeResponse
        } catch (e: MfaException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Error getting challenge from backend")
            throw MfaException("Failed to get challenge", e)
        }
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
    private suspend fun verifyRegistrationWithBackend(responseJson: String) = withContext(Dispatchers.IO) {
        try {
            val authToken = getAuthToken()
            val credentialJson = parseRegistrationResponse(responseJson)
            val request = RegisterCredentialRequest(credential = credentialJson)

            val response = api.registerMfaCredential("Bearer $authToken", request)

            if (!response.isSuccessful) {
                Timber.e("Registration verification failed: ${response.code()} - ${response.message()}")
                throw MfaException("Failed to verify registration with server: ${response.code()}")
            }

            val result = response.body()
            if (result?.success != true) {
                Timber.e("Backend rejected credential registration")
                throw MfaException("Backend rejected credential registration")
            }

            Timber.d("Registration verified successfully, credential ID: ${result.credentialId}")
        } catch (e: MfaException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Error verifying registration with backend")
            throw MfaException("Failed to verify registration", e)
        }
    }

    /**
     * Verify authentication with backend
     */
    private suspend fun verifyAuthenticationWithBackend(responseJson: String) = withContext(Dispatchers.IO) {
        try {
            val authToken = getAuthToken()
            val assertionJson = parseAuthenticationResponse(responseJson)
            val request = AuthenticateRequest(credential = assertionJson)

            val response = api.authenticateMfa("Bearer $authToken", request)

            if (!response.isSuccessful) {
                Timber.e("Authentication verification failed: ${response.code()} - ${response.message()}")
                throw MfaException("Failed to verify authentication with server: ${response.code()}")
            }

            val result = response.body()
            if (result?.success != true) {
                Timber.e("Backend rejected authentication")
                throw MfaException("Authentication rejected by server")
            }

            Timber.d("Authentication verified successfully")
        } catch (e: MfaException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Error verifying authentication with backend")
            throw MfaException("Failed to verify authentication", e)
        }
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

    /**
     * Get authentication token from secure storage
     */
    private suspend fun getAuthToken(): String {
        return secureStore.read(AUTH_TOKEN_KEY)
            ?: throw MfaException("User not authenticated - no auth token found")
    }

    /**
     * Parse registration response and convert to backend-compatible JSON format
     */
    private fun parseRegistrationResponse(responseJson: String): JsonObject {
        return try {
            val json = JSONObject(responseJson)
            JsonObject().apply {
                addProperty("id", json.optString("id", ""))
                addProperty("type", "public-key")

                val response = json.optJSONObject("response")
                if (response != null) {
                    add("response", JsonObject().apply {
                        addProperty("clientDataJSON", response.optString("clientDataJSON", ""))
                        addProperty("attestationObject", response.optString("attestationObject", ""))
                    })
                }

                // Add transports if available
                val transports = json.optJSONArray("transports")
                if (transports != null) {
                    val transportsList = com.google.gson.JsonArray()
                    for (i in 0 until transports.length()) {
                        transportsList.add(transports.getString(i))
                    }
                    add("transports", transportsList)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing registration response")
            throw MfaException("Failed to parse registration response", e)
        }
    }

    /**
     * Parse authentication response and convert to backend-compatible JSON format
     */
    private fun parseAuthenticationResponse(responseJson: String): JsonObject {
        return try {
            val json = JSONObject(responseJson)
            JsonObject().apply {
                addProperty("id", json.optString("id", ""))
                addProperty("type", "public-key")

                val response = json.optJSONObject("response")
                if (response != null) {
                    add("response", JsonObject().apply {
                        addProperty("clientDataJSON", response.optString("clientDataJSON", ""))
                        addProperty("authenticatorData", response.optString("authenticatorData", ""))
                        addProperty("signature", response.optString("signature", ""))

                        val userHandle = response.optString("userHandle")
                        if (userHandle.isNotEmpty()) {
                            addProperty("userHandle", userHandle)
                        }
                    })
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing authentication response")
            throw MfaException("Failed to parse authentication response", e)
        }
    }

    companion object {
        private const val PREF_CREDENTIAL_IDS = "fido2_credential_ids"
        private const val AUTH_TOKEN_KEY = "auth_token"
    }
}

