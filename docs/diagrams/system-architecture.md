# System Architecture Diagrams

Complete system architecture with visual diagrams.

## High-Level System Architecture

```
┌────────────────────────────────────────────────────────────────────┐
│                         USER DEVICE                                 │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │              Android App (Kotlin)                             │ │
│  │                                                               │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │ │
│  │  │     UI      │  │  ViewModel  │  │  Business   │         │ │
│  │  │  (Compose)  │◄─┤   (MVVM)    │◄─┤   Logic     │         │ │
│  │  └─────────────┘  └─────────────┘  └─────────────┘         │ │
│  │          │                │                │                 │ │
│  │          └────────────────┼────────────────┘                 │ │
│  │                           │                                   │ │
│  │                  ┌────────▼────────┐                         │ │
│  │                  │   Repository    │                         │ │
│  │                  │  - Plaid Repo   │                         │ │
│  │                  │  - Auth Repo    │                         │ │
│  │                  └────────┬────────┘                         │ │
│  │                           │                                   │ │
│  │          ┌────────────────┼────────────────┐                 │ │
│  │          │                │                │                 │ │
│  │    ┌─────▼─────┐  ┌──────▼──────┐  ┌─────▼─────┐           │ │
│  │    │  Network  │  │   Cache     │  │  Secure   │           │ │
│  │    │(Retrofit) │  │   (Room)    │  │  Storage  │           │ │
│  │    └─────┬─────┘  └─────────────┘  └───────────┘           │ │
│  └──────────┼────────────────────────────────────────────────── │
│             │ HTTPS + Certificate Pinning                       │
└─────────────┼────────────────────────────────────────────────────┘
              │
              │ JWT Bearer Token
              │
┌─────────────▼────────────────────────────────────────────────────┐
│                     BACKEND SERVER                                │
│                  (Node.js + Express)                              │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                    Middleware Stack                         │ │
│  │  Helmet → CORS → Body Parser → Rate Limit → Auth → Error  │ │
│  └────────────────────────────────────────────────────────────┘ │
│                              │                                   │
│  ┌───────────────────────────▼────────────────────────────────┐ │
│  │                     API Routes (v1)                         │ │
│  │  /auth/* | /plaid/* | /cards/* | /security/*              │ │
│  └───────────┬───────────────┬───────────────┬────────────────┘ │
│              │               │               │                   │
│      ┌───────▼──────┐  ┌────▼─────┐  ┌──────▼──────┐          │ │
│      │ Auth Service │  │  Plaid   │  │ LLM Service │          │ │
│      │              │  │ Service  │  │(AI Detect)  │          │ │
│      └───────┬──────┘  └────┬─────┘  └─────────────┘          │ │
│              │               │                                   │
│  ┌───────────▼───────────────▼───────────────────────────────┐ │
│  │              Repositories (Data Access)                    │ │
│  │          User Repo | Plaid Repo | Card Repo                │ │
│  └───────────────────────┬────────────────────────────────────┘ │
│                          │                                       │
│  ┌───────────────────────▼────────────────────────────────────┐ │
│  │                   Database Layer                            │ │
│  │       SQLite (dev) / PostgreSQL (production)                │ │
│  └───────────────────────┬────────────────────────────────────┘ │
└────────────────────────┬─┼──────────────────────────────────────┘
                         │ │
              ┌──────────┘ └──────────┐
              │                       │
   ┌──────────▼──────────┐  ┌────────▼─────────┐
   │   Plaid API         │  │  OpenAI/         │
   │  (Bank Data)        │  │  Anthropic API   │
   │  - Link             │  │  (AI Detection)  │
   │  - Accounts         │  │                  │
   │  - Transactions     │  │                  │
   │  - Liabilities      │  │                  │
   └─────────────────────┘  └──────────────────┘
```

## Component Interaction Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    USER ACTION FLOW                              │
└─────────────────────────────────────────────────────────────────┘

User taps "Connect with Plaid"
    │
    ▼
┌─────────────────────────────────┐
│  UI Layer (MainScreen)          │
│  - User taps button             │
│  - Trigger ViewModel event      │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│  ViewModel (MainViewModel)      │
│  - initiatePlaidLink()          │
│  - Call repository              │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│  Repository (PlaidRepository)   │
│  - createLinkToken()            │
│  - Make API call                │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│  Network (Retrofit + OkHttp)    │
│  - POST /api/v1/plaid/...       │
│  - Add JWT header               │
│  - Certificate pinning check    │
└────────────┬────────────────────┘
             │
             │ HTTPS
             ▼
