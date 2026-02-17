# Security Documentation

**Last Updated:** October 17, 2025

## Overview

CC Optimizer handles sensitive financial data. This document outlines our security architecture, threat model, and best practices.

## Threat Model

### In Scope

We protect against:

1. **Device loss/theft:** Encrypted storage ensures data is unreadable without device unlock
2. **Malware with file access:** Database and preferences encrypted at rest
3. **Network eavesdropping:** HTTPS-only with TLS 1.2+ and optional certificate pinning
4. **Log leakage:** Sensitive data redacted from logs and crash reports
5. **Unauthorized local access:** Biometric authentication for destructive actions

### Out of Scope (User Responsibility)

We cannot protect against:

- Compromised device (root/jailbreak with malware running as root)
- Screen recording or keylogging by other apps (requires device-level malware)
- Physical access to unlocked device
- User sharing their device unlock credentials

## Secrets Management Table

| Secret | Storage Location | Rotation Policy | Deleted by Wipe? |
|--------|-----------------|-----------------|------------------|
| Database encryption key | Android Keystore (hardware-backed) | Never (device-bound) | Yes (Keystore cleared) |
| Plaid access_token | EncryptedSharedPreferences | Manual (user re-link) | Yes |
| JWT auth token | EncryptedSharedPreferences | 7 days (auto-refresh) | Yes |
| Master encryption key | Android Keystore | Never (device-bound) | Yes |
| Telemetry endpoint URL | DataStore (encrypted) | User-controlled | Yes |

## Encryption

### Database Encryption (SQLCipher)

- **Algorithm:** AES-256-CBC (SQLCipher default)
- **Key derivation:** PBKDF2-HMAC-SHA512 with 256,000 iterations
- **Key storage:** 32-byte random key stored in Android Keystore, wrapped by hardware-backed master key
- **Database:** `credit_card_optimizer.db` is encrypted; raw queries return unreadable bytes without key

**Implementation:**
```kotlin
// DbKeyManager generates/retrieves 256-bit key from Keystore
val passphrase = DbKeyManager.getOrCreateKey(context)
val factory = SupportFactory(SQLiteDatabase.getBytes(passphrase))
Room.databaseBuilder(...).openHelperFactory(factory).build()
```

### Encrypted Shared Preferences

- **Algorithm:** AES-256-GCM for values, AES-256-SIV for keys
- **Key storage:** Android Keystore with hardware-backed MasterKey (Android 6+)
- **Files:** `user_preferences_encrypted` and `secure_token_prefs`

**Implementation:**
```kotlin
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

EncryptedSharedPreferences.create(
    context, "secure_token_prefs", masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

### Android Keystore

- **Hardware-backed:** On devices with Trusted Execution Environment (TEE) or Secure Element
- **Key never leaves Keystore:** Encryption/decryption operations happen inside secure hardware
- **Deletion on factory reset:** Keys are destroyed; data becomes unrecoverable

## Network Security

### HTTPS Everywhere

**NetworkSecurityConfig:**
```xml
<base-config cleartextTrafficPermitted="false">
  <trust-anchors>
    <certificates src="system" />
  </trust-anchors>
