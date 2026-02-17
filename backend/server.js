require('dotenv').config();
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');

// Import utilities and middleware
const logger = require('./src/utils/logger');
const db = require('./src/database/connection');
const { authMiddleware } = require('./src/middleware/auth');
const { apiLimiter, authLimiter, plaidLimiter } = require('./src/middleware/rateLimiter');
const { smartRateLimit } = require('./src/middleware/smartRateLimit');
const { errorHandler, notFoundHandler, asyncHandler } = require('./src/middleware/errorHandler');
const { webhookVerificationMiddleware } = require('./src/utils/webhookVerification');
const {
  registerValidation,
  loginValidation,
  exchangeTokenValidation,
  transactionsValidation,
  creditCardValidation,
} = require('./src/middleware/validation');
const {
  validateItemId,
  validateAccountId
} = require('./src/middleware/paramValidation');

// Import services
const authService = require('./src/services/authService');
const plaidService = require('./src/services/plaidService');
const plaidRepository = require('./src/repositories/plaidRepository');
const securityRoutes = require('./src/routes/security');

const app = express();

// Trust reverse proxy (for correct IPs, secure cookies behind proxies)
app.set('trust proxy', true);
// Remove X-Powered-By header
app.disable('x-powered-by');

// Security middleware
app.use(helmet());

// CORS configuration (tighten in production)
const allowedOrigins = (process.env.CORS_ORIGIN || '').split(',').map(o => o.trim()).filter(Boolean);
app.use(cors({
  origin: function (origin, callback) {
    if (!origin) {return callback(null, true);} // allow non-browser clients
    if (allowedOrigins.length === 0) {return callback(null, true);} // default open if not configured
    if (allowedOrigins.includes(origin)) {return callback(null, true);}
    return callback(new Error('CORS not allowed'), false);
  },
  credentials: false,
  optionsSuccessStatus: 200,
  methods: ['GET', 'POST', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Authorization', 'Content-Type', 'Plaid-Verification', 'Plaid-Signature'],
}));

// Capture rawBody for webhook verification before JSON parsing
app.use('/api/plaid/webhook', express.raw({ type: '*/*', limit: '256kb' }));

// Body parsing with limits
app.use(express.json({ limit: '512kb', strict: false }));
app.use(express.urlencoded({ extended: false, limit: '256kb' }));

// Enforce application/json on state-changing endpoints
app.use((req, res, next) => {
  const stateChanging = req.method === 'POST' || req.method === 'PUT' || req.method === 'PATCH' || req.method === 'DELETE';
  if (stateChanging && req.path !== '/api/plaid/webhook') {
    const ct = (req.headers['content-type'] || '').toLowerCase();
    if (!ct.startsWith('application/json')) {
      return res.status(415).json({ error: 'Unsupported Media Type. Use application/json' });
    }
  }
  next();
});

// Request logging
app.use(morgan('combined', {
  stream: {
    write: (message) => logger.info(message.trim())
  }
}));

// Health check endpoint (unversioned)
app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    message: 'Credit Card Optimizer Backend is running',
    timestamp: new Date().toISOString(),
    environment: process.env.PLAID_ENV || 'sandbox',
    apiVersion: 'v1',
    supportedVersions: ['v1'],
  });
});

// =============================================================================
// API VERSIONING
// =============================================================================

// Create API v1 router
const apiV1 = express.Router();

// Security routes (certificate pins, etc.)
apiV1.use('/security', securityRoutes);

// MFA routes (FIDO2, passkeys, biometric)
const mfaRoutes = require('./src/routes/mfa');
apiV1.use('/mfa', mfaRoutes);

// Sync routes (Epic 2 Story 2.3)
const syncRoutes = require('./src/routes/sync');
apiV1.use('/sync', syncRoutes);

// =============================================================================
// AUTH ROUTES (v1)
// =============================================================================

/**
 * Register a new user
 */
