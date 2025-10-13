# Plaid Integration API

Endpoints for Plaid bank integration and account management.

## Endpoints

### Create Link Token

Initialize Plaid Link flow.

**POST** `/api/v1/plaid/create_link_token`

**Headers**: `Authorization: Bearer <token>`

**Response**:
```json
{
  "link_token": "link-sandbox-abc123...",
  "expiration": "2025-10-13T16:00:00Z"
}
```

---

### Exchange Public Token

Exchange Plaid public token for access token.

**POST** `/api/v1/plaid/exchange_public_token`

**Request**:
```json
{
  "public_token": "public-sandbox-abc123..."
}
```

**Response**:
```json
{
  "itemId": "item-abc123",
  "success": true
}
```

---

### Get Accounts

Fetch all linked credit card accounts.

**GET** `/api/v1/plaid/accounts`

**Headers**: `Authorization: Bearer <token>`

**Response**:
```json
[
  {
    "accountId": "account-abc123",
    "name": "Chase Freedom",
    "mask": "1234",
    "balances": {
      "available": 8500.00,
      "current": 1500.00,
      "limit": 10000.00
    },
    "statementGenerationDay": 18
  }
]
```

---

### Get Transactions

Fetch transaction history.

**POST** `/api/v1/plaid/transactions`

**Request**:
```json
{
  "start_date": "2025-09-01",
  "end_date": "2025-10-01"
}
```

**Response**:
```json
{
  "transactions": [
    {
      "transaction_id": "tx-abc123",
      "account_id": "account-abc123",
      "amount": 45.67,
      "date": "2025-09-15",
      "name": "Amazon.com"
    }
  ]
}
```

---

### Remove Item

Disconnect a linked account.

**DELETE** `/api/v1/plaid/items/:itemId`

**Response**:
```json
{
  "success": true
}
```

---

## Complete API Reference

See [Backend API Documentation](../../backend/API.md) for complete details.

**Related**:
- [API Overview](overview.md)
- [Authentication](authentication.md)

