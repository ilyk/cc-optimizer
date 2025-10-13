# Certificate Pinning

Automated HTTPS certificate pinning for network security.

## Overview

Certificate pinning prevents man-in-the-middle (MITM) attacks by ensuring the app only trusts specific SSL certificates from your backend server.

## How It Works

**Without pinning**:
- App trusts any valid certificate
- Attacker can present rogue certificate
- MITM attack succeeds ❌

**With pinning**:
- App only trusts specific certificate pins
- Attacker's certificate rejected
- MITM attack fails ✅

## Implementation

### Network Security Configuration

`app/src/main/res/xml/network_security_config.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- Production: Block cleartext, pin certificates -->
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">api.your-domain.com</domain>
        <pin-set expiration="2026-01-01">
            <pin digest="SHA-256">AAAA...BASE64...</pin>
            <pin digest="SHA-256">BBBB...BASE64...</pin>
        </pin-set>
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </domain-config>
    
    <!-- Development: Allow localhost -->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">localhost</domain>
    </domain-config>
</network-security-config>
```

### Automated Pin Extraction

Gradle task extracts pins from running backend:

```bash
./gradlew getCertificatePins -PBACKEND_URL=https://api.your-domain.com
```

**Output**:
```
=== Certificate Pins Found ===
Pin 1: sha256/AAAA...
Pin 2: sha256/BBBB...
Saved to local.properties
```

### Build Enforcement

Production builds require pins:

```kotlin
// app/build.gradle.kts
android {
    buildTypes {
        release {
            // Fail if no pins found
            val pin1 = project.findProperty("pin1") as String?
            val pin2 = project.findProperty("pin2") as String?
            
            if (pin1.isNullOrBlank() || pin2.isNullOrBlank()) {
                throw GradleException(
                    "Certificate pins required for release builds. " +
                    "Run: ./gradlew getCertificatePins -PBACKEND_URL=..."
                )
            }
        }
    }
}
```

## Pin Rotation

When SSL certificate changes:

1. **Deploy new backend** with new certificate
2. **Extract new pins**: `./gradlew getCertificatePins`
3. **Update network_security_config.xml** with both old and new pins
4. **Deploy new app** version
5. **Wait for users** to update
6. **Remove old pins** after sufficient adoption

**Best practice**: Always include 2 pins (current + backup).

## Testing

**Test pinning works**:
```bash
# Should succeed
adb install app/build/outputs/apk/release/app-release.apk
# Try connecting to backend
# Should work ✅

# Test with wrong certificate
# Should fail with SSL error ✅
```

**Test pin mismatch**:
```xml
<!-- Deliberately wrong pin for testing -->
<pin digest="SHA-256">wrongpinwrongpin==</pin>
```

Should result in:
```
javax.net.ssl.SSLPeerUnverifiedException: 
Certificate pinning failure!
```

## Security Benefits

- ✅ Prevents MITM attacks
- ✅ Prevents rogue CA certificates
- ✅ Prevents SSL stripping
- ✅ Prevents certificate substitution

## Debug vs Release

**Debug builds**:
- No certificate pinning (allows localhost)
- Cleartext traffic allowed
- System + user CAs trusted

**Release builds**:
- Certificate pinning enforced
- Cleartext traffic blocked
- Only system CAs trusted

---

**See Also**:
- [Build Security Guide](../../app/README_CERTIFICATE_PINS.md)
- [Network Security](../security/android.md#network-security)
- [Android Network Security Config](https://developer.android.com/training/articles/security-config)

