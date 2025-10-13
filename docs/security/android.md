# Android Security

Mobile app security implementation and best practices.

## Security Layers

### 1. Network Security

**HTTPS Enforcement**:
```xml
<!-- network_security_config.xml -->
<base-config cleartextTrafficPermitted="false">
    <trust-anchors>
        <certificates src="system" />
    </trust-anchors>
</base-config>
```

**Certificate Pinning**:
```xml
<domain-config>
    <domain includeSubdomains="true">api.your-domain.com</domain>
    <pin-set expiration="2026-01-01">
        <pin digest="SHA-256">PIN_1</pin>
        <pin digest="SHA-256">PIN_2</pin>
    </pin-set>
</domain-config>
```

### 2. Data Protection

**Encrypted Token Storage**:
```kotlin
class SecureTokenStorage(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun saveToken(key: String, token: String) {
        encryptedPrefs.edit { putString(key, token) }
    }
}
```

**Features**:
- AES-256-GCM encryption
- Android Keystore integration
- Hardware-backed when available
- Auto-expiration

### 3. Code Protection

**ProGuard/R8 Obfuscation**:
```kotlin
// build.gradle.kts
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

**Features**:
- Code obfuscation
- Symbol stripping
- Log removal
- Resource shrinking

### 4. Runtime Protection

**Security Manager**:
```kotlin
object SecurityManager {
    fun isRooted(): Boolean {
        // Check for su binary
        // Check for root management apps
        // Try executing su command
    }
    
    fun isTampered(context: Context): Boolean {
        // Verify APK signature
    }
    
    fun isDebuggerAttached(): Boolean {
        return Debug.isDebuggerConnected()
    }
}
```

**Detects**:
- Root access
- App tampering
- Debugger attachment
- Emulator environment

## Security Checklist

- [ ] HTTPS enforced
- [ ] Certificate pinning enabled
- [ ] Tokens encrypted (EncryptedSharedPreferences)
- [ ] ProGuard/R8 enabled
- [ ] Root detection active
- [ ] Tamper detection active
- [ ] Backups disabled
- [ ] Debug logs removed

## Attack Prevention

| Attack | Mitigation |
|--------|------------|
| **MITM** | Certificate pinning |
| **Token theft** | Encrypted storage |
| **Reverse engineering** | ProGuard obfuscation |
| **Root exploitation** | Root detection |
| **App tampering** | Signature verification |

---

**See Also**:
- [Security Overview](overview.md)
- [Certificate Pinning](../features/certificate-pinning.md)
- [Backend Security](backend.md)

