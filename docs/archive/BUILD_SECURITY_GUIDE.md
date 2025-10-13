# 🔒 Automated Certificate Pinning Build System

## Overview

**Certificate pins are PUBLIC** (SHA-256 hashes of public keys), so we can safely:
- Fetch them automatically from the server
- Share them through endpoints  
- Store them in local files (gitignored)

**Production builds REQUIRE certificate pins or they FAIL** - This is intentional security.

---

## Quick Start

### Automatic Method (Recommended)

```bash
# Step 1: Start your backend server
cd backend
npm start

# Step 2: Auto-fetch certificate pins
cd ../app
./gradlew getCertificatePins -PBACKEND_URL=https://your-backend.com

# Step 3: Build production APK
./gradlew assembleRelease
```

Done! Pins are auto-extracted and saved to `local.properties`.

---

## How It Works

### 1. Gradle Task Auto-Fetches Pins

```kotlin
// build-logic/get-certificate-pins.gradle.kts
tasks.register("getCertificatePins") {
    // Connects to your HTTPS server
    // Extracts certificate chain
    // Calculates SHA-256 pins
    // Saves to local.properties
}
```

**Usage:**
```bash
./gradlew getCertificatePins -PBACKEND_URL=https://your-backend.com
```

**Output:**
```
=== Certificate Pins Found ===
Pin 1: sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=
Pin 2: sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=
==============================

Saved pins to local.properties
```

### 2. Production Build Validates Pins

```kotlin
// app/build.gradle.kts (release build)
if (certPin1.isEmpty() || certPin2.isEmpty()) {
    throw GradleException("""
        ╔═════════════════════════════════════════╗
        ║  PRODUCTION BUILD BLOCKED               ║
        ║  CERTIFICATE PINS REQUIRED              ║
        ╚═════════════════════════════════════════╝
        
        Run: ./gradlew getCertificatePins
    """)
}
```

**Result:** Production builds CANNOT proceed without pins.

### 3. Runtime Uses Pins from BuildConfig

```kotlin
// RetrofitClient.kt
private val certificatePinner = CertificatePinner.Builder().apply {
    if (!BuildConfig.DEBUG) {
        add(hostname, "sha256/${BuildConfig.CERT_PIN_1}")
        add(hostname, "sha256/${BuildConfig.CERT_PIN_2}")
    }
}.build()
```

**Result:** App only trusts pinned certificates.

---

## Build Scenarios

### ✅ Debug Build (Works Always)

```bash
./gradlew assembleDebug
# ✓ No certificate pinning required
# ✓ Can connect to localhost
# ✓ Builds immediately
```

### ❌ Production Build (No Server)

```bash
./gradlew assembleRelease

# ERROR:
# ╔═══════════════════════════════════════╗
# ║  PRODUCTION BUILD BLOCKED             ║
# ║  CERTIFICATE PINS REQUIRED            ║
# ╚═══════════════════════════════════════╝
```

**Expected behavior!** Cannot build production app without server.

### ✅ Production Build (With Auto-Fetch)

```bash
# Ensure backend is running
./gradlew getCertificatePins -PBACKEND_URL=https://your-backend.com
./gradlew assembleRelease

# ✓ Pins auto-fetched
# ✓ Saved to local.properties
# ✓ Build succeeds
```

### ✅ Production Build (Manual Pins)

```bash
# Add to local.properties:
echo "CERT_PIN_1=AAAA..." >> local.properties
echo "CERT_PIN_2=BBBB..." >> local.properties

./gradlew assembleRelease
# ✓ Build succeeds
```

### ✅ Production Build (Environment Variables)

```bash
export CERT_PIN_1=AAAA...
export CERT_PIN_2=BBBB...
./gradlew assembleRelease

# ✓ Build succeeds
```

---

## Pin Sources (Priority Order)

1. **Environment Variables** (highest priority)
   ```bash
   export CERT_PIN_1=xxx
   export CERT_PIN_2=yyy
   ```

2. **local.properties** (auto-generated)
   ```properties
   CERT_PIN_1=xxx
   CERT_PIN_2=yyy
   ```

3. **None** → Build FAILS ❌

---

## CI/CD Integration

### GitHub Actions

```yaml
name: Build Release APK

jobs:
  build:
    steps:
      - name: Checkout code
        uses: actions/checkout@v3
      
      - name: Auto-fetch certificate pins
        run: |
          cd app
          ./gradlew getCertificatePins -PBACKEND_URL=${{ secrets.BACKEND_URL }}
      
      - name: Build Release APK
        run: ./gradlew assembleRelease
```

**Secrets to set:**
- `BACKEND_URL`: Your production backend URL

**OR use environment variables:**
```yaml
      - name: Build Release APK
        env:
          CERT_PIN_1: ${{ secrets.CERT_PIN_1 }}
          CERT_PIN_2: ${{ secrets.CERT_PIN_2 }}
        run: ./gradlew assembleRelease
```

### GitLab CI

```yaml
build-release:
  script:
    - cd app
    - ./gradlew getCertificatePins -PBACKEND_URL=$BACKEND_URL
    - ./gradlew assembleRelease
  variables:
    BACKEND_URL: "https://your-backend.com"
```

### Jenkins

