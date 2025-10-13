# Configuration Guide

Complete configuration reference for the Credit Card Payment Optimizer.

## Environment Variables

### Backend Configuration

All backend configuration is done via environment variables in the `.env` file.

#### Required Variables

```env
# Plaid API Configuration
PLAID_CLIENT_ID=your_client_id_here
PLAID_SECRET=your_sandbox_secret_here
PLAID_ENV=sandbox

# JWT Authentication
JWT_SECRET=your-super-secret-jwt-key-min-32-characters-long

# Database
DATABASE_URL=./data/cc_optimizer.db
DB_TYPE=sqlite
```

#### Optional Variables

```env
# Server
PORT=3000
NODE_ENV=development

# CORS
CORS_ORIGIN=http://localhost:3000,http://10.0.2.2:3000

# Logging
LOG_LEVEL=info

# LLM (AI Statement Detection)
LLM_PROVIDER=none
OPENAI_API_KEY=sk-your-key
OPENAI_MODEL=gpt-4o-mini
ANTHROPIC_API_KEY=sk-ant-your-key
ANTHROPIC_MODEL=claude-3-haiku-20240307

# Monitoring
SENTRY_DSN=https://your-sentry-dsn
MIXPANEL_TOKEN=your-mixpanel-token
```

## Detailed Configuration

### Plaid Configuration

#### PLAID_CLIENT_ID
- **Description**: Your Plaid application client ID
- **Required**: Yes
- **Format**: UUID-like string
- **Example**: `5f9b2c1a3d4e5f6a7b8c9d0e`
- **Get it**: https://dashboard.plaid.com/

#### PLAID_SECRET
- **Description**: Your Plaid API secret
- **Required**: Yes
- **Format**: Long alphanumeric string
- **Security**: NEVER commit to git
- **Environments**:
  - `sandbox`: For development/testing (free)
  - `development`: For development with real banks
  - `production`: For production use

#### PLAID_ENV
- **Description**: Plaid environment to use
- **Required**: Yes
- **Options**:
  - `sandbox`: Test environment (recommended for development)
  - `development`: Development with real bank data
  - `production`: Production environment
- **Default**: `sandbox`

**Test Credentials (Sandbox)**:
- Username: `user_good`
- Password: `pass_good`
- MFA code: `1234`

### Authentication Configuration

#### JWT_SECRET
- **Description**: Secret key for signing JWT tokens
- **Required**: Yes
- **Format**: At least 32 characters
- **Security**: Must be cryptographically random
- **Generate**:
  ```bash
  openssl rand -base64 32
  # or
  node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"
  ```

**Token Expiration**: 7 days (configurable in `authService.js`)

### Database Configuration

#### DATABASE_URL
- **Description**: Path to SQLite database or PostgreSQL connection string
- **Required**: Yes
- **SQLite** (development):
  ```env
  DATABASE_URL=./data/cc_optimizer.db
  DB_TYPE=sqlite
  ```
- **PostgreSQL** (production):
  ```env
  DATABASE_URL=postgresql://user:password@host:5432/database
  DB_TYPE=postgres
  ```

#### DB_TYPE
- **Description**: Database type
- **Required**: Yes
- **Options**: `sqlite` | `postgres`
- **Default**: `sqlite`

### Server Configuration

#### PORT
- **Description**: Port for backend server
- **Required**: No
- **Default**: `3000`
- **Range**: 1024-65535
- **Note**: Use ports > 1024 to avoid needing root

#### NODE_ENV
- **Description**: Node.js environment
- **Required**: No
- **Options**:
  - `development`: Development mode (verbose logging)
  - `production`: Production mode (minimal logging, optimizations)
  - `test`: Testing mode
- **Default**: `development`
- **Affects**:
  - Logging verbosity
  - Error messages
  - Security headers
  - Performance optimizations

### CORS Configuration

#### CORS_ORIGIN
- **Description**: Comma-separated list of allowed origins
- **Required**: No
- **Format**: `origin1,origin2,origin3`
- **Default**: Allow all (development only)
- **Examples**:
  ```env
  # Development (Android emulator + localhost)
  CORS_ORIGIN=http://localhost:3000,http://10.0.2.2:3000
  
  # Production (specific domains)
  CORS_ORIGIN=https://app.example.com,https://www.example.com
  ```

**Security**: Always restrict CORS in production!

### Logging Configuration

