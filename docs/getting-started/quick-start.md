# Quick Start Guide

Get the Credit Card Payment Optimizer running in 10 minutes.

## Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or later
- **Node.js** 18+ and npm
- **JDK** 17 or later
- **Android SDK** API 26+
- **Plaid Account** (free sandbox)

## Step 1: Clone the Repository

```bash
cd /home/ilyk/projects/pets/cc
```

## Step 2: Backend Setup (5 minutes)

### Install Dependencies

```bash
cd backend
npm install
```

### Configure Environment

1. Sign up at [Plaid Dashboard](https://dashboard.plaid.com/signup)
2. Get your sandbox credentials
3. Create `.env` file:

```env
# Plaid Configuration
PLAID_CLIENT_ID=your_client_id_here
PLAID_SECRET=your_sandbox_secret_here
PLAID_ENV=sandbox

# JWT Secret (generate a random string)
JWT_SECRET=your-super-secret-jwt-key-change-this-in-production

# Database
DATABASE_URL=./data/cc_optimizer.db
DB_TYPE=sqlite

# Server
PORT=3000
NODE_ENV=development

# Optional: LLM for AI-powered statement detection
LLM_PROVIDER=none
# To enable LLM, use: openai or anthropic
# OPENAI_API_KEY=sk-your-key
# ANTHROPIC_API_KEY=sk-ant-your-key
```

### Start Backend

```bash
npm start
```

You should see:
```
Server running on port 3000
Environment: development
Plaid environment: sandbox
Health check: http://localhost:3000/health
```

## Step 3: Android App Setup (5 minutes)

### Open in Android Studio

1. Open Android Studio
2. `File > Open` → Select `/home/ilyk/projects/pets/cc`
3. Wait for Gradle sync

### Configure Backend URL

Edit `app/src/main/java/com/ccoptimizer/app/data/api/RetrofitClient.kt`:

```kotlin
// For Android Emulator
private const val BASE_URL = "http://10.0.2.2:3000/"

// For Physical Device (use your computer's local IP)
// private const val BASE_URL = "http://192.168.1.100:3000/"
```

### Run the App

1. Select a device/emulator (API 26+)
2. Click **Run** (▶️)
3. App launches!

## Step 4: Test with Plaid Sandbox

### In the App

1. Tap "Connect with Plaid"
2. Select any bank (e.g., "Chase")
3. Use test credentials:
   - Username: `user_good`
   - Password: `pass_good`
4. Select credit card accounts
5. Complete the flow

### See Results

The app will display:
- Your connected credit cards
- Current balances
- Statement closing dates
- Recommended payment dates
- Payment amounts

## 🎉 Success!

You now have a fully functional Credit Card Payment Optimizer!

## Next Steps

1. **Explore Features**: Try dark mode, notifications, and settings
2. **Read Documentation**: [System Overview](../architecture/system-overview.md)
3. **Customize**: Configure bank processing times and holidays
4. **Deploy**: See [Deployment Guide](../development/deployment.md)

## Common Issues

### Backend won't start

```bash
# Check if port 3000 is available
lsof -i :3000

# Try a different port
PORT=3001 npm start
```

### App can't connect to backend

**Emulator**: Use `http://10.0.2.2:3000/`  
**Physical Device**: Use your computer's IP `http://192.168.x.x:3000/`

```bash
# Find your IP (Linux/Mac)
ip addr show | grep "inet "

# Test backend from device browser
http://YOUR_IP:3000/health
```

### Plaid Link doesn't open

1. Verify backend is running
2. Check Plaid credentials in `.env`
3. Check logcat for errors

## Support

- [FAQ](../development/faq.md)
- [Troubleshooting Guide](../development/troubleshooting.md)
- [Full Installation Guide](installation.md)

---

**Next**: [System Overview](../architecture/system-overview.md) to understand how it works!

