# Installation Guide

Complete installation instructions for the Credit Card Payment Optimizer.

## Prerequisites

### Required Software

#### For Android Development
- **Android Studio**: Hedgehog (2023.1.1) or later
  - Download: https://developer.android.com/studio
- **JDK 17**: Required for Android development
  - Check version: `java -version`
  - Download: https://adoptium.net/
- **Android SDK**: API level 26+ (Android 8.0 Oreo)
  - Installed via Android Studio
- **Git**: For version control
  - Check: `git --version`

#### For Backend Development
- **Node.js 18+**: JavaScript runtime
  - Check version: `node --version`
  - Download: https://nodejs.org/
- **npm**: Node package manager (comes with Node.js)
  - Check version: `npm --version`

### Recommended Tools
- **Android Device or Emulator**: API 26+ for testing
- **Postman**: For API testing (optional)
- **VS Code**: For backend development (optional)

## Step 1: Clone the Repository

```bash
# Navigate to your projects directory
cd ~/projects

# Clone the repository
git clone <repository-url>
cd cc

# Verify structure
ls -la
```

You should see:
```
app/          # Android app
backend/      # Node.js backend
docs/         # Documentation
gradle/       # Gradle wrapper
build.gradle.kts
settings.gradle.kts
```

## Step 2: Backend Setup

### 2.1 Install Dependencies

```bash
cd backend
npm install
```

This installs:
- Express.js (web framework)
- Plaid SDK (bank integration)
- JWT & bcrypt (authentication)
- Winston (logging)
- And 30+ other dependencies

### 2.2 Get Plaid Credentials

1. **Sign up for Plaid**:
   - Go to https://dashboard.plaid.com/signup
   - Create a free account
   - Verify your email

2. **Create a new app**:
   - Click "Create App" in dashboard
   - App name: "Credit Card Optimizer"
   - Environment: Sandbox (for testing)

3. **Get credentials**:
   - Click on your app
   - Copy `client_id`
   - Copy `sandbox` secret
   - Note: Keep these secure!

### 2.3 Configure Environment

Create `.env` file in `backend/` directory:

```bash
# Copy example
cp .env.example .env

# Edit with your favorite editor
nano .env
# or
vim .env
# or
code .env
```

**Complete `.env` configuration**:

```env
# Plaid Configuration
PLAID_CLIENT_ID=your_client_id_here
PLAID_SECRET=your_sandbox_secret_here
PLAID_ENV=sandbox

# JWT Secret (generate a strong random string)
JWT_SECRET=your-super-secret-jwt-key-min-32-characters-long

# Database Configuration
DATABASE_URL=./data/cc_optimizer.db
DB_TYPE=sqlite

# Server Configuration
PORT=3000
NODE_ENV=development

# CORS (comma-separated allowed origins)
CORS_ORIGIN=http://localhost:3000,http://10.0.2.2:3000

# Logging
LOG_LEVEL=info

# Optional: LLM Configuration for AI Statement Detection
LLM_PROVIDER=none
# To enable OpenAI:
# LLM_PROVIDER=openai
# OPENAI_API_KEY=sk-your-key-here
# OPENAI_MODEL=gpt-4o-mini

# To enable Anthropic:
# LLM_PROVIDER=anthropic
# ANTHROPIC_API_KEY=sk-ant-your-key-here
# ANTHROPIC_MODEL=claude-3-haiku-20240307

# Optional: Monitoring (configure if using)
# SENTRY_DSN=https://your-sentry-dsn
# MIXPANEL_TOKEN=your-mixpanel-token
```

**Generate JWT Secret**:
```bash
# Linux/Mac
openssl rand -base64 32

# Node.js
node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"
```

### 2.4 Initialize Database

```bash
# Create data directory
mkdir -p data

# Start server (will auto-create database)
npm start
```

You should see:
```
Server running on port 3000
Environment: development
Plaid environment: sandbox
Database initialized successfully
Health check: http://localhost:3000/health
```

### 2.5 Test Backend

**Option 1: Browser**
```
http://localhost:3000/health
```

Should return:
```json
{
  "status": "ok",
  "message": "Credit Card Optimizer Backend is running",
  "environment": "sandbox",
  "apiVersion": "v1"
}
```