</base-config>
```

- Blocks HTTP traffic (except localhost in debug builds)
- TLS 1.2+ required
- Only system Certificate Authorities trusted (not user-installed CAs in production)

### Certificate Pinning (Optional)

**Important:** We pin ONLY our own backend. We do NOT pin Plaid's certificates (they rotate behind managed infrastructure).

Production builds support optional certificate pinning for our backend:

1. Generate pins: `./gradlew getCertificatePins`
2. Add to `local.properties`:
   ```
   CERT_PIN_1=sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=
   CERT_PIN_2=sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=
   ```
3. Build validates pins are present before release

Pins are SHA-256 hashes of the public key (SPKI) in base64.

**Not enabled by default** to avoid bricking the app if certificates rotate. Enable for high-security deployments.

## Logging & Observability

### Production Logging

- **No body logging:** OkHttp logging interceptor disabled in release builds
- **No sensitive fields:** Timber tree redacts PII even in debug

### Redaction

**RedactingTree** and **RedactingInterceptor** strip:

- Credit card numbers (regex pattern match)
- Balances, limits, amounts
- Dates (statement, due, payment)
- Auth tokens, API keys, Bearer headers
- Email, phone, SSN
- Plaid tokens and item IDs

**Example log (before redaction):**
```
Card: {"cardId":"123","last4":"4242","balance":1250.00,"authToken":"sk_live_abc123"}
```

**After redaction:**
```
Card: {"cardId":"***CARD_ID***","last4":"****","balance":"***BALANCE***","authToken":"***TOKEN***"}
```

### Crash Reporting

- **Default: DISABLED** in production (`CRASH_REPORTING_ENABLED=false`)
- If enabled, must exclude financial payloads (configure Sentry/Bugsnag to scrub fields)
- Recommendation: Only enable with strict PII filtering

## Authentication & Authorization

### Local Authentication

Destructive actions (delete all data) require:

- **BiometricPrompt:** Fingerprint, face unlock, or device credential (PIN/pattern/password)
- **Android API:** `androidx.biometric.BiometricPrompt`

### Backend Authentication (If Self-Hosting)

If using optional self-hosted backend:

- **JWT tokens:** Stored in `EncryptedSharedPreferences`
- **Token expiry:** 7-day default, validated on each request
- **Social auth:** Google/Facebook OAuth for convenience (optional)

## Data Retention

- **On device:** Data persists until user deletes or uninstalls app
- **Backups:** App excludes data from Android backups (`android:allowBackup="false"`)
- **Auto-delete:** No automatic expiry; user controls lifecycle

## Secure Deletion

**DataWiper** ensures complete removal:

1. `database.clearAllTables()` - Overwrites SQLCipher pages
2. `encryptedPrefs.edit().clear().apply()` - Deletes encrypted pref files
3. Clears cache dirs, Coil image cache, temp files
4. Cancels WorkManager jobs
5. Verification step confirms data is gone

**Note:** SQLCipher secure_delete pragma can be enabled for additional security (slight performance cost).

## Permissions

Minimal permissions requested:

- ✅ **INTERNET:** Required for Plaid Link and optional self-hosted sync
- ✅ **POST_NOTIFICATIONS:** For payment reminders (user consent required)
- ✅ **USE_BIOMETRIC:** For local authentication on delete

We do NOT request:

- ❌ Storage (Scoped Storage via SAF for exports only)
- ❌ Location
- ❌ Camera (unless QR code feature added)
- ❌ Contacts, SMS, Phone

## Build Security

### Obfuscation

Release builds use R8 with ProGuard rules:

- Obfuscates class/method names
- Removes unused code
- Shrinks APK size

### Signing

- **Debug:** Self-signed certificate (not for distribution)
- **Release:** Production keystore (keep `keystore.jks` secure, never commit)
- **Play App Signing:** Recommended for Google Play distribution

### Dependencies

- **Dependency scanning:** Run `./gradlew dependencyCheckAnalyze` to check for known CVEs
- **Update policy:** Patch security updates within 7 days of disclosure
- **Minimal dependencies:** Avoid bloated SDKs; prefer Jetpack/AndroidX

## Threat Scenarios & Mitigations

### Scenario 1: Lost/Stolen Device

**Threat:** Attacker has physical device  
**Mitigation:**
- Data encrypted at rest (requires device unlock)
- Biometric/device credential for destructive actions
- User can remotely wipe via Android Device Manager

### Scenario 2: Malicious App on Device

**Threat:** Malware tries to read app files  
**Mitigation:**
- Android sandbox isolates app data (private dir not readable by other apps)
- Encrypted storage even if sandbox bypassed (root exploit)
- No cleartext secrets in memory dumps (keys in Keystore)

### Scenario 3: Network MITM Attack

**Threat:** Attacker intercepts network traffic  
**Mitigation:**
- HTTPS-only (NetworkSecurityConfig)
- Optional certificate pinning
- No sensitive data in URLs (POST body only)

### Scenario 4: Log Leakage

**Threat:** Logs captured by debugging tools or crash reports  
**Mitigation:**
- RedactingTree strips PII
- No body logging in production
- Crash reporting disabled by default

### Scenario 5: Backup Extraction

**Threat:** Attacker extracts Android backup (adb backup)  
**Mitigation:**
- `android:allowBackup="false"` - No backups via ADB
- Cloud backup excluded via `dataExtractionRules`

## Responsible Disclosure

If you discover a security vulnerability:

1. **DO NOT** open a public GitHub issue
2. Email: **security@ccoptimizer.app** with:
   - Description of the vulnerability
   - Steps to reproduce
   - Impact assessment
   - Your contact information (for credit)

### Our Response SLA

- **Triage:** Within 5 business days
- **Initial response:** Within 48 hours
- **Critical patches:** Within 7 days
- **Moderate patches:** Within 30 days
- **Low-severity:** Within 90 days
- **Public disclosure:** After patch is released (coordinated disclosure)

We appreciate responsible disclosure and will credit researchers (with permission) in our security advisories.

### Hall of Fame

We appreciate security researchers who responsibly disclose issues. Contributors will be acknowledged here (with permission).

## Compliance

### GDPR (EU)

- **Data controller:** User (data stays on device)
- **Right to access:** Built-in (view in app)
- **Right to deletion:** Built-in ("Delete All Data")
- **Right to portability:** Built-in ("Export Data")

### CCPA (California)

- Similar rights as GDPR, covered by in-app features

### PCI-DSS

- **Not applicable:** We do not store full card numbers, CVVs, or process payments
- Only store metadata (issuer name, last 4 digits)

## Security Checklist (For Developers)

When contributing:

- [ ] Never log sensitive data (use Timber with RedactingTree)
- [ ] Never hardcode secrets (use BuildConfig or secure storage)
- [ ] Always use HTTPS for network calls
- [ ] Add unit tests for new security-critical code
- [ ] Run `./gradlew lint` and fix security warnings
- [ ] Update this doc if threat model changes

## Further Reading

- [Android Security Best Practices](https://developer.android.com/topic/security/best-practices)
- [OWASP Mobile Security Testing Guide](https://owasp.org/www-project-mobile-security-testing-guide/)
- [SQLCipher Documentation](https://www.zetetic.net/sqlcipher/sqlcipher-api/)

---

**Summary:** Military-grade encryption (AES-256), HTTPS-only, minimal permissions, redacted logs, open-source for audit. Your data is secure.

