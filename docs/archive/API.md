# API Documentation

## Base URL

```
http://localhost:3000
```

## Authentication

Most endpoints require authentication using JWT Bearer token:

```
Authorization: Bearer <your_jwt_token>
```

Get the token by registering or logging in via the Auth endpoints.

---

## Authentication Endpoints

### Register User

Create a new user account.

**Endpoint:** `POST /api/auth/register`

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123"
}
```

**Response:** `201 Created`
```json
{
  "user": {
    "id": 1,
    "email": "user@example.com"
  },
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Validation Rules:**
- Email must be valid
- Password must be at least 8 characters
- Password must contain uppercase, lowercase, and number

---

### Login

Authenticate and get JWT token.

**Endpoint:** `POST /api/auth/login`

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123"
}
```

**Response:** `200 OK`
```json
{
  "user": {
    "id": 1,
    "email": "user@example.com"
  },
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

---

### Get Profile

Get current user's profile.

**Endpoint:** `GET /api/auth/profile`

**Headers:**
```
Authorization: Bearer <token>
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "email": "user@example.com",
  "created_at": "2025-10-13T12:00:00.000Z"
}
```

---

## Plaid Integration Endpoints

### Create Link Token

Create a Plaid Link token to initialize Plaid Link in your app.

**Endpoint:** `POST /api/plaid/create_link_token`

**Headers:**
```
Authorization: Bearer <token>
```

**Response:** `200 OK`
```json
{
  "link_token": "link-sandbox-abc123...",
  "expiration": "2025-10-13T16:00:00Z"
}
```

---

### Exchange Public Token

Exchange the public token from Plaid Link for an access token.

**Endpoint:** `POST /api/plaid/exchange_public_token`

**Headers:**
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "public_token": "public-sandbox-abc123..."
}
```

**Response:** `200 OK`
```json
{
  "itemId": "item-abc123",
  "success": true
}
```

---

### Get Accounts

Get all linked credit card accounts.

**Endpoint:** `GET /api/plaid/accounts`

**Headers:**
```
Authorization: Bearer <token>
```

**Response:** `200 OK`
```json
[
  {
    "accountId": "account-abc123",
    "name": "Chase Freedom",
    "officialName": "Chase Freedom Unlimited®",
    "mask": "1234",
    "type": "credit",
    "subtype": "credit card",
    "balances": {
      "available": 8500.00,
      "current": 1500.00,
      "limit": 10000.00
    }
  }
]
```

---

### Get Transactions

Get transactions for all linked accounts.

**Endpoint:** `POST /api/plaid/transactions`

**Headers:**
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "start_date": "2025-09-01",
  "end_date": "2025-10-01"
}
```

**Response:** `200 OK`
```json
{
  "transactions": [
    {
      "transaction_id": "tx-abc123",
      "account_id": "account-abc123",
      "amount": 45.67,
      "date": "2025-09-15",
      "name": "Amazon.com",
      "merchant_name": "Amazon",
      "category": ["Shops", "Online Shopping"]
    }
  ],
  "accounts": [...]
}
```

---

### Remove Plaid Item

Disconnect a linked bank/card account.

**Endpoint:** `DELETE /api/plaid/items/:itemId`

**Headers:**
```
Authorization: Bearer <token>
```

**Response:** `200 OK`
```json
{
  "success": true
}
```

---

## Credit Card Configuration Endpoints

### Get All Credit Cards

Get all credit card configurations for the user.

**Endpoint:** `GET /api/cards`

**Headers:**
```
Authorization: Bearer <token>
```

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "account_id": "account-abc123",
    "name": "Chase Freedom",
    "mask": "1234",
    "credit_limit": 10000.00,
    "statement_date": 15,
    "due_date": 10,
    "auto_pay_enabled": true,
    "auto_pay_amount": 1500.00,
    "created_at": "2025-10-13T12:00:00.000Z"
  }
]
```

---

### Get Specific Credit Card

Get configuration for a specific credit card.

**Endpoint:** `GET /api/cards/:accountId`

**Headers:**
```
Authorization: Bearer <token>
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "account_id": "account-abc123",
  "name": "Chase Freedom",
  "mask": "1234",
  "credit_limit": 10000.00,
  "statement_date": 15,
  "due_date": 10,
  "auto_pay_enabled": true,
  "auto_pay_amount": 1500.00
}
```

---

### Save Credit Card Configuration

Create or update credit card configuration.

**Endpoint:** `POST /api/cards`

**Headers:**
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "account_id": "account-abc123",
  "name": "Chase Freedom",
  "mask": "1234",
  "credit_limit": 10000.00,
  "statement_date": 15,
  "due_date": 10,
  "auto_pay_enabled": true,
  "auto_pay_amount": 1500.00
}
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "account_id": "account-abc123",
  "name": "Chase Freedom",
  ...
}
```

**Validation Rules:**
- `account_id`: Required
- `name`: Required, max 255 characters
- `statement_date`: Optional, 1-31
- `due_date`: Optional, 1-31
- `auto_pay_enabled`: Optional, boolean
- `auto_pay_amount`: Optional, positive number

---

### Delete Credit Card Configuration

Delete a credit card configuration.

**Endpoint:** `DELETE /api/cards/:accountId`

**Headers:**
```
Authorization: Bearer <token>
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Credit card deleted"
}
```

---

## Webhook Endpoint

### Plaid Webhook

Receives Plaid webhook notifications.

**Endpoint:** `POST /api/plaid/webhook`

**Headers:**
```
Plaid-Verification: <signature>
Content-Type: application/json
```

**Request Body:**
```json
{
  "webhook_type": "TRANSACTIONS",
  "webhook_code": "DEFAULT_UPDATE",
  "item_id": "item-abc123",
  "new_transactions": 5
}
```

**Response:** `200 OK`
```json
{
  "status": "received"
}
```

---

## Error Responses

### 400 Bad Request
```json
{
  "error": "Validation failed",
  "details": [
    {
      "field": "email",
      "message": "Invalid email address"
    }
  ]
}
```

### 401 Unauthorized
```json
{
  "error": "Invalid or expired token"
}
```

### 404 Not Found
```json
{
  "error": "Not found",
  "path": "/api/unknown"
}
```

### 429 Too Many Requests
```json
{
  "error": "Too many requests, please try again later"
}
```

### 500 Internal Server Error
```json
{
  "error": "Internal server error"
}
```

---

## Rate Limits

- **General API**: 100 requests per 15 minutes
- **Authentication**: 5 requests per 15 minutes
- **Plaid API**: 20 requests per minute

---

## Testing with cURL

### Register and Login
```bash
# Register
curl -X POST http://localhost:3000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"TestPass123"}'

# Login
curl -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"TestPass123"}'
```

### Use API with Token
```bash
# Get profile
curl http://localhost:3000/api/auth/profile \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"

# Create link token
curl -X POST http://localhost:3000/api/plaid/create_link_token \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

