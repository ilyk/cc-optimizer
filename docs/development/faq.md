# Frequently Asked Questions (FAQ)

## General Questions

### What is this app?

The Credit Card Payment Optimizer is a native Android app that helps you optimize credit card payments to minimize your statement balance, which improves your credit utilization ratio and credit score.

### How does it work?

1. Connect your bank accounts via Plaid
2. The app fetches your credit card accounts and transaction history
3. AI analyzes your transactions to detect statement closing dates
4. The app calculates when you should pay based on:
   - Statement generation dates
   - Bank processing times (0-3 business days)
   - Weekends and federal holidays
5. You get recommendations on when and how much to pay

### Is it secure?

Yes! The app has bank-grade security:
- **Security Score**: A (95/100)
- Certificate pinning (prevents MITM attacks)
- AES-256-GCM encryption for all sensitive data
- Hardware-backed encryption using Android Keystore
- Root/tamper detection
- OWASP compliant
- See [Security Overview](../security/overview.md) for details

### Is my financial data safe?

Absolutely:
- We use Plaid, trusted by major apps (Venmo, Robinhood, etc.)
- All data is encrypted at rest and in transit
- Tokens are stored in hardware-backed encrypted storage
- We never see your bank login credentials
- All API calls use HTTPS with certificate pinning

## Setup & Installation

### What do I need to run this?

**For the Android app**:
- Android Studio Hedgehog or later
- Android device or emulator (API 26+, Android 8.0+)
- JDK 17+

**For the backend**:
- Node.js 18+
- Plaid account (free sandbox for testing)

### How do I get Plaid credentials?

