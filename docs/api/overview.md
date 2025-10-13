# API Overview

Complete API reference for the Credit Card Optimizer backend.

## Base URL

```
Development: http://localhost:3000
Production: https://your-backend-domain.com
```

## API Versioning

The API uses versioned endpoints to ensure backward compatibility:

```
/api/v1/*  - Current stable version
/api/*     - Legacy (redirects to v1, deprecated)
```

**Deprecation headers** on legacy endpoints:
```
X-API-Deprecated: true
X-API-Version: v1
Deprecation: version="legacy"
Sunset: <date 90 days from now>
```

## Authentication

Most endpoints require JWT authentication:

```http
Authorization: Bearer <your_jwt_token>
```

**Get token by**:
1. Register: `POST /api/v1/auth/register`
2. Login: `POST /api/v1/auth/login`

**Token expiration**: 7 days

## Content Type

All state-changing endpoints require:
```http
Content-Type: application/json
```

## Rate Limits

| Endpoint Type | Limit | Window |
|--------------|-------|--------|
| **Authentication** | 5 requests | 15 minutes |
| **Plaid API** | 20 requests | 1 minute |
| **General API** | 100 requests | 15 minutes |

**Rate limit headers**:
```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1234567890
```

**Rate limited response** (429):
```json
{
  "error": "Too many requests, please try again later"
}
```

## Response Format

### Success Response

```json
{
  "data": { ... },
  "message": "Success"
}
```

Or just the data object directly.

### Error Response

```json
{
  "error": "Error message",
  "details": [
    {
      "field": "email",
      "message": "Invalid email address"
    }
  ]
}
```

## Status Codes

| Code | Meaning |
|------|---------|
| **200** | OK - Request successful |
| **201** | Created - Resource created successfully |
| **400** | Bad Request - Invalid input/validation error |
| **401** | Unauthorized - Missing or invalid token |
| **403** | Forbidden - Valid token but insufficient permissions |
| **404** | Not Found - Resource doesn't exist |
| **429** | Too Many Requests - Rate limited |
| **500** | Internal Server Error - Server error |

## Endpoints

### Authentication

