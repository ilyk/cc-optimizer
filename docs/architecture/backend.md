# Backend Architecture

Complete backend server architecture and implementation details.

## Overview

The backend is a Node.js/Express REST API that serves as a secure proxy for Plaid API calls and manages user authentication and credit card configurations.

**Tech Stack**:
- **Runtime**: Node.js 18+
- **Framework**: Express.js 4
- **Database**: SQLite (dev) / PostgreSQL (production)
- **Authentication**: JWT + bcrypt
- **API**: Plaid Node SDK 18+
- **AI/LLM**: OpenAI / Anthropic SDK (optional)

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                     Express Server                           │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │              Middleware Stack                          │ │
│  │  1. Helmet (security headers)                          │ │
│  │  2. CORS (cross-origin control)                        │ │
│  │  3. Body parser (JSON parsing)                         │ │
│  │  4. Morgan (request logging)                           │ │
│  │  5. Rate limiting (3-tier)                             │ │
│  │  6. Authentication (JWT validation)                    │ │
│  │  7. Validation (input sanitization)                    │ │
│  │  8. Error handler (global)                             │ │
│  └────────────────────────────────────────────────────────┘ │
│                              │                               │
│  ┌────────────────────────────▼──────────────────────────┐ │
│  │              API Router (v1)                          │ │
│  │                                                       │ │
│  │  /api/v1/auth/*      → Authentication routes         │ │
│  │  /api/v1/plaid/*     → Plaid integration routes      │ │
│  │  /api/v1/cards/*     → Card config routes            │ │
│  │  /api/v1/security/*  → Security routes               │ │
│  └───────────────┬─────────────┬─────────────┬──────────┘ │
│                  │             │             │             │
│  ┌───────────────▼───┐ ┌──────▼──────┐ ┌────▼──────────┐ │
│  │  Auth Service   │ │ Plaid Service│ │ LLM Service   │ │
│  │  - register     │ │ - createLink │ │ - analyzeTx   │ │
│  │  - login        │ │ - exchange   │ │ - detectDate  │ │
│  │  - verify       │ │ - accounts   │ └───────────────┘ │
│  └────────┬────────┘ └──────┬───────┘                    │
│           │                 │                             │
│  ┌────────▼─────────────────▼──────────────────────────┐ │
│  │              Repository Layer                        │ │
│  │  - UserRepository (user CRUD)                        │ │
│  │  - PlaidRepository (Plaid data, webhooks)            │ │
│  │  - CardRepository (card configurations)              │ │
│  └────────┬──────────────────────────────────────────────┘ │
│           │                                               │
│  ┌────────▼──────────────────────────────────────────┐   │
│  │           Database Layer                          │   │
│  │  - SQLite (development)                           │   │
│  │  - PostgreSQL (production)                        │   │
│  │  - better-sqlite3 / pg (drivers)                  │   │
│  └───────────────────────────────────────────────────┘   │
└──────────────────────┬────────────────────────────────────┘
                       │
        ┌──────────────┼──────────────┐
        │              │              │
   ┌────▼─────┐  ┌────▼─────┐  ┌────▼─────┐
   │  Plaid   │  │ OpenAI   │  │ Database │
   │   API    │  │   API    │  │  (SQLite/│
   │          │  │          │  │  Postgres)│
   └──────────┘  └──────────┘  └──────────┘
```

## Project Structure

```
backend/
├── server.js                      # Main entry point
├── package.json                   # Dependencies
├── .env                          # Environment variables (gitignored)
├── .env.example                  # Environment template
│
├── src/
│   ├── config/                   # Configuration
│   │   ├── database.js           # Database config
│   │   └── monitoring.js         # Monitoring config
│   │
│   ├── database/                 # Database layer
│   │   └── connection.js         # Database connection
│   │
│   ├── middleware/               # Express middleware
│   │   ├── auth.js               # JWT authentication
│   │   ├── errorHandler.js       # Error handling
│   │   ├── paramValidation.js    # URL param validation
│   │   ├── rateLimiter.js        # Rate limiting
│   │   ├── smartRateLimit.js     # Smart rate limiting
│   │   └── validation.js         # Request validation
│   │
│   ├── repositories/             # Data access layer
│   │   ├── plaidRepository.js    # Plaid data operations
│   │   └── userRepository.js     # User data operations
│   │
│   ├── routes/                   # Route handlers
│   │   └── security.js           # Security routes
│   │
│   ├── services/                 # Business logic
│   │   ├── analytics.js          # Analytics service
│   │   ├── authService.js        # Authentication logic
│   │   ├── errorTracking.js      # Error tracking
│   │   ├── llmService.js         # LLM integration
│   │   ├── metrics.js            # Metrics collection
│   │   └── plaidService.js       # Plaid business logic
│   │
│   └── utils/                    # Utilities
│       ├── cache.js              # Caching utilities
│       ├── encryption.js         # Encryption utilities
│       ├── logger.js             # Winston logger
│       ├── retry.js              # Retry logic
│       └── webhookVerification.js # Webhook verification
│
├── data/                         # Database files (gitignored)
│   └── cc_optimizer.db           # SQLite database
│
├── logs/                         # Log files (gitignored)
│   └── app.log
│
└── scripts/                      # Setup scripts
    ├── generateKeys.js           # Generate JWT secret
    ├── setup.sh                  # Unix setup
    └── setup.bat                 # Windows setup
```

## Key Components

### 1. Server Entry Point (server.js)

```javascript
require('dotenv').config();
const express = require('express');
const helmet = require('helmet');
const cors = require('cors');
const morgan = require('morgan');

// Import middleware and routes
const { authMiddleware } = require('./src/middleware/auth');
const { apiLimiter, authLimiter, plaidLimiter } = require('./src/middleware/rateLimiter');
const { errorHandler, notFoundHandler } = require('./src/middleware/errorHandler');
const db = require('./src/database/connection');

const app = express();

// Security middleware
app.use(helmet());
app.use(cors({
    origin: process.env.CORS_ORIGIN?.split(',') || [],
    credentials: false
}));

// Body parsing
app.use(express.json({ limit: '512kb' }));

// Logging
app.use(morgan('combined'));

// Health check
app.get('/health', (req, res) => {
    res.json({
        status: 'ok',
        apiVersion: 'v1',
        environment: process.env.PLAID_ENV
    });
});

// API routes
const apiV1 = express.Router();
apiV1.use('/auth', authRoutes);
apiV1.use('/plaid', plaidRoutes);
apiV1.use('/cards', cardRoutes);
app.use('/api/v1', apiV1);

// Error handling
app.use(notFoundHandler);
app.use(errorHandler);

// Start server
async function startServer() {
    await db.initialize();
    app.listen(process.env.PORT || 3000);
}

startServer();
```

### 2. Middleware Layer

**Authentication** (`middleware/auth.js`):
```javascript
const jwt = require('jsonwebtoken');

function authMiddleware(req, res, next) {
    const authHeader = req.headers.authorization;
    
    if (!authHeader?.startsWith('Bearer ')) {
        return res.status(401).json({ error: 'No token provided' });
    }
    
    const token = authHeader.substring(7);
    
    try {
        const decoded = jwt.verify(token, process.env.JWT_SECRET, {
            algorithms: ['HS256']  // Prevent algorithm confusion
        });
        
        req.userId = decoded.userId;
        next();
    } catch (error) {
        return res.status(401).json({ error: 'Invalid token' });
    }
}
```

**Rate Limiting** (`middleware/rateLimiter.js`):
```javascript
const rateLimit = require('express-rate-limit');

// Auth endpoints: 5 requests per 15 minutes
const authLimiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 5,
    skipSuccessfulRequests: true  // Only count failed attempts
});

// Plaid endpoints: 20 requests per minute
const plaidLimiter = rateLimit({
    windowMs: 60 * 1000,
    max: 20
});

// General API: 100 requests per 15 minutes
const apiLimiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 100
});
```

**Validation** (`middleware/validation.js`):
```javascript
const { body, validationResult } = require('express-validator');

const registerValidation = [
    body('email')
        .isEmail()
        .normalizeEmail()
        .trim(),
    body('password')
        .isLength({ min: 8 })
        .matches(/[A-Z]/)  // At least one uppercase
        .matches(/[a-z]/)  // At least one lowercase
        .matches(/[0-9]/), // At least one digit
    (req, res, next) => {
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({
                error: 'Validation failed',
                details: errors.array()
            });
        }
        next();
    }
];
```

### 3. Service Layer

**Auth Service** (`services/authService.js`):
```javascript
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const userRepository = require('../repositories/userRepository');

async function register(email, password) {
    // Check if user exists
    const existing = await userRepository.findByEmail(email);
    if (existing) {
        throw new Error('User already exists');
    }
    
    // Hash password
    const passwordHash = await bcrypt.hash(password, 12);
    
    // Create user
    const user = await userRepository.create(email, passwordHash);
    
    // Generate JWT
    const token = jwt.sign(
        { userId: user.id },
        process.env.JWT_SECRET,
        {
            expiresIn: '7d',
            algorithm: 'HS256'
        }
    );
    
    return {
        user: {
            id: user.id,
            email: user.email
        },
        token
    };
}

async function login(email, password) {
    const user = await userRepository.findByEmail(email);
    
    if (!user) {
        throw new Error('Invalid credentials');  // Generic error
    }
    
    // Constant-time comparison
    const valid = await bcrypt.compare(password, user.password_hash);
    
    if (!valid) {
        throw new Error('Invalid credentials');  // Same generic error
    }
    
    const token = jwt.sign(
        { userId: user.id },
        process.env.JWT_SECRET,
        { expiresIn: '7d', algorithm: 'HS256' }
    );
    
    return {
        user: { id: user.id, email: user.email },
        token
    };
}
```

**Plaid Service** (`services/plaidService.js`):
```javascript
const { PlaidApi, Configuration, PlaidEnvironments } = require('plaid');
const llmService = require('./llmService');
const cache = require('../utils/cache');

const config = new Configuration({
    basePath: PlaidEnvironments[process.env.PLAID_ENV],
    baseOptions: {
        headers: {
            'PLAID-CLIENT-ID': process.env.PLAID_CLIENT_ID,
            'PLAID-SECRET': process.env.PLAID_SECRET
        }
    }
});

const plaidClient = new PlaidApi(config);

async function createLinkToken(userId) {
    const response = await plaidClient.linkTokenCreate({
        user: { client_user_id: userId.toString() },
        client_name: 'Credit Card Optimizer',
        products: ['auth', 'transactions', 'liabilities'],
        country_codes: ['US'],
        language: 'en'
    });
    
    return {
        linkToken: response.data.link_token,
        expiration: response.data.expiration
    };
}

async function exchangePublicToken(userId, publicToken) {
    const response = await plaidClient.itemPublicTokenExchange({
        public_token: publicToken
    });
    
    const accessToken = response.data.access_token;
    const itemId = response.data.item_id;
    
    // Save to database
    await plaidRepository.saveAccessToken(userId, accessToken, itemId);
    
    return { itemId, success: true };
}

async function getAccounts(userId, strategy = 'normal') {
    const accessToken = await plaidRepository.getAccessToken(userId);
    
    // Check cache
    if (strategy === 'cache-only') {
        const cached = cache.get(`accounts_${userId}`);
        if (cached) return cached;
    }
    
    // Fetch from Plaid
    const response = await plaidClient.accountsGet({
        access_token: accessToken
    });
    
    // Get transactions for statement detection
    const transactions = await getRecentTransactions(userId, accessToken);
    
    // Process accounts
    const accounts = await Promise.all(
        response.data.accounts.map(async account => {
            if (account.type !== 'credit') return null;
            
            // Try to detect statement date
            let statementDay = null;
            
            // Method 1: LLM analysis
            if (llmService.isAvailable()) {
                statementDay = await llmService.analyzeTransactionsWithLLM(
                    transactions,
                    account
                );
            }
            
            // Method 2: Pattern analysis (fallback)
            if (!statementDay) {
                statementDay = analyzeBalancePattern(transactions);
            }
            
            return {
                accountId: account.account_id,
                name: account.name,
                mask: account.mask,
                balances: account.balances,
                statementGenerationDay: statementDay
            };
        })
    );
    
    const creditCards = accounts.filter(Boolean);
    
    // Update cache
    cache.set(`accounts_${userId}`, creditCards, 5 * 60 * 1000);  // 5 min
    
    return creditCards;
}
```

### 4. Repository Layer

**User Repository** (`repositories/userRepository.js`):
```javascript
const db = require('../database/connection');

async function create(email, passwordHash) {
    const result = db.prepare(`
        INSERT INTO users (email, password_hash)
        VALUES (?, ?)
    `).run(email, passwordHash);
    
    return {
        id: result.lastInsertRowid,
        email
    };
}

async function findByEmail(email) {
    return db.prepare(`
        SELECT * FROM users WHERE email = ?
    `).get(email);
}

async function findById(id) {
    return db.prepare(`
        SELECT * FROM users WHERE id = ?
    `).get(id);
}
```

### 5. Database Layer

**Connection** (`database/connection.js`):
```javascript
const Database = require('better-sqlite3');
const path = require('path');

let db;

function initialize() {
    const dbPath = path.resolve(__dirname, '../../data/cc_optimizer.db');
    db = new Database(dbPath);
    
    // Enable foreign keys
    db.pragma('foreign_keys = ON');
    
    // Create tables
    db.exec(`
        CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            email TEXT UNIQUE NOT NULL,
            password_hash TEXT NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        );
        
        CREATE TABLE IF NOT EXISTS plaid_items (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL,
            access_token TEXT NOT NULL,
            item_id TEXT NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id)
        );
        
        CREATE TABLE IF NOT EXISTS credit_cards (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL,
            account_id TEXT NOT NULL,
            name TEXT,
            statement_date INTEGER,
            due_date INTEGER,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id)
        );
    `);
    
    return db;
}

module.exports = {
    initialize,
    get db() { return db; },
    prepare: (sql) => db.prepare(sql)
};
```

## API Endpoints

See [API Documentation](../api/overview.md) for complete reference.

**Authentication**:
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /api/v1/auth/profile`

**Plaid Integration**:
- `POST /api/v1/plaid/create_link_token`
- `POST /api/v1/plaid/exchange_public_token`
- `GET /api/v1/plaid/accounts`
- `POST /api/v1/plaid/transactions`
- `DELETE /api/v1/plaid/items/:itemId`
- `POST /api/v1/plaid/webhook`

**Card Management**:
- `GET /api/v1/cards`
- `GET /api/v1/cards/:accountId`
- `POST /api/v1/cards`
- `DELETE /api/v1/cards/:accountId`

## Security Features

1. **JWT Authentication**: HS256, 7-day expiration
2. **Password Hashing**: bcrypt with 12 rounds
3. **Rate Limiting**: 3-tier (auth, plaid, general)
4. **Input Validation**: express-validator
5. **SQL Injection Prevention**: Parameterized queries
6. **CORS**: Configurable origins
7. **Security Headers**: Helmet middleware
8. **Webhook Verification**: HMAC-SHA256

See [Backend Security](../security/backend.md) for details.

## Error Handling

```javascript
function errorHandler(err, req, res, next) {
    logger.error('Error:', err);
    
    // Generic message in production
    const message = process.env.NODE_ENV === 'production'
        ? 'Internal server error'
        : err.message;
    
    res.status(err.status || 500).json({
        error: message
    });
}
```

## Logging

```javascript
const winston = require('winston');

const logger = winston.createLogger({
    level: process.env.LOG_LEVEL || 'info',
    format: winston.format.json(),
    transports: [
        new winston.transports.File({ filename: 'logs/error.log', level: 'error' }),
        new winston.transports.File({ filename: 'logs/combined.log' })
    ]
});

if (process.env.NODE_ENV !== 'production') {
    logger.add(new winston.transports.Console({
        format: winston.format.simple()
    }));
}
```

## Deployment

See backend documentation:
- [Backend README](../../backend/README.md)
- [Deployment Guide](../development/deployment.md)

---

**Related Documentation**:
- [System Overview](system-overview.md)
- [Android App Architecture](android-app.md)
- [Backend Security](../security/backend.md)
- [API Reference](../api/overview.md)

