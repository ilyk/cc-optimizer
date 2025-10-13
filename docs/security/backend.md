# Backend Security

Server-side security implementation and hardening.

## Security Features

### 1. Authentication

**JWT with HS256**:
```javascript
const token = jwt.sign(
    { userId },
    process.env.JWT_SECRET,
    {
        expiresIn: '7d',
        algorithm: 'HS256'  // Enforced
    }
);
```

**Password Hashing**:
```javascript
// bcrypt with 12 rounds
const hash = await bcrypt.hash(password, 12);

// Constant-time comparison
const valid = await bcrypt.compare(password, hash);
```

### 2. Input Validation

**express-validator**:
```javascript
const registerValidation = [
    body('email').isEmail().normalizeEmail(),
    body('password')
        .isLength({ min: 8 })
        .matches(/[A-Z]/)
        .matches(/[a-z]/)
        .matches(/[0-9]/)
];
```

**SQL Injection Prevention**:
```javascript
// ✅ Parameterized queries
db.prepare('SELECT * FROM users WHERE id = ?').get(userId);

// ❌ NEVER do this
db.exec(`SELECT * FROM users WHERE id = ${userId}`);
```

### 3. Rate Limiting

**3-Tier System**:
```javascript
// Auth: 5 requests per 15 minutes
const authLimiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 5
});

// Plaid: 20 requests per minute
const plaidLimiter = rateLimit({
    windowMs: 60 * 1000,
    max: 20
});

// API: 100 requests per 15 minutes
const apiLimiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 100
});
```

### 4. Security Headers

**Helmet middleware**:
```javascript
app.use(helmet({
    contentSecurityPolicy: true,
    hsts: true,
    noSniff: true,
    frameguard: { action: 'deny' }
}));
```

**Headers added**:
- `Strict-Transport-Security`
- `X-Content-Type-Options`
- `X-Frame-Options`
- `X-XSS-Protection`

### 5. CORS Configuration

**Restricted origins**:
```javascript
app.use(cors({
    origin: process.env.CORS_ORIGIN?.split(','),
    credentials: false,
    methods: ['GET', 'POST', 'DELETE']
}));
```

### 6. Webhook Verification

**HMAC-SHA256**:
```javascript
function verifyWebhook(req) {
    const signature = req.headers['plaid-verification'];
    const body = req.rawBody;
    
    const hash = crypto
        .createHmac('sha256', process.env.PLAID_SECRET)
        .update(body)
        .digest('hex');
    
    return crypto.timingSafeEqual(
        Buffer.from(signature),
        Buffer.from(hash)
    );
}
```

## Security Checklist

- [ ] JWT HS256 enforced
- [ ] bcrypt password hashing (12 rounds)
- [ ] Input validation on all endpoints
- [ ] SQL injection prevention (parameterized queries)
- [ ] Rate limiting enabled
- [ ] CORS properly configured
- [ ] Security headers (Helmet)
- [ ] Webhook signature verification
- [ ] Generic error messages
- [ ] Logging enabled

## Vulnerabilities Fixed

| # | Vulnerability | Fix |
|---|--------------|-----|
| 1 | JWT algorithm confusion | Algorithm whitelist |
| 2 | SQL injection | Parameterized queries |
| 3 | User enumeration | Generic errors |
| 4 | Timing attacks | Constant-time comparison |
| 5 | Weak passwords | Strong validation |

---

**See Also**:
- [Security Overview](overview.md)
- [Backend Architecture](../architecture/backend.md)
- [Security Audit](audit.md)
- [Backend Security Documentation](../../backend/SECURITY_AUDIT.md)

