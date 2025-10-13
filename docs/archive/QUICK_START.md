# Quick Start Guide 🚀

Get your Credit Card Payment Optimizer running in 10 minutes!

## Prerequisites

- ✅ Android Studio (latest version)
- ✅ Node.js 18+ installed
- ✅ Git installed

## Step-by-Step Setup

### Step 1: Backend Setup (5 minutes)

```bash
# Navigate to backend directory
cd backend

# Install dependencies
npm install

# Get Plaid credentials (if you don't have them)
# 1. Go to https://dashboard.plaid.com/signup
# 2. Create account
# 3. Create new app
# 4. Copy client_id and sandbox secret

# Create environment file
cp .env.example .env

# Edit .env and add your credentials:
# PLAID_CLIENT_ID=your_client_id
# PLAID_SECRET=your_sandbox_secret
# PLAID_ENV=sandbox
# PORT=3000

# Start the server
npm start
```

✅ Server should be running at `http://localhost:3000`

Test it:
```bash
curl http://localhost:3000/health
```

### Step 2: Android App Setup (3 minutes)

1. **Open Project in Android Studio**
   - `File > Open`
   - Select `/home/ilyk/projects/pets/cc`
   - Wait for Gradle sync

2. **Configure Backend URL**
   - Open `app/src/main/java/com/ccoptimizer/app/data/api/RetrofitClient.kt`
   - For **emulator**: Keep `http://10.0.2.2:3000/` (already set)
   - For **physical device**: Change to `http://YOUR_LOCAL_IP:3000/`
   
   Find your local IP:
   ```bash
   # Linux/Mac
   ifconfig | grep "inet "
   
   # Or
   hostname -I
   ```

3. **Run the App**
   - Select device/emulator
   - Click Run ▶️
   - App will launch!

### Step 3: Test with Plaid Sandbox (2 minutes)

1. In the app, tap **"Connect with Plaid"**
2. Select any bank (e.g., **Chase**)
3. Use test credentials:
   - Username: `user_good`
   - Password: `pass_good`
4. Select credit card accounts
5. Complete the flow

🎉 You should now see your credit card recommendations!

## What You'll See

The app displays:
- 💳 All your connected credit cards
- 📅 When to make payments (accounting for bank processing times)
- 💵 How much to pay (current balance to zero out statement)
- ⏰ Days until payment needed
- 📊 Smart reasoning for each recommendation

## Example Recommendation

```
Chase Freedom •••• 1234
PAY IN 5 DAYS

Pay By: Oct 12, 2025
Amount: $1,247.50

Statement closes: Oct 18, 2025

💡 "Statement closes on Oct 18. Payment must hit account by Oct 17. 
   Chase takes 2 business day(s) to process. You have 5 days to make 
   the payment."
```

## How It Works

### The Smart Algorithm

1. **Finds next statement date** (e.g., 18th of month)
2. **Calculates target date** (1 day before statement)
3. **Accounts for bank processing**:
   - Amex: Instant (0 days)
   - Bank of America: 3 business days
   - Chase/Citi/Wells Fargo/Home Depot: 2 business days
   - Capital One/Discover: 1 business day
4. **Skips weekends and federal holidays**:
   - New Year's Day
   - MLK Jr. Day
   - Presidents' Day
   - Memorial Day
   - Juneteenth
   - Independence Day
   - Labor Day
   - Columbus Day
   - Veterans Day
   - Thanksgiving
   - Christmas
5. **Recommends payment date and amount**

## Customization

### Statement Dates - Automatically Fetched! 

The app automatically gets your actual statement closing dates from your bank via Plaid's Liabilities API. No manual configuration needed!

**How it works:**
1. When you connect your credit card, Plaid fetches liability data
2. The app extracts the day of the month from your last statement date
3. This becomes your statement generation day for calculations
4. If a bank doesn't provide this data, it defaults to the 15th

**Why this matters:**
- Uses your ACTUAL statement dates, not estimates
- Automatically stays accurate even if your bank changes dates
- Works for all supported institutions that provide liability data

### Add More Banks

Edit `app/src/main/java/com/ccoptimizer/app/data/models/CreditCard.kt`:

