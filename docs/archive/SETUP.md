# Setup Guide

## Quick Start

### 1. Prerequisites
- **Android Studio** Hedgehog (2023.1.1) or later
- **JDK 17** or later
- **Android SDK** with API level 26+ (Android 8.0+)
- **Git**

### 2. Clone & Open Project
```bash
cd /home/ilyk/projects/pets/cc
```

Open this directory in Android Studio:
- `File > Open` and select the `/home/ilyk/projects/pets/cc` directory
- Wait for Gradle sync to complete

### 3. Set Up Backend Server

⚠️ **CRITICAL**: This app will not work without a backend server. See `BACKEND_EXAMPLE.md` for implementation details.

**Quick backend setup:**

```bash
# Create a new directory for backend
mkdir cc-backend
cd cc-backend

# Initialize Node.js project
npm init -y
npm install express plaid dotenv cors

# Copy the server.js from BACKEND_EXAMPLE.md
# Create .env file with your Plaid credentials
```

**Get Plaid credentials:**
1. Sign up at https://dashboard.plaid.com/signup
2. Create a new app
3. Copy your `client_id` and `secret` (sandbox keys are free)
4. Add to `.env`:
   ```
   PLAID_CLIENT_ID=your_client_id
   PLAID_SECRET=your_sandbox_secret
   PLAID_ENV=sandbox
   ```

### 4. Configure Backend URL

Update the backend URL in the Android app:

```kotlin
// File: app/src/main/java/com/ccoptimizer/app/data/api/RetrofitClient.kt

private const val BASE_URL = "http://10.0.2.2:3000/" // For Android Emulator
// OR
private const val BASE_URL = "http://YOUR_LOCAL_IP:3000/" // For physical device
// OR
private const val BASE_URL = "https://your-deployed-backend.com/" // For production
```

**Note**: 
- `10.0.2.2` is the special IP that Android emulator uses to access `localhost` on your development machine
- For physical devices, use your computer's local IP (e.g., `192.168.1.100:3000`)

### 5. Run the Backend

```bash
cd cc-backend
node server.js
```

Server should start on `http://localhost:3000`

### 6. Build & Run Android App

In Android Studio:
1. Select a device/emulator (API 26+)
2. Click **Run** (▶️) or press `Shift + F10`
3. App should launch on your device

## Testing the App

### Using Sandbox Mode

Plaid Sandbox allows you to test without real bank accounts:

1. Launch the app
2. Tap "Connect with Plaid"
3. Select any bank (e.g., "Chase", "Bank of America")
4. Use Plaid's test credentials:
   - Username: `user_good`
   - Password: `pass_good`
5. Select credit card accounts
6. Complete the flow

The app will show mock credit card data with payment recommendations.

### Test Credentials

Plaid Sandbox test users:
- **Success**: `user_good` / `pass_good`
- **Invalid credentials**: `user_bad` / `pass_bad`
- **MFA required**: Select "Chase" and use `user_good`

## Statement Dates - Automatically Fetched

The app automatically retrieves your actual statement closing dates from your bank via Plaid's Liabilities API!

**How it works:**
1. When linking accounts, the app requests the 'liabilities' product from Plaid
2. Plaid fetches credit card liability data including `last_statement_issue_date`
3. The app extracts the day of the month from your last statement
4. This becomes your statement generation day for payment calculations

**Fallback:**
- If your bank doesn't provide liability data through Plaid, it defaults to day 15
- You can implement a manual override feature for these cases

**Benefits:**
- ✅ Uses ACTUAL statement dates from your bank
- ✅ No manual input required
- ✅ Automatically stays current if your bank changes dates
- ✅ Works for most major credit card issuers

**Code location:**
`PlaidRepository.kt` → `mapToCreditCard()` function extracts the statement day from `account.liability.lastStatementDate`

## Project Structure Overview

```
cc/
├── app/                                # Android app module
│   ├── src/main/
│   │   ├── java/com/ccoptimizer/app/
│   │   │   ├── MainActivity.kt         # Entry point
│   │   │   ├── business/               # Business logic
│   │   │   │   └── PaymentCalculator.kt
│   │   │   ├── data/                   # Data layer
│   │   │   │   ├── api/                # Network
│   │   │   │   ├── models/             # Data models
│   │   │   │   └── repository/         # Repositories
│   │   │   ├── ui/                     # UI layer
│   │   │   │   ├── screens/            # Compose screens
│   │   │   │   ├── theme/              # Material theme
│   │   │   │   └── viewmodel/          # ViewModels
│   │   │   └── utils/                  # Utilities
│   │   ├── res/                        # Android resources
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts                # App dependencies
├── build.gradle.kts                    # Project config
├── settings.gradle.kts                 # Gradle settings
├── README.md                           # Project overview
├── BACKEND_EXAMPLE.md                  # Backend implementation
└── SETUP.md                            # This file
```

## Common Issues

### 1. Gradle Sync Failed
- Ensure you have JDK 17 installed
- Check internet connection
- Try `File > Invalidate Caches > Invalidate and Restart`

### 2. Plaid Link Not Opening
- Verify backend is running
- Check backend URL in `RetrofitClient.kt`
- Verify Plaid credentials in backend `.env`
- Check logcat for errors

### 3. No Credit Cards Found
- In Plaid Link, make sure you select at least one credit card account
- Verify the account type is "credit" (not "depository")
- Check logcat for API errors

### 4. Cannot Connect to Backend from Emulator
- Use `10.0.2.2` instead of `localhost` for emulator
- Ensure backend is running on port 3000
- Check firewall settings

### 5. Cannot Connect from Physical Device
- Make sure device and computer are on same network
- Use computer's local IP (not localhost)
- Backend needs to listen on `0.0.0.0` not just `127.0.0.1`

## Running Tests

```bash
# Unit tests
./gradlew test

# Android instrumented tests
./gradlew connectedAndroidTest
```

## Building APK

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (requires signing config)
./gradlew assembleRelease
```

APK location: `app/build/outputs/apk/`

## Next Steps

1. ✅ Set up backend server
2. ✅ Configure Plaid credentials
3. ✅ Test with Plaid Sandbox
4. ✅ Automatic statement date fetching
5. ⬜ Add persistent storage (Room database)
6. ⬜ Implement user authentication
7. ⬜ Add manual statement date override (for banks without liability data)
8. ⬜ Deploy backend to production
9. ⬜ Submit to Google Play Store

## Need Help?

- Check `README.md` for feature documentation
- Review `BACKEND_EXAMPLE.md` for backend details
- Plaid documentation: https://plaid.com/docs/
- Android documentation: https://developer.android.com/

