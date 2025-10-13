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
       DO UPDATE SET access_token = $3, updated_at = CURRENT_TIMESTAMP
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
      due_date,
      auto_pay_enabled,
      auto_pay_amount
    } = cardData;

    const result = await db.query(
      `INSERT INTO credit_cards 
       (user_id, account_id, name, mask, credit_limit, statement_date, due_date, auto_pay_enabled, auto_pay_amount)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
       ON CONFLICT (user_id, account_id)
       DO UPDATE SET 
         name = $3,
         mask = $4,
         credit_limit = $5,
         statement_date = $6,
         due_date = $7,
         auto_pay_enabled = $8,
         auto_pay_amount = $9,
         updated_at = CURRENT_TIMESTAMP
       RETURNING *`,
      [userId, account_id, name, mask, credit_limit, statement_date, due_date, auto_pay_enabled, auto_pay_amount]
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
}

module.exports = new PlaidRepository();

