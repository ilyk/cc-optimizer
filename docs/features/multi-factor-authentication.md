# Multi-Factor Authentication (MFA)

Comprehensive MFA system with biometric, FIDO2 hardware keys, and passkeys.

## Overview

The Credit Card Optimizer implements a flexible, user-configurable MFA system that supports multiple authentication methods:

1. **Biometric Authentication** (Fingerprint/Face) - Must-have, optional for users
2. **FIDO2 Hardware Keys** (Yubikey, Google Titan, etc.)
3. **Passkeys** (WebAuthn platform authenticator)

**Key Features**:
- ✅ **Configurable security levels** per action
- ✅ **Interactive onboarding tutorial**
- ✅ **Graceful fallbacks** when methods unavailable
- ✅ **User choice** - All methods are optional
- ✅ **Bank-grade security** with WebAuthn standard

## Supported Authentication Methods

### 1. Biometric Authentication

**What it is**: Fingerprint or face recognition using device hardware

**Benefits**:
- ⚡ Fast and convenient
- 🔒 Secure (biometric data never leaves device)
- 📱 Works offline
- 💪 Hardware-backed encryption

**Requirements**:
- Android 8.0+ (API 26+)
- Device with fingerprint sensor or face recognition
- User must enroll biometrics in device settings

**Implementation**:
```kotlin
mfaManager.authenticateWithBiometric(
    activity = this,
    action = MfaAction.APP_UNLOCK,
    onSuccess = { /* Authenticated */ },
    onError = { error -> /* Handle error */ }
)
```

### 2. FIDO2 Hardware Keys

**What it is**: Physical USB/NFC security keys (Yubikey, Google Titan, etc.)

**Benefits**:
- 🛡️ Phishing-resistant
- 🔑 Separate physical device
- 🏦 Bank-grade security
- 🌐 Industry standard (WebAuthn/FIDO2)

**Supported Keys**:
- Yubikey (all models with FIDO2)
- Google Titan Security Key
- Feitian ePass FIDO
- Any FIDO2/WebAuthn compliant key

**Requirements**:
- Android 14+ (API 34+) for Credential Manager
- FIDO2-compatible hardware key
- USB-C or NFC connection

**Implementation**:
```kotlin
fido2Manager.registerCredential(
    activity = this,
    userId = user.id,
    userName = user.email,
    userDisplayName = user.name,
    requireUserVerification = true
)
```

### 3. Passkeys

**What it is**: Passwordless authentication using platform authenticator

**Benefits**:
- 🔐 No passwords to remember
- 🔄 Synced across devices (via Google Password Manager)
- 🚀 Modern standard
- 🛡️ Phishing-resistant

**Requirements**:
- Android 14+ (API 34+)
- Google account with Password Manager
- Device with biometric or PIN/pattern

**Implementation**:
```kotlin
// Passkeys use same Credential Manager API as FIDO2
fido2Manager.registerCredential(
    requireResidentKey = true  // Makes it a passkey
)
```

## Security Levels

Users can configure different security levels for different actions:

| Level | Description | Use Cases |
|-------|-------------|-----------|
| **NONE** | No additional auth | Low-risk actions |
| **BIOMETRIC** | Fingerprint or face | Quick unlock, view data |
| **HARDWARE_KEY** | FIDO2 key required | Connect bank, add card |
| **BIOMETRIC + HARDWARE** | Both required | High-value transactions, settings |
| **PASSKEY** | Passkey auth | Alternative to password |

### Configurable Actions

Users can set security requirements for each action:

```kotlin
enum class MfaAction {
    APP_UNLOCK,                    // Default: BIOMETRIC
    LOGIN,                         // Default: NONE
    CONNECT_BANK,                  // Default: BIOMETRIC
    ADD_CARD,                      // Default: BIOMETRIC_AND_HARDWARE
    REMOVE_CARD,                   // Default: BIOMETRIC
    VIEW_SENSITIVE_DATA,           // Default: BIOMETRIC
    CHANGE_SECURITY_SETTINGS,      // Default: BIOMETRIC_AND_HARDWARE
    EXPORT_DATA                    // Default: BIOMETRIC_AND_HARDWARE
}
```

**Example Configuration**:
- **App unlock after minimization**: Biometric only (fast, convenient)
- **Add credit card**: Biometric + Yubikey (high security)
- **Export data**: Biometric + Yubikey (maximum security)
- **View recommendations**: None (low risk)

## User Experience Flow

### First-Time Setup (Onboarding)