```groovy
pipeline {
  stages {
    stage('Get Pins') {
      steps {
        sh './gradlew getCertificatePins -PBACKEND_URL=${env.BACKEND_URL}'
      }
    }
    stage('Build') {
      steps {
        sh './gradlew assembleRelease'
      }
    }
  }
}
```

---

## Manual Pin Extraction

If you prefer manual control:

```bash
# Extract primary certificate pin
openssl s_client -servername your-backend.com \
  -connect your-backend.com:443 </dev/null 2>/dev/null | \
  openssl x509 -pubkey -noout | \
  openssl rsa -pubin -outform der 2>/dev/null | \
  openssl dgst -sha256 -binary | \
  openssl enc -base64

# Output: AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=
```

Then add to `local.properties`:
```properties
CERT_PIN_1=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=
CERT_PIN_2=BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=
```

---

## Backend Endpoint (Optional)

You can expose pins via API (safe because they're public):

```javascript
// backend/src/routes/security.js
router.get('/api/security/certificate-pins', (req, res) => {
  res.json({
    pins: ["sha256/AAA...", "sha256/BBB..."],
    note: "Pins are public information"
  });
});
```

Mobile apps or CI/CD can fetch pins:
```bash
curl https://your-backend.com/api/security/certificate-pins
```

---

## Certificate Rotation

### Before Rotating Certificate

1. Generate new certificate
2. Extract new pin
3. Add as `CERT_PIN_2` (backup)
4. Release app update with both pins
5. Wait for users to update
6. Rotate certificate
7. Remove old pin in next release

### Process

```bash
# Step 1: Before cert rotation
./gradlew getCertificatePins  # Gets current pins
# Pin 1: Current cert (soon to expire)
# Pin 2: Intermediate CA

# Step 2: Get new certificate's pin
# (Before installing new cert on server)
openssl x509 -in new-cert.pem -pubkey -noout | \
  openssl rsa -pubin -outform der | \
  openssl dgst -sha256 -binary | \
  openssl enc -base64

# Step 3: Update local.properties
CERT_PIN_1=current_cert_pin     # Keep old
CERT_PIN_2=new_cert_pin          # Add new

# Step 4: Build and release app update
./gradlew assembleRelease

# Step 5: After users update, rotate server cert
# Users with new app: ✓ Works (has new pin)
# Users with old app: ✓ Still works (has old pin briefly)

# Step 6: Next release, remove old pin
```

---

## Security Properties

### ✅ What We Achieved

1. **Automatic Pin Extraction**
   - No manual copy-paste errors
   - Always gets latest pins
   - Works in CI/CD pipelines

2. **Build-Time Validation**
   - Cannot build production without pins
   - Prevents accidental insecure builds
   - Clear error messages

3. **Runtime Enforcement**
   - App refuses non-pinned certificates
   - Blocks MITM even with rogue CAs
   - Works even on compromised devices

4. **Rotation Support**
   - Two pins support seamless rotation
   - No app downtime during cert updates
   - Users don't notice cert changes

### ❌ What We Prevented

1. **Accidental Production Builds**
   - Cannot deploy without pins
   - Explicit error if pins missing
   - Developer must take action

2. **Stale Pins**
   - Auto-fetch gets current cert
   - No outdated pins in builds
   - CI/CD always fresh

3. **MITM Attacks**
   - Rogue CA certificates blocked
   - Corporate SSL inspection blocked
   - Public WiFi attacks blocked

---

## Troubleshooting

### Build Fails: "CERTIFICATE PINS REQUIRED"

**Cause:** No pins configured for production build.

**Solution:**
```bash
./gradlew getCertificatePins -PBACKEND_URL=https://your-backend.com
```

### getCertificatePins Fails: "Unable to connect"

**Cause:** Backend not accessible.

**Solutions:**
1. Ensure backend is running
2. Check URL is correct (include https://)
3. Verify firewall allows connection
4. Check SSL certificate is valid

### Runtime: "Certificate pinning failure"

**Cause:** Server certificate doesn't match pins.

**Solutions:**
1. Re-fetch pins: `./gradlew getCertificatePins`
2. Rebuild app with new pins
3. Check if server certificate rotated

### Debug Build Can't Connect to Production

**Intended behavior!** Debug builds don't have pins, use HTTP for local testing.

**Solution:** Build release variant if testing production backend.

---

## Files Reference

```
app/
├── build.gradle.kts                      # Pin validation logic
├── build-logic/
│   └── get-certificate-pins.gradle.kts  # Auto-fetch task
├── src/main/java/.../RetrofitClient.kt  # Pin usage
└── README_CERTIFICATE_PINS.md           # Detailed docs

local.properties                          # Auto-generated pins (gitignored)
.gitignore                                # Excludes local.properties
```

---

## Summary

### Certificate Pins Are Public ✅
- SHA-256 of public key
- Already publicly accessible
- Safe to store/share/fetch

### Production Requires Pins ✅
- Build FAILS without pins
- Prevents insecure deployments
- Clear error messages

### Automatic Extraction ✅
- `./gradlew getCertificatePins`
- Connects to server
- Saves to local.properties
- Ready to build

### CI/CD Ready ✅
- Works in pipelines
- Environment variable support
- Auto-fetch from server
- No manual steps

**Result:** Secure certificate pinning with zero manual effort! 🎉
