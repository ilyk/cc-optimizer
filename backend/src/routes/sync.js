const express = require('express');
const router = express.Router();
const { authMiddleware } = require('../middleware/auth');
const { apiLimiter } = require('../middleware/rateLimiter');
const { asyncHandler } = require('../middleware/errorHandler');
const plaidRepository = require('../repositories/plaidRepository');
const plaidService = require('../services/plaidService');
const logger = require('../utils/logger');

/**
 * Get all cards for sync (Story 2.3 compatible format)
 * GET /api/v1/sync/cards
 */
router.get('/cards', authMiddleware, apiLimiter, asyncHandler(async (req, res) => {
  const cards = await plaidRepository.getCreditCards(req.userId);
  
  // Map to RemoteCard format
  const remoteCards = cards.map(card => ({
    id: card.account_id,
    issuer: card.institution_name || card.name,
    last4: card.mask || card.account_id.slice(-4),
    displayName: card.name,
    openedAt: null // Plaid doesn't provide this, set via manual input later
  }));
  
  res.json(remoteCards);
}));

/**
 * Get transactions for a specific card
 * GET /api/v1/sync/cards/:cardId/transactions?since=ISO-8601
 */
router.get('/cards/:cardId/transactions', authMiddleware, apiLimiter, asyncHandler(async (req, res) => {
  const { cardId } = req.params;
  const { since } = req.query;
  
  // Verify card belongs to user
  const card = await plaidRepository.getCreditCard(req.userId, cardId);
  if (!card) {
    return res.status(404).json({ error: 'Card not found' });
  }
  
  // Calculate date range
  const endDate = new Date().toISOString().split('T')[0]; // Today YYYY-MM-DD
  const startDate = since 
    ? new Date(since).toISOString().split('T')[0]
    : new Date(Date.now() - 180 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]; // 6 months ago
  
  // Fetch transactions from Plaid
  const result = await plaidService.getTransactions(req.userId, startDate, endDate);
  
  // Filter transactions for this specific account
  const accountTransactions = result.transactions.filter(tx => tx.account_id === cardId);
  
  // Map to RemoteTransaction format
  const plaidTransactions = accountTransactions.map(tx => {
    // Determine transaction type
    let type = 'PURCHASE';
    if (tx.amount < 0) {
      type = 'PAYMENT';
    } else if (tx.category && tx.category.includes('Interest')) {
      type = 'INTEREST';
    } else if (tx.category && tx.category.includes('Fee')) {
      type = 'FEE';
    } else if (tx.name && (tx.name.toLowerCase().includes('statement') || tx.name.toLowerCase().includes('finance charge'))) {
      type = 'STATEMENT';
    }
    
    return {
      id: tx.transaction_id,
      postedAt: tx.date + 'T00:00:00Z', // Plaid only provides date, not time
      amount: tx.amount,
      description: tx.name || 'Transaction',
      category: tx.category ? tx.category.join(', ') : null,
      type: type
    };
  });
  
  // **Epic 4: Merge with manual payments**
  const manualPayments = await plaidRepository.getManualPayments(req.userId, cardId);
  
  // Convert manual payments to transaction format
  const manualTransactions = manualPayments
    .filter(p => {
      // Only include if within requested date range
      if (!since) return true;
      return new Date(p.date) >= new Date(since);
    })
    .map(p => ({
      id: `manual_${p.id}`,
      postedAt: p.date + 'T12:00:00Z',  // Noon to distinguish from Plaid
      amount: -(p.amountCents / 100),  // Negative for payments
      description: 'Manual Payment',
      category: 'Payment',
      type: 'PAYMENT'
    }));
  
  // Merge and sort by date
  const allTransactions = [...plaidTransactions, ...manualTransactions]
    .sort((a, b) => new Date(b.postedAt) - new Date(a.postedAt));  // Newest first
  
  logger.info(`Returning ${plaidTransactions.length} Plaid + ${manualTransactions.length} manual transactions for card ${cardId}`);
  
  res.json(allTransactions);
}));

/**
 * Get daily balances for a specific card
 * GET /api/v1/sync/cards/:cardId/balances?since=ISO-8601
 * 
 * Note: Plaid API doesn't provide historical daily balances directly.
 * This endpoint returns current balance only or derives from transactions.
 */