#### LOG_LEVEL
- **Description**: Minimum logging level
- **Required**: No
- **Options**: `error` | `warn` | `info` | `debug` | `verbose`
- **Default**: `info`
- **Levels**:
  - `error`: Only errors
  - `warn`: Errors + warnings
  - `info`: Errors + warnings + info
  - `debug`: Everything + debug messages
  - `verbose`: Everything including trace

**Recommendation**:
- Development: `debug`
- Production: `info` or `warn`

### LLM Configuration

#### LLM_PROVIDER
- **Description**: AI provider for statement detection
- **Required**: No
- **Options**: `none` | `openai` | `anthropic`
- **Default**: `none`
- **Cost**: ~$0.0005 per card (one-time)

#### OpenAI Configuration
```env
LLM_PROVIDER=openai
OPENAI_API_KEY=sk-your-key-here
OPENAI_MODEL=gpt-4o-mini
```

**Models**:
- `gpt-4o-mini`: Fast, cheap, recommended (default)
- `gpt-4o`: More accurate, more expensive
- `gpt-4-turbo`: Alternative

**Get API key**: https://platform.openai.com/

#### Anthropic Configuration
```env
LLM_PROVIDER=anthropic
ANTHROPIC_API_KEY=sk-ant-your-key-here
ANTHROPIC_MODEL=claude-3-haiku-20240307
```

**Models**:
- `claude-3-haiku-20240307`: Fast, cheap, recommended (default)
- `claude-3-sonnet-20240229`: More accurate
- `claude-3-opus-20240229`: Most accurate, expensive

**Get API key**: https://console.anthropic.com/

### Monitoring Configuration

#### SENTRY_DSN
- **Description**: Sentry error tracking DSN
- **Required**: No
- **Format**: `https://key@sentry.io/project-id`
- **Get it**: https://sentry.io/
- **Benefits**:
  - Automatic error tracking
  - Stack traces
  - User context
  - Performance monitoring

#### MIXPANEL_TOKEN
- **Description**: Mixpanel analytics token
- **Required**: No
- **Format**: Alphanumeric string
- **Get it**: https://mixpanel.com/
- **Benefits**:
  - User analytics
  - Event tracking
  - Funnel analysis
  - Retention metrics

## Android App Configuration

### Build Configuration

Located in `app/build.gradle.kts`:

```kotlin
android {
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.ccoptimizer.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }
    
    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

### Backend URL Configuration

Located in `app/src/main/java/com/ccoptimizer/app/data/api/RetrofitClient.kt`:

```kotlin
private const val BASE_URL = "http://10.0.2.2:3000/"  // Emulator
// private const val BASE_URL = "http://192.168.1.100:3000/"  // Physical device
// private const val BASE_URL = "https://api.your-domain.com/"  // Production
```

**Configuration by environment**:

| Environment | Device | URL |
|------------|--------|-----|
| **Development** | Emulator | `http://10.0.2.2:3000/` |
| **Development** | Physical Device | `http://YOUR_LOCAL_IP:3000/` |
| **Production** | Any | `https://api.your-domain.com/` |

### Network Security Configuration

Located in `app/src/main/res/xml/network_security_config.xml`:

**Development** (debug builds):
```xml
<domain-config cleartextTrafficPermitted="true">
    <domain includeSubdomains="true">10.0.2.2</domain>
    <domain includeSubdomains="true">localhost</domain>
</domain-config>
```

**Production** (release builds):
```xml
<domain-config cleartextTrafficPermitted="false">
    <domain includeSubdomains="true">api.your-domain.com</domain>
    <pin-set expiration="2026-01-01">
        <pin digest="SHA-256">YOUR_PIN_1</pin>
        <pin digest="SHA-256">YOUR_PIN_2</pin>
    </pin-set>
</domain-config>
```

### Certificate Pinning

**Extract pins from backend**:
```bash
./gradlew getCertificatePins -PBACKEND_URL=https://api.your-domain.com
```

Pins are saved to `local.properties`:
```properties
pin1=sha256/AAAA....
pin2=sha256/BBBB....
```

### ProGuard Configuration

Located in `app/proguard-rules.pro`:

**Keep classes**:
```proguard
# Data models
-keep class com.ccoptimizer.app.data.models.** { *; }

# Retrofit
-keepattributes Signature
-keep class retrofit2.** { *; }

# Plaid
-keep class com.plaid.** { *; }
```

### String Resources

Located in `app/src/main/res/values/strings.xml`:

