# SSL Certificate Pinning Guide

## What is Certificate Pinning?

Certificate pinning prevents Man-in-the-Middle (MITM) attacks by hardcoding your backend's SSL certificate in the app. Even if an attacker has a valid SSL certificate, the app will only trust YOUR specific certificate.

---

## ✅ Implementation Status

**Status**: ✅ **IMPLEMENTED** (configurable)

The app now supports certificate pinning, configured via BuildConfig:
- **Development**: Disabled (no pinning needed for local testing)
- **Production**: Enabled when you provide certificate pins

---

## 🔧 How to Enable (Production)

### Step 1: Get Your Certificate Pin

Once your backend is deployed with SSL, get the pin:

```bash
# Replace your-domain.com with your actual backend domain
openssl s_client -connect your-domain.com:443 -servername your-domain.com < /dev/null 2>/dev/null | \
openssl x509 -pubkey -noout | \
openssl pkey -pubin -outform der | \
openssl dgst -sha256 -binary | \
openssl enc -base64
```

**Output**: `AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=`

### Step 2: Get Backup Pin

Get pin for backup certificate (in case you rotate certificates):

```bash
# This could be your CA's certificate or a backup cert
openssl s_client -connect your-domain.com:443 -showcerts < /dev/null 2>/dev/null | \
sed -n '/BEGIN CERTIFICATE/,/END CERTIFICATE/p' | \
openssl x509 -pubkey -noout | \
openssl pkey -pubin -outform der | \
openssl dgst -sha256 -binary | \
openssl enc -base64
```

### Step 3: Update build.gradle.kts

```kotlin
release {
    buildConfigField("String", "BACKEND_URL", "\"https://your-domain.com/\"")
    buildConfigField("String", "SSL_CERTIFICATE_PINS", 
        "\"sha256/YOUR_PRIMARY_PIN_HERE,sha256/YOUR_BACKUP_PIN_HERE\""
    )
}
```

### Step 4: Rebuild

```bash
./gradlew clean
./gradlew assembleRelease
```

---

## 🎯 Pin Format

**Correct Format**:
```
sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=
```

**Multiple Pins** (recommended):
```
sha256/PRIMARY_PIN,sha256/BACKUP_PIN
```

**Example**:
```kotlin
buildConfigField("String", "SSL_CERTIFICATE_PINS", 
    "\"sha256/k3v+DqKvHqnX8F1z8K5Y2Q3pN5c8X5m5p+pN5c8X5m=,sha256/backup_pin_here\""
)
```

---

## ⚠️ Important Notes

### Always Use Multiple Pins!

Include at least 2 pins:
1. **Primary**: Your current certificate
2. **Backup**: Your CA's certificate or future certificate

Why? If you only pin one certificate and it expires, your app will stop working!

### Certificate Rotation

When rotating certificates:
1. Add new certificate pin to BuildConfig (keep old one)
2. Release app update
3. Wait for 90%+ users to update
4. Remove old pin

### Testing

**Test pinning works**:
```bash
# With correct pin: Works ✅
# With wrong pin: Fails with SSLPeerUnverifiedException ✅
```

---

## 🔍 How It Works

**Code**:
```kotlin
// RetrofitClient.kt
private fun getCertificatePinner(): CertificatePinner? {
    val pins = BuildConfig.SSL_CERTIFICATE_PINS
    
    if (pins.isBlank()) return null  // Development mode
    
    val hostname = URL(BASE_URL).host
    val builder = CertificatePinner.Builder()
    
    pins.split(",").forEach { pin ->
        builder.add(hostname, pin.trim())
    }
    
    return builder.build()
}

// Applied to OkHttpClient
okHttpClient.certificatePinner(getCertificatePinner())
```

**Runtime**:
1. App makes HTTPS request
2. Server presents SSL certificate
3. OkHttpClient extracts public key
4. Calculates SHA-256 hash
5. Compares with pinned values
6. ✅ Match → Allow connection
7. ❌ No match → Reject with exception

---

## 🚨 Troubleshooting

### "CertificatePin mismatch"

**Cause**: Wrong pin in BuildConfig  
**Fix**: Re-generate pin from production server

### "Unable to connect"

**Cause**: Certificate pinning enabled but wrong hostname  
**Fix**: Ensure hostname in pin matches BASE_URL

### "Works in debug but not release"

**Cause**: Release has pinning enabled  
**Fix**: Verify production pins are correct

---

## 🧪 Testing Certificate Pinning

### Local Testing

1. **Use ngrok** to expose local backend with SSL:
```bash
ngrok http 3000
# Get pin from ngrok's certificate
```

2. **Use production backend**:
```bash
# Get pin from your deployed backend
```

3. **Test with wrong pin**:
```kotlin
// Should fail with certificate error
buildConfigField("String", "SSL_CERTIFICATE_PINS", "\"sha256/WRONG_PIN_HERE\"")
```

---

## 📝 Example Production Configuration

### build.gradle.kts

```kotlin
buildTypes {
    debug {
        buildConfigField("String", "BACKEND_URL", "\"http://10.0.2.2:3000/\"")
        buildConfigField("String", "SSL_CERTIFICATE_PINS", "\"\"")  // No pinning
    }
    
    release {
        isMinifyEnabled = true
        buildConfigField("String", "BACKEND_URL", "\"https://api.ccoptimizer.com/\"")
        buildConfigField("String", "SSL_CERTIFICATE_PINS", 
            "\"" +
            "sha256/your_certificate_pin_here," +
            "sha256/your_backup_pin_here" +
            "\""
        )
    }
}
```

---

## 🔐 Security Benefits

### Without Pinning
```
App → MITM Attacker (with valid cert) → Your Backend
❌ Attacker can intercept all traffic
```

### With Pinning
```
App → MITM Attacker (with valid cert) → X
✅ App rejects: "Certificate doesn't match pinned value"
```

**Protection**: Prevents MITM even with valid SSL certificates

---

## 🎯 Recommendation

### For MVP Launch
- ⬜ **Optional** (not critical)
- Use if security is paramount
- Skip if deploying quickly

### For Production (Long-term)
- ✅ **Recommended** (security best practice)
- Enable after backend deployment
- Include in app update

---

## 📚 Resources

- [OkHttp Certificate Pinning](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-certificate-pinner/)
- [OWASP Mobile Security](https://owasp.org/www-project-mobile-security-testing-guide/)
- [Android Security Best Practices](https://developer.android.com/training/articles/security-tips)

---

## ✅ Current Status

**Implementation**: ✅ Complete  
**Configuration**: Flexible (disabled by default)  
**Documentation**: Complete  
**Production Ready**: Yes (add pins when deploying)

**To enable**: Just add your SSL certificate pins to `build.gradle.kts`

---

*Certificate pinning implemented and ready to activate!*