```
1. User logs in for first time
   ↓
2. Show MFA onboarding screen
   "Secure Your Account"
   ↓
3. Step 1: Introduction
   - Explain MFA benefits
   - Show available methods
   - User taps "Get Started"
   ↓
4. Step 2: Biometric Setup
   - Check if biometric available
   - If yes: Show switch to enable
   - User toggles ON
   - Prompt for fingerprint/face
   - Verify and enable
   ✅ "Biometric authentication active!"
   ↓
5. Step 3: Hardware Key Setup
   - Explain hardware keys
   - List supported keys (Yubikey, etc.)
   - User can enable/skip
   - If enabled: Can register key in Settings
   ↓
6. Step 4: Summary
   - Show enabled methods
   - Explain security levels
   - User taps "Complete Setup"
   ↓
7. Done! MFA configured
```

### Using MFA in App

**Scenario: User wants to add a card**

```
1. User taps "Add Card"
   ↓
2. App checks security requirements
   Security Level: BIOMETRIC_AND_HARDWARE
   ↓
3. Step 1: Biometric verification
   - Show fingerprint/face prompt
   - "Adding credit card"
   - "Verify your identity"
   - User provides biometric
   ✅ Biometric verified
   ↓
4. Step 2: Hardware key verification
   - Show "Insert your Yubikey"
   - User inserts USB key or taps NFC
   - System prompts for PIN (if configured)
   - WebAuthn challenge/response
   ✅ Hardware key verified
   ↓
5. Both verifications passed
   - Proceed to add card
   - Connect via Plaid
```

## Interactive Tutorial

The app includes an interactive tutorial that shows users:

### How to Enable Biometric
```
Tutorial step:
┌─────────────────────────────────────┐
│  👆 Enable Biometric Auth           │
│                                     │
│  1. Toggle the switch →            │
│  2. Place your finger on sensor    │
│  3. Verify it's you                │
│  ✅ Done! Future unlocks are fast  │
└─────────────────────────────────────┘
```

### How to Register Yubikey
```
Tutorial step:
┌─────────────────────────────────────┐
│  🔑 Add Hardware Security Key       │
│                                     │
│  1. Tap "Register Hardware Key"     │
│  2. Insert Yubikey into USB-C port │
│     OR                               │
│     Tap Yubikey to NFC area         │
│  3. Follow system prompts           │
│  ✅ Key registered!                 │
└─────────────────────────────────────┘
```

### How to Create Passkey
```
Tutorial step:
┌─────────────────────────────────────┐
│  🔐 Create Passkey                  │
│                                     │
│  1. Tap "Create Passkey"            │
│  2. Choose where to save:           │
│     • This device                   │
│     • Google Password Manager       │
│  3. Verify with biometric/PIN       │
│  ✅ Passkey created & synced!       │
└─────────────────────────────────────┘
```

## Configuration UI

### MFA Settings Screen

```kotlin
MfaSettingsScreen {
    // Overview Card
    "MFA Active" / "MFA Not Configured"
    "Enabled: Biometric, Hardware Key"
    
    // Authentication Methods
    ├─ Biometric Authentication
    │  ├─ [Switch] Enable/Disable
    │  └─ Status: "Fingerprint or Face"
    │
    ├─ Hardware Security Keys
    │  ├─ [Switch] Enable/Disable
    │  └─ [Button] Register Hardware Key
    │
    └─ Passkeys
       ├─ [Switch] Enable/Disable
       └─ [Button] Create Passkey
    
    // Security Levels per Action
    ├─ Unlock App → Biometric
    ├─ Login → None
    ├─ Connect Bank → Biometric
    ├─ Add Card → Biometric + Hardware Key
    ├─ Remove Card → Biometric
    ├─ View Sensitive Data → Biometric
    ├─ Change Security Settings → Biometric + Hardware Key
    └─ Export Data → Biometric + Hardware Key
    
    // Tutorial
    [Button] "View Setup Tutorial Again"
}
```

### Tap to Configure
```
User taps "Add Card" security level
   ↓
Dialog shows all security levels:
┌─────────────────────────────────────┐
│  Choose security level:             │
│                                     │
│  ⭘ None                             │
│    No additional authentication     │
│                                     │
│  ● Biometric (selected)             │
│    Fingerprint or face required     │
│                                     │
│  ⭘ Hardware Key                     │
│    FIDO2 hardware key required      │
│                                     │
│  ⭘ Biometric + Hardware Key         │
│    Both required                    │
│                                     │
│  ⭘ Passkey                          │
│    Passkey authentication           │
│                                     │
│  [Cancel]                           │
└─────────────────────────────────────┘
```

## Backend API

### Endpoints

**Generate Challenge**:
```
POST /api/v1/mfa/challenge
Authorization: Bearer <token>

Response:
{
  "challenge": "base64url_encoded_challenge",
  "expiresAt": 1234567890
}
```

**Register Credential**:
```
POST /api/v1/mfa/register
Authorization: Bearer <token>

Body:
{
  "registrationResponse": {
    "id": "credential_id",
    "rawId": "base64_encoded",
    "response": {
      "attestationObject": "base64_encoded",
      "clientDataJSON": "base64_encoded"
    },
    "type": "public-key"
  }
}

Response:
{
  "credentialId": "...",
  "success": true
}
```