apiV1.post('/auth/register', authLimiter, registerValidation, asyncHandler(async (req, res) => {
  const { email, password } = req.body;
  const result = await authService.register(email, password);
  res.status(201).json(result);
}));

/**
 * Login user
 */
apiV1.post('/auth/login', authLimiter, loginValidation, asyncHandler(async (req, res) => {
  const { email, password } = req.body;
  const result = await authService.login(email, password);
  res.json(result);
}));

/**
 * Get user profile
 */
apiV1.get('/auth/profile', authMiddleware, asyncHandler(async (req, res) => {
  const profile = await authService.getProfile(req.userId);
  res.json(profile);
}));

// Social authentication routes
const socialAuthRoutes = require('./src/routes/socialAuth');
apiV1.use('/auth', socialAuthRoutes);

// =============================================================================
// PLAID ROUTES (v1)
// =============================================================================

/**
 * Create Link Token
 */
apiV1.post('/plaid/create_link_token', authMiddleware, plaidLimiter, asyncHandler(async (req, res) => {
  const result = await plaidService.createLinkToken(req.userId);
  res.json(result);
}));

/**
 * Exchange Public Token
 */
apiV1.post('/plaid/exchange_public_token', authMiddleware, plaidLimiter, exchangeTokenValidation, asyncHandler(async (req, res) => {
  const { public_token } = req.body;
  const result = await plaidService.exchangePublicToken(req.userId, public_token);
  res.json(result);
}));

/**
 * OAuth callback for mobile apps
 * Handles redirect from bank OAuth and redirects back to app
 */
apiV1.get('/plaid/oauth/callback', asyncHandler(async (req, res) => {
  try {
    const { oauth_state_id, error } = req.query;
    
    if (error) {
      // OAuth error - redirect to app with error
      const appRedirectUrl = `plaidlink://oauth?error=${encodeURIComponent(error)}`;
      return res.redirect(appRedirectUrl);
    }
    
    // Success - redirect to app with state
    const appRedirectUrl = `plaidlink://oauth?oauth_state_id=${oauth_state_id}`;
    res.redirect(appRedirectUrl);
  } catch (error) {
    logger.error('OAuth callback error:', error);
    const appRedirectUrl = `plaidlink://oauth?error=callback_error`;
    res.redirect(appRedirectUrl);
  }
}));

/**
 * Get Accounts with Liabilities (Android app format)
 */
apiV1.get('/plaid/accounts', authMiddleware, plaidLimiter, smartRateLimit('accounts'), asyncHandler(async (req, res) => {
  try {
    // Use smart rate limit strategy from middleware
    const strategy = req.rateLimit?.strategy || 'normal';
    const accounts = await plaidService.getAccounts(req.userId, strategy);
    
    // Map to Android app format with liabilities
    const mappedAccounts = accounts.map(account => ({
      account_id: account.accountId,
      name: account.name,
      official_name: account.officialName,
      type: account.type,
      subtype: account.subtype,
      balances: {
        available: account.balances.available,
        current: account.balances.current,
        limit: account.balances.limit
      },
      liabilities: account.liability ? {
        credit: [{
          account_id: account.accountId,
          aprs: account.liability.aprs || [],
          last_statement_issue_date: account.liability.lastStatementDate,
          last_statement_balance: account.liability.lastStatementBalance,
          minimum_payment_amount: account.liability.minimumPaymentAmount,
          next_payment_due_date: account.liability.nextPaymentDueDate
        }]
      } : null
    }));
    
    // Add cache headers based on strategy
    if (strategy === 'cache-only' || strategy === 'stale-while-revalidate') {
      res.setHeader('X-Cache-Strategy', strategy);
      res.setHeader('X-Rate-Limit-Info', req.rateLimit.reason);
    }
    
    res.json({ accounts: mappedAccounts });
  } catch (error) {
    logger.error('Error fetching accounts:', error);
    res.status(500).json({ error: 'Failed to fetch accounts' });
  }
}));

/**
 * Get Transactions (Android app format)
 */
