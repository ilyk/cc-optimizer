# Certificate Pinning Setup

## Why Certificate Pinning?

Certificate pinning prevents Man-in-the-Middle (MITM) attacks by ensuring the app only trusts specific SSL certificates, even if an attacker installs a rogue Certificate Authority on the device.

## Are Certificate Pins Secret?

**NO** - Certificate pins are **PUBLIC** information. They are SHA-256 hashes of the server's public key, which is already public by design. Anyone can extract them by connecting to your HTTPS server.

## Automatic Pin Extraction (Recommended)

### Method 1: Gradle Task (Easiest)

```bash
# Automatically fetch pins from your backend server
./gradlew getCertificatePins -PBACKEND_URL=https://your-backend.com

# This will:
# 1. Connect to your server
# 2. Extract certificate pins
# 3. Save to local.properties (gitignored)
```

### Method 2: Environment Variables

```bash
# Set environment variables before building
export BACKEND_URL=https://your-backend.com
export CERT_PIN_1=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=
export CERT_PIN_2=BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=

# Build release
./gradlew assembleRelease
```

### Method 3: Manual Extraction

```bash
# Extract primary pin from your server
echo | openssl s_client -connect your-backend.com:443 2>/dev/null | \
  openssl x509 -pubkey -noout | \
  openssl rsa -pubin -outform der 2>/dev/null | \
  openssl dgst -sha256 -binary | \
  openssl enc -base64
```

Then add to `local.properties`:
```properties
CERT_PIN_1=your_primary_pin_here
CERT_PIN_2=your_backup_pin_here
```

## Production Build Enforcement

**CRITICAL:** The app WILL NOT build for production without certificate pins.

```
# This WILL FAIL:
./gradlew assembleRelease

# Error: "PRODUCTION BUILD BLOCKED - CERTIFICATE PINS REQUIRED"
```

You MUST provide pins via one of these methods:
1. Run `./gradlew getCertificatePins` first
2. Set `CERT_PIN_1` and `CERT_PIN_2` in `local.properties`
3. Set `CERT_PIN_1` and `CERT_PIN_2` environment variables

## Why Two Pins?

You need **at least 2 pins**:
1. **Primary Pin**: Current certificate
2. **Backup Pin**: Intermediate CA or backup certificate

This prevents the app from breaking when you rotate certificates. Google recommends having a backup pin at all times.

## Certificate Rotation

When updating your SSL certificate:

1. **Before rotation**: Add new certificate's pin as CERT_PIN_2
2. **During rotation**: Both pins will work
3. **After rotation**: Remove old pin in next app release

This ensures users with the old app version can still connect.

## CI/CD Integration

### GitHub Actions Example

```yaml
- name: Get Certificate Pins
  run: ./gradlew getCertificatePins -PBACKEND_URL=${{ secrets.BACKEND_URL }}
  
- name: Build Release APK
  env:
    CERT_PIN_1: ${{ secrets.CERT_PIN_1 }}
    CERT_PIN_2: ${{ secrets.CERT_PIN_2 }}
  run: ./gradlew assembleRelease
```

### GitLab CI Example

```yaml
build:
  script:
    - export BACKEND_URL=$BACKEND_URL
    - ./gradlew getCertificatePins
    - ./gradlew assembleRelease
  variables:
    BACKEND_URL: "https://your-backend.com"
```

## Backend Endpoint (Optional)

Your backend can serve pins at `/api/security/certificate-pins`:

```bash
curl https://your-backend.com/api/security/certificate-pins

# Response:
{
  "pins": [
    "sha256/AAAA...===",
    "sha256/BBBB...==="
  ],
  "note": "Certificate pins are public information"
}
```

This is safe because pins are public.

## Troubleshooting

### "PRODUCTION BUILD BLOCKED" Error

**Solution:** Run `./gradlew getCertificatePins` or manually set pins.

### "Certificate pinning failure" at Runtime

**Causes:**
1. Server certificate changed (you need to update pins)
2. Wrong pins configured
3. Testing with localhost (use HTTP for local testing)

**Solution:** Re-run `./gradlew getCertificatePins` to get fresh pins.

### "Unable to connect to server" during getCertificatePins

**Causes:**
1. Server not running
2. Wrong URL
3. Firewall blocking connection

**Solution:** Ensure backend is accessible at the URL, then retry.

## Security Best Practices

✅ **DO:**
- Always have 2+ pins (primary + backup)
- Update pins before certificate expires
- Test pin updates in staging first
- Store pins in local.properties (gitignored)

❌ **DON'T:**
- Commit pins to version control (they're in local.properties for a reason)
- Use only one pin (app will break on cert rotation)
- Disable pinning in production
- Hard-code pins in source code (use BuildConfig)

## File Locations

```
local.properties          # Contains pins (gitignored)
build.gradle.kts          # Reads pins, fails build if missing
RetrofitClient.kt         # Uses pins from BuildConfig
build-logic/              # Contains getCertificatePins task
```

## Summary

1. **Pins are PUBLIC** - Safe to share
2. **Build will FAIL** without pins in production - Intentional security
3. **Use ./gradlew getCertificatePins** - Easiest method
4. **Always have 2 pins** - Prevents app breakage on cert rotation

