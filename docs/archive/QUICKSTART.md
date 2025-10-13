# Quick Start Guide

Get the backend running in 3 minutes.

## Prerequisites

- Node.js 18+ installed
- Plaid account (free at https://dashboard.plaid.com/signup)

## Step 1: Setup (30 seconds)

```bash
cd backend
npm run setup
```

This installs everything and generates secure keys.

## Step 2: Configure Plaid (1 minute)

1. Sign up at https://dashboard.plaid.com/signup
2. Get your credentials from the Keys page
3. Edit `.env`:

```env
PLAID_CLIENT_ID=your_client_id_here
PLAID_SECRET=your_sandbox_secret_here
PLAID_ENV=sandbox
```

4. Copy the generated `JWT_SECRET` and `ENCRYPTION_KEY` from the setup output into `.env`

## Step 3: Run (10 seconds)

```bash
npm start
```

Server starts at http://localhost:3000

## Step 4: Test (1 minute)

```bash
# Health check
curl http://localhost:3000/health

# Register user
curl -X POST http://localhost:3000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"TestPass123"}'

# You'll get back a JWT token - save it!
```

## That's It!

You now have a production-ready backend with:

✅ User authentication  
✅ Plaid integration  
✅ Encrypted database  
✅ Rate limiting  
✅ Complete API  

## Next Steps

- Read [API.md](./API.md) for complete API documentation
- Read [README.md](./README.md) for detailed information
- Use `test.http` file for easy API testing
- Deploy to Heroku/Railway/DigitalOcean (guides in README.md)

## Common Commands

```bash
# Development mode (auto-reload)
npm run dev

# Generate new secure keys
npm run generate-keys

# Re-run setup
npm run setup
```

## Plaid Sandbox Test Credentials

When testing Plaid Link, use:
- Username: `user_good`
- Password: `pass_good`

This gives you test credit card accounts!

## Troubleshooting

**Port 3000 already in use?**
```bash
# Change port in .env
PORT=3001
```

**Database errors?**
```bash
# Delete old database and restart
rm -rf data/
npm start
```

**Plaid errors?**
- Verify credentials in .env match your Plaid dashboard
- Ensure PLAID_ENV is "sandbox" for testing

## Need Help?

- Complete docs: [README.md](./README.md)
- API reference: [API.md](./API.md)
- Implementation details: [IMPLEMENTATION.md](./IMPLEMENTATION.md)