apiV1.post('/plaid/transactions', authMiddleware, plaidLimiter, transactionsValidation, asyncHandler(async (req, res) => {
  const { start_date, end_date } = req.body;
  
  try {
    const result = await plaidService.getTransactions(req.userId, start_date, end_date);
    
    // Map to Android app format
    const transactions = result.transactions.map(tx => ({
      transaction_id: tx.transaction_id,
      account_id: tx.account_id,
      date: tx.date,
      name: tx.name,
      merchant_name: tx.merchant_name,
      amount: tx.amount,
      category: tx.category || []
    }));
    
    res.json({ transactions });
  } catch (error) {
    logger.error('Error fetching transactions:', error);
    res.status(500).json({ error: 'Failed to fetch transactions' });
  }
}));

/**
 * Remove Plaid Item
 */
apiV1.delete('/plaid/items/:itemId', authMiddleware, plaidLimiter, validateItemId, asyncHandler(async (req, res) => {
  const { itemId } = req.params;
  const result = await plaidService.removeItem(req.userId, itemId);
  res.json(result);
}));

// =============================================================================
// CREDIT CARD CONFIGURATION ROUTES (v1)
// =============================================================================

/**
 * Get all credit cards for user (Android app format)
 */
apiV1.get('/cards', authMiddleware, apiLimiter, asyncHandler(async (req, res) => {
  try {
    // Get accounts from Plaid with full liability data
    const accounts = await plaidService.getAccounts(req.userId);
    
    // Map to Android app format
    const cards = accounts.map(account => ({
      id: account.accountId,
      externalId: account.accountId,
      issuer: account.name,
      network: mapNetwork(account.subtype),
      balance: account.balances.current || 0,
      minPayment: account.liability?.minimumPaymentAmount || 0,
      creditLimit: account.balances.limit || 0,
      aprPurchase: account.liability?.aprs?.[0]?.apr || 0,
      aprCash: account.liability?.aprs?.find(a => a.apr_type === 'cash')?.apr || null,
      statementClose: account.liability?.lastStatementDate ? 
        new Date(account.liability.lastStatementDate).toISOString().split('T')[0] : null,
      dueDate: account.liability?.nextPaymentDueDate ? 
        new Date(account.liability.nextPaymentDueDate).toISOString().split('T')[0] : null,
      hasPromo: false, // TODO: Implement promo detection
      promoEnd: null,
      promoBalance: 0,
      utilization: account.balances.current && account.balances.limit ? 
        account.balances.current / account.balances.limit : 0,
      currency: 'USD',
      autopayEnabled: false // TODO: Get from card configuration
    }));
    
    res.json({ cards });
  } catch (error) {
    logger.error('Error fetching cards:', error);
    res.status(500).json({ error: 'Failed to fetch credit cards' });
  }
}));

/**
 * Get a specific credit card
 */
apiV1.get('/cards/:accountId', authMiddleware, apiLimiter, validateAccountId, asyncHandler(async (req, res) => {
  const { accountId } = req.params;
  const card = await plaidRepository.getCreditCard(req.userId, accountId);
  
  if (!card) {
    return res.status(404).json({ error: 'Credit card not found' });
  }
  
  res.json(card);
}));

/**
 * Save or update credit card configuration
 */
apiV1.post('/cards', authMiddleware, apiLimiter, creditCardValidation, asyncHandler(async (req, res) => {
  const card = await plaidRepository.saveCreditCard(req.userId, req.body);
  res.json(card);
}));

/**
 * Delete credit card configuration
 */
apiV1.delete('/cards/:accountId', authMiddleware, apiLimiter, validateAccountId, asyncHandler(async (req, res) => {
  const { accountId } = req.params;
  await plaidRepository.deleteCreditCard(req.userId, accountId);
  res.json({ success: true, message: 'Credit card deleted' });
}));

// =============================================================================
// WEBHOOK ROUTES (v1)
// =============================================================================

/**
 * Plaid Webhook Handler
 */