**Authenticate**:
```
POST /api/v1/mfa/authenticate
Authorization: Bearer <token>

Body:
{
  "authenticationResponse": {
    "id": "credential_id",
    "rawId": "base64_encoded",
    "response": {
      "authenticatorData": "base64_encoded",
      "clientDataJSON": "base64_encoded",
      "signature": "base64_encoded"
    },
    "type": "public-key"
  }
}

Response:
{
  "success": true
}
```

**Get MFA Status**:
```
GET /api/v1/mfa/status
Authorization: Bearer <token>

Response:
{
  "biometric": true,
  "fido2": true,
  "passkey": false
}
```

## Security Considerations

### Biometric Security
- ✅ Biometric data never leaves device
- ✅ Hardware-backed encryption
- ✅ Android Keystore integration
- ✅ No server-side biometric storage

### FIDO2/WebAuthn Security
- ✅ Challenge-response protocol
- ✅ Public key cryptography
- ✅ Counter-based replay prevention
- ✅ Origin verification
- ✅ Signature verification
- ✅ No secrets transmitted

### Passkey Security
- ✅ Synced securely via Google Password Manager
- ✅ Encrypted end-to-end
- ✅ Phishing-resistant
- ✅ No password to steal

## Implementation Details

### Android Biometric Prompt

```kotlin
val promptInfo = BiometricPrompt.PromptInfo.Builder()
    .setTitle("Unlock App")
    .setSubtitle("Verify your identity")
    .setDescription("Use biometric to unlock")
    .setNegativeButtonText("Cancel")
    .setAllowedAuthenticators(
        BiometricManager.Authenticators.BIOMETRIC_STRONG
    )
    .build()

biometricPrompt.authenticate(promptInfo)
```

### Credential Manager (Android 14+)

```kotlin
val credentialManager = CredentialManager.create(context)

// Register
val request = CreatePublicKeyCredentialRequest(requestJson)
val result = credentialManager.createCredential(request, activity)

// Authenticate
val getRequest = GetCredentialRequest(listOf(publicKeyOption))
val credential = credentialManager.getCredential(getRequest, activity)
```

### WebAuthn Request Format

**Registration**:
```json
{
  "rp": {
    "id": "ccoptimizer.ilyk.im",
    "name": "Credit Card Optimizer"
  },
  "user": {
    "id": "user_123",
    "name": "user@example.com",
    "displayName": "John Doe"
  },
  "challenge": "random_base64url_challenge",
  "pubKeyCredParams": [
    { "type": "public-key", "alg": -7 },   // ES256
    { "type": "public-key", "alg": -257 }  // RS256
  ],
  "authenticatorSelection": {
    "authenticatorAttachment": "platform",
    "requireResidentKey": false,
    "userVerification": "required"
  },
  "timeout": 60000
}
```

## Example Configurations

### Conservative User (Convenience over Security)
```
APP_UNLOCK → BIOMETRIC
LOGIN → NONE
CONNECT_BANK → BIOMETRIC
ADD_CARD → BIOMETRIC
REMOVE_CARD → BIOMETRIC
VIEW_SENSITIVE_DATA → NONE
CHANGE_SECURITY_SETTINGS → BIOMETRIC
EXPORT_DATA → BIOMETRIC
```

### Balanced User (Default)
```
APP_UNLOCK → BIOMETRIC
LOGIN → NONE
CONNECT_BANK → BIOMETRIC
ADD_CARD → BIOMETRIC_AND_HARDWARE
REMOVE_CARD → BIOMETRIC
VIEW_SENSITIVE_DATA → BIOMETRIC
CHANGE_SECURITY_SETTINGS → BIOMETRIC_AND_HARDWARE
EXPORT_DATA → BIOMETRIC_AND_HARDWARE
```

### Security-Conscious User (Maximum Security)
```
APP_UNLOCK → BIOMETRIC
LOGIN → PASSKEY
CONNECT_BANK → BIOMETRIC_AND_HARDWARE
ADD_CARD → BIOMETRIC_AND_HARDWARE
REMOVE_CARD → BIOMETRIC_AND_HARDWARE
VIEW_SENSITIVE_DATA → BIOMETRIC
CHANGE_SECURITY_SETTINGS → BIOMETRIC_AND_HARDWARE
EXPORT_DATA → BIOMETRIC_AND_HARDWARE
```

## Graceful Fallbacks

### When Biometric Unavailable
```
If (biometric not available):
    - Don't show biometric option in settings
    - Use password for authentication
    - Suggest hardware key or passkey instead
```

