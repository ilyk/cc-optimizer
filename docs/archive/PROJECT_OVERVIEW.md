# Credit Card Payment Optimizer - Project Overview

## What This App Does

A native Android app that optimizes credit card payments by:

1. **Connecting to your bank accounts** via Plaid
2. **Analyzing your credit cards** (balances, statement dates)
3. **Calculating optimal payment dates** considering:
   - Statement generation dates
   - Bank processing times (Amex: instant, BoA: 3 days, etc.)
   - Weekends and federal holidays
4. **Recommending when and how much to pay** to keep statement balances near $0

## The Problem It Solves

**Challenge:** Credit card statement balances affect your credit utilization ratio. If you wait until the statement generates to pay, a high balance gets reported even if you pay in full.

**Solution:** Pay just before the statement generates so the reported balance is as low as possible, while accounting for bank processing times and holidays.

**Example:**
- Statement generates on 18th of each month
- Payment should hit account by 17th
- Bank of America takes 3 business days to process
- Need to initiate payment on 12th (accounting for weekend on 14-15)

## Technology Stack

### Android App
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Architecture:** MVVM (ViewModel + Repository pattern)
- **Networking:** Retrofit + OkHttp
- **Plaid Integration:** Plaid Link SDK
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 34

### Backend Server
- **Runtime:** Node.js 18+
- **Framework:** Express
- **API Integration:** Plaid Node SDK
- **Authentication:** Planned (JWT)
- **Database:** Planned (PostgreSQL for production)

## Project Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Android App (Kotlin)                  │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │              UI Layer (Compose)                   │  │
│  │  - MainActivity                                   │  │
│  │  - MainScreen (recommendations view)              │  │
│  │  - Theme & styling                                │  │
│  └────────────────────┬─────────────────────────────┘  │
│                       │                                  │
│  ┌────────────────────▼─────────────────────────────┐  │
│  │           ViewModel Layer                         │  │
│  │  - MainViewModel (state management)               │  │
│  │  - UiState (Loading/Success/Error)                │  │
│  └────────────────────┬─────────────────────────────┘  │
│                       │                                  │
│  ┌────────────────────▼─────────────────────────────┐  │
│  │        Business Logic Layer                       │  │
│  │  - PaymentCalculator (core algorithm)             │  │
│  │  - BusinessDayCalculator (holiday logic)          │  │
│  └──────────────────────────────────────────────────┘  │
│                       │                                  │
│  ┌────────────────────▼─────────────────────────────┐  │
│  │         Data Layer (Repository)                   │  │
│  │  - PlaidRepository                                │  │
│  │  - PlaidApi (Retrofit interface)                  │  │
│  │  - Data Models (CreditCard, etc.)                 │  │
│  └────────────────────┬─────────────────────────────┘  │
└───────────────────────┼─────────────────────────────────┘
                        │
                        │ HTTPS
                        │
         ┌──────────────▼──────────────┐
         │   Backend Server (Node.js)  │
         │   - Express REST API        │
         │   - Plaid SDK integration   │
         │   - Token management        │
         └──────────────┬──────────────┘
                        │
                        │ Plaid API
                        │
              ┌─────────▼─────────┐
              │   Plaid Service   │
              │   (Bank Data)     │
              └───────────────────┘
