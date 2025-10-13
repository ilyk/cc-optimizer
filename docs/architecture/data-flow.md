# Data Flow

Complete data flow diagrams and sequences for the Credit Card Payment Optimizer.

## Overview

This document details how data flows through the system for common operations.

## 1. App Startup Flow

```
┌─────────────────────────────────────────────────────────────┐
│ 1. User Opens App                                            │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 2. MainActivity.onCreate()                                   │
│    - Initialize theme preferences                            │
│    - Create MainViewModel                                    │
│    - Setup Compose UI                                        │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 3. MainViewModel.init()                                      │
│    - Set initial state: UiState.Loading                      │
│    - Call loadData()                                         │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 4. loadData() - viewModelScope.launch                        │
│    - Check if user logged in (has JWT)                       │
│    - If not logged in → Show login screen                    │
│    - If logged in → Continue                                 │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 5. PlaidRepository.getAccounts()                             │
│    - Get JWT token from SecureStorage                        │
│    - Check CacheManager for cached data                      │
└──────────────────────┬──────────────────────────────────────┘
                       │
                ┌──────┴──────┐
                │             │
       ┌────────▼──────┐ ┌────▼─────────┐
       │ Cache Hit     │ │ Cache Miss   │
       │ (< 5 min old) │ │ (or expired) │
       └────────┬──────┘ └────┬─────────┘
                │             │
                │      ┌──────▼──────────────────────────┐
                │      │ 6. API Call: GET /api/v1/plaid/accounts │
                │      │    Headers: Authorization: Bearer JWT   │
                │      └──────┬──────────────────────────┘
                │             │
                │      ┌──────▼──────────────────────────┐
                │      │ 7. Backend: authMiddleware      │
                │      │    - Verify JWT                  │
                │      │    - Extract userId              │
                │      └──────┬──────────────────────────┘
                │             │
                │      ┌──────▼──────────────────────────┐
                │      │ 8. Backend: PlaidService        │
                │      │    - Get user's Plaid access_token │
                │      │    - Call Plaid API              │
                │      │    - Fetch transactions          │
                │      │    - Analyze for statement dates │
                │      └──────┬──────────────────────────┘
                │             │
                │      ┌──────▼──────────────────────────┐
                │      │ 9. Response: List<Account>      │
                │      │    Update cache                  │
                │      └──────┬──────────────────────────┘
                │             │
                └─────────────┼─────────────────────────┘
                              │
┌─────────────────────────────▼──────────────────────────────┐
│ 10. Map accounts to CreditCard domain models                │
│     - Filter credit cards only                              │
│     - Detect bank institutions                              │
│     - Get processing times                                  │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│ 11. PaymentCalculator.calculateRecommendations()            │
│     For each credit card:                                    │
│     - Get next statement date                                │
│     - Calculate target payment hit date                      │
│     - Subtract bank processing days                          │
│     - Adjust for weekends/holidays                           │
│     - Calculate recommended amount                           │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│ 12. Update UI State                                          │
│     UiState.Success(recommendations)                         │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│ 13. UI Recomposes                                            │
│     - Show list of credit cards                              │
│     - Show payment recommendations                           │
│     - Show days until payment                                │
└──────────────────────────────────────────────────────────────┘
```

## 2. Plaid Connection Flow

```
┌─────────────────────────────────────────────────────────────┐
│ 1. User taps "Connect with Plaid"                            │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 2. MainViewModel.initiatePlaidLink()                         │
│    - Set state: UiState.Loading                              │
│    - Call PlaidRepository.createLinkToken()                  │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 3. API Call: POST /api/v1/plaid/create_link_token           │
│    Headers: Authorization: Bearer JWT                        │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 4. Backend: PlaidService.createLinkToken()                   │
│    - Call Plaid API to create link token                     │
│    - Include user ID, products, country                      │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 5. Plaid API Response                                        │
│    { link_token: "link-sandbox-...", expiration: "..." }     │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 6. Backend returns link_token to app                         │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 7. ViewModel updates linkToken state                         │
│    linkToken.value = "link-sandbox-..."                      │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 8. LaunchedEffect triggers                                   │
│    - Detect linkToken changed                                │
│    - Call openPlaidLink(token)                               │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 9. Plaid Link SDK Opens                                      │
│    - WebView with Plaid interface                            │
│    - User selects bank                                       │
│    - User enters credentials                                 │
│    - User selects accounts                                   │
└──────────────────────┬──────────────────────────────────────┘
                       │
                ┌──────┴──────┐
                │             │
       ┌────────▼──────┐ ┌────▼─────────┐
       │ Success       │ │ Exit/Cancel  │
       │ (LinkSuccess) │ │ (LinkExit)   │
       └────────┬──────┘ └────┬─────────┘
                │             │
                │             └──→ Show error/retry
                │
┌───────────────▼──────────────────────────────────────────────┐
│ 10. onPlaidSuccess(publicToken)                               │
│     - Plaid returns public_token                              │
│     - Call ViewModel.onPlaidSuccess(publicToken)              │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 11. API Call: POST /api/v1/plaid/exchange_public_token       │
│     Body: { "public_token": "public-sandbox-..." }           │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 12. Backend: PlaidService.exchangePublicToken()              │
│     - Call Plaid API to exchange token                       │
│     - Receive access_token and item_id                       │
│     - Save to database (encrypted)                           │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 13. Response: { "itemId": "...", "success": true }           │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 14. ViewModel.loadData()                                     │
│     - Refresh account list                                   │
│     - Show newly connected accounts                          │
└──────────────────────────────────────────────────────────────┘
```