- [POST /api/v1/auth/register](authentication.md#register) - Create new user
- [POST /api/v1/auth/login](authentication.md#login) - Login user
- [GET /api/v1/auth/profile](authentication.md#profile) - Get user profile

### Plaid Integration

- [POST /api/v1/plaid/create_link_token](plaid.md#create-link-token) - Create Plaid Link token
- [POST /api/v1/plaid/exchange_public_token](plaid.md#exchange-public-token) - Exchange public token
- [GET /api/v1/plaid/accounts](plaid.md#get-accounts) - Get accounts
- [POST /api/v1/plaid/transactions](plaid.md#get-transactions) - Get transactions
- [DELETE /api/v1/plaid/items/:itemId](plaid.md#remove-item) - Remove Plaid item
- [POST /api/v1/plaid/webhook](plaid.md#webhook) - Plaid webhook handler

### Credit Card Management

- [GET /api/v1/cards](cards.md#get-all-cards) - Get all credit cards
- [GET /api/v1/cards/:accountId](cards.md#get-card) - Get specific card
- [POST /api/v1/cards](cards.md#save-card) - Save/update card configuration
- [DELETE /api/v1/cards/:accountId](cards.md#delete-card) - Delete card configuration

### Security

- [GET /api/v1/security/certificate-pins](../security/backend.md#certificate-pins) - Get certificate pins

### Health Check

- [GET /health](#health-check-endpoint) - Server health check (no auth required)

## Common Headers

### Request Headers

```http
Content-Type: application/json
Authorization: Bearer <token>
```

### Response Headers

```http
Content-Type: application/json
X-API-Version: v1
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1234567890
```

**Security headers** (from Helmet):
```http
Strict-Transport-Security: max-age=15552000; includeSubDomains
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
```

## Error Handling

### Validation Errors (400)

```json
{
  "error": "Validation failed",
  "details": [
    {
      "field": "email",
      "message": "Invalid email address"
    },
    {
      "field": "password",
      "message": "Password must be at least 8 characters"
    }
  ]
}
```

### Authentication Errors (401)

```json
{
  "error": "Invalid or expired token"
}
```

### Not Found Errors (404)

```json
{
  "error": "Not found",
  "path": "/api/v1/unknown"
}
```

### Server Errors (500)

```json
{
  "error": "Internal server error"
}
```

*Details are hidden in production to prevent information leakage.*

## Pagination

For endpoints that return large datasets (future):

**Request**:
```http
GET /api/v1/transactions?page=2&limit=50
```

**Response**:
```json
{
  "data": [...],
  "pagination": {
    "page": 2,
    "limit": 50,
    "total": 500,
    "pages": 10
  }
}
```

## Caching

### Client-side Caching

App implements smart caching:
- Accounts cached for 5 minutes
- Transactions cached for 1 hour
- Liabilities cached for 1 day

### Server-side Caching

Backend uses in-memory cache:
- Statement date analysis cached for 30 days
- LLM results cached for 30 days

**Cache headers**:
```http
X-Cache-Strategy: cache-only
X-Rate-Limit-Info: Using cached data due to rate limit
```

## Webhooks

### Plaid Webhook

Plaid sends webhooks for account events:

**Verification**:
- HMAC-SHA256 signature in `Plaid-Verification` header
- Validates against raw request body

**Events handled**:
- `ITEM.ERROR` - Item error
- `ITEM.PENDING_EXPIRATION` - Item about to expire
- `TRANSACTIONS.DEFAULT_UPDATE` - New transactions available
- `AUTH.AUTOMATICALLY_VERIFIED` - Auth verified

See [Plaid Webhooks](plaid.md#webhook) for details.

## Health Check Endpoint

### GET /health

Check if server is running (no authentication required).

**Response**: 200 OK
```json
{
  "status": "ok",
  "message": "Credit Card Optimizer Backend is running",
  "timestamp": "2025-10-13T12:00:00.000Z",
  "environment": "sandbox",
  "apiVersion": "v1",
  "supportedVersions": ["v1"]
}
```

## Testing with cURL

### Register and Login

```bash
# Register
curl -X POST http://localhost:3000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "TestPass123"
  }'

# Login
curl -X POST http://localhost:3000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "TestPass123"
  }'
```

### Use API with Token

```bash
# Save token from login response
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Get profile
curl http://localhost:3000/api/v1/auth/profile \
  -H "Authorization: Bearer $TOKEN"

# Create link token
curl -X POST http://localhost:3000/api/v1/plaid/create_link_token \
  -H "Authorization: Bearer $TOKEN"

# Get accounts
curl http://localhost:3000/api/v1/plaid/accounts \
  -H "Authorization: Bearer $TOKEN"
```

## Testing with Postman

Import collection:

```json
{
  "info": {
    "name": "CC Optimizer API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "auth": {
    "type": "bearer",
    "bearer": [
      {
        "key": "token",
        "value": "{{jwt_token}}",
        "type": "string"
      }
    ]
  },
  "variable": [
    {
      "key": "base_url",
      "value": "http://localhost:3000/api/v1"
    },
    {
      "key": "jwt_token",
      "value": ""
    }
  ]
}
```

## SDKs and Libraries

### Official Client Libraries

**None yet** - Use HTTP client of your choice

**Recommended**:
- **Android**: Retrofit + OkHttp (used in this project)
- **iOS**: URLSession / Alamofire
- **Web**: fetch / axios
- **Node.js**: axios / got
- **Python**: requests / httpx

### Example HTTP Client (Retrofit)

```kotlin
interface CreditCardApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse
    
    @GET("auth/profile")
    suspend fun getProfile(): UserProfile
    
    @POST("plaid/create_link_token")
    suspend fun createLinkToken(): LinkTokenResponse
    
    @GET("plaid/accounts")
    suspend fun getAccounts(): List<Account>
}

val retrofit = Retrofit.Builder()
    .baseUrl("http://localhost:3000/api/v1/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val api = retrofit.create(CreditCardApi::class.java)
```

## Best Practices

### 1. Always use HTTPS in production

```kotlin
// Development
private const val BASE_URL = "http://localhost:3000/"

// Production
private const val BASE_URL = "https://api.your-domain.com/"
```

### 2. Implement retry logic

```kotlin
suspend fun <T> retryRequest(
    times: Int = 3,
    delay: Long = 1000,
    block: suspend () -> T
): T {
    repeat(times - 1) { attempt ->
        try {
            return block()
        } catch (e: IOException) {
            delay(delay * (attempt + 1))
        }
    }
    return block() // Last attempt
}
```

### 3. Handle rate limits gracefully

```kotlin
if (response.code() == 429) {
    // Serve cached data
    return cachedData
}
```

### 4. Validate input on client-side

```kotlin
fun validateEmail(email: String): Boolean {
    return Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

fun validatePassword(password: String): Boolean {
    return password.length >= 8 &&
           password.any { it.isUpperCase() } &&
           password.any { it.isLowerCase() } &&
           password.any { it.isDigit() }
}
```

### 5. Store tokens securely

```kotlin
// Use EncryptedSharedPreferences
val encryptedPrefs = EncryptedSharedPreferences.create(...)
encryptedPrefs.edit {
    putString("jwt_token", token)
}
```

## Detailed Documentation

- [Authentication API](authentication.md) - Auth endpoints
- [Plaid API](plaid.md) - Bank integration endpoints
- [Card Management API](cards.md) - Credit card configuration
- [Error Handling](errors.md) - Error codes and handling

## Support

For API questions or issues:
1. Check this documentation
2. Review [FAQ](../development/faq.md)
3. Check [Troubleshooting Guide](../development/troubleshooting.md)

---

**API Version**: v1  
**Last Updated**: October 2025

