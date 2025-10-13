# Backend Server for Credit Card Optimizer

Production-ready backend API server with Plaid integration for the Credit Card Optimizer Android app. Features full user authentication, encrypted token storage, rate limiting, and comprehensive security.

## Features

✅ **User Authentication** - JWT-based authentication with secure password hashing  
✅ **Plaid Integration** - Complete Plaid API integration for credit card accounts  
✅ **Database Support** - PostgreSQL for production, SQLite for development  
✅ **Token Encryption** - AES-256-GCM encryption for sensitive data  
✅ **Rate Limiting** - Protect against abuse with configurable rate limits  
✅ **Input Validation** - Comprehensive request validation  
✅ **Error Handling** - Centralized error handling and logging  
✅ **Webhook Support** - Plaid webhook handling with signature verification  
✅ **Security Headers** - Helmet.js for security best practices  
✅ **Request Logging** - Winston logger with file and console output  

## Quick Start

### 1. Run Setup Script

```bash
cd backend
npm run setup
```

This will:
- Install all dependencies
- Create `.env` file from template
- Generate secure JWT and encryption keys
- Create necessary directories

### 2. Configure Plaid Credentials

Edit `.env` and add your Plaid credentials:

```env
PLAID_CLIENT_ID=your_client_id
PLAID_SECRET=your_sandbox_secret
PLAID_ENV=sandbox
```

Get credentials from https://dashboard.plaid.com/signup

### 3. Run the Server

```bash
# Development mode with auto-reload
npm run dev

# Production mode
npm start
```

Server starts on `http://localhost:3000`

## Project Structure

```
backend/
├── src/
│   ├── config/           # Configuration files
│   │   └── database.js
│   ├── database/         # Database connection and queries
│   │   └── connection.js
│   ├── middleware/       # Express middleware
│   │   ├── auth.js       # JWT authentication
│   │   ├── errorHandler.js
│   │   ├── rateLimiter.js
│   │   └── validation.js
│   ├── repositories/     # Data access layer
│   │   ├── plaidRepository.js
│   │   └── userRepository.js
│   ├── services/         # Business logic
│   │   ├── authService.js
│   │   └── plaidService.js
│   └── utils/           # Utilities
│       ├── encryption.js
│       ├── logger.js
│       └── webhookVerification.js
├── scripts/             # Setup and utility scripts
│   ├── generateKeys.js
│   ├── setup.sh
│   └── setup.bat
├── data/               # SQLite database (gitignored)
├── logs/               # Log files (gitignored)
├── server.js           # Main application entry point
├── package.json
├── .env.example        # Environment template
├── API.md             # Complete API documentation
└── README.md
```

## API Endpoints

See [API.md](./API.md) for complete documentation.

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login
- `GET /api/auth/profile` - Get user profile

### Plaid Integration
- `POST /api/plaid/create_link_token` - Initialize Plaid Link
- `POST /api/plaid/exchange_public_token` - Exchange public token
- `GET /api/plaid/accounts` - Get linked accounts
- `POST /api/plaid/transactions` - Get transactions
- `DELETE /api/plaid/items/:itemId` - Remove linked account

### Credit Card Configuration
- `GET /api/cards` - Get all credit cards
- `GET /api/cards/:accountId` - Get specific card
- `POST /api/cards` - Save card configuration
- `DELETE /api/cards/:accountId` - Delete card

### Webhooks
- `POST /api/plaid/webhook` - Plaid webhook handler

## Database

### SQLite (Development)

Default for easy local development. Database stored in `./data/database.sqlite`.

### PostgreSQL (Production)

Configure in `.env`:

```env
DATABASE_TYPE=postgres
DB_HOST=your-db-host
DB_PORT=5432
DB_NAME=cc_optimizer
DB_USER=postgres
DB_PASSWORD=your-password
DB_SSL=true
```

### Automatic Migrations

Database tables are created automatically on first run:
- `users` - User accounts
- `plaid_items` - Plaid access tokens (encrypted)
- `credit_cards` - Credit card configurations
- `webhook_logs` - Webhook event logs

## Security

### Password Hashing
Passwords are hashed using bcrypt with 12 salt rounds.

### Token Encryption
Plaid access tokens are encrypted using AES-256-GCM before storage.

### JWT Authentication
JWT tokens with configurable expiration (default: 7 days).

### Rate Limiting
- General API: 100 requests / 15 minutes
- Auth endpoints: 5 requests / 15 minutes
- Plaid endpoints: 20 requests / minute