## 3. Statement Date Detection Flow

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Backend: getAccounts() for user                           │
│    - Has Plaid access_token                                  │
│    - Needs to detect statement dates                         │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 2. Fetch transactions (last 90 days)                         │
│    - Call Plaid transactions API                             │
│    - Get all transactions for account                        │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 3. Method 1: Try LLM Analysis (if enabled)                   │
│    - Check if LLM_PROVIDER is configured                     │
│    - Check if enough transactions (min 10)                   │
└──────────────────────┬──────────────────────────────────────┘
                       │
                ┌──────┴──────┐
                │             │
       ┌────────▼──────┐ ┌────▼─────────┐
       │ LLM Enabled   │ │ LLM Disabled │
       │               │ │ or no API key│
       └────────┬──────┘ └────┬─────────┘
                │             │
                │             └──→ Skip to Method 2
                │
┌───────────────▼──────────────────────────────────────────────┐
│ 4. LLMService.analyzeTransactionsWithLLM()                    │
│    - Prepare transaction sample (max 100)                     │
│    - Build analysis prompt                                    │
│    - Call OpenAI or Anthropic API                             │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 5. LLM analyzes pattern                                       │
│    Input: List of transactions with dates, names, amounts     │
│    Task: Find recurring day of month for statement close      │
│    Output: Day number (1-31) or "UNKNOWN"                     │
└──────────────────────┬──────────────────────────────────────┘
                       │
                ┌──────┴──────┐
                │             │
       ┌────────▼──────┐ ┌────▼─────────┐
       │ LLM Success   │ │ LLM Failed   │
       │ (day 1-31)    │ │ or UNKNOWN   │
       └────────┬──────┘ └────┬─────────┘
                │             │
                │             └──→ Try Method 2
                │
┌───────────────▼──────────────────────────────────────────────┐
│ 6. Cache result for 30 days                                   │
│    - Store in cache: statement_day_{accountId}                │
│    - Return detected day                                       │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       └──→ Success! Use this day
                       
┌─────────────────────────────────────────────────────────────┐
│ Method 2: Balance Pattern Analysis (Fallback)                │
│    - Look for recurring balance changes                       │
│    - Identify potential statement dates                       │
│    - Calculate confidence score                               │
└──────────────────────┬──────────────────────────────────────┘
                       │
                ┌──────┴──────┐
                │             │
       ┌────────▼──────┐ ┌────▼─────────┐
       │ Pattern Found │ │ No Pattern   │
       │ (confidence   │ │ (or low      │
       │ > 70%)        │ │ confidence)  │
       └────────┬──────┘ └────┬─────────┘
                │             │
                │             └──→ Method 3
                │
┌───────────────▼──────────────────────────────────────────────┐
│ Return detected day                                            │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       └──→ Success! Use this day