apiV1.post('/plaid/webhook', webhookVerificationMiddleware, asyncHandler(async (req, res) => {
  const { webhook_type, webhook_code, item_id, error } = req.body;

  logger.info('Webhook received:', { webhook_type, webhook_code, item_id });

  // Log webhook to database
  await plaidRepository.logWebhook(webhook_type, webhook_code, item_id, req.body);

  // Handle different webhook events
  if (webhook_type === 'ITEM') {
    if (webhook_code === 'ERROR') {
      logger.error(`Item error for item ${item_id}:`, error);
      // User notification can be implemented via email/push notifications
      // Error is logged to database for tracking
    } else if (webhook_code === 'PENDING_EXPIRATION') {
      logger.warn(`Item ${item_id} access will expire soon`);
      // User can be notified to re-authenticate via mobile app notification
      // Item will need re-linking through Plaid Link
    }
  } else if (webhook_type === 'TRANSACTIONS') {
    if (webhook_code === 'DEFAULT_UPDATE' || webhook_code === 'INITIAL_UPDATE') {
      logger.info(`New transactions available for item ${item_id}`);
      // Transactions can be fetched on-demand via GET /api/plaid/transactions
      // Or implement background job to fetch and cache transactions
    }
  } else if (webhook_type === 'AUTH') {
    if (webhook_code === 'AUTOMATICALLY_VERIFIED') {
      logger.info(`Auth automatically verified for item ${item_id}`);
    }
  }

  res.json({ status: 'received' });
}));

// Mount API v1 router
app.use('/api/v1', apiV1);

// Backward compatibility: redirect /api/* to /api/v1/* (temporary)
app.use('/api', (req, res, next) => {
  // Skip if already versioned
  if (req.path.startsWith('/v1')) {
    return next();
  }
  
  // Warn about deprecated endpoint
  logger.warn(`Deprecated API call: ${req.method} ${req.path} - Please use /api/v1${req.path}`);
  res.setHeader('X-API-Deprecated', 'true');
  res.setHeader('X-API-Version', 'v1');
  res.setHeader('Deprecation', 'version="legacy"');
  res.setHeader('Sunset', new Date(Date.now() + 90 * 24 * 60 * 60 * 1000).toUTCString()); // 90 days
  
  // Redirect to v1
  req.url = `/v1${req.url}`;
  apiV1(req, res, next);
});

// =============================================================================
// ERROR HANDLING
// =============================================================================

// 404 handler
app.use(notFoundHandler);

// Global error handler (must be last)
app.use(errorHandler);

// =============================================================================
// SERVER INITIALIZATION
// =============================================================================

const PORT = process.env.PORT || 12080;

async function startServer() {
  try {
    // Initialize database
    await db.initialize();
    logger.info('Database initialized successfully');

    // Start server
    app.listen(PORT, () => {
      logger.info(`Server running on port ${PORT}`);
      logger.info(`Environment: ${process.env.NODE_ENV || 'development'}`);
      logger.info(`Plaid environment: ${process.env.PLAID_ENV || 'sandbox'}`);
      logger.info(`Health check: http://localhost:${PORT}/health`);
    });
  } catch (error) {
    logger.error('Failed to start server:', error);
    process.exit(1);
  }
}

// Handle graceful shutdown
process.on('SIGTERM', async () => {
  logger.info('SIGTERM received, shutting down gracefully');
  await db.close();
  process.exit(0);
});

process.on('SIGINT', async () => {
  logger.info('SIGINT received, shutting down gracefully');
  await db.close();
  process.exit(0);
});

/**
 * Map Plaid account subtype to card network
 */
function mapNetwork(subtype) {
  const networkMap = {
    'credit card': 'VISA', // Default to VISA for credit cards
    'visa': 'VISA',
    'mastercard': 'MASTERCARD',
    'amex': 'AMEX',
    'discover': 'DISCOVER'
  };
  
  return networkMap[subtype?.toLowerCase()] || 'VISA';
}

// Start the server
startServer();

module.exports = app;
