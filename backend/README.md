# Credit Card Optimizer - Backend

Node.js/Express REST API for secure Plaid integration and user authentication.

## Quick Start

```bash
# Install dependencies
npm install

# Configure environment
cp .env.example .env
# Edit .env with your Plaid credentials

# Start server
npm start
```

Server runs on `http://localhost:3000`

## Documentation

Complete documentation is available in the `/docs` directory:

- **[Getting Started](../docs/getting-started/quick-start.md)** - Setup guide
- **[Backend Architecture](../docs/architecture/backend.md)** - Architecture details
- **[API Reference](../docs/api/overview.md)** - Complete API documentation
- **[Security](../docs/security/backend.md)** - Security implementation
- **[Development Guide](../docs/development/guide.md)** - Developer workflow

## Environment Variables

Required in `.env`:

```env
# Plaid
PLAID_CLIENT_ID=your_client_id
PLAID_SECRET=your_secret
PLAID_ENV=sandbox

# JWT
JWT_SECRET=your-secret-key-min-32-chars

# Database
DATABASE_URL=./data/cc_optimizer.db
DB_TYPE=sqlite

# Server
PORT=3000
NODE_ENV=development
```

## API Endpoints

### Authentication
- `POST /api/v1/auth/register` - Register user
- `POST /api/v1/auth/login` - Login user
- `GET /api/v1/auth/profile` - Get user profile

### Plaid Integration
- `POST /api/v1/plaid/create_link_token` - Create Plaid Link token
- `POST /api/v1/plaid/exchange_public_token` - Exchange public token
- `GET /api/v1/plaid/accounts` - Get accounts
- `POST /api/v1/plaid/transactions` - Get transactions
- `DELETE /api/v1/plaid/items/:itemId` - Remove item

### Card Management
- `GET /api/v1/cards` - Get all cards
- `POST /api/v1/cards` - Save card configuration
- `DELETE /api/v1/cards/:accountId` - Delete card

See [API Documentation](../docs/api/overview.md) for complete reference.

## Testing

```bash
npm test
npm run coverage  # Must be 95%+
```

## Linting

```bash
npm run lint:fix
npm run format
```

## Security Features

- ✅ JWT authentication (HS256)
- ✅ bcrypt password hashing (12 rounds)
- ✅ Input validation (express-validator)
- ✅ SQL injection prevention
- ✅ Rate limiting (3-tier)
- ✅ CORS configuration
- ✅ Security headers (Helmet)
- ✅ Webhook verification (HMAC)

Security Score: **A (98/100)**

## Project Structure

```
backend/
├── server.js              # Main entry point
├── src/
│   ├── middleware/        # Express middleware
│   ├── services/          # Business logic
│   ├── repositories/      # Data access
│   └── utils/            # Utilities
├── data/                  # Database files
└── scripts/              # Setup scripts
```

## Deployment

See [Production Deployment](../docs/development/deployment.md) for deployment guide.

## Support

- [Documentation](../docs/)
- [FAQ](../docs/development/faq.md)
- [Troubleshooting](../docs/development/troubleshooting.md)

---

For complete documentation, see [/docs](../docs/)