┌─────────────────────────────────┐
│  Backend (Express Middleware)   │
│  1. Helmet (security headers)   │
│  2. CORS (origin check)         │
│  3. Rate limit (check quota)    │
│  4. Auth (verify JWT)           │
│  5. Validation (sanitize input) │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│  Service (PlaidService)         │
│  - createLinkToken()            │
│  - Call Plaid API               │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│  External API (Plaid)           │
│  - Generate link_token          │
│  - Return response              │
└────────────┬────────────────────┘
             │
             ▼
       (Response bubbles back up through layers)
             │
             ▼
┌─────────────────────────────────┐
│  UI Updates                     │
│  - Show Plaid Link modal        │
│  - User selects bank            │
│  - User authenticates           │
└─────────────────────────────────┘
```

## Data Flow Diagram

```
┌────────────────────────────────────────────────────────────┐
│               DATA FLOW: Account Sync                       │
└────────────────────────────────────────────────────────────┘

App Launch
    │
    ├─→ Check Cache (Room DB)
    │       │
    │       ├─→ Cache Hit (< 5 min old)
    │       │       │
    │       │       └─→ Return Cached Data ──┐
    │       │                                 │
    │       └─→ Cache Miss or Expired         │
    │               │                         │
    │               └─→ API Call              │
    │                       │                 │
    └─→ API Call            │                 │
            │               │                 │
            └───────────────┼─────────────────┘
                            │
            ┌───────────────▼────────────────┐
            │  Backend API                   │
            │  GET /api/v1/plaid/accounts    │
            └───────────────┬────────────────┘
                            │
            ┌───────────────▼────────────────┐
            │  Check Backend Cache (30 days) │
            │  - LLM results cached          │
            │  - Statement dates cached      │
            └───────────────┬────────────────┘
                            │
            ┌───────────────▼────────────────┐
            │  Plaid API                     │
            │  - Fetch accounts              │
            │  - Fetch transactions          │
            │  - Fetch liabilities           │
            └───────────────┬────────────────┘
                            │
            ┌───────────────▼────────────────┐
            │  AI Analysis (LLM)             │
            │  - Analyze transactions        │
            │  - Detect statement dates      │
            │  - Cache result (30 days)      │
            └───────────────┬────────────────┘
                            │
            ┌───────────────▼────────────────┐
            │  Return to App                 │
            │  - Account data                │
            │  - Statement dates             │
            │  - Balances                    │
            └───────────────┬────────────────┘
                            │
            ┌───────────────▼────────────────┐
            │  Update Cache (Room DB)        │
            │  - Store accounts              │
            │  - Store metadata              │
            │  - Set cache timestamp         │
            └───────────────┬────────────────┘
                            │
            ┌───────────────▼────────────────┐
            │  Calculate Recommendations     │
            │  - PaymentCalculator           │
            │  - BusinessDayCalculator       │
            │  - For each card               │
            └───────────────┬────────────────┘
                            │
            ┌───────────────▼────────────────┐
            │  Update UI                     │
            │  - Display cards               │
            │  - Show recommendations        │
            │  - Show payment dates          │
            └────────────────────────────────┘
```

## Security Architecture

```
┌────────────────────────────────────────────────────────────┐
│                  SECURITY LAYERS                            │
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│ Layer 1: Network Security                                   │
│  ✓ HTTPS enforced (cleartext blocked)                      │
│  ✓ Certificate pinning (2 pins)                            │
│  ✓ TLS 1.2+ only                                           │
│  ✓ User CAs blocked in production                          │
└────────────────────────────────────────────────────────────┘
                           │
┌────────────────────────────────────────────────────────────┐
│ Layer 2: Code Protection                                    │
│  ✓ ProGuard/R8 obfuscation                                 │
│  ✓ Package rewriting (a.b.c)                               │
│  ✓ Symbol stripping                                        │
│  ✓ Log removal in release                                  │
└────────────────────────────────────────────────────────────┘
                           │
┌────────────────────────────────────────────────────────────┐
│ Layer 3: Data Protection                                    │
│  ✓ AES-256-GCM encryption                                  │
│  ✓ Android Keystore (hardware-backed)                      │
│  ✓ No plaintext tokens                                     │
│  ✓ Secure deletion on logout                               │
└────────────────────────────────────────────────────────────┘
                           │
