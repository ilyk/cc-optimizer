# Error Handling

API error responses and status codes.

## Error Format

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

| Code | Meaning | Description |
|------|---------|-------------|
| **200** | OK | Request successful |
| **201** | Created | Resource created |
| **400** | Bad Request | Invalid input |
| **401** | Unauthorized | Invalid/missing token |
| **403** | Forbidden | Insufficient permissions |
| **404** | Not Found | Resource not found |
| **429** | Too Many Requests | Rate limited |
| **500** | Internal Server Error | Server error |

## Common Errors

### Validation Error (400)
```json
{
  "error": "Validation failed",
  "details": [
    {
      "field": "password",
      "message": "Must be at least 8 characters"
    }
  ]
}
```

### Authentication Error (401)
```json
{
  "error": "Invalid or expired token"
}
```

### Rate Limit Error (429)
```json
{
  "error": "Too many requests, please try again later"
}
```

## Error Handling

### Client-side

```kotlin
try {
    val response = api.getAccounts()
} catch (e: HttpException) {
    when (e.code()) {
        401 -> showLoginScreen()
        429 -> useCachedData()
        else -> showError(e.message())
    }
}
```

---

**See Also**: [API Overview](overview.md)