**Option 2: cURL**
```bash
curl http://localhost:3000/health
```

**Option 3: Test registration**
```bash
curl -X POST http://localhost:3000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "TestPass123"
  }'
```

Should return a JWT token.

## Step 3: Android App Setup

### 3.1 Open in Android Studio

1. **Launch Android Studio**
2. **File > Open**
3. Navigate to your project directory
4. Select the root folder (containing `build.gradle.kts`)
5. Click **OK**

### 3.2 Wait for Gradle Sync

First-time setup takes 5-10 minutes:
- Downloading Gradle wrapper
- Downloading dependencies
- Indexing project
- Building

**If Gradle sync fails**:
```bash
# In project root
./gradlew clean
./gradlew build --refresh-dependencies
```

### 3.3 Configure SDK

**Check SDK location**:
1. **File > Project Structure > SDK Location**
2. Verify Android SDK path is set
3. Verify JDK path is set (should be JDK 17)

**If SDK not found**:
1. **Tools > SDK Manager**
2. Install Android SDK Platform 34
3. Install Android SDK Build-Tools 34.0.0
4. Install Android Emulator (if using emulator)

### 3.4 Configure Backend URL

Edit `app/src/main/java/com/ccoptimizer/app/data/api/RetrofitClient.kt`:

```kotlin
object RetrofitClient {
    // For Android Emulator
    private const val BASE_URL = "http://10.0.2.2:3000/"
    
    // For Physical Device (use your computer's local IP)
    // private const val BASE_URL = "http://192.168.1.100:3000/"
    
    // For Production
    // private const val BASE_URL = "https://api.your-domain.com/"
    
    // ... rest of file
}
```

**Finding your local IP** (for physical device):
```bash
# Linux
ip addr show | grep "inet " | grep -v 127.0.0.1

# Mac
ifconfig | grep "inet " | grep -v 127.0.0.1

# Windows
ipconfig | findstr IPv4
```

### 3.5 Configure local.properties

