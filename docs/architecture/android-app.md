# Android App Architecture

Deep dive into the Android application architecture and implementation.

## Overview

The Android app follows modern Android development best practices using:
- **MVVM Architecture** (Model-View-ViewModel)
- **Jetpack Compose** for UI
- **Kotlin Coroutines** for async operations
- **Repository Pattern** for data management
- **Dependency Injection** (manual, lightweight)

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                              │
│                   (Jetpack Compose)                          │
│                                                              │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐  │
│  │  MainScreen   │  │SettingsScreen │  │ LoadingScreen │  │
│  │  (Compose)    │  │  (Compose)    │  │  (Compose)    │  │
│  └───────┬───────┘  └───────┬───────┘  └───────┬───────┘  │
│          │                   │                   │           │
│          └───────────────────┼───────────────────┘           │
│                              │                               │
└──────────────────────────────┼───────────────────────────────┘
                               │
┌──────────────────────────────▼───────────────────────────────┐
│                      ViewModel Layer                          │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              MainViewModel                            │  │
│  │                                                       │  │
│  │  - State: UiState (Loading/Success/Error)           │  │
│  │  - Events: User actions (connect, refresh)           │  │
│  │  - Data: Flow<List<PaymentRecommendation>>          │  │
│  │                                                       │  │
│  │  Methods:                                             │  │
│  │  - initiatePlaidLink()                               │  │
│  │  - onPlaidSuccess(publicToken)                       │  │
│  │  - refreshData()                                      │  │
│  │  - updateStatementDate(card, date)                   │  │
│  └──────────────────────┬───────────────────────────────┘  │
│                         │                                   │
└─────────────────────────┼───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                    Business Logic Layer                      │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │          PaymentCalculator (Object)                   │  │
│  │  - calculateRecommendations(cards)                    │  │
│  │  - calculateRecommendation(card)                      │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │       BusinessDayCalculator (Object)                  │  │
│  │  - getBusinessDaysBefore(date, days)                 │  │
│  │  - isFederalHoliday(date)                            │  │
│  │  - isWeekend(date)                                    │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                    Repository Layer                          │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         PlaidRepository                               │  │
│  │  - createLinkToken()                                  │  │
│  │  - exchangePublicToken(token)                         │  │
│  │  - getAccounts()                                      │  │
│  │  - getTransactions(start, end)                        │  │
│  │  - getCachedData()                                    │  │
│  └────────────┬─────────────────────────┬────────────────┘  │
│               │                         │                   │
│  ┌────────────▼─────────────┐ ┌────────▼──────────────┐   │
│  │     PlaidApi              │ │   CacheManager        │   │
│  │  (Retrofit Interface)     │ │  (Room Database)      │   │
│  └───────────────────────────┘ └───────────────────────┘   │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                      Data Layer                              │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         RetrofitClient (HTTP Client)                  │  │
│  │  - OkHttpClient with interceptors                     │  │
│  │  - Certificate pinning                                │  │
│  │  - Logging interceptor                                │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         SecureTokenStorage                            │  │
│  │  - EncryptedSharedPreferences                         │  │
│  │  - AES-256-GCM encryption                             │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         AppDatabase (Room)                            │  │
│  │  - CreditCardEntity                                   │  │
│  │  - CreditCardDao                                      │  │
│  └──────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

## Project Structure

