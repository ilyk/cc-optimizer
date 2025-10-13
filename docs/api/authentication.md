# Authentication API

JWT-based authentication endpoints for user registration and login.

## Endpoints

### Register User

Create a new user account.

**Endpoint**: `POST /api/v1/auth/register`

**Headers**:
```http
Content-Type: application/json
```

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "SecurePass123"
}
```

**Validation Rules**:
- `email`: Valid email format
- `password`: Minimum 8 characters, must contain uppercase, lowercase, and number

**Response**: `201 Created`
```json
{
  "user": {
    "id": 1,
    "email": "user@example.com"
  },
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Errors**:
- `400`: Validation failed
- `409`: User already exists

---

### Login

Authenticate and receive JWT token.

**Endpoint**: `POST /api/v1/auth/login`

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "SecurePass123"
}
```

**Response**: `200 OK`
```json
{
  "user": {
    "id": 1,
    "email": "user@example.com"
  },
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Errors**:
- `400`: Validation failed
- `401`: Invalid credentials

---

### Get Profile

Get current user's profile.

**Endpoint**: `GET /api/v1/auth/profile`

**Headers**:
```http
Authorization: Bearer <token>
```

**Response**: `200 OK`
```json
{
  "id": 1,
  "email": "user@example.com",
  "created_at": "2025-10-13T12:00:00.000Z"
}
```

**Errors**:
- `401`: Invalid or expired token

---

## JWT Token

### Format
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.    # Header
eyJ1c2VySWQiOjEsImlhdCI6MTYzMDAwMDAwMH0.  # Payload
SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c  # Signature
```

### Payload
```json
{
  "userId": 1,
  "iat": 1630000000,
  "exp": 1630604800
}
```

### Properties
- **Algorithm**: HS256 (enforced)
- **Expiration**: 7 days
- **Secret**: From `JWT_SECRET` environment variable

### Usage

Include in `Authorization` header:
```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## Security

### Password Hashing
- **Algorithm**: bcrypt
- **Rounds**: 12
- **Salt**: Automatically generated

### Token Security
- Algorithm whitelisting (only HS256)
- Expiration enforcement
- Signature validation

### Error Messages
Generic errors to prevent user enumeration:
- ✅ "Invalid credentials"
- ❌ "User not found"
- ❌ "Wrong password"

---

## Examples

### cURL

**Register**:
```bash
curl -X POST http://localhost:3000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "TestPass123"
  }'
```

**Login**:
```bash
curl -X POST http://localhost:3000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "TestPass123"
  }'
```

**Get Profile**:
```bash
TOKEN="your-jwt-token"

curl http://localhost:3000/api/v1/auth/profile \
  -H "Authorization: Bearer $TOKEN"
```

### JavaScript (fetch)

```javascript
// Register
const response = await fetch('/api/v1/auth/register', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    email: 'test@example.com',
    password: 'TestPass123'
  })
});

const { user, token } = await response.json();
localStorage.setItem('token', token);

// Use token
const profile = await fetch('/api/v1/auth/profile', {
  headers: { 'Authorization': `Bearer ${token}` }
});
```

### Android (Retrofit)

```kotlin
interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse
    
    @GET("auth/profile")
    suspend fun getProfile(): UserProfile
}

// Usage
val response = api.register(RegisterRequest(email, password))
secureStorage.saveToken(response.token)
```

---

**See Also**:
- [API Overview](overview.md)
- [Backend Security](../security/backend.md)
- [Error Handling](errors.md)

