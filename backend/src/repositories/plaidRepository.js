const db = require('../database/connection');
const encryption = require('../utils/encryption');

class PlaidRepository {
  /**
   * Store Plaid access token
   */
  async storeAccessToken(userId, itemId, accessToken) {
    const encryptedToken = encryption.encrypt(accessToken);
    
    const result = await db.query(
      `INSERT INTO plaid_items (user_id, item_id, access_token) 
       VALUES ($1, $2, $3) 
       ON CONFLICT (item_id) 
       DO UPDATE SET access_token = EXCLUDED.access_token, updated_at = CURRENT_TIMESTAMP
       RETURNING id, item_id, created_at`,
      [userId, itemId, encryptedToken]
    );
    
    return result[0];
  }

  /**
   * Get access token by item ID
   */
  async getAccessToken(itemId) {
    const result = await db.query(
      'SELECT access_token, user_id FROM plaid_items WHERE item_id = $1',
      [itemId]
    );
    
    if (!result || result.length === 0) {
      return null;
    }

    return {
      accessToken: encryption.decrypt(result[0].access_token),
      userId: result[0].user_id
    };
  }

  /**
   * Get all items for a user
   */
  async getUserItems(userId) {
    const result = await db.query(
      'SELECT id, item_id, created_at FROM plaid_items WHERE user_id = $1',
      [userId]
    );
    
    return result;
  }

  /**
   * Delete Plaid item
   */
  async deleteItem(itemId, userId) {
    await db.query(
      'DELETE FROM plaid_items WHERE item_id = $1 AND user_id = $2',
      [itemId, userId]
    );
  }

  /**
   * Store or update credit card configuration
   */
  async saveCreditCard(userId, cardData) {
    const {
      account_id,
      name,
      mask,
      credit_limit,
      statement_date,
      due_date = null,  // Optional
      auto_pay_enabled = false,  // Optional, default false
      auto_pay_amount = null  // Optional
    } = cardData;

    // Convert boolean to SQLite integer (0 or 1)
    const autoPayEnabledInt = auto_pay_enabled ? 1 : 0;

    const result = await db.query(
      `INSERT INTO credit_cards 
       (user_id, account_id, name, mask, credit_limit, statement_date, due_date, auto_pay_enabled, auto_pay_amount)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
       ON CONFLICT (user_id, account_id)
       DO UPDATE SET 
         name = excluded.name,
         mask = excluded.mask,
         credit_limit = excluded.credit_limit,
         statement_date = excluded.statement_date,
         due_date = excluded.due_date,
         auto_pay_enabled = excluded.auto_pay_enabled,
         auto_pay_amount = excluded.auto_pay_amount,
         updated_at = CURRENT_TIMESTAMP
       RETURNING *`,
      [userId, account_id, name, mask, credit_limit, statement_date, due_date, autoPayEnabledInt, auto_pay_amount]
    );

    return result[0];
  }

  /**
   * Get credit cards for a user
   */
  async getCreditCards(userId) {
    const result = await db.query(
      'SELECT * FROM credit_cards WHERE user_id = $1 ORDER BY name',
      [userId]
    );
    
    return result;
  }

  /**
   * Get a specific credit card
   */
  async getCreditCard(userId, accountId) {
    const result = await db.query(
      'SELECT * FROM credit_cards WHERE user_id = $1 AND account_id = $2',
      [userId, accountId]
    );
    
    return result[0];
  }

  /**
   * Delete credit card
   */
  async deleteCreditCard(userId, accountId) {
    await db.query(
      'DELETE FROM credit_cards WHERE user_id = $1 AND account_id = $2',
      [userId, accountId]
    );
  }

  /**
   * Log webhook event
   */
  async logWebhook(webhookType, webhookCode, itemId, payload) {
    await db.query(
      'INSERT INTO webhook_logs (webhook_type, webhook_code, item_id, payload) VALUES ($1, $2, $3, $4)',
      [webhookType, webhookCode, itemId, JSON.stringify(payload)]
    );
  }

  /**
   * Record a manual payment
   * Stores as a transaction that will be merged with Plaid transactions
   */
  async recordManualPayment(userId, cardId, date, amountCents) {
    // Insert or update the payment
    await db.query(
      `INSERT INTO manual_payments 
       (user_id, account_id, payment_date, amount_cents, source)
       VALUES ($1, $2, $3, $4, 'manual')
       ON CONFLICT (user_id, account_id, payment_date, amount_cents)
       DO UPDATE SET updated_at = CURRENT_TIMESTAMP`,
      [userId, cardId, date, amountCents]
    );
    
    // Fetch the payment record (SQLite RETURNING doesn't work well with ON CONFLICT)
    const result = await db.query(
      `SELECT id, account_id, payment_date, amount_cents, source
       FROM manual_payments 
       WHERE user_id = $1 AND account_id = $2 AND payment_date = $3 AND amount_cents = $4`,
      [userId, cardId, date, amountCents]
    );
    
    const payment = result[0];
    
    if (!payment) {
      throw new Error('Failed to record payment');
    }
    
    // Map database fields (snake_case) to API format (camelCase)
    return {
      id: String(payment.id),
      cardId: payment.account_id,
      date: payment.payment_date,
      amountCents: payment.amount_cents,
      source: payment.source
    };
  }

  /**
   * Get manual payments for a card
   */
  async getManualPayments(userId, cardId) {
    const result = await db.query(
      `SELECT *
       FROM manual_payments 
       WHERE user_id = $1 AND account_id = $2 
       ORDER BY payment_date DESC`,
      [userId, cardId]
    );
    
    // Map database fields to API format
    return result.map(payment => ({
      id: payment.id.toString(),
      cardId: payment.account_id,
      date: payment.payment_date,
      amountCents: payment.amount_cents,
      source: payment.source
    }));
  }

  /**
   * Delete a manual payment
   */
  async deleteManualPayment(userId, paymentId) {
    await db.query(
      'DELETE FROM manual_payments WHERE id = $1 AND user_id = $2',
      [paymentId, userId]
    );
  }

  /**
   * Get all manual payments for a user (for merging with transactions)
   */
  async getAllManualPayments(userId) {
    const result = await db.query(
      `SELECT id, account_id as "cardId", payment_date as date, amount_cents as "amountCents", source, created_at
       FROM manual_payments 
       WHERE user_id = $1 
       ORDER BY payment_date DESC`,
      [userId]
    );
    
    return result;
  }
}

module.exports = new PlaidRepository();

