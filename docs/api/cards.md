# Card Management API

Endpoints for credit card configuration and management.

## Endpoints

### Get All Cards

**GET** `/api/v1/cards`

**Headers**: `Authorization: Bearer <token>`

**Response**:
```json
[
  {
    "id": 1,
    "account_id": "account-abc123",
    "name": "Chase Freedom",
    "mask": "1234",
    "credit_limit": 10000.00,
    "statement_date": 15,
    "due_date": 10
  }
]
```

---

### Get Specific Card

**GET** `/api/v1/cards/:accountId`

**Response**:
```json
{
  "id": 1,
  "account_id": "account-abc123",
  "name": "Chase Freedom",
  "statement_date": 15
}
```

---

### Save Card Configuration

**POST** `/api/v1/cards`

**Request**:
```json
{
  "account_id": "account-abc123",
  "name": "Chase Freedom",
  "statement_date": 15,
  "due_date": 10
}
```

---

### Delete Card

**DELETE** `/api/v1/cards/:accountId`

**Response**:
```json
{
  "success": true,
  "message": "Credit card deleted"
}
```

---

**See Also**: [API Overview](overview.md)