┌────────────────────────────────────────────────────────────┐
│ Layer 4: Runtime Protection                                 │
│  ✓ Root detection (3 methods)                              │
│  ✓ Tamper detection (signature check)                      │
│  ✓ Debugger detection                                      │
│  ✓ Emulator detection                                      │
└────────────────────────────────────────────────────────────┘
                           │
┌────────────────────────────────────────────────────────────┐
│ Layer 5: Authentication (Backend)                           │
│  ✓ JWT with HS256 (enforced)                               │
│  ✓ bcrypt password hashing (12 rounds)                     │
│  ✓ Constant-time verification                              │
│  ✓ Generic error messages                                  │
└────────────────────────────────────────────────────────────┘
                           │
┌────────────────────────────────────────────────────────────┐
│ Layer 6: Input Validation (Backend)                         │
│  ✓ Body validation (express-validator)                     │
│  ✓ URL parameter validation (regex)                        │
│  ✓ SQL injection prevention (parameterized)                │
│  ✓ Date range validation                                   │
└────────────────────────────────────────────────────────────┘
                           │
┌────────────────────────────────────────────────────────────┐
│ Layer 7: Rate Limiting (Backend)                            │
│  ✓ Auth: 5 requests/15 min                                 │
│  ✓ Plaid: 20 requests/min                                  │
│  ✓ API: 100 requests/15 min                                │
│  ✓ IP-based tracking                                       │
└────────────────────────────────────────────────────────────┘
                           │
┌────────────────────────────────────────────────────────────┐
│ Layer 8: Build Security                                     │
│  ✓ Auto pin extraction                                     │
│  ✓ Production build enforcement                            │
│  ✓ Pin validation                                          │
│  ✓ Server dependency check                                 │
└────────────────────────────────────────────────────────────┘
```

## Deployment Architecture

### Development Environment

```
┌──────────────────┐         ┌──────────────────┐
│   Developer      │         │   Developer      │
│   Machine        │         │   Machine        │
│                  │         │                  │
│  ┌────────────┐  │         │  ┌────────────┐  │
│  │  Android   │  │         │  │  Backend   │  │
│  │  Emulator  │  │         │  │  Node.js   │  │
│  │            │  │         │  │  :3000     │  │
│  └─────┬──────┘  │         │  └─────┬──────┘  │
│        │         │         │        │         │
└────────┼─────────┘         └────────┼─────────┘
         │                            │
         │ HTTP://10.0.2.2:3000      │
         └────────────┬───────────────┘
                      │
           ┌──────────▼─────────┐
           │   Plaid Sandbox    │
           │   (Test Data)      │
           └────────────────────┘
```

### Production Environment

```
┌──────────────────────────────────────────────────────────┐
│                    Google Play Store                      │
│              (APK Distribution)                           │
└───────────────────────┬──────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
┌───────▼──────┐ ┌──────▼─────┐ ┌──────▼──────┐
│  User Device │ │User Device │ │ User Device │
│  (Android)   │ │ (Android)  │ │  (Android)  │
└───────┬──────┘ └──────┬─────┘ └──────┬──────┘
        │               │               │
        │ HTTPS + Certificate Pinning   │
        └───────────────┼───────────────┘
                        │
        ┌───────────────▼───────────────┐
        │      CloudFlare CDN           │
        │   (DDoS Protection, SSL)      │
        └───────────────┬───────────────┘
                        │
        ┌───────────────▼───────────────┐
        │   Load Balancer (Optional)    │
        └───────────────┬───────────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
┌───────▼──────┐ ┌──────▼─────┐ ┌──────▼──────┐
│Backend Server│ │Backend     │ │Backend      │
│  Instance 1  │ │Instance 2  │ │Instance 3   │
│  (Node.js)   │ │(Node.js)   │ │(Node.js)    │
└───────┬──────┘ └──────┬─────┘ └──────┬──────┘
        │               │               │
        └───────────────┼───────────────┘
                        │
        ┌───────────────▼───────────────┐
        │   PostgreSQL Database         │
        │   (Managed, Encrypted)        │
        └───────────────┬───────────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
┌───────▼──────┐ ┌──────▼─────┐ ┌──────▼──────┐
│  Plaid API   │ │ OpenAI API │ │  Sentry     │
│ (Production) │ │(LLM Detect)│ │ (Monitoring)│
└──────────────┘ └────────────┘ └─────────────┘
```

---

**See Also**:
- [System Overview](../architecture/system-overview.md)
- [Data Flow](../architecture/data-flow.md)
- [Security Architecture](../security/overview.md)