### Security Headers
Helmet.js provides:
- Content Security Policy
- XSS Protection
- HSTS
- Frame protection
- And more...

### Webhook Verification
Plaid webhooks verified using HMAC SHA256 signatures.

## Environment Variables

| Variable | Required | Description | Default |
|----------|----------|-------------|---------|
| `NODE_ENV` | No | Environment mode | `development` |
| `PORT` | No | Server port | `3000` |
| `PLAID_CLIENT_ID` | Yes | Plaid client ID | - |
| `PLAID_SECRET` | Yes | Plaid secret | - |
| `PLAID_ENV` | Yes | Plaid environment | `sandbox` |
| `DATABASE_TYPE` | No | Database type | `sqlite` |
| `JWT_SECRET` | Yes | JWT signing secret | - |
| `ENCRYPTION_KEY` | Yes | Token encryption key | - |
| `CORS_ORIGIN` | No | CORS origin | `*` |
| `LOG_LEVEL` | No | Logging level | `info` |

Generate secure keys:
```bash
npm run generate-keys
```

## Testing

### Health Check
```bash
curl http://localhost:3000/health
```

### Register User
```bash
curl -X POST http://localhost:3000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"TestPass123"}'
```

### Plaid Sandbox

Use these test credentials in Plaid Link:
- Username: `user_good`
- Password: `pass_good`

## Deployment

### Heroku

```bash
heroku create your-app-name
heroku addons:create heroku-postgresql:mini
git push heroku main

# Set environment variables
heroku config:set PLAID_CLIENT_ID=xxx
heroku config:set PLAID_SECRET=xxx
heroku config:set PLAID_ENV=sandbox
heroku config:set JWT_SECRET=$(openssl rand -hex 32)
heroku config:set ENCRYPTION_KEY=$(openssl rand -hex 32)
heroku config:set DATABASE_TYPE=postgres
```

### Railway

1. Connect GitHub repository
2. Add PostgreSQL database
3. Set environment variables in dashboard
4. Deploy automatically

### DigitalOcean App Platform

1. Create new app from repository
2. Add managed PostgreSQL database
3. Configure environment variables
4. Deploy

### Docker

```bash
# Build image
docker build -t cc-optimizer-backend .

# Run container
docker run -p 3000:3000 --env-file .env cc-optimizer-backend

# Or use docker-compose
docker-compose up
```

## Logging

Logs are written to:
- **Console** - Colored output for development
- **logs/combined.log** - All logs
- **logs/error.log** - Error logs only

Configure log level in `.env`:
```env
LOG_LEVEL=info  # debug, info, warn, error
```

## Troubleshooting

### Database Connection Error
```bash
# For SQLite, ensure data directory exists
mkdir -p data

# For PostgreSQL, verify connection string
psql $DATABASE_URL
```

### Port Already in Use
```bash
# Kill process on port 3000
lsof -ti:3000 | xargs kill -9

# Or change port in .env
PORT=3001
```

### Plaid API Errors
- Verify credentials in Plaid dashboard
- Ensure `PLAID_ENV` matches credential environment
- Check rate limits

### Webhook Not Receiving Events
- Webhooks require public URL
- Use ngrok for local testing: `ngrok http 3000`
- Configure webhook URL in Plaid dashboard
- Set `PLAID_WEBHOOK_VERIFICATION_KEY` in `.env`

## Production Checklist

- [ ] Generate secure `JWT_SECRET` and `ENCRYPTION_KEY`
- [ ] Set up PostgreSQL database
- [ ] Configure production Plaid credentials
- [ ] Set `NODE_ENV=production`
- [ ] Configure CORS for your app domain
- [ ] Set up HTTPS/TLS
- [ ] Configure webhook URL
- [ ] Set up database backups
- [ ] Configure error monitoring (e.g., Sentry)
- [ ] Set up uptime monitoring
- [ ] Review rate limits
- [ ] Enable database SSL

## Development

### Adding New Endpoints

1. Create service in `src/services/`
2. Add validation in `src/middleware/validation.js`
3. Add route in `server.js`
4. Update `API.md`

### Database Changes

Modify migrations in `src/database/connection.js` `runMigrations()` method.

## Support

- **Plaid Docs**: https://plaid.com/docs/
- **Plaid API Reference**: https://plaid.com/docs/api/
- **Plaid Dashboard**: https://dashboard.plaid.com/

## License

ISC