```
app/src/main/java/com/ccoptimizer/app/
├── MainActivity.kt                    # App entry point
│
├── business/                          # Business logic
│   └── PaymentCalculator.kt          # Payment calculation algorithm
│
├── data/                              # Data layer
│   ├── api/                           # Network
│   │   ├── PlaidApi.kt               # Retrofit API interface
│   │   └── RetrofitClient.kt         # HTTP client configuration
│   │
│   ├── local/                         # Local storage
│   │   ├── AppDatabase.kt            # Room database
│   │   ├── CacheManager.kt           # Cache management
│   │   ├── dao/
│   │   │   └── CreditCardDao.kt      # Data Access Object
│   │   └── entities/
│   │       └── CreditCardEntity.kt   # Room entity
│   │
│   ├── models/                        # Data models
│   │   ├── AuthModels.kt             # Auth request/response
│   │   ├── CreditCard.kt             # Domain models
│   │   └── PlaidModels.kt            # Plaid API models
│   │
│   ├── repository/                    # Repository pattern
│   │   ├── AuthRepository.kt         # Auth operations
│   │   ├── PlaidRepository.kt        # Plaid operations
│   │   └── RequestThrottler.kt       # Request optimization
│   │
│   └── security/                      # Security
│       └── SecureStorage.kt          # Encrypted storage
│
├── monitoring/                        # Analytics & monitoring
│   ├── AnalyticsManager.kt           # Event tracking
│   └── CrashReporting.kt             # Crash tracking
│
├── notifications/                     # Notifications
│   └── NotificationService.kt        # Payment reminders
│
├── permissions/                       # Runtime permissions
│   └── PermissionManager.kt          # Permission handling
│
├── security/                          # Security features
│   ├── SecurityManager.kt            # Root/tamper detection
│   └── SecureTokenStorage.kt         # Token encryption
│
├── ui/                                # UI layer
│   ├── components/                    # Reusable components
│   │   ├── CreditCardDesign.kt       # Card artwork
│   │   ├── LoadingCard.kt            # Loading state
│   │   └── StatementDateInputDialog.kt
│   │
│   ├── screens/                       # Screen composables
│   │   ├── LoadingScreen.kt          # Loading UI
│   │   ├── MainScreen.kt             # Main screen
│   │   └── SettingsScreen.kt         # Settings UI
│   │
│   ├── theme/                         # Theming
│   │   ├── Theme.kt                  # Material 3 theme
│   │   ├── ThemePreferences.kt       # Theme persistence
│   │   └── Type.kt                   # Typography
│   │
│   └── viewmodel/                     # ViewModels
│       └── MainViewModel.kt          # Main screen ViewModel
│
└── utils/                             # Utilities
    ├── BusinessDayCalculator.kt      # Holiday calculations
    ├── RetryHelper.kt                # Retry logic
    └── Validators.kt                 # Input validation
```

## Layer Details

### UI Layer (Jetpack Compose)

**Responsibilities**:
- Render UI based on ViewModel state
- Handle user input
- Navigate between screens
- Display loading/error states

**Key Files**:

**MainActivity.kt**:
```kotlin
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by themePreferences.themeMode.collectAsState()
            
            CreditCardOptimizerTheme(themeMode = themeMode) {
                MainScreen(
                    viewModel = viewModel,
                    onConnectPlaid = { viewModel.initiatePlaidLink() }
                )
            }
        }
    }
}
```

**MainScreen.kt**:
```kotlin
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onConnectPlaid: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = { TopAppBar(...) },
        floatingActionButton = { ConnectButton(onConnectPlaid) }
    ) { padding ->
        when (uiState) {
            is UiState.Loading -> LoadingScreen()
            is UiState.Success -> RecommendationList(uiState.data)
            is UiState.Error -> ErrorScreen(uiState.message)
        }
    }
}
```

### ViewModel Layer

**Responsibilities**:
- Manage UI state
- Handle user events
- Coordinate with repositories
- Process business logic

**MainViewModel.kt**:
```kotlin
class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val plaidRepository = PlaidRepository()
    private val authRepository = AuthRepository()
    
    init {
        loadData()
    }
    
    fun initiatePlaidLink() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            plaidRepository.createLinkToken()
                .onSuccess { token ->
                    _linkToken.value = token
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message)
                }
        }
    }
    
    fun onPlaidSuccess(publicToken: String) {
        viewModelScope.launch {
            plaidRepository.exchangePublicToken(publicToken)
                .onSuccess {
                    loadData()
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message)
                }
        }
    }
    
    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            plaidRepository.getAccounts()
                .onSuccess { accounts ->
                    val recommendations = PaymentCalculator
                        .calculateRecommendations(accounts)
                    _uiState.value = UiState.Success(recommendations)
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message)
                }
        }
    }
}
```

