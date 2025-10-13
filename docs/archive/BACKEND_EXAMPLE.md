# Backend Server Example

This app requires a backend server to securely handle Plaid API calls. Below is an example implementation using Node.js/Express, but you can use any backend framework.

## Why a Backend?

⚠️ **CRITICAL SECURITY**: The Plaid API requires both a `client_id` and `client_secret`. The secret must NEVER be embedded in a mobile app as it can be extracted through reverse engineering. Always handle Plaid API calls through a secure backend server.

## Example Backend (Node.js/Express)

### Installation

```bash
npm init -y
npm install express plaid dotenv cors
```

### .env File

```env
PLAID_CLIENT_ID=your_client_id
PLAID_SECRET=your_secret
PLAID_ENV=sandbox  # or 'development' or 'production'
```

### server.js

```javascript
const express = require('express');
const { Configuration, PlaidApi, PlaidEnvironments } = require('plaid');
const cors = require('cors');
require('dotenv').config();

const app = express();
app.use(express.json());
app.use(cors());

// Initialize Plaid client
const configuration = new Configuration({
  basePath: PlaidEnvironments[process.env.PLAID_ENV],
  baseOptions: {
    headers: {
      'PLAID-CLIENT-ID': process.env.PLAID_CLIENT_ID,
      'PLAID-SECRET': process.env.PLAID_SECRET,
    },
  },
});

const plaidClient = new PlaidApi(configuration);

// Store access tokens (use a database in production!)
const accessTokens = new Map();

/**
 * Create Link Token
 * Required to initialize Plaid Link in the mobile app
 */
app.post('/api/plaid/create_link_token', async (req, res) => {
  try {
    const response = await plaidClient.linkTokenCreate({
      user: {
        client_user_id: 'user-' + Date.now(), // Use actual user ID in production
      },
      client_name: 'Credit Card Optimizer',
      products: ['auth', 'transactions'],
      country_codes: ['US'],
      language: 'en',
    });

    res.json({
      link_token: response.data.link_token,
      expiration: response.data.expiration,
    });
  } catch (error) {
    console.error('Error creating link token:', error);
    res.status(500).json({ error: error.message });
  }
});

/**
 * Exchange Public Token
 * Called after user successfully links their account
 */
app.post('/api/plaid/exchange_public_token', async (req, res) => {
  try {
    const { public_token } = req.body;

    const response = await plaidClient.itemPublicTokenExchange({
      public_token,
    });

    const accessToken = response.data.access_token;
    const itemId = response.data.item_id;

    // Store access token (use database in production!)
    accessTokens.set(itemId, accessToken);

    res.json({
      accessToken,
      itemId,
    });
  } catch (error) {
    console.error('Error exchanging token:', error);
    res.status(500).json({ error: error.message });
  }
});

/**
 * Get Accounts
 * Fetches all linked accounts
 */
app.post('/api/plaid/accounts', async (req, res) => {
  try {
    const accessToken = req.headers.authorization?.replace('Bearer ', '');

    if (!accessToken) {
      return res.status(401).json({ error: 'No access token provided' });
    }

    const accountsResponse = await plaidClient.accountsGet({
      access_token: accessToken,
    });

    const accounts = accountsResponse.data.accounts.map(account => ({
      accountId: account.account_id,
      name: account.name,
      mask: account.mask,
      type: account.type,
      subtype: account.subtype,
      balances: {
        available: account.balances.available,
        current: account.balances.current,
        limit: account.balances.limit,
      },
    }));

    res.json(accounts);
  } catch (error) {
    console.error('Error fetching accounts:', error);
    res.status(500).json({ error: error.message });
  }
});

/**
 * Get Transactions (optional, for future features)
 */
app.post('/api/plaid/transactions', async (req, res) => {
  try {
    const accessToken = req.headers.authorization?.replace('Bearer ', '');
    const { start_date, end_date } = req.body;

    if (!accessToken) {
      return res.status(401).json({ error: 'No access token provided' });
    }

    const response = await plaidClient.transactionsGet({
      access_token: accessToken,
      start_date,
      end_date,
    });

    res.json({
      transactions: response.data.transactions,
      accounts: response.data.accounts,
    });
  } catch (error) {
    console.error('Error fetching transactions:', error);
    res.status(500).json({ error: error.message });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
```

## Deployment Options

### 1. Heroku
```bash
git init
heroku create your-app-name
git push heroku main
heroku config:set PLAID_CLIENT_ID=xxx PLAID_SECRET=xxx PLAID_ENV=sandbox
```

### 2. Railway
- Connect your GitHub repo
- Set environment variables in dashboard
- Deploy automatically

### 3. DigitalOcean App Platform
- Connect repository
- Configure environment variables
- Deploy

### 4. AWS Lambda + API Gateway
- Use Serverless Framework or AWS SAM
- Deploy as serverless functions

## Security Checklist

- [ ] Use HTTPS only (enforce TLS)
- [ ] Implement user authentication (JWT, OAuth, etc.)
- [ ] Store access tokens in encrypted database
- [ ] Add rate limiting
- [ ] Validate all inputs
- [ ] Set up proper CORS policies
- [ ] Use environment variables for secrets
- [ ] Enable request logging
- [ ] Implement token refresh logic
- [ ] Add webhook handlers for Plaid events

## Production Considerations

1. **Database**: Use PostgreSQL/MySQL to store:
   - User accounts
   - Plaid access tokens (encrypted)
   - Credit card configurations
   - Payment history

2. **Authentication**: Implement user auth:
   ```javascript
   const jwt = require('jsonwebtoken');
   
   const authMiddleware = (req, res, next) => {
     const token = req.headers.authorization?.split(' ')[1];
     if (!token) return res.status(401).json({ error: 'Unauthorized' });
     
     try {
       const decoded = jwt.verify(token, process.env.JWT_SECRET);
       req.userId = decoded.userId;
       next();
     } catch (error) {
       res.status(401).json({ error: 'Invalid token' });
     }
   };
   ```

3. **Encryption**: Encrypt access tokens before storing:
   ```javascript
   const crypto = require('crypto');
   
   function encrypt(text) {
     const cipher = crypto.createCipher('aes-256-cbc', process.env.ENCRYPTION_KEY);
     let encrypted = cipher.update(text, 'utf8', 'hex');
     encrypted += cipher.final('hex');
     return encrypted;
   }
   ```

4. **Webhooks**: Handle Plaid webhooks for account updates:
   ```javascript
   app.post('/api/plaid/webhook', (req, res) => {
     const { webhook_type, webhook_code, item_id } = req.body;
     
     // Handle different webhook events
     if (webhook_type === 'ITEM' && webhook_code === 'ERROR') {
       // Handle item errors
     }
     
     res.json({ status: 'received' });
   });
   ```

## Getting Plaid Credentials

1. Sign up at https://dashboard.plaid.com/signup
2. Get your credentials from the dashboard
3. Start with Sandbox environment (free)
4. Move to Development for testing with real accounts (free, limited)
5. Production requires approval and has costs per API call