┌─────────────────────────────────────────────────────────────┐
│ Method 3: Manual User Input                                   │
│    - Return null for statement day                            │
│    - App shows "Statement date unknown" for card              │
│    - User taps to manually enter date                         │
│    - Date saved to backend                                    │
└──────────────────────────────────────────────────────────────┘
```

## 4. Payment Calculation Flow

```
┌─────────────────────────────────────────────────────────────┐
│ Input: CreditCard with statementGenerationDay                │
│ - name: "Chase Freedom"                                       │
│ - statementGenerationDay: 18                                  │
│ - institution: CHASE (2 business days)                        │
│ - currentBalance: $1,500                                      │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 1. Get today's date                                           │
│    today = October 10, 2025 (Friday)                          │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 2. Calculate next statement date                              │
│    getNextStatementDate(today, 18)                            │
│    - Statement day: 18                                        │
│    - Current month: October                                   │
│    - 18 > 10? Yes → Use October 18                            │
│    result = October 18, 2025 (Saturday)                       │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 3. Calculate target payment hit date                          │
│    targetDate = statementDate - 1 day                         │
│    result = October 17, 2025 (Friday)                         │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 4. Subtract bank processing days                              │
│    Chase processing: 2 business days                          │
│    BusinessDayCalculator.getBusinessDaysBefore(Oct 17, 2)     │
│                                                                │
│    Starting from: October 17 (Friday)                         │
│    - Subtract 1 day → Oct 16 (Thursday) [business day 1]      │
│    - Subtract 1 day → Oct 15 (Wednesday) [business day 2]     │
│                                                                │
│    result = October 15, 2025 (Wednesday)                      │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 5. Check if date is in the past                               │
│    recommendedDate = Oct 15                                    │
│    today = Oct 10                                              │
│    Oct 15 > Oct 10? Yes → Use Oct 15                           │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 6. Calculate days until payment                                │
│    daysUntil = Oct 15 - Oct 10 = 5 days                       │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 7. Build reasoning string                                      │
│    "Statement closes on 2025-10-18. Payment must hit          │
│     account by 2025-10-17. Chase takes 2 business day(s)      │
│     to process. You have 5 days to make the payment."         │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 8. Create PaymentRecommendation                               │
│    - creditCard: [original card]                              │
│    - recommendedPaymentDate: October 15, 2025                 │
│    - recommendedAmount: $1,500                                 │
│    - statementDueDate: October 18, 2025                        │
│    - daysUntilPayment: 5                                       │
│    - reasoning: [see above]                                    │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 9. Return recommendation to UI                                 │
└──────────────────────────────────────────────────────────────┘
```

## 5. Pull to Refresh Flow

```
┌─────────────────────────────────────────────────────────────┐
│ 1. User pulls down on screen                                  │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 2. MainScreen detects pull gesture                            │
│    - Show refresh indicator                                   │
│    - Call viewModel.refreshData()                             │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 3. ViewModel.refreshData()                                    │
│    - Set state: UiState.Loading                               │
│    - Call PlaidRepository.getAccounts(forceRefresh = true)    │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 4. PlaidRepository.getAccounts(forceRefresh = true)           │
│    - Skip cache check                                         │
│    - Force fresh API call                                     │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 5. Fetch from backend API                                     │
│    - GET /api/v1/plaid/accounts                               │
│    - Backend fetches from Plaid                               │
│    - Returns fresh data                                       │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 6. Update cache with fresh data                               │
│    CacheManager.cacheAccounts(freshData, now())               │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 7. Recalculate recommendations                                │
│    PaymentCalculator.calculateRecommendations(accounts)       │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 8. Update UI state                                             │
│    UiState.Success(newRecommendations)                        │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 9. Hide refresh indicator                                     │
│    Show updated data                                          │
└──────────────────────────────────────────────────────────────┘
```

## 6. Caching Strategy

```
Request Flow with Caching:

User Request
    │
    ▼
Check Cache
    │
    ├─→ Cache Hit (< 5 min old)
    │       │
    │       ▼
    │   Return Cached Data
    │   (Instant response)
    │
    └─→ Cache Miss or Expired
            │
            ▼
        API Call to Backend
            │
            ▼
        Backend fetches from Plaid
            │
            ▼
        Response Data
            │
            ├─→ Update Cache
            │   (5 min TTL)
            │
            └─→ Return Fresh Data


Cache Layers:
1. In-Memory (Repository) - 5 minutes
2. Room Database - Offline support
3. Backend Cache - 30 days for LLM results
```

## Summary

The system uses a multi-layered architecture with clear data flow:
1. **UI Layer** → Renders based on ViewModel state
2. **ViewModel** → Coordinates business logic
3. **Repository** → Manages data sources (API + Cache)
4. **Backend** → Secure Plaid proxy + business logic
5. **Plaid API** → Source of truth for financial data

**Key Patterns**:
- **Caching**: Multi-layer for performance
- **State Management**: Unidirectional data flow (StateFlow)
- **Error Handling**: Graceful fallbacks
- **Security**: JWT + encryption at every step

---

**Related Documentation**:
- [System Overview](system-overview.md)
- [Android Architecture](android-app.md)
- [Backend Architecture](backend.md)