**UiState sealed class**:
```kotlin
sealed class UiState {
    object Loading : UiState()
    data class Success(val data: List<PaymentRecommendation>) : UiState()
    data class Error(val message: String) : UiState()
}
```

### Business Logic Layer

**PaymentCalculator**:
```kotlin
object PaymentCalculator {
    fun calculateRecommendations(
        creditCards: List<CreditCard>
    ): List<PaymentRecommendation> {
        return creditCards
            .filter { it.statementGenerationDay != null }
            .mapNotNull { card ->
                try {
                    calculateRecommendation(card)
                } catch (e: Exception) {
                    null
                }
            }
            .sortedBy { it.recommendedPaymentDate }
    }
    
    fun calculateRecommendation(
        creditCard: CreditCard
    ): PaymentRecommendation {
        val today = LocalDate.now()
        val statementDay = creditCard.statementGenerationDay!!
        
        // 1. Next statement date
        val nextStatementDate = getNextStatementDate(today, statementDay)
        
        // 2. Target payment hit date (1 day before)
        val targetPaymentHitDate = nextStatementDate.minusDays(1)
        
        // 3. Subtract processing days
        val processingDays = creditCard.institution.processingDays
        val recommendedPaymentDate = BusinessDayCalculator
            .getBusinessDaysBefore(targetPaymentHitDate, processingDays)
        
        // 4. Ensure not in past
        val finalDate = if (recommendedPaymentDate.isBefore(today)) {
            today
        } else {
            recommendedPaymentDate
        }
        
        return PaymentRecommendation(
            creditCard = creditCard,
            recommendedPaymentDate = finalDate,
            recommendedAmount = creditCard.currentBalance,
            statementDueDate = nextStatementDate,
            daysUntilPayment = ChronoUnit.DAYS.between(today, finalDate).toInt(),
            reasoning = buildReasoningString(...)
        )
    }
}
```

### Repository Layer

**PlaidRepository**:
```kotlin
class PlaidRepository {
    private val api: PlaidApi = RetrofitClient.plaidApi
    private val secureStorage = SecureTokenStorage()
    private val cacheManager = CacheManager()
    
    suspend fun createLinkToken(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.createLinkToken()
            Result.success(response.linkToken)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun exchangePublicToken(
        publicToken: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.exchangePublicToken(
                ExchangeTokenRequest(publicToken)
            )
            
            // Save access token securely
            secureStorage.saveToken(
                "plaid_access_token",
                response.accessToken,
                System.currentTimeMillis() + 30.days()
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAccounts(): Result<List<CreditCard>> = 
        withContext(Dispatchers.IO) {
            try {
                // Check cache first
                val cached = cacheManager.getCachedAccounts()
                if (cached != null && !cacheManager.isCacheExpired()) {
                    return@withContext Result.success(cached)
                }
                
                // Fetch from API
                val response = api.getAccounts()
                
                // Map to domain models
                val accounts = response.accounts
                    .filter { it.type == "credit" }
                    .map { mapToCreditCard(it) }
                
                // Update cache
                cacheManager.cacheAccounts(accounts)
                
                Result.success(accounts)
            } catch (e: Exception) {
                // Return cached data if available, even if expired
                val cached = cacheManager.getCachedAccounts()
                if (cached != null) {
                    Result.success(cached)
                } else {
                    Result.failure(e)
                }
            }
        }
}
```

### Data Layer