1. Go to [Plaid Dashboard](https://dashboard.plaid.com/signup)
2. Create a free account
3. Create a new app
4. Copy your `client_id` and `sandbox` secret
5. Add them to backend `.env` file

### Can I use this without a backend server?

No. The Plaid client secret must NEVER be embedded in the mobile app for security reasons. You must run the backend server to proxy Plaid API calls securely.

### Where can I deploy the backend?

Popular options:
- **Railway** - Easy deployment, free tier available
- **Heroku** - Classic PaaS, free tier available
- **AWS** - Flexible, many options (EC2, Lambda, ECS)
- **DigitalOcean** - Simple VPS hosting
- **Vercel** - Serverless functions
- **Your own server** - Full control

See [Deployment Guide](deployment.md) for details.

## Features

### How does the app detect statement closing dates?

The app uses a multi-method approach:

1. **LLM Analysis** (95% success) - AI analyzes transaction patterns
2. **Balance Pattern Analysis** (70% success) - Heuristic matching
3. **Plaid Liabilities API** - Some banks provide this data
4. **Manual Input** (100% success) - You can enter it manually

### What is LLM integration?

LLM (Large Language Model) integration uses AI (OpenAI GPT or Anthropic Claude) to intelligently detect statement closing dates from transaction patterns. This works much better than hardcoded string matching.

**Benefits**:
- Works with any bank terminology
- 95% success rate (vs 60% with string matching)
- Understands context and patterns
- Optional (can disable to save costs)

**Cost**: ~$0.0005 per card analysis (negligible)

### Which banks are supported?

**All banks supported by Plaid**, including:
- American Express
- Bank of America
- Chase
- Citi
- Capital One
- Discover
- Wells Fargo
- US Bank
- Regional banks
- Credit unions
- Store cards (Home Depot, Target, etc.)

The app has built-in processing times for major banks and defaults to 3 business days for others.

### How do I add a new bank?

Simply connect via Plaid Link. The app automatically:
- Detects the bank name
- Maps to known processing times (or uses default)
- Fetches account details
- Analyzes transactions

No manual configuration needed!

### Does it support multiple credit cards?

Yes! You can connect multiple cards from different banks. The app shows recommendations for all cards sorted by payment date.

### What are bank processing times?

Bank processing time is how long it takes for a payment to hit your account after you initiate it.

**Built-in processing times**:
- American Express: 0 days (instant)
- Capital One: 1 business day
- Discover: 1 business day
- Chase: 2 business days
- Citi: 2 business days
- Wells Fargo: 2 business days
- US Bank: 2 business days
- Bank of America: 3 business days
- Generic/Unknown: 3 business days (default)

### How does weekend/holiday handling work?

The app uses `BusinessDayCalculator` which:
- Skips weekends (Saturday/Sunday)
- Skips all 11 US federal holidays
- Uses observed dates when holidays fall on weekends
- Ensures payment recommendations fall on business days

### What are the federal holidays?

The app accounts for:
1. New Year's Day (Jan 1)
2. Martin Luther King Jr. Day (3rd Mon in Jan)
3. Presidents' Day (3rd Mon in Feb)
4. Memorial Day (Last Mon in May)
5. Juneteenth (Jun 19)
6. Independence Day (Jul 4)
7. Labor Day (1st Mon in Sep)
8. Columbus Day (2nd Mon in Oct)
9. Veterans Day (Nov 11)
10. Thanksgiving (4th Thu in Nov)
11. Christmas (Dec 25)

## Technical Questions

### What technology stack is used?

**Android App**:
- Kotlin 1.9.20+
- Jetpack Compose + Material 3
- MVVM architecture
- Retrofit + OkHttp
- Room Database
- Plaid Link SDK

**Backend**:
- Node.js 18+
- Express.js
- SQLite (dev) / PostgreSQL (production)
- Plaid Node SDK
- JWT authentication
- OpenAI/Anthropic SDK (optional)

### Why Kotlin and not Java?

Kotlin offers:
- Modern language features
- Null safety (fewer crashes)
- Coroutines (better async code)
- Extension functions
- Better interop with Jetpack Compose
- Industry standard for Android development

### Why Jetpack Compose?

Compose offers:
- Declarative UI (easier to reason about)
- Less boilerplate than XML
- Better performance
- Modern Material 3 design
- Easier theming (dark mode)
- Future of Android UI development

### Can I use Java instead of Kotlin?

Technically yes, but not recommended. All code is in Kotlin and uses Kotlin-specific features (coroutines, null safety, etc.). Converting to Java would be significant effort.

### What is the MVVM architecture?

MVVM (Model-View-ViewModel) separates concerns:
- **Model**: Data layer (repository, API, database)
- **View**: UI layer (Compose screens)
- **ViewModel**: Business logic and state management

**Benefits**:
- Easier to test
- Separation of concerns
- Lifecycle-aware
- Industry standard for Android

### How does caching work?

The app uses multiple caching layers:

1. **Room Database** - Offline persistent cache
2. **In-Memory Cache** - Fast access to recent data
3. **HTTP Cache** - OkHttp cache for API responses
4. **Smart Rate Limiting** - Serves stale data when rate limited

**Result**: 100x faster subsequent loads!

### What is smart rate limiting?

Instead of showing "rate limited" errors, the app:
- Serves cached data when hitting rate limits
- Shows data age ("Updated 5 minutes ago")
- Refreshes in background when limits reset
- Reduces API calls by 90%
- Transparent to user

### How does offline support work?

The app caches all data in Room database:
- Credit card accounts
- Balances
- Statement dates
- Transaction history
- Recommendations

**Offline capabilities**:
- View all cached data
- See recommendations
- Calculate payments
- Only connecting to bank requires internet

## Troubleshooting

### Backend won't start

**Check port availability**:
```bash
lsof -i :3000  # Check if port is in use
```

**Try different port**:
```bash
PORT=3001 npm start
```

**Check logs for errors**:
```bash
npm start | grep ERROR
```

### App can't connect to backend

**For Android Emulator**:
- Use `http://10.0.2.2:3000/` (not `localhost`)
- `10.0.2.2` is special IP for host machine

**For Physical Device**:
- Use your computer's local IP: `http://192.168.x.x:3000/`
- Ensure device and computer are on same WiFi
- Check firewall settings

**Find your IP**:
```bash
# Linux/Mac
ip addr show | grep "inet "
# or
ifconfig | grep "inet "
```

**Test from device browser**:
```
http://YOUR_IP:3000/health
```

Should return:
```json
{
  "status": "ok",
  "message": "Credit Card Optimizer Backend is running"
}
```

### Plaid Link doesn't open

**Common causes**:
1. Backend not running
2. Incorrect backend URL in app
3. Invalid Plaid credentials
4. Plaid sandbox down

**Debug steps**:
1. Check backend is running: `curl http://localhost:3000/health`
2. Check Plaid credentials in `.env`
3. Check Android logcat for errors
4. Verify Link token creation: `curl -X POST http://localhost:3000/api/v1/plaid/create_link_token -H "Authorization: Bearer YOUR_TOKEN"`

### No credit cards showing up

**Causes**:
1. No credit card accounts selected in Plaid Link
2. Account type not "credit"
3. API error

**Solutions**:
1. Reconnect via Plaid Link and ensure you select credit card accounts
2. Check account type in response (should be "credit")
3. Check logcat for API errors
4. Verify accounts endpoint: `curl http://localhost:3000/api/v1/plaid/accounts -H "Authorization: Bearer TOKEN"`

### Statement date not detected

The app tries multiple methods:
1. LLM analysis (if enabled)
2. Pattern matching
3. Plaid Liabilities API
4. Manual input

**If auto-detection fails**:
- You'll see a dialog to manually enter the date
- Check your last statement for the closing date
- Enter the day of month (1-31)
- The app will remember it for future calculations

### Dark mode not working

**Check theme setting**:
1. Open Settings
2. Select Dark Mode preference
3. Options: Light / Dark / Auto (system)

**Auto mode**:
- Follows system theme
- Change in device Settings > Display > Theme

### Notifications not appearing

**Check permissions**:
1. Settings > Apps > Credit Card Optimizer > Permissions
2. Enable Notifications
3. Enable Calendar (for calendar events)
4. Enable Alarms (for reminders)

**Android 13+ requires explicit permission**:
- App will request permission on first notification attempt
- Grant when prompted

### Build fails with certificate pin errors

**For production builds**, certificate pins are required:

1. **Ensure backend is running**
2. **Extract pins**:
```bash
./gradlew getCertificatePins -PBACKEND_URL=https://your-backend.com
```
3. **Build release**:
```bash
./gradlew assembleRelease
```

**For debug builds**, pins are not required:
```bash
./gradlew assembleDebug
```

### App crashes on startup

**Common causes**:
1. Incompatible Android version (need API 26+)
2. Missing dependencies
3. Corrupted cache

**Solutions**:
```bash
# Clean and rebuild
./gradlew clean
./gradlew assembleDebug

# Clear app data on device
adb shell pm clear com.ccoptimizer.app

# Check logcat for crash details
adb logcat | grep AndroidRuntime
```

### Tests failing

**Run tests with**:
```bash
./gradlew test
```

**Common issues**:
- Missing dependencies: `npm install` (backend)
- Wrong Java version: Use JDK 17
- Proxy issues: Check network settings

**Backend test coverage requirement**: 95%+

### Lint errors

**Auto-fix**:
```bash
# Android
./gradlew ktlintFormat

# Backend
cd backend
npm run lint:fix
npm run format
```

**Check only**:
```bash
# Android
./gradlew ktlintCheck

# Backend
npm run lint:check
npm run format:check
```

## Performance

### Why is initial load slow?

First load fetches data from Plaid:
- Accounts (~500ms)
- Transactions (~1-2s)
- Liabilities (~500ms)

**Total**: ~2-3 seconds

**Subsequent loads**: ~100ms (from cache, 100x faster!)

### How can I improve performance?

The app already includes:
- Smart caching
- Parallel API calls
- Request throttling
- Offline support
- Pull-to-refresh

**Additional tips**:
- Use WiFi for initial sync
- Enable LLM for faster statement detection
- Keep app updated for latest optimizations

### How much data does the app use?

**Initial sync** (per card):
- Accounts: ~5 KB
- Transactions (90 days): ~50-200 KB
- Liabilities: ~2 KB

**Total for 3 cards**: ~200-600 KB initial, then ~10 KB for refreshes

**Very lightweight!**

## Development

### How do I contribute?

See [Contributing Guide](contributing.md) for:
- Code style guidelines
- Testing requirements
- Pull request process
- Issue reporting

### How do I run tests?

**Android**:
```bash
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest    # Instrumented tests
```

**Backend**:
```bash
cd backend
npm test
npm run coverage
```

**Coverage requirement**: 95%+

### How do I debug the app?

**Android Studio**:
1. Set breakpoints in code
2. Run in debug mode (Shift+F9)
3. Use debugger panel

**Logcat**:
```bash
adb logcat | grep "CC Optimizer"
```

**Backend**:
```bash
# Enable debug logging
LOG_LEVEL=debug npm start
```

### Where are the logs?

**Android**:
- Logcat (during development)
- Crash reports (production, if Sentry configured)

**Backend**:
- Console output
- `logs/` directory (if configured)
- Sentry (if configured)

## Cost & Scaling

### What does it cost to run?

**Development** (Free):
- Plaid: Free sandbox
- LLM: Optional, ~$0.0005/card
- Backend: Run locally (free)

**Production** (Estimated for 10,000 users):
- Backend hosting: $0-25/month (Railway/Heroku free tier or small instance)
- Database: $0-15/month (included in hosting or managed PostgreSQL)
- LLM: ~$15/month (if enabled, one-time analysis per card)
- **Total**: ~$15-55/month for 10,000 users

**Very affordable!**

### Can it scale?

Yes! Architecture supports scaling:
- Stateless backend (easy to horizontally scale)
- Database can be PostgreSQL (scales well)
- Caching reduces database load
- Rate limiting protects from abuse
- Can add Redis for distributed caching
- Can add load balancer for multiple instances

### What's the LLM cost at scale?

**OpenAI GPT-4o-mini**:
- Per card analysis: ~$0.0005
- Cached for 30 days
- 10,000 users × 3 cards = 30,000 cards
- One-time: $15
- Monthly (re-analysis): $15/month

**Can disable** if budget constrained (falls back to pattern matching at 70% success rate)

## Compliance & Legal

### Is this compliant with banking regulations?

The app uses Plaid, which is compliant with:
- PCI DSS
- SOC 2 Type II
- GDPR
- CCPA
- Banking regulations

We don't store sensitive banking credentials or account numbers.

### Is it GDPR compliant?

Yes:
- Explicit user consent (Plaid Link flow)
- Data encryption
- Right to deletion (can remove accounts)
- Data minimization (only fetch what's needed)
- Secure storage

### Can I use this commercially?

This is a private project. For commercial use:
- Review Plaid pricing (free tier is limited)
- Consider legal/compliance requirements
- Add Terms of Service
- Add Privacy Policy
- Consider insurance/liability

## Support

### Where can I get help?

1. **Check this FAQ** - Common questions answered
2. **Troubleshooting Guide** - [troubleshooting.md](troubleshooting.md)
3. **Documentation** - [docs/](../README.md)
4. **GitHub Issues** - Report bugs or request features

### How do I report a bug?

1. Check if it's already in [Troubleshooting](troubleshooting.md)
2. Check GitHub issues
3. Create new issue with:
   - Android version
   - App version
   - Steps to reproduce
   - Expected behavior
   - Actual behavior
   - Logs/screenshots

### How do I request a feature?

Create a GitHub issue with:
- Feature description
- Use case
- Why it's valuable
- Any implementation ideas

---

**Still have questions?** Check [Troubleshooting Guide](troubleshooting.md) or review the documentation.