**All user-facing text** should be in strings.xml for i18n:
```xml
<resources>
    <string name="app_name">Credit Card Optimizer</string>
    <string name="connect_plaid">Connect with Plaid</string>
    <string name="payment_recommendation">Payment Recommendation</string>
    <!-- ... -->
</resources>
```

## Configuration Examples

### Development Environment

**.env**:
```env
PLAID_CLIENT_ID=your_sandbox_client_id
PLAID_SECRET=your_sandbox_secret
PLAID_ENV=sandbox
JWT_SECRET=dev-secret-key-min-32-chars-long
DATABASE_URL=./data/cc_optimizer_dev.db
DB_TYPE=sqlite
PORT=3000
NODE_ENV=development
CORS_ORIGIN=http://localhost:3000,http://10.0.2.2:3000
LOG_LEVEL=debug
LLM_PROVIDER=none
```

**RetrofitClient.kt**:
```kotlin
private const val BASE_URL = "http://10.0.2.2:3000/"
```

### Staging Environment

**.env**:
```env
PLAID_CLIENT_ID=your_dev_client_id
PLAID_SECRET=your_dev_secret
PLAID_ENV=development
JWT_SECRET=staging-secret-key-min-32-chars-long
DATABASE_URL=postgresql://user:pass@host:5432/ccoptimizer_staging
DB_TYPE=postgres
PORT=3000
NODE_ENV=production
CORS_ORIGIN=https://staging.example.com
LOG_LEVEL=info
LLM_PROVIDER=openai
OPENAI_API_KEY=sk-your-key
```

**RetrofitClient.kt**:
```kotlin
private const val BASE_URL = "https://staging-api.example.com/"
```

### Production Environment

**.env**:
```env
PLAID_CLIENT_ID=your_prod_client_id
PLAID_SECRET=your_prod_secret
PLAID_ENV=production
JWT_SECRET=production-secret-key-min-32-chars-long
DATABASE_URL=postgresql://user:pass@host:5432/ccoptimizer_prod
DB_TYPE=postgres
PORT=3000
NODE_ENV=production
CORS_ORIGIN=https://app.example.com
LOG_LEVEL=warn
LLM_PROVIDER=openai
OPENAI_API_KEY=sk-your-prod-key
SENTRY_DSN=https://your-sentry-dsn
MIXPANEL_TOKEN=your-mixpanel-token
```

**RetrofitClient.kt**:
```kotlin
private const val BASE_URL = "https://api.example.com/"
```

## Security Best Practices

### Environment Variables

✅ **DO**:
- Store secrets in `.env` file
- Use different secrets per environment
- Rotate secrets regularly
- Use strong JWT secrets (32+ characters)
- Restrict CORS in production

❌ **DON'T**:
- Commit `.env` to git
- Share secrets via email/Slack
- Use same secrets across environments
- Use weak/predictable secrets
- Leave CORS wide open in production

### Android App

✅ **DO**:
- Use certificate pinning in production
- Enable ProGuard obfuscation
- Store tokens in EncryptedSharedPreferences
- Use HTTPS only
- Validate SSL certificates

❌ **DON'T**:
- Hardcode API keys in app
- Allow cleartext traffic in production
- Store tokens in plain SharedPreferences
- Disable certificate validation
- Skip security features

## Validation

### Validate Backend Configuration

```bash
# Check environment
node -e "console.log(require('dotenv').config().parsed)"

# Test connection
curl http://localhost:3000/health

# Verify Plaid credentials
npm run test-plaid  # If you create this script
```

### Validate Android Configuration

```bash
# Check build configuration
./gradlew :app:dependencies

# Verify backend URL
grep BASE_URL app/src/main/java/com/ccoptimizer/app/data/api/RetrofitClient.kt

# Test network security
adb shell dumpsys package com.ccoptimizer.app | grep -A 20 "networkSecurityConfig"
```

## Troubleshooting

### Configuration Not Loading

**Backend**:
```bash
# Check .env file exists
ls -la backend/.env

# Check dotenv is loaded
node -e "require('dotenv').config(); console.log(process.env.PLAID_CLIENT_ID)"
```

**Android**:
```bash
# Rebuild after changing configuration
./gradlew clean
./gradlew assembleDebug
```

### Wrong Environment

**Check environment**:
```javascript
// In server.js
console.log('Environment:', process.env.NODE_ENV);
console.log('Plaid Env:', process.env.PLAID_ENV);
```

**Force environment**:
```bash
NODE_ENV=production npm start
```

---

**Next**: [Quick Start Guide](quick-start.md) | [Development Guide](../development/guide.md)