**Retrofit Interface**:
```kotlin
interface PlaidApi {
    @POST("plaid/create_link_token")
    suspend fun createLinkToken(): LinkTokenResponse
    
    @POST("plaid/exchange_public_token")
    suspend fun exchangePublicToken(
        @Body request: ExchangeTokenRequest
    ): ExchangeTokenResponse
    
    @GET("plaid/accounts")
    suspend fun getAccounts(): AccountsResponse
    
    @POST("plaid/transactions")
    suspend fun getTransactions(
        @Body request: TransactionsRequest
    ): TransactionsResponse
}
```

**Room Database**:
```kotlin
@Database(entities = [CreditCardEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun creditCardDao(): CreditCardDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cc_optimizer_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

## Data Flow

### 1. User Opens App
```
MainActivity.onCreate()
  → MainViewModel.init()
    → loadData()
      → PlaidRepository.getAccounts()
        → Check cache
        → If expired: API call
        → Map to domain models
        → PaymentCalculator.calculateRecommendations()
          → For each card: calculateRecommendation()
            → Get next statement date
            → Calculate payment date
            → Account for processing + holidays
        → Update UI state
      → UI renders recommendations
```

### 2. User Connects Plaid
```
User taps "Connect with Plaid"
  → MainScreen.onConnectPlaid()
    → MainViewModel.initiatePlaidLink()
      → PlaidRepository.createLinkToken()
        → API call to backend
        → Backend creates Plaid Link token
      → UI opens Plaid Link
      
User completes Plaid Link
  → Plaid SDK returns public_token
    → MainViewModel.onPlaidSuccess(public_token)
      → PlaidRepository.exchangePublicToken()
        → API call to backend
        → Backend exchanges for access_token
        → Save access_token securely
      → loadData() (refresh accounts)
```

### 3. User Pulls to Refresh
```
User pulls to refresh
  → MainScreen triggers refresh
    → MainViewModel.refreshData()
      → PlaidRepository.getAccounts(forceRefresh = true)
        → Skip cache
        → API call
        → Update cache
        → Recalculate recommendations
      → Update UI state
```

## State Management

**StateFlow pattern**:
```kotlin
class MainViewModel : ViewModel() {
    // Private mutable state
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    
    // Public immutable state
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    // UI collects and recomposes on changes
}
```

**In Composable**:
```kotlin
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    // UI automatically recomposes when uiState changes
    when (uiState) {
        is UiState.Loading -> LoadingScreen()
        is UiState.Success -> ContentScreen(uiState.data)
        is UiState.Error -> ErrorScreen(uiState.message)
    }
}
```

## Threading Model

**Kotlin Coroutines**:
```kotlin
viewModelScope.launch {
    // Runs on Main thread
    _uiState.value = UiState.Loading
    
    withContext(Dispatchers.IO) {
        // Runs on IO thread pool
        val data = api.getData()
    }
    
    // Back on Main thread
    _uiState.value = UiState.Success(data)
}
```

**Thread pools**:
- **Main**: UI updates
- **IO**: Network, database
- **Default**: CPU-intensive operations

## Dependency Injection

**Manual DI** (lightweight approach):
```kotlin
object AppContainer {
    private val retrofit by lazy { RetrofitClient.instance }
    private val database by lazy { AppDatabase.getDatabase(context) }
    
    val plaidRepository by lazy {
        PlaidRepository(retrofit.create(PlaidApi::class.java))
    }
    
    val authRepository by lazy {
        AuthRepository(retrofit.create(AuthApi::class.java))
    }
}
```

**Could migrate to Hilt/Dagger** for larger apps.

## Testing Strategy

**Unit Tests**:
- PaymentCalculator
- BusinessDayCalculator
- ViewModels (with test dispatchers)

**Integration Tests**:
- Repository layer
- Database operations

**UI Tests**:
- Compose UI testing
- User flows

See [Testing Guide](../development/testing.md) for details.

---

**Related Documentation**:
- [System Overview](system-overview.md)
- [Backend Architecture](backend.md)
- [Data Flow](data-flow.md)

