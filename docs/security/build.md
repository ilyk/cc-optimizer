# Build Security

Automated security enforcement during build process.

## Automated Certificate Pinning

### Gradle Task

Extract certificate pins from running backend:

```bash
./gradlew getCertificatePins -PBACKEND_URL=https://api.your-domain.com
```

**Implementation** (`app/build-logic/get-certificate-pins.gradle.kts`):
```kotlin
tasks.register("getCertificatePins") {
    doLast {
        val backendUrl = project.findProperty("BACKEND_URL") as String?
            ?: System.getenv("BACKEND_URL")
            ?: throw GradleException("BACKEND_URL not set")
        
        // Fetch pins from /api/v1/security/certificate-pins
        val pins = fetchCertificatePins(backendUrl)
        
        // Save to local.properties
        File("local.properties").appendText("""
            pin1=${pins[0]}
            pin2=${pins[1]}
        """.trimIndent())
        
        println("=== Certificate Pins Found ===")
        println("Pin 1: ${pins[0]}")
        println("Pin 2: ${pins[1]}")
    }
}
```

### Build Enforcement

Production builds require pins:

```kotlin
// build.gradle.kts
buildTypes {
    release {
        val pin1 = project.findProperty("pin1") as String?
        val pin2 = project.findProperty("pin2") as String?
        
        if (pin1.isNullOrBlank() || pin2.isNullOrBlank()) {
            throw GradleException(
                "Certificate pins required for release builds.\n" +
                "Run: ./gradlew getCertificatePins -PBACKEND_URL=..."
            )
        }
        
        // Generate network_security_config.xml with pins
        generateNetworkSecurityConfig(pin1, pin2)
    }
}
```

### CI/CD Integration

```yaml
# GitHub Actions example
- name: Extract Certificate Pins
  run: |
    ./gradlew getCertificatePins -PBACKEND_URL=${{ secrets.BACKEND_URL }}
    
- name: Build Release APK
  run: |
    ./gradlew assembleRelease
```

## Security Benefits

✅ **Prevents**:
- Accidental insecure builds
- Missing certificate pins
- Stale/incorrect pins
- Manual configuration errors

✅ **Ensures**:
- Pins always match deployed backend
- Build fails without running backend
- Fresh pins for every build
- Automated validation

## Workflow

```
1. Deploy backend to production
   ↓
2. Run: ./gradlew getCertificatePins -PBACKEND_URL=...
   ↓
3. Pins extracted and saved to local.properties
   ↓
4. Run: ./gradlew assembleRelease
   ↓
5. Build validates pins exist
   ↓
6. network_security_config.xml generated with pins
   ↓
7. APK built with certificate pinning enabled ✅
```

---

**See Also**:
- [Certificate Pinning Guide](../../app/README_CERTIFICATE_PINS.md)
- [Security Overview](overview.md)
- [Android Security](android.md)