### When Hardware Key Not Present
```
If (hardware key required but not present):
    - Show "Insert your Yubikey" message
    - Allow fallback to password + biometric
    - Provide option to skip for now
```

### When Android < 14
```
If (Android version < 14):
    - Disable FIDO2 and Passkey options
    - Show "Requires Android 14+" message
    - Only offer biometric authentication
    - Suggest OS update for advanced features
```

## Testing

### Test Biometric

```kotlin
@Test
fun testBiometricAuthentication() {
    val manager = MfaManager(context)
    
    // Check availability
    val available = manager.canUseBiometric()
    assertTrue(available)
    
    // Authenticate
    manager.authenticateWithBiometric(
        activity = mockActivity,
        action = MfaAction.APP_UNLOCK,
        onSuccess = { verified = true },
        onError = { fail(it) }
    )
    
    assertTrue(verified)
}
```

### Test FIDO2 (Emulator)

```kotlin
@Test
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
fun testFido2Registration() {
    val manager = Fido2Manager(context)
    
    runBlocking {
        val result = manager.registerCredential(
            activity = mockActivity,
            userId = "test_user",
            userName = "test@example.com",
            userDisplayName = "Test User"
        )
        
        assertTrue(result.isSuccess)
    }
}
```

## User Flow Examples

### Example 1: First Time User

```
User opens app for first time
  → Shows MFA onboarding
    → Step 1: Intro (explains benefits)
    → Step 2: Enable biometric (user enables)
    → Step 3: Hardware keys (user skips)
    → Step 4: Summary
  → Completes onboarding
  → MFA tutorial marked as seen
  → Can access MFA settings anytime
```

### Example 2: Existing User Adds Yubikey

```
User already has biometric enabled
  → Opens Settings
  → Taps "Multi-Factor Authentication"
  → Taps "Hardware Security Keys" switch
  → Switch enables
  → Taps "Register Hardware Key"
  → Insert Yubikey prompt appears
  → User inserts Yubikey
  → System prompts for PIN
  → Registration complete
  ✅ Yubikey registered
  → Can now use for high-security actions
```

### Example 3: High-Security Transaction

```
User wants to add new credit card
  → Taps "Add Card"
  → App checks security level: BIOMETRIC_AND_HARDWARE
  → Step 1: Biometric prompt
    → "Adding credit card - Verify identity"
    → User provides fingerprint
    ✅ Biometric verified
  → Step 2: Hardware key prompt
    → "Insert your Yubikey"
    → User inserts key
    → System challenge/response
    ✅ Hardware key verified
  → Both verifications passed
  → Proceed to Plaid connection
```

## Best Practices

### For Users

1. **Enable biometric** for convenience (app unlock, quick auth)
2. **Add hardware key** for high-value actions (adding cards, exporting data)
3. **Configure security levels** based on your risk tolerance
4. **Keep Yubikey safe** - it's a physical key to your account
5. **Use passkeys** if you want passwordless login

### For Developers

1. **Always provide fallback** options
2. **Check availability** before showing options
3. **Handle errors gracefully** with clear messages
4. **Test on multiple devices** (different Android versions)
5. **Validate server-side** - never trust client-only verification

## Compatibility

| Feature | Min Android Version | Devices |
|---------|-------------------|---------|
| **Biometric** | 8.0 (API 26) | All with fingerprint/face |
| **FIDO2/Hardware Keys** | 14 (API 34) | All |
| **Passkeys** | 14 (API 34) | All with Google account |

### Android 8-13
- ✅ Biometric authentication
- ❌ FIDO2 hardware keys
- ❌ Passkeys

### Android 14+
- ✅ Biometric authentication
- ✅ FIDO2 hardware keys
- ✅ Passkeys

## Cost

**All MFA features are FREE**:
- ✅ Biometric: Built into Android
- ✅ FIDO2: Standard protocol
- ✅ Passkeys: Free with Google account
- ✅ Backend: No additional services needed

**Hardware costs** (optional):
- Yubikey 5C NFC: ~$55 (one-time purchase)
- Google Titan Security Key: ~$30
- Feitian ePass: ~$20

## Future Enhancements

- [ ] SMS/Email MFA codes (traditional 2FA)
- [ ] TOTP support (Google Authenticator, Authy)
- [ ] Backup codes for account recovery
- [ ] Trusted devices list
- [ ] Login history and device management
- [ ] Geofencing (require MFA outside trusted locations)

---

**See Also**:
- [Android Security](../security/android.md)
- [Backend Security](../security/backend.md)
- [User Guide](../getting-started/mfa-setup-guide.md)

**External Resources**:
- [WebAuthn Guide](https://webauthn.guide/)
- [FIDO Alliance](https://fidoalliance.org/)
- [Android Biometric API](https://developer.android.com/training/sign-in/biometric-auth)
- [Credential Manager](https://developer.android.com/training/sign-in/passkeys)