Create `local.properties` in project root (if it doesn't exist):

```properties
# Android SDK location
sdk.dir=/Users/yourname/Library/Android/sdk

# Or on Linux
# sdk.dir=/home/yourname/Android/Sdk

# Or on Windows
# sdk.dir=C\:\\Users\\yourname\\AppData\\Local\\Android\\Sdk
```

**Note**: This file is gitignored and specific to your machine.

### 3.6 Sync Project

1. **File > Sync Project with Gradle Files**
2. Wait for sync to complete
3. Check for errors in Build panel

## Step 4: Run the App

### 4.1 Set Up Device/Emulator

**Option A: Use Emulator**

1. **Tools > Device Manager**
2. **Create Device**:
   - Device: Pixel 6
   - System Image: API 34 (Android 14)
   - AVD Name: Pixel_6_API_34
3. Click **Finish**
4. Click **Play** button to start emulator

**Option B: Use Physical Device**

1. **Enable Developer Options** on your Android device:
   - Settings > About Phone
   - Tap "Build Number" 7 times
2. **Enable USB Debugging**:
   - Settings > Developer Options
   - Enable "USB Debugging"
3. **Connect device via USB**
4. **Accept debugging prompt** on device

**Verify device connection**:
```bash
adb devices
```

Should show:
```
List of devices attached
ABC123XYZ    device
```

### 4.2 Build and Run

**Option 1: Android Studio**
1. Select device/emulator from dropdown
2. Click **Run** button (▶️) or press `Shift + F10`
3. Wait for build to complete
4. App launches automatically

**Option 2: Command Line**
```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Or build and install in one command
./gradlew installDebug
```

APK location: `app/build/outputs/apk/debug/app-debug.apk`

### 4.3 First Launch

1. **App opens** to main screen
2. **Tap "Connect with Plaid"**
3. **Plaid Link opens** (web view)
4. **Select a bank** (e.g., "Chase")
5. **Enter Plaid sandbox credentials**:
   - Username: `user_good`
   - Password: `pass_good`
6. **Select accounts** (choose credit card accounts)
7. **Submit**
8. **App displays** your connected credit cards

## Step 5: Verify Installation

### 5.1 Backend Verification

```bash
# Health check
curl http://localhost:3000/health

# Create user
curl -X POST http://localhost:3000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"TestPass123"}'

# Login
curl -X POST http://localhost:3000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"TestPass123"}'
```

### 5.2 App Verification

**Check logs**:
```bash
# Android Studio: View > Tool Windows > Logcat
# Or via command line:
adb logcat | grep "CC_OPTIMIZER"
```

**Verify features**:
- [ ] App launches successfully
- [ ] Plaid Link opens
- [ ] Can connect bank account
- [ ] Credit cards appear
- [ ] Payment recommendations shown
- [ ] Dark mode toggle works
- [ ] Pull-to-refresh works

## Troubleshooting

### Backend Issues

**Error: Port 3000 already in use**
```bash
# Find process using port
lsof -i :3000

# Kill process
kill -9 <PID>

# Or use different port
PORT=3001 npm start
```

**Error: Cannot find module**
```bash
# Reinstall dependencies
rm -rf node_modules package-lock.json
npm install
```

**Error: Database locked**
```bash
# Stop server
# Delete database
rm backend/data/cc_optimizer.db
# Restart server (will recreate)
npm start
```

### Android App Issues

**Error: Gradle sync failed**
```bash
# Invalidate caches
# Android Studio: File > Invalidate Caches > Invalidate and Restart

# Or clean project
./gradlew clean
```

**Error: Cannot resolve symbol 'R'**
```bash
# Clean and rebuild
./gradlew clean
./gradlew build
```

**Error: INSTALL_FAILED_UPDATE_INCOMPATIBLE**
```bash
# Uninstall old version
adb uninstall com.ccoptimizer.app

# Reinstall
./gradlew installDebug
```

**Error: Connection refused (emulator)**
- Use `http://10.0.2.2:3000/` instead of `localhost:3000`
- Ensure backend is running
- Check firewall settings

**Error: Connection refused (physical device)**
- Use your computer's IP: `http://192.168.x.x:3000/`
- Ensure device and computer on same WiFi
- Check firewall allows connections on port 3000

### Plaid Issues

**Error: invalid_credentials**
- Using wrong Plaid environment
- Check `.env` has `PLAID_ENV=sandbox`
- Use sandbox credentials: `user_good` / `pass_good`

**Error: invalid_client**
- Wrong `PLAID_CLIENT_ID` or `PLAID_SECRET`
- Re-copy from Plaid dashboard
- Ensure no extra spaces

## Next Steps

1. **Read Documentation**: [Quick Start Guide](quick-start.md)
2. **Explore Features**: [Features Documentation](../features/)
3. **Review Security**: [Security Overview](../security/overview.md)
4. **API Reference**: [API Documentation](../api/overview.md)
5. **Development**: [Development Guide](../development/guide.md)

## Optional Setup

### Enable LLM (AI Statement Detection)

**OpenAI**:
1. Get API key from https://platform.openai.com/
2. Add to `.env`:
   ```env
   LLM_PROVIDER=openai
   OPENAI_API_KEY=sk-your-key-here
   ```
3. Restart backend

**Anthropic**:
1. Get API key from https://console.anthropic.com/
2. Add to `.env`:
   ```env
   LLM_PROVIDER=anthropic
   ANTHROPIC_API_KEY=sk-ant-your-key-here
   ```
3. Restart backend

### Enable Monitoring

**Sentry** (crash reporting):
1. Create account at https://sentry.io/
2. Add to `.env`:
   ```env
   SENTRY_DSN=https://your-sentry-dsn
   ```

**Mixpanel** (analytics):
1. Create account at https://mixpanel.com/
2. Add to `.env`:
   ```env
   MIXPANEL_TOKEN=your-mixpanel-token
   ```

### IDE Setup

**Android Studio Plugins** (recommended):
- Kotlin
- Android Material Design Components
- Rainbow Brackets
- GitToolBox

**VS Code Extensions** (for backend):
- ESLint
- Prettier
- JavaScript (ES6) code snippets
- REST Client

---

**Installation complete!** 🎉

**Next**: [Configuration Guide](configuration.md)

