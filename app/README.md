# Credit Card Optimizer - Android App

Native Android app for optimizing credit card payments.

## Quick Start

1. **Open in Android Studio**
2. **Sync Gradle**
3. **Configure backend URL** in `RetrofitClient.kt`:
   ```kotlin
   private const val BASE_URL = "http://10.0.2.2:3000/"  // Emulator
   ```
4. **Run** on device/emulator

## Documentation

Complete documentation is available in the `/docs` directory:

- **[Getting Started](../docs/getting-started/quick-start.md)** - Setup guide
- **[Android Architecture](../docs/architecture/android-app.md)** - App architecture
- **[Features](../docs/features/)** - Feature documentation
- **[Security](../docs/security/android.md)** - Security implementation
- **[Development Guide](../docs/development/guide.md)** - Developer workflow

## Requirements

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 26+ (Android 8.0+)
- Running backend server

## Features

- ✅ Plaid integration for bank connectivity
- ✅ AI-powered statement date detection (95% success rate)
- ✅ Smart payment calculation with business day handling
- ✅ Dark mode (Light/Dark/Auto)
- ✅ Offline support (Room database)
- ✅ Smart caching (100x faster subsequent loads)
- ✅ Payment reminders
- ✅ Certificate pinning
- ✅ Hardware-backed encryption

## Tech Stack

- **Language**: Kotlin 1.9.20+
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM
- **Networking**: Retrofit + OkHttp
- **Database**: Room
- **Security**: EncryptedSharedPreferences, Certificate Pinning
- **Min SDK**: 26
- **Target SDK**: 34

## Building

### Debug Build

```bash
./gradlew assembleDebug
```

### Release Build

Requires certificate pins:

```bash
./gradlew getCertificatePins -PBACKEND_URL=https://your-backend.com
./gradlew assembleRelease
```

See [Certificate Pinning Guide](../docs/features/certificate-pinning.md)

## Testing

```bash
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest    # Instrumented tests
./gradlew ktlintCheck             # Linting
```

## Security

- **Security Score**: A (92/100)
- **Network**: HTTPS + Certificate Pinning
- **Storage**: AES-256-GCM encryption
- **Code**: ProGuard/R8 obfuscation
- **Runtime**: Root/Tamper detection

See [Android Security](../docs/security/android.md)

## Project Structure

```
app/src/main/java/com/ccoptimizer/app/
├── MainActivity.kt
├── business/          # Business logic
├── data/             # Data layer
├── ui/               # UI layer
├── security/         # Security features
└── utils/            # Utilities
```

## Configuration

### Backend URL

Edit `app/src/main/java/com/ccoptimizer/app/data/api/RetrofitClient.kt`:

- **Emulator**: `http://10.0.2.2:3000/`
- **Physical Device**: `http://YOUR_LOCAL_IP:3000/`
- **Production**: `https://api.your-domain.com/`

### Network Security

Production builds require certificate pinning in `network_security_config.xml`.

Automatically configured via:
```bash
./gradlew getCertificatePins -PBACKEND_URL=...
```

## Support

- [Documentation](../docs/)
- [FAQ](../docs/development/faq.md)
- [Troubleshooting](../docs/development/troubleshooting.md)

---

For complete documentation, see [/docs](../docs/)