```kotlin
enum class BankInstitution(val displayName: String, val processingDays: Int) {
    // ... existing banks ...
    MY_BANK("My Bank", 2),
}
```

## Troubleshooting

### "Cannot connect to backend"

**Emulator:**
- Backend URL should be `http://10.0.2.2:3000/`
- Make sure backend is running: `curl http://localhost:3000/health`

**Physical Device:**
- Backend URL should be `http://YOUR_LOCAL_IP:3000/`
- Device and computer must be on same WiFi
- Test: `curl http://YOUR_LOCAL_IP:3000/health`

### "No credit cards found"

- Make sure you selected **credit card** accounts in Plaid Link
- Check logcat for API errors
- Verify backend is returning accounts:
  ```bash
  curl http://localhost:3000/health
  ```

### "Plaid Link not opening"

- Check backend URL in `RetrofitClient.kt`
- Verify Plaid credentials in backend `.env`
- Check Android logcat for errors

### Gradle sync failed

```bash
./gradlew clean
./gradlew build
```

Or in Android Studio: `File > Invalidate Caches > Invalidate and Restart`

## Project Structure

```
cc/
├── app/                           # Android app
│   ├── src/main/
│   │   ├── java/com/ccoptimizer/app/
│   │   │   ├── MainActivity.kt    # Entry point
│   │   │   ├── business/          # Business logic
│   │   │   │   └── PaymentCalculator.kt  # Core algorithm
│   │   │   ├── data/              # Data layer
│   │   │   ├── ui/                # UI components
│   │   │   └── utils/             # Utilities
│   │   │       └── BusinessDayCalculator.kt  # Holiday logic
│   │   └── res/                   # Android resources
│   └── build.gradle.kts
├── backend/                       # Node.js API server
│   ├── server.js                  # Express server
│   ├── package.json
│   └── .env                       # Plaid credentials
├── README.md                      # Full documentation
├── SETUP.md                       # Detailed setup
├── BACKEND_EXAMPLE.md            # Backend details
└── QUICK_START.md                # This file
```

## Next Steps

### Production Ready

1. **Add Persistent Storage**
   - Implement Room database for local storage
   - Store access tokens, card configurations

2. **User Authentication**
   - Add user accounts
   - Secure backend with JWT

3. **Deploy Backend**
   - Heroku, Railway, or DigitalOcean
   - Use production Plaid environment
   - Set up database (PostgreSQL)

4. **Add Features**
   - Payment reminders/notifications
   - Transaction history
   - Payment scheduling
   - Multiple payment strategies
   - Widgets

5. **Publish to Play Store**
   - Create signing key
   - Build release APK
   - Submit to Google Play

### Useful Commands

```bash
# Backend
cd backend
npm start                    # Start server
npm run dev                  # Start with auto-reload

# Android
./gradlew assembleDebug     # Build debug APK
./gradlew test              # Run unit tests
./gradlew clean             # Clean build

# Plaid Dashboard
# https://dashboard.plaid.com/
```

## Resources

- 📖 [Full Documentation](README.md)
- 🔧 [Setup Guide](SETUP.md)
- 🖥️ [Backend Details](BACKEND_EXAMPLE.md)
- 📚 [Plaid Docs](https://plaid.com/docs/)
- 📱 [Android Docs](https://developer.android.com/)

## Support

Having issues? Check:
1. Backend is running: `curl http://localhost:3000/health`
2. Correct backend URL in `RetrofitClient.kt`
3. Valid Plaid credentials in backend `.env`
4. Android logcat for errors
5. Backend console for errors

## Demo Flow

```
User Opens App
    ↓
Tap "Connect with Plaid"
    ↓
Backend creates link_token
    ↓
Plaid Link opens
    ↓
User selects bank & logs in (test credentials)
    ↓
User selects credit card accounts
    ↓
App exchanges public_token for access_token
    ↓
App fetches credit card accounts
    ↓
PaymentCalculator computes recommendations
    ↓
Display beautiful UI with payment dates
    ↓
User knows exactly when to pay! 🎉
```

Happy optimizing! 💳✨