```

## Key Components

### 1. PaymentCalculator.kt
The heart of the app - calculates optimal payment dates:

```kotlin
fun calculateRecommendation(creditCard: CreditCard): PaymentRecommendation {
    // 1. Find next statement date
    // 2. Calculate target date (1 day before)
    // 3. Subtract bank processing days
    // 4. Adjust for weekends/holidays
    // 5. Return recommendation
}
```

### 2. BusinessDayCalculator.kt
Handles weekend and holiday logic:

```kotlin
fun getBusinessDaysBefore(targetDate: LocalDate, businessDays: Int): LocalDate {
    // Counts backwards N business days
    // Skips weekends and federal holidays
}
```

### 3. PlaidRepository.kt
Manages Plaid integration:

```kotlin
suspend fun createLinkToken(): Result<String>
suspend fun exchangePublicToken(publicToken: String): Result<String>
suspend fun getCreditCards(): Result<List<CreditCard>>
```

### 4. Backend Server (server.js)
Secure Plaid API proxy:

```javascript
POST /api/plaid/create_link_token    // Initialize Plaid Link
POST /api/plaid/exchange_public_token // Get access token
POST /api/plaid/accounts              // Fetch credit cards
POST /api/plaid/transactions          // Get transactions (future)
```

## Data Models

### CreditCard
```kotlin
data class CreditCard(
    val id: String,
    val name: String,
    val lastFourDigits: String,
    val bankName: String,
    val statementGenerationDay: Int,  // Day of month
    val currentBalance: Double,
    val availableCredit: Double,
    val creditLimit: Double,
    val institution: BankInstitution   // Processing time info
)
```

### BankInstitution
```kotlin
enum class BankInstitution(val displayName: String, val processingDays: Int) {
    AMEX("American Express", 0),
    BANK_OF_AMERICA("Bank of America", 3),
    CHASE("Chase", 2),
    CITI("Citi", 2),
    HOME_DEPOT("Home Depot (Citi)", 2),
    CAPITAL_ONE("Capital One", 1),
    DISCOVER("Discover", 1),
    // ...
}
```

### PaymentRecommendation
```kotlin
data class PaymentRecommendation(
    val creditCard: CreditCard,
    val recommendedPaymentDate: LocalDate,
    val recommendedAmount: Double,
    val statementDueDate: LocalDate,
    val daysUntilPayment: Int,
    val reasoning: String
)
```

## File Structure

```
cc/
├── app/                                    # Android app
│   ├── src/main/
│   │   ├── java/com/ccoptimizer/app/
│   │   │   ├── MainActivity.kt             # App entry point
│   │   │   ├── business/                   # Business logic
│   │   │   │   └── PaymentCalculator.kt    # Core algorithm
│   │   │   ├── data/                       # Data layer
│   │   │   │   ├── api/
│   │   │   │   │   ├── PlaidApi.kt         # API interface
│   │   │   │   │   └── RetrofitClient.kt   # HTTP client
│   │   │   │   ├── models/
│   │   │   │   │   ├── CreditCard.kt       # Domain models
│   │   │   │   │   └── PlaidModels.kt      # API models
│   │   │   │   └── repository/
│   │   │   │       └── PlaidRepository.kt  # Data repository
│   │   │   ├── ui/                         # UI layer
│   │   │   │   ├── screens/
│   │   │   │   │   └── MainScreen.kt       # Main UI
│   │   │   │   ├── theme/
│   │   │   │   │   ├── Theme.kt            # App theme
│   │   │   │   │   └── Type.kt             # Typography
│   │   │   │   └── viewmodel/
│   │   │   │       └── MainViewModel.kt    # State management
│   │   │   └── utils/
│   │   │       └── BusinessDayCalculator.kt # Holiday logic
│   │   └── res/                            # Android resources
│   │       ├── values/
│   │       │   ├── strings.xml
│   │       │   └── themes.xml
│   │       └── xml/
│   │           ├── backup_rules.xml
│   │           └── data_extraction_rules.xml
│   ├── src/test/                           # Unit tests
│   │   └── java/com/ccoptimizer/app/
│   │       ├── BusinessDayCalculatorTest.kt
│   │       └── PaymentCalculatorTest.kt
│   └── build.gradle.kts                    # App dependencies
│
├── backend/                                # Backend server
│   ├── server.js                           # Express server
│   ├── package.json                        # Dependencies
│   ├── .env.example                        # Env template
│   ├── .gitignore                          # Git ignore
│   └── README.md                           # Backend docs
│
├── gradle/                                 # Gradle wrapper
│   └── wrapper/
│       └── gradle-wrapper.properties
│
├── build.gradle.kts                        # Project config
├── settings.gradle.kts                     # Gradle settings
├── gradle.properties                       # Gradle properties
├── gradlew                                 # Gradle wrapper (Unix)
├── gradlew.bat                             # Gradle wrapper (Windows)
├── .gitignore                              # Git ignore
├── local.properties.example                # SDK path template
│
├── README.md                               # Main documentation
├── SETUP.md                                # Detailed setup guide
├── QUICK_START.md                          # Quick start guide
├── BACKEND_EXAMPLE.md                      # Backend implementation
└── PROJECT_OVERVIEW.md                     # This file
```

## Federal Holidays Supported

The app accounts for all US federal holidays:
- New Year's Day (Jan 1)
- Martin Luther King Jr. Day (3rd Mon in Jan)
- Presidents' Day (3rd Mon in Feb)
- Memorial Day (Last Mon in May)
- Juneteenth (Jun 19)
- Independence Day (Jul 4)
- Labor Day (1st Mon in Sep)
- Columbus Day (2nd Mon in Oct)
- Veterans Day (Nov 11)
- Thanksgiving (4th Thu in Nov)
- Christmas (Dec 25)

When holidays fall on weekends, observed dates are used.

## Development Roadmap

### Phase 1: MVP ✅ (Current)
- [x] Plaid integration
- [x] Credit card fetching
- [x] Payment calculation algorithm
- [x] Holiday/weekend logic
- [x] Basic UI with recommendations
- [x] Backend API server
- [x] Unit tests

### Phase 2: Enhancement
- [ ] Persistent storage (Room database)
- [ ] User authentication
- [ ] Statement date configuration UI
- [ ] Payment reminders/notifications
- [ ] Transaction history view
- [ ] Multiple payment strategies

### Phase 3: Production
- [ ] Backend deployment (Heroku/Railway/AWS)
- [ ] Production Plaid environment
- [ ] Database (PostgreSQL)
- [ ] Encrypted token storage
- [ ] Rate limiting
- [ ] Error tracking (Sentry)
- [ ] Analytics

### Phase 4: Advanced Features
- [ ] Widgets for quick view
- [ ] Payment scheduling
- [ ] Credit score tracking
- [ ] Rewards optimization
- [ ] Multiple user accounts
- [ ] Dark mode
- [ ] Backup/restore
- [ ] Export to CSV

## Security Considerations

### ✅ Implemented
- Plaid secrets in backend only (not in app)
- HTTPS for API calls
- Proper Android permissions

### 🔄 To Implement
- JWT authentication
- Encrypted token storage (Android Keystore)
- Rate limiting on backend
- Input validation
- Request logging
- Webhook signature verification
- Database encryption

## Testing Strategy

### Unit Tests
- `BusinessDayCalculatorTest.kt` - Holiday/weekend logic
- `PaymentCalculatorTest.kt` - Payment calculations

### Integration Tests (Planned)
- Plaid API integration
- Repository layer
- End-to-end flow

### UI Tests (Planned)
- Compose UI testing
- Navigation flows
- Error states

## Getting Started

Choose your path:

1. **Quick Start** → See `QUICK_START.md` (10 minutes)
2. **Detailed Setup** → See `SETUP.md` (comprehensive)
3. **Backend Only** → See `backend/README.md`
4. **Backend Example** → See `BACKEND_EXAMPLE.md`

## Documentation Index

| Document | Purpose |
|----------|---------|
| `README.md` | Main project documentation |
| `QUICK_START.md` | 10-minute setup guide |
| `SETUP.md` | Detailed setup instructions |
| `BACKEND_EXAMPLE.md` | Backend implementation details |
| `backend/README.md` | Backend-specific docs |
| `PROJECT_OVERVIEW.md` | This file - project overview |

## Key Metrics

- **Lines of Code:** ~2,500+ (Kotlin + JavaScript)
- **Files Created:** 35+
- **API Endpoints:** 4 (+ webhook)
- **Supported Banks:** 9 major + generic
- **Federal Holidays:** 11
- **Min SDK:** 26 (Android 8.0+)
- **Target SDK:** 34

## License

Private project

## Acknowledgments

- **Plaid** - Bank account connectivity
- **Jetpack Compose** - Modern Android UI
- **Material 3** - Design system
- **Express.js** - Backend framework

---

**Status:** ✅ MVP Complete - Ready for development and testing

**Next Steps:** Run through `QUICK_START.md` to get the app running!

