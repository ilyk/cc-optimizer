const express = require('express');
const router = express.Router();
const { authMiddleware } = require('../middleware/auth');
const { apiLimiter } = require('../middleware/rateLimiter');
const { asyncHandler } = require('../middleware/errorHandler');
const plaidService = require('../services/plaidService');
const plaidRepository = require('../repositories/plaidRepository');
const logger = require('../utils/logger');

/**
 * Get all credit cards for user (Android app format)
 * GET /api/v1/cards
 */
router.get('/', authMiddleware, apiLimiter, asyncHandler(async (req, res) => {
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
 * GET /api/v1/cards/:accountId
 */
router.get('/:accountId', authMiddleware, apiLimiter, asyncHandler(async (req, res) => {
  const { accountId } = req.params;
  
  try {
    const accounts = await plaidService.getAccounts(req.userId);
    const account = accounts.find(acc => acc.accountId === accountId);
    
    if (!account) {
      return res.status(404).json({ error: 'Credit card not found' });
    }
    
    // Map to Android app format
    const card = {
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
      hasPromo: false,
      promoEnd: null,
      promoBalance: 0,
      utilization: account.balances.current && account.balances.limit ? 
        account.balances.current / account.balances.limit : 0,
      currency: 'USD',
      autopayEnabled: false
    };
    
    res.json(card);
  } catch (error) {
    logger.error('Error fetching card:', error);
    res.status(500).json({ error: 'Failed to fetch credit card' });
  }
}));

/**
 * Save or update credit card configuration
 * POST /api/v1/cards
 */
router.post('/', authMiddleware, apiLimiter, asyncHandler(async (req, res) => {
  try {
    const cardData = {
      account_id: req.body.accountId,
      name: req.body.name,
      mask: req.body.mask,
      credit_limit: req.body.creditLimit,
      statement_date: req.body.statementDate,
      due_date: req.body.dueDate,
      auto_pay_enabled: req.body.autoPayEnabled || false,
      auto_pay_amount: req.body.autoPayAmount
    };
    
    const card = await plaidRepository.saveCreditCard(req.userId, cardData);
    res.json(card);
  } catch (error) {
    logger.error('Error saving card:', error);
    res.status(500).json({ error: 'Failed to save credit card' });
  }
}));

/**
 * Delete credit card configuration
 * DELETE /api/v1/cards/:accountId
 */
router.delete('/:accountId', authMiddleware, apiLimiter, asyncHandler(async (req, res) => {
  const { accountId } = req.params;
  
  try {
    await plaidRepository.deleteCreditCard(req.userId, accountId);
    res.json({ success: true, message: 'Credit card deleted' });
  } catch (error) {
    logger.error('Error deleting card:', error);
    res.status(500).json({ error: 'Failed to delete credit card' });
  }
}));

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

module.exports = router;