router.get('/cards/:cardId/balances', authMiddleware, apiLimiter, asyncHandler(async (req, res) => {
  const { cardId } = req.params;
  const { since } = req.query;
  
  // Verify card belongs to user
  const card = await plaidRepository.getCreditCard(req.userId, cardId);
  if (!card) {
    return res.status(404).json({ error: 'Card not found' });
  }
  
  // Get accounts to fetch current balance
  const accounts = await plaidService.getAccounts(req.userId);
  const account = accounts.accounts.find(acc => acc.account_id === cardId);
  
  if (!account || !account.balances) {
    return res.json([]); // No balance data available
  }
  
  // Return current balance as a single snapshot
  // In production, you might store daily snapshots in your DB
  const remoteBalances = [
    {
      date: new Date().toISOString().split('T')[0], // Today
      balance: account.balances.current || 0
    }
  ];
  
  res.json(remoteBalances);
}));

/**
 * Record a manual payment
 * POST /api/v1/sync/cards/:cardId/payments
 * 
 * Body: { date: "YYYY-MM-DD", amountCents: 50000 }
 */
router.post('/cards/:cardId/payments', authMiddleware, apiLimiter, asyncHandler(async (req, res) => {
  const { cardId } = req.params;
  const { date, amountCents } = req.body;
  
  // Verify card belongs to user
  const card = await plaidRepository.getCreditCard(req.userId, cardId);
  if (!card) {
    return res.status(404).json({ error: 'Card not found' });
  }
  
  // Validate input
  if (!date || !amountCents) {
    return res.status(400).json({ error: 'Missing required fields: date, amountCents' });
  }
  
  if (amountCents <= 0) {
    return res.status(400).json({ error: 'Amount must be positive' });
  }
  
  // Store manual payment
  const payment = await plaidRepository.recordManualPayment(req.userId, cardId, date, amountCents);
  
  logger.info(`Manual payment recorded: userId=${req.userId}, cardId=${cardId}, date=${date}, amount=${amountCents}`);
  logger.debug(`Payment object from repo: ${JSON.stringify(payment)}`);
  
  const response = {
    id: payment.id,
    cardId: payment.cardId,
    date: payment.date,
    amountCents: payment.amountCents,
    source: payment.source
  };
  
  logger.debug(`Sending response: ${JSON.stringify(response)}`);
  
  res.status(201).json(response);
}));

/**
 * Get manual payments for a card
 * GET /api/v1/sync/cards/:cardId/payments
 */
router.get('/cards/:cardId/payments', authMiddleware, apiLimiter, asyncHandler(async (req, res) => {
  const { cardId } = req.params;
  
  // Verify card belongs to user
  const card = await plaidRepository.getCreditCard(req.userId, cardId);
  if (!card) {
    return res.status(404).json({ error: 'Card not found' });
  }
  
  const payments = await plaidRepository.getManualPayments(req.userId, cardId);
  
  logger.debug(`GET /cards/${cardId}/payments → returning ${payments.length} payments`);
  if (payments.length > 0) {
    logger.debug(`  First payment: ${JSON.stringify(payments[0])}`);
  }
  
  res.json(payments);
}));

/**
 * Delete a manual payment
 * DELETE /api/v1/sync/cards/:cardId/payments/:paymentId
 */
router.delete('/cards/:cardId/payments/:paymentId', authMiddleware, apiLimiter, asyncHandler(async (req, res) => {
  const { cardId, paymentId } = req.params;
  
  logger.info(`DELETE request: cardId=${cardId}, paymentId=${paymentId}, userId=${req.userId}`);
  
  // Verify card belongs to user
  const card = await plaidRepository.getCreditCard(req.userId, cardId);
  if (!card) {
    logger.warn(`Card ${cardId} not found for user ${req.userId}`);
    return res.status(404).json({ error: 'Card not found' });
  }
  
  await plaidRepository.deleteManualPayment(req.userId, paymentId);
  
  logger.info(`Deleted payment ${paymentId} for card ${cardId}`);
  
  res.json({ success: true });
}));

/**
 * Health check for sync endpoints
 * GET /api/v1/sync/health
 */
router.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    service: 'sync',
    endpoints: [
      'GET /cards',
      'GET /cards/:cardId/transactions',
      'GET /cards/:cardId/balances',
      'POST /cards/:cardId/payments',
      'GET /cards/:cardId/payments',
      'DELETE /cards/:cardId/payments/:paymentId'
    ]
  });
});

module.exports = router;
